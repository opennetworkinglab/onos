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
package org.onosproject.store.service;

import java.util.Collection;

import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.PartitionId;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Contains information about a database partition client.
 */
public class PartitionClientInfo {
    private final PartitionId partitionId;
    private final Collection<NodeId> servers;

    public PartitionClientInfo(PartitionId partitionId, Collection<NodeId> servers) {
        this.partitionId = checkNotNull(partitionId);
        this.servers = ImmutableList.copyOf(checkNotNull(servers));
    }

    /**
     * Returns the identifier for the partition.
     *
     * @return partition id
     */
    public PartitionId partitionId() {
        return partitionId;
    }

    /**
     * Returns the collection of servers that are members of the partition.
     *
     * @return active members of the partition
     */
    public Collection<NodeId> servers() {
        return servers;
    }
}
