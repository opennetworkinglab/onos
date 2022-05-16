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
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.KubevirtFloatingIp;
import org.onosproject.kubevirtnetworking.api.KubevirtFlowRuleService;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkService;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtPortService;
import org.onosproject.kubevirtnetworking.api.KubevirtRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterListener;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterService;
import org.onosproject.kubevirtnetworking.util.RulePopulatorUtil;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeEvent;
import org.onosproject.kubevirtnode.api.KubevirtNodeListener;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
import org.onosproject.net.Device;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
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
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.Constants.FORWARDING_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.GW_ENTRY_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_ARP_GATEWAY_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_FLOATING_GATEWAY_TUN_BRIDGE_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_FLOATING_IP_DOWNSTREAM_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_FLOATING_IP_UPSTREAM_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.TUNNEL_DEFAULT_TABLE;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.GENEVE;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.GRE;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.STT;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.VXLAN;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.buildGarpPacket;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.externalPatchPortNum;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.getRouterMacAddress;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.tunnelPort;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.buildExtension;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles kubevirt floating ip.
 */
@Component(immediate = true)
public class KubevirtFloatingIpHandler {
    protected final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceAdminService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

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

    private ApplicationId appId;
    private NodeId localNodeId;

    private final InternalRouterEventListener kubevirtRouterListener =
            new InternalRouterEventListener();
    private final InternalNodeListener kubevirtNodeListener =
            new InternalNodeListener();

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        kubevirtRouterService.addListener(kubevirtRouterListener);
        kubevirtNodeService.addListener(kubevirtNodeListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        leadershipService.withdraw(appId.name());
        kubevirtRouterService.removeListener(kubevirtRouterListener);
        kubevirtNodeService.removeListener(kubevirtNodeListener);

        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private void setFloatingIpRulesForFip(KubevirtRouter router,
                                          KubevirtFloatingIp floatingIp,
                                          KubevirtNode electedGw,
                                          boolean install) {

        KubevirtPort kubevirtPort = getKubevirtPortByFloatingIp(floatingIp);
        if (kubevirtPort == null) {
            return;
        }

        KubevirtNetwork kubevirtNetwork = kubevirtNetworkService.network(kubevirtPort.networkId());
        if (kubevirtNetwork.type() == VXLAN || kubevirtNetwork.type() == GENEVE ||
                kubevirtNetwork.type() == GRE || kubevirtNetwork.type() == STT) {
            setFloatingIpDownstreamRulesToGatewayTunBridge(floatingIp,
                    electedGw, kubevirtNetwork, kubevirtPort, install);
        }

        setFloatingIpArpResponseRules(router, floatingIp, kubevirtPort, electedGw, install);
        setFloatingIpUpstreamRules(router, floatingIp, kubevirtPort, electedGw, install);
        setFloatingIpDownstreamRules(router, floatingIp, kubevirtPort, electedGw, install);
    }

    private void setFloatingIpArpResponseRules(KubevirtRouter router,
                                               KubevirtFloatingIp floatingIp,
                                               KubevirtPort port,
                                               KubevirtNode electedGw,
                                               boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(externalPatchPortNum(deviceService, electedGw))
                .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                .matchArpOp(ARP.OP_REQUEST)
                .matchArpTpa(floatingIp.floatingIp().getIp4Address())
                .build();

        Device device = deviceService.getDevice(electedGw.intgBridge());

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .extension(RulePopulatorUtil.buildMoveEthSrcToDstExtension(device), device.id())
                .extension(RulePopulatorUtil.buildMoveArpShaToThaExtension(device), device.id())
                .extension(RulePopulatorUtil.buildMoveArpSpaToTpaExtension(device), device.id())
                .setArpOp(ARP.OP_REPLY)
                .setEthSrc(port.macAddress())
                .setArpSha(port.macAddress())
                .setArpSpa(floatingIp.floatingIp().getIp4Address())
                .setOutput(PortNumber.IN_PORT)
                .build();

        flowService.setRule(
                appId,
                electedGw.intgBridge(),
                selector,
                treatment,
                PRIORITY_ARP_GATEWAY_RULE,
                GW_ENTRY_TABLE,
                install);
    }

    private KubevirtPort getKubevirtPortByFloatingIp(KubevirtFloatingIp floatingIp) {

        return kubevirtPortService.ports().stream()
                .filter(port -> port.ipAddress().equals(floatingIp.fixedIp()))
                .filter(port -> port.vmName().equals(floatingIp.vmName()))
                .findAny().orElse(null);
    }

    private void setFloatingIpUpstreamRules(KubevirtRouter router,
                                            KubevirtFloatingIp floatingIp,
                                            KubevirtPort port,
                                            KubevirtNode electedGw,
                                            boolean install) {

        MacAddress peerMacAddress = router.peerRouter().macAddress();

        if (peerMacAddress == null) {
            log.warn("Failed to install floating Ip rules for floating ip {} and router {}" +
                    "because there's no peer router mac address", floatingIp.floatingIp(),
                    router.name());
            return;
        }

        MacAddress routerMacAddress = getRouterMacAddress(router);

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchEthSrc(port.macAddress())
                .matchEthDst(routerMacAddress)
                .matchIPSrc(IpPrefix.valueOf(floatingIp.fixedIp(), 32))
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthDst(peerMacAddress)
                .setEthSrc(port.macAddress())
                .setIpSrc(floatingIp.floatingIp())
                .setOutput(externalPatchPortNum(deviceService, electedGw))
                .build();

        flowService.setRule(
                appId,
                electedGw.intgBridge(),
                selector,
                treatment,
                PRIORITY_FLOATING_IP_UPSTREAM_RULE,
                GW_ENTRY_TABLE,
                install);
    }

    private void setFloatingIpDownstreamRules(KubevirtRouter router,
                                              KubevirtFloatingIp floatingIp,
                                              KubevirtPort port,
                                              KubevirtNode electedGw,
                                              boolean install) {
        MacAddress routerMacAddress = getRouterMacAddress(router);

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchEthDst(port.macAddress())
                .matchIPDst(IpPrefix.valueOf(floatingIp.floatingIp(), 32))
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthSrc(routerMacAddress)
                .setEthDst(port.macAddress())
                .setIpDst(floatingIp.fixedIp())
                .transition(FORWARDING_TABLE)
                .build();

        flowService.setRule(
                appId,
                electedGw.intgBridge(),
                selector,
                treatment,
                PRIORITY_FLOATING_IP_DOWNSTREAM_RULE,
                GW_ENTRY_TABLE,
                install);
    }

    private void setFloatingIpDownstreamRulesToGatewayTunBridge(KubevirtFloatingIp floatingIp,
                                                                KubevirtNode electedGw,
                                                                KubevirtNetwork network,
                                                                KubevirtPort port,
                                                                boolean install) {
        KubevirtNode workerNode = kubevirtNodeService.node(port.deviceId());
        if (workerNode == null) {
            log.warn("Failed to install floating Ip rules for floating ip {} " +
                    "because fail to fine the worker node that the associated port is running on",
                    floatingIp.floatingIp());
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
                PRIORITY_FLOATING_GATEWAY_TUN_BRIDGE_RULE,
                TUNNEL_DEFAULT_TABLE,
                install);
    }

    private void processGarpPacketForFloatingIp(KubevirtFloatingIp floatingIp, KubevirtNode electedGw) {

        if (floatingIp == null) {
            return;
        }

        KubevirtPort kubevirtPort = getKubevirtPortByFloatingIp(floatingIp);
        if (kubevirtPort == null) {
            return;
        }

        Ethernet ethernet = buildGarpPacket(kubevirtPort.macAddress(), floatingIp.floatingIp());
        if (ethernet == null) {
            return;
        }

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(externalPatchPortNum(deviceService, electedGw)).build();

        packetService.emit(new DefaultOutboundPacket(electedGw.intgBridge(), treatment,
                ByteBuffer.wrap(ethernet.serialize())));
    }

    private class InternalRouterEventListener implements KubevirtRouterListener {
        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtRouterEvent event) {
            switch (event.type()) {
                case KUBEVIRT_FLOATING_IP_ASSOCIATED:
                    eventExecutor.execute(() -> processFloatingIpAssociation(event.subject(),
                            event.floatingIp()));
                    break;
                case KUBEVIRT_FLOATING_IP_DISASSOCIATED:
                    eventExecutor.execute(() -> processFloatingIpDisassociation(event.subject(),
                            event.floatingIp()));
                    break;
                case KUBEVIRT_GATEWAY_NODE_CHANGED:
                    eventExecutor.execute(() -> processRouterGatewayNodeChanged(event.subject(),
                            event.gateway()));
                    break;
                case KUBEVIRT_GATEWAY_NODE_ATTACHED:
                    eventExecutor.execute(() -> processGatewayNodeAttachment(event.subject(),
                            event.gateway()));
                    break;
                case KUBEVIRT_GATEWAY_NODE_DETACHED:
                    eventExecutor.execute(() -> processGatewayNodeDetachment(event.subject(),
                            event.gateway()));
                    break;
                default:
                    //do nothing
                    break;
            }
        }

        private void processRouterGatewayNodeChanged(KubevirtRouter router, String disAssociatedGateway) {
            kubevirtRouterService.floatingIpsByRouter(router.name())
                    .forEach(fip -> {
                        KubevirtNode newGw = kubevirtNodeService.node(router.electedGateway());
                        if (newGw == null) {
                            return;
                        }
                        setFloatingIpRulesForFip(router, fip, newGw, true);
                        processGarpPacketForFloatingIp(fip, newGw);
                        KubevirtNode oldGw = kubevirtNodeService.node(disAssociatedGateway);

                        if (oldGw == null) {
                            return;
                        }
                        setFloatingIpRulesForFip(router, fip, oldGw, false);
                    });
        }

        private void processGatewayNodeAttachment(KubevirtRouter router, String gatewayName) {
            kubevirtRouterService.floatingIpsByRouter(router.name())
                    .forEach(fip -> {
                        KubevirtNode gw = kubevirtNodeService.node(gatewayName);
                        if (gw != null) {
                            setFloatingIpRulesForFip(router, fip, gw, true);
                        }
                    });
        }

        private void processGatewayNodeDetachment(KubevirtRouter router, String gatewayName) {
            kubevirtRouterService.floatingIpsByRouter(router.name())
                    .forEach(fip -> {
                        KubevirtNode gw = kubevirtNodeService.node(gatewayName);
                        if (gw != null) {
                            setFloatingIpRulesForFip(router, fip, gw, false);
                        }
                    });
        }

        private void processFloatingIpAssociation(KubevirtRouter router, KubevirtFloatingIp floatingIp) {
            if (!isRelevantHelper() || router.electedGateway() == null) {
                return;
            }

            KubevirtNode electedGw = kubevirtNodeService.node(router.electedGateway());

            if (electedGw == null) {
                return;
            }

            processGarpPacketForFloatingIp(floatingIp, electedGw);
            setFloatingIpRulesForFip(router, floatingIp, electedGw, true);
        }

        private void processFloatingIpDisassociation(KubevirtRouter router, KubevirtFloatingIp floatingIp) {
            if (!isRelevantHelper() || router.electedGateway() == null) {
                return;
            }

            KubevirtNode electedGw = kubevirtNodeService.node(router.electedGateway());

            if (electedGw == null) {
                return;
            }
            setFloatingIpRulesForFip(router, floatingIp, electedGw, false);
        }
    }

    private class InternalNodeListener implements KubevirtNodeListener {
        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtNodeEvent event) {
            switch (event.type()) {
                case KUBEVIRT_NODE_COMPLETE:
                    eventExecutor.execute(() -> processNodeCompletion(event.subject()));
                    break;
                default:
                    break;
            }
        }

        private void processNodeCompletion(KubevirtNode node) {
            if (!isRelevantHelper()) {
                return;
            }

            for (KubevirtFloatingIp fip : kubevirtRouterService.floatingIps()) {
                KubevirtRouter router = kubevirtRouterService.router(fip.routerName());
                if (router == null) {
                    log.warn("The router {} is not found", fip.routerName());
                    continue;
                }

                if (node.hostname().equals(router.electedGateway())) {
                    setFloatingIpRulesForFip(router, fip, node, true);
                    log.info("Configure floating IP {} on gateway {}",
                                fip.floatingIp().toString(), node.hostname());
                }
            }
        }
    }
}
