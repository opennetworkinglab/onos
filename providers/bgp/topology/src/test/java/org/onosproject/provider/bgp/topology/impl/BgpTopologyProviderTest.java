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
import org.onosproject.bgp.controller.BgpCfg;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.bgp.controller.BgpId;
import org.onosproject.bgp.controller.BgpPeer;
import org.onosproject.bgp.controller.BgpLocalRib;
import org.onosproject.bgp.controller.BgpNodeListener;
import org.onosproject.bgp.controller.BgpPeerManager;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpMessage;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.NodeDescriptors;
import org.onosproject.bgpio.types.AutonomousSystemTlv;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.RouteDistinguisher;
import org.onosproject.bgpio.util.Constants;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
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
    private final TestController controller = new TestController();

    @Before
    public void startUp() {
        provider.deviceProviderRegistry = nodeRegistry;
        provider.controller = controller;
        provider.activate();
        assertNotNull("provider should be registered", nodeRegistry.provider);
        assertNotNull("listener should be registered", controller.nodeListener);
    }

    @After
    public void tearDown() {
        provider.deactivate();
        assertNull("listener should be removed", controller.nodeListener);
        provider.controller = null;
        provider.deviceProviderRegistry = null;
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

    /* class implement test controller */
    private class TestController implements BgpController {
        protected Set<BgpNodeListener> nodeListener = new CopyOnWriteArraySet<>();

        @Override
        public void addListener(BgpNodeListener nodeListener) {
            this.nodeListener.add(nodeListener);
        }

        @Override
        public void removeListener(BgpNodeListener nodeListener) {
            this.nodeListener = null;
        }

        @Override
        public Iterable<BgpPeer> getPeers() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public BgpPeer getPeer(BgpId bgpId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void writeMsg(BgpId bgpId, BgpMessage msg) {
            // TODO Auto-generated method stub

        }

        @Override
        public void processBGPPacket(BgpId bgpId, BgpMessage msg) throws BgpParseException {
            // TODO Auto-generated method stub

        }

        @Override
        public void closeConnectedPeers() {
            // TODO Auto-generated method stub

        }

        @Override
        public BgpCfg getConfig() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int connectedPeerCount() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public BgpLocalRib bgpLocalRibVpn() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public BgpLocalRib bgpLocalRib() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public BgpPeerManager peerManager() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<BgpId, BgpPeer> connectedPeers() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<BgpNodeListener> listener() {
            // TODO Auto-generated method stub
            return null;
        }
    }

    /* Validate node is added to the device validating URI, RIB should get updated properly */
    @Test
    public void bgpTopologyProviderTestAddDevice1() {
        int deviceAddCount = 0;
        LinkedList<BgpValueType> subTlvs;
        subTlvs = new LinkedList<>();
        BgpValueType tlv = new AutonomousSystemTlv(100);
        short deslength = AutonomousSystemTlv.LENGTH;
        short desType = AutonomousSystemTlv.TYPE;

        subTlvs.add(tlv);
        BgpNodeLSIdentifier localNodeDescriptors = new BgpNodeLSIdentifier(new NodeDescriptors(subTlvs, deslength,
                                                                                               desType));
        BgpNodeLSNlriVer4 nodeNlri = new BgpNodeLSNlriVer4(0, (byte) Constants.DIRECT, localNodeDescriptors, false,
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

        nodeNlri.setNodeLSIdentifier(localNodeDescriptors);
        for (BgpNodeListener l : controller.nodeListener) {
            l.addNode(nodeNlri);
            assertTrue("Failed to add device", (nodeRegistry.connected.size() == 0));
        }
    }

    /* Delete node when node does not exist, RIB count should be zero */
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

        nodeNlri.setNodeLSIdentifier(localNodeDescriptors);
        for (BgpNodeListener l : controller.nodeListener) {
            l.deleteNode(nodeNlri);
            assertTrue("Failed to add device", (nodeRegistry.connected.size() == 0));
        }
    }
}
