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
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.DeviceId;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of multicast group entry of a protocol-independent packet
 * replication engine (PRE).
 */
@Beta
public final class PiMulticastGroupEntry implements PiPreEntry {

    private final int groupId;
    private final Set<PiPreReplica> replicas;

    private PiMulticastGroupEntry(int groupId, Set<PiPreReplica> replicas) {
        this.groupId = groupId;
        this.replicas = replicas;
    }

    /**
     * Returns the identifier of this multicast group, unique in the scope of a
     * PRE instance.
     *
     * @return group entry ID
     */
    public int groupId() {
        return groupId;
    }

    /**
     * Returns the packet replicas provided by this multicast group.
     *
     * @return packet replicas
     */
    public Set<PiPreReplica> replicas() {
        return replicas;
    }

    @Override
    public PiPreEntryType preEntryType() {
        return PiPreEntryType.MULTICAST_GROUP;
    }

    @Override
    public PiEntityType piEntityType() {
        return PiEntityType.PRE_ENTRY;
    }

    @Override
    public PiMulticastGroupEntryHandle handle(DeviceId deviceId) {
        return PiMulticastGroupEntryHandle.of(deviceId, this);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(groupId, replicas);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PiMulticastGroupEntry other = (PiMulticastGroupEntry) obj;
        return Objects.equal(this.groupId, other.groupId)
                && Objects.equal(this.replicas, other.replicas);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("groupId", "0x" + Integer.toHexString(groupId))
                .add("replicas", replicas)
                .toString();
    }

    /**
     * Returns a new builder of multicast group entries.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of PI multicast group entries.
     */
    public static final class Builder {

        private Integer groupId;
        private ImmutableSet.Builder<PiPreReplica> replicaSetBuilder = ImmutableSet.builder();

        private Builder() {
            // Hide constructor.
        }

        /**
         * Sets the identifier of this multicast group.
         *
         * @param groupId group ID
         * @return this
         */
        public Builder withGroupId(int groupId) {
            this.groupId = groupId;
            return this;
        }

        /**
         * Adds the given packet replica to this multicast group.
         *
         * @param replica packet replica
         * @return this
         */
        public Builder addReplica(PiPreReplica replica) {
            checkNotNull(replica);
            replicaSetBuilder.add(replica);
            return this;
        }

        /**
         * Adds the given packet replicas to this multicast group.
         *
         * @param replicas packet replicas
         * @return this
         */
        public Builder addReplicas(Collection<PiPreReplica> replicas) {
            checkNotNull(replicas);
            replicaSetBuilder.addAll(replicas);
            return this;
        }

        /**
         * Returns a new multicast group entry.
         *
         * @return multicast group entry
         */
        public PiMulticastGroupEntry build() {
            checkNotNull(groupId, "Multicast group ID must be set");
            final ImmutableSet<PiPreReplica> replicas = replicaSetBuilder.build();
            return new PiMulticastGroupEntry(groupId, replicas);
        }
    }
}
