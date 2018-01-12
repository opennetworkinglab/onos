/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.bgp;

import com.google.common.net.InetAddresses;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.onlab.junit.TestUtils;
import org.onlab.junit.TestUtils.TestUtilsException;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.bgp.controller.BgpCfg;
import org.onosproject.bgp.controller.BgpId;
import org.onosproject.bgp.controller.impl.AdjRibIn;
import org.onosproject.bgp.controller.impl.BgpControllerImpl;
import org.onosproject.bgp.controller.impl.BgpLocalRibImpl;
import org.onosproject.bgp.controller.impl.BgpPeerImpl;
import org.onosproject.bgp.controller.impl.VpnAdjRibIn;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BgpPrefixLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.NodeDescriptors;
import org.onosproject.bgpio.protocol.linkstate.PathAttrNlriDetails;
import org.onosproject.bgpio.protocol.linkstate.PathAttrNlriDetailsLocalRib;
import org.onosproject.bgpio.types.AutonomousSystemTlv;
import org.onosproject.bgpio.types.BgpLSIdentifierTlv;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.FourOctetAsNumCapabilityTlv;
import org.onosproject.bgpio.types.IPReachabilityInformationTlv;
import org.onosproject.bgpio.types.IPv4AddressTlv;
import org.onosproject.bgpio.types.IsIsNonPseudonode;
import org.onosproject.bgpio.types.MultiProtocolExtnCapabilityTlv;
import org.onosproject.bgpio.types.IsIsPseudonode;
import org.onosproject.bgpio.types.RouteDistinguisher;
import org.onosproject.bgpio.util.Constants;

/**
 * Test case for BGPControllerImpl.
 */
@Ignore("Tests are failing due to NPE and due to failure to bind port")
public class BgpControllerImplTest {

    private static final Logger log = LoggerFactory
            .getLogger(BgpControllerImplTest.class);

    private static final String IP_LOOPBACK_ID1 = "127.0.0.1";

    private static final int MESSAGE_TIMEOUT_MS = 3000;
    public byte version;
    public short asNumber;
    public short holdTime;
    public int bgpId = InetAddresses.coerceToInteger(InetAddresses.forString(IP_LOOPBACK_ID1));
    public boolean isLargeAsCapabilitySet = false;
    public LinkedList<BgpValueType> capabilityTlv = new LinkedList<>();

    @Before
    public void setUp() throws Exception {
        peer1 = new BgpPeerTest(version, asNumber,
                holdTime, bgpId, isLargeAsCapabilitySet,
                capabilityTlv);
        peer2 = new BgpPeerTest(version, asNumber,
                holdTime, bgpId, isLargeAsCapabilitySet,
                capabilityTlv);

        bgpControllerImpl = new BgpControllerImpl();

        // NOTE: We use port 0 to bind on any available port
        bgpControllerImpl.controller().setBgpPortNum();
        bgpControllerImpl.activate();

        Channel serverChannel = TestUtils.getField(bgpControllerImpl.controller(),
                                                  "serverChannel");
        SocketAddress socketAddress = serverChannel.getLocalAddress();
        InetSocketAddress inetSocketAddress =
           (InetSocketAddress) socketAddress;
        InetAddress connectToAddress = InetAddresses.forString("127.0.0.1");
        connectToSocket = new InetSocketAddress(connectToAddress,
                       inetSocketAddress.getPort());

        bgpControllerImpl.getConfig().setRouterId("1.1.1.1");
        bgpControllerImpl.getConfig().setAsNumber(200);
        bgpControllerImpl.getConfig().setHoldTime((short) 120);
        bgpControllerImpl.getConfig().setState(BgpCfg.State.IP_AS_CONFIGURED);

        bgpControllerImpl.getConfig().addPeer("127.0.0.1", 200);
        bgpControllerImpl.getConfig().addPeer("127.0.0.9", 200);
        bgpControllerImpl.getConfig().addPeer("127.0.0.33", 200);
        bgpControllerImpl.getConfig().addPeer("127.0.0.10", 200);
        bgpControllerImpl.getConfig().addPeer("127.0.0.20", 200);
        bgpControllerImpl.getConfig().addPeer("127.0.0.30", 200);
        bgpControllerImpl.getConfig().addPeer("127.0.0.40", 200);
        bgpControllerImpl.getConfig().addPeer("127.0.0.50", 200);
        bgpControllerImpl.getConfig().addPeer("127.0.0.60", 200);
        bgpControllerImpl.getConfig().addPeer("127.0.0.70", 200);
        bgpControllerImpl.getConfig().addPeer("127.0.0.80", 200);
        bgpControllerImpl.getConfig().addPeer("127.0.0.90", 200);
        bgpControllerImpl.getConfig().addPeer("127.0.0.91", 200);
        bgpControllerImpl.getConfig().addPeer("127.0.0.92", 200);
        bgpControllerImpl.getConfig().addPeer("127.0.0.99", 200);
        bgpControllerImpl.getConfig().addPeer("127.0.0.94", 200);
        bgpControllerImpl.getConfig().addPeer("127.0.0.95", 200);
        bgpControllerImpl.getConfig().addPeer("127.0.0.35", 200);
    }

    @After
    public void tearDown() throws Exception {
        bgpControllerImpl.deactivate();
        bgpControllerImpl = null;
    }

    private BgpControllerImpl bgpControllerImpl;

    BgpPeerTest peer1;
    BgpPeerTest peer2;
    // The socket that the remote peers should connect to
    private InetSocketAddress connectToSocket;

