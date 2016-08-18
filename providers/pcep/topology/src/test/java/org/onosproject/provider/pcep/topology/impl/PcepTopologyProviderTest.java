/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.onosproject.provider.pcep.topology.impl;

import static org.onosproject.net.Link.State.ACTIVE;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.pcep.api.DeviceCapability;
import org.onosproject.pcep.controller.ClientCapability;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepClient;
import org.onosproject.pcep.controller.PcepNodeListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
/**
 * Test for PCEP topology provider.
 */
public class PcepTopologyProviderTest {
    private static final String UNKNOWN = "unknown";
    public static ProviderId providerId = new ProviderId("l3", "foo");
    private final PcepClientControllerAdapter clientController = new PcepClientControllerAdapter();
    private final PcepTopologyProvider provider = new PcepTopologyProvider();
    private final MockDeviceRegistry nodeRegistry = new MockDeviceRegistry();
    private final PcepControllerAdapter controller = new PcepControllerAdapter();
    private final MockLinkRegistry linkRegistry = new MockLinkRegistry();
    private final MockDeviceService deviceService = new MockDeviceService();
    private final MockNetConfigRegistryAdapter netConfigRegistry = new MockNetConfigRegistryAdapter();
    private Map<DeviceId, Device> deviceMap = new HashMap<>();

    @Before
    public void startUp() {
        provider.pcepClientController = clientController;
        provider.deviceProviderRegistry = nodeRegistry;
        provider.linkProviderRegistry = linkRegistry;
        provider.controller = controller;
        provider.deviceService = deviceService;
        provider.netConfigRegistry = netConfigRegistry;
        provider.netConfigService = netConfigRegistry;
        provider.activate();
    }

    @After
    public void tearDown() {
        provider.deactivate();
        provider.deviceProviderRegistry = null;
        provider.pcepClientController = null;
        provider.linkProviderRegistry = null;
        provider.controller = null;
        provider.deviceService = null;
        provider.netConfigRegistry = null;
        provider.netConfigService = null;
    }

    /* Class implement device test registry */
    private class MockLinkRegistry implements LinkProviderRegistry {
        LinkProvider linkProvider;
        Set<Link> links = new HashSet<>();

        @Override
        public LinkProviderService register(LinkProvider provider) {
            this.linkProvider = provider;
            return new MockProviderService();
        }

        @Override
        public void unregister(LinkProvider provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public Set<ProviderId> getProviders() {
            return null;
        }

        private class MockProviderService implements LinkProviderService {

            @Override
            public void linkDetected(LinkDescription linkDescription) {
                links.add(DefaultLink.builder().src(linkDescription.src())
                        .dst(linkDescription.dst()).state(ACTIVE).type(linkDescription.type())
                        .providerId(ProviderId.NONE).build());
            }

            @Override
            public void linkVanished(LinkDescription linkDescription) {
                links.remove(DefaultLink.builder().src(linkDescription.src())
                        .dst(linkDescription.dst()).state(ACTIVE).type(linkDescription.type())
                        .providerId(ProviderId.NONE).build());
            }

            @Override
            public void linksVanished(ConnectPoint connectPoint) {
                // TODO Auto-generated method stub
            }

            @Override
            public void linksVanished(DeviceId deviceId) {
                // TODO Auto-generated method stub
            }

            @Override
            public LinkProvider provider() {
                // TODO Auto-generated method stub
                return null;
            }
        }
    }

    /* Class implement device test registry */
    private class MockDeviceRegistry implements DeviceProviderRegistry {
        DeviceProvider provider;

        Set<DeviceId> connected = new HashSet<>();

        @Override
        public DeviceProviderService register(DeviceProvider provider) {
            this.provider = provider;
            return new MockProviderService();
        }

        @Override
        public void unregister(DeviceProvider provider) {
        }

        @Override
        public Set<ProviderId> getProviders() {
            return null;
        }

        private class MockProviderService implements DeviceProviderService {

            @Override
            public DeviceProvider provider() {
                return null;
            }

            @Override
            public void deviceConnected(DeviceId deviceId, DeviceDescription deviceDescription) {
                connected.add(deviceId);
                Device device = new DefaultDevice(ProviderId.NONE, deviceId, Device.Type.ROUTER, UNKNOWN, UNKNOWN,
                        UNKNOWN, UNKNOWN, new ChassisId(), deviceDescription.annotations());
                deviceMap.put(deviceId, device);
            }

            @Override
            public void deviceDisconnected(DeviceId deviceId) {
                connected.remove(deviceId);
                deviceMap.remove(deviceId);
            }

            @Override
            public void updatePorts(DeviceId deviceId, List<PortDescription> portDescriptions) {
                // TODO Auto-generated method stub
            }

