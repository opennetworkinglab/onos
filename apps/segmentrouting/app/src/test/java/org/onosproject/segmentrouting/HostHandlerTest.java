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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.host.ProbeMode;
import org.onosproject.net.intf.Interface;
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
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.routeservice.ResolvedRoute;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteInfo;
import org.onosproject.routeservice.RouteService;
import org.onosproject.routeservice.RouteTableId;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.onosproject.segmentrouting.config.SegmentRoutingDeviceConfig;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TestConsistentMap;

import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;

/**r
 * Unit test for {@link HostHandler}.
 */
public class HostHandlerTest {
    private HostHandler hostHandler;

    // Mocked routing and bridging tables
    private static final Map<MockBridgingTableKey, MockBridgingTableValue> BRIDGING_TABLE =
            Maps.newConcurrentMap();
    private static final Map<MockRoutingTableKey, MockRoutingTableValue> ROUTING_TABLE =
            Maps.newConcurrentMap();
    private static final Map<ConnectPoint, Set<IpPrefix>> SUBNET_TABLE = Maps.newConcurrentMap();
    // Mocked Next Id
    private static final Map<Integer, TrafficTreatment> NEXT_TABLE = Maps.newConcurrentMap();

    // Host Mac, VLAN
    private static final ProviderId PROVIDER_ID = ProviderId.NONE;
    private static final MacAddress HOST_MAC = MacAddress.valueOf("00:00:00:00:00:01");
    private static final VlanId HOST_VLAN_UNTAGGED = VlanId.NONE;
    private static final HostId HOST_ID_UNTAGGED = HostId.hostId(HOST_MAC, HOST_VLAN_UNTAGGED);
    private static final VlanId HOST_VLAN_TAGGED = VlanId.vlanId((short) 20);
    private static final HostId HOST_ID_TAGGED = HostId.hostId(HOST_MAC, HOST_VLAN_TAGGED);
    // Host IP
    private static final IpAddress HOST_IP11 = IpAddress.valueOf("10.0.1.1");
    private static final IpAddress HOST_IP21 = IpAddress.valueOf("10.0.2.1");
    private static final IpAddress HOST_IP12 = IpAddress.valueOf("10.0.1.2");
    private static final IpAddress HOST_IP13 = IpAddress.valueOf("10.0.1.3");
    private static final IpAddress HOST_IP14 = IpAddress.valueOf("10.0.1.4");
    private static final IpAddress HOST_IP32 = IpAddress.valueOf("10.0.3.2");
    // Device
    private static final DeviceId DEV1 = DeviceId.deviceId("of:0000000000000001");
    private static final DeviceId DEV2 = DeviceId.deviceId("of:0000000000000002");
    private static final DeviceId DEV3 = DeviceId.deviceId("of:0000000000000003");
    private static final DeviceId DEV4 = DeviceId.deviceId("of:0000000000000004");
    private static final DeviceId DEV5 = DeviceId.deviceId("of:0000000000000005");
    private static final DeviceId DEV6 = DeviceId.deviceId("of:0000000000000006");
    // Port
    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final PortNumber P2 = PortNumber.portNumber(2);
    private static final PortNumber P3 = PortNumber.portNumber(3);
    private static final PortNumber P9 = PortNumber.portNumber(9);
    // Connect Point
    private static final ConnectPoint CP11 = new ConnectPoint(DEV1, P1);
    private static final HostLocation HOST_LOC11 = new HostLocation(CP11, 0);
    private static final ConnectPoint CP12 = new ConnectPoint(DEV1, P2);
    private static final HostLocation HOST_LOC12 = new HostLocation(CP12, 0);
    private static final ConnectPoint CP13 = new ConnectPoint(DEV1, P3);
    private static final HostLocation HOST_LOC13 = new HostLocation(CP13, 0);
    private static final ConnectPoint CP21 = new ConnectPoint(DEV2, P1);
    private static final HostLocation HOST_LOC21 = new HostLocation(CP21, 0);
    private static final ConnectPoint CP22 = new ConnectPoint(DEV2, P2);
    private static final HostLocation HOST_LOC22 = new HostLocation(CP22, 0);
    // Connect Point for dual-homed host failover
    private static final ConnectPoint CP31 = new ConnectPoint(DEV3, P1);
    private static final HostLocation HOST_LOC31 = new HostLocation(CP31, 0);
    private static final ConnectPoint CP32 = new ConnectPoint(DEV3, P2);
    private static final HostLocation HOST_LOC32 = new HostLocation(CP32, 0);
    private static final ConnectPoint CP41 = new ConnectPoint(DEV4, P1);
    private static final HostLocation HOST_LOC41 = new HostLocation(CP41, 0);
    private static final ConnectPoint CP39 = new ConnectPoint(DEV3, P9);
    private static final ConnectPoint CP49 = new ConnectPoint(DEV4, P9);
    // Conenct Point for mastership test
    private static final ConnectPoint CP51 = new ConnectPoint(DEV5, P1);
    private static final HostLocation HOST_LOC51 = new HostLocation(CP51, 0);
    private static final ConnectPoint CP61 = new ConnectPoint(DEV6, P1);
    private static final HostLocation HOST_LOC61 = new HostLocation(CP61, 0);
    // Interface VLAN
    private static final VlanId INTF_VLAN_UNTAGGED = VlanId.vlanId((short) 10);
    private static final VlanId INTF_VLAN_TAGGED_1 = VlanId.vlanId((short) 20);
    private static final Set<VlanId> INTF_VLAN_TAGGED = Sets.newHashSet(INTF_VLAN_TAGGED_1);
    private static final VlanId INTF_VLAN_NATIVE = VlanId.vlanId((short) 30);
    private static final Set<VlanId> INTF_VLAN_PAIR = Sets.newHashSet(VlanId.vlanId((short) 10),
            VlanId.vlanId((short) 20), VlanId.vlanId((short) 30));
    private static final VlanId INTF_VLAN_OTHER = VlanId.vlanId((short) 40);
    // Interface subnet
    private static final IpPrefix INTF_PREFIX1 = IpPrefix.valueOf("10.0.1.254/24");
    private static final IpPrefix INTF_PREFIX2 = IpPrefix.valueOf("10.0.2.254/24");
    private static final IpPrefix INTF_PREFIX3 = IpPrefix.valueOf("10.0.3.254/24");
    private static final InterfaceIpAddress INTF_IP1 =
            new InterfaceIpAddress(INTF_PREFIX1.address(), INTF_PREFIX1);
    private static final InterfaceIpAddress INTF_IP2 =
            new InterfaceIpAddress(INTF_PREFIX2.address(), INTF_PREFIX2);
    private static final InterfaceIpAddress INTF_IP3 =
            new InterfaceIpAddress(INTF_PREFIX3.address(), INTF_PREFIX3);
    // Interfaces
    private static final Interface INTF11 =
            new Interface(null, CP11, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                    INTF_VLAN_UNTAGGED, null, null);
    private static final Interface INTF12 =
            new Interface(null, CP12, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                    INTF_VLAN_UNTAGGED, null, null);
    private static final Interface INTF13 =
            new Interface(null, CP13, Lists.newArrayList(INTF_IP2), MacAddress.NONE, null,
                    null, INTF_VLAN_TAGGED, INTF_VLAN_NATIVE);
    private static final Interface INTF21 =
            new Interface(null, CP21, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                    INTF_VLAN_UNTAGGED, null, null);
    private static final Interface INTF22 =
            new Interface(null, CP22, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                    INTF_VLAN_UNTAGGED, null, null);
    private static final Interface INTF31 =
            new Interface(null, CP31, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                    INTF_VLAN_UNTAGGED, null, null);
    private static final Interface INTF32 =
            new Interface(null, CP32, Lists.newArrayList(INTF_IP3), MacAddress.NONE, null,
                    INTF_VLAN_OTHER, null, null);
    private static final Interface INTF39 =
            new Interface(null, CP39, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                    null, INTF_VLAN_PAIR, null);
    private static final Interface INTF41 =
            new Interface(null, CP41, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                    INTF_VLAN_UNTAGGED, null, null);
    private static final Interface INTF49 =
            new Interface(null, CP49, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                    null, INTF_VLAN_PAIR, null);
    // Host
    private static final Host HOST1 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC,
            HOST_VLAN_UNTAGGED, Sets.newHashSet(HOST_LOC11, HOST_LOC21), Sets.newHashSet(HOST_IP11),
            false);

    // A set of hosts
    private static final Set<Host> HOSTS = Sets.newHashSet(HOST1);
    // A set of devices of which we have mastership
    private static final Set<DeviceId> LOCAL_DEVICES = Sets.newHashSet(DEV1, DEV2, DEV3, DEV4);
    // A set of interfaces
    private static final Set<Interface> INTERFACES = Sets.newHashSet(INTF11, INTF12, INTF13, INTF21,
            INTF22, INTF31, INTF32, INTF39, INTF41, INTF49);

    private MockHostProbingService mockLocationProbingService;

    @Before
    public void setUp() throws Exception {
        // Initialize pairDevice and pairLocalPort config
        ObjectMapper mapper = new ObjectMapper();
        ConfigApplyDelegate delegate = config -> { };

        SegmentRoutingDeviceConfig dev3Config = new SegmentRoutingDeviceConfig();
        JsonNode dev3Tree = mapper.createObjectNode();
        dev3Config.init(DEV3, "host-handler-test", dev3Tree, mapper, delegate);
        dev3Config.setPairDeviceId(DEV4).setPairLocalPort(P9);

        SegmentRoutingDeviceConfig dev4Config = new SegmentRoutingDeviceConfig();
        JsonNode dev4Tree = mapper.createObjectNode();
        dev4Config.init(DEV4, "host-handler-test", dev4Tree, mapper, delegate);
        dev4Config.setPairDeviceId(DEV3).setPairLocalPort(P9);

        MockNetworkConfigRegistry mockNetworkConfigRegistry = new MockNetworkConfigRegistry();
        mockNetworkConfigRegistry.applyConfig(dev3Config);
        mockNetworkConfigRegistry.applyConfig(dev4Config);

        // Initialize Segment Routing Manager
        SegmentRoutingManager srManager = new MockSegmentRoutingManager(NEXT_TABLE);
        srManager.storageService = createMock(StorageService.class);
        expect(srManager.storageService.consistentMapBuilder()).andReturn(new TestConsistentMap.Builder<>()).anyTimes();
        replay(srManager.storageService);
        srManager.cfgService = new NetworkConfigRegistryAdapter();
        srManager.deviceConfiguration = new DeviceConfiguration(srManager);
        srManager.flowObjectiveService = new MockFlowObjectiveService(BRIDGING_TABLE, NEXT_TABLE);
        srManager.routingRulePopulator = new MockRoutingRulePopulator(srManager, ROUTING_TABLE);
        srManager.defaultRoutingHandler = new MockDefaultRoutingHandler(srManager, SUBNET_TABLE, ROUTING_TABLE);
        srManager.interfaceService = new MockInterfaceService(INTERFACES);
        srManager.mastershipService = new MockMastershipService(LOCAL_DEVICES);
        srManager.hostService = new MockHostService(HOSTS);
        srManager.cfgService = mockNetworkConfigRegistry;
        mockLocationProbingService = new MockHostProbingService();
        srManager.probingService = mockLocationProbingService;
        srManager.linkHandler = new MockLinkHandler(srManager);

        // Not important for most of the HostHandler test case. Simply return an empty set here
        srManager.routeService = createNiceMock(RouteService.class);
        expect(srManager.routeService.getRouteTables()).andReturn(Sets.newHashSet()).anyTimes();
        replay(srManager.routeService);

        hostHandler = new HostHandler(srManager);

        ROUTING_TABLE.clear();
        BRIDGING_TABLE.clear();
    }

    @Test
    public void init() throws Exception {
        hostHandler.init(DEV1);
        assertEquals(1, ROUTING_TABLE.size());
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertEquals(1, BRIDGING_TABLE.size());
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));

        hostHandler.init(DEV2);
        assertEquals(2, ROUTING_TABLE.size());
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP11.toIpPrefix())));
        assertEquals(2, BRIDGING_TABLE.size());
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHostAddedAtWrongLocation() throws Exception {
        hostHandler.processHostAddedAtLocation(HOST1, HOST_LOC13);
    }


    @Test()
    public void testHostAddedAtCorrectLocation() throws Exception {
        hostHandler.processHostAddedAtLocation(HOST1, HOST_LOC11);
        assertEquals(1, ROUTING_TABLE.size());
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertEquals(1, BRIDGING_TABLE.size());
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
    }

    @Test
    public void testHostAdded() throws Exception {
        Host subject;

        // Untagged host discovered on untagged port
        // Expect: add one routing rule and one bridging rule
        subject = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11), Sets.newHashSet(HOST_IP11), false);
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, subject));
        assertEquals(1, ROUTING_TABLE.size());
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertEquals(1, BRIDGING_TABLE.size());
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Untagged host discovered on tagged/native port
        // Expect: add one routing rule and one bridging rule
        subject = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC13), Sets.newHashSet(HOST_IP21), false);
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, subject));
        assertEquals(2, ROUTING_TABLE.size());
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP21.toIpPrefix())));
        assertEquals(2, BRIDGING_TABLE.size());
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_NATIVE)));

        // Tagged host discovered on untagged port
        // Expect: ignore the host. No rule is added.
        subject = new DefaultHost(PROVIDER_ID, HOST_ID_TAGGED, HOST_MAC, HOST_VLAN_TAGGED,
                Sets.newHashSet(HOST_LOC11), Sets.newHashSet(HOST_IP11), false);
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, subject));
        assertEquals(2, ROUTING_TABLE.size());
        assertEquals(2, BRIDGING_TABLE.size());

        // Tagged host discovered on tagged port with the same IP
        // Expect: update existing route, add one bridging rule
        subject = new DefaultHost(PROVIDER_ID, HOST_ID_TAGGED, HOST_MAC, HOST_VLAN_TAGGED,
                Sets.newHashSet(HOST_LOC13), Sets.newHashSet(HOST_IP21), false);
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, subject));
        assertEquals(2, ROUTING_TABLE.size());
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP21.toIpPrefix())));
        assertEquals(HOST_VLAN_TAGGED, ROUTING_TABLE.get(new MockRoutingTableKey(HOST_LOC13.deviceId(),
                HOST_IP21.toIpPrefix())).vlanId);
        assertEquals(3, BRIDGING_TABLE.size());
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, HOST_VLAN_TAGGED)));
    }

    @Test
    public void testDualHomedHostAdded() throws Exception {
        // Add a dual-homed host that has 2 locations
        // Expect: add two routing rules and two bridging rules
        Host subject = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11, HOST_LOC21), Sets.newHashSet(HOST_IP11), false);
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, subject));
        assertEquals(2, ROUTING_TABLE.size());
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP11.toIpPrefix())));
        assertEquals(2, BRIDGING_TABLE.size());
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)));
    }

    @Test
    public void testSingleHomedHostAddedOnPairLeaf() throws Exception {
        Host host1 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC32), Sets.newHashSet(HOST_IP32), false);

        // Add a single-homed host with one location
        // Expect: the pair link should not be utilized
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, host1));
        assertEquals(1, ROUTING_TABLE.size());
        assertEquals(P2, ROUTING_TABLE.get(new MockRoutingTableKey(DEV3, HOST_IP32.toIpPrefix())).portNumber);
        assertEquals(1, BRIDGING_TABLE.size());
        assertEquals(P2, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV3, HOST_MAC, INTF_VLAN_OTHER)).portNumber);
    }

    @Test
    public void testDualHomedHostAddedOneByOne() throws Exception {
        Host host1 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC31), Sets.newHashSet(HOST_IP11), false);
        Host host2 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC31, HOST_LOC41), Sets.newHashSet(HOST_IP11), false);

        // Add a dual-homed host with one location
        // Expect: the pair link is utilized temporarily before the second location is discovered
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, host1));
        assertEquals(2, ROUTING_TABLE.size());
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV3, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P9, ROUTING_TABLE.get(new MockRoutingTableKey(DEV4, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(2, BRIDGING_TABLE.size());
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV3, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P9, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV4, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        // Expect probe to be sent out on pair device
        assertTrue(mockLocationProbingService.verifyProbe(host1, CP41, ProbeMode.DISCOVER));

        // Add the second location of dual-homed host
        // Expect: no longer use the pair link
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host2, host1));
        assertEquals(2, ROUTING_TABLE.size());
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV3, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV4, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(2, BRIDGING_TABLE.size());
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV3, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV4, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
    }

    @Test
    public void testHostRemoved() throws Exception {
        Host subject = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11), Sets.newHashSet(HOST_IP11), false);

        // Add a host
        // Expect: add one routing rule and one bridging rule
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, subject));
        assertEquals(1, ROUTING_TABLE.size());
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertEquals(1, BRIDGING_TABLE.size());
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Remove the host
        // Expect: add the routing rule and the bridging rule
        hostHandler.processHostRemovedEvent(new HostEvent(HostEvent.Type.HOST_REMOVED, subject));
        assertEquals(0, ROUTING_TABLE.size());
        assertEquals(0, BRIDGING_TABLE.size());
    }

    @Test
    public void testDualHomedHostRemoved() throws Exception {
        // Add a dual-homed host that has 2 locations
        // Expect: add two routing rules and two bridging rules
        Host subject = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11, HOST_LOC21), Sets.newHashSet(HOST_IP11), false);
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, subject));
        assertEquals(2, ROUTING_TABLE.size());
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP11.toIpPrefix())));
        assertEquals(2, BRIDGING_TABLE.size());
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Remove a dual-homed host that has 2 locations
        // Expect: all routing and bridging rules are removed
        hostHandler.processHostRemovedEvent(new HostEvent(HostEvent.Type.HOST_REMOVED, subject));
        assertEquals(0, ROUTING_TABLE.size());
        assertEquals(0, BRIDGING_TABLE.size());
    }

    @Test
    public void testHostMoved() throws Exception {
        Host host1 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11), Sets.newHashSet(HOST_IP11), false);
        Host host2 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC21), Sets.newHashSet(HOST_IP11), false);
        Host host3 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC13), Sets.newHashSet(HOST_IP11), false);

        // Add a host
        // Expect: add one new routing rule, one new bridging rule
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, host1));
        assertEquals(1, ROUTING_TABLE.size());
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP21.toIpPrefix())));
        assertNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP13.toIpPrefix())));
        assertEquals(1, BRIDGING_TABLE.size());
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
        assertNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Move the host to CP13, which has different subnet setting
        // Expect: remove routing rule. Change vlan in bridging rule.
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host3, host1));
        assertEquals(0, ROUTING_TABLE.size());
        assertEquals(1, BRIDGING_TABLE.size());
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_NATIVE)));
        assertNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Move the host to CP21, which has same subnet setting
        // Expect: add a new routing rule. Change vlan in bridging rule.
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host2, host3));
        assertEquals(1, ROUTING_TABLE.size());
        assertNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP11.toIpPrefix())));
        assertEquals(1, BRIDGING_TABLE.size());
        assertNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)));
    }

    @Test
    public void testDualHomedHostMoved() throws Exception {
        Host host1 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11, HOST_LOC21), Sets.newHashSet(HOST_IP11, HOST_IP12), false);
        Host host2 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC12, HOST_LOC22), Sets.newHashSet(HOST_IP11, HOST_IP12), false);
        Host host3 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11, HOST_LOC21), Sets.newHashSet(HOST_IP13, HOST_IP14), false);
        Host host4 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11, HOST_LOC22), Sets.newHashSet(HOST_IP12, HOST_IP13), false);
        Host host5 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC12, HOST_LOC21), Sets.newHashSet(HOST_IP11, HOST_IP12), false);


        // Add a host with IP11, IP12 and LOC11, LOC21
        // Expect: 4 routing rules and 2 bridging rules
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, host1));
        assertEquals(4, ROUTING_TABLE.size());
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(2, BRIDGING_TABLE.size());
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Move the host to LOC12, LOC21 and keep the IP
        // Expect: 4 routing rules and 2 bridging rules all at the new location
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host5, host1));
        assertEquals(4, ROUTING_TABLE.size());
        assertEquals(P2, ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P2, ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(2, BRIDGING_TABLE.size());
        assertEquals(P2, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Move the host to LOC12, LOC22 and keep the IP
        // Expect: 4 routing rules and 2 bridging rules all at the new location
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host2, host5));
        assertEquals(4, ROUTING_TABLE.size());
        assertEquals(P2, ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P2, ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(P2, ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P2, ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(2, BRIDGING_TABLE.size());
        assertEquals(P2, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P2, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Move the host to LOC11, LOC21 and change the IP to IP13, IP14 at the same time
        // Expect: 4 routing rules and 2 bridging rules all at the new location
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host3, host2));
        assertEquals(4, ROUTING_TABLE.size());
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP13.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP14.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP13.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP14.toIpPrefix())).portNumber);
        assertEquals(2, BRIDGING_TABLE.size());
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Move the host to LOC11, LOC22 and change the IP to IP12, IP13 at the same time
        // Expect: 4 routing rules and 2 bridging rules all at the new location
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host4, host3));
        assertEquals(4, ROUTING_TABLE.size());
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP13.toIpPrefix())).portNumber);
        assertEquals(P2, ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(P2, ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP13.toIpPrefix())).portNumber);
        assertEquals(2, BRIDGING_TABLE.size());
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P2, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
    }

    @Test
    public void testHostMoveToInvalidLocation() throws Exception {
        Host host1 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11), Sets.newHashSet(HOST_IP11), false);
        Host host2 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC51), Sets.newHashSet(HOST_IP11), false);

        // Add a host
        // Expect: add one new routing rule, one new bridging rule
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, host1));
        assertEquals(1, ROUTING_TABLE.size());
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP21.toIpPrefix())));
        assertEquals(1, BRIDGING_TABLE.size());
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
        assertNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Move the host to an invalid location
        // Expect: Old flow is removed. New flow is not created
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host2, host1));
        assertEquals(0, ROUTING_TABLE.size());
        assertEquals(0, BRIDGING_TABLE.size());

        // Move the host to a valid location
        // Expect: add one new routing rule, one new bridging rule
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host1, host2));
        assertEquals(1, ROUTING_TABLE.size());
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP21.toIpPrefix())));
        assertEquals(1, BRIDGING_TABLE.size());
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
        assertNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)));
    }

    @Test
    public void testDualHomedHostMoveToInvalidLocation() throws Exception {
        Host host1 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11, HOST_LOC21), Sets.newHashSet(HOST_IP11), false);
        Host host2 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11, HOST_LOC51), Sets.newHashSet(HOST_IP11), false);
        Host host3 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC61, HOST_LOC51), Sets.newHashSet(HOST_IP11), false);

        // Add a host
        // Expect: add two new routing rules, two new bridging rules
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, host1));
        assertEquals(2, ROUTING_TABLE.size());
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(2, BRIDGING_TABLE.size());
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Move first host location to an invalid location
        // Expect: One routing and one bridging flow
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host2, host1));
        assertEquals(1, ROUTING_TABLE.size());
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(1, BRIDGING_TABLE.size());
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Move second host location to an invalid location
        // Expect: No routing or bridging rule
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host3, host2));
        assertEquals(0, ROUTING_TABLE.size());
        assertEquals(0, BRIDGING_TABLE.size());

        // Move second host location back to a valid location
        // Expect: One routing and one bridging flow
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host2, host3));
        assertEquals(1, ROUTING_TABLE.size());
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(1, BRIDGING_TABLE.size());
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Move first host location back to a valid location
        // Expect: Two routing and two bridging flow
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host1, host2));
        assertEquals(2, ROUTING_TABLE.size());
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(2, BRIDGING_TABLE.size());
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
    }

    @Test
    public void testDualHomingSingleLocationFail() throws Exception {
        Host host1 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC31, HOST_LOC41), Sets.newHashSet(HOST_IP11, HOST_IP12), false);
        Host host2 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC31), Sets.newHashSet(HOST_IP11, HOST_IP12), false);

        // Add a host
        // Expect: add four new routing rules, two new bridging rules
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, host1));
        assertEquals(4, ROUTING_TABLE.size());
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV3, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV3, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV4, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV4, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(2, BRIDGING_TABLE.size());
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV3, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV4, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Host becomes single-homed
        // Expect: redirect flows from host location to pair link
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host2, host1));
        assertEquals(4, ROUTING_TABLE.size());
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV3, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV3, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(P9, ROUTING_TABLE.get(new MockRoutingTableKey(DEV4, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P9, ROUTING_TABLE.get(new MockRoutingTableKey(DEV4, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(2, BRIDGING_TABLE.size());
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV3, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P9, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV4, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Host becomes dual-homed again
        // Expect: Redirect flows from pair link back to host location
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host1, host2));
        assertEquals(4, ROUTING_TABLE.size());
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV3, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV3, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV4, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV4, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(2, BRIDGING_TABLE.size());
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV3, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV4, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
    }

    @Test
    public void testDualHomingBothLocationFail() throws Exception {
        Host host1 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC31, HOST_LOC41), Sets.newHashSet(HOST_IP11, HOST_IP12), false);
        Host host2 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC31), Sets.newHashSet(HOST_IP11, HOST_IP12), false);

        // Add a host
        // Expect: add four new routing rules, two new bridging rules
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, host1));
        assertEquals(4, ROUTING_TABLE.size());
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV3, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV3, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV4, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV4, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(2, BRIDGING_TABLE.size());
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV3, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV4, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Host becomes single-homed
        // Expect: redirect flows from host location to pair link
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host2, host1));
        assertEquals(4, ROUTING_TABLE.size());
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV3, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV3, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(P9, ROUTING_TABLE.get(new MockRoutingTableKey(DEV4, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P9, ROUTING_TABLE.get(new MockRoutingTableKey(DEV4, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(2, BRIDGING_TABLE.size());
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV3, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P9, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV4, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Host loses both locations
        // Expect: Remove last location and all previous redirection flows
        hostHandler.processHostRemovedEvent(new HostEvent(HostEvent.Type.HOST_REMOVED, host2));
        assertEquals(0, ROUTING_TABLE.size());
        assertEquals(0, BRIDGING_TABLE.size());
    }

    @Test
    public void testHostUpdated() throws Exception {
        Host host1 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11), Sets.newHashSet(HOST_IP11), false);
        Host host2 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11), Sets.newHashSet(HOST_IP21), false);
        Host host3 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11), Sets.newHashSet(HOST_IP12), false);

        // Add a host
        // Expect: add one new routing rule. Add one new bridging rule.
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, host1));
        assertEquals(1, ROUTING_TABLE.size());
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP21.toIpPrefix())));
        assertNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP12.toIpPrefix())));
        assertEquals(1, BRIDGING_TABLE.size());
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(HOST_LOC11.deviceId(), HOST_MAC,
                INTF_VLAN_UNTAGGED)));

        // Update the host IP to same subnet
        // Expect: update routing rule with new IP. No change to bridging rule.
        hostHandler.processHostUpdatedEvent(new HostEvent(HostEvent.Type.HOST_UPDATED, host3, host1));
        assertEquals(1, ROUTING_TABLE.size());
        assertNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP21.toIpPrefix())));
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP12.toIpPrefix())));
        assertEquals(1, BRIDGING_TABLE.size());
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Update the host IP to different subnet
        // Expect: Remove routing rule. No change to bridging rule.
        hostHandler.processHostUpdatedEvent(new HostEvent(HostEvent.Type.HOST_UPDATED, host2, host3));
        assertEquals(0, ROUTING_TABLE.size());
        assertEquals(1, BRIDGING_TABLE.size());
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
    }

    @Test
    public void testDelayedIpAndLocation() throws Exception {
        Host host1 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC31), Sets.newHashSet(), false);
        Host host2 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC31), Sets.newHashSet(HOST_IP11), false);
        Host host3 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC31, HOST_LOC41), Sets.newHashSet(HOST_IP11), false);

        // Add a dual-home host with only one location and no IP
        // Expect: only bridging redirection works
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, host1));
        assertEquals(0, ROUTING_TABLE.size());
        assertEquals(2, BRIDGING_TABLE.size());
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV3, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P9, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV4, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Discover IP
        // Expect: routing redirection should also work
        hostHandler.processHostUpdatedEvent(new HostEvent(HostEvent.Type.HOST_UPDATED, host2, host1));
        assertEquals(2, ROUTING_TABLE.size());
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV3, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P9, ROUTING_TABLE.get(new MockRoutingTableKey(DEV4, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(2, BRIDGING_TABLE.size());
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV3, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P9, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV4, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        // Expect probe to be sent out on pair device
        assertTrue(mockLocationProbingService.verifyProbe(host2, CP41, ProbeMode.DISCOVER));

        // Discover location
        // Expect: cancel all redirections
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host3, host2));
        assertEquals(2, ROUTING_TABLE.size());
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV3, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV4, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(2, BRIDGING_TABLE.size());
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV3, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV4, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV4, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV4, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
    }

    @Test
    public void testDelayedIpAndLocation2() throws Exception {
        Host host1 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC31), Sets.newHashSet(), false);
        Host host2 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC31, HOST_LOC41), Sets.newHashSet(), false);
        Host host3 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC31, HOST_LOC41), Sets.newHashSet(HOST_IP11), false);

        // Add a dual-home host with only one location and no IP
        // Expect: only bridging redirection works
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, host1));
        assertEquals(0, ROUTING_TABLE.size());
        assertEquals(2, BRIDGING_TABLE.size());
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV3, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P9, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV4, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Discover Location
        // Expect: cancel bridging redirections
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host2, host1));
        assertEquals(0, ROUTING_TABLE.size());
        assertEquals(2, BRIDGING_TABLE.size());
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV3, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV4, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Discover IP
        // Expect: add IP rules to both location
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_UPDATED, host3, host2));
        assertEquals(2, ROUTING_TABLE.size());
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV3, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, ROUTING_TABLE.get(new MockRoutingTableKey(DEV4, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(2, BRIDGING_TABLE.size());
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV3, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P1, BRIDGING_TABLE.get(new MockBridgingTableKey(DEV4, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
    }

    @Test
    public void testDualHomedHostUpdated() throws Exception {
        Host host1 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11, HOST_LOC21), Sets.newHashSet(HOST_IP11, HOST_IP12), false);
        Host host2 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11, HOST_LOC21), Sets.newHashSet(HOST_IP11, HOST_IP21), false);
        Host host3 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11, HOST_LOC21), Sets.newHashSet(HOST_IP13, HOST_IP14), false);

        // Add a dual-homed host with two locations and two IPs
        // Expect: add four new routing rules. Add two new bridging rules
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, host1));
        assertEquals(4, ROUTING_TABLE.size());
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP12.toIpPrefix())));
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP11.toIpPrefix())));
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP12.toIpPrefix())));
        assertEquals(2, BRIDGING_TABLE.size());
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Update both host IPs
        // Expect: update routing rules with new IP. No change to bridging rule.
        hostHandler.processHostUpdatedEvent(new HostEvent(HostEvent.Type.HOST_UPDATED, host3, host1));
        assertEquals(4, ROUTING_TABLE.size());
        assertNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP12.toIpPrefix())));
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP13.toIpPrefix())));
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP14.toIpPrefix())));
        assertNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP11.toIpPrefix())));
        assertNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP12.toIpPrefix())));
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP13.toIpPrefix())));
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP14.toIpPrefix())));
        assertEquals(2, BRIDGING_TABLE.size());
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Update one of the host IP to different subnet
        // Expect: update routing rule with new IP. No change to bridging rule.
        hostHandler.processHostUpdatedEvent(new HostEvent(HostEvent.Type.HOST_UPDATED, host2, host3));
        assertEquals(2, ROUTING_TABLE.size());
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP21.toIpPrefix())));
        assertNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP12.toIpPrefix())));
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP11.toIpPrefix())));
        assertNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP21.toIpPrefix())));
        assertNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV2, HOST_IP12.toIpPrefix())));
        assertEquals(2, BRIDGING_TABLE.size());
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)));
    }

    @Test
    public void testVlanForPairPort() {
        assertEquals(INTF_VLAN_UNTAGGED, hostHandler.vlanForPairPort(VlanId.NONE, CP11));
        assertEquals(INTF_VLAN_NATIVE, hostHandler.vlanForPairPort(VlanId.NONE, CP13));
        assertEquals(INTF_VLAN_TAGGED_1, hostHandler.vlanForPairPort(INTF_VLAN_TAGGED_1, CP13));
        assertNull(hostHandler.vlanForPairPort(INTF_VLAN_UNTAGGED, CP11));
        assertNull(hostHandler.vlanForPairPort(INTF_VLAN_UNTAGGED, CP13));
        assertNull(hostHandler.vlanForPairPort(VlanId.NONE, CP51));
        assertNull(hostHandler.vlanForPairPort(INTF_VLAN_UNTAGGED, CP51));
    }

    @Test
    public void testHostRemovedWithRouteRemoved() throws Exception {
        Host subject = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11), Sets.newHashSet(HOST_IP11), false);

        // Add a host
        // Expect: add one routing rule and one bridging rule
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, subject));
        assertEquals(1, ROUTING_TABLE.size());
        assertNotNull(ROUTING_TABLE.get(new MockRoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertEquals(1, BRIDGING_TABLE.size());
        assertNotNull(BRIDGING_TABLE.get(new MockBridgingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));

        IpPrefix prefix = IpPrefix.valueOf("55.55.55.0/24");

        // Setting up mock route service
        RouteService routeService = hostHandler.srManager.routeService;
        reset(routeService);

        IpAddress nextHopIp2 = IpAddress.valueOf("20.0.0.1");
        MacAddress nextHopMac2 = MacAddress.valueOf("00:22:33:44:55:66");
        VlanId nextHopVlan2 = VlanId.NONE;

        Route r1 = new Route(Route.Source.STATIC, prefix, HOST_IP11);
        ResolvedRoute rr1 = new ResolvedRoute(r1, HOST_MAC, VlanId.NONE);
        Route r2 = new Route(Route.Source.STATIC, prefix, nextHopIp2);
        ResolvedRoute rr2 = new ResolvedRoute(r2, nextHopMac2, nextHopVlan2);
        RouteInfo routeInfo = new RouteInfo(prefix, rr1, Sets.newHashSet(rr1, rr2));
        RouteTableId routeTableId = new RouteTableId("ipv4");

        expect(routeService.getRouteTables()).andReturn(Sets.newHashSet(routeTableId));
        expect(routeService.getRoutes(routeTableId)).andReturn(Sets.newHashSet(routeInfo));
        replay(routeService);

        // Setting up mock device configuration
        hostHandler.srManager.deviceConfiguration = EasyMock.createNiceMock(DeviceConfiguration.class);
        DeviceConfiguration deviceConfiguration = hostHandler.srManager.deviceConfiguration;
        expect(deviceConfiguration.inSameSubnet(CP11, HOST_IP11)).andReturn(true);
        deviceConfiguration.removeSubnet(CP11, prefix);
        expectLastCall();
        replay(deviceConfiguration);

        // Remove the host
        // Expect: add the routing rule and the bridging rule
        hostHandler.processHostRemovedEvent(new HostEvent(HostEvent.Type.HOST_REMOVED, subject));
        assertEquals(0, ROUTING_TABLE.size());
        assertEquals(0, BRIDGING_TABLE.size());

        // Expect: subnet is removed from device config
        verify(deviceConfiguration);
    }
}
