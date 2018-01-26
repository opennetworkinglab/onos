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

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.onlab.util.ImmutableByteSequence;
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
import org.onosproject.net.pi.runtime.PiActionGroupId;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiGroupKey;
import org.onosproject.pipelines.fabric.FabricConstants;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for fabric.p4 pipeline next control block.
 */
public class FabricNextPipelinerTest extends FabricPipelinerTest {
    private FlowRule vlanMetaFlowRule;

    public FabricNextPipelinerTest() {
        PiCriterion nextIdCriterion = PiCriterion.builder()
                .matchExact(FabricConstants.FABRIC_METADATA_NEXT_ID, NEXT_ID_1)
                .build();
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchPi(nextIdCriterion)
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setVlanId(VLAN_100)
                .build();

        vlanMetaFlowRule = DefaultFlowRule.builder()
                .withSelector(selector)
                .withTreatment(treatment)
                .forTable(FabricConstants.FABRIC_INGRESS_NEXT_VLAN_META)
                .makePermanent()
                // FIXME: currently next objective doesn't support priority, ignore this
                .withPriority(0)
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .build();
    }

    /**
     * Test program output rule for Simple table.
     */
    @Test
    public void testSimpleOutput() {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(PORT_1)
                .build();
        testSimple(treatment);
    }

    /**
     * Test program set vlan and output rule for Simple table.
     */
    @Test
    public void testSimpleOutputWithVlanTranslation() {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setVlanId(VLAN_100)
                .setOutput(PORT_1)
                .build();
        testSimple(treatment);
    }

    /**
     * Test program set mac and output rule for Simple table.
     */
    @Test
    public void testSimpleOutputWithMacTranslation() {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthSrc(ROUTER_MAC)
                .setEthDst(HOST_MAC)
                .setOutput(PORT_1)
                .build();
        testSimple(treatment);
    }

    /**
     * Test program set mac, set vlan, and output rule for Simple table.
     */
    @Test
    public void testSimpleOutputWithVlanAndMacTranslation() {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthSrc(ROUTER_MAC)
                .setEthDst(HOST_MAC)
                .setVlanId(VLAN_100)
                .setOutput(PORT_1)
                .build();
        testSimple(treatment);
    }

    private void testSimple(TrafficTreatment treatment) {
        NextObjective nextObjective = DefaultNextObjective.builder()
                .withId(NEXT_ID_1)
                .withPriority(PRIORITY)
                .withMeta(VLAN_META)
                .addTreatment(treatment)
                .withType(NextObjective.Type.SIMPLE)
                .makePermanent()
                .fromApp(APP_ID)
                .add();

        PipelinerTranslationResult result = pipeliner.pipelinerNext.next(nextObjective);

        List<FlowRule> flowRulesInstalled = (List<FlowRule>) result.flowRules();
        List<GroupDescription> groupsInstalled = (List<GroupDescription>) result.groups();
        assertEquals(2, flowRulesInstalled.size());
        assertTrue(groupsInstalled.isEmpty());

        // Simple table
        PiCriterion nextIdCriterion = PiCriterion.builder()
                .matchExact(FabricConstants.FABRIC_METADATA_NEXT_ID, NEXT_ID_1)
                .build();
        TrafficSelector nextIdSelector = DefaultTrafficSelector.builder()
                .matchPi(nextIdCriterion)
                .build();

        // VLAN meta table
        FlowRule actualFlowRule = flowRulesInstalled.get(0);
        assertTrue(actualFlowRule.exactMatch(vlanMetaFlowRule));

        actualFlowRule = flowRulesInstalled.get(1);
        FlowRule expectedFlowRule = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .makePermanent()
                // FIXME: currently next objective doesn't support priority, ignore this
                .withPriority(0)
                .forTable(FabricConstants.FABRIC_INGRESS_NEXT_SIMPLE)
                .withSelector(nextIdSelector)
                .withTreatment(treatment)
                .build();
        assertTrue(expectedFlowRule.exactMatch(actualFlowRule));
    }

