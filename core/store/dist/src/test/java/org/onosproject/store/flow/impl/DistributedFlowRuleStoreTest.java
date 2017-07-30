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

import java.util.Collections;
import java.util.Iterator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleOperation;
import org.onosproject.net.intent.IntentTestsMocks;
import org.onosproject.store.cluster.messaging.ClusterCommunicationServiceAdapter;
import org.onosproject.store.service.AsyncDocumentTree;
import org.onosproject.store.service.AsyncDocumentTreeAdapter;
import org.onosproject.store.service.DocumentTree;
import org.onosproject.store.service.DocumentTreeBuilder;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.TestDocumentTree;
import org.onosproject.store.service.TestStorageService;
import org.onosproject.store.service.TestTopic;
import org.onosproject.store.service.Topic;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.did;

/**
 * Test class for DistributedFlowRuleStore.
 */
public class DistributedFlowRuleStoreTest {

    DistributedFlowRuleStore flowStoreImpl;
    private ClusterService mockClusterService;
    private ControllerNode mockControllerNode;

    private NodeId nodeId;

    private static final IntentTestsMocks.MockSelector SELECTOR =
            new IntentTestsMocks.MockSelector();
    private static final IntentTestsMocks.MockTreatment TREATMENT =
            new IntentTestsMocks.MockTreatment();
    DeviceId deviceId = did("device1");
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
        public Ip4Address ip() {
            return Ip4Address.valueOf("127.0.0.1");
        }

        @Override
        public int tcpPort() {
            return 0;
        }
    }

    private static class MockStorageService extends TestStorageService {
        @Override
        public <V> DocumentTreeBuilder<V> documentTreeBuilder() {
            return new DocumentTreeBuilder<V>() {
                @Override
                public AsyncDocumentTree<V> buildDocumentTree() {
                    return build();
                }

                @Override
                @SuppressWarnings("unchecked")
                public AsyncDocumentTree<V> build() {
                    String name = name();
                    return new AsyncDocumentTreeAdapter() {
                        @Override
                        public DocumentTree asDocumentTree() {
                            return new TestDocumentTree(name);
                        }
                    };
                }
            };
        }

        @Override
        public <T> Topic<T> getTopic(String name, Serializer serializer) {
            return new TestTopic<>(name);
        }
    }

    @Before
    public void setUp() throws Exception {
        flowStoreImpl = new DistributedFlowRuleStore();
        flowStoreImpl.storageService = new MockStorageService();
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
        flowStoreImpl.activate();
    }

    @After
    public void tearDown() throws Exception {
        flowStoreImpl.deactivate();
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
        Iterable<FlowEntry> flows = flowStoreImpl.getFlowEntries(deviceId);
        int sum = 0;
        Iterator it = flows.iterator();
        while (it.hasNext()) {
            it.next();
            sum++;
        }
        assertThat(sum, is(1));

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

        Iterable<FlowEntry> flows2 = flowStoreImpl.getFlowEntries(deviceId);
        int sum = 0;
        Iterator it = flows2.iterator();
        while (it.hasNext()) {
            it.next();
            sum++;
        }
        assertThat(sum, is(0));
    }

    /**
     * Tests purge flow for a device.
     */
    @Test
    public void testPurgeFlow() {
        FlowEntry flowEntry = new DefaultFlowEntry(flowRule);
        flowStoreImpl.storeBatch(new FlowRuleBatchOperation(
                Collections.singletonList(new FlowRuleBatchEntry(FlowRuleBatchEntry.FlowRuleOperation.ADD, flowEntry)),
                flowEntry.deviceId(), 1));

        FlowEntry flowEntry1 = new DefaultFlowEntry(flowRule1);
        flowStoreImpl.storeBatch(new FlowRuleBatchOperation(
                Collections.singletonList(new FlowRuleBatchEntry(FlowRuleBatchEntry.FlowRuleOperation.ADD, flowEntry1)),
                flowEntry1.deviceId(), 2));

        Iterable<FlowEntry> flows1 = flowStoreImpl.getFlowEntries(deviceId);
        int sum2 = 0;
        Iterator it1 = flows1.iterator();
        while (it1.hasNext()) {
            it1.next();
            sum2++;
        }
        assertThat(sum2, is(2));
        flowStoreImpl.purgeFlowRule(deviceId);

        Iterable<FlowEntry> flows3 = flowStoreImpl.getFlowEntries(deviceId);
        int sum3 = 0;
        Iterator it3 = flows3.iterator();
        while (it3.hasNext()) {
            it3.next();
            sum3++;
        }
        assertThat(sum3, is(0));
    }
}
