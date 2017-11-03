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
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.pipelines.fabric.FabricConstants;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        assertEquals(2, flowRulesInstalled.size());
        assertTrue(groupsInstalled.isEmpty());

        FlowRule actualFlowRule;
        FlowRule expectedFlowRule;

        // Next id mapping table
        actualFlowRule = flowRulesInstalled.get(0);
        byte[] nextIdVal = new byte[]{NEXT_ID_1.byteValue()};
        PiCriterion nextIdCriterion = PiCriterion.builder()
                .matchExact(FabricConstants.HF_FABRIC_METADATA_NEXT_ID_ID, nextIdVal)
                .build();
        TrafficSelector nextIdSelector = DefaultTrafficSelector.builder()
                .matchPi(nextIdCriterion)
                .build();
        PiActionParam setNextToSimpleParam = new PiActionParam(FabricConstants.ACT_PRM_NEXT_TYPE_ID,
                                                               ImmutableByteSequence.copyFrom(NEXT_TYPE_SIMPLE));
        PiAction setNextToSimpleAction = PiAction.builder()
                .withId(FabricConstants.ACT_SET_NEXT_TYPE_ID)
                .withParameter(setNextToSimpleParam)
                .build();
        TrafficTreatment setNextTypeTreatment = DefaultTrafficTreatment.builder()
                .piTableAction(setNextToSimpleAction)
                .build();
        expectedFlowRule = DefaultFlowRule.builder()
                .forDevice(DEVICE_ID)
                .fromApp(APP_ID)
                .makePermanent()
                // FIXME: currently next objective doesn't support priority, set priority to zero
                .withPriority(0)
                .forTable(FabricConstants.TBL_NEXT_ID_MAPPING_ID)
                .withSelector(nextIdSelector)
                .withTreatment(setNextTypeTreatment)
                .build();
        assertTrue(expectedFlowRule.exactMatch(actualFlowRule));

        // Simple table
        actualFlowRule = flowRulesInstalled.get(1);
        expectedFlowRule = DefaultFlowRule.builder()
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
    @Ignore
    public void testHashedOutput() {

    }

    /**
     * Test program output group for Broadcast table.
     */
    @Test
    @Ignore
    public void testBroadcastOutput() {

    }
}
