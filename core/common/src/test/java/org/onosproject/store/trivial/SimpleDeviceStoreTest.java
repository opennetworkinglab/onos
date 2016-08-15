/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.store.trivial;

import static org.junit.Assert.*;
import static org.onosproject.net.Device.Type.SWITCH;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.device.DeviceEvent.Type.*;
import static org.onosproject.net.NetTestTools.assertAnnotationsEquals;

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
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceStore;
import org.onosproject.net.device.DeviceStoreDelegate;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.provider.ProviderId;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import org.onlab.packet.ChassisId;

/**
 * Test of the simple DeviceStore implementation.
 */
public class SimpleDeviceStoreTest {

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final ProviderId PIDA = new ProviderId("of", "bar", true);
    private static final DeviceId DID1 = deviceId("of:foo");
    private static final DeviceId DID2 = deviceId("of:bar");
    private static final String MFR = "whitebox";
    private static final String HW = "1.1.x";
    private static final String SW1 = "3.8.1";
    private static final String SW2 = "3.9.5";
    private static final String SN = "43311-12345";
    private static final ChassisId CID = new ChassisId();

    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final PortNumber P2 = PortNumber.portNumber(2);
    private static final PortNumber P3 = PortNumber.portNumber(3);

    private static final SparseAnnotations A1 = DefaultAnnotations.builder()
            .set("A1", "a1")
            .set("B1", "b1")
            .build();
    private static final SparseAnnotations A1_2 = DefaultAnnotations.builder()
            .remove("A1")
            .set("B3", "b3")
            .build();
    private static final SparseAnnotations A2 = DefaultAnnotations.builder()
            .set("A2", "a2")
            .set("B2", "b2")
            .build();
    private static final SparseAnnotations A2_2 = DefaultAnnotations.builder()
            .remove("A2")
            .set("B4", "b4")
            .build();