    @Test
    public void bgpOpenMessageTest1() throws InterruptedException {
        peer1.peerChannelHandler.asNumber = 200;
        peer1.peerChannelHandler.version = 4;
        peer1.peerChannelHandler.holdTime = 120;
        peer1.connect(connectToSocket);
        boolean result;
        result = peer1.peerFrameDecoder.receivedOpenMessageLatch.await(
            MESSAGE_TIMEOUT_MS,
            TimeUnit.MILLISECONDS);
        assertThat(result, is(true));
        result = peer1.peerFrameDecoder.receivedKeepaliveMessageLatch.await(
            MESSAGE_TIMEOUT_MS,
            TimeUnit.MILLISECONDS);
        assertThat(result, is(true));
    }

    @Test
    public void bgpOpenMessageTest2() throws InterruptedException {
        // Open message with as number which is not configured at peer
        peer1.peerChannelHandler.asNumber = 500;
        peer1.peerChannelHandler.version = 4;
        peer1.peerChannelHandler.holdTime = 120;
        peer1.connect(connectToSocket);

        boolean result;
        result = peer1.peerFrameDecoder.receivedNotificationMessageLatch.await(
            MESSAGE_TIMEOUT_MS,
            TimeUnit.MILLISECONDS);
        assertThat(result, is(true));
    }

    @Test
    public void bgpOpenMessageTest3() throws InterruptedException {
        // Open message with invalid hold time value
        peer1.peerChannelHandler.asNumber = 200;
        peer1.peerChannelHandler.version = 4;
        peer1.peerChannelHandler.holdTime = 1;
        peer1.connect(connectToSocket);

        boolean result;
        result = peer1.peerFrameDecoder.receivedNotificationMessageLatch.await(
            MESSAGE_TIMEOUT_MS,
            TimeUnit.MILLISECONDS);
        assertThat(result, is(true));
    }

    @Test
    public void bgpOpenMessageTest4() throws InterruptedException {
        // Open message with invalid as number
        peer1.peerChannelHandler.asNumber = 200;
        peer1.peerChannelHandler.version = 4;
        peer1.peerChannelHandler.holdTime = 120;
        peer1.peerChannelHandler.isLargeAsCapabilitySet = true;
        BgpValueType tempTlv = new FourOctetAsNumCapabilityTlv(766545);
        peer1.peerChannelHandler.capabilityTlv.add(tempTlv);
        peer1.connect(connectToSocket);

        boolean result;
        result = peer1.peerFrameDecoder.receivedNotificationMessageLatch.await(
            MESSAGE_TIMEOUT_MS,
            TimeUnit.MILLISECONDS);
        assertThat(result, is(true));
    }

    @Test
    public void bgpOpenMessageTest5() throws InterruptedException {
        // Open message with LS capability
        short afi = 16388;
        byte res = 0;
        byte safi = 71;
        peer1.peerChannelHandler.asNumber = 200;
        peer1.peerChannelHandler.version = 4;
        peer1.peerChannelHandler.holdTime = 120;
        bgpControllerImpl.getConfig().setLsCapability(true);
        BgpValueType tempTlv1 = new MultiProtocolExtnCapabilityTlv(afi, res, safi);
        peer1.peerChannelHandler.capabilityTlv.add(tempTlv1);
        peer1.connect(connectToSocket);

        boolean result;
        result = peer1.peerFrameDecoder.receivedOpenMessageLatch.await(
            MESSAGE_TIMEOUT_MS,
            TimeUnit.MILLISECONDS);
        assertThat(result, is(true));
        result = peer1.peerFrameDecoder.receivedKeepaliveMessageLatch.await(
            MESSAGE_TIMEOUT_MS,
            TimeUnit.MILLISECONDS);
        assertThat(result, is(true));
    }

    @Test
    public void bgpOpenMessageTest6() throws InterruptedException {
        // Open message with as4 capability
        peer1.peerChannelHandler.asNumber = 200;
        peer1.peerChannelHandler.version = 4;
        peer1.peerChannelHandler.holdTime = 120;
        peer1.peerChannelHandler.isLargeAsCapabilitySet = true;
        bgpControllerImpl.getConfig().setLargeASCapability(true);
        BgpValueType tempTlv = new FourOctetAsNumCapabilityTlv(200);
        peer1.peerChannelHandler.capabilityTlv.add(tempTlv);
        peer1.connect(connectToSocket);

        boolean result;
        result = peer1.peerFrameDecoder.receivedOpenMessageLatch.await(
            MESSAGE_TIMEOUT_MS,
            TimeUnit.MILLISECONDS);
        assertThat(result, is(true));
        result = peer1.peerFrameDecoder.receivedKeepaliveMessageLatch.await(
            MESSAGE_TIMEOUT_MS,
            TimeUnit.MILLISECONDS);
        assertThat(result, is(true));

        result = peer1.peerFrameDecoder.receivedKeepaliveMessageLatch.await(
                MESSAGE_TIMEOUT_MS,
                TimeUnit.MILLISECONDS);
            assertThat(result, is(true));
    }

    @Test
    public void bgpOpenMessageTest7() throws InterruptedException {
        // Open message with both LS capability and as4 capability
        short afi = 16388;
        byte res = 0;
        byte safi = 71;
        peer1.peerChannelHandler.asNumber = 200;
        peer1.peerChannelHandler.version = 4;
        peer1.peerChannelHandler.holdTime = 120;

        peer1.peerChannelHandler.isLargeAsCapabilitySet = true;
        bgpControllerImpl.getConfig().setLargeASCapability(true);
        BgpValueType tempTlv = new FourOctetAsNumCapabilityTlv(200);
        peer1.peerChannelHandler.capabilityTlv.add(tempTlv);

        bgpControllerImpl.getConfig().setLsCapability(true);
        BgpValueType tempTlv1 = new MultiProtocolExtnCapabilityTlv(afi, res, safi);
        peer1.peerChannelHandler.capabilityTlv.add(tempTlv1);
        peer1.connect(connectToSocket);

        boolean result;
        result = peer1.peerFrameDecoder.receivedOpenMessageLatch.await(
            MESSAGE_TIMEOUT_MS,
            TimeUnit.MILLISECONDS);
        assertThat(result, is(true));
    }

