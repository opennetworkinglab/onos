/*
 * Copyright 2016-present Open Networking Laboratory
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
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
import org.onosproject.incubator.net.routing.RouteEvent;
import org.onosproject.incubator.net.routing.RouteListener;
import org.onosproject.incubator.net.routing.RouteServiceAdapter;
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
import org.onosproject.routing.IntentSynchronizationService;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.onosproject.routing.TestIntentServiceHelper.eqExceptId;

/**
 * Unit tests for SdnIpFib.
 */
public class SdnIpFibNoVlansTest extends AbstractIntentTest {

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

    private static final IpPrefix PREFIX1 = Ip4Prefix.valueOf("1.1.1.0/24");

    private SdnIpFib sdnipFib;
    private IntentSynchronizationService intentSynchronizer;
    private final Set<Interface> interfaces = Sets.newHashSet();

    private static final ApplicationId APPID = TestApplicationId.create("SDNIP");

    private RouteListener routeListener;
    private InterfaceListener interfaceListener;

    @Before
    public void setUp() throws Exception {
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
        sdnipFib.interfaceService = interfaceService;
        sdnipFib.intentSynchronizer = intentSynchronizer;

        sdnipFib.activate();
    }

    /**
     * Sets up the interface service.
     */
    private void setUpInterfaceService() {
        List<InterfaceIpAddress> interfaceIpAddresses1 = Lists.newArrayList();
        interfaceIpAddresses1.add(InterfaceIpAddress.valueOf("192.168.10.101/24"));
        Interface sw1Eth1 = new Interface("sw1-eth1", SW1_ETH1,
                                          interfaceIpAddresses1, MacAddress.valueOf("00:00:00:00:00:01"),
                                          VlanId.NONE);
        interfaces.add(sw1Eth1);

        List<InterfaceIpAddress> interfaceIpAddresses2 = Lists.newArrayList();
        interfaceIpAddresses2.add(InterfaceIpAddress.valueOf("192.168.20.101/24"));
        Interface sw2Eth1 = new Interface("sw2-eth1", SW2_ETH1,
                                          interfaceIpAddresses2, MacAddress.valueOf("00:00:00:00:00:02"),
                                          VlanId.NONE);
        interfaces.add(sw2Eth1);

        InterfaceIpAddress interfaceIpAddress3 = InterfaceIpAddress.valueOf("192.168.30.101/24");
        Interface sw3Eth1 = new Interface("sw3-eth1", SW3_ETH1,
                                          Lists.newArrayList(interfaceIpAddress3),
                                          MacAddress.valueOf("00:00:00:00:00:03"),
                                          VlanId.NONE);
        interfaces.add(sw3Eth1);

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
     * Tests adding a route. All interfaces have no VLAN Ids configured.
     *
     * We verify that the synchronizer records the correct state and that the
     * correct intent is submitted to the IntentService.
     */
    @Test
    public void testRouteAddNoVlans() {
        ResolvedRoute route = new ResolvedRoute(PREFIX1,
                                                Ip4Address.valueOf("192.168.30.1"),
                                                MacAddress.valueOf("00:00:00:00:00:03"));

        // Construct a MultiPointToSinglePointIntent intent
        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(PREFIX1);

        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(MacAddress.valueOf("00:00:00:00:00:03"));

        Set<ConnectPoint> ingressPoints = new HashSet<>();
        ingressPoints.add(SW1_ETH1);
        ingressPoints.add(SW2_ETH1);

        MultiPointToSinglePointIntent intent =
                MultiPointToSinglePointIntent.builder()
                        .appId(APPID)
                        .key(Key.of(PREFIX1.toString(), APPID))
                        .selector(selectorBuilder.build())
                        .treatment(treatmentBuilder.build())
                        .ingressPoints(ingressPoints)
                        .egressPoint(SW3_ETH1)
                        .constraints(SdnIpFib.CONSTRAINTS)
                        .build();

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
     * We verify that the synchronizer records the correct state and that the
     * correct intent is submitted to the IntentService.
     */
    @Test
    public void testRouteUpdate() {
        // Add a route first
        testRouteAddNoVlans();

        // Start to construct a new route entry and new intent
        ResolvedRoute route = new ResolvedRoute(PREFIX1,
                Ip4Address.valueOf("192.168.20.1"),
                MacAddress.valueOf("00:00:00:00:00:02"));

        // Construct a new MultiPointToSinglePointIntent intent
        TrafficSelector.Builder selectorBuilderNew =
                DefaultTrafficSelector.builder();
        selectorBuilderNew.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(PREFIX1);

        TrafficTreatment.Builder treatmentBuilderNew =
                DefaultTrafficTreatment.builder();
        treatmentBuilderNew.setEthDst(MacAddress.valueOf("00:00:00:00:00:02"));

        Set<ConnectPoint> ingressPointsNew = new HashSet<>();
        ingressPointsNew.add(SW1_ETH1);
        ingressPointsNew.add(SW3_ETH1);

        MultiPointToSinglePointIntent intentNew =
                MultiPointToSinglePointIntent.builder()
                        .appId(APPID)
                        .key(Key.of(PREFIX1.toString(), APPID))
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

        // Send in the update event
        routeListener.event(new RouteEvent(RouteEvent.Type.ROUTE_UPDATED, route));

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
        testRouteAddNoVlans();

        // Construct the existing route entry
        ResolvedRoute route = new ResolvedRoute(PREFIX1, null, null);

        // Construct the existing MultiPointToSinglePoint intent
        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(PREFIX1);

        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(MacAddress.valueOf("00:00:00:00:00:03"));

        Set<ConnectPoint> ingressPoints = new HashSet<>();
        ingressPoints.add(SW1_ETH1);
        ingressPoints.add(SW2_ETH1);

        MultiPointToSinglePointIntent addedIntent =
                MultiPointToSinglePointIntent.builder()
                        .appId(APPID)
                        .key(Key.of(PREFIX1.toString(), APPID))
                        .selector(selectorBuilder.build())
                        .treatment(treatmentBuilder.build())
                        .ingressPoints(ingressPoints)
                        .egressPoint(SW3_ETH1)
                        .constraints(SdnIpFib.CONSTRAINTS)
                        .build();

        // Set up expectation
        reset(intentSynchronizer);
        // Setup the expected intents
        intentSynchronizer.withdraw(eqExceptId(addedIntent));
        replay(intentSynchronizer);

        // Send in the removed event
        routeListener.event(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED, route));

        verify(intentSynchronizer);
    }

    @Test
    public void testAddInterface() {
        // Add a route first
        testRouteAddNoVlans();

        // Construct the existing MultiPointToSinglePoint intent
        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(PREFIX1);

        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(MacAddress.valueOf("00:00:00:00:00:03"));

        Set<ConnectPoint> ingressPoints = new HashSet<>();
        ingressPoints.add(SW1_ETH1);
        ingressPoints.add(SW2_ETH1);
        ingressPoints.add(SW4_ETH1);

        MultiPointToSinglePointIntent addedIntent =
                MultiPointToSinglePointIntent.builder()
                        .appId(APPID)
                        .key(Key.of(PREFIX1.toString(), APPID))
                        .selector(selectorBuilder.build())
                        .treatment(treatmentBuilder.build())
                        .ingressPoints(ingressPoints)
                        .egressPoint(SW3_ETH1)
                        .constraints(SdnIpFib.CONSTRAINTS)
                        .build();

        reset(intentSynchronizer);

        intentSynchronizer.submit(eqExceptId(addedIntent));
        expectLastCall().once();

        replay(intentSynchronizer);

        Interface intf = new Interface("sw4-eth1", SW4_ETH1,
                Collections.singletonList(InterfaceIpAddress.valueOf("192.168.40.101/24")),
                MacAddress.valueOf("00:00:00:00:00:04"), VlanId.NONE);
        InterfaceEvent intfEvent = new InterfaceEvent(InterfaceEvent.Type.INTERFACE_ADDED, intf);
        interfaceListener.event(intfEvent);

        verify(intentSynchronizer);
    }

    @Test
    public void testRemoveInterface() {
        // Add a route first
        testRouteAddNoVlans();

        // Construct the existing MultiPointToSinglePoint intent
        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(PREFIX1);

        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(MacAddress.valueOf("00:00:00:00:00:03"));

        Set<ConnectPoint> ingressPoints = new HashSet<>();
        ingressPoints.add(SW2_ETH1);

        MultiPointToSinglePointIntent addedIntent =
                MultiPointToSinglePointIntent.builder()
                        .appId(APPID)
                        .key(Key.of(PREFIX1.toString(), APPID))
                        .selector(selectorBuilder.build())
                        .treatment(treatmentBuilder.build())
                        .ingressPoints(ingressPoints)
                        .egressPoint(SW3_ETH1)
                        .constraints(SdnIpFib.CONSTRAINTS)
                        .build();

        reset(intentSynchronizer);

        intentSynchronizer.submit(eqExceptId(addedIntent));
        expectLastCall().once();

        replay(intentSynchronizer);

        Interface intf = new Interface("sw1-eth1", SW1_ETH1,
                Collections.singletonList(InterfaceIpAddress.valueOf("192.168.10.101/24")),
                MacAddress.valueOf("00:00:00:00:00:01"), VlanId.NONE);
        InterfaceEvent intfEvent = new InterfaceEvent(InterfaceEvent.Type.INTERFACE_REMOVED, intf);
        interfaceListener.event(intfEvent);

        verify(intentSynchronizer);
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
            SdnIpFibNoVlansTest.this.routeListener = routeListener;
        }
    }

    private class InterfaceServiceDelegate extends InterfaceServiceAdapter {
        @Override
        public void addListener(InterfaceListener listener) {
            SdnIpFibNoVlansTest.this.interfaceListener = listener;
        }
    }
}
