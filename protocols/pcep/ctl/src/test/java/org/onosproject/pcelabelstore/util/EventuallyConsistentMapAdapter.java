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
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapListener;

/**
 * Testing adapter for EventuallyConsistentMap.
 */
public class EventuallyConsistentMapAdapter<K, V> implements EventuallyConsistentMap<K, V> {

    @Override
    public String name() {
        return null;
    }

    @Override
    public Type primitiveType() {
        return Type.EVENTUALLY_CONSISTENT_MAP;
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
    public V get(K key) {
        return null;
    }

    @Override
    public void put(K key, V value) {

    }

    @Override
    public V remove(K key) {
        return null;
    }

    @Override
    public void remove(K key, V value) {

    }

    @Override
    public V compute(K key, BiFunction<K, V, V> recomputeFunction) {
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Set<K> keySet() {
        return null;
    }

    @Override
    public Collection<V> values() {
        return null;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return null;
    }

    @Override
    public void addListener(EventuallyConsistentMapListener<K, V> listener) {

    }

    @Override
    public void removeListener(EventuallyConsistentMapListener<K, V> listener) {

    }

    @Override
    public CompletableFuture<Void> destroy() {
        return CompletableFuture.completedFuture(null);
    }
}
