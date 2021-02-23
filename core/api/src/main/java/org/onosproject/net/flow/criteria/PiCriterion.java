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

package org.onosproject.net.flow.criteria;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.runtime.PiExactFieldMatch;
import org.onosproject.net.pi.runtime.PiFieldMatch;
import org.onosproject.net.pi.runtime.PiLpmFieldMatch;
import org.onosproject.net.pi.runtime.PiOptionalFieldMatch;
import org.onosproject.net.pi.runtime.PiRangeFieldMatch;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;

import java.util.Collection;
import java.util.Optional;
import java.util.StringJoiner;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onlab.util.ImmutableByteSequence.copyFrom;

/**
 * Protocol-independent criterion.
 */
@Beta
public final class PiCriterion implements Criterion {

    private final ImmutableMap<PiMatchFieldId, PiFieldMatch> fieldMatchMap;

    /**
     * Creates a new protocol-independent criterion for the given match fields.
     *
     * @param fieldMatchMap field match map
     */
    private PiCriterion(ImmutableMap<PiMatchFieldId, PiFieldMatch> fieldMatchMap) {
        this.fieldMatchMap = fieldMatchMap;
    }

    /**
     * Returns all protocol-independent field matches defined by this criterion.
     *
     * @return collection of match parameters
     */
    public Collection<PiFieldMatch> fieldMatches() {
        return fieldMatchMap.values();
    }

