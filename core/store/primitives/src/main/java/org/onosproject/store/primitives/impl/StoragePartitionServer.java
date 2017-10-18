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
package org.onosproject.store.primitives.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import io.atomix.protocols.raft.RaftServer;
import io.atomix.protocols.raft.cluster.MemberId;
import io.atomix.protocols.raft.cluster.RaftMember;
import io.atomix.protocols.raft.storage.RaftStorage;
import io.atomix.storage.StorageLevel;
import org.onosproject.core.Version;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.primitives.resources.impl.AtomixSerializerAdapter;
import org.onosproject.store.service.PartitionInfo;
import org.onosproject.store.service.Serializer;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@link StoragePartition} server.
 */
public class StoragePartitionServer implements Managed<StoragePartitionServer> {

    private final Logger log = getLogger(getClass());

    private static final int MAX_SEGMENT_SIZE = 1024 * 1024 * 64;
    private static final long ELECTION_TIMEOUT_MILLIS = 2500;
    private static final long HEARTBEAT_INTERVAL_MILLIS = 1000;

    private final MemberId localMemberId;
    private final StoragePartition partition;
    private final ClusterCommunicationService clusterCommunicator;
    private RaftServer server;

    public StoragePartitionServer(
            StoragePartition partition,
            MemberId localMemberId,
            ClusterCommunicationService clusterCommunicator) {
        this.partition = partition;
        this.localMemberId = localMemberId;
        this.clusterCommunicator = clusterCommunicator;
    }

    @Override
    public CompletableFuture<Void> open() {
        log.info("Starting server for partition {} ({})", partition.getId(), partition.getVersion());
        CompletableFuture<RaftServer> serverOpenFuture;
        if (partition.getMemberIds().contains(localMemberId)) {
            if (server != null && server.isRunning()) {
                return CompletableFuture.completedFuture(null);
            }
            synchronized (this) {
                server = buildServer();
            }
            serverOpenFuture = server.bootstrap(partition.getMemberIds());
        } else {
            serverOpenFuture = CompletableFuture.completedFuture(null);
        }
        return serverOpenFuture.whenComplete((r, e) -> {
            if (e == null) {
                log.info("Successfully started server for partition {} ({})",
                        partition.getId(), partition.getVersion());
            } else {
                log.info("Failed to start server for partition {} ({})",
                        partition.getId(), partition.getVersion(), e);
            }
        }).thenApply(v -> null);
    }

    @Override
    public CompletableFuture<Void> close() {
        return server.shutdown();
    }

    /**
     * Closes the server and exits the partition.
     * @return future that is completed when the operation is complete
     */
    public CompletableFuture<Void> closeAndExit() {
        return server.leave();
    }

    /**
     * Forks the existing partition into a new partition.
     *
     * @param version the version from which to fork the server
     * @return future to be completed once the fork operation is complete
     */
    public CompletableFuture<Void> fork(Version version) {
        log.info("Forking server for partition {} ({}->{})", partition.getId(), version, partition.getVersion());
        RaftServer.Builder builder = RaftServer.newBuilder(localMemberId)
                .withName(partition.getName(version))
                .withType(RaftMember.Type.PASSIVE)
                .withProtocol(new RaftServerCommunicator(
                        partition.getName(version),
                        Serializer.using(StorageNamespaces.RAFT_PROTOCOL),
                        clusterCommunicator))
                .withElectionTimeout(Duration.ofMillis(ELECTION_TIMEOUT_MILLIS))
                .withHeartbeatInterval(Duration.ofMillis(HEARTBEAT_INTERVAL_MILLIS))
                .withStorage(RaftStorage.newBuilder()
                        .withStorageLevel(StorageLevel.MAPPED)
                        .withSerializer(new AtomixSerializerAdapter(Serializer.using(StorageNamespaces.RAFT_STORAGE)))
                        .withDirectory(partition.getDataFolder())
                        .withMaxSegmentSize(MAX_SEGMENT_SIZE)
                        .build());
        StoragePartition.RAFT_SERVICES.forEach(builder::addService);
        RaftServer server = builder.build();
        return server.join(partition.getMemberIds(version))
                .thenCompose(v -> server.shutdown())
                .thenCompose(v -> {
                    // Delete the cluster configuration file from the forked partition.
                    try {
                        Files.delete(new File(partition.getDataFolder(), "atomix.conf").toPath());
                    } catch (IOException e) {
                        log.error("Failed to delete partition configuration: {}", e);
                    }

                    // Build and bootstrap a new server.
                    this.server = buildServer();
                    return this.server.bootstrap();
                }).whenComplete((r, e) -> {
                    if (e == null) {
                        log.info("Successfully forked server for partition {} ({}->{})",
                                partition.getId(), version, partition.getVersion());
                    } else {
                        log.info("Failed to fork server for partition {} ({}->{})",
                                partition.getId(), version, partition.getVersion(), e);
                    }
                }).thenApply(v -> null);
    }

    private RaftServer buildServer() {
        RaftServer.Builder builder = RaftServer.newBuilder(localMemberId)
                .withName(partition.getName())
                .withProtocol(new RaftServerCommunicator(
                        partition.getName(),
                        Serializer.using(StorageNamespaces.RAFT_PROTOCOL),
                        clusterCommunicator))
                .withElectionTimeout(Duration.ofMillis(ELECTION_TIMEOUT_MILLIS))
                .withHeartbeatInterval(Duration.ofMillis(HEARTBEAT_INTERVAL_MILLIS))
                .withStorage(RaftStorage.newBuilder()
                        .withStorageLevel(StorageLevel.MAPPED)
                        .withSerializer(new AtomixSerializerAdapter(Serializer.using(StorageNamespaces.RAFT_STORAGE)))
                        .withDirectory(partition.getDataFolder())
                        .withMaxSegmentSize(MAX_SEGMENT_SIZE)
                        .build());
        StoragePartition.RAFT_SERVICES.forEach(builder::addService);
        return builder.build();
    }

    public CompletableFuture<Void> join(Collection<MemberId> otherMembers) {
        log.info("Joining partition {} ({})", partition.getId(), partition.getVersion());
        server = buildServer();
        return server.join(otherMembers).whenComplete((r, e) -> {
            if (e == null) {
                log.info("Successfully joined partition {} ({})", partition.getId(), partition.getVersion());
            } else {
                log.info("Failed to join partition {} ({})", partition.getId(), partition.getVersion(), e);
            }
        }).thenApply(v -> null);
    }

    @Override
    public boolean isOpen() {
        return server.isRunning();
    }

    /**
     * Returns the partition information.
     * @return partition info
     */
    public PartitionInfo info() {
        return new StoragePartitionDetails(partition.getId(),
                server.cluster().getMembers(),
                server.cluster().getMembers(),
                server.cluster().getLeader(),
                server.cluster().getTerm()).toPartitionInfo();
    }
}
