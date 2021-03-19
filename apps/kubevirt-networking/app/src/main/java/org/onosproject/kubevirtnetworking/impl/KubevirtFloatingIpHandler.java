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
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
import org.onosproject.net.Device;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.Constants.FORWARDING_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.api.Constants.PRE_FLAT_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_ARP_GATEWAY_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_FLOATING_IP_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_FORWARDING_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.TUNNEL_DEFAULT_TABLE;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.GENEVE;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.GRE;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.VXLAN;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.externalPatchPortNum;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.gatewayNodeForSpecifiedRouter;
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

    private final InternalRouterEventListener kubevirtRouterlistener =
            new InternalRouterEventListener();

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        kubevirtRouterService.addListener(kubevirtRouterlistener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        leadershipService.withdraw(appId.name());
        kubevirtRouterService.removeListener(kubevirtRouterlistener);

        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private void setFloatingIpRules(KubevirtRouter router,
                                    KubevirtFloatingIp floatingIp,
                                    boolean install) {

        KubevirtPort kubevirtPort = getKubevirtPort(floatingIp);
        if (kubevirtPort == null) {
            log.warn("Failed to install floating Ip rules for floating ip {} " +
                    "because there's no kubevirt port associated to it", floatingIp.floatingIp());
            return;
        }

        KubevirtNetwork kubevirtNetwork = kubevirtNetworkService.network(kubevirtPort.networkId());
        if (kubevirtNetwork.type() == VXLAN || kubevirtNetwork.type() == GENEVE || kubevirtNetwork.type() == GRE) {
            setFloatingIpDownstreamRulesToGatewayTunBridge(router, floatingIp, kubevirtNetwork, kubevirtPort, install);
        }

        setFloatingIpArpResponseRules(router, floatingIp, kubevirtPort, install);
        setFloatingIpUpstreamRules(router, floatingIp, kubevirtPort, install);
        setFloatingIpDownstreamRules(router, floatingIp, kubevirtPort, install);
    }

    private void setFloatingIpArpResponseRules(KubevirtRouter router,
                                               KubevirtFloatingIp floatingIp,
                                               KubevirtPort port,
                                               boolean install) {

        KubevirtNode electedGw = gatewayNodeForSpecifiedRouter(kubevirtNodeService, router);

        if (electedGw == null) {
            log.warn("Failed to install floating Ip rules for floating ip {} " +
                    "because there's no gateway assigned to it", floatingIp.floatingIp());
            return;
        }

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
                PRE_FLAT_TABLE,
                install);
    }
    private KubevirtPort getKubevirtPort(KubevirtFloatingIp floatingIp) {

        return kubevirtPortService.ports().stream()
                .filter(port -> port.ipAddress().equals(floatingIp.fixedIp()))
                .findAny().orElse(null);
    }

    private void setFloatingIpUpstreamRules(KubevirtRouter router,
                                            KubevirtFloatingIp floatingIp,
                                            KubevirtPort port,
                                            boolean install) {

        KubevirtNode electedGw = gatewayNodeForSpecifiedRouter(kubevirtNodeService, router);

        if (electedGw == null) {
            log.warn("Failed to install floating Ip rules for floating ip {} " +
                    "because there's no gateway assigned to it", floatingIp.floatingIp());
            return;
        }

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
                PRIORITY_FLOATING_IP_RULE,
                PRE_FLAT_TABLE,
                install);
    }

    private void setFloatingIpDownstreamRules(KubevirtRouter router,
                                              KubevirtFloatingIp floatingIp,
                                              KubevirtPort port,
                                              boolean install) {
        KubevirtNode electedGw = gatewayNodeForSpecifiedRouter(kubevirtNodeService, router);

        if (electedGw == null) {
            log.warn("Failed to install floating Ip rules for floating ip {} " +
                    "because there's no gateway assigned to it", floatingIp.floatingIp());
            return;
        }

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
                PRIORITY_FLOATING_IP_RULE,
                PRE_FLAT_TABLE,
                install);
    }

    private void setFloatingIpDownstreamRulesToGatewayTunBridge(KubevirtRouter router,
                                                                KubevirtFloatingIp floatingIp,
                                                                KubevirtNetwork network,
                                                                KubevirtPort port,
                                                                boolean install) {
        KubevirtNode electedGw = gatewayNodeForSpecifiedRouter(kubevirtNodeService, router);

        if (electedGw == null) {
            log.warn("Failed to install floating Ip rules for floating ip {} " +
                    "because there's no gateway assigned to it", floatingIp.floatingIp());
            return;
        }

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
                PRIORITY_FORWARDING_RULE,
                TUNNEL_DEFAULT_TABLE,
                install);
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

                default:
                    //do nothing
                    break;
            }
        }

        private void processFloatingIpAssociation(KubevirtRouter router, KubevirtFloatingIp floatingIp) {
            if (!isRelevantHelper()) {
                return;
            }
            setFloatingIpRules(router, floatingIp, true);
        }

        private void processFloatingIpDisassociation(KubevirtRouter router, KubevirtFloatingIp floatingIp) {
            if (!isRelevantHelper()) {
                return;
            }
            setFloatingIpRules(router, floatingIp, false);
        }
    }
}