    @Test
    public void bgpOpenMessageTest8() throws InterruptedException {
        // Open message with route policy distribution capability
        short afi = Constants.AFI_FLOWSPEC_RPD_VALUE;
        byte res = 0;
        byte safi = Constants.SAFI_FLOWSPEC_RPD_VALUE;
        peer1.peerChannelHandler.asNumber = 200;
        peer1.peerChannelHandler.version = 4;
        peer1.peerChannelHandler.holdTime = 120;

        bgpControllerImpl.getConfig().setFlowSpecRpdCapability(true);
        BgpValueType tempTlv1 = new MultiProtocolExtnCapabilityTlv(afi, res, safi);
        peer1.peerChannelHandler.capabilityTlv.add(tempTlv1);

        peer1.connect(connectToSocket);

        boolean result;
        result = peer1.peerFrameDecoder.receivedOpenMessageLatch.await(
            MESSAGE_TIMEOUT_MS,
            TimeUnit.MILLISECONDS);
        assertThat(result, is(true));
    }

    /**
     * Peer1 has Node NLRI (MpReach).
     */
    @Test
    public void testBgpUpdateMessage1() throws InterruptedException {
        // Initiate the connections
        peer1.peerChannelHandler.asNumber = 200;
        peer1.peerChannelHandler.version = 4;
        peer1.peerChannelHandler.holdTime = 120;

        short afi = 16388;
        byte res = 0;
        byte safi = 71;

        bgpControllerImpl.getConfig().setLsCapability(true);
        BgpValueType tempTlv1 = new MultiProtocolExtnCapabilityTlv(afi, res, safi);
        peer1.peerChannelHandler.capabilityTlv.add(tempTlv1);
        peer1.connectFrom(connectToSocket, new InetSocketAddress("127.0.0.9", 0));
        TimeUnit.MILLISECONDS.sleep(1000);

        //Get peer1
        BgpId bgpId = new BgpId(IpAddress.valueOf("127.0.0.9"));
        BgpPeerImpl peer = (BgpPeerImpl) bgpControllerImpl.getPeer(bgpId);

        LinkedList<BgpValueType> subTlvs = new LinkedList<>();
        BgpValueType tlv = AutonomousSystemTlv.of(2478);
        subTlvs.add(tlv);
        tlv = BgpLSIdentifierTlv.of(33686018);
        subTlvs.add(tlv);
        NodeDescriptors nodeDes = new NodeDescriptors(subTlvs, (short) 0x10, (short) 256);
        BgpNodeLSIdentifier key = new BgpNodeLSIdentifier(nodeDes);
        AdjRibIn adj = peer.adjRib();

        //In Adj-RIB, nodeTree should contains specified key
        assertThat(adj.nodeTree().containsKey(key), is(true));

        BgpLocalRibImpl obj = (BgpLocalRibImpl) bgpControllerImpl.bgpLocalRib();
        //In Local-RIB, nodeTree should contains specified key
        assertThat(obj.nodeTree().containsKey(key), is(true));
    }

    /**
     * Peer1 has Node NLRI (MpReach) and Peer2 has Node NLRI with same MpReach and MpUnReach.
     */
    @Test
    public void testBgpUpdateMessage2() throws InterruptedException, TestUtilsException {
        // Initiate the connections
        peer1.peerChannelHandler.asNumber = 200;
        peer1.peerChannelHandler.version = 4;
        peer1.peerChannelHandler.holdTime = 120;
        short afi = 16388;
        byte res = 0;
        byte safi = 71;

        bgpControllerImpl.getConfig().setLsCapability(true);
        BgpValueType tempTlv1 = new MultiProtocolExtnCapabilityTlv(afi, res, safi);
        peer2.peerChannelHandler.capabilityTlv.add(tempTlv1);
        Channel channel = peer1.connectFrom(connectToSocket, new InetSocketAddress("127.0.0.95", 0));
        TimeUnit.MILLISECONDS.sleep(1000);

        //Get peer1
        BgpId bgpId = new BgpId(IpAddress.valueOf("127.0.0.95"));
        BgpPeerImpl peer = (BgpPeerImpl) bgpControllerImpl.getPeer(bgpId);

        LinkedList<BgpValueType> subTlvs = new LinkedList<>();
        BgpValueType tlv = AutonomousSystemTlv.of(2478);
        subTlvs.add(tlv);
        tlv = BgpLSIdentifierTlv.of(33686018);
        subTlvs.add(tlv);
        NodeDescriptors nodeDes = new NodeDescriptors(subTlvs, (short) 0x10, (short) 256);
        BgpNodeLSIdentifier key = new BgpNodeLSIdentifier(nodeDes);
        TimeUnit.MILLISECONDS.sleep(500);
        AdjRibIn adj = peer.adjRib();

        //In Adj-RIB, nodeTree should contains specified key
        assertThat(adj.nodeTree().containsKey(key), is(true));

        BgpLocalRibImpl obj = (BgpLocalRibImpl) bgpControllerImpl.bgpLocalRib();
        //In Local-RIB, nodeTree should contains specified key
        assertThat(obj.nodeTree().containsKey(key), is(true));

        peer2.peerChannelHandler.asNumber = 200;
        peer2.peerChannelHandler.version = 4;
        peer2.peerChannelHandler.holdTime = 120;

        bgpControllerImpl.getConfig().setLsCapability(true);
        tempTlv1 = new MultiProtocolExtnCapabilityTlv(afi, res, safi);
        peer2.peerChannelHandler.capabilityTlv.add(tempTlv1);
        peer2.connectFrom(connectToSocket, new InetSocketAddress("127.0.0.70", 0));
        TimeUnit.MILLISECONDS.sleep(1000);

        //Get peer2
        bgpId = new BgpId(IpAddress.valueOf("127.0.0.70"));
        peer = (BgpPeerImpl) bgpControllerImpl.getPeer(bgpId);
        TimeUnit.MILLISECONDS.sleep(200);
        adj = peer.adjRib();

        //In Adj-RIB, nodetree should be empty
        assertThat(adj.nodeTree().isEmpty(), is(true));

        //Disconnect peer1
        channel.disconnect();
        channel.close();

        obj = (BgpLocalRibImpl) bgpControllerImpl.bgpLocalRib();
        TimeUnit.MILLISECONDS.sleep(200);
        //In Local-RIB, nodetree should be empty
        assertThat(obj.nodeTree().isEmpty(), is(true));
    }

