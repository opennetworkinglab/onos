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
 * Exact field match in a protocol-independent pipeline.
 */
@Beta
public final class PiExactFieldMatch extends PiFieldMatch {

    private final ImmutableByteSequence value;

    /**
     * Creates an exact field match.
     *
     * @param fieldId field identifier
     * @param value   value
     */
    public PiExactFieldMatch(PiHeaderFieldId fieldId, ImmutableByteSequence value) {
        super(fieldId);
        this.value = checkNotNull(value);
        checkArgument(value.size() > 0, "Value can't have size 0");
    }

    @Override
    public PiMatchType type() {
        return PiMatchType.EXACT;
    }

    /**
     * Returns the byte sequence value to be matched.
     *
     * @return an immutable byte sequence
     */
    public ImmutableByteSequence value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiExactFieldMatch that = (PiExactFieldMatch) o;
        return Objects.equal(this.fieldId(), that.fieldId()) &&
                Objects.equal(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.fieldId(), value);
    }

    @Override
    public String toString() {
        return this.fieldId().toString() + "=" + value.toString();
    }
}
