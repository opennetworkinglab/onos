/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.store.flow.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.onlab.packet.IpAddress;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.mastership.MastershipInfo;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.flow.FlowRuleOperation;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchEntry;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchOperation;
import org.onosproject.net.intent.IntentTestsMocks;
import org.onosproject.store.cluster.messaging.ClusterCommunicationServiceAdapter;
import org.onosproject.store.persistence.PersistenceServiceAdapter;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.AsyncConsistentMapAdapter;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.ConsistentMapBuilder;
import org.onosproject.store.service.TestStorageService;

import org.onlab.packet.Ip4Address;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.APP_ID_2;
import static org.onosproject.net.NetTestTools.did;

/**
 * Test class for ECFlowRuleStore.
 */
public class ECFlowRuleStoreTest {

    ECFlowRuleStore flowStoreImpl;
    ComponentContext context = null;
    private ClusterService mockClusterService;
    private ControllerNode mockControllerNode;

    private NodeId nodeId;

    private static final IntentTestsMocks.MockSelector SELECTOR =
            new IntentTestsMocks.MockSelector();
    private static final IntentTestsMocks.MockTreatment TREATMENT =
            new IntentTestsMocks.MockTreatment();
    DeviceId deviceId = did("device1");
    DeviceId deviceId2 = did("device2");
    FlowRule flowRule =
            DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(SELECTOR)
                    .withTreatment(TREATMENT)
                    .withPriority(22)
                    .makeTemporary(44)
                    .fromApp(APP_ID)
                    .build();
    FlowRule flowRule1 =
            DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(SELECTOR)
                    .withTreatment(TREATMENT)
                    .withPriority(33)
                    .makeTemporary(44)
                    .fromApp(APP_ID)
                    .build();
    FlowRule flowRule2 =
            DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(SELECTOR)
                    .withTreatment(TREATMENT)
                    .withPriority(44)
                    .makePermanent()
                    .fromApp(APP_ID_2)
                    .build();
    FlowRule flowRule3 =
            DefaultFlowRule.builder()
                    .forDevice(deviceId2)
                    .withSelector(SELECTOR)
                    .withTreatment(TREATMENT)
                    .withPriority(55)
                    .makePermanent()
                    .fromApp(APP_ID_2)
                    .build();


    static class MasterOfAll extends MastershipServiceAdapter {
        @Override
        public MastershipRole getLocalRole(DeviceId deviceId) {
            return MastershipRole.MASTER;
        }

        @Override
        public NodeId getMasterFor(DeviceId deviceId) {
            return new NodeId("1");
        }

        @Override
        public MastershipInfo getMastershipFor(DeviceId deviceId) {
            return new MastershipInfo(
                1,
                Optional.of(NodeId.nodeId("1")),
                ImmutableMap.<NodeId, MastershipRole>builder()
                    .put(NodeId.nodeId("1"), MastershipRole.MASTER)
                    .build());
        }
    }


    private static class MockControllerNode implements ControllerNode {
        final NodeId id;

        public MockControllerNode(NodeId id) {
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
        public Ip4Address ip() {
            return Ip4Address.valueOf("127.0.0.1");
        }

        @Override
        public IpAddress ip(boolean resolve) {
            return ip();
        }

        @Override
        public int tcpPort() {
            return 0;
        }
    }

    @Before
    public void setUp() throws Exception {
        flowStoreImpl = new ECFlowRuleStore();
        flowStoreImpl.storageService = new TestStorageService() {
            @Override
            public <K, V> ConsistentMapBuilder<K, V> consistentMapBuilder() {
                return new ConsistentMapBuilder<K, V>() {
                    @Override
                    public AsyncConsistentMap<K, V> buildAsyncMap() {
                        return new AsyncConsistentMapAdapter<K, V>();
                    }

                    @Override
                    public ConsistentMap<K, V> build() {
                        return null;
                    }
                };
            }
        };

        ReplicaInfoManager replicaInfoManager = new ReplicaInfoManager();
        replicaInfoManager.mastershipService = new MasterOfAll();

        flowStoreImpl.replicaInfoManager = replicaInfoManager;
        mockClusterService = createMock(ClusterService.class);
        flowStoreImpl.clusterService = mockClusterService;
        nodeId = new NodeId("1");
        mockControllerNode = new MockControllerNode(nodeId);

        expect(mockClusterService.getLocalNode())
                .andReturn(mockControllerNode).anyTimes();
        replay(mockClusterService);

        flowStoreImpl.clusterCommunicator = new ClusterCommunicationServiceAdapter();
        flowStoreImpl.mastershipService = new MasterOfAll();
        flowStoreImpl.deviceService = new DeviceServiceAdapter();
        flowStoreImpl.coreService = new CoreServiceAdapter();
        flowStoreImpl.configService = new ComponentConfigAdapter();
        flowStoreImpl.persistenceService = new PersistenceServiceAdapter();
        flowStoreImpl.activate(context);
    }

