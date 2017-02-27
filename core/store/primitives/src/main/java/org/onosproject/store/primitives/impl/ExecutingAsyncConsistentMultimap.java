/*
 * Copyright 2017-present Open Networking Laboratory
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

import com.google.common.collect.Multiset;
import org.onosproject.store.service.AsyncConsistentMultimap;
import org.onosproject.store.service.MultimapEventListener;
import org.onosproject.store.service.Versioned;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * {@link org.onosproject.store.service.AsyncConsistentMultimap} that executes asynchronous callbacks on a provided
 * {@link Executor}.
 */
public class ExecutingAsyncConsistentMultimap<K, V>
        extends ExecutingDistributedPrimitive implements AsyncConsistentMultimap<K, V> {
    private final AsyncConsistentMultimap<K, V> delegateMap;

    public ExecutingAsyncConsistentMultimap(
            AsyncConsistentMultimap<K, V> delegateMap, Executor orderedExecutor, Executor threadPoolExecutor) {
        super(delegateMap, orderedExecutor, threadPoolExecutor);
        this.delegateMap = delegateMap;
    }

    @Override
    public CompletableFuture<Integer> size() {
        return asyncFuture(delegateMap.size());
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return asyncFuture(delegateMap.isEmpty());
    }

    @Override
    public CompletableFuture<Boolean> containsKey(K key) {
        return asyncFuture(delegateMap.containsKey(key));
    }

    @Override
    public CompletableFuture<Boolean> containsValue(V value) {
        return asyncFuture(delegateMap.containsValue(value));
    }

    @Override
    public CompletableFuture<Boolean> containsEntry(K key, V value) {
        return asyncFuture(delegateMap.containsEntry(key, value));
    }

    @Override
    public CompletableFuture<Boolean> put(K key, V value) {
        return asyncFuture(delegateMap.put(key, value));
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, V value) {
        return asyncFuture(delegateMap.remove(key, value));
    }

    @Override
    public CompletableFuture<Boolean> removeAll(K key, Collection<? extends V> values) {
        return asyncFuture(delegateMap.removeAll(key, values));
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends V>>> removeAll(K key) {
        return asyncFuture(delegateMap.removeAll(key));
    }

    @Override
    public CompletableFuture<Boolean> putAll(K key, Collection<? extends V> values) {
        return asyncFuture(delegateMap.putAll(key, values));
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends V>>> replaceValues(K key, Collection<V> values) {
        return asyncFuture(delegateMap.replaceValues(key, values));
    }

    @Override
    public CompletableFuture<Void> clear() {
        return asyncFuture(delegateMap.clear());
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends V>>> get(K key) {
        return asyncFuture(delegateMap.get(key));
    }

    @Override
    public CompletableFuture<Set<K>> keySet() {
        return asyncFuture(delegateMap.keySet());
    }

    @Override
    public CompletableFuture<Multiset<K>> keys() {
        return asyncFuture(delegateMap.keys());
    }

    @Override
    public CompletableFuture<Multiset<V>> values() {
        return asyncFuture(delegateMap.values());
    }

    @Override
    public CompletableFuture<Collection<Map.Entry<K, V>>> entries() {
        return asyncFuture(delegateMap.entries());
    }

    @Override
    public CompletableFuture<Void> addListener(MultimapEventListener<K, V> listener, Executor executor) {
        return asyncFuture(delegateMap.addListener(listener, executor));
    }

    @Override
    public CompletableFuture<Void> removeListener(MultimapEventListener<K, V> listener) {
        return asyncFuture(delegateMap.removeListener(listener));
    }

    @Override
    public CompletableFuture<Map<K, Collection<V>>> asMap() {
        return asyncFuture(delegateMap.asMap());
    }
}
