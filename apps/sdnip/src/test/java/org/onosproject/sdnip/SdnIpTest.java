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
package org.onosproject.sdnip;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.onlab.junit.IntegrationTest;
import org.onlab.junit.TestUtils;
import org.onlab.junit.TestUtils.TestUtilsException;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.sdnip.config.BgpPeer;
import org.onosproject.sdnip.config.Interface;
import org.onosproject.sdnip.config.SdnIpConfigurationService;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.MacAddress;

import com.google.common.collect.Sets;

/**
 * Integration tests for the SDN-IP application.
 * <p/>
 * The tests are very coarse-grained. They feed route updates in to
 * {@link Router} (simulating routes learnt from iBGP module inside SDN-IP
 * application), then they check that the correct intents are created and
 * submitted to the intent service. The entire route processing logic of
 * Router class is tested.
 */
@Category(IntegrationTest.class)
public class SdnIpTest extends AbstractIntentTest {
    private static final int MAC_ADDRESS_LENGTH = 6;
    private static final int MIN_PREFIX_LENGTH = 1;
    private static final int MAX_PREFIX_LENGTH = 32;

    private IntentSynchronizer intentSynchronizer;
    static Router router;

    private SdnIpConfigurationService sdnIpConfigService;
    private InterfaceService interfaceService;
    private HostService hostService;
    private IntentService intentService;

    private Map<IpAddress, BgpPeer> bgpPeers;

    private Random random;

