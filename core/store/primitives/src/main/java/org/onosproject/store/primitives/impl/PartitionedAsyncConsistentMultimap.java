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
package org.onosproject.store.primitives.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import org.onlab.util.Match;
import org.onlab.util.Tools;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.service.AsyncConsistentMultimap;
import org.onosproject.store.service.AsyncIterator;
import org.onosproject.store.service.MultimapEventListener;
import org.onosproject.store.service.Versioned;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link AsyncConsistentMultimap} that has its entries partitioned horizontally across
 * several {@link AsyncConsistentMultimap maps}.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class PartitionedAsyncConsistentMultimap<K, V> implements AsyncConsistentMultimap<K, V> {

    private final String name;
    private final TreeMap<PartitionId, AsyncConsistentMultimap<K, V>> partitions = Maps.newTreeMap();
    private final Hasher<K> keyHasher;

    public PartitionedAsyncConsistentMultimap(String name,
        Map<PartitionId, AsyncConsistentMultimap<K, V>> partitions,
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
        return Tools.allOf(getMultimaps().stream().map(m -> m.size()).collect(Collectors.toList()),
            Math::addExact,
            0);
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return size().thenApply(size -> size == 0);
    }

    @Override
    public CompletableFuture<Boolean> containsKey(K key) {
        return getMultimap(key).containsKey(key);
    }

    @Override
    public CompletableFuture<Boolean> containsValue(V value) {
        return Tools.firstOf(getMultimaps().stream().map(m -> m.containsValue(value)).collect(Collectors.toList()),
            Match.ifValue(true),
            false);
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends V>>> get(K key) {
        return getMultimap(key).get(key);
    }

    @Override
    public CompletableFuture<Boolean> containsEntry(K key, V value) {
        return getMultimap(key).containsEntry(key, value);
    }

    @Override
    public CompletableFuture<Boolean> put(K key, V value) {
        return getMultimap(key).put(key, value);
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends V>>> putAndGet(K key, V value) {
        return getMultimap(key).putAndGet(key, value);
    }

    @Override
    public CompletableFuture<Boolean> removeAll(K key, Collection<? extends V> values) {
        return getMultimap(key).removeAll(key, values);
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends V>>> removeAll(K key) {
        return getMultimap(key).removeAll(key);
    }

    @Override
    public CompletableFuture<Boolean> putAll(K key, Collection<? extends V> values) {
        return getMultimap(key).putAll(key, values);
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends V>>> replaceValues(K key, Collection<V> values) {
        return getMultimap(key).replaceValues(key, values);
    }

    @Override
    public CompletableFuture<Map<K, Collection<V>>> asMap() {
        throw new UnsupportedOperationException("Expensive operation.");
    }

    @Override
    public CompletableFuture<Void> clear() {
        return CompletableFuture.allOf(getMultimaps().stream()
            .map(map -> map.clear())
            .toArray(CompletableFuture[]::new));
    }

    @Override
    public CompletableFuture<Set<K>> keySet() {
        return Tools.allOf(getMultimaps().stream().map(m -> m.keySet()).collect(Collectors.toList()),
            (s1, s2) -> ImmutableSet.<K>builder().addAll(s1).addAll(s2).build(),
            ImmutableSet.of());
    }

    @Override
    public CompletableFuture<Multiset<K>> keys() {
        return Tools.allOf(getMultimaps().stream().map(m -> m.keys()).collect(Collectors.toList()))
            .thenApply(results -> results.stream().reduce(Multisets::sum).orElse(HashMultiset.create()));
    }

    @Override
    public CompletableFuture<Multiset<V>> values() {
        return Tools.allOf(getMultimaps().stream().map(m -> m.values()).collect(Collectors.toList()))
            .thenApply(results -> results.stream().reduce(Multisets::sum).orElse(HashMultiset.create()));
    }

    @Override
    public CompletableFuture<Collection<Entry<K, V>>> entries() {
        return Tools.allOf(getMultimaps().stream().map(m -> m.entries()).collect(Collectors.toList()))
            .thenApply(results -> results.stream().reduce((s1, s2) -> ImmutableList.copyOf(Iterables.concat(s1, s2)))
                .orElse(ImmutableList.of()));
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, V value) {
        return getMultimap(key).remove(key, value);
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends V>>> removeAndGet(K key, V value) {
        return getMultimap(key).removeAndGet(key, value);
    }

    @Override
    public CompletableFuture<AsyncIterator<Entry<K, V>>> iterator() {
        return Tools.allOf(getMultimaps().stream().map(m -> m.iterator()).collect(Collectors.toList()))
            .thenApply(PartitionedMultimapIterator::new);
    }

    @Override
    public CompletableFuture<Void> addListener(MultimapEventListener<K, V> listener, Executor executor) {
        return CompletableFuture.allOf(getMultimaps().stream()
            .map(map -> map.addListener(listener, executor))
            .toArray(CompletableFuture[]::new));
    }

    @Override
    public CompletableFuture<Void> removeListener(MultimapEventListener<K, V> listener) {
        return CompletableFuture.allOf(getMultimaps().stream()
            .map(map -> map.removeListener(listener))
            .toArray(CompletableFuture[]::new));
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
     *
     * @param key key
     * @return AsyncConsistentMap to which key maps
     */
    private AsyncConsistentMultimap<K, V> getMultimap(K key) {
        return partitions.get(keyHasher.hash(key));
    }

    /**
     * Returns all the constituent maps.
     *
     * @return collection of maps.
     */
    private Collection<AsyncConsistentMultimap<K, V>> getMultimaps() {
        return partitions.values();
    }

    private class PartitionedMultimapIterator<K, V> implements AsyncIterator<Map.Entry<K, V>> {
        private final Iterator<AsyncIterator<Entry<K, V>>> iterators;
        private volatile AsyncIterator<Entry<K, V>> iterator;

        public PartitionedMultimapIterator(List<AsyncIterator<Entry<K, V>>> iterators) {
            this.iterators = iterators.iterator();
        }

        @Override
        public CompletableFuture<Boolean> hasNext() {
            if (iterator == null && iterators.hasNext()) {
                iterator = iterators.next();
            }
            if (iterator == null) {
                return CompletableFuture.completedFuture(false);
            }
            return iterator.hasNext()
                .thenCompose(hasNext -> {
                    if (!hasNext) {
                        iterator = null;
                        return hasNext();
                    }
                    return CompletableFuture.completedFuture(true);
                });
        }

        @Override
        public CompletableFuture<Entry<K, V>> next() {
            if (iterator == null && iterators.hasNext()) {
                iterator = iterators.next();
            }
            if (iterator == null) {
                return Tools.exceptionalFuture(new NoSuchElementException());
            }
            return iterator.next();
        }
    }
}
