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

import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;

import static org.onlab.util.ImmutableByteSequence.ByteSequenceTrimException;
import static org.onlab.util.ImmutableByteSequence.copyFrom;

/**
 * Factory class of criterion translator implementations.
 */
final class CriterionTranslators {

    /**
     * Translator of PortCriterion.
     */
    static final class PortCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            PortCriterion c = (PortCriterion) criterion;
            initAsExactMatch(copyFrom(c.port().toLong()), bitWidth);
        }
    }

    /**
     * Translator of EthTypeCriterion.
     */
    static final class EthTypeCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            EthTypeCriterion c = (EthTypeCriterion) criterion;
            initAsExactMatch(copyFrom(c.ethType().toShort()), bitWidth);
        }
    }

    /**
     * Translator of EthCriterion.
     */
    static final class EthCriterionTranslator extends AbstractCriterionTranslator {
        @Override
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            EthCriterion c = (EthCriterion) criterion;
            ImmutableByteSequence value = copyFrom(c.mac().toBytes());
            if (c.mask() == null) {
                initAsExactMatch(value, bitWidth);
            } else {
                ImmutableByteSequence mask = copyFrom(c.mask().toBytes());
                initAsTernaryMatch(value, mask, bitWidth);
            }
        }
    }

    /**
     * Translator of IpCriterion.
     */
    static final class IpCriterionTranslator extends AbstractCriterionTranslator {
        public void init(Criterion criterion, int bitWidth) throws ByteSequenceTrimException {
            IPCriterion c = (IPCriterion) criterion;
            initAsLpm(copyFrom(c.ip().address().toOctets()), c.ip().prefixLength(), bitWidth);
        }
    }
}
