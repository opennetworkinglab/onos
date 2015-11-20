/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.bgp.controller.BGPCfg;
import org.onosproject.bgp.controller.BGPController;
import org.onosproject.bgp.controller.BGPId;
import org.onosproject.bgp.controller.BGPPeer;
import org.onosproject.bgp.controller.BgpLinkListener;
import org.onosproject.bgp.controller.BgpNodeListener;
import org.onosproject.bgp.controller.BgpPeerManager;
import org.onosproject.bgpio.exceptions.BGPParseException;
import org.onosproject.bgpio.protocol.BGPMessage;
import org.onosproject.bgpio.protocol.linkstate.BGPLinkLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BGPNodeLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BGPNodeLSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLsNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.NodeDescriptors;
import org.onosproject.bgpio.types.AutonomousSystemTlv;
import org.onosproject.bgpio.types.LinkLocalRemoteIdentifiersTlv;
import org.onosproject.bgpio.types.BGPValueType;
import org.onosproject.bgpio.types.RouteDistinguisher;
import org.onosproject.bgpio.util.Constants;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.provider.ProviderId;

public class BgpTopologyProviderTest {

    private static final DeviceId DID1 = DeviceId
            .deviceId("bgp:bgpls://0:direct:0/&=bgpnodelsidentifier%7bnodedescriptors=nodedescriptors%7bdestype=512,"
                    + "%20deslength=4,%20subtlvs=[autonomoussystemtlv%7btype=512,%20length=4,%20asnum=100%7d]%7d%7d");
    private static final DeviceId DID2 = DeviceId
            .deviceId("bgp:bgpls://0:direct:0/&=bgpnodelsidentifier%7bnodedescriptors=nodedescriptors%7bdestype=512,"
                    + "%20deslength=4,%20subtlvs=[autonomoussystemtlv%7btype=512,%20length=4,%20asnum=10%7d]%7d%7d");
    private static final DeviceId DID3 = DeviceId
            .deviceId("bgp:bgpls://direct:0/&=nodedescriptors%7bdestype=512,%20deslength=4,"
                    + "%20subtlvs=[autonomoussystemtlv%7btype=512,%20length=4,%20asnum=100%7d]%7d");
    private final BgpTopologyProvider provider = new BgpTopologyProvider();
    private final TestDeviceRegistry nodeRegistry = new TestDeviceRegistry();
    private final TestLinkRegistry linkRegistry = new TestLinkRegistry();
    private final TestController controller = new TestController();

    @Before
    public void startUp() {
        provider.deviceProviderRegistry = nodeRegistry;
        provider.linkProviderRegistry = linkRegistry;
        provider.controller = controller;
        provider.activate();
        assertNotNull("provider should be registered", nodeRegistry.provider);
        assertNotNull("provider should be registered", linkRegistry.provider);
        assertNotNull("listener should be registered", controller.nodeListener);
    }

    @After
    public void tearDown() {
        provider.deactivate();
        assertNull("listener should be removed", controller.nodeListener);
        provider.controller = null;
        provider.deviceProviderRegistry = null;
        provider.linkProviderRegistry = null;
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
                if (deviceId.equals(DID1)) {
                    connected.add(deviceId);
                }
            }

