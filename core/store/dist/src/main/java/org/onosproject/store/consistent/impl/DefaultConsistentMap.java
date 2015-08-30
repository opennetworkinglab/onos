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
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.Set;

import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.ConsistentMapException;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Versioned;

/**
 * ConsistentMap implementation that is backed by a Raft consensus
 * based database.
 *
 * @param <K> type of key.
 * @param <V> type of value.
 */
public class DefaultConsistentMap<K, V> implements ConsistentMap<K, V> {

    private static final int OPERATION_TIMEOUT_MILLIS = 5000;

    private final DefaultAsyncConsistentMap<K, V> asyncMap;
    private Map<K, V> javaMap;

    public String name() {
        return asyncMap.name();
    }

    public DefaultConsistentMap(DefaultAsyncConsistentMap<K, V> asyncMap) {
        this.asyncMap = asyncMap;
    }

    @Override
    public int size() {
        return complete(asyncMap.size());
    }

    @Override
    public boolean isEmpty() {
        return complete(asyncMap.isEmpty());
    }

    @Override
    public boolean containsKey(K key) {
        return complete(asyncMap.containsKey(key));
    }

    @Override
    public boolean containsValue(V value) {
        return complete(asyncMap.containsValue(value));
    }

    @Override
    public Versioned<V> get(K key) {
        return complete(asyncMap.get(key));
    }

    @Override
    public Versioned<V> computeIfAbsent(K key,
            Function<? super K, ? extends V> mappingFunction) {
        return complete(asyncMap.computeIfAbsent(key, mappingFunction));
    }

    @Override
    public Versioned<V> computeIfPresent(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return complete(asyncMap.computeIfPresent(key, remappingFunction));
    }

    @Override
    public Versioned<V> compute(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return complete(asyncMap.compute(key, remappingFunction));
    }

    @Override
    public Versioned<V> computeIf(K key,
            Predicate<? super V> condition,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return complete(asyncMap.computeIf(key, condition, remappingFunction));
    }

    @Override
    public Versioned<V> put(K key, V value) {
        return complete(asyncMap.put(key, value));
    }

    @Override
    public Versioned<V> putAndGet(K key, V value) {
        return complete(asyncMap.putAndGet(key, value));
    }

    @Override
    public Versioned<V> remove(K key) {
        return complete(asyncMap.remove(key));
    }

    @Override
    public void clear() {
        complete(asyncMap.clear());
    }

    @Override
    public Set<K> keySet() {
        return complete(asyncMap.keySet());
    }

    @Override
    public Collection<Versioned<V>> values() {
        return complete(asyncMap.values());
    }

    @Override
    public Set<Entry<K, Versioned<V>>> entrySet() {
        return complete(asyncMap.entrySet());
    }

    @Override
    public Versioned<V> putIfAbsent(K key, V value) {
        return complete(asyncMap.putIfAbsent(key, value));
    }

    @Override
    public boolean remove(K key, V value) {
        return complete(asyncMap.remove(key, value));
    }

    @Override
    public boolean remove(K key, long version) {
        return complete(asyncMap.remove(key, version));
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return complete(asyncMap.replace(key, oldValue, newValue));
    }

    @Override
    public boolean replace(K key, long oldVersion, V newValue) {
        return complete(asyncMap.replace(key, oldVersion, newValue));
    }

    private static <T> T complete(CompletableFuture<T> future) {
        try {
            return future.get(OPERATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConsistentMapException.Interrupted();
        } catch (TimeoutException e) {
            throw new ConsistentMapException.Timeout();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ConsistentMapException) {
                throw (ConsistentMapException) e.getCause();
            } else {
                throw new ConsistentMapException(e.getCause());
            }
        }
    }

    @Override
    public void addListener(MapEventListener<K, V> listener) {
        asyncMap.addListener(listener);
    }

    @Override
    public void removeListener(MapEventListener<K, V> listener) {
        asyncMap.addListener(listener);
    }

    @Override
    public Map<K, V> asJavaMap() {
        synchronized (this) {
            if (javaMap == null) {
                javaMap = new ConsistentMapBackedJavaMap<>(this);
            }
        }
        return javaMap;
    }
}