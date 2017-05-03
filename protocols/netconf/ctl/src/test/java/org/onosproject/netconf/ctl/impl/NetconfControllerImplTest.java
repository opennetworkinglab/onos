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
package org.onosproject.netconf.ctl.impl;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ComponentContextAdapter;
import org.onlab.packet.IpAddress;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.key.DeviceKeyService;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfDeviceFactory;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfDeviceListener;
import org.onosproject.netconf.NetconfDeviceOutputEvent;
import org.onosproject.netconf.NetconfDeviceOutputEventListener;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.osgi.service.component.ComponentContext;

import java.lang.reflect.Field;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for the Netconf controller implementation test.
 */
public class NetconfControllerImplTest {

    NetconfControllerImpl ctrl;

    //DeviceInfo
    NetconfDeviceInfo deviceInfo1;
    NetconfDeviceInfo deviceInfo2;
    NetconfDeviceInfo badDeviceInfo3;
    NetconfDeviceInfo deviceInfoIpV6;

    //Devices & DeviceId
    NetconfDevice device1;
    DeviceId deviceId1;
    NetconfDevice device2;
    DeviceId deviceId2;

    //Events
    NetconfDeviceOutputEvent eventForDeviceInfo1;
    NetconfDeviceOutputEvent eventForDeviceInfo2;

    private Map<DeviceId, NetconfDevice> reflectedDeviceMap;
    private NetconfDeviceOutputEventListener reflectedDownListener;

    //Test Device IP addresses and ports
    private static final String DEVICE_1_IP = "10.10.10.11";
    private static final String DEVICE_2_IP = "10.10.10.12";
    private static final String BAD_DEVICE_IP = "10.10.10.13";
    private static final String DEVICE_IPV6 = "2001:db8::1";

    private static final int DEVICE_1_PORT = 11;
    private static final int DEVICE_2_PORT = 12;
    private static final int BAD_DEVICE_PORT = 13;
    private static final int IPV6_DEVICE_PORT = 14;

    private static ComponentConfigService cfgService = new ComponentConfigAdapter();
    private static DeviceService deviceService = new NetconfDeviceServiceMock();
    private static DeviceKeyService deviceKeyService = new NetconfDeviceKeyServiceMock();

    private final ComponentContext context = new MockComponentContext();

    @Before
    public void setUp() throws Exception {
        ctrl = new NetconfControllerImpl();
        ctrl.deviceFactory = new TestNetconfDeviceFactory();
        ctrl.cfgService = cfgService;
        ctrl.deviceService = deviceService;
        ctrl.deviceKeyService = deviceKeyService;

        //Creating mock devices
        deviceInfo1 = new NetconfDeviceInfo("device1", "001", IpAddress.valueOf(DEVICE_1_IP), DEVICE_1_PORT);
        deviceInfo2 = new NetconfDeviceInfo("device2", "002", IpAddress.valueOf(DEVICE_2_IP), DEVICE_2_PORT);
        badDeviceInfo3 = new NetconfDeviceInfo("device3", "003", IpAddress.valueOf(BAD_DEVICE_IP), BAD_DEVICE_PORT);
        deviceInfoIpV6 = new NetconfDeviceInfo("deviceIpv6", "004", IpAddress.valueOf(DEVICE_IPV6), IPV6_DEVICE_PORT);

        device1 = new TestNetconfDevice(deviceInfo1);
        deviceId1 = deviceInfo1.getDeviceId();
        device2 = new TestNetconfDevice(deviceInfo2);
        deviceId2 = deviceInfo2.getDeviceId();

        //Adding to the map for testing get device calls.
        Field field1 = ctrl.getClass().getDeclaredField("netconfDeviceMap");
        field1.setAccessible(true);
        reflectedDeviceMap = (Map<DeviceId, NetconfDevice>) field1.get(ctrl);
        reflectedDeviceMap.put(deviceId1, device1);
        reflectedDeviceMap.put(deviceId2, device2);

        //Creating mock events for testing NetconfDeviceOutputEventListener
        Field field2 = ctrl.getClass().getDeclaredField("downListener");
        field2.setAccessible(true);
        reflectedDownListener = (NetconfDeviceOutputEventListener) field2.get(ctrl);

        eventForDeviceInfo1 = new NetconfDeviceOutputEvent(NetconfDeviceOutputEvent.Type.DEVICE_NOTIFICATION, null,
                                                           null, Optional.of(1), deviceInfo1);
        eventForDeviceInfo2 = new NetconfDeviceOutputEvent(NetconfDeviceOutputEvent.Type.DEVICE_UNREGISTERED, null,
                                                           null, Optional.of(2), deviceInfo2);
    }

    @After
    public void tearDown() {
        ctrl.deactivate();
    }

    /**
     * Test initialization of component configuration.
     */
    @Test
    public void testActivate() {
        assertEquals("Incorrect NetConf connect timeout, should be default",
                     5, ctrl.netconfConnectTimeout);
        assertEquals("Incorrect NetConf reply timeout, should be default",
                     5, ctrl.netconfReplyTimeout);
        ctrl.activate(null);
        assertEquals("Incorrect NetConf connect timeout, should be default",
                     5, ctrl.netconfConnectTimeout);
        assertEquals("Incorrect NetConf reply timeout, should be default",
                     5, ctrl.netconfReplyTimeout);
    }

