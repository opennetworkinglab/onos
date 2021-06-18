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

package org.onosproject.pipelines.fabric.impl.behaviour;

import com.google.common.collect.ImmutableMap;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModMplsLabelInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiPipelineInterpreter.PiInterpreterException;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.pipelines.fabric.FabricConstants;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.onosproject.net.flow.instructions.Instruction.Type.OUTPUT;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.ETH_DST;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.ETH_SRC;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.MPLS_LABEL;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.VLAN_ID;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.VLAN_POP;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.VLAN_PUSH;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.instruction;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.l2Instruction;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.l2InstructionOrFail;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.l2Instructions;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.treatmentException;

/**
 * Treatment translation logic.
 */
final class FabricTreatmentInterpreter {

    private final FabricCapabilities capabilities;
    private static final ImmutableMap<PiTableId, PiActionId> NOP_ACTIONS =
            ImmutableMap.<PiTableId, PiActionId>builder()
                    .put(FabricConstants.FABRIC_INGRESS_FORWARDING_ROUTING_V4,
                         FabricConstants.FABRIC_INGRESS_FORWARDING_NOP_ROUTING_V4)
                    .put(FabricConstants.FABRIC_INGRESS_ACL_ACL,
                         FabricConstants.FABRIC_INGRESS_ACL_NOP_ACL)
                    .put(FabricConstants.FABRIC_EGRESS_EGRESS_NEXT_EGRESS_VLAN,
                         FabricConstants.FABRIC_EGRESS_EGRESS_NEXT_POP_VLAN)
                    .build();


    FabricTreatmentInterpreter(FabricCapabilities capabilities) {
        this.capabilities = capabilities;
    }


    static PiAction mapForwardingTreatment(TrafficTreatment treatment, PiTableId tableId)
            throws PiInterpreterException {
        if (isNoAction(treatment)) {
            return nop(tableId);
        }
        treatmentException(
                tableId, treatment,
                "supports mapping only for empty/no-action treatments");
        return null;
    }

    PiAction mapPreNextTreatment(TrafficTreatment treatment, PiTableId tableId)
            throws PiInterpreterException {
        if (tableId == FabricConstants.FABRIC_INGRESS_PRE_NEXT_NEXT_MPLS) {
            return mapNextMplsTreatment(treatment, tableId);
        } else if (tableId == FabricConstants.FABRIC_INGRESS_PRE_NEXT_NEXT_VLAN) {
            return mapNextVlanTreatment(treatment, tableId);
        }
        throw new PiInterpreterException(format(
                "Treatment mapping not supported for table '%s'", tableId));
    }

    PiAction mapNextTreatment(TrafficTreatment treatment, PiTableId tableId)
            throws PiInterpreterException {
        if (tableId == FabricConstants.FABRIC_INGRESS_NEXT_HASHED) {
            return mapNextHashedOrSimpleTreatment(treatment, tableId, false);
        } else if (tableId == FabricConstants.FABRIC_INGRESS_NEXT_SIMPLE) {
           return mapNextHashedOrSimpleTreatment(treatment, tableId, true);
        } else if (tableId == FabricConstants.FABRIC_INGRESS_NEXT_XCONNECT) {
           return mapNextXconnect(treatment, tableId);
        }
        throw new PiInterpreterException(format(
                "Treatment mapping not supported for table '%s'", tableId));
    }

    private static PiAction mapNextMplsTreatment(TrafficTreatment treatment, PiTableId tableId)
            throws PiInterpreterException {
        final ModMplsLabelInstruction mplsLabel = (ModMplsLabelInstruction) l2Instruction(
                treatment, MPLS_LABEL);
        if (mplsLabel != null) {
            return PiAction.builder()
                    .withParameter(new PiActionParam(FabricConstants.LABEL, mplsLabel.label().toInt()))
                    .withId(FabricConstants.FABRIC_INGRESS_PRE_NEXT_SET_MPLS_LABEL)
                    .build();
        }
        throw new PiInterpreterException("There is no MPLS instruction");
    }

    private PiAction mapNextVlanTreatment(TrafficTreatment treatment, PiTableId tableId)
            throws PiInterpreterException {
        final List<ModVlanIdInstruction> modVlanIdInst = l2InstructionsOrFail(treatment, VLAN_ID, tableId)
                .stream().map(i -> (ModVlanIdInstruction) i).collect(Collectors.toList());
        if (modVlanIdInst.size() == 1) {
            return PiAction.builder().withId(FabricConstants.FABRIC_INGRESS_PRE_NEXT_SET_VLAN)
                    .withParameter(new PiActionParam(
                            FabricConstants.VLAN_ID,
                            modVlanIdInst.get(0).vlanId().toShort()))
                    .build();
        }
        // next_vlan has been moved to pre_next
        if (modVlanIdInst.size() == 2 && capabilities.supportDoubleVlanTerm()) {
            return PiAction.builder()
                    .withId(FabricConstants.FABRIC_INGRESS_PRE_NEXT_SET_DOUBLE_VLAN)
                    .withParameter(new PiActionParam(
                            FabricConstants.INNER_VLAN_ID,
                            modVlanIdInst.get(0).vlanId().toShort()))
                    .withParameter(new PiActionParam(
                            FabricConstants.OUTER_VLAN_ID,
                            modVlanIdInst.get(1).vlanId().toShort()))
                    .build();
        }
        throw new PiInterpreterException("Too many VLAN instructions");
    }

