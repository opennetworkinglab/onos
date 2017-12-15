/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.t3.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.EthType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.driver.DefaultDriver;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverServiceAdapter;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleServiceAdapter;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupServiceAdapter;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.link.LinkServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.t3.api.StaticPacketTrace;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.t3.impl.T3TestObjects.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Test Class for Troubleshoot Manager.
 */
public class TroubleshootManagerTest {

    private static final Logger log = getLogger(TroubleshootManager.class);

    private TroubleshootManager mngr;

    @Before
    public void setUp() throws Exception {
        mngr = new TroubleshootManager();
        mngr.flowRuleService = new TestFlowRuleService();
        mngr.hostService = new TestHostService();
        mngr.linkService = new TestLinkService();
        mngr.driverService = new TestDriverService();
        mngr.groupService = new TestGroupService();

        assertNotNull("Manager should not be null", mngr);

        assertNotNull("Flow rule Service should not be null", mngr.flowRuleService);
        assertNotNull("Host Service should not be null", mngr.hostService);
        assertNotNull("Group Service should not be null", mngr.groupService);
        assertNotNull("Driver Service should not be null", mngr.driverService);
        assertNotNull("Link Service should not be null", mngr.linkService);
    }

    /**
     * Tests failure on device with no flows
     */
    @Test
    public void noFlows() {
        StaticPacketTrace traceFail = mngr.trace(PACKET_OK, ConnectPoint.deviceConnectPoint("test/1"));
        assertNotNull("Trace should not be null", traceFail);
        assertNull("Trace should have 0 output", traceFail.getGroupOuputs(SINGLE_FLOW_DEVICE));
        log.info("trace {}", traceFail.resultMessage());
    }

    /**
     * Test a single flow rule that has output port in it.
     */
    @Test
    public void testSingleFlowRule() {

        testSuccess(PACKET_OK, SINGLE_FLOW_IN_CP, SINGLE_FLOW_DEVICE, SINGLE_FLOW_OUT_CP, 1);

        testFaliure(PACKET_FAIL, SINGLE_FLOW_IN_CP, SINGLE_FLOW_DEVICE);
    }

    /**
     * Tests two flow rule the last one of which has output port in it.
     */
    @Test
    public void testDualFlowRule() {

        //Test Success

        StaticPacketTrace traceSuccess = testSuccess(PACKET_OK, DUAL_FLOW_IN_CP, DUAL_FLOW_DEVICE, DUAL_FLOW_OUT_CP, 1);

        //Testing Vlan
        Criterion criterion = traceSuccess.getGroupOuputs(DUAL_FLOW_DEVICE).get(0).
                getFinalPacket().getCriterion(Criterion.Type.VLAN_VID);
        assertNotNull("Packet Should have Vlan", criterion);

        VlanIdCriterion vlanIdCriterion = (VlanIdCriterion) criterion;

        assertEquals("Vlan should be 100", VlanId.vlanId((short) 100), vlanIdCriterion.vlanId());

        //Test Faliure
        testFaliure(PACKET_FAIL, DUAL_FLOW_IN_CP, DUAL_FLOW_DEVICE);

    }

    /**
     * Test a single flow rule that points to a group with output port in it.
     */
    @Test
    public void flowAndGroup() throws Exception {

        StaticPacketTrace traceSuccess = testSuccess(PACKET_OK, GROUP_FLOW_IN_CP, GROUP_FLOW_DEVICE, GROUP_FLOW_OUT_CP, 1);

        assertTrue("Wrong Output Group", traceSuccess.getGroupOuputs(GROUP_FLOW_DEVICE)
                .get(0).getGroups().contains(GROUP));

    }

    /**
     * Test path through a 3 device topology.
     */
    @Test
    public void singlePathTopology() throws Exception {

        StaticPacketTrace traceSuccess = testSuccess(PACKET_OK_TOPO, TOPO_FLOW_1_IN_CP,
                TOPO_FLOW_3_DEVICE, TOPO_FLOW_3_OUT_CP, 1);

        assertTrue("Incorrect path",
                traceSuccess.getCompletePaths().get(0).contains(TOPO_FLOW_2_IN_CP));
        assertTrue("Incorrect path",
                traceSuccess.getCompletePaths().get(0).contains(TOPO_FLOW_2_OUT_CP));
        assertTrue("Incorrect path",
                traceSuccess.getCompletePaths().get(0).contains(TOPO_FLOW_3_IN_CP));

    }

