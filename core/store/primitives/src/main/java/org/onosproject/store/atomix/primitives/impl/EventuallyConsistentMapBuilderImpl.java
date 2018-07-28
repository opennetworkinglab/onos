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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.NodeId;
import org.onosproject.persistence.PersistenceService;
import org.onosproject.store.Timestamp;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapBuilder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Eventually consistent map builder.
 */
public class EventuallyConsistentMapBuilderImpl<K, V>
        implements EventuallyConsistentMapBuilder<K, V> {
    private final ClusterCommunicationService clusterCommunicator;

    private NodeId localNodeId;
    private String name;
    private KryoNamespace serializer;
    private KryoNamespace.Builder serializerBuilder;
    private ExecutorService eventExecutor;
    private ExecutorService communicationExecutor;
    private ScheduledExecutorService backgroundExecutor;
    private BiFunction<K, V, Timestamp> timestampProvider;
    private BiFunction<K, V, Collection<NodeId>> peerUpdateFunction;
    private boolean tombstonesDisabled = false;
    private long antiEntropyPeriod = 5;
    private TimeUnit antiEntropyTimeUnit = TimeUnit.SECONDS;
    private boolean convergeFaster = false;
    private boolean persistent = false;
    private boolean persistentMap = false;
    private final PersistenceService persistenceService;
    private Supplier<List<NodeId>> peersSupplier;
    private Supplier<List<NodeId>> bootstrapPeersSupplier;

    /**
     * Creates a new eventually consistent map builder.
     * @param localNodeId               local node id
     * @param clusterCommunicator       cluster communication service
     * @param persistenceService        persistence service
     * @param peersSupplier             supplier for peers
     * @param bootstrapPeersSupplier    supplier for peers for bootstrap
     */
    public EventuallyConsistentMapBuilderImpl(
            NodeId localNodeId,
            ClusterCommunicationService clusterCommunicator,
            PersistenceService persistenceService,
            Supplier<List<NodeId>> peersSupplier,
            Supplier<List<NodeId>> bootstrapPeersSupplier
    ) {
        this.localNodeId = localNodeId;
        this.persistenceService = persistenceService;
        this.clusterCommunicator = checkNotNull(clusterCommunicator);
        this.peersSupplier = peersSupplier;
        this.bootstrapPeersSupplier = bootstrapPeersSupplier;
    }

    @Override
    public EventuallyConsistentMapBuilder<K, V> withName(String name) {
        this.name = checkNotNull(name);
        return this;
    }

    @Override
    public EventuallyConsistentMapBuilder<K, V> withSerializer(
            KryoNamespace.Builder serializerBuilder) {
        this.serializerBuilder = checkNotNull(serializerBuilder);
        return this;
    }

    @Override
    public EventuallyConsistentMapBuilder<K, V> withSerializer(KryoNamespace serializer) {
        this.serializer = checkNotNull(serializer);
        return this;
    }

    @Override
    public EventuallyConsistentMapBuilder<K, V> withTimestampProvider(
            BiFunction<K, V, Timestamp> timestampProvider) {
        this.timestampProvider = checkNotNull(timestampProvider);
        return this;
    }

    @Override
    public EventuallyConsistentMapBuilder<K, V> withEventExecutor(ExecutorService executor) {
        this.eventExecutor = checkNotNull(executor);
        return this;
    }

    @Override
    public EventuallyConsistentMapBuilder<K, V> withCommunicationExecutor(
            ExecutorService executor) {
        communicationExecutor = checkNotNull(executor);
        return this;
    }

    @Override
    public EventuallyConsistentMapBuilder<K, V> withBackgroundExecutor(ScheduledExecutorService executor) {
        this.backgroundExecutor = checkNotNull(executor);
        return this;
    }

    @Override
    public EventuallyConsistentMapBuilder<K, V> withPeerUpdateFunction(
            BiFunction<K, V, Collection<NodeId>> peerUpdateFunction) {
        this.peerUpdateFunction = checkNotNull(peerUpdateFunction);
        return this;
    }

    @Override
    public EventuallyConsistentMapBuilder<K, V> withTombstonesDisabled() {
        tombstonesDisabled = true;
        return this;
    }

    @Override
    public EventuallyConsistentMapBuilder<K, V> withAntiEntropyPeriod(long period, TimeUnit unit) {
        checkArgument(period > 0, "anti-entropy period must be greater than 0");
        antiEntropyPeriod = period;
        antiEntropyTimeUnit = checkNotNull(unit);
        return this;
    }

    @Override
    public EventuallyConsistentMapBuilder<K, V> withFasterConvergence() {
        convergeFaster = true;
        return this;
    }

    @Override
    public EventuallyConsistentMapBuilder<K, V> withPersistence() {
        checkNotNull(this.persistenceService);
        persistent = true;
        return this;
    }

    @Override
    public EventuallyConsistentMap<K, V> build() {
        checkNotNull(name, "name is a mandatory parameter");
        checkNotNull(timestampProvider, "timestampProvider is a mandatory parameter");
        if (serializer == null && serializerBuilder != null) {
            serializer = serializerBuilder.build(name);
        }
        checkNotNull(serializer, "serializer is a mandatory parameter");
        checkNotNull(localNodeId, "local node id cannot be null");

        return new EventuallyConsistentMapImpl<>(
                localNodeId,
                name,
                clusterCommunicator,
                serializer,
                timestampProvider,
                peerUpdateFunction,
                eventExecutor,
                communicationExecutor,
                backgroundExecutor,
                tombstonesDisabled,
                antiEntropyPeriod,
                antiEntropyTimeUnit,
                convergeFaster,
                persistent,
                persistenceService,
                peersSupplier,
                bootstrapPeersSupplier
        );
    }
}
