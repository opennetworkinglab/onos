/*
 * Copyright 2016 Open Networking Laboratory
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

import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.transport.Address;
import io.atomix.resource.ResourceType;
import io.atomix.variables.DistributedLong;

import java.io.File;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultPartition;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.Partition;
import org.onosproject.store.cluster.messaging.MessagingService;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMap;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;

/**
 * Storage partition.
 */
public class StoragePartition extends DefaultPartition implements Managed<StoragePartition> {

    private final AtomicBoolean isOpened = new AtomicBoolean(false);
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Serializer serializer;
    private final MessagingService messagingService;
    private final ClusterService clusterService;
    private final File logFolder;
    private static final Collection<ResourceType> RESOURCE_TYPES = ImmutableSet.of(
                                                        new ResourceType(DistributedLong.class),
                                                        new ResourceType(AtomixConsistentMap.class));

    private NodeId localNodeId;
    private Optional<StoragePartitionServer> server = Optional.empty();
    private StoragePartitionClient client;

    public StoragePartition(Partition partition,
            MessagingService messagingService,
            ClusterService clusterService,
            Serializer serializer,
            File logFolder) {
        super(partition);
        this.messagingService = messagingService;
        this.clusterService = clusterService;
        this.localNodeId = clusterService.getLocalNode().id();
        this.serializer = serializer;
        this.logFolder = logFolder;
    }

    public StoragePartitionClient client() {
        return client;
    }

    @Override
    public CompletableFuture<Void> open() {
        return openServer().thenAccept(s -> server = Optional.of(s))
                           .thenCompose(v-> openClient())
                           .thenAccept(v -> isOpened.set(true))
                           .thenApply(v -> null);
    }

    @Override
    public CompletableFuture<Void> close() {
        return closeClient().thenCompose(v -> closeServer())
                            .thenAccept(v -> isClosed.set(true))
                            .thenApply(v -> null);
    }

    public Collection<Address> getMemberAddresses() {
        return Collections2.transform(getMembers(), this::toAddress);
    }

    private CompletableFuture<StoragePartitionServer> openServer() {
        if (!getMembers().contains(localNodeId)) {
            return CompletableFuture.completedFuture(null);
        }
        StoragePartitionServer server = new StoragePartitionServer(toAddress(localNodeId),
                this,
                serializer,
                () -> new CopycatTransport(CopycatTransport.Mode.SERVER,
                                     getId(),
                                     messagingService),
                RESOURCE_TYPES,
                logFolder);
        return server.open().thenApply(v -> server);
    }

    private CompletableFuture<StoragePartitionClient> openClient() {
        client = new StoragePartitionClient(this,
                serializer,
                new CopycatTransport(CopycatTransport.Mode.CLIENT,
                                     getId(),
                                     messagingService),
                RESOURCE_TYPES);
        return client.open().thenApply(v -> client);
    }

    private CompletableFuture<Void> closeServer() {
        if (server.isPresent()) {
            return server.get().close();
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    private CompletableFuture<Void> closeClient() {
        if (client != null) {
            return client.close();
        }
        return CompletableFuture.completedFuture(null);
    }

    private Address toAddress(NodeId nodeId) {
        ControllerNode node = clusterService.getNode(nodeId);
        return new Address(node.ip().toString(), node.tcpPort());
    }

    @Override
    public boolean isOpen() {
        return !isClosed.get() && isOpened.get();
    }

    @Override
    public boolean isClosed() {
        return isOpened.get() && isClosed.get();
    }
}