    /**
     * Test path through a 4 device topology with first device that has groups with multiple output buckets.
     */
    @Test
    public void testGroupTopo() throws Exception {

        StaticPacketTrace traceSuccess = testSuccess(PACKET_OK_TOPO, TOPO_FLOW_IN_CP,
                TOPO_FLOW_3_DEVICE, TOPO_FLOW_3_OUT_CP, 2);

        assertTrue("Incorrect groups",
                traceSuccess.getGroupOuputs(TOPO_GROUP_FLOW_DEVICE).get(0).getGroups().contains(TOPO_GROUP));
        assertTrue("Incorrect bucket",
                traceSuccess.getGroupOuputs(TOPO_GROUP_FLOW_DEVICE).get(1).getGroups().contains(TOPO_GROUP));
    }

    /**
     * Test HW support in a single device with 2 flow rules to check hit of static HW rules.
     */
    @Test
    public void hardwareTest() throws Exception {

        StaticPacketTrace traceSuccess = testSuccess(PACKET_OK, HARDWARE_DEVICE_IN_CP,
                HARDWARE_DEVICE, HARDWARE_DEVICE_OUT_CP, 1);

        assertEquals("wrong ETH type", EthType.EtherType.IPV4.ethType(),
                ((EthTypeCriterion) traceSuccess.getGroupOuputs(HARDWARE_DEVICE).get(0).getFinalPacket()
                        .getCriterion(Criterion.Type.ETH_TYPE)).ethType());

    }

    private StaticPacketTrace testSuccess(TrafficSelector packet, ConnectPoint in, DeviceId deviceId, ConnectPoint out,
                                          int paths) {
        StaticPacketTrace traceSuccess = mngr.trace(packet, in);

        log.info("trace {}", traceSuccess);

        log.info("trace {}", traceSuccess.resultMessage());

        assertNotNull("trace should not be null", traceSuccess);
        assertEquals("Trace should have " + paths + " output", paths, traceSuccess.getGroupOuputs(deviceId).size());
        assertEquals("Trace should only have " + paths + "output", paths, traceSuccess.getCompletePaths().size());
        assertTrue("Trace should be successful",
                traceSuccess.resultMessage().contains("Reached required destination Host"));
        assertEquals("Incorrect Output CP", out,
                traceSuccess.getGroupOuputs(deviceId).get(0).getOutput());

        return traceSuccess;
    }

    private void testFaliure(TrafficSelector packet, ConnectPoint in, DeviceId deviceId) {
        StaticPacketTrace traceFail = mngr.trace(packet, in);

        log.info("trace {}", traceFail.resultMessage());

        assertNotNull("Trace should not be null", traceFail);
        assertNull("Trace should have 0 output", traceFail.getGroupOuputs(deviceId));
    }

    private class TestFlowRuleService extends FlowRuleServiceAdapter {
        @Override
        public Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {
            if (deviceId.equals(SINGLE_FLOW_DEVICE)) {
                return ImmutableList.of(SINGLE_FLOW_ENTRY);
            } else if (deviceId.equals(DUAL_FLOW_DEVICE)) {
                return ImmutableList.of(FIRST_FLOW_ENTRY, SECOND_FLOW_ENTRY);
            } else if (deviceId.equals(GROUP_FLOW_DEVICE)) {
                return ImmutableList.of(GROUP_FLOW_ENTRY);
            } else if (deviceId.equals(TOPO_FLOW_DEVICE) ||
                    deviceId.equals(TOPO_FLOW_2_DEVICE) ||
                    deviceId.equals(TOPO_FLOW_3_DEVICE) ||
                    deviceId.equals(TOPO_FLOW_4_DEVICE)) {
                return ImmutableList.of(TOPO_SINGLE_FLOW_ENTRY, TOPO_SECOND_INPUT_FLOW_ENTRY);
            } else if (deviceId.equals(TOPO_GROUP_FLOW_DEVICE)) {
                return ImmutableList.of(TOPO_GROUP_FLOW_ENTRY);
            } else if (deviceId.equals(HARDWARE_DEVICE)){
                return ImmutableList.of(HARDWARE_ETH_FLOW_ENTRY, HARDWARE_FLOW_ENTRY);
            }
            return ImmutableList.of();
        }
    }

