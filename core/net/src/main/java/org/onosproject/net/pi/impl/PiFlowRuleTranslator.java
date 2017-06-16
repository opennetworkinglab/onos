/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.net.pi.impl;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.Device;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.IndexTableId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.PiInstruction;
import org.onosproject.net.pi.model.PiActionModel;
import org.onosproject.net.pi.model.PiActionParamModel;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.model.PiTableMatchFieldModel;
import org.onosproject.net.pi.model.PiTableModel;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiExactFieldMatch;
import org.onosproject.net.pi.runtime.PiFieldMatch;
import org.onosproject.net.pi.runtime.PiHeaderFieldId;
import org.onosproject.net.pi.runtime.PiLpmFieldMatch;
import org.onosproject.net.pi.runtime.PiRangeFieldMatch;
import org.onosproject.net.pi.runtime.PiTableAction;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTableId;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import static java.lang.String.format;
import static org.onlab.util.ImmutableByteSequence.ByteSequenceTrimException;
import static org.onlab.util.ImmutableByteSequence.fit;
import static org.onosproject.net.flow.criteria.Criterion.Type.PROTOCOL_INDEPENDENT;
import static org.onosproject.net.pi.impl.CriterionTranslatorHelper.translateCriterion;
import static org.onosproject.net.pi.runtime.PiFlowRuleTranslationService.PiFlowRuleTranslationException;

/**
 * Implementation of flow rule translation logic.
 */
final class PiFlowRuleTranslator {

    private static final Logger log = LoggerFactory.getLogger(PiFlowRuleTranslationServiceImpl.class);

    private PiFlowRuleTranslator() {
        // Hide constructor.
    }

    static PiTableEntry translateFlowRule(FlowRule rule, PiPipeconf pipeconf, Device device)
            throws PiFlowRuleTranslationException {

        PiPipelineModel pipelineModel = pipeconf.pipelineModel();

        // Retrieve interpreter, if any.
        // FIXME: get interpreter via driver once implemented.
        // final PiPipelineInterpreter interpreter = device.is(PiPipelineInterpreter.class)
        //        ? device.as(PiPipelineInterpreter.class) : null;

        final PiPipelineInterpreter interpreter;
        try {
            interpreter = (PiPipelineInterpreter) pipeconf.implementation(PiPipelineInterpreter.class)
                    .orElse(null)
                    .newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new PiFlowRuleTranslationException(format(
                    "Unable to instantiate interpreter of pipeconf %s", pipeconf.id()));
        }

        PiTableId piTableId;
        switch (rule.table().type()) {
            case PIPELINE_INDEPENDENT:
                piTableId = (PiTableId) rule.table();
                break;
            case INDEX:
                IndexTableId indexId = (IndexTableId) rule.table();
                if (interpreter == null) {
                    throw new PiFlowRuleTranslationException(format(
                            "Unable to map table ID '%d' from index to PI: missing interpreter", indexId.id()));
                } else if (!interpreter.mapFlowRuleTableId(indexId.id()).isPresent()) {
                    throw new PiFlowRuleTranslationException(format(
                            "Unable to map table ID '%d' from index to PI: missing ID in interpreter", indexId.id()));
                } else {
                    piTableId = interpreter.mapFlowRuleTableId(indexId.id()).get();
                }
                break;
            default:
                throw new PiFlowRuleTranslationException(format(
                        "Unrecognized table ID type %s", rule.table().type().name()));
        }

        PiTableModel table = pipelineModel.table(piTableId.toString())
                .orElseThrow(() -> new PiFlowRuleTranslationException(format(
                        "Not such a table in pipeline model: %s", piTableId)));

        /* Translate selector */
        Collection<PiFieldMatch> fieldMatches = buildFieldMatches(interpreter, rule.selector(), table);

        /* Translate treatment */
        PiAction piAction = buildAction(rule.treatment(), interpreter, pipeconf);
        piAction = typeCheckAction(piAction, table);

        PiTableEntry.Builder tableEntryBuilder = PiTableEntry.builder();

        // In BMv2 0 is the highest priority.
        // FIXME: Check P4Runtime and agree on maximum priority in the TableEntry javadoc.
        // int newPriority = Integer.MAX_VALUE - rule.priority();

        tableEntryBuilder
                .forTable(piTableId)
                .withPriority(rule.priority())
                .withFieldMatches(fieldMatches)
                .withAction(piAction);

        if (!rule.isPermanent()) {
            if (table.supportsAging()) {
                tableEntryBuilder.withTimeout((double) rule.timeout());
            } else {
                log.warn("Flow rule is temporary, but table '{}' doesn't support " +
                                 "aging, translating to permanent.", table.name());
            }

        }

        return tableEntryBuilder.build();
    }

