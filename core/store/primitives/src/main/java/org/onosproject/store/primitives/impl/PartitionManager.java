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

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.store.cluster.messaging.MessagingService;
import org.onosproject.store.primitives.DistributedPrimitiveCreator;
import org.onosproject.store.primitives.PartitionAdminService;
import org.onosproject.store.primitives.PartitionEvent;
import org.onosproject.store.primitives.PartitionEventListener;
import org.onosproject.store.primitives.PartitionService;
import org.onosproject.store.service.PartitionClientInfo;
import org.onosproject.store.service.PartitionInfo;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.PARTITION_READ;

/**
 * Implementation of {@code PartitionService} and {@code PartitionAdminService}.
 */
@Component
@Service
public class PartitionManager extends AbstractListenerManager<PartitionEvent, PartitionEventListener>
    implements PartitionService, PartitionAdminService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MessagingService messagingService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterMetadataService metadataService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    private final Map<PartitionId, StoragePartition> partitions = Maps.newConcurrentMap();
    private final AtomicReference<ClusterMetadata> currentClusterMetadata = new AtomicReference<>();
    private final InternalClusterMetadataListener metadataListener = new InternalClusterMetadataListener();
    private final ExecutorService sharedPrimitiveExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            groupedThreads("onos/primitives", "primitive-events", log));

    @Activate
    public void activate() {
        eventDispatcher.addSink(PartitionEvent.class, listenerRegistry);
        currentClusterMetadata.set(metadataService.getClusterMetadata());
        metadataService.addListener(metadataListener);
        currentClusterMetadata.get()
                       .getPartitions()
                       .forEach(partition -> partitions.put(partition.getId(), new StoragePartition(partition,
                               messagingService,
                               clusterService,
                               CatalystSerializers.getSerializer(),
                               sharedPrimitiveExecutor,
                               new File(System.getProperty("karaf.data") + "/partitions/" + partition.getId()))));

        CompletableFuture<Void> openFuture = CompletableFuture.allOf(partitions.values()
                                                                               .stream()
                                                                               .map(StoragePartition::open)
                                                                               .toArray(CompletableFuture[]::new));
        openFuture.join();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        metadataService.removeListener(metadataListener);
        eventDispatcher.removeSink(PartitionEvent.class);

        CompletableFuture<Void> closeFuture = CompletableFuture.allOf(partitions.values()
                                                                                .stream()
                                                                                .map(StoragePartition::close)
                                                                                .toArray(CompletableFuture[]::new));
        closeFuture.join();
        log.info("Stopped");
    }

    @Override
    public int getNumberOfPartitions() {
        checkPermission(PARTITION_READ);
        return partitions.size();
    }

    @Override
    public Set<PartitionId> getAllPartitionIds() {
        checkPermission(PARTITION_READ);
        return partitions.keySet();
    }

    @Override
    public DistributedPrimitiveCreator getDistributedPrimitiveCreator(PartitionId partitionId) {
        checkPermission(PARTITION_READ);
        return partitions.get(partitionId).client();
    }

    @Override
    public Set<NodeId> getConfiguredMembers(PartitionId partitionId) {
        checkPermission(PARTITION_READ);
        StoragePartition partition = partitions.get(partitionId);
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
        return partitions.values()
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
                    .forEach(diff -> partitions.get(diff.partitionId()).onUpdate(diff.newValue()));
    }

    private class InternalClusterMetadataListener implements ClusterMetadataEventListener {
        @Override
        public void event(ClusterMetadataEvent event) {
            processMetadataUpdate(event.subject());
        }
    }

    @Override
    public List<PartitionClientInfo> partitionClientInfo() {
        return partitions.values()
                         .stream()
                         .map(StoragePartition::client)
                         .map(StoragePartitionClient::clientInfo)
                         .collect(Collectors.toList());
    }
}
