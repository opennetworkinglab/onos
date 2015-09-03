/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.routing.FibEntry;
import org.onosproject.routing.FibUpdate;
import org.onosproject.routing.IntentSynchronizationService;
import org.onosproject.routing.config.BgpPeer;
import org.onosproject.routing.config.RoutingConfigurationService;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.onosproject.sdnip.TestIntentServiceHelper.eqExceptId;

/**
 * Unit tests for SdnIpFib.
 */
public class SdnIpFibTest extends AbstractIntentTest {

    private RoutingConfigurationService routingConfig;
    private InterfaceService interfaceService;

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

    private SdnIpFib sdnipFib;
    private IntentSynchronizationService intentSynchronizer;
    private final Set<Interface> interfaces = Sets.newHashSet();

    private static final ApplicationId APPID = TestApplicationId.create("SDNIP");

    @Before
    public void setUp() throws Exception {
        super.setUp();

        routingConfig = createMock(RoutingConfigurationService.class);
        interfaceService = createMock(InterfaceService.class);

        // These will set expectations on routingConfig and interfaceService
        setUpInterfaceService();
        setUpBgpPeers();

        replay(routingConfig);
        replay(interfaceService);

        intentSynchronizer = createMock(IntentSynchronizationService.class);

        sdnipFib = new SdnIpFib(APPID, interfaceService, intentSynchronizer);
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

        expect(interfaceService.getInterfacesByPort(SW4_ETH1)).andReturn(
                Collections.singleton(sw4Eth1)).anyTimes();
        expect(interfaceService.getMatchingInterface(Ip4Address.valueOf("192.168.40.1")))
                .andReturn(sw4Eth1).anyTimes();

        interfaces.add(sw4Eth1);

        expect(interfaceService.getInterfacesByPort(SW1_ETH1)).andReturn(
                Collections.singleton(sw1Eth1)).anyTimes();
        expect(interfaceService.getMatchingInterface(Ip4Address.valueOf("192.168.10.1")))
                .andReturn(sw1Eth1).anyTimes();
        expect(interfaceService.getInterfacesByPort(SW2_ETH1)).andReturn(
                Collections.singleton(sw2Eth1)).anyTimes();
        expect(interfaceService.getMatchingInterface(Ip4Address.valueOf("192.168.20.1")))
                .andReturn(sw2Eth1).anyTimes();
        expect(interfaceService.getInterfacesByPort(SW3_ETH1)).andReturn(
                Collections.singleton(sw3Eth1)).anyTimes();
        expect(interfaceService.getMatchingInterface(Ip4Address.valueOf("192.168.30.1")))
                .andReturn(sw3Eth1).anyTimes();
        expect(interfaceService.getInterfaces()).andReturn(interfaces).anyTimes();
    }

    /**
     * Tests adding a FIB entry to the IntentSynchronizer.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is submitted to the IntentService.
     */
    @Test
    public void testFibAdd() {
        IpPrefix prefix = Ip4Prefix.valueOf("1.1.1.0/24");
        FibEntry fibEntry = new FibEntry(prefix,
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
                        .key(Key.of(prefix.toString(), APPID))
                        .selector(selectorBuilder.build())
                        .treatment(treatmentBuilder.build())
                        .ingressPoints(ingressPoints)
                        .egressPoint(SW1_ETH1)
                        .constraints(SdnIpFib.CONSTRAINTS)
                        .build();

        // Setup the expected intents
        intentSynchronizer.submit(eqExceptId(intent));
        replay(intentSynchronizer);

        // Send in the UPDATE FibUpdate
        FibUpdate fibUpdate = new FibUpdate(FibUpdate.Type.UPDATE, fibEntry);
        sdnipFib.update(Collections.singleton(fibUpdate), Collections.emptyList());

        verify(intentSynchronizer);
    }

    /**
     * Tests adding a FIB entry with to a next hop in a VLAN.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is submitted to the IntentService.
     */
    @Test
    public void testFibAddWithVlan() {
        IpPrefix prefix = Ip4Prefix.valueOf("3.3.3.0/24");
        FibEntry fibEntry = new FibEntry(prefix,
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
                        .key(Key.of(prefix.toString(), APPID))
                        .selector(selectorBuilder.build())
                        .treatment(treatmentBuilder.build())
                        .ingressPoints(ingressPoints)
                        .egressPoint(SW4_ETH1)
                        .constraints(SdnIpFib.CONSTRAINTS)
                        .build();

        // Setup the expected intents
        intentSynchronizer.submit(eqExceptId(intent));

        replay(intentSynchronizer);

        // Send in the UPDATE FibUpdate
        FibUpdate fibUpdate = new FibUpdate(FibUpdate.Type.UPDATE, fibEntry);
        sdnipFib.update(Collections.singleton(fibUpdate), Collections.emptyList());

        verify(intentSynchronizer);
    }

    /**
     * Tests updating a FIB entry.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is submitted to the IntentService.
     */
    @Test
    public void testFibUpdate() {
        // Firstly add a route
        testFibAdd();

        IpPrefix prefix = Ip4Prefix.valueOf("1.1.1.0/24");

        // Start to construct a new route entry and new intent
        FibEntry fibEntryUpdate = new FibEntry(prefix,
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
                        .key(Key.of(prefix.toString(), APPID))
                        .selector(selectorBuilderNew.build())
                        .treatment(treatmentBuilderNew.build())
                        .ingressPoints(ingressPointsNew)
                        .egressPoint(SW2_ETH1)
                        .constraints(SdnIpFib.CONSTRAINTS)
                        .build();

        // Set up test expectation
        reset(intentSynchronizer);

        // Setup the expected intents
        intentSynchronizer.submit(eqExceptId(intentNew));
        replay(intentSynchronizer);

        // Send in the UPDATE FibUpdate
        FibUpdate fibUpdate = new FibUpdate(FibUpdate.Type.UPDATE,
                fibEntryUpdate);
        sdnipFib.update(Collections.singletonList(fibUpdate),
                Collections.emptyList());

        verify(intentSynchronizer);
    }

    /**
     * Tests deleting a FIB entry.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is withdrawn from the IntentService.
     */
    @Test
    public void testFibDelete() {
        // Firstly add a route
        testFibAdd();

        IpPrefix prefix = Ip4Prefix.valueOf("1.1.1.0/24");

        // Construct the existing route entry
        FibEntry fibEntry = new FibEntry(prefix, null, null);

        // Construct the existing MultiPointToSinglePoint intent
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

        MultiPointToSinglePointIntent addedIntent =
                MultiPointToSinglePointIntent.builder()
                        .appId(APPID)
                        .key(Key.of(prefix.toString(), APPID))
                        .selector(selectorBuilder.build())
                        .treatment(treatmentBuilder.build())
                        .ingressPoints(ingressPoints)
                        .egressPoint(SW1_ETH1)
                        .constraints(SdnIpFib.CONSTRAINTS)
                        .build();

        // Set up expectation
        reset(intentSynchronizer);
        // Setup the expected intents
        intentSynchronizer.withdraw(eqExceptId(addedIntent));
        replay(intentSynchronizer);

        // Send in the DELETE FibUpdate
        FibUpdate fibUpdate = new FibUpdate(FibUpdate.Type.DELETE, fibEntry);
        sdnipFib.update(Collections.emptyList(), Collections.singletonList(fibUpdate));

        verify(intentSynchronizer);
    }
}
