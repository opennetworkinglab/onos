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
 * Range field match in a protocol-independent pipeline.
 */
@Beta
public final class PiRangeFieldMatch extends PiFieldMatch {

    private final ImmutableByteSequence lowValue;
    private final ImmutableByteSequence highValue;

    /**
     * Creates a new range field match for the given low and high value.
     *
     * @param fieldId   field identifier
     * @param lowValue  low value
     * @param highValue high value
     */
    public PiRangeFieldMatch(PiHeaderFieldId fieldId, ImmutableByteSequence lowValue,
                             ImmutableByteSequence highValue) {
        super(fieldId);
        this.lowValue = checkNotNull(lowValue);
        this.highValue = checkNotNull(highValue);
        checkArgument(lowValue.size() == highValue.size() && lowValue.size() > 0,
                      "Low and high values must have the same non-zero size.");
    }

    @Override
    public PiMatchType type() {
        return PiMatchType.RANGE;
    }

    /**
     * Returns the low value of this range field match.
     *
     * @return low value
     */
    public ImmutableByteSequence lowValue() {
        return lowValue;
    }

    /**
     * Returns the high value of this range field match.
     *
     * @return high value
     */
    public ImmutableByteSequence highValue() {
        return highValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiRangeFieldMatch that = (PiRangeFieldMatch) o;
        return Objects.equal(this.fieldId(), that.fieldId()) &&
                Objects.equal(lowValue, that.lowValue) &&
                Objects.equal(highValue, that.highValue);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.fieldId(), lowValue, highValue);
    }

    @Override
    public String toString() {
        return this.fieldId().toString() + '=' + lowValue.toString() + "--" + highValue.toString();
    }
}
