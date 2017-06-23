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
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceServiceAdapter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flowobjective.FlowObjectiveServiceAdapter;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.onosproject.segmentrouting.config.SegmentRoutingDeviceConfig;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Unit test for {@link HostHandler}.
 */
public class HostHandlerTest {
    private SegmentRoutingManager srManager;
    private HostHandler hostHandler;

    // Mocked routing and bridging tables
    private Map<BridingTableKey, BridingTableValue> bridgingTable = Maps.newConcurrentMap();
    private Map<RoutingTableKey, RoutingTableValue> routingTable = Maps.newConcurrentMap();
    // Mocked Next Id
    private Map<Integer, TrafficTreatment> nextTable = Maps.newConcurrentMap();
    private AtomicInteger atomicNextId = new AtomicInteger();

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
    // Device
    private static final DeviceId DEV1 = DeviceId.deviceId("of:0000000000000001");
    private static final DeviceId DEV2 = DeviceId.deviceId("of:0000000000000002");
    private static final DeviceId DEV3 = DeviceId.deviceId("of:000000000000003");
    private static final DeviceId DEV4 = DeviceId.deviceId("of:000000000000004");
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
    private static final ConnectPoint CP41 = new ConnectPoint(DEV4, P1);
    private static final HostLocation HOST_LOC41 = new HostLocation(CP41, 0);
    private static final ConnectPoint CP39 = new ConnectPoint(DEV3, P9);
    private static final ConnectPoint CP49 = new ConnectPoint(DEV4, P9);
    // Interface VLAN
    private static final VlanId INTF_VLAN_UNTAGGED = VlanId.vlanId((short) 10);
    private static final Set<VlanId> INTF_VLAN_TAGGED = Sets.newHashSet(VlanId.vlanId((short) 20));
    private static final VlanId INTF_VLAN_NATIVE = VlanId.vlanId((short) 30);
    private static final Set<VlanId> INTF_VLAN_PAIR = Sets.newHashSet(VlanId.vlanId((short) 10),
            VlanId.vlanId((short) 20), VlanId.vlanId((short) 30));
    // Interface subnet
    private static final IpPrefix INTF_PREFIX1 = IpPrefix.valueOf("10.0.1.254/24");
    private static final IpPrefix INTF_PREFIX2 = IpPrefix.valueOf("10.0.2.254/24");
    private static final InterfaceIpAddress INTF_IP1 =
            new InterfaceIpAddress(INTF_PREFIX1.address(), INTF_PREFIX1);
    private static final InterfaceIpAddress INTF_IP2 =
            new InterfaceIpAddress(INTF_PREFIX2.address(), INTF_PREFIX2);
    // Host
    private static final Host HOST1 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC,
            HOST_VLAN_UNTAGGED, Sets.newHashSet(HOST_LOC11, HOST_LOC21), Sets.newHashSet(HOST_IP11),
            false);

    @Before
    public void setUp() throws Exception {
        srManager = new MockSegmentRoutingManager();
        srManager.cfgService = new NetworkConfigRegistryAdapter();
        srManager.deviceConfiguration = new DeviceConfiguration(srManager);
        srManager.flowObjectiveService = new MockFlowObjectiveService();
        srManager.routingRulePopulator = new MockRoutingRulePopulator();
        srManager.interfaceService = new MockInterfaceService();
        srManager.hostService = new MockHostService();
        srManager.cfgService = new MockNetworkConfigRegistry();

        hostHandler = new HostHandler(srManager);

        routingTable.clear();
        bridgingTable.clear();
    }

    @Test
    public void init() throws Exception {
        hostHandler.init(DEV1);
        assertEquals(1, routingTable.size());
        assertNotNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertEquals(1, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));

        hostHandler.init(DEV2);
        assertEquals(2, routingTable.size());
        assertNotNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNotNull(routingTable.get(new RoutingTableKey(DEV2, HOST_IP11.toIpPrefix())));
        assertEquals(2, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHostAddedAtWrongLocation() throws Exception {
        hostHandler.processHostAddedAtLocation(HOST1, HOST_LOC13);
    }


    @Test()
    public void testHostAddedAtCorrectLocation() throws Exception {
        hostHandler.processHostAddedAtLocation(HOST1, HOST_LOC11);
        assertEquals(1, routingTable.size());
        assertNotNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertEquals(1, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
    }

    @Test
    public void testHostAdded() throws Exception {
        Host subject;

        // Untagged host discovered on untagged port
        // Expect: add one routing rule and one bridging rule
        subject = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11), Sets.newHashSet(HOST_IP11), false);
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, subject));
        assertEquals(1, routingTable.size());
        assertNotNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertEquals(1, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Untagged host discovered on tagged/native port
        // Expect: add one routing rule and one bridging rule
        subject = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC13), Sets.newHashSet(HOST_IP21), false);
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, subject));
        assertEquals(2, routingTable.size());
        assertNotNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP21.toIpPrefix())));
        assertEquals(2, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_NATIVE)));

        // Tagged host discovered on untagged port
        // Expect: ignore the host. No rule is added.
        subject = new DefaultHost(PROVIDER_ID, HOST_ID_TAGGED, HOST_MAC, HOST_VLAN_TAGGED,
                Sets.newHashSet(HOST_LOC11), Sets.newHashSet(HOST_IP11), false);
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, subject));
        assertEquals(2, routingTable.size());
        assertEquals(2, bridgingTable.size());

        // Tagged host discovered on tagged port with the same IP
        // Expect: update existing route, add one bridging rule
        subject = new DefaultHost(PROVIDER_ID, HOST_ID_TAGGED, HOST_MAC, HOST_VLAN_TAGGED,
                Sets.newHashSet(HOST_LOC13), Sets.newHashSet(HOST_IP21), false);
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, subject));
        assertEquals(2, routingTable.size());
        assertNotNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP21.toIpPrefix())));
        assertEquals(HOST_VLAN_TAGGED, routingTable.get(new RoutingTableKey(HOST_LOC13.deviceId(),
                HOST_IP21.toIpPrefix())).vlanId);
        assertEquals(3, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, HOST_VLAN_TAGGED)));
    }

    @Test
    public void testDualHomedHostAdded() throws Exception {
        // Add a dual-homed host that has 2 locations
        // Expect: add two routing rules and two bridging rules
        Host subject = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11, HOST_LOC21), Sets.newHashSet(HOST_IP11), false);
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, subject));
        assertEquals(2, routingTable.size());
        assertNotNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNotNull(routingTable.get(new RoutingTableKey(DEV2, HOST_IP11.toIpPrefix())));
        assertEquals(2, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)));
    }

    @Test
    public void testHostRemoved() throws Exception {
        Host subject = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11), Sets.newHashSet(HOST_IP11), false);

        // Add a host
        // Expect: add one routing rule and one bridging rule
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, subject));
        assertEquals(1, routingTable.size());
        assertNotNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertEquals(1, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Remove the host
        // Expect: add the routing rule and the bridging rule
        hostHandler.processHostRemovedEvent(new HostEvent(HostEvent.Type.HOST_REMOVED, subject));
        assertEquals(0, routingTable.size());
        assertEquals(0, bridgingTable.size());
    }

    @Test
    public void testDualHomedHostRemoved() throws Exception {
        // Add a dual-homed host that has 2 locations
        // Expect: add two routing rules and two bridging rules
        Host subject = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC11, HOST_LOC21), Sets.newHashSet(HOST_IP11), false);
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, subject));
        assertEquals(2, routingTable.size());
        assertNotNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNotNull(routingTable.get(new RoutingTableKey(DEV2, HOST_IP11.toIpPrefix())));
        assertEquals(2, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Remove a dual-homed host that has 2 locations
        // Expect: all routing and bridging rules are removed
        hostHandler.processHostRemovedEvent(new HostEvent(HostEvent.Type.HOST_REMOVED, subject));
        assertEquals(0, routingTable.size());
        assertEquals(0, bridgingTable.size());
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
        assertEquals(1, routingTable.size());
        assertNotNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP21.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP13.toIpPrefix())));
        assertEquals(1, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
        assertNull(bridgingTable.get(new BridingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Move the host to CP13, which has different subnet setting
        // Expect: remove routing rule. Change vlan in bridging rule.
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host3, host1));
        assertEquals(0, routingTable.size());
        assertEquals(1, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_NATIVE)));
        assertNull(bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Move the host to CP21, which has same subnet setting
        // Expect: add a new routing rule. Change vlan in bridging rule.
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host2, host3));
        assertEquals(1, routingTable.size());
        assertNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNotNull(routingTable.get(new RoutingTableKey(DEV2, HOST_IP11.toIpPrefix())));
        assertEquals(1, bridgingTable.size());
        assertNull(bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)));
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

        // Add a host with IP11, IP12 and LOC11, LOC21
        // Expect: 4 routing rules and 2 bridging rules
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, host1));
        assertEquals(4, routingTable.size());
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV1, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV1, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV2, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV2, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(2, bridgingTable.size());
        assertEquals(P1, bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P1, bridgingTable.get(new BridingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Move the host to LOC12, LOC22 and keep the IP
        // Expect: 4 routing rules and 2 bridging rules all at the new location
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host2, host1));
        assertEquals(4, routingTable.size());
        assertEquals(P2, routingTable.get(new RoutingTableKey(DEV1, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P2, routingTable.get(new RoutingTableKey(DEV1, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(P2, routingTable.get(new RoutingTableKey(DEV2, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P2, routingTable.get(new RoutingTableKey(DEV2, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(2, bridgingTable.size());
        assertEquals(P2, bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P2, bridgingTable.get(new BridingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Move the host to LOC11, LOC21 and change the IP to IP13, IP14 at the same time
        // Expect: 4 routing rules and 2 bridging rules all at the new location
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host3, host2));
        assertEquals(4, routingTable.size());
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV1, HOST_IP13.toIpPrefix())).portNumber);
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV1, HOST_IP14.toIpPrefix())).portNumber);
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV2, HOST_IP13.toIpPrefix())).portNumber);
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV2, HOST_IP14.toIpPrefix())).portNumber);
        assertEquals(2, bridgingTable.size());
        assertEquals(P1, bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P1, bridgingTable.get(new BridingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Move the host to LOC11, LOC22 and change the IP to IP12, IP13 at the same time
        // Expect: 4 routing rules and 2 bridging rules all at the new location
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host4, host3));
        assertEquals(4, routingTable.size());
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV1, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV1, HOST_IP13.toIpPrefix())).portNumber);
        assertEquals(P2, routingTable.get(new RoutingTableKey(DEV2, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(P2, routingTable.get(new RoutingTableKey(DEV2, HOST_IP13.toIpPrefix())).portNumber);
        assertEquals(2, bridgingTable.size());
        assertEquals(P1, bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P2, bridgingTable.get(new BridingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
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
        assertEquals(4, routingTable.size());
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV3, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV3, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV4, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV4, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(2, bridgingTable.size());
        assertEquals(P1, bridgingTable.get(new BridingTableKey(DEV3, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P1, bridgingTable.get(new BridingTableKey(DEV4, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Host becomes single-homed
        // Expect: redirect flows from host location to pair link
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host2, host1));
        assertEquals(4, routingTable.size());
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV3, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV3, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(P9, routingTable.get(new RoutingTableKey(DEV4, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P9, routingTable.get(new RoutingTableKey(DEV4, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(2, bridgingTable.size());
        assertEquals(P1, bridgingTable.get(new BridingTableKey(DEV3, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P9, bridgingTable.get(new BridingTableKey(DEV4, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Host becomes dual-homed again
        // Expect: Redirect flows from pair link back to host location
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host1, host2));
        assertEquals(4, routingTable.size());
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV3, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV3, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV4, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV4, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(2, bridgingTable.size());
        assertEquals(P1, bridgingTable.get(new BridingTableKey(DEV3, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P1, bridgingTable.get(new BridingTableKey(DEV4, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
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
        assertEquals(4, routingTable.size());
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV3, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV3, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV4, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV4, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(2, bridgingTable.size());
        assertEquals(P1, bridgingTable.get(new BridingTableKey(DEV3, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P1, bridgingTable.get(new BridingTableKey(DEV4, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Host becomes single-homed
        // Expect: redirect flows from host location to pair link
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host2, host1));
        assertEquals(4, routingTable.size());
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV3, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P1, routingTable.get(new RoutingTableKey(DEV3, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(P9, routingTable.get(new RoutingTableKey(DEV4, HOST_IP11.toIpPrefix())).portNumber);
        assertEquals(P9, routingTable.get(new RoutingTableKey(DEV4, HOST_IP12.toIpPrefix())).portNumber);
        assertEquals(2, bridgingTable.size());
        assertEquals(P1, bridgingTable.get(new BridingTableKey(DEV3, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);
        assertEquals(P9, bridgingTable.get(new BridingTableKey(DEV4, HOST_MAC, INTF_VLAN_UNTAGGED)).portNumber);

        // Host loses both locations
        // Expect: Remove last location and all previous redirection flows
        hostHandler.processHostRemovedEvent(new HostEvent(HostEvent.Type.HOST_REMOVED, host2));
        assertEquals(0, routingTable.size());
        assertEquals(0, bridgingTable.size());
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
        assertEquals(1, routingTable.size());
        assertNotNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP21.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP12.toIpPrefix())));
        assertEquals(1, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(HOST_LOC11.deviceId(), HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Update the host IP to same subnet
        // Expect: update routing rule with new IP. No change to bridging rule.
        hostHandler.processHostUpdatedEvent(new HostEvent(HostEvent.Type.HOST_UPDATED, host3, host1));
        assertEquals(1, routingTable.size());
        assertNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP21.toIpPrefix())));
        assertNotNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP12.toIpPrefix())));
        assertEquals(1, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Update the host IP to different subnet
        // Expect: Remove routing rule. No change to bridging rule.
        hostHandler.processHostUpdatedEvent(new HostEvent(HostEvent.Type.HOST_UPDATED, host2, host3));
        assertEquals(0, routingTable.size());
        assertEquals(1, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
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
        assertEquals(4, routingTable.size());
        assertNotNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNotNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP12.toIpPrefix())));
        assertNotNull(routingTable.get(new RoutingTableKey(DEV2, HOST_IP11.toIpPrefix())));
        assertNotNull(routingTable.get(new RoutingTableKey(DEV2, HOST_IP12.toIpPrefix())));
        assertEquals(2, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Update both host IPs
        // Expect: update routing rules with new IP. No change to bridging rule.
        hostHandler.processHostUpdatedEvent(new HostEvent(HostEvent.Type.HOST_UPDATED, host3, host1));
        assertEquals(4, routingTable.size());
        assertNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP12.toIpPrefix())));
        assertNotNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP13.toIpPrefix())));
        assertNotNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP14.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(DEV2, HOST_IP11.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(DEV2, HOST_IP12.toIpPrefix())));
        assertNotNull(routingTable.get(new RoutingTableKey(DEV2, HOST_IP13.toIpPrefix())));
        assertNotNull(routingTable.get(new RoutingTableKey(DEV2, HOST_IP14.toIpPrefix())));
        assertEquals(2, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Update one of the host IP to different subnet
        // Expect: update routing rule with new IP. No change to bridging rule.
        hostHandler.processHostUpdatedEvent(new HostEvent(HostEvent.Type.HOST_UPDATED, host2, host3));
        assertEquals(2, routingTable.size());
        assertNotNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP11.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP21.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(DEV1, HOST_IP12.toIpPrefix())));
        assertNotNull(routingTable.get(new RoutingTableKey(DEV2, HOST_IP11.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(DEV2, HOST_IP21.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(DEV2, HOST_IP12.toIpPrefix())));
        assertEquals(2, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV1, HOST_MAC, INTF_VLAN_UNTAGGED)));
        assertNotNull(bridgingTable.get(new BridingTableKey(DEV2, HOST_MAC, INTF_VLAN_UNTAGGED)));
    }

    @Test
    public void testBridgingFwdObjBuilder() throws Exception {
        assertNotNull(hostHandler.bridgingFwdObjBuilder(DEV2, HOST_MAC, HOST_VLAN_UNTAGGED, P1, false));
        assertNull(hostHandler.bridgingFwdObjBuilder(DEV2, HOST_MAC, HOST_VLAN_UNTAGGED, P3, false));
    }

    class MockSegmentRoutingManager extends SegmentRoutingManager {
        MockSegmentRoutingManager() {
            appId = new DefaultApplicationId(1, SegmentRoutingManager.APP_NAME);
        }

        @Override
        public int getPortNextObjectiveId(DeviceId deviceId, PortNumber portNum,
                                          TrafficTreatment treatment,
                                          TrafficSelector meta,
                                          boolean createIfMissing) {
            int nextId = atomicNextId.incrementAndGet();
            nextTable.put(nextId, treatment);
            return nextId;
        }
    }

    class MockInterfaceService extends InterfaceServiceAdapter {
        @Override
        public Set<Interface> getInterfacesByPort(ConnectPoint cp) {
            Interface intf = null;

            if (CP11.equals(cp)) {
                intf = new Interface(null, CP11, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                        INTF_VLAN_UNTAGGED, null, null);
            } else if (CP12.equals(cp)) {
                intf = new Interface(null, CP12, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                        INTF_VLAN_UNTAGGED, null, null);
            } else if (CP13.equals(cp)) {
                intf = new Interface(null, CP13, Lists.newArrayList(INTF_IP2), MacAddress.NONE, null,
                        null, INTF_VLAN_TAGGED, INTF_VLAN_NATIVE);
            } else if (CP21.equals(cp)) {
                intf = new Interface(null, CP21, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                        INTF_VLAN_UNTAGGED, null, null);
            } else if (CP22.equals(cp)) {
                intf = new Interface(null, CP22, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                        INTF_VLAN_UNTAGGED, null, null);
            } else if (CP31.equals(cp)) {
                intf = new Interface(null, CP31, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                        INTF_VLAN_UNTAGGED, null, null);
            } else if (CP39.equals(cp)) {
                intf = new Interface(null, CP39, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                        null, INTF_VLAN_PAIR, null);
            } else if (CP41.equals(cp)) {
                intf = new Interface(null, CP41, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                        INTF_VLAN_UNTAGGED, null, null);
            } else if (CP49.equals(cp)) {
                intf = new Interface(null, CP49, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                        null, INTF_VLAN_PAIR, null);
            }
            return Objects.nonNull(intf) ? Sets.newHashSet(intf) : Sets.newHashSet();
        }
    }

    class MockNetworkConfigRegistry extends NetworkConfigRegistryAdapter {
        @Override
        public <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass) {
            if (subject instanceof DeviceId) {
                DeviceId deviceId = (DeviceId) subject;
                ObjectMapper mapper = new ObjectMapper();
                ConfigApplyDelegate delegate = new MockCfgDelegate();
                JsonNode emptyTree = new ObjectMapper().createObjectNode();

                SegmentRoutingDeviceConfig config = new SegmentRoutingDeviceConfig();
                config.init(deviceId, "host-handler-test", emptyTree, mapper, delegate);
                config.setPairDeviceId(subject.equals(DEV3) ? DEV4 : DEV3)
                        .setPairLocalPort(P9);

                return (C) config;
            } else {
                return null;
            }
        }
    }

    class MockCfgDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(@SuppressWarnings("rawtypes") Config config) {
            config.apply();
        }
    }

    class MockFlowObjectiveService extends FlowObjectiveServiceAdapter {
        @Override
        public void forward(DeviceId deviceId, ForwardingObjective forwardingObjective) {
            TrafficSelector selector = forwardingObjective.selector();
            TrafficTreatment treatment = nextTable.get(forwardingObjective.nextId());
            MacAddress macAddress = ((EthCriterion) selector.getCriterion(Criterion.Type.ETH_DST)).mac();
            VlanId vlanId = ((VlanIdCriterion) selector.getCriterion(Criterion.Type.VLAN_VID)).vlanId();

            boolean popVlan = treatment.allInstructions().stream()
                    .filter(instruction -> instruction.type().equals(Instruction.Type.L2MODIFICATION))
                    .anyMatch(instruction -> ((L2ModificationInstruction) instruction).subtype()
                            .equals(L2ModificationInstruction.L2SubType.VLAN_POP));
            PortNumber portNumber = treatment.allInstructions().stream()
                    .filter(instruction -> instruction.type().equals(Instruction.Type.OUTPUT))
                    .map(instruction -> ((Instructions.OutputInstruction) instruction).port()).findFirst().orElse(null);
            if (portNumber == null) {
                throw new IllegalArgumentException();
            }

            Objective.Operation op = forwardingObjective.op();

            BridingTableKey btKey = new BridingTableKey(deviceId, macAddress, vlanId);
            BridingTableValue btValue = new BridingTableValue(popVlan, portNumber);

            if (op.equals(Objective.Operation.ADD)) {
                bridgingTable.put(btKey, btValue);
            } else if (op.equals(Objective.Operation.REMOVE)) {
                bridgingTable.remove(btKey, btValue);
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    class MockHostService extends HostServiceAdapter {
        @Override
        public Set<Host> getHosts() {
            return Sets.newHashSet(HOST1);
        }
    }

    class MockRoutingRulePopulator extends RoutingRulePopulator {
        MockRoutingRulePopulator() {
            super(srManager);
        }

        @Override
        public void populateRoute(DeviceId deviceId, IpPrefix prefix,
                                  MacAddress hostMac, VlanId hostVlanId, PortNumber outPort) {
            RoutingTableKey rtKey = new RoutingTableKey(deviceId, prefix);
            RoutingTableValue rtValue = new RoutingTableValue(outPort, hostMac, hostVlanId);
            routingTable.put(rtKey, rtValue);
        }

        @Override
        public void revokeRoute(DeviceId deviceId, IpPrefix prefix,
                                MacAddress hostMac, VlanId hostVlanId, PortNumber outPort) {
            RoutingTableKey rtKey = new RoutingTableKey(deviceId, prefix);
            RoutingTableValue rtValue = new RoutingTableValue(outPort, hostMac, hostVlanId);
            routingTable.remove(rtKey, rtValue);
        }
    }

    class BridingTableKey {
        DeviceId deviceId;
        MacAddress macAddress;
        VlanId vlanId;

        BridingTableKey(DeviceId deviceId, MacAddress macAddress, VlanId vlanId) {
            this.deviceId = deviceId;
            this.macAddress = macAddress;
            this.vlanId = vlanId;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BridingTableKey)) {
                return false;
            }
            final BridingTableKey other = (BridingTableKey) obj;
            return Objects.equals(this.macAddress, other.macAddress) &&
                    Objects.equals(this.deviceId, other.deviceId) &&
                    Objects.equals(this.vlanId, other.vlanId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(macAddress, vlanId);
        }
    }

    class BridingTableValue {
        boolean popVlan;
        PortNumber portNumber;

        BridingTableValue(boolean popVlan, PortNumber portNumber) {
            this.popVlan = popVlan;
            this.portNumber = portNumber;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BridingTableValue)) {
                return false;
            }
            final BridingTableValue other = (BridingTableValue) obj;
            return Objects.equals(this.popVlan, other.popVlan) &&
                    Objects.equals(this.portNumber, other.portNumber);
        }

        @Override
        public int hashCode() {
            return Objects.hash(popVlan, portNumber);
        }
    }

    class RoutingTableKey {
        DeviceId deviceId;
        IpPrefix ipPrefix;

        RoutingTableKey(DeviceId deviceId, IpPrefix ipPrefix) {
            this.deviceId = deviceId;
            this.ipPrefix = ipPrefix;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof RoutingTableKey)) {
                return false;
            }
            final RoutingTableKey other = (RoutingTableKey) obj;
            return Objects.equals(this.deviceId, other.deviceId) &&
                    Objects.equals(this.ipPrefix, other.ipPrefix);
        }

        @Override
        public int hashCode() {
            return Objects.hash(deviceId, ipPrefix);
        }
    }

    class RoutingTableValue {
        PortNumber portNumber;
        MacAddress macAddress;
        VlanId vlanId;

        RoutingTableValue(PortNumber portNumber, MacAddress macAddress, VlanId vlanId) {
            this.portNumber = portNumber;
            this.macAddress = macAddress;
            this.vlanId = vlanId;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof RoutingTableValue)) {
                return false;
            }
            final RoutingTableValue other = (RoutingTableValue) obj;
            return Objects.equals(this.portNumber, other.portNumber) &&
                    Objects.equals(this.macAddress, other.macAddress) &&
                    Objects.equals(this.vlanId, other.vlanId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(portNumber, macAddress, vlanId);
        }
    }

}