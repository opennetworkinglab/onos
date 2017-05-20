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

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.primitives.DistributedPrimitiveCreator;
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
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.WorkQueue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@code DistributedPrimitiveCreator} that federates responsibility for creating
 * distributed primitives to a collection of other {@link DistributedPrimitiveCreator creators}.
 */
public class FederatedDistributedPrimitiveCreator implements DistributedPrimitiveCreator {
    private final TreeMap<PartitionId, DistributedPrimitiveCreator> members;
    private final List<PartitionId> sortedMemberPartitionIds;

    public FederatedDistributedPrimitiveCreator(Map<PartitionId, DistributedPrimitiveCreator> members) {
        this.members = Maps.newTreeMap();
        this.members.putAll(checkNotNull(members));
        this.sortedMemberPartitionIds = Lists.newArrayList(members.keySet());
    }

    @Override
    public <K, V> AsyncConsistentMap<K, V> newAsyncConsistentMap(
            String name, Serializer serializer, Supplier<Executor> executorSupplier) {
        checkNotNull(name);
        checkNotNull(serializer);
        Map<PartitionId, AsyncConsistentMap<K, V>> maps =
                Maps.transformValues(members,
                                     partition -> partition.newAsyncConsistentMap(name, serializer, executorSupplier));
        Hasher<K> hasher = key -> {
            int hashCode = Hashing.sha256().hashBytes(serializer.encode(key)).asInt();
            return sortedMemberPartitionIds.get(Math.abs(hashCode) % members.size());
        };
        return new PartitionedAsyncConsistentMap<>(name, maps, hasher);
    }

    @Override
    public <V> AsyncConsistentTreeMap<V> newAsyncConsistentTreeMap(
            String name, Serializer serializer, Supplier<Executor> executorSupplier) {
        return getCreator(name).newAsyncConsistentTreeMap(name, serializer, executorSupplier);
    }

    @Override
    public <K, V> AsyncConsistentMultimap<K, V> newAsyncConsistentSetMultimap(
            String name, Serializer serializer, Supplier<Executor> executorSupplier) {
        return getCreator(name).newAsyncConsistentSetMultimap(name, serializer, executorSupplier);
    }

    @Override
    public <E> AsyncDistributedSet<E> newAsyncDistributedSet(
            String name, Serializer serializer, Supplier<Executor> executorSupplier) {
        return DistributedPrimitives.newSetFromMap(newAsyncConsistentMap(name, serializer, executorSupplier));
    }

    @Override
    public <K> AsyncAtomicCounterMap<K> newAsyncAtomicCounterMap(
            String name, Serializer serializer, Supplier<Executor> executorSupplier) {
        return getCreator(name).newAsyncAtomicCounterMap(name, serializer, executorSupplier);
    }

    @Override
    public AsyncAtomicCounter newAsyncCounter(String name, Supplier<Executor> executorSupplier) {
        return getCreator(name).newAsyncCounter(name, executorSupplier);
    }

    @Override
    public AsyncAtomicIdGenerator newAsyncIdGenerator(String name, Supplier<Executor> executorSupplier) {
        return getCreator(name).newAsyncIdGenerator(name, executorSupplier);
    }

    @Override
    public <V> AsyncAtomicValue<V> newAsyncAtomicValue(
            String name, Serializer serializer, Supplier<Executor> executorSupplier) {
        return getCreator(name).newAsyncAtomicValue(name, serializer, executorSupplier);
    }

    @Override
    public AsyncLeaderElector newAsyncLeaderElector(String name, Supplier<Executor> executorSupplier) {
        checkNotNull(name);
        Map<PartitionId, AsyncLeaderElector> leaderElectors =
                Maps.transformValues(members,
                                     partition -> partition.newAsyncLeaderElector(name, executorSupplier));
        Hasher<String> hasher = topic -> {
            int hashCode = Hashing.sha256().hashString(topic, Charsets.UTF_8).asInt();
            return sortedMemberPartitionIds.get(Math.abs(hashCode) % members.size());
        };
        return new PartitionedAsyncLeaderElector(name, leaderElectors, hasher);
    }

    @Override
    public <E> WorkQueue<E> newWorkQueue(String name, Serializer serializer, Supplier<Executor> executorSupplier) {
        return getCreator(name).newWorkQueue(name, serializer, executorSupplier);
    }

    @Override
    public <V> AsyncDocumentTree<V> newAsyncDocumentTree(
            String name, Serializer serializer, Supplier<Executor> executorSupplier) {
        return getCreator(name).newAsyncDocumentTree(name, serializer, executorSupplier);
    }

    @Override
    public Set<String> getAsyncConsistentMapNames() {
        return members.values()
                      .stream()
                      .map(DistributedPrimitiveCreator::getAsyncConsistentMapNames)
                      .reduce(Sets::union)
                      .orElse(ImmutableSet.of());
    }

    @Override
    public Set<String> getAsyncAtomicCounterNames() {
        return members.values()
                      .stream()
                      .map(DistributedPrimitiveCreator::getAsyncAtomicCounterNames)
                      .reduce(Sets::union)
                      .orElse(ImmutableSet.of());
    }

    @Override
    public Set<String> getWorkQueueNames() {
        return members.values()
                      .stream()
                      .map(DistributedPrimitiveCreator::getWorkQueueNames)
                      .reduce(Sets::union)
                      .orElse(ImmutableSet.of());
    }

    /**
     * Returns the {@code DistributedPrimitiveCreator} to use for hosting a primitive.
     * @param name primitive name
     * @return primitive creator
     */
    private DistributedPrimitiveCreator getCreator(String name) {
        int hashCode = Hashing.sha256().hashString(name, Charsets.UTF_8).asInt();
        return members.get(sortedMemberPartitionIds.get(Math.abs(hashCode) % members.size()));
    }
}
