/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.p4runtime.ctl.codec;

import com.google.protobuf.ByteString;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiExactFieldMatch;
import org.onosproject.net.pi.runtime.PiFieldMatch;
import org.onosproject.net.pi.runtime.PiLpmFieldMatch;
import org.onosproject.net.pi.runtime.PiRangeFieldMatch;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;
import p4.config.v1.P4InfoOuterClass;
import p4.v1.P4RuntimeOuterClass;

import static java.lang.String.format;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.p4runtime.ctl.codec.Utils.assertPrefixLen;
import static org.onosproject.p4runtime.ctl.codec.Utils.assertSize;

/**
 * Codec for P4Runtime FieldMatch. Metadata is expected to be a Preamble for
 * P4Info.Table.
 */
public final class FieldMatchCodec
        extends AbstractCodec<PiFieldMatch, P4RuntimeOuterClass.FieldMatch,
        P4InfoOuterClass.Preamble> {

    private static final String VALUE_OF_PREFIX = "value of ";
    private static final String MASK_OF_PREFIX = "mask of ";
    private static final String HIGH_RANGE_VALUE_OF_PREFIX = "high range value of ";
    private static final String LOW_RANGE_VALUE_OF_PREFIX = "low range value of ";

    @Override
    public P4RuntimeOuterClass.FieldMatch encode(
            PiFieldMatch piFieldMatch, P4InfoOuterClass.Preamble tablePreamble,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws CodecException, P4InfoBrowser.NotFoundException {

        P4RuntimeOuterClass.FieldMatch.Builder messageBuilder = P4RuntimeOuterClass
                .FieldMatch.newBuilder();

        // FIXME: check how field names for stacked headers are constructed in P4Runtime.
        String fieldName = piFieldMatch.fieldId().id();
        P4InfoOuterClass.MatchField matchFieldInfo = browser.matchFields(
                tablePreamble.getId()).getByName(fieldName);
        String entityName = format("field match '%s' of table '%s'",
                                   matchFieldInfo.getName(), tablePreamble.getName());
        int fieldId = matchFieldInfo.getId();
        int fieldBitwidth = matchFieldInfo.getBitwidth();

        messageBuilder.setFieldId(fieldId);

        switch (piFieldMatch.type()) {
            case EXACT:
                PiExactFieldMatch fieldMatch = (PiExactFieldMatch) piFieldMatch;
                ByteString exactValue = ByteString.copyFrom(fieldMatch.value().asReadOnlyBuffer());
                assertSize(VALUE_OF_PREFIX + entityName, exactValue, fieldBitwidth);
                return messageBuilder.setExact(
                        P4RuntimeOuterClass.FieldMatch.Exact
                                .newBuilder()
                                .setValue(exactValue)
                                .build())
                        .build();
            case TERNARY:
                PiTernaryFieldMatch ternaryMatch = (PiTernaryFieldMatch) piFieldMatch;
                ByteString ternaryValue = ByteString.copyFrom(ternaryMatch.value().asReadOnlyBuffer());
                ByteString ternaryMask = ByteString.copyFrom(ternaryMatch.mask().asReadOnlyBuffer());
                assertSize(VALUE_OF_PREFIX + entityName, ternaryValue, fieldBitwidth);
                assertSize(MASK_OF_PREFIX + entityName, ternaryMask, fieldBitwidth);
                return messageBuilder.setTernary(
                        P4RuntimeOuterClass.FieldMatch.Ternary
                                .newBuilder()
                                .setValue(ternaryValue)
                                .setMask(ternaryMask)
                                .build())
                        .build();
            case LPM:
                PiLpmFieldMatch lpmMatch = (PiLpmFieldMatch) piFieldMatch;
                ByteString lpmValue = ByteString.copyFrom(lpmMatch.value().asReadOnlyBuffer());
                int lpmPrefixLen = lpmMatch.prefixLength();
                assertSize(VALUE_OF_PREFIX + entityName, lpmValue, fieldBitwidth);
                assertPrefixLen(entityName, lpmPrefixLen, fieldBitwidth);
                return messageBuilder.setLpm(
                        P4RuntimeOuterClass.FieldMatch.LPM.newBuilder()
                                .setValue(lpmValue)
                                .setPrefixLen(lpmPrefixLen)
                                .build())
                        .build();
            case RANGE:
                PiRangeFieldMatch rangeMatch = (PiRangeFieldMatch) piFieldMatch;
                ByteString rangeHighValue = ByteString.copyFrom(rangeMatch.highValue().asReadOnlyBuffer());
                ByteString rangeLowValue = ByteString.copyFrom(rangeMatch.lowValue().asReadOnlyBuffer());
                assertSize(HIGH_RANGE_VALUE_OF_PREFIX + entityName, rangeHighValue, fieldBitwidth);
                assertSize(LOW_RANGE_VALUE_OF_PREFIX + entityName, rangeLowValue, fieldBitwidth);
                return messageBuilder.setRange(
                        P4RuntimeOuterClass.FieldMatch.Range.newBuilder()
                                .setHigh(rangeHighValue)
                                .setLow(rangeLowValue)
                                .build())
                        .build();
            default:
                throw new CodecException(format(
                        "Building of match type %s not implemented", piFieldMatch.type()));
        }
    }

    @Override
    public PiFieldMatch decode(
            P4RuntimeOuterClass.FieldMatch message, P4InfoOuterClass.Preamble tablePreamble,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws CodecException, P4InfoBrowser.NotFoundException {

        String fieldMatchName = browser.matchFields(tablePreamble.getId())
                .getById(message.getFieldId()).getName();
        PiMatchFieldId headerFieldId = PiMatchFieldId.of(fieldMatchName);

        P4RuntimeOuterClass.FieldMatch.FieldMatchTypeCase typeCase = message.getFieldMatchTypeCase();

        switch (typeCase) {
            case EXACT:
                P4RuntimeOuterClass.FieldMatch.Exact exactFieldMatch = message.getExact();
                ImmutableByteSequence exactValue = copyFrom(exactFieldMatch.getValue().asReadOnlyByteBuffer());
                return new PiExactFieldMatch(headerFieldId, exactValue);
            case TERNARY:
                P4RuntimeOuterClass.FieldMatch.Ternary ternaryFieldMatch = message.getTernary();
                ImmutableByteSequence ternaryValue = copyFrom(ternaryFieldMatch.getValue().asReadOnlyByteBuffer());
                ImmutableByteSequence ternaryMask = copyFrom(ternaryFieldMatch.getMask().asReadOnlyByteBuffer());
                return new PiTernaryFieldMatch(headerFieldId, ternaryValue, ternaryMask);
            case LPM:
                P4RuntimeOuterClass.FieldMatch.LPM lpmFieldMatch = message.getLpm();
                ImmutableByteSequence lpmValue = copyFrom(lpmFieldMatch.getValue().asReadOnlyByteBuffer());
                int lpmPrefixLen = lpmFieldMatch.getPrefixLen();
                return new PiLpmFieldMatch(headerFieldId, lpmValue, lpmPrefixLen);
            case RANGE:
                P4RuntimeOuterClass.FieldMatch.Range rangeFieldMatch = message.getRange();
                ImmutableByteSequence rangeHighValue = copyFrom(rangeFieldMatch.getHigh().asReadOnlyByteBuffer());
                ImmutableByteSequence rangeLowValue = copyFrom(rangeFieldMatch.getLow().asReadOnlyByteBuffer());
                return new PiRangeFieldMatch(headerFieldId, rangeLowValue, rangeHighValue);
            default:
                throw new CodecException(format(
                        "Decoding of field match type '%s' not implemented", typeCase.name()));
        }
    }
}