    /**
     * If present, returns the field match associated with the given header field identifier.
     *
     * @param fieldId field identifier
     * @return optional field match
     */
    public Optional<PiFieldMatch> fieldMatch(PiMatchFieldId fieldId) {
        return Optional.ofNullable(fieldMatchMap.get(fieldId));
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
        return Objects.equal(fieldMatchMap, that.fieldMatchMap);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fieldMatchMap);
    }

    @Override
    public String toString() {
        StringJoiner stringParams = new StringJoiner(", ");
        fieldMatchMap.forEach((key, value) -> stringParams.add(value.toString()));
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
     * Returns the PiCriterion builder initialized by the given PiCriterion.
     *
     * @param piCriterion the input PiCriterion
     * @return PiCriterion builder
     */
    public static Builder builder(PiCriterion piCriterion) {
        return new Builder(piCriterion);
    }

    /**
     * PiCriterion Builder.
     */
    @Beta
    public static final class Builder {

        // Use map to guarantee that there's only one field match per field id.
        private final ImmutableMap.Builder<PiMatchFieldId, PiFieldMatch> fieldMatchMapBuilder = ImmutableMap.builder();

        private Builder() {
            // ban constructor.
        }

        private Builder(PiCriterion piCriterion) {
            piCriterion.fieldMatchMap.forEach(((piMatchFieldId, piFieldMatch) -> add(piFieldMatch)));
        }

        /**
         * Adds a match field to the builder.
         *
         * @param field the field value
         * @return this
         */
        public Builder add(PiFieldMatch field) {
            fieldMatchMapBuilder.put(field.fieldId(), field);
            return this;
        }

        /**
         * Adds an exact field match for the given fieldId and value.
         *
         * @param fieldId protocol-independent header field Id
         * @param value   exact match value
         * @return this
         */
        public Builder matchExact(PiMatchFieldId fieldId, short value) {
            fieldMatchMapBuilder.put(fieldId, new PiExactFieldMatch(fieldId, copyFrom(value)));
            return this;
        }

        /**
         * Adds an exact field match for the given fieldId and value.
         *
         * @param fieldId protocol-independent header field Id
         * @param value   exact match value
         * @return this
         */
        public Builder matchExact(PiMatchFieldId fieldId, int value) {
            fieldMatchMapBuilder.put(fieldId, new PiExactFieldMatch(fieldId, copyFrom(value)));
            return this;
        }

        /**
         * Adds an exact field match for the given fieldId and value.
         *
         * @param fieldId protocol-independent header field Id
         * @param value   exact match value
         * @return this
         */
        public Builder matchExact(PiMatchFieldId fieldId, long value) {
            fieldMatchMapBuilder.put(fieldId, new PiExactFieldMatch(fieldId, copyFrom(value)));
            return this;
        }

        /**
         * Adds an exact field match for the given fieldId and value.
         *
         * @param fieldId protocol-independent header field Id
         * @param value   exact match value
         * @return this
         */
        public Builder matchExact(PiMatchFieldId fieldId, byte[] value) {
            fieldMatchMapBuilder.put(fieldId, new PiExactFieldMatch(fieldId, copyFrom(value)));
            return this;
        }

        /**
         * Adds an exact field match for the given fieldId and value.
         *
         * @param fieldId protocol-independent header field Id
         * @param value   exact match value
         * @return this
         */
        public Builder matchExact(PiMatchFieldId fieldId, String value) {
            fieldMatchMapBuilder.put(fieldId, new PiExactFieldMatch(fieldId, copyFrom(value)));
            return this;
        }

        /**
         * Adds a ternary field match for the given fieldId, value and mask.
         *
         * @param fieldId protocol-independent header field Id
         * @param value   ternary match value
         * @param mask    ternary match mask
         * @return this
         */
        public Builder matchTernary(PiMatchFieldId fieldId, short value, short mask) {
            fieldMatchMapBuilder.put(fieldId, new PiTernaryFieldMatch(fieldId, copyFrom(value), copyFrom(mask)));
            return this;
        }

        /**
         * Adds a ternary field match for the given fieldId, value and mask.
         *
         * @param fieldId protocol-independent header field Id
         * @param value   ternary match value
         * @param mask    ternary match mask
         * @return this
         */
        public Builder matchTernary(PiMatchFieldId fieldId, int value, int mask) {
            fieldMatchMapBuilder.put(fieldId, new PiTernaryFieldMatch(fieldId, copyFrom(value), copyFrom(mask)));
            return this;
        }

        /**
         * Adds a ternary field match for the given fieldId, value and mask.
         *
         * @param fieldId protocol-independent header field Id
         * @param value   ternary match value
         * @param mask    ternary match mask
         * @return this
         */
        public Builder matchTernary(PiMatchFieldId fieldId, long value, long mask) {
            fieldMatchMapBuilder.put(fieldId, new PiTernaryFieldMatch(fieldId, copyFrom(value), copyFrom(mask)));
            return this;
        }

        /**
         * Adds a ternary field match for the given fieldId, value and mask.
         *
         * @param fieldId protocol-independent header field Id
         * @param value   ternary match value
         * @param mask    ternary match mask
         * @return this
         */
        public Builder matchTernary(PiMatchFieldId fieldId, byte[] value, byte[] mask) {
            fieldMatchMapBuilder.put(fieldId, new PiTernaryFieldMatch(fieldId, copyFrom(value), copyFrom(mask)));
            return this;
        }

        /**
         * Adds a longest-prefix field match for the given fieldId, value and prefix length.
         *
         * @param fieldId      protocol-independent header field Id
         * @param value        lpm match value
         * @param prefixLength lpm match prefix length
         * @return this
         */
        public Builder matchLpm(PiMatchFieldId fieldId, short value, int prefixLength) {
            fieldMatchMapBuilder.put(fieldId, new PiLpmFieldMatch(fieldId, copyFrom(value), prefixLength));
            return this;
        }

        /**
         * Adds a longest-prefix field match for the given fieldId, value and prefix length.
         *
         * @param fieldId      protocol-independent header field Id
         * @param value        lpm match value
         * @param prefixLength lpm match prefix length
         * @return this
         */
        public Builder matchLpm(PiMatchFieldId fieldId, int value, int prefixLength) {
            fieldMatchMapBuilder.put(fieldId, new PiLpmFieldMatch(fieldId, copyFrom(value), prefixLength));
            return this;
        }

        /**
         * Adds a longest-prefix field match for the given fieldId, value and prefix length.
         *
         * @param fieldId      protocol-independent header field Id
         * @param value        lpm match value
         * @param prefixLength lpm match prefix length
         * @return this
         */
        public Builder matchLpm(PiMatchFieldId fieldId, long value, int prefixLength) {
            fieldMatchMapBuilder.put(fieldId, new PiLpmFieldMatch(fieldId, copyFrom(value), prefixLength));
            return this;
        }

        /**
         * Adds a longest-prefix field match for the given fieldId, value and prefix length.
         *
         * @param fieldId      protocol-independent header field Id
         * @param value        lpm match value
         * @param prefixLength lpm match prefix length
         * @return this
         */
        public Builder matchLpm(PiMatchFieldId fieldId, byte[] value, int prefixLength) {
            fieldMatchMapBuilder.put(fieldId, new PiLpmFieldMatch(fieldId, copyFrom(value), prefixLength));
            return this;
        }

        /**
         * Adds a range field match for the given fieldId, low and high.
         *
         * @param fieldId protocol-independent header field Id
         * @param low     range match low value
         * @param high    range match high value
         * @return this
         */
        public Builder matchRange(PiMatchFieldId fieldId, short low, short high) {
            fieldMatchMapBuilder.put(fieldId, new PiRangeFieldMatch(fieldId, copyFrom(low), copyFrom(high)));
            return this;
        }

        /**
         * Adds a range field match for the given fieldId, low and high.
         *
         * @param fieldId protocol-independent header field Id
         * @param low     range match low value
         * @param high    range match high value
         * @return this
         */
        public Builder matchRange(PiMatchFieldId fieldId, int low, int high) {
            fieldMatchMapBuilder.put(fieldId, new PiRangeFieldMatch(fieldId, copyFrom(low), copyFrom(high)));
            return this;
        }

        /**
         * Adds a range field match for the given fieldId, low and high.
         *
         * @param fieldId protocol-independent header field Id
         * @param low     range match low value
         * @param high    range match high value
         * @return this
         */
        public Builder matchRange(PiMatchFieldId fieldId, long low, long high) {
            fieldMatchMapBuilder.put(fieldId, new PiRangeFieldMatch(fieldId, copyFrom(low), copyFrom(high)));
            return this;
        }

        /**
         * Adds a range field match for the given fieldId, low and high.
         *
         * @param fieldId protocol-independent header field Id
         * @param low     range match low value
         * @param high    range match high value
         * @return this
         */
        public Builder matchRange(PiMatchFieldId fieldId, byte[] low, byte[] high) {
            fieldMatchMapBuilder.put(fieldId, new PiRangeFieldMatch(fieldId, copyFrom(low), copyFrom(high)));
            return this;
        }

        /**
         * Adds an optional field match for the given fieldId and value.
         *
         * @param fieldId protocol-independent header field Id
         * @param value   optional match value
         * @return this
         */
        public Builder matchOptional(PiMatchFieldId fieldId, short value) {
            fieldMatchMapBuilder.put(fieldId, new PiOptionalFieldMatch(fieldId, copyFrom(value)));
            return this;
        }

        /**
         * Adds an optional field match for the given fieldId and value.
         *
         * @param fieldId protocol-independent header field Id
         * @param value   optional match value
         * @return this
         */
        public Builder matchOptional(PiMatchFieldId fieldId, int value) {
            fieldMatchMapBuilder.put(fieldId, new PiOptionalFieldMatch(fieldId, copyFrom(value)));
            return this;
        }

        /**
         * Adds an optional field match for the given fieldId and value.
         *
         * @param fieldId protocol-independent header field Id
         * @param value   optional match value
         * @return this
         */
        public Builder matchOptional(PiMatchFieldId fieldId, long value) {
            fieldMatchMapBuilder.put(fieldId, new PiOptionalFieldMatch(fieldId, copyFrom(value)));
            return this;
        }

        /**
         * Adds an optional field match for the given fieldId and value.
         *
         * @param fieldId protocol-independent header field Id
         * @param value   optional match value
         * @return this
         */
        public Builder matchOptional(PiMatchFieldId fieldId, byte[] value) {
            fieldMatchMapBuilder.put(fieldId, new PiOptionalFieldMatch(fieldId, copyFrom(value)));
            return this;
        }

        /**
         * Adds an optional field match for the given fieldId and value.
         *
         * @param fieldId protocol-independent header field Id
         * @param value   optional match value
         * @return this
         */
        public Builder matchOptional(PiMatchFieldId fieldId, String value) {
            fieldMatchMapBuilder.put(fieldId, new PiOptionalFieldMatch(fieldId, copyFrom(value)));
            return this;
        }

        /**
         * Builds a PiCriterion.
         *
         * @return PiCriterion
         */
        public PiCriterion build() {
            ImmutableMap<PiMatchFieldId, PiFieldMatch> fieldMatchMap = fieldMatchMapBuilder.build();
            checkArgument(fieldMatchMap.size() > 0, "Cannot build PI criterion with 0 field matches");
            return new PiCriterion(fieldMatchMap);
        }
    }
}
