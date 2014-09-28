/**
 *
 */
package org.onlab.onos.store.device.impl;

import static org.junit.Assert.*;
import static org.onlab.onos.net.Device.Type.SWITCH;
import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.device.DeviceEvent.Type.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.device.DefaultDeviceDescription;
import org.onlab.onos.net.device.DefaultPortDescription;
import org.onlab.onos.net.device.DeviceDescription;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceStoreDelegate;
import org.onlab.onos.net.device.PortDescription;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.common.StoreManager;
import org.onlab.onos.store.common.StoreService;
import org.onlab.onos.store.common.TestStoreManager;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;

/**
 * Test of the Hazelcast based distributed DeviceStore implementation.
 */
public class DistributedDeviceStoreTest {

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final DeviceId DID1 = deviceId("of:foo");
    private static final DeviceId DID2 = deviceId("of:bar");
    private static final String MFR = "whitebox";
    private static final String HW = "1.1.x";
    private static final String SW1 = "3.8.1";
    private static final String SW2 = "3.9.5";
    private static final String SN = "43311-12345";

    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final PortNumber P2 = PortNumber.portNumber(2);
    private static final PortNumber P3 = PortNumber.portNumber(3);

    private DistributedDeviceStore deviceStore;

    private StoreManager storeManager;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }


    @Before
    public void setUp() throws Exception {
        // TODO should find a way to clean Hazelcast instance without shutdown.
        Config config = TestStoreManager.getTestConfig();

        storeManager = new TestStoreManager(Hazelcast.newHazelcastInstance(config));
        storeManager.activate();

        deviceStore = new TestDistributedDeviceStore(storeManager);
        deviceStore.activate();
    }

    @After
    public void tearDown() throws Exception {
        deviceStore.deactivate();

        storeManager.deactivate();
    }

    private void putDevice(DeviceId deviceId, String swVersion) {
        DeviceDescription description =
                new DefaultDeviceDescription(deviceId.uri(), SWITCH, MFR,
                        HW, swVersion, SN);
        deviceStore.createOrUpdateDevice(PID, deviceId, description);
    }

    private static void assertDevice(DeviceId id, String swVersion, Device device) {
        assertNotNull(device);
        assertEquals(id, device.id());
        assertEquals(MFR, device.manufacturer());
        assertEquals(HW, device.hwVersion());
        assertEquals(swVersion, device.swVersion());
        assertEquals(SN, device.serialNumber());
    }

    @Test
    public final void testGetDeviceCount() {
        assertEquals("initialy empty", 0, deviceStore.getDeviceCount());

        putDevice(DID1, SW1);
        putDevice(DID2, SW2);
        putDevice(DID1, SW1);

        assertEquals("expect 2 uniq devices", 2, deviceStore.getDeviceCount());
    }

    @Test
    public final void testGetDevices() {
        assertEquals("initialy empty", 0, Iterables.size(deviceStore.getDevices()));

        putDevice(DID1, SW1);
        putDevice(DID2, SW2);
        putDevice(DID1, SW1);

        assertEquals("expect 2 uniq devices",
                2, Iterables.size(deviceStore.getDevices()));

        Map<DeviceId, Device> devices = new HashMap<>();
        for (Device device : deviceStore.getDevices()) {
            devices.put(device.id(), device);
        }

        assertDevice(DID1, SW1, devices.get(DID1));
        assertDevice(DID2, SW2, devices.get(DID2));

        // add case for new node?
    }

    @Test
    public final void testGetDevice() {

        putDevice(DID1, SW1);

        assertDevice(DID1, SW1, deviceStore.getDevice(DID1));
        assertNull("DID2 shouldn't be there", deviceStore.getDevice(DID2));
    }

    @Test
    public final void testCreateOrUpdateDevice() {
        DeviceDescription description =
                new DefaultDeviceDescription(DID1.uri(), SWITCH, MFR,
                        HW, SW1, SN);
        DeviceEvent event = deviceStore.createOrUpdateDevice(PID, DID1, description);
        assertEquals(DEVICE_ADDED, event.type());
        assertDevice(DID1, SW1, event.subject());

        DeviceDescription description2 =
                new DefaultDeviceDescription(DID1.uri(), SWITCH, MFR,
                        HW, SW2, SN);
        DeviceEvent event2 = deviceStore.createOrUpdateDevice(PID, DID1, description2);
        assertEquals(DEVICE_UPDATED, event2.type());
        assertDevice(DID1, SW2, event2.subject());

        assertNull("No change expected", deviceStore.createOrUpdateDevice(PID, DID1, description2));
    }

    @Test
    public final void testMarkOffline() {

        putDevice(DID1, SW1);
        assertTrue(deviceStore.isAvailable(DID1));

        DeviceEvent event = deviceStore.markOffline(DID1);
        assertEquals(DEVICE_AVAILABILITY_CHANGED, event.type());
        assertDevice(DID1, SW1, event.subject());
        assertFalse(deviceStore.isAvailable(DID1));

        DeviceEvent event2 = deviceStore.markOffline(DID1);
        assertNull("No change, no event", event2);
}

    @Test
    public final void testUpdatePorts() {
        putDevice(DID1, SW1);
        List<PortDescription> pds = Arrays.<PortDescription>asList(
                new DefaultPortDescription(P1, true),
                new DefaultPortDescription(P2, true)
                );

        List<DeviceEvent> events = deviceStore.updatePorts(DID1, pds);

        Set<PortNumber> expectedPorts = Sets.newHashSet(P1, P2);
        for (DeviceEvent event : events) {
            assertEquals(PORT_ADDED, event.type());
            assertDevice(DID1, SW1, event.subject());
            assertTrue("PortNumber is one of expected",
                    expectedPorts.remove(event.port().number()));
            assertTrue("Port is enabled", event.port().isEnabled());
        }
        assertTrue("Event for all expectedport appeared", expectedPorts.isEmpty());


        List<PortDescription> pds2 = Arrays.<PortDescription>asList(
                new DefaultPortDescription(P1, false),
                new DefaultPortDescription(P2, true),
                new DefaultPortDescription(P3, true)
                );

        events = deviceStore.updatePorts(DID1, pds2);
        assertFalse("event should be triggered", events.isEmpty());
        for (DeviceEvent event : events) {
            PortNumber num = event.port().number();
            if (P1.equals(num)) {
                assertEquals(PORT_UPDATED, event.type());
                assertDevice(DID1, SW1, event.subject());
                assertFalse("Port is disabled", event.port().isEnabled());
            } else if (P2.equals(num)) {
                fail("P2 event not expected.");
            } else if (P3.equals(num)) {
                assertEquals(PORT_ADDED, event.type());
                assertDevice(DID1, SW1, event.subject());
                assertTrue("Port is enabled", event.port().isEnabled());
            } else {
                fail("Unknown port number encountered: " + num);
            }
        }

        List<PortDescription> pds3 = Arrays.<PortDescription>asList(
                new DefaultPortDescription(P1, false),
                new DefaultPortDescription(P2, true)
                );
        events = deviceStore.updatePorts(DID1, pds3);
        assertFalse("event should be triggered", events.isEmpty());
        for (DeviceEvent event : events) {
            PortNumber num = event.port().number();
            if (P1.equals(num)) {
                fail("P1 event not expected.");
            } else if (P2.equals(num)) {
                fail("P2 event not expected.");
            } else if (P3.equals(num)) {
                assertEquals(PORT_REMOVED, event.type());
                assertDevice(DID1, SW1, event.subject());
                assertTrue("Port was enabled", event.port().isEnabled());
            } else {
                fail("Unknown port number encountered: " + num);
            }
        }

    }

    @Test
    public final void testUpdatePortStatus() {
        putDevice(DID1, SW1);
        List<PortDescription> pds = Arrays.<PortDescription>asList(
                new DefaultPortDescription(P1, true)
                );
        deviceStore.updatePorts(DID1, pds);

        DeviceEvent event = deviceStore.updatePortStatus(DID1,
                new DefaultPortDescription(P1, false));
        assertEquals(PORT_UPDATED, event.type());
        assertDevice(DID1, SW1, event.subject());
        assertEquals(P1, event.port().number());
        assertFalse("Port is disabled", event.port().isEnabled());
    }

    @Test
    public final void testGetPorts() {
        putDevice(DID1, SW1);
        putDevice(DID2, SW1);
        List<PortDescription> pds = Arrays.<PortDescription>asList(
                new DefaultPortDescription(P1, true),
                new DefaultPortDescription(P2, true)
                );
        deviceStore.updatePorts(DID1, pds);

        Set<PortNumber> expectedPorts = Sets.newHashSet(P1, P2);
        List<Port> ports = deviceStore.getPorts(DID1);
        for (Port port : ports) {
            assertTrue("Port is enabled", port.isEnabled());
            assertTrue("PortNumber is one of expected",
                    expectedPorts.remove(port.number()));
        }
        assertTrue("Event for all expectedport appeared", expectedPorts.isEmpty());


        assertTrue("DID2 has no ports", deviceStore.getPorts(DID2).isEmpty());
    }

    @Test
    public final void testGetPort() {
        putDevice(DID1, SW1);
        putDevice(DID2, SW1);
        List<PortDescription> pds = Arrays.<PortDescription>asList(
                new DefaultPortDescription(P1, true),
                new DefaultPortDescription(P2, false)
                );
        deviceStore.updatePorts(DID1, pds);

        Port port1 = deviceStore.getPort(DID1, P1);
        assertEquals(P1, port1.number());
        assertTrue("Port is enabled", port1.isEnabled());

        Port port2 = deviceStore.getPort(DID1, P2);
        assertEquals(P2, port2.number());
        assertFalse("Port is disabled", port2.isEnabled());

        Port port3 = deviceStore.getPort(DID1, P3);
        assertNull("P3 not expected", port3);
    }

    @Test
    public final void testRemoveDevice() {
        putDevice(DID1, SW1);
        putDevice(DID2, SW1);

        assertEquals(2, deviceStore.getDeviceCount());

        DeviceEvent event = deviceStore.removeDevice(DID1);
        assertEquals(DEVICE_REMOVED, event.type());
        assertDevice(DID1, SW1, event.subject());

        assertEquals(1, deviceStore.getDeviceCount());
    }

    // TODO add test for Port events when we have them
    @Ignore("Ignore until Delegate spec. is clear.")
    @Test
    public final void testEvents() throws InterruptedException {
        final CountDownLatch addLatch = new CountDownLatch(1);
        DeviceStoreDelegate checkAdd = new DeviceStoreDelegate() {
            @Override
            public void notify(DeviceEvent event) {
                assertEquals(DEVICE_ADDED, event.type());
                assertDevice(DID1, SW1, event.subject());
                addLatch.countDown();
            }
        };
        final CountDownLatch updateLatch = new CountDownLatch(1);
        DeviceStoreDelegate checkUpdate = new DeviceStoreDelegate() {
            @Override
            public void notify(DeviceEvent event) {
                assertEquals(DEVICE_UPDATED, event.type());
                assertDevice(DID1, SW2, event.subject());
                updateLatch.countDown();
            }
        };
        final CountDownLatch removeLatch = new CountDownLatch(1);
        DeviceStoreDelegate checkRemove = new DeviceStoreDelegate() {
            @Override
            public void notify(DeviceEvent event) {
                assertEquals(DEVICE_REMOVED, event.type());
                assertDevice(DID1, SW2, event.subject());
                removeLatch.countDown();
            }
        };

        DeviceDescription description =
                new DefaultDeviceDescription(DID1.uri(), SWITCH, MFR,
                        HW, SW1, SN);
        deviceStore.setDelegate(checkAdd);
        deviceStore.createOrUpdateDevice(PID, DID1, description);
        assertTrue("Add event fired", addLatch.await(1, TimeUnit.SECONDS));


        DeviceDescription description2 =
                new DefaultDeviceDescription(DID1.uri(), SWITCH, MFR,
                        HW, SW2, SN);
        deviceStore.unsetDelegate(checkAdd);
        deviceStore.setDelegate(checkUpdate);
        deviceStore.createOrUpdateDevice(PID, DID1, description2);
        assertTrue("Update event fired", updateLatch.await(1, TimeUnit.SECONDS));

        deviceStore.unsetDelegate(checkUpdate);
        deviceStore.setDelegate(checkRemove);
        deviceStore.removeDevice(DID1);
        assertTrue("Remove event fired", removeLatch.await(1, TimeUnit.SECONDS));
    }

    private class TestDistributedDeviceStore extends DistributedDeviceStore {
        public TestDistributedDeviceStore(StoreService storeService) {
            this.storeService = storeService;
        }
    }
}
