/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.incubator.net.virtual.impl;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestTools;
import org.onlab.junit.TestUtils;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.IdGenerator;
import org.onosproject.event.Event;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.incubator.store.virtual.impl.DistributedVirtualNetworkStore;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.store.service.TestStorageService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Junit tests for VirtualNetworkDeviceService.
 */
public class VirtualNetworkDeviceManagerTest extends VirtualNetworkTestUtil {
    private final String tenantIdValue1 = "TENANT_ID1";

    private VirtualNetworkManager manager;
    private DistributedVirtualNetworkStore virtualNetworkManagerStore;
    private CoreService coreService;
    private TestServiceDirectory testDirectory;
    private TestListener testListener = new TestListener();
    private TestEventDispatcher dispatcher = new TestEventDispatcher();

    @Before
    public void setUp() throws Exception {
        virtualNetworkManagerStore = new DistributedVirtualNetworkStore();

        coreService = new VirtualNetworkDeviceManagerTest.TestCoreService();
        TestUtils.setField(virtualNetworkManagerStore, "coreService", coreService);
        TestUtils.setField(virtualNetworkManagerStore, "storageService", new TestStorageService());
        virtualNetworkManagerStore.activate();

        manager = new VirtualNetworkManager();
        manager.store = virtualNetworkManagerStore;
        manager.coreService = coreService;
        NetTestTools.injectEventDispatcher(manager, dispatcher);

        testDirectory = new TestServiceDirectory();
        TestUtils.setField(manager, "serviceDirectory", testDirectory);

        manager.activate();
    }

    @After
    public void tearDown() {
        virtualNetworkManagerStore.deactivate();
        manager.deactivate();
        NetTestTools.injectEventDispatcher(manager, null);
    }

    /**
     * Tests the getDevices(), getAvailableDevices(), getDeviceCount(), getDevice(), and isAvailable() methods.
     */
    @Test
    public void testGetDevices() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        VirtualDevice device1 = manager.createVirtualDevice(virtualNetwork.id(), DID1);
        VirtualDevice device2 = manager.createVirtualDevice(virtualNetwork.id(), DID2);

        DeviceService deviceService = manager.get(virtualNetwork.id(), DeviceService.class);

        // test the getDevices() method
        Iterator<Device> it = deviceService.getDevices().iterator();
        assertEquals("The device set size did not match.", 2, Iterators.size(it));

        // test the getAvailableDevices() method
        Iterator<Device> it2 = deviceService.getAvailableDevices().iterator();
        assertEquals("The device set size did not match.", 2, Iterators.size(it2));

        // test the getDeviceCount() method
        assertEquals("The device set size did not match.", 2, deviceService.getDeviceCount());

        // test the getDevice() method
        assertEquals("The expect device did not match.", device1,
                     deviceService.getDevice(DID1));
        assertNotEquals("The expect device should not have matched.", device1,
                        deviceService.getDevice(DID2));

