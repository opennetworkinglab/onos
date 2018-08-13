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
package org.onosproject.openstacknode.impl;

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
import org.onosproject.net.Device;
import org.onosproject.openstacknode.api.DefaultOpenstackNode;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeTest;
import org.onosproject.store.service.TestStorageService;

import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.GATEWAY;
import static org.onosproject.openstacknode.api.OpenstackNodeEvent.Type.OPENSTACK_NODE_COMPLETE;
import static org.onosproject.openstacknode.api.OpenstackNodeEvent.Type.OPENSTACK_NODE_CREATED;
import static org.onosproject.openstacknode.api.OpenstackNodeEvent.Type.OPENSTACK_NODE_INCOMPLETE;
import static org.onosproject.openstacknode.api.OpenstackNodeEvent.Type.OPENSTACK_NODE_REMOVED;
import static org.onosproject.openstacknode.api.OpenstackNodeEvent.Type.OPENSTACK_NODE_UPDATED;

/**
 * Unit tests for OpenStack node manager.
 */
public class OpenstackNodeManagerTest extends OpenstackNodeTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String ERR_SIZE = "Number of nodes did not match";
    private static final String ERR_NOT_MATCH = "Node did not match";
    private static final String ERR_NOT_FOUND = "Node did not exist";

    private static final String COMPUTE_1_HOSTNAME = "compute_1";
    private static final String COMPUTE_2_HOSTNAME = "compute_2";
    private static final String COMPUTE_3_HOSTNAME = "compute_3";
    private static final String GATEWAY_1_HOSTNAME = "gateway_1";
    private static final String COMPUTE_1_DUP_INT_HOSTNAME = "compute_1_dup_int";

    private static final String GATEWAY_1_UPLINKPORT = "eth0";

    private static final Device COMPUTE_1_INTG_DEVICE = createDevice(1);
    private static final Device COMPUTE_2_INTG_DEVICE = createDevice(2);
    private static final Device COMPUTE_3_INTG_DEVICE = createDevice(3);
    private static final Device GATEWAY_1_INTG_DEVICE = createDevice(4);

    private static final OpenstackNode COMPUTE_1 = createNode(
            COMPUTE_1_HOSTNAME,
            COMPUTE,
            COMPUTE_1_INTG_DEVICE,
            IpAddress.valueOf("10.100.0.1"),
            NodeState.INIT
    );
    private static final OpenstackNode COMPUTE_2 = createNode(
            COMPUTE_2_HOSTNAME,
            COMPUTE,
            COMPUTE_2_INTG_DEVICE,
            IpAddress.valueOf("10.100.0.2"),
            NodeState.INIT
    );
    private static final OpenstackNode COMPUTE_3 = createNode(
            COMPUTE_3_HOSTNAME,
            COMPUTE,
            COMPUTE_3_INTG_DEVICE,
            IpAddress.valueOf("10.100.0.3"),
            NodeState.COMPLETE
    );
    private static final OpenstackNode GATEWAY_1 = createNode(
            GATEWAY_1_HOSTNAME,
            OpenstackNode.NodeType.GATEWAY,
            GATEWAY_1_INTG_DEVICE,
            IpAddress.valueOf("10.100.0.4"),
            GATEWAY_1_UPLINKPORT,
            NodeState.COMPLETE
    );
    private static final OpenstackNode COMPUTE_1_DUP_INT = createNode(
            COMPUTE_1_DUP_INT_HOSTNAME,
            COMPUTE,
            COMPUTE_1_INTG_DEVICE,
            IpAddress.valueOf("10.100.0.1"),
            NodeState.INIT
    );
    private static final OpenstackNode COMPUTE_2_DUP_INT = createNode(
            COMPUTE_2_HOSTNAME,
            COMPUTE,
            COMPUTE_3_INTG_DEVICE,
            IpAddress.valueOf("10.100.0.2"),
            NodeState.INIT
    );

    private final TestOpenstackNodeListener testListener = new TestOpenstackNodeListener();

    private OpenstackNodeManager target;
    private DistributedOpenstackNodeStore osNodeStore;

    @Before
    public void setUp() {
        osNodeStore = new DistributedOpenstackNodeStore();
        TestUtils.setField(osNodeStore, "coreService", new TestCoreService());
        TestUtils.setField(osNodeStore, "storageService", new TestStorageService());
        TestUtils.setField(osNodeStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        osNodeStore.activate();

        osNodeStore.createNode(COMPUTE_2);
        osNodeStore.createNode(COMPUTE_3);
        osNodeStore.createNode(GATEWAY_1);

        target = new org.onosproject.openstacknode.impl.OpenstackNodeManager();
        target.storageService = new TestStorageService();
        target.coreService = new TestCoreService();
        target.clusterService = new TestClusterService();
        target.leadershipService = new TestLeadershipService();
        target.osNodeStore = osNodeStore;
        target.addListener(testListener);
        target.activate();
        testListener.events.clear();
    }

    @After
    public void tearDown() {
        target.removeListener(testListener);
        target.deactivate();
        osNodeStore.deactivate();
        osNodeStore = null;
        target = null;
    }

    /**
     * Checks if creating and removing a node work well with proper events.
     */
    @Test
    public void testCreateAndRemoveNode() {
        target.createNode(COMPUTE_1);
        assertEquals(ERR_SIZE, 4, target.nodes().size());
        assertTrue(target.node(COMPUTE_1_HOSTNAME) != null);

        target.removeNode(COMPUTE_1_HOSTNAME);
        assertEquals(ERR_SIZE, 3, target.nodes().size());
        assertTrue(target.node(COMPUTE_1_HOSTNAME) == null);

        validateEvents(OPENSTACK_NODE_CREATED, OPENSTACK_NODE_REMOVED);
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
        target.createNode(COMPUTE_1);
        target.createNode(COMPUTE_1);
    }

    /**
     * Checks if creating a node with duplicated integration bridge.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateNodeWithDuplicateIntgBridge() {
        target.createNode(COMPUTE_1);
        target.createNode(COMPUTE_1_DUP_INT);
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
        OpenstackNode updated = DefaultOpenstackNode.from(COMPUTE_2)
                .dataIp(IpAddress.valueOf("10.200.0.100"))
                .build();
        target.updateNode(updated);
        assertEquals(ERR_NOT_MATCH, updated, target.node(COMPUTE_2_INTG_DEVICE.id()));
        validateEvents(OPENSTACK_NODE_UPDATED);
    }

    /**
     * Checks if updating a node with duplicated integration bridge.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateNodeWithDuplicateIntgBridge() {
        target.updateNode(COMPUTE_2_DUP_INT);
    }

    /**
     * Checks if updating a node state to complete generates proper events.
     */
    @Test
    public void testUpdateNodeStateComplete() {
        OpenstackNode updated = DefaultOpenstackNode.from(COMPUTE_2)
                .state(NodeState.COMPLETE)
                .build();
        target.updateNode(updated);
        assertEquals(ERR_NOT_MATCH, updated, target.node(COMPUTE_2_HOSTNAME));
        validateEvents(OPENSTACK_NODE_UPDATED, OPENSTACK_NODE_COMPLETE);
    }

    /**
     * Checks if updating a node state to incomplete generates proper events.
     */
    @Test
    public void testUpdateNodeStateIncomplete() {
        OpenstackNode updated = DefaultOpenstackNode.from(COMPUTE_3)
                .state(NodeState.INCOMPLETE)
                .build();
        target.updateNode(updated);
        assertEquals(ERR_NOT_MATCH, updated, target.node(COMPUTE_3_HOSTNAME));
        validateEvents(OPENSTACK_NODE_UPDATED, OPENSTACK_NODE_INCOMPLETE);
    }

    /**
     * Checks if updating a null node fails with proper exception.
     */
    @Test(expected = NullPointerException.class)
    public void testUpdateNullNode() {
        target.updateNode(null);
    }

    /**
     * Checks if updating not existing node fails with proper exception.
     */
    @Test(expected = NullPointerException.class)
    public void testUpdateNotExistingNode() {
        target.updateNode(COMPUTE_1);
    }

    /**
     * Checks if getting all nodes method returns correct set of nodes.
     */
    @Test
    public void testGetAllNodes() {
        assertEquals(ERR_SIZE, 3, target.nodes().size());
        assertTrue(ERR_NOT_FOUND, target.nodes().contains(COMPUTE_2));
        assertTrue(ERR_NOT_FOUND, target.nodes().contains(COMPUTE_3));
        assertTrue(ERR_NOT_FOUND, target.nodes().contains(GATEWAY_1));
    }

    /**
     * Checks if getting complete nodes method returns correct set of nodes.
     */
    @Test
    public void testGetCompleteNodes() {
        assertEquals(ERR_SIZE, 2, target.completeNodes().size());
        assertTrue(ERR_NOT_FOUND, target.completeNodes().contains(COMPUTE_3));
        assertTrue(ERR_NOT_FOUND, target.completeNodes().contains(GATEWAY_1));
    }

    /**
     * Checks if getting nodes by type method returns correct set of nodes.
     */
    @Test
    public void testGetNodesByType() {
        assertEquals(ERR_SIZE, 2, target.nodes(COMPUTE).size());
        assertTrue(ERR_NOT_FOUND, target.nodes(COMPUTE).contains(COMPUTE_2));
        assertTrue(ERR_NOT_FOUND, target.nodes(COMPUTE).contains(COMPUTE_3));

        assertEquals(ERR_SIZE, 1, target.nodes(GATEWAY).size());
        assertTrue(ERR_NOT_FOUND, target.nodes(GATEWAY).contains(GATEWAY_1));
    }

    /**
     * Checks if getting a node by hostname returns correct node.
     */
    @Test
    public void testGetNodeByHostname() {
        assertTrue(ERR_NOT_FOUND, Objects.equals(
                target.node(COMPUTE_2_HOSTNAME), COMPUTE_2));
        assertTrue(ERR_NOT_FOUND, Objects.equals(
                target.node(COMPUTE_3_HOSTNAME), COMPUTE_3));
        assertTrue(ERR_NOT_FOUND, Objects.equals(
                target.node(GATEWAY_1_HOSTNAME), GATEWAY_1));
    }

    /**
     * Checks if getting a node by device ID returns correct node.
     */
    @Test
    public void testGetNodeByDeviceId() {
        assertTrue(ERR_NOT_FOUND, Objects.equals(
                target.node(GATEWAY_1_INTG_DEVICE.id()), GATEWAY_1));
        assertTrue(ERR_NOT_FOUND, Objects.equals(
                target.node(GATEWAY_1.ovsdb()), GATEWAY_1));
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

    private static class TestOpenstackNodeListener implements OpenstackNodeListener {
        private List<OpenstackNodeEvent> events = Lists.newArrayList();

        @Override
        public void event(OpenstackNodeEvent event) {
            events.add(event);
        }
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