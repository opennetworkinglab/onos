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

package org.onosproject.net.pi.impl;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.Device;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.PiInstruction;
import org.onosproject.net.pi.model.PiActionModel;
import org.onosproject.net.pi.model.PiActionParamModel;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiMatchFieldModel;
import org.onosproject.net.pi.model.PiMatchType;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.model.PiTableModel;
import org.onosproject.net.pi.model.PiTableType;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiActionSet;
import org.onosproject.net.pi.runtime.PiExactFieldMatch;
import org.onosproject.net.pi.runtime.PiFieldMatch;
import org.onosproject.net.pi.runtime.PiLpmFieldMatch;
import org.onosproject.net.pi.runtime.PiMatchKey;
import org.onosproject.net.pi.runtime.PiOptionalFieldMatch;
import org.onosproject.net.pi.runtime.PiRangeFieldMatch;
import org.onosproject.net.pi.runtime.PiTableAction;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;
import org.onosproject.net.pi.service.PiTranslationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import static java.lang.String.format;
import static org.onlab.util.ImmutableByteSequence.ByteSequenceTrimException;
import static org.onlab.util.ImmutableByteSequence.prefixOnes;
import static org.onosproject.net.flow.criteria.Criterion.Type.PROTOCOL_INDEPENDENT;
import static org.onosproject.net.pi.impl.CriterionTranslatorHelper.translateCriterion;
import static org.onosproject.net.pi.impl.PiUtils.getInterpreterOrNull;
import static org.onosproject.net.pi.impl.PiUtils.translateTableId;

/**
 * Implementation of flow rule translation logic.
 */
final class PiFlowRuleTranslatorImpl {

    public static final int MAX_PI_PRIORITY = (int) Math.pow(2, 24);
    public static final int MIN_PI_PRIORITY = 1;
    private static final Logger log = LoggerFactory.getLogger(PiFlowRuleTranslatorImpl.class);

    private PiFlowRuleTranslatorImpl() {
        // Hide constructor.
    }

    /**
     * Returns a PI table entry equivalent to the given flow rule, for the given
     * pipeconf and device.
     *
     * @param rule     flow rule
     * @param pipeconf pipeconf
     * @param device   device
     * @return PI table entry
     * @throws PiTranslationException if the flow rule cannot be translated
     */
    static PiTableEntry translate(FlowRule rule, PiPipeconf pipeconf, Device device)
            throws PiTranslationException {

        PiPipelineModel pipelineModel = pipeconf.pipelineModel();

        // Retrieve interpreter, if any.
        final PiPipelineInterpreter interpreter = getInterpreterOrNull(device, pipeconf);
        // Get table model.
        final PiTableId piTableId = translateTableId(rule.table(), interpreter);
        final PiTableModel tableModel = getTableModel(piTableId, pipelineModel);
        // Translate selector.
        final PiMatchKey piMatchKey;
        final boolean needPriority;
        if (rule.selector().criteria().isEmpty()) {
            piMatchKey = PiMatchKey.EMPTY;
            needPriority = false;
        } else {
            final Collection<PiFieldMatch> fieldMatches = translateFieldMatches(
                    interpreter, rule.selector(), tableModel);
            piMatchKey = PiMatchKey.builder()
                    .addFieldMatches(fieldMatches)
                    .build();
            // FIXME: P4Runtime limit
            // Need to ignore priority if no TCAM lookup match field
            needPriority = tableModel.matchFields().stream()
                    .anyMatch(match -> match.matchType() == PiMatchType.TERNARY ||
                            match.matchType() == PiMatchType.RANGE ||
                            match.matchType() == PiMatchType.OPTIONAL);
        }
        // Translate treatment.
        final PiTableAction piTableAction = translateTreatment(rule.treatment(), interpreter, piTableId, pipelineModel);

        // Build PI entry.
        final PiTableEntry.Builder tableEntryBuilder = PiTableEntry.builder();

        tableEntryBuilder
                .forTable(piTableId)
                .withMatchKey(piMatchKey);

        if (piTableAction != null) {
            tableEntryBuilder.withAction(piTableAction);
        }

        if (needPriority) {
            // FIXME: move priority check to P4Runtime driver.
            final int newPriority;
            if (rule.priority() > MAX_PI_PRIORITY) {
                log.warn("Flow rule priority too big, setting translated priority to max value {}: {}",
                         MAX_PI_PRIORITY, rule);
                newPriority = MAX_PI_PRIORITY;
            } else {
                newPriority = MIN_PI_PRIORITY + rule.priority();
            }
            tableEntryBuilder.withPriority(newPriority);
        }

        if (!rule.isPermanent()) {
            if (tableModel.supportsAging()) {
                tableEntryBuilder.withTimeout(rule.timeout());
            } else {
                log.debug("Flow rule is temporary, but table '{}' doesn't support " +
                                  "aging, translating to permanent.", tableModel.id());
            }

        }

        return tableEntryBuilder.build();
    }


