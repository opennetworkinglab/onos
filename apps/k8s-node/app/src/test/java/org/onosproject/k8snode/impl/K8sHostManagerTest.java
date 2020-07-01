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
package org.onosproject.k8snode.impl;

import com.google.common.collect.ImmutableSet;
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
import org.onosproject.k8snode.api.DefaultK8sHost;
import org.onosproject.k8snode.api.K8sHost;
import org.onosproject.k8snode.api.K8sHostEvent;
import org.onosproject.k8snode.api.K8sHostListener;
import org.onosproject.k8snode.api.K8sHostState;
import org.onosproject.store.service.TestStorageService;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.k8snode.api.K8sHostEvent.Type.K8S_HOST_COMPLETE;
import static org.onosproject.k8snode.api.K8sHostEvent.Type.K8S_HOST_CREATED;
import static org.onosproject.k8snode.api.K8sHostEvent.Type.K8S_HOST_REMOVED;
import static org.onosproject.k8snode.api.K8sHostEvent.Type.K8S_HOST_UPDATED;
import static org.onosproject.k8snode.api.K8sHostEvent.Type.K8S_NODES_ADDED;
import static org.onosproject.k8snode.api.K8sHostEvent.Type.K8S_NODES_REMOVED;
import static org.onosproject.k8snode.api.K8sHostState.COMPLETE;
import static org.onosproject.k8snode.api.K8sHostState.INIT;

/**
 * Unit tests for kubernetes host manager.
 */
public class K8sHostManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String ERR_SIZE = "Number of hosts did not match";
    private static final String ERR_NOT_MATCH = "Host did not match";
    private static final String ERR_NOT_FOUND = "Host did not exist";

    private static final IpAddress HOST_IP_1 = IpAddress.valueOf("192.168.100.2");
    private static final IpAddress HOST_IP_2 = IpAddress.valueOf("192.168.101.2");
    private static final IpAddress HOST_IP_3 = IpAddress.valueOf("192.168.102.2");

    private static final K8sHost HOST_1 = createHost(
            HOST_IP_1,
            INIT,
            ImmutableSet.of("1", "2")
    );

    private static final K8sHost HOST_2 = createHost(
            HOST_IP_2,
            INIT,
            ImmutableSet.of("3", "4")
    );

    private static final K8sHost HOST_3 = createHost(
            HOST_IP_3,
            COMPLETE,
            ImmutableSet.of("5", "6")
    );

    private final TestK8sHostListener testListener = new TestK8sHostListener();

    private K8sHostManager target;
    private DistributedK8sHostStore hostStore;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        hostStore = new DistributedK8sHostStore();
        TestUtils.setField(hostStore, "coreService", new TestCoreService());
        TestUtils.setField(hostStore, "storageService", new TestStorageService());
        TestUtils.setField(hostStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        hostStore.activate();

        hostStore.createHost(HOST_2);
        hostStore.createHost(HOST_3);

        target = new K8sHostManager();
        target.storageService = new TestStorageService();
        target.coreService = new TestCoreService();
        target.clusterService = new TestClusterService();
        target.leadershipService = new TestLeadershipService();
        target.hostStore = hostStore;
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
        hostStore.deactivate();
        hostStore = null;
        target = null;
    }

    /**
     * Checks if creating and removing a host work well with proper events.
     */
    @Test
    public void testCreateAndRemoveHost() {
        target.createHost(HOST_1);
        assertEquals(ERR_SIZE, 3, target.hosts().size());
        assertNotNull(target.host(HOST_IP_1));

        target.removeHost(HOST_IP_1);
        assertEquals(ERR_SIZE, 2, target.hosts().size());
        assertNull(target.host(HOST_IP_1));

        validateEvents(K8S_HOST_CREATED, K8S_HOST_REMOVED);
    }

    /**
     * Checks if creating null host fails with proper exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullHost() {
        target.createHost(null);
    }

    /**
     * Checks if creating a duplicated host fails with proper exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateDuplicateHost() {
        target.createHost(HOST_1);
        target.createHost(HOST_1);
    }

    /**
     * Checks if removing null host fails with proper exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRemoveNullHost() {
        target.removeHost(null);
    }

    /**
     * Checks if updating a host works well with proper event.
     */
    @Test
    public void testUpdateHost() {
        K8sHost updated = HOST_2.updateState(COMPLETE);
        target.updateHost(updated);
        validateEvents(K8S_HOST_UPDATED, K8S_HOST_COMPLETE);
    }

    /**
     * Checks if updating a null host fails with proper exception.
     */
    @Test(expected = NullPointerException.class)
    public void testUpdateNullHost() {
        target.updateHost(null);
    }

    /**
     * Checks if updating not existing host fails with proper exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateNotExistingHost() {
        target.updateHost(HOST_1);
    }

    /**
     * Checks if adding nodes into host works well with proper event.
     */
    @Test
    public void testAddNodesToHost() {
        K8sHost updated = HOST_2.updateNodeNames(ImmutableSet.of("3", "4", "5"));
        target.updateHost(updated);
        validateEvents(K8S_HOST_UPDATED, K8S_NODES_ADDED);
    }

    /**
     * Checks if removing nodes from host works well with proper event.
     */
    @Test
    public void testRemoveNodesFromHost() {
        K8sHost updated = HOST_2.updateNodeNames(ImmutableSet.of("3"));
        target.updateHost(updated);
        validateEvents(K8S_HOST_UPDATED, K8S_NODES_REMOVED);
    }

    /**
     * Checks if getting all hosts method returns correct set of nodes.
     */
    @Test
    public void testGetAllHosts() {
        assertEquals(ERR_SIZE, 2, target.hosts().size());
        assertTrue(ERR_NOT_FOUND, target.hosts().contains(HOST_2));
        assertTrue(ERR_NOT_FOUND, target.hosts().contains(HOST_3));
    }

    /**
     * Checks if getting complete hosts method returns correct set of nodes.
     */
    @Test
    public void testGetCompleteHosts() {
        assertEquals(ERR_SIZE, 1, target.completeHosts().size());
        assertTrue(ERR_NOT_FOUND, target.completeHosts().contains(HOST_3));
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

    private static class TestK8sHostListener implements K8sHostListener {
        private List<K8sHostEvent> events = Lists.newArrayList();

        @Override
        public void event(K8sHostEvent event) {
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

    private static K8sHost createHost(IpAddress hostIp, K8sHostState state, Set<String> nodeNames) {
        return DefaultK8sHost.builder()
                .hostIp(hostIp)
                .nodeNames(nodeNames)
                .state(state)
                .build();
    }
}
