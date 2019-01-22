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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiActionProfileId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Global identifier of a PI action profile member, uniquely defined by a
 * device ID, action profile ID, and member ID.
 */
public final class PiActionProfileMemberHandle extends PiHandle {

    private final PiActionProfileId actionProfileId;
    private final PiActionProfileMemberId memberId;

    private PiActionProfileMemberHandle(DeviceId deviceId,
                                        PiActionProfileId actionProfileId,
                                        PiActionProfileMemberId memberId) {
        super(deviceId);
        this.actionProfileId = actionProfileId;
        this.memberId = memberId;
    }

    /**
     * Creates a new handle for the given device ID, action profile ID, and
     * member ID.
     *
     * @param deviceId        device ID
     * @param actionProfileId action profile ID
     * @param memberId        member ID
     * @return action profile group member handle
     */
    public static PiActionProfileMemberHandle of(
            DeviceId deviceId,
            PiActionProfileId actionProfileId,
            PiActionProfileMemberId memberId) {
        return new PiActionProfileMemberHandle(
                deviceId, actionProfileId, memberId);
    }

    /**
     * Creates a new handle for the given device ID, and action profile group
     * member instance.
     *
     * @param deviceId device ID
     * @param member   member instance
     * @return action profile group member handle
     */
    public static PiActionProfileMemberHandle of(
            DeviceId deviceId,
            PiActionProfileMember member) {
        checkNotNull(member);
        return new PiActionProfileMemberHandle(
                deviceId, member.actionProfile(), member.id());
    }

    /**
     * Returns the member ID of this handle.
     *
     * @return member ID
     */
    public PiActionProfileMemberId memberId() {
        return memberId;
    }

    /**
     * Returns the action profile ID of this handle.
     *
     * @return action profile ID
     */
    public PiActionProfileId actionProfileId() {
        return actionProfileId;
    }

    @Override
    public PiEntityType entityType() {
        return PiEntityType.ACTION_PROFILE_MEMBER;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId(), actionProfileId, memberId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PiActionProfileMemberHandle other = (PiActionProfileMemberHandle) obj;
        return Objects.equal(this.deviceId(), other.deviceId())
                && Objects.equal(this.actionProfileId, other.actionProfileId)
                && Objects.equal(this.memberId, other.memberId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("deviceId", deviceId())
                .add("actionProfile", actionProfileId)
                .add("memberId", memberId)
                .toString();
    }
}
