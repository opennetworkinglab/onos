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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.routeservice.ResolvedRoute;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteEvent;
import org.onosproject.routeservice.RouteInfo;
import org.onosproject.segmentrouting.config.DeviceConfiguration;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit test for {@link RouteHandler}.
 */
public class RouteHandlerTest {
    private RouteHandler routeHandler;

    // Mocked routing and bridging tables
    private static final Map<MockBridgingTableKey, MockBridgingTableValue> BRIDGING_TABLE =
            Maps.newConcurrentMap();
    private static final Map<MockRoutingTableKey, MockRoutingTableValue> ROUTING_TABLE =
            Maps.newConcurrentMap();
    private static final Map<ConnectPoint, Set<IpPrefix>> SUBNET_TABLE = Maps.newConcurrentMap();
    // Mocked Next Id
    private static final Map<Integer, TrafficTreatment> NEXT_TABLE = Maps.newConcurrentMap();

    private static final IpPrefix P1 = IpPrefix.valueOf("10.0.0.0/24");
    private static final IpAddress N1 = IpAddress.valueOf("10.0.1.254");
    private static final MacAddress M1 = MacAddress.valueOf("00:00:00:00:00:01");
    private static final VlanId V1 = VlanId.vlanId((short) 1);
    private static final ConnectPoint CP1 = ConnectPoint.deviceConnectPoint("of:0000000000000001/1");
    private static final Route R1 = new Route(Route.Source.STATIC, P1, N1);
    private static final ResolvedRoute RR1 = new ResolvedRoute(R1, M1, V1);

    private static final IpAddress N2 = IpAddress.valueOf("10.0.2.254");
    private static final MacAddress M2 = MacAddress.valueOf("00:00:00:00:00:02");
    private static final VlanId V2 = VlanId.vlanId((short) 2);
    private static final ConnectPoint CP2 = ConnectPoint.deviceConnectPoint("of:0000000000000001/2");
    private static final Route R2 = new Route(Route.Source.STATIC, P1, N2);
    private static final ResolvedRoute RR2 = new ResolvedRoute(R2, M2, V2);

    private static final RouteInfo RI1 = new RouteInfo(P1, RR1, Sets.newHashSet(RR1));

    private static final Host H1 = new DefaultHost(ProviderId.NONE, HostId.hostId(M1, V1), M1, V1,
            Sets.newHashSet(new HostLocation(CP1, 0)), Sets.newHashSet(N1), false);
    private static final Host H2 = new DefaultHost(ProviderId.NONE, HostId.hostId(M2, V2), M2, V2,
            Sets.newHashSet(new HostLocation(CP2, 0)), Sets.newHashSet(N2), false);

    // A set of hosts
    private static final Set<Host> HOSTS = Sets.newHashSet(H1, H2);
    // A set of devices of which we have mastership
    private static final Set<DeviceId> LOCAL_DEVICES = Sets.newHashSet();
    // A set of interfaces
    private static final Set<Interface> INTERFACES = Sets.newHashSet();
    // A set of routes
    private static final Set<RouteInfo> ROUTE_INFOS = Sets.newHashSet(RI1);

    @Before
    public void setUp() throws Exception {
// TODO Initialize pairDevice and pairLocalPort config
//        ObjectMapper mapper = new ObjectMapper();
//        ConfigApplyDelegate delegate = config -> {};
//
//        SegmentRoutingDeviceConfig dev3Config = new SegmentRoutingDeviceConfig();
//        JsonNode dev3Tree = mapper.createObjectNode();
//        dev3Config.init(DEV3, "host-handler-test", dev3Tree, mapper, delegate);
//        dev3Config.setPairDeviceId(DEV4).setPairLocalPort(P9);
//
//        SegmentRoutingDeviceConfig dev4Config = new SegmentRoutingDeviceConfig();
//        JsonNode dev4Tree = mapper.createObjectNode();
//        dev4Config.init(DEV4, "host-handler-test", dev4Tree, mapper, delegate);
//        dev4Config.setPairDeviceId(DEV3).setPairLocalPort(P9);

        MockNetworkConfigRegistry mockNetworkConfigRegistry = new MockNetworkConfigRegistry();
//        mockNetworkConfigRegistry.applyConfig(dev3Config);
//        mockNetworkConfigRegistry.applyConfig(dev4Config);

        // Initialize Segment Routing Manager
        SegmentRoutingManager srManager = new MockSegmentRoutingManager(NEXT_TABLE);
        srManager.cfgService = new NetworkConfigRegistryAdapter();
        srManager.deviceConfiguration = new DeviceConfiguration(srManager);
        srManager.flowObjectiveService = new MockFlowObjectiveService(BRIDGING_TABLE, NEXT_TABLE);
        srManager.routingRulePopulator = new MockRoutingRulePopulator(srManager, ROUTING_TABLE);
        srManager.defaultRoutingHandler = new MockDefaultRoutingHandler(srManager, SUBNET_TABLE);
        srManager.interfaceService = new MockInterfaceService(INTERFACES);
        srManager.mastershipService = new MockMastershipService(LOCAL_DEVICES);
        srManager.hostService = new MockHostService(HOSTS);
        srManager.cfgService = mockNetworkConfigRegistry;
        srManager.routeService = new MockRouteService(ROUTE_INFOS);

        routeHandler = new RouteHandler(srManager);

        ROUTING_TABLE.clear();
        BRIDGING_TABLE.clear();
        SUBNET_TABLE.clear();
    }

    @Test
    public void init() throws Exception {
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
    public void processRouteAdded() throws Exception {
        RouteEvent re = new RouteEvent(RouteEvent.Type.ROUTE_ADDED, RR1);
        routeHandler.processRouteAdded(re);

        assertEquals(1, ROUTING_TABLE.size());
        MockRoutingTableValue rtv1 = ROUTING_TABLE.get(new MockRoutingTableKey(CP1.deviceId(), P1));
        assertEquals(M1, rtv1.macAddress);
        assertEquals(V1, rtv1.vlanId);
        assertEquals(CP1.port(), rtv1.portNumber);

        assertEquals(1, SUBNET_TABLE.size());
        assertTrue(SUBNET_TABLE.get(CP1).contains(P1));
    }

    @Test
    public void processRouteUpdated() throws Exception {
        processRouteAdded();

        RouteEvent re = new RouteEvent(RouteEvent.Type.ROUTE_UPDATED, RR2, RR1);
        routeHandler.processRouteUpdated(re);

        assertEquals(1, ROUTING_TABLE.size());
        MockRoutingTableValue rtv2 = ROUTING_TABLE.get(new MockRoutingTableKey(CP1.deviceId(), P1));
        assertEquals(M2, rtv2.macAddress);
        assertEquals(V2, rtv2.vlanId);
        assertEquals(CP2.port(), rtv2.portNumber);

        assertEquals(1, SUBNET_TABLE.size());
        assertTrue(SUBNET_TABLE.get(CP2).contains(P1));
    }

    @Test
    public void processRouteRemoved() throws Exception {
        processRouteAdded();

        RouteEvent re = new RouteEvent(RouteEvent.Type.ROUTE_REMOVED, RR1);
        routeHandler.processRouteRemoved(re);

        assertEquals(0, ROUTING_TABLE.size());
        assertEquals(0, SUBNET_TABLE.size());
    }
}