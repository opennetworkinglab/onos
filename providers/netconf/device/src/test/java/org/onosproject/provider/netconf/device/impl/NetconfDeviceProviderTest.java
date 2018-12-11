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

package org.onosproject.provider.netconf.device.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.AbstractProjectableModel;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderRegistryAdapter;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceProviderServiceAdapter;
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
import org.onosproject.netconf.config.NetconfDeviceConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.onosproject.provider.netconf.device.impl.NetconfDeviceProvider.APP_NAME;
import static org.onosproject.provider.netconf.device.impl.NetconfDeviceProvider.SCHEME_NAME;

/**
 * Netconf device provider basic test.
 */
public class NetconfDeviceProviderTest {

    private final NetconfDeviceProvider provider = new NetconfDeviceProvider();
    private final NetconfController controller = new MockNetconfController();

    //Provider Mock
    private final DeviceProviderRegistry deviceRegistry = new MockDeviceProviderRegistry();
    private final MockDeviceProviderService providerService = new MockDeviceProviderService();
    private final MockDeviceService deviceService = new MockDeviceService();
    private final MastershipService mastershipService = new MockMastershipService();
    private final Driver driver = new MockDriver();
    private final NetworkConfigRegistry cfgService = new MockNetworkConfigRegistry();
    private final Set<ConfigFactory> cfgFactories = new HashSet<>();
    private final DeviceKeyAdminService deviceKeyAdminService = new DeviceKeyAdminServiceAdapter();
    private final DeviceStore deviceStore = new MockDeviceStore();

    //Class for testing
    private final NetconfDeviceConfig netconfDeviceConfig = new NetconfDeviceConfig();
    private final NetconfDeviceConfig netconfDeviceConfigSshKey = new NetconfDeviceConfig();
    private final NetconfDeviceConfig netconfDeviceConfigEmptyIpv4 = new NetconfDeviceConfig();
    private final NetconfDeviceConfig netconfDeviceConfigEmptyIpv6 = new NetconfDeviceConfig();
    private final NetworkConfigEvent deviceAddedEvent =
            new NetworkConfigEvent(NetworkConfigEvent.Type.CONFIG_ADDED,
                                   DeviceId.deviceId(NETCONF_DEVICE_ID_STRING),
                                   netconfDeviceConfig, null,
                                   NetconfDeviceConfig.class);
    private final NetworkConfigEvent deviceAddedEventTranslated =
            new NetworkConfigEvent(NetworkConfigEvent.Type.CONFIG_ADDED,
                                   DeviceId.deviceId(NETCONF_DEVICE_ID_STRING_OLD),
                                   NetconfDeviceConfig.class);
    private static final String NETCONF_DEVICE_ID_STRING = "netconf:1.1.1.1:830";
    private static final String NETCONF_DEVICE_ID_STRING_OLD = "netconf:1.1.1.2:1";
    private static final String NETCONF_DEVICE_ID_STRING_IPV6 = "netconf:2001:0db8:0000:0000:0000:ff00:0042:8329:830";
    private static final String IP_STRING = "1.1.1.1";
    private static final String IP_STRING_OLD = "1.1.1.2";
    private static final String IP_STRING_IPV6 = "2001:0db8:0000:0000:0000:ff00:0042:8329";
    private static final IpAddress IP = IpAddress.valueOf(IP_STRING);
    private static final IpAddress IP_OLD = IpAddress.valueOf(IP_STRING_OLD);
    private static final IpAddress IP_V6 = IpAddress.valueOf(IP_STRING_IPV6);
    private static final int PORT = 830;
    private static final String TEST = "test";
    private static final int DELAY_DISCOVERY = 500;
    private static final int DELAY_DURATION_DISCOVERY = 3000;
    private static final int PORT_COUNT = 5;
    private final TestDescription deviceDescription = new TestDescription();
    private final Device netconfDevice = new MockDevice(DeviceId.deviceId("netconf:127.0.0.1"));
    private final Device notNetconfDevice = new MockDevice(DeviceId.deviceId("other:127.0.0.1"));

