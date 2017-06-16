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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.pi.impl.CriterionTranslators.EthCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.EthTypeCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.IpCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.PortCriterionTranslator;
import org.onosproject.net.pi.model.PiMatchType;
import org.onosproject.net.pi.runtime.PiExactFieldMatch;
import org.onosproject.net.pi.runtime.PiFieldMatch;
import org.onosproject.net.pi.runtime.PiHeaderFieldId;
import org.onosproject.net.pi.runtime.PiLpmFieldMatch;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;

import java.util.Map;

import static java.lang.String.format;
import static org.onlab.util.ImmutableByteSequence.ByteSequenceTrimException;
import static org.onosproject.net.pi.impl.CriterionTranslator.CriterionTranslatorException;
import static org.onosproject.net.pi.runtime.PiFlowRuleTranslationService.PiFlowRuleTranslationException;

/**
 * Helper class to translate criterion instances to PI field matches.
 */
final class CriterionTranslatorHelper {

    private static final Map<Class<? extends Criterion>, CriterionTranslator> TRANSLATORS = ImmutableMap.of(
            // Add here new CriterionTranslator implementations.
            PortCriterion.class, new PortCriterionTranslator(),
            EthCriterion.class, new EthCriterionTranslator(),
            EthTypeCriterion.class, new EthTypeCriterionTranslator(),
            IPCriterion.class, new IpCriterionTranslator()
    );

    private CriterionTranslatorHelper() {
        // Hides constructor.
    }

    /**
     * Translates a given criterion instance to a PiFieldMatch with the given id, match type, and bit-width.
     *
     * @param fieldId   PI header field identifier
     * @param criterion criterion
     * @param matchType match type
     * @param bitWidth  size of the field match in bits
     * @return a PI field match
     * @throws PiFlowRuleTranslationException if the criterion cannot be translated (see exception message)
     */
    static PiFieldMatch translateCriterion(Criterion criterion, PiHeaderFieldId fieldId, PiMatchType matchType,
                                           int bitWidth)
            throws PiFlowRuleTranslationException {

        if (!TRANSLATORS.containsKey(criterion.getClass())) {
            throw new PiFlowRuleTranslationException(format(
                    "Translation of criterion class %s is not implemented.",
                    criterion.getClass().getSimpleName()));
        }

        CriterionTranslator translator = TRANSLATORS.get(criterion.getClass());

        try {
            translator.init(criterion, bitWidth);
            switch (matchType) {
                case EXACT:
                    return new PiExactFieldMatch(fieldId, translator.exactMatch());
                case TERNARY:
                    Pair<ImmutableByteSequence, ImmutableByteSequence> tp = translator.ternaryMatch();
                    return new PiTernaryFieldMatch(fieldId, tp.getLeft(), tp.getRight());
                case LPM:
                    Pair<ImmutableByteSequence, Integer> lp = translator.lpmMatch();
                    return new PiLpmFieldMatch(fieldId, lp.getLeft(), lp.getRight());
                default:
                    throw new PiFlowRuleTranslationException(format(
                            "Translation of criterion %s (%s class) to match type %s is not implemented.",
                            criterion.type().name(), criterion.getClass().getSimpleName(), matchType.name()));
            }
        } catch (ByteSequenceTrimException e) {
            throw new PiFlowRuleTranslationException(format(
                    "Size mismatch for criterion %s: %s", criterion.type(), e.getMessage()));
        } catch (CriterionTranslatorException e) {
            throw new PiFlowRuleTranslationException(format(
                    "Unable to translate criterion %s: %s", criterion.type(), e.getMessage()));
        }
    }
}
