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
package org.onosproject.pcelabelstore.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.onosproject.store.primitives.ConsistentMapBackedJavaMap;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.ConsistentMapBuilder;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Versioned;

import com.google.common.base.Objects;

/**
 * Test implementation of the consistent map.
 */
public final class TestConsistentMap<K, V> extends ConsistentMapAdapter<K, V> {

    private final List<MapEventListener<K, V>> listeners;
    private final Map<K, Versioned<V>> map;
    private final String mapName;
    private final AtomicLong counter = new AtomicLong(0);

    private TestConsistentMap(String mapName) {
        map = new HashMap<>();
        listeners = new LinkedList<>();
        this.mapName = mapName;
    }

    private Versioned<V> version(V v) {
        return new Versioned<>(v, counter.incrementAndGet(), System.currentTimeMillis());
    }

    /**
     * Notify all listeners of an event.
     */
    private void notifyListeners(String mapName,
                                 K key, Versioned<V> newvalue, Versioned<V> oldValue) {
        MapEvent<K, V> event = new MapEvent<>(mapName, key, newvalue, oldValue);
        listeners.forEach(
                listener -> listener.event(event)
        );
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(V value) {
        return map.containsValue(value);
    }

    @Override
    public Versioned<V> get(K key) {
        return map.get(key);
    }

    @Override
    public Versioned<V> computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        AtomicBoolean updated = new AtomicBoolean(false);
        Versioned<V> result = map.compute(key, (k, v) -> {
            if (v == null) {
                updated.set(true);
                return version(mappingFunction.apply(key));
            }
            return v;
        });
        if (updated.get()) {
            notifyListeners(mapName, key, result, null);
        }
        return result;
    }

    @Override
    public Versioned<V> compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            AtomicBoolean updated = new AtomicBoolean(false);
            AtomicReference<Versioned<V>> previousValue = new AtomicReference<>();
            Versioned<V> result = map.compute(key, (k, v) -> {
                    updated.set(true);
                    previousValue.set(v);
                    return version(remappingFunction.apply(k, Versioned.valueOrNull(v)));
                });
            if (updated.get()) {
                notifyListeners(mapName, key, result, previousValue.get());
            }
            return result;
    }

    @Override
    public Versioned<V> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        AtomicBoolean updated = new AtomicBoolean(false);
        AtomicReference<Versioned<V>> previousValue = new AtomicReference<>();
        Versioned<V> result = map.compute(key, (k, v) -> {
            if (v != null) {
                updated.set(true);
                previousValue.set(v);
                return version(remappingFunction.apply(k, v.value()));
            }
            return v;
        });
        if (updated.get()) {
            notifyListeners(mapName, key, result, previousValue.get());
        }
        return result;
    }

    @Override
    public Versioned<V> computeIf(K key, Predicate<? super V> condition,
                                  BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        AtomicBoolean updated = new AtomicBoolean(false);
        AtomicReference<Versioned<V>> previousValue = new AtomicReference<>();
        Versioned<V> result = map.compute(key, (k, v) -> {
            if (condition.test(Versioned.valueOrNull(v))) {
                previousValue.set(v);
                updated.set(true);
                return version(remappingFunction.apply(k, Versioned.valueOrNull(v)));
            }
            return v;
        });
        if (updated.get()) {
            notifyListeners(mapName, key, result, previousValue.get());
        }
        return result;
    }

    @Override
    public Versioned<V> put(K key, V value) {
        Versioned<V> newValue = version(value);
        Versioned<V> previousValue = map.put(key, newValue);
        notifyListeners(mapName, key, newValue, previousValue);
        return previousValue;
    }

    @Override
    public Versioned<V> putAndGet(K key, V value) {
        Versioned<V> newValue = version(value);
        Versioned<V> previousValue = map.put(key, newValue);
        notifyListeners(mapName, key, newValue, previousValue);
        return newValue;
    }

    @Override
    public Versioned<V> remove(K key) {
        Versioned<V> result = map.remove(key);
        notifyListeners(mapName, key, null, result);
        return result;
    }

    @Override
    public void clear() {
        map.keySet().forEach(this::remove);
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<Versioned<V>> values() {
        return map.values()
                .stream()
                .collect(Collectors.toList());
    }

    @Override
    public Set<Map.Entry<K, Versioned<V>>> entrySet() {
        return map.entrySet();
    }

    @Override
    public Versioned<V> putIfAbsent(K key, V value) {
        Versioned<V> newValue = version(value);
        Versioned<V> result =  map.putIfAbsent(key, newValue);
        if (result == null) {
            notifyListeners(mapName, key, newValue, result);
        }
        return result;
    }

    @Override
    public boolean remove(K key, V value) {
        Versioned<V> existingValue = map.get(key);
        if (Objects.equal(Versioned.valueOrNull(existingValue), value)) {
            map.remove(key);
            notifyListeners(mapName, key, null, existingValue);
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(K key, long version) {
        Versioned<V> existingValue = map.get(key);
        if (existingValue == null) {
            return false;
        }
        if (existingValue.version() == version) {
            map.remove(key);
            notifyListeners(mapName, key, null, existingValue);
            return true;
        }
        return false;
    }

    @Override
    public Versioned<V> replace(K key, V value) {
        Versioned<V> existingValue = map.get(key);
        if (existingValue == null) {
            return null;
        }
        Versioned<V> newValue = version(value);
        Versioned<V> result = map.put(key, newValue);
        notifyListeners(mapName, key, newValue, result);
        return result;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        Versioned<V> existingValue = map.get(key);
        if (existingValue == null || !existingValue.value().equals(oldValue)) {
            return false;
        }
        Versioned<V> value = version(newValue);
        Versioned<V> result = map.put(key, value);
        notifyListeners(mapName, key, value, result);
        return true;
    }

    @Override
    public boolean replace(K key, long oldVersion, V newValue) {
        Versioned<V> existingValue = map.get(key);
        if (existingValue == null || existingValue.version() != oldVersion) {
            return false;
        }
        Versioned<V> value = version(newValue);
        Versioned<V> result = map.put(key, value);
        notifyListeners(mapName, key, value, result);
        return true;
    }

    @Override
    public void addListener(MapEventListener<K, V> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(MapEventListener<K, V> listener) {
        listeners.remove(listener);
    }

    @Override
    public Map<K, V> asJavaMap() {
        return new ConsistentMapBackedJavaMap<>(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder<K, V> extends ConsistentMapBuilder<K, V> {

        @Override
        public ConsistentMap<K, V> build() {
            return new TestConsistentMap<>(name());
        }

        @Override
        public AsyncConsistentMap<K, V> buildAsyncMap() {
            return null;
        }

    }

}
