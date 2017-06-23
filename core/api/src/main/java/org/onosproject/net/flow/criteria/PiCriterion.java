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

package org.onosproject.net.flow.criteria;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import org.onosproject.net.pi.runtime.PiFieldMatch;
import org.onosproject.net.pi.runtime.PiHeaderFieldId;
import org.onosproject.net.pi.runtime.PiExactFieldMatch;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;
import org.onosproject.net.pi.runtime.PiLpmFieldMatch;
import org.onosproject.net.pi.runtime.PiRangeFieldMatch;
import org.onosproject.net.pi.runtime.PiValidFieldMatch;

import java.util.Collection;
import java.util.Map;
import java.util.StringJoiner;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onlab.util.ImmutableByteSequence.copyFrom;

/**
 * Protocol-indepedent criterion.
 */
@Beta
public final class PiCriterion implements Criterion {

    private final Collection<PiFieldMatch> fieldMatches;

    /**
     * Creates a new protocol-independent criterion for the given match fields.
     *
     * @param fieldMatches fields to match
     */
    private PiCriterion(Collection<PiFieldMatch> fieldMatches) {
        this.fieldMatches = fieldMatches;
    }

    /**
     * Returns the match parameters map of this selector.
     *
     * @return a match parameter map
     */
    public Collection<PiFieldMatch> fieldMatches() {
        return fieldMatches;
    }

