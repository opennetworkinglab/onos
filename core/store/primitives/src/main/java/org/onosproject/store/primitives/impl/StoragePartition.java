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

import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.transport.Address;
import io.atomix.resource.ResourceType;

import java.io.File;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.Partition;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.cluster.messaging.MessagingService;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMap;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElector;
import org.onosproject.store.service.PartitionInfo;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;

/**
 * Storage partition.
 */
public class StoragePartition implements Managed<StoragePartition> {

    private final AtomicBoolean isOpened = new AtomicBoolean(false);
    private final Serializer serializer;
    private final MessagingService messagingService;
    private final ClusterService clusterService;
    private final File logFolder;
    private Partition partition;
    private NodeId localNodeId;
    private StoragePartitionServer server;
    private StoragePartitionClient client;

    public static final Collection<ResourceType> RESOURCE_TYPES = ImmutableSet.of(
                                                                    new ResourceType(AtomixLeaderElector.class),
                                                                    new ResourceType(AtomixConsistentMap.class));

    public StoragePartition(Partition partition,
            MessagingService messagingService,
            ClusterService clusterService,
            Serializer serializer,
            File logFolder) {
        this.partition = partition;
        this.messagingService = messagingService;
        this.clusterService = clusterService;
        this.localNodeId = clusterService.getLocalNode().id();
        this.serializer = serializer;
        this.logFolder = logFolder;
    }

    /**
     * Returns the partition client instance.
     * @return client
     */
    public StoragePartitionClient client() {
        return client;
    }

    @Override
    public CompletableFuture<Void> open() {
        if (partition.getMembers().contains(localNodeId)) {
            openServer();
        }
        return openClient().thenAccept(v -> isOpened.set(true))
                           .thenApply(v -> null);
    }

    @Override
    public CompletableFuture<Void> close() {
        // We do not explicitly close the server and instead let the cluster
        // deal with this as an unclean exit.
        return closeClient();
    }

    /**
     * Returns the identifier of the {@link Partition partition} associated with this instance.
     * @return partition identifier
     */
    public PartitionId getId() {
        return partition.getId();
    }

    /**
     * Returns the identifiers of partition members.
     * @return partition member instance ids
     */
    public Collection<NodeId> getMembers() {
        return partition.getMembers();
    }

    /**
     * Returns the {@link Address addresses} of partition members.
     * @return partition member addresses
     */
    public Collection<Address> getMemberAddresses() {
        return Collections2.transform(partition.getMembers(), this::toAddress);
    }

    /**
     * Attempts to rejoin the partition.
     * @return future that is completed after the operation is complete
     */
    private CompletableFuture<Void> openServer() {
        if (!partition.getMembers().contains(localNodeId) || server != null) {
            return CompletableFuture.completedFuture(null);
        }
        StoragePartitionServer server = new StoragePartitionServer(toAddress(localNodeId),
                this,
                serializer,
                () -> new CopycatTransport(CopycatTransport.Mode.SERVER,
                                     partition.getId(),
                                     messagingService),
                logFolder);
        return server.open().thenRun(() -> this.server = server);
    }

    /**
     * Attempts to join the partition as a new member.
     * @return future that is completed after the operation is complete
     */
    private CompletableFuture<Void> joinCluster() {
        Set<NodeId> otherMembers = partition.getMembers()
                 .stream()
                 .filter(nodeId -> !nodeId.equals(localNodeId))
                 .collect(Collectors.toSet());
        StoragePartitionServer server = new StoragePartitionServer(toAddress(localNodeId),
                this,
                serializer,
                () -> new CopycatTransport(CopycatTransport.Mode.SERVER,
                                     partition.getId(),
                                     messagingService),
                logFolder);
        return server.join(Collections2.transform(otherMembers, this::toAddress)).thenRun(() -> this.server = server);
    }

    private CompletableFuture<StoragePartitionClient> openClient() {
        client = new StoragePartitionClient(this,
                serializer,
                new CopycatTransport(CopycatTransport.Mode.CLIENT,
                                     partition.getId(),
                                     messagingService));
        return client.open().thenApply(v -> client);
    }

    /**
     * Closes the partition server if it was previously opened.
     * @return future that is completed when the operation completes
     */
    public CompletableFuture<Void> leaveCluster() {
        return server != null ? server.closeAndExit() : CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean isOpen() {
        return isOpened.get();
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

    /**
     * Returns the partition information if this partition is locally managed i.e.
     * this node is a active member of the partition.
     * @return partition info
     */
    public Optional<PartitionInfo> info() {
        return server != null && server.isOpen() ? Optional.of(server.info()) : Optional.empty();
    }

    /**
     * Process updates to partitions and handles joining or leaving a partition.
     * @param newValue new Partition
     */
    public void onUpdate(Partition newValue) {

        boolean wasPresent = partition.getMembers().contains(localNodeId);
        boolean isPresent = newValue.getMembers().contains(localNodeId);
        this.partition = newValue;
        if ((wasPresent && isPresent) || (!wasPresent && !isPresent)) {
            // no action needed
            return;
        }
        //only need to do action if our membership changed
        if (wasPresent) {
            leaveCluster();
        } else if (isPresent) {
            joinCluster();
        }
    }
}