    /**
     * Returns a PI action equivalent to the given treatment, optionally using
     * the given interpreter. This method also checks that the produced PI table
     * action is suitable for the given table ID and pipeline model. If
     * suitable, the returned action instance will have parameters well-sized,
     * according to the table model.
     *
     * @param treatment     traffic treatment
     * @param interpreter   interpreter
     * @param tableId       PI table ID
     * @param pipelineModel pipeline model
     * @return PI table action
     * @throws PiTranslationException if the treatment cannot be translated or
     *                                if the PI action is not suitable for the
     *                                given pipeline model
     */
    static PiTableAction translateTreatment(TrafficTreatment treatment, PiPipelineInterpreter interpreter,
                                            PiTableId tableId, PiPipelineModel pipelineModel)
            throws PiTranslationException {
        PiTableModel tableModel = getTableModel(tableId, pipelineModel);
        return typeCheckAction(buildAction(treatment, interpreter, tableId), tableModel);
    }

    private static PiTableModel getTableModel(PiTableId piTableId, PiPipelineModel pipelineModel)
            throws PiTranslationException {
        return pipelineModel.table(piTableId)
                .orElseThrow(() -> new PiTranslationException(format(
                        "Not such a table in pipeline model: %s", piTableId)));
    }

    /**
     * Builds a PI action out of the given treatment, optionally using the given
     * interpreter.
     */
    private static PiTableAction buildAction(TrafficTreatment treatment, PiPipelineInterpreter interpreter,
                                             PiTableId tableId)
            throws PiTranslationException {

        PiTableAction piTableAction = null;

        // If treatment has only one instruction of type PiInstruction, use that.
        for (Instruction inst : treatment.allInstructions()) {
            if (inst.type() == Instruction.Type.PROTOCOL_INDEPENDENT) {
                if (treatment.allInstructions().size() == 1) {
                    piTableAction = ((PiInstruction) inst).action();
                } else {
                    throw new PiTranslationException(format(
                            "Unable to translate treatment, found multiple instructions " +
                                    "of which one is protocol-independent: %s", treatment));
                }
            }
        }

        if (piTableAction == null && interpreter != null) {
            // No PiInstruction, use interpreter to build action.
            try {
                piTableAction = interpreter.mapTreatment(treatment, tableId);
            } catch (PiPipelineInterpreter.PiInterpreterException e) {
                throw new PiTranslationException(
                        "Interpreter was unable to translate treatment. " + e.getMessage());
            }
        }

        return piTableAction;
    }

