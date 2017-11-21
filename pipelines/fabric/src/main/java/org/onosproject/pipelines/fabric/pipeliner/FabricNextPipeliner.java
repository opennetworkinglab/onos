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
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionGroupId;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiGroupKey;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.pipelines.fabric.FabricConstants.ACT_PRF_ECMP_SELECTOR_ID;
import static org.onosproject.pipelines.fabric.FabricConstants.ACT_PRM_NEXT_TYPE_ID;
import static org.onosproject.pipelines.fabric.FabricConstants.ACT_SET_NEXT_TYPE_ID;
import static org.onosproject.pipelines.fabric.FabricConstants.HF_FABRIC_METADATA_NEXT_ID_ID;
import static org.onosproject.pipelines.fabric.FabricConstants.TBL_HASHED_ID;
import static org.onosproject.pipelines.fabric.FabricConstants.TBL_NEXT_ID_MAPPING_ID;
import static org.onosproject.pipelines.fabric.FabricConstants.TBL_SIMPLE_ID;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handling next objective for fabric pipeliner.
 */
public class FabricNextPipeliner {
    private static final Logger log = getLogger(FabricNextPipeliner.class);

    // Next types
    private static final byte NXT_TYPE_SIMPLE = 0;
    private static final byte NXT_TYPE_HASHED = 1;
    private static final byte NXT_TYPE_BROADCAST = 2;
    private static final byte NXT_TYPE_PUNT = 3;
    private static final Map<NextObjective.Type, Byte> NEXT_TYPE_MAP =
            ImmutableMap.<NextObjective.Type, Byte>builder()
                    .put(NextObjective.Type.SIMPLE, NXT_TYPE_SIMPLE)
                    .put(NextObjective.Type.HASHED, NXT_TYPE_HASHED)
                    .put(NextObjective.Type.BROADCAST, NXT_TYPE_BROADCAST)
                    .build();

    protected DeviceId deviceId;

    public FabricNextPipeliner(DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    public PipelinerTranslationResult next(NextObjective nextObjective) {
        PipelinerTranslationResult.Builder resultBuilder = PipelinerTranslationResult.builder();
        processNextIdMapping(nextObjective, resultBuilder);

        switch (nextObjective.type()) {
            case SIMPLE:
                processSimpleNext(nextObjective, resultBuilder);
                break;
            case HASHED:
                processHashedNext(nextObjective, resultBuilder);
                break;
            default:
                log.warn("Unsupported next type {}", nextObjective);
                resultBuilder.setError(ObjectiveError.UNSUPPORTED);
                break;
        }

        return resultBuilder.build();
    }

    private void processNextIdMapping(NextObjective next,
                                      PipelinerTranslationResult.Builder resultBuilder) {
        // program the next id mapping table
        TrafficSelector nextIdSelector = buildNextIdSelector(next.id());
        TrafficTreatment setNextTypeTreatment = buildSetNextTypeTreatment(next.type());

        resultBuilder.addFlowRule(DefaultFlowRule.builder()
                                          .withSelector(nextIdSelector)
                                          .withTreatment(setNextTypeTreatment)
                                          .forDevice(deviceId)
                                          .forTable(TBL_NEXT_ID_MAPPING_ID)
                                          .makePermanent()
                                          .withPriority(next.priority())
                                          .fromApp(next.appId())
                                          .build());
    }

    private void processSimpleNext(NextObjective next,
                                   PipelinerTranslationResult.Builder resultBuilder) {

        if (next.next().size() > 1) {
            log.warn("Only one treatment in simple next objective");
            resultBuilder.setError(ObjectiveError.BADPARAMS);
            return;
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
            resultBuilder.setError(ObjectiveError.BADPARAMS);
            return;
        }
        resultBuilder.addFlowRule(DefaultFlowRule.builder()
                                          .withSelector(selector)
                                          .withTreatment(treatment)
                                          .forTable(TBL_SIMPLE_ID)
                                          .makePermanent()
                                          .withPriority(next.priority())
                                          .forDevice(deviceId)
                                          .fromApp(next.appId())
                                          .build());
    }

    private void processHashedNext(NextObjective nextObjective, PipelinerTranslationResult.Builder resultBuilder) {
        // create hash groups
        int groupId = nextObjective.id();
        List<GroupBucket> bucketList = nextObjective.next().stream()
                .map(DefaultGroupBucket::createSelectGroupBucket)
                .collect(Collectors.toList());

        if (bucketList.size() != nextObjective.next().size()) {
            // some action not converted
            // set error
            log.warn("Expected bucket size {}, got {}", nextObjective.next().size(), bucketList.size());
            resultBuilder.setError(ObjectiveError.BADPARAMS);
            return;
        }

        GroupBuckets buckets = new GroupBuckets(bucketList);
        PiGroupKey groupKey = new PiGroupKey(TBL_HASHED_ID,
                                             ACT_PRF_ECMP_SELECTOR_ID,
                                             groupId);

        resultBuilder.addGroup(new DefaultGroupDescription(deviceId,
                                                           GroupDescription.Type.SELECT,
                                                           buckets,
                                                           groupKey,
                                                           groupId,
                                                           nextObjective.appId()));

        // flow
        TrafficSelector selector = buildNextIdSelector(nextObjective.id());
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .piTableAction(PiActionGroupId.of(nextObjective.id()))
                .build();

        resultBuilder.addFlowRule(DefaultFlowRule.builder()
                                          .withSelector(selector)
                                          .withTreatment(treatment)
                                          .forTable(TBL_HASHED_ID)
                                          .makePermanent()
                                          .withPriority(nextObjective.priority())
                                          .forDevice(deviceId)
                                          .fromApp(nextObjective.appId())
                                          .build());
    }

    private TrafficSelector buildNextIdSelector(int nextId) {
        PiCriterion nextIdCriterion = PiCriterion.builder()
                .matchExact(HF_FABRIC_METADATA_NEXT_ID_ID, nextId)
                .build();
        return DefaultTrafficSelector.builder()
                .matchPi(nextIdCriterion)
                .build();
    }

    private TrafficTreatment buildSetNextTypeTreatment(NextObjective.Type nextType) {
        byte nextTypeVal = NEXT_TYPE_MAP.getOrDefault(nextType, NXT_TYPE_PUNT);
        PiActionParam nextTypeParam = new PiActionParam(ACT_PRM_NEXT_TYPE_ID,
                                                        copyFrom(nextTypeVal));
        PiAction nextTypeAction = PiAction.builder()
                .withId(ACT_SET_NEXT_TYPE_ID)
                .withParameter(nextTypeParam)
                .build();
        return DefaultTrafficTreatment.builder()
                .piTableAction(nextTypeAction)
                .build();
    }
}
