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

package org.onosproject.provider.ospf.topology.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.util.Bandwidth;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.link.LinkServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.ospf.controller.OspfController;
import org.onosproject.ospf.controller.OspfDeviceTed;
import org.onosproject.ospf.controller.OspfLinkListener;
import org.onosproject.ospf.controller.OspfLinkTed;
import org.onosproject.ospf.controller.OspfProcess;
import org.onosproject.ospf.controller.OspfRouter;
import org.onosproject.ospf.controller.OspfRouterListener;
import org.onosproject.ospf.controller.impl.OspfDeviceTedImpl;
import org.onosproject.ospf.controller.impl.OspfLinkTedImpl;
import org.onosproject.ospf.controller.impl.OspfRouterImpl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.junit.Assert.*;

/**
 * Test cases for OSPF topology provider.
 */
public class OspfTopologyProviderTest {

    private final OspfTopologyProvider provider = new OspfTopologyProvider();
    private final TestDeviceRegistry nodeRegistry = new TestDeviceRegistry();
    private final TestLinkRegistry linkRegistry = new TestLinkRegistry();
    private final TestController controller = new TestController();
    private final TestLinkService linkService = new TestLinkService();

    @Before
    public void setUp() throws Exception {
        provider.deviceProviderRegistry = nodeRegistry;
        provider.linkProviderRegistry = linkRegistry;
        provider.controller = controller;
        provider.linkService = linkService;
        provider.activate();
        assertNotNull("provider should be registered", nodeRegistry.provider);
        assertNotNull("listener should be registered", controller.nodeListener);

    }

    @After
    public void tearDown() throws Exception {
        provider.deactivate();
        assertNull("listener should be removed", controller.nodeListener);
        provider.controller = null;
        provider.deviceProviderRegistry = null;
    }

    @Test
    public void triggerProbe() {
        DeviceId deviceId = DeviceId.deviceId("2.2.2.2");
        provider.triggerProbe(deviceId);
    }

    @Test
    public void roleChanged() {
        DeviceId deviceId = DeviceId.deviceId("2.2.2.2");
        provider.roleChanged(deviceId, MastershipRole.MASTER);
    }

    @Test
    public void changePortState() {
        DeviceId deviceId = DeviceId.deviceId("2.2.2.2");
        provider.changePortState(deviceId, PortNumber.portNumber(0), false);
    }

    @Test
    public void isReachable() {
        DeviceId deviceId = DeviceId.deviceId("1.1.1.1");
        provider.isReachable(deviceId);
    }

    /* Validate node is added to the device validating URI, RIB should get updated properly */
    @Test
    public void ospfTopologyProviderTestAddDevice1() {
        int deviceAddCount = 0;
        OspfRouter ospfRouter = new OspfRouterImpl();
        ospfRouter.setDr(false);
        ospfRouter.setOpaque(false);
        ospfRouter.setNeighborRouterId(Ip4Address.valueOf("2.2.2.2"));
        ospfRouter.setInterfaceId(Ip4Address.valueOf("10.10.10.2"));
        ospfRouter.setAreaIdOfInterface(Ip4Address.valueOf("5.5.5.5"));
        ospfRouter.setRouterIp(Ip4Address.valueOf("1.1.1.1"));
        OspfDeviceTed ospfDeviceTed = new OspfDeviceTedImpl();
        ospfDeviceTed.setAbr(false);
        ospfDeviceTed.setAsbr(false);
        ospfRouter.setDeviceTed(ospfDeviceTed);
        OspfLinkTed ospfLinkTed = new OspfLinkTedImpl();
        ospfLinkTed.setMaximumLink(Bandwidth.bps(10));
        ospfLinkTed.setMaxReserved(Bandwidth.bps(20));
        ospfLinkTed.setTeMetric(10);
        OspfRouter ospfRouter1 = new OspfRouterImpl();
        ospfRouter1.setDr(true);
        ospfRouter1.setOpaque(true);
        ospfRouter1.setNeighborRouterId(Ip4Address.valueOf("2.2.2.2"));
        ospfRouter1.setInterfaceId(Ip4Address.valueOf("10.10.10.2"));
        ospfRouter1.setAreaIdOfInterface(Ip4Address.valueOf("5.5.5.5"));
        ospfRouter1.setRouterIp(Ip4Address.valueOf("1.1.1.1"));
        OspfDeviceTed ospfDeviceTed1 = new OspfDeviceTedImpl();
        ospfDeviceTed1.setAbr(false);
        ospfDeviceTed1.setAsbr(false);
        ospfRouter.setDeviceTed(ospfDeviceTed);
        OspfLinkTed ospfLinkTed1 = new OspfLinkTedImpl();
        ospfLinkTed1.setMaximumLink(Bandwidth.bps(10));
        ospfLinkTed1.setMaxReserved(Bandwidth.bps(20));
        ospfLinkTed1.setTeMetric(10);
        for (OspfRouterListener l : controller.nodeListener) {
            l.routerAdded(ospfRouter);

            deviceAddCount = nodeRegistry.connected.size();
            assertTrue(deviceAddCount == 1);
            l.routerRemoved(ospfRouter);
            deviceAddCount = nodeRegistry.connected.size();
            assertTrue(deviceAddCount == 0);
        }
        for (OspfLinkListener l : controller.linkListener) {
            l.addLink(ospfRouter, ospfLinkTed);
            l.deleteLink(ospfRouter, ospfLinkTed);

        }
    }