        // test the isAvailable() method
        assertTrue("The expect device availability did not match.",
                   deviceService.isAvailable(DID1));
        assertFalse("The expect device availability did not match.",
                    deviceService.isAvailable(DID3));
    }

    /**
     * Tests querying for a device using a null device identifier.
     */
    @Test(expected = NullPointerException.class)
    public void testGetDeviceByNullId() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        DeviceService deviceService = manager.get(virtualNetwork.id(), DeviceService.class);

        // test the getDevice() method with null device id value.
        deviceService.getDevice(null);
    }

    /**
     * Tests querying for a device using a null device type.
     */
    @Test(expected = NullPointerException.class)
    public void testGetDeviceByNullType() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        DeviceService deviceService = manager.get(virtualNetwork.id(), DeviceService.class);

        // test the getDevices() method with null type value.
        deviceService.getDevices(null);
    }

    /**
     * Tests the isAvailable method using a null device identifier.
     */
    @Test(expected = NullPointerException.class)
    public void testIsAvailableByNullId() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        DeviceService deviceService = manager.get(virtualNetwork.id(), DeviceService.class);

        // test the isAvailable() method with null device id value.
        deviceService.isAvailable(null);
    }

    /**
     * Tests querying for a device and available devices by device type.
     */
    @Test
    public void testGetDeviceType() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        manager.createVirtualDevice(virtualNetwork.id(), DID1);
        manager.createVirtualDevice(virtualNetwork.id(), DID2);

        DeviceService deviceService = manager.get(virtualNetwork.id(), DeviceService.class);

        // test the getDevices(Type) method.
        Iterator<Device> it = deviceService.getDevices(Device.Type.VIRTUAL).iterator();
        assertEquals("The device set size did not match.", 2, Iterators.size(it));
        Iterator<Device> it2 = deviceService.getDevices(Device.Type.SWITCH).iterator();
        assertEquals("The device set size did not match.", 0, Iterators.size(it2));

        // test the getAvailableDevices(Type) method.
        Iterator<Device> it3 = deviceService.getAvailableDevices(Device.Type.VIRTUAL).iterator();
        assertEquals("The device set size did not match.", 2, Iterators.size(it3));
    }

    /**
     * Tests querying the role of a device by null device identifier.
     */
    @Test(expected = NullPointerException.class)
    public void testGetRoleByNullId() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        DeviceService deviceService = manager.get(virtualNetwork.id(), DeviceService.class);

        // test the getRole() method using a null device identifier
        deviceService.getRole(null);
    }

    /**
     * Tests querying the role of a device by device identifier.
     */
    @Test
    public void testGetRole() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        DeviceService deviceService = manager.get(virtualNetwork.id(), DeviceService.class);

        // test the getRole() method
        assertEquals("The expect device role did not match.", MastershipRole.MASTER,
                     deviceService.getRole(DID1));
    }

    /**
     * Tests querying the ports of a device by null device identifier.
     */
    @Test(expected = NullPointerException.class)
    public void testGetPortsByNullId() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        DeviceService deviceService = manager.get(virtualNetwork.id(), DeviceService.class);

        // test the getPorts() method using a null device identifier
        deviceService.getPorts(null);
    }

    /**
     * Tests querying the ports of a device by device identifier.
     */
    @Test
    public void testGetPorts() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        VirtualDevice virtualDevice = manager.createVirtualDevice(virtualNetwork.id(), DID1);
        manager.createVirtualDevice(virtualNetwork.id(), DID2);

        DeviceService deviceService = manager.get(virtualNetwork.id(), DeviceService.class);

        ConnectPoint cp = new ConnectPoint(virtualDevice.id(), PortNumber.portNumber(1));

        manager.createVirtualPort(virtualNetwork.id(), virtualDevice.id(), PortNumber.portNumber(1), cp);
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice.id(), PortNumber.portNumber(2), cp);

        // test the getPorts() method
        assertEquals("The port set size did not match.", 2,
                     deviceService.getPorts(DID1).size());
        assertEquals("The port set size did not match.", 0,
                     deviceService.getPorts(DID2).size());
    }

    /**
     * Tests querying the port of a device by device identifier and port number.
     */
    @Test
    public void testGetPort() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        VirtualDevice virtualDevice = manager.createVirtualDevice(virtualNetwork.id(), DID1);
        manager.createVirtualDevice(virtualNetwork.id(), DID2);

        DeviceService deviceService = manager.get(virtualNetwork.id(), DeviceService.class);

        ConnectPoint cp = new ConnectPoint(virtualDevice.id(), PortNumber.portNumber(1));

        VirtualPort virtualPort1 = manager.createVirtualPort(virtualNetwork.id(), virtualDevice.id(),
                                                             PortNumber.portNumber(1), cp);
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice.id(), PortNumber.portNumber(2), cp);

        // test the getPort() method
        assertEquals("The port did not match as expected.", virtualPort1,
                     deviceService.getPort(DID1, PortNumber.portNumber(1)));
        assertNotEquals("The port did not match as expected.", virtualPort1,
                        deviceService.getPort(DID1, PortNumber.portNumber(3)));
    }

    /**
     * Tests querying the port statistics of a device by null device identifier.
     */
    @Test(expected = NullPointerException.class)
    public void testGetPortsStatisticsByNullId() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        DeviceService deviceService = manager.get(virtualNetwork.id(), DeviceService.class);

        // test the getPortStatistics() method using a null device identifier
        deviceService.getPortStatistics(null);
    }

    /**
     * Tests querying the port statistics of a device by device identifier.
     */
    @Test
    public void testGetPortStatistics() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        VirtualDevice virtualDevice = manager.createVirtualDevice(virtualNetwork.id(), DID1);
        manager.createVirtualDevice(virtualNetwork.id(), DID2);

        DeviceService deviceService = manager.get(virtualNetwork.id(), DeviceService.class);

        // test the getPortStatistics() method
        assertEquals("The port statistics set size did not match.", 0,
                     deviceService.getPortStatistics(DID1).size());
    }

    /**
     * Tests querying the port delta statistics of a device by null device identifier.
     */
    @Test(expected = NullPointerException.class)
    public void testGetPortsDeltaStatisticsByNullId() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        DeviceService deviceService = manager.get(virtualNetwork.id(), DeviceService.class);

        // test the getPortDeltaStatistics() method using a null device identifier
        deviceService.getPortDeltaStatistics(null);
    }

    /**
     * Tests querying the port delta statistics of a device by device identifier.
     */
    @Test
    public void testGetPortDeltaStatistics() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        VirtualDevice virtualDevice = manager.createVirtualDevice(virtualNetwork.id(), DID1);
        manager.createVirtualDevice(virtualNetwork.id(), DID2);

        DeviceService deviceService = manager.get(virtualNetwork.id(), DeviceService.class);

        // test the getPortDeltaStatistics() method
        assertEquals("The port delta statistics set size did not match.", 0,
                     deviceService.getPortDeltaStatistics(DID1).size());
    }

    /**
     * Tests DeviceEvents received during virtual device/port addition and removal.
     */
    @Test
    public void testDeviceEventsForAddRemovalDeviceAndPorts() throws TestUtils.TestUtilsException {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));

        // add virtual device before virtual device manager is created
        VirtualDevice device1 = manager.createVirtualDevice(virtualNetwork.id(), VDID1);
        validateEvents(); // no DeviceEvent expected

        testDirectory.add(EventDeliveryService.class, dispatcher);
        DeviceService deviceService = manager.get(virtualNetwork.id(), DeviceService.class);

        // virtual device manager is created; register DeviceEvent listener
        deviceService.addListener(testListener);

        // list to keep track of expected event types
        List<DeviceEvent.Type> expectedEventTypes = new ArrayList<>();

        // add virtual device
        VirtualDevice device2 = manager.createVirtualDevice(virtualNetwork.id(), VDID2);
        expectedEventTypes.add(DeviceEvent.Type.DEVICE_ADDED);

        ConnectPoint cp = new ConnectPoint(PHYDID1, PortNumber.portNumber(1));

        // add 2 virtual ports
        manager.createVirtualPort(virtualNetwork.id(),
                                  device2.id(), PortNumber.portNumber(1), cp);
        expectedEventTypes.add(DeviceEvent.Type.PORT_ADDED);
        manager.createVirtualPort(virtualNetwork.id(),
                                  device2.id(), PortNumber.portNumber(2), cp);
        expectedEventTypes.add(DeviceEvent.Type.PORT_ADDED);

        // verify virtual ports were added
        Set<VirtualPort> virtualPorts = manager.getVirtualPorts(virtualNetwork.id(), device2.id());
        assertNotNull("The virtual port set should not be null", virtualPorts);
        assertEquals("The virtual port set size did not match.", 2, virtualPorts.size());

        // remove 2 virtual ports
        for (VirtualPort virtualPort : virtualPorts) {
            manager.removeVirtualPort(virtualNetwork.id(),
                                      (DeviceId) virtualPort.element().id(), virtualPort.number());
            expectedEventTypes.add(DeviceEvent.Type.PORT_REMOVED);
            // attempt to remove the same virtual port again - no DeviceEvent.Type.PORT_REMOVED expected.
            manager.removeVirtualPort(virtualNetwork.id(),
                                      (DeviceId) virtualPort.element().id(), virtualPort.number());
        }

        // verify virtual ports were removed
        virtualPorts = manager.getVirtualPorts(virtualNetwork.id(), device2.id());
        assertTrue("The virtual port set should be empty.", virtualPorts.isEmpty());

        // Add/remove one virtual port again.
        VirtualPort virtualPort =
                manager.createVirtualPort(virtualNetwork.id(), device2.id(),
                                                            PortNumber.portNumber(1), cp);
        expectedEventTypes.add(DeviceEvent.Type.PORT_ADDED);

        ConnectPoint newCp = new ConnectPoint(PHYDID3, PortNumber.portNumber(2));
        manager.bindVirtualPort(virtualNetwork.id(), device2.id(),
                                PortNumber.portNumber(1), newCp);
        expectedEventTypes.add(DeviceEvent.Type.PORT_UPDATED);

        manager.removeVirtualPort(virtualNetwork.id(),
                                  (DeviceId) virtualPort.element().id(), virtualPort.number());
        expectedEventTypes.add(DeviceEvent.Type.PORT_REMOVED);

        // verify no virtual ports remain
        virtualPorts = manager.getVirtualPorts(virtualNetwork.id(), device2.id());
        assertTrue("The virtual port set should be empty.", virtualPorts.isEmpty());

        // remove virtual device
        manager.removeVirtualDevice(virtualNetwork.id(), device2.id());
        expectedEventTypes.add(DeviceEvent.Type.DEVICE_REMOVED);

        // Validate that the events were all received in the correct order.
        validateEvents((Enum[]) expectedEventTypes.toArray(
                new DeviceEvent.Type[expectedEventTypes.size()]));

        // cleanup
        deviceService.removeListener(testListener);
    }

    /**
     * Core service test class.
     */
    private class TestCoreService extends CoreServiceAdapter {

        @Override
        public IdGenerator getIdGenerator(String topic) {
            return new IdGenerator() {
                private AtomicLong counter = new AtomicLong(0);

                @Override
                public long getNewId() {
                    return counter.getAndIncrement();
                }
            };
        }
    }

    /**
     * Method to validate that the actual versus expected virtual network events were
     * received correctly.
     *
     * @param types expected virtual network events.
     */
    private void validateEvents(Enum... types) {
        TestTools.assertAfter(100, () -> {
            int i = 0;
            assertEquals("wrong events received", types.length, testListener.events.size());
            for (Event event : testListener.events) {
                assertEquals("incorrect event type", types[i], event.type());
                i++;
            }
            testListener.events.clear();
        });
    }

    /**
     * Test listener class to receive device events.
     */
    private static class TestListener implements DeviceListener {

        private List<DeviceEvent> events = Lists.newArrayList();

        @Override
        public void event(DeviceEvent event) {
            events.add(event);
        }
    }
}
