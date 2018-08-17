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

import io.atomix.core.Atomix;
import io.atomix.primitive.partition.PartitionGroup;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.persistence.PersistenceService;
import org.onosproject.store.atomix.impl.AtomixManager;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
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
import org.onosproject.store.service.CoordinationService;
import org.onosproject.store.service.DistributedSetBuilder;
import org.onosproject.store.service.DocumentTreeBuilder;
import org.onosproject.store.service.EventuallyConsistentMapBuilder;
import org.onosproject.store.service.LeaderElectorBuilder;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Topic;
import org.onosproject.store.service.TopicBuilder;
import org.onosproject.store.service.TransactionContextBuilder;
import org.onosproject.store.service.WorkQueue;
import org.onosproject.store.service.WorkQueueBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.STORAGE_WRITE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of {@code CoordinationService} that uses the Atomix management partition group.
 */
@Component(immediate = true, service = CoordinationService.class)
public class CoordinationManager implements CoordinationService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PersistenceService persistenceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected AtomixManager atomixManager;

    private Atomix atomix;
    private PartitionGroup group;

    @Activate
    public void activate() {
        atomix = atomixManager.getAtomix();
        group = atomix.getPartitionService().getSystemPartitionGroup();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public <K, V> EventuallyConsistentMapBuilder<K, V> eventuallyConsistentMapBuilder() {
        checkPermission(STORAGE_WRITE);
        final NodeId localNodeId = clusterService.getLocalNode().id();

        Supplier<List<NodeId>> peersSupplier = () -> clusterService.getNodes().stream()
            .map(ControllerNode::id)
            .filter(nodeId -> !nodeId.equals(localNodeId))
            .filter(id -> clusterService.getState(id).isActive())
            .collect(Collectors.toList());

        Supplier<List<NodeId>> bootstrapPeersSupplier = () -> clusterService.getNodes()
            .stream()
            .map(ControllerNode::id)
            .filter(id -> !localNodeId.equals(id))
            .filter(id -> clusterService.getState(id).isActive())
            .collect(Collectors.toList());

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
        return new AtomixDocumentTreeBuilder<>(atomix, group.name());
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
        return this.<K, V>consistentMultimapBuilder()
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
}
