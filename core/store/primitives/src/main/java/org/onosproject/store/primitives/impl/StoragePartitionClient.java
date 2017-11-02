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

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.base.Suppliers;
import io.atomix.protocols.raft.RaftClient;
import io.atomix.protocols.raft.ReadConsistency;
import io.atomix.protocols.raft.cluster.MemberId;
import io.atomix.protocols.raft.protocol.RaftClientProtocol;
import io.atomix.protocols.raft.proxy.CommunicationStrategy;
import io.atomix.protocols.raft.session.RaftSessionMetadata;
import org.onlab.util.HexString;
import org.onosproject.store.primitives.DistributedPrimitiveCreator;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMap;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMap;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimap;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMap;
import org.onosproject.store.primitives.resources.impl.AtomixCounter;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTree;
import org.onosproject.store.primitives.resources.impl.AtomixIdGenerator;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElector;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueue;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncAtomicCounter;
import org.onosproject.store.service.AsyncAtomicCounterMap;
import org.onosproject.store.service.AsyncAtomicIdGenerator;
import org.onosproject.store.service.AsyncAtomicValue;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.AsyncConsistentMultimap;
import org.onosproject.store.service.AsyncConsistentTreeMap;
import org.onosproject.store.service.AsyncDistributedSet;
import org.onosproject.store.service.AsyncDocumentTree;
import org.onosproject.store.service.AsyncLeaderElector;
import org.onosproject.store.service.DistributedPrimitive;
import org.onosproject.store.service.Ordering;
import org.onosproject.store.service.PartitionClientInfo;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.WorkQueue;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * StoragePartition client.
 */
public class StoragePartitionClient implements DistributedPrimitiveCreator, Managed<StoragePartitionClient> {

    private final Logger log = getLogger(getClass());

    private final StoragePartition partition;
    private final MemberId localMemberId;
    private final RaftClientProtocol protocol;
    private RaftClient client;
    private static final String ATOMIC_VALUES_CONSISTENT_MAP_NAME = "onos-atomic-values";
    private final com.google.common.base.Supplier<AsyncConsistentMap<String, byte[]>> onosAtomicValuesMap =
            Suppliers.memoize(() -> newAsyncConsistentMap(ATOMIC_VALUES_CONSISTENT_MAP_NAME,
                                                          Serializer.using(KryoNamespaces.BASIC)));

    public StoragePartitionClient(StoragePartition partition, MemberId localMemberId, RaftClientProtocol protocol) {
        this.partition = partition;
        this.localMemberId = localMemberId;
        this.protocol = protocol;
    }

    @Override
    public CompletableFuture<Void> open() {
        synchronized (StoragePartitionClient.this) {
            client = newRaftClient(protocol);
        }
        return client.connect(partition.getMemberIds()).whenComplete((r, e) -> {
            if (e == null) {
                log.info("Successfully started client for partition {}", partition.getId());
            } else {
                log.info("Failed to start client for partition {}", partition.getId(), e);
            }
        }).thenApply(v -> null);
    }

