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
import org.onosproject.net.meter.MeterCellId;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.model.PiMeterType;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Identifier of a meter cell in a protocol-independent pipeline.
 */
@Beta
public final class PiMeterCellId implements MeterCellId {

    private final PiMeterId meterId;
    private final PiMeterType meterType;
    private final long index;
    private final PiTableEntry tableEntry;

    private PiMeterCellId(PiMeterId meterId, PiMeterType meterType, long index,
                            PiTableEntry tableEntry) {
        this.meterId = meterId;
        this.meterType = meterType;
        this.index = index;
        this.tableEntry = tableEntry;
    }

    /**
     * Returns the identifier of the meter instance where this cell is contained.
     * Meaningful only if the meter is of type {@link PiMeterType#DIRECT}, otherwise returns null.
     *
     * @return meter identifier
     */
    public PiMeterId meterId() {
        return meterId;
    }

    /**
     * Returns the type of the meter identified.
     *
     * @return meter type
     */
    public PiMeterType meterType() {
        return meterType;
    }

    /**
     * Returns the meter index to which this cell ID is associated.
     * Meaningful only if the meter is of type {@link PiMeterType#INDIRECT}.
     *
     * @return meter index
     */
    public long index() {
        return index;
    }

    /**
     * Returns the table entry to which this cell ID is associated.
     * Meaningful only if the meter is of type {@link PiMeterType#DIRECT}, otherwise returns null.
     *
     * @return PI table entry or null
     */
    public PiTableEntry tableEntry() {
        return tableEntry;
    }

    @Override
    public MeterCellType type() {
        return MeterCellType.PIPELINE_INDEPENDENT;
    }

    /**
     * Return a direct meter cell ID for the given meter ID and table entry.
     *
     * @param tableEntry table entry
     * @return meter cell ID
     */
    public static PiMeterCellId ofDirect(PiTableEntry tableEntry) {
        checkNotNull(tableEntry);
        return new PiMeterCellId(null, PiMeterType.DIRECT, -1, tableEntry);
    }

    /**
     * Return an indirect meter cell ID for the given meter ID and index.
     *
     * @param meterId meter ID
     * @param index     index
     * @return meter cell ID
     */
    public static PiMeterCellId ofIndirect(PiMeterId meterId, long index) {
        checkNotNull(meterId);
        checkArgument(index >= 0, "Index must be a positive number");
        return new PiMeterCellId(meterId, PiMeterType.INDIRECT, index, null);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PiMeterCellId other = (PiMeterCellId) obj;
        return Objects.equal(this.meterId, other.meterId)
                && Objects.equal(this.meterType, other.meterType)
                && Objects.equal(this.index, other.index)
                && Objects.equal(this.tableEntry, other.tableEntry);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(meterId, meterType, index, tableEntry);
    }

    @Override
    public String toString() {
        return meterType == PiMeterType.DIRECT
                ? tableEntry.toString()
                : meterId.toString() + ':' + String.valueOf(index);
    }
}