    //Testing Files
    private final InputStream jsonStream = NetconfDeviceProviderTest.class
            .getResourceAsStream("/device.json");
    private final InputStream jsonStreamSshKey = NetconfDeviceProviderTest.class
            .getResourceAsStream("/deviceSshKey.json");

    //Provider related classes
    private CoreService coreService;
    private final ApplicationId appId =
            new DefaultApplicationId(100, APP_NAME);
    private final DeviceDescriptionDiscovery descriptionDiscovery = new TestDescription();
    private final Set<NetworkConfigListener> netCfgListeners = new HashSet<>();
    private final HashMap<DeviceId, Device> devices = new HashMap<>();

    //Controller related classes
    private final Set<NetconfDeviceListener> netconfDeviceListeners = new CopyOnWriteArraySet<>();
    private boolean available = false;
    private boolean firstRequest = true;

    private CountDownLatch deviceAdded;

    @Before
    public void setUp() throws IOException {
        coreService = createMock(CoreService.class);
        expect(coreService.registerApplication(APP_NAME))
                .andReturn(appId).anyTimes();
        replay(coreService);
        provider.coreService = coreService;
        provider.providerRegistry = deviceRegistry;
        provider.mastershipService = mastershipService;
        provider.deviceService = deviceService;
        provider.providerService = providerService;
        provider.cfgService = cfgService;
        provider.controller = controller;
        provider.deviceKeyAdminService = deviceKeyAdminService;
        provider.componentConfigService = new ComponentConfigAdapter();
        AbstractProjectableModel.setDriverService(null, new DriverServiceAdapter());
        provider.activate(null);
        devices.clear();
        available = false;
        firstRequest = true;
        DeviceId subject = DeviceId.deviceId(NETCONF_DEVICE_ID_STRING);
        DeviceId subjectIpv6 = DeviceId.deviceId(NETCONF_DEVICE_ID_STRING_IPV6);
        String key = "netconf";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonStream);
        ConfigApplyDelegate delegate = new MockDelegate();
        netconfDeviceConfig.init(subject, key, jsonNode, mapper, delegate);
        JsonNode jsonNodesshKey = mapper.readTree(jsonStreamSshKey);
        netconfDeviceConfigSshKey.init(subject, key, jsonNodesshKey, mapper, delegate);
        JsonNode jsonNodeEmpty = mapper.createObjectNode();
        netconfDeviceConfigEmptyIpv4.init(subject, key, jsonNodeEmpty, mapper, delegate);
        netconfDeviceConfigEmptyIpv6.init(subjectIpv6, key, jsonNodeEmpty, mapper, delegate);
        deviceAdded = new CountDownLatch(0);
    }

    @Test
    public void activate() throws Exception {
        assertTrue("Provider should be registered", deviceRegistry.getProviders().contains(provider.id()));
        assertEquals("Incorrect device service", deviceService, provider.deviceService);
        assertEquals("Incorrect provider service", providerService, provider.providerService);
        assertTrue("Incorrect config factories", cfgFactories.contains(provider.factory));
        assertNotNull("Device listener should be added", deviceService.listener);
        assertFalse("Thread to connect device should be running",
                    provider.connectionExecutor.isShutdown() || provider.connectionExecutor.isTerminated());
        assertFalse("Scheduled task to update device should be running", provider.scheduledTask.isCancelled());
    }

    @Test
    public void deactivate() throws Exception {
        provider.deactivate();
        assertNull("Device listener should be removed", deviceService.listener);
        assertFalse("Provider should not be registered", deviceRegistry.getProviders().contains(provider.id()));
        assertTrue("Thread to connect device should be shutdown", provider.connectionExecutor.isShutdown());
        assertTrue("Scheduled task to update device should be shutdown", provider.scheduledTask.isCancelled());
        assertNull("Provider service should be null", provider.providerService);
        assertTrue("Network config factories not removed", cfgFactories.isEmpty());
        assertEquals("Controller listener should be removed", 0, netconfDeviceListeners.size());
    }

    @Test
    public void configuration() {
        assertTrue("Configuration should be valid", netconfDeviceConfig.isValid());
        assertThat(netconfDeviceConfig.ip(), is(IP));
        assertThat(netconfDeviceConfig.port(), is(PORT));
        assertThat(netconfDeviceConfig.username(), is(TEST));
        assertThat(netconfDeviceConfig.password(), is(TEST));
        assertThat(netconfDeviceConfigSshKey.sshKey(), is(TEST));
    }

    @Test
    public void configurationDeviceIdIpv4() {
        assertTrue("Configuration should be valid", netconfDeviceConfigEmptyIpv4.isValid());
        assertThat(netconfDeviceConfigEmptyIpv4.ip(), is(IP));
        assertThat(netconfDeviceConfigEmptyIpv4.port(), is(PORT));
        assertThat(netconfDeviceConfigEmptyIpv4.username(), is(StringUtils.EMPTY));
        assertThat(netconfDeviceConfigEmptyIpv4.password(), is(StringUtils.EMPTY));
        assertThat(netconfDeviceConfigEmptyIpv4.sshKey(), is(StringUtils.EMPTY));
    }

    @Test
    public void configurationDeviceIdIpv6() {
        assertTrue("Configuration should be valid", netconfDeviceConfigEmptyIpv6.isValid());
        assertThat(netconfDeviceConfigEmptyIpv6.ip(), is(IP_V6));
        assertThat(netconfDeviceConfigEmptyIpv6.port(), is(PORT));
        assertThat(netconfDeviceConfigEmptyIpv6.username(), is(StringUtils.EMPTY));
        assertThat(netconfDeviceConfigEmptyIpv6.password(), is(StringUtils.EMPTY));
        assertThat(netconfDeviceConfigEmptyIpv6.sshKey(), is(StringUtils.EMPTY));
    }

    @Test
    public void addDeviceNew() throws InterruptedException {
        // expecting 1 device add
        deviceAdded = new CountDownLatch(1);
        assertNotNull(providerService);
        assertTrue("Event should be relevant", provider.cfgListener.isRelevant(deviceAddedEvent));
        available = true;
        assertFalse("Device should not be reachable" + NETCONF_DEVICE_ID_STRING,
                provider.isReachable(DeviceId.deviceId(NETCONF_DEVICE_ID_STRING)));
        devices.clear();
    }

    @Test
    public void testDiscoverPortsAfterDeviceAdded() {
        provider.connectionExecutor = MoreExecutors.newDirectExecutorService();
        prepareMocks(PORT_COUNT);

        deviceService.listener.event(new DeviceEvent(DeviceEvent.Type.DEVICE_ADDED, netconfDevice));
        assertEquals("Ports should be added", PORT_COUNT, providerService.ports.get(netconfDevice.id()).size());

        provider.triggerDisconnect(netconfDevice.id());
        assertEquals("Ports should be removed", 0, providerService.ports.get(netconfDevice.id()).size());
    }

    private void prepareMocks(int count) {
        for (int i = 1; i <= count; i++) {
            deviceDescription.portDescriptions.add(DefaultPortDescription.builder()
                    .withPortNumber(PortNumber.portNumber(i)).isEnabled(true).build());
        }
    }

    private List<Port> createMockPorts(Collection<PortDescription> descs, DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        return descs.stream()
                .map(desc -> new DefaultPort(device, desc.portNumber(), desc.isEnabled(), desc.annotations()))
                .collect(Collectors.toList());
    }

    //TODO: check updates of the device description


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

        @Override
        public void disconnectDevice(DeviceId deviceId, boolean remove) {
            netconfDeviceListeners.forEach(l -> l.deviceRemoved(deviceId));
        }
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

    private class MockDeviceService extends DeviceServiceAdapter {
        DeviceListener listener = null;

        @Override
        public boolean isAvailable(DeviceId deviceId) {
            return true;
        }

        @Override
        public Device getDevice(DeviceId deviceId) {
            if (deviceId.toString().equals(NETCONF_DEVICE_ID_STRING)) {
                return null;
            } else if (deviceId.uri().getScheme().equals(SCHEME_NAME)) {
                return netconfDevice;
            } else {
                return notNetconfDevice;
            }

        }

        @Override
        public List<Port> getPorts(DeviceId deviceId) {
            return createMockPorts(providerService.ports.get(deviceId), deviceId);
        }

        @Override
        public void addListener(DeviceListener listener) {
            this.listener = listener;
        }

        @Override
        public void removeListener(DeviceListener listener) {
            this.listener = null;
        }
    }

    private class MockDeviceProviderService extends DeviceProviderServiceAdapter {

        final Multimap<DeviceId, PortDescription> ports = HashMultimap.create();

        @Override
        public void deviceConnected(DeviceId deviceId, DeviceDescription desc) {
            assertNotNull("DeviceId should be not null", deviceId);
            assertNotNull("DeviceDescription should be not null", desc);
            deviceStore.createOrUpdateDevice(ProviderId.NONE, deviceId, desc);
        }

        @Override
        public void updatePorts(DeviceId deviceId,
                                List<PortDescription> portDescriptions) {
            for (PortDescription p : portDescriptions) {
                ports.put(deviceId, p);
            }
        }

        @Override
        public void deviceDisconnected(DeviceId deviceId) {
            ports.removeAll(deviceId);
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
            deviceAdded.countDown();
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
        NetconfDeviceConfig cfg = null;

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
            if (available) {
                DeviceId did = (DeviceId) subject;
                if (configClass.equals(NetconfDeviceConfig.class)
                        && did.equals(DeviceId.deviceId(NETCONF_DEVICE_ID_STRING))) {
                    return (C) netconfDeviceConfig;
                } else if (configClass.equals(NetconfDeviceConfig.class)
                        && did.equals(DeviceId.deviceId(NETCONF_DEVICE_ID_STRING_OLD))) {
                    if (firstRequest) {
                        firstRequest = false;
                        return null;
                    }
                    return (C) cfg;
                } else {
                    return (C) new BasicDeviceConfig();
                }
            }
            return null;
        }

        @Override
        public <S, C extends Config<S>> C applyConfig(S subject, Class<C> configClass,
                                                      JsonNode json) {
            cfg = new NetconfDeviceConfig();
            ObjectMapper mapper = new ObjectMapper();
            cfg.init((DeviceId) subject, "netconf", mapper.createObjectNode(), mapper, null);
            cfg.setIp(json.get("ip").asText())
                    .setPort(json.get("port").asInt())
                    .setUsername(json.get("username").asText())
                    .setPassword(json.get("password").asText())
                    .setSshKey(json.get("sshkey").asText());
            provider.cfgListener.event(deviceAddedEventTranslated);
            return (C) cfg;
        }

        @Override
        public <S, C extends Config<S>> Set<S> getSubjects(Class<S> subjectClass, Class<C> configClass) {
            Set<S> subjects = new HashSet<>();
            if (available) {
                if (cfg != null) {
                    subjects.add((S) DeviceId.deviceId(NETCONF_DEVICE_ID_STRING_OLD));
                } else {
                    subjects.add((S) DeviceId.deviceId(NETCONF_DEVICE_ID_STRING));
                }
            }
            return subjects;
        }

    }

    private class MockDevice extends DefaultDevice {

        MockDevice(DeviceId id) {
            super(null, id, null, null, null, null, null,
                  null, DefaultAnnotations.EMPTY);
        }

        @Override
        protected Driver locateDriver() {
            return driver;
        }

        @Override
        public Driver driver() {
            return driver;
        }

        @Override
        public <B extends Behaviour> B as(Class<B> projectionClass) {
            return (B) deviceDescription;
        }

        @Override
        public <B extends Behaviour> boolean is(Class<B> projectionClass) {
            return projectionClass.isAssignableFrom(DeviceDescriptionDiscovery.class);
        }
    }

    private class MockDriver extends DriverAdapter {
        @Override
        public <T extends Behaviour> T createBehaviour(DriverHandler handler, Class<T> behaviourClass) {

            return (T) descriptionDiscovery;
        }
    }

    private class TestDescription extends AbstractHandlerBehaviour implements DeviceDescriptionDiscovery {

        final List<PortDescription> portDescriptions = new ArrayList<>();

        @Override
        public DeviceDescription discoverDeviceDetails() {
            return null;
        }

        @Override
        public List<PortDescription> discoverPortDetails() {
            return portDescriptions;
        }

        private void addPortDesc(PortDescription portDescription) {
            portDescriptions.add(portDescription);
        }
    }

    private class MockDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(Config configFile) {
        }
    }

}