    /**
     * Builds a PI action out of the given treatment, optionally using the given interpreter.
     */
    private static PiAction buildAction(TrafficTreatment treatment, PiPipelineInterpreter interpreter,
                                        PiPipeconf pipeconf)
            throws PiFlowRuleTranslationException {

        PiTableAction piTableAction = null;

        // If treatment has only one instruction of type PiInstruction, use that.
        for (Instruction inst : treatment.allInstructions()) {
            if (inst.type() == Instruction.Type.PROTOCOL_INDEPENDENT) {
                if (treatment.allInstructions().size() == 1) {
                    piTableAction = ((PiInstruction) inst).action();
                } else {
                    throw new PiFlowRuleTranslationException(format(
                            "Unable to translate treatment, found multiple instructions " +
                                    "of which one is protocol-independent: %s", treatment));
                }
            }
        }

        if (piTableAction == null && interpreter != null) {
            // No PiInstruction, use interpreter to build action.
            try {
                piTableAction = interpreter.mapTreatment(treatment, pipeconf);
            } catch (PiPipelineInterpreter.PiInterpreterException e) {
                throw new PiFlowRuleTranslationException(
                        "Interpreter was unable to translate treatment. " + e.getMessage());
            }
        }

        if (piTableAction == null) {
            // No PiInstruction, no interpreter. It's time to give up.
            throw new PiFlowRuleTranslationException(
                    "Unable to translate treatment, neither an interpreter or a "
                            + "protocol-independent instruction were provided.");
        }

        if (piTableAction.type() != PiTableAction.Type.ACTION) {
            // TODO: implement handling of other table action types, e.g. action profiles.
            throw new PiFlowRuleTranslationException(format(
                    "PiTableAction type %s is not supported yet.", piTableAction.type()));
        }

        return (PiAction) piTableAction;
    }

    /**
     * Checks that the given PI action is suitable for the given table model and returns a new action instance with
     * parameters well-sized, according to the table model. If not suitable, throws an exception explaining why.
     */
    private static PiAction typeCheckAction(PiAction piAction, PiTableModel table)
            throws PiFlowRuleTranslationException {

        // Table supports this action?
        PiActionModel actionModel = table.action(piAction.id().name()).orElseThrow(
                () -> new PiFlowRuleTranslationException(format("Not such action '%s' for table '%s'",
                                                                piAction.id(), table.name())));

        // Is the number of runtime parameters correct?
        if (actionModel.params().size() != piAction.parameters().size()) {
            throw new PiFlowRuleTranslationException(format(
                    "Wrong number of runtime parameters for action '%s', expected %d but found %d",
                    actionModel.name(), actionModel.params().size(), piAction.parameters().size()));
        }

        // Forge a new action instance with well-sized parameters.
        // The same comment as in typeCheckFieldMatch() about duplicating field match instances applies here.
        PiAction.Builder newActionBuilder = PiAction.builder().withId(piAction.id());
        for (PiActionParam param : piAction.parameters()) {
            PiActionParamModel paramModel = actionModel.param(param.id().name())
                    .orElseThrow(() -> new PiFlowRuleTranslationException(format(
                            "Not such parameter '%s' for action '%s'", param.id(), actionModel.name())));
            try {
                newActionBuilder.withParameter(new PiActionParam(param.id(),
                                                                 fit(param.value(), paramModel.bitWidth())));
            } catch (ByteSequenceTrimException e) {
                throw new PiFlowRuleTranslationException(format(
                        "Size mismatch for parameter '%s' of action '%s': %s",
                        param.id(), piAction.id(), e.getMessage()));
            }
        }

        return newActionBuilder.build();
    }

