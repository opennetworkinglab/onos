/*
 * Copyright 2018-present Open Networking Foundation
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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.LeadershipServiceAdapter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperation;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleServiceAdapter;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.openstacknode.api.DefaultOpenstackNode;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.onosproject.openstacknetworking.api.Constants.ACL_EGRESS_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.ACL_INGRESS_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.ARP_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.DHCP_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.FLAT_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.FORWARDING_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.JUMP_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.PRE_FLAT_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.STAT_INBOUND_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.STAT_OUTBOUND_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.VTAG_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.VTAP_INBOUND_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.VTAP_OUTBOUND_TABLE;

/**
 * Unit tests for flow rule manager.
 */
public class OpenstackFlowRuleManagerTest {

    private static final ApplicationId TEST_APP_ID =
                                        new DefaultApplicationId(1, "test");

    private static final int DROP_PRIORITY = 0;

    private static final DeviceId DEVICE_ID = DeviceId.deviceId("of:000000000000000a");

    private OpenstackFlowRuleManager target;

    private Set<FlowRuleOperation> fros;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        target = new OpenstackFlowRuleManager();
        TestUtils.setField(target, "coreService", new TestCoreService());
        TestUtils.setField(target, "flowRuleService", new TestFlowRuleService());
        TestUtils.setField(target, "clusterService", new TestClusterService());
        TestUtils.setField(target, "leadershipService", new TestLeadershipService());
        TestUtils.setField(target, "osNodeService", new TestOpenstackNodeService());
        TestUtils.setField(target, "deviceEventExecutor", MoreExecutors.newDirectExecutorService());

        target.activate();
    }

    /**
     * Tears down of this unit test.
     */
    @After
    public void tearDown() {
        target.deactivate();
        target = null;
    }

    /**
     * Tests whether the set rule method installs the flow rules properly.
     */
    @Test
    public void testSetRule() {
        int testPriority = 10;
        int testTableType = 10;

        fros = Sets.newConcurrentHashSet();

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();

        FlowRule.Builder flowRuleBuilder = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentBuilder.build())
                .withPriority(testPriority)
                .fromApp(TEST_APP_ID)
                .forTable(testTableType)
                .makePermanent();

        target.setRule(TEST_APP_ID, DEVICE_ID, selectorBuilder.build(),
                treatmentBuilder.build(), testPriority, testTableType, true);
        validateFlowRule(flowRuleBuilder.build());
    }

    /**
     * Tests whether the connect tables method installs the flow rules properly.
     */
    @Test
    public void testConnectTables() {
        int testFromTable = 1;
        int testToTable = 2;

        fros = Sets.newConcurrentHashSet();

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();

        target.connectTables(DEVICE_ID, testFromTable, testToTable);

        FlowRule.Builder flowRuleBuilder = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentBuilder.transition(testToTable).build())
                .withPriority(DROP_PRIORITY)
                .fromApp(TEST_APP_ID)
                .forTable(testFromTable)
                .makePermanent();

        validateFlowRule(flowRuleBuilder.build());
    }

    /**
     * Tests whether the setup table miss entry method installs the flow rules properly.
     */
    @Test
    public void testSetUpTableMissEntry() {
        int testTable = 10;

        fros = Sets.newConcurrentHashSet();

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();

        target.setUpTableMissEntry(DEVICE_ID, testTable);

        FlowRule.Builder flowRuleBuilder = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentBuilder.drop().build())
                .withPriority(DROP_PRIORITY)
                .fromApp(TEST_APP_ID)
                .forTable(testTable)
                .makePermanent();

        validateFlowRule(flowRuleBuilder.build());
    }

    /**
     * Tests whether initialize pipeline method installs the flow rules properly.
     */
    @Test
    public void testInitializePipeline() {

        fros = Sets.newConcurrentHashSet();

        target.initializePipeline(DEVICE_ID);
        assertEquals("Flow Rule size was not match", 12, fros.size());

        Map<Integer, Integer> fromToTableMap = Maps.newConcurrentMap();
        fromToTableMap.put(STAT_INBOUND_TABLE, VTAP_INBOUND_TABLE);
        fromToTableMap.put(VTAP_INBOUND_TABLE, DHCP_TABLE);
        fromToTableMap.put(DHCP_TABLE, VTAG_TABLE);
        fromToTableMap.put(VTAG_TABLE, ARP_TABLE);
        fromToTableMap.put(ARP_TABLE, ACL_INGRESS_TABLE);
        fromToTableMap.put(ACL_EGRESS_TABLE, JUMP_TABLE);
        fromToTableMap.put(STAT_OUTBOUND_TABLE, VTAP_OUTBOUND_TABLE);
        fromToTableMap.put(VTAP_OUTBOUND_TABLE, FORWARDING_TABLE);
        fromToTableMap.put(PRE_FLAT_TABLE, FLAT_TABLE);

        fros.stream().map(FlowRuleOperation::rule).forEach(fr -> {
            if (fr.tableId() != JUMP_TABLE && fr.tableId() != FLAT_TABLE) {
                assertEquals("To Table did not match,",
                        fromToTableMap.get(fr.tableId()),
                        fr.treatment().tableTransition().tableId());
            }
        });
    }

    private void validateFlowRule(FlowRule ref) {
        assertEquals("Flow Rule size was not match", 1, fros.size());
        List<FlowRuleOperation> froList = Lists.newArrayList();
        froList.addAll(fros);
        FlowRuleOperation fro = froList.get(0);
        FlowRule fr = fro.rule();

        assertEquals("Application ID did not match", ref.appId(), fr.appId());
        assertEquals("Device ID did not match", ref.deviceId(), fr.deviceId());
        assertEquals("Selector did not match", ref.selector(), fr.selector());
        assertEquals("Treatment did not match", ref.treatment(), fr.treatment());
        assertEquals("Priority did not match", ref.priority(), fr.priority());
        assertEquals("Table ID did not match", ref.table(), fr.table());
        assertEquals("Permanent did not match", ref.isPermanent(), fr.isPermanent());
    }

    private class TestOpenstackNodeService extends OpenstackNodeServiceAdapter {
        @Override
        public OpenstackNode node(DeviceId deviceId) {
            return DefaultOpenstackNode.builder()
                    .hostname("host")
                    .type(OpenstackNode.NodeType.COMPUTE)
                    .state(NodeState.COMPLETE)
                    .managementIp(IpAddress.valueOf("1.1.1.1"))
                    .dataIp(IpAddress.valueOf("1.1.1.1"))
                    .build();
        }
    }

    private class TestFlowRuleService extends FlowRuleServiceAdapter {
        @Override
        public void apply(FlowRuleOperations ops) {
            fros.addAll(ops.stages().get(0));
        }
    }

    private class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private class TestClusterService extends ClusterServiceAdapter {
    }

    private class TestLeadershipService extends LeadershipServiceAdapter {
    }
}
