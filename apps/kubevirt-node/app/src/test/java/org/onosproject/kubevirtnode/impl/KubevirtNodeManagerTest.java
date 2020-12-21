/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.impl;

import com.google.common.collect.Lists;
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
import org.onosproject.event.Event;
import org.onosproject.kubevirtnode.api.DefaultKubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeEvent;
import org.onosproject.kubevirtnode.api.KubevirtNodeListener;
import org.onosproject.kubevirtnode.api.KubevirtNodeState;
import org.onosproject.kubevirtnode.api.KubevirtNodeTest;
import org.onosproject.net.Device;
import org.onosproject.store.service.TestStorageService;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.WORKER;
import static org.onosproject.kubevirtnode.api.KubevirtNodeEvent.Type.KUBEVIRT_NODE_COMPLETE;
import static org.onosproject.kubevirtnode.api.KubevirtNodeEvent.Type.KUBEVIRT_NODE_CREATED;
import static org.onosproject.kubevirtnode.api.KubevirtNodeEvent.Type.KUBEVIRT_NODE_REMOVED;
import static org.onosproject.kubevirtnode.api.KubevirtNodeEvent.Type.KUBEVIRT_NODE_UPDATED;

/**
 * Unit tests for KubeVirt node manager.
 */
public class KubevirtNodeManagerTest extends KubevirtNodeTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String ERR_SIZE = "Number of nodes did not match";
    private static final String ERR_NOT_MATCH = "Node did not match";
    private static final String ERR_NOT_FOUND = "Node did not exist";

    private static final String WORKER_1_HOSTNAME = "worker_1";
    private static final String WORKER_2_HOSTNAME = "worker_2";
    private static final String WORKER_3_HOSTNAME = "worker_3";
    private static final String WORKER_1_DUP_INT_HOSTNAME = "worker_1_dup_int";

    private static final Device WORKER_1_INTG_DEVICE = createDevice(1);
    private static final Device WORKER_2_INTG_DEVICE = createDevice(2);
    private static final Device WORKER_3_INTG_DEVICE = createDevice(3);

    private static final KubevirtNode WORKER_1 = createNode(
            WORKER_1_HOSTNAME,
            WORKER,
            WORKER_1_INTG_DEVICE,
            IpAddress.valueOf("10.100.0.1"),
            KubevirtNodeState.INIT
    );
    private static final KubevirtNode WORKER_2 = createNode(
            WORKER_2_HOSTNAME,
            WORKER,
            WORKER_2_INTG_DEVICE,
            IpAddress.valueOf("10.100.0.2"),
            KubevirtNodeState.INIT
    );
    private static final KubevirtNode WORKER_3 = createNode(
            WORKER_3_HOSTNAME,
            WORKER,
            WORKER_3_INTG_DEVICE,
            IpAddress.valueOf("10.100.0.3"),
            KubevirtNodeState.COMPLETE
    );
    private static final KubevirtNode WORKER_DUP_INT = createNode(
            WORKER_1_DUP_INT_HOSTNAME,
            WORKER,
            WORKER_3_INTG_DEVICE,
            IpAddress.valueOf("10.100.0.2"),
            KubevirtNodeState.COMPLETE
    );

    private final TestKubevirtNodeListener testListener = new TestKubevirtNodeListener();

    private KubevirtNodeManager target;
    private DistributedKubevirtNodeStore nodeStore;

    @Before
    public void setUp() {
        nodeStore = new DistributedKubevirtNodeStore();
        TestUtils.setField(nodeStore, "coreService", new TestCoreService());
        TestUtils.setField(nodeStore, "storageService", new TestStorageService());
        TestUtils.setField(nodeStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        nodeStore.activate();

        nodeStore.createNode(WORKER_2);
        nodeStore.createNode(WORKER_3);

        target = new KubevirtNodeManager();
        target.storageService = new TestStorageService();
        target.coreService = new TestCoreService();
        target.clusterService = new TestClusterService();
        target.leadershipService = new TestLeadershipService();
        target.nodeStore = nodeStore;
        target.addListener(testListener);
        target.activate();
        testListener.events.clear();
    }

    @After
    public void tearDown() {
        target.removeListener(testListener);
        target.deactivate();
        nodeStore.deactivate();
        nodeStore = null;
        target = null;
    }

    private static class TestKubevirtNodeListener implements KubevirtNodeListener {
        private List<KubevirtNodeEvent> events = Lists.newArrayList();

        @Override
        public void event(KubevirtNodeEvent event) {
            events.add(event);
        }
    }

    /**
     * Checks if creating and removing a node work well with proper events.
     */
    @Test
    public void testCreateAndRemoveNode() {
        target.createNode(WORKER_1);
        assertEquals(ERR_SIZE, 3, target.nodes().size());
        assertTrue(target.node(WORKER_1_HOSTNAME) != null);

        target.removeNode(WORKER_1_HOSTNAME);
        assertEquals(ERR_SIZE, 2, target.nodes().size());
        assertTrue(target.node(WORKER_1_HOSTNAME) == null);

        validateEvents(KUBEVIRT_NODE_CREATED, KUBEVIRT_NODE_REMOVED);
    }

    /**
     * Checks if creating null node fails with proper exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullNode() {
        target.createNode(null);
    }

    /**
     * Checks if creating a duplicated node fails with proper exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateDuplicateNode() {
        target.createNode(WORKER_1);
        target.createNode(WORKER_1);
    }

    /**
     * Checks if removing null node fails with proper exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRemoveNullNode() {
        target.removeNode(null);
    }

    /**
     * Checks if updating a node works well with proper event.
     */
    @Test
    public void testUpdateNode() {
        KubevirtNode updated = DefaultKubevirtNode.from(WORKER_2)
                .dataIp(IpAddress.valueOf("10.200.0.100"))
                .build();
        target.updateNode(updated);
        assertEquals(ERR_NOT_MATCH, updated, target.node(WORKER_2_INTG_DEVICE.id()));
        validateEvents(KUBEVIRT_NODE_UPDATED);
    }

    /**
     * Checks if updating a node state to complete generates proper events.
     */
    @Test
    public void testUpdateNodeStateComplete() {
        KubevirtNode updated = DefaultKubevirtNode.from(WORKER_2)
                .state(KubevirtNodeState.COMPLETE)
                .build();
        target.updateNode(updated);
        assertEquals(ERR_NOT_MATCH, updated, target.node(WORKER_2_HOSTNAME));
        validateEvents(KUBEVIRT_NODE_UPDATED, KUBEVIRT_NODE_COMPLETE);
    }

    private void validateEvents(Enum... types) {
        int i = 0;
        assertEquals("Number of events did not match", types.length, testListener.events.size());
        for (Event event : testListener.events) {
            assertEquals("Incorrect event received", types[i], event.type());
            i++;
        }
        testListener.events.clear();
    }

    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private class TestClusterService extends ClusterServiceAdapter {

    }

    private static class TestLeadershipService extends LeadershipServiceAdapter {

    }
}
