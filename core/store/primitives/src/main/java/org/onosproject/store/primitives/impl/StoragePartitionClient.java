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

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
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
import org.onlab.util.HexString;
import org.onlab.util.OrderedExecutor;
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
import org.onosproject.store.service.DistributedPrimitive.Status;
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
    private final Transport transport;
    private final io.atomix.catalyst.serializer.Serializer serializer;
    private final Executor sharedExecutor;
    private AtomixClient client;
    private ResourceClient resourceClient;
    private static final String ATOMIC_VALUES_CONSISTENT_MAP_NAME = "onos-atomic-values";
    private final com.google.common.base.Supplier<AsyncConsistentMap<String, byte[]>> onosAtomicValuesMap =
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
            Transport transport,
            Executor sharedExecutor) {
        this.partition = partition;
        this.serializer = serializer;
        this.transport = transport;
        this.sharedExecutor = sharedExecutor;
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

    /**
     * Returns the executor provided by the given supplier or a serial executor if the supplier is {@code null}.
     *
     * @param executorSupplier the user-provided executor supplier
     * @return the executor
     */
    private Executor defaultExecutor(Supplier<Executor> executorSupplier) {
        return executorSupplier != null ? executorSupplier.get() : new OrderedExecutor(sharedExecutor);
    }

    @Override
    public <K, V> AsyncConsistentMap<K, V> newAsyncConsistentMap(
            String name, Serializer serializer, Supplier<Executor> executorSupplier) {
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

        // We have to ensure serialization is done on the Copycat threads since Kryo is not thread safe.
        AsyncConsistentMap<K, V> transcodedMap = DistributedPrimitives.newTranscodingMap(rawMap,
            key -> HexString.toHexString(serializer.encode(key)),
            string -> serializer.decode(HexString.fromHexString(string)),
            value -> value == null ? null : serializer.encode(value),
            bytes -> serializer.decode(bytes));

        return new ExecutingAsyncConsistentMap<>(transcodedMap, defaultExecutor(executorSupplier), sharedExecutor);
    }

    @Override
    public <V> AsyncConsistentTreeMap<V> newAsyncConsistentTreeMap(
            String name, Serializer serializer, Supplier<Executor> executorSupplier) {
        AtomixConsistentTreeMap atomixConsistentTreeMap =
                client.getResource(name, AtomixConsistentTreeMap.class).join();
        Consumer<State> statusListener = state -> {
            atomixConsistentTreeMap.statusChangeListeners()
                    .forEach(listener -> listener.accept(mapper.apply(state)));
        };
        resourceClient.client().onStateChange(statusListener);

        AsyncConsistentTreeMap<byte[]> rawMap =
                new DelegatingAsyncConsistentTreeMap<byte[]>(atomixConsistentTreeMap) {
                    @Override
                    public String name() {
                        return name;
                    }
                };

        AsyncConsistentTreeMap<V> transcodedMap =
                DistributedPrimitives.<V, byte[]>newTranscodingTreeMap(
                    rawMap,
                    value -> value == null ? null : serializer.encode(value),
                    bytes -> serializer.decode(bytes));

        return new ExecutingAsyncConsistentTreeMap<>(transcodedMap, defaultExecutor(executorSupplier), sharedExecutor);
    }

    @Override
    public <K, V> AsyncConsistentMultimap<K, V> newAsyncConsistentSetMultimap(
            String name, Serializer serializer, Supplier<Executor> executorSupplier) {
        AtomixConsistentSetMultimap atomixConsistentSetMultimap =
                client.getResource(name, AtomixConsistentSetMultimap.class)
                        .join();
        Consumer<State> statusListener = state -> {
            atomixConsistentSetMultimap.statusChangeListeners()
                    .forEach(listener -> listener.accept(mapper.apply(state)));
        };
        resourceClient.client().onStateChange(statusListener);

        AsyncConsistentMultimap<String, byte[]> rawMap =
                new DelegatingAsyncConsistentMultimap<String, byte[]>(
                        atomixConsistentSetMultimap) {
                    @Override
                    public String name() {
                        return super.name();
                    }
                };

        AsyncConsistentMultimap<K, V> transcodedMap =
                DistributedPrimitives.newTranscodingMultimap(
                        rawMap,
                        key -> HexString.toHexString(serializer.encode(key)),
                        string -> serializer.decode(HexString.fromHexString(string)),
                        value -> serializer.encode(value),
                        bytes -> serializer.decode(bytes));

        return new ExecutingAsyncConsistentMultimap<>(transcodedMap, defaultExecutor(executorSupplier), sharedExecutor);
    }

    @Override
    public <E> AsyncDistributedSet<E> newAsyncDistributedSet(
            String name, Serializer serializer, Supplier<Executor> executorSupplier) {
        return DistributedPrimitives.newSetFromMap(newAsyncConsistentMap(name, serializer, executorSupplier));
    }

    @Override
    public <K> AsyncAtomicCounterMap<K> newAsyncAtomicCounterMap(
            String name, Serializer serializer, Supplier<Executor> executorSupplier) {
        AtomixAtomicCounterMap atomixAtomicCounterMap =
                client.getResource(name, AtomixAtomicCounterMap.class)
                        .join();

        AsyncAtomicCounterMap<K> transcodedMap =
                DistributedPrimitives.<K, String>newTranscodingAtomicCounterMap(
                       atomixAtomicCounterMap,
                        key -> HexString.toHexString(serializer.encode(key)),
                        string -> serializer.decode(HexString.fromHexString(string)));

        return new ExecutingAsyncAtomicCounterMap<>(transcodedMap, defaultExecutor(executorSupplier), sharedExecutor);
    }

    @Override
    public AsyncAtomicCounter newAsyncCounter(String name, Supplier<Executor> executorSupplier) {
        DistributedLong distributedLong = client.getLong(name).join();
        AsyncAtomicCounter asyncCounter = new AtomixCounter(name, distributedLong);
        return new ExecutingAsyncAtomicCounter(asyncCounter, defaultExecutor(executorSupplier), sharedExecutor);
    }

    @Override
    public AsyncAtomicIdGenerator newAsyncIdGenerator(String name, Supplier<Executor> executorSupplier) {
        DistributedLong distributedLong = client.getLong(name).join();
        AsyncAtomicIdGenerator asyncIdGenerator = new AtomixIdGenerator(name, distributedLong);
        return new ExecutingAsyncAtomicIdGenerator(asyncIdGenerator, defaultExecutor(executorSupplier), sharedExecutor);
    }

    @Override
    public <V> AsyncAtomicValue<V> newAsyncAtomicValue(
            String name, Serializer serializer, Supplier<Executor> executorSupplier) {
       AsyncAtomicValue<V> asyncValue = new DefaultAsyncAtomicValue<>(name, serializer, onosAtomicValuesMap.get());
       return new ExecutingAsyncAtomicValue<>(asyncValue, defaultExecutor(executorSupplier), sharedExecutor);
    }

    @Override
    public <E> WorkQueue<E> newWorkQueue(String name, Serializer serializer, Supplier<Executor> executorSupplier) {
        AtomixWorkQueue atomixWorkQueue = client.getResource(name, AtomixWorkQueue.class).join();
        WorkQueue<E> workQueue = new DefaultDistributedWorkQueue<>(atomixWorkQueue, serializer);
        return new ExecutingWorkQueue<>(workQueue, defaultExecutor(executorSupplier), sharedExecutor);
    }

    @Override
    public <V> AsyncDocumentTree<V> newAsyncDocumentTree(
            String name, Serializer serializer, Supplier<Executor> executorSupplier) {
        AtomixDocumentTree atomixDocumentTree = client.getResource(name, AtomixDocumentTree.class).join();
        AsyncDocumentTree<V> asyncDocumentTree = new DefaultDistributedDocumentTree<>(
                name, atomixDocumentTree, serializer);
        return new ExecutingAsyncDocumentTree<>(asyncDocumentTree, defaultExecutor(executorSupplier), sharedExecutor);
    }

    @Override
    public AsyncLeaderElector newAsyncLeaderElector(String name, Supplier<Executor> executorSupplier) {
        AtomixLeaderElector leaderElector = client.getResource(name, AtomixLeaderElector.class)
                                                  .thenCompose(AtomixLeaderElector::setupCache)
                                                  .join();
        Consumer<State> statusListener = state -> leaderElector.statusChangeListeners()
                .forEach(listener -> listener.accept(mapper.apply(state)));
        resourceClient.client().onStateChange(statusListener);
        return new ExecutingAsyncLeaderElector(leaderElector, defaultExecutor(executorSupplier), sharedExecutor);
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
    public Set<String> getWorkQueueNames() {
        return client.keys(AtomixWorkQueue.class).join();
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
