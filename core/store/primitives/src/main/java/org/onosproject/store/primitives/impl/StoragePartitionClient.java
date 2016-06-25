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

import static org.slf4j.LoggerFactory.getLogger;
import io.atomix.AtomixClient;
import io.atomix.catalyst.transport.Transport;
import io.atomix.copycat.client.ConnectionStrategies;
import io.atomix.copycat.client.CopycatClient;
import io.atomix.copycat.client.CopycatClient.State;
import io.atomix.copycat.client.RecoveryStrategies;
import io.atomix.copycat.client.ServerSelectionStrategies;
import io.atomix.manager.ResourceClient;
import io.atomix.manager.ResourceManagerException;
import io.atomix.manager.util.ResourceManagerTypeResolver;
import io.atomix.resource.ResourceRegistry;
import io.atomix.resource.ResourceType;
import io.atomix.variables.DistributedLong;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

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
import org.onosproject.store.service.DistributedPrimitive.Status;
import org.onosproject.store.service.DistributedQueue;
import org.onosproject.store.service.PartitionClientInfo;
import org.onosproject.store.service.Serializer;
import org.slf4j.Logger;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 * StoragePartition client.
 */
public class StoragePartitionClient implements DistributedPrimitiveCreator, Managed<StoragePartitionClient> {

    private final Logger log = getLogger(getClass());

    private final StoragePartition partition;
    private final Transport transport;
    private final io.atomix.catalyst.serializer.Serializer serializer;
    private AtomixClient client;
    private ResourceClient resourceClient;
    private static final String ATOMIC_VALUES_CONSISTENT_MAP_NAME = "onos-atomic-values";
    private final Supplier<AsyncConsistentMap<String, byte[]>> onosAtomicValuesMap =
            Suppliers.memoize(() -> newAsyncConsistentMap(ATOMIC_VALUES_CONSISTENT_MAP_NAME,
                                                          Serializer.using(KryoNamespaces.BASIC)));
    Function<State, Status> mapper = state -> {
                                        switch (state) {
                                        case CONNECTED:
                                            return Status.ACTIVE;
                                        case SUSPENDED:
                                            return Status.SUSPENDED;
                                        case CLOSED:
                                            return Status.INACTIVE;
                                        default:
                                            throw new IllegalStateException("Unknown state " + state);
                                        }
                                    };

    public StoragePartitionClient(StoragePartition partition,
            io.atomix.catalyst.serializer.Serializer serializer,
            Transport transport) {
        this.partition = partition;
        this.serializer = serializer;
        this.transport = transport;
    }

    @Override
    public CompletableFuture<Void> open() {
        synchronized (StoragePartitionClient.this) {
            resourceClient = newResourceClient(transport,
                                             serializer.clone(),
                                             StoragePartition.RESOURCE_TYPES);
            resourceClient.client().onStateChange(state -> log.debug("Partition {} client state"
                    + " changed to {}", partition.getId(), state));
            client = new AtomixClient(resourceClient);
        }
        return client.connect(partition.getMemberAddresses()).whenComplete((r, e) -> {
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
        AtomixConsistentMap atomixConsistentMap = client.getResource(name, AtomixConsistentMap.class).join();
        Consumer<State> statusListener = state -> {
            atomixConsistentMap.statusChangeListeners()
                               .forEach(listener -> listener.accept(mapper.apply(state)));
        };
        resourceClient.client().onStateChange(statusListener);
        AsyncConsistentMap<String, byte[]> rawMap =
                new DelegatingAsyncConsistentMap<String, byte[]>(atomixConsistentMap) {
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

        return transcodedMap;
    }

    @Override
    public <E> AsyncDistributedSet<E> newAsyncDistributedSet(String name, Serializer serializer) {
        return DistributedPrimitives.newSetFromMap(this.<E, Boolean>newAsyncConsistentMap(name, serializer));
    }

    @Override
    public AsyncAtomicCounter newAsyncCounter(String name) {
        DistributedLong distributedLong = client.getLong(name).join();
        return new AtomixCounter(name, distributedLong);
    }

    @Override
    public <V> AsyncAtomicValue<V> newAsyncAtomicValue(String name, Serializer serializer) {
       return new DefaultAsyncAtomicValue<>(name, serializer, onosAtomicValuesMap.get());
    }

    @Override
    public <E> DistributedQueue<E> newDistributedQueue(String name, Serializer serializer) {
        // TODO: Implement
        throw new UnsupportedOperationException();
    }

    @Override
    public AsyncLeaderElector newAsyncLeaderElector(String name) {
        AtomixLeaderElector leaderElector = client.getResource(name, AtomixLeaderElector.class)
                                                  .thenCompose(AtomixLeaderElector::setupCache)
                                                  .join();
        Consumer<State> statusListener = state -> {
            leaderElector.statusChangeListeners()
                         .forEach(listener -> listener.accept(mapper.apply(state)));
        };
        resourceClient.client().onStateChange(statusListener);
        return leaderElector;
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
        return resourceClient.client().state() != State.CLOSED;
    }

    /**
     * Returns the {@link PartitionClientInfo information} for this client.
     * @return partition client information
     */
    public PartitionClientInfo clientInfo() {
        return new PartitionClientInfo(partition.getId(),
                partition.getMembers(),
                resourceClient.client().session().id(),
                mapper.apply(resourceClient.client().state()));
    }

    private ResourceClient newResourceClient(Transport transport,
                                           io.atomix.catalyst.serializer.Serializer serializer,
                                           Collection<ResourceType> resourceTypes) {
        ResourceRegistry registry = new ResourceRegistry();
        resourceTypes.forEach(registry::register);
        CopycatClient copycatClient = CopycatClient.builder()
                .withServerSelectionStrategy(ServerSelectionStrategies.ANY)
                .withConnectionStrategy(ConnectionStrategies.FIBONACCI_BACKOFF)
                .withRecoveryStrategy(RecoveryStrategies.RECOVER)
                .withTransport(transport)
                .withSerializer(serializer)
                .build();
        copycatClient.serializer().resolve(new ResourceManagerTypeResolver());
        for (ResourceType type : registry.types()) {
            try {
                type.factory()
                    .newInstance()
                    .createSerializableTypeResolver()
                    .resolve(copycatClient.serializer().registry());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new ResourceManagerException(e);
            }
        }
        return new ResourceClient(new OnosCopycatClient(copycatClient, 5, 100));
    }
}
