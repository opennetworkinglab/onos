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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.onosproject.net.Link.State.ACTIVE;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ChassisId;
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
import org.onosproject.bgpio.types.RouteDistinguisher;
import org.onosproject.bgpio.util.Constants;
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

/**
 * Test for BGP topology provider.
 */
public class BgpTopologyProviderTest {
    private static final DeviceId DID2 = DeviceId.deviceId("l3:rd=0::routinguniverse=0:asn=10");
    private static final String UNKNOWN = new String("unknown");
    public static ProviderId providerId = new ProviderId("l3", "foo");

    private final BgpTopologyProvider provider = new BgpTopologyProvider();
    private final TestDeviceRegistry nodeRegistry = new TestDeviceRegistry();
    private final TestLinkRegistry linkRegistry = new TestLinkRegistry();
    private final MockBgpController controller = new MockBgpController();
    private MockDeviceService deviceService = new MockDeviceService();
    private Map<DeviceId, Device> deviceMap = new HashMap<>();


    @Before
    public void startUp() {
        provider.deviceProviderRegistry = nodeRegistry;
        provider.linkProviderRegistry = linkRegistry;
        provider.controller = controller;
        provider.deviceService = deviceService;
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
        assertThat(controller.nodeListener, is(new HashSet<BgpNodeListener>()));
        assertThat(controller.linkListener, is(new HashSet<BgpLinkListener>()));
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
                if (!deviceId.equals(DID2)) {
                    connected.add(deviceId);
                    Device device = new DefaultDevice(BgpTopologyProviderTest.providerId, deviceId, Device.Type.ROUTER,
                            UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, new ChassisId());
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

    /* Class implement device test registry */
    private class TestLinkRegistry implements LinkProviderRegistry {
        LinkProvider linkProvider;
        Set<Link> links = new HashSet<>();

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
                links.add(DefaultLink.builder().src(linkDescription.src())
                        .dst(linkDescription.dst()).state(ACTIVE).type(linkDescription.type())
                        .providerId(BgpTopologyProviderTest.providerId).build());
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
}
