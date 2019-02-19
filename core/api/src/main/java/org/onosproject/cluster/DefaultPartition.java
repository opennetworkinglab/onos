/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.cluster;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link Partition} implementation.
 */
public class DefaultPartition implements Partition {

    private final PartitionId id;
    private final Collection<NodeId> members;

    /**
     * Constructs an empty partition for the serializer.
     */
    protected DefaultPartition() {
        id = null;
        members = null;
    }

    /**
     * Constructs a partition.
     *
     * @param id partition identifier
     * @param members partition member nodes
     */
    public DefaultPartition(PartitionId id, Collection<NodeId> members) {
        this.id = checkNotNull(id);
        this.members = ImmutableSet.copyOf(members);
    }

    /**
     * Constructs a partition that is a copy of another.
     *
     * @param other partition to copy
     */
    public DefaultPartition(Partition other) {
        this.id = checkNotNull(other.getId());
        this.members = ImmutableSet.copyOf(other.getMembers());
    }

    @Override
    public PartitionId getId() {
        return id;
    }

    @Override
    public Collection<NodeId> getMembers() {
        return members;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id)
                .add("members", members)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, members);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DefaultPartition)) {
            return false;
        }
        DefaultPartition that = (DefaultPartition) other;
        return this.getId().equals(that.getId()) &&
                Sets.symmetricDifference(Sets.newHashSet(this.members), Sets.newHashSet(that.members)).isEmpty();
    }
}