    static final ConnectPoint SW1_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000001"),
            PortNumber.portNumber(1));

    static final ConnectPoint SW2_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000002"),
            PortNumber.portNumber(1));

    static final ConnectPoint SW3_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000003"),
            PortNumber.portNumber(1));

    private static final ApplicationId APPID = new ApplicationId() {
        @Override
        public short id() {
            return 1;
        }

        @Override
        public String name() {
            return "SDNIP";
        }
    };

    @Before
    public void setUp() throws Exception {
        super.setUp();

        setUpInterfaceService();
        setUpSdnIpConfigService();

        hostService = new TestHostService();
        intentService = createMock(IntentService.class);
        random = new Random();

        intentSynchronizer = new IntentSynchronizer(APPID, intentService);
        router = new Router(APPID, intentSynchronizer, sdnIpConfigService,
                            interfaceService, hostService);
    }

    /**
     * Sets up InterfaceService and virtual {@link Interface}s.
     */
    private void setUpInterfaceService() {

        interfaceService = createMock(InterfaceService.class);

        Set<Interface> interfaces = Sets.newHashSet();

        Set<InterfaceIpAddress> interfaceIpAddresses1 = Sets.newHashSet();
        interfaceIpAddresses1.add(new InterfaceIpAddress(
                IpAddress.valueOf("192.168.10.101"),
                IpPrefix.valueOf("192.168.10.0/24")));
        Interface sw1Eth1 = new Interface(SW1_ETH1,
                interfaceIpAddresses1, MacAddress.valueOf("00:00:00:00:00:01"));
        interfaces.add(sw1Eth1);

        Set<InterfaceIpAddress> interfaceIpAddresses2 = Sets.newHashSet();
        interfaceIpAddresses2.add(new InterfaceIpAddress(
                IpAddress.valueOf("192.168.20.101"),
                IpPrefix.valueOf("192.168.20.0/24")));
        Interface sw2Eth1 = new Interface(SW2_ETH1,
                interfaceIpAddresses2, MacAddress.valueOf("00:00:00:00:00:02"));
        interfaces.add(sw2Eth1);

        Set<InterfaceIpAddress> interfaceIpAddresses3 = Sets.newHashSet();
        interfaceIpAddresses3.add(new InterfaceIpAddress(
                IpAddress.valueOf("192.168.30.101"),
                IpPrefix.valueOf("192.168.30.0/24")));
        Interface sw3Eth1 = new Interface(SW3_ETH1,
                interfaceIpAddresses3, MacAddress.valueOf("00:00:00:00:00:03"));
        interfaces.add(sw3Eth1);

        expect(interfaceService.getInterface(SW1_ETH1)).andReturn(
                sw1Eth1).anyTimes();
        expect(interfaceService.getInterface(SW2_ETH1)).andReturn(
                sw2Eth1).anyTimes();
        expect(interfaceService.getInterface(SW3_ETH1)).andReturn(
                sw3Eth1).anyTimes();

        expect(interfaceService.getInterfaces()).andReturn(
                interfaces).anyTimes();
        replay(interfaceService);
    }

    /**
     * Sets up SdnIpConfigService and BGP peers in external networks.
     */
    private void setUpSdnIpConfigService() {

        sdnIpConfigService = createMock(SdnIpConfigurationService.class);

        bgpPeers = new HashMap<>();

        String peerSw1Eth1 = "192.168.10.1";
        bgpPeers.put(IpAddress.valueOf(peerSw1Eth1),
                new BgpPeer("00:00:00:00:00:00:00:01", 1, peerSw1Eth1));

        String peer1Sw2Eth1 = "192.168.20.1";
        bgpPeers.put(IpAddress.valueOf(peer1Sw2Eth1),
                new BgpPeer("00:00:00:00:00:00:00:02", 1, peer1Sw2Eth1));

        String peer2Sw2Eth1 = "192.168.30.1";
        bgpPeers.put(IpAddress.valueOf(peer2Sw2Eth1),
                new BgpPeer("00:00:00:00:00:00:00:03", 1, peer2Sw2Eth1));

        expect(sdnIpConfigService.getBgpPeers()).andReturn(bgpPeers).anyTimes();
        replay(sdnIpConfigService);
    }

    /**
     * Tests adding a set of routes into {@link Router}.
     * <p/>
     * Random routes are generated and fed in to the route processing
     * logic (via processRouteAdd in Router class). We check that the correct
     * intents are generated and submitted to our mock intent service.
     *
     * @throws InterruptedException if interrupted while waiting on a latch
     * @throws TestUtilsException if exceptions when using TestUtils
     */
    @Test
    public void testAddRoutes() throws InterruptedException, TestUtilsException {
        int numRoutes = 100;

        final CountDownLatch latch = new CountDownLatch(numRoutes);

        List<RouteUpdate> routeUpdates = generateRouteUpdates(numRoutes);

        // Set up expectation
        reset(intentService);

        for (RouteUpdate update : routeUpdates) {
            IpAddress nextHopAddress = update.routeEntry().nextHop();

            // Find out the egress ConnectPoint
            ConnectPoint egressConnectPoint = getConnectPoint(nextHopAddress);

            MultiPointToSinglePointIntent intent = getIntentForUpdate(update,
                    generateMacAddress(nextHopAddress),
                    egressConnectPoint);
            intentService.submit(TestIntentServiceHelper.eqExceptId(intent));

            expectLastCall().andAnswer(new IAnswer<Object>() {
                @Override
                public Object answer() throws Throwable {
                    latch.countDown();
                    return null;
                }
            }).once();
        }

        replay(intentService);

        intentSynchronizer.leaderChanged(true);
        TestUtils.setField(intentSynchronizer, "isActivatedLeader", true);

        // Add route updates
        router.processRouteUpdates(routeUpdates);

        latch.await(5000, TimeUnit.MILLISECONDS);

        assertEquals(router.getRoutes4().size(), numRoutes);
        assertEquals(intentSynchronizer.getRouteIntents().size(),
                     numRoutes);

        verify(intentService);
    }

    /**
     * Tests adding then deleting a set of routes from {@link Router}.
     * <p/>
     * Random routes are generated and fed in to the route processing
     * logic (via processRouteAdd in Router class), and we check that the
     * correct intents are generated. We then delete the entire set of routes
     * (by feeding updates to processRouteDelete), and check that the correct
     * intents are withdrawn from the intent service.
     *
     * @throws InterruptedException if interrupted while waiting on a latch
     * @throws TestUtilsException exceptions when using TestUtils
     */
    @Test
    public void testDeleteRoutes() throws InterruptedException, TestUtilsException {
        int numRoutes = 100;
        List<RouteUpdate> routeUpdates = generateRouteUpdates(numRoutes);

        final CountDownLatch installCount = new CountDownLatch(numRoutes);
        final CountDownLatch deleteCount = new CountDownLatch(numRoutes);

        // Set up expectation
        reset(intentService);

        for (RouteUpdate update : routeUpdates) {
            IpAddress nextHopAddress = update.routeEntry().nextHop();

            // Find out the egress ConnectPoint
            ConnectPoint egressConnectPoint = getConnectPoint(nextHopAddress);
            MultiPointToSinglePointIntent intent = getIntentForUpdate(update,
                    generateMacAddress(nextHopAddress),
                    egressConnectPoint);
            intentService.submit(TestIntentServiceHelper.eqExceptId(intent));
            expectLastCall().andAnswer(new IAnswer<Object>() {
                @Override
                public Object answer() throws Throwable {
                    installCount.countDown();
                    return null;
                }
            }).once();
            intentService.withdraw(TestIntentServiceHelper.eqExceptId(intent));
            expectLastCall().andAnswer(new IAnswer<Object>() {
                @Override
                public Object answer() throws Throwable {
                    deleteCount.countDown();
                    return null;
                }
            }).once();
        }

        replay(intentService);

        intentSynchronizer.leaderChanged(true);
        TestUtils.setField(intentSynchronizer, "isActivatedLeader", true);

        // Send the add updates first
        router.processRouteUpdates(routeUpdates);

        // Give some time to let the intents be submitted
        installCount.await(5000, TimeUnit.MILLISECONDS);

        // Send the DELETE updates
        List<RouteUpdate> deleteRouteUpdates = new ArrayList<>();
        for (RouteUpdate update : routeUpdates) {
            RouteUpdate deleteUpdate = new RouteUpdate(RouteUpdate.Type.DELETE,
                                                       update.routeEntry());
            deleteRouteUpdates.add(deleteUpdate);
        }
        router.processRouteUpdates(deleteRouteUpdates);

        deleteCount.await(5000, TimeUnit.MILLISECONDS);

        assertEquals(0, router.getRoutes4().size());
        assertEquals(0, intentSynchronizer.getRouteIntents().size());
        verify(intentService);
    }

    /**
     * This methods generates random route updates.
     *
     * @param numRoutes the number of route updates to generate
     * @return a list of route update
     */
    private List<RouteUpdate> generateRouteUpdates(int numRoutes) {
        List<RouteUpdate> routeUpdates = new ArrayList<>(numRoutes);

        Set<Ip4Prefix> prefixes = new HashSet<>();

        for (int i = 0; i < numRoutes; i++) {
            Ip4Prefix prefix;
            do {
                // Generate a random prefix length between MIN_PREFIX_LENGTH
                // and MAX_PREFIX_LENGTH
                int prefixLength = random.nextInt(
                        (MAX_PREFIX_LENGTH - MIN_PREFIX_LENGTH) + 1)
                        + MIN_PREFIX_LENGTH;
                prefix =
                    Ip4Prefix.valueOf(Ip4Address.valueOf(random.nextInt()),
                                      prefixLength);
                // We have to ensure we don't generate the same prefix twice
                // (this is quite easy to happen with small prefix lengths).
            } while (prefixes.contains(prefix));

            prefixes.add(prefix);

            // Randomly select a peer to use as the next hop
            BgpPeer nextHop = null;
            int peerNumber = random.nextInt(sdnIpConfigService.getBgpPeers()
                    .size());
            int j = 0;
            for (BgpPeer peer : sdnIpConfigService.getBgpPeers().values()) {
                if (j++ == peerNumber) {
                    nextHop = peer;
                    break;
                }
            }

            assertNotNull(nextHop);

            RouteUpdate update =
                new RouteUpdate(RouteUpdate.Type.UPDATE,
                        new RouteEntry(prefix,
                                       nextHop.ipAddress().getIp4Address()));

            routeUpdates.add(update);
        }

        return routeUpdates;
    }

    /**
     * Generates the MultiPointToSinglePointIntent that should be
     * submitted/withdrawn for a particular RouteUpdate.
     *
     * @param update the RouteUpdate to generate an intent for
     * @param nextHopMac a MAC address to use as the dst-mac for the intent
     * @param egressConnectPoint the outgoing ConnectPoint for the intent
     * @return the generated intent
     */
    private MultiPointToSinglePointIntent getIntentForUpdate(RouteUpdate update,
            MacAddress nextHopMac, ConnectPoint egressConnectPoint) {
        IpPrefix ip4Prefix = update.routeEntry().prefix();

        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();

        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(ip4Prefix);

        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(nextHopMac);

        Set<ConnectPoint> ingressPoints = new HashSet<ConnectPoint>();
        for (Interface intf : interfaceService.getInterfaces()) {
            if (!intf.connectPoint().equals(egressConnectPoint)) {
                ConnectPoint srcPort = intf.connectPoint();
                ingressPoints.add(srcPort);
            }
        }

        MultiPointToSinglePointIntent intent =
                new MultiPointToSinglePointIntent(APPID,
                selectorBuilder.build(), treatmentBuilder.build(),
                ingressPoints, egressConnectPoint);

        return intent;
    }

    /**
     * Generates a MAC address based on an IP address.
     * For the test we need MAC addresses but the actual values don't have any
     * meaning, so we'll just generate them based on the IP address. This means
     * we have a deterministic mapping from IP address to MAC address.
     *
     * @param ipAddress IP address used to generate a MAC address
     * @return generated MAC address
     */
    static MacAddress generateMacAddress(IpAddress ipAddress) {
        byte[] macAddress = new byte[MAC_ADDRESS_LENGTH];
        ByteBuffer bb = ByteBuffer.wrap(macAddress);

        // Put the IP address bytes into the lower four bytes of the MAC
        // address. Leave the first two bytes set to 0.
        bb.position(2);
        bb.put(ipAddress.toOctets());

        return MacAddress.valueOf(bb.array());
    }

    /**
     * Finds out the ConnectPoint for a BGP peer address.
     *
     * @param bgpPeerAddress the BGP peer address.
     */
    private ConnectPoint getConnectPoint(IpAddress bgpPeerAddress) {
        ConnectPoint connectPoint = null;

        for (BgpPeer bgpPeer: bgpPeers.values()) {
            if (bgpPeer.ipAddress().equals(bgpPeerAddress)) {
                connectPoint = bgpPeer.connectPoint();
                break;
            }
        }
        return connectPoint;
    }
}