    private SimpleDeviceStore simpleDeviceStore;
    private DeviceStore deviceStore;



    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }


    @Before
    public void setUp() throws Exception {
        simpleDeviceStore = new SimpleDeviceStore();
        simpleDeviceStore.activate();
        deviceStore = simpleDeviceStore;
    }

    @After
    public void tearDown() throws Exception {
        simpleDeviceStore.deactivate();
    }

    private void putDevice(DeviceId deviceId, String swVersion,
                           SparseAnnotations... annotations) {
        DeviceDescription description =
                new DefaultDeviceDescription(deviceId.uri(), SWITCH, MFR,
                        HW, swVersion, SN, CID, annotations);
        deviceStore.createOrUpdateDevice(PID, deviceId, description);
    }

    private void putDeviceAncillary(DeviceId deviceId, String swVersion,
                                    SparseAnnotations... annotations) {
        DeviceDescription description =
                new DefaultDeviceDescription(deviceId.uri(), SWITCH, MFR,
                        HW, swVersion, SN, CID, annotations);
        deviceStore.createOrUpdateDevice(PIDA, deviceId, description);
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
                        HW, SW1, SN, CID);
        DeviceEvent event = deviceStore.createOrUpdateDevice(PID, DID1, description);
        assertEquals(DEVICE_ADDED, event.type());
        assertDevice(DID1, SW1, event.subject());

        DeviceDescription description2 =
                new DefaultDeviceDescription(DID1.uri(), SWITCH, MFR,
                        HW, SW2, SN, CID);
        DeviceEvent event2 = deviceStore.createOrUpdateDevice(PID, DID1, description2);
        assertEquals(DEVICE_UPDATED, event2.type());
        assertDevice(DID1, SW2, event2.subject());

        assertNull("No change expected", deviceStore.createOrUpdateDevice(PID, DID1, description2));
    }

    @Test
    public final void testCreateOrUpdateDeviceAncillary() {
        DeviceDescription description =
                new DefaultDeviceDescription(DID1.uri(), SWITCH, MFR,
                        HW, SW1, SN, CID, A2);
        DeviceEvent event = deviceStore.createOrUpdateDevice(PIDA, DID1, description);
        assertEquals(DEVICE_ADDED, event.type());
        assertDevice(DID1, SW1, event.subject());
        assertEquals(PIDA, event.subject().providerId());
        assertAnnotationsEquals(event.subject().annotations(), A2);
        assertFalse("Ancillary will not bring device up", deviceStore.isAvailable(DID1));

        DeviceDescription description2 =
                new DefaultDeviceDescription(DID1.uri(), SWITCH, MFR,
                        HW, SW2, SN, CID, A1);
        DeviceEvent event2 = deviceStore.createOrUpdateDevice(PID, DID1, description2);
        assertEquals(DEVICE_UPDATED, event2.type());
        assertDevice(DID1, SW2, event2.subject());
        assertEquals(PID, event2.subject().providerId());
        assertAnnotationsEquals(event2.subject().annotations(), A1, A2);
        assertTrue(deviceStore.isAvailable(DID1));

        assertNull("No change expected", deviceStore.createOrUpdateDevice(PID, DID1, description2));

        // For now, Ancillary is ignored once primary appears
        assertNull("No change expected", deviceStore.createOrUpdateDevice(PIDA, DID1, description));

        // But, Ancillary annotations will be in effect
        DeviceDescription description3 =
                new DefaultDeviceDescription(DID1.uri(), SWITCH, MFR,
                        HW, SW1, SN, CID, A2_2);
        DeviceEvent event3 = deviceStore.createOrUpdateDevice(PIDA, DID1, description3);
        assertEquals(DEVICE_UPDATED, event3.type());
        // basic information will be the one from Primary
        assertDevice(DID1, SW2, event3.subject());
        assertEquals(PID, event3.subject().providerId());
        // but annotation from Ancillary will be merged
        assertAnnotationsEquals(event3.subject().annotations(), A1, A2, A2_2);
        assertTrue(deviceStore.isAvailable(DID1));
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
        List<PortDescription> pds = Arrays.asList(
                new DefaultPortDescription(P1, true),
                new DefaultPortDescription(P2, true)
                );

        List<DeviceEvent> events = deviceStore.updatePorts(PID, DID1, pds);

        Set<PortNumber> expectedPorts = Sets.newHashSet(P1, P2);
        for (DeviceEvent event : events) {
            assertEquals(PORT_ADDED, event.type());
            assertDevice(DID1, SW1, event.subject());
            assertTrue("PortNumber is one of expected",
                    expectedPorts.remove(event.port().number()));
            assertTrue("Port is enabled", event.port().isEnabled());
        }
        assertTrue("Event for all expectedport appeared", expectedPorts.isEmpty());


        List<PortDescription> pds2 = Arrays.asList(
                new DefaultPortDescription(P1, false),
                new DefaultPortDescription(P2, true),
                new DefaultPortDescription(P3, true)
                );

        events = deviceStore.updatePorts(PID, DID1, pds2);
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

        List<PortDescription> pds3 = Arrays.asList(
                new DefaultPortDescription(P1, false),
                new DefaultPortDescription(P2, true)
                );
        events = deviceStore.updatePorts(PID, DID1, pds3);
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
        List<PortDescription> pds = Arrays.asList(
                new DefaultPortDescription(P1, true)
                );
        deviceStore.updatePorts(PID, DID1, pds);

        DeviceEvent event = deviceStore.updatePortStatus(PID, DID1,
                new DefaultPortDescription(P1, false));
        assertEquals(PORT_UPDATED, event.type());
        assertDevice(DID1, SW1, event.subject());
        assertEquals(P1, event.port().number());
        assertFalse("Port is disabled", event.port().isEnabled());

    }

    @Test
    public final void testUpdatePortStatusAncillary() {
        putDeviceAncillary(DID1, SW1);
        putDevice(DID1, SW1);
        List<PortDescription> pds = Arrays.asList(
                new DefaultPortDescription(P1, true, A1)
                );
        deviceStore.updatePorts(PID, DID1, pds);

        DeviceEvent event = deviceStore.updatePortStatus(PID, DID1,
                new DefaultPortDescription(P1, false, A1_2));
        assertEquals(PORT_UPDATED, event.type());
        assertDevice(DID1, SW1, event.subject());
        assertEquals(P1, event.port().number());
        assertAnnotationsEquals(event.port().annotations(), A1, A1_2);
        assertFalse("Port is disabled", event.port().isEnabled());

        DeviceEvent event2 = deviceStore.updatePortStatus(PIDA, DID1,
                new DefaultPortDescription(P1, true));
        assertNull("Ancillary is ignored if primary exists", event2);

        // but, Ancillary annotation update will be notified
        DeviceEvent event3 = deviceStore.updatePortStatus(PIDA, DID1,
                new DefaultPortDescription(P1, true, A2));
        assertEquals(PORT_UPDATED, event3.type());
        assertDevice(DID1, SW1, event3.subject());
        assertEquals(P1, event3.port().number());
        assertAnnotationsEquals(event3.port().annotations(), A1, A1_2, A2);
        assertFalse("Port is disabled", event3.port().isEnabled());

        // port only reported from Ancillary will be notified as down
        DeviceEvent event4 = deviceStore.updatePortStatus(PIDA, DID1,
                new DefaultPortDescription(P2, true));
        assertEquals(PORT_ADDED, event4.type());
        assertDevice(DID1, SW1, event4.subject());
        assertEquals(P2, event4.port().number());
        assertAnnotationsEquals(event4.port().annotations());
        assertFalse("Port is disabled if not given from primary provider",
                        event4.port().isEnabled());
    }

    @Test
    public final void testGetPorts() {
        putDevice(DID1, SW1);
        putDevice(DID2, SW1);
        List<PortDescription> pds = Arrays.<PortDescription>asList(
                new DefaultPortDescription(P1, true),
                new DefaultPortDescription(P2, true)
                );
        deviceStore.updatePorts(PID, DID1, pds);

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
        List<PortDescription> pds = Arrays.asList(
                new DefaultPortDescription(P1, true),
                new DefaultPortDescription(P2, false)
                );
        deviceStore.updatePorts(PID, DID1, pds);

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
        putDevice(DID1, SW1, A1);
        List<PortDescription> pds = Arrays.asList(
                new DefaultPortDescription(P1, true, A2)
                );
        deviceStore.updatePorts(PID, DID1, pds);
        putDevice(DID2, SW1);

        assertEquals(2, deviceStore.getDeviceCount());
        assertEquals(1, deviceStore.getPorts(DID1).size());
        assertAnnotationsEquals(deviceStore.getDevice(DID1).annotations(), A1);
        assertAnnotationsEquals(deviceStore.getPort(DID1, P1).annotations(), A2);

        DeviceEvent event = deviceStore.removeDevice(DID1);
        assertEquals(DEVICE_REMOVED, event.type());
        assertDevice(DID1, SW1, event.subject());

        assertEquals(1, deviceStore.getDeviceCount());
        assertEquals(0, deviceStore.getPorts(DID1).size());

        // putBack Device, Port w/o annotation
        putDevice(DID1, SW1);
        List<PortDescription> pds2 = Arrays.asList(
                new DefaultPortDescription(P1, true)
                );
        deviceStore.updatePorts(PID, DID1, pds2);

        // annotations should not survive
        assertEquals(2, deviceStore.getDeviceCount());
        assertEquals(1, deviceStore.getPorts(DID1).size());
        assertAnnotationsEquals(deviceStore.getDevice(DID1).annotations());
        assertAnnotationsEquals(deviceStore.getPort(DID1, P1).annotations());
    }

    // If Delegates should be called only on remote events,
    // then Simple* should never call them, thus not test required.
    // TODO add test for Port events when we have them
    @Ignore("Ignore until Delegate spec. is clear.")
    @Test
    public final void testEvents() throws InterruptedException {
        final CountDownLatch addLatch = new CountDownLatch(1);
        DeviceStoreDelegate checkAdd = event -> {
            assertEquals(DEVICE_ADDED, event.type());
            assertDevice(DID1, SW1, event.subject());
            addLatch.countDown();
        };
        final CountDownLatch updateLatch = new CountDownLatch(1);
        DeviceStoreDelegate checkUpdate = event -> {
            assertEquals(DEVICE_UPDATED, event.type());
            assertDevice(DID1, SW2, event.subject());
            updateLatch.countDown();
        };
        final CountDownLatch removeLatch = new CountDownLatch(1);
        DeviceStoreDelegate checkRemove = event -> {
            assertEquals(DEVICE_REMOVED, event.type());
            assertDevice(DID1, SW2, event.subject());
            removeLatch.countDown();
        };

        DeviceDescription description =
                new DefaultDeviceDescription(DID1.uri(), SWITCH, MFR,
                        HW, SW1, SN, CID);
        deviceStore.setDelegate(checkAdd);
        deviceStore.createOrUpdateDevice(PID, DID1, description);
        assertTrue("Add event fired", addLatch.await(1, TimeUnit.SECONDS));


        DeviceDescription description2 =
                new DefaultDeviceDescription(DID1.uri(), SWITCH, MFR,
                        HW, SW2, SN, CID);
        deviceStore.unsetDelegate(checkAdd);
        deviceStore.setDelegate(checkUpdate);
        deviceStore.createOrUpdateDevice(PID, DID1, description2);
        assertTrue("Update event fired", updateLatch.await(1, TimeUnit.SECONDS));

        deviceStore.unsetDelegate(checkUpdate);
        deviceStore.setDelegate(checkRemove);
        deviceStore.removeDevice(DID1);
        assertTrue("Remove event fired", removeLatch.await(1, TimeUnit.SECONDS));
    }
}