    /**
     * Builds a collection of PI field matches out of the given selector, optionally using the given interpreter. The
     * field matches returned are guaranteed to be compatible for the given table model.
     */
    private static Collection<PiFieldMatch> buildFieldMatches(PiPipelineInterpreter interpreter,
                                                              TrafficSelector selector, PiTableModel tableModel)
            throws PiFlowRuleTranslationException {

        Map<PiHeaderFieldId, PiFieldMatch> fieldMatches = Maps.newHashMap();

        // If present, find a PiCriterion and get its field matches as a map. Otherwise, use an empty map.
        Map<PiHeaderFieldId, PiFieldMatch> piCriterionFields = selector.criteria().stream()
                .filter(c -> c.type().equals(PROTOCOL_INDEPENDENT))
                .map(c -> (PiCriterion) c)
                .findFirst()
                .map(PiCriterion::fieldMatches)
                .map(c -> {
                    Map<PiHeaderFieldId, PiFieldMatch> fieldMap = Maps.newHashMap();
                    c.forEach(fieldMatch -> fieldMap.put(fieldMatch.fieldId(), fieldMatch));
                    return fieldMap;
                })
                .orElse(Maps.newHashMap());

        Set<Criterion> translatedCriteria = Sets.newHashSet();
        Set<Criterion> ignoredCriteria = Sets.newHashSet();
        Set<PiHeaderFieldId> usedPiCriterionFields = Sets.newHashSet();
        Set<PiHeaderFieldId> ignoredPiCriterionFields = Sets.newHashSet();

        for (PiTableMatchFieldModel fieldModel : tableModel.matchFields()) {

            PiHeaderFieldId fieldId = PiHeaderFieldId.of(fieldModel.field().header().type().name(),
                                                         fieldModel.field().type().name(),
                                                         fieldModel.field().header().index());

            int bitWidth = fieldModel.field().type().bitWidth();
            int fieldByteWidth = (int) Math.ceil((double) bitWidth / 8);

            Optional<Criterion.Type> criterionType =
                    interpreter == null
                            ? Optional.empty()
                            : interpreter.mapPiHeaderFieldId(fieldId);

            Criterion criterion = criterionType.map(selector::getCriterion).orElse(null);

            if (!piCriterionFields.containsKey(fieldId) && criterion == null) {
                // Neither a field in PiCriterion is available nor a Criterion mapping is possible.
                // Can ignore if the match is ternary or LPM.
                switch (fieldModel.matchType()) {
                    case TERNARY:
                        // Wildcard the whole field.
                        fieldMatches.put(fieldId, new PiTernaryFieldMatch(
                                fieldId,
                                ImmutableByteSequence.ofZeros(fieldByteWidth),
                                ImmutableByteSequence.ofZeros(fieldByteWidth)));
                        break;
                    case LPM:
                        // LPM with prefix 0
                        fieldMatches.put(fieldId, new PiLpmFieldMatch(fieldId,
                                                                      ImmutableByteSequence.ofZeros(fieldByteWidth),
                                                                      0));
                        break;
                    // FIXME: Can we handle the case of RANGE or VALID match?
                    default:
                        throw new PiFlowRuleTranslationException(format(
                                "No value found for required match field '%s'", fieldId));
                }
                // Next field.
                continue;
            }

            PiFieldMatch fieldMatch = null;

            if (criterion != null) {
                // Criterion mapping is possible for this field id.
                try {
                    fieldMatch = translateCriterion(criterion, fieldId, fieldModel.matchType(), bitWidth);
                    translatedCriteria.add(criterion);
                } catch (PiFlowRuleTranslationException ex) {
                    // Ignore exception if the same field was found in PiCriterion.
                    if (piCriterionFields.containsKey(fieldId)) {
                        ignoredCriteria.add(criterion);
                    } else {
                        throw ex;
                    }
                }
            }

            if (piCriterionFields.containsKey(fieldId)) {
                // Field was found in PiCriterion.
                if (fieldMatch != null) {
                    // Field was already translated from other criterion.
                    // Throw exception only if we are trying to match on different values of the same field...
                    if (!fieldMatch.equals(piCriterionFields.get(fieldId))) {
                        throw new PiFlowRuleTranslationException(format(
                                "Duplicate match field '%s': instance translated from criterion '%s' is different to " +
                                        "what found in PiCriterion.", fieldId, criterion.type()));
                    }
                    ignoredPiCriterionFields.add(fieldId);
                } else {
                    fieldMatch = piCriterionFields.get(fieldId);
                    fieldMatch = typeCheckFieldMatch(fieldMatch, fieldModel);
                    usedPiCriterionFields.add(fieldId);
                }
            }

            fieldMatches.put(fieldId, fieldMatch);
        }

        // Check if all criteria have been translated.
        StringJoiner skippedCriteriaJoiner = new StringJoiner(", ");
        selector.criteria().stream()
                .filter(c -> !c.type().equals(PROTOCOL_INDEPENDENT))
                .filter(c -> !translatedCriteria.contains(c) && !ignoredCriteria.contains(c))
                .forEach(c -> skippedCriteriaJoiner.add(c.type().name()));
        if (skippedCriteriaJoiner.length() > 0) {
            throw new PiFlowRuleTranslationException(format(
                    "The following criteria cannot be translated for table '%s': %s",
                    tableModel.name(), skippedCriteriaJoiner.toString()));
        }

        // Check if all fields found in PiCriterion have been used.
        StringJoiner skippedPiFieldsJoiner = new StringJoiner(", ");
        piCriterionFields.keySet().stream()
                .filter(k -> !usedPiCriterionFields.contains(k) && !ignoredPiCriterionFields.contains(k))
                .forEach(k -> skippedPiFieldsJoiner.add(k.id()));
        if (skippedPiFieldsJoiner.length() > 0) {
            throw new PiFlowRuleTranslationException(format(
                    "The following PiCriterion field matches are not supported in table '%s': %s",
                    tableModel.name(), skippedPiFieldsJoiner.toString()));
        }

        return fieldMatches.values();
    }

