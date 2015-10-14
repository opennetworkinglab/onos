/*
 * Copyright 2015 Open Networking Laboratory
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

import java.util.Collection;

import org.onosproject.store.Store;
import org.onosproject.store.service.Versioned;

/**
 * Manages persistence of cluster metadata; not intended for direct use.
 */
public interface ClusterMetadataStore extends Store<ClusterMetadataEvent, ClusterMetadataStoreDelegate> {

    /**
     * Returns the cluster metadata.
     * <p>
     * The returned metadata is versioned to aid determining if a metadata instance is more recent than another.
     * @return cluster metadata
     */
    Versioned<ClusterMetadata> getClusterMetadata();

    /**
     * Updates the cluster metadata.
     * @param metadata new metadata value
     */
    void setClusterMetadata(ClusterMetadata metadata);

    // TODO: The below methods should move to a separate store interface that is responsible for
    // tracking cluster partition operational state.

    /**
     * Sets a controller node as an active member of a partition.
     * <p>
     * Active members are those replicas that are up to speed with the rest of the system and are
     * usually capable of participating in the replica state management activities in accordance with
     * the data consistency and replication protocol in use.
     * @param partitionId partition identifier
     * @param nodeId id of controller node
     */
    void setActiveReplica(String partitionId, NodeId nodeId);

    /**
     * Removes a controller node as an active member for a partition.
     * <p>
     * Active members are those replicas that are up to speed with the rest of the system and are
     * usually capable of participating in the replica state management activities in accordance with
     * the data consistency and replication protocol in use.
     * @param partitionId partition identifier
     * @param nodeId id of controller node
     */
    void unsetActiveReplica(String partitionId, NodeId nodeId);

    /**
     * Returns the collection of controller nodes that are the active replicas for a partition.
     * <p>
     * Active members are those replicas that are up to speed with the rest of the system and are
     * usually capable of participating in the replica state management activities in accordance with
     * the data consistency and replication protocol in use.
     * @param partitionId partition identifier
     * @return identifiers of controller nodes that are the active replicas
     */
    Collection<NodeId> getActiveReplicas(String partitionId);
}