    @Test
    public void ospfTopologyProviderTestAddDevice2() {
        int deviceAddCount = 0;
        OspfRouter ospfRouter = new OspfRouterImpl();
        ospfRouter.setDr(true);
        ospfRouter.setOpaque(true);
        ospfRouter.setNeighborRouterId(Ip4Address.valueOf("3.3.3.3"));
        ospfRouter.setInterfaceId(Ip4Address.valueOf("10.10.10.3"));
        ospfRouter.setAreaIdOfInterface(Ip4Address.valueOf("6.6.6.6"));
        ospfRouter.setRouterIp(Ip4Address.valueOf("7.7.7.7"));
        OspfDeviceTed ospfDeviceTed = new OspfDeviceTedImpl();
        ospfDeviceTed.setAbr(true);
        ospfDeviceTed.setAsbr(true);
        ospfRouter.setDeviceTed(ospfDeviceTed);
        OspfLinkTed ospfLinkTed = new OspfLinkTedImpl();
        ospfLinkTed.setMaximumLink(Bandwidth.bps(30));
        ospfLinkTed.setMaxReserved(Bandwidth.bps(40));
        ospfLinkTed.setTeMetric(50);
        for (OspfRouterListener l : controller.nodeListener) {
            l.routerAdded(ospfRouter);
            deviceAddCount = nodeRegistry.connected.size();
            assertTrue(deviceAddCount == 1);
            l.routerRemoved(ospfRouter);
            deviceAddCount = nodeRegistry.connected.size();
            assertTrue(deviceAddCount == 0);
        }
    }


    /* Class implement device test registry */
    private class TestDeviceRegistry implements DeviceProviderRegistry {
        DeviceProvider provider;
        Set<DeviceId> connected = new HashSet<>();

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
            public void deviceConnected(DeviceId deviceId, DeviceDescription deviceDescription) {
                connected.add(deviceId);
            }

            @Override
            public void deviceDisconnected(DeviceId deviceId) {
                connected.remove(deviceId);
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

    private class TestDeviceService extends DeviceServiceAdapter {
        private DeviceListener listener;

        @Override
        public void addListener(DeviceListener listener) {
            this.listener = listener;
        }

        @Override
        public Iterable<Device> getDevices() {
            return Collections.emptyList();
        }
    }

    private class TestLinkService extends LinkServiceAdapter {
        private LinkListener listener;

        @Override
        public void addListener(LinkListener listener) {
            this.listener = listener;
        }

        @Override
        public Iterable<Link> getLinks() {
            return Collections.emptyList();
        }
    }

    /* Class implement device test registry */
    private class TestLinkRegistry implements LinkProviderRegistry {
        LinkProvider provider;
        Set<DeviceId> connected = new HashSet<>();

        @Override
        public LinkProviderService register(LinkProvider provider) {
            this.provider = provider;
            return new TestLinkProviderService();
        }

        @Override
        public void unregister(LinkProvider provider) {
        }

        @Override
        public Set<ProviderId> getProviders() {
            return null;
        }

        private class TestLinkProviderService implements LinkProviderService {
            @Override
            public void linkDetected(LinkDescription linkDescription) {
            }

            @Override
            public void linkVanished(LinkDescription linkDescription) {
            }

            @Override
            public void linksVanished(ConnectPoint connectPoint) {
            }

            @Override
            public void linksVanished(DeviceId deviceId) {
            }

            @Override
            public LinkProvider provider() {
                return null;
            }
        }
    }

    /* class implement test controller */
    private class TestController implements OspfController {
        protected Set<OspfRouterListener> nodeListener = new CopyOnWriteArraySet<>();
        protected Set<OspfLinkListener> linkListener = new CopyOnWriteArraySet<>();

        @Override
        public void addRouterListener(OspfRouterListener nodeListener) {
            this.nodeListener.add(nodeListener);
        }

        @Override
        public void removeRouterListener(OspfRouterListener nodeListener) {
            this.nodeListener = null;
        }

        @Override
        public void addLinkListener(OspfLinkListener listener) {
            this.linkListener.add(listener);
        }

        @Override
        public void removeLinkListener(OspfLinkListener listener) {
            this.nodeListener = null;
        }

        @Override
        public void updateConfig(JsonNode processesNode) {
        }


        @Override
        public void deleteConfig(List<OspfProcess> processes, String attribute) {
        }

        @Override
        public Set<OspfRouterListener> listener() {
            return null;
        }

        @Override
        public Set<OspfLinkListener> linkListener() {
            return null;
        }

        @Override
        public List<OspfProcess> getAllConfiguredProcesses() {
            return null;
        }
    }
}