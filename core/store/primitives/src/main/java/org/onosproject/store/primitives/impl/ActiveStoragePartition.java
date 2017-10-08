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
package org.onosproject.store.primitives.impl;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import io.atomix.protocols.raft.cluster.MemberId;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.Partition;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;

/**
 * Storage partition active on the local node.
 */
public class ActiveStoragePartition extends StoragePartition {
    public ActiveStoragePartition(
            Partition partition,
            ClusterCommunicationService clusterCommunicator,
            ClusterService clusterService) {
        super(partition, clusterCommunicator, clusterService);
    }

    @Override
    public String getName() {
        return partition.getId().toString();
    }

    @Override
    public File getDataFolder() {
        return new File(PARTITIONS_DIR + partition.getId());
    }

    @Override
    protected CompletableFuture<Void> openServer() {
        StoragePartitionServer server = new StoragePartitionServer(
                this,
                MemberId.from(localNodeId.id()),
                clusterCommunicator);
        return server.open().thenRun(() -> this.server = server);
    }
}
