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
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import io.atomix.core.collection.impl.TranscodingAsyncDistributedCollection;
import io.atomix.core.map.impl.DelegatingAsyncDistributedNavigableMap;
import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.service.AsyncConsistentTreeMap;
import org.onosproject.store.service.AsyncIterator;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.TransactionLog;
import org.onosproject.store.service.Version;
import org.onosproject.store.service.Versioned;

import static org.onosproject.store.atomix.primitives.impl.AtomixFutures.adaptMapFuture;

/**
 * Atomix consistent tree map.
 */
public class AtomixConsistentTreeMap<V> implements AsyncConsistentTreeMap<V> {
    private final io.atomix.core.map.AsyncAtomicNavigableMap<String, V> atomixTreeMap;
    private final Map<MapEventListener<String, V>, io.atomix.core.map.AtomicMapEventListener<String, V>> listenerMap =
        Maps.newIdentityHashMap();

    public AtomixConsistentTreeMap(io.atomix.core.map.AsyncAtomicNavigableMap<String, V> atomixTreeMap) {
        this.atomixTreeMap = atomixTreeMap;
    }

    @Override
    public String name() {
        return atomixTreeMap.name();
    }

    @Override
    public CompletableFuture<Integer> size() {
        return atomixTreeMap.size();
    }

    @Override
    public CompletableFuture<String> firstKey() {
        return atomixTreeMap.firstKey();
    }