    /**
     * Peer1 has Link NLRI (MpReach).
     */
    @Test
    public void testBgpUpdateMessage3() throws InterruptedException, TestUtilsException {
        // Initiate the connections
        peer1.peerChannelHandler.asNumber = 200;
        peer1.peerChannelHandler.version = 4;
        peer1.peerChannelHandler.holdTime = 120;

        short afi = 16388;
        byte res = 0;
        byte safi = 71;

        bgpControllerImpl.getConfig().setLsCapability(true);
        BgpValueType tempTlv1 = new MultiProtocolExtnCapabilityTlv(afi, res, safi);
        peer1.peerChannelHandler.capabilityTlv.add(tempTlv1);
        peer1.connectFrom(connectToSocket, new InetSocketAddress("127.0.0.10", 0));
        TimeUnit.MILLISECONDS.sleep(1000);

        //Get peer1
        BgpId bgpId = new BgpId(IpAddress.valueOf("127.0.0.10"));
        BgpPeerImpl peer = (BgpPeerImpl) bgpControllerImpl.getPeer(bgpId);

        LinkedList<BgpValueType> localNodeSubTlvs = new LinkedList<>();
        LinkedList<BgpValueType> remoteNodeSubTlvs = new LinkedList<>();
        BgpValueType tlv = AutonomousSystemTlv.of(2222);
        localNodeSubTlvs.add(tlv);
        remoteNodeSubTlvs.add(tlv);
        tlv = BgpLSIdentifierTlv.of(33686018);
        localNodeSubTlvs.add(tlv);
        remoteNodeSubTlvs.add(tlv);
        byte[] isoNodeID = new byte[] {0x19, 0x00, (byte) 0x95, 0x02, 0x50, 0x21 };
        tlv = IsIsPseudonode.of(isoNodeID, (byte) 0x03);
        localNodeSubTlvs.add(tlv);
        isoNodeID = new byte[] {0x19, 0x00, (byte) 0x95, 0x02, 0x50, 0x21 };
        tlv = IsIsNonPseudonode.of(isoNodeID);
        remoteNodeSubTlvs.add(tlv);
        NodeDescriptors localNodeDes = new NodeDescriptors(localNodeSubTlvs, (short) 0x1b, (short) 256);
        NodeDescriptors remoteNodeDes = new NodeDescriptors(remoteNodeSubTlvs, (short) 0x1a, (short) 0x101);
        LinkedList<BgpValueType> linkDescriptor = new LinkedList<>();
        tlv = IPv4AddressTlv.of(Ip4Address.valueOf("2.2.2.2"), (short) 0x103);
        linkDescriptor.add(tlv);

        BgpLinkLSIdentifier key = new BgpLinkLSIdentifier(localNodeDes, remoteNodeDes, linkDescriptor);
        TimeUnit.MILLISECONDS.sleep(200);
        AdjRibIn adj = peer.adjRib();

        //In Adj-RIB, linkTree should contain specified key
        assertThat(adj.linkTree().containsKey(key), is(true));

        BgpLocalRibImpl obj = (BgpLocalRibImpl) bgpControllerImpl.bgpLocalRib();
        TimeUnit.MILLISECONDS.sleep(200);

        //In Local-RIB, linkTree should contain specified key
        assertThat(obj.linkTree().containsKey(key), is(true));
    }

