/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.store.atomix.primitives.impl;

import com.google.common.collect.Maps;
import io.atomix.core.Atomix;
import io.atomix.core.counter.AtomicCounter;
import io.atomix.core.counter.AtomicCounterType;
import io.atomix.core.map.AtomicMapType;
import io.atomix.core.workqueue.WorkQueueType;
import io.atomix.primitive.partition.PartitionGroup;
import io.atomix.protocols.raft.MultiRaftProtocol;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.Member;
import org.onosproject.cluster.MembershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.persistence.PersistenceService;
import org.onosproject.store.atomix.impl.AtomixManager;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.primitives.PartitionAdminService;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncConsistentMultimap;
import org.onosproject.store.service.AsyncConsistentTreeMap;
import org.onosproject.store.service.AsyncDocumentTree;
import org.onosproject.store.service.AtomicCounterBuilder;
import org.onosproject.store.service.AtomicCounterMapBuilder;
import org.onosproject.store.service.AtomicIdGeneratorBuilder;
import org.onosproject.store.service.AtomicValueBuilder;
import org.onosproject.store.service.ConsistentMapBuilder;
import org.onosproject.store.service.ConsistentMultimapBuilder;
import org.onosproject.store.service.ConsistentTreeMapBuilder;
import org.onosproject.store.service.DistributedLockBuilder;
import org.onosproject.store.service.DistributedSetBuilder;
import org.onosproject.store.service.DocumentTreeBuilder;
import org.onosproject.store.service.EventuallyConsistentMapBuilder;
import org.onosproject.store.service.LeaderElectorBuilder;
import org.onosproject.store.service.MapInfo;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageAdminService;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Topic;
import org.onosproject.store.service.TopicBuilder;
import org.onosproject.store.service.TransactionContextBuilder;
import org.onosproject.store.service.WorkQueue;
import org.onosproject.store.service.WorkQueueBuilder;
import org.onosproject.store.service.WorkQueueStats;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.STORAGE_WRITE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation for {@code StorageService} and {@code StorageAdminService}.
 */