    @Override
    public CompletableFuture<String> lastKey() {
        return atomixTreeMap.lastKey();
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> ceilingEntry(String key) {
        return atomixTreeMap.ceilingEntry(key).thenApply(this::toVersioned);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> floorEntry(String key) {
        return atomixTreeMap.floorEntry(key).thenApply(this::toVersioned);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> higherEntry(String key) {
        return atomixTreeMap.higherEntry(key).thenApply(this::toVersioned);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> lowerEntry(String key) {
        return atomixTreeMap.lowerEntry(key).thenApply(this::toVersioned);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> firstEntry() {
        return atomixTreeMap.firstEntry().thenApply(this::toVersioned);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> lastEntry() {
        return atomixTreeMap.lastEntry().thenApply(this::toVersioned);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> pollFirstEntry() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> pollLastEntry() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Boolean> containsKey(String key) {
        return atomixTreeMap.containsKey(key);
    }

    @Override
    public CompletableFuture<String> lowerKey(String key) {
        return atomixTreeMap.lowerKey(key);
    }

    @Override
    public CompletableFuture<String> floorKey(String key) {
        return atomixTreeMap.floorKey(key);
    }

    @Override
    public CompletableFuture<String> ceilingKey(String key) {
        return atomixTreeMap.ceilingKey(key);
    }

    @Override
    public CompletableFuture<Versioned<V>> get(String key) {
        return atomixTreeMap.get(key).thenApply(this::toVersioned);
    }

    @Override
    public CompletableFuture<String> higherKey(String key) {
        return atomixTreeMap.higherKey(key);
    }

    @Override
    public CompletableFuture<NavigableSet<String>> navigableKeySet() {
        return CompletableFuture.completedFuture(atomixTreeMap.navigableKeySet()
            .sync(Duration.ofMillis(DEFAULT_OPERATION_TIMEOUT_MILLIS)));
    }

    @Override
    public CompletableFuture<Versioned<V>> getOrDefault(String key, V defaultValue) {
        return atomixTreeMap.getOrDefault(key, defaultValue).thenApply(this::toVersioned);
    }

    @Override
    public CompletableFuture<NavigableMap<String, V>> subMap(
        String upperKey, String lowerKey, boolean inclusiveUpper, boolean inclusiveLower) {
        return CompletableFuture.completedFuture(
            new DelegatingAsyncDistributedNavigableMap<>(
                atomixTreeMap.subMap(lowerKey, inclusiveLower, upperKey, inclusiveUpper))
                .sync(Duration.ofMillis(DEFAULT_OPERATION_TIMEOUT_MILLIS)));
    }

    @Override
    public CompletableFuture<Versioned<V>> computeIf(
        String key,
        Predicate<? super V> condition, BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
        return adaptMapFuture(atomixTreeMap.computeIf(key, condition, remappingFunction)).thenApply(this::toVersioned);
    }

    @Override
    public CompletableFuture<Versioned<V>> put(String key, V value) {
        return adaptMapFuture(atomixTreeMap.put(key, value)).thenApply(this::toVersioned);
    }

    @Override
    public CompletableFuture<Versioned<V>> putAndGet(String key, V value) {
        return adaptMapFuture(atomixTreeMap.putAndGet(key, value)).thenApply(this::toVersioned);
    }

    @Override
    public CompletableFuture<Versioned<V>> remove(String key) {
        return adaptMapFuture(atomixTreeMap.remove(key)).thenApply(this::toVersioned);
    }

    @Override
    public CompletableFuture<Set<String>> keySet() {
        return CompletableFuture.completedFuture(atomixTreeMap.keySet()
            .sync(Duration.ofMillis(DEFAULT_OPERATION_TIMEOUT_MILLIS)));
    }

    @Override
    public CompletableFuture<Set<Map.Entry<String, Versioned<V>>>> entrySet() {
        return CompletableFuture.completedFuture(atomixTreeMap.entrySet().stream()
            .map(this::toVersioned)
            .collect(Collectors.toSet()));
    }

    @Override
    public CompletableFuture<Versioned<V>> putIfAbsent(String key, V value) {
        return adaptMapFuture(atomixTreeMap.putIfAbsent(key, value)).thenApply(this::toVersioned);
    }

    @Override
    public CompletableFuture<Boolean> remove(String key, V value) {
        return adaptMapFuture(atomixTreeMap.remove(key, value));
    }

    @Override
    public CompletableFuture<Boolean> remove(String key, long version) {
        return adaptMapFuture(atomixTreeMap.remove(key, version));
    }

    @Override
    public CompletableFuture<Versioned<V>> replace(String key, V value) {
        return adaptMapFuture(atomixTreeMap.replace(key, value)).thenApply(this::toVersioned);
    }

    @Override
    public CompletableFuture<Boolean> replace(String key, V oldValue, V newValue) {
        return adaptMapFuture(atomixTreeMap.replace(key, oldValue, newValue));
    }

    @Override
    public CompletableFuture<Boolean> replace(String key, long oldVersion, V newValue) {
        return adaptMapFuture(atomixTreeMap.replace(key, oldVersion, newValue));
    }

    @Override
    public CompletableFuture<Boolean> containsValue(V value) {
        return atomixTreeMap.containsValue(value);
    }

    @Override
    public CompletableFuture<AsyncIterator<Map.Entry<String, Versioned<V>>>> iterator() {
        io.atomix.core.iterator.AsyncIterator<Map.Entry<String, io.atomix.utils.time.Versioned<V>>> atomixIterator
            = atomixTreeMap.entrySet().iterator();
        return CompletableFuture.completedFuture(new AsyncIterator<Map.Entry<String, Versioned<V>>>() {
            @Override
            public CompletableFuture<Boolean> hasNext() {
                return atomixIterator.hasNext();
            }

            @Override
            public CompletableFuture<Map.Entry<String, Versioned<V>>> next() {
                return atomixIterator.next()
                    .thenApply(entry -> Maps.immutableEntry(entry.getKey(), toVersioned(entry.getValue())));
            }
        });
    }

    @Override
    public CompletableFuture<Void> clear() {
        return atomixTreeMap.clear();
    }

    @Override
    public CompletableFuture<Collection<Versioned<V>>> values() {
        return CompletableFuture.completedFuture(
            new TranscodingAsyncDistributedCollection<Versioned<V>, io.atomix.utils.time.Versioned<V>>(
                atomixTreeMap.values(),
                e -> new io.atomix.utils.time.Versioned<>(e.value(), e.version()),
                e -> new Versioned<>(e.value(), e.version()))
                .sync(Duration.ofMillis(DEFAULT_OPERATION_TIMEOUT_MILLIS)));
    }

    @Override
    public synchronized CompletableFuture<Void> addListener(MapEventListener<String, V> listener, Executor executor) {
        io.atomix.core.map.AtomicMapEventListener<String, V> atomixListener = event ->
            listener.event(new MapEvent<String, V>(
                MapEvent.Type.valueOf(event.type().name()),
                name(),
                event.key(),
                toVersioned(event.newValue()),
                toVersioned(event.oldValue())));
        listenerMap.put(listener, atomixListener);
        return atomixTreeMap.addListener(atomixListener, executor);
    }

    @Override
    public CompletableFuture<Void> removeListener(MapEventListener<String, V> listener) {
        io.atomix.core.map.AtomicMapEventListener<String, V> atomixListener = listenerMap.remove(listener);
        if (atomixListener != null) {
            return atomixTreeMap.removeListener(atomixListener);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Version> begin(TransactionId transactionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Boolean> prepare(TransactionLog<MapUpdate<String, V>> transactionLog) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Boolean> prepareAndCommit(TransactionLog<MapUpdate<String, V>> transactionLog) {
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

    private Map.Entry<String, Versioned<V>> toVersioned(Map.Entry<String, io.atomix.utils.time.Versioned<V>> entry) {
        return entry != null ? Maps.immutableEntry(entry.getKey(), toVersioned(entry.getValue())) : null;
    }
}
