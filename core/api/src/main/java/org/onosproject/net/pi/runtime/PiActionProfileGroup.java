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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.onosproject.net.pi.model.PiActionProfileId;

import java.util.Collection;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Instance of an action profile group of a protocol-independent pipeline.
 */
@Beta
public final class PiActionProfileGroup implements PiEntity {

    private final PiActionProfileGroupId id;
    private final ImmutableSet<PiActionProfileMember> members;
    private final PiActionProfileId actionProfileId;

    private PiActionProfileGroup(PiActionProfileGroupId id,
                                 ImmutableSet<PiActionProfileMember> members,
                                 PiActionProfileId actionProfileId) {
        this.id = id;
        this.members = members;
        this.actionProfileId = actionProfileId;
    }

    /**
     * Returns the identifier of this action profile group.
     *
     * @return action profile group identifier
     */
    public PiActionProfileGroupId id() {
        return id;
    }

    /**
     * Returns the members of this action profile group.
     *
     * @return collection of action profile members.
     */
    public Collection<PiActionProfileMember> members() {
        return members;
    }

    /**
     * Gets identifier of the action profile.
     *
     * @return action profile id
     */
    public PiActionProfileId actionProfileId() {
        return actionProfileId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof PiActionProfileGroup)) {
            return false;
        }
        PiActionProfileGroup that = (PiActionProfileGroup) o;
        return Objects.equal(id, that.id) &&
                Objects.equal(members, that.members) &&
                Objects.equal(actionProfileId, that.actionProfileId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, members);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("groupId", id)
                .add("members", members)
                .add("piActionProfileId", actionProfileId)
                .toString();
    }

    /**
     * Returns a new builder of action profile groups.
     *
     * @return action profile group builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public PiEntityType piEntityType() {
        return PiEntityType.ACTION_PROFILE_GROUP;
    }

    /**
     * Builder of action profile groups.
     */
    public static final class Builder {

        private PiActionProfileGroupId id;
        private Map<PiActionProfileMemberId, PiActionProfileMember> members = Maps.newHashMap();
        private PiActionProfileId piActionProfileId;

        private Builder() {
            // hides constructor.
        }

        /**
         * Sets the identifier of this action profile group.
         *
         * @param id action profile group identifier
         * @return this
         */
        public Builder withId(PiActionProfileGroupId id) {
            this.id = id;
            return this;
        }

        /**
         * Adds one member to this action profile group.
         *
         * @param member action profile member
         * @return this
         */
        public Builder addMember(PiActionProfileMember member) {
            members.put(member.id(), member);
            return this;
        }

        /**
         * Adds many members to this action profile group.
         *
         * @param members action profile members
         * @return this
         */
        public Builder addMembers(Collection<PiActionProfileMember> members) {
            members.forEach(this::addMember);
            return this;
        }

        /**
         * Sets the identifier of the action profile.
         *
         * @param piActionProfileId the identifier of the action profile
         * @return this
         */
        public Builder withActionProfileId(PiActionProfileId piActionProfileId) {
            this.piActionProfileId = piActionProfileId;
            return this;
        }

        /**
         * Creates a new action profile group.
         *
         * @return action profile group
         */
        public PiActionProfileGroup build() {
            checkNotNull(id);
            checkNotNull(piActionProfileId);
            return new PiActionProfileGroup(
                    id, ImmutableSet.copyOf(members.values()), piActionProfileId);
        }
    }
}
