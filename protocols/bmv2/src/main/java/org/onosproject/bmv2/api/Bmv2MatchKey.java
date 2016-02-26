/*
 * Copyright 2014-2016 Open Networking Laboratory
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

package org.onosproject.bmv2.api;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.p4.bmv2.thrift.BmMatchParam;
import org.p4.bmv2.thrift.BmMatchParamExact;
import org.p4.bmv2.thrift.BmMatchParamLPM;
import org.p4.bmv2.thrift.BmMatchParamTernary;
import org.p4.bmv2.thrift.BmMatchParamType;
import org.p4.bmv2.thrift.BmMatchParamValid;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Bmv2 match key representation.
 */
public final class Bmv2MatchKey {

    private final List<BmMatchParam> matchParams;

    /**
     * Creates a new match key.
     *
     * @param matchParams The ordered list of match parameters
     */
    private Bmv2MatchKey(List<BmMatchParam> matchParams) {
        this.matchParams = matchParams;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the match parameters defined for this match key (read-only).
     *
     * @return match parameters
     */
    public final List<BmMatchParam> bmMatchParams() {
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
     * Builder of a Bmv2 match key.
     */
    public static final class Builder {

        private List<BmMatchParam> matchParams;

        private Builder() {
            this.matchParams = Lists.newArrayList();
        }

        /**
         * Adds an exact match parameter.
         *
         * @param key a ByteBuffer value
         * @return this
         */
        public Builder withExact(ByteBuffer key) {
            this.matchParams.add(
                    new BmMatchParam(BmMatchParamType.EXACT)
                            .setExact(new BmMatchParamExact(key)));
            return this;
        }


        /**
         * Adds a longest prefix match parameter.
         *
         * @param key          a ByteBuffer value
         * @param prefixLength an integer value
         * @return this
         */
        public Builder withLpm(ByteBuffer key, int prefixLength) {
            this.matchParams.add(
                    new BmMatchParam(BmMatchParamType.LPM)
                            .setLpm(new BmMatchParamLPM(key, prefixLength)));
            return this;
        }

        /**
         * Adds a ternary match parameter.
         *
         * @param key  a ByteBuffer value
         * @param mask an ByteBuffer value
         * @return this
         */
        public Builder withTernary(ByteBuffer key, ByteBuffer mask) {
            this.matchParams.add(
                    new BmMatchParam(BmMatchParamType.TERNARY).
                            setTernary(new BmMatchParamTernary(key, mask)));
            return this;
        }

        /**
         * Adds a ternary match parameter where all bits are don't-care.
         *
         * @param byteLength an integer value representing the length in byte of the parameter
         * @return this
         */
        public Builder withWildcard(int byteLength) {
            byte[] zeros = new byte[byteLength];
            Arrays.fill(zeros, (byte) 0);
            return this.withTernary(ByteBuffer.wrap(zeros), ByteBuffer.wrap(zeros));
        }

        /**
         * Adds a valid match parameter.
         *
         * @param key a boolean value
         * @return this
         */
        public Builder withValid(boolean key) {
            this.matchParams.add(
                    new BmMatchParam(BmMatchParamType.VALID)
                            .setValid(new BmMatchParamValid(key)));
            return this;
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