    /**
     * Peer1 has Node NLRI and Peer2 has Node NLRI with different MpReach and MpUnReach with VPN.
     */
    @Test
    public void testBgpUpdateMessage4() throws InterruptedException {
        // Initiate the connections
        peer1.peerChannelHandler.asNumber = 200;
        peer1.peerChannelHandler.version = 4;
        peer1.peerChannelHandler.holdTime = 120;

        short afi = 16388;
        byte res = 0;
        byte safi = (byte) 0x80;

        bgpControllerImpl.getConfig().setLsCapability(true);
        BgpValueType tempTlv1 = new MultiProtocolExtnCapabilityTlv(afi, res, safi);
        peer1.peerChannelHandler.capabilityTlv.add(tempTlv1);
        Channel channel =  peer1.connectFrom(connectToSocket, new InetSocketAddress("127.0.0.35", 0));
        TimeUnit.MILLISECONDS.sleep(1000);

        //Get peer1
        IpAddress ipAddress = IpAddress.valueOf("127.0.0.35");
        BgpId bgpId = new BgpId(ipAddress);
        BgpPeerImpl peer = (BgpPeerImpl) bgpControllerImpl.getPeer(bgpId);
        LinkedList<BgpValueType> subTlvs1 = new LinkedList<>();

        LinkedList<BgpValueType> subTlvs = new LinkedList<>();
        BgpValueType tlv = AutonomousSystemTlv.of(2478);
        subTlvs.add(tlv);
        tlv = BgpLSIdentifierTlv.of(33686018);
        subTlvs.add(tlv);

        NodeDescriptors nodeDes = new NodeDescriptors(subTlvs, (short) 0x10, (short) 256);
        BgpNodeLSIdentifier key = new BgpNodeLSIdentifier(nodeDes);
        RouteDistinguisher rd = new RouteDistinguisher((long) 0x0A);
        VpnAdjRibIn vpnAdj = peer.vpnAdjRib();

        //In Adj-RIB, vpnNodeTree should contain rd
        assertThat(vpnAdj.vpnNodeTree().containsKey(rd), is(true));

        Map<BgpNodeLSIdentifier, PathAttrNlriDetails> treeValue = vpnAdj.vpnNodeTree().get(rd);
        //In Adj-RIB, vpnNodeTree should contain rd key which contains specified value
        assertThat(treeValue.containsKey(key), is(true));

        BgpLocalRibImpl obj = (BgpLocalRibImpl) bgpControllerImpl.bgpLocalRibVpn();
        //In Local-RIB, vpnNodeTree should contain rd
        assertThat(obj.vpnNodeTree().containsKey(rd), is(true));

        Map<BgpNodeLSIdentifier, PathAttrNlriDetailsLocalRib> value = obj.vpnNodeTree().get(rd);
        //In Local-RIB, vpnNodeTree should contain rd key which contains specified value
        assertThat(value.containsKey(key), is(true));

        peer2.peerChannelHandler.asNumber = 200;
        peer2.peerChannelHandler.version = 4;
        peer2.peerChannelHandler.holdTime = 120;

        bgpControllerImpl.getConfig().setLsCapability(true);
        tempTlv1 = new MultiProtocolExtnCapabilityTlv(afi, res, safi);
        peer1.peerChannelHandler.capabilityTlv.add(tempTlv1);
        peer2.connectFrom(connectToSocket, new InetSocketAddress("127.0.0.40", 0));
        TimeUnit.MILLISECONDS.sleep(1000);

        //Get peer2
        bgpId = new BgpId(IpAddress.valueOf("127.0.0.40"));
        peer = (BgpPeerImpl) bgpControllerImpl.getPeer(bgpId);

        tlv = AutonomousSystemTlv.of(686);
        subTlvs1.add(tlv);
        tlv = BgpLSIdentifierTlv.of(33686018);
        subTlvs1.add(tlv);
        nodeDes = new NodeDescriptors(subTlvs1, (short) 0x10, (short) 256);
        key = new BgpNodeLSIdentifier(nodeDes);
        vpnAdj = peer.vpnAdjRib();

        //In Adj-RIB, vpnNodeTree should contain rd
        assertThat(vpnAdj.vpnNodeTree().containsKey(rd), is(true));

        treeValue = vpnAdj.vpnNodeTree().get(rd);
        //In Adj-RIB, vpnNodeTree should contain rd key which contains specified value
        assertThat(treeValue.containsKey(key), is(true));

        //Disconnect peer1
        channel.disconnect();
        channel.close();

        obj = (BgpLocalRibImpl) bgpControllerImpl.bgpLocalRibVpn();

        //In Local-RIB, vpnNodeTree should contain rd
        assertThat(obj.vpnNodeTree().containsKey(rd), is(true));

        value = obj.vpnNodeTree().get(rd);
        //In Local-RIB, vpnNodeTree should contain rd key which contains specified value
        assertThat(value.containsKey(key), is(true));
    }

    /**
     * Peer1 has Node NLRI and Peer2 has Node NLRI with different MpReach and MpUnReach.
     */
    @Test
    public void testBgpUpdateMessage5() throws InterruptedException, TestUtilsException {
        // Initiate the connections
        peer1.peerChannelHandler.asNumber = 200;
        peer1.peerChannelHandler.version = 4;
        peer1.peerChannelHandler.holdTime = 120;

        short afi = 16388;
        byte res = 0;
        byte safi = 71;

        bgpControllerImpl.getConfig().setLsCapability(true);
        BgpValueType tempTlv1 = new MultiProtocolExtnCapabilityTlv(afi, res, safi);
        peer1.peerChannelHandler.capabilityTlv.add(tempTlv1);
        Channel channel = peer1.connectFrom(connectToSocket, new InetSocketAddress("127.0.0.99", 0));
        TimeUnit.MILLISECONDS.sleep(1000);

        //Get peer1
        BgpId bgpId = new BgpId(IpAddress.valueOf("127.0.0.99"));
        BgpPeerImpl peer = (BgpPeerImpl) bgpControllerImpl.getPeer(bgpId);

        LinkedList<BgpValueType> subTlvs = new LinkedList<>();
        BgpValueType tlv = null;
        tlv = AutonomousSystemTlv.of(2478);
        subTlvs.add(tlv);
        tlv = BgpLSIdentifierTlv.of(33686018);
        subTlvs.add(tlv);
        NodeDescriptors nodeDes = new NodeDescriptors(subTlvs, (short) 0x10, (short) 256);
        BgpNodeLSIdentifier key = new BgpNodeLSIdentifier(nodeDes);
        TimeUnit.MILLISECONDS.sleep(500);
        AdjRibIn adj = peer.adjRib();

        //In Adj-RIB, nodeTree should contain specified key
        assertThat(adj.nodeTree().containsKey(key), is(true));

        BgpLocalRibImpl obj = (BgpLocalRibImpl) bgpControllerImpl.bgpLocalRib();
        //In Local-RIB, nodeTree should contain specified key
        assertThat(obj.nodeTree().containsKey(key), is(true));

        peer2.peerChannelHandler.asNumber = 200;
        peer2.peerChannelHandler.version = 4;
        peer2.peerChannelHandler.holdTime = 120;

        bgpControllerImpl.getConfig().setLsCapability(true);
        tempTlv1 = new MultiProtocolExtnCapabilityTlv(afi, res, safi);
        peer2.peerChannelHandler.capabilityTlv.add(tempTlv1);
        peer2.connectFrom(connectToSocket, new InetSocketAddress("127.0.0.92", 0));
        TimeUnit.MILLISECONDS.sleep(1000);

        //Get peer2
        bgpId = new BgpId(IpAddress.valueOf("127.0.0.92"));
        peer = (BgpPeerImpl) bgpControllerImpl.getPeer(bgpId);
        adj = peer.adjRib();

        //In Adj-RIB, nodetree should be empty
        assertThat(adj.nodeTree().isEmpty(), is(true));

        //peer1 disconnects
        channel.disconnect();
        channel.close();

        obj = (BgpLocalRibImpl) bgpControllerImpl.bgpLocalRib();
        TimeUnit.MILLISECONDS.sleep(200);

        //In Local-RIB, nodeTree should be empty
        assertThat(obj.nodeTree().isEmpty(), is(true));
    }

