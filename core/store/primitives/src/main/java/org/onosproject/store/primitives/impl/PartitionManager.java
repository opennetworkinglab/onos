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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterMetadata;
import org.onosproject.cluster.ClusterMetadataDiff;
import org.onosproject.cluster.ClusterMetadataEvent;
import org.onosproject.cluster.ClusterMetadataEventListener;
import org.onosproject.cluster.ClusterMetadataService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.DefaultPartition;
import org.onosproject.cluster.Member;
import org.onosproject.cluster.MembershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.Partition;
import org.onosproject.cluster.PartitionDiff;
import org.onosproject.cluster.PartitionId;
import org.onosproject.core.Version;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.primitives.DistributedPrimitiveCreator;
import org.onosproject.store.primitives.PartitionAdminService;
import org.onosproject.store.primitives.PartitionEvent;
import org.onosproject.store.primitives.PartitionEventListener;
import org.onosproject.store.primitives.PartitionService;
import org.onosproject.store.service.PartitionClientInfo;
import org.onosproject.store.service.PartitionInfo;
import org.onosproject.upgrade.Upgrade;
import org.onosproject.upgrade.UpgradeEvent;
import org.onosproject.upgrade.UpgradeEventListener;
import org.onosproject.upgrade.UpgradeService;
import org.slf4j.Logger;

import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.PARTITION_READ;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of {@code PartitionService} and {@code PartitionAdminService}.
 */
