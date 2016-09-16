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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedPrimitive;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Versioned;

/**
 * Testing adapter for the consistent map.
 */
public class ConsistentMapAdapter<K, V> implements ConsistentMap<K, V> {

    @Override
    public String name() {
        return null;
    }

    @Override
    public DistributedPrimitive.Type primitiveType() {
        return DistributedPrimitive.Type.CONSISTENT_MAP;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(K key) {
        return false;
    }

    @Override
    public boolean containsValue(V value) {
        return false;
    }

    @Override
    public Versioned<V> get(K key) {
        return null;
    }

    @Override
    public Versioned<V> computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return null;
    }

    @Override
    public Versioned<V> compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return null;
    }

    @Override
    public Versioned<V> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return null;
    }

    @Override
    public Versioned<V> computeIf(K key, Predicate<? super V> condition,
                                  BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return null;
    }

    @Override
    public Versioned<V> put(K key, V value) {
        return null;
    }

    @Override
    public Versioned<V> putAndGet(K key, V value) {
        return null;
    }

    @Override
    public Versioned<V> remove(K key) {
        return null;
    }

    @Override
    public void clear() {

    }

    @Override
    public Set<K> keySet() {
        return null;
    }

    @Override
    public Collection<Versioned<V>> values() {
        return null;
    }

    @Override
    public Set<Map.Entry<K, Versioned<V>>> entrySet() {
        return null;
    }

    @Override
    public Versioned<V> putIfAbsent(K key, V value) {
        return null;
    }

    @Override
    public boolean remove(K key, V value) {
        return false;
    }

    @Override
    public boolean remove(K key, long version) {
        return false;
    }

    @Override
    public Versioned replace(K key, V value) {
        return null;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return false;
    }

    @Override
    public boolean replace(K key, long oldVersion, V newValue) {
        return false;
    }

    @Override
    public void addListener(MapEventListener<K, V> listener, Executor executor) {

    }

    @Override
    public void removeListener(MapEventListener<K, V> listener) {

    }

    @Override
    public Map<K, V> asJavaMap() {
        return null;
    }
}
