/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
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
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceEvent;
import org.onosproject.incubator.net.intf.InterfaceListener;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.incubator.net.intf.InterfaceServiceAdapter;
import org.onosproject.incubator.net.routing.ResolvedRoute;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteEvent;
import org.onosproject.incubator.net.routing.RouteListener;
import org.onosproject.incubator.net.routing.RouteServiceAdapter;
import org.onosproject.intentsync.IntentSynchronizationService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.NetworkConfigServiceAdapter;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.sdnip.config.SdnIpConfig;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.onosproject.routing.TestIntentServiceHelper.eqExceptId;

/**
 * Unit tests for SdnIpFib.
 */
public class SdnIpFibTest extends AbstractIntentTest {

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

    private static final MacAddress MAC1 = MacAddress.valueOf("00:00:00:00:00:01");
    private static final MacAddress MAC2 = MacAddress.valueOf("00:00:00:00:00:02");
    private static final MacAddress MAC3 = MacAddress.valueOf("00:00:00:00:00:03");
    private static final MacAddress MAC4 = MacAddress.valueOf("00:00:00:00:00:04");

    private static final VlanId NO_VLAN = VlanId.NONE;
    private static final VlanId VLAN10 = VlanId.vlanId(Short.valueOf("10"));
    private static final VlanId VLAN20 = VlanId.vlanId(Short.valueOf("20"));

    private static final InterfaceIpAddress IIP1 =
            InterfaceIpAddress.valueOf("192.168.10.101/24");
    private static final InterfaceIpAddress IIP2 =
            InterfaceIpAddress.valueOf("192.168.20.101/24");
    private static final InterfaceIpAddress IIP3 =
            InterfaceIpAddress.valueOf("192.168.30.101/24");
    private static final InterfaceIpAddress IIP4 =
            InterfaceIpAddress.valueOf("192.168.40.101/24");

    private static final IpAddress IP1 = Ip4Address.valueOf("192.168.10.1");
    private static final IpAddress IP2 = Ip4Address.valueOf("192.168.20.1");
    private static final IpAddress IP3 = Ip4Address.valueOf("192.168.30.1");

    private static final IpPrefix PREFIX1 = Ip4Prefix.valueOf("1.1.1.0/24");
    private static final IpPrefix PREFIX2 = Ip4Prefix.valueOf("1.1.2.0/24");

    private SdnIpFib sdnipFib;
    private IntentSynchronizationService intentSynchronizer;
    private final Set<Interface> interfaces = Sets.newHashSet();

    private static final ApplicationId APPID = TestApplicationId.create("SDNIP");

    private RouteListener routeListener;
    private InterfaceListener interfaceListener;

    @Before
    public void setUp() {
        super.setUp();

        interfaceService = createMock(InterfaceService.class);

        interfaceService.addListener(anyObject(InterfaceListener.class));
        expectLastCall().andDelegateTo(new InterfaceServiceDelegate());

        // These will set expectations on routingConfig and interfaceService
        setUpInterfaceService();

        replay(interfaceService);

        intentSynchronizer = createMock(IntentSynchronizationService.class);

        sdnipFib = new SdnIpFib();
        sdnipFib.routeService = new TestRouteService();
        sdnipFib.coreService = new TestCoreService();
        sdnipFib.networkConfigService = new TestNetworkConfigService();
        sdnipFib.interfaceService = interfaceService;
        sdnipFib.intentSynchronizer = intentSynchronizer;

        sdnipFib.activate();
    }