    @Override
    public CompletableFuture<Void> close() {
        return client != null ? client.close() : CompletableFuture.completedFuture(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> AsyncConsistentMap<K, V> newAsyncConsistentMap(String name, Serializer serializer) {
        AtomixConsistentMap rawMap =
                new AtomixConsistentMap(client.newProxyBuilder()
                        .withName(name)
                        .withServiceType(DistributedPrimitive.Type.CONSISTENT_MAP.name())
                        .withReadConsistency(ReadConsistency.SEQUENTIAL)
                        .withCommunicationStrategy(CommunicationStrategy.ANY)
                        .withTimeout(Duration.ofSeconds(30))
                        .withMaxRetries(5)
                        .build()
                        .open()
                        .join());

        if (serializer != null) {
            return DistributedPrimitives.newTranscodingMap(rawMap,
                    key -> HexString.toHexString(serializer.encode(key)),
                    string -> serializer.decode(HexString.fromHexString(string)),
                    value -> value == null ? null : serializer.encode(value),
                    bytes -> serializer.decode(bytes));
        }
        return (AsyncConsistentMap<K, V>) rawMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> AsyncConsistentTreeMap<V> newAsyncConsistentTreeMap(String name, Serializer serializer) {
        AtomixConsistentTreeMap rawMap =
                new AtomixConsistentTreeMap(client.newProxyBuilder()
                        .withName(name)
                        .withServiceType(DistributedPrimitive.Type.CONSISTENT_TREEMAP.name())
                        .withReadConsistency(ReadConsistency.SEQUENTIAL)
                        .withCommunicationStrategy(CommunicationStrategy.ANY)
                        .withTimeout(Duration.ofSeconds(30))
                        .withMaxRetries(5)
                        .build()
                        .open()
                        .join());

        if (serializer != null) {
            return DistributedPrimitives.newTranscodingTreeMap(
                            rawMap,
                            value -> value == null ? null : serializer.encode(value),
                            bytes -> serializer.decode(bytes));
        }
        return (AsyncConsistentTreeMap<V>) rawMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> AsyncConsistentMultimap<K, V> newAsyncConsistentSetMultimap(String name, Serializer serializer) {
        AtomixConsistentSetMultimap rawMap =
                new AtomixConsistentSetMultimap(client.newProxyBuilder()
                        .withName(name)
                        .withServiceType(DistributedPrimitive.Type.CONSISTENT_MULTIMAP.name())
                        .withReadConsistency(ReadConsistency.SEQUENTIAL)
                        .withCommunicationStrategy(CommunicationStrategy.ANY)
                        .withTimeout(Duration.ofSeconds(30))
                        .withMaxRetries(5)
                        .build()
                        .open()
                        .join());

        if (serializer != null) {
            return DistributedPrimitives.newTranscodingMultimap(
                            rawMap,
                            key -> HexString.toHexString(serializer.encode(key)),
                            string -> serializer.decode(HexString.fromHexString(string)),
                            value -> serializer.encode(value),
                            bytes -> serializer.decode(bytes));
        }
        return (AsyncConsistentMultimap<K, V>) rawMap;
    }

    @Override
    public <E> AsyncDistributedSet<E> newAsyncDistributedSet(String name, Serializer serializer) {
        return DistributedPrimitives.newSetFromMap(newAsyncConsistentMap(name, serializer));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K> AsyncAtomicCounterMap<K> newAsyncAtomicCounterMap(String name, Serializer serializer) {
        AtomixAtomicCounterMap rawMap = new AtomixAtomicCounterMap(client.newProxyBuilder()
                .withName(name)
                .withServiceType(DistributedPrimitive.Type.COUNTER_MAP.name())
                .withReadConsistency(ReadConsistency.LINEARIZABLE_LEASE)
                .withCommunicationStrategy(CommunicationStrategy.LEADER)
                .withTimeout(Duration.ofSeconds(30))
                .withMaxRetries(5)
                .build()
                .open()
                .join());

        if (serializer != null) {
            return DistributedPrimitives.newTranscodingAtomicCounterMap(
                            rawMap,
                            key -> HexString.toHexString(serializer.encode(key)),
                            string -> serializer.decode(HexString.fromHexString(string)));
        }
        return (AsyncAtomicCounterMap<K>) rawMap;
    }

    @Override
    public AsyncAtomicCounter newAsyncCounter(String name) {
        return new AtomixCounter(client.newProxyBuilder()
                .withName(name)
                .withServiceType(DistributedPrimitive.Type.COUNTER.name())
                .withReadConsistency(ReadConsistency.LINEARIZABLE_LEASE)
                .withCommunicationStrategy(CommunicationStrategy.LEADER)
                .withTimeout(Duration.ofSeconds(30))
                .withMaxRetries(5)
                .build()
                .open()
                .join());
    }

    @Override
    public AsyncAtomicIdGenerator newAsyncIdGenerator(String name) {
        return new AtomixIdGenerator(newAsyncCounter(name));
    }

    @Override
    public <V> AsyncAtomicValue<V> newAsyncAtomicValue(String name, Serializer serializer) {
        return new DefaultAsyncAtomicValue<>(name, serializer, onosAtomicValuesMap.get());
    }

    @Override
    public <E> WorkQueue<E> newWorkQueue(String name, Serializer serializer) {
        AtomixWorkQueue atomixWorkQueue = new AtomixWorkQueue(client.newProxyBuilder()
                .withName(name)
                .withServiceType(DistributedPrimitive.Type.WORK_QUEUE.name())
                .withReadConsistency(ReadConsistency.LINEARIZABLE_LEASE)
                .withCommunicationStrategy(CommunicationStrategy.LEADER)
                .withTimeout(Duration.ofSeconds(5))
                .withMaxRetries(5)
                .build()
                .open()
                .join());
        return new DefaultDistributedWorkQueue<>(atomixWorkQueue, serializer);
    }

    @Override
    public <V> AsyncDocumentTree<V> newAsyncDocumentTree(String name, Serializer serializer, Ordering ordering) {
        AtomixDocumentTree atomixDocumentTree = new AtomixDocumentTree(client.newProxyBuilder()
                .withName(name)
                .withServiceType(String.format("%s-%s", DistributedPrimitive.Type.DOCUMENT_TREE.name(), ordering))
                .withReadConsistency(ReadConsistency.SEQUENTIAL)
                .withCommunicationStrategy(CommunicationStrategy.ANY)
                .withTimeout(Duration.ofSeconds(30))
                .withMaxRetries(5)
                .build()
                .open()
                .join());
        return new DefaultDistributedDocumentTree<>(name, atomixDocumentTree, serializer);
    }

    @Override
    public AsyncLeaderElector newAsyncLeaderElector(String name, long leaderTimeout, TimeUnit timeUnit) {
        AtomixLeaderElector leaderElector = new AtomixLeaderElector(client.newProxyBuilder()
                .withName(name)
                .withServiceType(DistributedPrimitive.Type.LEADER_ELECTOR.name())
                .withReadConsistency(ReadConsistency.LINEARIZABLE)
                .withCommunicationStrategy(CommunicationStrategy.LEADER)
                .withMinTimeout(Duration.ofMillis(timeUnit.toMillis(leaderTimeout)))
                .withMaxTimeout(Duration.ofSeconds(5))
                .withMaxRetries(5)
                .build()
                .open()
                .join());
        leaderElector.setupCache().join();
        return leaderElector;
    }

    @Override
    public Set<String> getAsyncConsistentMapNames() {
        return client.metadata().getSessions(DistributedPrimitive.Type.CONSISTENT_MAP.name())
                .join()
                .stream()
                .map(RaftSessionMetadata::serviceName)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getAsyncAtomicCounterNames() {
        return client.metadata().getSessions(DistributedPrimitive.Type.COUNTER.name())
                .join()
                .stream()
                .map(RaftSessionMetadata::serviceName)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getWorkQueueNames() {
        return client.metadata().getSessions(DistributedPrimitive.Type.WORK_QUEUE.name())
                .join()
                .stream()
                .map(RaftSessionMetadata::serviceName)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isOpen() {
        return client != null;
    }

    /**
     * Returns the {@link PartitionClientInfo information} for this client.
     * @return partition client information
     */
    public PartitionClientInfo clientInfo() {
        return new PartitionClientInfo(partition.getId(), partition.getMembers());
    }

    private RaftClient newRaftClient(RaftClientProtocol protocol) {
        return RaftClient.newBuilder()
                .withClientId("partition-" + partition.getId())
                .withMemberId(localMemberId)
                .withProtocol(protocol)
                .build();
    }
}
