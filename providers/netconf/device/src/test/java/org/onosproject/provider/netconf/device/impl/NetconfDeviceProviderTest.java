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

package org.onosproject.provider.netconf.device.impl;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.AbstractProjectableModel;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderRegistryAdapter;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceProviderServiceAdapter;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.device.DeviceStore;
import org.onosproject.net.device.DeviceStoreAdapter;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverAdapter;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverServiceAdapter;
import org.onosproject.net.key.DeviceKeyAdminService;
import org.onosproject.net.key.DeviceKeyAdminServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDeviceListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.onlab.junit.TestTools.assertAfter;
import static org.onosproject.provider.netconf.device.impl.NetconfDeviceProvider.APP_NAME;

/**
 * Netconf device provider basic test.
 */
public class NetconfDeviceProviderTest {

    private final NetconfDeviceProvider provider = new NetconfDeviceProvider();
    private final NetconfController controller = new MockNetconfController();

    //Provider Mock
    private final DeviceProviderRegistry deviceRegistry = new MockDeviceProviderRegistry();
    private final DeviceProviderService providerService = new MockDeviceProviderService();
    private final DeviceService deviceService = new MockDeviceService();
    private final MastershipService mastershipService = new MockMastershipService();
    private final Driver driver = new MockDriver();
    private final NetworkConfigRegistry cfgService = new MockNetworkConfigRegistry();
    private final DeviceKeyAdminService deviceKeyAdminService = new DeviceKeyAdminServiceAdapter();
    private final DeviceStore deviceStore = new MockDeviceStore();

    //Class for testing
    private final NetworkConfigEvent deviceAddedEvent =
            new NetworkConfigEvent(NetworkConfigEvent.Type.CONFIG_ADDED,
                                   null, NetconfProviderConfig.class);
    private final NetconfProviderConfig netconfProviderConfig = new MockNetconfProviderConfig();
    private static final String IP = "1.1.1.1";
    private static final String TEST = "test";
    private static final int DELAY_DISCOVERY = 500;

    //Provider related classes
    private CoreService coreService;
    private ApplicationId appId =
            new DefaultApplicationId(100, APP_NAME);
    private DeviceDescriptionDiscovery descriptionDiscovery = new TestDescription();
    private Set<DeviceListener> deviceListeners = new HashSet<>();
    private ConfigFactory cfgFactory;
    private Set<NetworkConfigListener> netCfgListeners = new HashSet<>();
    private HashMap<DeviceId, Device> devices = new HashMap<>();

    //Controller related classes
    private Set<NetconfDeviceListener> netconfDeviceListeners = new CopyOnWriteArraySet<>();

    @Before
    public void setUp() {
        coreService = createMock(CoreService.class);
        expect(coreService.registerApplication(APP_NAME))
                .andReturn(appId).anyTimes();
        replay(coreService);
        provider.coreService = coreService;
        provider.providerRegistry = deviceRegistry;
        provider.mastershipService = mastershipService;
        provider.deviceService = deviceService;
        provider.cfgService = cfgService;
        provider.controller = controller;
        provider.deviceKeyAdminService = deviceKeyAdminService;
        AbstractProjectableModel.setDriverService(null, new DriverServiceAdapter());
        provider.activate();
    }

    @Test
    public void activate() throws Exception {

        assertTrue("Provider should be registered", deviceRegistry.getProviders().contains(provider.id()));
        assertEquals("Incorrect device service", deviceService, provider.deviceService);
        assertEquals("Incorrect provider service", providerService, provider.providerService);
        assertEquals("Device listener should be added", 1, deviceListeners.size());
        assertFalse("Thread to connect device should be running",
                    provider.executor.isShutdown() || provider.executor.isTerminated());
        assertFalse("Scheduled task to update device should be running", provider.scheduledTask.isCancelled());
    }

    @Test
    public void deactivate() throws Exception {
        provider.deactivate();
        assertEquals("Device listener should be removed", 0, deviceListeners.size());
        assertFalse("Provider should not be registered", deviceRegistry.getProviders().contains(provider));
        assertTrue("Thread to connect device should be shutdown", provider.executor.isShutdown());
        assertTrue("Scheduled task to update device should be shutdown", provider.scheduledTask.isCancelled());
        assertNull("Provider service should be null", provider.providerService);
        assertEquals("Controller listener should be removed", 0, netconfDeviceListeners.size());
    }

