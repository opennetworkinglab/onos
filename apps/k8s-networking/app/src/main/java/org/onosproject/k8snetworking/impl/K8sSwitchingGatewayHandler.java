/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.impl;

import org.apache.commons.lang.StringUtils;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpPrefix;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sFlowRuleService;
import org.onosproject.k8snetworking.api.K8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetworkEvent;
import org.onosproject.k8snetworking.api.K8sNetworkListener;
import org.onosproject.k8snetworking.api.K8sNetworkService;
import org.onosproject.k8snode.api.K8sHost;
import org.onosproject.k8snode.api.K8sHostService;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeEvent;
import org.onosproject.k8snode.api.K8sNodeListener;
import org.onosproject.k8snode.api.K8sNodeService;
import org.onosproject.net.Device;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.packet.PacketService;
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
import static org.onosproject.k8snetworking.api.Constants.B_CLASS;
import static org.onosproject.k8snetworking.api.Constants.DST;
import static org.onosproject.k8snetworking.api.Constants.HOST_PREFIX;
import static org.onosproject.k8snetworking.api.Constants.K8S_NETWORKING_APP_ID;
import static org.onosproject.k8snetworking.api.Constants.LOCAL_ENTRY_TABLE;
import static org.onosproject.k8snetworking.api.Constants.NODE_IP_PREFIX;
import static org.onosproject.k8snetworking.api.Constants.PRIORITY_ARP_REPLY_RULE;
import static org.onosproject.k8snetworking.api.Constants.PRIORITY_GATEWAY_RULE;
import static org.onosproject.k8snetworking.api.Constants.PRIORITY_INTER_NODE_RULE;
import static org.onosproject.k8snetworking.api.Constants.PRIORITY_LOCAL_BRIDGE_RULE;
import static org.onosproject.k8snetworking.api.Constants.ROUTING_TABLE;
import static org.onosproject.k8snetworking.api.Constants.SHIFTED_IP_PREFIX;
import static org.onosproject.k8snetworking.api.Constants.SHIFTED_LOCAL_IP_PREFIX;
import static org.onosproject.k8snetworking.api.Constants.SRC;
import static org.onosproject.k8snetworking.api.Constants.TUN_ENTRY_TABLE;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.shiftIpDomain;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.tunnelPortNumByNetId;
import static org.onosproject.k8snetworking.util.RulePopulatorUtil.buildExtension;
import static org.onosproject.k8snetworking.util.RulePopulatorUtil.buildLoadExtension;
import static org.onosproject.k8snetworking.util.RulePopulatorUtil.buildMoveArpShaToThaExtension;
import static org.onosproject.k8snetworking.util.RulePopulatorUtil.buildMoveArpSpaToTpaExtension;
import static org.onosproject.k8snetworking.util.RulePopulatorUtil.buildMoveEthSrcToDstExtension;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Populates switching flow rules on OVS for providing the connectivity between
 * container and network gateway.
 */
@Component(immediate = true)
public class K8sSwitchingGatewayHandler {

    private final Logger log = getLogger(getClass());

