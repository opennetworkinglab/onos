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

package org.onosproject.drivers.bmv2.translators;

import com.google.common.annotations.Beta;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.model.Bmv2ModelField;
import org.onosproject.bmv2.api.model.Bmv2ModelTable;
import org.onosproject.bmv2.api.model.Bmv2ModelTableKey;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionSelector;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionTreatment;
import org.onosproject.bmv2.api.runtime.Bmv2LpmMatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2MatchKey;
import org.onosproject.bmv2.api.runtime.Bmv2TableEntry;
import org.onosproject.bmv2.api.runtime.Bmv2TernaryMatchParam;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.ExtensionCriterion;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType.ExtensionSelectorTypes;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.Instructions.ExtensionInstructionWrapper;

/**
 * Default Bmv2 flow rule translator implementation.
 * <p>
 * Flow rules are translated into {@link Bmv2TableEntry BMv2 table entries} according to the following logic:
 * <ul>
 * <li> table name: obtained from the Bmv2 model using the flow rule table ID;
 * <li> match key: if the flow rule selector defines only a criterion of type {@link Criterion.Type#EXTENSION EXTENSION}
 * , then the latter is expected to contain a {@link Bmv2ExtensionSelector Bmv2ExtensionSelector}, which should provide
 * a match key already formatted for the given table; otherwise a match key is built using the
 * {@link TranslatorConfig#fieldToCriterionTypeMap() mapping} defined by this translator configuration.
 * <li> action: if the flow rule treatment contains only one instruction of type
 * {@link Instruction.Type#EXTENSION EXTENSION}, then the latter is expected to contain a {@link Bmv2ExtensionTreatment}
 * , which should provide a {@link Bmv2Action} already formatted for the given table; otherwise, an action is
 * {@link TranslatorConfig#buildAction(TrafficTreatment) built} using this translator configuration.
 * <li> priority: the same as the flow rule.
 * <li> timeout: if the table supports timeout, use the same as the flow rule, otherwise none (i.e. permanent entry).
 * </ul>
 */
@Beta
public class Bmv2DefaultFlowRuleTranslator implements Bmv2FlowRuleTranslator {

    private final TranslatorConfig config;

    public Bmv2DefaultFlowRuleTranslator(TranslatorConfig config) {
        this.config = config;
    }

