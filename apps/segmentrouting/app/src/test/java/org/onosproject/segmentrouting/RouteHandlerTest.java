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

package org.onosproject.segmentrouting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.routeservice.ResolvedRoute;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteEvent;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.onosproject.segmentrouting.config.SegmentRoutingDeviceConfig;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TestConsistentMap;

import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.*;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * Unit test for {@link RouteHandler}.
 */
public class RouteHandlerTest {
    private SegmentRoutingManager srManager;
    private RouteHandler routeHandler;
    private HostService hostService;

    // Mocked routing and bridging tables
    private static final Map<MockBridgingTableKey, MockBridgingTableValue> BRIDGING_TABLE =
            Maps.newConcurrentMap();
    private static final Map<MockRoutingTableKey, MockRoutingTableValue> ROUTING_TABLE =
            Maps.newConcurrentMap();
    private static final Map<ConnectPoint, Set<IpPrefix>> SUBNET_TABLE = Maps.newConcurrentMap();
    // Mocked Next Id
    private static final Map<Integer, TrafficTreatment> NEXT_TABLE = Maps.newConcurrentMap();
    private static final Map<IpPrefix, Set<ResolvedRoute>> ROUTE_STORE = Maps.newConcurrentMap();

    private static final IpPrefix P1 = IpPrefix.valueOf("10.0.0.0/24");

    // Single homed router 1
    private static final IpAddress N1 = IpAddress.valueOf("10.0.1.254");
    private static final MacAddress M1 = MacAddress.valueOf("00:00:00:00:00:01");
    private static final VlanId V1 = VlanId.vlanId((short) 1);
    private static final ConnectPoint CP1 = ConnectPoint.deviceConnectPoint("of:0000000000000001/1");
    private static final Route R1 = new Route(Route.Source.STATIC, P1, N1);
    private static final ResolvedRoute RR1 = new ResolvedRoute(R1, M1, V1);

    // Single homed router 2
    private static final IpAddress N2 = IpAddress.valueOf("10.0.2.254");
    private static final MacAddress M2 = MacAddress.valueOf("00:00:00:00:00:02");
    private static final VlanId V2 = VlanId.vlanId((short) 2);
    private static final ConnectPoint CP2 = ConnectPoint.deviceConnectPoint("of:0000000000000002/2");
    private static final Route R2 = new Route(Route.Source.STATIC, P1, N2);
    private static final ResolvedRoute RR2 = new ResolvedRoute(R2, M2, V2);

    // Dual homed router 1
    private static final IpAddress N3 = IpAddress.valueOf("10.0.3.254");
    private static final MacAddress M3 = MacAddress.valueOf("00:00:00:00:00:03");
    private static final VlanId V3 = VlanId.vlanId((short) 3);
    private static final Route R3 = new Route(Route.Source.STATIC, P1, N3);
    private static final ResolvedRoute RR3 = new ResolvedRoute(R3, M3, V3);

    // Single homed router 3
    private static final IpAddress N4 = IpAddress.valueOf("10.0.4.254");
    private static final MacAddress M4 = MacAddress.valueOf("00:00:00:00:00:04");
    private static final VlanId V4 = VlanId.vlanId((short) 4);
    private static final ConnectPoint CP4 = ConnectPoint.deviceConnectPoint("of:0000000000000004/4");
    private static final Route R4 = new Route(Route.Source.STATIC, P1, N4);
    private static final ResolvedRoute RR4 = new ResolvedRoute(R4, M4, V4);

    // Hosts
    private static final Host H1 = new DefaultHost(ProviderId.NONE, HostId.hostId(M1, V1), M1, V1,
            Sets.newHashSet(new HostLocation(CP1, 0)), Sets.newHashSet(N1), false);
    private static final Host H2 = new DefaultHost(ProviderId.NONE, HostId.hostId(M2, V2), M2, V2,
            Sets.newHashSet(new HostLocation(CP2, 0)), Sets.newHashSet(N2), false);
    private static final Host H3D = new DefaultHost(ProviderId.NONE, HostId.hostId(M3, V3), M3, V3,
            Sets.newHashSet(new HostLocation(CP1, 0), new HostLocation(CP2, 0)), Sets.newHashSet(N3), false);
    private static final Host H3S = new DefaultHost(ProviderId.NONE, HostId.hostId(M3, V3), M3, V3,
            Sets.newHashSet(new HostLocation(CP1, 0)), Sets.newHashSet(N3), false);
    private static final Host H4 = new DefaultHost(ProviderId.NONE, HostId.hostId(M4, V4), M4, V4,
            Sets.newHashSet(new HostLocation(CP4, 0)), Sets.newHashSet(N4), false);

