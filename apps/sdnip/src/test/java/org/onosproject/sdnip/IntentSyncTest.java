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
package org.onosproject.sdnip;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.junit.TestUtils.TestUtilsException;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.routing.FibEntry;
import org.onosproject.routing.FibUpdate;
import org.onosproject.routing.RouteEntry;
import org.onosproject.routing.config.BgpPeer;
import org.onosproject.routing.config.Interface;
import org.onosproject.routing.config.RoutingConfigurationService;
import org.onosproject.sdnip.IntentSynchronizer.IntentKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.onosproject.sdnip.TestIntentServiceHelper.eqExceptId;

/**
 * This class tests the intent synchronization function in the
 * IntentSynchronizer class.
 */
public class IntentSyncTest extends AbstractIntentTest {

    private RoutingConfigurationService routingConfig;
    private IntentService intentService;

    private static final ConnectPoint SW1_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000001"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW2_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000002"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW3_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000003"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW4_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000004"),
            PortNumber.portNumber(1));

    private IntentSynchronizer intentSynchronizer;

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

        routingConfig = createMock(RoutingConfigurationService.class);

        // These will set expectations on routingConfig
        setUpInterfaceService();
        setUpBgpPeers();

        replay(routingConfig);

        intentService = createMock(IntentService.class);

        intentSynchronizer = new IntentSynchronizer(APPID, intentService,
                                                    null, routingConfig);
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

        String peer1Sw4Eth1 = "192.168.40.1";
        peers.put(IpAddress.valueOf(peer1Sw4Eth1),
                  new BgpPeer("00:00:00:00:00:00:00:04", 1, peer1Sw4Eth1));

        expect(routingConfig.getBgpPeers()).andReturn(peers).anyTimes();
    }

    /**
     * Sets up InterfaceService.
     */
    private void setUpInterfaceService() {

        Set<Interface> interfaces = Sets.newHashSet();

        Set<InterfaceIpAddress> interfaceIpAddresses1 = Sets.newHashSet();
        interfaceIpAddresses1.add(new InterfaceIpAddress(
                IpAddress.valueOf("192.168.10.101"),
                IpPrefix.valueOf("192.168.10.0/24")));
        Interface sw1Eth1 = new Interface(SW1_ETH1,
                interfaceIpAddresses1, MacAddress.valueOf("00:00:00:00:00:01"),
                VlanId.NONE);
        interfaces.add(sw1Eth1);

        Set<InterfaceIpAddress> interfaceIpAddresses2 = Sets.newHashSet();
        interfaceIpAddresses2.add(
                new InterfaceIpAddress(IpAddress.valueOf("192.168.20.101"),
                                       IpPrefix.valueOf("192.168.20.0/24")));
        Interface sw2Eth1 = new Interface(SW2_ETH1,
                interfaceIpAddresses2, MacAddress.valueOf("00:00:00:00:00:02"),
                VlanId.NONE);
        interfaces.add(sw2Eth1);

        Set<InterfaceIpAddress> interfaceIpAddresses3 = Sets.newHashSet();
        interfaceIpAddresses3.add(
                new InterfaceIpAddress(IpAddress.valueOf("192.168.30.101"),
                                       IpPrefix.valueOf("192.168.30.0/24")));
        Interface sw3Eth1 = new Interface(SW3_ETH1,
                interfaceIpAddresses3, MacAddress.valueOf("00:00:00:00:00:03"),
                VlanId.NONE);
        interfaces.add(sw3Eth1);

        InterfaceIpAddress interfaceIpAddress4 =
                new InterfaceIpAddress(IpAddress.valueOf("192.168.40.101"),
                                       IpPrefix.valueOf("192.168.40.0/24"));
        Interface sw4Eth1 = new Interface(SW4_ETH1,
                                          Sets.newHashSet(interfaceIpAddress4),
                                          MacAddress.valueOf("00:00:00:00:00:04"),
                                          VlanId.vlanId((short) 1));

        expect(routingConfig.getInterface(SW4_ETH1)).andReturn(
                sw4Eth1).anyTimes();
        interfaces.add(sw4Eth1);

        expect(routingConfig.getInterface(SW1_ETH1)).andReturn(
                sw1Eth1).anyTimes();
        expect(routingConfig.getInterface(SW2_ETH1)).andReturn(
                sw2Eth1).anyTimes();
        expect(routingConfig.getInterface(SW3_ETH1)).andReturn(sw3Eth1).anyTimes();
        expect(routingConfig.getInterfaces()).andReturn(interfaces).anyTimes();
    }

    /**
     * Tests adding a FIB entry to the IntentSynchronizer.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is submitted to the IntentService.
     *
     * @throws TestUtilsException
     */
    @Test
    public void testFibAdd() throws TestUtilsException {
        FibEntry fibEntry = new FibEntry(
                Ip4Prefix.valueOf("1.1.1.0/24"),
                Ip4Address.valueOf("192.168.10.1"),
                MacAddress.valueOf("00:00:00:00:00:01"));

        // Construct a MultiPointToSinglePointIntent intent
        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(
                fibEntry.prefix());

        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(MacAddress.valueOf("00:00:00:00:00:01"));

        Set<ConnectPoint> ingressPoints = new HashSet<>();
        ingressPoints.add(SW2_ETH1);
        ingressPoints.add(SW3_ETH1);
        ingressPoints.add(SW4_ETH1);

        MultiPointToSinglePointIntent intent =
                MultiPointToSinglePointIntent.builder()
                        .appId(APPID)
                        .selector(selectorBuilder.build())
                        .treatment(treatmentBuilder.build())
                        .ingressPoints(ingressPoints)
                        .egressPoint(SW1_ETH1)
                        .build();

        // Setup the expected intents
        intentService.submit(eqExceptId(intent));
        replay(intentService);

        intentSynchronizer.leaderChanged(true);
        TestUtils.setField(intentSynchronizer, "isActivatedLeader", true);

        FibUpdate fibUpdate = new FibUpdate(FibUpdate.Type.UPDATE,
                                            fibEntry);
        intentSynchronizer.update(Collections.singleton(fibUpdate),
                                  Collections.emptyList());

        assertEquals(intentSynchronizer.getRouteIntents().size(), 1);
        Intent firstIntent =
                intentSynchronizer.getRouteIntents().iterator().next();
        IntentKey firstIntentKey = new IntentKey(firstIntent);
        IntentKey intentKey = new IntentKey(intent);
        assertTrue(firstIntentKey.equals(intentKey));
        verify(intentService);
    }

    /**
     * Tests adding a FIB entry with to a next hop in a VLAN.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is submitted to the IntentService.
     *
     * @throws TestUtilsException
     */
    @Test
    public void testFibAddWithVlan() throws TestUtilsException {
        FibEntry fibEntry = new FibEntry(
                Ip4Prefix.valueOf("3.3.3.0/24"),
                Ip4Address.valueOf("192.168.40.1"),
                MacAddress.valueOf("00:00:00:00:00:04"));

        // Construct a MultiPointToSinglePointIntent intent
        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4)
                       .matchIPDst(fibEntry.prefix())
                       .matchVlanId(VlanId.ANY);

        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(MacAddress.valueOf("00:00:00:00:00:04"))
                        .setVlanId(VlanId.vlanId((short) 1));

        Set<ConnectPoint> ingressPoints = new HashSet<>();
        ingressPoints.add(SW1_ETH1);
        ingressPoints.add(SW2_ETH1);
        ingressPoints.add(SW3_ETH1);

        MultiPointToSinglePointIntent intent =
                MultiPointToSinglePointIntent.builder()
                        .appId(APPID)
                        .selector(selectorBuilder.build())
                        .treatment(treatmentBuilder.build())
                        .ingressPoints(ingressPoints)
                        .egressPoint(SW4_ETH1)
                        .build();

        // Setup the expected intents
        intentService.submit(eqExceptId(intent));

        replay(intentService);

        // Run the test
        intentSynchronizer.leaderChanged(true);
        TestUtils.setField(intentSynchronizer, "isActivatedLeader", true);
        FibUpdate fibUpdate = new FibUpdate(FibUpdate.Type.UPDATE, fibEntry);

        intentSynchronizer.update(Collections.singleton(fibUpdate),
                                  Collections.emptyList());

        // Verify
        assertEquals(intentSynchronizer.getRouteIntents().size(), 1);
        Intent firstIntent =
            intentSynchronizer.getRouteIntents().iterator().next();
        IntentKey firstIntentKey = new IntentKey(firstIntent);
        IntentKey intentKey = new IntentKey(intent);
        assertTrue(firstIntentKey.equals(intentKey));
        verify(intentService);
    }

    /**
     * Tests updating a FIB entry.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is submitted to the IntentService.
     *
     * @throws TestUtilsException
     */
    @Test
    public void testFibUpdate() throws TestUtilsException {
        // Firstly add a route
        testFibAdd();

        Intent addedIntent =
                intentSynchronizer.getRouteIntents().iterator().next();

        // Start to construct a new route entry and new intent
        FibEntry fibEntryUpdate = new FibEntry(
                Ip4Prefix.valueOf("1.1.1.0/24"),
                Ip4Address.valueOf("192.168.20.1"),
                MacAddress.valueOf("00:00:00:00:00:02"));

        // Construct a new MultiPointToSinglePointIntent intent
        TrafficSelector.Builder selectorBuilderNew =
                DefaultTrafficSelector.builder();
        selectorBuilderNew.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(
                fibEntryUpdate.prefix());

        TrafficTreatment.Builder treatmentBuilderNew =
                DefaultTrafficTreatment.builder();
        treatmentBuilderNew.setEthDst(MacAddress.valueOf("00:00:00:00:00:02"));


        Set<ConnectPoint> ingressPointsNew = new HashSet<>();
        ingressPointsNew.add(SW1_ETH1);
        ingressPointsNew.add(SW3_ETH1);
        ingressPointsNew.add(SW4_ETH1);

        MultiPointToSinglePointIntent intentNew =
                MultiPointToSinglePointIntent.builder()
                        .appId(APPID)
                        .selector(selectorBuilderNew.build())
                        .treatment(treatmentBuilderNew.build())
                        .ingressPoints(ingressPointsNew)
                        .egressPoint(SW2_ETH1)
                        .build();

        // Set up test expectation
        reset(intentService);
        // Setup the expected intents
        intentService.withdraw(eqExceptId(addedIntent));
        intentService.submit(eqExceptId(intentNew));
        replay(intentService);

        // Call the update() method in IntentSynchronizer class
        intentSynchronizer.leaderChanged(true);
        TestUtils.setField(intentSynchronizer, "isActivatedLeader", true);
        FibUpdate fibUpdate = new FibUpdate(FibUpdate.Type.UPDATE,
                                                  fibEntryUpdate);
        intentSynchronizer.update(Collections.singletonList(fibUpdate),
                                  Collections.emptyList());

        // Verify
        assertEquals(intentSynchronizer.getRouteIntents().size(), 1);
        Intent firstIntent =
                intentSynchronizer.getRouteIntents().iterator().next();
        IntentKey firstIntentKey = new IntentKey(firstIntent);
        IntentKey intentNewKey = new IntentKey(intentNew);
        assertTrue(firstIntentKey.equals(intentNewKey));
        verify(intentService);
    }

    /**
     * Tests deleting a FIB entry.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is withdrawn from the IntentService.
     *
     * @throws TestUtilsException
     */
    @Test
    public void testFibDelete() throws TestUtilsException {
        // Firstly add a route
        testFibAdd();

        Intent addedIntent =
                intentSynchronizer.getRouteIntents().iterator().next();

        // Construct the existing route entry
        FibEntry fibEntry = new FibEntry(
                Ip4Prefix.valueOf("1.1.1.0/24"), null, null);

        // Set up expectation
        reset(intentService);
        // Setup the expected intents
        intentService.withdraw(eqExceptId(addedIntent));
        replay(intentService);

        // Call the update() method in IntentSynchronizer class
        intentSynchronizer.leaderChanged(true);
        TestUtils.setField(intentSynchronizer, "isActivatedLeader", true);
        FibUpdate fibUpdate = new FibUpdate(FibUpdate.Type.DELETE, fibEntry);
        intentSynchronizer.update(Collections.emptyList(),
                                  Collections.singletonList(fibUpdate));

        // Verify
        assertEquals(intentSynchronizer.getRouteIntents().size(), 0);
        verify(intentService);
    }

    /**
     * This method tests the behavior of intent Synchronizer.
     *
     * @throws TestUtilsException
     */
    @Test
    public void testIntentSync() throws TestUtilsException {

        //
        // Construct routes and intents.
        // This test simulates the following cases during the master change
        // time interval:
        // 1. RouteEntry1 did not change and the intent also did not change.
        // 2. RouteEntry2 was deleted, but the intent was not deleted.
        // 3. RouteEntry3 was newly added, and the intent was also submitted.
        // 4. RouteEntry4 was updated to RouteEntry4Update, and the intent was
        // also updated to a new one.
        // 5. RouteEntry5 did not change, but its intent id changed.
        // 6. RouteEntry6 was newly added, but the intent was not submitted.
        //
        RouteEntry routeEntry1 = new RouteEntry(
                Ip4Prefix.valueOf("1.1.1.0/24"),
                Ip4Address.valueOf("192.168.10.1"));

        RouteEntry routeEntry2 = new RouteEntry(
                Ip4Prefix.valueOf("2.2.2.0/24"),
                Ip4Address.valueOf("192.168.20.1"));

        RouteEntry routeEntry3 = new RouteEntry(
                Ip4Prefix.valueOf("3.3.3.0/24"),
                Ip4Address.valueOf("192.168.30.1"));

        RouteEntry routeEntry4 = new RouteEntry(
                Ip4Prefix.valueOf("4.4.4.0/24"),
                Ip4Address.valueOf("192.168.30.1"));

        RouteEntry routeEntry4Update = new RouteEntry(
                Ip4Prefix.valueOf("4.4.4.0/24"),
                Ip4Address.valueOf("192.168.20.1"));

        RouteEntry routeEntry5 = new RouteEntry(
                Ip4Prefix.valueOf("5.5.5.0/24"),
                Ip4Address.valueOf("192.168.10.1"));

        RouteEntry routeEntry6 = new RouteEntry(
                Ip4Prefix.valueOf("6.6.6.0/24"),
                Ip4Address.valueOf("192.168.10.1"));

        RouteEntry routeEntry7 = new RouteEntry(
                Ip4Prefix.valueOf("7.7.7.0/24"),
                Ip4Address.valueOf("192.168.10.1"));

        MultiPointToSinglePointIntent intent1 = intentBuilder(
                routeEntry1.prefix(), "00:00:00:00:00:01", SW1_ETH1);
        MultiPointToSinglePointIntent intent2 = intentBuilder(
                routeEntry2.prefix(), "00:00:00:00:00:02", SW2_ETH1);
        MultiPointToSinglePointIntent intent3 = intentBuilder(
                routeEntry3.prefix(), "00:00:00:00:00:03", SW3_ETH1);
        MultiPointToSinglePointIntent intent4 = intentBuilder(
                routeEntry4.prefix(), "00:00:00:00:00:03", SW3_ETH1);
        MultiPointToSinglePointIntent intent4Update = intentBuilder(
                routeEntry4Update.prefix(), "00:00:00:00:00:02", SW2_ETH1);
        MultiPointToSinglePointIntent intent5 = intentBuilder(
                routeEntry5.prefix(), "00:00:00:00:00:01",  SW1_ETH1);
        MultiPointToSinglePointIntent intent7 = intentBuilder(
                routeEntry7.prefix(), "00:00:00:00:00:01",  SW1_ETH1);

        // Compose a intent, which is equal to intent5 but the id is different.
        MultiPointToSinglePointIntent intent5New =
                staticIntentBuilder(intent5, routeEntry5, "00:00:00:00:00:01");
        assertThat(IntentSynchronizer.IntentKey.equalIntents(
                        intent5, intent5New),
                   is(true));
        assertFalse(intent5.equals(intent5New));

        MultiPointToSinglePointIntent intent6 = intentBuilder(
                routeEntry6.prefix(), "00:00:00:00:00:01",  SW1_ETH1);

        // Set up the routeIntents field in IntentSynchronizer class
        ConcurrentHashMap<IpPrefix, MultiPointToSinglePointIntent>
            routeIntents =  new ConcurrentHashMap<>();
        routeIntents.put(routeEntry1.prefix(), intent1);
        routeIntents.put(routeEntry3.prefix(), intent3);
        routeIntents.put(routeEntry4Update.prefix(), intent4Update);
        routeIntents.put(routeEntry5.prefix(), intent5New);
        routeIntents.put(routeEntry6.prefix(), intent6);
        routeIntents.put(routeEntry7.prefix(), intent7);
        TestUtils.setField(intentSynchronizer, "routeIntents", routeIntents);

        // Set up expectation
        reset(intentService);
        Set<Intent> intents = new HashSet<>();
        intents.add(intent1);
        expect(intentService.getIntentState(intent1.key()))
                .andReturn(IntentState.INSTALLED).anyTimes();
        intents.add(intent2);
        expect(intentService.getIntentState(intent2.key()))
                .andReturn(IntentState.INSTALLED).anyTimes();
        intents.add(intent4);
        expect(intentService.getIntentState(intent4.key()))
                .andReturn(IntentState.INSTALLED).anyTimes();
        intents.add(intent5);
        expect(intentService.getIntentState(intent5.key()))
                .andReturn(IntentState.INSTALLED).anyTimes();
        intents.add(intent7);
        expect(intentService.getIntentState(intent7.key()))
                .andReturn(IntentState.WITHDRAWING).anyTimes();
        expect(intentService.getIntents()).andReturn(intents).anyTimes();

        intentService.withdraw(intent2);
        intentService.withdraw(intent4);

        intentService.submit(intent3);
        intentService.submit(intent4Update);
        intentService.submit(intent6);
        intentService.submit(intent7);
        replay(intentService);

        // Start the test
        intentSynchronizer.leaderChanged(true);
        intentSynchronizer.synchronizeIntents();

        // Verify
        assertEquals(intentSynchronizer.getRouteIntents().size(), 6);
        assertTrue(intentSynchronizer.getRouteIntents().contains(intent1));
        assertTrue(intentSynchronizer.getRouteIntents().contains(intent3));
        assertTrue(intentSynchronizer.getRouteIntents().contains(intent4Update));
        assertTrue(intentSynchronizer.getRouteIntents().contains(intent5));
        assertTrue(intentSynchronizer.getRouteIntents().contains(intent6));

        verify(intentService);
    }

    /**
     * MultiPointToSinglePointIntent builder.
     *
     * @param ipPrefix the ipPrefix to match
     * @param nextHopMacAddress to which the destination MAC address in packet
     * should be rewritten
     * @param egressPoint to which packets should be sent
     * @return the constructed MultiPointToSinglePointIntent
     */
    private MultiPointToSinglePointIntent intentBuilder(IpPrefix ipPrefix,
            String nextHopMacAddress, ConnectPoint egressPoint) {

        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        if (ipPrefix.isIp4()) {
            selectorBuilder.matchEthType(Ethernet.TYPE_IPV4);   // IPv4
            selectorBuilder.matchIPDst(ipPrefix);
        } else {
            selectorBuilder.matchEthType(Ethernet.TYPE_IPV6);   // IPv6
            selectorBuilder.matchIPv6Dst(ipPrefix);
        }

        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(MacAddress.valueOf(nextHopMacAddress));

        Set<ConnectPoint> ingressPoints = new HashSet<>();
        for (Interface intf : routingConfig.getInterfaces()) {
            if (!intf.equals(routingConfig.getInterface(egressPoint))) {
                ConnectPoint srcPort = intf.connectPoint();
                ingressPoints.add(srcPort);
            }
        }
        MultiPointToSinglePointIntent intent =
                MultiPointToSinglePointIntent.builder()
                        .appId(APPID)
                        .selector(selectorBuilder.build())
                        .treatment(treatmentBuilder.build())
                        .ingressPoints(ingressPoints)
                        .egressPoint(egressPoint)
                        .build();
        return intent;
    }

    /**
     * A static MultiPointToSinglePointIntent builder, the returned intent is
     * equal to the input intent except that the id is different.
     *
     *
     * @param intent the intent to be used for building a new intent
     * @param routeEntry the relative routeEntry of the intent
     * @return the newly constructed MultiPointToSinglePointIntent
     * @throws TestUtilsException
     */
    private  MultiPointToSinglePointIntent staticIntentBuilder(
            MultiPointToSinglePointIntent intent, RouteEntry routeEntry,
            String nextHopMacAddress) throws TestUtilsException {

        // Use a different egress ConnectPoint with that in intent
        // to generate a different id
        MultiPointToSinglePointIntent intentNew = intentBuilder(
                routeEntry.prefix(), nextHopMacAddress, SW2_ETH1);
        TestUtils.setField(intentNew, "egressPoint", intent.egressPoint());
        TestUtils.setField(intentNew,
                "ingressPoints", intent.ingressPoints());
        return intentNew;
    }
}
