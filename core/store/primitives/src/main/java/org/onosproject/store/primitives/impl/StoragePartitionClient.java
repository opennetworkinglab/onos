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
import io.atomix.Atomix;
import io.atomix.AtomixClient;
import io.atomix.catalyst.transport.Transport;
import io.atomix.resource.ResourceType;
import io.atomix.variables.DistributedLong;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.onlab.util.HexString;
import org.onosproject.store.primitives.DistributedPrimitiveCreator;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMap;
import org.onosproject.store.primitives.resources.impl.AtomixCounter;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElector;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncAtomicCounter;
import org.onosproject.store.service.AsyncAtomicValue;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.AsyncDistributedSet;
import org.onosproject.store.service.AsyncLeaderElector;
import org.onosproject.store.service.DistributedQueue;
import org.onosproject.store.service.Serializer;
import org.slf4j.Logger;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;

/**
 * StoragePartition client.
 */
public class StoragePartitionClient implements DistributedPrimitiveCreator, Managed<StoragePartitionClient> {

    private final Logger log = getLogger(getClass());

    private final StoragePartition partition;
    private final Transport transport;
    private final io.atomix.catalyst.serializer.Serializer serializer;
    private final Collection<ResourceType> resourceTypes;
    private Atomix client;
    private static final String ATOMIC_VALUES_CONSISTENT_MAP_NAME = "onos-atomic-values";
    private final Supplier<AsyncConsistentMap<String, byte[]>> onosAtomicValuesMap =
            Suppliers.memoize(() -> newAsyncConsistentMap(ATOMIC_VALUES_CONSISTENT_MAP_NAME,
                                                          Serializer.using(KryoNamespaces.BASIC)));

    public StoragePartitionClient(StoragePartition partition,
            io.atomix.catalyst.serializer.Serializer serializer,
            Transport transport,
            Collection<ResourceType> resourceTypes) {
        this.partition = partition;
        this.serializer = serializer;
        this.transport = transport;
        this.resourceTypes = ImmutableSet.copyOf(resourceTypes);
    }

    @Override
    public CompletableFuture<Void> open() {
        if (client != null && client.isOpen()) {
            return CompletableFuture.completedFuture(null);
        }
        synchronized (StoragePartitionClient.this) {
            client = AtomixClient.builder(partition.getMemberAddresses())
                                .withSerializer(serializer.clone())
                                .withResourceResolver(r -> {
                                    resourceTypes.forEach(r::register);
                                })
                                .withTransport(transport)
                                .build();
        }
        return client.open().whenComplete((r, e) -> {
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
    public <K, V> AsyncConsistentMap<K, V> newAsyncConsistentMap(String name, Serializer serializer) {
        AsyncConsistentMap<String, byte[]> rawMap =
                new DelegatingAsyncConsistentMap<String, byte[]>(client.get(name, AtomixConsistentMap.class).join()) {
                    @Override
                    public String name() {
                        return name;
                    }
                };
        AsyncConsistentMap<K, V> transcodedMap = DistributedPrimitives.<K, V, String, byte[]>newTranscodingMap(rawMap,
                        key -> HexString.toHexString(serializer.encode(key)),
                        string -> serializer.decode(HexString.fromHexString(string)),
                        value -> value == null ? null : serializer.encode(value),
                        bytes -> serializer.decode(bytes));

        return DistributedPrimitives.newCachingMap(transcodedMap);
    }

    @Override
    public <E> AsyncDistributedSet<E> newAsyncDistributedSet(String name, Serializer serializer) {
        return DistributedPrimitives.newSetFromMap(this.<E, Boolean>newAsyncConsistentMap(name, serializer));
    }

    @Override
    public AsyncAtomicCounter newAsyncCounter(String name) {
        DistributedLong distributedLong = client.get(name, DistributedLong.class).join();
        return new AtomixCounter(name, distributedLong);
    }

    @Override
    public <V> AsyncAtomicValue<V> newAsyncAtomicValue(String name, Serializer serializer) {
       return new DefaultAsyncAtomicValue<>(name,
                                        serializer,
                                        onosAtomicValuesMap.get());
    }

    @Override
    public <E> DistributedQueue<E> newDistributedQueue(String name, Serializer serializer) {
        // TODO: Implement
        throw new UnsupportedOperationException();
    }

    @Override
    public AsyncLeaderElector newAsyncLeaderElector(String name) {
        return client.get(name, AtomixLeaderElector.class).join();
    }

    @Override
    public Set<String> getAsyncConsistentMapNames() {
        return client.keys(AtomixConsistentMap.class).join();
    }

    @Override
    public Set<String> getAsyncAtomicCounterNames() {
        return client.keys(DistributedLong.class).join();
    }

    @Override
    public boolean isOpen() {
        return client.isOpen();
    }

    @Override
    public boolean isClosed() {
        return client.isClosed();
    }
}
