/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.segmentrouting.config.DeviceConfiguration;

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

    // Host information
    private static final ProviderId PROVIDER_ID = ProviderId.NONE;
    private static final MacAddress HOST_MAC = MacAddress.valueOf("00:00:00:00:00:01");
    private static final VlanId HOST_VLAN_UNTAGGED = VlanId.NONE;
    private static final HostId HOST_ID_UNTAGGED = HostId.hostId(HOST_MAC, HOST_VLAN_UNTAGGED);
    private static final VlanId HOST_VLAN_TAGGED = VlanId.vlanId((short) 20);
    private static final HostId HOST_ID_TAGGED = HostId.hostId(HOST_MAC, HOST_VLAN_TAGGED);
    private static final IpAddress HOST_IP1 = IpAddress.valueOf("10.0.1.1");
    private static final IpAddress HOST_IP2 = IpAddress.valueOf("10.0.2.1");
    private static final IpAddress HOST_IP3 = IpAddress.valueOf("10.0.1.2");

    // Untagged interface
    private static final ConnectPoint CP1 = new ConnectPoint(DeviceId.deviceId("of:0000000000000001"),
            PortNumber.portNumber(10));
    private static final HostLocation HOST_LOC1 = new HostLocation(CP1, 0);
    private static final IpPrefix INTF_PREFIX1 = IpPrefix.valueOf("10.0.1.254/24");
    private static final InterfaceIpAddress INTF_IP1 = new InterfaceIpAddress(INTF_PREFIX1.address(),
            INTF_PREFIX1);
    private static final VlanId INTF_VLAN_UNTAGGED = VlanId.vlanId((short) 10);

    // Another untagged interface with same subnet and vlan
    private static final ConnectPoint CP3 = new ConnectPoint(DeviceId.deviceId("of:0000000000000002"),
            PortNumber.portNumber(30));
    private static final HostLocation HOST_LOC3 = new HostLocation(CP3, 0);

    // Tagged/Native interface
    private static final ConnectPoint CP2 = new ConnectPoint(DeviceId.deviceId("of:0000000000000001"),
            PortNumber.portNumber(20));
    private static final HostLocation HOST_LOC2 = new HostLocation(CP2, 0);
    private static final IpPrefix INTF_PREFIX2 = IpPrefix.valueOf("10.0.2.254/24");
    private static final InterfaceIpAddress INTF_IP2 = new  InterfaceIpAddress(INTF_PREFIX2.address(),
            INTF_PREFIX2);
    private static final Set<VlanId> INTF_VLAN_TAGGED = Sets.newHashSet(VlanId.vlanId((short) 20));
    private static final VlanId INTF_VLAN_NATIVE = VlanId.vlanId((short) 30);

    @Before
    public void setUp() throws Exception {
        srManager = new MockSegmentRoutingManager();
        srManager.cfgService = new NetworkConfigRegistryAdapter();
        srManager.deviceConfiguration = new DeviceConfiguration(srManager);
        srManager.flowObjectiveService = new MockFlowObjectiveService();
        srManager.routingRulePopulator = new MockRoutingRulePopulator();
        srManager.interfaceService = new MockInterfaceService();

        hostHandler = new HostHandler(srManager);

        routingTable.clear();
        bridgingTable.clear();
    }

    @Test
    public void init() throws Exception {
        // TODO Implement test for init()
    }

    @Test
    public void testHostAdded() throws Exception {
        Host subject;

        // Untagged host discovered on untagged port
        // Expect: add one routing rule and one bridging rule
        subject = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC1), Sets.newHashSet(HOST_IP1), false);
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, subject));
        assertEquals(1, routingTable.size());
        assertNotNull(routingTable.get(new RoutingTableKey(HOST_LOC1.deviceId(), HOST_IP1.toIpPrefix())));
        assertEquals(1, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(HOST_LOC1.deviceId(), HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Untagged host discovered on tagged/native port
        // Expect: add one routing rule and one bridging rule
        subject = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC2), Sets.newHashSet(HOST_IP2), false);
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, subject));
        assertEquals(2, routingTable.size());
        assertNotNull(routingTable.get(new RoutingTableKey(HOST_LOC2.deviceId(), HOST_IP2.toIpPrefix())));
        assertEquals(2, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(HOST_LOC2.deviceId(), HOST_MAC, INTF_VLAN_NATIVE)));

        // Tagged host discovered on untagged port
        // Expect: ignore the host. No rule is added.
        subject = new DefaultHost(PROVIDER_ID, HOST_ID_TAGGED, HOST_MAC, HOST_VLAN_TAGGED,
                Sets.newHashSet(HOST_LOC1), Sets.newHashSet(HOST_IP1), false);
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, subject));
        assertEquals(2, routingTable.size());
        assertEquals(2, bridgingTable.size());

        // Tagged host discovered on tagged port with the same IP
        // Expect: update existing route, add one bridging rule
        subject = new DefaultHost(PROVIDER_ID, HOST_ID_TAGGED, HOST_MAC, HOST_VLAN_TAGGED,
                Sets.newHashSet(HOST_LOC2), Sets.newHashSet(HOST_IP2), false);
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, subject));
        assertEquals(2, routingTable.size());
        assertNotNull(routingTable.get(new RoutingTableKey(HOST_LOC2.deviceId(), HOST_IP2.toIpPrefix())));
        assertEquals(HOST_VLAN_TAGGED, routingTable.get(new RoutingTableKey(HOST_LOC2.deviceId(),
                HOST_IP2.toIpPrefix())).vlanId);
        assertEquals(3, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(HOST_LOC2.deviceId(), HOST_MAC, HOST_VLAN_TAGGED)));
    }

    @Test
    public void testHostRemoved() throws Exception {
        Host subject = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC1), Sets.newHashSet(HOST_IP1), false);

        // Add a host
        // Expect: add one routing rule and one bridging rule
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, subject));
        assertEquals(1, routingTable.size());
        assertNotNull(routingTable.get(new RoutingTableKey(HOST_LOC1.deviceId(), HOST_IP1.toIpPrefix())));
        assertEquals(1, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(HOST_LOC1.deviceId(), HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Remove the host
        // Expect: add the routing rule and the bridging rule
        hostHandler.processHostRemoveEvent(new HostEvent(HostEvent.Type.HOST_REMOVED, subject));
        assertEquals(0, routingTable.size());
        assertNull(routingTable.get(new RoutingTableKey(HOST_LOC1.deviceId(), HOST_IP2.toIpPrefix())));
        assertEquals(0, bridgingTable.size());
        assertNull(bridgingTable.get(new BridingTableKey(HOST_LOC1.deviceId(), HOST_MAC, INTF_VLAN_UNTAGGED)));
    }

    @Test
    public void testHostMoved() throws Exception {
        Host host1 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC1), Sets.newHashSet(HOST_IP1), false);
        Host host2 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC2), Sets.newHashSet(HOST_IP1), false);
        Host host3 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC3), Sets.newHashSet(HOST_IP1), false);

        // Add a host
        // Expect: add a new routing rule. no change to bridging rule.
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, host1));
        assertEquals(1, routingTable.size());
        assertNotNull(routingTable.get(new RoutingTableKey(HOST_LOC1.deviceId(), HOST_IP1.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(HOST_LOC1.deviceId(), HOST_IP2.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(HOST_LOC1.deviceId(), HOST_IP3.toIpPrefix())));
        assertEquals(1, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(HOST_LOC1.deviceId(), HOST_MAC, INTF_VLAN_UNTAGGED)));
        assertNull(bridgingTable.get(new BridingTableKey(HOST_LOC3.deviceId(), HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Move the host to CP2, which has different subnet setting
        // Expect: remove routing rule. Change vlan in bridging rule.
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host2, host1));
        assertEquals(0, routingTable.size());
        assertNull(routingTable.get(new RoutingTableKey(HOST_LOC1.deviceId(), HOST_IP1.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(HOST_LOC2.deviceId(), HOST_IP1.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(HOST_LOC3.deviceId(), HOST_IP1.toIpPrefix())));
        assertEquals(1, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(HOST_LOC2.deviceId(), HOST_MAC, INTF_VLAN_NATIVE)));
        assertNull(bridgingTable.get(new BridingTableKey(HOST_LOC3.deviceId(), HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Move the host to CP3, which has same subnet setting
        // Expect: add a new routing rule. Change vlan in bridging rule.
        hostHandler.processHostMovedEvent(new HostEvent(HostEvent.Type.HOST_MOVED, host3, host2));
        assertEquals(1, routingTable.size());
        assertNull(routingTable.get(new RoutingTableKey(HOST_LOC1.deviceId(), HOST_IP1.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(HOST_LOC2.deviceId(), HOST_IP1.toIpPrefix())));
        assertNotNull(routingTable.get(new RoutingTableKey(HOST_LOC3.deviceId(), HOST_IP1.toIpPrefix())));
        assertEquals(1, bridgingTable.size());
        assertNull(bridgingTable.get(new BridingTableKey(HOST_LOC1.deviceId(), HOST_MAC, INTF_VLAN_UNTAGGED)));
        assertNotNull(bridgingTable.get(new BridingTableKey(HOST_LOC3.deviceId(), HOST_MAC, INTF_VLAN_UNTAGGED)));
    }

    @Test
    public void testHostUpdated() throws Exception {
        Host host1 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC1), Sets.newHashSet(HOST_IP1), false);
        Host host2 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC1), Sets.newHashSet(HOST_IP2), false);
        Host host3 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC, HOST_VLAN_UNTAGGED,
                Sets.newHashSet(HOST_LOC1), Sets.newHashSet(HOST_IP3), false);

        // Add a host
        // Expect: add a new routing rule. no change to bridging rule.
        hostHandler.processHostAddedEvent(new HostEvent(HostEvent.Type.HOST_ADDED, host1));
        assertEquals(1, routingTable.size());
        assertNotNull(routingTable.get(new RoutingTableKey(HOST_LOC1.deviceId(), HOST_IP1.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(HOST_LOC1.deviceId(), HOST_IP2.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(HOST_LOC1.deviceId(), HOST_IP3.toIpPrefix())));
        assertEquals(1, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(HOST_LOC1.deviceId(), HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Update the host IP to same subnet
        // Expect: update routing rule with new IP. No change to bridging rule.
        hostHandler.processHostUpdatedEvent(new HostEvent(HostEvent.Type.HOST_UPDATED, host3, host1));
        assertEquals(1, routingTable.size());
        assertNull(routingTable.get(new RoutingTableKey(HOST_LOC1.deviceId(), HOST_IP1.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(HOST_LOC1.deviceId(), HOST_IP2.toIpPrefix())));
        assertNotNull(routingTable.get(new RoutingTableKey(HOST_LOC1.deviceId(), HOST_IP3.toIpPrefix())));
        assertEquals(1, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(HOST_LOC1.deviceId(), HOST_MAC, INTF_VLAN_UNTAGGED)));

        // Update the host IP to different subnet
        // Expect: Remove routing rule. No change to bridging rule.
        hostHandler.processHostUpdatedEvent(new HostEvent(HostEvent.Type.HOST_UPDATED, host2, host3));
        assertEquals(0, routingTable.size());
        assertNull(routingTable.get(new RoutingTableKey(HOST_LOC1.deviceId(), HOST_IP1.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(HOST_LOC1.deviceId(), HOST_IP2.toIpPrefix())));
        assertNull(routingTable.get(new RoutingTableKey(HOST_LOC1.deviceId(), HOST_IP3.toIpPrefix())));
        assertEquals(1, bridgingTable.size());
        assertNotNull(bridgingTable.get(new BridingTableKey(HOST_LOC1.deviceId(), HOST_MAC, INTF_VLAN_UNTAGGED)));
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

            if (CP1.equals(cp)) {
                intf = new Interface(null, CP1, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                        INTF_VLAN_UNTAGGED, null, null);
            } else if (CP2.equals(cp)) {
                intf = new Interface(null, CP2, Lists.newArrayList(INTF_IP2), MacAddress.NONE, null,
                        null, INTF_VLAN_TAGGED, INTF_VLAN_NATIVE);
            } else if (CP3.equals(cp)) {
                intf = new Interface(null, CP3, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                        INTF_VLAN_UNTAGGED, null, null);
            }

            return Objects.nonNull(intf) ? Sets.newHashSet(intf) : Sets.newHashSet();
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