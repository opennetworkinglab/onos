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
 * Global identifier of a PI table entry applied on a device, uniquely defined
 * by a device ID, table ID and match key.
 */
@Beta
public final class PiTableEntryHandle extends PiHandle<PiTableEntry> {

    private PiTableEntryHandle(DeviceId deviceId, PiTableEntry entry) {
        super(deviceId, entry);
    }

    /**
     * Creates a new handle for the given PI table entry and device ID.
     *
     * @param deviceId device ID
     * @param entry    PI table entry
     * @return PI table entry handle
     */
    public static PiTableEntryHandle of(DeviceId deviceId, PiTableEntry entry) {
        return new PiTableEntryHandle(deviceId, entry);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId(),
                                piEntity().table(),
                                piEntity().matchKey());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PiTableEntryHandle other = (PiTableEntryHandle) obj;
        return Objects.equal(this.deviceId(), other.deviceId())
                && Objects.equal(this.piEntity().table(),
                                 other.piEntity().table())
                && Objects.equal(this.piEntity().matchKey(),
                                 other.piEntity().matchKey());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("deviceId", deviceId())
                .add("tableId", piEntity().table())
                .add("matchKey", piEntity().matchKey())
                .toString();
    }
}
