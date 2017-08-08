/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.routing.bgp;

import com.google.common.net.InetAddresses;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
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
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.routeservice.RouteAdminService;
import org.osgi.service.component.ComponentContext;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for the BgpSessionManager class.
 */
public class BgpSessionManagerTest {
    private static final Ip4Address IP_LOOPBACK_ID =
        Ip4Address.valueOf("127.0.0.1");
    private static final Ip4Address BGP_PEER1_ID =
        Ip4Address.valueOf("10.0.0.1");
    private static final Ip4Address BGP_PEER2_ID =
        Ip4Address.valueOf("10.0.0.2");
    private static final Ip4Address BGP_PEER3_ID =
        Ip4Address.valueOf("10.0.0.3");
    private static final Ip4Address NEXT_HOP1_ROUTER =
        Ip4Address.valueOf("10.20.30.41");
    private static final Ip4Address NEXT_HOP2_ROUTER =
        Ip4Address.valueOf("10.20.30.42");
    private static final Ip4Address NEXT_HOP3_ROUTER =
        Ip4Address.valueOf("10.20.30.43");

    private static final long DEFAULT_LOCAL_PREF = 10;
    private static final long BETTER_LOCAL_PREF = 20;
    private static final long DEFAULT_MULTI_EXIT_DISC = 20;
    private static final long BETTER_MULTI_EXIT_DISC = 30;

    private static final NodeId NODE_ID = new NodeId("local");
    private static final IpAddress LOCAL = IpAddress.valueOf("127.0.0.1");

    BgpRouteEntry.AsPath asPathShort;
    BgpRouteEntry.AsPath asPathLong;

    // Timeout waiting for a message to be received
    private static final int MESSAGE_TIMEOUT_MS = 5000; // 5s

    private RouteAdminService routeService;

    // The BGP Session Manager to test
    private BgpSessionManager bgpSessionManager;

    // Remote Peer state
    private final Collection<TestBgpPeer> peers = new LinkedList<>();
    TestBgpPeer peer1;
    TestBgpPeer peer2;
    TestBgpPeer peer3;

    // Local BGP per-peer session state
    BgpSession bgpSession1;
    BgpSession bgpSession2;
    BgpSession bgpSession3;

    // The socket that the remote peers should connect to
    private InetSocketAddress connectToSocket;

    /**
     * A class to capture the state for a BGP peer.
     */
    private final class TestBgpPeer {
        private final Ip4Address peerId;
        private ClientBootstrap peerBootstrap;
        private TestBgpPeerChannelHandler peerChannelHandler;
        private TestBgpPeerFrameDecoder peerFrameDecoder =
            new TestBgpPeerFrameDecoder();

        /**
         * Constructor.
         *
         * @param peerId the peer ID
         */
        private TestBgpPeer(Ip4Address peerId) {
            this.peerId = peerId;
            peerChannelHandler = new TestBgpPeerChannelHandler(peerId);
        }

        /**
         * Starts up the BGP peer and connects it to the tested BgpSessionManager
         * instance.
         *
         * @param connectToSocket the socket to connect to
         */
        private void connect(InetSocketAddress connectToSocket)
            throws InterruptedException {
            //
            // Setup the BGP Peer, i.e., the "remote" BGP router that will
            // initiate the BGP connection, send BGP UPDATE messages, etc.
            //
            ChannelFactory channelFactory =
                new NioClientSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool());
            ChannelPipelineFactory pipelineFactory = () -> {
                // Setup the transmitting pipeline
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("TestBgpPeerFrameDecoder",
                        peerFrameDecoder);
                pipeline.addLast("TestBgpPeerChannelHandler",
                        peerChannelHandler);
                return pipeline;
            };

            peerBootstrap = new ClientBootstrap(channelFactory);
            peerBootstrap.setOption("child.keepAlive", true);
            peerBootstrap.setOption("child.tcpNoDelay", true);
            peerBootstrap.setPipelineFactory(pipelineFactory);
            peerBootstrap.connect(connectToSocket);

            boolean result;
            // Wait until the OPEN message is received
            result = peerFrameDecoder.receivedOpenMessageLatch.await(
                MESSAGE_TIMEOUT_MS,
                TimeUnit.MILLISECONDS);
            assertThat(result, is(true));
            // Wait until the KEEPALIVE message is received
            result = peerFrameDecoder.receivedKeepaliveMessageLatch.await(
                MESSAGE_TIMEOUT_MS,
                TimeUnit.MILLISECONDS);
            assertThat(result, is(true));

