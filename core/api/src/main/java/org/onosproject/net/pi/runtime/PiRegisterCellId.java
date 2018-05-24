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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import org.onosproject.net.pi.model.PiRegisterId;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Identifier of a register cell in a protocol-independent pipeline.
 */
@Beta
public final class PiRegisterCellId {

    private final PiRegisterId registerId;
    private final long index;

    private PiRegisterCellId(PiRegisterId registerId, long index) {
        this.registerId = registerId;
        this.index = index;
    }

    /**
     * Returns the identifier of the register instance where this cell is
     * contained.
     *
     * @return register identifier
     */
    public PiRegisterId registerId() {
        return registerId;
    }

    /**
     * Returns the register index to which this cell ID is associated.
     *
     * @return register index
     */
    public long index() {
        return index;
    }

    /**
     * Return a register cell ID for the given register ID and index.
     *
     * @param registerId register ID
     * @param index     index
     * @return register cell ID
     */
    public static PiRegisterCellId of(PiRegisterId registerId, long index) {
        checkNotNull(registerId);
        checkArgument(index >= 0, "Index must be a positive number");
        return new PiRegisterCellId(registerId, index);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PiRegisterCellId other = (PiRegisterCellId) obj;
        return Objects.equal(this.registerId, other.registerId)
                && Objects.equal(this.index, other.index);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(registerId, index);
    }

    @Override
    public String toString() {
        return registerId.toString() + ':' + String.valueOf(index);
    }
}