    private class TestDriverService extends DriverServiceAdapter {
        @Override
        public Driver getDriver(DeviceId deviceId) {
            if(deviceId.equals(HARDWARE_DEVICE)){
                return new DefaultDriver("ofdpa", ImmutableList.of(),
                        "test", "test", "test", new HashMap<>(), new HashMap<>());
            }
            return new DefaultDriver("NotHWDriver", ImmutableList.of(),
                    "test", "test", "test", new HashMap<>(), new HashMap<>());
        }
    }

    private class TestGroupService extends GroupServiceAdapter {
        @Override
        public Iterable<Group> getGroups(DeviceId deviceId) {
            if (deviceId.equals(GROUP_FLOW_DEVICE)) {
                return ImmutableList.of(GROUP);
            } else if (deviceId.equals(TOPO_GROUP_FLOW_DEVICE)) {
                return ImmutableList.of(TOPO_GROUP);
            }
            return ImmutableList.of();
        }
    }

    private class TestHostService extends HostServiceAdapter {
        @Override
        public Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
            if (connectPoint.equals(TOPO_FLOW_3_OUT_CP)) {
                return ImmutableSet.of(H2);
            }
            return ImmutableSet.of(H1);
        }

        @Override
        public Set<Host> getHostsByMac(MacAddress mac) {
            if (mac.equals(H1.mac())) {
                return ImmutableSet.of(H1);
            } else if (mac.equals(H2.mac())) {
                return ImmutableSet.of(H2);
            }
            return ImmutableSet.of();
        }

        @Override
        public Set<Host> getHostsByIp(IpAddress ip) {
            if ((H1.ipAddresses().contains(ip))) {
                return ImmutableSet.of(H1);
            } else if ((H2.ipAddresses().contains(ip))) {
                return ImmutableSet.of(H2);
            }
            return ImmutableSet.of();
        }
    }

    private class TestLinkService extends LinkServiceAdapter {
        @Override
        public Set<Link> getEgressLinks(ConnectPoint connectPoint) {
            if (connectPoint.equals(TOPO_FLOW_1_OUT_CP)
                    || connectPoint.equals(TOPO_FLOW_OUT_CP_1)) {
                return ImmutableSet.of(DefaultLink.builder()
                        .providerId(ProviderId.NONE)
                        .type(Link.Type.DIRECT)
                        .src(connectPoint)
                        .dst(TOPO_FLOW_2_IN_CP)
                        .build());
            } else if (connectPoint.equals(TOPO_FLOW_2_OUT_CP)) {
                return ImmutableSet.of(DefaultLink.builder()
                        .providerId(ProviderId.NONE)
                        .type(Link.Type.DIRECT)
                        .src(TOPO_FLOW_2_OUT_CP)
                        .dst(TOPO_FLOW_3_IN_CP)
                        .build());
            } else if (connectPoint.equals(TOPO_FLOW_OUT_CP_2)) {
                return ImmutableSet.of(DefaultLink.builder()
                        .providerId(ProviderId.NONE)
                        .type(Link.Type.DIRECT)
                        .src(TOPO_FLOW_OUT_CP_2)
                        .dst(TOPO_FLOW_4_IN_CP)
                        .build());
            } else if (connectPoint.equals(TOPO_FLOW_4_OUT_CP)) {
                return ImmutableSet.of(DefaultLink.builder()
                        .providerId(ProviderId.NONE)
                        .type(Link.Type.DIRECT)
                        .src(TOPO_FLOW_4_OUT_CP)
                        .dst(TOPO_FLOW_3_IN_2_CP)
                        .build());
            }
            return ImmutableSet.of();
        }
    }
}