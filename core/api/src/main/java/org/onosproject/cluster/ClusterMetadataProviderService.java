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

import org.onosproject.net.provider.ProviderService;
import org.onosproject.store.service.Versioned;

/**
 * Service through which a {@link ClusterMetadataProvider provider} can notify core of
 * updates made to cluster metadata.
 */
public interface ClusterMetadataProviderService extends ProviderService<ClusterMetadataProvider> {

    /**
     * Notifies about a change to cluster metadata.
     * @param newMetadata new cluster metadata value
     */
    void clusterMetadataChanged(Versioned<ClusterMetadata> newMetadata);

    /**
     * Notifies that a node just become the active member of a partition.
     * @param partitionId partition identifier
     * @param nodeId identifier of node
     */
    void newActiveMemberForPartition(PartitionId partitionId, NodeId nodeId);
}