            @Override
            public void portStatusChanged(DeviceId deviceId, PortDescription portDescription) {
                // TODO Auto-generated method stub
            }

            @Override
            public void receivedRoleReply(DeviceId deviceId, MastershipRole requested, MastershipRole response) {
                // TODO Auto-generated method stub
            }

            @Override
            public void updatePortStatistics(DeviceId deviceId, Collection<PortStatistics> portStatistics) {
                // TODO Auto-generated method stub
            }
        }
    }

    /* Mock test for device service */
    private class MockDeviceService extends DeviceServiceAdapter {
        @Override
        public Device getDevice(DeviceId deviceId) {
            return deviceMap.get(deviceId);
        }
    }

    /* Mock test for device service */
    private class MockNetConfigRegistryAdapter extends NetworkConfigRegistryAdapter {
        private ConfigFactory cfgFactory;
        private Map<DeviceId, DeviceCapability> classConfig = new HashMap<>();

        @Override
        public void registerConfigFactory(ConfigFactory configFactory) {
            cfgFactory = configFactory;
        }

        @Override
        public void unregisterConfigFactory(ConfigFactory configFactory) {
            cfgFactory = null;
        }

        @Override
        public <S, C extends Config<S>> C addConfig(S subject, Class<C> configClass) {
            if (configClass == DeviceCapability.class) {
                DeviceCapability devCap = new DeviceCapability();
                classConfig.put((DeviceId) subject, devCap);

                JsonNode node = new ObjectNode(new MockJsonNode());
                ObjectMapper mapper = new ObjectMapper();
                ConfigApplyDelegate delegate = new InternalApplyDelegate();
                devCap.init((DeviceId) subject, null, node, mapper, delegate);
                return (C) devCap;
            }

            return null;
        }

        @Override
        public <S, C extends Config<S>> void removeConfig(S subject, Class<C> configClass) {
            classConfig.remove(subject);
        }

        @Override
        public <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass) {
            if (configClass == DeviceCapability.class) {
                return (C) classConfig.get(subject);
            }
            return null;
        }

        private class MockJsonNode extends JsonNodeFactory {
        }

        // Auxiliary delegate to receive notifications about changes applied to
        // the network configuration - by the apps.
        private class InternalApplyDelegate implements ConfigApplyDelegate {
            @Override
            public void onApply(Config config) {
                //configs.put(config.subject(), config.node());
            }
        }
    }

    /**
     * Adds the PCEP device with SR, label stack and local label capabilities and deletes the device.
     */
    @Test
    public void testPcepTopologyProviderTestAddDevice1() {
        PcepClient pc = clientController.getClient(PccId.pccId(IpAddress.valueOf("1.1.1.1")));
        for (PcepNodeListener l : clientController.pcepNodeListener) {
            pc.setCapability(new ClientCapability(true, true, false, true, true));
            l.addDevicePcepConfig(pc);

            DeviceId pccDeviceId = DeviceId.deviceId(String.valueOf(pc.getPccId().ipAddress()));
            DeviceCapability deviceCap = netConfigRegistry.getConfig(pccDeviceId, DeviceCapability.class);
            assertThat(deviceCap.srCap(), is(true));
            assertThat(deviceCap.labelStackCap(), is(true));
            assertThat(deviceCap.localLabelCap(), is(true));

            l.deleteDevicePcepConfig(pc.getPccId());
            deviceCap = netConfigRegistry.getConfig(pccDeviceId, DeviceCapability.class);
            assertThat(deviceCap, is(nullValue()));
        }
    }

    /**
     * Adds the PCEP device with SR, and local label capabilities and deletes the device.
     */
    @Test
    public void testPcepTopologyProviderTestAddDevice2() {
        PcepClient pc = clientController.getClient(PccId.pccId(IpAddress.valueOf("1.1.1.1")));
        for (PcepNodeListener l : clientController.pcepNodeListener) {
            pc.setCapability(new ClientCapability(true, true, false, false, true));
            l.addDevicePcepConfig(pc);

            DeviceId pccDeviceId = DeviceId.deviceId(String.valueOf(pc.getPccId().ipAddress()));
            DeviceCapability deviceCap = netConfigRegistry.getConfig(pccDeviceId, DeviceCapability.class);
            assertThat(deviceCap.srCap(), is(true));
            assertThat(deviceCap.labelStackCap(), is(false));
            assertThat(deviceCap.localLabelCap(), is(true));

            l.deleteDevicePcepConfig(pc.getPccId());
            deviceCap = netConfigRegistry.getConfig(pccDeviceId, DeviceCapability.class);
            assertThat(deviceCap, is(nullValue()));
        }
    }
}