    @Test
    @Ignore("Test is brittle")
    public void addDevice() {
        assertNotNull(providerService);
        assertTrue("Event should be relevant", provider.cfgListener.isRelevant(deviceAddedEvent));
        provider.cfgListener.event(deviceAddedEvent);

        assertAfter(DELAY_DISCOVERY, () ->
                assertEquals("Device should be added", 1, deviceStore.getDeviceCount()));
        devices.clear();
    }

    //TODO: implement ports discovery and check updates of the device description


    //Mock classes
    private class MockNetconfController extends NetconfControllerAdapter {

        @Override
        public void addDeviceListener(NetconfDeviceListener listener) {
            if (!netconfDeviceListeners.contains(listener)) {
                netconfDeviceListeners.add(listener);
            }
        }

        @Override
        public void removeDeviceListener(NetconfDeviceListener listener) {
            netconfDeviceListeners.remove(listener);
        }
    }

    private class MockDeviceProviderRegistry extends DeviceProviderRegistryAdapter {

        Set<ProviderId> providers = new HashSet<>();

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

    private class MockDeviceService extends DeviceServiceAdapter {
        @Override
        public void addListener(DeviceListener listener) {
            deviceListeners.add(listener);
        }

        @Override
        public void removeListener(DeviceListener listener) {
            deviceListeners.remove(listener);
        }
    }

    private class MockDeviceProviderService extends DeviceProviderServiceAdapter {

        @Override
        public void deviceConnected(DeviceId deviceId, DeviceDescription desc) {
            assertNotNull("DeviceId should be not null", deviceId);
            assertNotNull("DeviceDescription should be not null", desc);
            deviceStore.createOrUpdateDevice(ProviderId.NONE, deviceId, desc);
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
        public Device getDevice(DeviceId deviceId) {
            return devices.get(deviceId);
        }

        @Override
        public int getDeviceCount() {
            return devices.size();
        }

    }

    private class MockMastershipService extends MastershipServiceAdapter {

        @Override
        public boolean isLocalMaster(DeviceId deviceId) {
            return true;
        }
    }

    private class MockNetworkConfigRegistry extends NetworkConfigRegistryAdapter {

        @Override
        public void registerConfigFactory(ConfigFactory configFactory) {
            cfgFactory = configFactory;
        }

        @Override
        public void unregisterConfigFactory(ConfigFactory configFactory) {
            cfgFactory = null;
        }

        @Override
        public void addListener(NetworkConfigListener listener) {
            netCfgListeners.add(listener);
        }

        @Override
        public void removeListener(NetworkConfigListener listener) {
            netCfgListeners.remove(listener);
        }


        @Override
        public <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass) {
            if (configClass.equals(NetconfProviderConfig.class)) {
                return (C) netconfProviderConfig;
            } else {
                return (C) new BasicDeviceConfig();
            }
        }
    }

    private class MockNetconfProviderConfig extends NetconfProviderConfig {
        protected NetconfDeviceAddress deviceInfo =
                new NetconfDeviceAddress(IpAddress.valueOf(IP), 1, TEST, TEST);

        @Override
        public Set<NetconfProviderConfig.NetconfDeviceAddress> getDevicesAddresses() throws ConfigException {
            return ImmutableSet.of(deviceInfo);
        }

    }

    private class MockDevice extends DefaultDevice {

        public MockDevice(ProviderId providerId, DeviceId id, Type type,
                          String manufacturer, String hwVersion, String swVersion,
                          String serialNumber, ChassisId chassisId, Annotations... annotations) {
            super(providerId, id, type, manufacturer, hwVersion, swVersion, serialNumber,
                  chassisId, annotations);
        }

        @Override
        protected Driver locateDriver() {
            return driver;
        }

        @Override
        public Driver driver() {
            return driver;
        }
    }

    private class MockDriver extends DriverAdapter {
        @Override
        public <T extends Behaviour> T createBehaviour(DriverHandler handler, Class<T> behaviourClass) {

            return (T) descriptionDiscovery;
        }
    }

    private class TestDescription extends AbstractHandlerBehaviour implements DeviceDescriptionDiscovery {

        List<PortDescription> portDescriptions = new ArrayList<>();

        @Override
        public DeviceDescription discoverDeviceDetails() {
            return null;
        }

        @Override
        public List<PortDescription> discoverPortDetails() {
            return portDescriptions;
        }

        private void addDeviceDetails() {

        }

        private void addPortDesc(PortDescription portDescription) {
            portDescriptions.add(portDescription);
        }
    }
}
