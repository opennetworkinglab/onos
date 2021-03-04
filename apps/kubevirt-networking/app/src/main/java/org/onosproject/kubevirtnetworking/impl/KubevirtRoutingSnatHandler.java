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
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.KubevirtFlowRuleService;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkListener;
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
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.Constants.DEFAULT_GATEWAY_MAC;
import static org.onosproject.kubevirtnetworking.api.Constants.FLAT_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.FORWARDING_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.api.Constants.PRE_FLAT_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_ARP_GATEWAY_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_STATEFUL_SNAT_RULE;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.gatewayNodeForSpecifiedRouter;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.getRouterSnatIpAddress;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.getbrIntMacAddress;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.CT_NAT_SRC_FLAG;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
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
    protected KubevirtRouterService kubevirtRouterService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));

    private final InternalKubevirtPortListener kubevirtPortListener =
            new InternalKubevirtPortListener();

    private final InternalRouterEventListener kubevirtRouterlistener =
            new InternalRouterEventListener();

    private final InternalNetworkEventListener kubevirtNetworkEventListener =
            new InternalNetworkEventListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());

        kubevirtPortService.addListener(kubevirtPortListener);
        kubevirtRouterService.addListener(kubevirtRouterlistener);
        kubevirtNetworkService.addListener(kubevirtNetworkEventListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        leadershipService.withdraw(appId.name());
        kubevirtPortService.removeListener(kubevirtPortListener);
        kubevirtRouterService.removeListener(kubevirtRouterlistener);
        kubevirtNetworkService.removeListener(kubevirtNetworkEventListener);

        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private void initGatewayNodeSnatForRouter(KubevirtRouter router, boolean install) {
        KubevirtNode electedGw = gatewayNodeForSpecifiedRouter(kubevirtNodeService, router);

        if (electedGw == null) {
            log.warn("Fail to initialize gateway node snat for router {} " +
                    "there's no gateway assigned to it", router.name());
            return;
        }

        String routerSnatIp = router.external().keySet().stream().findAny().orElse(null);

        if (routerSnatIp == null) {
            log.warn("Fail to initialize gateway node snat for router {} " +
                    "there's no gateway snat ip assigned to it", router.name());
            return;
        }

        setArpResponseToPeerRouter(electedGw, Ip4Address.valueOf(routerSnatIp), install);
        setStatefulSnatUpstreamRules(electedGw, router, Ip4Address.valueOf(routerSnatIp), install);
        setStatefulSnatDownstreamRuleForRouter(router, electedGw, Ip4Address.valueOf(routerSnatIp), install);
    }

    private void setArpResponseToPeerRouter(KubevirtNode gatewayNode, Ip4Address ip4Address, boolean install) {

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(externalPatchPortNum(gatewayNode))
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
                PRE_FLAT_TABLE,
                install);
    }

    private void setStatefulSnatUpstreamRules(KubevirtNode gatewayNode, KubevirtRouter router,
                                              Ip4Address ip4Address, boolean install) {

        MacAddress brIntMacAddress = getbrIntMacAddress(deviceService, gatewayNode.intgBridge());

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchEthDst(brIntMacAddress)
                .build();

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        ExtensionTreatment natTreatment = RulePopulatorUtil
                .niciraConnTrackTreatmentBuilder(driverService, gatewayNode.intgBridge())
                .commit(true)
                .natFlag(CT_NAT_SRC_FLAG)
                .natAction(true)
                .natIp(ip4Address)
                .natPortMin(TpPort.tpPort(TP_PORT_MINIMUM_NUM))
                .natPortMax(TpPort.tpPort(TP_PORT_MAXIMUM_NUM))
                .build();

        tBuilder.extension(natTreatment, gatewayNode.intgBridge())
                .setEthDst(router.peerRouter().macAddress())
                .setEthSrc(DEFAULT_GATEWAY_MAC)
                .setOutput(externalPatchPortNum(gatewayNode));

        flowService.setRule(
                appId,
                gatewayNode.intgBridge(),
                selector,
                tBuilder.build(),
                PRIORITY_STATEFUL_SNAT_RULE,
                PRE_FLAT_TABLE,
                install);
    }

    private void setStatefulSnatDownStreamRuleForNetwork(KubevirtNode gatewayNode,
                                                         KubevirtRouter router,
                                                         KubevirtNetwork network,
                                                         boolean install) {
        kubevirtPortService.ports(network.networkId()).forEach(kubevirtPort -> {
            String routerSnatIp = router.external().keySet().stream().findAny().orElse(null);
            if (routerSnatIp == null) {
                return;
            }
            setStatefulSnatDownStreamRuleForKubevirtPort(gatewayNode, IpAddress.valueOf(routerSnatIp),
                    kubevirtPort, install);
        });
    }

    private void setStatefulSnatDownStreamRuleForKubevirtPort(KubevirtNode gatewayNode,
                                                         IpAddress gatewaySnatIp,
                                                         KubevirtPort kubevirtPort,
                                                         boolean install) {
        MacAddress brIntMacAddress = getbrIntMacAddress(deviceService, gatewayNode.intgBridge());

        if (brIntMacAddress == null) {
            log.error("Failed to set stateful snat downstream rule because " +
                    "there's no br-int port for device {}", gatewayNode.intgBridge());
            return;
        }

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(IpPrefix.valueOf(kubevirtPort.ipAddress(), 32));

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthDst(kubevirtPort.macAddress())
                .transition(FORWARDING_TABLE)
                .build();

        flowService.setRule(
                appId,
                gatewayNode.intgBridge(),
                sBuilder.build(),
                treatment,
                PRIORITY_STATEFUL_SNAT_RULE,
                FLAT_TABLE,
                install);
    }

    private void setStatefulSnatDownstreamRuleForRouter(KubevirtRouter router,
                                                        KubevirtNode gatewayNode,
                                                        IpAddress gatewaySnatIp,
                                                        boolean install) {

        MacAddress brIntMacAddress = getbrIntMacAddress(deviceService, gatewayNode.intgBridge());

        if (brIntMacAddress == null) {
            log.error("Failed to set stateful snat downstream rule because " +
                    "there's no br-int port for device {}", gatewayNode.intgBridge());
            return;
        }

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(IpPrefix.valueOf(gatewaySnatIp, 32));

        ExtensionTreatment natTreatment = RulePopulatorUtil
                .niciraConnTrackTreatmentBuilder(driverService, gatewayNode.intgBridge())
                .commit(false)
                .natAction(true)
                .table((short) PRE_FLAT_TABLE)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthSrc(brIntMacAddress)
                .extension(natTreatment, gatewayNode.intgBridge())
                .build();

        flowService.setRule(
                appId,
                gatewayNode.intgBridge(),
                sBuilder.build(),
                treatment,
                PRIORITY_STATEFUL_SNAT_RULE,
                PRE_FLAT_TABLE,
                install);

        router.internal().forEach(networkName -> {
            KubevirtNetwork network = kubevirtNetworkService.network(networkName);

            if (network != null) {
                setStatefulSnatDownStreamRuleForNetwork(gatewayNode, router, network, install);
            }
        });
    }

    private PortNumber externalPatchPortNum(KubevirtNode gatewayNode) {
        Port port = deviceService.getPorts(gatewayNode.intgBridge()).stream()
                .filter(p -> p.isEnabled() &&
                        Objects.equals(p.annotations().value(PORT_NAME), "int-to-gateway"))
                .findAny().orElse(null);

        return port != null ? port.number() : null;
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
                default:
                    //do nothing
                    break;
            }
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

            attachedInternalNetworks.forEach(networkId -> {
                kubevirtPortService.ports(networkId).forEach(kubevirtPort -> {
                    String routerSnatIp = router.external().keySet().stream().findAny().orElse(null);
                    if (routerSnatIp == null) {
                        return;
                    }
                    setStatefulSnatDownStreamRuleForKubevirtPort(gwNode, IpAddress.valueOf(routerSnatIp),
                            kubevirtPort, true);
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

            detachedInternalNetworks.forEach(networkId -> {
                kubevirtPortService.ports(networkId).forEach(kubevirtPort -> {
                    String routerSnatIp = router.external().keySet().stream().findAny().orElse(null);
                    if (routerSnatIp == null) {
                        log.info("snatIp is null");
                        return;
                    }
                    setStatefulSnatDownStreamRuleForKubevirtPort(gwNode, IpAddress.valueOf(routerSnatIp),
                            kubevirtPort, false);
                });
            });
        }
        private void processRouterCreation(KubevirtRouter router) {
            if (!isRelevantHelper()) {
                return;
            }
            if (router.enableSnat() && !router.external().isEmpty() && router.peerRouter() != null) {
                initGatewayNodeSnatForRouter(router, true);
            }
        }

        private void processRouterDeletion(KubevirtRouter router) {
            if (!isRelevantHelper()) {
                return;
            }
            if (router.enableSnat() && !router.external().isEmpty() && router.peerRouter() != null) {
                initGatewayNodeSnatForRouter(router, false);
            }
        }

        private void processRouterUpdate(KubevirtRouter router) {
            if (!isRelevantHelper()) {
                return;
            }
            if (router.enableSnat() && !router.external().isEmpty() && router.peerRouter() != null) {
                initGatewayNodeSnatForRouter(router, true);
            }
        }
    }

    private class InternalNetworkEventListener implements KubevirtNetworkListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtNetworkEvent event) {
            switch (event.type()) {
                case KUBEVIRT_NETWORK_CREATED:
                    eventExecutor.execute(() -> processNetworkCreation(event.subject()));
                    break;
                case KUBEVIRT_NETWORK_REMOVED:
                    eventExecutor.execute(() -> processNetworkRemoval(event.subject()));
                    break;
                case KUBEVIRT_NETWORK_UPDATED:
                default:
                    // do nothing
                    break;
            }
        }

        private void processNetworkCreation(KubevirtNetwork network) {
            if (!isRelevantHelper()) {
                return;
            }

            switch (network.type()) {
                case VXLAN:
                case GRE:
                case GENEVE:
                    break;
                case FLAT:
                case VLAN:
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        private void processNetworkRemoval(KubevirtNetwork network) {
            if (!isRelevantHelper()) {
                return;
            }

            switch (network.type()) {
                case VXLAN:
                case GRE:
                case GENEVE:
                    break;
                case FLAT:
                case VLAN:
                    break;
                default:
                    // do nothing
                    break;
            }
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

            KubevirtRouter router = routerForKubevirtPort(kubevirtPort);
            if (router == null) {
                return;
            }

            KubevirtNode gwNode = gatewayNodeForSpecifiedRouter(kubevirtNodeService, router);

            if (gwNode != null) {
                IpAddress gatewaySnatIp = getRouterSnatIpAddress(kubevirtRouterService, kubevirtPort.networkId());
                if (gatewaySnatIp == null) {
                    return;
                }
                setStatefulSnatDownStreamRuleForKubevirtPort(gwNode, gatewaySnatIp, kubevirtPort, true);
            }
        }

        private void processPortUpdate(KubevirtPort kubevirtPort) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtRouter router = routerForKubevirtPort(kubevirtPort);
            if (router == null) {
                return;
            }

            KubevirtNode gwNode = gatewayNodeForSpecifiedRouter(kubevirtNodeService, router);

            if (gwNode != null) {
                IpAddress gatewaySnatIp = getRouterSnatIpAddress(kubevirtRouterService, kubevirtPort.networkId());
                if (gatewaySnatIp == null) {
                    return;
                }
                setStatefulSnatDownStreamRuleForKubevirtPort(gwNode, gatewaySnatIp, kubevirtPort, true);
            }
        }

        private void processPortDeletion(KubevirtPort kubevirtPort) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtRouter router = routerForKubevirtPort(kubevirtPort);
            if (router == null) {
                return;
            }

            KubevirtNode gwNode = gatewayNodeForSpecifiedRouter(kubevirtNodeService, router);

            if (gwNode != null) {
                IpAddress gatewaySnatIp = getRouterSnatIpAddress(kubevirtRouterService, kubevirtPort.networkId());
                if (gatewaySnatIp == null) {
                    return;
                }
                setStatefulSnatDownStreamRuleForKubevirtPort(gwNode, gatewaySnatIp, kubevirtPort, false);
            }
        }

        private KubevirtRouter routerForKubevirtPort(KubevirtPort kubevirtPort) {
            if (kubevirtPort.ipAddress() != null) {
                return kubevirtRouterService.routers().stream()
                        .filter(r -> r.internal().contains(kubevirtPort.networkId()))
                        .findAny().orElse(null);
            }

            return null;
        }
    }
}
