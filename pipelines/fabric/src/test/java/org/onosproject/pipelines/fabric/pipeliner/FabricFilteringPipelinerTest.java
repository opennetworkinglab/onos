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
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.pipelines.fabric.FabricConstants;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for fabric.p4 pipeline filtering control block.
 */
public class FabricFilteringPipelinerTest extends FabricPipelinerTest {

    /**
     * Creates one rule for ingress_port_vlan table and 3 rules for
     * fwd_classifier table (IPv4, IPv6 and MPLS unicast) when
     * the condition is VLAN + MAC.
     */
    @Test
    public void testRouterMacAndVlanFilter() {
        FilteringObjective filteringObjective = buildFilteringObjective(ROUTER_MAC);
        PipelinerTranslationResult result = pipeliner.pipelinerFilter.filter(filteringObjective);

        List<FlowRule> flowRulesInstalled = (List<FlowRule>) result.flowRules();
        List<GroupDescription> groupsInstalled = (List<GroupDescription>) result.groups();

        assertTrue(groupsInstalled.isEmpty());

        // in port vlan flow rule
        FlowRule actualFlowRule = flowRulesInstalled.get(0);
        FlowRule flowRuleExpected =
                buildExpectedVlanInPortRule(PORT_1,
                                            VlanId.NONE,
                                            VLAN_100,
                                            FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN);
        assertTrue(flowRuleExpected.exactMatch(actualFlowRule));

        // forwarding classifier ipv4
        actualFlowRule = flowRulesInstalled.get(1);
        flowRuleExpected = buildExpectedFwdClassifierRule(PORT_1,
                                                          ROUTER_MAC,
                                                          Ethernet.TYPE_IPV4,
                                                          FWD_IPV4_UNICAST);
        assertTrue(flowRuleExpected.exactMatch(actualFlowRule));

        // forwarding classifier ipv6
        actualFlowRule = flowRulesInstalled.get(2);
        flowRuleExpected = buildExpectedFwdClassifierRule(PORT_1,
                                                          ROUTER_MAC,
                                                          Ethernet.TYPE_IPV6,
                                                          FWD_IPV6_UNICAST);
        assertTrue(flowRuleExpected.exactMatch(actualFlowRule));

        // forwarding classifier mpls
        actualFlowRule = flowRulesInstalled.get(3);
        flowRuleExpected = buildExpectedFwdClassifierRule(PORT_1,
                                                          ROUTER_MAC,
                                                          Ethernet.MPLS_UNICAST,
                                                          FWD_MPLS);
        assertTrue(flowRuleExpected.exactMatch(actualFlowRule));
    }

    /**
     * Creates one rule for ingress_port_vlan table and one rule for
     * fwd_classifier table (IPv4 multicast) when the condition is ipv4
     * multicast mac address.
     */
    @Test
    public void testIpv4MulticastFwdClass() {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .pushVlan()
                .setVlanId(VLAN_100)
                .build();
        FilteringObjective filteringObjective = DefaultFilteringObjective.builder()
                .permit()
                .withPriority(PRIORITY)
                .withKey(Criteria.matchInPort(PORT_1))
                .addCondition(Criteria.matchEthDst(MacAddress.IPV4_MULTICAST))
                .addCondition(Criteria.matchVlanId(VlanId.NONE))
                .withMeta(treatment)
                .fromApp(APP_ID)
                .makePermanent()
                .add();
        PipelinerTranslationResult result = pipeliner.pipelinerFilter.filter(filteringObjective);
        List<FlowRule> flowRulesInstalled = (List<FlowRule>) result.flowRules();
        List<GroupDescription> groupsInstalled = (List<GroupDescription>) result.groups();

        assertTrue(groupsInstalled.isEmpty());

        // in port vlan flow rule
        FlowRule actualFlowRule = flowRulesInstalled.get(0);
        FlowRule flowRuleExpected =
                buildExpectedVlanInPortRule(PORT_1,
                                            VlanId.NONE,
                                            VLAN_100,
                                            FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN);
        assertTrue(flowRuleExpected.exactMatch(actualFlowRule));

        // forwarding classifier
        actualFlowRule = flowRulesInstalled.get(1);
        flowRuleExpected = buildExpectedFwdClassifierRule(PORT_1,
                                                          MacAddress.IPV4_MULTICAST,
                                                          Ethernet.TYPE_IPV4,
                                                          FWD_IPV4_MULTICAST);
        assertTrue(flowRuleExpected.exactMatch(actualFlowRule));
    }

