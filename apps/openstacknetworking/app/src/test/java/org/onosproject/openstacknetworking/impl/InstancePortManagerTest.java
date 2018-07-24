/*
 * Copyright 2018-present Open Networking Foundation
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.LeadershipServiceAdapter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.Event;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortEvent;
import org.onosproject.openstacknetworking.api.InstancePortListener;
import org.onosproject.store.service.TestStorageService;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onosproject.openstacknetworking.api.Constants.ANNOTATION_CREATE_TIME;
import static org.onosproject.openstacknetworking.api.InstancePort.State.ACTIVE;
import static org.onosproject.openstacknetworking.api.InstancePort.State.INACTIVE;
import static org.onosproject.openstacknetworking.api.InstancePort.State.MIGRATED;
import static org.onosproject.openstacknetworking.api.InstancePort.State.MIGRATING;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_MIGRATION_ENDED;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_MIGRATION_STARTED;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_PORT_DETECTED;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_PORT_UPDATED;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_PORT_VANISHED;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_RESTARTED;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_TERMINATED;

/**
 * Unit tests for instance port manager.
 */
public class InstancePortManagerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");

    private static final String ANNOTATION_NETWORK_ID = "networkId";
    private static final String ANNOTATION_PORT_ID = "portId";

    private static final IpAddress IP_ADDRESS_1 = IpAddress.valueOf("1.2.3.4");
    private static final IpAddress IP_ADDRESS_2 = IpAddress.valueOf("5.6.7.8");

    private static final MacAddress MAC_ADDRESS_1 = MacAddress.valueOf("11:22:33:44:55:66");
    private static final MacAddress MAC_ADDRESS_2 = MacAddress.valueOf("77:88:99:AA:BB:CC");

    private static final DeviceId DEV_ID_1 = DeviceId.deviceId("of:0000000000000001");
    private static final DeviceId DEV_ID_2 = DeviceId.deviceId("of:0000000000000002");
    private static final PortNumber PORT_NUM_1 = PortNumber.portNumber(1L);
    private static final PortNumber PORT_NUM_2 = PortNumber.portNumber(2L);

    private static final VlanId VLAN_ID = VlanId.vlanId();
    private static final ProviderId PROVIDER_ID = ProviderId.NONE;
    private static final HostId HOST_ID_1 = HostId.hostId("00:00:11:00:00:01/1");
    private static final HostId HOST_ID_2 = HostId.hostId("00:00:11:00:00:02/1");

    private static final String NETWORK_ID_1 = "net-id-1";
    private static final String NETWORK_ID_2 = "net-id-2";
    private static final String PORT_ID_1 = "port-id-1";
    private static final String PORT_ID_2 = "port-id-2";
    private static final String UNKNOWN_ID = "port-id-x";

    private static final long TIME_1 = 1L;
    private static final long TIME_2 = 2L;

    private InstancePort instancePort1;
    private InstancePort instancePort2;

    private InstancePortManager target;
    private DistributedInstancePortStore store;

    private final TestInstancePortListener testInstancePortListener = new TestInstancePortListener();

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {

        store = new DistributedInstancePortStore();
        TestUtils.setField(store, "coreService", new TestCoreService());
        TestUtils.setField(store, "storageService", new TestStorageService());
        TestUtils.setField(store, "eventExecutor", MoreExecutors.newDirectExecutorService());
        store.activate();

        target = new InstancePortManager();
        TestUtils.setField(target, "coreService", new TestCoreService());
        TestUtils.setField(target, "hostService", new TestHostService());
        TestUtils.setField(target, "leadershipService", new TestLeadershipService());
        TestUtils.setField(target, "clusterService", new TestClusterService());
        target.instancePortStore = store;
        target.addListener(testInstancePortListener);
        target.activate();

        HostLocation location1 = new HostLocation(DEV_ID_1, PORT_NUM_1, TIME_1);
        HostLocation location2 = new HostLocation(DEV_ID_2, PORT_NUM_2, TIME_2);

        DefaultAnnotations.Builder annotations1 = DefaultAnnotations.builder()
                .set(ANNOTATION_NETWORK_ID, NETWORK_ID_1)
                .set(ANNOTATION_PORT_ID, PORT_ID_1)
                .set(ANNOTATION_CREATE_TIME, String.valueOf(TIME_1));

        DefaultAnnotations.Builder annotations2 = DefaultAnnotations.builder()
                .set(ANNOTATION_NETWORK_ID, NETWORK_ID_2)
                .set(ANNOTATION_PORT_ID, PORT_ID_2)
                .set(ANNOTATION_CREATE_TIME, String.valueOf(TIME_2));

        Host host1 = new DefaultHost(PROVIDER_ID, HOST_ID_1, MAC_ADDRESS_1,
                VLAN_ID, location1, ImmutableSet.of(IP_ADDRESS_1),
                annotations1.build());
        Host host2 = new DefaultHost(PROVIDER_ID, HOST_ID_2, MAC_ADDRESS_2,
                VLAN_ID, location2, ImmutableSet.of(IP_ADDRESS_2),
                annotations2.build());

        instancePort1 = DefaultInstancePort.from(host1, ACTIVE);
        instancePort2 = DefaultInstancePort.from(host2, INACTIVE);
    }

    /**
     * Tears down all of this unit test.
     */
    @After
    public void tearDown() {
        target.removeListener(testInstancePortListener);
        store.deactivate();
        target.deactivate();
        store = null;
        target = null;
    }

    /**
     * Tests if getting all instance ports returns the correct set of ports.
     */
    @Test
    public void testGetInstancePorts() {
        createBasicInstancePorts();
        assertEquals("Number of instance port did not match",
                2, target.instancePorts().size());
    }

    /**
     * Tests if getting an instance port with port ID returns the correct port.
     */
    @Test
    public void testGetInstancePortById() {
        createBasicInstancePorts();
        assertNotNull("Instance port did not match", target.instancePort(PORT_ID_1));
        assertNotNull("Instance port did not match", target.instancePort(PORT_ID_2));
        assertNull("Instance port did not match", target.instancePort(UNKNOWN_ID));
    }


    /**
     * Tests if getting an instance port with IP and network ID returns correct port.
     */
    @Test
    public void testGetInstancePortByIpAndNetId() {
        createBasicInstancePorts();
        InstancePort port = target.instancePort(IP_ADDRESS_1, NETWORK_ID_1);
        assertEquals("Instance port did not match", port, instancePort1);
    }

    /**
     * Tests if getting an instance port with MAC returns correct port.
     */
    @Test
    public void testGetInstancePortByMac() {
        createBasicInstancePorts();
        InstancePort port = target.instancePort(MAC_ADDRESS_1);
        assertEquals("Instance port did not match", port, instancePort1);
    }

    /**
     * Tests if getting instance ports with network ID returns correct ports.
     */
    @Test
    public void testGetInstancePortsByNetId() {
        createBasicInstancePorts();
        Set<InstancePort> ports = target.instancePorts(NETWORK_ID_1);
        assertEquals("Number of instance port did not match",
                1, ports.size());
    }

    /**
     * Tests creating and removing an instance port, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndRemoveInstancePort() {
        target.createInstancePort(instancePort1);
        assertEquals("Number of instance port did not match",
                1, target.instancePorts().size());
        assertNotNull("Instance port did not match", target.instancePort(PORT_ID_1));

        target.removeInstancePort(PORT_ID_1);
        assertEquals("Number of instance port did not match",
                0, target.instancePorts().size());
        assertNull("Instance port did not match", target.instancePort(PORT_ID_1));

        validateEvents(OPENSTACK_INSTANCE_PORT_DETECTED, OPENSTACK_INSTANCE_PORT_VANISHED);
    }

    /**
     * Tests updating an instance port, and checks if it triggers proper events.
     */
    @Test
    public void testCreateAndUpdateInstancePort() {
        target.createInstancePort(instancePort1);
        assertEquals("Number of instance port did not match",
                1, target.instancePorts().size());
        assertEquals("Instance port did not match", PORT_NUM_1,
                target.instancePort(PORT_ID_1).portNumber());

        HostLocation location = new HostLocation(DEV_ID_2, PORT_NUM_2, TIME_2);

        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                .set(ANNOTATION_NETWORK_ID, NETWORK_ID_2)
                .set(ANNOTATION_PORT_ID, PORT_ID_1)
                .set(ANNOTATION_CREATE_TIME, String.valueOf(TIME_2));

        Host host = new DefaultHost(PROVIDER_ID, HOST_ID_2, MAC_ADDRESS_2,
                VLAN_ID, location, ImmutableSet.of(IP_ADDRESS_2),
                annotations.build());

        final InstancePort updated = DefaultInstancePort.from(host, ACTIVE);
        target.updateInstancePort(updated);

        assertEquals("Number of instance port did not match",
                1, target.instancePorts().size());
        assertEquals("Instance port did not match", PORT_NUM_2,
                target.instancePort(PORT_ID_1).portNumber());

        validateEvents(OPENSTACK_INSTANCE_PORT_DETECTED, OPENSTACK_INSTANCE_PORT_UPDATED);
    }

    /**
     * Tests if creating a null instance port fails with an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateNullInstancePort() {
        target.createInstancePort(null);
    }

    /**
     * Tests if creating a duplicated instance port fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateDuplicateInstancePort() {
        target.createInstancePort(instancePort1);
        target.createInstancePort(instancePort1);
    }

    /**
     * Tests if removing instance port with null ID fails with an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRemoveInstancePortWithNull() {
        target.removeInstancePort(null);
    }

    /**
     * Tests if updating an unregistered instance port fails with an exception.
     */
    @Test
    public void testUpdateUnregisteredInstancePort() {
        target.updateInstancePort(instancePort1);
    }

    /**
     * Tests if it triggers the instance termination event.
     */
    @Test
    public void testTerminateInstance() {
        InstancePort instancePort = instancePort1;
        target.createInstancePort(instancePort);

        InstancePort inactiveInstancePort = instancePort.updateState(INACTIVE);
        target.updateInstancePort(inactiveInstancePort);

        assertEquals("Number of instance port did not match",
                1, target.instancePorts().size());
        assertNotNull("Instance port did not match", target.instancePort(PORT_ID_1));

        validateEvents(OPENSTACK_INSTANCE_PORT_DETECTED, OPENSTACK_INSTANCE_TERMINATED);
    }

    /**
     * Tests if it triggers the instance restart event.
     */
    @Test
    public void testRestartInstance() {
        InstancePort instancePort = instancePort1;
        InstancePort inactiveInstancePort = instancePort.updateState(INACTIVE);

        target.createInstancePort(inactiveInstancePort);
        target.updateInstancePort(instancePort);

        assertEquals("Number of instance port did not match",
                1, target.instancePorts().size());
        assertNotNull("Instance port did not match", target.instancePort(PORT_ID_1));

        validateEvents(OPENSTACK_INSTANCE_PORT_DETECTED, OPENSTACK_INSTANCE_RESTARTED);
    }

    /**
     * Tests if it triggers the instance migration start event.
     */
    @Test
    public void testMigrateInstanceStart() {
        InstancePort instancePort = instancePort1;
        target.createInstancePort(instancePort);

        InstancePort migratingPort = instancePort.updateState(MIGRATING);
        target.updateInstancePort(migratingPort);

        assertEquals("Number of instance port did not match",
                1, target.instancePorts().size());
        assertNotNull("Instance port did not match", target.instancePort(PORT_ID_1));

        validateEvents(OPENSTACK_INSTANCE_PORT_DETECTED, OPENSTACK_INSTANCE_MIGRATION_STARTED);
    }

    /**
     * Tests if it triggers the instance migration end event.
     */
    @Test
    public void testMigrateInstanceEnd() {
        InstancePort instancePort = instancePort1;
        InstancePort migratingPort = instancePort.updateState(MIGRATING);
        target.createInstancePort(migratingPort);

        InstancePort migratedPort = instancePort.updateState(MIGRATED);
        target.updateInstancePort(migratedPort);

        assertEquals("Number of instance port did not match",
                1, target.instancePorts().size());
        assertNotNull("Instance port did not match", target.instancePort(PORT_ID_1));

        validateEvents(OPENSTACK_INSTANCE_PORT_DETECTED, OPENSTACK_INSTANCE_MIGRATION_ENDED);
    }

    /**
     * Tests if it triggers the instance removal event from termination status.
     */
    @Test
    public void testRemoveInstanceFromTermination() {
        InstancePort instancePort = instancePort1;
        target.createInstancePort(instancePort);

        InstancePort inactiveInstancePort = instancePort.updateState(INACTIVE);
        target.updateInstancePort(inactiveInstancePort);

        assertEquals("Number of instance port did not match",
                1, target.instancePorts().size());
        assertNotNull("Instance port did not match", target.instancePort(PORT_ID_1));

        target.removeInstancePort(PORT_ID_1);

        assertEquals("Number of instance port did not match",
                0, target.instancePorts().size());
        assertNull("Instance port did not match", target.instancePort(PORT_ID_1));

        validateEvents(OPENSTACK_INSTANCE_PORT_DETECTED, OPENSTACK_INSTANCE_TERMINATED,
                OPENSTACK_INSTANCE_PORT_VANISHED);
    }

    /**
     * Tests if it triggers the instance removal event from migration status.
     */
    @Test
    public void testRemoveInstanceFromMigration() {
        InstancePort instancePort = instancePort1;
        target.createInstancePort(instancePort);

        InstancePort inactiveInstancePort = instancePort.updateState(MIGRATING);
        target.updateInstancePort(inactiveInstancePort);

        assertEquals("Number of instance port did not match",
                1, target.instancePorts().size());
        assertNotNull("Instance port did not match", target.instancePort(PORT_ID_1));

        target.removeInstancePort(PORT_ID_1);

        assertEquals("Number of instance port did not match",
                0, target.instancePorts().size());
        assertNull("Instance port did not match", target.instancePort(PORT_ID_1));

        validateEvents(OPENSTACK_INSTANCE_PORT_DETECTED, OPENSTACK_INSTANCE_MIGRATION_STARTED,
                OPENSTACK_INSTANCE_PORT_VANISHED);
    }

    private void createBasicInstancePorts() {
        target.createInstancePort(instancePort1);
        target.createInstancePort(instancePort2);
    }

    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return TEST_APP_ID;
        }
    }

    private static class TestLeadershipService extends LeadershipServiceAdapter {
    }

    private static class TestClusterService extends ClusterServiceAdapter {
    }

    private static class TestHostService extends HostServiceAdapter {
    }

    private static class TestInstancePortListener implements InstancePortListener {
        private List<InstancePortEvent> events = Lists.newArrayList();

        @Override
        public void event(InstancePortEvent event) {
            events.add(event);
        }
    }

    private void validateEvents(Enum... types) {
        int i = 0;
        assertEquals("Number of events did not match", types.length,
                                        testInstancePortListener.events.size());
        for (Event event : testInstancePortListener.events) {
            assertEquals("Incorrect event received", types[i], event.type());
            i++;
        }
        testInstancePortListener.events.clear();
    }
}
