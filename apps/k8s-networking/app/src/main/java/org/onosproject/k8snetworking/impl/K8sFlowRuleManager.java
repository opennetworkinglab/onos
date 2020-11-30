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

import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
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
import org.onosproject.k8snode.api.K8sRouterBridge;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
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
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snetworking.api.Constants.ACL_TABLE;
import static org.onosproject.k8snetworking.api.Constants.ARP_TABLE;
import static org.onosproject.k8snetworking.api.Constants.DEFAULT_GATEWAY_MAC;
import static org.onosproject.k8snetworking.api.Constants.FORWARDING_TABLE;
import static org.onosproject.k8snetworking.api.Constants.GROUPING_TABLE;
import static org.onosproject.k8snetworking.api.Constants.HOST_PREFIX;
import static org.onosproject.k8snetworking.api.Constants.JUMP_TABLE;
import static org.onosproject.k8snetworking.api.Constants.K8S_NETWORKING_APP_ID;
import static org.onosproject.k8snetworking.api.Constants.NAMESPACE_TABLE;
import static org.onosproject.k8snetworking.api.Constants.PRIORITY_CIDR_RULE;
import static org.onosproject.k8snetworking.api.Constants.PRIORITY_DEFAULT_RULE;
import static org.onosproject.k8snetworking.api.Constants.PRIORITY_SNAT_RULE;
import static org.onosproject.k8snetworking.api.Constants.ROUTER_ENTRY_TABLE;
import static org.onosproject.k8snetworking.api.Constants.ROUTING_TABLE;
import static org.onosproject.k8snetworking.api.Constants.STAT_EGRESS_TABLE;
import static org.onosproject.k8snetworking.api.Constants.STAT_INGRESS_TABLE;
import static org.onosproject.k8snetworking.api.Constants.TUN_ENTRY_TABLE;
import static org.onosproject.k8snetworking.api.Constants.VTAG_TABLE;
import static org.onosproject.k8snetworking.api.Constants.VTAP_EGRESS_TABLE;
import static org.onosproject.k8snetworking.api.Constants.VTAP_INGRESS_TABLE;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.tunnelPortNumByNetId;
import static org.onosproject.k8snetworking.util.RulePopulatorUtil.buildExtension;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Sets flow rules directly using FlowRuleService.
 */
@Component(immediate = true, service = K8sFlowRuleService.class)
public class K8sFlowRuleManager implements K8sFlowRuleService {

    private final Logger log = getLogger(getClass());

    private static final int DROP_PRIORITY = 0;
    private static final int HIGH_PRIORITY = 30000;
    private static final int TIMEOUT_SNAT_RULE = 60;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNetworkService k8sNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNodeService k8sNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sHostService k8sHostService;

