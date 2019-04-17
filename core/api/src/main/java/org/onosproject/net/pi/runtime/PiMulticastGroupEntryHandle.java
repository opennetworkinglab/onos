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
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onosproject.net.DeviceId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Global identifier of a PI multicast group entry applied to the packet
 * replication engine of a device, uniquely defined by a device ID, and group
 * ID.
 */
@Beta
public final class PiMulticastGroupEntryHandle extends PiPreEntryHandle {

    private final int groupId;

    private PiMulticastGroupEntryHandle(DeviceId deviceId, int groupId) {
        super(deviceId);
        this.groupId = groupId;
    }

    /**
     * Creates a new handle for the given device ID and PI multicast group ID.
     *
     * @param deviceId device ID
     * @param groupId  multicast group ID
     * @return PI multicast group entry handle
     */
    public static PiMulticastGroupEntryHandle of(DeviceId deviceId,
                                                 int groupId) {
        return new PiMulticastGroupEntryHandle(deviceId, groupId);
    }

    /**
     * Creates a new handle for the given device ID and PI multicast group
     * entry.
     *
     * @param deviceId device ID
     * @param entry    PI multicast group entry
     * @return PI multicast group entry handle
     */
    public static PiMulticastGroupEntryHandle of(DeviceId deviceId,
                                                 PiMulticastGroupEntry entry) {
        checkNotNull(entry);
        return new PiMulticastGroupEntryHandle(deviceId, entry.groupId());
    }

    /**
     * Returns the multicast group ID associated with this handle.
     *
     * @return group ID
     */
    public int groupId() {
        return groupId;
    }

    @Override
    public PiPreEntryType preEntryType() {
        return PiPreEntryType.MULTICAST_GROUP;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId(), groupId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiMulticastGroupEntryHandle that = (PiMulticastGroupEntryHandle) o;
        return Objects.equal(deviceId(), that.deviceId()) &&
                Objects.equal(groupId, that.groupId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("deviceId", deviceId())
                .add("groupId", groupId)
                .toString();
    }
}