    /**
     * Test modification of component configuration.
     */
    @Test
    public void testModified() {
        assertEquals("Incorrect NetConf connect timeout, should be default",
                     5, ctrl.netconfConnectTimeout);
        assertEquals("Incorrect NetConf session timeout, should be default",
                     5, ctrl.netconfReplyTimeout);
        ctrl.modified(context);
        assertEquals("Incorrect NetConf connect timeout, should be default",
                     2, ctrl.netconfConnectTimeout);
        assertEquals("Incorrect NetConf session timeout",
                     1, ctrl.netconfReplyTimeout);
        assertEquals("ethz-ssh2", ctrl.sshLibrary);
    }

    /**
     * Test to add DeviceListeners,
     * and also to check whether the netconfDeviceListeners set is
     * updating or not which was present in NetconfControllerImpl class.
     */
    @Test
    public void testAddRemoveDeviceListener() {
        NetconfDeviceListener deviceListener1 = EasyMock.createMock(NetconfDeviceListener.class);
        NetconfDeviceListener deviceListener2 = EasyMock.createMock(NetconfDeviceListener.class);
        NetconfDeviceListener deviceListener3 = EasyMock.createMock(NetconfDeviceListener.class);

        ctrl.addDeviceListener(deviceListener1);
        ctrl.addDeviceListener(deviceListener2);
        ctrl.addDeviceListener(deviceListener3);
        assertThat("Incorrect number of listeners", ctrl.netconfDeviceListeners, hasSize(3));
        assertThat("Not matching listeners", ctrl.netconfDeviceListeners, hasItems(deviceListener1,
                                                                                   deviceListener2, deviceListener3));

        ctrl.removeDeviceListener(deviceListener1);
        assertThat("Incorrect number of listeners", ctrl.netconfDeviceListeners, hasSize(2));
        assertThat("Not matching listeners", ctrl.netconfDeviceListeners, hasItems(deviceListener2, deviceListener3));
    }

    @Test
    public void testGetNetconfDevices() {
        Set<DeviceId> devices = new HashSet<>();
        devices.add(deviceId1);
        devices.add(deviceId2);
        assertTrue("Incorrect devices", ctrl.getNetconfDevices().containsAll(devices));
    }

    @Test
    public void testGetNetconfDevice() {
        NetconfDevice fetchedDevice1 = ctrl.getNetconfDevice(deviceId1);
        assertThat("Incorrect device fetched", fetchedDevice1, is(device1));

        NetconfDevice fetchedDevice2 = ctrl.getNetconfDevice(deviceId2);
        assertThat("Incorrect device fetched", fetchedDevice2, is(device2));
    }

    @Test
    public void testGetNetconfDeviceWithIPPort() {
        NetconfDevice fetchedDevice1 = ctrl.getNetconfDevice(IpAddress.valueOf(DEVICE_1_IP), DEVICE_1_PORT);
        assertEquals("Incorrect device fetched", fetchedDevice1.getDeviceInfo().ip(), device1.getDeviceInfo().ip());

        NetconfDevice fetchedDevice2 = ctrl.getNetconfDevice(IpAddress.valueOf(DEVICE_2_IP), DEVICE_2_PORT);
        assertEquals("Incorrect device fetched", fetchedDevice2.getDeviceInfo().ip(), device2.getDeviceInfo().ip());
    }

    /**
     * Check for bad device connection. In this case the device map shouldn't get modified.
     */
    @Test(expected = NetconfException.class)
    public void testConnectBadDevice() throws Exception {
        reflectedDeviceMap.clear();
        try {
            ctrl.connectDevice(badDeviceInfo3.getDeviceId());
        } finally {
            assertEquals("Incorrect device connection", 0, ctrl.getDevicesMap().size());
        }
    }

    /**
     * Check for correct device connection. In this case the device map get modified.
     */
    @Test
    public void testConnectCorrectDevice() throws Exception {
        reflectedDeviceMap.clear();
        ctrl.connectDevice(deviceInfo1.getDeviceId());
        ctrl.connectDevice(deviceInfo2.getDeviceId());
        assertTrue("Incorrect device connection", ctrl.getDevicesMap().containsKey(deviceId1));
        assertTrue("Incorrect device connection", ctrl.getDevicesMap().containsKey(deviceId2));
        assertEquals("Incorrect device connection", 2, ctrl.getDevicesMap().size());
    }

    /**
     * Check for correct ipv6 device connection. In this case the device map get modified.
     */
    @Test
    public void testConnectCorrectIpv6Device() throws Exception {
        reflectedDeviceMap.clear();
        ctrl.connectDevice(deviceInfoIpV6.getDeviceId());
        assertTrue("Incorrect device connection", ctrl.getDevicesMap()
                .containsKey(deviceInfoIpV6.getDeviceId()));
        assertEquals("Incorrect device connection", 1, ctrl.getDevicesMap().size());
    }


