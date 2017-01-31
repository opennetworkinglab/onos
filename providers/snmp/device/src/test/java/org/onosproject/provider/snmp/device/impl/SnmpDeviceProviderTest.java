/*
 * Copyright 2016-present Open Networking Foundation
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.net.AbstractProjectableModel;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
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
import org.onosproject.snmp.SnmpDeviceConfig;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onlab.junit.TestTools.assertAfter;

/**
 * Testing class for SnmpDeviceProvider.
 */
public class SnmpDeviceProviderTest {

    public static final int TEST_DURATION = 1500;
    public static final int DELAY = 500;
    private final SnmpDeviceProvider provider = new SnmpDeviceProvider();
    private final SnmpController controller = new SnmpControllerAdapter();
    private final DeviceProviderRegistry providerRegistry = new MockDeviceProviderRegistry();
    private final DeviceService deviceService = new MockDeviceService();
    private final NetworkConfigRegistry netCfgService = new MockNetworkConfigRegistry();
    private final DeviceStore deviceStore = new MockDeviceStore();
    protected CoreService coreService = new MockCoreService();
    private final DeviceProviderService deviceProviderService = new MockDeviceProviderService();
    private final TestApplicationId applicationId = new TestApplicationId("TestAppId");
    private final DeviceId deviceId = DeviceId.deviceId("snmp:1.1.1.1:1");
    private final DeviceId wrongDeviceId = DeviceId.deviceId("snmp:2.2.2.2:2");
    private final Set<ConfigFactory> cfgFactories = new HashSet<>();
    private final Set<NetworkConfigListener> netCfgListeners = new HashSet<>();
    private final NetworkConfigEvent deviceAddedIrrelevantEvent =
            new NetworkConfigEvent(NetworkConfigEvent.Type.CONFIG_ADDED,
                                   null, BasicDeviceConfig.class);
    private final NetworkConfigEvent deviceAddedNewEvent =
            new NetworkConfigEvent(NetworkConfigEvent.Type.CONFIG_ADDED,
                                   deviceId, SnmpDeviceConfig.class);
    private final SnmpDeviceConfig config = new SnmpDeviceConfig();
    //Testing Files
    private final InputStream jsonStream = SnmpDeviceProviderTest.class
            .getResourceAsStream("/device.json");
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String KEY = "snmp";


    @Before
    public void setUp() throws Exception {
        provider.controller = controller;
        provider.providerRegistry = providerRegistry;
        provider.deviceService = deviceService;
        provider.netCfgService = netCfgService;
        provider.deviceStore = deviceStore;
        provider.coreService = coreService;
        JsonNode jsonNode = mapper.readTree(jsonStream);
        ConfigApplyDelegate delegate = new MockDelegate();
        config.init(deviceId, KEY, jsonNode, mapper, delegate);
        provider.activate(null);
    }

    @Test
    public void testActivate() {
        assertEquals("Incorrect provider service", deviceProviderService, provider.providerService);
        assertEquals("Incorrect application id", applicationId, provider.appId);
        assertTrue("Incorrect config factories", cfgFactories.contains(provider.factory));
        assertTrue("Incorrect network config listener", netCfgListeners.contains(provider.cfgLister));


    }

    @Test
    public void testDeactivate() {
        this.addDevice();
        provider.deactivate(null);
        assertAfter(DELAY, TEST_DURATION, () ->
                assertNull("Device should be removed", controller.getDevice(deviceId)));
        assertTrue("Network config factory not removed", cfgFactories.isEmpty());
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
        AbstractProjectableModel.setDriverService(null, new MockDriverService());
        //FIXME this needs sleep
        assertAfter(DELAY, TEST_DURATION, () ->
                assertNotNull("Device should be added to controller", controller.getDevice(deviceId)));
        assertTrue("Device should be reachable", provider.isReachable(deviceId));
    }

    @Test
    public void addDeviceNew() {
        assertTrue("Event should be relevant", provider.cfgLister.isRelevant(deviceAddedNewEvent));
        provider.cfgLister.event(deviceAddedNewEvent);
        AbstractProjectableModel.setDriverService(null, new MockDriverService());
        //FIXME this needs sleep
        assertAfter(DELAY, TEST_DURATION, () ->
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

    private class MockNetworkConfigRegistry extends NetworkConfigRegistryAdapter {

        @Override
        public void registerConfigFactory(ConfigFactory configFactory) {
            cfgFactories.add(configFactory);
        }

        @Override
        public void unregisterConfigFactory(ConfigFactory configFactory) {
            cfgFactories.remove(configFactory);
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
            if (configClass.equals(SnmpDeviceConfig.class)) {
                return (C) config;
            } else {
                return (C) new BasicDeviceConfig();
            }
        }

        @Override
        public <S, C extends Config<S>> Set<S> getSubjects(Class<S> subjectClass, Class<C> configClass) {
            return ImmutableSet.of((S) deviceId);
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

    private class MockDriverService extends DriverServiceAdapter {

    }

    private class MockDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(Config config) {

        }
    }
}