            for (BgpSession bgpSession : bgpSessionManager.getBgpSessions()) {
                if (bgpSession.remoteInfo().bgpId().equals(BGP_PEER1_ID)) {
                    bgpSession1 = bgpSession;
                }
                if (bgpSession.remoteInfo().bgpId().equals(BGP_PEER2_ID)) {
                    bgpSession2 = bgpSession;
                }
                if (bgpSession.remoteInfo().bgpId().equals(BGP_PEER3_ID)) {
                    bgpSession3 = bgpSession;
                }
            }
        }
    }

    /**
     * Class that implements a matcher for BgpRouteEntry by considering
     * the BGP peer the entry was received from.
     */
    private static final class BgpRouteEntryAndPeerMatcher
        extends TypeSafeMatcher<Collection<BgpRouteEntry>> {
        private final BgpRouteEntry bgpRouteEntry;

        private BgpRouteEntryAndPeerMatcher(BgpRouteEntry bgpRouteEntry) {
            this.bgpRouteEntry = bgpRouteEntry;
        }

        @Override
        public boolean matchesSafely(Collection<BgpRouteEntry> entries) {
            for (BgpRouteEntry entry : entries) {
                if (bgpRouteEntry.equals(entry) &&
                    bgpRouteEntry.getBgpSession() == entry.getBgpSession()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("BGP route entry lookup for entry \"").
                appendText(bgpRouteEntry.toString()).
                appendText("\"");
        }
    }

    /**
     * A helper method used for testing whether a collection of
     * BGP route entries contains an entry from a specific BGP peer.
     *
     * @param bgpRouteEntry the BGP route entry to test
     * @return an instance of BgpRouteEntryAndPeerMatcher that implements
     * the matching logic
     */
    private static BgpRouteEntryAndPeerMatcher hasBgpRouteEntry(
        BgpRouteEntry bgpRouteEntry) {
        return new BgpRouteEntryAndPeerMatcher(bgpRouteEntry);
    }

    @SuppressWarnings("unchecked")
    private void getDictionaryMock(ComponentContext componentContext) {
        Dictionary dictionary = createMock(Dictionary.class);
        expect(dictionary.get("bgpPort")).andReturn("0");
        replay(dictionary);
        expect(componentContext.getProperties()).andReturn(dictionary);
    }

    @Before
    public void setUp() throws Exception {
        peer1 = new TestBgpPeer(BGP_PEER1_ID);
        peer2 = new TestBgpPeer(BGP_PEER2_ID);
        peer3 = new TestBgpPeer(BGP_PEER3_ID);
        peers.clear();
        peers.add(peer1);
        peers.add(peer2);
        peers.add(peer3);

        //
        // Setup the BGP Session Manager to test, and start listening for BGP
        // connections.
        //
        bgpSessionManager = new BgpSessionManager();

        routeService = createNiceMock(RouteAdminService.class);
        replay(routeService);
        bgpSessionManager.routeService = routeService;

        ClusterService clusterService = createMock(ClusterService.class);
        expect(clusterService.getLocalNode())
                .andReturn(new DefaultControllerNode(NODE_ID, LOCAL)).anyTimes();
        replay(clusterService);
        bgpSessionManager.clusterService = clusterService;

        // NOTE: We use port 0 to bind on any available port
        ComponentContext componentContext = createMock(ComponentContext.class);
        getDictionaryMock(componentContext);
        replay(componentContext);
        bgpSessionManager.activate(componentContext);

        // Get the port number the BGP Session Manager is listening on
        Channel serverChannel = TestUtils.getField(bgpSessionManager,
                                                   "serverChannel");
        SocketAddress socketAddress = serverChannel.getLocalAddress();
        InetSocketAddress inetSocketAddress =
            (InetSocketAddress) socketAddress;
        InetAddress connectToAddress = InetAddresses.forString("127.0.0.1");
        connectToSocket = new InetSocketAddress(connectToAddress,
                                                inetSocketAddress.getPort());

        //
        // Setup the AS Paths
        //
        ArrayList<BgpRouteEntry.PathSegment> pathSegments = new ArrayList<>();
        byte pathSegmentType1 = (byte) BgpConstants.Update.AsPath.AS_SEQUENCE;
        ArrayList<Long> segmentAsNumbers1 = new ArrayList<>();
        segmentAsNumbers1.add(65010L);
        segmentAsNumbers1.add(65020L);
        segmentAsNumbers1.add(65030L);
        BgpRouteEntry.PathSegment pathSegment1 =
            new BgpRouteEntry.PathSegment(pathSegmentType1, segmentAsNumbers1);
        pathSegments.add(pathSegment1);
        asPathShort = new BgpRouteEntry.AsPath(new ArrayList<>(pathSegments));
        //
        byte pathSegmentType2 = (byte) BgpConstants.Update.AsPath.AS_SET;
        ArrayList<Long> segmentAsNumbers2 = new ArrayList<>();
        segmentAsNumbers2.add(65041L);
        segmentAsNumbers2.add(65042L);
        segmentAsNumbers2.add(65043L);
        BgpRouteEntry.PathSegment pathSegment2 =
            new BgpRouteEntry.PathSegment(pathSegmentType2, segmentAsNumbers2);
        pathSegments.add(pathSegment2);
        //
        asPathLong = new BgpRouteEntry.AsPath(pathSegments);
    }

    @After
    public void tearDown() throws Exception {
        bgpSessionManager.stop();
        bgpSessionManager = null;
    }

    /**
     * Gets BGP RIB-IN routes by waiting until they are received.
     * <p>
     * NOTE: We keep checking once every 10ms the number of received routes,
     * up to 5 seconds.
     * </p>
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
        Collection<BgpRouteEntry> bgpRibIn = bgpSession.getBgpRibIn4();

        final int maxChecks = 500;              // Max wait of 5 seconds
        for (int i = 0; i < maxChecks; i++) {
            if (bgpRibIn.size() == expectedRoutes) {
                break;
            }
            Thread.sleep(10);
            bgpRibIn = bgpSession.getBgpRibIn4();
        }

        return bgpRibIn;
    }

    /**
     * Gets BGP merged routes by waiting until they are received.
     * <p>
     * NOTE: We keep checking once every 10ms the number of received routes,
     * up to 5 seconds.
     * </p>
     *
     * @param expectedRoutes the expected number of routes
     * @return the BGP Session Manager routes as received within the expected
     * time interval
     */
    private Collection<BgpRouteEntry> waitForBgpRoutes(long expectedRoutes)
        throws InterruptedException {
        Collection<BgpRouteEntry> bgpRoutes =
            bgpSessionManager.getBgpRoutes4();

        final int maxChecks = 500;              // Max wait of 5 seconds
        for (int i = 0; i < maxChecks; i++) {
            if (bgpRoutes.size() == expectedRoutes) {
                break;
            }
            Thread.sleep(10);
            bgpRoutes = bgpSessionManager.getBgpRoutes4();
        }

        return bgpRoutes;
    }

    /**
     * Gets a merged BGP route by waiting until it is received.
     * <p>
     * NOTE: We keep checking once every 10ms whether the route is received,
     * up to 5 seconds.
     * </p>
     *
     * @param expectedRoute the expected route
     * @return the merged BGP route if received within the expected time
     * interval, otherwise null
     */
    private BgpRouteEntry waitForBgpRoute(BgpRouteEntry expectedRoute)
        throws InterruptedException {
        Collection<BgpRouteEntry> bgpRoutes =
            bgpSessionManager.getBgpRoutes4();

        final int maxChecks = 500;              // Max wait of 5 seconds
        for (int i = 0; i < maxChecks; i++) {
            for (BgpRouteEntry bgpRouteEntry : bgpRoutes) {
                if (bgpRouteEntry.equals(expectedRoute) &&
                    bgpRouteEntry.getBgpSession() ==
                    expectedRoute.getBgpSession()) {
                    return bgpRouteEntry;
                }
            }
            Thread.sleep(10);
            bgpRoutes = bgpSessionManager.getBgpRoutes4();
        }

        return null;
    }

    /**
     * Tests that the BGP OPEN messages have been exchanged, followed by
     * KEEPALIVE.
     * <p>
     * The BGP Peer opens the sessions and transmits OPEN Message, eventually
     * followed by KEEPALIVE. The tested BGP listener should respond by
     * OPEN Message, followed by KEEPALIVE.
     * </p>
     *
     * @throws TestUtilsException TestUtils error
     */
    @Test
    public void testExchangedBgpOpenMessages()
            throws InterruptedException, TestUtilsException {
        // Initiate the connections
        peer1.connect(connectToSocket);
        peer2.connect(connectToSocket);
        peer3.connect(connectToSocket);

        //
        // Test the fields from the BGP OPEN message:
        // BGP version, AS number, BGP ID
        //
        for (TestBgpPeer peer : peers) {
            assertThat(peer.peerFrameDecoder.remoteInfo.bgpVersion(),
                       is(BgpConstants.BGP_VERSION));
            assertThat(peer.peerFrameDecoder.remoteInfo.bgpId(),
                       is(IP_LOOPBACK_ID));
            assertThat(peer.peerFrameDecoder.remoteInfo.asNumber(),
                       is(TestBgpPeerChannelHandler.PEER_AS));
        }

        //
        // Test that the BgpSession instances have been created
        //
        assertThat(bgpSessionManager.getMyBgpId(), is(IP_LOOPBACK_ID));
        assertThat(bgpSessionManager.getBgpSessions(), hasSize(3));
        assertThat(bgpSession1, notNullValue());
        assertThat(bgpSession2, notNullValue());
        assertThat(bgpSession3, notNullValue());
        for (BgpSession bgpSession : bgpSessionManager.getBgpSessions()) {
            long sessionAs = bgpSession.localInfo().asNumber();
            assertThat(sessionAs, is(TestBgpPeerChannelHandler.PEER_AS));
        }
    }


    /**
     * Tests that the BGP OPEN with Capability messages have been exchanged,
     * followed by KEEPALIVE.
     * <p>
     * The BGP Peer opens the sessions and transmits OPEN Message, eventually
     * followed by KEEPALIVE. The tested BGP listener should respond by
     * OPEN Message, followed by KEEPALIVE.
     * </p>
     *
     * @throws TestUtilsException TestUtils error
     */
    @Test
    public void testExchangedBgpOpenCapabilityMessages()
            throws InterruptedException, TestUtilsException {
        //
        // Setup the BGP Capabilities for all peers
        //
        for (TestBgpPeer peer : peers) {
            peer.peerChannelHandler.localInfo.setIpv4Unicast();
            peer.peerChannelHandler.localInfo.setIpv4Multicast();
            peer.peerChannelHandler.localInfo.setIpv6Unicast();
            peer.peerChannelHandler.localInfo.setIpv6Multicast();
            peer.peerChannelHandler.localInfo.setAs4OctetCapability();
            peer.peerChannelHandler.localInfo.setAs4Number(
                TestBgpPeerChannelHandler.PEER_AS4);
        }

        // Initiate the connections
        peer1.connect(connectToSocket);
        peer2.connect(connectToSocket);
        peer3.connect(connectToSocket);

        //
        // Test the fields from the BGP OPEN message:
        // BGP version, BGP ID
        //
        for (TestBgpPeer peer : peers) {
            assertThat(peer.peerFrameDecoder.remoteInfo.bgpVersion(),
                       is(BgpConstants.BGP_VERSION));
            assertThat(peer.peerFrameDecoder.remoteInfo.bgpId(),
                       is(IP_LOOPBACK_ID));
        }

        //
        // Test that the BgpSession instances have been created,
        // and contain the appropriate BGP session information.
        //
        assertThat(bgpSessionManager.getMyBgpId(), is(IP_LOOPBACK_ID));
        assertThat(bgpSessionManager.getBgpSessions(), hasSize(3));
        assertThat(bgpSession1, notNullValue());
        assertThat(bgpSession2, notNullValue());
        assertThat(bgpSession3, notNullValue());
        for (BgpSession bgpSession : bgpSessionManager.getBgpSessions()) {
            BgpSessionInfo localInfo = bgpSession.localInfo();
            assertThat(localInfo.ipv4Unicast(), is(true));
            assertThat(localInfo.ipv4Multicast(), is(true));
            assertThat(localInfo.ipv6Unicast(), is(true));
            assertThat(localInfo.ipv6Multicast(), is(true));
            assertThat(localInfo.as4OctetCapability(), is(true));
            assertThat(localInfo.asNumber(),
                       is(TestBgpPeerChannelHandler.PEER_AS4));
            assertThat(localInfo.as4Number(),
                       is(TestBgpPeerChannelHandler.PEER_AS4));
        }
    }

    /**
     * Tests that the BGP UPDATE messages have been received and processed.
     */
    @Test
    public void testProcessedBgpUpdateMessages() throws InterruptedException {
        ChannelBuffer message;
        BgpRouteEntry bgpRouteEntry;
        Collection<BgpRouteEntry> bgpRibIn1;
        Collection<BgpRouteEntry> bgpRibIn2;
        Collection<BgpRouteEntry> bgpRibIn3;
        Collection<BgpRouteEntry> bgpRoutes;

        // Initiate the connections
        peer1.connect(connectToSocket);
        peer2.connect(connectToSocket);
        peer3.connect(connectToSocket);

        // Prepare routes to add/delete
        Collection<Ip4Prefix> addedRoutes = new LinkedList<>();
        Collection<Ip4Prefix> withdrawnRoutes = new LinkedList<>();

        //
        // Add and delete some routes
        //
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
        message = peer1.peerChannelHandler.prepareBgpUpdate(
                        NEXT_HOP1_ROUTER,
                        DEFAULT_LOCAL_PREF,
                        DEFAULT_MULTI_EXIT_DISC,
                        asPathLong,
                        addedRoutes,
                        withdrawnRoutes);
        peer1.peerChannelHandler.savedCtx.getChannel().write(message);
        //
        // Check that the routes have been received, processed and stored
        //
        bgpRibIn1 = waitForBgpRibIn(bgpSession1, 5);
        assertThat(bgpRibIn1, hasSize(5));
        bgpRoutes = waitForBgpRoutes(5);
        assertThat(bgpRoutes, hasSize(5));
        //
        bgpRouteEntry =
            new BgpRouteEntry(bgpSession1,
                              Ip4Prefix.valueOf("0.0.0.0/0"),
                              NEXT_HOP1_ROUTER,
                              (byte) BgpConstants.Update.Origin.IGP,
                              asPathLong,
                              DEFAULT_LOCAL_PREF);
        bgpRouteEntry.setMultiExitDisc(DEFAULT_MULTI_EXIT_DISC);
        assertThat(bgpRibIn1, hasBgpRouteEntry(bgpRouteEntry));
        assertThat(waitForBgpRoute(bgpRouteEntry), notNullValue());
        //
        bgpRouteEntry =
            new BgpRouteEntry(bgpSession1,
                              Ip4Prefix.valueOf("20.0.0.0/8"),
                              NEXT_HOP1_ROUTER,
                              (byte) BgpConstants.Update.Origin.IGP,
                              asPathLong,
                              DEFAULT_LOCAL_PREF);
        bgpRouteEntry.setMultiExitDisc(DEFAULT_MULTI_EXIT_DISC);
        assertThat(bgpRibIn1, hasBgpRouteEntry(bgpRouteEntry));
        assertThat(waitForBgpRoute(bgpRouteEntry), notNullValue());
        //
        bgpRouteEntry =
            new BgpRouteEntry(bgpSession1,
                              Ip4Prefix.valueOf("30.0.0.0/16"),
                              NEXT_HOP1_ROUTER,
                              (byte) BgpConstants.Update.Origin.IGP,
                              asPathLong,
                              DEFAULT_LOCAL_PREF);
        bgpRouteEntry.setMultiExitDisc(DEFAULT_MULTI_EXIT_DISC);
        assertThat(bgpRibIn1, hasBgpRouteEntry(bgpRouteEntry));
        assertThat(waitForBgpRoute(bgpRouteEntry), notNullValue());
        //
        bgpRouteEntry =
            new BgpRouteEntry(bgpSession1,
                              Ip4Prefix.valueOf("40.0.0.0/24"),
                              NEXT_HOP1_ROUTER,
                              (byte) BgpConstants.Update.Origin.IGP,
                              asPathLong,
                              DEFAULT_LOCAL_PREF);
        bgpRouteEntry.setMultiExitDisc(DEFAULT_MULTI_EXIT_DISC);
        assertThat(bgpRibIn1, hasBgpRouteEntry(bgpRouteEntry));
        assertThat(waitForBgpRoute(bgpRouteEntry), notNullValue());
        //
        bgpRouteEntry =
            new BgpRouteEntry(bgpSession1,
                              Ip4Prefix.valueOf("50.0.0.0/32"),
                              NEXT_HOP1_ROUTER,
                              (byte) BgpConstants.Update.Origin.IGP,
                              asPathLong,
                              DEFAULT_LOCAL_PREF);
        bgpRouteEntry.setMultiExitDisc(DEFAULT_MULTI_EXIT_DISC);
        assertThat(bgpRibIn1, hasBgpRouteEntry(bgpRouteEntry));
        assertThat(waitForBgpRoute(bgpRouteEntry), notNullValue());

        //
        // Delete some routes
        //
        addedRoutes = new LinkedList<>();
        withdrawnRoutes = new LinkedList<>();
        withdrawnRoutes.add(Ip4Prefix.valueOf("0.0.0.0/0"));
        withdrawnRoutes.add(Ip4Prefix.valueOf("50.0.0.0/32"));
        // Write the routes
        message = peer1.peerChannelHandler.prepareBgpUpdate(
                        NEXT_HOP1_ROUTER,
                        DEFAULT_LOCAL_PREF,
                        DEFAULT_MULTI_EXIT_DISC,
                        asPathLong,
                        addedRoutes,
                        withdrawnRoutes);
        peer1.peerChannelHandler.savedCtx.getChannel().write(message);
        //
        // Check that the routes have been received, processed and stored
        //
        bgpRibIn1 = waitForBgpRibIn(bgpSession1, 3);
        assertThat(bgpRibIn1, hasSize(3));
        bgpRoutes = waitForBgpRoutes(3);
        assertThat(bgpRoutes, hasSize(3));
        //
        bgpRouteEntry =
            new BgpRouteEntry(bgpSession1,
                              Ip4Prefix.valueOf("20.0.0.0/8"),
                              NEXT_HOP1_ROUTER,
                              (byte) BgpConstants.Update.Origin.IGP,
                              asPathLong,
                              DEFAULT_LOCAL_PREF);
        bgpRouteEntry.setMultiExitDisc(DEFAULT_MULTI_EXIT_DISC);
        assertThat(bgpRibIn1, hasBgpRouteEntry(bgpRouteEntry));
        assertThat(waitForBgpRoute(bgpRouteEntry), notNullValue());
        //
        bgpRouteEntry =
            new BgpRouteEntry(bgpSession1,
                              Ip4Prefix.valueOf("30.0.0.0/16"),
                              NEXT_HOP1_ROUTER,
                              (byte) BgpConstants.Update.Origin.IGP,
                              asPathLong,
                              DEFAULT_LOCAL_PREF);
        bgpRouteEntry.setMultiExitDisc(DEFAULT_MULTI_EXIT_DISC);
        assertThat(bgpRibIn1, hasBgpRouteEntry(bgpRouteEntry));
        assertThat(waitForBgpRoute(bgpRouteEntry), notNullValue());
        //
        bgpRouteEntry =
            new BgpRouteEntry(bgpSession1,
                              Ip4Prefix.valueOf("40.0.0.0/24"),
                              NEXT_HOP1_ROUTER,
                              (byte) BgpConstants.Update.Origin.IGP,
                              asPathLong,
                              DEFAULT_LOCAL_PREF);
        bgpRouteEntry.setMultiExitDisc(DEFAULT_MULTI_EXIT_DISC);
        assertThat(bgpRibIn1, hasBgpRouteEntry(bgpRouteEntry));
        assertThat(waitForBgpRoute(bgpRouteEntry), notNullValue());


        // Close the channels and test there are no routes
        peer1.peerChannelHandler.closeChannel();
        peer2.peerChannelHandler.closeChannel();
        peer3.peerChannelHandler.closeChannel();
        bgpRoutes = waitForBgpRoutes(0);
        assertThat(bgpRoutes, hasSize(0));
    }

    /**
     * Tests the BGP route preference.
     */
    @Test
    public void testBgpRoutePreference() throws InterruptedException {
        ChannelBuffer message;
        BgpRouteEntry bgpRouteEntry;
        Collection<BgpRouteEntry> bgpRibIn1;
        Collection<BgpRouteEntry> bgpRibIn2;
        Collection<BgpRouteEntry> bgpRibIn3;
        Collection<BgpRouteEntry> bgpRoutes;
        Collection<Ip4Prefix> addedRoutes = new LinkedList<>();
        Collection<Ip4Prefix> withdrawnRoutes = new LinkedList<>();

        // Initiate the connections
        peer1.connect(connectToSocket);
        peer2.connect(connectToSocket);
        peer3.connect(connectToSocket);

        //
        // Setup the initial set of routes to Peer1
        //
        addedRoutes.add(Ip4Prefix.valueOf("20.0.0.0/8"));
        addedRoutes.add(Ip4Prefix.valueOf("30.0.0.0/16"));
        // Write the routes
        message = peer1.peerChannelHandler.prepareBgpUpdate(
                        NEXT_HOP1_ROUTER,
                        DEFAULT_LOCAL_PREF,
                        DEFAULT_MULTI_EXIT_DISC,
                        asPathLong,
                        addedRoutes,
                        withdrawnRoutes);
        peer1.peerChannelHandler.savedCtx.getChannel().write(message);
        bgpRoutes = waitForBgpRoutes(2);
        assertThat(bgpRoutes, hasSize(2));

        //
        // Add a route entry to Peer2 with a better LOCAL_PREF
        //
        addedRoutes = new LinkedList<>();
        withdrawnRoutes = new LinkedList<>();
        addedRoutes.add(Ip4Prefix.valueOf("20.0.0.0/8"));
        // Write the routes
        message = peer2.peerChannelHandler.prepareBgpUpdate(
                        NEXT_HOP2_ROUTER,
                        BETTER_LOCAL_PREF,
                        DEFAULT_MULTI_EXIT_DISC,
                        asPathLong,
                        addedRoutes,
                        withdrawnRoutes);
        peer2.peerChannelHandler.savedCtx.getChannel().write(message);
        //
        // Check that the routes have been received, processed and stored
        //
        bgpRibIn2 = waitForBgpRibIn(bgpSession2, 1);
        assertThat(bgpRibIn2, hasSize(1));
        bgpRoutes = waitForBgpRoutes(2);
        assertThat(bgpRoutes, hasSize(2));
        //
        bgpRouteEntry =
            new BgpRouteEntry(bgpSession2,
                              Ip4Prefix.valueOf("20.0.0.0/8"),
                              NEXT_HOP2_ROUTER,
                              (byte) BgpConstants.Update.Origin.IGP,
                              asPathLong,
                              BETTER_LOCAL_PREF);
        bgpRouteEntry.setMultiExitDisc(DEFAULT_MULTI_EXIT_DISC);
        assertThat(bgpRibIn2, hasBgpRouteEntry(bgpRouteEntry));
        assertThat(waitForBgpRoute(bgpRouteEntry), notNullValue());

        //
        // Add a route entry to Peer3 with a shorter AS path
        //
        addedRoutes = new LinkedList<>();
        withdrawnRoutes = new LinkedList<>();
        addedRoutes.add(Ip4Prefix.valueOf("20.0.0.0/8"));
        // Write the routes
        message = peer3.peerChannelHandler.prepareBgpUpdate(
                        NEXT_HOP3_ROUTER,
                        BETTER_LOCAL_PREF,
                        DEFAULT_MULTI_EXIT_DISC,
                        asPathShort,
                        addedRoutes,
                        withdrawnRoutes);
        peer3.peerChannelHandler.savedCtx.getChannel().write(message);
        //
        // Check that the routes have been received, processed and stored
        //
        bgpRibIn3 = waitForBgpRibIn(bgpSession3, 1);
        assertThat(bgpRibIn3, hasSize(1));
        bgpRoutes = waitForBgpRoutes(2);
        assertThat(bgpRoutes, hasSize(2));
        //
        bgpRouteEntry =
            new BgpRouteEntry(bgpSession3,
                              Ip4Prefix.valueOf("20.0.0.0/8"),
                              NEXT_HOP3_ROUTER,
                              (byte) BgpConstants.Update.Origin.IGP,
                              asPathShort,
                              BETTER_LOCAL_PREF);
        bgpRouteEntry.setMultiExitDisc(DEFAULT_MULTI_EXIT_DISC);
        assertThat(bgpRibIn3, hasBgpRouteEntry(bgpRouteEntry));
        assertThat(waitForBgpRoute(bgpRouteEntry), notNullValue());

        //
        // Cleanup in preparation for next test: delete old route entry from
        // Peer2
        //
        addedRoutes = new LinkedList<>();
        withdrawnRoutes = new LinkedList<>();
        withdrawnRoutes.add(Ip4Prefix.valueOf("20.0.0.0/8"));
        // Write the routes
        message = peer2.peerChannelHandler.prepareBgpUpdate(
                        NEXT_HOP2_ROUTER,
                        BETTER_LOCAL_PREF,
                        BETTER_MULTI_EXIT_DISC,
                        asPathShort,
                        addedRoutes,
                        withdrawnRoutes);
        peer2.peerChannelHandler.savedCtx.getChannel().write(message);
        //
        // Check that the routes have been received, processed and stored
        //
        bgpRibIn2 = waitForBgpRibIn(bgpSession2, 0);
        assertThat(bgpRibIn2, hasSize(0));

        //
        // Add a route entry to Peer2 with a better MED
        //
        addedRoutes = new LinkedList<>();
        withdrawnRoutes = new LinkedList<>();
        addedRoutes.add(Ip4Prefix.valueOf("20.0.0.0/8"));
        // Write the routes
        message = peer2.peerChannelHandler.prepareBgpUpdate(
                        NEXT_HOP2_ROUTER,
                        BETTER_LOCAL_PREF,
                        BETTER_MULTI_EXIT_DISC,
                        asPathShort,
                        addedRoutes,
                        withdrawnRoutes);
        peer2.peerChannelHandler.savedCtx.getChannel().write(message);
        //
        // Check that the routes have been received, processed and stored
        //
        bgpRibIn2 = waitForBgpRibIn(bgpSession2, 1);
        assertThat(bgpRibIn2, hasSize(1));
        bgpRoutes = waitForBgpRoutes(2);
        assertThat(bgpRoutes, hasSize(2));
        //
        bgpRouteEntry =
            new BgpRouteEntry(bgpSession2,
                              Ip4Prefix.valueOf("20.0.0.0/8"),
                              NEXT_HOP2_ROUTER,
                              (byte) BgpConstants.Update.Origin.IGP,
                              asPathShort,
                              BETTER_LOCAL_PREF);
        bgpRouteEntry.setMultiExitDisc(BETTER_MULTI_EXIT_DISC);
        assertThat(bgpRibIn2, hasBgpRouteEntry(bgpRouteEntry));
        assertThat(waitForBgpRoute(bgpRouteEntry), notNullValue());

        //
        // Add a route entry to Peer1 with a better (lower) BGP ID
        //
        addedRoutes = new LinkedList<>();
        withdrawnRoutes = new LinkedList<>();
        addedRoutes.add(Ip4Prefix.valueOf("20.0.0.0/8"));
        withdrawnRoutes.add(Ip4Prefix.valueOf("30.0.0.0/16"));
        // Write the routes
        message = peer1.peerChannelHandler.prepareBgpUpdate(
                        NEXT_HOP1_ROUTER,
                        BETTER_LOCAL_PREF,
                        BETTER_MULTI_EXIT_DISC,
                        asPathShort,
                        addedRoutes,
                        withdrawnRoutes);
        peer1.peerChannelHandler.savedCtx.getChannel().write(message);
        //
        // Check that the routes have been received, processed and stored
        //
        bgpRibIn1 = waitForBgpRibIn(bgpSession1, 1);
        assertThat(bgpRibIn1, hasSize(1));
        bgpRoutes = waitForBgpRoutes(1);
        assertThat(bgpRoutes, hasSize(1));
        //
        bgpRouteEntry =
            new BgpRouteEntry(bgpSession1,
                              Ip4Prefix.valueOf("20.0.0.0/8"),
                              NEXT_HOP1_ROUTER,
                              (byte) BgpConstants.Update.Origin.IGP,
                              asPathShort,
                              BETTER_LOCAL_PREF);
        bgpRouteEntry.setMultiExitDisc(BETTER_MULTI_EXIT_DISC);
        assertThat(bgpRibIn1, hasBgpRouteEntry(bgpRouteEntry));
        assertThat(waitForBgpRoute(bgpRouteEntry), notNullValue());


        // Close the channels and test there are no routes
        peer1.peerChannelHandler.closeChannel();
        peer2.peerChannelHandler.closeChannel();
        peer3.peerChannelHandler.closeChannel();
        bgpRoutes = waitForBgpRoutes(0);
        assertThat(bgpRoutes, hasSize(0));
    }
}
