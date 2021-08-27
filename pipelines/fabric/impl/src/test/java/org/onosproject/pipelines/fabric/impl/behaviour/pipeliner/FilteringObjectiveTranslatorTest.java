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

package org.onosproject.pipelines.fabric.impl.behaviour.pipeliner;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TableId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.pipelines.fabric.FabricConstants;

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.DEFAULT_PW_TRANSPORT_VLAN;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.DEFAULT_VLAN;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.EDGE_PORT;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.ETH_TYPE_EXACT_MASK;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.FWD_IPV4_ROUTING;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.FWD_IPV6_ROUTING;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.FWD_MPLS;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.INFRA_PORT;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.ONE;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.PAIR_PORT;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.PORT_TYPE_EDGE;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.PORT_TYPE_INFRA;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.ZERO;

/**
 * Test cases for fabric.p4 pipeline filtering control block.
 */
public class FilteringObjectiveTranslatorTest extends BaseObjectiveTranslatorTest {

    private FilteringObjectiveTranslator translator;

    @Before
    public void setup() {
        super.doSetup();
        translator = new FilteringObjectiveTranslator(DEVICE_ID, capabilitiesHashed);
    }

    /**
     * Creates one rule for ingress_port_vlan table and 3 rules for
     * fwd_classifier table (IPv4, IPv6 and MPLS unicast) when the condition is
     * VLAN + MAC.
     */
    @Test
    public void testRouterMacAndVlanFilter() throws FabricPipelinerException {
        FilteringObjective filteringObjective = buildFilteringObjective(ROUTER_MAC, EDGE_PORT);
        ObjectiveTranslation actualTranslation = translator.translate(filteringObjective);
        Collection<FlowRule> expectedFlowRules = Lists.newArrayList();
        // in port vlan flow rule
        expectedFlowRules.add(buildExpectedVlanInPortRule(
                PORT_1,
                VlanId.NONE,
                VlanId.NONE,
                VLAN_100,
                PORT_TYPE_EDGE,
                FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN));

        // forwarding classifier ipv4
        expectedFlowRules.addAll(buildExpectedFwdClassifierRule(
                PORT_1,
                ROUTER_MAC,
                null,
                Ethernet.TYPE_IPV4,
                FWD_IPV4_ROUTING));

        // forwarding classifier ipv6
        expectedFlowRules.addAll(buildExpectedFwdClassifierRule(
                PORT_1,
                ROUTER_MAC,
                null,
                Ethernet.TYPE_IPV6,
                FWD_IPV6_ROUTING));

        // forwarding classifier mpls
        expectedFlowRules.addAll(buildExpectedFwdClassifierRule(
                PORT_1,
                ROUTER_MAC,
                null,
                Ethernet.MPLS_UNICAST,
                FWD_MPLS));

//        ObjectiveTranslation.Builder expectedTranslationBuilder = ObjectiveTranslation.builder()
//                .addFlowRule(inportFlowRuleExpected);
//        for (FlowRule flowRule : classifierV4FlowRuleExpected) {
//            expectedTranslationBuilder.addFlowRule(flowRule);
//        }
//        for (FlowRule flowRule : classifierV6FlowRuleExpected) {
//            expectedTranslationBuilder.addFlowRule(flowRule);
//        }
//        for (FlowRule flowRule : classifierMplsFlowRuleExpected) {
//            expectedTranslationBuilder.addFlowRule(flowRule);
//        }
        ObjectiveTranslation expectedTranslation = buildExpectedTranslation(expectedFlowRules);

        assertEquals(expectedTranslation, actualTranslation);
    }

