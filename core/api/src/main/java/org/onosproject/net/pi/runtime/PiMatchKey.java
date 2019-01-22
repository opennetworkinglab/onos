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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import org.onosproject.net.pi.model.PiMatchFieldId;

import java.util.Collection;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Representation of all field matches of an entry of a match+action table of a protocol-independent pipeline.
 */
@Beta
public final class PiMatchKey {

    public static final PiMatchKey EMPTY = new PiMatchKey(ImmutableMap.of());

    private final ImmutableMap<PiMatchFieldId, PiFieldMatch> fieldMatches;

    private PiMatchKey(ImmutableMap<PiMatchFieldId, PiFieldMatch> fieldMatches) {
        this.fieldMatches = fieldMatches;
    }

    /**
     * Returns the collection of field matches of this match key.
     *
     * @return collection of field matches
     */
    public Collection<PiFieldMatch> fieldMatches() {
        return fieldMatches.values();
    }

    /**
     * If present, returns the field match associated with the given header field identifier.
     *
     * @param fieldId field identifier
     * @return optional field match
     */
    public Optional<PiFieldMatch> fieldMatch(PiMatchFieldId fieldId) {
        return Optional.ofNullable(fieldMatches.get(fieldId));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PiMatchKey)) {
            return false;
        }
        PiMatchKey that = (PiMatchKey) o;
        return Objects.equal(fieldMatches, that.fieldMatches);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fieldMatches);
    }

    @Override
    public String toString() {
        StringJoiner stringFieldMatches = new StringJoiner(", ", "{", "}");
        this.fieldMatches.values().forEach(f -> stringFieldMatches.add(f.toString()));
        return stringFieldMatches.toString();
    }

    /**
     * Returns a new builder of match keys.
     *
     * @return match key builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of match keys.
     */
    public static final class Builder {

        private final ImmutableMap.Builder<PiMatchFieldId, PiFieldMatch> fieldMatchesBuilder = ImmutableMap.builder();

        private Builder() {
            // hides constructor.
        }

        /**
         * Adds one field match to this match key.
         *
         * @param fieldMatch field match
         * @return this
         */
        public Builder addFieldMatch(PiFieldMatch fieldMatch) {
            this.fieldMatchesBuilder.put(fieldMatch.fieldId(), fieldMatch);
            return this;
        }

        /**
         * Adds many field matches to this match key.
         *
         * @param fieldMatches collection of field matches
         * @return this
         */
        public Builder addFieldMatches(Collection<PiFieldMatch> fieldMatches) {
            fieldMatches.forEach(this::addFieldMatch);
            return this;
        }

        /**
         * Creates a new match key.
         *
         * @return match key
         */
        public PiMatchKey build() {
            ImmutableMap<PiMatchFieldId, PiFieldMatch> fieldMatches = fieldMatchesBuilder.build();
            return new PiMatchKey(fieldMatches);
        }
    }
}