            @Override
            public void deviceDisconnected(DeviceId deviceId) {
                if (deviceId.equals(DID1)) {
                    connected.remove(deviceId);
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

    /* class implement link test registery */
    private class TestLinkRegistry implements LinkProviderRegistry {
        LinkProvider provider;

        Set<DeviceId> connected = new HashSet<>();

        @Override
        public LinkProviderService register(LinkProvider provider) {
            this.provider = provider;
            return new TestProviderService();
        }

        @Override
        public void unregister(LinkProvider provider) {
        }

        @Override
        public Set<ProviderId> getProviders() {
            return null;
        }

        private class TestProviderService implements LinkProviderService {

            @Override
            public LinkProvider provider() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void linkDetected(LinkDescription linkDescription) {
                if ((linkDescription.src().deviceId().equals(DID3))
                    && (linkDescription.dst().deviceId().equals(DID3))) {
                    connected.add(linkDescription.src().deviceId());
                }
            }

            @Override
            public void linkVanished(LinkDescription linkDescription) {
                if ((linkDescription.src().deviceId().equals(DID3))
                    && (linkDescription.dst().deviceId().equals(DID3))) {
                    connected.remove(linkDescription.src().deviceId());
                }
            }

            @Override
            public void linksVanished(ConnectPoint connectPoint) {
                // TODO Auto-generated method stub

            }

            @Override
            public void linksVanished(DeviceId deviceId) {
                connected.remove(deviceId);
            }

        }
    }

    /* class implement test controller */
    private class TestController implements BGPController {
        protected Set<BgpNodeListener> nodeListener = new CopyOnWriteArraySet<>();
        protected Set<BgpLinkListener> linkListener = new CopyOnWriteArraySet<>();

        @Override
        public void addListener(BgpNodeListener nodeListener) {
            this.nodeListener.add(nodeListener);
        }

        @Override
        public void removeListener(BgpNodeListener nodeListener) {
            this.nodeListener = null;
        }

        @Override
        public void addLinkListener(BgpLinkListener linkListener) {
            this.linkListener.add(linkListener);
        }

        @Override
        public void removeLinkListener(BgpLinkListener linkListener) {
            this.linkListener = null;
        }

        @Override
        public Iterable<BGPPeer> getPeers() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public BGPPeer getPeer(BGPId bgpId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void writeMsg(BGPId bgpId, BGPMessage msg) {
            // TODO Auto-generated method stub

        }

        @Override
        public void processBGPPacket(BGPId bgpId, BGPMessage msg) throws BGPParseException {
            // TODO Auto-generated method stub

        }

        @Override
        public void closeConnectedPeers() {
            // TODO Auto-generated method stub

        }

        @Override
        public BGPCfg getConfig() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int connectedPeerCount() {
            // TODO Auto-generated method stub
            return 0;
        }


        @Override
        public BgpPeerManager peerManager() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<BGPId, BGPPeer> connectedPeers() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<BgpNodeListener> listener() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<BgpLinkListener> linkListener() {
            // TODO Auto-generated method stub
            return null;
        }
    }

    /* Validate node is added to the device validating URI, RIB should get updated properly */
    @Test
    public void bgpTopologyProviderTestAddDevice1() {
        int deviceAddCount = 0;
        LinkedList<BGPValueType> subTlvs;
        subTlvs = new LinkedList<>();
        BGPValueType tlv = new AutonomousSystemTlv(100);
        short deslength = AutonomousSystemTlv.LENGTH;
        short desType = AutonomousSystemTlv.TYPE;

        subTlvs.add(tlv);
        BGPNodeLSIdentifier localNodeDescriptors = new BGPNodeLSIdentifier(new NodeDescriptors(subTlvs, deslength,
                                                                                               desType));
        BGPNodeLSNlriVer4 nodeNlri = new BGPNodeLSNlriVer4(0, (byte) Constants.DIRECT, localNodeDescriptors, false,
                                                           new RouteDistinguisher());

        nodeNlri.setNodeLSIdentifier(localNodeDescriptors);
        for (BgpNodeListener l : controller.nodeListener) {
            l.addNode(nodeNlri);
            deviceAddCount = nodeRegistry.connected.size();
            assertTrue(deviceAddCount == 1);
            l.deleteNode(nodeNlri);
            deviceAddCount = nodeRegistry.connected.size();
            assertTrue(deviceAddCount == 0);
        }
    }

    /* Validate node is not added to the device for invalid URI, RIB count should be zero */
    @Test
    public void bgpTopologyProviderTestAddDevice2() {
        LinkedList<BGPValueType> subTlvs;
        BGPValueType tlv = new AutonomousSystemTlv(10);
        short deslength = AutonomousSystemTlv.LENGTH;
        short desType = AutonomousSystemTlv.TYPE;

        subTlvs = new LinkedList<>();
        subTlvs.add(tlv);
        BGPNodeLSIdentifier localNodeDescriptors = new BGPNodeLSIdentifier(new NodeDescriptors(subTlvs, deslength,
                                                                                               desType));
        BGPNodeLSNlriVer4 nodeNlri = new BGPNodeLSNlriVer4(0, (byte) Constants.DIRECT, localNodeDescriptors, false,
                                                           new RouteDistinguisher());

        nodeNlri.setNodeLSIdentifier(localNodeDescriptors);
        for (BgpNodeListener l : controller.nodeListener) {
            l.addNode(nodeNlri);
            assertTrue("Failed to add device", (nodeRegistry.connected.size() == 0));
        }
    }

    /* Delete node when node does not exist, RIB count should be zero */
    @Test
    public void bgpTopologyProviderTestAddDevice3() {
        LinkedList<BGPValueType> subTlvs;
        BGPValueType tlv = new AutonomousSystemTlv(10);
        short deslength = AutonomousSystemTlv.LENGTH;
        short desType = AutonomousSystemTlv.TYPE;

        subTlvs = new LinkedList<>();
        subTlvs.add(tlv);
        BGPNodeLSIdentifier localNodeDescriptors = new BGPNodeLSIdentifier(new NodeDescriptors(subTlvs, deslength,
                                                                                               desType));
        BGPNodeLSNlriVer4 nodeNlri = new BGPNodeLSNlriVer4(0, (byte) Constants.DIRECT, localNodeDescriptors, false,
                                                           new RouteDistinguisher());

        nodeNlri.setNodeLSIdentifier(localNodeDescriptors);
        for (BgpNodeListener l : controller.nodeListener) {
            l.deleteNode(nodeNlri);
            assertTrue("Failed to add device", (nodeRegistry.connected.size() == 0));
        }
    }

    /* Validate link is added to the device validating URI, RIB should get updated properly */
    @Test
    public void bgpTopologyProviderTestAddLink1() {

        NodeDescriptors localNodeDescriptors;
        NodeDescriptors remoteNodeDescriptors;
        LinkedList<BGPValueType> subTlvs;
        LinkedList<BGPValueType> linkDescriptor = new LinkedList<>();
        BGPValueType tlvLocalRemoteId;
        short deslength = AutonomousSystemTlv.LENGTH;
        short desType = AutonomousSystemTlv.TYPE;

        BGPValueType tlv = new AutonomousSystemTlv(100);
        subTlvs = new LinkedList<>();
        subTlvs.add(tlv);

        localNodeDescriptors = new NodeDescriptors(subTlvs, deslength, desType);
        remoteNodeDescriptors = new NodeDescriptors(subTlvs, deslength, desType);
        tlvLocalRemoteId = new LinkLocalRemoteIdentifiersTlv(1, 2);
        linkDescriptor.add(tlvLocalRemoteId);

        BGPLinkLSIdentifier linkLSIdentifier = new BGPLinkLSIdentifier(localNodeDescriptors, remoteNodeDescriptors,
                                                                       linkDescriptor);
        BgpLinkLsNlriVer4 linkNlri = new BgpLinkLsNlriVer4((byte) Constants.DIRECT, 0, linkLSIdentifier, null, false);

        for (BgpLinkListener l : controller.linkListener) {
            l.addLink(linkNlri);
            assertTrue(linkRegistry.connected.size() == 1);
            l.deleteLink(linkNlri);
            assertTrue(linkRegistry.connected.size() == 0);
        }
    }
}
