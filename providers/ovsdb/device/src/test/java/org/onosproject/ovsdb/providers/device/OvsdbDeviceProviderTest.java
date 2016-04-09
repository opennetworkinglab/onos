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
package org.onosproject.ovsdb.providers.device;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbEventListener;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.ovsdb.controller.OvsdbNodeListener;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Test for ovsdb device provider.
 */
public class OvsdbDeviceProviderTest {
    private final OvsdbDeviceProvider provider = new OvsdbDeviceProvider();
    private final TestDeviceRegistry registry = new TestDeviceRegistry();
    private final TestController controller = new TestController();

    @Before
    public void startUp() {
        provider.providerRegistry = registry;
        provider.controller = controller;
        provider.activate();
        assertNotNull("provider should be registered", registry.provider);
    }

    @After
    public void tearDown() {
        provider.deactivate();
        provider.controller = null;
        provider.providerRegistry = null;
    }

    @Test
    public void triggerProbe() {

    }

    @Test
    public void testNodeAdded() {
        controller.listener.nodeAdded(new OvsdbNodeId(IpAddress
                .valueOf("192.168.202.36"), 5000));
        assertEquals("ovsdb node added", 1, registry.connected.size());
    }

    @Test
    public void testNodeRemoved() {
        controller.listener.nodeAdded(new OvsdbNodeId(IpAddress
                .valueOf("192.168.202.36"), 5000));
        controller.listener.nodeRemoved(new OvsdbNodeId(IpAddress
                .valueOf("192.168.202.36"), 5000));
        assertEquals("ovsdb node removded", 0, registry.connected.size());
    }

    private class TestDeviceRegistry implements DeviceProviderRegistry {
        DeviceProvider provider;

        Set<DeviceId> connected = new HashSet<>();
        Multimap<DeviceId, PortDescription> ports = HashMultimap.create();
        PortDescription descr = null;

        @Override
        public DeviceProviderService register(DeviceProvider provider) {
            this.provider = provider;
            return new TestProviderService();
        }

        @Override
        public void unregister(DeviceProvider provider) {
        }

        @Override
        public Set<ProviderId> getProviders() {
            return null;
        }

        private class TestProviderService implements DeviceProviderService {

            @Override
            public DeviceProvider provider() {
                return null;
            }

            @Override
            public void deviceConnected(DeviceId deviceId,
                                        DeviceDescription deviceDescription) {
                connected.add(deviceId);
            }

            @Override
            public void deviceDisconnected(DeviceId deviceId) {
                connected.remove(deviceId);
                ports.removeAll(deviceId);
            }

            @Override
            public void updatePorts(DeviceId deviceId,
                                    List<PortDescription> portDescriptions) {
                for (PortDescription p : portDescriptions) {
                    ports.put(deviceId, p);
                }
            }

            @Override
            public void portStatusChanged(DeviceId deviceId,
                                          PortDescription portDescription) {
                ports.put(deviceId, portDescription);
                descr = portDescription;
            }

            @Override
            public void receivedRoleReply(DeviceId deviceId,
                                          MastershipRole requested,
                                          MastershipRole response) {
            }

            @Override
            public void updatePortStatistics(DeviceId deviceId,
                                             Collection<PortStatistics> portStatistics) {

            }

        }
    }

    private class TestController implements OvsdbController {
        OvsdbNodeListener listener = null;

        @Override
        public void addNodeListener(OvsdbNodeListener listener) {
            this.listener = listener;
        }

        @Override
        public void removeNodeListener(OvsdbNodeListener listener) {
            this.listener = null;
        }

        @Override
        public void addOvsdbEventListener(OvsdbEventListener listener) {

        }

        @Override
        public void removeOvsdbEventListener(OvsdbEventListener listener) {

        }

        @Override
        public List<OvsdbNodeId> getNodeIds() {
            return null;
        }

        @Override
        public OvsdbClientService getOvsdbClient(OvsdbNodeId nodeId) {
            return null;
        }

        @Override
        public void connect(IpAddress ip, TpPort port) {

        }
    }

}
