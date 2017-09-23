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
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import io.atomix.protocols.raft.cluster.MemberId;
import io.atomix.protocols.raft.service.RaftService;
import org.onosproject.cluster.MembershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.Partition;
import org.onosproject.cluster.PartitionId;
import org.onosproject.core.Version;
import org.onosproject.store.cluster.messaging.UnifiedClusterCommunicationService;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapService;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapService;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapService;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapService;
import org.onosproject.store.primitives.resources.impl.AtomixCounterService;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeService;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorService;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueueService;
import org.onosproject.store.service.DistributedPrimitive;
import org.onosproject.store.service.Ordering;
import org.onosproject.store.service.PartitionInfo;
import org.onosproject.store.service.Serializer;

/**
 * Storage partition.
 */
public class StoragePartition implements Managed<StoragePartition> {

    private final AtomicBoolean isOpened = new AtomicBoolean(false);
    private final UnifiedClusterCommunicationService clusterCommunicator;
    private final MembershipService clusterService;
    private final Version version;
    private final Version source;
    private final File dataFolder;
    private Partition partition;
    private NodeId localNodeId;
    private StoragePartitionServer server;
    private StoragePartitionClient client;

    public static final Map<String, Supplier<RaftService>> RAFT_SERVICES =
            ImmutableMap.<String, Supplier<RaftService>>builder()
                    .put(DistributedPrimitive.Type.CONSISTENT_MAP.name(), AtomixConsistentMapService::new)
                    .put(DistributedPrimitive.Type.CONSISTENT_TREEMAP.name(), AtomixConsistentTreeMapService::new)
                    .put(DistributedPrimitive.Type.CONSISTENT_MULTIMAP.name(), AtomixConsistentSetMultimapService::new)
                    .put(DistributedPrimitive.Type.COUNTER_MAP.name(), AtomixAtomicCounterMapService::new)
                    .put(DistributedPrimitive.Type.COUNTER.name(), AtomixCounterService::new)
                    .put(DistributedPrimitive.Type.LEADER_ELECTOR.name(), AtomixLeaderElectorService::new)
                    .put(DistributedPrimitive.Type.WORK_QUEUE.name(), AtomixWorkQueueService::new)
                    .put(DistributedPrimitive.Type.DOCUMENT_TREE.name(),
                            () -> new AtomixDocumentTreeService(Ordering.NATURAL))
                    .put(String.format("%s-%s", DistributedPrimitive.Type.DOCUMENT_TREE.name(), Ordering.NATURAL),
                            () -> new AtomixDocumentTreeService(Ordering.NATURAL))
                    .put(String.format("%s-%s", DistributedPrimitive.Type.DOCUMENT_TREE.name(), Ordering.INSERTION),
                            () -> new AtomixDocumentTreeService(Ordering.INSERTION))
                    .build();

    public StoragePartition(
            Partition partition,
            Version version,
            Version source,
            UnifiedClusterCommunicationService clusterCommunicator,
            MembershipService clusterService,
            File dataFolder) {
        this.partition = partition;
        this.version = version;
        this.source = source;
        this.clusterCommunicator = clusterCommunicator;
        this.clusterService = clusterService;
        this.localNodeId = clusterService.getLocalNode().id();
        this.dataFolder = dataFolder;
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
        if (source != null) {
            return forkServer(source)
                    .thenCompose(v -> openClient())
                    .thenAccept(v -> isOpened.set(true))
                    .thenApply(v -> null);
        } else if (partition.getMembers().contains(localNodeId)) {
            return openServer()
                    .thenCompose(v -> openClient())
                    .thenAccept(v -> isOpened.set(true))
                    .thenApply(v -> null);
        }
        return openClient()
                .thenAccept(v -> isOpened.set(true))
                .thenApply(v -> null);
    }

    @Override
    public CompletableFuture<Void> close() {
        // We do not explicitly close the server and instead let the cluster
        // deal with this as an unclean exit.
        return closeClient();
    }

    /**
     * Returns the partition name.
     *
     * @return the partition name
     */
    public String getName() {
        return getName(version);
    }

    /**
     * Returns the partition name for the given version.
     *
     * @param version the version for which to return the partition name
     * @return the partition name for the given version
     */
    String getName(Version version) {
        return version != null ? String.format("partition-%d-%s", partition.getId().id(), version) : "partition-core";
    }

    /**
     * Returns the partition version.
     *
     * @return the partition version
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Returns the partition data folder.
     *
     * @return the partition data folder
     */
    public File getDataFolder() {
        return dataFolder;
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
     * Returns the {@link MemberId identifiers} of partition members.
     * @return partition member identifiers
     */
    public Collection<MemberId> getMemberIds() {
        return source != null ?
                clusterService.getNodes()
                        .stream()
                        .map(node -> MemberId.from(node.id().id()))
                        .collect(Collectors.toList()) :
                Collections2.transform(partition.getMembers(), n -> MemberId.from(n.id()));
    }

    Collection<MemberId> getMemberIds(Version version) {
        if (source == null || version.equals(source)) {
            return Collections2.transform(partition.getMembers(), n -> MemberId.from(n.id()));
        } else {
            return clusterService.getNodes()
                    .stream()
                    .map(node -> MemberId.from(node.id().id()))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Attempts to rejoin the partition.
     * @return future that is completed after the operation is complete
     */
    private CompletableFuture<Void> openServer() {
        StoragePartitionServer server = new StoragePartitionServer(
                this,
                MemberId.from(localNodeId.id()),
                clusterCommunicator);
        return server.open().thenRun(() -> this.server = server);
    }

    /**
     * Forks the server from the given version.
     *
     * @return future to be completed once the server has been forked
     */
    private CompletableFuture<Void> forkServer(Version version) {
        StoragePartitionServer server = new StoragePartitionServer(
                this,
                MemberId.from(localNodeId.id()),
                clusterCommunicator);

        CompletableFuture<Void> future;
        if (clusterService.getNodes().size() == 1) {
            future = server.fork(version);
        } else {
            future = server.join(clusterService.getNodes().stream()
                    .filter(node -> !node.id().equals(localNodeId))
                    .map(node -> MemberId.from(node.id().id()))
                    .collect(Collectors.toList()));
        }
        return future.thenRun(() -> this.server = server);
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
        StoragePartitionServer server = new StoragePartitionServer(this,
                MemberId.from(localNodeId.id()),
                clusterCommunicator);
        return server.join(Collections2.transform(otherMembers, n -> MemberId.from(n.id())))
                .thenRun(() -> this.server = server);
    }

    private CompletableFuture<StoragePartitionClient> openClient() {
        client = new StoragePartitionClient(this,
                MemberId.from(localNodeId.id()),
                new RaftClientCommunicator(
                        getName(),
                        Serializer.using(StorageNamespaces.RAFT_PROTOCOL),
                        clusterCommunicator));
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