    /**
     * Creates one rule for ingress_port_vlan table and one rule for
     * fwd_classifier table (IPv6 multicast) when the condition is ipv6
     * multicast mac address.
     */
    @Test
    public void testIpv6MulticastFwdClass() {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .pushVlan()
                .setVlanId(VLAN_100)
                .build();
        FilteringObjective filteringObjective = DefaultFilteringObjective.builder()
                .permit()
                .withPriority(PRIORITY)
                .withKey(Criteria.matchInPort(PORT_1))
                .addCondition(Criteria.matchEthDst(MacAddress.IPV6_MULTICAST))
                .addCondition(Criteria.matchVlanId(VlanId.NONE))
                .withMeta(treatment)
                .fromApp(APP_ID)
                .makePermanent()
                .add();
        PipelinerTranslationResult result = pipeliner.pipelinerFilter.filter(filteringObjective);
        List<FlowRule> flowRulesInstalled = (List<FlowRule>) result.flowRules();
        List<GroupDescription> groupsInstalled = (List<GroupDescription>) result.groups();

        assertTrue(groupsInstalled.isEmpty());

        // in port vlan flow rule
        FlowRule actualFlowRule = flowRulesInstalled.get(0);
        FlowRule flowRuleExpected =
                buildExpectedVlanInPortRule(PORT_1,
                                            VlanId.NONE,
                                            VLAN_100,
                                            FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN);
        assertTrue(flowRuleExpected.exactMatch(actualFlowRule));

        // forwarding classifier
        actualFlowRule = flowRulesInstalled.get(1);
        flowRuleExpected = buildExpectedFwdClassifierRule(PORT_1,
                                                          MacAddress.IPV6_MULTICAST,
                                                          Ethernet.TYPE_IPV6,
                                                          FWD_IPV6_MULTICAST);
        assertTrue(flowRuleExpected.exactMatch(actualFlowRule));
    }

    /**
     * Creates only one rule for ingress_port_vlan table if there is no condition
     * of destination mac address.
     * The packet will be handled by bridging table by default.
     */
    @Test
    public void testFwdBridging() {
        FilteringObjective filteringObjective = buildFilteringObjective(null);
        PipelinerTranslationResult result = pipeliner.pipelinerFilter.filter(filteringObjective);
        List<FlowRule> flowRulesInstalled = (List<FlowRule>) result.flowRules();
        List<GroupDescription> groupsInstalled = (List<GroupDescription>) result.groups();

        assertTrue(groupsInstalled.isEmpty());

        // in port vlan flow rule
        FlowRule actualFlowRule = flowRulesInstalled.get(0);
        FlowRule flowRuleExpected =
                buildExpectedVlanInPortRule(PORT_1,
                                            VlanId.NONE,
                                            VLAN_100,
                                            FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN);
        assertTrue(flowRuleExpected.exactMatch(actualFlowRule));

        // No rules in forwarding classifier, will do default action: set fwd type to bridging
    }

    /**
     * We supports only PERMIT type of filtering objective.
     */
    @Test
    public void testUnsupportedObjective() {
        FilteringObjective filteringObjective = DefaultFilteringObjective.builder()
                .deny()
                .withKey(Criteria.matchInPort(PORT_1))
                .addCondition(Criteria.matchVlanId(VLAN_100))
                .fromApp(APP_ID)
                .makePermanent()
                .add();

        PipelinerTranslationResult result = pipeliner.pipelinerFilter.filter(filteringObjective);
        pipeliner.pipelinerFilter.filter(filteringObjective);

        List<FlowRule> flowRulesInstalled = (List<FlowRule>) result.flowRules();
        List<GroupDescription> groupsInstalled = (List<GroupDescription>) result.groups();

        assertTrue(flowRulesInstalled.isEmpty());
        assertTrue(groupsInstalled.isEmpty());

        assertTrue(result.error().isPresent());
        ObjectiveError error = result.error().get();
        assertEquals(ObjectiveError.UNSUPPORTED, error);
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

        PipelinerTranslationResult result = pipeliner.pipelinerFilter.filter(filteringObjective);
        pipeliner.pipelinerFilter.filter(filteringObjective);

        assertTrue(result.error().isPresent());
        ObjectiveError error = result.error().get();
        assertEquals(ObjectiveError.BADPARAMS, error);

        // Filtering objective should use in_port as key
        filteringObjective = DefaultFilteringObjective.builder()
                .permit()
                .withKey(Criteria.matchEthDst(ROUTER_MAC))
                .addCondition(Criteria.matchVlanId(VLAN_100))
                .withMeta(DefaultTrafficTreatment.emptyTreatment())
                .fromApp(APP_ID)
                .makePermanent()
                .add();

        result = pipeliner.pipelinerFilter.filter(filteringObjective);
        pipeliner.pipelinerFilter.filter(filteringObjective);

        assertTrue(result.error().isPresent());
        error = result.error().get();
        assertEquals(ObjectiveError.BADPARAMS, error);
    }

    /* Utilities */

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
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        if (vlanId == null || vlanId.equals(VlanId.NONE)) {
            selector.matchPi(VLAN_INVALID);
            treatment.pushVlan();
            treatment.setVlanId(internalVlan);
        } else {
            selector.matchPi(VLAN_VALID);
            selector.matchVlanId(vlanId);
        }

        return DefaultFlowRule.builder()
                .withPriority(PRIORITY)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .fromApp(APP_ID)
                .forDevice(DEVICE_ID)
                .makePermanent()
                .forTable(tableId)
                .build();
    }

    private FlowRule buildExpectedFwdClassifierRule(PortNumber inPort,
                                                    MacAddress dstMac,
                                                    short ethType,
                                                    byte fwdClass) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthDst(dstMac)
                .matchInPort(inPort)
                .matchEthType(ethType)
                .build();
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