    private static final String REQUEST = "req";
    private static final String REPLY = "rep";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sFlowRuleService k8sFlowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNetworkService k8sNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNodeService k8sNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sHostService k8sHostService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));
    private final InternalK8sNetworkListener k8sNetworkListener =
            new InternalK8sNetworkListener();
    private final InternalK8sNodeListener k8sNodeListener =
            new InternalK8sNodeListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);
        k8sNetworkService.addListener(k8sNetworkListener);
        k8sNodeService.addListener(k8sNodeListener);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        k8sNodeService.removeListener(k8sNodeListener);
        k8sNetworkService.removeListener(k8sNetworkListener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private void setGatewayRule(K8sNetwork k8sNetwork, boolean install) {
        for (K8sNode node : k8sNodeService.completeNodes()) {
            TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPDst(IpPrefix.valueOf(k8sNetwork.gatewayIp(),
                            HOST_PREFIX));

            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

            if (node.hostname().equals(k8sNetwork.name())) {
                tBuilder.setEthDst(node.intgBridgeMac())
                        .setOutput(node.intgEntryPortNum());
            } else {
                K8sNode localNode = k8sNodeService.node(k8sNetwork.name());

                tBuilder.setOutput(node.intgToTunPortNum());

                // install flows into tunnel bridge
                PortNumber portNum = tunnelPortNumByNetId(k8sNetwork.networkId(),
                        k8sNetworkService, node);
                TrafficTreatment treatmentToRemote = DefaultTrafficTreatment.builder()
                        .extension(buildExtension(
                                deviceService,
                                node.tunBridge(),
                                localNode.dataIp().getIp4Address()),
                                node.tunBridge())
                        .setTunnelId(Long.valueOf(k8sNetwork.segmentId()))
                        .setOutput(portNum)
                        .build();

                k8sFlowRuleService.setRule(
                        appId,
                        node.tunBridge(),
                        sBuilder.build(),
                        treatmentToRemote,
                        PRIORITY_GATEWAY_RULE,
                        TUN_ENTRY_TABLE,
                        install);
            }

            k8sFlowRuleService.setRule(
                    appId,
                    node.intgBridge(),
                    sBuilder.build(),
                    tBuilder.build(),
                    PRIORITY_GATEWAY_RULE,
                    ROUTING_TABLE,
                    install);

            if (node.hostname().equals(k8sNetwork.name())) {
                sBuilder = DefaultTrafficSelector.builder()
                        .matchInPort(node.intgEntryPortNum())
                        .matchEthType(Ethernet.TYPE_IPV4)
                        .matchIPDst(IpPrefix.valueOf(k8sNetwork.gatewayIp(),
                                HOST_PREFIX));

                tBuilder = DefaultTrafficTreatment.builder()
                        .setOutput(node.intgToLocalPatchPortNum());

                k8sFlowRuleService.setRule(
                        appId,
                        node.intgBridge(),
                        sBuilder.build(),
                        tBuilder.build(),
                        PRIORITY_LOCAL_BRIDGE_RULE,
                        ROUTING_TABLE,
                        install);
            }
        }
    }

    private void setInterNodeRoutingRules(K8sNode srcNode, boolean install) {
        if (srcNode == null) {
            return;
        }

        for (K8sNode dstNode : k8sNodeService.nodes()) {
            if (StringUtils.equals(srcNode.hostname(), dstNode.hostname())) {
                continue;
            }

            boolean sameHost = false;
            for (K8sHost host : k8sHostService.completeHosts()) {
                Set<String> nodeNames = host.nodeNames();
                // if the src and dst nodes located in the same hosts,
                // we simply do not tunnel the traffic, instead we route the traffic
                if (nodeNames.contains(srcNode.hostname()) &&
                        nodeNames.contains(dstNode.hostname())) {
                    sameHost = true;
                }
            }

            if (sameHost) {
                TrafficSelector originalSelector = DefaultTrafficSelector.builder()
                        .matchEthType(Ethernet.TYPE_IPV4)
                        .matchIPSrc(IpPrefix.valueOf(srcNode.podCidr()))
                        .matchIPDst(IpPrefix.valueOf(dstNode.podCidr()))
                        .build();

                TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                        .setOutput(dstNode.tunToIntgPortNum())
                        .build();

                k8sFlowRuleService.setRule(
                        appId,
                        dstNode.tunBridge(),
                        originalSelector,
                        treatment,
                        PRIORITY_INTER_NODE_RULE,
                        TUN_ENTRY_TABLE,
                        install);

                TrafficSelector transformedSelector = DefaultTrafficSelector.builder()
                        .matchEthType(Ethernet.TYPE_IPV4)
                        .matchIPSrc(IpPrefix.valueOf(shiftIpDomain(srcNode.podCidr(), SHIFTED_IP_PREFIX)))
                        .matchIPDst(IpPrefix.valueOf(dstNode.podCidr()))
                        .build();

                k8sFlowRuleService.setRule(
                        appId,
                        dstNode.tunBridge(),
                        transformedSelector,
                        treatment,
                        PRIORITY_INTER_NODE_RULE,
                        TUN_ENTRY_TABLE,
                        install);

                String nodeIpPrefix = NODE_IP_PREFIX + ".0.0.0/8";

                TrafficSelector nodePortSelector = DefaultTrafficSelector.builder()
                        .matchEthType(Ethernet.TYPE_IPV4)
                        .matchIPSrc(IpPrefix.valueOf(nodeIpPrefix))
                        .matchIPDst(IpPrefix.valueOf(dstNode.podCidr()))
                        .build();

                k8sFlowRuleService.setRule(
                        appId,
                        dstNode.tunBridge(),
                        nodePortSelector,
                        treatment,
                        PRIORITY_INTER_NODE_RULE,
                        TUN_ENTRY_TABLE,
                        install);
            }
        }
    }

    private void setLocalBridgeRules(K8sNetwork k8sNetwork, boolean install) {
        for (K8sNode node : k8sNodeService.completeNodes()) {
            if (node.hostname().equals(k8sNetwork.name())) {
                setLocalBridgeRule(k8sNetwork, node, REQUEST, install);
                setLocalBridgeRule(k8sNetwork, node, REPLY, install);
            }
        }
    }

    private void setLocalBridgeRule(K8sNetwork k8sNetwork, K8sNode k8sNode,
                                    String type, boolean install) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4);
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        ExtensionTreatment loadTreatment = null;

        if (REQUEST.equals(type)) {
            loadTreatment = buildLoadExtension(deviceService.getDevice(
                    k8sNode.localBridge()), B_CLASS, SRC, SHIFTED_LOCAL_IP_PREFIX);
        }

        if (REPLY.equals(type)) {
            loadTreatment = buildLoadExtension(deviceService.getDevice(
                    k8sNode.localBridge()), B_CLASS, DST, SHIFTED_IP_PREFIX);
        }

        tBuilder.extension(loadTreatment, k8sNode.localBridge());

        if (REQUEST.equals(type)) {
            sBuilder.matchIPDst(IpPrefix.valueOf(k8sNetwork.gatewayIp(),
                    HOST_PREFIX));
            tBuilder.setOutput(PortNumber.LOCAL);
        }

        if (REPLY.equals(type)) {
            sBuilder.matchIPSrc(IpPrefix.valueOf(k8sNetwork.gatewayIp(),
                    HOST_PREFIX));
            tBuilder.setOutput(k8sNode.localToIntgPatchPortNum());
        }

        k8sFlowRuleService.setRule(
                appId,
                k8sNode.localBridge(),
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_LOCAL_BRIDGE_RULE,
                LOCAL_ENTRY_TABLE,
                install);
    }

    private void setLocalBridgeArpRules(K8sNetwork k8sNetwork, boolean install) {
        for (K8sNode node : k8sNodeService.completeNodes()) {
            if (node.hostname().equals(k8sNetwork.name())) {
                setLocalBridgeArpRule(k8sNetwork, node, install);
            }
        }
    }

    private void setLocalBridgeArpRule(K8sNetwork k8sNetwork, K8sNode k8sNode, boolean install) {
        Device device = deviceService.getDevice(k8sNode.localBridge());

        String shiftedLocalIp = shiftIpDomain(
                k8sNetwork.gatewayIp().toString(), SHIFTED_LOCAL_IP_PREFIX);

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_ARP)
                .matchArpOp(ARP.OP_REQUEST)
                .matchArpTpa(Ip4Address.valueOf(shiftedLocalIp))
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setArpOp(ARP.OP_REPLY)
                .extension(buildMoveEthSrcToDstExtension(device), device.id())
                .extension(buildMoveArpShaToThaExtension(device), device.id())
                .extension(buildMoveArpSpaToTpaExtension(device), device.id())
                .setArpSpa(Ip4Address.valueOf(shiftedLocalIp))
                .setArpSha(k8sNode.intgBridgeMac())
                .setOutput(PortNumber.IN_PORT)
                .build();

        k8sFlowRuleService.setRule(
                appId,
                device.id(),
                selector,
                treatment,
                PRIORITY_ARP_REPLY_RULE,
                LOCAL_ENTRY_TABLE,
                install);
    }

    private class InternalK8sNetworkListener implements K8sNetworkListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sNetworkEvent event) {
            switch (event.type()) {
                case K8S_NETWORK_CREATED:
                case K8S_NETWORK_UPDATED:
                    eventExecutor.execute(() -> processNetworkCreation(event));
                    break;
                case K8S_NETWORK_REMOVED:
                    eventExecutor.execute(() -> processNetworkRemoval(event));
                    break;
                default:
                    break;
            }
        }

        private void processNetworkCreation(K8sNetworkEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            setGatewayRule(event.subject(), true);
            setLocalBridgeRules(event.subject(), true);
            setLocalBridgeArpRules(event.subject(), true);

            K8sNode k8sNode = k8sNodeService.node(event.subject().networkId());
            setInterNodeRoutingRules(k8sNode, true);
        }

        private void processNetworkRemoval(K8sNetworkEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            setGatewayRule(event.subject(), false);
            setLocalBridgeRules(event.subject(), false);
            setLocalBridgeArpRules(event.subject(), false);

            K8sNode k8sNode = k8sNodeService.node(event.subject().networkId());
            setInterNodeRoutingRules(k8sNode, false);
        }
    }

    private class InternalK8sNodeListener implements K8sNodeListener {
        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sNodeEvent event) {
            switch (event.type()) {
                case K8S_NODE_COMPLETE:
                    eventExecutor.execute(() -> processNodeCompletion(event.subject()));
                    break;
                case K8S_NODE_INCOMPLETE:
                default:
                    break;
            }
        }

        private void processNodeCompletion(K8sNode node) {
            log.info("COMPLETE node {} is detected", node.hostname());

            if (!isRelevantHelper()) {
                return;
            }

            k8sNetworkService.networks().forEach(n -> setGatewayRule(n, true));
            k8sNetworkService.networks().forEach(n -> setLocalBridgeRules(n, true));
            k8sNetworkService.networks().forEach(n -> setLocalBridgeArpRules(n, true));

            setInterNodeRoutingRules(node, true);
        }
    }
}
