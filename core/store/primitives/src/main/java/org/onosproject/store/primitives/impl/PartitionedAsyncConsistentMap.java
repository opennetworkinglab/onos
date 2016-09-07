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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.onlab.util.Match;
import org.onlab.util.Tools;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.MapTransaction;
import org.onosproject.store.service.Versioned;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * {@link AsyncConsistentMap} that has its entries partitioned horizontally across
 * several {@link AsyncConsistentMap maps}.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class PartitionedAsyncConsistentMap<K, V> implements AsyncConsistentMap<K, V> {

    private final String name;
    private final TreeMap<PartitionId, AsyncConsistentMap<K, V>> partitions = Maps.newTreeMap();
    private final Hasher<K> keyHasher;

    public PartitionedAsyncConsistentMap(String name,
            Map<PartitionId, AsyncConsistentMap<K, V>> partitions,
            Hasher<K> keyHasher) {
        this.name = name;
        this.partitions.putAll(checkNotNull(partitions));
        this.keyHasher = checkNotNull(keyHasher);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public CompletableFuture<Integer> size() {
        return Tools.allOf(getMaps().stream().map(m -> m.size()).collect(Collectors.toList()),
                            Math::addExact,
                            0);
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return size().thenApply(size -> size == 0);
    }

    @Override
    public CompletableFuture<Boolean> containsKey(K key) {
        return getMap(key).containsKey(key);
    }

    @Override
    public CompletableFuture<Boolean> containsValue(V value) {
        return Tools.firstOf(getMaps().stream().map(m -> m.containsValue(value)).collect(Collectors.toList()),
                            Match.ifValue(true),
                            false);
    }
    @Override
    public CompletableFuture<Versioned<V>> get(K key) {
        return getMap(key).get(key);
    }

    @Override
    public CompletableFuture<Versioned<V>> computeIf(K key,
            Predicate<? super V> condition,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return getMap(key).computeIf(key, condition, remappingFunction);
    }

    @Override
    public CompletableFuture<Versioned<V>> put(K key, V value) {
        return getMap(key).put(key, value);
    }

    @Override
    public CompletableFuture<Versioned<V>> putAndGet(K key, V value) {
        return getMap(key).putAndGet(key, value);
    }

    @Override
    public CompletableFuture<Versioned<V>> remove(K key) {
        return getMap(key).remove(key);
    }

    @Override
    public CompletableFuture<Void> clear() {
        return CompletableFuture.allOf(getMaps().stream()
                                                .map(map -> map.clear())
                                                .toArray(CompletableFuture[]::new));
    }

    @Override
    public CompletableFuture<Set<K>> keySet() {
        return Tools.allOf(getMaps().stream().map(m -> m.keySet()).collect(Collectors.toList()),
                    (s1, s2) -> ImmutableSet.<K>builder().addAll(s1).addAll(s2).build(),
                    ImmutableSet.of());
    }

    @Override
    public CompletableFuture<Collection<Versioned<V>>> values() {
        return Tools.allOf(getMaps().stream().map(m -> m.values()).collect(Collectors.toList()),
                    (c1, c2) -> ImmutableList.<Versioned<V>>builder().addAll(c1).addAll(c2).build(),
                    ImmutableList.of());
    }

    @Override
    public CompletableFuture<Set<Entry<K, Versioned<V>>>> entrySet() {
        return Tools.allOf(getMaps().stream().map(m -> m.entrySet()).collect(Collectors.toList()),
                (s1, s2) -> ImmutableSet.<Entry<K, Versioned<V>>>builder().addAll(s1).addAll(s2).build(),
                ImmutableSet.of());
    }

    @Override
    public CompletableFuture<Versioned<V>> putIfAbsent(K key, V value) {
        return getMap(key).putIfAbsent(key, value);
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, V value) {
        return getMap(key).remove(key, value);
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, long version) {
        return getMap(key).remove(key, version);
    }

    @Override
    public CompletableFuture<Versioned<V>> replace(K key, V value) {
        return getMap(key).replace(key, value);
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, V oldValue, V newValue) {
        return getMap(key).replace(key, oldValue, newValue);
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, long oldVersion, V newValue) {
        return getMap(key).replace(key, oldVersion, newValue);
    }

    @Override
    public CompletableFuture<Void> addListener(MapEventListener<K, V> listener, Executor executor) {
        return CompletableFuture.allOf(getMaps().stream()
                                                .map(map -> map.addListener(listener, executor))
                                                .toArray(CompletableFuture[]::new));
    }

    @Override
    public CompletableFuture<Void> removeListener(MapEventListener<K, V> listener) {
        return CompletableFuture.allOf(getMaps().stream()
                                                .map(map -> map.removeListener(listener))
                                                .toArray(CompletableFuture[]::new));
    }

    @Override
    public CompletableFuture<Boolean> prepare(MapTransaction<K, V> transaction) {

        Map<AsyncConsistentMap<K, V>, List<MapUpdate<K, V>>> updatesGroupedByMap = Maps.newIdentityHashMap();
        transaction.updates().forEach(update -> {
            AsyncConsistentMap<K, V> map = getMap(update.key());
            updatesGroupedByMap.computeIfAbsent(map, k -> Lists.newLinkedList()).add(update);
        });
        Map<AsyncConsistentMap<K, V>, MapTransaction<K, V>> transactionsByMap =
                Maps.transformValues(updatesGroupedByMap,
                                     list -> new MapTransaction<>(transaction.transactionId(), list));

        return Tools.allOf(transactionsByMap.entrySet()
                         .stream()
                         .map(e -> e.getKey().prepare(e.getValue()))
                         .collect(Collectors.toList()))
                    .thenApply(list -> list.stream().reduce(Boolean::logicalAnd).orElse(true));
    }

    @Override
    public CompletableFuture<Void> commit(TransactionId transactionId) {
        return CompletableFuture.allOf(getMaps().stream()
                                                .map(e -> e.commit(transactionId))
                                                .toArray(CompletableFuture[]::new));
    }

    @Override
    public CompletableFuture<Void> rollback(TransactionId transactionId) {
        return CompletableFuture.allOf(getMaps().stream()
                .map(e -> e.rollback(transactionId))
                .toArray(CompletableFuture[]::new));
    }

    @Override
    public CompletableFuture<Boolean> prepareAndCommit(MapTransaction<K, V> transaction) {
        Map<AsyncConsistentMap<K, V>, List<MapUpdate<K, V>>> updatesGroupedByMap = Maps.newIdentityHashMap();
        transaction.updates().forEach(update -> {
            AsyncConsistentMap<K, V> map = getMap(update.key());
            updatesGroupedByMap.computeIfAbsent(map, k -> Lists.newLinkedList()).add(update);
        });
        Map<AsyncConsistentMap<K, V>, MapTransaction<K, V>> transactionsByMap =
                Maps.transformValues(updatesGroupedByMap,
                                     list -> new MapTransaction<>(transaction.transactionId(), list));

        return Tools.allOf(transactionsByMap.entrySet()
                                            .stream()
                                            .map(e -> e.getKey().prepareAndCommit(e.getValue()))
                                            .collect(Collectors.toList()))
                    .thenApply(list -> list.stream().reduce(Boolean::logicalAnd).orElse(true));
    }

    @Override
    public void addStatusChangeListener(Consumer<Status> listener) {
        partitions.values().forEach(map -> map.addStatusChangeListener(listener));
    }

    @Override
    public void removeStatusChangeListener(Consumer<Status> listener) {
        partitions.values().forEach(map -> map.removeStatusChangeListener(listener));
    }

    @Override
    public Collection<Consumer<Status>> statusChangeListeners() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the map (partition) to which the specified key maps.
     * @param key key
     * @return AsyncConsistentMap to which key maps
     */
    private AsyncConsistentMap<K, V> getMap(K key) {
        return partitions.get(keyHasher.hash(key));
    }

    /**
     * Returns all the constituent maps.
     * @return collection of maps.
     */
    private Collection<AsyncConsistentMap<K, V>> getMaps() {
        return partitions.values();
    }
}