    private static PiFieldMatch typeCheckFieldMatch(PiFieldMatch fieldMatch, PiTableMatchFieldModel fieldModel)
            throws PiFlowRuleTranslationException {

        // Check parameter type and size
        if (!fieldModel.matchType().equals(fieldMatch.type())) {
            throw new PiFlowRuleTranslationException(format(
                    "Wrong match type for field '%s', expected %s, but found %s",
                    fieldMatch.fieldId(), fieldModel.matchType().name(), fieldMatch.type().name()));
        }

        int modelBitWidth = fieldModel.field().type().bitWidth();

        /*
        Here we try to be robust against wrong size fields with the goal of having PiCriterion independent of the
        pipeline model. We duplicate the field match, fitting the byte sequences to the bit-width specified in the
        model. This operation is expensive when performed for each field match of each flow rule, but should be
        mitigated by the translation cache provided by PiFlowRuleTranslationServiceImpl.
        */

        try {
            switch (fieldModel.matchType()) {
                case EXACT:
                    return new PiExactFieldMatch(fieldMatch.fieldId(),
                                                 fit(((PiExactFieldMatch) fieldMatch).value(), modelBitWidth));
                case TERNARY:
                    return new PiTernaryFieldMatch(fieldMatch.fieldId(),
                                                   fit(((PiTernaryFieldMatch) fieldMatch).value(), modelBitWidth),
                                                   fit(((PiTernaryFieldMatch) fieldMatch).mask(), modelBitWidth));
                case LPM:
                    PiLpmFieldMatch lpmfield = (PiLpmFieldMatch) fieldMatch;
                    if (lpmfield.prefixLength() > modelBitWidth) {
                        throw new PiFlowRuleTranslationException(format(
                                "Invalid prefix length for LPM field '%s', found %d but field has bit-width %d",
                                fieldMatch.fieldId(), lpmfield.prefixLength(), modelBitWidth));
                    }
                    return new PiLpmFieldMatch(fieldMatch.fieldId(),
                                               fit(lpmfield.value(), modelBitWidth),
                                               lpmfield.prefixLength());
                case RANGE:
                    return new PiRangeFieldMatch(fieldMatch.fieldId(),
                                                 fit(((PiRangeFieldMatch) fieldMatch).lowValue(), modelBitWidth),
                                                 fit(((PiRangeFieldMatch) fieldMatch).highValue(), modelBitWidth));
                case VALID:
                    return fieldMatch;
                default:
                    // Should never be here.
                    throw new RuntimeException(
                            "Unrecognized match type " + fieldModel.matchType().name());
            }
        } catch (ByteSequenceTrimException e) {
            throw new PiFlowRuleTranslationException(format(
                    "Size mismatch for field %s: %s", fieldMatch.fieldId(), e.getMessage()));
        }
    }
}