    /**
     * Sets up the interface service.
     */
    private void setUpInterfaceService() {
        List<InterfaceIpAddress> iIps1 = Lists.newArrayList();
        iIps1.add(IIP1);
        Interface sw1Eth1 = new Interface("sw1-eth1", SW1_ETH1, iIps1, MAC1, VLAN10);
        interfaces.add(sw1Eth1);

        List<InterfaceIpAddress> iIps2 = Lists.newArrayList();
        iIps2.add(IIP2);
        Interface sw2Eth1 = new Interface("sw2-eth1", SW2_ETH1, iIps2, MAC2, VLAN20);
        interfaces.add(sw2Eth1);

        List<InterfaceIpAddress> iIps3 = Lists.newArrayList();
        iIps3.add(IIP3);
        Interface sw3Eth1 = new Interface("sw3-eth1", SW3_ETH1, iIps3, MAC3, NO_VLAN);
        interfaces.add(sw3Eth1);

        expect(interfaceService.getInterfacesByPort(SW1_ETH1)).andReturn(
                Collections.singleton(sw1Eth1)).anyTimes();
        expect(interfaceService.getMatchingInterface(IP1))
                .andReturn(sw1Eth1).anyTimes();
        expect(interfaceService.getInterfacesByPort(SW2_ETH1)).andReturn(
                Collections.singleton(sw2Eth1)).anyTimes();
        expect(interfaceService.getMatchingInterface(IP2))
                .andReturn(sw2Eth1).anyTimes();
        expect(interfaceService.getInterfacesByPort(SW3_ETH1)).andReturn(
                Collections.singleton(sw3Eth1)).anyTimes();
        expect(interfaceService.getMatchingInterface(IP3))
                .andReturn(sw3Eth1).anyTimes();
        expect(interfaceService.getInterfaces()).andReturn(interfaces).anyTimes();
    }

    /**
     * Tests adding a route. The egress interface has no VLAN configured.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is submitted to the IntentService.
     */
    @Test
    public void testRouteAddToNoVlan() {
        // Build the expected route
        ResolvedRoute route = createRoute(PREFIX1, IP3, MAC3, SW3_ETH1);

        MultiPointToSinglePointIntent intent =
                createIntentToThreeSrcOneTwo(PREFIX1);

        // Setup the expected intents
        intentSynchronizer.submit(eqExceptId(intent));
        replay(intentSynchronizer);

        // Send in the added event
        routeListener.event(new RouteEvent(RouteEvent.Type.ROUTE_ADDED, route));

        verify(intentSynchronizer);
    }

    /**
     * Tests adding a route. The egress interface has a VLAN configured.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is submitted to the IntentService.
     */
    @Test
    public void testRouteAddToVlan() {
        // Build the expected route
        ResolvedRoute route = createRoute(PREFIX2, IP1, MAC1, SW1_ETH1);

        MultiPointToSinglePointIntent intent = createIntentToOne(PREFIX2);

        // Setup the expected intents
        intentSynchronizer.submit(eqExceptId(intent));
        replay(intentSynchronizer);

        // Send in the added event
        routeListener.event(new RouteEvent(RouteEvent.Type.ROUTE_ADDED, route));

        verify(intentSynchronizer);
    }

    /**
     * Tests updating a route.
     *
     * We first add a route from a next-hop with no vlan. We then announce the
     * same route from another next-hop with a vlan.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is submitted to the IntentService.
     */
    @Test
    public void testRouteUpdatesToVlan() {
        // Add a route first to a destination with no VLAN
        testRouteAddToNoVlan();

        // Build the new route entries for prefix1 and prefix2
        ResolvedRoute oldRoutePrefixOne = createRoute(PREFIX1, IP3, MAC3, SW3_ETH1);
        ResolvedRoute routePrefixOne = createRoute(PREFIX1, IP1, MAC1, SW1_ETH1);

        // Create the new expected intents
        MultiPointToSinglePointIntent newPrefixOneIntent = createIntentToOne(PREFIX1);

        // Set up test expectation
        reset(intentSynchronizer);

        // Setup the expected intents
        intentSynchronizer.submit(eqExceptId(newPrefixOneIntent));
        replay(intentSynchronizer);

        // Send in the update events
        routeListener.event(new RouteEvent(RouteEvent.Type.ROUTE_UPDATED,
                                           routePrefixOne, oldRoutePrefixOne));

        verify(intentSynchronizer);
    }

    /**
     * Tests updating a route.
     *
     * We first add a route from a next-hop with a vlan. We then announce the
     * same route from another next-hop with no vlan.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is submitted to the IntentService.
     */
    @Test
    public void testRouteUpdatesToNoVlan() {
        // Add a route first to a destination with no VLAN
        testRouteAddToVlan();

        // Build the new route entries for prefix1 and prefix2
        ResolvedRoute oldRoutePrefix = createRoute(PREFIX2, IP1, MAC1, SW1_ETH1);
        ResolvedRoute routePrefix = createRoute(PREFIX2, IP3, MAC3, SW3_ETH1);

        // Create the new expected intents
        MultiPointToSinglePointIntent newPrefixIntent =
                createIntentToThreeSrcOneTwo(PREFIX2);

        // Set up test expectation
        reset(intentSynchronizer);

        // Setup the expected intents
        intentSynchronizer.submit(eqExceptId(newPrefixIntent));
        replay(intentSynchronizer);

        // Send in the update events
        routeListener.event(new RouteEvent(RouteEvent.Type.ROUTE_UPDATED,
                                           routePrefix, oldRoutePrefix));

        verify(intentSynchronizer);
    }

