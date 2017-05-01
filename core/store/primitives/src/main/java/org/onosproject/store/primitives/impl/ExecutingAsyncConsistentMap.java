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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.TransactionLog;
import org.onosproject.store.service.Version;
import org.onosproject.store.service.Versioned;

/**
 * An {@link org.onosproject.store.service.AsyncConsistentMap} that completes asynchronous calls on a provided
 * {@link Executor}.
 */
public class ExecutingAsyncConsistentMap<K, V>
        extends ExecutingDistributedPrimitive implements AsyncConsistentMap<K, V> {
    private final AsyncConsistentMap<K, V> delegateMap;

    public ExecutingAsyncConsistentMap(
            AsyncConsistentMap<K, V> delegateMap, Executor orderedExecutor, Executor threadPoolExecutor) {
        super(delegateMap, orderedExecutor, threadPoolExecutor);
        this.delegateMap = delegateMap;
    }

    @Override
    public CompletableFuture<Integer> size() {
        return asyncFuture(delegateMap.size());
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
    public CompletableFuture<Versioned<V>> get(K key) {
        return asyncFuture(delegateMap.get(key));
    }

    @Override
    public CompletableFuture<Versioned<V>> getOrDefault(K key, V defaultValue) {
        return asyncFuture(delegateMap.getOrDefault(key, defaultValue));
    }

    @Override
    public CompletableFuture<Versioned<V>> computeIf(
            K key, Predicate<? super V> condition, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return asyncFuture(delegateMap.computeIf(key, condition, remappingFunction));
    }

    @Override
    public CompletableFuture<Versioned<V>> put(K key, V value) {
        return asyncFuture(delegateMap.put(key, value));
    }

    @Override
    public CompletableFuture<Versioned<V>> putAndGet(K key, V value) {
        return asyncFuture(delegateMap.putAndGet(key, value));
    }

    @Override
    public CompletableFuture<Versioned<V>> remove(K key) {
        return asyncFuture(delegateMap.remove(key));
    }

    @Override
    public CompletableFuture<Void> clear() {
        return asyncFuture(delegateMap.clear());
    }

    @Override
    public CompletableFuture<Set<K>> keySet() {
        return asyncFuture(delegateMap.keySet());
    }

    @Override
    public CompletableFuture<Collection<Versioned<V>>> values() {
        return asyncFuture(delegateMap.values());
    }

    @Override
    public CompletableFuture<Set<Map.Entry<K, Versioned<V>>>> entrySet() {
        return asyncFuture(delegateMap.entrySet());
    }

    @Override
    public CompletableFuture<Versioned<V>> putIfAbsent(K key, V value) {
        return asyncFuture(delegateMap.putIfAbsent(key, value));
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, V value) {
        return asyncFuture(delegateMap.remove(key, value));
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, long version) {
        return asyncFuture(delegateMap.remove(key, version));
    }

    @Override
    public CompletableFuture<Versioned<V>> replace(K key, V value) {
        return asyncFuture(delegateMap.replace(key, value));
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, V oldValue, V newValue) {
        return asyncFuture(delegateMap.replace(key, oldValue, newValue));
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, long oldVersion, V newValue) {
        return asyncFuture(delegateMap.replace(key, oldVersion, newValue));
    }

    @Override
    public CompletableFuture<Version> begin(TransactionId transactionId) {
        return asyncFuture(delegateMap.begin(transactionId));
    }

    @Override
    public CompletableFuture<Boolean> prepare(TransactionLog<MapUpdate<K, V>> transactionLog) {
        return asyncFuture(delegateMap.prepare(transactionLog));
    }

    @Override
    public CompletableFuture<Void> commit(TransactionId transactionId) {
        return asyncFuture(delegateMap.commit(transactionId));
    }

    @Override
    public CompletableFuture<Void> rollback(TransactionId transactionId) {
        return asyncFuture(delegateMap.rollback(transactionId));
    }

    @Override
    public CompletableFuture<Boolean> prepareAndCommit(TransactionLog<MapUpdate<K, V>> transactionLog) {
        return asyncFuture(delegateMap.prepareAndCommit(transactionLog));
    }

    @Override
    public CompletableFuture<Void> addListener(MapEventListener<K, V> listener) {
        return addListener(listener);
    }

    @Override
    public CompletableFuture<Void> addListener(MapEventListener<K, V> listener, Executor executor) {
        return asyncFuture(delegateMap.addListener(listener, executor));
    }

    @Override
    public CompletableFuture<Void> removeListener(MapEventListener<K, V> listener) {
        return asyncFuture(delegateMap.removeListener(listener));
    }
}
