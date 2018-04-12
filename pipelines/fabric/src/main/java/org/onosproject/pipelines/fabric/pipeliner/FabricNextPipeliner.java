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

import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.pi.runtime.PiActionGroupId;
import org.onosproject.net.pi.runtime.PiGroupKey;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

import static org.onosproject.pipelines.fabric.FabricConstants.ACT_PRF_FABRICINGRESS_NEXT_ECMP_SELECTOR_ID;
import static org.onosproject.pipelines.fabric.FabricConstants.HF_FABRIC_METADATA_NEXT_ID_ID;
import static org.onosproject.pipelines.fabric.FabricConstants.TBL_HASHED_ID;
import static org.onosproject.pipelines.fabric.FabricConstants.TBL_SIMPLE_ID;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handling next objective for fabric pipeliner.
 */
public class FabricNextPipeliner {
    private static final Logger log = getLogger(FabricNextPipeliner.class);
    private static final String NO_HASHED_TABLE = "noHashedTable";

    protected DeviceId deviceId;
    protected Driver driver;

    public FabricNextPipeliner(DeviceId deviceId, Driver driver) {
        this.deviceId = deviceId;
        this.driver = driver;
    }

    public PipelinerTranslationResult next(NextObjective nextObjective) {
        PipelinerTranslationResult.Builder resultBuilder = PipelinerTranslationResult.builder();

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
        boolean noHashedTable = Boolean.parseBoolean(driver.getProperty(NO_HASHED_TABLE));

        if (noHashedTable) {
            if (nextObjective.next().isEmpty()) {
                return;
            }
            // use first action if not support hashed group
            TrafficTreatment treatment = nextObjective.next().iterator().next();

            NextObjective.Builder simpleNext = DefaultNextObjective.builder()
                    .addTreatment(treatment)
                    .withId(nextObjective.id())
                    .fromApp(nextObjective.appId())
                    .makePermanent()
                    .withMeta(nextObjective.meta())
                    .withPriority(nextObjective.priority())
                    .withType(NextObjective.Type.SIMPLE);

            if (nextObjective.context().isPresent()) {
                processSimpleNext(simpleNext.add(nextObjective.context().get()), resultBuilder);
            } else {
                processSimpleNext(simpleNext.add(), resultBuilder);
            }
            return;
        }

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
                                             ACT_PRF_FABRICINGRESS_NEXT_ECMP_SELECTOR_ID,
                                             groupId);

        resultBuilder.addGroup(new DefaultGroupDescription(deviceId,
                                                           GroupDescription.Type.SELECT,
                                                           buckets,
                                                           groupKey,
                                                           groupId,
                                                           nextObjective.appId()));

        // flow
        // If operation is ADD_TO_EXIST or REMOVE_FROM_EXIST, means we modify
        // group buckets only, no changes for flow rule
        if (nextObjective.op() == Objective.Operation.ADD_TO_EXISTING ||
                nextObjective.op() == Objective.Operation.REMOVE_FROM_EXISTING) {
            return;
        }
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
}
