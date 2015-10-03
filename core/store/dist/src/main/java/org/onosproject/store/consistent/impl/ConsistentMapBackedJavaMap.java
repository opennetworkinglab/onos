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
package org.onosproject.store.consistent.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Versioned;

import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;

/**
 * Standard java Map backed by a ConsistentMap.
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class ConsistentMapBackedJavaMap<K, V> implements Map<K, V> {

    private final ConsistentMap<K, V> backingMap;

    public ConsistentMapBackedJavaMap(ConsistentMap<K, V> backingMap) {
        this.backingMap = backingMap;
    }

    @Override
    public int size() {
        return backingMap.size();
    }

    @Override
    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return backingMap.containsKey((K) key);
    }

    @Override
    public boolean containsValue(Object value) {
        return backingMap.containsValue((V) value);
    }

    @Override
    public V get(Object key) {
        return Versioned.valueOrElse(backingMap.get((K) key), null);
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return Versioned.valueOrElse(backingMap.get((K) key), defaultValue);
    }

    @Override
    public V put(K key, V value) {
        return Versioned.valueOrElse(backingMap.put(key, value), null);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return Versioned.valueOrElse(backingMap.putIfAbsent(key, value), null);
    }

    @Override
    public V remove(Object key) {
        return Versioned.valueOrElse(backingMap.remove((K) key), null);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return backingMap.remove((K) key, (V) value);
    }

    @Override
    public V replace(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return  backingMap.replace(key, oldValue, newValue);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.forEach((k, v) -> {
            backingMap.put(k, v);
        });
    }

    @Override
    public void clear() {
        backingMap.clear();
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return Versioned.valueOrElse(backingMap.compute(key, remappingFunction), null);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return Versioned.valueOrElse(backingMap.computeIfAbsent(key, mappingFunction), null);
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return Versioned.valueOrElse(backingMap.computeIfPresent(key, remappingFunction), null);
    }

    @Override
    public Set<K> keySet() {
        return backingMap.keySet();
    }

    @Override
    public Collection<V> values() {
        return Collections2.transform(backingMap.values(), v -> v.value());
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return backingMap.entrySet()
                         .stream()
                         .map(entry -> Maps.immutableEntry(entry.getKey(), entry.getValue().value()))
                         .collect(Collectors.toSet());
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        entrySet().forEach(e -> action.accept(e.getKey(), e.getValue()));
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return computeIfPresent(key, (k, v) -> v == null ? value : remappingFunction.apply(v, value));
    }
}