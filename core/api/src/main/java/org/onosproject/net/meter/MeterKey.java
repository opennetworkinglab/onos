/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.meter;

import com.google.common.base.Objects;
import org.onosproject.net.DeviceId;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * A meter key represents a meter uniquely.
 */
public final class MeterKey {

    private final DeviceId deviceId;
    private final MeterCellId id;

    private MeterKey(DeviceId deviceId, MeterCellId id) {
        this.deviceId = deviceId;
        this.id = id;
    }

    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * @return a MeterId iff the id is a MeterId
     * otherwise, return null
     * @deprecated in onos-2.5 replaced by {@link #key(DeviceId,MeterCellId)}
     * extends MeterKey to support both MeterId and PiMeterCellId
     */
    @Deprecated
    public MeterId meterId() {
        if (id instanceof MeterId) {
            return (MeterId) id;
        } else {
            return null;
        }
    }

    public MeterCellId meterCellId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MeterKey meterKey = (MeterKey) o;
        return Objects.equal(deviceId, meterKey.deviceId) &&
                Objects.equal(id, meterKey.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId, id);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("deviceId", deviceId)
                .add("meterCellId", id).toString();
    }

    /**
     * @param deviceId a DeviceId
     * @param id a MeterId
     * @return a MeterKey contains DeviceId and MeterId
     * @deprecated in onos-2.5 replaced by {@link #key(DeviceId,MeterCellId)}
     * extends MeterKey to support both MeterId and PiMeterCellId
     */
    @Deprecated
    public static MeterKey key(DeviceId deviceId, MeterId id) {
        return new MeterKey(deviceId, id);
    }

    public static MeterKey key(DeviceId deviceId, MeterCellId id) {
        return new MeterKey(deviceId, id);
    }
}
