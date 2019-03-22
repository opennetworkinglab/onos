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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiActionProfileId;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Instance of an action profile group of a protocol-independent pipeline.
 */
@Beta
public final class PiActionProfileGroup implements PiEntity {

    private final PiActionProfileId actionProfileId;
    private final PiActionProfileGroupId groupId;
    private final ImmutableMap<PiActionProfileMemberId, WeightedMember> members;
    private final int maxSize;

    private PiActionProfileGroup(PiActionProfileGroupId groupId,
                                 ImmutableMap<PiActionProfileMemberId, WeightedMember> members,
                                 PiActionProfileId actionProfileId,
                                 int maxSize) {
        this.groupId = groupId;
        this.members = members;
        this.actionProfileId = actionProfileId;
        this.maxSize = maxSize;
    }

    /**
     * Returns the ID of this action profile group.
     *
     * @return action profile group ID
     */
    public PiActionProfileGroupId id() {
        return groupId;
    }

    /**
     * Returns the list of member references of this action profile group.
     *
     * @return collection of action profile members.
     */
    public Collection<WeightedMember> members() {
        return members.values();
    }

    /**
     * Returns the group member identified by the given action profile member
     * ID, if present.
     *
     * @param memberId action profile member ID
     * @return optional group member
     */
    public Optional<WeightedMember> member(PiActionProfileMemberId memberId) {
        return Optional.of(members.get(memberId));
    }

    /**
     * Returns the maximum number of members that this group can hold. 0
     * signifies that a limit is not set.
     *
     * @return maximum number of members that this group can hold
     */
    public int maxSize() {
        return maxSize;
    }

