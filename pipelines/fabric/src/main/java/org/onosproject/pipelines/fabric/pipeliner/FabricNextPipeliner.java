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

import com.google.common.collect.ImmutableMap;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.pipelines.fabric.FabricConstants;
import org.slf4j.Logger;

import java.util.Map;

import static org.onosproject.pipelines.fabric.pipeliner.FabricPipeliner.fail;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handling next objective for fabric pipeliner.
 */
public class FabricNextPipeliner {
    private static final Logger log = getLogger(FabricNextPipeliner.class);

    // Next types
    private static final byte TBL_SIMPLE = 0;
    private static final byte TBL_HASHED = 1;
    private static final byte TBL_BROADCAST = 2;
    private static final byte PUNT = 3;
    private static final Map<NextObjective.Type, Byte> NEXT_TYPE_MAP =
            ImmutableMap.<NextObjective.Type, Byte>builder()
                    .put(NextObjective.Type.SIMPLE, TBL_SIMPLE)
                    .put(NextObjective.Type.HASHED, TBL_HASHED)
                    .put(NextObjective.Type.BROADCAST, TBL_BROADCAST)
                    .build();

    protected DeviceId deviceId;

    public FabricNextPipeliner(DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    public PipelinerTranslationResult next(NextObjective nextObjective) {
        PipelinerTranslationResult.Builder resultBuilder = PipelinerTranslationResult.builder();
        FlowRule nextIdMappingRule = processNextIdMapping(nextObjective);
        FlowRule nextRule = null;
        switch (nextObjective.type()) {
            case SIMPLE:
                nextRule = processSimpleNext(nextObjective);
                break;
            default:
                log.warn("Unsupported next type {}", nextObjective);
                resultBuilder.setError(ObjectiveError.UNSUPPORTED);
                break;
        }

        if (nextIdMappingRule != null && nextRule != null) {
            resultBuilder.addFlowRule(nextIdMappingRule);
            resultBuilder.addFlowRule(nextRule);
        }

        return resultBuilder.build();
    }

    private FlowRule processNextIdMapping(NextObjective next) {
        // program the next id mapping table
        TrafficSelector nextIdSelector = buildNextIdSelector(next.id());
        TrafficTreatment setNextTypeTreatment = buildSetNextTypeTreatment(next.type());

        return DefaultFlowRule.builder()
                .withSelector(nextIdSelector)
                .withTreatment(setNextTypeTreatment)
                .forDevice(deviceId)
                .forTable(FabricConstants.TBL_NEXT_ID_MAPPING_ID)
                .makePermanent()
                .withPriority(next.priority())
                .fromApp(next.appId())
                .build();
    }

    private FlowRule processSimpleNext(NextObjective next) {
        if (next.next().size() > 1) {
            log.warn("Only one treatment in simple next objective");
            fail(next, ObjectiveError.BADPARAMS);
            return null;
        }

        TrafficSelector selector = buildNextIdSelector(next.id());
        TrafficTreatment treatment = next.next().iterator().next();
        OutputInstruction outputInst = treatment.allInstructions()
                .stream()
                .filter(inst -> inst.type() == Instruction.Type.OUTPUT)
                .map(inst -> (OutputInstruction) inst)
                .findFirst()
                .orElse(null);

        if (outputInst == null) {
            log.warn("At least one output instruction in simple next objective");
            fail(next, ObjectiveError.BADPARAMS);
            return null;
        }
        return DefaultFlowRule.builder()
                .withSelector(selector)
                .withTreatment(treatment)
                .forTable(FabricConstants.TBL_SIMPLE_ID)
                .makePermanent()
                .withPriority(next.priority())
                .forDevice(deviceId)
                .fromApp(next.appId())
                .build();
    }

    private TrafficSelector buildNextIdSelector(int nextId) {
        byte[] nextIdVal = new byte[]{(byte) nextId};
        PiCriterion nextIdCrriterion = PiCriterion.builder()
                .matchExact(FabricConstants.HF_FABRIC_METADATA_NEXT_ID_ID, nextIdVal)
                .build();
        return DefaultTrafficSelector.builder()
                .matchPi(nextIdCrriterion)
                .build();
    }

    private TrafficTreatment buildSetNextTypeTreatment(NextObjective.Type nextType) {
        byte nextTypeVal = NEXT_TYPE_MAP.getOrDefault(nextType, PUNT);
        PiActionParam nextTypeParam = new PiActionParam(FabricConstants.ACT_PRM_NEXT_TYPE_ID,
                                                        ImmutableByteSequence.copyFrom(nextTypeVal));
        PiAction nextTypeAction = PiAction.builder()
                .withId(FabricConstants.ACT_SET_NEXT_TYPE_ID)
                .withParameter(nextTypeParam)
                .build();
        return DefaultTrafficTreatment.builder()
                .piTableAction(nextTypeAction)
                .build();
    }
}
