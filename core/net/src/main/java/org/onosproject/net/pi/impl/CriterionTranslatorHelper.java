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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.flow.criteria.ArpHaCriterion;
import org.onosproject.net.flow.criteria.ArpOpCriterion;
import org.onosproject.net.flow.criteria.ArpPaCriterion;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPDscpCriterion;
import org.onosproject.net.flow.criteria.IPEcnCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.IPv6ExthdrFlagsCriterion;
import org.onosproject.net.flow.criteria.IPv6FlowLabelCriterion;
import org.onosproject.net.flow.criteria.IPv6NDLinkLayerAddressCriterion;
import org.onosproject.net.flow.criteria.IPv6NDTargetAddressCriterion;
import org.onosproject.net.flow.criteria.IcmpCodeCriterion;
import org.onosproject.net.flow.criteria.IcmpTypeCriterion;
import org.onosproject.net.flow.criteria.Icmpv6CodeCriterion;
import org.onosproject.net.flow.criteria.Icmpv6TypeCriterion;
import org.onosproject.net.flow.criteria.MetadataCriterion;
import org.onosproject.net.flow.criteria.MplsBosCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.MplsTcCriterion;
import org.onosproject.net.flow.criteria.PbbIsidCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.SctpPortCriterion;
import org.onosproject.net.flow.criteria.TcpFlagsCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.TunnelIdCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.criteria.VlanPcpCriterion;
import org.onosproject.net.pi.impl.CriterionTranslators.ArpHaCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.ArpOpCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.ArpPaCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.EthCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.EthTypeCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.IPDscpCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.IPEcnCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.IPProtocolCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.IPv6ExthdrFlagsCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.IPv6FlowLabelCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.IPv6NDLinkLayerAddressCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.IPv6NDTargetAddressCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.IcmpCodeCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.IcmpTypeCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.Icmpv6CodeCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.Icmpv6TypeCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.IpCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.MetadataCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.MplsBosCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.MplsCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.MplsTcCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.PbbIsidCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.PortCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.SctpPortCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.TcpFlagsCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.TcpPortCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.TunnelIdCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.UdpPortCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.VlanIdCriterionTranslator;
import org.onosproject.net.pi.impl.CriterionTranslators.VlanPcpCriterionTranslator;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiMatchType;
import org.onosproject.net.pi.runtime.PiExactFieldMatch;
import org.onosproject.net.pi.runtime.PiFieldMatch;
import org.onosproject.net.pi.runtime.PiLpmFieldMatch;
import org.onosproject.net.pi.runtime.PiOptionalFieldMatch;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;
import org.onosproject.net.pi.service.PiTranslationException;

import java.util.Map;

import static java.lang.String.format;
import static org.onlab.util.ImmutableByteSequence.ByteSequenceTrimException;
import static org.onosproject.net.pi.impl.CriterionTranslator.CriterionTranslatorException;

/**
 * Helper class to translate criterion instances to PI field matches.
 */
