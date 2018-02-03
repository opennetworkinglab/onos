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

package org.onosproject.provider.xmpp.device.impl;

import org.dom4j.Document;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderRegistryAdapter;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceProviderServiceAdapter;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.device.DeviceStoreAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.xmpp.core.XmppController;
import org.onosproject.xmpp.core.XmppDevice;
import org.onosproject.xmpp.core.XmppDeviceId;
import org.onosproject.xmpp.core.XmppDeviceListener;
import org.onosproject.xmpp.core.XmppIqListener;
import org.onosproject.xmpp.core.XmppMessageListener;
import org.onosproject.xmpp.core.XmppPresenceListener;
import org.onosproject.xmpp.core.XmppSession;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

/**
 * Testing class for XmppDeviceProvider.
 */
public class XmppDeviceProviderTest {


    private final XmppDeviceProvider provider = new XmppDeviceProvider();
    XmppControllerAdapter xmppController = new XmppControllerAdapter();

    //Provider Mock
    private final DeviceProviderRegistry deviceRegistry = new MockDeviceProviderRegistry();
    private final DeviceProviderService providerService = new MockDeviceProviderService();
    private final DeviceServiceAdapter deviceService = new DeviceServiceAdapter();
    private final MockDeviceStore deviceStore = new MockDeviceStore();

    //Provider related classes
    private CoreService coreService;
    private final ApplicationId appId =
            new DefaultApplicationId(100, APP_NAME);
    private static final String APP_NAME = "org.onosproject.xmpp";

    private final HashMap<DeviceId, Device> devices = new HashMap<>();

    private final String agentOneId = "agent1@test.org";
    private final XmppDeviceId agentOneXmppId = new XmppDeviceId(new JID(agentOneId));

    @Before
    public void setUp() throws IOException {
        coreService = createMock(CoreService.class);
        expect(coreService.registerApplication(APP_NAME))
                .andReturn(appId).anyTimes();
        replay(coreService);
        provider.coreService = coreService;
        provider.providerRegistry = deviceRegistry;
        provider.deviceService = deviceService;
        provider.providerService = providerService;
        provider.controller = xmppController;
        provider.activate(null);
        devices.clear();
    }

    @Test
    public void activate() throws Exception {
        assertTrue("Provider should be registered", deviceRegistry.getProviders().contains(provider.id()));
        assertEquals("Incorrect device service", deviceService, provider.deviceService);
        assertEquals("Incorrect provider service", providerService, provider.providerService);
        assertEquals("Incorrent application id", appId, provider.appId);
        assertNotNull("XMPP device listener should be added", xmppController.listener);
    }

    @Test
    public void deactivate() throws Exception {
        provider.deactivate();
        assertNull("Device listener should be removed", xmppController.listener);
        assertFalse("Provider should not be registered", deviceRegistry.getProviders().contains(provider.id()));
        assertNull("Provider service should be null", provider.providerService);
    }

    @Test
    public void testDeviceAdded() {
        xmppController.listener.deviceConnected(agentOneXmppId);
        assertEquals("XMPP device added", 1, devices.size());
    }

    @Test
    public void testDeviceRemoved() {
        xmppController.listener.deviceDisconnected(agentOneXmppId);
        assertEquals("XMPP device removed", 0, devices.size());
    }

    @Test
    public void testIsReachable() {
        assertTrue(provider.isReachable(DeviceId.deviceId("reachable@xmpp.org")));
        assertFalse(provider.isReachable(DeviceId.deviceId("non-reachable@xmpp.org")));
    }


    private class MockDeviceProviderRegistry extends DeviceProviderRegistryAdapter {

        final Set<ProviderId> providers = new HashSet<>();

        @Override
        public DeviceProviderService register(DeviceProvider provider) {
            providers.add(provider.id());
            return providerService;
        }

        @Override
        public void unregister(DeviceProvider provider) {
            providers.remove(provider.id());
        }

        @Override
        public Set<ProviderId> getProviders() {
            return providers;
        }

    }

