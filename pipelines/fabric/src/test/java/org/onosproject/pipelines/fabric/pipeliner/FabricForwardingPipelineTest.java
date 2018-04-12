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

package org.onosproject.pipelines.fabric.pipeliner;

import org.junit.Ignore;
import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.pipelines.fabric.FabricConstants;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for fabric.p4 pipeline forwarding control block.
 */
public class FabricForwardingPipelineTest extends FabricPipelinerTest {

    /**
     * Test versatile flag of forwarding objective with ARP match.
     */
    @Test
    public void testAclArp() {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .wipeDeferred()
                .punt()
                .build();
        // ARP
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_ARP)
                .build();
        ForwardingObjective fwd = DefaultForwardingObjective.builder()
                .withSelector(selector)
                .withPriority(PRIORITY)
                .fromApp(APP_ID)
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withTreatment(treatment)
                .add();

        PipelinerTranslationResult result = pipeliner.pipelinerForward.forward(fwd);

        List<FlowRule> flowRulesInstalled = (List<FlowRule>) result.flowRules();
        List<GroupDescription> groupsInstalled = (List<GroupDescription>) result.groups();
        assertEquals(1, flowRulesInstalled.size());
        assertTrue(groupsInstalled.isEmpty());

        FlowRule actualFlowRule = flowRulesInstalled.get(0);
        FlowRule expectedFlowRule = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .forTable(FabricConstants.FABRIC_INGRESS_FORWARDING_ACL)
                .withPriority(PRIORITY)
                .makePermanent()
                .withSelector(selector)
                .withTreatment(treatment)
                .fromApp(APP_ID)
                .build();

