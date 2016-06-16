/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.bmv2.ctl;

import com.google.common.annotations.Beta;
import com.google.common.collect.Sets;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.context.Bmv2ActionModel;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2DeviceContext;
import org.onosproject.bmv2.api.context.Bmv2FieldModel;
import org.onosproject.bmv2.api.context.Bmv2FlowRuleTranslator;
import org.onosproject.bmv2.api.context.Bmv2FlowRuleTranslatorException;
import org.onosproject.bmv2.api.context.Bmv2Interpreter;
import org.onosproject.bmv2.api.context.Bmv2InterpreterException;
import org.onosproject.bmv2.api.context.Bmv2RuntimeDataModel;
import org.onosproject.bmv2.api.context.Bmv2TableKeyModel;
import org.onosproject.bmv2.api.context.Bmv2TableModel;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.bmv2.api.runtime.Bmv2ExactMatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionSelector;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionTreatment;
import org.onosproject.bmv2.api.runtime.Bmv2LpmMatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2MatchKey;
import org.onosproject.bmv2.api.runtime.Bmv2MatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2TableEntry;
import org.onosproject.bmv2.api.runtime.Bmv2TernaryMatchParam;
import org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.ExtensionCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.Instructions.ExtensionInstructionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils.roundToBytes;
import static org.onosproject.net.flow.criteria.Criterion.Type.EXTENSION;
import static org.onosproject.net.flow.criteria.ExtensionSelectorType.ExtensionSelectorTypes.BMV2_MATCH_PARAMS;

/**
 * Default implementation of a BMv2 flow rule translator.
 */
@Beta
public class Bmv2FlowRuleTranslatorImpl implements Bmv2FlowRuleTranslator {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public Bmv2TableEntry translate(FlowRule rule, Bmv2DeviceContext context)
            throws Bmv2FlowRuleTranslatorException {

        Bmv2Configuration configuration = context.configuration();
        Bmv2Interpreter interpreter = context.interpreter();

        int tableId = rule.tableId();
        String tableName = interpreter.tableIdMap().get(tableId);

        Bmv2TableModel table = (tableName == null) ? configuration.table(tableId) : configuration.table(tableName);

        if (table == null) {
            throw new Bmv2FlowRuleTranslatorException("Unknown table ID: " + tableId);
        }

        /* Translate selector */
        Bmv2MatchKey bmv2MatchKey = buildMatchKey(interpreter, rule.selector(), table);

        /* Translate treatment */
        TrafficTreatment treatment = rule.treatment();
        Bmv2Action bmv2Action = null;
        // If treatment has only 1 instruction of type extension, use that
        for (Instruction inst : treatment.allInstructions()) {
            if (inst.type() == Instruction.Type.EXTENSION) {
                if (treatment.allInstructions().size() == 1) {
                    bmv2Action = getActionFromExtension((ExtensionInstructionWrapper) inst);
                } else {
                    throw new Bmv2FlowRuleTranslatorException("Unable to translate traffic treatment, found multiple " +
                                                                      "instructions of which one is an extension: " +
                                                                      treatment.toString());
                }
            }
        }

        if (bmv2Action == null) {
            // No extension, use interpreter to build action.
            try {
                bmv2Action = interpreter.mapTreatment(treatment, configuration);
            } catch (Bmv2InterpreterException e) {
                throw new Bmv2FlowRuleTranslatorException("Unable to translate treatment. " + e.toString());
            }
        }

        if (bmv2Action == null) {
            // Interpreter returned null.
            throw new Bmv2FlowRuleTranslatorException("Unable to translate treatment");
        }

        // Check action
        Bmv2ActionModel actionModel = configuration.action(bmv2Action.name());
        if (actionModel == null) {
            throw new Bmv2FlowRuleTranslatorException("Unknown action " + bmv2Action.name());
        }
        if (!table.actions().contains(actionModel)) {
            throw new Bmv2FlowRuleTranslatorException("Action " + bmv2Action.name()
                                                              + " is not defined for table " + tableName);
        }
        if (actionModel.runtimeDatas().size() != bmv2Action.parameters().size()) {
            throw new Bmv2FlowRuleTranslatorException("Wrong number of parameters for action "
                                                              + actionModel.name() + ", expected "
                                                              + actionModel.runtimeDatas().size() + ", but found "
                                                              + bmv2Action.parameters().size());
        }
        for (int i = 0; i < actionModel.runtimeDatas().size(); i++) {
            Bmv2RuntimeDataModel data = actionModel.runtimeDatas().get(i);
            ImmutableByteSequence param = bmv2Action.parameters().get(i);
            if (param.size() != roundToBytes(data.bitWidth())) {
                throw new Bmv2FlowRuleTranslatorException("Wrong byte-width for parameter " + data.name()
                                                                  + " of action " + actionModel.name()
                                                                  + ", expected " + roundToBytes(data.bitWidth())
                                                                  + " bytes, but found " + param.size());
            }
        }

        Bmv2TableEntry.Builder tableEntryBuilder = Bmv2TableEntry.builder();

        // In BMv2 0 is the highest priority.
        int newPriority = Integer.MAX_VALUE - rule.priority();

        tableEntryBuilder
                .withTableName(table.name())
                .withPriority(newPriority)
                .withMatchKey(bmv2MatchKey)
                .withAction(bmv2Action);

        if (!rule.isPermanent()) {
            if (table.hasTimeouts()) {
                tableEntryBuilder.withTimeout((double) rule.timeout());
            } else {
                log.warn("Flow rule is temporary but table {} doesn't support timeouts, translating to permanent",
                         table.name());
            }

        }

        return tableEntryBuilder.build();
    }

