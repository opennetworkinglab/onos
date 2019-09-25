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

import com.google.common.collect.Lists;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onosproject.net.flowobjective.DefaultNextTreatment;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.NextTreatment;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiActionProfileGroupId;
import org.onosproject.net.pi.runtime.PiGroupKey;
import org.onosproject.pipelines.fabric.impl.behaviour.FabricCapabilities;
import org.onosproject.pipelines.fabric.impl.behaviour.FabricConstants;
import org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.VLAN_ID;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.VLAN_POP;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.criterion;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.l2Instruction;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.l2Instructions;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.outputPort;

/**
 * ObjectiveTranslator implementation for NextObjective.
 */
class NextObjectiveTranslator
        extends AbstractObjectiveTranslator<NextObjective> {

    private static final String XCONNECT = "xconnect";

    NextObjectiveTranslator(DeviceId deviceId, FabricCapabilities capabilities) {
        super(deviceId, capabilities);
    }

    @Override
    public ObjectiveTranslation doTranslate(NextObjective obj)
            throws FabricPipelinerException {

        final ObjectiveTranslation.Builder resultBuilder =
                ObjectiveTranslation.builder();

        switch (obj.type()) {
            case SIMPLE:
                simpleNext(obj, resultBuilder, false);
                break;
            case HASHED:
                hashedNext(obj, resultBuilder);
                break;
            case BROADCAST:
                if (isXconnect(obj)) {
                    xconnectNext(obj, resultBuilder);
                } else {
                    multicastNext(obj, resultBuilder);
                }
                break;
            default:
                log.warn("Unsupported NextObjective type '{}'", obj);
                return ObjectiveTranslation.ofError(ObjectiveError.UNSUPPORTED);
        }

        if (!isGroupModifyOp(obj)) {
            // Generate next VLAN rules.
            nextVlan(obj, resultBuilder);
        }

        return resultBuilder.build();
    }

    private void nextVlan(NextObjective obj,
                          ObjectiveTranslation.Builder resultBuilder)
            throws FabricPipelinerException {
        // We expect NextObjective treatments to contain one or two VLAN instructions.
        // If two, this treatment should be mapped to an action for double-vlan push.
        // In fabric.p4, mapping next IDs to VLAN IDs is done by a direct table (next_vlan),
        // for this reason, we also make sure that all treatments in the NextObjective
        // have exactly the same VLAN instructions, as they will be mapped to a single action

        // Try to extract VLAN instructions in the treatment,
        //  later we check if we support multiple VLAN termination.
        final List<List<ModVlanIdInstruction>> vlanInstructions = defaultNextTreatments(
                obj.nextTreatments(), false).stream()
                .map(defaultNextTreatment ->
                             l2Instructions(defaultNextTreatment.treatment(), VLAN_ID)
                                     .stream().map(v -> (ModVlanIdInstruction) v)
                                     .collect(Collectors.toList()))
                .filter(l -> !l.isEmpty())
                .collect(Collectors.toList());

        final VlanIdCriterion vlanIdCriterion = obj.meta() == null ? null
                : (VlanIdCriterion) criterion(obj.meta().criteria(), Criterion.Type.VLAN_VID);

        final List<VlanId> vlanIdList;
        if (vlanInstructions.isEmpty() && vlanIdCriterion == null) {
            // No VLAN_ID to apply.
            return;
        }
        if (!vlanInstructions.isEmpty()) {
            // Give priority to what found in the instructions.
            // Expect the same VLAN ID (or two VLAN IDs in the same order) for all instructions.
            final Set<List<VlanId>> vlanIds = vlanInstructions.stream()
                    .map(l -> l.stream().map(ModVlanIdInstruction::vlanId).collect(Collectors.toList()))
                    .collect(Collectors.toSet());
            if (obj.nextTreatments().size() != vlanInstructions.size() ||
                    vlanIds.size() != 1) {
                throw new FabricPipelinerException(
                        "Inconsistent VLAN_ID instructions, cannot process " +
                                "next_vlan rule. It is required that all " +
                                "treatments have the same VLAN_ID instructions.");
            }
            vlanIdList = vlanIds.iterator().next();
        } else {
            // Use the value in meta.
            // FIXME: there should be no need to generate a next_vlan rule for
            //  the value found in meta. Meta describes the fields that were
            //  expected to be matched in previous pipeline stages, i.e.
            //  existing packet fields. But, for some reason, if we remove this
            //  rule, traffic is not forwarded at spines. We might need to look
            //  at the way default VLANs are handled in fabric.p4.
            vlanIdList = List.of(vlanIdCriterion.vlanId());
        }

        final TrafficSelector selector = nextIdSelector(obj.id());
        final TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        vlanIdList.stream().forEach(vlanId -> treatmentBuilder.setVlanId(vlanId));
        final TrafficTreatment treatment = treatmentBuilder.build();

        resultBuilder.addFlowRule(flowRule(
                obj, FabricConstants.FABRIC_INGRESS_NEXT_NEXT_VLAN,
                selector, treatment));
    }

    private void simpleNext(NextObjective obj,
                            ObjectiveTranslation.Builder resultBuilder,
                            boolean forceSimple)
            throws FabricPipelinerException {

        if (capabilities.hasHashedTable()) {
            // Use hashed table when possible.
            hashedNext(obj, resultBuilder);
            return;
        }

        if (obj.nextTreatments().isEmpty()) {
            // Do nothing.
            return;
        } else if (!forceSimple && obj.nextTreatments().size() != 1) {
            throw new FabricPipelinerException(format(
                    "SIMPLE NextObjective should contain only 1 treatment, found %d",
                    obj.nextTreatments().size()), ObjectiveError.BADPARAMS);
        }

        final TrafficSelector selector = nextIdSelector(obj.id());

        final List<DefaultNextTreatment> treatments = defaultNextTreatments(
                obj.nextTreatments(), true);

        if (forceSimple && treatments.size() > 1) {
            log.warn("Forcing SIMPLE behavior for NextObjective with {} treatments []",
                     treatments.size(), obj);
        }

        // If not forcing, we are essentially extracting the only available treatment.
        final TrafficTreatment treatment = defaultNextTreatments(
                obj.nextTreatments(), true).get(0).treatment();

        resultBuilder.addFlowRule(flowRule(
                obj, FabricConstants.FABRIC_INGRESS_NEXT_SIMPLE,
                selector, treatment));

        handleEgress(obj, treatment, resultBuilder, false);
    }

    private void hashedNext(NextObjective obj,
                            ObjectiveTranslation.Builder resultBuilder)
            throws FabricPipelinerException {

        if (!capabilities.hasHashedTable()) {
            simpleNext(obj, resultBuilder, true);
            return;
        }

        // Updated result builder with hashed group.
        final int groupId = selectGroup(obj, resultBuilder);

        if (isGroupModifyOp(obj)) {
            // No changes to flow rules.
            return;
        }

        final TrafficSelector selector = nextIdSelector(obj.id());
        final TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .piTableAction(PiActionProfileGroupId.of(groupId))
                .build();

        resultBuilder.addFlowRule(flowRule(
                obj, FabricConstants.FABRIC_INGRESS_NEXT_HASHED,
                selector, treatment));
    }

    private void handleEgress(NextObjective obj, TrafficTreatment treatment,
                              ObjectiveTranslation.Builder resultBuilder,
                              boolean strict)
            throws FabricPipelinerException {
        final PortNumber outPort = outputPort(treatment);
        final Instruction popVlanInst = l2Instruction(treatment, VLAN_POP);
        if (popVlanInst != null && outPort != null) {
            if (strict && treatment.allInstructions().size() > 2) {
                throw new FabricPipelinerException(
                        "Treatment contains instructions other " +
                                "than OUTPUT and VLAN_POP, cannot generate " +
                                "egress rules");
            }
            egressVlanPop(outPort, obj, resultBuilder);
        }
    }

    private void egressVlanPop(PortNumber outPort, NextObjective obj,
                               ObjectiveTranslation.Builder resultBuilder)
            throws FabricPipelinerException {

        if (obj.meta() == null) {
            throw new FabricPipelinerException(
                    "Cannot process egress pop VLAN rule, NextObjective has null meta",
                    ObjectiveError.BADPARAMS);
        }

        final VlanIdCriterion vlanIdCriterion = (VlanIdCriterion) criterion(
                obj.meta(), Criterion.Type.VLAN_VID);
        if (vlanIdCriterion == null) {
            throw new FabricPipelinerException(
                    "Cannot process egress pop VLAN rule, missing VLAN_VID criterion " +
                            "in NextObjective meta",
                    ObjectiveError.BADPARAMS);
        }

        final PiCriterion egressVlanTableMatch = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_EG_PORT, outPort.toLong())
                .build();
        final TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchPi(egressVlanTableMatch)
                .matchVlanId(vlanIdCriterion.vlanId())
                .build();
        final TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .popVlan()
                .build();

        resultBuilder.addFlowRule(flowRule(
                obj, FabricConstants.FABRIC_EGRESS_EGRESS_NEXT_EGRESS_VLAN,
                selector, treatment));
    }

    private TrafficSelector nextIdSelector(int nextId) {
        return nextIdSelectorBuilder(nextId).build();
    }

    private TrafficSelector.Builder nextIdSelectorBuilder(int nextId) {
        final PiCriterion nextIdCriterion = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_NEXT_ID, nextId)
                .build();
        return DefaultTrafficSelector.builder()
                .matchPi(nextIdCriterion);
    }

    private void xconnectNext(NextObjective obj, ObjectiveTranslation.Builder resultBuilder)
            throws FabricPipelinerException {

        final Collection<DefaultNextTreatment> defaultNextTreatments =
                defaultNextTreatments(obj.nextTreatments(), true);

        final List<PortNumber> outPorts = defaultNextTreatments.stream()
                .map(DefaultNextTreatment::treatment)
                .map(FabricUtils::outputPort)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (outPorts.size() != 2) {
            throw new FabricPipelinerException(format(
                    "Handling XCONNECT with %d treatments (ports), but expected is 2",
                    defaultNextTreatments.size()), ObjectiveError.UNSUPPORTED);
        }

        final PortNumber port1 = outPorts.get(0);
        final PortNumber port2 = outPorts.get(1);
        final TrafficSelector selector1 = nextIdSelectorBuilder(obj.id())
                .matchInPort(port1)
                .build();
        final TrafficTreatment treatment1 = DefaultTrafficTreatment.builder()
                .setOutput(port2)
                .build();
        final TrafficSelector selector2 = nextIdSelectorBuilder(obj.id())
                .matchInPort(port2)
                .build();
        final TrafficTreatment treatment2 = DefaultTrafficTreatment.builder()
                .setOutput(port1)
                .build();

        resultBuilder.addFlowRule(flowRule(
                obj, FabricConstants.FABRIC_INGRESS_NEXT_XCONNECT,
                selector1, treatment1));
        resultBuilder.addFlowRule(flowRule(
                obj, FabricConstants.FABRIC_INGRESS_NEXT_XCONNECT,
                selector2, treatment2));

    }

    private void multicastNext(NextObjective obj,
                               ObjectiveTranslation.Builder resultBuilder)
            throws FabricPipelinerException {

        // Create ALL group that will be translated to a PRE multicast entry.
        final int groupId = allGroup(obj, resultBuilder);

        if (isGroupModifyOp(obj)) {
            // No changes to flow rules.
            return;
        }

        final TrafficSelector selector = nextIdSelector(obj.id());
        final PiActionParam groupIdParam = new PiActionParam(
                FabricConstants.GROUP_ID, groupId);
        final PiAction setMcGroupAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_NEXT_SET_MCAST_GROUP_ID)
                .withParameter(groupIdParam)
                .build();
        final TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .piTableAction(setMcGroupAction)
                .build();

        resultBuilder.addFlowRule(flowRule(
                obj, FabricConstants.FABRIC_INGRESS_NEXT_MULTICAST,
                selector, treatment));
    }

    private int selectGroup(NextObjective obj,
                            ObjectiveTranslation.Builder resultBuilder)
            throws FabricPipelinerException {

        final PiTableId hashedTableId = FabricConstants.FABRIC_INGRESS_NEXT_HASHED;
        final List<DefaultNextTreatment> defaultNextTreatments =
                defaultNextTreatments(obj.nextTreatments(), true);
        final List<TrafficTreatment> piTreatments = Lists.newArrayList();

        for (DefaultNextTreatment t : defaultNextTreatments) {
            // Map treatment to PI...
            piTreatments.add(mapTreatmentToPiIfNeeded(t.treatment(), hashedTableId));
            // ...and handle egress if necessary.
            handleEgress(obj, t.treatment(), resultBuilder, false);
        }

        final List<GroupBucket> bucketList = piTreatments.stream()
                .map(DefaultGroupBucket::createSelectGroupBucket)
                .collect(Collectors.toList());

        final int groupId = obj.id();
        final PiGroupKey groupKey = new PiGroupKey(
                hashedTableId,
                FabricConstants.FABRIC_INGRESS_NEXT_HASHED_SELECTOR,
                groupId);

        resultBuilder.addGroup(new DefaultGroupDescription(
                deviceId,
                GroupDescription.Type.SELECT,
                new GroupBuckets(bucketList),
                groupKey,
                groupId,
                obj.appId()));

        return groupId;
    }

    private int allGroup(NextObjective obj,
                         ObjectiveTranslation.Builder resultBuilder)
            throws FabricPipelinerException {

        final Collection<DefaultNextTreatment> defaultNextTreatments =
                defaultNextTreatments(obj.nextTreatments(), true);
        // No need to map treatments to PI as translation of ALL groups to PRE
        // multicast entries is based solely on the output port.
        for (DefaultNextTreatment t : defaultNextTreatments) {
            handleEgress(obj, t.treatment(), resultBuilder, true);
        }

        // FIXME: this implementation supports only the case in which each
        // switch interface is associated with only one VLAN, otherwise we would
        // need to support replicating multiple times the same packet for the
        // same port while setting different VLAN IDs. Hence, collect in a set.
        final Set<PortNumber> outPorts = defaultNextTreatments.stream()
                .map(DefaultNextTreatment::treatment)
                .map(FabricUtils::outputPort)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (outPorts.size() != defaultNextTreatments.size()) {
            throw new FabricPipelinerException(format(
                    "Found BROADCAST NextObjective with %d treatments but " +
                            "found only %d distinct OUTPUT port numbers, cannot " +
                            "translate to ALL groups",
                    defaultNextTreatments.size(), outPorts.size()),
                                               ObjectiveError.UNSUPPORTED);
        }

        final List<GroupBucket> bucketList = outPorts.stream()
                .map(p -> DefaultTrafficTreatment.builder().setOutput(p).build())
                .map(DefaultGroupBucket::createAllGroupBucket)
                .collect(Collectors.toList());

        final int groupId = obj.id();
        // Use DefaultGroupKey instead of PiGroupKey as we don't have any
        // action profile to apply to the groups of ALL type.
        final GroupKey groupKey = new DefaultGroupKey(
                FabricPipeliner.KRYO.serialize(groupId));

        resultBuilder.addGroup(
                new DefaultGroupDescription(
                        deviceId,
                        GroupDescription.Type.ALL,
                        new GroupBuckets(bucketList),
                        groupKey,
                        groupId,
                        obj.appId()));

        return groupId;
    }

    private List<DefaultNextTreatment> defaultNextTreatments(
            Collection<NextTreatment> nextTreatments, boolean strict)
            throws FabricPipelinerException {
        final List<DefaultNextTreatment> defaultNextTreatments = Lists.newArrayList();
        final List<NextTreatment> unsupportedNextTreatments = Lists.newArrayList();
        for (NextTreatment n : nextTreatments) {
            if (n.type() == NextTreatment.Type.TREATMENT) {
                defaultNextTreatments.add((DefaultNextTreatment) n);
            } else {
                unsupportedNextTreatments.add(n);
            }
        }
        if (strict && !unsupportedNextTreatments.isEmpty()) {
            throw new FabricPipelinerException(format(
                    "Unsupported NextTreatments: %s",
                    unsupportedNextTreatments));
        }
        return defaultNextTreatments;
    }

    private TrafficTreatment getFirstDefaultNextTreatmentIfAny(
            Collection<NextTreatment> nextTreatments)
            throws FabricPipelinerException {
        final Collection<DefaultNextTreatment> nexts = defaultNextTreatments(nextTreatments, false);
        return nexts.isEmpty() ? null : nexts.iterator().next().treatment();
    }

    private boolean isGroupModifyOp(NextObjective obj) {
        // If operation is ADD_TO_EXIST or REMOVE_FROM_EXIST, it means we modify
        // group buckets only, no changes for flow rules.
        return obj.op() == Objective.Operation.ADD_TO_EXISTING ||
                obj.op() == Objective.Operation.REMOVE_FROM_EXISTING;
    }

    private boolean isXconnect(NextObjective obj) {
        return obj.appId().name().contains(XCONNECT);
    }
}
