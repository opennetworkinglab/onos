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

package org.onosproject.provider.isis.topology.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.util.Bandwidth;
import org.onosproject.isis.controller.IsisController;
import org.onosproject.isis.controller.IsisProcess;
import org.onosproject.isis.controller.impl.topology.DefaultIsisLink;
import org.onosproject.isis.controller.impl.topology.DefaultIsisLinkTed;
import org.onosproject.isis.controller.impl.topology.DefaultIsisRouter;
import org.onosproject.isis.controller.topology.IsisLink;
import org.onosproject.isis.controller.topology.IsisLinkListener;
import org.onosproject.isis.controller.topology.IsisLinkTed;
import org.onosproject.isis.controller.topology.IsisRouter;
import org.onosproject.isis.controller.topology.IsisRouterListener;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.net.config.basics.BandwidthCapacity;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for ISIS topology provider.
 */
public class IsisTopologyProviderTest {

    private final IsisTopologyProvider provider = new IsisTopologyProvider();
    private final TestDeviceRegistry nodeRegistry = new TestDeviceRegistry();
    private final TestLinkRegistry linkRegistry = new TestLinkRegistry();
    private final TestController controller = new TestController();
    private final TestLinkService linkService = new TestLinkService();
    private MockNetConfigRegistryAdapter networkConfigService = new MockNetConfigRegistryAdapter();

    @Before
    public void setUp() throws Exception {
        provider.deviceProviderRegistry = nodeRegistry;
        provider.linkProviderRegistry = linkRegistry;
        provider.networkConfigService = networkConfigService;
        provider.controller = controller;
        provider.linkService = linkService;
        provider.activate();
        assertNotNull("provider should be registered", nodeRegistry.provider);
        assertNotNull("listener should be registered", controller.nodeListener);
    }

    @After
    public void tearDown() throws Exception {
        provider.deactivate();
        provider.controller = null;
        provider.deviceProviderRegistry = null;
        provider.networkConfigService = null;
    }

    @Test
    public void triggerProbe() {
        DeviceId deviceId = DeviceId.deviceId("2929.2929.2929.00-00");
        provider.triggerProbe(deviceId);
    }

    @Test
    public void roleChanged() {
        DeviceId deviceId = DeviceId.deviceId("1111.1111.1111.00-00");
        provider.roleChanged(deviceId, MastershipRole.MASTER);
    }

    @Test
    public void changePortState() {
        DeviceId deviceId = DeviceId.deviceId("2222.2222.2222.00-82");
        provider.changePortState(deviceId, PortNumber.portNumber(168430087), false);
    }

    @Test
    public void isReachable() {
        DeviceId deviceId = DeviceId.deviceId("1010.1010.1111.00-22");
        provider.isReachable(deviceId);
    }


    /* Validate node is added to the device validating URI and should get updated properly */
    @Test
    public void isisTopologyProviderTestAddDevice1() {
        int deviceAddCount = 0;
        IsisRouter isisRouter = new DefaultIsisRouter();
        isisRouter.setSystemId("2929.2929.2929.00");
        isisRouter.setNeighborRouterId(Ip4Address.valueOf("10.10.10.1"));
        isisRouter.setInterfaceId(Ip4Address.valueOf("10.10.10.2"));
        isisRouter.setDis(false);

        for (IsisRouterListener l : controller.nodeListener) {
            l.routerAdded(isisRouter);
            deviceAddCount = nodeRegistry.connected.size();
            assertTrue(deviceAddCount == 1);
            l.routerRemoved(isisRouter);
            deviceAddCount = nodeRegistry.connected.size();
            assertTrue(deviceAddCount == 0);
        }
    }

    @Test
    public void isisTopologyProviderTestAddDevice2() {
        int deviceAddCount = 0;
        IsisRouter isisRouter = new DefaultIsisRouter();
        isisRouter.setSystemId("7777.7777.7777.00");
        isisRouter.setNeighborRouterId(Ip4Address.valueOf("10.10.10.1"));
        isisRouter.setInterfaceId(Ip4Address.valueOf("10.10.10.7"));
        isisRouter.setDis(false);
        IsisRouter isisRouter1 = new DefaultIsisRouter();
        isisRouter1.setSystemId("1111.1111.1111.00");
        isisRouter1.setNeighborRouterId(Ip4Address.valueOf("10.10.10.7"));
        isisRouter1.setInterfaceId(Ip4Address.valueOf("10.10.10.1"));
        isisRouter1.setDis(true);
        for (IsisRouterListener l : controller.nodeListener) {
            l.routerAdded(isisRouter);
            deviceAddCount = nodeRegistry.connected.size();
            assertTrue(deviceAddCount == 1);
            l.routerAdded(isisRouter1);
            deviceAddCount = nodeRegistry.connected.size();
            assertTrue(deviceAddCount == 2);
            l.routerRemoved(isisRouter);
            deviceAddCount = nodeRegistry.connected.size();
            assertTrue(deviceAddCount == 1);
        }
    }