    /**
     * Check for connect devices already added to the map.
     */
    @Test
    public void testConnectAlreadyExistingDevice() throws Exception {
        NetconfDevice alreadyExistingDevice1 = ctrl.connectDevice(deviceInfo1.getDeviceId());
        NetconfDevice alreadyExistingDevice2 = ctrl.connectDevice(deviceInfo2.getDeviceId());
        assertEquals("Incorrect device connection", alreadyExistingDevice1.getDeviceInfo().getDeviceId(),
                     deviceInfo1.getDeviceId());
        assertEquals("Incorrect device connection", alreadyExistingDevice2.getDeviceInfo().getDeviceId(),
                     deviceInfo2.getDeviceId());
    }

    /**
     * Check that disconnectDevice actually disconnects the device and removes it.
     */
    @Test
    public void testDisconnectDevice() throws Exception {
        ctrl.disconnectDevice(deviceInfo1.getDeviceId(), true);
        assertFalse("Incorrect device removal", ctrl.getDevicesMap().containsKey(deviceId1));
    }

    /**
     * Checks that disconnectDevice actually disconnects the device and removes it.
     */
    @Test
    public void testRemoveDevice() throws Exception {
        ctrl.removeDevice(deviceInfo1.getDeviceId());
        assertFalse("Incorrect device removal", ctrl.getDevicesMap().containsKey(deviceId1));
    }

    /**
     * Test to get the connected device map.
     */
    @Test
    public void testGetDevicesMap() {
        assertEquals("Incorrect device map size", 2, ctrl.getDevicesMap().size());
    }

    /**
     * Test to check whether the DeviceDownEventListener removes the device from the map when session
     * for a particular device getting closed.
     */
    @Test
    public void testDeviceDownEventListener() throws Exception {
        reflectedDeviceMap.clear();
        ctrl.connectDevice(deviceInfo1.getDeviceId());
        boolean result1 = reflectedDownListener.isRelevant(eventForDeviceInfo2);
        assertFalse("Irrelevant Device Event", result1);
        assertEquals("Incorrect device map size", 1, ctrl.getDevicesMap().size());
        reflectedDownListener.event(eventForDeviceInfo1);
        assertEquals("Incorrect device map size", 1, ctrl.getDevicesMap().size());
        ctrl.connectDevice(deviceInfo2.getDeviceId());
        boolean result2 = reflectedDownListener.isRelevant(eventForDeviceInfo2);
        assertTrue("Irrelevant Device Event", result2);
        assertEquals("Incorrect device map size", 2, ctrl.getDevicesMap().size());
        reflectedDownListener.event(eventForDeviceInfo2);
        assertEquals("Incorrect device map size", 1, ctrl.getDevicesMap().size());
    }

    /**
     * Mock NetconfDeviceFactory class.
     */
    private class TestNetconfDeviceFactory implements NetconfDeviceFactory {

        @Override
        public NetconfDevice createNetconfDevice(NetconfDeviceInfo netconfDeviceInfo) throws NetconfException {
            return new TestNetconfDevice(netconfDeviceInfo);
        }
    }

    /**
     * Mock NetconfDeviceImpl class, used for creating test devices.
     */
    protected class TestNetconfDevice implements NetconfDevice {
        private NetconfDeviceInfo netconfDeviceInfo;
        private boolean deviceState = false;
        private NetconfSession netconfSession;

        public TestNetconfDevice(NetconfDeviceInfo deviceInfo) throws NetconfException {
            netconfDeviceInfo = deviceInfo;
            if (!badDeviceInfo3.getDeviceId().equals(deviceInfo.getDeviceId())) {
                netconfSession = EasyMock.createMock(NetconfSession.class);
                deviceState = true;
            } else {
                throw new NetconfException("Cannot create Connection and Session");
            }
        }

        @Override
        public boolean isActive() {
            return deviceState;
        }

        @Override
        public NetconfSession getSession() {
            return netconfSession;
        }

        @Override
        public void disconnect() {
            deviceState = false;
            netconfSession = null;
        }

        @Override
        public NetconfDeviceInfo getDeviceInfo() {
            return netconfDeviceInfo;
        }

    }

    private class MockComponentContext extends ComponentContextAdapter {
        @Override
        public Dictionary getProperties() {
            return new MockDictionary();
        }
    }

    private class MockDictionary extends Dictionary {

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Enumeration keys() {
            return null;
        }

        @Override
        public Enumeration elements() {
            return null;
        }

        @Override
        public Object get(Object key) {
            if (key.equals("netconfConnectTimeout")) {
                return "2";
            } else if (key.equals("netconfReplyTimeout")) {
                return "1";
            } else if (key.equals("sshLibrary")) {
                return "ethz-ssh2";
            }
            return null;
        }

        @Override
        public Object put(Object key, Object value) {
            return null;
        }

        @Override
        public Object remove(Object key) {
            return null;
        }
    }
}
