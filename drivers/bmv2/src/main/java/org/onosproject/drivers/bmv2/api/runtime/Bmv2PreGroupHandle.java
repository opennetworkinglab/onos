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
 *
 */

package org.onosproject.drivers.bmv2.api.runtime;


import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onosproject.net.DeviceId;

/**
 * Global identifier of a BMv2 PRE group applied to a device, uniquely defined
 * by a device ID and group ID.
 */
public final class Bmv2PreGroupHandle extends Bmv2Handle<Bmv2PreGroup> {

    private Bmv2PreGroupHandle(DeviceId deviceId, Bmv2PreGroup group) {
        super(deviceId, group);
    }

    /**
     * Creates a new handle for the given device ID and BMv2 PRE group.
     *
     * @param deviceId device ID
     * @param group    BMv2 PRE group
     * @return BMv2 PRE group handle
     */
    public static Bmv2PreGroupHandle of(DeviceId deviceId,
                                        Bmv2PreGroup group) {
        return new Bmv2PreGroupHandle(deviceId, group);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId(),
                                bmv2Entity().groupId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Bmv2PreGroupHandle that = (Bmv2PreGroupHandle) o;
        return Objects.equal(deviceId(), that.deviceId()) &&
                Objects.equal(bmv2Entity().groupId(), that.bmv2Entity().groupId());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("deviceId", deviceId())
                .add("groupId", bmv2Entity().groupId())
                .toString();
    }
}
