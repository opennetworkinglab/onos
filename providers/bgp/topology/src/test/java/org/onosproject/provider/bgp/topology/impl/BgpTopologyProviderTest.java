/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.provider.bgp.topology.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.junit.TestUtils.TestUtilsException;
import org.onlab.packet.ChassisId;
import org.onlab.packet.Ip4Address;
import org.onosproject.bgp.controller.BgpLinkListener;
import org.onosproject.bgp.controller.BgpNodeListener;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLsNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4.ProtocolType;
import org.onosproject.bgpio.protocol.linkstate.NodeDescriptors;
import org.onosproject.bgpio.protocol.linkstate.PathAttrNlriDetails;
import org.onosproject.bgpio.types.AutonomousSystemTlv;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.IsIsNonPseudonode;
import org.onosproject.bgpio.types.LinkLocalRemoteIdentifiersTlv;
import org.onosproject.bgpio.types.LinkStateAttributes;
import org.onosproject.bgpio.types.RouteDistinguisher;
import org.onosproject.bgpio.types.attr.BgpAttrNodeFlagBitTlv;
import org.onosproject.bgpio.types.attr.BgpAttrNodeIsIsAreaId;
import org.onosproject.bgpio.types.attr.BgpAttrRouterIdV4;
import org.onosproject.bgpio.types.attr.BgpLinkAttrIgpMetric;
import org.onosproject.bgpio.types.attr.BgpLinkAttrMaxLinkBandwidth;
import org.onosproject.bgpio.types.attr.BgpLinkAttrTeDefaultMetric;
import org.onosproject.bgpio.util.Constants;
import org.onosproject.cluster.NodeId;
import org.onosproject.incubator.net.resource.label.LabelResourceAdminService;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.resource.label.LabelResourcePool;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.net.config.basics.BandwidthCapacity;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
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
import org.onosproject.net.link.LinkServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceAdminService;
import org.onosproject.net.resource.ResourceId;
import org.onosproject.pcep.api.TeLinkConfig;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.onosproject.net.Link.State.ACTIVE;
import static org.onosproject.net.MastershipRole.MASTER;

/**
 * Test for BGP topology provider.
 */
public class BgpTopologyProviderTest {
    private static final DeviceId DID2 = DeviceId.deviceId("l3:rd=0::routinguniverse=0:asn=10");
    private static final String UNKNOWN = "unknown";
    public static ProviderId providerId = new ProviderId("l3", "foo");
    private static final NodeId NODE1 = new NodeId("Master1");

    private final BgpTopologyProvider provider = new BgpTopologyProvider();
    private final TestDeviceRegistry nodeRegistry = new TestDeviceRegistry();
    private final TestLinkRegistry linkRegistry = new TestLinkRegistry();
    private final MockBgpController controller = new MockBgpController();
    private MockDeviceService deviceService = new MockDeviceService();
    private MockLinkService linkService = new MockLinkService();
    private MockMastershipService mastershipService = new MockMastershipService();
    private MockNetConfigRegistryAdapter networkConfigService = new MockNetConfigRegistryAdapter();
    private MockLabelResourceService labelResourceAdminService = new MockLabelResourceService();
    private Map<DeviceId, Device> deviceMap = new HashMap<>();
    private DeviceListener listener;

    @Before
    public void startUp() throws TestUtilsException {
        provider.deviceProviderRegistry = nodeRegistry;
        provider.linkProviderRegistry = linkRegistry;
        provider.controller = controller;
        provider.deviceService = deviceService;
        provider.linkService = linkService;
        provider.labelResourceAdminService = labelResourceAdminService;
        provider.mastershipService = mastershipService;
        provider.networkConfigService = networkConfigService;
        listener = TestUtils.getField(provider, "deviceListener");
        provider.activate();
        assertThat("device provider should be registered", not(nodeRegistry.provider));
        assertThat("link provider should be registered", not(linkRegistry.linkProvider));
        assertThat("node listener should be registered", not(controller.nodeListener));
        assertThat("link listener should be registered", not(controller.linkListener));
    }

    @After
    public void tearDown() {
        provider.deactivate();
        provider.controller = null;
        provider.deviceService = null;
        provider.deviceProviderRegistry = null;
        provider.linkService = null;
        provider.mastershipService = null;
        provider.networkConfigService = null;
        provider.labelResourceAdminService = null;
        assertThat(controller.nodeListener, is(new HashSet<BgpNodeListener>()));
        assertThat(controller.linkListener, is(new HashSet<BgpLinkListener>()));
    }

    private class MockLabelResourceService implements LabelResourceAdminService {

        Map<DeviceId, LabelResourcePool> resourcePool = new HashMap<>();

        @Override
        public boolean createDevicePool(DeviceId deviceId, LabelResourceId beginLabel, LabelResourceId endLabel) {
            LabelResourcePool labelResource = new LabelResourcePool(deviceId.toString(),
                    beginLabel.labelId(),
                    endLabel.labelId());
            if (resourcePool.containsValue(labelResource)) {
                return false;
            }

            resourcePool.put(deviceId, labelResource);
            return true;
        }

        @Override
        public boolean createGlobalPool(LabelResourceId beginLabel, LabelResourceId endLabel) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean destroyDevicePool(DeviceId deviceId) {
            LabelResourcePool devicePool = resourcePool.get(deviceId);

            if (devicePool == null) {
                return false;
            }

            resourcePool.remove(deviceId);
            return true;
        }

