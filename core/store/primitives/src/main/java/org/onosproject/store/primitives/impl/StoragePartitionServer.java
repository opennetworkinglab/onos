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
package org.onosproject.store.primitives.impl;

import static org.slf4j.LoggerFactory.getLogger;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.Transport;
import io.atomix.copycat.server.CopycatServer;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.copycat.server.storage.StorageLevel;
import io.atomix.manager.internal.ResourceManagerState;
import io.atomix.manager.util.ResourceManagerTypeResolver;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.onosproject.store.service.PartitionInfo;
import org.slf4j.Logger;

/**
 * {@link StoragePartition} server.
 */
public class StoragePartitionServer implements Managed<StoragePartitionServer> {

    private final Logger log = getLogger(getClass());

    private static final int MAX_ENTRIES_PER_LOG_SEGMENT = 32768;
    private final StoragePartition partition;
    private final Address localAddress;
    private final Supplier<Transport> transport;
    private final Serializer serializer;
    private final File dataFolder;
    private CopycatServer server;

    public StoragePartitionServer(Address localAddress,
            StoragePartition partition,
            Serializer serializer,
            Supplier<Transport> transport,
            File dataFolder) {
        this.partition = partition;
        this.localAddress = localAddress;
        this.serializer = serializer;
        this.transport = transport;
        this.dataFolder = dataFolder;
    }

    @Override
    public CompletableFuture<Void> open() {
        CompletableFuture<CopycatServer> serverOpenFuture;
        if (partition.getMemberAddresses().contains(localAddress)) {
            if (server != null && server.isRunning()) {
                return CompletableFuture.completedFuture(null);
            }
            synchronized (this) {
                server = buildServer();
            }
            serverOpenFuture = server.bootstrap(partition.getMemberAddresses());
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

    private CopycatServer buildServer() {
        CopycatServer server = CopycatServer.builder(localAddress)
                .withName("partition-" + partition.getId())
                .withSerializer(serializer.clone())
                .withTransport(transport.get())
                .withStateMachine(ResourceManagerState::new)
                .withStorage(Storage.builder()
                        .withStorageLevel(StorageLevel.DISK)
                        .withCompactionThreads(1)
                        .withDirectory(dataFolder)
                        .withMaxEntriesPerSegment(MAX_ENTRIES_PER_LOG_SEGMENT)
                        .build())
                .build();
        server.serializer().resolve(new ResourceManagerTypeResolver());
        return server;
    }

    public CompletableFuture<Void> join(Collection<Address> otherMembers) {
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
                server.cluster().members(),
                server.cluster().members(),
                server.cluster().leader(),
                server.cluster().term()).toPartitionInfo();
    }
}