    @Test
    public void isisTopologyProviderTestAddLink() {
        int deviceAddCount = 0;
        IsisRouter isisRouter = new DefaultIsisRouter();
        isisRouter.setSystemId("7777.7777.7777.00");
        isisRouter.setNeighborRouterId(Ip4Address.valueOf("10.10.10.1"));
        isisRouter.setInterfaceId(Ip4Address.valueOf("10.10.10.7"));
        isisRouter.setDis(false);
        IsisRouter isisRouter1 = new DefaultIsisRouter();
        isisRouter1.setSystemId("1111.1111.1111.00");
        isisRouter1.setNeighborRouterId(Ip4Address.valueOf("10.10.10.7"));
        isisRouter1.setInterfaceId(Ip4Address.valueOf("10.10.10.1"));
        isisRouter1.setDis(true);
        IsisLink isisLink = new DefaultIsisLink();
        isisLink.setRemoteSystemId("7777.7777.7777.00");
        isisLink.setLocalSystemId("1111.1111.1111.00");
        isisLink.setInterfaceIp(Ip4Address.valueOf("10.10.10.1"));
        isisLink.setNeighborIp(Ip4Address.valueOf("10.10.10.7"));
        IsisLinkTed isisLinkTed = new DefaultIsisLinkTed();
        isisLinkTed.setTeDefaultMetric(10);
        isisLinkTed.setAdministrativeGroup(5);
        isisLinkTed.setIpv4InterfaceAddress(Ip4Address.valueOf("10.10.10.1"));
        isisLinkTed.setIpv4NeighborAddress(Ip4Address.valueOf("10.10.10.7"));
        isisLinkTed.setMaximumLinkBandwidth(Bandwidth.bps(0));
        isisLinkTed.setMaximumReservableLinkBandwidth(Bandwidth.bps(1.0));
        List<Bandwidth> unresList = new ArrayList<>();
        unresList.add(Bandwidth.bps(0.0));
        unresList.add(Bandwidth.bps(1.0));
        unresList.add(Bandwidth.bps(2.0));
        unresList.add(Bandwidth.bps(3.0));
        isisLinkTed.setUnreservedBandwidth(unresList);
        isisLink.setLinkTed(isisLinkTed);
        for (IsisRouterListener l : controller.nodeListener) {
            l.routerAdded(isisRouter);
            deviceAddCount = nodeRegistry.connected.size();
            assertTrue(deviceAddCount == 1);
            l.routerAdded(isisRouter1);
            deviceAddCount = nodeRegistry.connected.size();
            assertTrue(deviceAddCount == 2);
        }
        for (IsisLinkListener l : controller.linkListener) {
            l.addLink(isisLink);
            l.deleteLink(isisLink);

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
    private class TestController implements IsisController {
        protected Set<IsisRouterListener> nodeListener = new CopyOnWriteArraySet<>();
        protected Set<IsisLinkListener> linkListener = new CopyOnWriteArraySet<>();

        @Override
        public void addRouterListener(IsisRouterListener nodeListener) {
            this.nodeListener.add(nodeListener);
        }

        @Override
        public void removeRouterListener(IsisRouterListener nodeListener) {
            this.nodeListener.remove(nodeListener);
        }

        @Override
        public void addLinkListener(IsisLinkListener listener) {
            this.linkListener.add(listener);
        }

        @Override
        public void removeLinkListener(IsisLinkListener listener) {
            this.linkListener.remove(listener);
        }

        @Override
        public void updateConfig(JsonNode processesNode) {

        }

        @Override
        public List<IsisProcess> allConfiguredProcesses() {
            return null;
        }

        @Override
        public Set<IsisRouterListener> listener() {
            return null;
        }

        @Override
        public Set<IsisLinkListener> linkListener() {
            return null;
        }

    }

    /* Mock test for device service */
    private class MockNetConfigRegistryAdapter extends NetworkConfigRegistryAdapter {
        private ConfigFactory cfgFactory;
        private Map<ConnectPoint, BandwidthCapacity> classConfig = new HashMap<>();

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
            if (configClass == BandwidthCapacity.class) {
                BandwidthCapacity devCap = new BandwidthCapacity();
                classConfig.put((ConnectPoint) subject, devCap);

                JsonNode node = new ObjectNode(new MockJsonNode());
                ObjectMapper mapper = new ObjectMapper();
                ConfigApplyDelegate delegate = new InternalApplyDelegate();
                devCap.init((ConnectPoint) subject, null, node, mapper, delegate);
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
            if (configClass == BandwidthCapacity.class) {
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
            }
        }
    }
}