    private static Bmv2TernaryMatchParam buildTernaryParam(Bmv2ModelField field, Criterion criterion, int byteWidth)
            throws Bmv2FlowRuleTranslatorException {

        // Value and mask will be filled according to criterion type
        ImmutableByteSequence value;
        ImmutableByteSequence mask = null;

        switch (criterion.type()) {
            case IN_PORT:
                // FIXME: allow port numbers of variable bit length (based on model), truncating when necessary
                short port = (short) ((PortCriterion) criterion).port().toLong();
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

        if (mask == null) {
            // no mask, all ones
            mask = ImmutableByteSequence.ofOnes(byteWidth);
        }

        return new Bmv2TernaryMatchParam(value, mask);
    }

    private static Bmv2MatchKey getMatchKeyFromExtension(ExtensionCriterion criterion)
            throws Bmv2FlowRuleTranslatorException {

        ExtensionSelector extSelector = criterion.extensionSelector();

        if (extSelector.type() == ExtensionSelectorTypes.P4_BMV2_MATCH_KEY.type()) {
            if (extSelector instanceof Bmv2ExtensionSelector) {
                return ((Bmv2ExtensionSelector) extSelector).matchKey();
            } else {
                throw new Bmv2FlowRuleTranslatorException("Unable to decode extension selector " + extSelector);
            }
        } else {
            throw new Bmv2FlowRuleTranslatorException("Unsupported extension selector type " + extSelector.type());
        }
    }

    private static Bmv2Action getActionFromExtension(Instructions.ExtensionInstructionWrapper inst)
            throws Bmv2FlowRuleTranslatorException {

        ExtensionTreatment extTreatment = inst.extensionInstruction();

        if (extTreatment.type() == ExtensionTreatmentTypes.P4_BMV2_ACTION.type()) {
            if (extTreatment instanceof Bmv2ExtensionTreatment) {
                return ((Bmv2ExtensionTreatment) extTreatment).getAction();
            } else {
                throw new Bmv2FlowRuleTranslatorException("Unable to decode treatment extension: " + extTreatment);
            }
        } else {
            throw new Bmv2FlowRuleTranslatorException("Unsupported treatment extension type: " + extTreatment.type());
        }
    }

    private static Bmv2MatchKey buildMatchKey(TranslatorConfig config, TrafficSelector selector, Bmv2ModelTable table)
            throws Bmv2FlowRuleTranslatorException {

        Bmv2MatchKey.Builder matchKeyBuilder = Bmv2MatchKey.builder();

        for (Bmv2ModelTableKey key : table.keys()) {

            String fieldName = key.field().header().name() + "." + key.field().type().name();
            int byteWidth = (int) Math.ceil((double) key.field().type().bitWidth() / 8.0);
            Criterion.Type criterionType = config.fieldToCriterionTypeMap().get(fieldName);

            if (criterionType == null || selector.getCriterion(criterionType) == null) {
                // A mapping is not available or the selector doesn't have such a type
                switch (key.matchType()) {
                    case TERNARY:
                        // Wildcard field
                        matchKeyBuilder.withWildcard(byteWidth);
                        break;
                    case LPM:
                        // LPM with prefix 0
                        matchKeyBuilder.add(new Bmv2LpmMatchParam(ImmutableByteSequence.ofZeros(byteWidth), 0));
                        break;
                    default:
                        throw new Bmv2FlowRuleTranslatorException("Match field not supported: " + fieldName);
                }
                // Next key
                continue;
            }

            Criterion criterion = selector.getCriterion(criterionType);
            Bmv2TernaryMatchParam matchParam = null;

            switch (key.matchType()) {
                case TERNARY:
                    matchParam = buildTernaryParam(key.field(), criterion, byteWidth);
                    break;
                default:
                    // TODO: implement other match param builders (exact, LPM, etc.)
                    throw new Bmv2FlowRuleTranslatorException("Feature not implemented, match param builder: "
                                                                      + key.matchType().name());
            }

            matchKeyBuilder.add(matchParam);
        }

        return matchKeyBuilder.build();
    }

    @Override
    public Bmv2TableEntry translate(FlowRule rule)
            throws Bmv2FlowRuleTranslatorException {

        int tableId = rule.tableId();

        Bmv2ModelTable table = config.model().table(tableId);

        if (table == null) {
            throw new Bmv2FlowRuleTranslatorException("Unknown table ID: " + tableId);
        }

        /* Translate selector */

        TrafficSelector selector = rule.selector();
        Bmv2MatchKey bmv2MatchKey = null;

        // If selector has only 1 criterion of type extension, use that
        Criterion criterion = selector.getCriterion(Criterion.Type.EXTENSION);
        if (criterion != null) {
            if (selector.criteria().size() == 1) {
                bmv2MatchKey = getMatchKeyFromExtension((ExtensionCriterion) criterion);
            } else {
                throw new Bmv2FlowRuleTranslatorException("Unable to translate traffic selector, found multiple " +
                                                                  "criteria of which one is an extension: " +
                                                                  selector.toString());
            }
        }

        if (bmv2MatchKey == null) {
            // not an extension
            bmv2MatchKey = buildMatchKey(config, selector, table);
        }

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
                                                                      selector.toString());
                }
            }
        }

        if (bmv2Action == null) {
            // No extension, use config to build action
            bmv2Action = config.buildAction(treatment);
        }

        if (bmv2Action == null) {
            // Config returned null
            throw new Bmv2FlowRuleTranslatorException("Unable to translate treatment: " + treatment);
        }

        Bmv2TableEntry.Builder tableEntryBuilder = Bmv2TableEntry.builder();

        // In BMv2 0 is the highest priority, i.e. the opposite than ONOS.
        int newPriority = Integer.MAX_VALUE - rule.priority();

        tableEntryBuilder
                .withTableName(table.name())
                .withPriority(newPriority)
                .withMatchKey(bmv2MatchKey)
                .withAction(bmv2Action);

        if (!rule.isPermanent()) {
            if (table.hasTimeouts()) {
                tableEntryBuilder.withTimeout((double) rule.timeout());
            }
            //FIXME: add warn log or exception?
        }

        return tableEntryBuilder.build();
    }

    @Override
    public TranslatorConfig config() {
        return this.config;
    }
}