@Component(immediate = true, service = { StorageService.class, StorageAdminService.class })
public class StorageManager implements StorageService, StorageAdminService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PersistenceService persistenceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PartitionAdminService partitionAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MembershipService membershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected AtomixManager atomixManager;

    private Atomix atomix;
    private PartitionGroup group;

    @Activate
    public void activate() {
        atomix = atomixManager.getAtomix();
        group = atomix.getPartitionService().getPartitionGroup(MultiRaftProtocol.TYPE);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public <K, V> EventuallyConsistentMapBuilder<K, V> eventuallyConsistentMapBuilder() {
        checkPermission(STORAGE_WRITE);

        // Note: NPE in the usage of ClusterService/MembershipService prevents rebooting the Karaf container.
        // We need to reference these services outside the following peer suppliers.
        final MembershipService membershipService = this.membershipService;
        final ClusterService clusterService = this.clusterService;

        final NodeId localNodeId = clusterService.getLocalNode().id();

        // Use the MembershipService to provide peers for the map that are isolated within the current version.
        Supplier<List<NodeId>> peersSupplier = () -> membershipService.getMembers().stream()
            .map(Member::nodeId)
            .filter(nodeId -> !nodeId.equals(localNodeId))
            .filter(id -> clusterService.getState(id).isActive())
            .collect(Collectors.toList());

        // If this is the first node in its version, bootstrap from the previous version. Otherwise, bootstrap the
        // map from members isolated within the current version.
        Supplier<List<NodeId>> bootstrapPeersSupplier = () -> {
            if (membershipService.getMembers().size() == 1) {
                return clusterService.getNodes()
                    .stream()
                    .map(ControllerNode::id)
                    .filter(id -> !localNodeId.equals(id))
                    .filter(id -> clusterService.getState(id).isActive())
                    .collect(Collectors.toList());
            } else {
                return membershipService.getMembers()
                    .stream()
                    .map(Member::nodeId)
                    .filter(id -> !localNodeId.equals(id))
                    .filter(id -> clusterService.getState(id).isActive())
                    .collect(Collectors.toList());
            }
        };

        return new EventuallyConsistentMapBuilderImpl<>(
            localNodeId,
            clusterCommunicator,
            persistenceService,
            peersSupplier,
            bootstrapPeersSupplier
        );
    }

    @Override
    public <K, V> ConsistentMapBuilder<K, V> consistentMapBuilder() {
        checkPermission(STORAGE_WRITE);
        return new AtomixConsistentMapBuilder<>(atomix, group.name());
    }

    @Override
    public <V> DocumentTreeBuilder<V> documentTreeBuilder() {
        checkPermission(STORAGE_WRITE);
        return new AtomixDocumentTreeBuilder<V>(atomix, group.name());
    }

    @Override
    public <V> ConsistentTreeMapBuilder<V> consistentTreeMapBuilder() {
        return new AtomixConsistentTreeMapBuilder<>(atomix, group.name());
    }

    @Override
    public <K, V> ConsistentMultimapBuilder<K, V> consistentMultimapBuilder() {
        checkPermission(STORAGE_WRITE);
        return new AtomixConsistentMultimapBuilder<>(atomix, group.name());
    }

    @Override
    public <K> AtomicCounterMapBuilder<K> atomicCounterMapBuilder() {
        checkPermission(STORAGE_WRITE);
        return new AtomixAtomicCounterMapBuilder<>(atomix, group.name());
    }

    @Override
    public <E> DistributedSetBuilder<E> setBuilder() {
        checkPermission(STORAGE_WRITE);
        return new AtomixDistributedSetBuilder<>(atomix, group.name());
    }

    @Override
    public AtomicCounterBuilder atomicCounterBuilder() {
        checkPermission(STORAGE_WRITE);
        return new AtomixAtomicCounterBuilder(atomix, group.name());
    }

    @Override
    public AtomicIdGeneratorBuilder atomicIdGeneratorBuilder() {
        checkPermission(STORAGE_WRITE);
        return new AtomixAtomicIdGeneratorBuilder(atomix, group.name());
    }

    @Override
    public <V> AtomicValueBuilder<V> atomicValueBuilder() {
        checkPermission(STORAGE_WRITE);
        return new AtomixAtomicValueBuilder<>(atomix, group.name());
    }

    @Override
    public TransactionContextBuilder transactionContextBuilder() {
        checkPermission(STORAGE_WRITE);
        return new AtomixTransactionContextBuilder(atomix, group.name());
    }

    @Override
    public DistributedLockBuilder lockBuilder() {
        checkPermission(STORAGE_WRITE);
        return new AtomixDistributedLockBuilder(atomix, group.name());
    }

    @Override
    public LeaderElectorBuilder leaderElectorBuilder() {
        checkPermission(STORAGE_WRITE);
        return new AtomixLeaderElectorBuilder(atomix, group.name(), clusterService.getLocalNode().id());
    }

    @Override
    public <T> TopicBuilder<T> topicBuilder() {
        checkPermission(STORAGE_WRITE);
        return new AtomixDistributedTopicBuilder<>(atomix, group.name());
    }

    @Override
    public <E> WorkQueueBuilder<E> workQueueBuilder() {
        checkPermission(STORAGE_WRITE);
        return new AtomixWorkQueueBuilder<>(atomix, group.name());
    }

    @Override
    public <E> WorkQueue<E> getWorkQueue(String name, Serializer serializer) {
        checkPermission(STORAGE_WRITE);
        return this.<E>workQueueBuilder()
            .withName(name)
            .withSerializer(serializer)
            .build();
    }

    @Override
    public <V> AsyncDocumentTree<V> getDocumentTree(String name, Serializer serializer) {
        checkPermission(STORAGE_WRITE);
        return this.<V>documentTreeBuilder()
            .withName(name)
            .withSerializer(serializer)
            .build();
    }

    @Override
    public <K, V> AsyncConsistentMultimap<K, V> getAsyncSetMultimap(String name, Serializer serializer) {
        checkPermission(STORAGE_WRITE);
        return new AtomixConsistentMultimapBuilder<K, V>(atomix, group.name())
            .withName(name)
            .withSerializer(serializer)
            .buildMultimap();
    }

    @Override
    public <V> AsyncConsistentTreeMap<V> getAsyncTreeMap(String name, Serializer serializer) {
        checkPermission(STORAGE_WRITE);
        return this.<V>consistentTreeMapBuilder()
            .withName(name)
            .withSerializer(serializer)
            .buildTreeMap();
    }

    @Override
    public <T> Topic<T> getTopic(String name, Serializer serializer) {
        checkPermission(STORAGE_WRITE);
        return this.<T>topicBuilder()
            .withName(name)
            .withSerializer(serializer)
            .build();
    }

    @Override
    public List<MapInfo> getMapInfo() {
        Serializer serializer = Serializer.using(KryoNamespaces.BASIC);
        return atomix.getPrimitives(AtomicMapType.instance())
            .stream()
            .map(info -> {
                io.atomix.core.map.AtomicMap<String, byte[]> map =
                    atomix.<String, byte[]>atomicMapBuilder(info.name())
                        .withSerializer(new AtomixSerializerAdapter(serializer))
                        .build();
                int size = map.size();
                map.close();
                return new MapInfo(info.name(), size);
            }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> getCounters() {
        return atomix.getPrimitives(AtomicCounterType.instance())
            .stream()
            .map(info -> {
                AtomicCounter counter = atomix.atomicCounterBuilder(info.name()).build();
                long value = counter.get();
                counter.close();
                return Maps.immutableEntry(info.name(), value);
            }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    @Override
    public Map<String, WorkQueueStats> getQueueStats() {
        Serializer serializer = Serializer.using(KryoNamespaces.BASIC);
        return atomix.getPrimitives(WorkQueueType.instance())
            .stream()
            .map(info -> {
                io.atomix.core.workqueue.WorkQueue queue = atomix.workQueueBuilder(info.name())
                    .withSerializer(new AtomixSerializerAdapter(serializer))
                    .build();
                io.atomix.core.workqueue.WorkQueueStats stats = queue.stats();
                return Maps.immutableEntry(info.name(), WorkQueueStats.builder()
                    .withTotalCompleted(stats.totalCompleted())
                    .withTotalInProgress(stats.totalInProgress())
                    .withTotalPending(stats.totalPending())
                    .build());
            }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    @Override
    public Collection<TransactionId> getPendingTransactions() {
        return atomix.getTransactionService().getActiveTransactions()
            .stream()
            .map(transactionId -> TransactionId.from(transactionId.id()))
            .collect(Collectors.toList());
    }
}
