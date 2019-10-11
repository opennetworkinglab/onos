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

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import io.atomix.core.map.impl.TranscodingAsyncDistributedMap;
import org.onosproject.store.service.AsyncConsistentMultimap;
import org.onosproject.store.service.AsyncIterator;
import org.onosproject.store.service.MultimapEvent;
import org.onosproject.store.service.MultimapEventListener;
import org.onosproject.store.service.Versioned;

import static org.onosproject.store.atomix.primitives.impl.AtomixFutures.adaptMapFuture;

/**
 * Atomix consistent map.
 */
public class AtomixConsistentMultimap<K, V> implements AsyncConsistentMultimap<K, V> {
    private final io.atomix.core.multimap.AsyncAtomicMultimap<K, V> atomixMultimap;
    private final Map<MultimapEventListener<K, V>, io.atomix.core.multimap.AtomicMultimapEventListener<K, V>>
        listenerMap = Maps.newIdentityHashMap();

    public AtomixConsistentMultimap(io.atomix.core.multimap.AsyncAtomicMultimap<K, V> atomixMultimap) {
        this.atomixMultimap = atomixMultimap;
    }

    @Override
    public String name() {
        return atomixMultimap.name();
    }

    @Override
    public CompletableFuture<Integer> size() {
        return adaptMapFuture(atomixMultimap.size());
    }

    @Override
    public CompletableFuture<Boolean> containsKey(K key) {
        return adaptMapFuture(atomixMultimap.containsKey(key));
    }

    @Override
    public CompletableFuture<Boolean> containsValue(V value) {
        return adaptMapFuture(atomixMultimap.containsValue(value));
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return adaptMapFuture(atomixMultimap.isEmpty());
    }

    @Override
    public CompletableFuture<Boolean> containsEntry(K key, V value) {
        return adaptMapFuture(atomixMultimap.containsEntry(key, value));
    }

    @Override
    public CompletableFuture<Boolean> put(K key, V value) {
        return adaptMapFuture(atomixMultimap.put(key, value));
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends V>>> putAndGet(K key, V value) {
        return adaptMapFuture(atomixMultimap.put(key, value).thenCompose(v -> atomixMultimap.get(key))
            .thenApply(this::toVersioned));
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, V value) {
        return adaptMapFuture(atomixMultimap.remove(key, value));
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends V>>> removeAndGet(K key, V value) {
        return adaptMapFuture(atomixMultimap.remove(key, value).thenCompose(v -> atomixMultimap.get(key))
            .thenApply(this::toVersioned));
    }

    @Override
    public CompletableFuture<Boolean> removeAll(K key, Collection<? extends V> values) {
        return adaptMapFuture(atomixMultimap.removeAll(key, values));
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends V>>> removeAll(K key) {
        return adaptMapFuture(atomixMultimap.removeAll(key).thenApply(this::toVersioned));
    }

    @Override
    public CompletableFuture<Boolean> removeAll(Map<K, Collection<? extends V>> mapping) {
        return adaptMapFuture(atomixMultimap.removeAll(mapping));
    }

    @Override
    public CompletableFuture<Boolean> putAll(K key, Collection<? extends V> values) {
        return adaptMapFuture(atomixMultimap.putAll(key, values));
    }

    @Override
    public CompletableFuture<Boolean> putAll(Map<K, Collection<? extends V>> mapping) {
        return adaptMapFuture(atomixMultimap.putAll(mapping));
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<Collection<? extends V>>> replaceValues(K key, Collection<V> values) {
        return adaptMapFuture(atomixMultimap.replaceValues(key, values).thenApply(this::toVersioned));
    }

    @Override
    public CompletableFuture<Void> clear() {
        return adaptMapFuture(atomixMultimap.clear());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Versioned<Collection<? extends V>>> get(K key) {
        return adaptMapFuture(atomixMultimap.get(key).thenApply(this::toVersioned));
    }

    @Override
    public CompletableFuture<Set<K>> keySet() {
        return CompletableFuture.completedFuture(atomixMultimap.keySet()
            .sync(Duration.ofMillis(DEFAULT_OPERATION_TIMEOUT_MILLIS)));
    }

    @Override
    public CompletableFuture<Multiset<K>> keys() {
        return CompletableFuture.completedFuture(atomixMultimap.keys()
            .sync(Duration.ofMillis(DEFAULT_OPERATION_TIMEOUT_MILLIS)));
    }

    @Override
    public CompletableFuture<Multiset<V>> values() {
        return CompletableFuture.completedFuture(atomixMultimap.values()
            .sync(Duration.ofMillis(DEFAULT_OPERATION_TIMEOUT_MILLIS)));
    }

    @Override
    public CompletableFuture<Collection<Map.Entry<K, V>>> entries() {
        return CompletableFuture.completedFuture(atomixMultimap.entries()
            .sync(Duration.ofMillis(DEFAULT_OPERATION_TIMEOUT_MILLIS)));
    }

    @Override
    public CompletableFuture<AsyncIterator<Map.Entry<K, V>>> iterator() {
        return CompletableFuture.completedFuture(new AtomixIterator<>(atomixMultimap.entries().iterator()));
    }

    @Override
    public CompletableFuture<Map<K, Collection<V>>> asMap() {
        return CompletableFuture.completedFuture(
            new TranscodingAsyncDistributedMap<K, Collection<V>, K, io.atomix.utils.time.Versioned<Collection<V>>>(
                atomixMultimap.asMap(),
                Function.identity(),
                Function.identity(),
                v -> new io.atomix.utils.time.Versioned<>(v, 0),
                v -> v.value())
                .sync(Duration.ofMillis(DEFAULT_OPERATION_TIMEOUT_MILLIS)));
    }

    @Override
    public synchronized CompletableFuture<Void> addListener(MultimapEventListener<K, V> listener, Executor executor) {
        io.atomix.core.multimap.AtomicMultimapEventListener<K, V> atomixListener = event ->
            listener.event(new MultimapEvent<K, V>(
                name(),
                event.key(),
                event.newValue(),
                event.oldValue()));
        listenerMap.put(listener, atomixListener);
        return adaptMapFuture(atomixMultimap.addListener(atomixListener, executor));
    }

    @Override
    public CompletableFuture<Void> removeListener(MultimapEventListener<K, V> listener) {
        io.atomix.core.multimap.AtomicMultimapEventListener<K, V> atomixListener = listenerMap.remove(listener);
        if (atomixListener != null) {
            return adaptMapFuture(atomixMultimap.removeListener(atomixListener));
        }
        return CompletableFuture.completedFuture(null);
    }

    private Versioned<Collection<? extends V>> toVersioned(
        io.atomix.utils.time.Versioned<Collection<V>> versioned) {
        return versioned != null
            ? new Versioned<>(versioned.value(), versioned.version(), versioned.creationTime())
            : null;
    }
}
