/*
 * Copyright 2019-present Open Networking Foundation
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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onosproject.net.DeviceId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Global identifier of a PI counter cell instantiated on a device, uniquely
 * defined by a device ID and cell ID.
 */
public final class PiCounterCellHandle extends PiHandle {

    private final PiCounterCellId cellId;

    private PiCounterCellHandle(DeviceId deviceId, PiCounterCellId cellId) {
        super(deviceId);
        this.cellId = checkNotNull(cellId);
    }

    /**
     * Creates a new handle for the given device ID and counter cell ID.
     *
     * @param deviceId device ID
     * @param cellId counter cell ID
     * @return new counter cell handle
     */
    public static PiCounterCellHandle of(DeviceId deviceId, PiCounterCellId cellId) {
        return new PiCounterCellHandle(deviceId, cellId);
    }

    /**
     * Creates a new handle for the given device ID and counter cell.
     *
     * @param deviceId device ID
     * @param cell counter cell
     * @return new counter cell handle
     */
    public static PiCounterCellHandle of(DeviceId deviceId, PiCounterCell cell) {
        return new PiCounterCellHandle(deviceId, cell.cellId());
    }

    /**
     * Returns the counter cell ID associated with this handle.
     *
     * @return counter cell ID
     */
    public PiCounterCellId cellId() {
        return cellId;
    }

    @Override
    public PiEntityType entityType() {
        return PiEntityType.COUNTER_CELL;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId(), cellId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PiCounterCellHandle other = (PiCounterCellHandle) obj;
        return Objects.equal(this.deviceId(), other.deviceId())
                && Objects.equal(this.cellId, other.cellId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("deviceId", deviceId())
                .add("cellId", cellId)
                .toString();
    }
}
