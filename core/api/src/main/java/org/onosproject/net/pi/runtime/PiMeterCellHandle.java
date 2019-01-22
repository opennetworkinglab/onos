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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Global identifier of a PI meter cell instantiated on a device, uniquely
 * defined by a device ID and meter cell ID.
 */
@Beta
public final class PiMeterCellHandle extends PiHandle {

    private final PiMeterCellId cellId;

    private PiMeterCellHandle(DeviceId deviceId, PiMeterCellId meterCellId) {
        super(deviceId);
        this.cellId = meterCellId;
    }

    /**
     * Creates a new handle for the given device ID and PI meter cell ID.
     *
     * @param deviceId    device ID
     * @param meterCellId meter cell ID
     * @return PI meter handle
     */
    public static PiMeterCellHandle of(DeviceId deviceId,
                                       PiMeterCellId meterCellId) {
        return new PiMeterCellHandle(deviceId, meterCellId);
    }

    /**
     * Creates a new handle for the given device ID and PI meter cell
     * configuration.
     *
     * @param deviceId        device ID
     * @param meterCellConfig meter config
     * @return PI meter handle
     */
    public static PiMeterCellHandle of(DeviceId deviceId,
                                       PiMeterCellConfig meterCellConfig) {
        checkNotNull(meterCellConfig);
        return new PiMeterCellHandle(deviceId, meterCellConfig.cellId());
    }

    /**
     * Returns the cell ID associated with this handle.
     *
     * @return cell ID
     */
    public PiMeterCellId cellId() {
        return cellId;
    }

    @Override
    public PiEntityType entityType() {
        return PiEntityType.METER_CELL_CONFIG;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId(), cellId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiMeterCellHandle that = (PiMeterCellHandle) o;
        return Objects.equal(deviceId(), that.deviceId()) &&
                Objects.equal(cellId, that.cellId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("deviceId", deviceId())
                .add("meterCellId", cellId)
                .toString();
    }
}
