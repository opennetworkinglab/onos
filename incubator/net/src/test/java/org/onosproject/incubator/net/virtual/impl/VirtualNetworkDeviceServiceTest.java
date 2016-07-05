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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.IdGenerator;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.incubator.store.virtual.impl.DistributedVirtualNetworkStore;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TestDeviceParams;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.FakeIntentManager;
import org.onosproject.net.intent.TestableIntentService;
import org.onosproject.store.service.TestStorageService;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Junit tests for VirtualNetworkDeviceService.
 */
public class VirtualNetworkDeviceServiceTest extends TestDeviceParams {
    private final String tenantIdValue1 = "TENANT_ID1";

    private VirtualNetworkManager manager;
    private DistributedVirtualNetworkStore virtualNetworkManagerStore;
    private CoreService coreService;
    private TestableIntentService intentService = new FakeIntentManager();

    @Before
    public void setUp() throws Exception {
        virtualNetworkManagerStore = new DistributedVirtualNetworkStore();

        coreService = new VirtualNetworkDeviceServiceTest.TestCoreService();
        virtualNetworkManagerStore.setCoreService(coreService);
        TestUtils.setField(coreService, "coreService", new VirtualNetworkDeviceServiceTest.TestCoreService());
        TestUtils.setField(virtualNetworkManagerStore, "storageService", new TestStorageService());
        virtualNetworkManagerStore.activate();

        manager = new VirtualNetworkManager();
        manager.store = virtualNetworkManagerStore;
        manager.intentService = intentService;
        NetTestTools.injectEventDispatcher(manager, new TestEventDispatcher());
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

        Port port = new DefaultPort(virtualDevice, PortNumber.portNumber(1), true);

        manager.createVirtualPort(virtualNetwork.id(), virtualDevice.id(), PortNumber.portNumber(1), port);
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice.id(), PortNumber.portNumber(2), port);

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

        Port port = new DefaultPort(virtualDevice, PortNumber.portNumber(1), true);

        VirtualPort virtualPort1 = manager.createVirtualPort(virtualNetwork.id(), virtualDevice.id(),
                                                             PortNumber.portNumber(1), port);
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice.id(), PortNumber.portNumber(2), port);

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
}
