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
import java.util.function.BiFunction;
import java.util.function.Predicate;

import com.google.common.collect.Maps;
import io.atomix.core.collection.impl.TranscodingAsyncDistributedCollection;
import io.atomix.core.set.impl.TranscodingAsyncDistributedSet;
import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.AsyncIterator;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.TransactionLog;
import org.onosproject.store.service.Version;
import org.onosproject.store.service.Versioned;

import static org.onosproject.store.atomix.primitives.impl.AtomixFutures.adaptMapFuture;

/**
 * Atomix consistent map.
 */
public class AtomixConsistentMap<K, V> implements AsyncConsistentMap<K, V> {
    private final io.atomix.core.map.AsyncAtomicMap<K, V> atomixMap;
    private final Map<MapEventListener<K, V>, io.atomix.core.map.AtomicMapEventListener<K, V>> listenerMap =
        Maps.newIdentityHashMap();

    public AtomixConsistentMap(io.atomix.core.map.AsyncAtomicMap<K, V> atomixMap) {
        this.atomixMap = atomixMap;
    }

    @Override
    public String name() {
        return atomixMap.name();
    }

    @Override
    public CompletableFuture<Integer> size() {
        return atomixMap.size();
    }

    @Override
    public CompletableFuture<Boolean> containsKey(K key) {
        return atomixMap.containsKey(key);
    }

    @Override
    public CompletableFuture<Boolean> containsValue(V value) {
        return atomixMap.containsValue(value);
    }

    @Override
    public CompletableFuture<Versioned<V>> get(K key) {
        return atomixMap.get(key).thenApply(this::toVersioned);
    }

    @Override
    public CompletableFuture<Versioned<V>> getOrDefault(K key, V defaultValue) {
        return atomixMap.getOrDefault(key, defaultValue).thenApply(this::toVersioned);
    }

    @Override
    public CompletableFuture<Versioned<V>> computeIf(
        K key, Predicate<? super V> condition, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return adaptMapFuture(atomixMap.computeIf(key, condition, remappingFunction).thenApply(this::toVersioned));
    }

    @Override
    public CompletableFuture<Versioned<V>> put(K key, V value) {
        return adaptMapFuture(atomixMap.put(key, value).thenApply(this::toVersioned));
    }

    @Override
    public CompletableFuture<Versioned<V>> putAndGet(K key, V value) {
        return adaptMapFuture(atomixMap.putAndGet(key, value).thenApply(this::toVersioned));
    }

    @Override
    public CompletableFuture<Versioned<V>> remove(K key) {
        return adaptMapFuture(atomixMap.remove(key).thenApply(this::toVersioned));
    }

    @Override
    public CompletableFuture<Void> clear() {
        return atomixMap.clear();
    }

    @Override
    public CompletableFuture<Set<K>> keySet() {
        return CompletableFuture.completedFuture(atomixMap.keySet()
            .sync(Duration.ofMillis(DEFAULT_OPERATION_TIMEOUT_MILLIS)));
    }

    @Override
    public CompletableFuture<Collection<Versioned<V>>> values() {
        return CompletableFuture.completedFuture(
            new TranscodingAsyncDistributedCollection<Versioned<V>, io.atomix.utils.time.Versioned<V>>(
                atomixMap.values(),
                v -> new io.atomix.utils.time.Versioned<>(v.value(), v.version()),
                v -> new Versioned<>(v.value(), v.version()))
                .sync(Duration.ofMillis(DEFAULT_OPERATION_TIMEOUT_MILLIS)));
    }

    @Override
    public CompletableFuture<Set<Map.Entry<K, Versioned<V>>>> entrySet() {
        return CompletableFuture.completedFuture(
            new TranscodingAsyncDistributedSet<Map.Entry<K, Versioned<V>>,
                Map.Entry<K, io.atomix.utils.time.Versioned<V>>>(
                atomixMap.entrySet(),
                e -> Maps.immutableEntry(e.getKey(),
                    new io.atomix.utils.time.Versioned<>(e.getValue().value(), e.getValue().version())),
                e -> Maps.immutableEntry(e.getKey(), new Versioned<>(e.getValue().value(), e.getValue().version())))
                .sync(Duration.ofMillis(DEFAULT_OPERATION_TIMEOUT_MILLIS)));
    }

    @Override
    public CompletableFuture<Versioned<V>> putIfAbsent(K key, V value) {
        return adaptMapFuture(atomixMap.putIfAbsent(key, value).thenApply(this::toVersioned));
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, V value) {
        return adaptMapFuture(atomixMap.remove(key, value));
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, long version) {
        return adaptMapFuture(atomixMap.remove(key, version));
    }

    @Override
    public CompletableFuture<Versioned<V>> replace(K key, V value) {
        return adaptMapFuture(atomixMap.replace(key, value).thenApply(this::toVersioned));
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, V oldValue, V newValue) {
        return adaptMapFuture(atomixMap.replace(key, oldValue, newValue));
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, long oldVersion, V newValue) {
        return adaptMapFuture(atomixMap.replace(key, oldVersion, newValue));
    }

    @Override
    public CompletableFuture<AsyncIterator<Map.Entry<K, Versioned<V>>>> iterator() {
        io.atomix.core.iterator.AsyncIterator<Map.Entry<K, io.atomix.utils.time.Versioned<V>>> atomixIterator
            = atomixMap.entrySet().iterator();
        return CompletableFuture.completedFuture(new AsyncIterator<Map.Entry<K, Versioned<V>>>() {
            @Override
            public CompletableFuture<Boolean> hasNext() {
                return atomixIterator.hasNext();
            }

            @Override
            public CompletableFuture<Map.Entry<K, Versioned<V>>> next() {
                return atomixIterator.next()
                    .thenApply(entry -> Maps.immutableEntry(entry.getKey(), toVersioned(entry.getValue())));
            }
        });
    }

    @Override
    public synchronized CompletableFuture<Void> addListener(MapEventListener<K, V> listener, Executor executor) {
        io.atomix.core.map.AtomicMapEventListener<K, V> atomixListener = event ->
            listener.event(new MapEvent<K, V>(
                MapEvent.Type.valueOf(event.type().name()),
                name(),
                event.key(),
                toVersioned(event.newValue()),
                toVersioned(event.oldValue())));
        listenerMap.put(listener, atomixListener);
        return atomixMap.addListener(atomixListener, executor);
    }

    @Override
    public CompletableFuture<Void> removeListener(MapEventListener<K, V> listener) {
        io.atomix.core.map.AtomicMapEventListener<K, V> atomixListener = listenerMap.remove(listener);
        if (atomixListener != null) {
            return atomixMap.removeListener(atomixListener);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Version> begin(TransactionId transactionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Boolean> prepare(TransactionLog<MapUpdate<K, V>> transactionLog) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Boolean> prepareAndCommit(TransactionLog<MapUpdate<K, V>> transactionLog) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> commit(TransactionId transactionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> rollback(TransactionId transactionId) {
        throw new UnsupportedOperationException();
    }

    private Versioned<V> toVersioned(io.atomix.utils.time.Versioned<V> versioned) {
        return versioned != null
            ? new Versioned<>(versioned.value(), versioned.version(), versioned.creationTime())
            : null;
    }
}
