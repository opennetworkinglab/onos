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

import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.KubevirtFlowRuleService;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeEvent;
import org.onosproject.kubevirtnode.api.KubevirtNodeListener;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
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
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.Constants.ACL_EGRESS_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.ACL_INGRESS_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.ARP_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.DEFAULT_GATEWAY_MAC;
import static org.onosproject.kubevirtnetworking.api.Constants.DHCP_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.FLAT_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.FORWARDING_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.JUMP_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.api.Constants.PRE_FLAT_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.ROUTING_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.STAT_INBOUND_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.STAT_OUTBOUND_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.VTAG_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.VTAP_INBOUND_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.VTAP_OUTBOUND_TABLE;
import static org.onosproject.kubevirtnetworking.impl.OsgiPropertyConstants.PROVIDER_NETWORK_ONLY;
import static org.onosproject.kubevirtnetworking.impl.OsgiPropertyConstants.PROVIDER_NETWORK_ONLY_DEFAULT;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.getPropertyValueAsBoolean;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.GATEWAY;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.WORKER;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Sets flow rules directly using FlowRuleService.
 */
@Component(
        immediate = true,
        service = KubevirtFlowRuleService.class,
        property = {
                PROVIDER_NETWORK_ONLY + ":Boolean=" + PROVIDER_NETWORK_ONLY_DEFAULT
        }
)
public class KubevirtFlowRuleManager implements KubevirtFlowRuleService {

    private final Logger log = getLogger(getClass());

    private static final int DROP_PRIORITY = 0;
    private static final int LOW_PRIORITY = 10000;
    private static final int MID_PRIORITY = 20000;
    private static final int HIGH_PRIORITY = 30000;
    private static final int TIMEOUT_SNAT_RULE = 60;

    /** Use provider network only. */
    private boolean providerNetworkOnly = PROVIDER_NETWORK_ONLY_DEFAULT;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNodeService nodeService;