    private static PiTableAction typeCheckAction(PiTableAction piTableAction, PiTableModel table)
            throws PiTranslationException {
        if (piTableAction == null) {
            // skip check if null
            return null;
        }
        switch (piTableAction.type()) {
            case ACTION:
                return checkPiAction((PiAction) piTableAction, table);
            case ACTION_SET:
                for (var actProfAct : ((PiActionSet) piTableAction).actions()) {
                    checkPiAction(actProfAct.action(), table);
                }
            case ACTION_PROFILE_GROUP_ID:
                if (table.actionProfile() == null || !table.actionProfile().hasSelector()) {
                    throw new PiTranslationException(format(
                            "action is of type '%s', but table '%s' does not" +
                                    "implement an action profile with dynamic selection",
                            piTableAction.type(), table.id()));
                }
            case ACTION_PROFILE_MEMBER_ID:
                if (!table.tableType().equals(PiTableType.INDIRECT)) {
                    throw new PiTranslationException(format(
                            "action is indirect of type '%s', but table '%s' is of type '%s'",
                            piTableAction.type(), table.id(), table.tableType()));
                }
                if (!piTableAction.type().equals(PiTableAction.Type.ACTION_SET) &&
                        table.oneShotOnly()) {
                    throw new PiTranslationException(format(
                            "table '%s' supports only one shot programming", table.id()
                    ));
                }
                return piTableAction;
            default:
                throw new PiTranslationException(format(
                        "Unknown table action type %s", piTableAction.type()));

        }
    }

    private static PiTableAction checkPiAction(PiAction piAction, PiTableModel table)
            throws PiTranslationException {
        // Table supports this action?
        PiActionModel actionModel = table.action(piAction.id()).orElseThrow(
                () -> new PiTranslationException(format("Not such action '%s' for table '%s'",
                                                        piAction.id(), table.id())));

        // Is the number of runtime parameters correct?
        if (actionModel.params().size() != piAction.parameters().size()) {
            throw new PiTranslationException(format(
                    "Wrong number of runtime parameters for action '%s', expected %d but found %d",
                    actionModel.id(), actionModel.params().size(), piAction.parameters().size()));
        }

        // Forge a new action instance with well-sized parameters.
        // The same comment as in typeCheckFieldMatch() about duplicating field match instances applies here.
        PiAction.Builder newActionBuilder = PiAction.builder().withId(piAction.id());
        for (PiActionParam param : piAction.parameters()) {
            PiActionParamModel paramModel = actionModel.param(param.id())
                    .orElseThrow(() -> new PiTranslationException(format(
                            "Not such parameter '%s' for action '%s'", param.id(), actionModel)));
            try {
                newActionBuilder.withParameter(new PiActionParam(param.id(),
                                                                 paramModel.hasBitWidth() ?
                                                                         param.value().fit(paramModel.bitWidth()) :
                                                                         param.value()));
            } catch (ByteSequenceTrimException e) {
                throw new PiTranslationException(format(
                        "Size mismatch for parameter '%s' of action '%s': %s",
                        param.id(), piAction.id(), e.getMessage()));
            }
        }

        return newActionBuilder.build();
    }

