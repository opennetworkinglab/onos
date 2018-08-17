/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.store.statistic.impl;

import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import static org.hamcrest.Matchers.empty;

import static org.hamcrest.Matchers.is;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.intent.IntentTestsMocks;
import org.onosproject.net.statistic.StatisticStore;
import org.onosproject.store.cluster.messaging.ClusterCommunicationServiceAdapter;
import org.osgi.service.component.ComponentContext;

import java.util.Iterator;
import java.util.Set;

import static org.easymock.EasyMock.createMock;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.did;


public class DistributedStatisticStoreTest {
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    DistributedStatisticStore statStore;
    StatisticStore store;
    ComponentContext mockContext = null;
    ControllerNode mockControllerNode;
    NodeId nodeId;
    ConnectPoint cp1;
    DeviceId deviceId = did("1");

    private ClusterService mockClusterService;
    private final FlowRule fRule = new IntentTestsMocks.MockFlowRule(1);
    Instruction output = Instructions.createOutput(PortNumber.portNumber(0));
    TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
    TrafficTreatment treatment = tBuilder
            .add(output)
            .build();
    private final DefaultFlowRule nullFlowRule = new DefaultFlowRule(fRule);
    private final FlowRule flowRule1 =
            DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(new IntentTestsMocks.MockSelector())
                    .withTreatment(treatment)
                    .withPriority(22)
                    .makeTemporary(44)
                    .fromApp(APP_ID)
                    .build();
    private final ConnectPoint testConnectPoint = new ConnectPoint(flowRule1.deviceId(), PortNumber.portNumber(1));
    private final DefaultFlowEntry makeFlowEntry(int uniqueValue) {
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(new IntentTestsMocks.MockSelector())
                .withTreatment(treatment)
                .withPriority(uniqueValue)
                .withCookie(uniqueValue)
                .makeTemporary(uniqueValue)
                .build();

        return new DefaultFlowEntry(rule, FlowEntry.FlowEntryState.ADDED,
                uniqueValue, uniqueValue, uniqueValue);
    }

    static class MasterOfAll extends MastershipServiceAdapter {
        @Override
        public MastershipRole getLocalRole(DeviceId deviceId) {
            return MastershipRole.MASTER;
        }

        @Override
        public NodeId getMasterFor(DeviceId deviceId) {
            return new NodeId("1");
        }
    }

    private static class SetMockControllerNode implements ControllerNode {
        final NodeId id;

        public SetMockControllerNode(NodeId id) {
            this.id = id;
        }

        @Override
        public NodeId id() {
            return this.id;
        }

        @Override
        public String host() {
            return "127.0.0.1";
        }

        @Override
        public Ip4Address ip(boolean resolve) {
            return Ip4Address.valueOf("127.0.0.1");
        }

        @Override
        public int tcpPort() {
            return 0;
        }
    }

    @Before
    public void setUp() throws Exception {
        statStore = new DistributedStatisticStore();
        statStore.cfgService = new ComponentConfigAdapter();
        mockClusterService = createMock(ClusterService.class);
        statStore.clusterService = mockClusterService;
        statStore.clusterService = mockClusterService;
        nodeId = new NodeId("1");
        mockControllerNode = new SetMockControllerNode(nodeId);

        expect(mockClusterService.getLocalNode())
                .andReturn(mockControllerNode).anyTimes();
        replay(mockClusterService);

        statStore.clusterCommunicator = new ClusterCommunicationServiceAdapter();
        statStore.mastershipService = new MasterOfAll();

        statStore.activate(mockContext);
        store = statStore;
    }

    @After
    public void tearDown() throws Exception {
        statStore.deactivate();
    }

    @Test
    public void testEmpty() {
          assertThat(store.getPreviousStatistic(testConnectPoint), is(nullValue()));
          assertThat(store.getCurrentStatistic(testConnectPoint), is(nullValue()));
    }

    @Test
    public void testAddStatistic() {

        FlowEntry flowEntry2 = new DefaultFlowEntry(flowRule1);
        assertEquals("PENDING_ADD", flowEntry2.state().toString());

        FlowEntry flowTest1 = makeFlowEntry(1);
        store.prepareForStatistics(flowTest1);
        store.addOrUpdateStatistic(flowTest1);
        cp1 = new ConnectPoint(flowTest1.deviceId(), PortNumber.portNumber(0));
        ConnectPoint cp2 = new ConnectPoint(flowTest1.deviceId(), PortNumber.portNumber(1));
        assertThat("Current map should be null", store.getCurrentStatistic(cp2), is(nullValue()));

        Set<FlowEntry> currStatistic = store.getCurrentStatistic(cp1);
        int currTotal = 0;
        Iterator count = currStatistic.iterator();
            while (count.hasNext()) {
                count.next();
                currTotal++;
            }
        assertThat(currTotal, is(1));
        assertEquals(new NodeId("1"), nodeId);
        assertEquals("ADDED", flowTest1.state().toString());
        assertThat(store.getPreviousStatistic(cp1), is(empty()));

        FlowEntry flowTest2 = makeFlowEntry(10);
        ConnectPoint cp3 = new ConnectPoint(flowTest2.deviceId(), PortNumber.portNumber(0));
        store.addOrUpdateStatistic(flowTest2);
        Set<FlowEntry> prevStatistic = store.getPreviousStatistic(cp3);
        int prevTotal = 0;
        count = prevStatistic.iterator();
            while (count.hasNext()) {
                count.next();
                prevTotal++;
            }
        assertThat(prevTotal, is(1));
    }

    @Test
    public void testRemoveStatistic() {
        FlowEntry flowEntry = makeFlowEntry(1);
        store.prepareForStatistics(flowEntry);
        store.addOrUpdateStatistic(flowEntry);
        cp1 = new ConnectPoint(flowEntry.deviceId(), PortNumber.portNumber(0));
        assertNotNull(store.getCurrentStatistic(cp1));
        store.removeFromStatistics(flowEntry);
        assertThat(store.getCurrentStatistic(cp1), is(empty()));
    }

}