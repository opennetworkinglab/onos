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

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiActionProfileGroupId;
import org.onosproject.net.pi.runtime.PiGroupKey;
import org.onosproject.pipelines.fabric.FabricConstants;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.outputPort;

/**
 * Test cases for fabric.p4 pipeline next control block.
 */
public class NextObjectiveTranslatorTest extends BaseObjectiveTranslatorTest {

    private NextObjectiveTranslator translatorHashed;
    private NextObjectiveTranslator translatorSimple;

    private FlowRule vlanMetaFlowRule;
    private FlowRule mplsFlowRule;

    @Before
    public void setup() {
        super.doSetup();

        translatorHashed = new NextObjectiveTranslator(DEVICE_ID, capabilitiesHashed);
        translatorSimple = new NextObjectiveTranslator(DEVICE_ID, capabilitiesSimple);

        PiCriterion nextIdCriterion = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_NEXT_ID, NEXT_ID_1)
                .build();
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchPi(nextIdCriterion)
                .build();
        PiAction piAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_PRE_NEXT_SET_VLAN)
                .withParameter(new PiActionParam(FabricConstants.VLAN_ID, VLAN_100.toShort()))
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .piTableAction(piAction)
                .build();
        vlanMetaFlowRule = DefaultFlowRule.builder()
                .withSelector(selector)
                .withTreatment(treatment)
                .forTable(FabricConstants.FABRIC_INGRESS_PRE_NEXT_NEXT_VLAN)
                .makePermanent()
                // FIXME: currently next objective doesn't support priority, ignore this
                .withPriority(0)
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .build();
        piAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_PRE_NEXT_SET_MPLS_LABEL)
                .withParameter(new PiActionParam(FabricConstants.LABEL, MPLS_10.toInt()))
                .build();
        treatment = DefaultTrafficTreatment.builder()
                .piTableAction(piAction)
                .build();
        mplsFlowRule = DefaultFlowRule.builder()
                .withSelector(selector)
                .withTreatment(treatment)
                .forTable(FabricConstants.FABRIC_INGRESS_PRE_NEXT_NEXT_MPLS)
                .makePermanent()
                // FIXME: currently next objective doesn't support priority, ignore this
                .withPriority(0)
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .build();
    }

    /**
     * Test program mpls ecmp output group for Hashed table.
     */
    @Test
    public void testMplsHashedOutput() throws Exception {
        TrafficTreatment treatment1 = DefaultTrafficTreatment.builder()
                .setEthSrc(ROUTER_MAC)
                .setEthDst(SPINE1_MAC)
                .pushMpls()
                .copyTtlOut()
                .setMpls(MPLS_10)
                .popVlan()
                .setOutput(PORT_1)
                .build();
        TrafficTreatment treatment2 = DefaultTrafficTreatment.builder()
                .setEthSrc(ROUTER_MAC)
                .setEthDst(SPINE2_MAC)
                .pushMpls()
                .copyTtlOut()
                .setMpls(MPLS_10)
                .popVlan()
                .setOutput(PORT_2)
                .build();

        NextObjective nextObjective = DefaultNextObjective.builder()
                .withId(NEXT_ID_1)
                .withPriority(PRIORITY)
                .withMeta(VLAN_META)
                .addTreatment(treatment1)
                .addTreatment(treatment2)
                .withType(NextObjective.Type.HASHED)
                .makePermanent()
                .fromApp(APP_ID)
                .add();

        ObjectiveTranslation actualTranslation = translatorHashed.doTranslate(nextObjective);

        // Expected hashed table flow rule.
        PiCriterion nextIdCriterion = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_NEXT_ID, NEXT_ID_1)
                .build();
        TrafficSelector nextIdSelector = DefaultTrafficSelector.builder()
                .matchPi(nextIdCriterion)
                .build();
        PiActionProfileGroupId actionGroupId = PiActionProfileGroupId.of(NEXT_ID_1);
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .piTableAction(actionGroupId)
                .build();
        FlowRule expectedFlowRule = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .makePermanent()
                // FIXME: currently next objective doesn't support priority, ignore this
                .withPriority(0)
                .forTable(FabricConstants.FABRIC_INGRESS_NEXT_HASHED)
                .withSelector(nextIdSelector)
                .withTreatment(treatment)
                .build();

        // First egress rule - port1
        PortNumber outPort = outputPort(treatment1);
        PiCriterion egressVlanTableMatch = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_EG_PORT, outPort.toLong())
                .build();
        TrafficSelector selectorForEgressVlan = DefaultTrafficSelector.builder()
                .matchPi(egressVlanTableMatch)
                .matchVlanId(VLAN_100)
                .build();
        PiAction piActionForEgressVlan = PiAction.builder()
                .withId(FabricConstants.FABRIC_EGRESS_EGRESS_NEXT_POP_VLAN)
                .build();
        TrafficTreatment treatmentForEgressVlan = DefaultTrafficTreatment.builder()
                .piTableAction(piActionForEgressVlan)
                .build();
        FlowRule expectedEgressVlanPopRule1 = DefaultFlowRule.builder()
                .withSelector(selectorForEgressVlan)
                .withTreatment(treatmentForEgressVlan)
                .forTable(FabricConstants.FABRIC_EGRESS_EGRESS_NEXT_EGRESS_VLAN)
                .makePermanent()
                .withPriority(nextObjective.priority())
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .build();

        // Second egress rule - port2
        outPort = outputPort(treatment2);
        egressVlanTableMatch = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_EG_PORT, outPort.toLong())
                .build();
        selectorForEgressVlan = DefaultTrafficSelector.builder()
                .matchPi(egressVlanTableMatch)
                .matchVlanId(VLAN_100)
                .build();
        FlowRule expectedEgressVlanPopRule2 = DefaultFlowRule.builder()
                .withSelector(selectorForEgressVlan)
                .withTreatment(treatmentForEgressVlan)
                .forTable(FabricConstants.FABRIC_EGRESS_EGRESS_NEXT_EGRESS_VLAN)
                .makePermanent()
                .withPriority(nextObjective.priority())
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .build();

        // Expected group
        PiAction piAction1 = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_NEXT_ROUTING_HASHED)
                .withParameter(new PiActionParam(
                        FabricConstants.SMAC, ROUTER_MAC.toBytes()))
                .withParameter(new PiActionParam(
                        FabricConstants.DMAC, SPINE1_MAC.toBytes()))
                .withParameter(new PiActionParam(
                        FabricConstants.PORT_NUM, PORT_1.toLong()))
                .build();
        PiAction piAction2 = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_NEXT_ROUTING_HASHED)
                .withParameter(new PiActionParam(
                        FabricConstants.SMAC, ROUTER_MAC.toBytes()))
                .withParameter(new PiActionParam(
                        FabricConstants.DMAC, SPINE2_MAC.toBytes()))
                .withParameter(new PiActionParam(
                        FabricConstants.PORT_NUM, PORT_2.toLong()))
                .build();
        treatment1 = DefaultTrafficTreatment.builder()
                .piTableAction(piAction1)
                .build();
        treatment2 = DefaultTrafficTreatment.builder()
                .piTableAction(piAction2)
                .build();
        List<TrafficTreatment> treatments = ImmutableList.of(treatment1, treatment2);
        List<GroupBucket> buckets = treatments.stream()
                .map(DefaultGroupBucket::createSelectGroupBucket)
                .collect(Collectors.toList());
        GroupBuckets groupBuckets = new GroupBuckets(buckets);
        PiGroupKey groupKey = new PiGroupKey(FabricConstants.FABRIC_INGRESS_NEXT_HASHED,
                FabricConstants.FABRIC_INGRESS_NEXT_HASHED_SELECTOR,
                NEXT_ID_1);
        GroupDescription expectedGroup = new DefaultGroupDescription(
                DEVICE_ID,
                GroupDescription.Type.SELECT,
                groupBuckets,
                groupKey,
                NEXT_ID_1,
                APP_ID
        );

        ObjectiveTranslation expectedTranslation = ObjectiveTranslation.builder()
                .addFlowRule(expectedFlowRule)
                .addFlowRule(vlanMetaFlowRule)
                .addFlowRule(mplsFlowRule)
                .addGroup(expectedGroup)
                .addFlowRule(expectedEgressVlanPopRule1)
                .addFlowRule(expectedEgressVlanPopRule2)
                .build();

        assertEquals(expectedTranslation, actualTranslation);
    }

    // TODO: add profile with simple next or remove tests
    /**
     * Test program output rule for Simple table.
     */
    @Test
    public void testSimpleOutput() throws FabricPipelinerException {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(PORT_1)
                .build();
        PiAction piAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_NEXT_OUTPUT_SIMPLE)
                .withParameter(new PiActionParam(
                        FabricConstants.PORT_NUM, PORT_1.toLong()))
                .build();
        testSimple(treatment, piAction);
    }

    /**
     * Test program set vlan and output rule for Simple table.
     */
    @Test
    public void testSimpleOutputWithVlanTranslation() throws FabricPipelinerException {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setVlanId(VLAN_100)
                .setOutput(PORT_1)
                .build();
        PiAction piAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_NEXT_OUTPUT_SIMPLE)
                .withParameter(new PiActionParam(
                        FabricConstants.PORT_NUM, PORT_1.toLong()))
                .build();
        testSimple(treatment, piAction);
    }

    /**
     * Test program set mac and output rule for Simple table.
     */
    @Test
    public void testSimpleOutputWithMacTranslation() throws FabricPipelinerException {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthSrc(ROUTER_MAC)
                .setEthDst(HOST_MAC)
                .setOutput(PORT_1)
                .build();
        PiAction piAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_NEXT_ROUTING_SIMPLE)
                .withParameter(new PiActionParam(
                        FabricConstants.SMAC, ROUTER_MAC.toBytes()))
                .withParameter(new PiActionParam(
                        FabricConstants.DMAC, HOST_MAC.toBytes()))
                .withParameter(new PiActionParam(
                        FabricConstants.PORT_NUM, PORT_1.toLong()))
                .build();
        testSimple(treatment, piAction);
    }

    /**
     * Test program set mac, set vlan, and output rule for Simple table.
     */
    @Test
    public void testSimpleOutputWithVlanAndMacTranslation() throws FabricPipelinerException {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthSrc(ROUTER_MAC)
                .setEthDst(HOST_MAC)
                .setVlanId(VLAN_100)
                .setOutput(PORT_1)
                .build();
        PiAction piAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_NEXT_ROUTING_SIMPLE)
                .withParameter(new PiActionParam(
                        FabricConstants.SMAC, ROUTER_MAC.toBytes()))
                .withParameter(new PiActionParam(
                        FabricConstants.DMAC, HOST_MAC.toBytes()))
                .withParameter(new PiActionParam(
                        FabricConstants.PORT_NUM, PORT_1.toLong()))
                .build();
        testSimple(treatment, piAction);
    }

    private void testSimple(TrafficTreatment treatment, PiAction piAction) throws FabricPipelinerException {
        NextObjective nextObjective = DefaultNextObjective.builder()
                .withId(NEXT_ID_1)
                .withPriority(PRIORITY)
                .withMeta(VLAN_META)
                .addTreatment(treatment)
                .withType(NextObjective.Type.SIMPLE)
                .makePermanent()
                .fromApp(APP_ID)
                .add();

        ObjectiveTranslation actualTranslation = translatorSimple.translate(nextObjective);

        // Simple table
        PiCriterion nextIdCriterion = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_NEXT_ID, NEXT_ID_1)
                .build();
        TrafficSelector nextIdSelector = DefaultTrafficSelector.builder()
                .matchPi(nextIdCriterion)
                .build();
        FlowRule expectedFlowRule = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .makePermanent()
                // FIXME: currently next objective doesn't support priority, ignore this
                .withPriority(0)
                .forTable(FabricConstants.FABRIC_INGRESS_NEXT_SIMPLE)
                .withSelector(nextIdSelector)
                .withTreatment(DefaultTrafficTreatment.builder()
                                       .piTableAction(piAction).build())
                .build();
        // Expected egress VLAN_PUSH flow rule.
        final PortNumber outPort = outputPort(treatment);
        PiCriterion egressVlanTableMatch = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_EG_PORT, outPort.toLong())
                .build();
        TrafficSelector selectorForEgressVlan = DefaultTrafficSelector.builder()
                .matchPi(egressVlanTableMatch)
                .matchVlanId(VLAN_100)
                .build();
        PiAction piActionForEgressVlan = PiAction.builder()
                .withId(FabricConstants.FABRIC_EGRESS_EGRESS_NEXT_PUSH_VLAN)
                .build();
        TrafficTreatment treatmentForEgressVlan = DefaultTrafficTreatment.builder()
                .piTableAction(piActionForEgressVlan)
                .build();
        FlowRule expectedEgressVlanPushRule = DefaultFlowRule.builder()
                .withSelector(selectorForEgressVlan)
                .withTreatment(treatmentForEgressVlan)
                .forTable(FabricConstants.FABRIC_EGRESS_EGRESS_NEXT_EGRESS_VLAN)
                .makePermanent()
                .withPriority(nextObjective.priority())
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .build();

        ObjectiveTranslation expectedTranslation = ObjectiveTranslation.builder()
                .addFlowRule(vlanMetaFlowRule)
                .addFlowRule(expectedFlowRule)
                .addFlowRule(expectedEgressVlanPushRule)
                .build();

        assertEquals(expectedTranslation, actualTranslation);
    }

    /**
     * Test Route and Push Next Objective (set mac, set double vlan and output port).
     */
    @Test
    public void testRouteAndPushNextObjective() throws FabricPipelinerException {
        TrafficTreatment routeAndPushTreatment = DefaultTrafficTreatment.builder()
                .setEthSrc(ROUTER_MAC)
                .setEthDst(HOST_MAC)
                .setOutput(PORT_1)
                .setVlanId(VLAN_100)
                .pushVlan()
                .setVlanId(VLAN_200)
                .build();

        NextObjective nextObjective = DefaultNextObjective.builder()
                .withId(NEXT_ID_1)
                .withPriority(PRIORITY)
                .addTreatment(routeAndPushTreatment)
                .withType(NextObjective.Type.SIMPLE)
                .makePermanent()
                .fromApp(APP_ID)
                .add();

        ObjectiveTranslation actualTranslation = translatorSimple.translate(nextObjective);

        PiAction piActionRouting = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_NEXT_ROUTING_SIMPLE)
                .withParameter(new PiActionParam(
                        FabricConstants.SMAC, ROUTER_MAC.toBytes()))
                .withParameter(new PiActionParam(
                        FabricConstants.DMAC, HOST_MAC.toBytes()))
                .withParameter(new PiActionParam(
                        FabricConstants.PORT_NUM, PORT_1.toLong()))
                .build();

        PiAction piActionPush = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_PRE_NEXT_SET_DOUBLE_VLAN)
                .withParameter(new PiActionParam(
                        FabricConstants.INNER_VLAN_ID, VLAN_100.toShort()))
                .withParameter(new PiActionParam(
                        FabricConstants.OUTER_VLAN_ID, VLAN_200.toShort()))
                .build();


        TrafficSelector nextIdSelector = DefaultTrafficSelector.builder()
                .matchPi(PiCriterion.builder()
                                 .matchExact(FabricConstants.HDR_NEXT_ID, NEXT_ID_1)
                                 .build())
                .build();
        FlowRule expectedFlowRuleRouting = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .makePermanent()
                // FIXME: currently next objective doesn't support priority, ignore this
                .withPriority(0)
                .forTable(FabricConstants.FABRIC_INGRESS_NEXT_SIMPLE)
                .withSelector(nextIdSelector)
                .withTreatment(DefaultTrafficTreatment.builder()
                                       .piTableAction(piActionRouting).build())
                .build();
        FlowRule expectedFlowRuleDoublePush = DefaultFlowRule.builder()
                .withSelector(nextIdSelector)
                .withTreatment(DefaultTrafficTreatment.builder()
                                       .piTableAction(piActionPush)
                                       .build())
                .forTable(FabricConstants.FABRIC_INGRESS_PRE_NEXT_NEXT_VLAN)
                .makePermanent()
                // FIXME: currently next objective doesn't support priority, ignore this
                .withPriority(0)
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .build();

        ObjectiveTranslation expectedTranslation = ObjectiveTranslation.builder()
                .addFlowRule(expectedFlowRuleDoublePush)
                .addFlowRule(expectedFlowRuleRouting)
                .build();


        assertEquals(expectedTranslation, actualTranslation);
    }

    /**
     * Test program ecmp output group for Hashed table.
     */
    @Test
    public void testHashedOutput() throws Exception {
        PiAction piAction1 = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_NEXT_ROUTING_HASHED)
                .withParameter(new PiActionParam(
                        FabricConstants.SMAC, ROUTER_MAC.toBytes()))
                .withParameter(new PiActionParam(
                        FabricConstants.DMAC, HOST_MAC.toBytes()))
                .withParameter(new PiActionParam(
                        FabricConstants.PORT_NUM, PORT_1.toLong()))
                .build();
        PiAction piAction2 = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_NEXT_ROUTING_HASHED)
                .withParameter(new PiActionParam(
                        FabricConstants.SMAC, ROUTER_MAC.toBytes()))
                .withParameter(new PiActionParam(
                        FabricConstants.DMAC, HOST_MAC.toBytes()))
                .withParameter(new PiActionParam(
                        FabricConstants.PORT_NUM, PORT_1.toLong()))
                .build();
        TrafficTreatment treatment1 = DefaultTrafficTreatment.builder()
                .piTableAction(piAction1)
                .build();
        TrafficTreatment treatment2 = DefaultTrafficTreatment.builder()
                .piTableAction(piAction2)
                .build();

        NextObjective nextObjective = DefaultNextObjective.builder()
                .withId(NEXT_ID_1)
                .withPriority(PRIORITY)
                .withMeta(VLAN_META)
                .addTreatment(treatment1)
                .addTreatment(treatment2)
                .withType(NextObjective.Type.HASHED)
                .makePermanent()
                .fromApp(APP_ID)
                .add();

        ObjectiveTranslation actualTranslation = translatorHashed.doTranslate(nextObjective);

        // Expected hashed table flow rule.
        PiCriterion nextIdCriterion = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_NEXT_ID, NEXT_ID_1)
                .build();
        TrafficSelector nextIdSelector = DefaultTrafficSelector.builder()
                .matchPi(nextIdCriterion)
                .build();
        PiActionProfileGroupId actionGroupId = PiActionProfileGroupId.of(NEXT_ID_1);
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .piTableAction(actionGroupId)
                .build();
        FlowRule expectedFlowRule = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .makePermanent()
                // FIXME: currently next objective doesn't support priority, ignore this
                .withPriority(0)
                .forTable(FabricConstants.FABRIC_INGRESS_NEXT_HASHED)
                .withSelector(nextIdSelector)
                .withTreatment(treatment)
                .build();

        // Expected group
        List<TrafficTreatment> treatments = ImmutableList.of(treatment1, treatment2);
        List<GroupBucket> buckets = treatments.stream()
                .map(DefaultGroupBucket::createSelectGroupBucket)
                .collect(Collectors.toList());
        GroupBuckets groupBuckets = new GroupBuckets(buckets);
        PiGroupKey groupKey = new PiGroupKey(FabricConstants.FABRIC_INGRESS_NEXT_HASHED,
                                             FabricConstants.FABRIC_INGRESS_NEXT_HASHED_SELECTOR,
                                             NEXT_ID_1);
        GroupDescription expectedGroup = new DefaultGroupDescription(
                DEVICE_ID,
                GroupDescription.Type.SELECT,
                groupBuckets,
                groupKey,
                NEXT_ID_1,
                APP_ID
        );

        ObjectiveTranslation expectedTranslation = ObjectiveTranslation.builder()
                .addFlowRule(expectedFlowRule)
                .addFlowRule(vlanMetaFlowRule)
                .addGroup(expectedGroup)
                .build();

        assertEquals(expectedTranslation, actualTranslation);

    }

    /**
     * Test program output group for Broadcast table.
     */
    @Test
    public void testBroadcastOutput() throws FabricPipelinerException {
        TrafficTreatment treatment1 = DefaultTrafficTreatment.builder()
                .setOutput(PORT_1)
                .build();
        TrafficTreatment treatment2 = DefaultTrafficTreatment.builder()
                .popVlan()
                .setOutput(PORT_2)
                .build();
        NextObjective nextObjective = DefaultNextObjective.builder()
                .withId(NEXT_ID_1)
                .withPriority(PRIORITY)
                .addTreatment(treatment1)
                .addTreatment(treatment2)
                .withMeta(VLAN_META)
                .withType(NextObjective.Type.BROADCAST)
                .makePermanent()
                .fromApp(APP_ID)
                .add();

        ObjectiveTranslation actualTranslation = translatorHashed.doTranslate(nextObjective);

        // Should generate 3 flows:
        // - Multicast table flow that matches on next-id and set multicast group (1)
        // - Egress VLAN pop handling for treatment2 (0)
        // - Next VLAN flow (2)
        // And 2 groups:
        // - Multicast group

        // Expected multicast table flow rule.
        PiCriterion nextIdCriterion = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_NEXT_ID, NEXT_ID_1)
                .build();
        TrafficSelector nextIdSelector = DefaultTrafficSelector.builder()
                .matchPi(nextIdCriterion)
                .build();
        PiAction setMcGroupAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_NEXT_SET_MCAST_GROUP_ID)
                .withParameter(new PiActionParam(
                        FabricConstants.GROUP_ID, NEXT_ID_1))
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .piTableAction(setMcGroupAction)
                .build();
        FlowRule expectedHashedFlowRule = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .makePermanent()
                .withPriority(nextObjective.priority())
                .forTable(FabricConstants.FABRIC_INGRESS_NEXT_MULTICAST)
                .withSelector(nextIdSelector)
                .withTreatment(treatment)
                .build();

        // Expected egress VLAN_PUSH flow rule.
        PiCriterion egressVlanTableMatch = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_EG_PORT, PORT_1.toLong())
                .build();
        TrafficSelector selectorForEgressVlan = DefaultTrafficSelector.builder()
                .matchPi(egressVlanTableMatch)
                .matchVlanId(VLAN_100)
                .build();
        PiAction piActionForEgressVlan = PiAction.builder()
                .withId(FabricConstants.FABRIC_EGRESS_EGRESS_NEXT_PUSH_VLAN)
                .build();
        TrafficTreatment treatmentForEgressVlan = DefaultTrafficTreatment.builder()
                .piTableAction(piActionForEgressVlan)
                .build();
        FlowRule expectedEgressVlanPushRule = DefaultFlowRule.builder()
                .withSelector(selectorForEgressVlan)
                .withTreatment(treatmentForEgressVlan)
                .forTable(FabricConstants.FABRIC_EGRESS_EGRESS_NEXT_EGRESS_VLAN)
                .makePermanent()
                .withPriority(nextObjective.priority())
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .build();
        // Expected egress VLAN POP flow rule.
        egressVlanTableMatch = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_EG_PORT, PORT_2.toLong())
                .build();
        selectorForEgressVlan = DefaultTrafficSelector.builder()
                .matchPi(egressVlanTableMatch)
                .matchVlanId(VLAN_100)
                .build();
        piActionForEgressVlan = PiAction.builder()
                .withId(FabricConstants.FABRIC_EGRESS_EGRESS_NEXT_POP_VLAN)
                .build();
        treatmentForEgressVlan = DefaultTrafficTreatment.builder()
                .piTableAction(piActionForEgressVlan)
                .build();
        FlowRule expectedEgressVlanPopRule = DefaultFlowRule.builder()
                .withSelector(selectorForEgressVlan)
                .withTreatment(treatmentForEgressVlan)
                .forTable(FabricConstants.FABRIC_EGRESS_EGRESS_NEXT_EGRESS_VLAN)
                .makePermanent()
                .withPriority(nextObjective.priority())
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .build();

        // Expected ALL group.
        TrafficTreatment allGroupTreatment1 = DefaultTrafficTreatment.builder()
                .setOutput(PORT_1)
                .build();
        TrafficTreatment allGroupTreatment2 = DefaultTrafficTreatment.builder()
                .setOutput(PORT_2)
                .build();
        List<TrafficTreatment> allTreatments = ImmutableList.of(
                allGroupTreatment1, allGroupTreatment2);
        List<GroupBucket> allBuckets = allTreatments.stream()
                .map(DefaultGroupBucket::createAllGroupBucket)
                .collect(Collectors.toList());
        GroupBuckets allGroupBuckets = new GroupBuckets(allBuckets);
        GroupKey allGroupKey = new DefaultGroupKey(FabricPipeliner.KRYO.serialize(NEXT_ID_1));
        GroupDescription expectedAllGroup = new DefaultGroupDescription(
                DEVICE_ID,
                GroupDescription.Type.ALL,
                allGroupBuckets,
                allGroupKey,
                NEXT_ID_1,
                APP_ID
        );

        ObjectiveTranslation expectedTranslation = ObjectiveTranslation.builder()
                .addFlowRule(expectedHashedFlowRule)
                .addFlowRule(vlanMetaFlowRule)
                .addFlowRule(expectedEgressVlanPushRule)
                .addFlowRule(expectedEgressVlanPopRule)
                .addGroup(expectedAllGroup)
                .build();

        assertEquals(expectedTranslation, actualTranslation);
    }

    /**
     * Test XConnect NextObjective.
     *
     * @throws FabricPipelinerException
     */
    @Test
    public void testXconnectOutput() throws FabricPipelinerException {
        TrafficTreatment treatment1 = DefaultTrafficTreatment.builder()
                .setOutput(PORT_1)
                .build();
        TrafficTreatment treatment2 = DefaultTrafficTreatment.builder()
                .setOutput(PORT_2)
                .build();
        NextObjective nextObjective = DefaultNextObjective.builder()
                .withId(NEXT_ID_1)
                .withPriority(PRIORITY)
                .addTreatment(treatment1)
                .addTreatment(treatment2)
                .withType(NextObjective.Type.BROADCAST)
                .makePermanent()
                .fromApp(XCONNECT_APP_ID)
                .add();

        ObjectiveTranslation actualTranslation = translatorHashed.doTranslate(nextObjective);

        // Should generate 2 flows for the xconnect table.

        // Expected multicast table flow rule.
        PiCriterion nextIdCriterion = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_NEXT_ID, NEXT_ID_1)
                .build();
        TrafficSelector xcSelector1 = DefaultTrafficSelector.builder()
                .matchPi(nextIdCriterion)
                .matchInPort(PORT_1)
                .build();
        TrafficTreatment xcTreatment1 = DefaultTrafficTreatment.builder()
                .piTableAction(PiAction.builder()
                                       .withId(FabricConstants.FABRIC_INGRESS_NEXT_OUTPUT_XCONNECT)
                                       .withParameter(new PiActionParam(FabricConstants.PORT_NUM, PORT_2.toLong()))
                                       .build())
                .build();
        TrafficSelector xcSelector2 = DefaultTrafficSelector.builder()
                .matchPi(nextIdCriterion)
                .matchInPort(PORT_2)
                .build();
        TrafficTreatment xcTreatment2 = DefaultTrafficTreatment.builder()
                .piTableAction(PiAction.builder()
                                       .withId(FabricConstants.FABRIC_INGRESS_NEXT_OUTPUT_XCONNECT)
                                       .withParameter(new PiActionParam(FabricConstants.PORT_NUM, PORT_1.toLong()))
                                       .build())
                .build();

        FlowRule expectedXcFlowRule1 = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .fromApp(XCONNECT_APP_ID)
                .makePermanent()
                .withPriority(nextObjective.priority())
                .forTable(FabricConstants.FABRIC_INGRESS_NEXT_XCONNECT)
                .withSelector(xcSelector1)
                .withTreatment(xcTreatment1)
                .build();
        FlowRule expectedXcFlowRule2 = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .fromApp(XCONNECT_APP_ID)
                .makePermanent()
                .withPriority(nextObjective.priority())
                .forTable(FabricConstants.FABRIC_INGRESS_NEXT_XCONNECT)
                .withSelector(xcSelector2)
                .withTreatment(xcTreatment2)
                .build();

        ObjectiveTranslation expectedTranslation = ObjectiveTranslation.builder()
                .addFlowRule(expectedXcFlowRule1)
                .addFlowRule(expectedXcFlowRule2)
                .build();

        assertEquals(expectedTranslation, actualTranslation);
    }
}
