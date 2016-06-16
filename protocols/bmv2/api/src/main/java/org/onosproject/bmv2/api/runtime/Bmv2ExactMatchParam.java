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

/**
 * A BMv2 exact match parameter.
 */
@Beta
public final class Bmv2ExactMatchParam implements Bmv2MatchParam {

    private final ImmutableByteSequence value;

    /**
     * Creates a new match parameter object that matches exactly on the
     * given byte sequence.
     *
     * @param value a byte sequence value
     */
    public Bmv2ExactMatchParam(ImmutableByteSequence value) {
        this.value = checkNotNull(value, "value cannot be null");
    }

    @Override
    public Type type() {
        return Type.EXACT;
    }

    /**
     * Return the byte sequence matched by this parameter.
     *
     * @return an immutable byte buffer value
     */
    public ImmutableByteSequence value() {
        return this.value;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2ExactMatchParam other = (Bmv2ExactMatchParam) obj;
        return Objects.equal(this.value, other.value);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("value", value)
                .toString();
    }
}
