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
 * Instance of an action group of a protocol-independent pipeline.
 */
@Beta
public final class PiActionGroup implements PiEntity {

    private final PiActionGroupId id;
    private final ImmutableSet<PiActionGroupMember> members;
    private final PiActionProfileId piActionProfileId;

    private PiActionGroup(PiActionGroupId id, ImmutableSet<PiActionGroupMember> members,
                          PiActionProfileId piActionProfileId) {
        this.id = id;
        this.members = members;
        this.piActionProfileId = piActionProfileId;
    }

    /**
     * Returns the identifier of this action group.
     *
     * @return action group identifier
     */
    public PiActionGroupId id() {
        return id;
    }

    /**
     * Returns the members of this action group.
     *
     * @return collection of action members.
     */
    public Collection<PiActionGroupMember> members() {
        return members;
    }

    /**
     * Gets identifier of the action profile.
     *
     * @return action profile id
     */
    public PiActionProfileId actionProfileId() {
        return piActionProfileId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof PiActionGroup)) {
            return false;
        }
        PiActionGroup that = (PiActionGroup) o;
        return Objects.equal(id, that.id) &&
                Objects.equal(members, that.members) &&
                Objects.equal(piActionProfileId, that.piActionProfileId);
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
                .add("piActionProfileId", piActionProfileId)
                .toString();
    }

    /**
     * Returns a new builder of action groups.
     *
     * @return action group builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public PiEntityType piEntityType() {
        return PiEntityType.GROUP;
    }

    /**
     * Builder of action groups.
     */
    public static final class Builder {

        private PiActionGroupId id;
        private Map<PiActionGroupMemberId, PiActionGroupMember> members = Maps.newHashMap();
        private PiActionProfileId piActionProfileId;

        private Builder() {
            // hides constructor.
        }

        /**
         * Sets the identifier of this action group.
         *
         * @param id action group identifier
         * @return this
         */
        public Builder withId(PiActionGroupId id) {
            this.id = id;
            return this;
        }

        /**
         * Adds one member to this action group.
         *
         * @param member action group member
         * @return this
         */
        public Builder addMember(PiActionGroupMember member) {
            members.put(member.id(), member);
            return this;
        }

        /**
         * Adds many members to this action group.
         *
         * @param members action group members
         * @return this
         */
        public Builder addMembers(Collection<PiActionGroupMember> members) {
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
         * Creates a new action group.
         *
         * @return action group
         */
        public PiActionGroup build() {
            checkNotNull(id);
            checkNotNull(piActionProfileId);
            return new PiActionGroup(id, ImmutableSet.copyOf(members.values()),
                                     piActionProfileId);
        }
    }
}
