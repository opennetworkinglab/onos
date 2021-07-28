/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.pipelines.fabric.impl.behaviour.upf;

import org.apache.commons.lang3.tuple.Pair;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.behaviour.upf.UpfProgrammableException;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.PiInstruction;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiMatchType;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiExactFieldMatch;
import org.onosproject.net.pi.runtime.PiFieldMatch;
import org.onosproject.net.pi.runtime.PiLpmFieldMatch;
import org.onosproject.net.pi.runtime.PiRangeFieldMatch;
import org.onosproject.net.pi.runtime.PiTableAction;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;

import java.util.Optional;

/**
 * Utility class for manipulation of FlowRules and PiTableEntry objects specific to fabric-tna.
 */
final class FabricUpfTranslatorUtil {

    private FabricUpfTranslatorUtil() {
    }

    static ImmutableByteSequence getFieldValue(PiFieldMatch field, PiMatchFieldId fieldId)
            throws UpfProgrammableException {
        if (field == null) {
            throw new UpfProgrammableException(
                    String.format("Unable to find field %s where expected!", fieldId.toString()));
        }
        if (field.type() == PiMatchType.EXACT) {
            return ((PiExactFieldMatch) field).value();
        } else if (field.type() == PiMatchType.LPM) {
            return ((PiLpmFieldMatch) field).value();
        } else if (field.type() == PiMatchType.TERNARY) {
            return ((PiTernaryFieldMatch) field).value();
        } else if (field.type() == PiMatchType.RANGE) {
            return ((PiRangeFieldMatch) field).lowValue();
        } else {
            throw new UpfProgrammableException(
                    String.format("Field %s has unknown match type: %s", fieldId.toString(), field.type().toString()));
        }
    }

    static ImmutableByteSequence getFieldValue(PiCriterion criterion, PiMatchFieldId fieldId)
            throws UpfProgrammableException {
        return getFieldValue(criterion.fieldMatch(fieldId).orElse(null), fieldId);
    }

    static boolean fieldIsPresent(PiCriterion criterion, PiMatchFieldId fieldId) {
        return criterion.fieldMatch(fieldId).isPresent();
    }

    static ImmutableByteSequence getParamValue(PiAction action, PiActionParamId paramId)
            throws UpfProgrammableException {

        for (PiActionParam param : action.parameters()) {
            if (param.id().equals(paramId)) {
                return param.value();
            }
        }
        throw new UpfProgrammableException(
                String.format("Unable to find parameter %s where expected!", paramId.toString()));
    }

    static int getFieldInt(PiCriterion criterion, PiMatchFieldId fieldId)
            throws UpfProgrammableException {
        return byteSeqToInt(getFieldValue(criterion, fieldId));
    }

    static byte getFieldByte(PiCriterion criterion, PiMatchFieldId fieldId)
            throws UpfProgrammableException {
        return byteSeqToByte(getFieldValue(criterion, fieldId));
    }

    static int getParamInt(PiAction action, PiActionParamId paramId)
            throws UpfProgrammableException {
        return byteSeqToInt(getParamValue(action, paramId));
    }

    static byte getParamByte(PiAction action, PiActionParamId paramId)
            throws UpfProgrammableException {
        return byteSeqToByte(getParamValue(action, paramId));
    }

    static Ip4Address getParamAddress(PiAction action, PiActionParamId paramId)
            throws UpfProgrammableException {
        return Ip4Address.valueOf(getParamValue(action, paramId).asArray());
    }

    static Ip4Prefix getFieldPrefix(PiCriterion criterion, PiMatchFieldId fieldId) {
        Optional<PiFieldMatch> optField = criterion.fieldMatch(fieldId);
        if (optField.isEmpty()) {
            return null;
        }
        PiLpmFieldMatch field = (PiLpmFieldMatch) optField.get();
        Ip4Address address = Ip4Address.valueOf(field.value().asArray());
        return Ip4Prefix.valueOf(address, field.prefixLength());
    }

    static Ip4Address getFieldAddress(PiCriterion criterion, PiMatchFieldId fieldId)
            throws UpfProgrammableException {
        return Ip4Address.valueOf(getFieldValue(criterion, fieldId).asArray());
    }

    static int byteSeqToInt(ImmutableByteSequence sequence) {
        try {
            return sequence.fit(32).asReadOnlyBuffer().getInt();
        } catch (ImmutableByteSequence.ByteSequenceTrimException e) {
            throw new IllegalArgumentException("Attempted to convert a >4 byte wide sequence to an integer!");
        }
    }

    static byte byteSeqToByte(ImmutableByteSequence sequence) {
        try {
            return sequence.fit(8).asReadOnlyBuffer().get();
        } catch (ImmutableByteSequence.ByteSequenceTrimException e) {
            throw new IllegalArgumentException("Attempted to convert a >1 byte wide sequence to a byte!");
        }
    }

    static Pair<PiCriterion, PiTableAction> fabricEntryToPiPair(FlowRule entry) {
        PiCriterion match = (PiCriterion) entry.selector().getCriterion(Criterion.Type.PROTOCOL_INDEPENDENT);
        PiTableAction action = null;
        for (Instruction instruction : entry.treatment().allInstructions()) {
            if (instruction.type() == Instruction.Type.PROTOCOL_INDEPENDENT) {
                PiInstruction piInstruction = (PiInstruction) instruction;
                action = piInstruction.action();
                break;
            }
        }
        return Pair.of(match, action);
    }
}
