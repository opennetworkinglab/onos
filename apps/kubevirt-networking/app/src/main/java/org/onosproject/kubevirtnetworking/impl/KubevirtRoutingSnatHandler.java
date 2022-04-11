/*
 * Copyright 2021-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.kubevirtnetworking.impl;

import org.onlab.packet.ARP;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.KubevirtFlowRuleService;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkService;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtPortEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtPortListener;
import org.onosproject.kubevirtnetworking.api.KubevirtPortService;
import org.onosproject.kubevirtnetworking.api.KubevirtRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterListener;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterService;
import org.onosproject.kubevirtnetworking.util.RulePopulatorUtil;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.Constants.DEFAULT_GATEWAY_MAC;
import static org.onosproject.kubevirtnetworking.api.Constants.FORWARDING_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.GW_DROP_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.GW_ENTRY_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_ARP_GATEWAY_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_FORWARDING_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_STATEFUL_SNAT_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.TUNNEL_DEFAULT_TABLE;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.GENEVE;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.GRE;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.STT;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.VLAN;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.VXLAN;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.buildGarpPacket;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.externalPatchPortNum;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.gatewayNodeForSpecifiedRouter;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.getRouterForKubevirtPort;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.getRouterMacAddress;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.getRouterSnatIpAddress;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.tunnelPort;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.CT_NAT_SRC_FLAG;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.buildExtension;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles kubevirt routing snat.
 */

@Component(immediate = true)
public class KubevirtRoutingSnatHandler {
    protected final Logger log = getLogger(getClass());
    private static final int DEFAULT_TTL = 0xff;

