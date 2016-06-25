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

import static com.google.common.base.Preconditions.checkState;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Utility for examining differences between two {@link ClusterMetadata metadata} values.
 */
public class ClusterMetadataDiff {

    private final ClusterMetadata oldValue;
    private final ClusterMetadata newValue;
    private final Set<ControllerNode> nodesAdded;
    private final Set<NodeId> nodesRemoved;

     public ClusterMetadataDiff(ClusterMetadata oldValue, ClusterMetadata newValue) {
         this.oldValue = oldValue;
         this.newValue = newValue;

         Set<ControllerNode> currentNodeSet = oldValue == null
                 ? ImmutableSet.of() : ImmutableSet.copyOf(oldValue.getNodes());
         Set<ControllerNode> newNodeSet = newValue == null
                 ? ImmutableSet.of() : ImmutableSet.copyOf(newValue.getNodes());
         nodesAdded = Sets.difference(newNodeSet, currentNodeSet);
         nodesRemoved = Sets.difference(currentNodeSet, newNodeSet)
                            .stream()
                            .map(ControllerNode::id)
                            .collect(Collectors.toSet());
     }

    /**
     * Returns the set of {@link ControllerNode nodes} added with this metadata change.
     * @return set of controller nodes
     */
    public Set<ControllerNode> nodesAdded() {
        return nodesAdded;
    }

    /**
     * Returns the set of {@link ControllerNode nodes} removed with this metadata change.
     * @return set of controller node identifiers
     */
    public Set<NodeId> nodesRemoved() {
        return nodesRemoved;
    }

    /**
     * Returns a mapping of all partition diffs.
     * @return partition diffs.
     */
    public Map<PartitionId, PartitionDiff> partitionDiffs() {
        Map<PartitionId, Partition> oldPartitions = Maps.newHashMap();
        oldValue.getPartitions()
                .forEach(p -> oldPartitions.put(p.getId(), p));
        Map<PartitionId, Partition> newPartitions = Maps.newHashMap();
        newValue.getPartitions()
                .forEach(p -> newPartitions.put(p.getId(), p));
        checkState(Sets.symmetricDifference(oldPartitions.keySet(), newPartitions.keySet()).isEmpty(),
                   "Number of partitions cannot change");
        Map<PartitionId, PartitionDiff> partitionDiffs = Maps.newHashMap();
        oldPartitions.forEach((k, v) -> {
            partitionDiffs.put(k, new PartitionDiff(v, newPartitions.get(k)));
        });
        return partitionDiffs;
    }
}