        @Override
        public boolean destroyGlobalPool() {
            // TODO Auto-generated method stub
            return false;
        }
    }

    /* Mock test for device service */
    private class MockNetConfigRegistryAdapter extends NetworkConfigRegistryAdapter {
        private ConfigFactory cfgFactory;
        private Map<ConnectPoint, BandwidthCapacity> classConfig = new HashMap<>();
        private Map<LinkKey, TeLinkConfig> teLinkConfig = new HashMap<>();

        public Map<LinkKey, TeLinkConfig> getTeLinkConfig() {
            return teLinkConfig;
        }

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
            } else if (configClass == TeLinkConfig.class) {
                TeLinkConfig linkConfig = new TeLinkConfig();
                teLinkConfig.put((LinkKey) subject, linkConfig);

                JsonNode node = new ObjectNode(new MockJsonNode());
                ObjectMapper mapper = new ObjectMapper();
                ConfigApplyDelegate delegate = new InternalApplyDelegate();
                linkConfig.init((LinkKey) subject, null, node, mapper, delegate);
                return (C) linkConfig;
            }

            return null;
        }

        @Override
        public <S, C extends Config<S>> void removeConfig(S subject, Class<C> configClass) {
            if (configClass == BandwidthCapacity.class) {
                classConfig.remove(subject);
            } else if (configClass == TeLinkConfig.class) {
                teLinkConfig.remove(subject);
            }
        }

        @Override
        public <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass) {
            if (configClass == BandwidthCapacity.class) {
                return (C) classConfig.get(subject);
            } else if (configClass == TeLinkConfig.class) {
                return (C) teLinkConfig.get(subject);
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

    private class MockMastershipService extends MastershipServiceAdapter {
        @Override
        public MastershipRole getLocalRole(DeviceId deviceId) {
            return MASTER;
        }

        @Override
        public boolean isLocalMaster(DeviceId deviceId) {
            return getLocalRole(deviceId) == MASTER;
        }

        @Override
        public NodeId getMasterFor(DeviceId deviceId) {
            return NODE1;
        }
    }

    private class MockResourceAdminService implements ResourceAdminService {
        Map<ResourceId, List<Resource>> registeredRes = new HashMap<>();

        @Override
        public boolean register(List<? extends Resource> resources) {
            for (Resource res : resources) {
                List<Resource> resource = new LinkedList<>();
                resource.add(res);
                if (registeredRes.containsKey(res.id())) {
                    resource.addAll(registeredRes.get(res.id()));
                }
                registeredRes.put(res.id(), resource);
            }
            return true;
        }

        @Override
        public boolean unregister(List<? extends ResourceId> ids) {
            for (ResourceId id : ids) {
                if (registeredRes.containsKey(id)) {
                    registeredRes.remove(id);
                } else {
                    return false;
                }
            }
            return true;
        }
    }

    private class MockLinkService extends LinkServiceAdapter {

        @Override
        public Link getLink(ConnectPoint src, ConnectPoint dst) {
            for (Link link : linkRegistry.links) {
                if (link.src().equals(src) && link.dst().equals(dst)) {
                    return link;
                }
            }
            return null;
        }
    }

    /* Class implement device test registry */
    private class TestDeviceRegistry implements DeviceProviderRegistry {
        DeviceProvider provider;

        Set<DeviceId> connected = new HashSet<>();
        Map<DeviceId, List<PortDescription>> portUpdated = new HashMap<>();

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
                if (!deviceId.equals(DID2)) {
                    connected.add(deviceId);
                    Device device = new DefaultDevice(BgpTopologyProviderTest.providerId, deviceId, Device.Type.ROUTER,
                            UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, new ChassisId(), deviceDescription.annotations());
                    deviceMap.put(deviceId, device);
                }
            }

            @Override
            public void deviceDisconnected(DeviceId deviceId) {
                if (!deviceId.equals(DID2)) {
                    connected.remove(deviceId);
                    deviceMap.remove(deviceId);
                }
            }

            @Override
            public void updatePorts(DeviceId deviceId, List<PortDescription> portDescriptions) {
                portUpdated.put(deviceId, portDescriptions);
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

    /* Class implement device test registry */
    private class TestLinkRegistry implements LinkProviderRegistry {
        LinkProvider linkProvider;
        LinkedList<Link> links = new LinkedList<>();

        @Override
        public LinkProviderService register(LinkProvider provider) {
            this.linkProvider = provider;
            return new TestProviderService();
        }

        @Override
        public void unregister(LinkProvider provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public Set<ProviderId> getProviders() {
            return null;
        }

        private class TestProviderService implements LinkProviderService {

            @Override
            public void linkDetected(LinkDescription linkDescription) {
                links.add(DefaultLink.builder()
                        .src(linkDescription.src())
                        .dst(linkDescription.dst())
                        .state(ACTIVE)
                        .type(linkDescription.type())
                        .providerId(BgpTopologyProviderTest.providerId)
                        .annotations(linkDescription.annotations())
                        .build());
            }

            @Override
            public void linkVanished(LinkDescription linkDescription) {
                links.remove(DefaultLink.builder().src(linkDescription.src())
                        .dst(linkDescription.dst()).state(ACTIVE).type(linkDescription.type())
                        .providerId(BgpTopologyProviderTest.providerId).build());
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

    /* Test class for BGP controller */
    private class MockBgpController extends BgpControllerAdapter {
        protected Set<BgpNodeListener> nodeListener = new CopyOnWriteArraySet<>();
        protected Set<BgpLinkListener> linkListener = new CopyOnWriteArraySet<>();

        @Override
        public void addListener(BgpNodeListener nodeListener) {
            this.nodeListener.add(nodeListener);
        }

        @Override
        public void removeListener(BgpNodeListener nodeListener) {
            this.nodeListener.remove(nodeListener);
        }

        @Override
        public void addLinkListener(BgpLinkListener linkListener) {
            this.linkListener.add(linkListener);
        }

        @Override
        public void removeLinkListener(BgpLinkListener linkListener) {
            this.linkListener.remove(linkListener);
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
     * Validate node is added to the device validating URI, RIB should get updated properly.
     */
    @Test
    public void bgpTopologyProviderTestAddDevice1() {
        LinkedList<BgpValueType> subTlvs = new LinkedList<>();
        BgpValueType tlv = new AutonomousSystemTlv(100);
        short deslength = AutonomousSystemTlv.LENGTH;
        short desType = AutonomousSystemTlv.TYPE;

        subTlvs.add(tlv);
        BgpNodeLSIdentifier localNodeDescriptors = new BgpNodeLSIdentifier(new NodeDescriptors(subTlvs, deslength,
                                                                                               desType));
        BgpNodeLSNlriVer4 nodeNlri = new BgpNodeLSNlriVer4(0, (byte) Constants.DIRECT, localNodeDescriptors, false,
                                                           new RouteDistinguisher());

        PathAttrNlriDetails details = new PathAttrNlriDetails();
        details.setIdentifier(0);
        details.setProtocolID(ProtocolType.DIRECT);
        List<BgpValueType> pathAttributes = new LinkedList<>();
        details.setPathAttribute(pathAttributes);

        for (BgpNodeListener l : controller.nodeListener) {
            l.addNode(nodeNlri, details);
            assertThat(nodeRegistry.connected.size(), is(1));
            l.deleteNode(nodeNlri);
            assertThat(nodeRegistry.connected.size(), is(0));
        }
    }

    /**
     * Validate node is not added to the device for invalid URI, RIB count should be zero.
     */
    @Test
    public void bgpTopologyProviderTestAddDevice2() {
        LinkedList<BgpValueType> subTlvs;
        BgpValueType tlv = new AutonomousSystemTlv(10);
        short deslength = AutonomousSystemTlv.LENGTH;
        short desType = AutonomousSystemTlv.TYPE;

        PathAttrNlriDetails details = new PathAttrNlriDetails();
        details.setIdentifier(0);
        details.setProtocolID(ProtocolType.DIRECT);
        List<BgpValueType> pathAttributes = new LinkedList<>();
        details.setPathAttribute(pathAttributes);

        subTlvs = new LinkedList<>();
        subTlvs.add(tlv);
        BgpNodeLSIdentifier localNodeDescriptors = new BgpNodeLSIdentifier(new NodeDescriptors(subTlvs, deslength,
                                                                                               desType));
        BgpNodeLSNlriVer4 nodeNlri = new BgpNodeLSNlriVer4(0, (byte) Constants.DIRECT, localNodeDescriptors, false,
                                                           new RouteDistinguisher());


        for (BgpNodeListener l : controller.nodeListener) {
            l.addNode(nodeNlri, details);
            assertThat(nodeRegistry.connected.size(), is(0));
        }
    }

    /**
     * Delete node when node does not exist, RIB count should be zero.
     */
    @Test
    public void bgpTopologyProviderTestAddDevice3() {
        LinkedList<BgpValueType> subTlvs;
        BgpValueType tlv = new AutonomousSystemTlv(10);
        short deslength = AutonomousSystemTlv.LENGTH;
        short desType = AutonomousSystemTlv.TYPE;

        subTlvs = new LinkedList<>();
        subTlvs.add(tlv);
        BgpNodeLSIdentifier localNodeDescriptors = new BgpNodeLSIdentifier(new NodeDescriptors(subTlvs, deslength,
                                                                                               desType));
        BgpNodeLSNlriVer4 nodeNlri = new BgpNodeLSNlriVer4(0, (byte) Constants.DIRECT, localNodeDescriptors, false,
                                                           new RouteDistinguisher());

        for (BgpNodeListener l : controller.nodeListener) {
            l.deleteNode(nodeNlri);
            assertThat(nodeRegistry.connected.size(), is(0));
        }
    }


    /**
     * Validate node is added to the device with all device annotations.
     */
    @Test
    public void bgpTopologyProviderTestAddDevice4() {
        LinkedList<BgpValueType> subTlvs = new LinkedList<>();
        BgpValueType tlv = new AutonomousSystemTlv(100);
        short deslength = AutonomousSystemTlv.LENGTH;
        short desType = AutonomousSystemTlv.TYPE;

        subTlvs.add(tlv);
        BgpNodeLSIdentifier localNodeDescriptors = new BgpNodeLSIdentifier(new NodeDescriptors(subTlvs, deslength,
                                                                                               desType));
        BgpNodeLSNlriVer4 nodeNlri = new BgpNodeLSNlriVer4(0, (byte) Constants.DIRECT, localNodeDescriptors, false,
                                                           new RouteDistinguisher());

        PathAttrNlriDetails details = new PathAttrNlriDetails();
        details.setIdentifier(0);
        details.setProtocolID(ProtocolType.DIRECT);
        List<BgpValueType> pathAttributes = new LinkedList<>();
        List<BgpValueType> linkStateAttr = new LinkedList<>();
        tlv = BgpAttrNodeFlagBitTlv.of(true, true, true, false);
        linkStateAttr.add(tlv);
        tlv = BgpAttrNodeIsIsAreaId.of(new byte[] {01, 01, 01, 01});
        linkStateAttr.add(tlv);
        tlv = BgpAttrRouterIdV4.of(Ip4Address.valueOf("1.1.1.1"), LinkStateAttributes.ATTR_NODE_IPV4_LOCAL_ROUTER_ID);
        linkStateAttr.add(tlv);
        pathAttributes.add(new LinkStateAttributes(linkStateAttr));
        details.setPathAttribute(pathAttributes);

        for (BgpNodeListener l : controller.nodeListener) {
            l.addNode(nodeNlri, details);

            assertThat(deviceMap.values().iterator().next().annotations().value(BgpTopologyProvider.ABR_BIT),
                    is("false"));
            assertThat(deviceMap.values().iterator().next().annotations().value(BgpTopologyProvider.EXTERNAL_BIT),
                    is("true"));
            assertThat(deviceMap.values().iterator().next().annotations().value(BgpTopologyProvider.INTERNAL_BIT),
                    is("false"));
            assertThat(deviceMap.values().iterator().next().annotations().value(BgpTopologyProvider.PSEUDO),
                    is("false"));
            assertThat(deviceMap.values().iterator().next().annotations().value(BgpTopologyProvider.AREAID).getBytes(),
                    is(new byte[] {01, 01, 01, 01}));
            assertThat(deviceMap.values().iterator().next().annotations().value(BgpTopologyProvider.LSRID),
                    is("1.1.1.1"));

            assertThat(nodeRegistry.connected.size(), is(1));
            l.deleteNode(nodeNlri);
            assertThat(nodeRegistry.connected.size(), is(0));
        }
    }

    /**
     * Add a link and two devices.
     *
     * @throws BgpParseException while adding a link.
     */
    @Test
    public void bgpTopologyProviderTestAddLink1() throws BgpParseException {
        LinkedList<BgpValueType> localTlvs = new LinkedList<>();
        LinkedList<BgpValueType> remoteTlvs = new LinkedList<>();
        LinkedList<BgpValueType> linkdes = new LinkedList<>();
        BgpValueType tlv = new AutonomousSystemTlv(10);
        short deslength = AutonomousSystemTlv.LENGTH;
        short desType = AutonomousSystemTlv.TYPE;

        localTlvs.add(tlv);
        remoteTlvs.add(tlv);
        tlv = IsIsNonPseudonode.of(new byte[] {20, 20, 20, 20, 00, 20});
        localTlvs.add(tlv);
        tlv = IsIsNonPseudonode.of(new byte[] {30, 30, 30, 30, 00, 30});
        remoteTlvs.add(tlv);
        NodeDescriptors localNode = new NodeDescriptors(localTlvs, deslength, desType);
        NodeDescriptors remoteNode = new NodeDescriptors(remoteTlvs, deslength, desType);
        BgpNodeLSIdentifier localNodeDescriptors = new BgpNodeLSIdentifier(localNode);
        BgpNodeLSNlriVer4 nodeNlri = new BgpNodeLSNlriVer4(0, (byte) Constants.DIRECT, localNodeDescriptors, false,
                                                           new RouteDistinguisher());

        BgpNodeLSIdentifier remoteNodeDescriptors = new BgpNodeLSIdentifier(remoteNode);
        BgpNodeLSNlriVer4 remNodeNlri = new BgpNodeLSNlriVer4(0, (byte) Constants.DIRECT, remoteNodeDescriptors, false,
                                                           new RouteDistinguisher());

        PathAttrNlriDetails details = new PathAttrNlriDetails();
        details.setIdentifier(0);
        details.setProtocolID(ProtocolType.DIRECT);
        List<BgpValueType> pathAttributes = new LinkedList<>();
        details.setPathAttribute(pathAttributes);

        tlv = LinkLocalRemoteIdentifiersTlv.of(99, 100);
        linkdes.add(tlv);
        BgpLinkLSIdentifier linkId = new BgpLinkLSIdentifier(localNode, remoteNode, linkdes);
        BgpLinkLsNlriVer4 linkNlri = new BgpLinkLsNlriVer4((byte) Constants.DIRECT, 0, linkId,
                new RouteDistinguisher(), false);
        for (BgpNodeListener l : controller.nodeListener) {
            l.addNode(nodeNlri, details);
            assertThat(nodeRegistry.connected.size(), is(1));
            l.addNode(remNodeNlri, details);
            assertThat(nodeRegistry.connected.size(), is(2));
        }
        for (BgpLinkListener l : controller.linkListener) {
            l.addLink(linkNlri, details);
            assertThat(linkRegistry.links.size(), is(1));
        }
    }

    /**
     * Add a link and delete a link.
     *
     * @throws BgpParseException while adding or removing the link
     */
    @Test
    public void bgpTopologyProviderTestAddLink2() throws BgpParseException {
        LinkedList<BgpValueType> localTlvs = new LinkedList<>();
        LinkedList<BgpValueType> remoteTlvs = new LinkedList<>();
        LinkedList<BgpValueType> linkdes = new LinkedList<>();
        BgpValueType tlv = new AutonomousSystemTlv(10);
        short deslength = AutonomousSystemTlv.LENGTH;
        short desType = AutonomousSystemTlv.TYPE;

        localTlvs.add(tlv);
        remoteTlvs.add(tlv);
        tlv = IsIsNonPseudonode.of(new byte[] {20, 20, 20, 20, 00, 20});
        localTlvs.add(tlv);
        tlv = IsIsNonPseudonode.of(new byte[] {30, 30, 30, 30, 00, 30});
        remoteTlvs.add(tlv);
        NodeDescriptors localNode = new NodeDescriptors(localTlvs, deslength, desType);
        NodeDescriptors remoteNode = new NodeDescriptors(remoteTlvs, deslength, desType);
        BgpNodeLSIdentifier localNodeDescriptors = new BgpNodeLSIdentifier(localNode);
        BgpNodeLSNlriVer4 nodeNlri = new BgpNodeLSNlriVer4(0, (byte) Constants.DIRECT, localNodeDescriptors, false,
                                                           new RouteDistinguisher());

        BgpNodeLSIdentifier remoteNodeDescriptors = new BgpNodeLSIdentifier(remoteNode);
        BgpNodeLSNlriVer4 remNodeNlri = new BgpNodeLSNlriVer4(0, (byte) Constants.DIRECT, remoteNodeDescriptors, false,
                                                           new RouteDistinguisher());

        PathAttrNlriDetails details = new PathAttrNlriDetails();
        details.setIdentifier(0);
        details.setProtocolID(ProtocolType.DIRECT);
        List<BgpValueType> pathAttributes = new LinkedList<>();
        details.setPathAttribute(pathAttributes);

        tlv = LinkLocalRemoteIdentifiersTlv.of(99, 100);
        linkdes.add(tlv);
        BgpLinkLSIdentifier linkId = new BgpLinkLSIdentifier(localNode, remoteNode, linkdes);
        BgpLinkLsNlriVer4 linkNlri = new BgpLinkLsNlriVer4((byte) Constants.DIRECT, 0, linkId,
                new RouteDistinguisher(), false);
        for (BgpNodeListener l : controller.nodeListener) {
            l.addNode(nodeNlri, details);
            assertThat(nodeRegistry.connected.size(), is(1));
            l.addNode(remNodeNlri, details);
            assertThat(nodeRegistry.connected.size(), is(2));
            l.deleteNode(nodeNlri);
            assertThat(nodeRegistry.connected.size(), is(1));
        }
        for (BgpLinkListener l : controller.linkListener) {
            l.addLink(linkNlri, details);
            assertThat(linkRegistry.links.size(), is(1));
            l.deleteLink(linkNlri);
            assertThat(linkRegistry.links.size(), is(0));
        }
    }

    /**
     * Add a link and delete a link with registering/unregistering bandwidth.
     *
     * @throws BgpParseException while adding or removing the link
     * @throws InterruptedException while registering for bandwidth
     */
    @Test
    public void bgpTopologyProviderTestAddLink3() throws BgpParseException, InterruptedException {
        LinkedList<BgpValueType> localTlvs = new LinkedList<>();
        LinkedList<BgpValueType> remoteTlvs = new LinkedList<>();
        LinkedList<BgpValueType> linkdes = new LinkedList<>();
        BgpValueType tlv = new AutonomousSystemTlv(10);
        short deslength = AutonomousSystemTlv.LENGTH;
        short desType = AutonomousSystemTlv.TYPE;

        localTlvs.add(tlv);
        remoteTlvs.add(tlv);
        tlv = IsIsNonPseudonode.of(new byte[] {20, 20, 20, 20, 00, 20});
        localTlvs.add(tlv);
        tlv = IsIsNonPseudonode.of(new byte[] {30, 30, 30, 30, 00, 30});
        remoteTlvs.add(tlv);
        NodeDescriptors localNode = new NodeDescriptors(localTlvs, deslength, desType);
        NodeDescriptors remoteNode = new NodeDescriptors(remoteTlvs, deslength, desType);
        BgpNodeLSIdentifier localNodeDescriptors = new BgpNodeLSIdentifier(localNode);
        BgpNodeLSNlriVer4 nodeNlri = new BgpNodeLSNlriVer4(0, (byte) Constants.DIRECT, localNodeDescriptors, false,
                                                           new RouteDistinguisher());

        BgpNodeLSIdentifier remoteNodeDescriptors = new BgpNodeLSIdentifier(remoteNode);
        BgpNodeLSNlriVer4 remNodeNlri = new BgpNodeLSNlriVer4(0, (byte) Constants.DIRECT, remoteNodeDescriptors, false,
                                                           new RouteDistinguisher());

        PathAttrNlriDetails details = new PathAttrNlriDetails();
        details.setIdentifier(0);
        details.setProtocolID(ProtocolType.DIRECT);
        List<BgpValueType> pathAttributes = new LinkedList<>();
        details.setPathAttribute(pathAttributes);

        tlv = LinkLocalRemoteIdentifiersTlv.of(99, 100);
        linkdes.add(tlv);
        BgpLinkLSIdentifier linkId = new BgpLinkLSIdentifier(localNode, remoteNode, linkdes);
        BgpLinkLsNlriVer4 linkNlri = new BgpLinkLsNlriVer4((byte) Constants.DIRECT, 0, linkId,
                new RouteDistinguisher(), false);

        for (BgpNodeListener l : controller.nodeListener) {
            l.addNode(nodeNlri, details);
            assertThat(nodeRegistry.connected.size(), is(1));
            l.addNode(remNodeNlri, details);
            assertThat(nodeRegistry.connected.size(), is(2));
            l.deleteNode(remNodeNlri);
            assertThat(nodeRegistry.connected.size(), is(1));
        }

        List<BgpValueType> linkPathAttributes = new LinkedList<>();
        List<BgpValueType> linkStateAttr = new LinkedList<>();
        tlv = BgpLinkAttrIgpMetric.of(10, 4);
        linkStateAttr.add(tlv);
        tlv = BgpLinkAttrTeDefaultMetric.of(20);
        linkStateAttr.add(tlv);
        tlv = BgpLinkAttrMaxLinkBandwidth.of(30, LinkStateAttributes.ATTR_LINK_MAX_RES_BANDWIDTH);
        linkStateAttr.add(tlv);
        linkPathAttributes.add(new LinkStateAttributes(linkStateAttr));
        details.setPathAttribute(linkPathAttributes);

        for (BgpLinkListener l : controller.linkListener) {
            l.addLink(linkNlri, details);
            assertThat(linkRegistry.links.size(), is(1));
            TeLinkConfig config = networkConfigService.getTeLinkConfig().get(LinkKey.linkKey(linkRegistry.links
                            .getFirst().src(), linkRegistry.links.getLast().dst()));

            assertThat(config.igpCost(), is(10));
            assertThat(config.teCost(), is(20));

            l.deleteLink(linkNlri);
            assertThat(linkRegistry.links.size(), is(0));
        }
    }

    /**
     * Invalid link.
     *
     * @throws BgpParseException while adding or deleting a link
     */
    @Test
    public void bgpTopologyProviderTestDeleteLink3() throws BgpParseException {
        LinkedList<BgpValueType> localTlvs = new LinkedList<>();
        LinkedList<BgpValueType> remoteTlvs = new LinkedList<>();
        LinkedList<BgpValueType> linkdes = new LinkedList<>();
        BgpValueType tlv = new AutonomousSystemTlv(10);
        short deslength = AutonomousSystemTlv.LENGTH;
        short desType = AutonomousSystemTlv.TYPE;

        localTlvs.add(tlv);
        remoteTlvs.add(tlv);
        tlv = IsIsNonPseudonode.of(new byte[] {20, 20, 20, 20, 00, 20});
        localTlvs.add(tlv);
        tlv = IsIsNonPseudonode.of(new byte[] {30, 30, 30, 30, 00, 30});
        remoteTlvs.add(tlv);
        NodeDescriptors localNode = new NodeDescriptors(localTlvs, deslength, desType);
        NodeDescriptors remoteNode = new NodeDescriptors(remoteTlvs, deslength, desType);
        BgpNodeLSIdentifier localNodeDescriptors = new BgpNodeLSIdentifier(localNode);
        BgpNodeLSNlriVer4 nodeNlri = new BgpNodeLSNlriVer4(0, (byte) Constants.DIRECT, localNodeDescriptors, false,
                                                           new RouteDistinguisher());

        BgpNodeLSIdentifier remoteNodeDescriptors = new BgpNodeLSIdentifier(remoteNode);
        BgpNodeLSNlriVer4 remNodeNlri = new BgpNodeLSNlriVer4(0, (byte) Constants.DIRECT, remoteNodeDescriptors, false,
                                                           new RouteDistinguisher());

        PathAttrNlriDetails details = new PathAttrNlriDetails();
        details.setIdentifier(0);
        details.setProtocolID(ProtocolType.DIRECT);
        List<BgpValueType> pathAttributes = new LinkedList<>();
        details.setPathAttribute(pathAttributes);

        tlv = LinkLocalRemoteIdentifiersTlv.of(99, 100);
        linkdes.add(tlv);
        BgpLinkLSIdentifier linkId = new BgpLinkLSIdentifier(localNode, remoteNode, linkdes);
        BgpLinkLsNlriVer4 linkNlri = new BgpLinkLsNlriVer4((byte) Constants.DIRECT, 0, linkId,
                new RouteDistinguisher(), false);
        for (BgpNodeListener l : controller.nodeListener) {
            l.addNode(nodeNlri, details);
            l.addNode(remNodeNlri, details);
            assertThat(nodeRegistry.connected.size(), is(2));
            l.deleteNode(nodeNlri);
            assertThat(nodeRegistry.connected.size(), is(1));
        }
        for (BgpLinkListener l : controller.linkListener) {
            l.deleteLink(linkNlri);
            assertThat(linkRegistry.links.size(), is(0));
        }
    }

    /**
     * Add device check label registration is done.
     *
     * @throws BgpParseException while adding a device
     */
    @Test
    public void bgpTopologyProviderDeviceTestLabel1() throws BgpParseException {
        LinkedList<BgpValueType> subTlvs = new LinkedList<>();
        BgpValueType tlv = new AutonomousSystemTlv(100);
        short deslength = AutonomousSystemTlv.LENGTH;
        short desType = AutonomousSystemTlv.TYPE;

        subTlvs.add(tlv);
        BgpNodeLSIdentifier localNodeDescriptors = new BgpNodeLSIdentifier(new NodeDescriptors(subTlvs, deslength,
                                                                                               desType));
        BgpNodeLSNlriVer4 nodeNlri = new BgpNodeLSNlriVer4(0, (byte) Constants.DIRECT, localNodeDescriptors, false,
                                                           new RouteDistinguisher());

        PathAttrNlriDetails details = new PathAttrNlriDetails();
        details.setIdentifier(0);
        details.setProtocolID(ProtocolType.DIRECT);
        List<BgpValueType> pathAttributes = new LinkedList<>();
        List<BgpValueType> linkStateAttributes = new LinkedList<>();
        tlv = BgpAttrRouterIdV4.of(Ip4Address.valueOf("1.1.1.1"), LinkStateAttributes.ATTR_NODE_IPV4_LOCAL_ROUTER_ID);
        linkStateAttributes.add(tlv);
        pathAttributes.add(new LinkStateAttributes(linkStateAttributes));
        details.setPathAttribute(pathAttributes);

        for (BgpNodeListener l : controller.nodeListener) {
            l.addNode(nodeNlri, details);
            assertThat(nodeRegistry.connected.size(), is(1));
        }
        DefaultAnnotations.Builder newBuilder = DefaultAnnotations.builder();

        newBuilder.set("lsrId", "1.1.1.1");

        Device device = new DefaultDevice(BgpTopologyProviderTest.providerId, nodeRegistry.connected.iterator().next(),
                Device.Type.ROUTER, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, new ChassisId(), newBuilder.build());

        DeviceEvent event = new DeviceEvent(DeviceEvent.Type.DEVICE_ADDED, device);
        listener.event(event);
        assertThat(labelResourceAdminService.resourcePool.keySet().size(), is(1));
    }

    /**
     * Add device check label registration is done and delete node destroy label pool.
     *
     * @throws BgpParseException while adding a device
     */
    @Test
    public void bgpTopologyProviderDeviceTestLabel2() throws BgpParseException {
        LinkedList<BgpValueType> subTlvs = new LinkedList<>();
        BgpValueType tlv = new AutonomousSystemTlv(100);
        short deslength = AutonomousSystemTlv.LENGTH;
        short desType = AutonomousSystemTlv.TYPE;

        subTlvs.add(tlv);
        BgpNodeLSIdentifier localNodeDescriptors = new BgpNodeLSIdentifier(new NodeDescriptors(subTlvs, deslength,
                                                                                               desType));
        BgpNodeLSNlriVer4 nodeNlri = new BgpNodeLSNlriVer4(0, (byte) Constants.DIRECT, localNodeDescriptors, false,
                                                           new RouteDistinguisher());

        PathAttrNlriDetails details = new PathAttrNlriDetails();
        details.setIdentifier(0);
        details.setProtocolID(ProtocolType.DIRECT);
        List<BgpValueType> pathAttributes = new LinkedList<>();
        List<BgpValueType> linkStateAttributes = new LinkedList<>();
        tlv = BgpAttrRouterIdV4.of(Ip4Address.valueOf("1.1.1.1"), LinkStateAttributes.ATTR_NODE_IPV4_LOCAL_ROUTER_ID);
        linkStateAttributes.add(tlv);
        pathAttributes.add(new LinkStateAttributes(linkStateAttributes));
        details.setPathAttribute(pathAttributes);

        for (BgpNodeListener l : controller.nodeListener) {
            l.addNode(nodeNlri, details);
            assertThat(nodeRegistry.connected.size(), is(1));

            DefaultAnnotations.Builder newBuilder = DefaultAnnotations.builder();

            newBuilder.set("lsrId", "1.1.1.1");

            Device device = new DefaultDevice(BgpTopologyProviderTest.providerId,
                   nodeRegistry.connected.iterator().next(), Device.Type.ROUTER, UNKNOWN,
                   UNKNOWN, UNKNOWN, UNKNOWN, new ChassisId(), newBuilder.build());

            DeviceEvent event = new DeviceEvent(DeviceEvent.Type.DEVICE_ADDED, device);
            listener.event(event);
            assertThat(labelResourceAdminService.resourcePool.keySet().size(), is(1));

            l.deleteNode(nodeNlri);
            assertThat(nodeRegistry.connected.size(), is(0));
            assertThat(labelResourceAdminService.resourcePool.keySet().size(), is(0));
        }
    }

    /**
     * Add a link register bandwidth and remove link unregister bandwidth.
     *
     * @throws BgpParseException while registering/unregistering bandwidth
     */
    @Test
    public void bgpTopologyProviderDeviceTestLabel3() throws BgpParseException {
        LinkedList<BgpValueType> localTlvs = new LinkedList<>();
        LinkedList<BgpValueType> remoteTlvs = new LinkedList<>();
        LinkedList<BgpValueType> linkdes = new LinkedList<>();
        BgpValueType tlv = new AutonomousSystemTlv(10);
        short deslength = AutonomousSystemTlv.LENGTH;
        short desType = AutonomousSystemTlv.TYPE;

        localTlvs.add(tlv);
        remoteTlvs.add(tlv);
        tlv = IsIsNonPseudonode.of(new byte[] {20, 20, 20, 20, 00, 20});
        localTlvs.add(tlv);
        tlv = IsIsNonPseudonode.of(new byte[] {30, 30, 30, 30, 00, 30});
        remoteTlvs.add(tlv);
        NodeDescriptors localNode = new NodeDescriptors(localTlvs, deslength, desType);
        NodeDescriptors remoteNode = new NodeDescriptors(remoteTlvs, deslength, desType);
        BgpNodeLSIdentifier localNodeDescriptors = new BgpNodeLSIdentifier(localNode);
        BgpNodeLSNlriVer4 nodeNlri = new BgpNodeLSNlriVer4(0, (byte) Constants.DIRECT, localNodeDescriptors, false,
                                                           new RouteDistinguisher());

        BgpNodeLSIdentifier remoteNodeDescriptors = new BgpNodeLSIdentifier(remoteNode);
        BgpNodeLSNlriVer4 remNodeNlri = new BgpNodeLSNlriVer4(0, (byte) Constants.DIRECT, remoteNodeDescriptors, false,
                                                           new RouteDistinguisher());

        PathAttrNlriDetails details = new PathAttrNlriDetails();
        details.setIdentifier(0);
        details.setProtocolID(ProtocolType.DIRECT);
        List<BgpValueType> pathAttributes = new LinkedList<>();
        List<BgpValueType> linkStateAttributes = new LinkedList<>();
        tlv = BgpAttrRouterIdV4.of(Ip4Address.valueOf("1.1.1.1"), LinkStateAttributes.ATTR_NODE_IPV4_LOCAL_ROUTER_ID);
        linkStateAttributes.add(tlv);
        pathAttributes.add(new LinkStateAttributes(linkStateAttributes));
        details.setPathAttribute(pathAttributes);

        tlv = LinkLocalRemoteIdentifiersTlv.of(99, 100);
        linkdes.add(tlv);
        BgpLinkLSIdentifier linkId = new BgpLinkLSIdentifier(localNode, remoteNode, linkdes);
        BgpLinkLsNlriVer4 linkNlri = new BgpLinkLsNlriVer4((byte) Constants.DIRECT, 0, linkId,
                new RouteDistinguisher(), false);

        for (BgpNodeListener l : controller.nodeListener) {
            l.addNode(nodeNlri, details);
            assertThat(nodeRegistry.connected.size(), is(1));
            //Check label resource reserved for that device
            DefaultAnnotations.Builder newBuilder = DefaultAnnotations.builder();

            newBuilder.set("lsrId", "1.1.1.1");

            Device device = new DefaultDevice(BgpTopologyProviderTest.providerId,
                    nodeRegistry.connected.iterator().next(), Device.Type.ROUTER,
                    UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, new ChassisId(), newBuilder.build());

            DeviceEvent event = new DeviceEvent(DeviceEvent.Type.DEVICE_ADDED, device);
            listener.event(event);
            assertThat(labelResourceAdminService.resourcePool.keySet().size(), is(1));
            l.addNode(remNodeNlri, details);
            assertThat(nodeRegistry.connected.size(), is(2));
            l.deleteNode(remNodeNlri);
            assertThat(nodeRegistry.connected.size(), is(1));
            assertThat(labelResourceAdminService.resourcePool.keySet().size(), is(1));
        }

        List<BgpValueType> linkPathAttributes = new LinkedList<>();
        List<BgpValueType> linkStateAttr = new LinkedList<>();
        tlv = BgpLinkAttrIgpMetric.of(10, 4);
        linkStateAttr.add(tlv);
        tlv = BgpLinkAttrTeDefaultMetric.of(20);
        linkStateAttr.add(tlv);
        tlv = BgpLinkAttrMaxLinkBandwidth.of((float) 70 * 1_000_000L,
                LinkStateAttributes.ATTR_LINK_MAX_RES_BANDWIDTH);
        linkStateAttr.add(tlv);
        linkPathAttributes.add(new LinkStateAttributes(linkStateAttr));
        details.setPathAttribute(linkPathAttributes);

        for (BgpLinkListener l : controller.linkListener) {
            l.addLink(linkNlri, details);
            LinkKey linkKey = LinkKey.linkKey(linkRegistry.links.getFirst().src(),
                    linkRegistry.links.getLast().dst());
            assertThat(linkRegistry.links.size(), is(1));
            TeLinkConfig config = networkConfigService.getTeLinkConfig().get(linkKey);

            assertThat(config.igpCost(), is(10));
            assertThat(config.teCost(), is(20));

            ConnectPoint src = new ConnectPoint(
                    DeviceId.deviceId("l3:rd=0::routinguniverse=0:asn=10:isoid=1414.1414.0014"),
                    PortNumber.portNumber(4294967395L));
            ConnectPoint dst = new ConnectPoint(
                    DeviceId.deviceId("l3:rd=0::routinguniverse=0:asn=10:isoid=1e1e.1e1e.001e"),
                    PortNumber.portNumber(4294967396L));

            assertThat(config.maxResvBandwidth(), is(70.0));

            l.deleteLink(linkNlri);
            assertThat(linkRegistry.links.size(), is(0));
            config = networkConfigService.getTeLinkConfig().get(linkKey);
            assertThat(config, is(nullValue()));
        }
    }
}
