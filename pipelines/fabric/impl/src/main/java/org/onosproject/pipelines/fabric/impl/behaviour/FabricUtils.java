/*
 * Copyright 2018-present Open Networking Foundation
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

import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.MetadataCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flowobjective.DefaultNextTreatment;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextTreatment;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiTableId;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.EDGE_PORT;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.INFRA_PORT;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.METADATA_MASK;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.METADATA_TO_PORT_TYPE;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.PAIR_PORT;

/**
 * Utility class with methods common to fabric pipeconf operations.
 */
public final class FabricUtils {

    private FabricUtils() {
        // Hides constructor.
    }

    public static Criterion criterion(Collection<Criterion> criteria, Criterion.Type type) {
        return criteria.stream()
                .filter(c -> c.type().equals(type))
                .findFirst().orElse(null);
    }

    public static Criterion criterion(TrafficSelector selector, Criterion.Type type) {
        return selector.getCriterion(type);
    }

    public static Criterion criterionNotNull(TrafficSelector selector, Criterion.Type type) {
        return checkNotNull(criterion(selector, type),
                            format("%s criterion cannot be null", type));
    }

    public static Criterion criterionNotNull(Collection<Criterion> criteria, Criterion.Type type) {
        return checkNotNull(criterion(criteria, type),
                            format("%s criterion cannot be null", type));
    }

    public static Instruction instruction(TrafficTreatment treatment, Instruction.Type type) {
        return treatment.allInstructions()
                .stream()
                .filter(inst -> inst.type() == type)
                .findFirst().orElse(null);
    }

    public static L2ModificationInstruction l2Instruction(
            TrafficTreatment treatment, L2ModificationInstruction.L2SubType subType) {
        return treatment.allInstructions().stream()
                .filter(i -> i.type().equals(Instruction.Type.L2MODIFICATION))
                .map(i -> (L2ModificationInstruction) i)
                .filter(i -> i.subtype().equals(subType))
                .findFirst().orElse(null);
    }

    public static Instruction l2InstructionOrFail(
            TrafficTreatment treatment,
            L2ModificationInstruction.L2SubType subType, PiTableId tableId)
            throws PiPipelineInterpreter.PiInterpreterException {
        final Instruction inst = l2Instruction(treatment, subType);
        if (inst == null) {
            treatmentException(tableId, treatment, format("missing %s instruction", subType));
        }
        return inst;
    }

    public static List<L2ModificationInstruction> l2Instructions(
            TrafficTreatment treatment, L2ModificationInstruction.L2SubType subType) {
        return treatment.allInstructions().stream()
                .filter(i -> i.type().equals(Instruction.Type.L2MODIFICATION))
                .map(i -> (L2ModificationInstruction) i)
                .filter(i -> i.subtype().equals(subType))
                .collect(Collectors.toList());
    }

    public static Instructions.OutputInstruction outputInstruction(TrafficTreatment treatment) {
        return (Instructions.OutputInstruction) instruction(treatment, Instruction.Type.OUTPUT);
    }

    public static PortNumber outputPort(TrafficTreatment treatment) {
        final Instructions.OutputInstruction inst = outputInstruction(treatment);
        return inst == null ? null : inst.port();
    }

    public static PortNumber outputPort(NextTreatment treatment) {
        if (treatment.type() == NextTreatment.Type.TREATMENT) {
            final DefaultNextTreatment t = (DefaultNextTreatment) treatment;
            return outputPort(t.treatment());
        }
        return null;
    }

    public static void treatmentException(
            PiTableId tableId, TrafficTreatment treatment, String explanation)
            throws PiPipelineInterpreter.PiInterpreterException {
        throw new PiPipelineInterpreter.PiInterpreterException(format(
                "Invalid treatment for table '%s', %s: %s", tableId, explanation, treatment));
    }

    /**
     * Port type metadata conversion.
     *
     * @param obj the objective
     * @return the port type associated to the metadata, null otherwise
     */
    public static Byte portType(Objective obj) {
        Byte portType = null;
        if (isSrMetadataSet(obj, PAIR_PORT) && METADATA_TO_PORT_TYPE.containsKey(PAIR_PORT)) {
            portType = METADATA_TO_PORT_TYPE.get(PAIR_PORT);
        } else if (isSrMetadataSet(obj, EDGE_PORT) && METADATA_TO_PORT_TYPE.containsKey(EDGE_PORT)) {
            portType = METADATA_TO_PORT_TYPE.get(EDGE_PORT);
        } else if (isSrMetadataSet(obj, INFRA_PORT) && METADATA_TO_PORT_TYPE.containsKey(INFRA_PORT)) {
            portType = METADATA_TO_PORT_TYPE.get(INFRA_PORT);
        }
        return portType;
    }

    /**
     * Check metadata passed from SegmentRouting app.
     *
     * @param obj the objective containing the metadata
     * @return true if the objective contains valid metadata, false otherwise
     */
    public static boolean isValidSrMetadata(Objective obj) {
        long meta = 0;
        if (obj instanceof FilteringObjective) {
            FilteringObjective filtObj = (FilteringObjective) obj;
            if (filtObj.meta() == null) {
                return true;
            }
            Instructions.MetadataInstruction metaIns = filtObj.meta().writeMetadata();
            if (metaIns == null) {
                return true;
            }
            meta = metaIns.metadata() & metaIns.metadataMask();
        } else if (obj instanceof ForwardingObjective) {
            ForwardingObjective fwdObj = (ForwardingObjective) obj;
            if (fwdObj.meta() == null) {
                return true;
            }
            MetadataCriterion metaCrit = (MetadataCriterion) fwdObj.meta().getCriterion(Criterion.Type.METADATA);
            if (metaCrit == null) {
                return true;
            }
            meta = metaCrit.metadata();
        }
        return meta != 0 && ((meta ^ METADATA_MASK) <= METADATA_MASK);
    }

    /**
     * Verify if a given flag has been set into the metadata.
     *
     * @param obj the objective containing the metadata
     * @param flag the flag to verify
     * @return true if the flag is set, false otherwise
     */
    public static boolean isSrMetadataSet(Objective obj, long flag) {
        long meta = 0;
        if (obj instanceof FilteringObjective) {
            FilteringObjective filtObj = (FilteringObjective) obj;
            if (filtObj.meta() == null) {
                return false;
            }
            Instructions.MetadataInstruction metaIns = filtObj.meta().writeMetadata();
            if (metaIns == null) {
                return false;
            }
            meta = metaIns.metadata() & metaIns.metadataMask();
        } else if (obj instanceof ForwardingObjective) {
            ForwardingObjective fwdObj = (ForwardingObjective) obj;
            if (fwdObj.meta() == null) {
                return false;
            }
            MetadataCriterion metaCrit = (MetadataCriterion) fwdObj.meta().getCriterion(Criterion.Type.METADATA);
            if (metaCrit == null) {
                return false;
            }
            meta = metaCrit.metadata();
        }
        return (meta & flag) == flag;
    }
}
