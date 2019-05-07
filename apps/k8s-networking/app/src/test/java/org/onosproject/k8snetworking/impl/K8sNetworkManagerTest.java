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
package org.onosproject.k8snetworking.impl;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.Event;
import org.onosproject.k8snetworking.api.DefaultK8sNetwork;
import org.onosproject.k8snetworking.api.DefaultK8sPort;
import org.onosproject.k8snetworking.api.K8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetworkEvent;
import org.onosproject.k8snetworking.api.K8sNetworkListener;
import org.onosproject.k8snetworking.api.K8sPort;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.store.service.TestStorageService;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onosproject.k8snetworking.api.K8sNetworkEvent.Type.K8S_NETWORK_CREATED;
import static org.onosproject.k8snetworking.api.K8sNetworkEvent.Type.K8S_NETWORK_REMOVED;
import static org.onosproject.k8snetworking.api.K8sNetworkEvent.Type.K8S_NETWORK_UPDATED;
import static org.onosproject.k8snetworking.api.K8sNetworkEvent.Type.K8S_PORT_CREATED;
import static org.onosproject.k8snetworking.api.K8sNetworkEvent.Type.K8S_PORT_REMOVED;
import static org.onosproject.k8snetworking.api.K8sNetworkEvent.Type.K8S_PORT_UPDATED;

/**
 * Unit tests for kubernetes network manager.
 */
public class K8sNetworkManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String UNKNOWN_ID = "unknown_id";
    private static final String UPDATED_ID = "updated_id";
    private static final String UPDATED_NAME = "updated_name";

    private static final String NETWORK_ID = "network_id";
    private static final String NETWORK_NAME = "network_name";
    private static final K8sNetwork NETWORK = DefaultK8sNetwork.builder()
            .networkId(NETWORK_ID)
            .name(NETWORK_NAME)
            .type(K8sNetwork.Type.VXLAN)
            .segmentId("1")
            .cidr("10.10.0.0/24")
            .mtu(1500)
            .build();
    private static final K8sNetwork NETWORK_UPDATED = DefaultK8sNetwork.builder()
            .networkId(NETWORK_ID)
            .name(UPDATED_NAME)
            .type(K8sNetwork.Type.VXLAN)
            .segmentId("1")
            .cidr("10.10.0.0/24")
            .mtu(1500)
            .build();

    private static final String PORT_ID = "port_id";
    private static final K8sPort PORT = DefaultK8sPort.builder()
            .portId(PORT_ID)
            .networkId(NETWORK_ID)
            .deviceId(DeviceId.deviceId("dev-1"))
            .ipAddress(IpAddress.valueOf("20.20.20.20"))
            .macAddress(MacAddress.valueOf("00:11:22:33:44:55"))
            .portNumber(PortNumber.portNumber("1"))
            .state(K8sPort.State.ACTIVE)
            .build();
    private static final K8sPort PORT_UPDATED = DefaultK8sPort.builder()
            .portId(PORT_ID)
            .networkId(UPDATED_ID)
            .deviceId(DeviceId.deviceId("dev-1"))
            .ipAddress(IpAddress.valueOf("20.20.20.20"))
            .macAddress(MacAddress.valueOf("00:11:22:33:44:55"))
            .portNumber(PortNumber.portNumber("1"))
            .state(K8sPort.State.ACTIVE)
            .build();

    private final TestK8sNetworkListener testListener = new TestK8sNetworkListener();

    private K8sNetworkManager target;
    private DistributedK8sNetworkStore k8sNetworkStore;

    @Before
    public void setUp() throws Exception {
        k8sNetworkStore = new DistributedK8sNetworkStore();
        TestUtils.setField(k8sNetworkStore, "coreService", new TestCoreService());
        TestUtils.setField(k8sNetworkStore, "storageService", new TestStorageService());
        TestUtils.setField(k8sNetworkStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        k8sNetworkStore.activate();

        target = new K8sNetworkManager();
        TestUtils.setField(target, "coreService", new TestCoreService());
        target.k8sNetworkStore = k8sNetworkStore;
        target.addListener(testListener);
        target.activate();
    }

    @After
    public void tearDown() {
        target.removeListener(testListener);
        k8sNetworkStore.deactivate();
        target.deactivate();
        k8sNetworkStore = null;
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

        validateEvents(K8S_NETWORK_CREATED, K8S_NETWORK_REMOVED);
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
        validateEvents(K8S_NETWORK_CREATED, K8S_NETWORK_UPDATED);
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

    /**
     * Tests if getting all ports returns correct set of values.
     */
    @Test
    public void testGetPorts() {
        createBasicNetworks();
        assertEquals("Number of port did not match", 1, target.ports().size());
    }

    /**
     * Tests if getting a port with network ID returns correct set of values.
     */
    @Test
    public void testGetPortsByNetworkId() {
        createBasicNetworks();
        assertEquals("Number of port did not match", 1, target.ports(NETWORK_ID).size());
        assertEquals("Number of port did not match", 0, target.ports(UNKNOWN_ID).size());
    }

    /**
     * Tests if getting a port with ID returns correct value.
     */
    @Test
    public void testGetPortById() {
        createBasicNetworks();
        assertNotNull("Port did not match", target.port(PORT_ID));
        assertNull("Port did not match", target.port(UNKNOWN_ID));
    }

    /**
     * Tests creating and removing a port, and checks if proper event is triggered.
     */
    @Test
    public void testCreateAndRemovePort() {
        target.createPort(PORT);
        assertEquals("Number of port did not match", 1, target.ports().size());
        assertNotNull("Port was not created", target.port(PORT_ID));

        target.removePort(PORT_ID);
        assertEquals("Number of port did not match", 0, target.ports().size());
        assertNull("Port was not created", target.port(PORT_ID));

        validateEvents(K8S_PORT_CREATED, K8S_PORT_REMOVED);
    }

    /**
     * Tests creating and updating a port, and checks if proper event is triggered.
     */
    @Test
    public void testCreateAndUpdatePort() {
        target.createPort(PORT);
        assertEquals("Number of port did not match", 1, target.ports().size());

        target.updatePort(PORT_UPDATED);

        assertEquals("Number of port did not match", 1, target.ports().size());
        assertEquals("Port did not match", UPDATED_ID, target.port(PORT_ID).networkId());

        validateEvents(K8S_PORT_CREATED, K8S_PORT_UPDATED);
    }

    /**
     * Tests if creating a null port fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullPort() {
        target.createPort(null);
    }


    /**
     * Tests if creating a duplicate port fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createDuplicatePort() {
        target.createPort(PORT);
        target.createPort(PORT);
    }

    /**
     * Tests if updating an unregistered port fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateUnregisteredPort() {
        target.updatePort(PORT);
    }

    /**
     * Tests if updating a null port fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testUpdateNullPort() {
        target.updatePort(null);
    }


    private void createBasicNetworks() {
        target.createNetwork(NETWORK);
        target.createPort(PORT);
    }

    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private static class TestK8sNetworkListener implements K8sNetworkListener {
        private List<K8sNetworkEvent> events = Lists.newArrayList();

        @Override
        public void event(K8sNetworkEvent event) {
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