    // Pair Local Port
    private static final PortNumber P9 = PortNumber.portNumber(9);

    // A set of hosts
    private static final Set<Host> HOSTS = Sets.newHashSet(H1, H2, H3D, H4);
    private static final Set<Host> HOSTS_ONE_FAIL = Sets.newHashSet(H1, H2, H3S);
    private static final Set<Host> HOSTS_BOTH_FAIL = Sets.newHashSet(H1, H2);
    // A set of devices of which we have mastership
    private static final Set<DeviceId> LOCAL_DEVICES = Sets.newHashSet(CP1.deviceId(), CP2.deviceId());
    // A set of interfaces
    private static final Set<Interface> INTERFACES = Sets.newHashSet();

    @Before
    public void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        ConfigApplyDelegate delegate = config -> { };

        SegmentRoutingDeviceConfig dev1Config = new SegmentRoutingDeviceConfig();
        JsonNode dev1Tree = mapper.createObjectNode();
        dev1Config.init(CP1.deviceId(), "host-handler-test", dev1Tree, mapper, delegate);
        dev1Config.setPairDeviceId(CP2.deviceId()).setPairLocalPort(P9);

        SegmentRoutingDeviceConfig dev2Config = new SegmentRoutingDeviceConfig();
        JsonNode dev2Tree = mapper.createObjectNode();
        dev2Config.init(CP2.deviceId(), "host-handler-test", dev2Tree, mapper, delegate);
        dev2Config.setPairDeviceId(CP1.deviceId()).setPairLocalPort(P9);

        MockNetworkConfigRegistry mockNetworkConfigRegistry = new MockNetworkConfigRegistry();
        mockNetworkConfigRegistry.applyConfig(dev1Config);
        mockNetworkConfigRegistry.applyConfig(dev2Config);

        // Initialize Segment Routing Manager
        srManager = new MockSegmentRoutingManager(NEXT_TABLE);
        srManager.storageService = createMock(StorageService.class);
        expect(srManager.storageService.consistentMapBuilder()).andReturn(new TestConsistentMap.Builder<>()).anyTimes();
        replay(srManager.storageService);
        srManager.cfgService = new NetworkConfigRegistryAdapter();
        srManager.deviceConfiguration = createMock(DeviceConfiguration.class);
        srManager.flowObjectiveService = new MockFlowObjectiveService(BRIDGING_TABLE, NEXT_TABLE);
        srManager.routingRulePopulator = new MockRoutingRulePopulator(srManager, ROUTING_TABLE);
        srManager.defaultRoutingHandler = new MockDefaultRoutingHandler(srManager, SUBNET_TABLE, ROUTING_TABLE);
        srManager.interfaceService = new MockInterfaceService(INTERFACES);
        srManager.mastershipService = new MockMastershipService(LOCAL_DEVICES);
        hostService = new MockHostService(HOSTS);
        srManager.hostService = hostService;
        srManager.cfgService = mockNetworkConfigRegistry;
        srManager.routeService = new MockRouteService(ROUTE_STORE);

        routeHandler = new RouteHandler(srManager);