    private final ExecutorService deviceEventExecutor =
            Executors.newSingleThreadExecutor(groupedThreads(
                    getClass().getSimpleName(), "device-event"));
    private final K8sNetworkListener internalNetworkListener = new InternalK8sNetworkListener();
    private final K8sNodeListener internalNodeListener = new InternalK8sNodeListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);
        coreService.registerApplication(K8S_NETWORKING_APP_ID);
        k8sNodeService.addListener(internalNodeListener);
        k8sNetworkService.addListener(internalNetworkListener);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        k8sNodeService.completeNodes().forEach(this::initializePipeline);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        k8sNodeService.removeListener(internalNodeListener);
        k8sNetworkService.removeListener(internalNetworkListener);
        leadershipService.withdraw(appId.name());
        deviceEventExecutor.shutdown();

        log.info("Stopped");
    }

    @Override
    public void setRule(ApplicationId appId, DeviceId deviceId,
                        TrafficSelector selector, TrafficTreatment treatment,
                        int priority, int tableType, boolean install) {
        FlowRule.Builder flowRuleBuilder = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(priority)
                .fromApp(appId)
                .forTable(tableType);

        if (priority == PRIORITY_SNAT_RULE) {
            flowRuleBuilder.makeTemporary(TIMEOUT_SNAT_RULE);
        } else {
            flowRuleBuilder.makePermanent();
        }

        applyRule(flowRuleBuilder.build(), install);
    }

    @Override
    public void setUpTableMissEntry(DeviceId deviceId, int table) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        treatment.drop();

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(table)
                .build();

        applyRule(flowRule, true);
    }

    @Override
    public void connectTables(DeviceId deviceId, int fromTable, int toTable) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        treatment.transition(toTable);

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(fromTable)
                .build();

        applyRule(flowRule, true);
    }

    private void applyRule(FlowRule flowRule, boolean install) {
        FlowRuleOperations.Builder flowOpsBuilder = FlowRuleOperations.builder();

        flowOpsBuilder = install ? flowOpsBuilder.add(flowRule) : flowOpsBuilder.remove(flowRule);

        flowRuleService.apply(flowOpsBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.debug("Provisioned vni or forwarding table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.debug("Failed to provision vni or forwarding table");
            }
        }));
    }

    protected void initializePipeline(K8sNode k8sNode) {

        DeviceId deviceId = k8sNode.intgBridge();

        // for inbound table transition
        connectTables(deviceId, STAT_INGRESS_TABLE, VTAP_INGRESS_TABLE);
        connectTables(deviceId, VTAP_INGRESS_TABLE, VTAG_TABLE);

        // for vTag and ARP table transition
        connectTables(deviceId, VTAG_TABLE, ARP_TABLE);

        // for jump and namespace table transition
        connectTables(deviceId, JUMP_TABLE, NAMESPACE_TABLE);

        // for ARP and ACL table transition
        connectTables(deviceId, ARP_TABLE, NAMESPACE_TABLE);

        // for namespace table transition to grouping table
        connectTables(deviceId, NAMESPACE_TABLE, GROUPING_TABLE);

        // for grouping table transition to ACL table
        connectTables(deviceId, GROUPING_TABLE, ACL_TABLE);

        // for ACL table transition to routing table
        connectTables(deviceId, ACL_TABLE, ROUTING_TABLE);

        // for grouping table transition
        // we need grouping table for bypassing routing table which contains large
        // amount of flow rules which might cause performance degradation during
        // table lookup
        // setupJumpTable(k8sNode);

        // for routing and outbound table transition
        connectTables(deviceId, ROUTING_TABLE, STAT_EGRESS_TABLE);

        // for outbound table transition
        connectTables(deviceId, STAT_EGRESS_TABLE, VTAP_EGRESS_TABLE);
        connectTables(deviceId, VTAP_EGRESS_TABLE, FORWARDING_TABLE);
    }

    private void setupRouter(K8sNode k8sNode, K8sRouterBridge bridge) {
        if (k8sNode.routerPortNum() == null) {
            return;
        }

        TrafficSelector selector = DefaultTrafficSelector.builder().build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.NORMAL)
                .build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(bridge.deviceId())
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(PRIORITY_DEFAULT_RULE)
                .fromApp(appId)
                .makePermanent()
                .forTable(ROUTER_ENTRY_TABLE)
                .build();

        applyRule(flowRule, true);
    }

    private void setupJumpTable(K8sNode k8sNode) {
        DeviceId deviceId = k8sNode.intgBridge();

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        selector.matchEthDst(DEFAULT_GATEWAY_MAC);
        treatment.transition(ROUTING_TABLE);

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(HIGH_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(GROUPING_TABLE)
                .build();

        applyRule(flowRule, true);

        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();

        treatment.transition(STAT_EGRESS_TABLE);

        flowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(GROUPING_TABLE)
                .build();

        applyRule(flowRule, true);
    }

    private void setAnyRoutingRule(IpPrefix srcIpPrefix, MacAddress mac,
                                   K8sNetwork k8sNetwork) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(srcIpPrefix)
                .matchIPDst(IpPrefix.valueOf(k8sNetwork.cidr()));

        for (K8sNode node : k8sNodeService.completeNodes()) {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                    .setTunnelId(Long.valueOf(k8sNetwork.segmentId()));

            if (node.hostname().equals(k8sNetwork.name())) {
                if (mac != null) {
                    tBuilder.setEthSrc(mac);
                }
                tBuilder.transition(STAT_EGRESS_TABLE);
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

                FlowRule remoteFlowRule = DefaultFlowRule.builder()
                        .forDevice(node.tunBridge())
                        .withSelector(sBuilder.build())
                        .withTreatment(treatmentToRemote)
                        .withPriority(PRIORITY_CIDR_RULE)
                        .fromApp(appId)
                        .makePermanent()
                        .forTable(TUN_ENTRY_TABLE)
                        .build();
                applyRule(remoteFlowRule, true);
            }

            FlowRule flowRule = DefaultFlowRule.builder()
                    .forDevice(node.intgBridge())
                    .withSelector(sBuilder.build())
                    .withTreatment(tBuilder.build())
                    .withPriority(PRIORITY_CIDR_RULE)
                    .fromApp(appId)
                    .makePermanent()
                    .forTable(ROUTING_TABLE)
                    .build();
            applyRule(flowRule, true);
        }
    }

    private void setupHostRoutingRule(K8sNetwork k8sNetwork) {
        setAnyRoutingRule(IpPrefix.valueOf(
                k8sNetwork.gatewayIp(), HOST_PREFIX), null, k8sNetwork);
    }

    private class InternalK8sNodeListener implements K8sNodeListener {
        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sNodeEvent event) {
            switch (event.type()) {
                case K8S_NODE_COMPLETE:
                    deviceEventExecutor.execute(() -> processNodeCompletion(event.subject()));
                    break;
                case K8S_NODE_CREATED:
                default:
                    // do nothing
                    break;
            }
        }

        private void processNodeCompletion(K8sNode node) {
            log.info("COMPLETE node {} is detected", node.hostname());

            if (!isRelevantHelper()) {
                return;
            }

            initializePipeline(node);

            k8sNetworkService.networks().forEach(K8sFlowRuleManager.this::setupHostRoutingRule);

            for (K8sHost host : k8sHostService.completeHosts()) {
                if (host.nodeNames().contains(node.hostname())) {
                    host.routerBridges().stream()
                            .filter(b -> b.segmentId() == node.segmentId())
                            .findAny().ifPresent(bridge -> setupRouter(node, bridge));
                }
            }
        }
    }

    private class InternalK8sNetworkListener implements K8sNetworkListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sNetworkEvent event) {

            switch (event.type()) {
                case K8S_NETWORK_CREATED:
                    deviceEventExecutor.execute(() -> processNetworkCreation(event.subject()));
                    break;
                case K8S_NETWORK_REMOVED:
                default:
                    // do nothing
                    break;
            }
        }

        private void processNetworkCreation(K8sNetwork network) {
            if (!isRelevantHelper()) {
                return;
            }

            setupHostRoutingRule(network);
        }
    }
}
