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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A BMv2 longest prefix match (LPM) parameter.
 */
@Beta
public final class Bmv2LpmMatchParam implements Bmv2MatchParam {

    private final ImmutableByteSequence value;
    private final int prefixLength;

    /**
     * Creates a new LPM parameter using the given byte sequence value and
     * prefix length.
     *
     * @param value        a byte sequence value
     * @param prefixLength an integer value
     */
    public Bmv2LpmMatchParam(ImmutableByteSequence value, int prefixLength) {
        checkArgument(prefixLength >= 0, "prefix length cannot be negative");
        this.value = checkNotNull(value);
        this.prefixLength = prefixLength;
    }

    @Override
    public Bmv2MatchParam.Type type() {
        return Type.LPM;
    }

    /**
     * Returns the byte sequence value of this parameter.
     *
     * @return a byte sequence value
     */
    public ImmutableByteSequence value() {
        return this.value;
    }

    /**
     * Returns the prefix length of this parameter.
     *
     * @return an integer value
     */
    public int prefixLength() {
        return this.prefixLength;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value, prefixLength);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2LpmMatchParam other = (Bmv2LpmMatchParam) obj;
        return Objects.equal(this.value, other.value)
                && Objects.equal(this.prefixLength, other.prefixLength);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("value", value)
                .add("prefixLength", prefixLength)
                .toString();
    }
}
