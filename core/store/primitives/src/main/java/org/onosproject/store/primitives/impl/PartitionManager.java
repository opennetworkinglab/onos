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
import java.util.List;
import java.util.Map;
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
import org.onosproject.cluster.ClusterMetadata;
import org.onosproject.cluster.ClusterMetadataDiff;
import org.onosproject.cluster.ClusterMetadataEvent;
import org.onosproject.cluster.ClusterMetadataEventListener;
import org.onosproject.cluster.ClusterMetadataService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.PartitionDiff;
import org.onosproject.cluster.PartitionId;
import org.onosproject.core.Version;
import org.onosproject.core.VersionService;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.primitives.DistributedPrimitiveCreator;
import org.onosproject.store.primitives.PartitionAdminService;
import org.onosproject.store.primitives.PartitionEvent;
import org.onosproject.store.primitives.PartitionEventListener;
import org.onosproject.store.primitives.PartitionService;
import org.onosproject.store.service.PartitionClientInfo;
import org.onosproject.store.service.PartitionInfo;
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
    protected UpgradeService upgradeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VersionService versionService;

    private final Map<PartitionId, StoragePartition> inactivePartitions = Maps.newConcurrentMap();
    private final Map<PartitionId, StoragePartition> activePartitions = Maps.newConcurrentMap();
    private final AtomicReference<ClusterMetadata> currentClusterMetadata = new AtomicReference<>();
    private final InternalClusterMetadataListener metadataListener = new InternalClusterMetadataListener();

    @Activate
    public void activate() {
        eventDispatcher.addSink(PartitionEvent.class, listenerRegistry);
        currentClusterMetadata.set(metadataService.getClusterMetadata());
        metadataService.addListener(metadataListener);

        // If an upgrade is currently in progress and this node is an upgraded node, initialize upgrade partitions.
        CompletableFuture<Void> openFuture;
        if (upgradeService.isUpgrading() && upgradeService.isLocalUpgraded()) {
            Version sourceVersion = upgradeService.getState().source();
            Version targetVersion = upgradeService.getState().target();
            currentClusterMetadata.get()
                    .getPartitions()
                    .forEach(partition -> {
                        inactivePartitions.put(partition.getId(), new StoragePartition(
                                partition,
                                sourceVersion,
                                null,
                                clusterCommunicator,
                                clusterService,
                                new File(System.getProperty("karaf.data") +
                                        "/partitions/" + sourceVersion + "/" + partition.getId())));
                        activePartitions.put(partition.getId(), new StoragePartition(
                                partition,
                                targetVersion,
                                sourceVersion,
                                clusterCommunicator,
                                clusterService,
                                new File(System.getProperty("karaf.data") +
                                        "/partitions/" + targetVersion + "/" + partition.getId())));
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
            Version version = versionService.version();
            currentClusterMetadata.get()
                    .getPartitions()
                    .forEach(partition -> activePartitions.put(partition.getId(), new StoragePartition(
                            partition,
                            version,
                            null,
                            clusterCommunicator,
                            clusterService,
                            new File(System.getProperty("karaf.data") +
                                    "/partitions/" + version + "/" + partition.getId()))));
            openFuture = CompletableFuture.allOf(activePartitions.values().stream()
                    .map(StoragePartition::open)
                    .toArray(CompletableFuture[]::new));
        }

        openFuture.join();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
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

    private void processMetadataUpdate(ClusterMetadata clusterMetadata) {
        ClusterMetadataDiff diffExaminer =
                new ClusterMetadataDiff(currentClusterMetadata.get(), clusterMetadata);
        diffExaminer.partitionDiffs()
                    .values()
                    .stream()
                    .filter(PartitionDiff::hasChanged)
                    .forEach(diff -> activePartitions.get(diff.partitionId()).onUpdate(diff.newValue()));
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
