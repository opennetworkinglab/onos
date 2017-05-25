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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.pi.model.PiMatchType;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Longest-prefix field match in a protocol-independent pipeline.
 */
@Beta
public final class PiLpmFieldMatch extends PiFieldMatch {

    private final ImmutableByteSequence value;
    private final int prefixLength;

    /**
     * Creates a new LPM field match.
     *
     * @param fieldId      field identifier
     * @param value        value
     * @param prefixLength prefix length
     */
    public PiLpmFieldMatch(PiHeaderFieldId fieldId, ImmutableByteSequence value, int prefixLength) {
        super(fieldId);
        this.value = checkNotNull(value);
        this.prefixLength = prefixLength;
        checkArgument(value.size() > 0, "Value must have non-zero size");
        checkArgument(prefixLength >= 0, "Prefix length must be a non-negative integer");
    }

    @Override
    public PiMatchType type() {
        return PiMatchType.LPM;
    }

    /**
     * Returns the value matched by this field.
     *
     * @return a byte sequence value
     */
    public ImmutableByteSequence value() {
        return value;
    }

    /**
     * Returns the prefix length to be matched.
     *
     * @return an integer value
     */
    public int prefixLength() {
        return prefixLength;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiLpmFieldMatch that = (PiLpmFieldMatch) o;
        return prefixLength == that.prefixLength &&
                Objects.equal(value, that.value) &&
                Objects.equal(this.fieldId(), that.fieldId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.fieldId(), value, prefixLength);
    }

    @Override
    public String toString() {
        return this.fieldId().toString() + '=' + value.toString()
                + '/' + String.valueOf(prefixLength);
    }
}