    private Bmv2TernaryMatchParam buildTernaryParam(Bmv2FieldModel field, Criterion criterion, int bitWidth)
            throws Bmv2FlowRuleTranslatorException {

        // Value and mask will be filled according to criterion type
        ImmutableByteSequence value;
        ImmutableByteSequence mask = null;

        int byteWidth = roundToBytes(bitWidth);

        switch (criterion.type()) {
            case IN_PORT:
                long port = ((PortCriterion) criterion).port().toLong();
                value = ImmutableByteSequence.copyFrom(port);
                break;
            case ETH_DST:
                EthCriterion c = (EthCriterion) criterion;
                value = ImmutableByteSequence.copyFrom(c.mac().toBytes());
                if (c.mask() != null) {
                    mask = ImmutableByteSequence.copyFrom(c.mask().toBytes());
                }
                break;
            case ETH_SRC:
                EthCriterion c2 = (EthCriterion) criterion;
                value = ImmutableByteSequence.copyFrom(c2.mac().toBytes());
                if (c2.mask() != null) {
                    mask = ImmutableByteSequence.copyFrom(c2.mask().toBytes());
                }
                break;
            case ETH_TYPE:
                short ethType = ((EthTypeCriterion) criterion).ethType().toShort();
                value = ImmutableByteSequence.copyFrom(ethType);
                break;
            // TODO: implement building for other criterion types (easy with DefaultCriterion of ONOS-4034)
            default:
                throw new Bmv2FlowRuleTranslatorException("Feature not implemented, ternary builder for criterion" +
                                                                  "type: " + criterion.type().name());
        }

        // Fit byte sequence in field model bit-width.
        try {
            value = Bmv2TranslatorUtils.fitByteSequence(value, bitWidth);
        } catch (Bmv2TranslatorUtils.ByteSequenceFitException e) {
            throw new Bmv2FlowRuleTranslatorException(
                    "Fit exception for criterion " + criterion.type().name() + " value, " + e.getMessage());
        }

        if (mask == null) {
            // no mask, all ones
            mask = ImmutableByteSequence.ofOnes(byteWidth);
        } else {
            try {
                mask = Bmv2TranslatorUtils.fitByteSequence(mask, bitWidth);
            } catch (Bmv2TranslatorUtils.ByteSequenceFitException e) {
                throw new Bmv2FlowRuleTranslatorException(
                        "Fit exception for criterion " + criterion.type().name() + " mask, " + e.getMessage());
            }
        }

        return new Bmv2TernaryMatchParam(value, mask);
    }

    private Bmv2Action getActionFromExtension(Instructions.ExtensionInstructionWrapper inst)
            throws Bmv2FlowRuleTranslatorException {

        ExtensionTreatment extTreatment = inst.extensionInstruction();

        if (extTreatment.type() == ExtensionTreatmentTypes.BMV2_ACTION.type()) {
            if (extTreatment instanceof Bmv2ExtensionTreatment) {
                return ((Bmv2ExtensionTreatment) extTreatment).action();
            } else {
                throw new Bmv2FlowRuleTranslatorException("Unable to decode treatment extension: " + extTreatment);
            }
        } else {
            throw new Bmv2FlowRuleTranslatorException("Unsupported treatment extension type: " + extTreatment.type());
        }
    }

