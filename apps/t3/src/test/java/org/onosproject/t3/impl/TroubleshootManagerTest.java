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
import org.onlab.packet.ChassisId;
import org.onlab.packet.EthType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.driver.DefaultDriver;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverServiceAdapter;
import org.onosproject.net.edge.EdgePortServiceAdapter;
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
import org.onosproject.routeservice.ResolvedRoute;
import org.onosproject.routeservice.RouteServiceAdapter;
import org.onosproject.t3.api.StaticPacketTrace;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.Device.Type.SWITCH;
import static org.onosproject.t3.impl.T3TestObjects.*;
import static org.onosproject.t3.impl.TroubleshootManager.PACKET_TO_CONTROLLER;
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
        mngr.deviceService = new TestDeviceService();
        mngr.mastershipService = new TestMastershipService();
        mngr.edgePortService = new TestEdgePortService();
        mngr.routeService = new TestRouteService();

        assertNotNull("Manager should not be null", mngr);

        assertNotNull("Flow rule Service should not be null", mngr.flowRuleService);
        assertNotNull("Host Service should not be null", mngr.hostService);
        assertNotNull("Group Service should not be null", mngr.groupService);
        assertNotNull("Driver Service should not be null", mngr.driverService);
        assertNotNull("Link Service should not be null", mngr.linkService);
        assertNotNull("Device Service should not be null", mngr.deviceService);
    }

    /**
     * Tests failure on non existent device.
     */
    @Test(expected = NullPointerException.class)
    public void nonExistentDevice() {
        StaticPacketTrace traceFail = mngr.trace(PACKET_OK, ConnectPoint.deviceConnectPoint("nonexistent" + "/1"));
    }

    /**
     * Tests failure on offline device.
     */
    @Test
    public void offlineDevice() {
        StaticPacketTrace traceFail = mngr.trace(PACKET_OK, ConnectPoint.deviceConnectPoint(OFFLINE_DEVICE + "/1"));
        assertNotNull("Trace should not be null", traceFail);
        assertNull("Trace should have 0 output", traceFail.getGroupOuputs(SINGLE_FLOW_DEVICE));
    }

    /**
     * Tests failure on same output.
     */
    @Test
    public void sameOutput() {
        StaticPacketTrace traceFail = mngr.trace(PACKET_OK, SAME_OUTPUT_FLOW_CP);
        assertNotNull("Trace should not be null", traceFail);
        assertTrue("Trace should be unsuccessful",
                traceFail.resultMessage().contains("is same as initial input"));
        log.info("trace {}", traceFail.resultMessage());
    }

    /**
     * Tests ARP to controller.
     */
    @Test
    public void arpToController() {
        StaticPacketTrace traceSuccess = mngr.trace(PACKET_ARP, ARP_FLOW_CP);
        assertNotNull("Trace should not be null", traceSuccess);
        assertTrue("Trace should be successful",
                traceSuccess.resultMessage().contains(PACKET_TO_CONTROLLER));
        assertTrue("Master should be Master1",
                traceSuccess.resultMessage().contains(MASTER_1));
        ConnectPoint connectPoint = traceSuccess.getGroupOuputs(ARP_FLOW_DEVICE).get(0).getOutput();
        assertEquals("Packet Should go to CONTROLLER", PortNumber.CONTROLLER, connectPoint.port());
        assertNull("VlanId should be null", traceSuccess.getGroupOuputs(ARP_FLOW_DEVICE).get(0)
                .getFinalPacket().getCriterion(Criterion.Type.VLAN_VID));
        log.info("trace {}", traceSuccess.resultMessage());
    }


    /**
     * Tests failure on device with no flows.
     */
    @Test
    public void noFlows() {
        StaticPacketTrace traceFail = mngr.trace(PACKET_OK, ConnectPoint.deviceConnectPoint("test/1"));
        assertNotNull("Trace should not be null", traceFail);
        assertNull("Trace should have 0 output", traceFail.getGroupOuputs(SINGLE_FLOW_DEVICE));
        log.info("trace {}", traceFail.resultMessage());
    }

    /**
     * Test group with no buckets.
     */
    @Test
    public void noBucketsTest() throws Exception {

        StaticPacketTrace traceFail = mngr.trace(PACKET_OK, NO_BUCKET_CP);
        assertNotNull("Trace should not be null", traceFail);
        assertTrue("Trace should be unsuccessful",
                traceFail.resultMessage().contains("no buckets"));
        log.info("trace {}", traceFail.resultMessage());

    }

    /**
     * Test a single flow rule that has output port in it.
     */
    @Test
    public void testSingleFlowRule() {

        testSuccess(PACKET_OK, SINGLE_FLOW_IN_CP, SINGLE_FLOW_DEVICE, SINGLE_FLOW_OUT_CP, 1, 1);

        testFailure(PACKET_FAIL, SINGLE_FLOW_IN_CP, SINGLE_FLOW_DEVICE);
    }

    /**
     * Tests two flow rule the last one of which has output port in it.
     */
    @Test
    public void testDualFlowRule() {

        //Test Success

        StaticPacketTrace traceSuccess = testSuccess(PACKET_OK, DUAL_FLOW_IN_CP, DUAL_FLOW_DEVICE,
                DUAL_FLOW_OUT_CP, 1, 1);

        //Testing Vlan
        Criterion criterion = traceSuccess.getGroupOuputs(DUAL_FLOW_DEVICE).get(0).
                getFinalPacket().getCriterion(Criterion.Type.VLAN_VID);
        assertNotNull("Packet Should have Vlan", criterion);

        VlanIdCriterion vlanIdCriterion = (VlanIdCriterion) criterion;

        assertEquals("Vlan should be 100", VlanId.vlanId((short) 100), vlanIdCriterion.vlanId());

        //Test Faliure
        testFailure(PACKET_FAIL, DUAL_FLOW_IN_CP, DUAL_FLOW_DEVICE);

    }

    /**
     * Test a single flow rule that points to a group with output port in it.
     */
    @Test
    public void flowAndGroup() throws Exception {

        StaticPacketTrace traceSuccess = testSuccess(PACKET_OK, GROUP_FLOW_IN_CP, GROUP_FLOW_DEVICE,
                GROUP_FLOW_OUT_CP, 1, 1);

        assertTrue("Wrong Output Group", traceSuccess.getGroupOuputs(GROUP_FLOW_DEVICE)
                .get(0).getGroups().contains(GROUP));
        assertEquals("Packet should not have MPLS Label", EthType.EtherType.IPV4.ethType(),
                ((EthTypeCriterion) traceSuccess.getGroupOuputs(GROUP_FLOW_DEVICE)
                        .get(0).getFinalPacket().getCriterion(Criterion.Type.ETH_TYPE)).ethType());
        assertNull("Packet should not have MPLS Label", traceSuccess.getGroupOuputs(GROUP_FLOW_DEVICE)
                .get(0).getFinalPacket().getCriterion(Criterion.Type.MPLS_LABEL));
        assertNull("Packet should not have MPLS Label", traceSuccess.getGroupOuputs(GROUP_FLOW_DEVICE)
                .get(0).getFinalPacket().getCriterion(Criterion.Type.MPLS_BOS));

    }

    /**
     * Test path through a 3 device topology.
     */
    @Test
    public void singlePathTopology() throws Exception {

        StaticPacketTrace traceSuccess = testSuccess(PACKET_OK_TOPO, TOPO_FLOW_1_IN_CP,
                TOPO_FLOW_3_DEVICE, TOPO_FLOW_3_OUT_CP, 1, 1);

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
                TOPO_FLOW_3_DEVICE, TOPO_FLOW_3_OUT_CP, 2, 1);

        log.info("{}", traceSuccess);

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
                HARDWARE_DEVICE, HARDWARE_DEVICE_OUT_CP, 1, 1);

        assertEquals("wrong ETH type", EthType.EtherType.IPV4.ethType(),
                ((EthTypeCriterion) traceSuccess.getGroupOuputs(HARDWARE_DEVICE).get(0).getFinalPacket()
                        .getCriterion(Criterion.Type.ETH_TYPE)).ethType());

    }

    /**
     * Test that HW has two rules on table 10 for untagged packets.
     */
    @Test
    public void hardwareTable10Test() throws Exception {

        StaticPacketTrace traceSuccess = testSuccess(PACKET_OK, HARDWARE_DEVICE_10_IN_CP,
                HARDWARE_DEVICE_10, HARDWARE_DEVICE_10_OUT_CP, 1, 1);

        assertTrue("Second flow rule is absent", traceSuccess.getFlowsForDevice(HARDWARE_DEVICE_10)
                .contains(HARDWARE_10_SECOND_FLOW_ENTRY));

    }

    /**
     * Test dual links between 3 topology elements.
     */
    @Test
    public void dualLinks() throws Exception {

        StaticPacketTrace traceSuccess = testSuccess(PACKET_OK, DUAL_LINK_1_CP_1_IN,
                DUAL_LINK_3, DUAL_LINK_3_CP_3_OUT, 4, 1);

        //TODO tests

    }

    /**
     * Test proper clear deferred behaviour.
     */
    @Test
    public void clearDeferred() throws Exception {

        StaticPacketTrace traceSuccess = testSuccess(PACKET_OK, DEFERRED_CP_1_IN,
                DEFERRED_1, DEFERRED_CP_2_OUT, 1, 1);

        assertNull("MPLS should have been not applied due to clear deferred", traceSuccess
                .getGroupOuputs(DEFERRED_1).get(0).getFinalPacket().getCriterion(Criterion.Type.MPLS_LABEL));

    }


    /**
     * Test LLDP output to controller.
     */
    @Test
    public void lldpToController() {
        StaticPacketTrace traceSuccess = mngr.trace(PACKET_LLDP, LLDP_FLOW_CP);
        assertNotNull("Trace should not be null", traceSuccess);
        assertTrue("Trace should be successful",
                traceSuccess.resultMessage().contains("Packet goes to the controller"));
        assertTrue("Master should be Master1",
                traceSuccess.resultMessage().contains(MASTER_1));
        ConnectPoint connectPoint = traceSuccess.getGroupOuputs(LLDP_FLOW_DEVICE).get(0).getOutput();
        assertEquals("Packet Should go to CONTROLLER", PortNumber.CONTROLLER, connectPoint.port());
        log.info("trace {}", traceSuccess.resultMessage());
    }

    /**
     * Test multicast in single device.
     */
    @Test
    public void multicastTest() throws Exception {

        StaticPacketTrace traceSuccess = mngr.trace(PACKET_OK_MULTICAST, MULTICAST_IN_CP);

        log.info("trace {}", traceSuccess);

        log.info("trace {}", traceSuccess.resultMessage());

        assertNotNull("trace should not be null", traceSuccess);
        assertEquals("Trace should have " + 2 + " output", 2,
                traceSuccess.getGroupOuputs(MULTICAST_GROUP_FLOW_DEVICE).size());
        assertEquals("Trace should only have " + 2 + "output", 2,
                traceSuccess.getCompletePaths().size());
        assertTrue("Trace should be successful",
                traceSuccess.resultMessage().contains("reached output"));
        assertEquals("Incorrect Output CP", MULTICAST_OUT_CP_2,
                traceSuccess.getGroupOuputs(MULTICAST_GROUP_FLOW_DEVICE).get(0).getOutput());
        assertEquals("Incorrect Output CP", MULTICAST_OUT_CP,
                traceSuccess.getGroupOuputs(MULTICAST_GROUP_FLOW_DEVICE).get(1).getOutput());

    }

    /**
     * Tests dual homing of a host.
     */
    @Test
    public void dualhomedTest() throws Exception {
        StaticPacketTrace traceSuccess = mngr.trace(PACKET_DUAL_HOME, DUAL_HOME_CP_1_1);

        assertNotNull("trace should not be null", traceSuccess);
        assertTrue("Should have 2 output paths", traceSuccess.getCompletePaths().size() == 2);
        assertTrue("Should contain proper path", traceSuccess.getCompletePaths()
                .contains(ImmutableList.of(DUAL_HOME_CP_1_1, DUAL_HOME_CP_1_2, DUAL_HOME_CP_2_1, DUAL_HOME_CP_2_2)));
        assertTrue("Should contain proper path", traceSuccess.getCompletePaths()
                .contains(ImmutableList.of(DUAL_HOME_CP_1_1, DUAL_HOME_CP_1_3, DUAL_HOME_CP_3_1, DUAL_HOME_CP_3_2)));

    }


    private StaticPacketTrace testSuccess(TrafficSelector packet, ConnectPoint in, DeviceId deviceId, ConnectPoint out,
                                          int paths, int outputs) {
        StaticPacketTrace traceSuccess = mngr.trace(packet, in);

        log.info("trace {}", traceSuccess);

        log.info("trace {}", traceSuccess.resultMessage());

        assertNotNull("trace should not be null", traceSuccess);
        assertEquals("Trace should have " + outputs + " output", outputs,
                traceSuccess.getGroupOuputs(deviceId).size());
        assertEquals("Trace should only have " + paths + "output", paths, traceSuccess.getCompletePaths().size());
        assertTrue("Trace should be successful",
                traceSuccess.resultMessage().contains("Reached required destination Host"));
        assertEquals("Incorrect Output CP", out,
                traceSuccess.getGroupOuputs(deviceId).get(0).getOutput());

        return traceSuccess;
    }

    private void testFailure(TrafficSelector packet, ConnectPoint in, DeviceId deviceId) {
        StaticPacketTrace traceFail = mngr.trace(packet, in);

        log.info("trace {}", traceFail.resultMessage());

        assertNotNull("Trace should not be null", traceFail);
        assertNull("Trace should have 0 output", traceFail.getGroupOuputs(deviceId));
    }

    private class TestFlowRuleService extends FlowRuleServiceAdapter {
        @Override
        public Iterable<FlowEntry> getFlowEntriesByState(DeviceId deviceId, FlowEntry.FlowEntryState state) {
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
            } else if (deviceId.equals(HARDWARE_DEVICE)) {
                return ImmutableList.of(HARDWARE_ETH_FLOW_ENTRY, HARDWARE_FLOW_ENTRY);
            } else if (deviceId.equals(SAME_OUTPUT_FLOW_DEVICE)) {
                return ImmutableList.of(SAME_OUTPUT_FLOW_ENTRY);
            } else if (deviceId.equals(ARP_FLOW_DEVICE)) {
                return ImmutableList.of(ARP_FLOW_ENTRY);
            } else if (deviceId.equals(DUAL_LINK_1)) {
                return ImmutableList.of(DUAL_LINK_1_GROUP_FLOW_ENTRY);
            } else if (deviceId.equals(DUAL_LINK_2)) {
                return ImmutableList.of(DUAL_LINK_1_GROUP_FLOW_ENTRY, DUAL_LINK_2_GROUP_FLOW_ENTRY);
            } else if (deviceId.equals(DUAL_LINK_3)) {
                return ImmutableList.of(DUAL_LINK_3_FLOW_ENTRY, DUAL_LINK_3_FLOW_ENTRY_2);
            } else if (deviceId.equals(DEFERRED_1)) {
                return ImmutableList.of(DEFERRED_FLOW_ENTRY, DEFERRED_CLEAR_FLOW_ENTRY);
            } else if (deviceId.equals(HARDWARE_DEVICE_10)) {
                return ImmutableList.of(HARDWARE_10_FLOW_ENTRY, HARDWARE_10_SECOND_FLOW_ENTRY,
                        HARDWARE_10_OUTPUT_FLOW_ENTRY);
            } else if (deviceId.equals(LLDP_FLOW_DEVICE)) {
                return ImmutableList.of(LLDP_FLOW_ENTRY);
            } else if (deviceId.equals(MULTICAST_GROUP_FLOW_DEVICE)) {
                return ImmutableList.of(MULTICAST_GROUP_FLOW_ENTRY);
            } else if (deviceId.equals(NO_BUCKET_DEVICE)) {
                return ImmutableList.of(NO_BUCKET_ENTRY);
            } else if (deviceId.equals(DUAL_HOME_DEVICE_1)) {
                return ImmutableList.of(DUAL_HOME_FLOW_ENTRY);
            } else if (deviceId.equals(DUAL_HOME_DEVICE_2) || deviceId.equals(DUAL_HOME_DEVICE_3)) {
                return ImmutableList.of(DUAL_HOME_OUT_FLOW_ENTRY);
            }
            return ImmutableList.of();
        }
    }

    private class TestDriverService extends DriverServiceAdapter {
        @Override
        public Driver getDriver(DeviceId deviceId) {
            if (deviceId.equals(HARDWARE_DEVICE) || deviceId.equals(HARDWARE_DEVICE_10)) {
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
            } else if (deviceId.equals(DUAL_LINK_1) || deviceId.equals(DUAL_LINK_2)) {
                return ImmutableList.of(DUAL_LINK_GROUP);
            } else if (deviceId.equals(MULTICAST_GROUP_FLOW_DEVICE)) {
                return ImmutableList.of(MULTICAST_GROUP);
            } else if (deviceId.equals(NO_BUCKET_DEVICE)) {
                return ImmutableList.of(NO_BUCKET_GROUP);
            } else if (deviceId.equals(DUAL_HOME_DEVICE_1)) {
                return ImmutableList.of(DUAL_HOME_GROUP);
            }
            return ImmutableList.of();
        }
    }

    private class TestHostService extends HostServiceAdapter {
        @Override
        public Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
            if (connectPoint.equals(TOPO_FLOW_3_OUT_CP)) {
                return ImmutableSet.of(H2);
            } else if (connectPoint.equals(DUAL_LINK_1_CP_2_OUT) || connectPoint.equals(DUAL_LINK_1_CP_3_OUT) ||
                    connectPoint.equals(DUAL_LINK_2_CP_2_OUT) || connectPoint.equals(DUAL_LINK_2_CP_3_OUT)) {
                return ImmutableSet.of();
            }
            if (connectPoint.equals(SINGLE_FLOW_OUT_CP) ||
                    connectPoint.equals(DUAL_FLOW_OUT_CP) ||
                    connectPoint.equals(GROUP_FLOW_OUT_CP) ||
                    connectPoint.equals(HARDWARE_DEVICE_OUT_CP) ||
                    connectPoint.equals(HARDWARE_DEVICE_10_OUT_CP) ||
                    connectPoint.equals(DEFERRED_CP_2_OUT) ||
                    connectPoint.equals(DUAL_LINK_3_CP_3_OUT)) {
                return ImmutableSet.of(H1);
            }
            if (connectPoint.equals(DUAL_HOME_CP_2_2) || connectPoint.equals(DUAL_HOME_CP_3_2)) {
                return ImmutableSet.of(DUAL_HOME_H);
            }
            return ImmutableSet.of();
        }

        @Override
        public Set<Host> getHostsByMac(MacAddress mac) {
            if (mac.equals(H1.mac())) {
                return ImmutableSet.of(H1);
            } else if (mac.equals(H2.mac())) {
                return ImmutableSet.of(H2);
            } else if (mac.equals(DUAL_HOME_H.mac())) {
                return ImmutableSet.of(DUAL_HOME_H);
            }
            return ImmutableSet.of();
        }

        @Override
        public Set<Host> getHostsByIp(IpAddress ip) {
            if ((H1.ipAddresses().contains(ip))) {
                return ImmutableSet.of(H1);
            } else if ((H2.ipAddresses().contains(ip))) {
                return ImmutableSet.of(H2);
            } else if ((DUAL_HOME_H.ipAddresses().contains(ip))) {
                return ImmutableSet.of(DUAL_HOME_H);
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
            } else if (connectPoint.equals(DUAL_LINK_1_CP_2_OUT)) {
                return ImmutableSet.of(DefaultLink.builder()
                        .providerId(ProviderId.NONE)
                        .type(Link.Type.DIRECT)
                        .src(DUAL_LINK_1_CP_2_OUT)
                        .dst(DUAL_LINK_2_CP_1_IN)
                        .build());
            } else if (connectPoint.equals(DUAL_LINK_1_CP_3_OUT)) {
                return ImmutableSet.of(DefaultLink.builder()
                        .providerId(ProviderId.NONE)
                        .type(Link.Type.DIRECT)
                        .src(DUAL_LINK_1_CP_3_OUT)
                        .dst(DUAL_LINK_2_CP_4_IN)
                        .build());
            } else if (connectPoint.equals(DUAL_LINK_2_CP_2_OUT)) {
                return ImmutableSet.of(DefaultLink.builder()
                        .providerId(ProviderId.NONE)
                        .type(Link.Type.DIRECT)
                        .src(DUAL_LINK_2_CP_2_OUT)
                        .dst(DUAL_LINK_3_CP_1_IN)
                        .build());
            } else if (connectPoint.equals(DUAL_LINK_2_CP_3_OUT)) {
                return ImmutableSet.of(DefaultLink.builder()
                        .providerId(ProviderId.NONE)
                        .type(Link.Type.DIRECT)
                        .src(DUAL_LINK_2_CP_3_OUT)
                        .dst(DUAL_LINK_3_CP_2_IN)
                        .build());
            } else if (connectPoint.equals(DUAL_HOME_CP_1_2)) {
                return ImmutableSet.of(DefaultLink.builder()
                        .providerId(ProviderId.NONE)
                        .type(Link.Type.DIRECT)
                        .src(DUAL_HOME_CP_1_2)
                        .dst(DUAL_HOME_CP_2_1)
                        .build());
            } else if (connectPoint.equals(DUAL_HOME_CP_1_3)) {
                return ImmutableSet.of(DefaultLink.builder()
                        .providerId(ProviderId.NONE)
                        .type(Link.Type.DIRECT)
                        .src(DUAL_HOME_CP_1_3)
                        .dst(DUAL_HOME_CP_3_1)
                        .build());
            }
            return ImmutableSet.of();
        }
    }

    private class TestDeviceService extends DeviceServiceAdapter {
        @Override
        public Device getDevice(DeviceId deviceId) {
            if (deviceId.equals(DeviceId.deviceId("nonexistent"))) {
                return null;
            }
            return new DefaultDevice(ProviderId.NONE, DeviceId.deviceId("test"), SWITCH,
                    "test", "test", "test", "test", new ChassisId(),
                    DefaultAnnotations.builder().set("foo", "bar").build());
        }

        @Override
        public Port getPort(ConnectPoint cp) {
            return new DefaultPort(null, cp.port(), true, DefaultAnnotations.builder().build());
        }

        @Override
        public boolean isAvailable(DeviceId deviceId) {
            return !deviceId.equals(OFFLINE_DEVICE);
        }
    }

    private class TestEdgePortService extends EdgePortServiceAdapter {

        @Override
        public boolean isEdgePoint(ConnectPoint point) {
            return point.equals(MULTICAST_OUT_CP) ||
                    point.equals(MULTICAST_OUT_CP_2);
        }
    }

    private class TestRouteService extends RouteServiceAdapter {
        @Override
        public Optional<ResolvedRoute> longestPrefixLookup(IpAddress ip) {
            return Optional.empty();
        }
    }

    private class TestMastershipService extends MastershipServiceAdapter {
        @Override
        public NodeId getMasterFor(DeviceId deviceId) {
            return NodeId.nodeId(MASTER_1);
        }
    }
}