    /**
     * Creates one rule for ingress_port_vlan table and one rule for
     * fwd_classifier table (IPv4 multicast) when the condition is ipv4
     * multicast mac address.
     */
    @Test
    public void testIpv4MulticastFwdClass() throws FabricPipelinerException {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .pushVlan()
                .setVlanId(VLAN_100)
                .writeMetadata(EDGE_PORT, 0xffffffffffffffffL)
                .build();
        FilteringObjective filteringObjective = DefaultFilteringObjective.builder()
                .permit()
                .withPriority(PRIORITY)
                .withKey(Criteria.matchInPort(PORT_1))
                .addCondition(Criteria.matchEthDstMasked(MacAddress.IPV4_MULTICAST, MacAddress.IPV4_MULTICAST_MASK))
                .addCondition(Criteria.matchVlanId(VlanId.NONE))
                .withMeta(treatment)
                .fromApp(APP_ID)
                .makePermanent()
                .add();
        ObjectiveTranslation actualTranslation = translator.translate(filteringObjective);
        List<FlowRule> expectedFlowRules = Lists.newArrayList();
        // in port vlan flow rule
        expectedFlowRules.add(buildExpectedVlanInPortRule(
                PORT_1,
                VlanId.NONE,
                VlanId.NONE,
                VLAN_100,
                PORT_TYPE_EDGE,
                FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN));

        // forwarding classifier
        expectedFlowRules.addAll(buildExpectedFwdClassifierRule(
                PORT_1,
                MacAddress.IPV4_MULTICAST,
                MacAddress.IPV4_MULTICAST_MASK,
                Ethernet.TYPE_IPV4,
                FWD_IPV4_ROUTING));

        ObjectiveTranslation expectedTranslation = buildExpectedTranslation(expectedFlowRules);

        assertEquals(expectedTranslation, actualTranslation);
    }

    /**
     * Creates one rule for ingress_port_vlan table and one rule for
     * fwd_classifier table (IPv6 multicast) when the condition is ipv6
     * multicast mac address.
     */
    @Test
    public void testIpv6MulticastFwdClass() throws FabricPipelinerException {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .pushVlan()
                .setVlanId(VLAN_100)
                .writeMetadata(EDGE_PORT, 0xffffffffffffffffL)
                .build();
        FilteringObjective filteringObjective = DefaultFilteringObjective.builder()
                .permit()
                .withPriority(PRIORITY)
                .withKey(Criteria.matchInPort(PORT_1))
                .addCondition(Criteria.matchEthDstMasked(MacAddress.IPV6_MULTICAST, MacAddress.IPV6_MULTICAST_MASK))
                .addCondition(Criteria.matchVlanId(VlanId.NONE))
                .withMeta(treatment)
                .fromApp(APP_ID)
                .makePermanent()
                .add();
        ObjectiveTranslation actualTranslation = translator.translate(filteringObjective);
        Collection<FlowRule> flowRules = Lists.newArrayList();
        // in port vlan flow rule
        flowRules.add(buildExpectedVlanInPortRule(
                PORT_1,
                VlanId.NONE,
                VlanId.NONE,
                VLAN_100,
                PORT_TYPE_EDGE,
                FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN));

        flowRules.addAll(buildExpectedFwdClassifierRule(
                PORT_1,
                MacAddress.IPV6_MULTICAST,
                MacAddress.IPV6_MULTICAST_MASK,
                Ethernet.TYPE_IPV6,
                FWD_IPV6_ROUTING));

        ObjectiveTranslation expectedTranslation = buildExpectedTranslation(flowRules);

        assertEquals(expectedTranslation, actualTranslation);
    }

    /**
     * Creates only one rule for ingress_port_vlan table if there is no
     * condition of destination mac address. The packet will be handled by
     * bridging table by default.
     */
    @Test
    public void testFwdBridging() throws Exception {
        FilteringObjective filteringObjective = buildFilteringObjective(null, EDGE_PORT);
        ObjectiveTranslation actualTranslation = translator.translate(filteringObjective);

        // in port vlan flow rule
        FlowRule flowRuleExpected = buildExpectedVlanInPortRule(
                PORT_1,
                VlanId.NONE,
                VlanId.NONE,
                VLAN_100,
                PORT_TYPE_EDGE,
                FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN);

        // No rules in forwarding classifier, will do default action: set fwd type to bridging

        ObjectiveTranslation expectedTranslation = ObjectiveTranslation.builder()
                .addFlowRule(flowRuleExpected)
                .build();

        assertEquals(expectedTranslation, actualTranslation);
    }

