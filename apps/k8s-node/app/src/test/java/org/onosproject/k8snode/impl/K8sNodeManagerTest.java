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
package org.onosproject.k8snode.impl;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.LeadershipServiceAdapter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.Event;
import org.onosproject.k8snode.api.DefaultK8sNode;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeEvent;
import org.onosproject.k8snode.api.K8sNodeInfo;
import org.onosproject.k8snode.api.K8sNodeListener;
import org.onosproject.k8snode.api.K8sNodeState;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.service.TestStorageService;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.k8snode.api.K8sNode.Type.MINION;
import static org.onosproject.k8snode.api.K8sNodeEvent.Type.K8S_NODE_COMPLETE;
import static org.onosproject.k8snode.api.K8sNodeEvent.Type.K8S_NODE_CREATED;
import static org.onosproject.k8snode.api.K8sNodeEvent.Type.K8S_NODE_INCOMPLETE;
import static org.onosproject.k8snode.api.K8sNodeEvent.Type.K8S_NODE_REMOVED;
import static org.onosproject.k8snode.api.K8sNodeEvent.Type.K8S_NODE_UPDATED;
import static org.onosproject.k8snode.api.K8sNodeState.COMPLETE;
import static org.onosproject.k8snode.api.K8sNodeState.INCOMPLETE;
import static org.onosproject.k8snode.api.K8sNodeState.INIT;
import static org.onosproject.net.Device.Type.SWITCH;

/**
 * Unit tests for Kubernetes node manager.
 */
