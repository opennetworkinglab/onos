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
import org.junit.Ignore;
import org.junit.Test;
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
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.pi.runtime.PiActionGroupId;
import org.onosproject.net.pi.runtime.PiGroupKey;
import org.onosproject.pipelines.fabric.FabricConstants;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.pipelines.fabric.FabricConstants.ACT_PRF_FABRICINGRESS_NEXT_ECMP_SELECTOR_ID;
import static org.onosproject.pipelines.fabric.FabricConstants.TBL_HASHED_ID;

/**
 * Test cases for fabric.p4 pipeline next control block.
 */
public class FabricNextPipelinerTest extends FabricPipelinerTest {

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

    private void testSimple(TrafficTreatment treatment) {
        NextObjective nextObjective = DefaultNextObjective.builder()
                .withId(NEXT_ID_1)
                .withPriority(PRIORITY)
                .addTreatment(treatment)
                .withType(NextObjective.Type.SIMPLE)
                .makePermanent()
                .fromApp(APP_ID)
                .add();

        PipelinerTranslationResult result = pipeliner.pipelinerNext.next(nextObjective);

        List<FlowRule> flowRulesInstalled = (List<FlowRule>) result.flowRules();
        List<GroupDescription> groupsInstalled = (List<GroupDescription>) result.groups();
        assertEquals(1, flowRulesInstalled.size());
        assertTrue(groupsInstalled.isEmpty());

        // Simple table
        PiCriterion nextIdCriterion = PiCriterion.builder()
                .matchExact(FabricConstants.HF_FABRIC_METADATA_NEXT_ID_ID, NEXT_ID_1)
                .build();
        TrafficSelector nextIdSelector = DefaultTrafficSelector.builder()
                .matchPi(nextIdCriterion)
                .build();
        FlowRule actualFlowRule = flowRulesInstalled.get(0);
        FlowRule expectedFlowRule = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .makePermanent()
                // FIXME: currently next objective doesn't support priority, ignore this
                .withPriority(0)
                .forTable(FabricConstants.TBL_SIMPLE_ID)
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
        assertEquals(1, flowRulesInstalled.size());
        assertEquals(1, groupsInstalled.size());

        // Hashed table
        PiCriterion nextIdCriterion = PiCriterion.builder()
                .matchExact(FabricConstants.HF_FABRIC_METADATA_NEXT_ID_ID, NEXT_ID_1)
                .build();
        TrafficSelector nextIdSelector = DefaultTrafficSelector.builder()
                .matchPi(nextIdCriterion)
                .build();
        PiActionGroupId actionGroupId = PiActionGroupId.of(NEXT_ID_1);
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .piTableAction(actionGroupId)
                .build();
        FlowRule actualFlowRule = flowRulesInstalled.get(0);
        FlowRule expectedFlowRule = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .makePermanent()
                // FIXME: currently next objective doesn't support priority, ignore this
                .withPriority(0)
                .forTable(TBL_HASHED_ID)
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
        PiGroupKey groupKey = new PiGroupKey(TBL_HASHED_ID, ACT_PRF_FABRICINGRESS_NEXT_ECMP_SELECTOR_ID, NEXT_ID_1);
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
    @Ignore
    public void testBroadcastOutput() {

    }
}
