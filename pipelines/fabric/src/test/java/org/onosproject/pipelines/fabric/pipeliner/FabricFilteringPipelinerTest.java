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
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.pipelines.fabric.FabricConstants;

import static org.junit.Assert.assertEquals;

/**
 * Test cases for fabric.p4 pipeline filtering control block.
 */
public class FabricFilteringPipelinerTest extends FabricPipelinerTest {

    private FilteringObjectiveTranslator translator;

    @Before
    public void setup() {
        super.doSetup();
        translator = new FilteringObjectiveTranslator(DEVICE_ID, capabilitiesHashed);
    }

    /**
     * Creates one rule for ingress_port_vlan table and 3 rules for
     * fwd_classifier table (IPv4, IPv6 and MPLS unicast) when
     * the condition is VLAN + MAC.
     */
    @Test
    public void testRouterMacAndVlanFilter() throws FabricPipelinerException {
        FilteringObjective filteringObjective = buildFilteringObjective(ROUTER_MAC);
        ObjectiveTranslation actualTranslation = translator.translate(filteringObjective);

        // in port vlan flow rule
        FlowRule inportFlowRuleExpected =
                buildExpectedVlanInPortRule(PORT_1,
                                            VlanId.NONE,
                                            VLAN_100,
                                            FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN);

        // forwarding classifier ipv4
        FlowRule classifierV4FlowRuleExpected = buildExpectedFwdClassifierRule(PORT_1,
                                                          ROUTER_MAC,
                                                          null,
                                                          Ethernet.TYPE_IPV4,
                                                          FilteringObjectiveTranslator.FWD_IPV4_ROUTING);

        // forwarding classifier ipv6
        FlowRule classifierV6FlowRuleExpected = buildExpectedFwdClassifierRule(PORT_1,
                                                          ROUTER_MAC,
                                                          null,
                                                          Ethernet.TYPE_IPV6,
                                                          FilteringObjectiveTranslator.FWD_IPV6_ROUTING);

        // forwarding classifier mpls
        FlowRule classifierMplsFlowRuleExpected = buildExpectedFwdClassifierRule(PORT_1,
                                                          ROUTER_MAC,
                                                          null,
                                                          Ethernet.MPLS_UNICAST,
                                                          FilteringObjectiveTranslator.FWD_MPLS);

        ObjectiveTranslation expectedTranslation = ObjectiveTranslation.builder()
                .addFlowRule(inportFlowRuleExpected)
                .addFlowRule(classifierV4FlowRuleExpected)
                .addFlowRule(classifierV6FlowRuleExpected)
                .addFlowRule(classifierMplsFlowRuleExpected)
                .build();

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

        // in port vlan flow rule
        FlowRule inportFlowRuleExpected =
                buildExpectedVlanInPortRule(PORT_1,
                                            VlanId.NONE,
                                            VLAN_100,
                                            FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN);

        // forwarding classifier
        FlowRule classifierFlowRuleExpected = buildExpectedFwdClassifierRule(PORT_1,
                                                          MacAddress.IPV4_MULTICAST,
                                                          MacAddress.IPV4_MULTICAST_MASK,
                                                          Ethernet.TYPE_IPV4,
                                                          FilteringObjectiveTranslator.FWD_IPV4_ROUTING);

        ObjectiveTranslation expectedTranslation = ObjectiveTranslation.builder()
                .addFlowRule(inportFlowRuleExpected)
                .addFlowRule(classifierFlowRuleExpected)
                .build();

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

        // in port vlan flow rule
        FlowRule inportFlowRuleExpected =
                buildExpectedVlanInPortRule(PORT_1,
                                            VlanId.NONE,
                                            VLAN_100,
                                            FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN);

        FlowRule classifierFlowRuleExpected = buildExpectedFwdClassifierRule(PORT_1,
                                                          MacAddress.IPV6_MULTICAST,
                                                          MacAddress.IPV6_MULTICAST_MASK,
                                                          Ethernet.TYPE_IPV6,
                                                          FilteringObjectiveTranslator.FWD_IPV6_ROUTING);

        ObjectiveTranslation expectedTranslation = ObjectiveTranslation.builder()
                .addFlowRule(inportFlowRuleExpected)
                .addFlowRule(classifierFlowRuleExpected)
                .build();

        assertEquals(expectedTranslation, actualTranslation);
    }

    /**
     * Creates only one rule for ingress_port_vlan table if there is no condition
     * of destination mac address.
     * The packet will be handled by bridging table by default.
     */
    @Test
    public void testFwdBridging() throws Exception {
        FilteringObjective filteringObjective = buildFilteringObjective(null);
        ObjectiveTranslation actualTranslation = translator.translate(filteringObjective);

        // in port vlan flow rule
        FlowRule flowRuleExpected =
                buildExpectedVlanInPortRule(PORT_1,
                                            VlanId.NONE,
                                            VLAN_100,
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
                .matchPi(VLAN_INVALID);
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

    /* Utilities */

    private void assertError(ObjectiveError error, ObjectiveTranslation actualTranslation) {
        ObjectiveTranslation expectedTranslation = ObjectiveTranslation.ofError(error);
        assertEquals(expectedTranslation, actualTranslation);
    }

    private FilteringObjective buildFilteringObjective(MacAddress dstMac) {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .pushVlan()
                .setVlanId(VLAN_100)
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

    private FlowRule buildExpectedVlanInPortRule(PortNumber inPort, VlanId vlanId,
                                                 VlanId internalVlan,
                                                 TableId tableId) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                .matchInPort(inPort);
        PiAction piAction;
        if (vlanId == null || vlanId.equals(VlanId.NONE)) {
            selector.matchPi(VLAN_INVALID);
            piAction = PiAction.builder()
                    .withId(FabricConstants.FABRIC_INGRESS_FILTERING_PERMIT_WITH_INTERNAL_VLAN)
                    .withParameter(new PiActionParam(
                            FabricConstants.VLAN_ID, internalVlan.toShort()))
                    .build();
        } else {
            selector.matchPi(VLAN_VALID);
            selector.matchVlanId(vlanId);
            piAction = PiAction.builder()
                    .withId(FabricConstants.FABRIC_INGRESS_FILTERING_PERMIT)
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

    private FlowRule buildExpectedFwdClassifierRule(PortNumber inPort,
                                                    MacAddress dstMac,
                                                    MacAddress dstMacMask,
                                                    short ethType,
                                                    byte fwdClass) {
        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder()
                .matchInPort(inPort)
                .matchEthType(ethType);
        if (dstMacMask != null) {
            sbuilder.matchEthDstMasked(dstMac, dstMacMask);
        } else {
            sbuilder.matchEthDstMasked(dstMac, MacAddress.EXACT_MASK);
        }
        TrafficSelector selector = sbuilder.build();

        PiActionParam classParam = new PiActionParam(FabricConstants.FWD_TYPE,
                                                     ImmutableByteSequence.copyFrom(fwdClass));
        PiAction fwdClassifierAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_FILTERING_SET_FORWARDING_TYPE)
                .withParameter(classParam)
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .piTableAction(fwdClassifierAction)
                .build();

        return DefaultFlowRule.builder()
                .withPriority(PRIORITY)
                .withSelector(selector)
                .withTreatment(treatment)
                .fromApp(APP_ID)
                .forDevice(DEVICE_ID)
                .makePermanent()
                .forTable(FabricConstants.FABRIC_INGRESS_FILTERING_FWD_CLASSIFIER)
                .build();
    }
}