    /**
     * Returns the ID of the action profile where this group belong.
     *
     * @return action profile ID
     */
    public PiActionProfileId actionProfile() {
        return actionProfileId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PiActionProfileGroup other = (PiActionProfileGroup) obj;
        return Objects.equal(this.groupId, other.groupId)
                && Objects.equal(this.members, other.members)
                // FIXME: re-enable when this PI bug will be fixed:
                // https://github.com/p4lang/PI/issues/452
                // Currently PI-based devices always return max_group_size 0,
                // event if we set a different one.
                // && Objects.equal(this.maxSize, other.maxSize)
                && Objects.equal(this.actionProfileId, other.actionProfileId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(groupId, members, maxSize, actionProfileId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("actionProfile", actionProfileId)
                .add("id", groupId)
                .add("members", members.values())
                .add("maxSize", maxSize)
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

    @Override
    public PiActionProfileGroupHandle handle(DeviceId deviceId) {
        return PiActionProfileGroupHandle.of(deviceId, this);
    }

    /**
     * Builder of action profile groups.
     */
    public static final class Builder {

        private PiActionProfileGroupId groupId;
        private Map<PiActionProfileMemberId, WeightedMember> members = Maps.newHashMap();
        private PiActionProfileId actionProfileId;
        private int maxSize;

        private Builder() {
            // hides constructor.
        }

        /**
         * Sets the ID of this action profile group.
         *
         * @param id action profile group ID
         * @return this
         */
        public Builder withId(PiActionProfileGroupId id) {
            this.groupId = id;
            return this;
        }

        /**
         * Adds one member to this action profile.
         *
         * @param member member to add
         * @return this
         */
        public Builder addMember(WeightedMember member) {
            checkNotNull(member);
            members.put(member.id(), member);
            return this;
        }

        /**
         * Adds one member to this action profile group with default weight.
         *
         * @param memberId ID of the action profile member to add
         * @return this
         */
        public Builder addMember(PiActionProfileMemberId memberId) {
            addMember(new WeightedMember(memberId, WeightedMember.DEFAULT_WEIGHT));
            return this;
        }

        /**
         * Adds one member to this action profile group with default weight.
         *
         * @param memberInstance the action profile member instance to add
         * @return this
         */
        public Builder addMember(PiActionProfileMember memberInstance) {
            addMember(new WeightedMember(memberInstance, WeightedMember.DEFAULT_WEIGHT));
            return this;
        }

        /**
         * Adds all members to this action profile group with default weight.
         *
         * @param memberInstances the action profile member instance to add
         * @return this
         */
        public Builder addMembers(Iterable<PiActionProfileMember> memberInstances) {
            memberInstances.forEach(this::addMember);
            return this;
        }

        /**
         * Adds one member to this action profile group with the given weight.
         *
         * @param memberId ID of the action profile member to add
         * @param weight   weight
         * @return this
         */
        public Builder addMember(PiActionProfileMemberId memberId, int weight) {
            addMember(new WeightedMember(memberId, weight));
            return this;
        }

        /**
         * Adds one member to this action profile group with the given weight.
         *
         * @param memberInstance the action profile member instance to add
         * @param weight         weight
         * @return this
         */
        public Builder addMember(PiActionProfileMember memberInstance, int weight) {
            addMember(new WeightedMember(memberInstance, weight));
            return this;
        }

        /**
         * Sets the ID of the action profile.
         *
         * @param piActionProfileId the ID of the action profile
         * @return this
         */
        public Builder withActionProfileId(PiActionProfileId piActionProfileId) {
            this.actionProfileId = piActionProfileId;
            return this;
        }

        /**
         * Sets the maximum number of members that this group can hold.
         *
         * @param maxSize maximum number of members that this group can hold
         * @return this
         */
        public Builder withMaxSize(int maxSize) {
            checkArgument(maxSize >= 0, "maxSize cannot be negative");
            this.maxSize = maxSize;
            return this;
        }

        /**
         * Creates a new action profile group.
         *
         * @return action profile group
         */
        public PiActionProfileGroup build() {
            checkNotNull(groupId);
            checkNotNull(actionProfileId);
            checkArgument(maxSize == 0 || members.size() <= maxSize,
                          "The number of members cannot exceed maxSize");
            final boolean validActionProfileId = members.isEmpty() || members.values()
                    .stream().allMatch(m -> m.instance() == null || m.instance()
                            .actionProfile().equals(actionProfileId));
            checkArgument(
                    validActionProfileId,
                    "The members' action profile ID must match the group one");
            return new PiActionProfileGroup(
                    groupId, ImmutableMap.copyOf(members), actionProfileId, maxSize);
        }
    }

    /**
     * Weighted reference to an action profile member as used in an action
     * profile group.
     */
    public static final class WeightedMember {

        public static final int DEFAULT_WEIGHT = 1;

        private final PiActionProfileMemberId memberId;
        private final int weight;
        private final PiActionProfileMember memberInstance;

        /**
         * Creates a new reference for the given action profile member ID and
         * weight.
         *
         * @param memberId action profile member ID
         * @param weight   weight
         */
        public WeightedMember(PiActionProfileMemberId memberId, int weight) {
            checkNotNull(memberId);
            this.memberId = memberId;
            this.weight = weight;
            this.memberInstance = null;
        }

        /**
         * Creates a new reference from the given action profile member instance
         * and weight. This constructor should be used when performing one-shot
         * group programming (see {@link #instance()}).
         *
         * @param memberInstance action profile member instance
         * @param weight         weight
         */
        public WeightedMember(PiActionProfileMember memberInstance, int weight) {
            checkNotNull(memberInstance);
            this.memberId = memberInstance.id();
            this.weight = weight;
            this.memberInstance = memberInstance;
        }

        /**
         * Returns the ID of the action profile member.
         *
         * @return action profile member ID
         */
        public PiActionProfileMemberId id() {
            return memberId;
        }

        /**
         * Returns the weight of this group member.
         *
         * @return weight
         */
        public int weight() {
            return weight;
        }

        /**
         * If present, returns the instance of the action profile member pointed
         * by this reference, otherwise returns null. This method is provided as
         * a convenient way to perform one-shot group programming, and as such
         * is meaningful only when performing write operations to a device. In
         * other words, when reading groups from a device only the member
         * reference should be returned and not the actual instance, hence this
         * method should return null.
         *
         * @return action profile member instance, or null
         */
        public PiActionProfileMember instance() {
            return memberInstance;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(memberId, weight);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final WeightedMember other = (WeightedMember) obj;
            return Objects.equal(this.memberId, other.memberId)
                    && Objects.equal(this.weight, other.weight);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("memberId", memberId)
                    .add("weight", weight)
                    .toString();
        }
    }
}