    /**
     * Peer2 has Prefix NLRI (MpReach).
     */
    @Test
    public void testBgpUpdateMessage6() throws InterruptedException {
        // Initiate the connections
        peer1.peerChannelHandler.asNumber = 200;
        peer1.peerChannelHandler.version = 4;
        peer1.peerChannelHandler.holdTime = 120;

        short afi = 16388;
        byte res = 0;
        byte safi = 71;

        bgpControllerImpl.getConfig().setLsCapability(true);
        BgpValueType tempTlv1 = new MultiProtocolExtnCapabilityTlv(afi, res, safi);
        peer1.peerChannelHandler.capabilityTlv.add(tempTlv1);
        Channel channel = peer1.connectFrom(connectToSocket, new InetSocketAddress("127.0.0.94", 0));
        TimeUnit.MILLISECONDS.sleep(1000);

        //Get peer1
        BgpId bgpId = new BgpId(IpAddress.valueOf("127.0.0.94"));
        BgpPeerImpl peer = (BgpPeerImpl) bgpControllerImpl.getPeer(bgpId);

        LinkedList<BgpValueType> subTlvs = new LinkedList<>();
        BgpValueType tlv = AutonomousSystemTlv.of(2478);
        subTlvs.add(tlv);
        tlv = BgpLSIdentifierTlv.of(33686018);
        subTlvs.add(tlv);
        NodeDescriptors nodeDes = new NodeDescriptors(subTlvs, (short) 0x10, (short) 256);
        BgpNodeLSIdentifier key = new BgpNodeLSIdentifier(nodeDes);
        TimeUnit.MILLISECONDS.sleep(500);
        AdjRibIn adj = peer.adjRib();

        //In Adj-RIB, nodeTree should contain specified key
        assertThat(adj.nodeTree().containsKey(key), is(true));

        BgpLocalRibImpl obj = (BgpLocalRibImpl) bgpControllerImpl.bgpLocalRib();
        //In Local-RIB, nodeTree should contain specified key
        assertThat(obj.nodeTree().containsKey(key), is(true));

        peer2.peerChannelHandler.asNumber = 200;
        peer2.peerChannelHandler.version = 4;
        peer2.peerChannelHandler.holdTime = 120;

        bgpControllerImpl.getConfig().setLsCapability(true);
        tempTlv1 = new MultiProtocolExtnCapabilityTlv(afi, res, safi);
        peer2.peerChannelHandler.capabilityTlv.add(tempTlv1);
        peer2.connectFrom(connectToSocket, new InetSocketAddress("127.0.0.80", 0));
        TimeUnit.MILLISECONDS.sleep(1000);

        //Get peer2
        bgpId = new BgpId(IpAddress.valueOf("127.0.0.80"));
        peer = (BgpPeerImpl) bgpControllerImpl.getPeer(bgpId);
        TimeUnit.MILLISECONDS.sleep(500);
        adj = peer.adjRib();

        //In Adj-RIB, nodeTree should contain specified key
        assertThat(adj.nodeTree().containsKey(key), is(true));

        //peer1 disconnects
        channel.disconnect();
        channel.close();

        obj = (BgpLocalRibImpl) bgpControllerImpl.bgpLocalRib();
        TimeUnit.MILLISECONDS.sleep(200);

        //In Local-RIB, nodeTree should contain specified key
        assertThat(obj.nodeTree().containsKey(key), is(true));
    }