    @Override
    public Type type() {
        return Type.PROTOCOL_INDEPENDENT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiCriterion that = (PiCriterion) o;
        return Objects.equal(fieldMatches, that.fieldMatches);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fieldMatches);
    }

    @Override
    public String toString() {
        StringJoiner stringParams = new StringJoiner(", ", "{", "}");
        fieldMatches.forEach(f -> stringParams.add(f.toString()));
        return stringParams.toString();
    }

    /**
     * Returns the PiCriterion builder.
     *
     * @return PiCriterion builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * PiCriterion Builder.
     */
    @Beta
    public static final class Builder {

        private final Map<PiHeaderFieldId, PiFieldMatch> piFieldMatches = Maps.newHashMap();

        private Builder() {
            // ban constructor.
        }

        /**
         * Adds an exact field match for the given fieldId and value.
         *
         * @param fieldId protocol-independent header field Id
         * @param value  exact match value
         * @return this
         */
        public Builder matchExact(PiHeaderFieldId fieldId, short value) {
            piFieldMatches.put(fieldId, new PiExactFieldMatch(fieldId, copyFrom(value)));
            return this;
        }

        /**
         * Adds an exact field match for the given fieldId and value.
         *
         * @param fieldId protocol-independent header field Id
         * @param value  exact match value
         * @return this
         */
        public Builder matchExact(PiHeaderFieldId fieldId, int value) {
            piFieldMatches.put(fieldId, new PiExactFieldMatch(fieldId, copyFrom(value)));
            return this;
        }

        /**
         * Adds an exact field match for the given fieldId and value.
         *
         * @param fieldId protocol-independent header field Id
         * @param value  exact match value
         * @return this
         */
        public Builder matchExact(PiHeaderFieldId fieldId, long value) {
            piFieldMatches.put(fieldId, new PiExactFieldMatch(fieldId, copyFrom(value)));
            return this;
        }

        /**
         * Adds an exact field match for the given fieldId and value.
         *
         * @param fieldId protocol-independent header field Id
         * @param value  exact match value
         * @return this
         */
        public Builder matchExact(PiHeaderFieldId fieldId, byte[] value) {
            piFieldMatches.put(fieldId, new PiExactFieldMatch(fieldId, copyFrom(value)));
            return this;
        }

        /**
         * Adds a ternary field match for the given fieldId, value and mask.
         *
         * @param fieldId protocol-independent header field Id
         * @param value  ternary match value
         * @param mask   ternary match mask
         * @return this
         */
        public Builder matchTernary(PiHeaderFieldId fieldId, short value, short mask) {
            piFieldMatches.put(fieldId, new PiTernaryFieldMatch(fieldId, copyFrom(value), copyFrom(mask)));
            return this;
        }

        /**
         * Adds a ternary field match for the given fieldId, value and mask.
         *
         * @param fieldId protocol-independent header field Id
         * @param value  ternary match value
         * @param mask   ternary match mask
         * @return this
         */
        public Builder matchTernary(PiHeaderFieldId fieldId, int value, int mask) {
            piFieldMatches.put(fieldId, new PiTernaryFieldMatch(fieldId, copyFrom(value), copyFrom(mask)));
            return this;
        }

        /**
         * Adds a ternary field match for the given fieldId, value and mask.
         *
         * @param fieldId protocol-independent header field Id
         * @param value  ternary match value
         * @param mask   ternary match mask
         * @return this
         */
        public Builder matchTernary(PiHeaderFieldId fieldId, long value, long mask) {
            piFieldMatches.put(fieldId, new PiTernaryFieldMatch(fieldId, copyFrom(value), copyFrom(mask)));
            return this;
        }

        /**
         * Adds a ternary field match for the given fieldId, value and mask.
         *
         * @param fieldId protocol-independent header field Id
         * @param value  ternary match value
         * @param mask   ternary match mask
         * @return this
         */
        public Builder matchTernary(PiHeaderFieldId fieldId, byte[] value, byte[] mask) {
            piFieldMatches.put(fieldId, new PiTernaryFieldMatch(fieldId, copyFrom(value), copyFrom(mask)));
            return this;
        }

        /**
         * Adds a longest-prefix field match for the given fieldId, value and prefix length.
         *
         * @param fieldId  protocol-independent header field Id
         * @param value    lpm match value
         * @param prefixLength lpm match prefix length
         * @return this
         */
        public Builder matchLpm(PiHeaderFieldId fieldId, short value, int prefixLength) {
            piFieldMatches.put(fieldId, new PiLpmFieldMatch(fieldId, copyFrom(value), prefixLength));
            return this;
        }

        /**
         * Adds a longest-prefix field match for the given fieldId, value and prefix length.
         *
         * @param fieldId  protocol-independent header field Id
         * @param value    lpm match value
         * @param prefixLength lpm match prefix length
         * @return this
         */
        public Builder matchLpm(PiHeaderFieldId fieldId, int value, int prefixLength) {
            piFieldMatches.put(fieldId, new PiLpmFieldMatch(fieldId, copyFrom(value), prefixLength));
            return this;
        }

        /**
         * Adds a longest-prefix field match for the given fieldId, value and prefix length.
         *
         * @param fieldId  protocol-independent header field Id
         * @param value    lpm match value
         * @param prefixLength lpm match prefix length
         * @return this
         */
        public Builder matchLpm(PiHeaderFieldId fieldId, long value, int prefixLength) {
            piFieldMatches.put(fieldId, new PiLpmFieldMatch(fieldId, copyFrom(value), prefixLength));
            return this;
        }

        /**
         * Adds a longest-prefix field match for the given fieldId, value and prefix length.
         *
         * @param fieldId  protocol-independent header field Id
         * @param value    lpm match value
         * @param prefixLength lpm match prefix length
         * @return this
         */
        public Builder matchLpm(PiHeaderFieldId fieldId, byte[] value, int prefixLength) {
            piFieldMatches.put(fieldId, new PiLpmFieldMatch(fieldId, copyFrom(value), prefixLength));
            return this;
        }

        /**
         * Adds a valid field match for the given fieldId and flag.
         *
         * @param fieldId protocol-independent header field Id
         * @param flag    a boolean value
         * @return this
         */
        public Builder matchValid(PiHeaderFieldId fieldId, boolean flag) {
            piFieldMatches.put(fieldId, new PiValidFieldMatch(fieldId, flag));
            return this;
        }

        /**
         * Adds a range field match for the given fieldId, low and high.
         *
         * @param fieldId protocol-independent header field Id
         * @param low   range match low value
         * @param high  range match high value
         * @return this
         */
        public Builder matchRange(PiHeaderFieldId fieldId, short low, short high) {
            piFieldMatches.put(fieldId, new PiRangeFieldMatch(fieldId, copyFrom(low), copyFrom(high)));
            return this;
        }

        /**
         * Adds a range field match for the given fieldId, low and high.
         *
         * @param fieldId protocol-independent header field Id
         * @param low   range match low value
         * @param high  range match high value
         * @return this
         */
        public Builder matchRange(PiHeaderFieldId fieldId, int low, int high) {
            piFieldMatches.put(fieldId, new PiRangeFieldMatch(fieldId, copyFrom(low), copyFrom(high)));
            return this;
        }
        /**
         * Adds a range field match for the given fieldId, low and high.
         *
         * @param fieldId protocol-independent header field Id
         * @param low   range match low value
         * @param high  range match high value
         * @return this
         */
        public Builder matchRange(PiHeaderFieldId fieldId, long low, long high) {
            piFieldMatches.put(fieldId, new PiRangeFieldMatch(fieldId, copyFrom(low), copyFrom(high)));
            return this;
        }
        /**
         * Adds a range field match for the given fieldId, low and high.
         *
         * @param fieldId protocol-independent header field Id
         * @param low   range match low value
         * @param high  range match high value
         * @return this
         */
        public Builder matchRange(PiHeaderFieldId fieldId, byte[] low, byte[] high) {
            piFieldMatches.put(fieldId, new PiRangeFieldMatch(fieldId, copyFrom(low), copyFrom(high)));
            return this;
        }

        /**
         * Builds a PiCriterion.
         *
         * @return PiCriterion
         */
        public Criterion build() {
            checkArgument(piFieldMatches.size() > 0);
            return new PiCriterion(piFieldMatches.values());
        }
    }
}