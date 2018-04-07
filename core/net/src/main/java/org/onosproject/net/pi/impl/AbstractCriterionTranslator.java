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

import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.pi.model.PiMatchType;

import java.util.Optional;

import static org.onlab.util.ImmutableByteSequence.ByteSequenceTrimException;

/**
 * Abstract implementation of a criterion translator that opportunistically tries to generate different types of match
 * based on an initialization method. It throws an exception when a safe translation is not possible, e.g. when trying
 * to translate a masked criterion to an exact match.
 */
abstract class AbstractCriterionTranslator implements CriterionTranslator {

    private PiMatchType initType;
    private int bitWidth;
    private ImmutableByteSequence value;
    private ImmutableByteSequence mask;
    private Integer prefixLength;

    /**
     * Initializes the translator as an exact match.
     *
     * @param value     exact match value
     * @param bitWidth  field bit-width
     * @throws ByteSequenceTrimException if the match value cannot be trimmed to fit the field match bit-width
     */
    void initAsExactMatch(ImmutableByteSequence value, int bitWidth)
            throws ByteSequenceTrimException {
        this.initType = PiMatchType.EXACT;
        this.value = value.fit(bitWidth);
        this.bitWidth = bitWidth;
    }

    /**
     * Initializes the translator as a ternary match.
     *
     * @param value     match value
     * @param mask      match mask
     * @param bitWidth  field bit-width
     * @throws ByteSequenceTrimException if the match value or mask cannot be trimmed to fit the field match bit-width
     */
    void initAsTernaryMatch(ImmutableByteSequence value, ImmutableByteSequence mask, int bitWidth)
            throws ByteSequenceTrimException {
        this.initType = PiMatchType.TERNARY;
        this.value = value.fit(bitWidth);
        this.mask = mask.fit(bitWidth);
        this.bitWidth = bitWidth;
    }

    /**
     * Initializes the translator as a longest-prefix match.
     *
     * @param value        match value
     * @param prefixLength prefix length
     * @param bitWidth     field bit-width
     * @throws ByteSequenceTrimException if the match value cannot be trimmed to fit the field match bit-width
     */
    void initAsLpm(ImmutableByteSequence value, int prefixLength, int bitWidth)
            throws ByteSequenceTrimException {
        this.initType = PiMatchType.LPM;
        this.value = value.fit(bitWidth);
        this.prefixLength = prefixLength;
        this.bitWidth = bitWidth;
    }

    /**
     * Computes the prefix padding size (in bits) based on value and bit width.
     *
     * @return prefix padding in bits
     */
    private int prefixPadding() {
        return value.size() * Byte.SIZE - this.bitWidth;
    }

    @Override
    public ImmutableByteSequence exactMatch() throws CriterionTranslatorException {
        switch (initType) {
            case EXACT:
                break;
            case TERNARY:
                ImmutableByteSequence exactMask = ImmutableByteSequence.ofOnes(value.size());
                if (!mask.equals(exactMask)) {
                    throw new CriterionTranslator.CriterionTranslatorException(
                            "trying to use masked field as an exact match, but mask is not exact");
                }
                break;
            case LPM:
                if (prefixLength < bitWidth) {
                    throw new CriterionTranslator.CriterionTranslatorException(
                            "trying to use LPM field as an exact match, but prefix is not full");
                }
                break;
            default:
                throw new IllegalArgumentException("Unrecognized init type " + initType.name());
        }
        return value;
    }

    @Override
    public Pair<ImmutableByteSequence, ImmutableByteSequence> ternaryMatch() {
        switch (initType) {
            case EXACT:
                mask = ImmutableByteSequence.prefixZeros(value.size(), prefixPadding());
                break;
            case TERNARY:
                break;
            case LPM:
                mask = getMaskFromPrefixLength(prefixLength, value.size());
                break;
            default:
                throw new IllegalArgumentException("Unrecognized init type " + initType.name());
        }

        return Pair.of(value, mask);
    }

    @Override
    public Pair<ImmutableByteSequence, Integer> lpmMatch() throws CriterionTranslatorException {
        switch (initType) {
            case EXACT:
                prefixLength = bitWidth;
                break;
            case TERNARY:
                prefixLength = getPrefixLengthFromMask(mask).orElseThrow(
                        () -> new CriterionTranslatorException(
                                "trying to use masked field as a longest-prefix match, " +
                                        "but mask is not equivalent to a prefix length"));
                break;
            case LPM:
                break;
            default:
                throw new IllegalArgumentException("Unrecognized init type " + initType.name());
        }

        return Pair.of(value, prefixLength);
    }

    /**
     * Returns a bit mask equivalent to the given prefix length.
     *
     * @param prefixLength prefix length (in bits)
     * @param maskSize     mask size (in bytes)
     * @return a byte sequence
     */
    private ImmutableByteSequence getMaskFromPrefixLength(int prefixLength, int maskSize) {
        return ImmutableByteSequence.prefixOnes(maskSize, prefixLength);
    }

    /**
     * Checks that the given mask is equivalent to a longest-prefix match and returns the prefix length. If not
     * possible, the optional value will not be present.
     *
     * @param mask byte sequence
     * @return optional prefix length
     */
    private Optional<Integer> getPrefixLengthFromMask(ImmutableByteSequence mask) {
        Integer prefixLength = 0;

        byte[] byteArray = mask.asArray();
        int byteArrayIndex = 0;
        while (byteArrayIndex < byteArray.length && byteArray[byteArrayIndex] == (byte) 0xff) {
            prefixLength += Byte.SIZE;
            byteArrayIndex++;
        }

        byte byteVal = byteArray[byteArrayIndex];
        while (byteVal != 0) {
            if ((byteVal & Integer.MIN_VALUE) != Integer.MIN_VALUE) {
                return Optional.empty();
            }
            prefixLength++;
            byteVal <<= 1;
        }

        byteArrayIndex++;
        while (byteArrayIndex < byteArray.length) {
            if (byteArray[byteArrayIndex] != 0) {
                return Optional.empty();
            }
            byteArrayIndex++;
        }

        return Optional.of(prefixLength);
    }
}