@Component
@Service
public class PartitionManager extends AbstractListenerManager<PartitionEvent, PartitionEventListener>
    implements PartitionService, PartitionAdminService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterMetadataService metadataService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MembershipService membershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected UpgradeService upgradeService;

    private final Map<PartitionId, StoragePartition> inactivePartitions = Maps.newConcurrentMap();
    private final Map<PartitionId, StoragePartition> activePartitions = Maps.newConcurrentMap();
    private final AtomicReference<ClusterMetadata> currentClusterMetadata = new AtomicReference<>();

    private final ClusterEventListener clusterListener = new InternalClusterEventListener();
    private final UpgradeEventListener upgradeListener = new InternalUpgradeEventListener();
    private final ClusterMetadataEventListener metadataListener = new InternalClusterMetadataListener();

    @Activate
    public void activate() {
        eventDispatcher.addSink(PartitionEvent.class, listenerRegistry);
        currentClusterMetadata.set(metadataService.getClusterMetadata());

        clusterService.addListener(clusterListener);
        upgradeService.addListener(upgradeListener);
        metadataService.addListener(metadataListener);

        // If an upgrade is currently in progress and this node is an upgraded node, initialize upgrade partitions.
        CompletableFuture<Void> openFuture;
        if (upgradeService.isUpgrading() && upgradeService.isLocalUpgraded()) {
            currentClusterMetadata.get()
                    .getPartitions()
                    .forEach(partition -> {
                        // Create a default partition and assign it to inactive partitions. This node will join
                        // inactive partitions to participate in consensus for fault tolerance, but the partitions
                        // won't be accessible via client proxies.
                        inactivePartitions.put(partition.getId(), new InactiveStoragePartition(
                                partition,
                                clusterCommunicator,
                                clusterService));

                        // Create a forked partition and assign it to active partitions. These partitions will be
                        // forked from commit logs for previous version partitions.
                        Partition forkedPartition = computeInitialPartition(
                                partition,
                                upgradeService.getState().target(),
                                getLocalNodes());
                        activePartitions.put(partition.getId(), new ForkedStoragePartition(
                                forkedPartition,
                                partition,
                                clusterCommunicator,
                                clusterService));
                    });

            // We have to fork existing partitions before we can start inactive partition servers to
            // avoid duplicate message handlers when both servers are running.
            openFuture = CompletableFuture.allOf(activePartitions.values().stream()
                    .map(StoragePartition::open)
                    .toArray(CompletableFuture[]::new))
                    .thenCompose(v -> CompletableFuture.allOf(inactivePartitions.values().stream()
                            .map(StoragePartition::open)
                            .toArray(CompletableFuture[]::new)));
        } else {
            currentClusterMetadata.get()
                    .getPartitions()
                    .forEach(partition -> activePartitions.put(partition.getId(), new ActiveStoragePartition(
                            partition,
                            clusterCommunicator,
                            clusterService)));
            openFuture = CompletableFuture.allOf(activePartitions.values().stream()
                    .map(StoragePartition::open)
                    .toArray(CompletableFuture[]::new));
        }

        openFuture.join();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        clusterService.removeListener(clusterListener);
        upgradeService.removeListener(upgradeListener);
        metadataService.removeListener(metadataListener);
        eventDispatcher.removeSink(PartitionEvent.class);

        CompletableFuture<Void> closeFuture = CompletableFuture.allOf(
                CompletableFuture.allOf(inactivePartitions.values().stream()
                        .map(StoragePartition::close)
                        .toArray(CompletableFuture[]::new)),
                CompletableFuture.allOf(activePartitions.values().stream()
                        .map(StoragePartition::close)
                        .toArray(CompletableFuture[]::new)));
        closeFuture.join();
        log.info("Stopped");
    }

    @Override
    public int getNumberOfPartitions() {
        checkPermission(PARTITION_READ);
        return activePartitions.size();
    }

    @Override
    public Set<PartitionId> getAllPartitionIds() {
        checkPermission(PARTITION_READ);
        return activePartitions.keySet();
    }

    @Override
    public DistributedPrimitiveCreator getDistributedPrimitiveCreator(PartitionId partitionId) {
        checkPermission(PARTITION_READ);
        return activePartitions.get(partitionId).client();
    }

    @Override
    public Set<NodeId> getConfiguredMembers(PartitionId partitionId) {
        checkPermission(PARTITION_READ);
        StoragePartition partition = activePartitions.get(partitionId);
        return ImmutableSet.copyOf(partition.getMembers());
    }

    @Override
    public Set<NodeId> getActiveMembersMembers(PartitionId partitionId) {
        checkPermission(PARTITION_READ);
        // TODO: This needs to query metadata to determine currently active
        // members of partition
        return getConfiguredMembers(partitionId);
    }

    @Override
    public List<PartitionInfo> partitionInfo() {
        return activePartitions.values()
                         .stream()
                         .flatMap(x -> Tools.stream(x.info()))
                         .collect(Collectors.toList());
    }

    /**
     * Returns a list of nodes sorted by time ordered oldest to newest.
     *
     * @return a list of nodes sorted by time
     */
    private List<NodeId> getLocalNodes() {
        return membershipService.getLocalGroup()
                .members()
                .stream()
                .map(Member::nodeId)
                .collect(Collectors.toList());
    }

    /**
     * Computes an initial forked partition from the given source partition.
     *
     * @param sourcePartition the source partition from which to compute the partition
     * @param targetVersion the target partition version
     * @param members the set of members available to the partition
     * @return the computed forked partition
     */
    protected static Partition computeInitialPartition(
            Partition sourcePartition,
            Version targetVersion,
            List<NodeId> members) {
        return computePartition(sourcePartition, targetVersion, members, 1);
    }

    /**
     * Computes a final forked partition from the given source partition.
     *
     * @param sourcePartition the source partition from which to compute the partition
     * @param targetVersion the target partition version
     * @param members the set of members available to the partition
     * @return the computed forked partition
     */
    protected static Partition computeFinalPartition(
            Partition sourcePartition,
            Version targetVersion,
            List<NodeId> members) {
        return computePartition(sourcePartition, targetVersion, members, 0);
    }

    /**
     * Computes a forked partition from the given source partition.
     *
     * @param sourcePartition the source partition from which to compute the partition
     * @param targetVersion the target partition version
     * @param members the set of members available to the partition
     * @param delta the number of additional members to preserve outside the partition
     * @return the computed forked partition
     */
    private static Partition computePartition(
            Partition sourcePartition,
            Version targetVersion,
            List<NodeId> members,
            int delta) {
        // Create a collection of members of the forked/isolated partition. Initial membership
        // will include up to n upgraded nodes until all n nodes in the partition have been upgraded.
        List<NodeId> sortedMembers = members.stream()
                .sorted()
                .collect(Collectors.toList());

        // Create a list of members of the partition that have been upgraded according to the
        // version isolated cluster membership.
        List<NodeId> partitionMembers = sortedMembers.stream()
                .filter(nodeId -> sourcePartition.getMembers().contains(nodeId))
                .collect(Collectors.toList());

        // If additional members need to be added to the partition to make up a full member list,
        // add members in sorted order to create deterministic rebalancing of nodes.
        int totalMembers = sourcePartition.getMembers().size() + delta;
        if (partitionMembers.size() < totalMembers) {
            for (int i = partitionMembers.size(); i < totalMembers; i++) {
                Optional<NodeId> nextMember = sortedMembers.stream()
                        .filter(nodeId -> !partitionMembers.contains(nodeId))
                        .findFirst();
                if (nextMember.isPresent()) {
                    partitionMembers.add(nextMember.get());
                } else {
                    break;
                }
            }
        }

        return new DefaultPartition(
                sourcePartition.getId(),
                targetVersion,
                partitionMembers);
    }

    private void processInstanceReady(NodeId nodeId) {
        if (upgradeService.isUpgrading() && upgradeService.isLocalUpgraded()) {
            currentClusterMetadata.get()
                    .getPartitions()
                    .forEach(partition -> {
                        StoragePartition activePartition = activePartitions.get(partition.getId());
                        if (activePartition != null) {
                            Partition newPartition = computeFinalPartition(
                                    partition,
                                    upgradeService.getState().target(),
                                    getLocalNodes());
                            log.info("Updating storage partition {}: {}", partition, newPartition);
                            activePartition.onUpdate(newPartition);
                        }
                    });
        }
    }

    private void processUpgradeComplete(Upgrade upgrade) {
        if (!inactivePartitions.isEmpty()) {
            List<CompletableFuture<Void>> futures = inactivePartitions.values()
                    .stream()
                    .map(StoragePartition::delete)
                    .collect(Collectors.toList());
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).thenRun(() -> {
                try {
                    Files.delete(new File(InactiveStoragePartition.INACTIVE_DIR).toPath());
                } catch (IOException e) {
                    log.error("Failed to delete partition archive");
                }
            });
            inactivePartitions.clear();
        }
    }

    private void processMetadataUpdate(ClusterMetadata clusterMetadata) {
        ClusterMetadataDiff diffExaminer =
                new ClusterMetadataDiff(currentClusterMetadata.get(), clusterMetadata);
        diffExaminer.partitionDiffs()
                    .values()
                    .stream()
                    .filter(PartitionDiff::hasChanged)
                    .forEach(diff -> activePartitions.get(diff.partitionId()).onUpdate(diff.newValue()));
        currentClusterMetadata.set(clusterMetadata);
    }

    private class InternalClusterEventListener implements ClusterEventListener {
        @Override
        public void event(ClusterEvent event) {
            if (event.type() == ClusterEvent.Type.INSTANCE_READY) {
                processInstanceReady(event.subject().id());
            }
        }
    }

    private class InternalUpgradeEventListener implements UpgradeEventListener {
        @Override
        public void event(UpgradeEvent event) {
            if (event.type() == UpgradeEvent.Type.COMMITTED) {
                processUpgradeComplete(event.subject());
            }
        }
    }

    private class InternalClusterMetadataListener implements ClusterMetadataEventListener {
        @Override
        public void event(ClusterMetadataEvent event) {
            processMetadataUpdate(event.subject());
        }
    }

    @Override
    public List<PartitionClientInfo> partitionClientInfo() {
        return activePartitions.values()
                         .stream()
                         .map(StoragePartition::client)
                         .map(StoragePartitionClient::clientInfo)
                         .collect(Collectors.toList());
    }
}
