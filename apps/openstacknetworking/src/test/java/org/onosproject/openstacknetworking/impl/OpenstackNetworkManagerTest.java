/*
 * Copyright 2017-present Open Networking Laboratory
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
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.Event;
import org.onosproject.openstacknetworking.api.OpenstackNetworkEvent;
import org.onosproject.openstacknetworking.api.OpenstackNetworkListener;
import org.onosproject.store.service.TestStorageService;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.openstack.networking.domain.NeutronNetwork;
import org.openstack4j.openstack.networking.domain.NeutronPort;
import org.openstack4j.openstack.networking.domain.NeutronSubnet;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.*;

/**
 * Unit tests for OpenStack network manager.
 */
public class OpenstackNetworkManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String UNKNOWN_ID = "unknown_id";
    private static final String UPDATED_NAME = "updated_name";

    private static final String NETWORK_ID = "network_id";
    private static final String NETWORK_NAME = "network_name";
    private static final Network NETWORK = NeutronNetwork.builder()
            .name(NETWORK_NAME)
            .build();
    private static final Network NETWORK_COPY = NeutronNetwork.builder()
            .name("network")
            .build();

    private static final String SUBNET_ID = "subnet_id";
    private static final Subnet SUBNET = NeutronSubnet.builder()
            .networkId(NETWORK_ID)
            .cidr("192.168.0.0/24")
            .build();
    private static final Subnet SUBNET_COPY = NeutronSubnet.builder()
            .networkId(NETWORK_ID)
            .cidr("192.168.0.0/24")
            .build();

    private static final String PORT_ID = "port_id";
    private static final Port PORT = NeutronPort.builder()
            .networkId(NETWORK_ID)
            .fixedIp("192.168.0.1", SUBNET_ID)
            .build();
    private static final Port PORT_COPY = NeutronPort.builder()
            .networkId(NETWORK_ID)
            .fixedIp("192.168.0.1", SUBNET_ID)
            .build();

    private final TestOpenstackNetworkListener testListener = new TestOpenstackNetworkListener();

    private OpenstackNetworkManager target;
    private DistributedOpenstackNetworkStore osNetworkStore;

    @Before
    public void setUp() throws Exception {
        NETWORK.setId(NETWORK_ID);
        NETWORK_COPY.setId(NETWORK_ID);
        SUBNET.setId(SUBNET_ID);
        SUBNET_COPY.setId(SUBNET_ID);
        PORT.setId(PORT_ID);
        PORT_COPY.setId(PORT_ID);

        osNetworkStore = new DistributedOpenstackNetworkStore();
        TestUtils.setField(osNetworkStore, "coreService", new TestCoreService());
        TestUtils.setField(osNetworkStore, "storageService", new TestStorageService());
        TestUtils.setField(osNetworkStore, "eventExecutor", MoreExecutors.newDirectExecutorService());
        osNetworkStore.activate();

        target = new OpenstackNetworkManager();
        target.coreService = new TestCoreService();
        target.osNetworkStore = osNetworkStore;
        target.addListener(testListener);
        target.activate();
    }

    @After
    public void tearDown() {
        target.removeListener(testListener);
        osNetworkStore.deactivate();
        target.deactivate();
        osNetworkStore = null;
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
        assertTrue("Network did not match", target.network(NETWORK_ID) != null);
        assertTrue("Network did not match", target.network(UNKNOWN_ID) == null);
    }

    /**
     * Tests creating and removing a network, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndRemoveNetwork() {
        target.createNetwork(NETWORK);
        assertEquals("Number of networks did not match", 1, target.networks().size());
        assertTrue("Network was not created", target.network(NETWORK_ID) != null);

        target.removeNetwork(NETWORK_ID);
        assertEquals("Number of networks did not match", 0, target.networks().size());
        assertTrue("Network was not removed", target.network(NETWORK_ID) == null);

        validateEvents(OPENSTACK_NETWORK_CREATED, OPENSTACK_NETWORK_REMOVED);
    }

    /**
     * Tests updating a network, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndUpdateNetwork() {
        target.createNetwork(NETWORK);
        assertEquals("Number of networks did not match", 1, target.networks().size());
        assertEquals("Network did not match", NETWORK_NAME, target.network(NETWORK_ID).getName());

        final Network updated = NeutronNetwork.builder()
                .from(NETWORK_COPY)
                .name(UPDATED_NAME)
                .build();
        target.updateNetwork(updated);

        assertEquals("Number of networks did not match", 1, target.networks().size());
        assertEquals("Network did not match", UPDATED_NAME, target.network(NETWORK_ID).getName());
        validateEvents(OPENSTACK_NETWORK_CREATED, OPENSTACK_NETWORK_UPDATED);
    }

    /**
     * Tests if creating a null network fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullNetwork() {
        target.createNetwork(null);
    }

    /**
     * Tests if creating a network with null ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateNetworkWithNullId() {
        final Network testNet = NeutronNetwork.builder().build();
        target.createNetwork(testNet);
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
     * Tests if updating a network with null name fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateNetworkWithNullName() {
        final Network updated = NeutronNetwork.builder()
                .name(null)
                .build();
        updated.setId(NETWORK_ID);
        target.updateNetwork(updated);
    }

    /**
     * Tests if updating an unregistered network fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateUnregisteredNetwork() {
        target.updateNetwork(NETWORK);
    }

    /**
     * Tests if updating a network with null ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateNetworkWithNullId() {
        final Network testNet = NeutronNetwork.builder().build();
        target.updateNetwork(testNet);
    }


    /**
     * Tests if getting all subnets returns the correct set of subnets.
     */
    @Test
    public void testGetSubnets() {
        createBasicNetworks();
        assertEquals("Number of subnet did not match", 1, target.subnets().size());
    }

    @Test
    public void testGetSubnetsByNetworkId() {
        createBasicNetworks();
        assertEquals("Subnet did not match", 1, target.subnets(NETWORK_ID).size());
        assertEquals("Subnet did not match", 0, target.subnets(UNKNOWN_ID).size());
    }

    /**
     * Tests if getting a subnet with ID returns the correct subnet.
     */
    @Test
    public void testGetSubnetById() {
        createBasicNetworks();
        assertTrue("Subnet did not match", target.subnet(SUBNET_ID) != null);
        assertTrue("Subnet did not match", target.subnet(UNKNOWN_ID) == null);
    }

    /**
     * Tests creating and removing a subnet, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndRemoveSubnet() {
        target.createSubnet(SUBNET);
        assertEquals("Number of subnet did not match", 1, target.subnets().size());
        assertTrue("Subnet was not created", target.subnet(SUBNET_ID) != null);

        target.removeSubnet(SUBNET_ID);
        assertEquals("Number of subnet did not match", 0, target.subnets().size());
        assertTrue("Subnet was not removed", target.subnet(SUBNET_ID) == null);

        validateEvents(OPENSTACK_SUBNET_CREATED, OPENSTACK_SUBNET_REMOVED);
    }

    /**
     * Tests updating a subnet, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndUpdateSubnet() {
        target.createSubnet(SUBNET_COPY);
        assertEquals("Number of subnet did not match", 1, target.subnets().size());
        assertEquals("Subnet did not match", null, target.subnet(SUBNET_ID).getName());

        // TODO fix NeutronSubnet.builder().from() in openstack4j
        final Subnet updated = NeutronSubnet.builder()
                .networkId(NETWORK_ID)
                .cidr("192.168.0.0/24")
                .name(UPDATED_NAME)
                .build();
        updated.setId(SUBNET_ID);
        target.updateSubnet(updated);

        assertEquals("Number of subnet did not match", 1, target.subnets().size());
        assertEquals("Subnet did not match", UPDATED_NAME, target.subnet(SUBNET_ID).getName());

        validateEvents(OPENSTACK_SUBNET_CREATED, OPENSTACK_SUBNET_UPDATED);
    }

    /**
     * Tests if creating a null subnet fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullSubnet() {
        target.createSubnet(null);
    }

    /**
     * Tests if creating a subnet with null ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateSubnetWithNullId() {
        final Subnet testSubnet = NeutronSubnet.builder()
                .networkId(NETWORK_ID)
                .cidr("192.168.0.0/24")
                .build();
        target.createSubnet(testSubnet);
    }

    /**
     * Tests if creating subnet with null network ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateSubnetWithNullNetworkId() {
        final Subnet testSubnet = NeutronSubnet.builder()
                .cidr("192.168.0.0/24")
                .build();
        testSubnet.setId(SUBNET_ID);
        target.createSubnet(testSubnet);
    }

    /**
     * Tests if creating a subnet with null CIDR fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateSubnetWithNullCidr() {
        final Subnet testSubnet = NeutronSubnet.builder()
                .networkId(NETWORK_ID)
                .build();
        testSubnet.setId(SUBNET_ID);
        target.createSubnet(testSubnet);
    }

    /**
     * Tests if creating a duplicate subnet fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateDuplicateSubnet() {
        target.createSubnet(SUBNET);
        target.createSubnet(SUBNET);
    }

    /**
     * Tests if updating an unregistered subnet fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateUnregisteredSubnet() {
        target.updateSubnet(SUBNET);
    }

    /**
     * Tests if updating a null subnet fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testUpdateSubnetWithNull() {
        target.updateSubnet(null);
    }

    /**
     * Tests if updating a subnet with null ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateSubnetWithNullId() {
        final Subnet testSubnet = NeutronSubnet.builder()
                .networkId(NETWORK_ID)
                .cidr("192.168.0.0/24")
                .build();
        target.updateSubnet(testSubnet);
    }

    /**
     * Tests if updating a subnet with null network ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateSubnetWithNullNetworkId() {
        final Subnet testSubnet = NeutronSubnet.builder()
                .cidr("192.168.0.0/24")
                .build();
        testSubnet.setId(SUBNET_ID);
        target.updateSubnet(testSubnet);
    }

    /**
     * Tests if updating a subnet with null CIDR fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateSubnetWithNullCidr() {
        final Subnet testSubnet = NeutronSubnet.builder()
                .networkId(NETWORK_ID)
                .build();
        testSubnet.setId(SUBNET_ID);
        target.updateSubnet(testSubnet);
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
        assertTrue("Port did not match", target.port(PORT_ID) != null);
        assertTrue("Port did not match", target.port(UNKNOWN_ID) == null);
    }

    /**
     * Tests creating and removing a port, and checks if proper event is triggered.
     */
    @Test
    public void testCreateAndRemovePort() {
        target.createPort(PORT);
        assertEquals("Number of port did not match", 1, target.ports().size());
        assertTrue("Port was not created", target.port(PORT_ID) != null);

        target.removePort(PORT_ID);
        assertEquals("Number of port did not match", 0, target.ports().size());
        assertTrue("Port was not created", target.port(PORT_ID) == null);

        validateEvents(OPENSTACK_PORT_CREATED, OPENSTACK_PORT_REMOVED);
    }

    /**
     * Tests creating and updating a port, and checks if proper event is triggered.
     */
    @Test
    public void testCreateAndUpdatePort() {
        target.createPort(PORT);
        assertEquals("Number of port did not match", 1, target.ports().size());
        assertEquals("Port did not match", null, target.port(PORT_ID).getName());

        final Port updated = PORT_COPY.toBuilder()
                .name(UPDATED_NAME)
                .build();
        target.updatePort(updated);

        assertEquals("Number of port did not match", 1, target.ports().size());
        assertEquals("Port did not match", UPDATED_NAME, target.port(PORT_ID).getName());

        validateEvents(OPENSTACK_PORT_CREATED, OPENSTACK_PORT_UPDATED);
    }

    /**
     * Tests if creating a null port fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullPort() {
        target.createPort(null);
    }

    /**
     * Tests if creating a port with null ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreatePortWithNullId() {
        final Port testPort = NeutronPort.builder()
                .networkId(NETWORK_ID)
                .build();
        target.createPort(testPort);
    }

    /**
     * Tests if creating a port with null network ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreatePortWithNullNetworkId() {
        final Port testPort = NeutronPort.builder().build();
        testPort.setId(PORT_ID);
        target.createPort(testPort);
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

    /**
     * Tests if updating a port with null ID fails with exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdatePortWithNullId() {
        final Port testPort = NeutronPort.builder()
                .networkId(NETWORK_ID)
                .build();
        target.updatePort(testPort);
    }

    /**
     * Tests if updating a port with null network ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdatePortWithNullNetworkId() {
        final Port testPort = NeutronPort.builder().build();
        testPort.setId(PORT_ID);
        target.updatePort(testPort);
    }

    private void createBasicNetworks() {
        target.createNetwork(NETWORK);
        target.createSubnet(SUBNET);
        target.createPort(PORT);
    }

    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private static class TestOpenstackNetworkListener implements OpenstackNetworkListener {
        private List<OpenstackNetworkEvent> events = Lists.newArrayList();

        @Override
        public void event(OpenstackNetworkEvent event) {
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