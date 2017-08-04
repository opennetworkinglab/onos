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
import org.onosproject.net.flow.criteria.Criterion;

import static org.onlab.util.ImmutableByteSequence.ByteSequenceTrimException;

/**
 * A translator of a criterion instance to different match types represented as primitive byte sequences.
 */
interface CriterionTranslator {

    /**
     * Initialize this translator with the given criterion and field match bit-width.
     *
     * @param criterion criterion
     * @param bitWidth  field bit-width
     * @throws ByteSequenceTrimException if the criterion cannot be trimmed to fit the field match bit-width
     */
    void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException;

    /**
     * Returns a pair of byte sequences representing a ternary match (value and mask) equivalent to the criterion given
     * when initialized. The returned byte sequences will have minimum size (i.e. rounded to the nearest byte) to
     * contain the initialized field bit-width.
     *
     * @return a pair of byte sequences with value on the left and mask on the right
     * @throws CriterionTranslatorException if the criterion cannot be translated
     */
    Pair<ImmutableByteSequence, ImmutableByteSequence> ternaryMatch() throws CriterionTranslatorException;

    /**
     * Returns a pair comprising a byte sequence and a prefix length representing a longest-prefix match equivalent to
     * the criterion given when initialized. The returned byte sequence will have minimum size (i.e. rounded to the
     * nearest byte) to contain the initialized field bit-width.
     *
     * @return a pair with the match value's byte sequence on the left, and prefix length on the right
     * @throws CriterionTranslatorException if the criterion cannot be translated
     */
    Pair<ImmutableByteSequence, Integer> lpmMatch() throws CriterionTranslatorException;

    /**
     * Returns a byte sequence representing an exact match equivalent to the criterion given when initialized. The
     * returned byte sequence will have minimum size (i.e. rounded to the nearest byte) to contain the initialized field
     * bit-width.
     *
     * @return exact match value as a byte sequence
     * @throws CriterionTranslatorException if the criterion cannot be translated
     */
    ImmutableByteSequence exactMatch() throws CriterionTranslatorException;

    /**
     * Signifies that the criterion cannot be translated.
     */
    class CriterionTranslatorException extends Exception {
        CriterionTranslatorException(String message) {
            super(message);
        }
    }
}