    private static PiAction mapNextHashedOrSimpleTreatment(
            TrafficTreatment treatment, PiTableId tableId, boolean simple)
            throws PiInterpreterException {
        // Provide mapping for output_hashed and routing_hashed; multicast_hashed
        // can only be invoked with PiAction, hence no mapping. outPort required for
        // all actions. Presence of other instructions will determine which action to map to.
        final PortNumber outPort = ((OutputInstruction) instructionOrFail(
                treatment, OUTPUT, tableId)).port();
        final ModEtherInstruction ethDst = (ModEtherInstruction) l2Instruction(
                treatment, ETH_DST);
        final ModEtherInstruction ethSrc = (ModEtherInstruction) l2Instruction(
                treatment, ETH_SRC);

        final PiAction.Builder actionBuilder = PiAction.builder()
                .withParameter(new PiActionParam(FabricConstants.PORT_NUM, outPort.toLong()));

        if (ethDst != null && ethSrc != null) {
            actionBuilder.withParameter(new PiActionParam(
                    FabricConstants.SMAC, ethSrc.mac().toBytes()));
            actionBuilder.withParameter(new PiActionParam(
                    FabricConstants.DMAC, ethDst.mac().toBytes()));
            // routing_hashed
            return actionBuilder
                    .withId(simple ? FabricConstants.FABRIC_INGRESS_NEXT_ROUTING_SIMPLE
                                : FabricConstants.FABRIC_INGRESS_NEXT_ROUTING_HASHED)
                    .build();
        } else {
            // output_hashed
            return actionBuilder
                    .withId(simple ? FabricConstants.FABRIC_INGRESS_NEXT_OUTPUT_SIMPLE
                                    : FabricConstants.FABRIC_INGRESS_NEXT_OUTPUT_HASHED)
                    .build();
        }
    }

    private static PiAction mapNextXconnect(
            TrafficTreatment treatment, PiTableId tableId)
            throws PiInterpreterException {
        final PortNumber outPort = ((OutputInstruction) instructionOrFail(
                treatment, OUTPUT, tableId)).port();
        return PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_NEXT_OUTPUT_XCONNECT)
                .withParameter(new PiActionParam(
                        FabricConstants.PORT_NUM, outPort.toLong()))
                .build();
    }

    static PiAction mapAclTreatment(TrafficTreatment treatment, PiTableId tableId)
            throws PiInterpreterException {
        if (isDrop(treatment)) {
                return drop(tableId);
        }
        if (isNoAction(treatment)) {
                return nop(tableId);
        }
        treatmentException(
                tableId, treatment,
                "unsupported treatment");

        // This function will never return null
        return null;
    }


    static PiAction mapEgressNextTreatment(
            TrafficTreatment treatment, PiTableId tableId)
            throws PiInterpreterException {
        L2ModificationInstruction pushVlan = l2Instruction(treatment, VLAN_PUSH);
        if (pushVlan != null) {
            return PiAction.builder()
                    .withId(FabricConstants.FABRIC_EGRESS_EGRESS_NEXT_PUSH_VLAN)
                    .build();
        }
        l2InstructionOrFail(treatment, VLAN_POP, tableId);
        return PiAction.builder()
                .withId(FabricConstants.FABRIC_EGRESS_EGRESS_NEXT_POP_VLAN)
                .build();

    }

    private static PiAction nop(PiTableId tableId) throws PiInterpreterException {
        if (!NOP_ACTIONS.containsKey(tableId)) {
            throw new PiInterpreterException(format("table '%s' doe not specify a nop action", tableId));
        }
        return PiAction.builder().withId(NOP_ACTIONS.get(tableId)).build();
    }

    private static PiAction drop(PiTableId tableId) throws PiInterpreterException {
        if (!tableId.equals(FabricConstants.FABRIC_INGRESS_ACL_ACL)) {
            throw new PiInterpreterException(format("table '%s' doe not specify a nop action", tableId));
        }
        return PiAction.builder().withId(FabricConstants.FABRIC_INGRESS_ACL_DROP).build();
    }

    // NOTE: clearDeferred is used by the routing application to implement ACL drop and route black-holing
    private static boolean isNoAction(TrafficTreatment treatment) {
        return treatment.equals(DefaultTrafficTreatment.emptyTreatment()) ||
                (treatment.allInstructions().isEmpty() && !treatment.clearedDeferred()) ||
                (treatment.allInstructions().size() == 1 && treatment.writeMetadata() != null);
    }

    private static boolean isDrop(TrafficTreatment treatment) {
        return treatment.allInstructions().isEmpty() && treatment.clearedDeferred();
    }

    private static List<L2ModificationInstruction> l2InstructionsOrFail(
            TrafficTreatment treatment,
            L2ModificationInstruction.L2SubType subType, PiTableId tableId)
            throws PiInterpreterException {
        final List<L2ModificationInstruction> inst = l2Instructions(treatment, subType);
        if (inst == null || inst.isEmpty()) {
            treatmentException(tableId, treatment, format("missing %s instruction", subType));
        }
        return inst;
    }

    private static Instruction instructionOrFail(
            TrafficTreatment treatment, Instruction.Type type, PiTableId tableId)
            throws PiInterpreterException {
        final Instruction inst = instruction(treatment, type);
        if (inst == null) {
            treatmentException(tableId, treatment, format("missing %s instruction", type));
        }
        return inst;
    }
}