    /**
     * Test program ecmp output group for Hashed table.
     */
    @Test
    public void testHashedOutput() throws Exception {
        TrafficTreatment treatment1 = DefaultTrafficTreatment.builder()
                .setEthSrc(ROUTER_MAC)
                .setEthDst(HOST_MAC)
                .setOutput(PORT_1)
                .build();
        TrafficTreatment treatment2 = DefaultTrafficTreatment.builder()
                .setEthSrc(ROUTER_MAC)
                .setEthDst(HOST_MAC)
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

        PipelinerTranslationResult result = pipeliner.pipelinerNext.next(nextObjective);

        // Should generates 2 flows and 1 group
        List<FlowRule> flowRulesInstalled = (List<FlowRule>) result.flowRules();
        List<GroupDescription> groupsInstalled = (List<GroupDescription>) result.groups();
        assertEquals(2, flowRulesInstalled.size());
        assertEquals(1, groupsInstalled.size());

        // Hashed table
        PiCriterion nextIdCriterion = PiCriterion.builder()
                .matchExact(FabricConstants.FABRIC_METADATA_NEXT_ID, NEXT_ID_1)
                .build();
        TrafficSelector nextIdSelector = DefaultTrafficSelector.builder()
                .matchPi(nextIdCriterion)
                .build();
        PiActionGroupId actionGroupId = PiActionGroupId.of(NEXT_ID_1);
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .piTableAction(actionGroupId)
                .build();

        // VLAN meta table
        FlowRule actualFlowRule = flowRulesInstalled.get(0);
        assertTrue(actualFlowRule.exactMatch(vlanMetaFlowRule));

        actualFlowRule = flowRulesInstalled.get(1);
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
        assertTrue(expectedFlowRule.exactMatch(actualFlowRule));

        // Group
        GroupDescription actualGroup = groupsInstalled.get(0);
        List<TrafficTreatment> treatments = ImmutableList.of(treatment1, treatment2);

        List<GroupBucket> buckets = treatments.stream()
                .map(DefaultGroupBucket::createSelectGroupBucket)
                .collect(Collectors.toList());
        GroupBuckets groupBuckets = new GroupBuckets(buckets);
        PiGroupKey groupKey = new PiGroupKey(FabricConstants.FABRIC_INGRESS_NEXT_HASHED,
                                             FabricConstants.FABRIC_INGRESS_NEXT_ECMP_SELECTOR,
                                             NEXT_ID_1);
        GroupDescription expectedGroup = new DefaultGroupDescription(
                DEVICE_ID,
                GroupDescription.Type.SELECT,
                groupBuckets,
                groupKey,
                NEXT_ID_1,
                APP_ID
        );
        assertEquals(expectedGroup, actualGroup);

    }

    /**
     * Test program output group for Broadcast table.
     */
    @Test
    public void testBroadcastOutput() {
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

        PipelinerTranslationResult result = pipeliner.pipelinerNext.next(nextObjective);

        // Should generate 1 flow, 1 group and 2 buckets in it
        List<FlowRule> flowRulesInstalled = (List<FlowRule>) result.flowRules();
        List<GroupDescription> groupsInstalled = (List<GroupDescription>) result.groups();
        assertEquals(3, flowRulesInstalled.size());
        assertEquals(1, groupsInstalled.size());
        assertEquals(2, groupsInstalled.get(0).buckets().buckets().size());

        //create the expected flow rule
        PiCriterion nextIdCriterion = PiCriterion.builder()
                .matchExact(FabricConstants.FABRIC_METADATA_NEXT_ID, NEXT_ID_1)
                .build();
        TrafficSelector nextIdSelector = DefaultTrafficSelector.builder()
                .matchPi(nextIdCriterion)
                .build();

        PiActionParam groupIdParam = new PiActionParam(FabricConstants.GID,
                                                       ImmutableByteSequence.copyFrom(NEXT_ID_1));
        PiAction setMcGroupAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_NEXT_SET_MCAST_GROUP)
                .withParameter(groupIdParam)
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .piTableAction(setMcGroupAction)
                .build();
        FlowRule expectedFlowRule = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .makePermanent()
                .withPriority(nextObjective.priority())
                .forTable(FabricConstants.FABRIC_INGRESS_NEXT_MULTICAST)
                .withSelector(nextIdSelector)
                .withTreatment(treatment)
                .build();

        // VLAN meta table
        FlowRule vmFlowRule = flowRulesInstalled.get(0);
        assertTrue(vmFlowRule.exactMatch(vlanMetaFlowRule));

        FlowRule actualFlowRule = flowRulesInstalled.get(1);
        assertTrue(expectedFlowRule.exactMatch(actualFlowRule));

        //prepare expected egress rule for the egress vlan pipeline
        PiCriterion egressVlanTableMatch = PiCriterion.builder()
                .matchExact(FabricConstants.STANDARD_METADATA_EGRESS_PORT,
                            (short) PORT_2.toLong())
                .build();
        TrafficSelector selectorForEgressVlan = DefaultTrafficSelector.builder()
                .matchPi(egressVlanTableMatch)
                .matchVlanId(VLAN_100)
                .build();
        TrafficTreatment treatmentForEgressVlan = DefaultTrafficTreatment.builder()
                .popVlan()
                .build();
        FlowRule expectedEgressVlanRule = DefaultFlowRule.builder()
                .withSelector(selectorForEgressVlan)
                .withTreatment(treatmentForEgressVlan)
                .forTable(FabricConstants.FABRIC_EGRESS_EGRESS_NEXT_EGRESS_VLAN)
                .makePermanent()
                .withPriority(nextObjective.priority())
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .build();
        //egress vlan table
        FlowRule actualEgressVlanFlowRule = flowRulesInstalled.get(2);
        assertTrue(expectedEgressVlanRule.exactMatch(actualEgressVlanFlowRule));

        //create the expected group
        GroupDescription actualGroup = groupsInstalled.get(0);
        List<TrafficTreatment> treatments = ImmutableList.of(treatment1, treatment2);

        List<GroupBucket> buckets = treatments.stream()
                .map(DefaultGroupBucket::createAllGroupBucket)
                .collect(Collectors.toList());

        GroupBuckets groupBuckets = new GroupBuckets(buckets);

        GroupKey groupKey = new DefaultGroupKey(FabricPipeliner.KRYO.serialize(NEXT_ID_1));

        GroupDescription expectedGroup = new DefaultGroupDescription(
                DEVICE_ID,
                GroupDescription.Type.ALL,
                groupBuckets,
                groupKey,
                NEXT_ID_1,
                APP_ID
        );
        assertEquals(expectedGroup, actualGroup);
    }
}