    private final ExecutorService deviceEventExecutor = Executors.newSingleThreadExecutor(
                    groupedThreads(getClass().getSimpleName(), "device-event"));
    private final KubevirtNodeListener internalNodeListener = new InternalKubevirtNodeListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);
        coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);
        configService.registerProperties(getClass());
        nodeService.addListener(internalNodeListener);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        nodeService.completeNodes(WORKER)
                .forEach(node -> initializeWorkerNodePipeline(node.intgBridge()));

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        nodeService.removeListener(internalNodeListener);
        configService.unregisterProperties(getClass(), false);
        leadershipService.withdraw(appId.name());
        deviceEventExecutor.shutdown();

        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Boolean flag;

        flag = Tools.isPropertyEnabled(properties, PROVIDER_NETWORK_ONLY);
        if (flag == null) {
            log.info("providerNetworkOnly is not configured, " +
                    "using current value of {}", providerNetworkOnly);
        } else {
            providerNetworkOnly = flag;
            log.info("Configured. providerNetworkOnly is {}",
                    providerNetworkOnly ? "enabled" : "disabled");
        }
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
                .forTable(tableType)
                .makePermanent();

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

    protected void initializeGatewayNodePipeline(DeviceId deviceId) {
        // for inbound table transition
        connectTables(deviceId, STAT_INBOUND_TABLE, VTAG_TABLE);

        if (getProviderNetworkOnlyFlag()) {
            // we directly transit from vTag table to PRE_FLAT table for provider
            // network only mode, because there is no need to differentiate ARP
            // and IP packets on this mode
            connectTables(deviceId, VTAG_TABLE, PRE_FLAT_TABLE);
        } else {
            // for vTag and ARP table transition
            connectTables(deviceId, VTAG_TABLE, ARP_TABLE);
        }

        // for PRE_FLAT and FLAT table transition
        connectTables(deviceId, PRE_FLAT_TABLE, FLAT_TABLE);

        // for setting up default FLAT table behavior which is drop
        setupGatewayNodeFlatTable(deviceId);

        // for setting up default Forwarding table behavior which is NORMAL
        setupForwardingTable(deviceId);
    }
    protected void initializeWorkerNodePipeline(DeviceId deviceId) {
        // for inbound table transition
        connectTables(deviceId, STAT_INBOUND_TABLE, VTAP_INBOUND_TABLE);
        connectTables(deviceId, VTAP_INBOUND_TABLE, DHCP_TABLE);

        // for DHCP and vTag table transition
        connectTables(deviceId, DHCP_TABLE, VTAG_TABLE);

        if (getProviderNetworkOnlyFlag()) {
            // we directly transit from vTag table to PRE_FLAT table for provider
            // network only mode, because there is no need to differentiate ARP
            // and IP packets on this mode
            connectTables(deviceId, VTAG_TABLE, PRE_FLAT_TABLE);
        } else {
            // for vTag and ARP table transition
            connectTables(deviceId, VTAG_TABLE, ARP_TABLE);
        }

        // for PRE_FLAT and FLAT table transition
        connectTables(deviceId, PRE_FLAT_TABLE, FLAT_TABLE);

        // for FLAT table and ACL table transition
        connectTables(deviceId, FLAT_TABLE, ACL_EGRESS_TABLE);

        // for ARP and ACL table transition
        connectTables(deviceId, ARP_TABLE, ACL_INGRESS_TABLE);

        // for ACL and JUMP table transition
        connectTables(deviceId, ACL_EGRESS_TABLE, JUMP_TABLE);

        // for outbound table transition
        connectTables(deviceId, STAT_OUTBOUND_TABLE, VTAP_OUTBOUND_TABLE);
        connectTables(deviceId, VTAP_OUTBOUND_TABLE, FORWARDING_TABLE);

        // for JUMP table transition
        // we need JUMP table for bypassing routing table which contains large
        // amount of flow rules which might cause performance degradation during
        // table lookup
        setupJumpTable(deviceId);

        // for setting up default Forwarding table behavior which is NORMAL
        setupForwardingTable(deviceId);
    }

    private void setupJumpTable(DeviceId deviceId) {
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
                .forTable(JUMP_TABLE)
                .build();

        applyRule(flowRule, true);

        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();

        treatment.transition(STAT_OUTBOUND_TABLE);

        flowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(JUMP_TABLE)
                .build();

        applyRule(flowRule, true);
    }

    private void setupForwardingTable(DeviceId deviceId) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.NORMAL);

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(LOW_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(FORWARDING_TABLE)
                .build();

        applyRule(flowRule, true);
    }

    private void setupGatewayNodeFlatTable(DeviceId deviceId) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .drop();

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(FLAT_TABLE)
                .build();

        applyRule(flowRule, true);

    }

    private boolean getProviderNetworkOnlyFlag() {
        Set<ConfigProperty> properties =
                configService.getProperties(getClass().getName());
        return getPropertyValueAsBoolean(properties, PROVIDER_NETWORK_ONLY);
    }

    private class InternalKubevirtNodeListener implements KubevirtNodeListener {

        @Override
        public boolean isRelevant(KubevirtNodeEvent event) {
            return event.subject().type().equals(WORKER) ||
                    event.subject().type().equals(GATEWAY);
        }

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtNodeEvent event) {
            KubevirtNode node = event.subject();

            switch (event.type()) {
                case KUBEVIRT_NODE_COMPLETE:
                    deviceEventExecutor.execute(() -> {
                        log.info("COMPLETE node {} is detected", node.hostname());

                        if (!isRelevantHelper()) {
                            return;
                        }

                        if (event.subject().type().equals(WORKER)) {
                            initializeWorkerNodePipeline(node.intgBridge());
                        } else {
                            initializeGatewayNodePipeline(node.intgBridge());
                        }
                    });
                    break;
                case KUBEVIRT_NODE_CREATED:
                case KUBEVIRT_NODE_UPDATED:
                case KUBEVIRT_NODE_REMOVED:
                default:
                    // do nothing
                    break;
            }
        }
    }
}
