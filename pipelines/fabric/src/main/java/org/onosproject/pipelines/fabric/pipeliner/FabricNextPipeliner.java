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

import org.onlab.packet.VlanId;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
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
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

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

        processNextVlanMeta(nextObjective, resultBuilder);

        switch (nextObjective.type()) {
            case SIMPLE:
                processSimpleNext(nextObjective, resultBuilder);
                break;
            case HASHED:
                processHashedNext(nextObjective, resultBuilder);
                break;
            case BROADCAST:
                processBroadcastNext(nextObjective, resultBuilder);
                break;
            default:
                log.warn("Unsupported next type {}", nextObjective);
                resultBuilder.setError(ObjectiveError.UNSUPPORTED);
                break;
        }

        return resultBuilder.build();
    }

    private void processNextVlanMeta(NextObjective next,
                                     PipelinerTranslationResult.Builder resultBuilder) {
        TrafficSelector meta = next.meta();
        if (meta == null) {
            // do nothing if there is no metadata in the next objective.
            return;
        }
        VlanIdCriterion vlanIdCriterion =
                (VlanIdCriterion) meta.getCriterion(Criterion.Type.VLAN_VID);

        if (vlanIdCriterion == null) {
            // do nothing if we can't find vlan from next objective metadata.
            return;
        }

        VlanId vlanId = vlanIdCriterion.vlanId();
        TrafficSelector selector = buildNextIdSelector(next.id());
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setVlanId(vlanId)
                .build();

        resultBuilder.addFlowRule(DefaultFlowRule.builder()
                                          .withSelector(selector)
                                          .withTreatment(treatment)
                                          .forTable(FabricConstants.FABRIC_INGRESS_NEXT_VLAN_META)
                                          .makePermanent()
                                          .withPriority(next.priority())
                                          .forDevice(deviceId)
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
        PortNumber outputPort = getOutputPort(treatment);

        if (outputPort == null) {
            log.warn("At least one output instruction in simple next objective");
            resultBuilder.setError(ObjectiveError.BADPARAMS);
            return;
        }

        resultBuilder.addFlowRule(DefaultFlowRule.builder()
                                          .withSelector(selector)
                                          .withTreatment(treatment)
                                          .forTable(FabricConstants.FABRIC_INGRESS_NEXT_SIMPLE)
                                          .makePermanent()
                                          .withPriority(next.priority())
                                          .forDevice(deviceId)
                                          .fromApp(next.appId())
                                          .build());

        if (includesPopVlanInst(treatment)) {
            processVlanPopRule(outputPort, next, resultBuilder);
        }
    }

    private PortNumber getOutputPort(TrafficTreatment treatment) {
        return treatment.allInstructions()
                .stream()
                .filter(inst -> inst.type() == Instruction.Type.OUTPUT)
                .map(inst -> (OutputInstruction) inst)
                .findFirst()
                .map(OutputInstruction::port)
                .orElse(null);
    }

    private boolean includesPopVlanInst(TrafficTreatment treatment) {
        return treatment.allInstructions()
                .stream()
                .filter(inst -> inst.type() == Instruction.Type.L2MODIFICATION)
                .map(inst -> (L2ModificationInstruction) inst)
                .anyMatch(inst -> inst.subtype() == L2ModificationInstruction.L2SubType.VLAN_POP);
    }

    private void processVlanPopRule(PortNumber port, NextObjective next,
                                    PipelinerTranslationResult.Builder resultBuilder) {
        TrafficSelector meta = next.meta();
        VlanIdCriterion vlanIdCriterion =
                (VlanIdCriterion) meta.getCriterion(Criterion.Type.VLAN_VID);
        VlanId vlanId = vlanIdCriterion.vlanId();

        PiCriterion egressVlanTableMatch = PiCriterion.builder()
                .matchExact(FabricConstants.STANDARD_METADATA_EGRESS_PORT,
                            (short) port.toLong())
                .build();
        // Add VLAN pop rule to egress pipeline table
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchPi(egressVlanTableMatch)
                .matchVlanId(vlanId)
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .popVlan()
                .build();
        resultBuilder.addFlowRule(DefaultFlowRule.builder()
                                          .withSelector(selector)
                                          .withTreatment(treatment)
                                          .forTable(FabricConstants.FABRIC_EGRESS_EGRESS_NEXT_EGRESS_VLAN)
                                          .makePermanent()
                                          .withPriority(next.priority())
                                          .forDevice(deviceId)
                                          .fromApp(next.appId())
                                          .build());
    }

    private void processHashedNext(NextObjective next, PipelinerTranslationResult.Builder resultBuilder) {
        boolean noHashedTable = Boolean.parseBoolean(driver.getProperty(NO_HASHED_TABLE));

        if (noHashedTable) {
            if (next.next().isEmpty()) {
                return;
            }
            // use first action if not support hashed group
            TrafficTreatment treatment = next.next().iterator().next();

            NextObjective.Builder simpleNext = DefaultNextObjective.builder()
                    .addTreatment(treatment)
                    .withId(next.id())
                    .fromApp(next.appId())
                    .makePermanent()
                    .withMeta(next.meta())
                    .withPriority(next.priority())
                    .withType(NextObjective.Type.SIMPLE);

            if (next.context().isPresent()) {
                processSimpleNext(simpleNext.add(next.context().get()), resultBuilder);
            } else {
                processSimpleNext(simpleNext.add(), resultBuilder);
            }
            return;
        }

        // create hash groups
        int groupId = next.id();
        List<GroupBucket> bucketList = next.next().stream()
                .map(DefaultGroupBucket::createSelectGroupBucket)
                .collect(Collectors.toList());

        // Egress VLAN handling
        next.next().forEach(treatment -> {
            PortNumber outputPort = getOutputPort(treatment);
            if (includesPopVlanInst(treatment) && outputPort != null) {
                processVlanPopRule(outputPort, next, resultBuilder);
            }
        });

        if (bucketList.size() != next.next().size()) {
            // some action not converted
            // set error
            log.warn("Expected bucket size {}, got {}", next.next().size(), bucketList.size());
            resultBuilder.setError(ObjectiveError.BADPARAMS);
            return;
        }

        GroupBuckets buckets = new GroupBuckets(bucketList);
        PiGroupKey groupKey = new PiGroupKey(FabricConstants.FABRIC_INGRESS_NEXT_HASHED,
                                             FabricConstants.FABRIC_INGRESS_NEXT_ECMP_SELECTOR,
                                             groupId);

        resultBuilder.addGroup(new DefaultGroupDescription(deviceId,
                                                           GroupDescription.Type.SELECT,
                                                           buckets,
                                                           groupKey,
                                                           groupId,
                                                           next.appId()));

        // flow
        // If operation is ADD_TO_EXIST or REMOVE_FROM_EXIST, means we modify
        // group buckets only, no changes for flow rule
        if (next.op() == Objective.Operation.ADD_TO_EXISTING ||
                next.op() == Objective.Operation.REMOVE_FROM_EXISTING) {
            return;
        }
        TrafficSelector selector = buildNextIdSelector(next.id());
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .piTableAction(PiActionGroupId.of(next.id()))
                .build();

        resultBuilder.addFlowRule(DefaultFlowRule.builder()
                                          .withSelector(selector)
                                          .withTreatment(treatment)
                                          .forTable(FabricConstants.FABRIC_INGRESS_NEXT_HASHED)
                                          .makePermanent()
                                          .withPriority(next.priority())
                                          .forDevice(deviceId)
                                          .fromApp(next.appId())
                                          .build());
    }

    private TrafficSelector buildNextIdSelector(int nextId) {
        PiCriterion nextIdCriterion = PiCriterion.builder()
                .matchExact(FabricConstants.FABRIC_METADATA_NEXT_ID, nextId)
                .build();
        return DefaultTrafficSelector.builder()
                .matchPi(nextIdCriterion)
                .build();
    }

    private void processBroadcastNext(NextObjective next, PipelinerTranslationResult.Builder resultBuilder) {
        int groupId = next.id();
        List<GroupBucket> bucketList = next.next().stream()
                .filter(treatment -> treatment != null)
                .map(DefaultGroupBucket::createAllGroupBucket)
                .collect(Collectors.toList());

        if (bucketList.size() != next.next().size()) {
            // some action not converted
            // set error
            log.warn("Expected bucket size {}, got {}", next.next().size(), bucketList.size());
            resultBuilder.setError(ObjectiveError.BADPARAMS);
            return;
        }

        GroupBuckets buckets = new GroupBuckets(bucketList);
        //Used DefaultGroupKey instead of PiGroupKey
        //as we don't have any action profile to apply to the groups of ALL type
        GroupKey groupKey = new DefaultGroupKey(FabricPipeliner.KRYO.serialize(groupId));

        resultBuilder.addGroup(new DefaultGroupDescription(deviceId,
                                                           GroupDescription.Type.ALL,
                                                           buckets,
                                                           groupKey,
                                                           groupId,
                                                           next.appId()));
        //flow rule
        TrafficSelector selector = buildNextIdSelector(next.id());
        PiActionParam groupIdParam = new PiActionParam(FabricConstants.GID,
                                                       ImmutableByteSequence.copyFrom(groupId));

        PiAction setMcGroupAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_NEXT_SET_MCAST_GROUP)
                .withParameter(groupIdParam)
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .piTableAction(setMcGroupAction)
                .build();

        resultBuilder.addFlowRule(DefaultFlowRule.builder()
                                          .withSelector(selector)
                                          .withTreatment(treatment)
                                          .forTable(FabricConstants.FABRIC_INGRESS_NEXT_MULTICAST)
                                          .makePermanent()
                                          .withPriority(next.priority())
                                          .forDevice(deviceId)
                                          .fromApp(next.appId())
                                          .build());

        // Egress VLAN handling
        next.next().forEach(trafficTreatment -> {
            PortNumber outputPort = getOutputPort(trafficTreatment);
            if (includesPopVlanInst(trafficTreatment) && outputPort != null) {
                processVlanPopRule(outputPort, next, resultBuilder);
            }
        });
    }
}
