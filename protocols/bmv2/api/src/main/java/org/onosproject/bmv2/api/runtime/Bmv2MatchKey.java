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

package org.onosproject.bmv2.api.runtime;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.onlab.util.ImmutableByteSequence;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A match key of a BMv2 match-action table entry.
 */
@Beta
public final class Bmv2MatchKey {

    private final List<Bmv2MatchParam> matchParams;

    private Bmv2MatchKey(List<Bmv2MatchParam> matchParams) {
        // ban constructor
        this.matchParams = matchParams;
    }

    /**
     * Returns a new match key builder.
     *
     * @return a match key builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the list of match parameters of this match key.
     *
     * @return list match parameters
     */
    public final List<Bmv2MatchParam> matchParams() {
        return Collections.unmodifiableList(matchParams);
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(matchParams);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2MatchKey other = (Bmv2MatchKey) obj;

        return Objects.equals(this.matchParams, other.matchParams);
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(matchParams)
                .toString();
    }

    /**
     * Builder of a BMv2 match key.
     */
    public static final class Builder {

        private List<Bmv2MatchParam> matchParams;

        private Builder() {
            this.matchParams = Lists.newArrayList();
        }

        /**
         * Adds a match parameter to the match key.
         *
         * @param param a match parameter
         * @return this
         */
        public Builder add(Bmv2MatchParam param) {
            this.matchParams.add(checkNotNull(param));
            return this;
        }

        /**
         * Adds a ternary match parameter where all bits are don't-care.
         *
         * @param byteLength length in bytes of the parameter
         * @return this
         */
        public Builder withWildcard(int byteLength) {
            checkArgument(byteLength > 0, "length must be a positive integer");
            return add(new Bmv2TernaryMatchParam(
                    ImmutableByteSequence.ofZeros(byteLength),
                    ImmutableByteSequence.ofZeros(byteLength)));
        }

        /**
         * Builds a new match key object.
         *
         * @return match key
         */
        public Bmv2MatchKey build() {
            return new Bmv2MatchKey(this.matchParams);
        }
    }
}