final class CriterionTranslatorHelper {
    private static final Map<Class<? extends Criterion>, Class<? extends CriterionTranslator>> TRANSLATORS =
            // Add here new CriterionTranslator implementations.
            new ImmutableMap.Builder<Class<? extends Criterion>, Class<? extends CriterionTranslator>>()
                    .put(PortCriterion.class, PortCriterionTranslator.class)
                    .put(EthCriterion.class, EthCriterionTranslator.class)
                    .put(EthTypeCriterion.class, EthTypeCriterionTranslator.class)
                    .put(IPCriterion.class, IpCriterionTranslator.class)
                    .put(VlanIdCriterion.class, VlanIdCriterionTranslator.class)
                    .put(UdpPortCriterion.class, UdpPortCriterionTranslator.class)
                    .put(IPDscpCriterion.class, IPDscpCriterionTranslator.class)
                    .put(IPProtocolCriterion.class, IPProtocolCriterionTranslator.class)
                    .put(IPv6ExthdrFlagsCriterion.class, IPv6ExthdrFlagsCriterionTranslator.class)
                    .put(IPv6FlowLabelCriterion.class, IPv6FlowLabelCriterionTranslator.class)
                    .put(IPv6NDLinkLayerAddressCriterion.class, IPv6NDLinkLayerAddressCriterionTranslator.class)
                    .put(IPv6NDTargetAddressCriterion.class, IPv6NDTargetAddressCriterionTranslator.class)
                    .put(IcmpCodeCriterion.class, IcmpCodeCriterionTranslator.class)
                    .put(IcmpTypeCriterion.class, IcmpTypeCriterionTranslator.class)
                    .put(Icmpv6CodeCriterion.class, Icmpv6CodeCriterionTranslator.class)
                    .put(Icmpv6TypeCriterion.class, Icmpv6TypeCriterionTranslator.class)
                    .put(MplsBosCriterion.class, MplsBosCriterionTranslator.class)
                    .put(MplsCriterion.class, MplsCriterionTranslator.class)
                    .put(MplsTcCriterion.class, MplsTcCriterionTranslator.class)
                    .put(PbbIsidCriterion.class, PbbIsidCriterionTranslator.class)
                    .put(SctpPortCriterion.class, SctpPortCriterionTranslator.class)
                    .put(TcpFlagsCriterion.class, TcpFlagsCriterionTranslator.class)
                    .put(TcpPortCriterion.class, TcpPortCriterionTranslator.class)
                    .put(TunnelIdCriterion.class, TunnelIdCriterionTranslator.class)
                    .put(VlanPcpCriterion.class, VlanPcpCriterionTranslator.class)
                    .put(ArpHaCriterion.class, ArpHaCriterionTranslator.class)
                    .put(ArpOpCriterion.class, ArpOpCriterionTranslator.class)
                    .put(ArpPaCriterion.class, ArpPaCriterionTranslator.class)
                    .put(IPEcnCriterion.class, IPEcnCriterionTranslator.class)
                    .put(MetadataCriterion.class, MetadataCriterionTranslator.class)
                    .build();

    private CriterionTranslatorHelper() {
        // Hides constructor.
    }

    /**
     * Translates a given criterion instance to a PiFieldMatch with the given id, match type, and bit-width.
     *
     * @param fieldId   PI match field identifier
     * @param criterion criterion
     * @param matchType match type
     * @param bitWidth  size of the field match in bits
     * @return a PI field match
     * @throws PiTranslationException if the criterion cannot be translated (see exception message)
     */
    static PiFieldMatch translateCriterion(Criterion criterion, PiMatchFieldId fieldId, PiMatchType matchType,
                                           int bitWidth)
            throws PiTranslationException {

        if (!TRANSLATORS.containsKey(criterion.getClass())) {
            throw new PiTranslationException(format(
                    "Translation of criterion class %s is not implemented.",
                    criterion.getClass().getSimpleName()));
        }

        try {
            final CriterionTranslator translator = TRANSLATORS.get(criterion.getClass()).newInstance();
            translator.init(criterion, bitWidth);
            switch (matchType) {
                case EXACT:
                    return new PiExactFieldMatch(fieldId, translator.exactMatch());
                case OPTIONAL:
                    return new PiOptionalFieldMatch(fieldId, translator.exactMatch());
                case TERNARY:
                    final Pair<ImmutableByteSequence, ImmutableByteSequence> tp = translator.ternaryMatch();
                    return new PiTernaryFieldMatch(fieldId, tp.getLeft(), tp.getRight());
                case LPM:
                    final Pair<ImmutableByteSequence, Integer> lp = translator.lpmMatch();
                    return new PiLpmFieldMatch(fieldId, lp.getLeft(), lp.getRight());
                default:
                    throw new PiTranslationException(format(
                            "Translation of criterion %s (%s class) to match type %s is not implemented.",
                            criterion.type().name(), criterion.getClass().getSimpleName(), matchType.name()));
            }
        } catch (ByteSequenceTrimException e) {
            throw new PiTranslationException(format(
                    "Size mismatch for criterion %s: %s", criterion.type(), e.getMessage()));
        } catch (CriterionTranslatorException e) {
            throw new PiTranslationException(format(
                    "Unable to translate criterion %s: %s", criterion.type(), e.getMessage()));
        } catch (InstantiationException | IllegalAccessException e) {
            // Was not able to instantiate the criterion translator.
            throw new IllegalStateException(e);
        }
    }
}