    /**
     * Peer1 has Node NLRI (MpReach) and peer2 has Node NLRI with same MpReach and MpUnReach with IsIsNonPseudonode.
     */
    @Test
    public void testBgpUpdateMessage7() throws InterruptedException, TestUtilsException {
        // Initiate the connections
        peer1.peerChannelHandler.asNumber = 200;
        peer1.peerChannelHandler.version = 4;
        peer1.peerChannelHandler.holdTime = 120;

        short afi = 16388;
        byte res = 0;
        byte safi = 71;

        bgpControllerImpl.getConfig().setLsCapability(true);
        BgpValueType tempTlv1 = new MultiProtocolExtnCapabilityTlv(afi, res, safi);
        peer1.peerChannelHandler.capabilityTlv.add(tempTlv1);
        Channel channel = peer1.connectFrom(connectToSocket, new InetSocketAddress("127.0.0.91", 0));
        TimeUnit.MILLISECONDS.sleep(1000);

        //Get peer1
        BgpId bgpId = new BgpId(IpAddress.valueOf("127.0.0.91"));
        BgpPeerImpl peer = (BgpPeerImpl) bgpControllerImpl.getPeer(bgpId);

        LinkedList<BgpValueType> subTlvs = new LinkedList<>();
        LinkedList<BgpValueType> subTlvs1 = new LinkedList<>();
        BgpValueType tlv = null;
        tlv = AutonomousSystemTlv.of(2478);
        subTlvs.add(tlv);
        tlv = BgpLSIdentifierTlv.of(33686018);
        subTlvs.add(tlv);
        subTlvs1.add(tlv);
        NodeDescriptors nodeDes = new NodeDescriptors(subTlvs, (short) 0x10, (short) 256);
        BgpNodeLSIdentifier key = new BgpNodeLSIdentifier(nodeDes);
        AdjRibIn adj = peer.adjRib();

        //In Adj-RIB, nodeTree should contains specified key
        assertThat(adj.nodeTree().containsKey(key), is(true));

        BgpLocalRibImpl obj = (BgpLocalRibImpl) bgpControllerImpl.bgpLocalRib();
        //In Local-RIB, nodeTree should contains specified key
        assertThat(obj.nodeTree().containsKey(key), is(true));

        peer2.peerChannelHandler.asNumber = 200;
        peer2.peerChannelHandler.version = 4;
        peer2.peerChannelHandler.holdTime = 120;

        bgpControllerImpl.getConfig().setLsCapability(true);
        tempTlv1 = new MultiProtocolExtnCapabilityTlv(afi, res, safi);
        peer2.peerChannelHandler.capabilityTlv.add(tempTlv1);
        peer2.connectFrom(connectToSocket, new InetSocketAddress("127.0.0.90", 0));
        TimeUnit.MILLISECONDS.sleep(1000);

        //Get peer2
        bgpId = new BgpId(IpAddress.valueOf("127.0.0.90"));
        peer = (BgpPeerImpl) bgpControllerImpl.getPeer(bgpId);

        tlv = AutonomousSystemTlv.of(2222);
        subTlvs1.add(tlv);
        byte[] isoNodeID = new byte[] {0x19, 0x00, (byte) 0x95, 0x01, (byte) 0x90, 0x58};
        tlv = IsIsNonPseudonode.of(isoNodeID);
        subTlvs1.add(tlv);
        nodeDes = new NodeDescriptors(subTlvs1, (short) 0x1a, (short) 256);
        key = new BgpNodeLSIdentifier(nodeDes);
        adj = peer.adjRib();

        //In Adj-RIB, nodeTree should contains specified key
        log.info("key " + key.toString());
        log.info("adj.nodeTree() " + adj.nodeTree().toString());
        assertThat(adj.nodeTree().containsKey(key), is(true));

        //peer1 disconnects
        channel.disconnect();
        channel.close();

        obj = (BgpLocalRibImpl) bgpControllerImpl.bgpLocalRib();
        TimeUnit.MILLISECONDS.sleep(200);

        //In Local-RIB, nodeTree should contains specified key
        assertThat(obj.nodeTree().containsKey(key), is(true));
    }

    /**
     * Peer1 has Prefix NLRI (MpReach).
     */
    @Test
    public void testBgpUpdateMessage8() throws InterruptedException {
        // Initiate the connections
        peer1.peerChannelHandler.asNumber = 200;
        peer1.peerChannelHandler.version = 4;
        peer1.peerChannelHandler.holdTime = 150;

        short afi = 16388;
        byte res = 0;
        byte safi = 71;

        bgpControllerImpl.getConfig().setLsCapability(true);
        BgpValueType tempTlv1 = new MultiProtocolExtnCapabilityTlv(afi, res, safi);
        peer1.peerChannelHandler.capabilityTlv.add(tempTlv1);
        peer1.connectFrom(connectToSocket, new InetSocketAddress("127.0.0.20", 0));
        TimeUnit.MILLISECONDS.sleep(1000);

        //Get peer1
        BgpId bgpId = new BgpId(IpAddress.valueOf("127.0.0.20"));
        BgpPeerImpl peer = (BgpPeerImpl) bgpControllerImpl.getPeer(bgpId);

        LinkedList<BgpValueType> subTlvs = new LinkedList<>();
        BgpValueType tlv = AutonomousSystemTlv.of(2222);
        subTlvs.add(tlv);
        tlv = BgpLSIdentifierTlv.of(33686018);
        subTlvs.add(tlv);
        byte[] isoNodeID = new byte[] {0x19, 0x21, 0x68, 0x07, 0x70, 0x01};
        tlv = IsIsNonPseudonode.of(isoNodeID);
        subTlvs.add(tlv);
        NodeDescriptors nodeDes = new NodeDescriptors(subTlvs, (short) 0x1a, (short) 256);
        LinkedList<BgpValueType> prefixDescriptor = new LinkedList<>();
        byte[] prefix = new byte[] {0x20, (byte) 0xc0, (byte) 0xa8, 0x4d, 0x01};
        ChannelBuffer tempCb = ChannelBuffers.dynamicBuffer();
        tempCb.writeBytes(prefix);
        tlv = IPReachabilityInformationTlv.read(tempCb, (short) 5);
        prefixDescriptor.add(tlv);
        BgpPrefixLSIdentifier key = new BgpPrefixLSIdentifier(nodeDes, prefixDescriptor);

        AdjRibIn adj = peer.adjRib();

        //In Adj-RIB, prefixTree should contain specified key
        assertThat(adj.prefixTree().containsKey(key), is(true));

        BgpLocalRibImpl obj = (BgpLocalRibImpl) bgpControllerImpl.bgpLocalRib();
        //In Local-RIB, prefixTree should contain specified key
        assertThat(obj.prefixTree().containsKey(key), is(true));
    }

