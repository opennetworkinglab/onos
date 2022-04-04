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

import org.onlab.packet.EthType;
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
import static org.onosproject.kubevirtnetworking.api.Constants.ARP_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.DHCP_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.FORWARDING_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.GW_DROP_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.GW_ENTRY_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_ARP_DEFAULT_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.STAT_INBOUND_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.VTAP_INBOUND_TABLE;
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
        nodeService.completeNodes(WORKER).forEach(this::initializeGatewayNodePipeline);
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

        this.setRule(
                appId,
                deviceId,
                selector.build(),
                treatment.build(),
                DROP_PRIORITY,
                table,
                true);
    }

    @Override
    public void connectTables(DeviceId deviceId, int fromTable, int toTable) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        treatment.transition(toTable);

        this.setRule(
                appId,
                deviceId,
                selector.build(),
                treatment.build(),
                DROP_PRIORITY,
                fromTable,
                true);
    }

    @Override
    public void purgeRules(DeviceId deviceId) {
        flowRuleService.purgeFlowRules(deviceId);
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

    protected void initializeGatewayNodePipeline(KubevirtNode kubevirtNode) {
        DeviceId deviceId = kubevirtNode.intgBridge();
        // for inbound to gateway entry table transition
        connectTables(deviceId, STAT_INBOUND_TABLE, GW_ENTRY_TABLE);

        // for gateway entry to gateway drop table transition
        connectTables(deviceId, GW_ENTRY_TABLE, GW_DROP_TABLE);

        // for setting up default gateway drop table
        setupGatewayNodeDropTable(deviceId);

        // for setting up default Forwarding table behavior which is NORMAL
        setupNormalTable(deviceId, FORWARDING_TABLE);

        kubevirtNode.phyIntfs().stream().filter(intf -> intf.physBridge() != null)
                .forEach(phyIntf -> {
                    setupNormalTable(phyIntf.physBridge(), STAT_INBOUND_TABLE);
                });
    }
    protected void initializeWorkerNodePipeline(KubevirtNode kubevirtNode) {
        DeviceId deviceId = kubevirtNode.intgBridge();
        // for inbound table transition
        connectTables(deviceId, STAT_INBOUND_TABLE, VTAP_INBOUND_TABLE);
        connectTables(deviceId, VTAP_INBOUND_TABLE, DHCP_TABLE);

        // for DHCP and ARP table transition
        connectTables(deviceId, DHCP_TABLE, ARP_TABLE);

        // for ARP table and ACL egress table transition
        connectTables(deviceId, ARP_TABLE, ACL_EGRESS_TABLE);

        // for setting up default ARP table behavior
        setupArpTable(deviceId);

        // for setting up default Forwarding table behavior which is NORMAL
        setupNormalTable(deviceId, FORWARDING_TABLE);

        kubevirtNode.phyIntfs().stream().filter(intf -> intf.physBridge() != null)
                .forEach(phyIntf -> {
                    setupNormalTable(phyIntf.physBridge(), STAT_INBOUND_TABLE);
                });
    }

    private void setupArpTable(DeviceId deviceId) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(EthType.EtherType.ARP.ethType().toShort());

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        tBuilder.transition(FORWARDING_TABLE);

        this.setRule(
                appId,
                deviceId,
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_ARP_DEFAULT_RULE,
                ARP_TABLE,
                true);
    }

    private void setupNormalTable(DeviceId deviceId, int tableNum) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.NORMAL);

        this.setRule(
                appId,
                deviceId,
                selector.build(),
                treatment.build(),
                LOW_PRIORITY,
                tableNum,
                true);
    }

    private void setupGatewayNodeDropTable(DeviceId deviceId) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .drop();

        this.setRule(
                appId,
                deviceId,
                selector.build(),
                treatment.build(),
                DROP_PRIORITY,
                GW_DROP_TABLE,
                true);
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
                            initializeWorkerNodePipeline(node);
                        } else {
                            initializeGatewayNodePipeline(node);
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
