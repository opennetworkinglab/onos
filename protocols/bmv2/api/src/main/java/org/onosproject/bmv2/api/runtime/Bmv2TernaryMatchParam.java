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
import com.google.common.base.Objects;
import org.onlab.util.ImmutableByteSequence;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Representation of a BMv2 ternary match parameter.
 */
@Beta
public final class Bmv2TernaryMatchParam implements Bmv2MatchParam {

    private final ImmutableByteSequence value;
    private final ImmutableByteSequence mask;

    /**
     * Creates a new ternary match parameter using the given byte sequences of
     * value and mask.
     *
     * @param value a byte sequence value
     * @param mask  a byte sequence value
     */
    public Bmv2TernaryMatchParam(ImmutableByteSequence value,
                                 ImmutableByteSequence mask) {
        this.value = checkNotNull(value, "value cannot be null");
        this.mask = checkNotNull(mask, "value cannot be null");
        checkState(value.size() == mask.size(),
                   "value and mask must have equal size");
    }

    @Override
    public Type type() {
        return Type.TERNARY;
    }

    /**
     * Returns the byte sequence value of by this parameter.
     *
     * @return a byte sequence value
     */
    public ImmutableByteSequence value() {
        return this.value;
    }

    /**
     * Returns the byte sequence mask of by this parameter.
     *
     * @return a byte sequence value
     */
    public ImmutableByteSequence mask() {
        return this.mask;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value, mask);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2TernaryMatchParam other = (Bmv2TernaryMatchParam) obj;
        return Objects.equal(this.value, other.value)
                && Objects.equal(this.mask, other.mask);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("value", value)
                .add("mask", mask)
                .toString();
    }
}