        assertTrue(expectedFlowRule.exactMatch(actualFlowRule));
    }

    /**
     * Test versatile flag of forwarding objective with DHCP match.
     */
    @Test
    public void testAclDhcp() {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .wipeDeferred()
                .punt()
                .build();
        // DHCP
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT))
                .matchUdpDst(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
                .build();
        ForwardingObjective fwd = DefaultForwardingObjective.builder()
                .withSelector(selector)
                .withPriority(PRIORITY)
                .fromApp(APP_ID)
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withTreatment(treatment)
                .add();

        PipelinerTranslationResult result = pipeliner.pipelinerForward.forward(fwd);

        List<FlowRule> flowRulesInstalled = (List<FlowRule>) result.flowRules();
        List<GroupDescription> groupsInstalled = (List<GroupDescription>) result.groups();
        assertEquals(1, flowRulesInstalled.size());
        assertTrue(groupsInstalled.isEmpty());

        FlowRule actualFlowRule = flowRulesInstalled.get(0);
        FlowRule expectedFlowRule = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .forTable(FabricConstants.FABRIC_INGRESS_FORWARDING_ACL)
                .withPriority(PRIORITY)
                .makePermanent()
                .withSelector(selector)
                .withTreatment(treatment)
                .fromApp(APP_ID)
                .build();

        assertTrue(expectedFlowRule.exactMatch(actualFlowRule));
    }

    /**
     * Test programming L2 unicast rule to bridging table.
     */
    @Test
    public void testL2Unicast() {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchVlanId(VLAN_100)
                .matchEthDst(HOST_MAC)
                .build();
        testSpecificForward(FabricConstants.FABRIC_INGRESS_FORWARDING_BRIDGING,
                            selector, selector, NEXT_ID_1);
    }

    @Test
    public void testL2Broadcast() {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchVlanId(VLAN_100)
                .build();
        testSpecificForward(FabricConstants.FABRIC_INGRESS_FORWARDING_BRIDGING,
                            selector, selector, NEXT_ID_1);
    }

    @Test
    public void testIPv4Unicast() {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(IPV4_UNICAST_ADDR)
                .build();
        TrafficSelector expectedSelector = DefaultTrafficSelector.builder()
                .matchIPDst(IPV4_UNICAST_ADDR)
                .build();
        testSpecificForward(FabricConstants.FABRIC_INGRESS_FORWARDING_UNICAST_V4,
                            expectedSelector, selector, NEXT_ID_1);
    }

    @Test
    public void testIPv4UnicastWithNoNextId() {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(IPV4_UNICAST_ADDR)
                .build();
        TrafficSelector expectedSelector = DefaultTrafficSelector.builder()
                .matchIPDst(IPV4_UNICAST_ADDR)
                .build();
        testSpecificForward(FabricConstants.FABRIC_INGRESS_FORWARDING_UNICAST_V4,
                            expectedSelector, selector, null);
    }

    @Test
    @Ignore
    public void testIPv4Multicast() {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchVlanId(VLAN_100)
                .matchIPDst(IPV4_MCAST_ADDR)
                .build();
        TrafficSelector expectedSelector = DefaultTrafficSelector.builder()
                .matchIPDst(IPV4_MCAST_ADDR)
                .build();
        testSpecificForward(FabricConstants.FABRIC_INGRESS_FORWARDING_UNICAST_V4,
                            expectedSelector, selector, NEXT_ID_1);
    }

    @Test
    @Ignore
    public void testIPv6Unicast() {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV6)
                .matchIPDst(IPV6_UNICAST_ADDR)
                .build();
        TrafficSelector expectedSelector = DefaultTrafficSelector.builder()
                .matchIPDst(IPV6_UNICAST_ADDR)
                .build();
        testSpecificForward(FabricConstants.FABRIC_INGRESS_FORWARDING_UNICAST_V6,
                            expectedSelector, selector, NEXT_ID_1);

    }

    @Test
    @Ignore
    public void testIPv6Multicast() {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV6)
                .matchVlanId(VLAN_100)
                .matchIPDst(IPV6_MCAST_ADDR)
                .build();
        TrafficSelector expectedSelector = DefaultTrafficSelector.builder()
                .matchIPDst(IPV6_MCAST_ADDR)
                .build();
        testSpecificForward(FabricConstants.FABRIC_INGRESS_FORWARDING_UNICAST_V6,
                            expectedSelector, selector, NEXT_ID_1);
    }

    @Test
    public void testMpls() {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.MPLS_UNICAST)
                .matchMplsLabel(MPLS_10)
                .matchMplsBos(true)
                .build();
        TrafficSelector expectedSelector = DefaultTrafficSelector.builder()
                .matchMplsLabel(MPLS_10)
                .build();

        PiActionParam nextIdParam = new PiActionParam(FabricConstants.NEXT_ID,
                                                      ImmutableByteSequence.copyFrom(NEXT_ID_1.byteValue()));
        PiAction setNextIdAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_FORWARDING_POP_MPLS_AND_NEXT)
                .withParameter(nextIdParam)
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .piTableAction(setNextIdAction)
                .build();
        testSpecificForward(FabricConstants.FABRIC_INGRESS_FORWARDING_MPLS,
                            expectedSelector, selector, NEXT_ID_1, treatment);
    }

    private void testSpecificForward(PiTableId expectedTableId, TrafficSelector expectedSelector,
                                     TrafficSelector selector, Integer nextId) {
        TrafficTreatment setNextIdTreatment;
        if (nextId == null) {
            // Ref: RoutingRulePopulator.java->revokeIpRuleForRouter
            setNextIdTreatment = DefaultTrafficTreatment.builder().build();
        } else {
            PiActionParam nextIdParam = new PiActionParam(FabricConstants.NEXT_ID,
                                                          ImmutableByteSequence.copyFrom(nextId.byteValue()));
            PiAction setNextIdAction = PiAction.builder()
                    .withId(FabricConstants.FABRIC_INGRESS_FORWARDING_SET_NEXT_ID)
                    .withParameter(nextIdParam)
                    .build();
            setNextIdTreatment = DefaultTrafficTreatment.builder()
                    .piTableAction(setNextIdAction)
                    .build();
        }

        testSpecificForward(expectedTableId, expectedSelector, selector, nextId, setNextIdTreatment);

    }

    private void testSpecificForward(PiTableId expectedTableId, TrafficSelector expectedSelector,
                                     TrafficSelector selector, Integer nextId, TrafficTreatment treatment) {
        ForwardingObjective.Builder fwd = DefaultForwardingObjective.builder()
                .withSelector(selector)
                .withPriority(PRIORITY)
                .fromApp(APP_ID)
                .makePermanent()
                .withTreatment(treatment)
                .withFlag(ForwardingObjective.Flag.SPECIFIC);

        if (nextId != null) {
            fwd.nextStep(nextId);
        }

        PipelinerTranslationResult result = pipeliner.pipelinerForward.forward(fwd.add());

        List<FlowRule> flowRulesInstalled = (List<FlowRule>) result.flowRules();
        List<GroupDescription> groupsInstalled = (List<GroupDescription>) result.groups();
        assertEquals(1, flowRulesInstalled.size());
        assertTrue(groupsInstalled.isEmpty());

        FlowRule actualFlowRule = flowRulesInstalled.get(0);

        FlowRule expectedFlowRule = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .forTable(expectedTableId)
                .withPriority(PRIORITY)
                .makePermanent()
                .withSelector(expectedSelector)
                .withTreatment(treatment)
                .fromApp(APP_ID)
                .build();

        assertTrue(expectedFlowRule.exactMatch(actualFlowRule));
    }
}