        ROUTING_TABLE.clear();
        BRIDGING_TABLE.clear();
        SUBNET_TABLE.clear();
    }

    @Test
    public void init() {
        ROUTE_STORE.put(P1, Sets.newHashSet(RR1));

        routeHandler.init(CP1.deviceId());

        assertEquals(1, ROUTING_TABLE.size());
        MockRoutingTableValue rtv1 = ROUTING_TABLE.get(new MockRoutingTableKey(CP1.deviceId(), P1));
        assertEquals(M1, rtv1.macAddress);
        assertEquals(V1, rtv1.vlanId);
        assertEquals(CP1.port(), rtv1.portNumber);

        assertEquals(1, SUBNET_TABLE.size());
        assertTrue(SUBNET_TABLE.get(CP1).contains(P1));
    }

    @Test
    public void initTwoNextHops() {
        ROUTE_STORE.put(P1, Sets.newHashSet(RR1, RR2));

        routeHandler.init(CP1.deviceId());

        assertEquals(2, ROUTING_TABLE.size());
        MockRoutingTableValue rtv1 = ROUTING_TABLE.get(new MockRoutingTableKey(CP1.deviceId(), P1));
        assertEquals(M1, rtv1.macAddress);
        assertEquals(V1, rtv1.vlanId);
        assertEquals(CP1.port(), rtv1.portNumber);
        MockRoutingTableValue rtv2 = ROUTING_TABLE.get(new MockRoutingTableKey(CP2.deviceId(), P1));
        assertEquals(M2, rtv2.macAddress);
        assertEquals(V2, rtv2.vlanId);
        assertEquals(CP2.port(), rtv2.portNumber);

        assertEquals(2, SUBNET_TABLE.size());
        assertTrue(SUBNET_TABLE.get(CP1).contains(P1));
        assertTrue(SUBNET_TABLE.get(CP2).contains(P1));
    }

    @Test
    public void processRouteAdded() {
        reset(srManager.deviceConfiguration);
        srManager.deviceConfiguration.addSubnet(CP1, P1);
        expectLastCall().once();
        replay(srManager.deviceConfiguration);

        RouteEvent re = new RouteEvent(RouteEvent.Type.ROUTE_ADDED, RR1, Sets.newHashSet(RR1));
        routeHandler.processRouteAdded(re);

        assertEquals(1, ROUTING_TABLE.size());
        MockRoutingTableValue rtv1 = ROUTING_TABLE.get(new MockRoutingTableKey(CP1.deviceId(), P1));
        assertEquals(M1, rtv1.macAddress);
        assertEquals(V1, rtv1.vlanId);
        assertEquals(CP1.port(), rtv1.portNumber);

        assertEquals(1, SUBNET_TABLE.size());
        assertTrue(SUBNET_TABLE.get(CP1).contains(P1));

        verify(srManager.deviceConfiguration);
    }

    @Test
    public void processRouteUpdated() {
        processRouteAdded();

        reset(srManager.deviceConfiguration);
        srManager.deviceConfiguration.removeSubnet(CP1, P1);
        expectLastCall().once();
        srManager.deviceConfiguration.addSubnet(CP2, P1);
        expectLastCall().once();
        replay(srManager.deviceConfiguration);

        RouteEvent re = new RouteEvent(RouteEvent.Type.ROUTE_UPDATED, RR2, RR1, Sets.newHashSet(RR2),
                Sets.newHashSet(RR1));
        routeHandler.processRouteUpdated(re);

        // Note: We shouldn't remove the old nexthop during the occasion of route update
        //       since the populate subnet will take care of it and point it to an ECMP group
        assertEquals(2, ROUTING_TABLE.size());
        MockRoutingTableValue rtv2 = ROUTING_TABLE.get(new MockRoutingTableKey(CP2.deviceId(), P1));
        assertEquals(M2, rtv2.macAddress);
        assertEquals(V2, rtv2.vlanId);
        assertEquals(CP2.port(), rtv2.portNumber);

        assertEquals(1, SUBNET_TABLE.size());
        assertTrue(SUBNET_TABLE.get(CP2).contains(P1));

        verify(srManager.deviceConfiguration);
    }

    @Test
    public void processRouteRemoved() {
        processRouteAdded();

        reset(srManager.deviceConfiguration);
        srManager.deviceConfiguration.removeSubnet(CP1, P1);
        expectLastCall().once();
        replay(srManager.deviceConfiguration);

        RouteEvent re = new RouteEvent(RouteEvent.Type.ROUTE_REMOVED, RR1, Sets.newHashSet(RR1));
        routeHandler.processRouteRemoved(re);

        assertEquals(0, ROUTING_TABLE.size());
        assertEquals(0, SUBNET_TABLE.size());

        verify(srManager.deviceConfiguration);
    }

    @Test
    public void testTwoSingleHomedAdded() {
        reset(srManager.deviceConfiguration);
        srManager.deviceConfiguration.addSubnet(CP1, P1);
        expectLastCall().once();
        srManager.deviceConfiguration.addSubnet(CP2, P1);
        expectLastCall().once();
        replay(srManager.deviceConfiguration);

        RouteEvent re = new RouteEvent(RouteEvent.Type.ROUTE_ADDED, RR1, Sets.newHashSet(RR1, RR2));
        routeHandler.processRouteAdded(re);

        assertEquals(2, ROUTING_TABLE.size());
        MockRoutingTableValue rtv1 = ROUTING_TABLE.get(new MockRoutingTableKey(CP1.deviceId(), P1));
        MockRoutingTableValue rtv2 = ROUTING_TABLE.get(new MockRoutingTableKey(CP2.deviceId(), P1));
        assertEquals(M1, rtv1.macAddress);
        assertEquals(M2, rtv2.macAddress);
        assertEquals(V1, rtv1.vlanId);
        assertEquals(V2, rtv2.vlanId);
        assertEquals(CP1.port(), rtv1.portNumber);
        assertEquals(CP2.port(), rtv2.portNumber);

        assertEquals(2, SUBNET_TABLE.size());
        assertTrue(SUBNET_TABLE.get(CP1).contains(P1));
        assertTrue(SUBNET_TABLE.get(CP2).contains(P1));

        verify(srManager.deviceConfiguration);
    }

    @Test
    public void testOneDualHomedAdded() {
        reset(srManager.deviceConfiguration);
        srManager.deviceConfiguration.addSubnet(CP1, P1);
        expectLastCall().once();
        srManager.deviceConfiguration.addSubnet(CP2, P1);
        expectLastCall().once();
        replay(srManager.deviceConfiguration);

        RouteEvent re = new RouteEvent(RouteEvent.Type.ROUTE_ADDED, RR3, Sets.newHashSet(RR3));
        routeHandler.processRouteAdded(re);

        assertEquals(2, ROUTING_TABLE.size());
        MockRoutingTableValue rtv1 = ROUTING_TABLE.get(new MockRoutingTableKey(CP1.deviceId(), P1));
        MockRoutingTableValue rtv2 = ROUTING_TABLE.get(new MockRoutingTableKey(CP2.deviceId(), P1));
        assertEquals(M3, rtv1.macAddress);
        assertEquals(M3, rtv2.macAddress);
        assertEquals(V3, rtv1.vlanId);
        assertEquals(V3, rtv2.vlanId);
        assertEquals(CP1.port(), rtv1.portNumber);
        assertEquals(CP2.port(), rtv2.portNumber);

        assertEquals(2, SUBNET_TABLE.size());
        assertTrue(SUBNET_TABLE.get(CP1).contains(P1));
        assertTrue(SUBNET_TABLE.get(CP2).contains(P1));

        verify(srManager.deviceConfiguration);
    }

    @Test
    public void testOneSingleHomedToTwoSingleHomed() {
        processRouteAdded();

        reset(srManager.deviceConfiguration);
        srManager.deviceConfiguration.addSubnet(CP2, P1);
        expectLastCall().once();
        replay(srManager.deviceConfiguration);

        RouteEvent re = new RouteEvent(RouteEvent.Type.ALTERNATIVE_ROUTES_CHANGED, RR1, null,
                Sets.newHashSet(RR1, RR2), Sets.newHashSet(RR1));
        routeHandler.processAlternativeRoutesChanged(re);

        assertEquals(2, ROUTING_TABLE.size());
        MockRoutingTableValue rtv1 = ROUTING_TABLE.get(new MockRoutingTableKey(CP1.deviceId(), P1));
        MockRoutingTableValue rtv2 = ROUTING_TABLE.get(new MockRoutingTableKey(CP2.deviceId(), P1));
        assertEquals(M1, rtv1.macAddress);
        assertEquals(M2, rtv2.macAddress);
        assertEquals(V1, rtv1.vlanId);
        assertEquals(V2, rtv2.vlanId);
        assertEquals(CP1.port(), rtv1.portNumber);
        assertEquals(CP2.port(), rtv2.portNumber);

        assertEquals(2, SUBNET_TABLE.size());
        assertTrue(SUBNET_TABLE.get(CP1).contains(P1));
        assertTrue(SUBNET_TABLE.get(CP2).contains(P1));

        verify(srManager.deviceConfiguration);
    }

    @Test
    public void testTwoSingleHomedToOneSingleHomed() {
        testTwoSingleHomedAdded();

        reset(srManager.deviceConfiguration);
        srManager.deviceConfiguration.removeSubnet(CP2, P1);
        expectLastCall().once();
        replay(srManager.deviceConfiguration);

        RouteEvent re = new RouteEvent(RouteEvent.Type.ALTERNATIVE_ROUTES_CHANGED, RR1, null,
                Sets.newHashSet(RR1), Sets.newHashSet(RR1, RR2));
        routeHandler.processAlternativeRoutesChanged(re);

        // Note: We shouldn't remove the old nexthop during the occasion of route update
        //       since the populate subnet will take care of it and point it to an ECMP group
        assertEquals(2, ROUTING_TABLE.size());
        MockRoutingTableValue rtv1 = ROUTING_TABLE.get(new MockRoutingTableKey(CP1.deviceId(), P1));
        assertEquals(M1, rtv1.macAddress);
        assertEquals(V1, rtv1.vlanId);
        assertEquals(CP1.port(), rtv1.portNumber);

        assertEquals(1, SUBNET_TABLE.size());
        assertTrue(SUBNET_TABLE.get(CP1).contains(P1));

        verify(srManager.deviceConfiguration);
    }

    // TODO Add test cases for two single homed next hop at same location

    @Test
    public void testDualHomedSingleLocationFail() {
        testOneDualHomedAdded();

        ROUTE_STORE.put(P1, Sets.newHashSet(RR3));

        HostEvent he = new HostEvent(HostEvent.Type.HOST_MOVED, H3S, H3D);
        routeHandler.processHostMovedEvent(he);

        assertEquals(2, ROUTING_TABLE.size());
        MockRoutingTableValue rtv1 = ROUTING_TABLE.get(new MockRoutingTableKey(CP1.deviceId(), P1));
        MockRoutingTableValue rtv2 = ROUTING_TABLE.get(new MockRoutingTableKey(CP2.deviceId(), P1));
        assertEquals(M3, rtv1.macAddress);
        assertEquals(M3, rtv2.macAddress);
        assertEquals(V3, rtv1.vlanId);
        assertEquals(V3, rtv2.vlanId);
        assertEquals(CP1.port(), rtv1.portNumber);
        assertEquals(P9, rtv2.portNumber);

        // ECMP route table hasn't changed
        assertEquals(2, SUBNET_TABLE.size());
        assertTrue(SUBNET_TABLE.get(CP1).contains(P1));
        assertTrue(SUBNET_TABLE.get(CP2).contains(P1));
    }

    @Test
    public void testDualHomedBothLocationFail() {
        testDualHomedSingleLocationFail();

        hostService = new MockHostService(HOSTS_ONE_FAIL);

        reset(srManager.deviceConfiguration);
        srManager.deviceConfiguration.removeSubnet(CP1, P1);
        expectLastCall().once();
        srManager.deviceConfiguration.removeSubnet(CP2, P1);
        expectLastCall().once();
        replay(srManager.deviceConfiguration);

        RouteEvent re = new RouteEvent(RouteEvent.Type.ROUTE_REMOVED, RR3, Sets.newHashSet(RR3));
        routeHandler.processRouteRemoved(re);

        assertEquals(0, ROUTING_TABLE.size());
        assertEquals(0, SUBNET_TABLE.size());

        verify(srManager.deviceConfiguration);
    }

    @Test
    public void testTwoSingleHomedRemoved() {
        testTwoSingleHomedAdded();

        hostService = new MockHostService(HOSTS_BOTH_FAIL);

        reset(srManager.deviceConfiguration);
        srManager.deviceConfiguration.removeSubnet(CP1, P1);
        expectLastCall().once();
        srManager.deviceConfiguration.removeSubnet(CP2, P1);
        expectLastCall().once();
        replay(srManager.deviceConfiguration);

        RouteEvent re = new RouteEvent(RouteEvent.Type.ROUTE_REMOVED, RR1, Sets.newHashSet(RR1, RR2));
        routeHandler.processRouteRemoved(re);

        assertEquals(0, ROUTING_TABLE.size());
        assertEquals(0, SUBNET_TABLE.size());

        verify(srManager.deviceConfiguration);
    }

    @Test
    public void testOneDualHomeRemoved() {
        testOneDualHomedAdded();

        reset(srManager.deviceConfiguration);
        srManager.deviceConfiguration.removeSubnet(CP1, P1);
        expectLastCall().once();
        srManager.deviceConfiguration.removeSubnet(CP2, P1);
        expectLastCall().once();
        replay(srManager.deviceConfiguration);

        RouteEvent re = new RouteEvent(RouteEvent.Type.ROUTE_REMOVED, RR3, Sets.newHashSet(RR3));
        routeHandler.processRouteRemoved(re);

        assertEquals(0, ROUTING_TABLE.size());
        assertEquals(0, SUBNET_TABLE.size());

        verify(srManager.deviceConfiguration);
    }

    @Test
    public void testMoreThanTwoNextHop() {
        // next hop = CP1, CP2
        reset(srManager.deviceConfiguration);
        srManager.deviceConfiguration.addSubnet(CP1, P1);
        srManager.deviceConfiguration.addSubnet(CP2, P1);
        expectLastCall().once();
        replay(srManager.deviceConfiguration);

        RouteEvent re = new RouteEvent(RouteEvent.Type.ROUTE_ADDED, RR1, Sets.newHashSet(RR1, RR2));
        routeHandler.processRouteAdded(re);

        assertEquals(2, ROUTING_TABLE.size());
        MockRoutingTableValue rtv1 = ROUTING_TABLE.get(new MockRoutingTableKey(CP1.deviceId(), P1));
        assertEquals(M1, rtv1.macAddress);
        assertEquals(V1, rtv1.vlanId);
        assertEquals(CP1.port(), rtv1.portNumber);
        MockRoutingTableValue rtv2 = ROUTING_TABLE.get(new MockRoutingTableKey(CP2.deviceId(), P1));
        assertEquals(M2, rtv2.macAddress);
        assertEquals(V2, rtv2.vlanId);
        assertEquals(CP2.port(), rtv2.portNumber);

        assertEquals(2, SUBNET_TABLE.size());
        assertTrue(SUBNET_TABLE.get(CP1).contains(P1));
        assertTrue(SUBNET_TABLE.get(CP2).contains(P1));

        verify(srManager.deviceConfiguration);

        // next hop = CP1, CP2, CP4 (invalid)
        re = new RouteEvent(RouteEvent.Type.ALTERNATIVE_ROUTES_CHANGED, RR1, null,
                Sets.newHashSet(RR1, RR2, RR4), Sets.newHashSet(RR1, RR2));
        routeHandler.processAlternativeRoutesChanged(re);

        assertEquals(2, ROUTING_TABLE.size());
        rtv1 = ROUTING_TABLE.get(new MockRoutingTableKey(CP1.deviceId(), P1));
        assertEquals(M1, rtv1.macAddress);
        assertEquals(V1, rtv1.vlanId);
        assertEquals(CP1.port(), rtv1.portNumber);
        rtv2 = ROUTING_TABLE.get(new MockRoutingTableKey(CP2.deviceId(), P1));
        assertEquals(M2, rtv2.macAddress);
        assertEquals(V2, rtv2.vlanId);
        assertEquals(CP2.port(), rtv2.portNumber);

        assertEquals(2, SUBNET_TABLE.size());
        assertTrue(SUBNET_TABLE.get(CP1).contains(P1));
        assertTrue(SUBNET_TABLE.get(CP2).contains(P1));

        // next hop = CP2, CP4
        reset(srManager.deviceConfiguration);
        srManager.deviceConfiguration.addSubnet(CP2, P1);
        srManager.deviceConfiguration.addSubnet(CP4, P1);
        expectLastCall().once();
        replay(srManager.deviceConfiguration);

        re = new RouteEvent(RouteEvent.Type.ALTERNATIVE_ROUTES_CHANGED, RR1, null,
                Sets.newHashSet(RR2, RR4), Sets.newHashSet(RR1, RR2, RR4));
        routeHandler.processAlternativeRoutesChanged(re);

        assertEquals(2, ROUTING_TABLE.size());
        rtv1 = ROUTING_TABLE.get(new MockRoutingTableKey(CP2.deviceId(), P1));
        assertEquals(M2, rtv1.macAddress);
        assertEquals(V2, rtv1.vlanId);
        assertEquals(CP2.port(), rtv1.portNumber);
        rtv2 = ROUTING_TABLE.get(new MockRoutingTableKey(CP4.deviceId(), P1));
        assertEquals(M4, rtv2.macAddress);
        assertEquals(V4, rtv2.vlanId);
        assertEquals(CP4.port(), rtv2.portNumber);

        assertEquals(2, SUBNET_TABLE.size());
        assertTrue(SUBNET_TABLE.get(CP2).contains(P1));
        assertTrue(SUBNET_TABLE.get(CP4).contains(P1));

        verify(srManager.deviceConfiguration);
    }
}