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
import org.onosproject.net.pi.model.PiActionProfileId;

/**
 * Global identifier of a PI action profile group applied to a device, uniquely
 * defined by a device ID, action profile ID and group ID.
 */
@Beta
public final class PiActionProfileGroupHandle extends PiHandle {

    private final PiActionProfileId actionProfileId;
    private final PiActionProfileGroupId groupId;

    private PiActionProfileGroupHandle(DeviceId deviceId, PiActionProfileGroup group) {
        super(deviceId);
        actionProfileId = group.actionProfile();
        groupId = group.id();
    }

    /**
     * Creates a new handle for the given device ID and PI action profile
     * group.
     *
     * @param deviceId device ID
     * @param group    PI action profile group
     * @return PI action profile group handle
     */
    public static PiActionProfileGroupHandle of(DeviceId deviceId,
                                                PiActionProfileGroup group) {
        return new PiActionProfileGroupHandle(deviceId, group);
    }

    /**
     * Returns the action profile ID of this handle.
     *
     * @return action profile ID
     */
    public PiActionProfileId actionProfile() {
        return actionProfileId;
    }

    /**
     * Returns the group ID of this handle.
     *
     * @return group ID
     */
    public PiActionProfileGroupId groupId() {
        return groupId;
    }

    @Override
    public PiEntityType entityType() {
        return PiEntityType.ACTION_PROFILE_GROUP;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId(),
                                actionProfileId,
                                groupId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiActionProfileGroupHandle that = (PiActionProfileGroupHandle) o;
        return Objects.equal(deviceId(), that.deviceId()) &&
                Objects.equal(actionProfileId,
                              that.actionProfileId) &&
                Objects.equal(groupId, that.groupId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("deviceId", deviceId())
                .add("actionProfile", actionProfileId)
                .add("groupId", groupId)
                .toString();
    }
}
