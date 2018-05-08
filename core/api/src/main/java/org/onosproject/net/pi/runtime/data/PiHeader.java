/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.net.pi.runtime.data;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.pi.model.PiData;

import java.util.List;
import java.util.StringJoiner;

/**
 * Instance of a PiHeader in a protocol-independent pipeline.
 */
@Beta
public final class PiHeader implements PiData {
    private final Boolean isValid;
    private final ImmutableList<ImmutableByteSequence> bitStrings;

    /**
     * Creates a new protocol-independent header instance.
     *
     * @param isValid whether the header is valid
     * @param bitStrings bitstrings
     */
    private PiHeader(Boolean isValid, List<ImmutableByteSequence> bitStrings) {
        this.isValid = isValid;
        this.bitStrings = ImmutableList.copyOf(bitStrings);
    }

    /**
     * Returns a new protocol-independent header.
     * @param isValid whether the header is valid
     * @param bitStrings bitstrings
     * @return header
     */
    public static PiHeader of(Boolean isValid, List<ImmutableByteSequence> bitStrings) {
        return new PiHeader(isValid, bitStrings);
    }

    /**
     * Return whether the header is valid.
     *
     * @return bool
     */
    public Boolean isValid() {
        return this.isValid;
    }

    /**
     * Return the header bit strings.
     *
     * @return the list of bit string
     */
    public List<ImmutableByteSequence> bitStrings() {
        return this.bitStrings;
    }

    @Override
    public Type type() {
        return Type.HEADER;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiHeader header = (PiHeader) o;
        return Objects.equal(isValid, header.isValid) &&
                Objects.equal(bitStrings, header.bitStrings);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(isValid, bitStrings);
    }

    @Override
    public String toString() {
        StringJoiner stringParams = new StringJoiner(", ", "(", ")");
        this.bitStrings().forEach(p -> stringParams.add(p.toString()));
        return MoreObjects.toStringHelper(getClass())
                .add("bitString", stringParams)
                .add("isValid", isValid)
                .toString();
    }
}