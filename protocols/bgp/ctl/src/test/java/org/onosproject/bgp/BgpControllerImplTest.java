/*
 * Copyright 2014-2015 Open Networking Laboratory
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
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.onlab.junit.TestUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.onosproject.bgp.controller.BgpCfg;
import org.onosproject.bgp.controller.impl.BgpControllerImpl;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.FourOctetAsNumCapabilityTlv;
import org.onosproject.bgpio.types.MultiProtocolExtnCapabilityTlv;

/**
 * Test case for BGPControllerImpl.
 */
public class BgpControllerImplTest {

    protected static final Logger log = LoggerFactory
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
    }

    @After
    public void tearDown() throws Exception {
        bgpControllerImpl.deactivate();
        bgpControllerImpl = null;
    }

    private BgpControllerImpl bgpControllerImpl;

    BgpPeerTest peer1;

    // The socket that the remote peers should connect to
    private InetSocketAddress connectToSocket;

    @Test
    public void bgpOpenMessageTest1() throws InterruptedException {
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
    public void bgpOpenMessageTest2() throws InterruptedException {
        // Open message with as number which is not configured at peer
        short afi = 16388;
        byte res = 0;
        byte safi = 71;
        peer1.peerChannelHandler.asNumber = 500;
        peer1.peerChannelHandler.version = 4;
        peer1.peerChannelHandler.holdTime = 120;
        bgpControllerImpl.getConfig().setLsCapability(true);
        BgpValueType tempTlv1 = new MultiProtocolExtnCapabilityTlv(afi, res, safi);
        peer1.peerChannelHandler.capabilityTlv.add(tempTlv1);
        peer1.connect(connectToSocket);

        boolean result;
        result = peer1.peerFrameDecoder.receivedOpenMessageLatch.await(MESSAGE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertThat(result, is(true));
        result = peer1.peerFrameDecoder.receivedKeepaliveMessageLatch.await(MESSAGE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertThat(result, is(true));
        result = peer1.peerFrameDecoder.receivedNotificationMessageLatch.await(
            MESSAGE_TIMEOUT_MS,
            TimeUnit.MILLISECONDS);
        assertThat(result, is(false));
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
    }
}