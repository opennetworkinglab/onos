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

package org.onosproject.p4runtime.api;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.runtime.PiActionProfileId;
import org.onosproject.net.pi.runtime.PiActionGroupId;

/**
 * Class containing the reference for a group in P4Runtime.
 */
@Beta
public final class P4RuntimeGroupReference {
    private final DeviceId deviceId;
    private final PiActionProfileId piActionProfileId;
    private final PiActionGroupId groupId;

    /**
     * Creates P4 runtime group reference.
     *
     * @param deviceId the device id of group
     * @param piActionProfileId the action profile id
     * @param groupId the group Id of group
     */
    public P4RuntimeGroupReference(DeviceId deviceId, PiActionProfileId piActionProfileId,
                                   PiActionGroupId groupId) {
        this.deviceId = deviceId;
        this.piActionProfileId = piActionProfileId;
        this.groupId = groupId;
    }

    /**
     * Gets device id of this group.
     *
     * @return the device id
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Gets action profile id of this group.
     *
     * @return the action profile id
     */
    public PiActionProfileId actionProfileId() {
        return piActionProfileId;
    }

    /**
     * Gets group id of this group.
     *
     * @return group id
     */
    public PiActionGroupId groupId() {
        return groupId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        P4RuntimeGroupReference that = (P4RuntimeGroupReference) o;
        return Objects.equal(deviceId, that.deviceId) &&
                Objects.equal(piActionProfileId, that.piActionProfileId) &&
                Objects.equal(groupId, that.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId, piActionProfileId, groupId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("deviceId", deviceId)
                .add("piActionProfileId", piActionProfileId)
                .add("groupId", groupId)
                .toString();
    }
}