    private Bmv2MatchKey buildMatchKey(Bmv2Interpreter interpreter, TrafficSelector selector, Bmv2TableModel tableModel)
            throws Bmv2FlowRuleTranslatorException {

        // Find a bmv2 extension selector (if any) and get the parameter map.
        Optional<Bmv2ExtensionSelector> extSelector = selector.criteria().stream()
                .filter(c -> c.type().equals(EXTENSION))
                .map(c -> (ExtensionCriterion) c)
                .map(ExtensionCriterion::extensionSelector)
                .filter(c -> c.type().equals(BMV2_MATCH_PARAMS.type()))
                .map(c -> (Bmv2ExtensionSelector) c)
                .findFirst();
        Map<String, Bmv2MatchParam> extParamMap =
                (extSelector.isPresent()) ? extSelector.get().parameterMap() : Collections.emptyMap();

        Set<Criterion> translatedCriteria = Sets.newHashSet();
        Set<String> usedExtParams = Sets.newHashSet();

        Bmv2MatchKey.Builder matchKeyBuilder = Bmv2MatchKey.builder();

        keysLoop:
        for (Bmv2TableKeyModel keyModel : tableModel.keys()) {

            // use fieldName dot notation (e.g. ethernet.dstAddr)
            String fieldName = keyModel.field().header().name() + "." + keyModel.field().type().name();

            int bitWidth = keyModel.field().type().bitWidth();
            int byteWidth = roundToBytes(bitWidth);

            Criterion.Type criterionType = interpreter.criterionTypeMap().inverse().get(fieldName);

            if (!extParamMap.containsKey(fieldName) &&
                    (criterionType == null || selector.getCriterion(criterionType) == null)) {
                // Neither an extension nor a mapping / criterion is available for this field.
                switch (keyModel.matchType()) {
                    case TERNARY:
                        // Wildcard field
                        matchKeyBuilder.withWildcard(byteWidth);
                        break;
                    case LPM:
                        // LPM with prefix 0
                        matchKeyBuilder.add(new Bmv2LpmMatchParam(ImmutableByteSequence.ofZeros(byteWidth), 0));
                        break;
                    default:
                        throw new Bmv2FlowRuleTranslatorException("No value found for required match field "
                                                                          + fieldName);
                }
                // Next key
                continue keysLoop;
            }

            Bmv2MatchParam matchParam;

            if (extParamMap.containsKey(fieldName)) {
                // Parameter found in extension
                if (criterionType != null && selector.getCriterion(criterionType) != null) {
                    // Found also a criterion that can be mapped. This is bad.
                    throw new Bmv2FlowRuleTranslatorException("Both an extension and a criterion mapping are defined " +
                                                                      "for match field " + fieldName);
                }

                matchParam = extParamMap.get(fieldName);
                usedExtParams.add(fieldName);

                // Check parameter type and size
                if (!keyModel.matchType().equals(matchParam.type())) {
                    throw new Bmv2FlowRuleTranslatorException("Wrong match type for parameter " + fieldName
                                                                      + ", expected " + keyModel.matchType().name()
                                                                      + ", but found " + matchParam.type().name());
                }
                int foundByteWidth;
                switch (keyModel.matchType()) {
                    case EXACT:
                        Bmv2ExactMatchParam m1 = (Bmv2ExactMatchParam) matchParam;
                        foundByteWidth = m1.value().size();
                        break;
                    case TERNARY:
                        Bmv2TernaryMatchParam m2 = (Bmv2TernaryMatchParam) matchParam;
                        foundByteWidth = m2.value().size();
                        break;
                    case LPM:
                        Bmv2LpmMatchParam m3 = (Bmv2LpmMatchParam) matchParam;
                        foundByteWidth = m3.value().size();
                        break;
                    case VALID:
                        foundByteWidth = -1;
                        break;
                    default:
                        // should never be her
                        throw new RuntimeException("Unrecognized match type " + keyModel.matchType().name());
                }
                if (foundByteWidth != -1 && foundByteWidth != byteWidth) {
                    throw new Bmv2FlowRuleTranslatorException("Wrong byte-width for match parameter " + fieldName
                                                                      + ", expected " + byteWidth + ", but found "
                                                                      + foundByteWidth);
                }

            } else {
                // A criterion mapping is available for this key
                Criterion criterion = selector.getCriterion(criterionType);
                translatedCriteria.add(criterion);
                switch (keyModel.matchType()) {
                    case TERNARY:
                        matchParam = buildTernaryParam(keyModel.field(), criterion, bitWidth);
                        break;
                    default:
                        // TODO: implement other match param builders (exact, LPM, etc.)
                        throw new Bmv2FlowRuleTranslatorException("Feature not yet implemented, match param builder: "
                                                                          + keyModel.matchType().name());
                }
            }

            matchKeyBuilder.add(matchParam);
        }

        // Check if all criteria have been translated
        Set<Criterion> ignoredCriteria = selector.criteria()
                .stream()
                .filter(c -> !c.type().equals(EXTENSION))
                .filter(c -> !translatedCriteria.contains(c))
                .collect(Collectors.toSet());

        if (ignoredCriteria.size() > 0) {
            throw new Bmv2FlowRuleTranslatorException("The following criteria cannot be translated for table "
                                                              + tableModel.name() + ": " + ignoredCriteria.toString());
        }

        // Check is all extension parameters have been used
        Set<String> ignoredExtParams = extParamMap.keySet()
                .stream()
                .filter(k -> !usedExtParams.contains(k))
                .collect(Collectors.toSet());

        if (ignoredExtParams.size() > 0) {
            throw new Bmv2FlowRuleTranslatorException("The following extension match parameters cannot be used for " +
                                                              "table " + tableModel.name() + ": "
                                                              + ignoredExtParams.toString());
        }

        return matchKeyBuilder.build();
    }

}