    @After
    public void tearDown() throws Exception {
        flowStoreImpl.deactivate(context);
    }

    /**
     * Tests the initial state of the store.
     */
    @Test
    public void testEmptyStore() {
        assertThat(flowStoreImpl.getFlowRuleCount(), is(0));
        assertThat(flowStoreImpl.getFlowEntries(deviceId), is(emptyIterable()));
    }

    /**
     * Tests initial state of flowrule.
     */
    @Test
    public void testStoreBatch() {
        FlowRuleOperation op = new FlowRuleOperation(flowRule, FlowRuleOperation.Type.ADD);
        Multimap<DeviceId, FlowRuleBatchEntry> perDeviceBatches = ArrayListMultimap.create();
        perDeviceBatches.put(op.rule().deviceId(),
                new FlowRuleBatchEntry(FlowRuleBatchEntry.FlowRuleOperation.ADD, op.rule()));
        FlowRuleBatchOperation b = new FlowRuleBatchOperation(perDeviceBatches.get(deviceId),
                deviceId, 1);
        flowStoreImpl.storeBatch(b);
        FlowEntry flowEntry1 = flowStoreImpl.getFlowEntry(flowRule);
        assertEquals("PENDING_ADD", flowEntry1.state().toString());
    }

    /**
     * Tests adding a flowrule.
     */
    @Test
    public void testAddFlow() {
        FlowEntry flowEntry = new DefaultFlowEntry(flowRule);
        FlowRuleOperation op = new FlowRuleOperation(flowRule, FlowRuleOperation.Type.ADD);
        Multimap<DeviceId, FlowRuleBatchEntry> perDeviceBatches = ArrayListMultimap.create();
        perDeviceBatches.put(op.rule().deviceId(),
                new FlowRuleBatchEntry(FlowRuleBatchEntry.FlowRuleOperation.ADD, op.rule()));
        FlowRuleBatchOperation b = new FlowRuleBatchOperation(perDeviceBatches.get(deviceId),
                deviceId, 1);
        flowStoreImpl.storeBatch(b);
        FlowEntry flowEntry1 = flowStoreImpl.getFlowEntry(flowRule);
        assertEquals("PENDING_ADD", flowEntry1.state().toString());

        flowStoreImpl.addOrUpdateFlowRule(flowEntry);
        assertFlowsOnDevice(deviceId, 1);

        FlowEntry flowEntry2 = flowStoreImpl.getFlowEntry(flowRule);
        assertEquals("ADDED", flowEntry2.state().toString());
        assertThat(flowStoreImpl.getTableStatistics(deviceId), notNullValue());
    }

    /**
     * Tests flow removal.
     */
    @Test
    public void testRemoveFlow() {
        Iterable<FlowEntry> flows1 = flowStoreImpl.getFlowEntries(deviceId);
        for (FlowEntry flow : flows1) {
            flowStoreImpl.removeFlowRule(flow);
        }
        assertFlowsOnDevice(deviceId, 0);
    }

    /**
     * Tests purge flow for a device.
     */
    @Test
    public void testPurgeFlow() {
        FlowEntry flowEntry = new DefaultFlowEntry(flowRule);
        flowStoreImpl.addOrUpdateFlowRule(flowEntry);

        FlowEntry flowEntry1 = new DefaultFlowEntry(flowRule1);
        flowStoreImpl.addOrUpdateFlowRule(flowEntry1);
        assertFlowsOnDevice(deviceId, 2);
        flowStoreImpl.purgeFlowRule(deviceId);

        assertFlowsOnDevice(deviceId, 0);
    }

    /**
     * Tests purge flow for a device and an application.
     */
    @Test
    public void testPurgeFlowAppId() {
        FlowEntry flowEntry1 = new DefaultFlowEntry(flowRule1);
        flowStoreImpl.addOrUpdateFlowRule(flowEntry1);

        FlowEntry flowEntry2 = new DefaultFlowEntry(flowRule2);
        flowStoreImpl.addOrUpdateFlowRule(flowEntry2);

        FlowEntry flowEntry3 = new DefaultFlowEntry(flowRule3);
        flowStoreImpl.addOrUpdateFlowRule(flowEntry3);

        assertFlowsOnDevice(deviceId, 2);
        assertFlowsOnDevice(deviceId2, 1);

        flowStoreImpl.purgeFlowRules(deviceId, APP_ID_2);

        assertFlowsOnDevice(deviceId, 1);
        assertFlowsOnDevice(deviceId2, 1);
    }

    private void assertFlowsOnDevice(DeviceId deviceId, int nFlows) {
        Iterable<FlowEntry> flows1 = flowStoreImpl.getFlowEntries(deviceId);
        int sum1 = 0;
        for (FlowEntry flowEntry : flows1) {
            sum1++;
        }
        assertThat(sum1, is(nFlows));
    }
}
