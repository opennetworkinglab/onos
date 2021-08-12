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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.Event;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtIpPool;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkListener;
import org.onosproject.store.service.TestStorageService;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.VXLAN;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetworkEvent.Type.KUBEVIRT_NETWORK_CREATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetworkEvent.Type.KUBEVIRT_NETWORK_REMOVED;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetworkEvent.Type.KUBEVIRT_NETWORK_UPDATED;

/**
 * Unit tests for kubevirt network manager.
 */
public class KubevirtNetworkManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String UNKNOWN_ID = "unknown_id";
    private static final String UPDATED_NAME = "updated_name";

    private static final String NETWORK_ID = "network_id";
    private static final String NETWORK_NAME = "network_name";

    private static final KubevirtNetwork.Type TYPE = VXLAN;
    private static final Integer MTU = 1500;
    private static final String SEGMENT_ID = "1";
    private static final IpAddress GATEWAY_IP = IpAddress.valueOf("10.10.10.1");
    private static final boolean DEFAULT_ROUTE = true;
    private static final String CIDR = "10.10.10.0/24";
    private static final IpAddress IP_POOL_START = IpAddress.valueOf("10.10.10.100");
    private static final IpAddress IP_POOL_END = IpAddress.valueOf("10.10.10.200");

    private static final KubevirtNetwork NETWORK = DefaultKubevirtNetwork.builder()
            .networkId(NETWORK_ID)
            .name(NETWORK_NAME)
            .type(TYPE)
            .mtu(MTU)
            .segmentId(SEGMENT_ID)
            .gatewayIp(GATEWAY_IP)
            .defaultRoute(DEFAULT_ROUTE)
            .cidr(CIDR)
            .ipPool(new KubevirtIpPool(IP_POOL_START, IP_POOL_END))
            .hostRoutes(ImmutableSet.of())
            .build();

    private static final KubevirtNetwork NETWORK_UPDATED = DefaultKubevirtNetwork.builder()
            .networkId(NETWORK_ID)
            .name(UPDATED_NAME)
            .type(TYPE)
            .mtu(MTU)
            .segmentId(SEGMENT_ID)
            .gatewayIp(GATEWAY_IP)
            .defaultRoute(DEFAULT_ROUTE)
            .cidr(CIDR)
            .ipPool(new KubevirtIpPool(IP_POOL_START, IP_POOL_END))
            .hostRoutes(ImmutableSet.of())
            .build();

    private final TestKubevirtnetworkListener testListener = new TestKubevirtnetworkListener();

    private KubevirtNetworkManager target;
    private DistributedKubevirtNetworkStore networkStore;

    @Before
    public void setUp() throws Exception {
        networkStore = new DistributedKubevirtNetworkStore();
        TestUtils.setField(networkStore, "coreService", new TestCoreService());
        TestUtils.setField(networkStore, "storageService", new TestStorageService());
        TestUtils.setField(networkStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        networkStore.activate();

        target = new KubevirtNetworkManager();
        TestUtils.setField(target, "coreService", new TestCoreService());
        target.networkStore = networkStore;
        target.addListener(testListener);
        target.activate();
    }

    @After
    public void tearDown() {
        target.removeListener(testListener);
        networkStore.deactivate();
        target.deactivate();
        networkStore = null;
        target = null;
    }

    /**
     * Tests if getting all networks returns the correct set of networks.
     */
    @Test
    public void testGetNetworks() {
        createBasicNetworks();
        assertEquals("Number of network did not match", 1, target.networks().size());
    }

    /**
     * Tests if getting a network with ID returns the correct network.
     */
    @Test
    public void testGetNetworkById() {
        createBasicNetworks();
        assertNotNull("Network did not match", target.network(NETWORK_ID));
        assertNull("Network did not match", target.network(UNKNOWN_ID));
    }

    /**
     * Tests creating and removing a network, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndRemoveNetwork() {
        target.createNetwork(NETWORK);
        assertEquals("Number of networks did not match", 1, target.networks().size());
        assertNotNull("Network was not created", target.network(NETWORK_ID));

        target.removeNetwork(NETWORK_ID);
        assertEquals("Number of networks did not match", 0, target.networks().size());
        assertNull("Network was not removed", target.network(NETWORK_ID));

        validateEvents(KUBEVIRT_NETWORK_CREATED, KUBEVIRT_NETWORK_REMOVED);
    }

    /**
     * Tests updating a network, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndUpdateNetwork() {
        target.createNetwork(NETWORK);
        assertEquals("Number of networks did not match", 1, target.networks().size());
        assertEquals("Network did not match", NETWORK_NAME, target.network(NETWORK_ID).name());

        target.updateNetwork(NETWORK_UPDATED);

        assertEquals("Number of networks did not match", 1, target.networks().size());
        assertEquals("Network did not match", UPDATED_NAME, target.network(NETWORK_ID).name());
        validateEvents(KUBEVIRT_NETWORK_CREATED, KUBEVIRT_NETWORK_UPDATED);
    }

    /**
     * Tests if creating a null network fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullNetwork() {
        target.createNetwork(null);
    }

    /**
     * Tests if creating a duplicate network fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateDuplicateNetwork() {
        target.createNetwork(NETWORK);
        target.createNetwork(NETWORK);
    }

    /**
     * Tests if removing network with null ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRemoveNetworkWithNull() {
        target.removeNetwork(null);
    }

    /**
     * Tests if updating an unregistered network fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateUnregisteredNetwork() {
        target.updateNetwork(NETWORK);
    }

    private void createBasicNetworks() {
        target.createNetwork(NETWORK);
    }

    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private static class TestKubevirtnetworkListener implements KubevirtNetworkListener {
        private List<KubevirtNetworkEvent> events = Lists.newArrayList();

        @Override
        public void event(KubevirtNetworkEvent event) {
            events.add(event);
        }
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
}
