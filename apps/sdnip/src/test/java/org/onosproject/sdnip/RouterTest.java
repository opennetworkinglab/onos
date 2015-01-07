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
import static org.easymock.EasyMock.expectLastCall;
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

import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.junit.TestUtils.TestUtilsException;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
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
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentOperations;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.sdnip.IntentSynchronizer.IntentKey;
import org.onosproject.sdnip.config.BgpPeer;
import org.onosproject.sdnip.config.Interface;
import org.onosproject.sdnip.config.SdnIpConfigurationService;

import com.google.common.collect.Sets;

/**
 * This class tests adding a route, updating a route, deleting a route,
 * and adding a route whose next hop is the local BGP speaker.
 * <p/>
 * ARP module answers the MAC address synchronously.
 */
public class RouterTest extends AbstractIntentTest {

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

    private IntentSynchronizer intentSynchronizer;
    private Router router;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        setUpBgpPeers();

        setUpInterfaceService();
        setUpHostService();

        intentService = createMock(IntentService.class);

        intentSynchronizer = new IntentSynchronizer(APPID, intentService);
        router = new Router(APPID, intentSynchronizer, sdnIpConfigService,
                            interfaceService, hostService);
    }

    /**
     * Sets up BGP peers in external networks.
     */
    private void setUpBgpPeers() {

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

        sdnIpConfigService = createMock(SdnIpConfigurationService.class);
        expect(sdnIpConfigService.getBgpPeers()).andReturn(peers).anyTimes();
        replay(sdnIpConfigService);

    }

    /**
     * Sets up logical interfaces, which emulate the configured interfaces
     * in SDN-IP application.
     */
    private void setUpInterfaceService() {
        interfaceService = createMock(InterfaceService.class);

        Set<Interface> interfaces = Sets.newHashSet();

        InterfaceIpAddress ia1 =
            new InterfaceIpAddress(IpAddress.valueOf("192.168.10.101"),
                                   IpPrefix.valueOf("192.168.10.0/24"));
        Interface sw1Eth1 = new Interface(SW1_ETH1,
                Sets.newHashSet(ia1),
                MacAddress.valueOf("00:00:00:00:00:01"));

        expect(interfaceService.getInterface(SW1_ETH1)).andReturn(sw1Eth1).anyTimes();
        interfaces.add(sw1Eth1);

        InterfaceIpAddress ia2 =
            new InterfaceIpAddress(IpAddress.valueOf("192.168.20.101"),
                                   IpPrefix.valueOf("192.168.20.0/24"));
        Interface sw2Eth1 = new Interface(SW2_ETH1,
                Sets.newHashSet(ia2),
                MacAddress.valueOf("00:00:00:00:00:02"));

        expect(interfaceService.getInterface(SW2_ETH1)).andReturn(sw2Eth1).anyTimes();
        interfaces.add(sw2Eth1);

        InterfaceIpAddress ia3 =
            new InterfaceIpAddress(IpAddress.valueOf("192.168.30.101"),
                                   IpPrefix.valueOf("192.168.30.0/24"));
        Interface sw3Eth1 = new Interface(SW3_ETH1,
                Sets.newHashSet(ia3),
                MacAddress.valueOf("00:00:00:00:00:03"));

        expect(interfaceService.getInterface(SW3_ETH1)).andReturn(sw3Eth1).anyTimes();
        interfaces.add(sw3Eth1);

        expect(interfaceService.getInterfaces()).andReturn(interfaces).anyTimes();

        replay(interfaceService);
    }

    /**
     * Sets up the host service with details of some hosts.
     */
    private void setUpHostService() {
        hostService = createMock(HostService.class);

        hostService.addListener(anyObject(HostListener.class));
        expectLastCall().anyTimes();

        IpAddress host1Address = IpAddress.valueOf("192.168.10.1");
        Host host1 = new DefaultHost(ProviderId.NONE, HostId.NONE,
                MacAddress.valueOf("00:00:00:00:00:01"), VlanId.NONE,
                new HostLocation(SW1_ETH1, 1),
                Sets.newHashSet(host1Address));

        expect(hostService.getHostsByIp(host1Address))
                .andReturn(Sets.newHashSet(host1)).anyTimes();
        hostService.startMonitoringIp(host1Address);
        expectLastCall().anyTimes();


        IpAddress host2Address = IpAddress.valueOf("192.168.20.1");
        Host host2 = new DefaultHost(ProviderId.NONE, HostId.NONE,
                MacAddress.valueOf("00:00:00:00:00:02"), VlanId.NONE,
                new HostLocation(SW2_ETH1, 1),
                Sets.newHashSet(host2Address));

        expect(hostService.getHostsByIp(host2Address))
                .andReturn(Sets.newHashSet(host2)).anyTimes();
        hostService.startMonitoringIp(host2Address);
        expectLastCall().anyTimes();


        replay(hostService);
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

        // Construct a MultiPointToSinglePointIntent intent
        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(
                routeEntry.prefix());

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

        // Set up test expectation
        reset(intentService);
        // Setup the expected intents
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
    }

    /**
     * This method tests updating a route entry.
     *
     * @throws TestUtilsException
     */
    @Test
    public void testRouteUpdate() throws TestUtilsException {
        // Firstly add a route
        testRouteAdd();

        Intent addedIntent =
            intentSynchronizer.getRouteIntents().iterator().next();

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
        reset(intentService);
        // Setup the expected intents
        IntentOperations.Builder builder = IntentOperations.builder(APPID);
        builder.addWithdrawOperation(addedIntent.id());
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
    }

    /**
     * This method tests deleting a route entry.
     */
    @Test
    public void testRouteDelete() throws TestUtilsException {
        // Firstly add a route
        testRouteAdd();

        Intent addedIntent =
            intentSynchronizer.getRouteIntents().iterator().next();

        // Construct the existing route entry
        RouteEntry routeEntry = new RouteEntry(
                Ip4Prefix.valueOf("1.1.1.0/24"),
                Ip4Address.valueOf("192.168.10.1"));

        // Set up expectation
        reset(intentService);
        // Setup the expected intents
        IntentOperations.Builder builder = IntentOperations.builder(APPID);
        builder.addWithdrawOperation(addedIntent.id());
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
     * This method tests when the next hop of a route is the local BGP speaker.
     *
     * @throws TestUtilsException
     */
    @Test
    public void testLocalRouteAdd() throws TestUtilsException {
        // Construct a route entry, the next hop is the local BGP speaker
        RouteEntry routeEntry = new RouteEntry(
                Ip4Prefix.valueOf("1.1.1.0/24"),
                Ip4Address.valueOf("0.0.0.0"));

        // Reset intentService to check whether the submit method is called
        reset(intentService);
        replay(intentService);

        // Call the processRouteUpdates() method in Router class
        intentSynchronizer.leaderChanged(true);
        TestUtils.setField(intentSynchronizer, "isActivatedLeader", true);
        RouteUpdate routeUpdate = new RouteUpdate(RouteUpdate.Type.UPDATE,
                                                  routeEntry);
        router.processRouteUpdates(Collections.<RouteUpdate>singletonList(routeUpdate));

        // Verify
        assertEquals(router.getRoutes4().size(), 1);
        assertTrue(router.getRoutes4().contains(routeEntry));
        assertEquals(intentSynchronizer.getRouteIntents().size(), 0);
        verify(intentService);
    }
}
