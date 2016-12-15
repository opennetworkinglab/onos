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
package org.onosproject.drivers.microsemi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.EthType.EtherType;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowEntry.FlowEntryState;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.Criterion.Type;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class EA1000FlowRuleProgrammableTest {
    EA1000FlowRuleProgrammable frProgramable;

    @Before
    public void setUp() throws Exception {
        frProgramable = new EA1000FlowRuleProgrammable();
        frProgramable.setHandler(new MockEa1000DriverHandler());
        assertNotNull(frProgramable.handler().data().deviceId());
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetFlowEntries() {
        //From MockNetconfSession sample of MseaSaFiltering
        Collection<FlowEntry> flowEntries = frProgramable.getFlowEntries();

        assertNotNull(flowEntries);
        //There will be 12 flow entries
        // 2 for IP Src Address filtering
        // 2 for EVC 7 - one each port
        // 8 for EVC 8 - one for host port, 7 on optics port because of ceVlanMap 12:14,20:22,25
        assertEquals(12, flowEntries.size());

        //Test the first Flow Entry
        Iterator<FlowEntry> feIter = flowEntries.iterator();
        while (feIter.hasNext()) {
            FlowEntry fe = feIter.next();
            assertTrue(fe.isPermanent());
            assertEquals(EA1000FlowRuleProgrammable.PRIORITY_DEFAULT, fe.priority());

            Set<Criterion> criteria = fe.selector().criteria();
            IPCriterion ipCr = null;
            PortNumber port = null;
            for (Criterion cr:criteria.toArray(new Criterion[criteria.size()])) {
                if (cr.type() == Criterion.Type.IPV4_SRC) {
                    ipCr = (IPCriterion) cr;
                } else if (cr.type() == Criterion.Type.IN_PORT) {
                    port = ((PortCriterion) cr).port();
                } else if (cr.type() == Criterion.Type.VLAN_VID) {
                    VlanId vid = ((VlanIdCriterion) cr).vlanId();
                } else {
                    fail("Unexpected Criterion type: " + cr.type().toString());
                }
            }
            if (ipCr != null && (port == null || port.toLong() != 0L)) {
                fail("Port number not equal 0 when IP Src Address filter is present");
            }

            List<Instruction> instructions = fe.treatment().allInstructions();

            if (fe.tableId() == 1) {
                //Note that in MockNetconf session 10.10.10.10/16 was entered
                //but it has been corrected to the following by the OF implementation
                assertEquals("10.10.0.0/16", ipCr.ip().toString());
                assertEquals(FlowEntryState.ADDED, fe.state());
            } else if (fe.tableId() == 2) {
                //Likewise 20.30.40.50 has been truncated because of the 18 bit mask
                assertEquals("20.30.0.0/18", ipCr.ip().toString());
                assertEquals(FlowEntryState.ADDED, fe.state());
            } else if (fe.tableId() == 7 || fe.tableId() == 8) {
                // 7 and 8 are EVC entries - 2 elements - IN_PORT and VLAN_ID
                assertEquals(2, fe.selector().criteria().size());
                //In MockNetconfSession we're rigged it so that the last two chars of the
                //flow id is the same as the VlanId
                short vlanId = ((VlanIdCriterion) fe.selector().getCriterion(Type.VLAN_VID)).vlanId().toShort();
                long flowId = fe.id().id();
                String flowIdStr = String.valueOf(flowId).substring(String.valueOf(flowId).length() - 2);
                assertEquals(flowIdStr, String.valueOf(vlanId));
                if (((PortCriterion) fe.selector().getCriterion(Type.IN_PORT)).port().toLong() == 1L) {
                    assertEquals(Instruction.Type.L2MODIFICATION, instructions.get(0).type());
                }
            } else {
                fail("Unexpected Flow Entry Rule " + fe.tableId());
            }
        }
    }

    @Test
    public void testSetFlowEntries() {
        Criterion matchInPort = Criteria.matchInPort(PortNumber.portNumber(0));

        TrafficTreatment treatmentDrop = DefaultTrafficTreatment.builder().drop().build();

        Collection<FlowRule> frAddedList = new HashSet<FlowRule>();

        FlowRule fr4 = new DefaultFlowRule.Builder()
            .forDevice(frProgramable.handler().data().deviceId())
            .forTable(4)
            .withSelector(DefaultTrafficSelector.builder()
                    .matchIPSrc(IpPrefix.valueOf("192.168.60.0/22"))
                    .add(matchInPort).build())
            .withTreatment(treatmentDrop)
            .fromApp(new DefaultApplicationId(4, "Filter4"))
            .makePermanent()
            .withPriority(EA1000FlowRuleProgrammable.PRIORITY_DEFAULT)
            .build();
        frAddedList.add(fr4);

        FlowRule fr5 = new DefaultFlowRule.Builder()
                .forDevice(frProgramable.handler().data().deviceId())
                .forTable(5)
                .withSelector(DefaultTrafficSelector.builder()
                        .matchIPSrc(IpPrefix.valueOf("192.168.50.0/23"))
                        .add(matchInPort).build())
                .withTreatment(treatmentDrop)
                .withCookie(Long.valueOf("5e0000abaa2772", 16))
                .makePermanent()
                .withPriority(EA1000FlowRuleProgrammable.PRIORITY_DEFAULT)
                .build();
        frAddedList.add(fr5);

        //Add in some EVCs - especially with complex ceVlanMaps
        FlowRule frEvc1Vid19P0 = new DefaultFlowRule.Builder()
                .forDevice(frProgramable.handler().data().deviceId())
                .forTable(1)
                .withSelector(DefaultTrafficSelector.builder()
                        .matchInPort(PortNumber.portNumber(0L))
                        .matchVlanId(VlanId.vlanId((short) 19))
                        .build())
                .withTreatment(DefaultTrafficTreatment.builder()
                        .popVlan()
                        .build())
                .withCookie(Long.valueOf("1e0000abaa0019", 16))
                .makePermanent()
                .withPriority(EA1000FlowRuleProgrammable.PRIORITY_DEFAULT)
                .build();
        frAddedList.add(frEvc1Vid19P0);

        FlowRule frEvc1Vid20P0 = new DefaultFlowRule.Builder()
                .forDevice(frProgramable.handler().data().deviceId())
                .forTable(1)
                .withSelector(DefaultTrafficSelector.builder()
                        .matchInPort(PortNumber.portNumber(0L))
                        .matchVlanId(VlanId.vlanId((short) 20))
                        .build())
                .withTreatment(DefaultTrafficTreatment.builder()
                        .popVlan()
                        .build())
                .withCookie(Long.valueOf("1e0000abaa0020", 16))
                .makePermanent()
                .withPriority(EA1000FlowRuleProgrammable.PRIORITY_DEFAULT)
                .build();
        frAddedList.add(frEvc1Vid20P0);

        FlowRule frEvc1Vid21P1 = new DefaultFlowRule.Builder()
                .forDevice(frProgramable.handler().data().deviceId())
                .forTable(1)
                .withSelector(DefaultTrafficSelector.builder()
                        .matchInPort(PortNumber.portNumber(1L))
                        .matchVlanId(VlanId.vlanId((short) 21))
                        .build())
                .withTreatment(DefaultTrafficTreatment.builder()
                        .setVlanId(VlanId.vlanId((short) 250))
                        .pushVlan(EtherType.QINQ.ethType())
                        .build())
                .withCookie(Long.valueOf("1e0000abaa0121", 16))
                .makePermanent()
                .withPriority(EA1000FlowRuleProgrammable.PRIORITY_DEFAULT)
                .build();
        frAddedList.add(frEvc1Vid21P1);

        FlowRule frEvc1Vid22P1 = new DefaultFlowRule.Builder()
                .forDevice(frProgramable.handler().data().deviceId())
                .forTable(1)
                .withSelector(DefaultTrafficSelector.builder()
                        .matchInPort(PortNumber.portNumber(1L))
                        .matchVlanId(VlanId.vlanId((short) 22))
                        .build())
                .withTreatment(DefaultTrafficTreatment.builder()
                        .setVlanId(VlanId.vlanId((short) 250))
                        .pushVlan(EtherType.QINQ.ethType())
                        .build())
                .withCookie(Long.valueOf("1e0000abaa0122", 16))
                .makePermanent()
                .withPriority(EA1000FlowRuleProgrammable.PRIORITY_DEFAULT)
                .build();
        frAddedList.add(frEvc1Vid22P1);

        Collection<FlowRule> returnedFrList = frProgramable.applyFlowRules(frAddedList);

        assertNotNull(returnedFrList);
        assertEquals(6, returnedFrList.size());

        //Test the scenario like in FlowRuleManager$InternalFlowRuleProviderService.pushFlowMetricsInternal()
        Map<FlowEntry, FlowEntry> storedRules = Maps.newHashMap();
        frAddedList.forEach(f -> storedRules.put(new DefaultFlowEntry(f), new DefaultFlowEntry(f)));
        List<FlowEntry> feList = Lists.newArrayList();
        returnedFrList.forEach(f -> feList.add(new DefaultFlowEntry(f)));

        for (FlowEntry rule : feList) {
            FlowEntry fer = storedRules.remove(rule);
            assertNotNull(fer);
            assertTrue(fer.exactMatch(rule));
        }

        for (FlowRule fr:returnedFrList.toArray(new FlowRule[2])) {
            if (fr.tableId() == 4) {
                assertEquals("IPV4_SRC:192.168.60.0/22",
                        ((IPCriterion) fr.selector().getCriterion(Type.IPV4_SRC)).toString());
            } else if (fr.tableId() == 5) {
                assertEquals("IPV4_SRC:192.168.50.0/23",
                        ((IPCriterion) fr.selector().getCriterion(Type.IPV4_SRC)).toString());
                assertEquals(Long.valueOf("5e0000abaa2772", 16), fr.id().id());
            } else if (fr.tableId() == 1) {
                //TODO add in tests
            } else {
                fail("Unexpected flow rule " + fr.tableId() + " in test");
            }
        }
    }

    @Test
    public void testRemoveFlowEntries() {
        TrafficSelector.Builder tsBuilder = DefaultTrafficSelector.builder();
        Criterion matchInPort0 = Criteria.matchInPort(PortNumber.portNumber(0));
        Criterion matchInPort1 = Criteria.matchInPort(PortNumber.portNumber(1));

        TrafficTreatment.Builder trDropBuilder = DefaultTrafficTreatment.builder();
        TrafficTreatment treatmentDrop = trDropBuilder.drop().build();

        Collection<FlowRule> frRemoveList = new HashSet<FlowRule>();
        ApplicationId app = new DefaultApplicationId(1, "org.onosproject.rest");

        Criterion matchIpSrc1 = Criteria.matchIPSrc(IpPrefix.valueOf("10.10.10.10/16"));
        TrafficSelector selector1 = tsBuilder.add(matchIpSrc1).add(matchInPort0).build();
        FlowRule.Builder frBuilder1 = new DefaultFlowRule.Builder();
        frBuilder1.forDevice(frProgramable.handler().data().deviceId())
                .withSelector(selector1)
                .withTreatment(treatmentDrop)
                .forTable(2)
                .fromApp(app)
                .makePermanent()
                .withPriority(EA1000FlowRuleProgrammable.PRIORITY_DEFAULT);
        frRemoveList.add(frBuilder1.build());

        Criterion matchIpSrc2 = Criteria.matchIPSrc(IpPrefix.valueOf("10.30.10.10/16"));
        TrafficSelector selector2 = tsBuilder.add(matchIpSrc2).add(matchInPort0).build();
        FlowRule.Builder frBuilder2 = new DefaultFlowRule.Builder();
        frBuilder2.forDevice(frProgramable.handler().data().deviceId())
                .withSelector(selector2)
                .withTreatment(treatmentDrop)
                .forTable(3)
                .fromApp(app)
                .makePermanent()
                .withPriority(EA1000FlowRuleProgrammable.PRIORITY_DEFAULT)
                .build();
        frRemoveList.add(frBuilder2.build());


        TrafficTreatment.Builder trVlanPopBuilder = DefaultTrafficTreatment.builder();
        TrafficTreatment treatmentVlanPop = trVlanPopBuilder.popVlan().build();


        Criterion matchVlan710 = Criteria.matchVlanId(VlanId.vlanId((short) 710));
        TrafficSelector selector3 = DefaultTrafficSelector.builder().add(matchVlan710).add(matchInPort1).build();
        FlowRule.Builder frBuilder3 = new DefaultFlowRule.Builder();
        frBuilder3.forDevice(frProgramable.handler().data().deviceId())
            .withSelector(selector3)
            .withTreatment(treatmentVlanPop)
            .forTable(7)
            .fromApp(app)
            .makePermanent()
            .withPriority(EA1000FlowRuleProgrammable.PRIORITY_DEFAULT)
            .build();
        frRemoveList.add(frBuilder3.build());


        Criterion matchVlan101 = Criteria.matchVlanId(VlanId.vlanId((short) 101));
        TrafficSelector selector4 = DefaultTrafficSelector.builder().add(matchVlan101).add(matchInPort1).build();
        FlowRule.Builder frBuilder4 = new DefaultFlowRule.Builder();
        frBuilder4.forDevice(frProgramable.handler().data().deviceId())
            .withSelector(selector4)
            .withTreatment(treatmentVlanPop)
            .forTable(1)
            .fromApp(app)
            .makePermanent()
            .withPriority(EA1000FlowRuleProgrammable.PRIORITY_DEFAULT)
            .build();
        frRemoveList.add(frBuilder4.build());

        Criterion matchVlan102 = Criteria.matchVlanId(VlanId.vlanId((short) 102));
        TrafficSelector selector5 = DefaultTrafficSelector.builder().add(matchVlan102).add(matchInPort0).build();
        FlowRule.Builder frBuilder5 = new DefaultFlowRule.Builder();
        frBuilder5.forDevice(frProgramable.handler().data().deviceId())
            .withSelector(selector5)
            .withTreatment(treatmentVlanPop)
            .forTable(1)
            .fromApp(app)
            .makePermanent()
            .withPriority(EA1000FlowRuleProgrammable.PRIORITY_DEFAULT)
            .build();
        frRemoveList.add(frBuilder5.build());

        Collection<FlowRule> removedFrList = frProgramable.removeFlowRules(frRemoveList);
        assertNotNull(removedFrList);
        assertEquals(5, removedFrList.size());

        for (FlowRule frRemoved:removedFrList) {
            assertNotNull(frRemoved);
        }
    }
}
