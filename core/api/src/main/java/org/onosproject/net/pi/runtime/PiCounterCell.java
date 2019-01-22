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
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onosproject.net.DeviceId;

/**
 * Counter cell of a protocol-independent pipeline.
 */
@Beta
public final class PiCounterCell implements PiEntity {

    private final PiCounterCellId cellId;
    private final PiCounterCellData counterData;

    /**
     * Creates a new counter cell for the given cell identifier and counter cell
     * data.
     *
     * @param cellId            counter cell identifier
     * @param piCounterCellData counter cell data
     */
    public PiCounterCell(PiCounterCellId cellId, PiCounterCellData piCounterCellData) {
        this.cellId = cellId;
        this.counterData = piCounterCellData;
    }

    /**
     * Creates a new counter cell for the given cell identifier, number of
     * packets and bytes.
     *
     * @param cellId  counter cell identifier
     * @param packets number of packets
     * @param bytes   number of bytes
     */
    public PiCounterCell(PiCounterCellId cellId, long packets, long bytes) {
        this.cellId = cellId;
        this.counterData = new PiCounterCellData(packets, bytes);
    }

    /**
     * Returns the cell identifier.
     *
     * @return cell identifier
     */
    public PiCounterCellId cellId() {
        return cellId;
    }

    /**
     * Returns the data contained by this cell.
     *
     * @return counter cell data
     */
    public PiCounterCellData data() {
        return counterData;
    }

    @Override
    public PiEntityType piEntityType() {
        return PiEntityType.COUNTER_CELL;
    }

    @Override
    public PiCounterCellHandle handle(DeviceId deviceId) {
        return PiCounterCellHandle.of(deviceId, this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PiCounterCell)) {
            return false;
        }
        PiCounterCell that = (PiCounterCell) o;
        return Objects.equal(cellId, that.cellId) &&
                Objects.equal(counterData, that.counterData);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(cellId, counterData);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("cellId", cellId)
                .add("counterData", counterData)
                .toString();
    }
}
