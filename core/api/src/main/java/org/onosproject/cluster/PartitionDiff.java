/*
 * Copyright 2016-present Open Networking Laboratory
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

import java.util.Objects;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Utility for examining differences between two {@link Partition partition} values.
 */
public class PartitionDiff {

    private final Partition oldValue;
    private final Partition newValue;
    private final PartitionId partitionId;
    private final Set<NodeId> currentMembers;
    private final Set<NodeId> newMembers;

    public PartitionDiff(Partition oldValue, Partition newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.partitionId = oldValue == null ? null : oldValue.getId();
        this.currentMembers = oldValue == null ? ImmutableSet.of() : ImmutableSet.copyOf(oldValue.getMembers());
        this.newMembers = newValue == null ? ImmutableSet.of() : ImmutableSet.copyOf(newValue.getMembers());
    }

    /**
     * Returns the new partition identifier.
     * @return partition id
     */
    public PartitionId partitionId() {
        return partitionId;
    }

    /**
     * Returns the old partition value.
     * @return partition
     */
    public Partition oldValue() {
        return oldValue;
    }

    /**
     * Returns the new partition value.
     * @return partition
     */
    public Partition newValue() {
        return newValue;
    }

    /**
     * Returns if there are differences between the two values.
     * @return {@code true} if yes; {@code false} otherwise
     */
    public boolean hasChanged() {
        return !Sets.symmetricDifference(currentMembers, newMembers).isEmpty();
    }

    /**
     * Returns if the specified node is introduced in the new value.
     * @param nodeId node identifier
     * @return {@code true} if yes; {@code false} otherwise
     */
    public boolean isAdded(NodeId nodeId) {
        return !currentMembers.contains(nodeId) && newMembers.contains(nodeId);
    }

    /**
     * Returns if the specified node is removed in the new value.
     * @param nodeId node identifier
     * @return {@code true} if yes; {@code false} otherwise
     */
    public boolean isRemoved(NodeId nodeId) {
        return currentMembers.contains(nodeId) && !newMembers.contains(nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldValue, newValue);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof PartitionDiff)) {
            return false;
        }
        PartitionDiff that = (PartitionDiff) other;
        return Objects.equals(this.oldValue, that.oldValue) &&
                Objects.equals(this.newValue, that.newValue);

    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("oldValue", oldValue)
                .add("newValue", newValue)
                .toString();
    }
}