    /**
     * Builds a collection of PI field matches out of the given selector,
     * optionally using the given interpreter. The field matches returned are
     * guaranteed to be compatible for the given table model.
     */
    private static Collection<PiFieldMatch> translateFieldMatches(PiPipelineInterpreter interpreter,
                                                                  TrafficSelector selector, PiTableModel tableModel)
            throws PiTranslationException {

        Map<PiMatchFieldId, PiFieldMatch> fieldMatches = Maps.newHashMap();

        // If present, find a PiCriterion and get its field matches as a map. Otherwise, use an empty map.
        Map<PiMatchFieldId, PiFieldMatch> piCriterionFields = selector.criteria().stream()
                .filter(c -> c.type().equals(PROTOCOL_INDEPENDENT))
                .map(c -> (PiCriterion) c)
                .findFirst()
                .map(PiCriterion::fieldMatches)
                .map(c -> {
                    Map<PiMatchFieldId, PiFieldMatch> fieldMap = Maps.newHashMap();
                    c.forEach(fieldMatch -> fieldMap.put(fieldMatch.fieldId(), fieldMatch));
                    return fieldMap;
                })
                .orElse(Maps.newHashMap());

        Set<Criterion> translatedCriteria = Sets.newHashSet();
        Set<Criterion> ignoredCriteria = Sets.newHashSet();
        Set<PiMatchFieldId> usedPiCriterionFields = Sets.newHashSet();
        Set<PiMatchFieldId> ignoredPiCriterionFields = Sets.newHashSet();


        Map<PiMatchFieldId, Criterion> criterionMap = Maps.newHashMap();
        if (interpreter != null) {
            // NOTE: if two criterion types map to the same match field ID, and
            //  those two criterion types are present in the selector, this won't
            //  work. This is unlikely to happen since those cases should be
            //  mutually exclusive:
            //  e.g. ICMPV6_TYPE ->  metadata.my_normalized_icmp_type
            //       ICMPV4_TYPE ->  metadata.my_normalized_icmp_type
            //  A packet can be either ICMPv6 or ICMPv4 but not both.
            selector.criteria()
                    .stream()
                    .map(Criterion::type)
                    .filter(t -> t != PROTOCOL_INDEPENDENT)
                    .forEach(t -> {
                        PiMatchFieldId mfid = interpreter.mapCriterionType(t)
                                .orElse(null);
                        if (mfid != null) {
                            if (criterionMap.containsKey(mfid)) {
                                log.warn("Detected criterion mapping " +
                                                 "conflict for PiMatchFieldId {}",
                                         mfid);
                            }
                            criterionMap.put(mfid, selector.getCriterion(t));
                        }
                    });
        }

        for (PiMatchFieldModel fieldModel : tableModel.matchFields()) {

            PiMatchFieldId fieldId = fieldModel.id();

            int bitWidth = fieldModel.bitWidth();

            Criterion criterion = criterionMap.get(fieldId);

            if (!piCriterionFields.containsKey(fieldId) && criterion == null) {
                // Neither a field in PiCriterion is available nor a Criterion mapping is possible.
                // Can ignore if match is ternary-like, as it means "don't care".
                switch (fieldModel.matchType()) {
                    case TERNARY:
                    case LPM:
                    case RANGE:
                    case OPTIONAL:
                        // Skip field.
                        break;
                    default:
                        throw new PiTranslationException(format(
                                "No value found for required match field '%s'", fieldId));
                }
                // Next field.
                continue;
            }

            PiFieldMatch fieldMatch = null;

            // TODO: we currently do not support fields with arbitrary bit width
            if (criterion != null && fieldModel.hasBitWidth()) {
                // Criterion mapping is possible for this field id.
                try {
                    fieldMatch = translateCriterion(criterion, fieldId, fieldModel.matchType(), bitWidth);
                    translatedCriteria.add(criterion);
                } catch (PiTranslationException ex) {
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
                        throw new PiTranslationException(format(
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
            throw new PiTranslationException(format(
                    "The following criteria cannot be translated for table '%s': %s",
                    tableModel.id(), skippedCriteriaJoiner.toString()));
        }

        // Check if all fields found in PiCriterion have been used.
        StringJoiner skippedPiFieldsJoiner = new StringJoiner(", ");
        piCriterionFields.keySet().stream()
                .filter(k -> !usedPiCriterionFields.contains(k) && !ignoredPiCriterionFields.contains(k))
                .forEach(k -> skippedPiFieldsJoiner.add(k.id()));
        if (skippedPiFieldsJoiner.length() > 0) {
            throw new PiTranslationException(format(
                    "The following PiCriterion field matches are not supported in table '%s': %s",
                    tableModel.id(), skippedPiFieldsJoiner.toString()));
        }

        return fieldMatches.values();
    }

    private static PiFieldMatch typeCheckFieldMatch(PiFieldMatch fieldMatch, PiMatchFieldModel fieldModel)
            throws PiTranslationException {

        // Check parameter type and size
        if (!fieldModel.matchType().equals(fieldMatch.type())) {
            throw new PiTranslationException(format(
                    "Wrong match type for field '%s', expected %s, but found %s",
                    fieldMatch.fieldId(), fieldModel.matchType().name(), fieldMatch.type().name()));
        }

        // Check if the arbitrary bit width is supported
        if (!fieldModel.hasBitWidth() &&
                !fieldModel.matchType().equals(PiMatchType.EXACT) &&
                !fieldModel.matchType().equals(PiMatchType.OPTIONAL)) {
            throw new PiTranslationException(format(
                    "Arbitrary bit width for field '%s' and match type %s is not supported",
                    fieldMatch.fieldId(), fieldModel.matchType().name()));
        }

        int modelBitWidth = fieldModel.bitWidth();

        /*
        Here we try to be robust against wrong size fields with the goal of having PiCriterion independent of the
        pipeline model. We duplicate the field match, fitting the byte sequences to the bit-width specified in the
        model. We also normalize ternary (and LPM) field matches by setting to 0 unused bits, as required by P4Runtime.

        These operations are expensive when performed for each field match of each flow rule, but should be
        mitigated by the translation cache provided by PiFlowRuleTranslationServiceImpl.
        */

        try {
            switch (fieldModel.matchType()) {
                case EXACT:
                    PiExactFieldMatch exactField = (PiExactFieldMatch) fieldMatch;
                    return new PiExactFieldMatch(fieldMatch.fieldId(),
                                                 fieldModel.hasBitWidth() ?
                                                         exactField.value().fit(modelBitWidth) :
                                                         exactField.value());
                case TERNARY:
                    PiTernaryFieldMatch ternField = (PiTernaryFieldMatch) fieldMatch;
                    ImmutableByteSequence ternMask = ternField.mask().fit(modelBitWidth);
                    ImmutableByteSequence ternValue = ternField.value()
                            .fit(modelBitWidth)
                            .bitwiseAnd(ternMask);
                    return new PiTernaryFieldMatch(fieldMatch.fieldId(), ternValue, ternMask);
                case LPM:
                    PiLpmFieldMatch lpmfield = (PiLpmFieldMatch) fieldMatch;
                    if (lpmfield.prefixLength() > modelBitWidth) {
                        throw new PiTranslationException(format(
                                "Invalid prefix length for LPM field '%s', found %d but field has bit-width %d",
                                fieldMatch.fieldId(), lpmfield.prefixLength(), modelBitWidth));
                    }
                    ImmutableByteSequence lpmValue = lpmfield.value()
                            .fit(modelBitWidth);
                    ImmutableByteSequence lpmMask = prefixOnes(lpmValue.size(),
                                                               lpmfield.prefixLength());
                    lpmValue = lpmValue.bitwiseAnd(lpmMask);
                    return new PiLpmFieldMatch(fieldMatch.fieldId(),
                                               lpmValue, lpmfield.prefixLength());
                case RANGE:
                    return new PiRangeFieldMatch(fieldMatch.fieldId(),
                                                 ((PiRangeFieldMatch) fieldMatch).lowValue().fit(modelBitWidth),
                                                 ((PiRangeFieldMatch) fieldMatch).highValue().fit(modelBitWidth));
                case OPTIONAL:
                    PiOptionalFieldMatch optionalField = (PiOptionalFieldMatch) fieldMatch;
                    return new PiOptionalFieldMatch(fieldMatch.fieldId(),
                                                    fieldModel.hasBitWidth() ?
                                                            optionalField.value().fit(modelBitWidth) :
                                                            optionalField.value());
                default:
                    // Should never be here.
                    throw new IllegalArgumentException(
                            "Unrecognized match type " + fieldModel.matchType().name());
            }
        } catch (ByteSequenceTrimException e) {
            throw new PiTranslationException(format(
                    "Size mismatch for field %s: %s", fieldMatch.fieldId(), e.getMessage()));
        }
    }
}
