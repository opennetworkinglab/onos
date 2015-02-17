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

package org.onosproject.store.consistent.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import net.kuujo.copycat.CopycatConfig;
import net.kuujo.copycat.cluster.ClusterConfig;
import net.kuujo.copycat.cluster.internal.coordinator.ClusterCoordinator;
import net.kuujo.copycat.cluster.internal.coordinator.DefaultClusterCoordinator;
import net.kuujo.copycat.util.concurrent.NamedThreadFactory;

/**
 * Manages a PartitionedDatabase.
 */
public interface PartitionedDatabaseManager {
    /**
     * Opens the database.
     *
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<PartitionedDatabase> open();

    /**
     * Closes the database.
     *
     * @return A completable future to be completed with the result once complete.
     */
    CompletableFuture<Void> close();

    /**
     * Sets the partitioner to use for mapping keys to partitions.
     *
     * @param partitioner partitioner
     */
    void setPartitioner(Partitioner<String> partitioner);

    /**
     * Registers a new partition.
     *
     * @param partitionName partition name.
     * @param partition partition.
     */
    void registerPartition(String partitionName, Database partition);

    /**
     * Returns all the registered database partitions.
     *
     * @return mapping of all registered database partitions.
     */
    Map<String, Database> getRegisteredPartitions();


    /**
     * Creates a new partitioned database.
     *
     * @param name The database name.
     * @param clusterConfig The cluster configuration.
     * @param partitionedDatabaseConfig The database configuration.

     * @return The database.
     */
    public static PartitionedDatabase create(
            String name,
            ClusterConfig clusterConfig,
            PartitionedDatabaseConfig partitionedDatabaseConfig) {
        CopycatConfig copycatConfig = new CopycatConfig()
            .withName(name)
            .withClusterConfig(clusterConfig)
            .withDefaultSerializer(new DatabaseSerializer())
            .withDefaultExecutor(Executors.newSingleThreadExecutor(new NamedThreadFactory("copycat-coordinator-%d")));
        ClusterCoordinator coordinator = new DefaultClusterCoordinator(copycatConfig.resolve());
        PartitionedDatabase partitionedDatabase = new PartitionedDatabase(coordinator);
        partitionedDatabaseConfig.partitions().forEach((partitionName, partitionConfig) ->
            partitionedDatabase.registerPartition(partitionName ,
                    coordinator.getResource(partitionName, partitionConfig.resolve(clusterConfig)
                        .withSerializer(copycatConfig.getDefaultSerializer())
                        .withDefaultExecutor(copycatConfig.getDefaultExecutor()))));
        partitionedDatabase.setPartitioner(
                new SimpleKeyHashPartitioner(partitionedDatabase.getRegisteredPartitions()));
        return partitionedDatabase;
    }
}
