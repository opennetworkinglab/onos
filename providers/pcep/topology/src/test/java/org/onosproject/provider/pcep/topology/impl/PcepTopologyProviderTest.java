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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.onosproject.net.Link.State.ACTIVE;
import static org.onosproject.provider.pcep.topology.impl.PcepTopologyProvider.LABEL_STACK_CAPABILITY;
import static org.onosproject.provider.pcep.topology.impl.PcepTopologyProvider.LSRID;
import static org.onosproject.provider.pcep.topology.impl.PcepTopologyProvider.PCECC_CAPABILITY;
import static org.onosproject.provider.pcep.topology.impl.PcepTopologyProvider.SR_CAPABILITY;

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
import org.onosproject.pcep.controller.ClientCapability;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepClient;
import org.onosproject.pcep.controller.PcepNodeListener;

/**
 * Test for PCEP topology provider.
 */
public class PcepTopologyProviderTest {
    private static final String UNKNOWN = new String("unknown");
    public static ProviderId providerId = new ProviderId("l3", "foo");
    private final PcepClientControllerAdapter clientController = new PcepClientControllerAdapter();
    private final PcepTopologyProvider provider = new PcepTopologyProvider();
    private final MockDeviceRegistry nodeRegistry = new MockDeviceRegistry();
    private final PcepControllerAdapter controller = new PcepControllerAdapter();
    private final MockLinkRegistry linkRegistry = new MockLinkRegistry();
    private final MockDeviceService deviceService = new MockDeviceService();
    private Map<DeviceId, Device> deviceMap = new HashMap<>();

    @Before
    public void startUp() {
        provider.pcepClientController = clientController;
        provider.deviceProviderRegistry = nodeRegistry;
        provider.linkProviderRegistry = linkRegistry;
        provider.controller = controller;
        provider.deviceService = deviceService;
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

    /**
     * Adds the PCEP device and removes it.
     */
    @Test
    public void testPcepTopologyProviderTestAddDevice1() {
        PcepClient pc = clientController.getClient(PccId.pccId(IpAddress.valueOf("1.1.1.1")));
        for (PcepNodeListener l : clientController.pcepNodeListener) {
            pc.setCapability(new ClientCapability(true, true, false, true, true));
            l.addNode(pc);
            assertThat(nodeRegistry.connected.size(), is(1));
            assertThat(deviceMap.keySet().iterator().next(), is(DeviceId.deviceId("l3:1.1.1.1")));
            assertThat(deviceMap.values().iterator().next().annotations().value(LABEL_STACK_CAPABILITY), is("true"));
            assertThat(deviceMap.values().iterator().next().annotations().value(LSRID), is("1.1.1.1"));
            assertThat(deviceMap.values().iterator().next().annotations().value(PCECC_CAPABILITY), is("true"));
            assertThat(deviceMap.values().iterator().next().annotations().value(SR_CAPABILITY), is("true"));

            l.deleteNode(pc.getPccId());
            assertThat(nodeRegistry.connected.size(), is(0));
        }
    }
}