public class K8sNodeManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String ERR_SIZE = "Number of nodes did not match";
    private static final String ERR_NOT_MATCH = "Node did not match";
    private static final String ERR_NOT_FOUND = "Node did not exist";

    private static final String CLUSTER_NAME = "kubernetes";

    private static final String MINION_1_HOSTNAME = "minion_1";
    private static final String MINION_2_HOSTNAME = "minion_2";
    private static final String MINION_3_HOSTNAME = "minion_3";

    private static final IpAddress NODE_IP = IpAddress.valueOf("30.30.30.30");
    private static final MacAddress NODE_MAC = MacAddress.valueOf("fa:00:00:00:00:08");
    private static final K8sNodeInfo NODE_INFO = new K8sNodeInfo(NODE_IP, NODE_MAC);

    private static final Device MINION_1_INTG_DEVICE = createDevice(1);
    private static final Device MINION_2_INTG_DEVICE = createDevice(2);
    private static final Device MINION_3_INTG_DEVICE = createDevice(3);

    private static final Device MINION_1_EXT_DEVICE = createDevice(4);
    private static final Device MINION_2_EXT_DEVICE = createDevice(5);
    private static final Device MINION_3_EXT_DEVICE = createDevice(6);

    private static final Device MINION_1_LOCAL_DEVICE = createDevice(7);
    private static final Device MINION_2_LOCAL_DEVICE = createDevice(8);
    private static final Device MINION_3_LOCAL_DEVICE = createDevice(9);

    private static final Device MINION_1_TUN_DEVICE = createDevice(10);
    private static final Device MINION_2_TUN_DEVICE = createDevice(11);
    private static final Device MINION_3_TUN_DEVICE = createDevice(12);


    private static final K8sNode MINION_1 = createNode(
            CLUSTER_NAME,
            MINION_1_HOSTNAME,
            MINION,
            MINION_1_INTG_DEVICE,
            MINION_1_EXT_DEVICE,
            MINION_1_LOCAL_DEVICE,
            MINION_1_TUN_DEVICE,
            IpAddress.valueOf("10.100.0.1"),
            NODE_INFO,
            INIT
    );
    private static final K8sNode MINION_2 = createNode(
            CLUSTER_NAME,
            MINION_2_HOSTNAME,
            MINION,
            MINION_2_INTG_DEVICE,
            MINION_2_EXT_DEVICE,
            MINION_2_LOCAL_DEVICE,
            MINION_2_TUN_DEVICE,
            IpAddress.valueOf("10.100.0.2"),
            NODE_INFO,
            INIT
    );
    private static final K8sNode MINION_3 = createNode(
            CLUSTER_NAME,
            MINION_3_HOSTNAME,
            MINION,
            MINION_3_INTG_DEVICE,
            MINION_3_EXT_DEVICE,
            MINION_3_LOCAL_DEVICE,
            MINION_3_TUN_DEVICE,
            IpAddress.valueOf("10.100.0.3"),
            NODE_INFO,
            COMPLETE
    );

    private final TestK8sNodeListener testListener = new TestK8sNodeListener();

    private K8sNodeManager target;
    private DistributedK8sNodeStore nodeStore;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        nodeStore = new DistributedK8sNodeStore();
        TestUtils.setField(nodeStore, "coreService", new TestCoreService());
        TestUtils.setField(nodeStore, "storageService", new TestStorageService());
        TestUtils.setField(nodeStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        nodeStore.activate();

        nodeStore.createNode(MINION_2);
        nodeStore.createNode(MINION_3);

        target = new K8sNodeManager();
        target.storageService = new TestStorageService();
        target.coreService = new TestCoreService();
        target.clusterService = new TestClusterService();
        target.leadershipService = new TestLeadershipService();
        target.nodeStore = nodeStore;
        target.addListener(testListener);
        target.activate();
        testListener.events.clear();
    }

    /**
     * Clean up unit test.
     */
    @After
    public void tearDown() {
        target.removeListener(testListener);
        target.deactivate();
        nodeStore.deactivate();
        nodeStore = null;
        target = null;
    }

    /**
     * Checks if creating and removing a node work well with proper events.
     */
    @Test
    public void testCreateAndRemoveNode() {
        target.createNode(MINION_1);
        assertEquals(ERR_SIZE, 3, target.nodes().size());
        assertNotNull(target.node(MINION_1_HOSTNAME));

        target.removeNode(MINION_1_HOSTNAME);
        assertEquals(ERR_SIZE, 2, target.nodes().size());
        assertNull(target.node(MINION_1_HOSTNAME));

        validateEvents(K8S_NODE_CREATED, K8S_NODE_REMOVED);
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
        target.createNode(MINION_1);
        target.createNode(MINION_1);
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
        K8sNode updated = DefaultK8sNode.from(MINION_2)
                .dataIp(IpAddress.valueOf("10.200.0.100"))
                .build();
        target.updateNode(updated);
        assertEquals(ERR_NOT_MATCH, updated, target.node(MINION_2_INTG_DEVICE.id()));
        validateEvents(K8S_NODE_UPDATED);
    }

    /**
     * Checks if updating a node state to complete generates proper events.
     */
    @Test
    public void testUpdateNodeStateComplete() {
        K8sNode updated = DefaultK8sNode.from(MINION_2)
                .state(COMPLETE)
                .build();
        target.updateNode(updated);
        assertEquals(ERR_NOT_MATCH, updated, target.node(MINION_2_HOSTNAME));
        validateEvents(K8S_NODE_UPDATED, K8S_NODE_COMPLETE);
    }

    /**
     * Checks if updating a node state to incomplete generates proper events.
     */
    @Test
    public void testUpdateNodeStateIncomplete() {
        K8sNode updated = DefaultK8sNode.from(MINION_3)
                .state(INCOMPLETE)
                .build();
        target.updateNode(updated);
        assertEquals(ERR_NOT_MATCH, updated, target.node(MINION_3_HOSTNAME));
        validateEvents(K8S_NODE_UPDATED, K8S_NODE_INCOMPLETE);
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
        target.updateNode(MINION_1);
    }

    /**
     * Checks if getting all nodes method returns correct set of nodes.
     */
    @Test
    public void testGetAllNodes() {
        assertEquals(ERR_SIZE, 2, target.nodes().size());
        assertTrue(ERR_NOT_FOUND, target.nodes().contains(MINION_2));
        assertTrue(ERR_NOT_FOUND, target.nodes().contains(MINION_3));
    }

    /**
     * Checks if getting complete nodes method returns correct set of nodes.
     */
    @Test
    public void testGetCompleteNodes() {
        assertEquals(ERR_SIZE, 1, target.completeNodes().size());
        assertTrue(ERR_NOT_FOUND, target.completeNodes().contains(MINION_3));
    }

    /**
     * Checks if getting nodes by type method returns correct set of nodes.
     */
    @Test
    public void testGetNodesByType() {
        assertEquals(ERR_SIZE, 2, target.nodes(MINION).size());
        assertTrue(ERR_NOT_FOUND, target.nodes(MINION).contains(MINION_2));
        assertTrue(ERR_NOT_FOUND, target.nodes(MINION).contains(MINION_3));
    }

    /**
     * Checks if getting a node by hostname returns correct node.
     */
    @Test
    public void testGetNodeByHostname() {
        assertEquals(ERR_NOT_FOUND, target.node(MINION_2_HOSTNAME), MINION_2);
        assertEquals(ERR_NOT_FOUND, target.node(MINION_3_HOSTNAME), MINION_3);
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

    private static Device createDevice(long devIdNum) {
        return new DefaultDevice(new ProviderId("of", "foo"),
                DeviceId.deviceId(String.format("of:%016d", devIdNum)),
                SWITCH,
                "manufacturer",
                "hwVersion",
                "swVersion",
                "serialNumber",
                new ChassisId(1));
    }

    private static class TestK8sNodeListener implements K8sNodeListener {
        private List<K8sNodeEvent> events = Lists.newArrayList();

        @Override
        public void event(K8sNodeEvent event) {
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

    private static K8sNode createNode(String clusterName, String hostname, K8sNode.Type type,
                                      Device intgBridge, Device extBridge,
                                      Device localBridge, Device tunBridge,
                                      IpAddress ipAddr, K8sNodeInfo nodeInfo,
                                      K8sNodeState state) {
        return DefaultK8sNode.builder()
                .hostname(hostname)
                .clusterName(clusterName)
                .type(type)
                .intgBridge(intgBridge.id())
                .extBridge(extBridge.id())
                .localBridge(localBridge.id())
                .tunBridge(tunBridge.id())
                .managementIp(ipAddr)
                .dataIp(ipAddr)
                .nodeInfo(nodeInfo)
                .state(state)
                .build();
    }
}