    /**
     * Tests deleting a route.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is withdrawn from the IntentService.
     */
    @Test
    public void testRouteDelete() {
        // Add a route first
        testRouteAddToNoVlan();

        // Construct the existing route entry
        ResolvedRoute route = createRoute(PREFIX1, IP3, MAC3, SW3_ETH1);

        // Create existing intent
        MultiPointToSinglePointIntent removedIntent =
                createIntentToThreeSrcOneTwo(PREFIX1);

        // Set up expectation
        reset(intentSynchronizer);
        // Setup the expected intents
        intentSynchronizer.withdraw(eqExceptId(removedIntent));
        replay(intentSynchronizer);

        // Send in the removed event
        routeListener.event(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED, route));

        verify(intentSynchronizer);
    }

    /**
     * Tests adding a new interface.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is withdrawn from the IntentService.
     */
    @Test
    public void testAddInterface() {
        // Add a route first
        testRouteAddToNoVlan();

        // Create the new expected intent
        MultiPointToSinglePointIntent addedIntent =
                createIntentToThreeSrcOneTwoFour(PREFIX1);

        reset(intentSynchronizer);

        intentSynchronizer.submit(eqExceptId(addedIntent));
        expectLastCall().once();

        replay(intentSynchronizer);

        // Create the new interface and add notify it
        Interface intf = new Interface("sw4-eth1", SW4_ETH1,
                                       Collections.singletonList(IIP4),
                                       MAC4, NO_VLAN);
        InterfaceEvent intfEvent =
                new InterfaceEvent(InterfaceEvent.Type.INTERFACE_ADDED, intf);

        interfaceListener.event(intfEvent);

        verify(intentSynchronizer);
    }

    /**
     * Tests removing an existing interface.
     *
     * We first push an intent with destination sw3 and source sw1 and sw2. We
     * then remove the ingress interface on sw1.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is withdrawn from the IntentService.
     */
    @Test
    public void testRemoveIngressInterface() {
        // Add a route first
        testRouteAddToNoVlan();

        // Create the new expected intent
        MultiPointToSinglePointIntent remainingIntent =
                createIntentToThreeSrcTwo(PREFIX1);

        reset(intentSynchronizer);

        intentSynchronizer.submit(eqExceptId(remainingIntent));
        expectLastCall().once();

        replay(intentSynchronizer);

        // Define the existing ingress interface and remove it
        Interface intf = new Interface("sw1-eth1", SW1_ETH1,
                                       Collections.singletonList(IIP1),
                                       MAC1, VLAN10);
        InterfaceEvent intfEvent =
                new InterfaceEvent(InterfaceEvent.Type.INTERFACE_REMOVED, intf);
        interfaceListener.event(intfEvent);

        verify(intentSynchronizer);
    }

    /**
     * Tests removing an existing egress interface.
     *
     * We first push an intent with destination sw3 and source sw1 and sw2. We
     * then remove the egress interface on sw3.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is withdrawn from the IntentService.
     */
    @Test
    public void testRemoveEgressInterface() {
        // Add a route first
        testRouteAddToNoVlan();

        // Create existing intent
        MultiPointToSinglePointIntent removedIntent =
                createIntentToThreeSrcOneTwo(PREFIX1);

        // Set up expectation
        reset(intentSynchronizer);
        // Setup the expected intents
        intentSynchronizer.withdraw(eqExceptId(removedIntent));
        replay(intentSynchronizer);

        // Define the existing egress interface and remove it
        Interface intf = new Interface("sw3-eth1", SW3_ETH1,
                                       Collections.singletonList(IIP3),
                                       MAC3, VlanId.NONE);
        InterfaceEvent intfEvent =
                new InterfaceEvent(InterfaceEvent.Type.INTERFACE_REMOVED, intf);
        interfaceListener.event(intfEvent);

        verify(intentSynchronizer);
    }

    /*
     * Builds a MultiPointToSinglePointIntent with dest sw1 (VLAN Id) and src
     * sw2, sw3.
     */
    private MultiPointToSinglePointIntent createIntentToOne(IpPrefix ipPrefix) {
        // Build the expected treatment
        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(MAC1);

        // Build the expected egress FilteredConnectPoint
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchVlanId(VLAN10);
        FilteredConnectPoint egressFilteredCP =
                new FilteredConnectPoint(SW1_ETH1, selector.build());

        // Build the expected selectors
        Set<FilteredConnectPoint> ingressFilteredCPs = Sets.newHashSet();

        // Build the expected ingress FilteredConnectPoint for sw2
        selector = DefaultTrafficSelector.builder();
        selector.matchVlanId(VLAN20);
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPDst(ipPrefix);
        FilteredConnectPoint ingressFilteredCP =
                new FilteredConnectPoint(SW2_ETH1, selector.build());
        ingressFilteredCPs.add(ingressFilteredCP);

        // Build the expected ingress FilteredConnectPoint for sw3
        selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPDst(ipPrefix);
        ingressFilteredCP = new FilteredConnectPoint(SW3_ETH1, selector.build());
        ingressFilteredCPs.add(ingressFilteredCP);

        // Build the expected intent
        MultiPointToSinglePointIntent intent =
                MultiPointToSinglePointIntent.builder()
                        .appId(APPID)
                        .key(Key.of(ipPrefix.toString(), APPID))
                        .filteredIngressPoints(ingressFilteredCPs)
                        .filteredEgressPoint(egressFilteredCP)
                        .treatment(treatmentBuilder.build())
                        .constraints(SdnIpFib.CONSTRAINTS)
                        .build();

        return intent;
    }

    /*
     * Builds a MultiPointToSinglePointIntent with dest sw3 (no VLAN Id) and src
     * sw1, sw2.
     */
    private MultiPointToSinglePointIntent createIntentToThreeSrcOneTwo(IpPrefix ipPrefix) {
        // Build the expected treatment
        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(MAC3);

        // Build the expected egress FilteredConnectPoint
        FilteredConnectPoint egressFilteredCP = new FilteredConnectPoint(SW3_ETH1);

        // Build the expected selectors
        Set<FilteredConnectPoint> ingressFilteredCPs = Sets.newHashSet();

        // Build the expected ingress FilteredConnectPoint for sw1
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchVlanId(VLAN10);
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPDst(ipPrefix);
        FilteredConnectPoint ingressFilteredCP =
                new FilteredConnectPoint(SW1_ETH1, selector.build());
        ingressFilteredCPs.add(ingressFilteredCP);

        // Build the expected ingress FilteredConnectPoint for sw2
        selector = DefaultTrafficSelector.builder();
        selector.matchVlanId(VLAN20);
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPDst(ipPrefix);
        ingressFilteredCP = new FilteredConnectPoint(SW2_ETH1, selector.build());
        ingressFilteredCPs.add(ingressFilteredCP);

        // Build the expected intent
        MultiPointToSinglePointIntent intent =
                MultiPointToSinglePointIntent.builder()
                        .appId(APPID)
                        .key(Key.of(ipPrefix.toString(), APPID))
                        .filteredIngressPoints(ingressFilteredCPs)
                        .filteredEgressPoint(egressFilteredCP)
                        .treatment(treatmentBuilder.build())
                        .constraints(SdnIpFib.CONSTRAINTS)
                        .build();

        return intent;
    }

    /*
     * Builds a MultiPointToSinglePointIntent with dest sw3 (no VLAN Id) and src
     * sw2.
     */
    private MultiPointToSinglePointIntent createIntentToThreeSrcTwo(IpPrefix ipPrefix) {
        // Build the expected treatment
        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(MAC3);

        // Build the expected egress FilteredConnectPoint
        FilteredConnectPoint egressFilteredCP = new FilteredConnectPoint(SW3_ETH1);

        // Build the expected ingress FilteredConnectPoint for sw2
        Set<FilteredConnectPoint> ingressFilteredCPs = Sets.newHashSet();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchVlanId(VLAN20);
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPDst(ipPrefix);
        FilteredConnectPoint ingressFilteredCP =
                new FilteredConnectPoint(SW2_ETH1, selector.build());
        ingressFilteredCPs.add(ingressFilteredCP);

        // Build the expected intent
        MultiPointToSinglePointIntent intent =
                MultiPointToSinglePointIntent.builder()
                        .appId(APPID)
                        .key(Key.of(ipPrefix.toString(), APPID))
                        .filteredIngressPoints(ingressFilteredCPs)
                        .filteredEgressPoint(egressFilteredCP)
                        .treatment(treatmentBuilder.build())
                        .constraints(SdnIpFib.CONSTRAINTS)
                        .build();

        return intent;
    }

    /*
     * Builds a MultiPointToSinglePointIntent with dest sw3 (no VLAN Id) and src
     * sw1, sw2, sw4.
     */
    private MultiPointToSinglePointIntent createIntentToThreeSrcOneTwoFour(IpPrefix ipPrefix) {
        // Build the expected treatment
        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(MAC3);

        // Build the expected egress FilteredConnectPoint
        FilteredConnectPoint egressFilteredCP = new FilteredConnectPoint(SW3_ETH1);

        // Build the expected selectors
        Set<FilteredConnectPoint> ingressFilteredCPs = Sets.newHashSet();

        // Build the expected ingress FilteredConnectPoint for sw1
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchVlanId(VLAN10);
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPDst(ipPrefix);
        FilteredConnectPoint ingressFilteredCP =
                new FilteredConnectPoint(SW1_ETH1, selector.build());
        ingressFilteredCPs.add(ingressFilteredCP);

        // Build the expected ingress FilteredConnectPoint for sw2
        selector = DefaultTrafficSelector.builder();
        selector.matchVlanId(VLAN20);
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPDst(ipPrefix);
        ingressFilteredCP = new FilteredConnectPoint(SW2_ETH1, selector.build());
        ingressFilteredCPs.add(ingressFilteredCP);

        // Build the expected ingress FilteredConnectPoint for sw4
        selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPDst(ipPrefix);
        ingressFilteredCP = new FilteredConnectPoint(SW4_ETH1, selector.build());
        ingressFilteredCPs.add(ingressFilteredCP);

        // Build the expected intent
        MultiPointToSinglePointIntent intent =
                MultiPointToSinglePointIntent.builder()
                        .appId(APPID)
                        .key(Key.of(ipPrefix.toString(), APPID))
                        .filteredIngressPoints(ingressFilteredCPs)
                        .filteredEgressPoint(egressFilteredCP)
                        .treatment(treatmentBuilder.build())
                        .constraints(SdnIpFib.CONSTRAINTS)
                        .build();

        return intent;
    }

    private static ResolvedRoute createRoute(IpPrefix prefix, IpAddress nextHop,
                                             MacAddress nextHopMac, ConnectPoint location) {
        return new ResolvedRoute(
                new Route(Route.Source.UNDEFINED, prefix, nextHop), nextHopMac, location);
    }

    private class TestCoreService extends CoreServiceAdapter {
        @Override
        public ApplicationId getAppId(String name) {
            return APPID;
        }
    }

    private class TestRouteService extends RouteServiceAdapter {
        @Override
        public void addListener(RouteListener routeListener) {
            SdnIpFibTest.this.routeListener = routeListener;
        }
    }

    private class TestNetworkConfigService extends NetworkConfigServiceAdapter {
        /**
         * Returns an empty BGP network configuration to be able to correctly
         * return the encapsulation parameter when needed.
         *
         * @return an empty BGP network configuration object
         */
        @Override
        public <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass) {
            ApplicationId appId =
                    new TestApplicationId(SdnIp.SDN_IP_APP);

            ObjectMapper mapper = new ObjectMapper();
            ConfigApplyDelegate delegate = new MockCfgDelegate();
            JsonNode emptyTree = new ObjectMapper().createObjectNode();

            SdnIpConfig sdnIpConfig = new SdnIpConfig();

            sdnIpConfig.init(appId, "sdnip-test", emptyTree, mapper, delegate);

            return (C) sdnIpConfig;
        }
    }

    private class InterfaceServiceDelegate extends InterfaceServiceAdapter {
        @Override
        public void addListener(InterfaceListener listener) {
            SdnIpFibTest.this.interfaceListener = listener;
        }
    }

    private class MockCfgDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(@SuppressWarnings("rawtypes") Config config) {
            config.apply();
        }
    }
}
