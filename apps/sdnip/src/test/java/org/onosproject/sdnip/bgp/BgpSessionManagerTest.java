/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.sdnip.bgp;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.junit.TestUtils.TestUtilsException;
import org.onosproject.sdnip.RouteListener;
import org.onosproject.sdnip.RouteUpdate;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;

import com.google.common.net.InetAddresses;

/**
 * Unit tests for the BgpSessionManager class.
 */
public class BgpSessionManagerTest {
    private static final Ip4Address IP_LOOPBACK_ID =
        Ip4Address.valueOf("127.0.0.1");
    private static final Ip4Address BGP_PEER1_ID =
        Ip4Address.valueOf("10.0.0.1");
    private static final long DEFAULT_LOCAL_PREF = 10;
    private static final long DEFAULT_MULTI_EXIT_DISC = 20;

    // Timeout waiting for a message to be received
    private static final int MESSAGE_TIMEOUT_MS = 5000; // 5s

    // The BGP Session Manager to test
    private BgpSessionManager bgpSessionManager;

    // Remote Peer state
    private ClientBootstrap peerBootstrap;
    private TestBgpPeerChannelHandler peerChannelHandler =
        new TestBgpPeerChannelHandler(BGP_PEER1_ID, DEFAULT_LOCAL_PREF);
    private TestBgpPeerFrameDecoder peerFrameDecoder =
        new TestBgpPeerFrameDecoder();

    // The socket that the Remote Peer should connect to
    private InetSocketAddress connectToSocket;

    private final DummyRouteListener dummyRouteListener =
        new DummyRouteListener();

    /**
     * Dummy implementation for the RouteListener interface.
     */
    private class DummyRouteListener implements RouteListener {
        @Override
        public void update(Collection<RouteUpdate> routeUpdate) {
            // Nothing to do
        }
    }