    private class MockDeviceProviderService extends DeviceProviderServiceAdapter {

        @Override
        public void deviceConnected(DeviceId deviceId, DeviceDescription desc) {
            assertNotNull("DeviceId should be not null", deviceId);
            assertNotNull("DeviceDescription should be not null", desc);
            deviceStore.createOrUpdateDevice(ProviderId.NONE, deviceId, desc);
        }


        @Override
        public void deviceDisconnected(DeviceId deviceId) {
            deviceStore.removeDevice(deviceId);
        }

    }

    private class MockDeviceStore extends DeviceStoreAdapter {

        @Override
        public DeviceEvent createOrUpdateDevice(ProviderId providerId, DeviceId deviceId,
                                                DeviceDescription desc) {

            devices.put(deviceId, new DefaultDevice(providerId, deviceId, desc.type(),
                                                    desc.manufacturer(), desc.hwVersion(),
                                                    desc.swVersion(), desc.serialNumber(),
                                                    desc.chassisId(), desc.annotations()));
            return null;
        }

        @Override
        public DeviceEvent removeDevice(DeviceId deviceId) {
            devices.remove(deviceId);
            return null;
        }

        @Override
        public Device getDevice(DeviceId deviceId) {
            return devices.get(deviceId);
        }

        @Override
        public int getDeviceCount() {
            return devices.size();
        }

    }

    private class XmppControllerAdapter implements XmppController {

        XmppDeviceListener listener = null;
        Map<XmppDeviceId, XmppDevice> xmppDevices = new HashMap();

        XmppControllerAdapter() {
            XmppDeviceAdapter reachable = new XmppDeviceAdapter("reachable@xmpp.org", "127.0.0.1", 54333);
            xmppDevices.put(reachable.xmppDeviceId, reachable);
            XmppDeviceAdapter testDevice = new XmppDeviceAdapter(agentOneId, "127.0.0.1", 54334);
            xmppDevices.put(testDevice.xmppDeviceId, testDevice);
        }

        @java.lang.Override
        public XmppDevice getDevice(XmppDeviceId xmppDeviceId) {
            return xmppDevices.get(xmppDeviceId);
        }

        @java.lang.Override
        public void addXmppDeviceListener(XmppDeviceListener deviceListener) {
            this.listener = deviceListener;
        }

        @java.lang.Override
        public void removeXmppDeviceListener(XmppDeviceListener deviceListener) {
            this.listener = null;
        }

        @java.lang.Override
        public void addXmppIqListener(XmppIqListener iqListener, String namespace) {

        }

        @java.lang.Override
        public void removeXmppIqListener(XmppIqListener iqListener, String namespace) {

        }

        @java.lang.Override
        public void addXmppMessageListener(XmppMessageListener messageListener) {

        }

        @java.lang.Override
        public void removeXmppMessageListener(XmppMessageListener messageListener) {

        }

        @java.lang.Override
        public void addXmppPresenceListener(XmppPresenceListener presenceListener) {

        }

        @java.lang.Override
        public void removeXmppPresenceListener(XmppPresenceListener presenceListener) {

        }
    }

    private class XmppDeviceAdapter implements XmppDevice {

        InetSocketAddress testAddress;
        XmppDeviceId xmppDeviceId;

        public XmppDeviceAdapter(String jid, String address, int port) {
            testAddress = new InetSocketAddress(address, port);
            this.xmppDeviceId = new XmppDeviceId(new JID(jid));
        }

        @Override
        public XmppSession getSession() {
            return null;
        }

        @Override
        public InetSocketAddress getIpAddress() {
            return testAddress;
        }

        @Override
        public void registerConnectedDevice() {

        }

        @Override
        public void disconnectDevice() {

        }

        @Override
        public void sendPacket(Packet packet) {

        }

        @Override
        public void writeRawXml(Document document) {

        }

        @Override
        public void handlePacket(Packet packet) {

        }

        @Override
        public void sendError(PacketError packetError) {

        }

    }

}