    private static final int TP_PORT_MINIMUM_NUM = 1025;
    private static final int TP_PORT_MAXIMUM_NUM = 65535;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceAdminService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtPortService kubevirtPortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNodeService kubevirtNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNetworkService kubevirtNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtFlowRuleService flowService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtRouterService kubevirtRouterService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));

    private final InternalKubevirtPortListener kubevirtPortListener =
            new InternalKubevirtPortListener();

    private final InternalRouterEventListener kubevirtRouterlistener =
            new InternalRouterEventListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());

        kubevirtPortService.addListener(kubevirtPortListener);
        kubevirtRouterService.addListener(kubevirtRouterlistener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        leadershipService.withdraw(appId.name());
        kubevirtPortService.removeListener(kubevirtPortListener);
        kubevirtRouterService.removeListener(kubevirtRouterlistener);

        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private void initGatewayNodeSnatForRouter(KubevirtRouter router, String gateway, boolean install) {
        if (gateway == null) {
            log.warn("Fail to initialize gateway node snat for router {} " +
                    "because there's no gateway assigned to it", router.name());
            return;
        }

        KubevirtNode electedGw = kubevirtNodeService.node(gateway);
        if (electedGw == null) {
            log.warn("Fail to initialize gateway node snat for router {} " +
                    "because there's no gateway assigned to it", router.name());
            return;
        }

        String routerSnatIp = router.external().keySet().stream().findAny().orElse(null);

        if (routerSnatIp == null) {
            log.warn("Fail to initialize gateway node snat for router {} " +
                    "because there's no gateway snat ip assigned to it", router.name());
            return;
        }

        String externalNet = router.external().values().stream().findAny().orElse(null);
        if (externalNet == null) {
            return;
        }

        if (router.peerRouter() != null &&
                router.peerRouter().ipAddress() != null && router.peerRouter().macAddress() != null) {
            setArpResponseToPeerRouter(electedGw, Ip4Address.valueOf(routerSnatIp), install);
            setStatefulSnatUpstreamRules(electedGw, router, Ip4Address.valueOf(routerSnatIp),
                    router.peerRouter().macAddress(), install);
            setStatefulSnatDownstreamRuleForRouter(electedGw, router, Ip4Address.valueOf(routerSnatIp),
                    kubevirtNetworkService.network(externalNet), install);
        }
    }

    private void setArpResponseToPeerRouter(KubevirtNode gatewayNode, Ip4Address ip4Address, boolean install) {

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(externalPatchPortNum(deviceService, gatewayNode))
                .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                .matchArpOp(ARP.OP_REQUEST)
                .matchArpTpa(ip4Address)
                .build();

        Device device = deviceService.getDevice(gatewayNode.intgBridge());

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .extension(RulePopulatorUtil.buildMoveEthSrcToDstExtension(device), device.id())
                .extension(RulePopulatorUtil.buildMoveArpShaToThaExtension(device), device.id())
                .extension(RulePopulatorUtil.buildMoveArpSpaToTpaExtension(device), device.id())
                .setArpOp(ARP.OP_REPLY)
                .setEthSrc(DEFAULT_GATEWAY_MAC)
                .setArpSha(DEFAULT_GATEWAY_MAC)
                .setArpSpa(ip4Address)
                .setOutput(PortNumber.IN_PORT)
                .build();

        flowService.setRule(
                appId,
                gatewayNode.intgBridge(),
                selector,
                treatment,
                PRIORITY_ARP_GATEWAY_RULE,
                GW_ENTRY_TABLE,
                install);
    }

    private void setStatefulSnatUpstreamRules(KubevirtNode gatewayNode,
                                              KubevirtRouter router,
                                              Ip4Address routerSnatIp,
                                              MacAddress peerRouterMacAddress,
                                              boolean install) {
        MacAddress routerMacAddress = getRouterMacAddress(router);
        if (routerMacAddress == null) {
            return;
        }

        if (routerSnatIp == null || peerRouterMacAddress == null) {
            return;
        }

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchEthDst(routerMacAddress);

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        ExtensionTreatment natTreatment = RulePopulatorUtil
                .niciraConnTrackTreatmentBuilder(driverService, gatewayNode.intgBridge())
                .commit(true)
                .natFlag(CT_NAT_SRC_FLAG)
                .natAction(true)
                .natIp(routerSnatIp)
                .natPortMin(TpPort.tpPort(TP_PORT_MINIMUM_NUM))
                .natPortMax(TpPort.tpPort(TP_PORT_MAXIMUM_NUM))
                .build();

        tBuilder.extension(natTreatment, gatewayNode.intgBridge())
                .setEthDst(peerRouterMacAddress)
                .setEthSrc(DEFAULT_GATEWAY_MAC)
                .setOutput(externalPatchPortNum(deviceService, gatewayNode));

        flowService.setRule(
                appId,
                gatewayNode.intgBridge(),
                selector.build(),
                tBuilder.build(),
                PRIORITY_STATEFUL_SNAT_RULE,
                GW_ENTRY_TABLE,
                install);
    }

    private void setStatefulSnatDownStreamRuleForKubevirtPort(KubevirtRouter router,
                                                              KubevirtNode gatewayNode,
                                                              KubevirtPort kubevirtPort,
                                                              boolean install) {
        MacAddress routerMacAddress = getRouterMacAddress(router);

        if (routerMacAddress == null) {
            log.error("Failed to set stateful snat downstream rule because " +
                    "there's no br-int port for device {}", gatewayNode.intgBridge());
            return;
        }

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchEthSrc(routerMacAddress)
                .matchIPDst(IpPrefix.valueOf(kubevirtPort.ipAddress(), 32));

        KubevirtNetwork network = kubevirtNetworkService.network(kubevirtPort.networkId());

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setEthDst(kubevirtPort.macAddress())
                .transition(FORWARDING_TABLE);

        flowService.setRule(
                appId,
                gatewayNode.intgBridge(),
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_STATEFUL_SNAT_RULE,
                GW_DROP_TABLE,
                install);

        if (network.type() == VXLAN || network.type() == GENEVE || network.type() == GRE || network.type() == STT) {
            setDownStreamRulesToGatewayTunBridge(network, gatewayNode, kubevirtPort, install);
        }
    }

    private void setDownStreamRulesToGatewayTunBridge(KubevirtNetwork network,
                                                      KubevirtNode electedGw,
                                                      KubevirtPort port, boolean install) {
        KubevirtNode workerNode = kubevirtNodeService.node(port.deviceId());
        if (workerNode == null) {
            return;
        }

        PortNumber tunnelPortNumber = tunnelPort(electedGw, network);
        if (tunnelPortNumber == null) {
            return;
        }

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(IpPrefix.valueOf(port.ipAddress(), 32))
                .matchEthDst(port.macAddress());

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setTunnelId(Long.parseLong(network.segmentId()))
                .extension(buildExtension(
                        deviceService,
                        electedGw.tunBridge(),
                        workerNode.dataIp().getIp4Address()),
                        electedGw.tunBridge())
                .setOutput(tunnelPortNumber);

        flowService.setRule(
                appId,
                electedGw.tunBridge(),
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_FORWARDING_RULE,
                TUNNEL_DEFAULT_TABLE,
                install);
    }

    private void setStatefulSnatDownstreamRuleForRouter(KubevirtNode gatewayNode,
                                                        KubevirtRouter router,
                                                        IpAddress routerSnatIp,
                                                        KubevirtNetwork externalNetwork,
                                                        boolean install) {

        MacAddress routerMacAddress = getRouterMacAddress(router);

        if (routerMacAddress == null) {
            log.warn("Failed to set stateful snat downstream rule because " +
                    "there's no br-int port for device {}", gatewayNode.intgBridge());
            return;
        }

        if (externalNetwork == null) {
            log.warn("Failed to set stateful snat downstream rule because " +
                    "there's no external network router {}", router.name());
            return;
        }

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4);

        if (externalNetwork.type() == VLAN) {
            sBuilder.matchVlanId(VlanId.vlanId(externalNetwork.segmentId()));
            tBuilder.popVlan();
        }

        sBuilder.matchIPDst(IpPrefix.valueOf(routerSnatIp, 32));

        ExtensionTreatment natTreatment = RulePopulatorUtil
                .niciraConnTrackTreatmentBuilder(driverService, gatewayNode.intgBridge())
                .commit(false)
                .natAction(true)
                .table((short) GW_DROP_TABLE)
                .build();

        tBuilder.setEthSrc(routerMacAddress)
                .extension(natTreatment, gatewayNode.intgBridge());

        flowService.setRule(
                appId,
                gatewayNode.intgBridge(),
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_STATEFUL_SNAT_RULE,
                GW_ENTRY_TABLE,
                install);
    }

    private class InternalRouterEventListener implements KubevirtRouterListener {
        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtRouterEvent event) {
            switch (event.type()) {
                case KUBEVIRT_ROUTER_CREATED:
                    eventExecutor.execute(() -> processRouterCreation(event.subject()));
                    break;
                case KUBEVIRT_SNAT_STATUS_DISABLED:
                case KUBEVIRT_ROUTER_REMOVED:
                    eventExecutor.execute(() -> processRouterDeletion(event.subject()));
                    break;
                case KUBEVIRT_ROUTER_UPDATED:
                    eventExecutor.execute(() -> processRouterUpdate(event.subject()));
                    break;
                case KUBEVIRT_ROUTER_INTERNAL_NETWORKS_ATTACHED:
                    eventExecutor.execute(() -> processRouterInternalNetworksAttached(event.subject(),
                            event.internal()));
                    break;
                case KUBEVIRT_ROUTER_INTERNAL_NETWORKS_DETACHED:
                    eventExecutor.execute(() -> processRouterInternalNetworksDetached(event.subject(),
                            event.internal()));
                    break;
                case KUBEVIRT_GATEWAY_NODE_ATTACHED:
                    eventExecutor.execute(() -> processRouterGatewayNodeAttached(event.subject(),
                            event.gateway()));
                    break;
                case KUBEVIRT_GATEWAY_NODE_DETACHED:
                    eventExecutor.execute(() -> processRouterGatewayNodeDetached(event.subject(),
                            event.gateway()));
                    break;
                case KUBEVIRT_GATEWAY_NODE_CHANGED:
                    eventExecutor.execute(() -> processRouterGatewayNodeChanged(event.subject(),
                            event.gateway()));
                    break;
                case KUBEVIRT_ROUTER_EXTERNAL_NETWORK_ATTACHED:
                    eventExecutor.execute(() -> processRouterExternalNetAttached(event.subject(),
                            event.externalIp(), event.externalNet(),
                            event.externalPeerRouterIp(), event.peerRouterMac()));
                    break;
                case KUBEVIRT_ROUTER_EXTERNAL_NETWORK_DETACHED:
                    eventExecutor.execute(() -> processRouterExternalNetDetached(event.subject(),
                            event.externalIp(), event.externalNet(),
                            event.externalPeerRouterIp(), event.peerRouterMac()));
                    break;
                default:
                    //do nothing
                    break;
            }
        }

        private void processRouterExternalNetAttached(KubevirtRouter router, String externalIp, String externalNet,
                                                      String peerRouterIp, MacAddress peerRouterMac) {
            if (!isRelevantHelper()) {
                return;
            }
            KubevirtNode electedGw = gatewayNodeForSpecifiedRouter(kubevirtNodeService, router);

            if (electedGw == null) {
                log.warn("Fail to process router external network attached gateway node snat for router {} " +
                        "there's no gateway assigned to it", router.name());
                return;
            }

            if (router.enableSnat() &&
                    peerRouterIp != null && peerRouterMac != null && externalIp != null && externalNet != null) {
                setArpResponseToPeerRouter(electedGw, Ip4Address.valueOf(externalIp), true);
                setStatefulSnatUpstreamRules(electedGw, router, Ip4Address.valueOf(externalIp),
                        peerRouterMac, true);
                setStatefulSnatDownstreamRuleForRouter(electedGw, router, Ip4Address.valueOf(externalIp),
                        kubevirtNetworkService.network(externalNet), true);
            }

            router.internal()
                    .stream()
                    .filter(networkId -> kubevirtNetworkService.network(networkId) != null)
                    .map(kubevirtNetworkService::network)
                    .forEach(network -> {
                        kubevirtPortService.ports(network.networkId()).forEach(kubevirtPort -> {
                            setStatefulSnatDownStreamRuleForKubevirtPort(router,
                                    electedGw, kubevirtPort, true);
                        });
                    });
        }

        private void processRouterExternalNetDetached(KubevirtRouter router, String externalIp, String externalNet,
                                                      String peerRouterIp, MacAddress peerRouterMac) {
            if (!isRelevantHelper()) {
                return;
            }
            if (!isRelevantHelper()) {
                return;
            }
            KubevirtNode electedGw = kubevirtNodeService.node(router.electedGateway());

            if (electedGw == null) {
                log.warn("Fail to process router external network attached gateway node snat for router {} " +
                        "there's no gateway assigned to it", router.name());
                return;
            }

            if (router.enableSnat() &&
                    peerRouterIp != null && peerRouterMac != null && externalIp != null && externalNet != null) {
                setArpResponseToPeerRouter(electedGw, Ip4Address.valueOf(externalIp), false);
                setStatefulSnatUpstreamRules(electedGw, router,
                        Ip4Address.valueOf(externalIp), peerRouterMac, false);
                setStatefulSnatDownstreamRuleForRouter(electedGw, router, Ip4Address.valueOf(externalIp),
                        kubevirtNetworkService.network(externalNet), false);
            }

            router.internal()
                    .stream()
                    .filter(networkId -> kubevirtNetworkService.network(networkId) != null)
                    .map(kubevirtNetworkService::network)
                    .forEach(network -> {
                        kubevirtPortService.ports(network.networkId()).forEach(kubevirtPort -> {
                            setStatefulSnatDownStreamRuleForKubevirtPort(router,
                                    electedGw, kubevirtPort, false);
                        });
                    });
        }

        private void processRouterGatewayNodeAttached(KubevirtRouter router, String attachedGatewayId) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtNode attachedGateway = kubevirtNodeService.node(attachedGatewayId);
            if (attachedGateway == null) {
                return;
            }

            if (!router.enableSnat()) {
                return;
            }

            router.internal()
                    .stream()
                    .filter(networkId -> kubevirtNetworkService.network(networkId) != null)
                    .map(kubevirtNetworkService::network)
                    .forEach(network -> {
                        String routerSnatIp = router.external().keySet().stream().findAny().orElse(null);
                        if (routerSnatIp == null) {
                            return;
                        }
                        kubevirtPortService.ports(network.networkId()).forEach(kubevirtPort -> {
                            setStatefulSnatDownStreamRuleForKubevirtPort(router,
                                    attachedGateway, kubevirtPort, true);
                });
            });
        }

        private void processRouterGatewayNodeDetached(KubevirtRouter router, String detachedGatewayId) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtNode detachedGateway = kubevirtNodeService.node(detachedGatewayId);
            if (detachedGateway == null) {
                return;
            }

            if (!router.enableSnat()) {
                return;
            }

            router.internal()
                    .stream()
                    .filter(networkId -> kubevirtNetworkService.network(networkId) != null)
                    .map(kubevirtNetworkService::network)
                    .forEach(network -> {
                        String routerSnatIp = router.external().keySet().stream().findAny().orElse(null);
                        if (routerSnatIp == null) {
                            return;
                        }

                        kubevirtPortService.ports(network.networkId()).forEach(kubevirtPort -> {
                            setStatefulSnatDownStreamRuleForKubevirtPort(router,
                                    detachedGateway, kubevirtPort, false);
                        });
                    });
        }

        private void processRouterInternalNetworksAttached(KubevirtRouter router,
                                                           Set<String> attachedInternalNetworks) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtNode gwNode = gatewayNodeForSpecifiedRouter(kubevirtNodeService, router);
            if (gwNode == null) {
                return;
            }

            if (!router.enableSnat()) {
                return;
            }

            attachedInternalNetworks.forEach(networkId -> {
                String routerSnatIp = router.external().keySet().stream().findAny().orElse(null);
                if (routerSnatIp == null) {
                    return;
                }

                kubevirtPortService.ports(networkId).forEach(kubevirtPort -> {
                    setStatefulSnatDownStreamRuleForKubevirtPort(router, gwNode, kubevirtPort, true);
                });
            });
        }

        private void processRouterInternalNetworksDetached(KubevirtRouter router,
                                                           Set<String> detachedInternalNetworks) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtNode gwNode = gatewayNodeForSpecifiedRouter(kubevirtNodeService, router);
            if (gwNode == null) {
                return;
            }

            if (!router.enableSnat()) {
                return;
            }

            detachedInternalNetworks.forEach(networkId -> {
                String routerSnatIp = router.external().keySet().stream().findAny().orElse(null);
                if (routerSnatIp == null) {
                    log.info("snatIp is null");
                    return;
                }

                kubevirtPortService.ports(networkId).forEach(kubevirtPort -> {
                    setStatefulSnatDownStreamRuleForKubevirtPort(router, gwNode, kubevirtPort, false);
                });
            });
        }
        private void processRouterCreation(KubevirtRouter router) {
            if (!isRelevantHelper()) {
                return;
            }
            if (router.enableSnat() && !router.external().isEmpty() && router.peerRouter() != null) {
                initGatewayNodeSnatForRouter(router, router.electedGateway(), true);
            }
        }

        private void processRouterDeletion(KubevirtRouter router) {
            if (!isRelevantHelper()) {
                return;
            }


            if (!router.external().isEmpty() && router.peerRouter() != null && router.electedGateway() != null) {
                initGatewayNodeSnatForRouter(router, router.electedGateway(), false);

                KubevirtNode gatewayNode = kubevirtNodeService.node(router.electedGateway());

                router.internal()
                        .stream()
                        .filter(networkId -> kubevirtNetworkService.network(networkId) != null)
                        .map(kubevirtNetworkService::network)
                        .forEach(network -> {
                            String routerSnatIp = router.external().keySet().stream().findAny().orElse(null);
                            if (routerSnatIp == null) {
                                return;
                            }

                            kubevirtPortService.ports(network.networkId()).forEach(kubevirtPort -> {
                                setStatefulSnatDownStreamRuleForKubevirtPort(router,
                                        gatewayNode, kubevirtPort, false);
                            });
                        });
            }
        }

        private void processRouterUpdate(KubevirtRouter router) {
            if (!isRelevantHelper()) {
                return;
            }
            if (router.enableSnat() && !router.external().isEmpty() && router.peerRouter() != null) {
                initGatewayNodeSnatForRouter(router, router.electedGateway(), true);

                KubevirtNode gatewayNode = kubevirtNodeService.node(router.electedGateway());

                router.internal()
                        .stream()
                        .filter(networkId -> kubevirtNetworkService.network(networkId) != null)
                        .map(kubevirtNetworkService::network)
                        .forEach(network -> {
                            String routerSnatIp = router.external().keySet().stream().findAny().orElse(null);
                            if (routerSnatIp == null) {
                                return;
                            }

                            kubevirtPortService.ports(network.networkId()).forEach(kubevirtPort -> {
                                setStatefulSnatDownStreamRuleForKubevirtPort(router,
                                        gatewayNode, kubevirtPort, true);
                            });
                        });
                sendGarpPacketForSnatIp(router);
            }
        }

        private void processRouterGatewayNodeChanged(KubevirtRouter router,
                                                     String disAssociatedGateway) {
            if (!isRelevantHelper()) {
                return;
            }

            if (router.enableSnat() && !router.external().isEmpty() && router.peerRouter() != null) {
                DeviceId disAssociatedGatewayIntDeviceId = kubevirtNodeService.node(disAssociatedGateway).intgBridge();

                //Only do this in case disassociated gateway device is still alive.
                if (disAssociatedGatewayIntDeviceId != null &&
                        deviceService.isAvailable(disAssociatedGatewayIntDeviceId)) {
                    initGatewayNodeSnatForRouter(router, disAssociatedGateway, false);
                }
                initGatewayNodeSnatForRouter(router, router.electedGateway(), true);

                processRouterGatewayNodeDetached(router, disAssociatedGateway);
                processRouterGatewayNodeAttached(router, router.electedGateway());

                sendGarpPacketForSnatIp(router);
            }
        }

        private void sendGarpPacketForSnatIp(KubevirtRouter router) {
            if (router == null || router.electedGateway() == null) {
                return;
            }

            String routerSnatIp = router.external().keySet().stream().findAny().orElse(null);

            if (routerSnatIp == null) {
                log.warn("Fail to initialize gateway node snat for router {} " +
                        "because there's no gateway snat ip assigned to it", router.name());
                return;
            }

            Ethernet ethernet = buildGarpPacket(DEFAULT_GATEWAY_MAC, IpAddress.valueOf(routerSnatIp));

            if (ethernet == null) {
                return;
            }

            KubevirtNode gatewayNode = kubevirtNodeService.node(router.electedGateway());

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(externalPatchPortNum(deviceService, gatewayNode)).build();

            packetService.emit(new DefaultOutboundPacket(gatewayNode.intgBridge(), treatment,
                    ByteBuffer.wrap(ethernet.serialize())));
        }
    }

    private class InternalKubevirtPortListener implements KubevirtPortListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtPortEvent event) {
            switch (event.type()) {
                case KUBEVIRT_PORT_CREATED:
                    eventExecutor.execute(() -> processPortCreation(event.subject()));
                    break;
                case KUBEVIRT_PORT_UPDATED:
                    eventExecutor.execute(() -> processPortUpdate(event.subject()));
                    break;
                case KUBEVIRT_PORT_REMOVED:
                    eventExecutor.execute(() -> processPortDeletion(event.subject()));
                    break;
                default:
                    //do nothing
                    break;
            }
        }

        private void processPortCreation(KubevirtPort kubevirtPort) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtRouter router = getRouterForKubevirtPort(kubevirtRouterService, kubevirtPort);
            if (router == null) {
                return;
            }

            KubevirtNode gwNode = gatewayNodeForSpecifiedRouter(kubevirtNodeService, router);

            if (gwNode != null) {
                IpAddress gatewaySnatIp = getRouterSnatIpAddress(kubevirtRouterService, kubevirtPort.networkId());
                if (gatewaySnatIp == null) {
                    return;
                }
                setStatefulSnatDownStreamRuleForKubevirtPort(router, gwNode, kubevirtPort, true);
            }
        }

        private void processPortUpdate(KubevirtPort kubevirtPort) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtRouter router = getRouterForKubevirtPort(kubevirtRouterService, kubevirtPort);
            if (router == null) {
                return;
            }

            KubevirtNode gwNode = gatewayNodeForSpecifiedRouter(kubevirtNodeService, router);

            if (gwNode != null) {
                setStatefulSnatDownStreamRuleForKubevirtPort(router, gwNode, kubevirtPort, true);
            }
        }

        private void processPortDeletion(KubevirtPort kubevirtPort) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtRouter router = getRouterForKubevirtPort(kubevirtRouterService, kubevirtPort);
            if (router == null) {
                return;
            }

            KubevirtNode gwNode = gatewayNodeForSpecifiedRouter(kubevirtNodeService, router);

            if (gwNode != null) {
                setStatefulSnatDownStreamRuleForKubevirtPort(router, gwNode, kubevirtPort, false);
            }
        }
    }
}
