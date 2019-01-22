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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Instance of a member of an action profile in a protocol-independent pipeline.
 */
@Beta
public final class PiActionProfileMember implements PiEntity {

    private final PiActionProfileId actionProfileId;
    private final PiActionProfileMemberId memberId;
    private final PiAction action;

    private PiActionProfileMember(PiActionProfileId actionProfileId,
                                  PiActionProfileMemberId memberId,
                                  PiAction action) {
        this.actionProfileId = actionProfileId;
        this.memberId = memberId;
        this.action = action;
    }

    /**
     * Returns the identifier of this member.
     *
     * @return member identifier
     */
    public PiActionProfileMemberId id() {
        return memberId;
    }

    /**
     * Returns the identifier of the action profile.
     *
     * @return action profile identifier
     */
    public PiActionProfileId actionProfile() {
        return actionProfileId;
    }

    /**
     * Returns the action associated to this member.
     *
     * @return action
     */
    public PiAction action() {
        return action;
    }

    @Override
    public PiEntityType piEntityType() {
        return PiEntityType.ACTION_PROFILE_MEMBER;
    }

    @Override
    public PiActionProfileMemberHandle handle(DeviceId deviceId) {
        return PiActionProfileMemberHandle.of(deviceId, this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PiActionProfileMember)) {
            return false;
        }
        PiActionProfileMember that = (PiActionProfileMember) o;
        return Objects.equal(actionProfileId, that.actionProfileId) &&
                Objects.equal(memberId, that.memberId) &&
                Objects.equal(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(actionProfileId, memberId, action);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("actionProfile", actionProfileId)
                .add("id", memberId)
                .add("action", action)
                .toString();
    }

    /**
     * Returns a new builder of action profile members.
     *
     * @return member builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of action profile members.
     */
    public static final class Builder {

        private PiActionProfileId actionProfileId;
        private PiActionProfileMemberId memberId;
        private PiAction action;

        private Builder() {
            // Hides constructor.
        }

        /**
         * Sets the action profile identifier of this member.
         *
         * @param actionProfileId action profile identifier
         * @return this
         */
        public Builder forActionProfile(PiActionProfileId actionProfileId) {
            this.actionProfileId = actionProfileId;
            return this;
        }

        /**
         * Sets the identifier of this member.
         *
         * @param id member identifier
         * @return this
         */
        public Builder withId(PiActionProfileMemberId id) {
            this.memberId = id;
            return this;
        }

        /**
         * Sets the action of this member.
         *
         * @param action action
         * @return this
         */
        public Builder withAction(PiAction action) {
            this.action = action;
            return this;
        }

        /**
         * Creates a new action profile member.
         *
         * @return action profile member
         */
        public PiActionProfileMember build() {
            checkNotNull(actionProfileId);
            checkNotNull(memberId);
            checkNotNull(action);
            return new PiActionProfileMember(actionProfileId, memberId, action);
        }
    }
}
