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

import java.util.Set;

import org.onosproject.net.provider.Provider;
import org.onosproject.store.service.Versioned;

/**
 * Abstraction of a {@link ClusterMetadata cluster metadata} provider.
 */
public interface ClusterMetadataProvider extends Provider {

    /**
     * Tells if this provider is currently available and therefore can provide ClusterMetadata.
     * @return {@code true} if this provider is available and can provide cluster metadata.
     */
    boolean isAvailable();

    /**
     * Returns the current cluster metadata.
     * @return cluster metadata
     */
    Versioned<ClusterMetadata> getClusterMetadata();

    /**
     * Updates cluster metadata.
     * @param metadata new metadata
     */
    void setClusterMetadata(ClusterMetadata metadata);

    /**
     * Adds a controller node to the list of active members for a partition.
     * <p>
     * Active members of a partition are those that are actively participating
     * in the data replication protocol being employed. When a node first added
     * to a partition, it is in a passive or catch up mode where it attempts to
     * bring it self up to speed with other active members in the partition.
     * @param partitionId partition identifier
     * @param nodeId identifier of controller node
     */
    void addActivePartitionMember(PartitionId partitionId, NodeId nodeId);

    /**
     * Removes a controller node from the list of active members for a partition.
     * @param partitionId partition identifier
     * @param nodeId identifier of controller node
     */
    void removeActivePartitionMember(PartitionId partitionId, NodeId nodeId);

    /**
     * Returns the set of controller nodes that are the active members for a partition.
     * <p>
     * Active members of a partition are typically those that are actively
     * participating in the data replication protocol being employed. When
     * a node first added to a partition, it is in a passive or catch up mode where
     * it attempts to bring it self up to speed with other active members in the partition.
     * <p>
     * <b>Note:</b>If is possible for this list to different from the list of partition members
     * specified by cluster meta data. The discrepancy can arise due to the fact that
     * adding/removing members from a partition requires a data hand-off mechanism to complete.
     * @param partitionId partition identifier
     * @return identifiers of controller nodes that are active members
     */
    Set<NodeId> getActivePartitionMembers(PartitionId partitionId);
}
