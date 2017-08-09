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

import org.onosproject.store.service.AsyncConsistentMapAdapter;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Versioned;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/*Simple template for asynchronous map
Serializer must be defined independently
 */
public class AsyncConsistentMapMock<K, V> extends AsyncConsistentMapAdapter<K, V> {
    private final Map<K, V> baseMap = new HashMap<>();
    private final List<MapEventListener<K, V>> listeners;

    Versioned<V> makeVersioned(V v) {
        return new Versioned<>(v, 1, 0);
    }

    public AsyncConsistentMapMock() {
        this.listeners = new ArrayList<>();
    }

    public CompletableFuture<Integer> size() {
        return CompletableFuture.completedFuture(baseMap.size());
    }

    @Override
    public CompletableFuture<Boolean> containsKey(K key) {
        return CompletableFuture.completedFuture(baseMap.containsKey(key));
    }

    @Override
    public CompletableFuture<Boolean> containsValue(V value) {
        return CompletableFuture.completedFuture(baseMap.values()
                .stream()
                .anyMatch(v -> {
                    if (v instanceof byte[] && value instanceof byte[]) {
                        return Arrays.equals((byte[]) v, (byte[]) value);
                    } else {
                        return Objects.equals(v, value);
                    }
                }));
    }

    @Override
    public CompletableFuture<Versioned<V>> get(K key) {
        return CompletableFuture.completedFuture(makeVersioned(baseMap.get(key)));
    }

    @Override
    public CompletableFuture<Versioned<V>> getOrDefault(K key, V defaultValue) {
        return CompletableFuture.completedFuture(makeVersioned(baseMap.getOrDefault(key, defaultValue)));
    }

    @Override
    public CompletableFuture<Versioned<V>>
    computeIf(K key, Predicate<? super V> condition,
              BiFunction<? super K, ? super V, ? extends V> remappingFunction) {

        V value = baseMap.get(key);

        if (condition.test(value)) {
            value = baseMap.compute(key, remappingFunction);
        }
        return CompletableFuture.completedFuture(makeVersioned(value));
    }

    @Override
    public CompletableFuture<Versioned<V>> put(K key, V value) {
        return CompletableFuture.completedFuture(makeVersioned(baseMap.put(key, value)));
    }

    @Override
    public CompletableFuture<Versioned<V>> putAndGet(K key, V value) {
        return CompletableFuture.completedFuture(makeVersioned(baseMap.put(key, value)));
    }

    @Override
    public CompletableFuture<Versioned<V>> remove(K key) {
        return CompletableFuture.completedFuture(makeVersioned(baseMap.remove(key)));
    }

    @Override
    public CompletableFuture<Void> clear() {
        baseMap.clear();
        return CompletableFuture.allOf();
    }

    @Override
    public CompletableFuture<Set<K>> keySet() {
        return CompletableFuture.completedFuture(baseMap.keySet());
    }

    @Override
    public CompletableFuture<Collection<Versioned<V>>> values() {
        Set<Versioned<V>> valuesAsVersionedCollection =
                baseMap.values().stream().map(this::makeVersioned)
                        .collect(Collectors.toSet());
        return CompletableFuture.completedFuture(valuesAsVersionedCollection);
    }

    @Override
    public CompletableFuture<Set<Map.Entry<K, Versioned<V>>>> entrySet() {
        Map<K, Versioned<V>> valuesAsVersionedMap = new HashMap<>();
        baseMap.entrySet()
                .forEach(e -> valuesAsVersionedMap.put(e.getKey(),
                        makeVersioned(e.getValue())));
        return CompletableFuture.completedFuture(valuesAsVersionedMap.entrySet());
    }

    @Override
    public CompletableFuture<Versioned<V>> putIfAbsent(K key, V value) {
        return CompletableFuture.completedFuture(makeVersioned(baseMap.putIfAbsent(key, value)));
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, V value) {
        return CompletableFuture.completedFuture(baseMap.remove(key, value));
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, long version) {
        Object value = baseMap.remove(key);
        return CompletableFuture.completedFuture(value != null);
    }

    @Override
    public CompletableFuture<Versioned<V>> replace(K key, V value) {
        return CompletableFuture.completedFuture(makeVersioned(baseMap.replace(key, value)));
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, V oldValue, V newValue) {
        V current = baseMap.get(key);
            if (current instanceof byte[] && oldValue instanceof byte[]) {
                baseMap.put(key, newValue);
                return CompletableFuture.completedFuture(Arrays.equals((byte[]) current, (byte[]) oldValue));
            } else {
                return CompletableFuture.completedFuture(Objects.equals(current, oldValue));
            }
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, long oldVersion, V newValue) {
        return CompletableFuture.completedFuture(baseMap.replace(key, newValue) != null);
    }

    @Override
    public CompletableFuture<Void> addListener(MapEventListener<K, V> listener, Executor e) {
        listeners.add(listener);
        return CompletableFuture.allOf();
    }

    @Override
    public CompletableFuture<Void> removeListener(MapEventListener<K, V> listener) {
        listeners.remove(listener);
        return CompletableFuture.allOf();
    }
}

