/*
 * Copyright 2017-present Open Networking Foundation
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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.UnifiedClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultPartition;
import org.onosproject.cluster.PartitionId;
import org.onosproject.persistence.PersistenceService;
import org.onosproject.store.cluster.messaging.UnifiedClusterCommunicationService;
import org.onosproject.store.primitives.DistributedPrimitiveCreator;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncAtomicValue;
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
import org.onosproject.store.service.TransactionContextBuilder;
import org.onosproject.store.service.WorkQueue;
import org.slf4j.Logger;

import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.STORAGE_WRITE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of {@code CoordinationService} that uses a {@link StoragePartition} that spans all the nodes
 * in the cluster regardless of version.
 */
@Service
@Component(immediate = true)
public class CoordinationManager implements CoordinationService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected UnifiedClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected UnifiedClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PersistenceService persistenceService;

    private StoragePartition partition;
    private DistributedPrimitiveCreator primitiveCreator;

    @Activate
    public void activate() {
        partition = new StoragePartition(
                new DefaultPartition(
                        PartitionId.SHARED,
                        clusterService.getNodes()
                                .stream()
                                .map(ControllerNode::id)
                                .collect(Collectors.toSet())),
                null,
                null,
                clusterCommunicator,
                clusterService,
                new File(System.getProperty("karaf.data") + "/partitions/coordination"));
        partition.open().join();
        primitiveCreator = partition.client();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public <K, V> EventuallyConsistentMapBuilder<K, V> eventuallyConsistentMapBuilder() {
        checkPermission(STORAGE_WRITE);
        return new EventuallyConsistentMapBuilderImpl<>(clusterService,
                clusterCommunicator,
                persistenceService);
    }

    @Override
    public <K, V> ConsistentMapBuilder<K, V> consistentMapBuilder() {
        checkPermission(STORAGE_WRITE);
        return new DefaultConsistentMapBuilder<>(primitiveCreator);
    }

    @Override
    public <V> DocumentTreeBuilder<V> documentTreeBuilder() {
        checkPermission(STORAGE_WRITE);
        return new DefaultDocumentTreeBuilder<>(primitiveCreator);
    }

    @Override
    public <V> ConsistentTreeMapBuilder<V> consistentTreeMapBuilder() {
        return new DefaultConsistentTreeMapBuilder<>(primitiveCreator);
    }

    @Override
    public <K, V> ConsistentMultimapBuilder<K, V> consistentMultimapBuilder() {
        checkPermission(STORAGE_WRITE);
        return new DefaultConsistentMultimapBuilder<>(primitiveCreator);
    }

    @Override
    public <K> AtomicCounterMapBuilder<K> atomicCounterMapBuilder() {
        checkPermission(STORAGE_WRITE);
        return new DefaultAtomicCounterMapBuilder<>(primitiveCreator);
    }

    @Override
    public <E> DistributedSetBuilder<E> setBuilder() {
        checkPermission(STORAGE_WRITE);
        return new DefaultDistributedSetBuilder<>(() -> this.<E, Boolean>consistentMapBuilder());
    }

    @Override
    public AtomicCounterBuilder atomicCounterBuilder() {
        checkPermission(STORAGE_WRITE);
        return new DefaultAtomicCounterBuilder(primitiveCreator);
    }

    @Override
    public AtomicIdGeneratorBuilder atomicIdGeneratorBuilder() {
        checkPermission(STORAGE_WRITE);
        return new DefaultAtomicIdGeneratorBuilder(primitiveCreator);
    }

    @Override
    public <V> AtomicValueBuilder<V> atomicValueBuilder() {
        checkPermission(STORAGE_WRITE);
        Supplier<ConsistentMapBuilder<String, byte[]>> mapBuilderSupplier =
                () -> this.<String, byte[]>consistentMapBuilder()
                          .withName("onos-atomic-values")
                          .withSerializer(Serializer.using(KryoNamespaces.BASIC));
        return new DefaultAtomicValueBuilder<>(mapBuilderSupplier);
    }

    @Override
    public TransactionContextBuilder transactionContextBuilder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LeaderElectorBuilder leaderElectorBuilder() {
        checkPermission(STORAGE_WRITE);
        return new DefaultLeaderElectorBuilder(primitiveCreator);
    }

    @Override
    public <E> WorkQueue<E> getWorkQueue(String name, Serializer serializer) {
        checkPermission(STORAGE_WRITE);
        return primitiveCreator.newWorkQueue(name, serializer);
    }

    @Override
    public <V> AsyncDocumentTree<V> getDocumentTree(String name, Serializer serializer) {
        checkPermission(STORAGE_WRITE);
        return primitiveCreator.newAsyncDocumentTree(name, serializer);
    }

    @Override
    public <K, V> AsyncConsistentMultimap<K, V> getAsyncSetMultimap(
            String name, Serializer serializer) {
        checkPermission(STORAGE_WRITE);
        return primitiveCreator.newAsyncConsistentSetMultimap(name,
                                                                serializer);
    }

    @Override
    public <V> AsyncConsistentTreeMap<V> getAsyncTreeMap(
            String name, Serializer serializer) {
        checkPermission(STORAGE_WRITE);
        return primitiveCreator.newAsyncConsistentTreeMap(name, serializer);
    }

    @Override
    public <T> Topic<T> getTopic(String name, Serializer serializer) {
        AsyncAtomicValue<T> atomicValue = this.<T>atomicValueBuilder()
                                              .withName("topic-" + name)
                                              .withSerializer(serializer)
                                              .build();
        return new DefaultDistributedTopic<>(atomicValue);
    }
}
