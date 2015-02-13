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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.junit.TestUtils.TestUtilsException;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentOperations;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.sdnip.IntentSynchronizer.IntentKey;
import org.onosproject.sdnip.Router.InternalHostListener;
import org.onosproject.sdnip.config.BgpPeer;
import org.onosproject.sdnip.config.Interface;
import org.onosproject.sdnip.config.SdnIpConfigurationService;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import com.google.common.collect.Sets;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;

/**
 * This class tests adding a route, updating a route, deleting a route, and
 * the ARP module answers the MAC address asynchronously.
 */
public class RouterAsyncArpTest extends AbstractIntentTest {

    private SdnIpConfigurationService sdnIpConfigService;
    private InterfaceService interfaceService;
    private IntentService intentService;
    private HostService hostService;

    private static final ConnectPoint SW1_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000001"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW2_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000002"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW3_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000003"),
            PortNumber.portNumber(1));

    private IntentSynchronizer intentSynchronizer;
    private Router router;
    private InternalHostListener internalHostListener;

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

        setUpSdnIpConfigService();
        setUpInterfaceService();
        hostService = createMock(HostService.class);
        intentService = createMock(IntentService.class);

        intentSynchronizer = new IntentSynchronizer(APPID, intentService);
        router = new Router(APPID, intentSynchronizer,
                            sdnIpConfigService, interfaceService, hostService);
        internalHostListener = router.new InternalHostListener();
    }

    /**
     * Sets up SdnIpConfigService.
     */
    private void setUpSdnIpConfigService() {

        sdnIpConfigService = createMock(SdnIpConfigurationService.class);

        Map<IpAddress, BgpPeer> peers = new HashMap<>();

        String peerSw1Eth1 = "192.168.10.1";
        peers.put(IpAddress.valueOf(peerSw1Eth1),
                new BgpPeer("00:00:00:00:00:00:00:01", 1, peerSw1Eth1));

        // Two BGP peers are connected to switch 2 port 1.
        String peer1Sw2Eth1 = "192.168.20.1";
        peers.put(IpAddress.valueOf(peer1Sw2Eth1),
                new BgpPeer("00:00:00:00:00:00:00:02", 1, peer1Sw2Eth1));

        String peer2Sw2Eth1 = "192.168.20.2";
        peers.put(IpAddress.valueOf(peer2Sw2Eth1),
                new BgpPeer("00:00:00:00:00:00:00:02", 1, peer2Sw2Eth1));

        expect(sdnIpConfigService.getBgpPeers()).andReturn(peers).anyTimes();
        replay(sdnIpConfigService);
    }

    /**
     * Sets up InterfaceService.
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

        expect(interfaceService.getInterface(SW1_ETH1)).andReturn(sw1Eth1).anyTimes();
        expect(interfaceService.getInterface(SW2_ETH1)).andReturn(sw2Eth1).anyTimes();
        expect(interfaceService.getInterface(SW3_ETH1)).andReturn(sw3Eth1).anyTimes();
        expect(interfaceService.getInterfaces()).andReturn(interfaces).anyTimes();
        replay(interfaceService);
    }

    /**
     * This method tests adding a route entry.
     */
    @Test
    public void testRouteAdd() throws TestUtilsException {

        // Construct a route entry
        RouteEntry routeEntry = new RouteEntry(
                Ip4Prefix.valueOf("1.1.1.0/24"),
                Ip4Address.valueOf("192.168.10.1"));

        // Construct a route intent
        MultiPointToSinglePointIntent intent = staticIntentBuilder();

        // Set up test expectation
        reset(hostService);
        expect(hostService.getHostsByIp(anyObject(IpAddress.class))).andReturn(
                new HashSet<Host>()).anyTimes();
        hostService.startMonitoringIp(IpAddress.valueOf("192.168.10.1"));
        replay(hostService);

        reset(intentService);
        IntentOperations.Builder builder = IntentOperations.builder(APPID);
        builder.addSubmitOperation(intent);
        intentService.execute(TestIntentServiceHelper.eqExceptId(
                                builder.build()));
        replay(intentService);

        // Call the processRouteUpdates() method in Router class
        intentSynchronizer.leaderChanged(true);
        TestUtils.setField(intentSynchronizer, "isActivatedLeader", true);
        RouteUpdate routeUpdate = new RouteUpdate(RouteUpdate.Type.UPDATE,
                                                  routeEntry);
        router.processRouteUpdates(Collections.<RouteUpdate>singletonList(routeUpdate));

        Host host = new DefaultHost(ProviderId.NONE, HostId.NONE,
                MacAddress.valueOf("00:00:00:00:00:01"), VlanId.NONE,
                new HostLocation(
                        SW1_ETH1.deviceId(),
                        SW1_ETH1.port(), 1),
                        Sets.newHashSet(IpAddress.valueOf("192.168.10.1")));
        internalHostListener.event(
                new HostEvent(HostEvent.Type.HOST_ADDED, host));

        // Verify
        assertEquals(router.getRoutes4().size(), 1);
        assertTrue(router.getRoutes4().contains(routeEntry));
        assertEquals(intentSynchronizer.getRouteIntents().size(), 1);
        Intent firstIntent =
            intentSynchronizer.getRouteIntents().iterator().next();
        IntentKey firstIntentKey = new IntentKey(firstIntent);
        IntentKey intentKey = new IntentKey(intent);
        assertTrue(firstIntentKey.equals(intentKey));
        verify(intentService);
        verify(hostService);

    }

    /**
     * This method tests updating a route entry.
     *
     * @throws TestUtilsException
     */
    @Test
    public void testRouteUpdate() throws TestUtilsException {

        // Construct the existing route entry
        RouteEntry routeEntry = new RouteEntry(
                Ip4Prefix.valueOf("1.1.1.0/24"),
                Ip4Address.valueOf("192.168.10.1"));

        // Construct the existing MultiPointToSinglePointIntent intent
        MultiPointToSinglePointIntent intent = staticIntentBuilder();

        // Set up the ribTable field of Router class with existing route, and
        // routeIntents field with the corresponding existing intent
        setRibTableField(routeEntry);
        setRouteIntentsField(routeEntry, intent);

        // Start to construct a new route entry and new intent
        RouteEntry routeEntryUpdate = new RouteEntry(
                Ip4Prefix.valueOf("1.1.1.0/24"),
                Ip4Address.valueOf("192.168.20.1"));

        // Construct a new MultiPointToSinglePointIntent intent
        TrafficSelector.Builder selectorBuilderNew =
                DefaultTrafficSelector.builder();
        selectorBuilderNew.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(
                routeEntryUpdate.prefix());

        TrafficTreatment.Builder treatmentBuilderNew =
                DefaultTrafficTreatment.builder();
        treatmentBuilderNew.setEthDst(MacAddress.valueOf("00:00:00:00:00:02"));

        Set<ConnectPoint> ingressPointsNew = new HashSet<ConnectPoint>();
        ingressPointsNew.add(SW1_ETH1);
        ingressPointsNew.add(SW3_ETH1);

        MultiPointToSinglePointIntent intentNew =
                new MultiPointToSinglePointIntent(APPID,
                        selectorBuilderNew.build(),
                        treatmentBuilderNew.build(),
                        ingressPointsNew, SW2_ETH1);

        // Set up test expectation
        reset(hostService);
        expect(hostService.getHostsByIp(anyObject(IpAddress.class))).andReturn(
                new HashSet<Host>()).anyTimes();
        hostService.startMonitoringIp(IpAddress.valueOf("192.168.20.1"));
        replay(hostService);

        reset(intentService);
        IntentOperations.Builder builder = IntentOperations.builder(APPID);
        builder.addWithdrawOperation(intent.id());
        intentService.execute(TestIntentServiceHelper.eqExceptId(
                                builder.build()));
        builder = IntentOperations.builder(APPID);
        builder.addSubmitOperation(intentNew);
        intentService.execute(TestIntentServiceHelper.eqExceptId(
                                builder.build()));
        replay(intentService);

        // Call the processRouteUpdates() method in Router class
        intentSynchronizer.leaderChanged(true);
        TestUtils.setField(intentSynchronizer, "isActivatedLeader", true);
        RouteUpdate routeUpdate = new RouteUpdate(RouteUpdate.Type.UPDATE,
                                                  routeEntryUpdate);
        router.processRouteUpdates(Collections.<RouteUpdate>singletonList(routeUpdate));

        Host host = new DefaultHost(ProviderId.NONE, HostId.NONE,
                MacAddress.valueOf("00:00:00:00:00:02"), VlanId.NONE,
                new HostLocation(
                        SW2_ETH1.deviceId(),
                        SW2_ETH1.port(), 1),
                        Sets.newHashSet(IpAddress.valueOf("192.168.20.1")));
        internalHostListener.event(
                new HostEvent(HostEvent.Type.HOST_ADDED, host));

        // Verify
        assertEquals(router.getRoutes4().size(), 1);
        assertTrue(router.getRoutes4().contains(routeEntryUpdate));
        assertEquals(intentSynchronizer.getRouteIntents().size(), 1);
        Intent firstIntent =
            intentSynchronizer.getRouteIntents().iterator().next();
        IntentKey firstIntentKey = new IntentKey(firstIntent);
        IntentKey intentNewKey = new IntentKey(intentNew);
        assertTrue(firstIntentKey.equals(intentNewKey));
        verify(intentService);
        verify(hostService);
    }

    /**
     * This method tests deleting a route entry.
     */
    @Test
    public void testRouteDelete() throws TestUtilsException {

        // Construct the existing route entry
        RouteEntry routeEntry = new RouteEntry(
                Ip4Prefix.valueOf("1.1.1.0/24"),
                Ip4Address.valueOf("192.168.10.1"));

        // Construct the existing MultiPointToSinglePointIntent intent
        MultiPointToSinglePointIntent intent = staticIntentBuilder();

        // Set up the ribTable field of Router class with existing route, and
        // routeIntents field with the corresponding existing intent
        setRibTableField(routeEntry);
        setRouteIntentsField(routeEntry, intent);

        // Set up expectation
        reset(intentService);
        IntentOperations.Builder builder = IntentOperations.builder(APPID);
        builder.addWithdrawOperation(intent.id());
        intentService.execute(TestIntentServiceHelper.eqExceptId(
                                builder.build()));
        replay(intentService);

        // Call the processRouteUpdates() method in Router class
        intentSynchronizer.leaderChanged(true);
        TestUtils.setField(intentSynchronizer, "isActivatedLeader", true);
        RouteUpdate routeUpdate = new RouteUpdate(RouteUpdate.Type.DELETE,
                                                  routeEntry);
        router.processRouteUpdates(Collections.<RouteUpdate>singletonList(routeUpdate));

        // Verify
        assertEquals(router.getRoutes4().size(), 0);
        assertEquals(intentSynchronizer.getRouteIntents().size(), 0);
        verify(intentService);
    }

    /**
     * Constructs a static MultiPointToSinglePointIntent.
     */
    private MultiPointToSinglePointIntent staticIntentBuilder() {

        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(
                IpPrefix.valueOf("1.1.1.0/24"));

        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(MacAddress.valueOf("00:00:00:00:00:01"));

        Set<ConnectPoint> ingressPoints = new HashSet<ConnectPoint>();
        ingressPoints.add(SW2_ETH1);
        ingressPoints.add(SW3_ETH1);

        MultiPointToSinglePointIntent intent =
                new MultiPointToSinglePointIntent(APPID,
                        selectorBuilder.build(), treatmentBuilder.build(),
                        ingressPoints, SW1_ETH1);

        return intent;
    }

    /**
     * Sets ribTable Field in Router class.
     *
     * @throws TestUtilsException
     */
    private void setRibTableField(RouteEntry routeEntry)
            throws TestUtilsException {

        InvertedRadixTree<RouteEntry> ribTable =
                new ConcurrentInvertedRadixTree<>(
                new DefaultByteArrayNodeFactory());
        ribTable.put(RouteEntry.createBinaryString(routeEntry.prefix()),
                     routeEntry);
        TestUtils.setField(router, "ribTable4", ribTable);
    }

    /**
     * Sets routeIntentsField in IntentSynchronizer class.
     *
     * @throws TestUtilsException
     */
    private void setRouteIntentsField(RouteEntry routeEntry,
            MultiPointToSinglePointIntent intent)
            throws TestUtilsException {

        ConcurrentHashMap<IpPrefix, MultiPointToSinglePointIntent>
            routeIntents =  new ConcurrentHashMap<>();
        routeIntents.put(routeEntry.prefix(), intent);
        TestUtils.setField(intentSynchronizer, "routeIntents", routeIntents);
    }
}