    /**
     * Peer1 has Node NLRI (MpReach) and Peer2 has node NLRI with different MpReach
     * and MpUnReach with IsIsNonPseudonode.
     */
    @Test
    public void testBgpUpdateMessage9() throws InterruptedException {
        // Initiate the connections
        peer1.peerChannelHandler.asNumber = 200;
        peer1.peerChannelHandler.version = 4;
        peer1.peerChannelHandler.holdTime = 120;

        short afi = 16388;
        byte res = 0;
        byte safi = (byte) 0x80;

        bgpControllerImpl.getConfig().setLsCapability(true);
        BgpValueType tempTlv1 = new MultiProtocolExtnCapabilityTlv(afi, res, safi);
        peer1.peerChannelHandler.capabilityTlv.add(tempTlv1);
        Channel channel = peer1.connectFrom(connectToSocket, new InetSocketAddress("127.0.0.30", 0));
        TimeUnit.MILLISECONDS.sleep(1000);

        //Get peer1
        BgpId bgpId = new BgpId(IpAddress.valueOf("127.0.0.30"));
        BgpPeerImpl peer = (BgpPeerImpl) bgpControllerImpl.getPeer(bgpId);

        LinkedList<BgpValueType> subTlvs = new LinkedList<>();
        BgpValueType tlv = AutonomousSystemTlv.of(2478);
        subTlvs.add(tlv);
        tlv = BgpLSIdentifierTlv.of(33686018);
        subTlvs.add(tlv);

        NodeDescriptors nodeDes = new NodeDescriptors(subTlvs, (short) 0x10, (short) 256);
        BgpNodeLSIdentifier key = new BgpNodeLSIdentifier(nodeDes);
        RouteDistinguisher rd = new RouteDistinguisher((long) 0x0A);
        VpnAdjRibIn vpnAdj = peer.vpnAdjRib();

        //In Adj-RIB, vpnNodeTree should contain specified rd
        assertThat(vpnAdj.vpnNodeTree().containsKey(rd), is(true));

        Map<BgpNodeLSIdentifier, PathAttrNlriDetails> treeValue = vpnAdj.vpnNodeTree().get(rd);
        //In Adj-RIB, vpnNodeTree should contain specified rd with specified value
        assertThat(treeValue.containsKey(key), is(true));

        BgpLocalRibImpl obj = (BgpLocalRibImpl) bgpControllerImpl.bgpLocalRibVpn();
        //In Local-RIB, vpnNodeTree should contain specified rd
        assertThat(obj.vpnNodeTree().containsKey(rd), is(true));

        Map<BgpNodeLSIdentifier, PathAttrNlriDetailsLocalRib> value = obj.vpnNodeTree().get(rd);
        //In Local-RIB, vpnNodeTree should contain specified rd with specified value
        assertThat(value.containsKey(key), is(true));

        peer2.peerChannelHandler.asNumber = 200;
        peer2.peerChannelHandler.version = 4;
        peer2.peerChannelHandler.holdTime = 120;

        bgpControllerImpl.getConfig().setLsCapability(true);
        tempTlv1 = new MultiProtocolExtnCapabilityTlv(afi, res, safi);
        peer2.peerChannelHandler.capabilityTlv.add(tempTlv1);
        peer2.connectFrom(connectToSocket, new InetSocketAddress("127.0.0.50", 0));
        TimeUnit.MILLISECONDS.sleep(1000);

        //Get peer2
        bgpId = new BgpId(IpAddress.valueOf("127.0.0.50"));
        peer = (BgpPeerImpl) bgpControllerImpl.getPeer(bgpId);
        key = new BgpNodeLSIdentifier(nodeDes);
        vpnAdj = peer.vpnAdjRib();

        //In Adj-RIB, vpnNodeTree should be empty
        assertThat(vpnAdj.vpnNodeTree().isEmpty(), is(true));

        //peer1 disconnects
        channel.disconnect();
        channel.close();

        obj = (BgpLocalRibImpl) bgpControllerImpl.bgpLocalRibVpn();
        TimeUnit.MILLISECONDS.sleep(200);

        //In Local-RIB, vpnNodeTree should be empty
        assertThat(obj.vpnNodeTree().isEmpty(), is(true));
    }

    /**
     * A class to capture the state for a BGP peer.
     */
    private final class BgpPeerTest {
        private ClientBootstrap peerBootstrap;
        private BgpPeerFrameDecoderTest peerFrameDecoder =
                new BgpPeerFrameDecoderTest();
        private BgpPeerChannelHandlerTest peerChannelHandler;

        private BgpPeerTest(byte version, short asNumber,
                short holdTime, int bgpId, boolean isLargeAsCapabilitySet,
                LinkedList<BgpValueType> capabilityTlv) {
            peerChannelHandler = new BgpPeerChannelHandlerTest(version,
                asNumber, holdTime, bgpId, isLargeAsCapabilitySet, capabilityTlv);
        }

        /**
         * Starts the BGP peer.
         *
         * @param connectToSocket the socket to connect to
         */
        private void connect(InetSocketAddress connectToSocket)
            throws InterruptedException {

            ChannelFactory channelFactory =
                new NioClientSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool());
            ChannelPipelineFactory pipelineFactory = () -> {
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("BgpPeerFrameDecoderTest",
                        peerFrameDecoder);
                pipeline.addLast("BgpPeerChannelHandlerTest",
                        peerChannelHandler);
                return pipeline;
            };

            peerBootstrap = new ClientBootstrap(channelFactory);
            peerBootstrap.setOption("child.keepAlive", true);
            peerBootstrap.setOption("child.tcpNoDelay", true);
            peerBootstrap.setPipelineFactory(pipelineFactory);
            peerBootstrap.connect(connectToSocket);
       }

        private Channel connectFrom(InetSocketAddress connectToSocket, SocketAddress localAddress)
                throws InterruptedException {

                ChannelFactory channelFactory =
                    new NioClientSocketChannelFactory(
                            Executors.newCachedThreadPool(),
                            Executors.newCachedThreadPool());
                ChannelPipelineFactory pipelineFactory = () -> {
                    ChannelPipeline pipeline = Channels.pipeline();
                    pipeline.addLast("BgpPeerFrameDecoderTest",
                            peerFrameDecoder);
                    pipeline.addLast("BgpPeerChannelHandlerTest",
                            peerChannelHandler);
                    return pipeline;
                };

                peerBootstrap = new ClientBootstrap(channelFactory);
                peerBootstrap.setOption("child.keepAlive", true);
                peerBootstrap.setOption("child.tcpNoDelay", true);
                peerBootstrap.setPipelineFactory(pipelineFactory);
                Channel channel = peerBootstrap.connect(connectToSocket, localAddress).getChannel();
                return channel;
           }
    }
}