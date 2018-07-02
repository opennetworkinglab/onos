/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.openstacknetworking.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Sets flow rules directly using FlowRuleService.
 */
@Service
@Component(immediate = true)
public class OpenstackFlowRuleManager implements OpenstackFlowRuleService {

    private final Logger log = getLogger(getClass());

    private static final int DROP_PRIORITY = 0;
    private static final int HIGH_PRIORITY = 30000;
    private static final int TIMEOUT_SNAT_RULE = 60;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService osNodeService;

    private final ExecutorService deviceEventExecutor =
            Executors.newSingleThreadExecutor(groupedThreads("openstacknetworking", "device-event"));
    private final OpenstackNodeListener internalNodeListener = new InternalOpenstackNodeListener();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        osNodeService.addListener(internalNodeListener);

        osNodeService.completeNodes(OpenstackNode.NodeType.COMPUTE)
                .forEach(node -> initializePipeline(node.intgBridge()));

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        osNodeService.removeListener(internalNodeListener);
        deviceEventExecutor.shutdown();

        log.info("Stopped");
    }


    @Override
    public void setRule(ApplicationId appId,
                               DeviceId deviceId,
                               TrafficSelector selector,
                               TrafficTreatment treatment,
                               int priority,
                               int tableType,
                               boolean install) {

        FlowRule.Builder flowRuleBuilder = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(priority)
                .fromApp(appId)
                .forTable(tableType);

        if (priority == Constants.PRIORITY_SNAT_RULE) {
            flowRuleBuilder.makeTemporary(TIMEOUT_SNAT_RULE);
        } else {
            flowRuleBuilder.makePermanent();
        }

        applyRule(flowRuleBuilder.build(), install);
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
                log.debug("Failed to privision vni or forwarding table");
            }
        }));
    }

    private void initializePipeline(DeviceId deviceId) {
        // for inbound table transition
        connectTables(deviceId, Constants.STAT_INBOUND_TABLE, Constants.VTAP_INBOUND_TABLE);
        connectTables(deviceId, Constants.VTAP_INBOUND_TABLE, Constants.DHCP_ARP_TABLE);

        // for vTag and ACL table transition
        connectTables(deviceId, Constants.DHCP_ARP_TABLE, Constants.VTAG_TABLE);
        connectTables(deviceId, Constants.VTAG_TABLE, Constants.ACL_TABLE);
        connectTables(deviceId, Constants.ACL_TABLE, Constants.JUMP_TABLE);

        // for JUMP table transition
        // we need JUMP table for bypassing routing table which contains large
        // amount of flow rules which might cause performance degradation during
        // table lookup
        setupJumpTable(deviceId);

        // for outbound table transition
        connectTables(deviceId, Constants.STAT_OUTBOUND_TABLE, Constants.VTAP_OUTBOUND_TABLE);
        connectTables(deviceId, Constants.VTAP_OUTBOUND_TABLE, Constants.FORWARDING_TABLE);

        // for FLAT outbound table transition
        connectTables(deviceId, Constants.STAT_FLAT_OUTBOUND_TABLE, Constants.VTAP_FLAT_OUTBOUND_TABLE);
        connectTables(deviceId, Constants.VTAP_FLAT_OUTBOUND_TABLE, Constants.FLAT_TABLE);
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

    private void setupJumpTable(DeviceId deviceId) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        selector.matchEthDst(Constants.DEFAULT_GATEWAY_MAC);
        treatment.transition(Constants.ROUTING_TABLE);

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(HIGH_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(Constants.JUMP_TABLE)
                .build();

        applyRule(flowRule, true);

        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();

        treatment.transition(Constants.STAT_OUTBOUND_TABLE);

        flowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(Constants.JUMP_TABLE)
                .build();

        applyRule(flowRule, true);
    }

    private class InternalOpenstackNodeListener implements OpenstackNodeListener {

        @Override
        public void event(OpenstackNodeEvent event) {
            OpenstackNode osNode = event.subject();
            // TODO check leadership of the node and make only the leader process

            switch (event.type()) {
                case OPENSTACK_NODE_COMPLETE:
                    deviceEventExecutor.execute(() -> {
                        log.info("COMPLETE node {} is detected", osNode.hostname());
                        processCompleteNode(event.subject());
                    });
                    break;
                case OPENSTACK_NODE_CREATED:
                case OPENSTACK_NODE_UPDATED:
                case OPENSTACK_NODE_REMOVED:
                case OPENSTACK_NODE_INCOMPLETE:
                default:
                    // do nothing
                    break;
            }
        }

        private void processCompleteNode(OpenstackNode osNode) {
            if (osNode.type().equals(OpenstackNode.NodeType.COMPUTE)) {
                initializePipeline(osNode.intgBridge());
            }
        }
    }
}
