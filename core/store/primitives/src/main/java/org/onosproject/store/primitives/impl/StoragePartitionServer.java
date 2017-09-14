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
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import io.atomix.protocols.raft.RaftServer;
import io.atomix.protocols.raft.cluster.MemberId;
import io.atomix.protocols.raft.protocol.RaftServerProtocol;
import io.atomix.protocols.raft.storage.RaftStorage;
import io.atomix.storage.StorageLevel;
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
    private final Supplier<RaftServerProtocol> protocol;
    private final File dataFolder;
    private RaftServer server;

    public StoragePartitionServer(
            StoragePartition partition,
            MemberId localMemberId,
            Supplier<RaftServerProtocol> protocol,
            File dataFolder) {
        this.partition = partition;
        this.localMemberId = localMemberId;
        this.protocol = protocol;
        this.dataFolder = dataFolder;
    }

    @Override
    public CompletableFuture<Void> open() {
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
                log.info("Successfully started server for partition {}", partition.getId());
            } else {
                log.info("Failed to start server for partition {}", partition.getId(), e);
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

    private RaftServer buildServer() {
        RaftServer.Builder builder = RaftServer.newBuilder(localMemberId)
                .withName("partition-" + partition.getId())
                .withProtocol(protocol.get())
                .withElectionTimeout(Duration.ofMillis(ELECTION_TIMEOUT_MILLIS))
                .withHeartbeatInterval(Duration.ofMillis(HEARTBEAT_INTERVAL_MILLIS))
                .withStorage(RaftStorage.newBuilder()
                        .withStorageLevel(StorageLevel.MAPPED)
                        .withSerializer(new AtomixSerializerAdapter(Serializer.using(StorageNamespaces.RAFT_STORAGE)))
                        .withDirectory(dataFolder)
                        .withMaxSegmentSize(MAX_SEGMENT_SIZE)
                        .build());
        StoragePartition.RAFT_SERVICES.forEach(builder::addService);
        return builder.build();
    }

    public CompletableFuture<Void> join(Collection<MemberId> otherMembers) {
        server = buildServer();
        return server.join(otherMembers).whenComplete((r, e) -> {
            if (e == null) {
                log.info("Successfully joined partition {}", partition.getId());
            } else {
                log.info("Failed to join partition {}", partition.getId(), e);
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
