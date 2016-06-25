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

package org.onosproject.provider.snmp.device.impl;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.AbstractProjectableModel;
import org.onosproject.net.DefaultAnnotations;
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
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderRegistryAdapter;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceProviderServiceAdapter;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.device.DeviceStore;
import org.onosproject.net.device.DeviceStoreAdapter;
import org.onosproject.net.driver.DriverServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.snmp.SnmpController;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.onlab.junit.TestTools.assertAfter;

/**
 * Testing class for SnmpDeviceProvider.
 */
public class SnmpDeviceProviderTest {

    private final SnmpDeviceProvider provider = new SnmpDeviceProvider();
    private final SnmpController controller = new SnmpControllerAdapter();
    private final DeviceProviderRegistry providerRegistry = new MockDeviceProviderRegistry();
    private final DeviceService deviceService = new MockDeviceService();
    private final NetworkConfigRegistry netCfgService = new MockNetworkConfigRegistry();
    private final DeviceStore deviceStore = new MockDeviceStore();
    protected CoreService coreService = new MockCoreService();
    private final DeviceProviderService deviceProviderService = new MockDeviceProviderService();
    private final TestApplicationId applicationId = new TestApplicationId("TestAppId");
    private final NetworkConfigEvent deviceAddedEvent =
            new NetworkConfigEvent(NetworkConfigEvent.Type.CONFIG_ADDED,
                                   null, SnmpProviderConfig.class);
    private final SnmpProviderConfig snmpProviderConfig = new MockSnmpProviderConfig();
    private final NetworkConfigEvent deviceAddedIrrelevantEvent =
            new NetworkConfigEvent(NetworkConfigEvent.Type.CONFIG_ADDED,
                                   null, BasicDeviceConfig.class);
    private final DeviceId deviceId = DeviceId.deviceId("snmp:1.1.1.1:1");
    private final DeviceId wrongDeviceId = DeviceId.deviceId("snmp:2.2.2.2:2");


    @Before
    public void setUp() throws Exception {
        provider.controller = controller;
        provider.providerRegistry = providerRegistry;
        provider.deviceService = deviceService;
        provider.netCfgService = netCfgService;
        provider.deviceStore = deviceStore;
        provider.coreService = coreService;
        provider.activate(null);
    }

    @Test
    public void testActivate() {
        assertEquals("Incorrect provider service", deviceProviderService, provider.providerService);
        assertEquals("Incorrect application id", applicationId, provider.appId);
        assertEquals("Incorrect config factory", cfgFactory, provider.factory);
        assertTrue("Incorrect network config listener", netCfgListeners.contains(provider.cfgLister));


    }

    @Test
    public void testDeactivate() {
        this.addDevice();
        provider.deactivate(null);
        assertAfter(500, () ->
                assertNull("Device should be removed", controller.getDevice(deviceId)));
        assertNull("Network config factory not removed", cfgFactory);
        assertFalse("Network config listener not removed", netCfgListeners.contains(provider.cfgLister));
        assertFalse("Provider not unregistered", providerRegistry.getProviders().contains(provider.id()));
        assertNull("Provider registry not removed", provider.providerService);
    }

    @Test
    public void eventNotRelevant() {
        assertFalse("Event should not be relevant", provider.cfgLister.isRelevant(deviceAddedIrrelevantEvent));
        assertFalse("Device should not be reachable", provider.isReachable(wrongDeviceId));
    }

    @Test
    public void addDevice() {
        assertTrue("Event should be relevant", provider.cfgLister.isRelevant(deviceAddedEvent));
        provider.cfgLister.event(deviceAddedEvent);
        AbstractProjectableModel.setDriverService(null, new MockDriverService());
        //FIXME this needs sleep
        assertAfter(500, () ->
                assertNotNull("Device should be added to controller", controller.getDevice(deviceId)));
        assertTrue("Device should be reachable", provider.isReachable(deviceId));
    }

    private class MockDeviceProviderRegistry extends DeviceProviderRegistryAdapter {

        Set<ProviderId> providers = new HashSet<>();

        @Override
        public DeviceProviderService register(DeviceProvider provider) {
            providers.add(provider.id());
            return deviceProviderService;
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
        public Device getDevice(DeviceId deviceId) {
            return deviceStore.getDevice(deviceId);
        }
    }

    private ConfigFactory cfgFactory;
    private Set<NetworkConfigListener> netCfgListeners = new HashSet<>();

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
            if (configClass.equals(SnmpProviderConfig.class)) {
                return (C) snmpProviderConfig;
            } else {
                return (C) new BasicDeviceConfig();
            }
        }
    }

    private class MockDeviceStore extends DeviceStoreAdapter {
        protected HashMap<DeviceId, Device> devices = new HashMap<>();

        @Override
        public DeviceEvent createOrUpdateDevice(ProviderId providerId, DeviceId deviceId,
                                                DeviceDescription desc) {

            devices.put(deviceId, new DefaultDevice(providerId, deviceId, desc.type(),
                                                    desc.manufacturer(), desc.hwVersion(),
                                                    desc.swVersion(), desc.serialNumber(),
                                                    desc.chassisId(), DefaultAnnotations.builder().build()));
            return null;
        }

        @Override
        public Device getDevice(DeviceId deviceId) {
            return devices.get(deviceId);
        }

    }

    private class MockCoreService extends CoreServiceAdapter {
        @Override
        public ApplicationId registerApplication(String name) {
            return applicationId;
        }
    }

    private class MockDeviceProviderService extends DeviceProviderServiceAdapter {
        DeviceStore store = deviceStore;

        @Override
        public void deviceConnected(DeviceId deviceId, DeviceDescription desc) {
            store.createOrUpdateDevice(ProviderId.NONE, deviceId, desc);
        }
    }

    private class MockSnmpProviderConfig extends SnmpProviderConfig {
        protected SnmpDeviceInfo deviceInfo = new SnmpDeviceInfo(IpAddress.valueOf("1.1.1.1"), 1, "test", "test");

        @Override
        public Set<SnmpProviderConfig.SnmpDeviceInfo> getDevicesInfo() throws ConfigException {
            return ImmutableSet.of(deviceInfo);
        }

    }

    private class MockDriverService extends DriverServiceAdapter {

    }
}