    /**
     * Test DENY objective.
     */
    @Test
    public void testDenyObjective() throws FabricPipelinerException {
        FilteringObjective filteringObjective = DefaultFilteringObjective.builder()
                .deny()
                .withKey(Criteria.matchInPort(PORT_1))
                .addCondition(Criteria.matchVlanId(VlanId.NONE))
                .fromApp(APP_ID)
                .makePermanent()
                .withPriority(PRIORITY)
                .add();

        ObjectiveTranslation actualTranslation = translator.translate(filteringObjective);

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                .matchInPort(PORT_1)
                .matchPi(buildPiCriterionVlan(null, null));
        PiAction piAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_FILTERING_DENY)
                .build();
        FlowRule expectedFlowRule = DefaultFlowRule.builder()
                .withPriority(PRIORITY)
                .withSelector(selector.build())
                .withTreatment(DefaultTrafficTreatment.builder()
                                       .piTableAction(piAction).build())
                .fromApp(APP_ID)
                .forDevice(DEVICE_ID)
                .makePermanent()
                .forTable(FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN)
                .build();

        ObjectiveTranslation expectedTranslation = ObjectiveTranslation.builder()
                .addFlowRule(expectedFlowRule)
                .build();

        assertEquals(expectedTranslation, actualTranslation);

    }

    /**
     * Test double VLAN pop filtering objective Creates one rule for
     * ingress_port_vlan table and 3 rules for fwd_classifier table (IPv4, IPv6
     * and MPLS unicast) when the condition is MAC + VLAN + INNER_VLAN.
     */
    @Test
    public void testPopVlan() throws FabricPipelinerException {
        FilteringObjective filteringObjective = DefaultFilteringObjective.builder()
                .withKey(Criteria.matchInPort(PORT_1))
                .addCondition(Criteria.matchEthDst(ROUTER_MAC))
                .addCondition(Criteria.matchVlanId(VLAN_100))
                .addCondition(Criteria.matchInnerVlanId(VLAN_200))
                .withPriority(PRIORITY)
                .fromApp(APP_ID)
                .withMeta(DefaultTrafficTreatment.builder()
                        .popVlan()
                        .writeMetadata(EDGE_PORT, 0xffffffffffffffffL)
                        .build())
                .permit()
                .add();
        ObjectiveTranslation actualTranslation = translator.translate(filteringObjective);
        Collection<FlowRule> expectedFlowRules = Lists.newArrayList();
        // Ingress port vlan rule
        expectedFlowRules.add(buildExpectedVlanInPortRule(
                PORT_1, VLAN_100, VLAN_200, VlanId.NONE, PORT_TYPE_EDGE,
                FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN));
        // Forwarding classifier rules (ipv6, ipv4, mpls)
        expectedFlowRules.addAll(buildExpectedFwdClassifierRule(
                PORT_1, ROUTER_MAC, null, Ethernet.TYPE_IPV4,
                FWD_IPV4_ROUTING));
        expectedFlowRules.addAll(buildExpectedFwdClassifierRule(
                PORT_1, ROUTER_MAC, null, Ethernet.TYPE_IPV6,
                FWD_IPV6_ROUTING));
        expectedFlowRules.addAll(buildExpectedFwdClassifierRule(
                PORT_1, ROUTER_MAC, null, Ethernet.MPLS_UNICAST,
                FWD_MPLS));
        ObjectiveTranslation expectedTranslation = buildExpectedTranslation(expectedFlowRules);

        assertEquals(expectedTranslation, actualTranslation);
    }

    /**
     * Incorrect filtering key or filtering conditions test.
     */
    @Test
    public void badParamTest() {
        // Filtering objective should contains filtering key
        FilteringObjective filteringObjective = DefaultFilteringObjective.builder()
                .permit()
                .addCondition(Criteria.matchVlanId(VLAN_100))
                .fromApp(APP_ID)
                .makePermanent()
                .add();

        ObjectiveTranslation result1 = translator.translate(filteringObjective);
        assertError(ObjectiveError.BADPARAMS, result1);

        // Filtering objective should use in_port as key
        filteringObjective = DefaultFilteringObjective.builder()
                .permit()
                .withKey(Criteria.matchEthDst(ROUTER_MAC))
                .addCondition(Criteria.matchVlanId(VLAN_100))
                .withMeta(DefaultTrafficTreatment.emptyTreatment())
                .fromApp(APP_ID)
                .makePermanent()
                .add();

        ObjectiveTranslation result2 = translator.translate(filteringObjective);
        assertError(ObjectiveError.BADPARAMS, result2);
    }

    /**
     * Test port update scenarios for filtering objective. Creates only one rule for
     * ingress_port_vlan table.
     */
    @Test
    public void testIsPortUpdate() throws FabricPipelinerException {
        // Tagged port scenario
        FilteringObjective filteringObjective = DefaultFilteringObjective.builder()
                .withKey(Criteria.matchInPort(PORT_1))
                .addCondition(Criteria.matchEthDst(ROUTER_MAC))
                .addCondition(Criteria.matchVlanId(VLAN_100))
                .withPriority(PRIORITY)
                .fromApp(APP_ID)
                .withMeta(DefaultTrafficTreatment.builder()
                        .writeMetadata(10, 0xffffffffffffffffL)
                        .build())
                .permit()
                .add();
        ObjectiveTranslation actualTranslation = translator.translate(filteringObjective);
        Collection<FlowRule> expectedFlowRules = Lists.newArrayList();
        // Ingress port vlan rule
        expectedFlowRules.add(buildExpectedVlanInPortRule(
                PORT_1, VLAN_100, null, VlanId.NONE, PORT_TYPE_EDGE,
                FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN));
        ObjectiveTranslation expectedTranslation = buildExpectedTranslation(expectedFlowRules);
        assertEquals(expectedTranslation, actualTranslation);

        // Untagged port scenario
        filteringObjective = DefaultFilteringObjective.builder()
                .withKey(Criteria.matchInPort(PORT_1))
                .addCondition(Criteria.matchEthDst(ROUTER_MAC))
                .addCondition(Criteria.matchVlanId(VlanId.NONE))
                .withPriority(PRIORITY)
                .fromApp(APP_ID)
                .withMeta(DefaultTrafficTreatment.builder()
                        .pushVlan()
                        .setVlanId(VLAN_200)
                        .writeMetadata(10, 0xffffffffffffffffL)
                        .build())
                .permit()
                .add();
        actualTranslation = translator.translate(filteringObjective);
        expectedFlowRules = Lists.newArrayList();
        // Ingress port vlan rule
        expectedFlowRules.add(buildExpectedVlanInPortRule(
                PORT_1, VlanId.NONE, null, VLAN_200, PORT_TYPE_EDGE,
                FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN));
        expectedTranslation = buildExpectedTranslation(expectedFlowRules);
        assertEquals(expectedTranslation, actualTranslation);
    }

    /**
     * Test no more ports scenario for filtering objective.
     */
    @Test
    public void testNoMorePorts() throws FabricPipelinerException {
        // Tagged port scenario
        FilteringObjective filteringObjective = DefaultFilteringObjective.builder()
                .withKey(Criteria.matchInPort(PORT_1))
                .addCondition(Criteria.matchEthDst(ROUTER_MAC))
                .addCondition(Criteria.matchVlanId(VLAN_100))
                .withPriority(PRIORITY)
                .fromApp(APP_ID)
                .withMeta(DefaultTrafficTreatment.builder()
                        .writeMetadata(EDGE_PORT, 0xffffffffffffffffL)
                        .wipeDeferred()
                        .build())
                .permit()
                .add();
        ObjectiveTranslation actualTranslation = translator.translate(filteringObjective);
        Collection<FlowRule> expectedFlowRules = Lists.newArrayList();
        // Ingress port vlan rule
        expectedFlowRules.add(buildExpectedVlanInPortRule(
                PORT_1, VLAN_100, null, VlanId.NONE, PORT_TYPE_EDGE,
                FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN));
        // forwarding classifier ipv4
        expectedFlowRules.addAll(buildExpectedFwdClassifierRule(
                PORT_1,
                ROUTER_MAC,
                null,
                Ethernet.TYPE_IPV4,
                FWD_IPV4_ROUTING));
        // forwarding classifier ipv6
        expectedFlowRules.addAll(buildExpectedFwdClassifierRule(
                PORT_1,
                ROUTER_MAC,
                null,
                Ethernet.TYPE_IPV6,
                FWD_IPV6_ROUTING));
        // forwarding classifier mpls
        expectedFlowRules.addAll(buildExpectedFwdClassifierRule(
                PORT_1,
                ROUTER_MAC,
                null,
                Ethernet.MPLS_UNICAST,
                FWD_MPLS));

        ObjectiveTranslation expectedTranslation = buildExpectedTranslation(expectedFlowRules);
        assertEquals(expectedTranslation, actualTranslation);
    }

    /**
     * Test is infra port scenarios for filtering objective.
     */
    @Test
    public void testIsInfraPort() throws FabricPipelinerException {
        // PW transport vlan
        FilteringObjective filteringObjective = DefaultFilteringObjective.builder()
                .withKey(Criteria.matchInPort(PORT_1))
                .addCondition(Criteria.matchEthDst(ROUTER_MAC))
                .addCondition(Criteria.matchVlanId(VlanId.vlanId((short) DEFAULT_PW_TRANSPORT_VLAN)))
                .withPriority(PRIORITY)
                .withMeta(DefaultTrafficTreatment.builder()
                        .writeMetadata(INFRA_PORT, 0xffffffffffffffffL)
                        .build())
                .fromApp(APP_ID)
                .permit()
                .add();
        ObjectiveTranslation actualTranslation = translator.translate(filteringObjective);

        Collection<FlowRule> expectedFlowRules = Lists.newArrayList();
        expectedFlowRules.add(buildExpectedVlanInPortRule(
                PORT_1, VlanId.vlanId((short) DEFAULT_PW_TRANSPORT_VLAN), null, VlanId.NONE,
                PORT_TYPE_INFRA, FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN));
        expectedFlowRules.addAll(buildExpectedFwdClassifierRule(
                PORT_1,
                ROUTER_MAC,
                null,
                Ethernet.TYPE_IPV4,
                FWD_IPV4_ROUTING));
        expectedFlowRules.addAll(buildExpectedFwdClassifierRule(
                PORT_1,
                ROUTER_MAC,
                null,
                Ethernet.TYPE_IPV6,
                FWD_IPV6_ROUTING));
        expectedFlowRules.addAll(buildExpectedFwdClassifierRule(
                PORT_1,
                ROUTER_MAC,
                null,
                Ethernet.MPLS_UNICAST,
                FWD_MPLS));

        ObjectiveTranslation expectedTranslation = buildExpectedTranslation(expectedFlowRules);
        assertEquals(expectedTranslation, actualTranslation);

        // Untagged port scenario
        filteringObjective = DefaultFilteringObjective.builder()
                .withKey(Criteria.matchInPort(PORT_1))
                .addCondition(Criteria.matchEthDst(ROUTER_MAC))
                .addCondition(Criteria.matchVlanId(VlanId.NONE))
                .withPriority(PRIORITY)
                .fromApp(APP_ID)
                .withMeta(DefaultTrafficTreatment.builder()
                        .pushVlan()
                        .setVlanId(VlanId.vlanId((short) DEFAULT_VLAN))
                        .writeMetadata(INFRA_PORT, 0xffffffffffffffffL)
                        .build())
                .permit()
                .add();
        actualTranslation = translator.translate(filteringObjective);
        expectedFlowRules = Lists.newArrayList();
        expectedFlowRules.add(buildExpectedVlanInPortRule(
                PORT_1, null, null, VlanId.vlanId((short) DEFAULT_VLAN),
                PORT_TYPE_INFRA, FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN));
        expectedFlowRules.addAll(buildExpectedFwdClassifierRule(
                PORT_1,
                ROUTER_MAC,
                null,
                Ethernet.TYPE_IPV4,
                FWD_IPV4_ROUTING));
        expectedFlowRules.addAll(buildExpectedFwdClassifierRule(
                PORT_1,
                ROUTER_MAC,
                null,
                Ethernet.TYPE_IPV6,
                FWD_IPV6_ROUTING));
        expectedFlowRules.addAll(buildExpectedFwdClassifierRule(
                PORT_1,
                ROUTER_MAC,
                null,
                Ethernet.MPLS_UNICAST,
                FWD_MPLS));

        expectedTranslation = buildExpectedTranslation(expectedFlowRules);
        assertEquals(expectedTranslation, actualTranslation);
    }

    /**
     * Test is pair port scenarios for filtering objective.
     */
    @Test
    public void testIsPairPort() throws FabricPipelinerException {
        // Only pair port flag
        FilteringObjective filteringObjective = DefaultFilteringObjective.builder()
                .withKey(Criteria.matchInPort(PORT_1))
                .addCondition(Criteria.matchEthDst(ROUTER_MAC))
                .addCondition(Criteria.matchVlanId(VLAN_100))
                .withPriority(PRIORITY)
                .withMeta(DefaultTrafficTreatment.builder()
                        .writeMetadata(PAIR_PORT, 0xffffffffffffffffL)
                        .build())
                .fromApp(APP_ID)
                .permit()
                .add();
        ObjectiveTranslation actualTranslation = translator.translate(filteringObjective);

        Collection<FlowRule> expectedFlowRules = Lists.newArrayList();
        expectedFlowRules.add(buildExpectedVlanInPortRule(
                PORT_1, VLAN_100, null, VlanId.NONE,
                PORT_TYPE_INFRA, FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN));
        expectedFlowRules.addAll(buildExpectedFwdClassifierRule(
                PORT_1,
                ROUTER_MAC,
                null,
                Ethernet.TYPE_IPV4,
                FWD_IPV4_ROUTING));
        expectedFlowRules.addAll(buildExpectedFwdClassifierRule(
                PORT_1,
                ROUTER_MAC,
                null,
                Ethernet.TYPE_IPV6,
                FWD_IPV6_ROUTING));
        expectedFlowRules.addAll(buildExpectedFwdClassifierRule(
                PORT_1,
                ROUTER_MAC,
                null,
                Ethernet.MPLS_UNICAST,
                FWD_MPLS));

        ObjectiveTranslation expectedTranslation = buildExpectedTranslation(expectedFlowRules);
        assertEquals(expectedTranslation, actualTranslation);

        // Pair port and config update flags
        filteringObjective = DefaultFilteringObjective.builder()
                .withKey(Criteria.matchInPort(PORT_1))
                .addCondition(Criteria.matchEthDst(ROUTER_MAC))
                .addCondition(Criteria.matchVlanId(VLAN_100))
                .withPriority(PRIORITY)
                .fromApp(APP_ID)
                .withMeta(DefaultTrafficTreatment.builder()
                        .writeMetadata(6, 0xffffffffffffffffL)
                        .build())
                .permit()
                .add();

        actualTranslation = translator.translate(filteringObjective);
        expectedFlowRules = Lists.newArrayList();
        // Ingress port vlan rule
        expectedFlowRules.add(buildExpectedVlanInPortRule(
                PORT_1, VLAN_100, null, VlanId.NONE, PORT_TYPE_INFRA,
                FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN));
        expectedTranslation = buildExpectedTranslation(expectedFlowRules);
        assertEquals(expectedTranslation, actualTranslation);
    }

    /* Utilities */

    private void assertError(ObjectiveError error, ObjectiveTranslation actualTranslation) {
        ObjectiveTranslation expectedTranslation = ObjectiveTranslation.ofError(error);
        assertEquals(expectedTranslation, actualTranslation);
    }

    private FilteringObjective buildFilteringObjective(MacAddress dstMac, long portType) {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .pushVlan()
                .setVlanId(VLAN_100)
                .writeMetadata(portType, 0xffffffffffffffffL)
                .build();
        DefaultFilteringObjective.Builder builder = DefaultFilteringObjective.builder()
                .permit()
                .withPriority(PRIORITY)
                .withKey(Criteria.matchInPort(PORT_1));
        if (dstMac != null) {
            builder.addCondition(Criteria.matchEthDst(dstMac));
        }

        builder.addCondition(Criteria.matchVlanId(VlanId.NONE))
                .withMeta(treatment)
                .fromApp(APP_ID)
                .makePermanent();
        return builder.add();
    }

    private FlowRule buildExpectedVlanInPortRule(PortNumber inPort,
                                                 VlanId vlanId,
                                                 VlanId innerVlanId,
                                                 VlanId internalVlan,
                                                 byte portType,
                                                 TableId tableId) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                .matchInPort(inPort);
        PiAction piAction;
        selector.matchPi(buildPiCriterionVlan(vlanId, innerVlanId));
        if (!vlanValid(vlanId)) {
            piAction = PiAction.builder()
                    .withId(FabricConstants.FABRIC_INGRESS_FILTERING_PERMIT_WITH_INTERNAL_VLAN)
                    .withParameter(new PiActionParam(FabricConstants.VLAN_ID, internalVlan.toShort()))
                    .withParameter(new PiActionParam(FabricConstants.PORT_TYPE, portType))
                    .build();
        } else {
            selector.matchVlanId(vlanId);
            if (vlanValid(innerVlanId)) {
                selector.matchInnerVlanId(innerVlanId);
            }
            piAction = PiAction.builder()
                    .withId(FabricConstants.FABRIC_INGRESS_FILTERING_PERMIT)
                    .withParameter(new PiActionParam(FabricConstants.PORT_TYPE, portType))
                    .build();
        }

        return DefaultFlowRule.builder()
                .withPriority(PRIORITY)
                .withSelector(selector.build())
                .withTreatment(DefaultTrafficTreatment.builder()
                                       .piTableAction(piAction).build())
                .fromApp(APP_ID)
                .forDevice(DEVICE_ID)
                .makePermanent()
                .forTable(tableId)
                .build();
    }

    private boolean vlanValid(VlanId vlanId) {
        return (vlanId != null && !vlanId.equals(VlanId.NONE));
    }

    private PiCriterion buildPiCriterionVlan(VlanId vlanId,
                                             VlanId innerVlanId) {
        PiCriterion.Builder piCriterionBuilder = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_VLAN_IS_VALID,
                            vlanValid(vlanId) ? ONE : ZERO);
        return piCriterionBuilder.build();
    }

    private Collection<FlowRule> buildExpectedFwdClassifierRule(PortNumber inPort,
                                                                MacAddress dstMac,
                                                                MacAddress dstMacMask,
                                                                short ethType,
                                                                byte fwdClass) {
        PiActionParam classParam = new PiActionParam(FabricConstants.FWD_TYPE,
                                                     ImmutableByteSequence.copyFrom(fwdClass));
        PiAction fwdClassifierAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_FILTERING_SET_FORWARDING_TYPE)
                .withParameter(classParam)
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .piTableAction(fwdClassifierAction)
                .build();

        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder()
                .matchInPort(inPort);
        if (dstMacMask != null) {
            sbuilder.matchEthDstMasked(dstMac, dstMacMask);
        } else {
            sbuilder.matchEthDstMasked(dstMac, MacAddress.EXACT_MASK);
        }
        // Special case for MPLS UNICAST forwarding, need to build 2 rules for MPLS+IPv4 and MPLS+IPv6
        if (ethType == Ethernet.MPLS_UNICAST) {
            return buildExpectedFwdClassifierRulesMpls(fwdClassifierAction, treatment, sbuilder);
        }
        sbuilder.matchPi(PiCriterion.builder()
                                 .matchExact(FabricConstants.HDR_IP_ETH_TYPE, ethType)
                                 .build());
        TrafficSelector selector = sbuilder.build();
        return List.of(DefaultFlowRule.builder()
                               .withPriority(PRIORITY)
                               .withSelector(selector)
                               .withTreatment(treatment)
                               .fromApp(APP_ID)
                               .forDevice(DEVICE_ID)
                               .makePermanent()
                               .forTable(FabricConstants.FABRIC_INGRESS_FILTERING_FWD_CLASSIFIER)
                               .build());
    }

    private Collection<FlowRule> buildExpectedFwdClassifierRulesMpls(PiAction fwdClassifierAction,
                                                                     TrafficTreatment treatment,
                                                                     TrafficSelector.Builder selectorBuilder) {

        Collection<FlowRule> flowRules = Lists.newArrayList();
        TrafficSelector selectorIpv4 = selectorBuilder
                .add(PiCriterion.builder()
                             .matchTernary(FabricConstants.HDR_ETH_TYPE, Ethernet.MPLS_UNICAST, ETH_TYPE_EXACT_MASK)
                             .matchExact(FabricConstants.HDR_IP_ETH_TYPE, Ethernet.TYPE_IPV4)
                             .build())
                .build();
        TrafficSelector selectorIpv6 = selectorBuilder
                .add(PiCriterion.builder()
                             .matchTernary(FabricConstants.HDR_ETH_TYPE, Ethernet.MPLS_UNICAST, ETH_TYPE_EXACT_MASK)
                             .matchExact(FabricConstants.HDR_IP_ETH_TYPE, Ethernet.TYPE_IPV6)
                             .build())
                .build();
        flowRules.add(DefaultFlowRule.builder()
                              .withPriority(PRIORITY + 1)
                              .withSelector(selectorIpv4)
                              .withTreatment(treatment)
                              .fromApp(APP_ID)
                              .forDevice(DEVICE_ID)
                              .makePermanent()
                              .forTable(FabricConstants.FABRIC_INGRESS_FILTERING_FWD_CLASSIFIER)
                              .build());
        flowRules.add(DefaultFlowRule.builder()
                              .withPriority(PRIORITY + 1)
                              .withSelector(selectorIpv6)
                              .withTreatment(treatment)
                              .fromApp(APP_ID)
                              .forDevice(DEVICE_ID)
                              .makePermanent()
                              .forTable(FabricConstants.FABRIC_INGRESS_FILTERING_FWD_CLASSIFIER)
                              .build());
        return flowRules;
    }

    private ObjectiveTranslation buildExpectedTranslation(Collection<FlowRule> flowRules)
            throws FabricPipelinerException {
        ObjectiveTranslation.Builder expectedTranslationBuilder = ObjectiveTranslation.builder();
        for (FlowRule flowRule : flowRules) {
            expectedTranslationBuilder.addFlowRule(flowRule);
        }
        return expectedTranslationBuilder.build();
    }
}
