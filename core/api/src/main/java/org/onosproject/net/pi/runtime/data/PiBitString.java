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
import com.google.common.base.Objects;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.pi.model.PiData;

/**
 * BitString entity in a protocol-independent pipeline.
 */
@Beta
public final class PiBitString implements PiData {
    private final ImmutableByteSequence bitString;

    /**
     * Creates a new protocol-independent bit string instance.
     *
     * @param bitString bitString
     */
    private PiBitString(ImmutableByteSequence bitString) {
        this.bitString = bitString;
    }

    /**
     * Returns a new protocol-independent BitString.
     * @param bitString bitString
     * @return BitString
     */
    public static PiBitString of(ImmutableByteSequence bitString) {
        return new PiBitString(bitString);
    }

    /**
     * Return protocol-independent bitString instance.
     *
     * @return bitString
     */
    public ImmutableByteSequence bitString() {
        return this.bitString;
    }

    @Override
    public Type type() {
        return Type.BITSTRING;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiBitString bitStr = (PiBitString) o;
        return Objects.equal(bitString, bitStr.bitString);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bitString);
    }

    @Override
    public String toString() {
        return bitString.toString();
    }
}
