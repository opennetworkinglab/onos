/*
 * Copyright 2017-present Open Networking Foundation
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
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiCounterType;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Identifier of a counter cell in a protocol-independent pipeline.
 */
@Beta
public final class PiCounterCellId {

    private final PiCounterId counterId;
    private final PiCounterType counterType;
    private final long index;
    private final PiTableEntry tableEntry;

    private PiCounterCellId(PiCounterId counterId, PiCounterType counterType,
                            long index, PiTableEntry tableEntry) {
        this.counterId = counterId;
        this.counterType = counterType;
        this.index = index;
        this.tableEntry = tableEntry;
    }

    /**
     * Returns the identifier of the counter instance where this cell is
     * contained. Meaningful only if the counter is of type {@link
     * PiCounterType#INDIRECT}.
     *
     * @return counter identifier
     */
    public PiCounterId counterId() {
        return counterId;
    }

    /**
     * Returns the type of the counter identified.
     *
     * @return counter type
     */
    public PiCounterType counterType() {
        return counterType;
    }

    /**
     * Returns the counter index to which this cell ID is associated. Meaningful
     * only if the counter is of type {@link PiCounterType#INDIRECT}.
     *
     * @return counter index
     */
    public long index() {
        return index;
    }

    /**
     * Returns the table entry to which this cell ID is associated. Meaningful
     * only if the counter is of type {@link PiCounterType#DIRECT}, otherwise
     * returns null.
     *
     * @return PI table entry or null
     */
    public PiTableEntry tableEntry() {
        return tableEntry;
    }

    /**
     * Return a direct counter cell ID for the given counter ID and table
     * entry.
     *
     * @param tableEntry table entry
     * @return counter cell ID
     */
    public static PiCounterCellId ofDirect(PiTableEntry tableEntry) {
        checkNotNull(tableEntry);
        return new PiCounterCellId(null, PiCounterType.DIRECT, -1, tableEntry);
    }

    /**
     * Return an indirect counter cell ID for the given counter ID and index.
     *
     * @param counterId counter ID
     * @param index     index
     * @return counter cell ID
     */
    public static PiCounterCellId ofIndirect(PiCounterId counterId, long index) {
        checkNotNull(counterId);
        checkArgument(index >= 0, "Index must be a positive number");
        return new PiCounterCellId(counterId, PiCounterType.INDIRECT, index, null);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PiCounterCellId other = (PiCounterCellId) obj;
        return Objects.equal(this.counterId, other.counterId)
                && Objects.equal(this.counterType, other.counterType)
                && Objects.equal(this.index, other.index)
                && Objects.equal(this.tableEntry, other.tableEntry);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(counterId, counterType, index, tableEntry);
    }

    @Override
    public String toString() {
        return counterType == PiCounterType.DIRECT
                ? tableEntry.toString()
                : counterId.toString() + ':' + String.valueOf(index);
    }
}
