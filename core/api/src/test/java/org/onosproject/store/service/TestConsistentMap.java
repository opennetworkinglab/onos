/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.onosproject.core.ApplicationId;
import static org.onosproject.store.service.MapEvent.Type;
import static org.onosproject.store.service.MapEvent.Type.*;

/**
 * Test implementation of the consistent map.
 */
public final class TestConsistentMap<K, V> extends ConsistentMapAdapter<K, V> {

    private final List<MapEventListener<K, V>> listeners;
    private final HashMap<K, V> map;
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
    private void notifyListeners(String mapName, Type type,
                                 K key, Versioned<V> value) {
        MapEvent<K, V> event = new MapEvent<>(mapName, type, key, value);
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
        V value = map.get(key);
        if (value != null) {
            return version(value);
        } else {
            return null;
        }
    }

    @Override
    public Versioned<V> computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        Versioned<V> result = version(map.computeIfAbsent(key, mappingFunction));
        notifyListeners(mapName, INSERT, key, result);
        return result;
    }

    @Override
    public Versioned<V> compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return version(map.compute(key, remappingFunction));
    }

    @Override
    public Versioned<V> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return version(map.computeIfPresent(key, remappingFunction));
    }

    @Override
    public Versioned<V> computeIf(K key, Predicate<? super V> condition,
                                  BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return version(map.compute(key, (k, existingValue) -> {
            if (condition.test(existingValue)) {
                return remappingFunction.apply(k, existingValue);
            } else {
                return existingValue;
            }
        }));
    }

    @Override
    public Versioned<V> put(K key, V value) {
        Versioned<V> result = version(value);
        if (map.put(key, value) == null) {
            notifyListeners(mapName, INSERT, key, result);
        } else {
            notifyListeners(mapName, UPDATE, key, result);
        }
        return result;
    }

    @Override
    public Versioned<V> putAndGet(K key, V value) {
        Versioned<V> result = version(map.put(key, value));
        notifyListeners(mapName, UPDATE, key, result);
        return result;
    }

    @Override
    public Versioned<V> remove(K key) {
        Versioned<V> result = version(map.remove(key));
        notifyListeners(mapName, REMOVE, key, result);
        return result;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<Versioned<V>> values() {
        return map
                .values()
                .stream()
                .map(this::version)
                .collect(Collectors.toList());
    }

    @Override
    public Set<Map.Entry<K, Versioned<V>>> entrySet() {
        return super.entrySet();
    }

    @Override
    public Versioned<V> putIfAbsent(K key, V value) {
        Versioned<V> result =  version(map.putIfAbsent(key, value));
        if (map.get(key).equals(value)) {
            notifyListeners(mapName, INSERT, key, result);
        }
        return result;
    }

    @Override
    public boolean remove(K key, V value) {
        boolean removed = map.remove(key, value);
        if (removed) {
            notifyListeners(mapName, REMOVE, key, null);
        }
        return removed;
    }

    @Override
    public boolean remove(K key, long version) {
        boolean removed =  map.remove(key, version);
        if (removed) {
            notifyListeners(mapName, REMOVE, key, null);
        }
        return removed;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        boolean replaced = map.replace(key, oldValue, newValue);
        if (replaced) {
            notifyListeners(mapName, REMOVE, key, null);
        }
        return replaced;
    }

    @Override
    public boolean replace(K key, long oldVersion, V newValue) {
        boolean replaced =  map.replace(key, map.get(key), newValue);
        if (replaced) {
            notifyListeners(mapName, REMOVE, key, null);
        }
        return replaced;
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
        return map;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder<K, V> implements ConsistentMapBuilder<K, V> {
        String mapName = "map";

        @Override
        public ConsistentMapBuilder<K, V> withName(String mapName) {
            this.mapName = mapName;
            return this;
        }

        @Override
        public ConsistentMapBuilder<K, V> withApplicationId(ApplicationId id) {
            return this;
        }

        @Override
        public ConsistentMapBuilder<K, V> withSerializer(Serializer serializer) {
            return this;
        }

        @Override
        public ConsistentMapBuilder<K, V> withPartitionsDisabled() {
            return this;
        }

        @Override
        public ConsistentMapBuilder<K, V> withUpdatesDisabled() {
            return this;
        }

        @Override
        public ConsistentMapBuilder<K, V> withPurgeOnUninstall() {
            return this;
        }

        @Override
        public ConsistentMapBuilder<K, V> withRelaxedReadConsistency() {
            return this;
        }

        @Override
        public ConsistentMapBuilder<K, V> withMeteringDisabled() {
            return this;
        }

        @Override
        public ConsistentMap<K, V> build() {
            return new TestConsistentMap<>(mapName);
        }

        @Override
        public AsyncConsistentMap<K, V> buildAsyncMap() {
            return null;
        }

    }

}
