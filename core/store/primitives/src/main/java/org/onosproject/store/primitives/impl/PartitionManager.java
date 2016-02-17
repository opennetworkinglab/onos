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

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterMetadataService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.PartitionId;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.store.cluster.messaging.MessagingService;
import org.onosproject.store.primitives.DistributedPrimitiveCreator;
import org.onosproject.store.primitives.PartitionAdminService;
import org.onosproject.store.primitives.PartitionEvent;
import org.onosproject.store.primitives.PartitionEventListener;
import org.onosproject.store.primitives.PartitionService;
import org.onosproject.store.service.PartitionInfo;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

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

    Map<PartitionId, StoragePartition> partitions = Maps.newConcurrentMap();

    @Activate
    public void activate() {
        eventDispatcher.addSink(PartitionEvent.class, listenerRegistry);

        metadataService.getClusterMetadata()
                       .getPartitions()
                       .stream()
                       .forEach(partition -> partitions.put(partition.getId(), new StoragePartition(partition,
                               messagingService,
                               clusterService,
                               CatalystSerializers.getSerializer(),
                               new File(System.getProperty("karaf.data") + "/data/" + partition.getId()))));

        CompletableFuture<Void> openFuture = CompletableFuture.allOf(partitions.values()
                                                                               .stream()
                                                                               .map(StoragePartition::open)
                                                                               .toArray(CompletableFuture[]::new));
        openFuture.join();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(PartitionEvent.class);

        CompletableFuture<Void> closeFuture = CompletableFuture.allOf(partitions.values()
                                                                                .stream()
                                                                                .map(StoragePartition::close)
                                                                                .toArray(CompletableFuture[]::new));
        closeFuture.join();
        log.info("Stopped");
    }

    @Override
    public CompletableFuture<Void> leave(PartitionId partitionId) {
        // TODO: Implement
        return Tools.exceptionalFuture(new UnsupportedOperationException());
    }

    @Override
    public CompletableFuture<Void> join(PartitionId partitionId) {
        // TODO: Implement
        return Tools.exceptionalFuture(new UnsupportedOperationException());
    }

    @Override
    public int getNumberOfPartitions() {
        return partitions.size();
    }

    @Override
    public Set<PartitionId> getAllPartitionIds() {
        return partitions.keySet();
    }

    @Override
    public DistributedPrimitiveCreator getDistributedPrimitiveCreator(PartitionId partitionId) {
        return partitions.get(partitionId).client();
    }

    @Override
    public Set<NodeId> getConfiguredMembers(PartitionId partitionId) {
        StoragePartition partition = partitions.get(partitionId);
        return ImmutableSet.copyOf(partition.getMembers());
    }

    @Override
    public Set<NodeId> getActiveMembersMembers(PartitionId partitionId) {
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
}