    @Before
    public void setUp() throws Exception {
        //
        // Setup the BGP Session Manager to test, and start listening for BGP
        // connections.
        //
        bgpSessionManager = new BgpSessionManager(dummyRouteListener);
        // NOTE: We use port 0 to bind on any available port
        bgpSessionManager.start(0);

        // Get the port number the BGP Session Manager is listening on
        Channel serverChannel = TestUtils.getField(bgpSessionManager,
                                                   "serverChannel");
        SocketAddress socketAddress = serverChannel.getLocalAddress();
        InetSocketAddress inetSocketAddress =
            (InetSocketAddress) socketAddress;

        //
        // Setup the BGP Peer, i.e., the "remote" BGP router that will
        // initiate the BGP connection, send BGP UPDATE messages, etc.
        //
        ChannelFactory channelFactory =
            new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),
                                              Executors.newCachedThreadPool());
        ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory() {
                @Override
                public ChannelPipeline getPipeline() throws Exception {
                    // Setup the transmitting pipeline
                    ChannelPipeline pipeline = Channels.pipeline();
                    pipeline.addLast("TestBgpPeerFrameDecoder",
                                     peerFrameDecoder);
                    pipeline.addLast("TestBgpPeerChannelHandler",
                                     peerChannelHandler);
                    return pipeline;
                }
            };

        peerBootstrap = new ClientBootstrap(channelFactory);
        peerBootstrap.setOption("child.keepAlive", true);
        peerBootstrap.setOption("child.tcpNoDelay", true);
        peerBootstrap.setPipelineFactory(pipelineFactory);

        InetAddress connectToAddress = InetAddresses.forString("127.0.0.1");
        connectToSocket = new InetSocketAddress(connectToAddress,
                                                inetSocketAddress.getPort());
    }

    @After
    public void tearDown() throws Exception {
        bgpSessionManager.stop();
        bgpSessionManager = null;
    }

    /**
     * Gets BGP RIB-IN routes by waiting until they are received.
     * <p/>
     * NOTE: We keep checking once a second the number of received routes,
     * up to 5 seconds.
     *
     * @param bgpSession the BGP session that is expected to receive the
     * routes
     * @param expectedRoutes the expected number of routes
     * @return the BGP RIB-IN routes as received within the expected
     * time interval
     */
    private Collection<BgpRouteEntry> waitForBgpRibIn(BgpSession bgpSession,
                                                      long expectedRoutes)
        throws InterruptedException {
        Collection<BgpRouteEntry> bgpRibIn = bgpSession.getBgpRibIn();

        final int maxChecks = 5;                // Max wait of 5 seconds
        for (int i = 0; i < maxChecks; i++) {
            if (bgpRibIn.size() == expectedRoutes) {
                break;
            }
            Thread.sleep(1000);
            bgpRibIn = bgpSession.getBgpRibIn();
        }

        return bgpRibIn;
    }

    /**
     * Gets BGP merged routes by waiting until they are received.
     * <p/>
     * NOTE: We keep checking once a second the number of received routes,
     * up to 5 seconds.
     *
     * @param expectedRoutes the expected number of routes
     * @return the BGP Session Manager routes as received within the expected
     * time interval
     */
    private Collection<BgpRouteEntry> waitForBgpRoutes(long expectedRoutes)
        throws InterruptedException {
        Collection<BgpRouteEntry> bgpRoutes = bgpSessionManager.getBgpRoutes();

        final int maxChecks = 5;                // Max wait of 5 seconds
        for (int i = 0; i < maxChecks; i++) {
            if (bgpRoutes.size() == expectedRoutes) {
                break;
            }
            Thread.sleep(1000);
            bgpRoutes = bgpSessionManager.getBgpRoutes();
        }

        return bgpRoutes;
    }

    /**
     * Tests that the BGP OPEN messages have been exchanged, followed by
     * KEEPALIVE.
     * <p>
     * The BGP Peer opens the sessions and transmits OPEN Message, eventually
     * followed by KEEPALIVE. The tested BGP listener should respond by
     * OPEN Message, followed by KEEPALIVE.
     *
     * @throws TestUtilsException TestUtils error
     */
    @Test
    public void testExchangedBgpOpenMessages()
            throws InterruptedException, TestUtilsException {
        // Initiate the connection
        peerBootstrap.connect(connectToSocket);

        // Wait until the OPEN message is received
        peerFrameDecoder.receivedOpenMessageLatch.await(MESSAGE_TIMEOUT_MS,
                                                        TimeUnit.MILLISECONDS);
        // Wait until the KEEPALIVE message is received
        peerFrameDecoder.receivedKeepaliveMessageLatch.await(MESSAGE_TIMEOUT_MS,
                                                        TimeUnit.MILLISECONDS);

        //
        // Test the fields from the BGP OPEN message:
        // BGP version, AS number, BGP ID
        //
        assertThat(peerFrameDecoder.remoteBgpVersion,
                   is(BgpConstants.BGP_VERSION));
        assertThat(peerFrameDecoder.remoteAs,
                   is(TestBgpPeerChannelHandler.PEER_AS));
        assertThat(peerFrameDecoder.remoteBgpIdentifier, is(IP_LOOPBACK_ID));

        //
        // Test that a BgpSession instance has been created
        //
        assertThat(bgpSessionManager.getMyBgpId(), is(IP_LOOPBACK_ID));
        assertThat(bgpSessionManager.getBgpSessions(), hasSize(1));
        BgpSession bgpSession =
            bgpSessionManager.getBgpSessions().iterator().next();
        assertThat(bgpSession, notNullValue());
        long sessionAs = TestUtils.getField(bgpSession, "localAs");
        assertThat(sessionAs, is(TestBgpPeerChannelHandler.PEER_AS));
    }

    /**
     * Tests that the BGP UPDATE messages have been received and processed.
     */
    @Test
    public void testProcessedBgpUpdateMessages() throws InterruptedException {
        BgpSession bgpSession;
        Ip4Address nextHopRouter;
        BgpRouteEntry bgpRouteEntry;
        ChannelBuffer message;
        Collection<BgpRouteEntry> bgpRibIn;
        Collection<BgpRouteEntry> bgpRoutes;

        // Initiate the connection
        peerBootstrap.connect(connectToSocket);

        // Wait until the OPEN message is received
        peerFrameDecoder.receivedOpenMessageLatch.await(MESSAGE_TIMEOUT_MS,
                                                        TimeUnit.MILLISECONDS);
        // Wait until the KEEPALIVE message is received
        peerFrameDecoder.receivedKeepaliveMessageLatch.await(MESSAGE_TIMEOUT_MS,
                                                        TimeUnit.MILLISECONDS);

        // Get the BGP Session handler
        bgpSession = bgpSessionManager.getBgpSessions().iterator().next();

        // Prepare routes to add/delete
        nextHopRouter = Ip4Address.valueOf("10.20.30.40");
        Collection<Ip4Prefix> addedRoutes = new LinkedList<>();
        Collection<Ip4Prefix> withdrawnRoutes = new LinkedList<>();
        addedRoutes.add(Ip4Prefix.valueOf("0.0.0.0/0"));
        addedRoutes.add(Ip4Prefix.valueOf("20.0.0.0/8"));
        addedRoutes.add(Ip4Prefix.valueOf("30.0.0.0/16"));
        addedRoutes.add(Ip4Prefix.valueOf("40.0.0.0/24"));
        addedRoutes.add(Ip4Prefix.valueOf("50.0.0.0/32"));
        withdrawnRoutes.add(Ip4Prefix.valueOf("60.0.0.0/8"));
        withdrawnRoutes.add(Ip4Prefix.valueOf("70.0.0.0/16"));
        withdrawnRoutes.add(Ip4Prefix.valueOf("80.0.0.0/24"));
        withdrawnRoutes.add(Ip4Prefix.valueOf("90.0.0.0/32"));
        // Write the routes
        message = peerChannelHandler.prepareBgpUpdate(nextHopRouter,
                                                      addedRoutes,
                                                      withdrawnRoutes);
        peerChannelHandler.savedCtx.getChannel().write(message);

        // Check that the routes have been received, processed and stored
        bgpRibIn = waitForBgpRibIn(bgpSession, 5);
        assertThat(bgpRibIn, hasSize(5));
        bgpRoutes = waitForBgpRoutes(5);
        assertThat(bgpRoutes, hasSize(5));

        // Setup the AS Path
        ArrayList<BgpRouteEntry.PathSegment> pathSegments = new ArrayList<>();
        byte pathSegmentType1 = (byte) BgpConstants.Update.AsPath.AS_SEQUENCE;
        ArrayList<Long> segmentAsNumbers1 = new ArrayList<>();
        segmentAsNumbers1.add((long) 65010);
        segmentAsNumbers1.add((long) 65020);
        segmentAsNumbers1.add((long) 65030);
        BgpRouteEntry.PathSegment pathSegment1 =
            new BgpRouteEntry.PathSegment(pathSegmentType1, segmentAsNumbers1);
        pathSegments.add(pathSegment1);
        //
        byte pathSegmentType2 = (byte) BgpConstants.Update.AsPath.AS_SET;
        ArrayList<Long> segmentAsNumbers2 = new ArrayList<>();
        segmentAsNumbers2.add((long) 65041);
        segmentAsNumbers2.add((long) 65042);
        segmentAsNumbers2.add((long) 65043);
        BgpRouteEntry.PathSegment pathSegment2 =
            new BgpRouteEntry.PathSegment(pathSegmentType2, segmentAsNumbers2);
        pathSegments.add(pathSegment2);
        //
        BgpRouteEntry.AsPath asPath = new BgpRouteEntry.AsPath(pathSegments);

        //
        bgpRouteEntry =
            new BgpRouteEntry(bgpSession,
                              Ip4Prefix.valueOf("0.0.0.0/0"),
                              nextHopRouter,
                              (byte) BgpConstants.Update.Origin.IGP,
                              asPath,
                              DEFAULT_LOCAL_PREF);
        bgpRouteEntry.setMultiExitDisc(DEFAULT_MULTI_EXIT_DISC);
        assertThat(bgpRibIn, hasItem(bgpRouteEntry));
        //
        bgpRouteEntry =
            new BgpRouteEntry(bgpSession,
                              Ip4Prefix.valueOf("20.0.0.0/8"),
                              nextHopRouter,
                              (byte) BgpConstants.Update.Origin.IGP,
                              asPath,
                              DEFAULT_LOCAL_PREF);
        bgpRouteEntry.setMultiExitDisc(DEFAULT_MULTI_EXIT_DISC);
        assertThat(bgpRibIn, hasItem(bgpRouteEntry));
        //
        bgpRouteEntry =
            new BgpRouteEntry(bgpSession,
                              Ip4Prefix.valueOf("30.0.0.0/16"),
                              nextHopRouter,
                              (byte) BgpConstants.Update.Origin.IGP,
                              asPath,
                              DEFAULT_LOCAL_PREF);
        bgpRouteEntry.setMultiExitDisc(DEFAULT_MULTI_EXIT_DISC);
        assertThat(bgpRibIn, hasItem(bgpRouteEntry));
        //
        bgpRouteEntry =
            new BgpRouteEntry(bgpSession,
                              Ip4Prefix.valueOf("40.0.0.0/24"),
                              nextHopRouter,
                              (byte) BgpConstants.Update.Origin.IGP,
                              asPath,
                              DEFAULT_LOCAL_PREF);
        bgpRouteEntry.setMultiExitDisc(DEFAULT_MULTI_EXIT_DISC);
        assertThat(bgpRibIn, hasItem(bgpRouteEntry));
        //
        bgpRouteEntry =
            new BgpRouteEntry(bgpSession,
                              Ip4Prefix.valueOf("50.0.0.0/32"),
                              nextHopRouter,
                              (byte) BgpConstants.Update.Origin.IGP,
                              asPath,
                              DEFAULT_LOCAL_PREF);
        bgpRouteEntry.setMultiExitDisc(DEFAULT_MULTI_EXIT_DISC);
        assertThat(bgpRibIn, hasItem(bgpRouteEntry));

        // Delete some routes
        addedRoutes = new LinkedList<>();
        withdrawnRoutes = new LinkedList<>();
        withdrawnRoutes.add(Ip4Prefix.valueOf("0.0.0.0/0"));
        withdrawnRoutes.add(Ip4Prefix.valueOf("50.0.0.0/32"));

        // Write the routes
        message = peerChannelHandler.prepareBgpUpdate(nextHopRouter,
                                                      addedRoutes,
                                                      withdrawnRoutes);
        peerChannelHandler.savedCtx.getChannel().write(message);

        // Check that the routes have been received, processed and stored
        bgpRibIn = waitForBgpRibIn(bgpSession, 3);
        assertThat(bgpRibIn, hasSize(3));
        bgpRoutes = waitForBgpRoutes(3);
        assertThat(bgpRoutes, hasSize(3));
        //
        bgpRouteEntry =
            new BgpRouteEntry(bgpSession,
                              Ip4Prefix.valueOf("20.0.0.0/8"),
                              nextHopRouter,
                              (byte) BgpConstants.Update.Origin.IGP,
                              asPath,
                              DEFAULT_LOCAL_PREF);
        bgpRouteEntry.setMultiExitDisc(DEFAULT_MULTI_EXIT_DISC);
        assertThat(bgpRibIn, hasItem(bgpRouteEntry));
        //
        bgpRouteEntry =
            new BgpRouteEntry(bgpSession,
                              Ip4Prefix.valueOf("30.0.0.0/16"),
                              nextHopRouter,
                              (byte) BgpConstants.Update.Origin.IGP,
                              asPath,
                              DEFAULT_LOCAL_PREF);
        bgpRouteEntry.setMultiExitDisc(DEFAULT_MULTI_EXIT_DISC);
        assertThat(bgpRibIn, hasItem(bgpRouteEntry));
        //
        bgpRouteEntry =
            new BgpRouteEntry(bgpSession,
                              Ip4Prefix.valueOf("40.0.0.0/24"),
                              nextHopRouter,
                              (byte) BgpConstants.Update.Origin.IGP,
                              asPath,
                              DEFAULT_LOCAL_PREF);
        bgpRouteEntry.setMultiExitDisc(DEFAULT_MULTI_EXIT_DISC);
        assertThat(bgpRibIn, hasItem(bgpRouteEntry));

        // Close the channel and test there are no routes
        peerChannelHandler.closeChannel();
        bgpRoutes = waitForBgpRoutes(0);
        assertThat(bgpRoutes, hasSize(0));
    }
}
