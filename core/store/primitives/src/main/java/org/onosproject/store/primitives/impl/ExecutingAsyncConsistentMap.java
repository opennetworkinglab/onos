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

import org.onlab.util.Tools;
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
    private final Executor executor;

    public ExecutingAsyncConsistentMap(AsyncConsistentMap<K, V> delegateMap, Executor executor) {
        super(delegateMap, executor);
        this.delegateMap = delegateMap;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Integer> size() {
        return Tools.asyncFuture(delegateMap.size(), executor);
    }

    @Override
    public CompletableFuture<Boolean> containsKey(K key) {
        return Tools.asyncFuture(delegateMap.containsKey(key), executor);
    }

    @Override
    public CompletableFuture<Boolean> containsValue(V value) {
        return Tools.asyncFuture(delegateMap.containsValue(value), executor);
    }

    @Override
    public CompletableFuture<Versioned<V>> get(K key) {
        return Tools.asyncFuture(delegateMap.get(key), executor);
    }

    @Override
    public CompletableFuture<Versioned<V>> getOrDefault(K key, V defaultValue) {
        return Tools.asyncFuture(delegateMap.getOrDefault(key, defaultValue), executor);
    }

    @Override
    public CompletableFuture<Versioned<V>> computeIf(
            K key, Predicate<? super V> condition, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return Tools.asyncFuture(delegateMap.computeIf(key, condition, remappingFunction), executor);
    }

    @Override
    public CompletableFuture<Versioned<V>> put(K key, V value) {
        return Tools.asyncFuture(delegateMap.put(key, value), executor);
    }

    @Override
    public CompletableFuture<Versioned<V>> putAndGet(K key, V value) {
        return Tools.asyncFuture(delegateMap.putAndGet(key, value), executor);
    }

    @Override
    public CompletableFuture<Versioned<V>> remove(K key) {
        return Tools.asyncFuture(delegateMap.remove(key), executor);
    }

    @Override
    public CompletableFuture<Void> clear() {
        return Tools.asyncFuture(delegateMap.clear(), executor);
    }

    @Override
    public CompletableFuture<Set<K>> keySet() {
        return Tools.asyncFuture(delegateMap.keySet(), executor);
    }

    @Override
    public CompletableFuture<Collection<Versioned<V>>> values() {
        return Tools.asyncFuture(delegateMap.values(), executor);
    }

    @Override
    public CompletableFuture<Set<Map.Entry<K, Versioned<V>>>> entrySet() {
        return Tools.asyncFuture(delegateMap.entrySet(), executor);
    }

    @Override
    public CompletableFuture<Versioned<V>> putIfAbsent(K key, V value) {
        return Tools.asyncFuture(delegateMap.putIfAbsent(key, value), executor);
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, V value) {
        return Tools.asyncFuture(delegateMap.remove(key, value), executor);
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, long version) {
        return Tools.asyncFuture(delegateMap.remove(key, version), executor);
    }

    @Override
    public CompletableFuture<Versioned<V>> replace(K key, V value) {
        return Tools.asyncFuture(delegateMap.replace(key, value), executor);
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, V oldValue, V newValue) {
        return Tools.asyncFuture(delegateMap.replace(key, oldValue, newValue), executor);
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, long oldVersion, V newValue) {
        return Tools.asyncFuture(delegateMap.replace(key, oldVersion, newValue), executor);
    }

    @Override
    public CompletableFuture<Version> begin(TransactionId transactionId) {
        return Tools.asyncFuture(delegateMap.begin(transactionId), executor);
    }

    @Override
    public CompletableFuture<Boolean> prepare(TransactionLog<MapUpdate<K, V>> transactionLog) {
        return Tools.asyncFuture(delegateMap.prepare(transactionLog), executor);
    }

    @Override
    public CompletableFuture<Void> commit(TransactionId transactionId) {
        return Tools.asyncFuture(delegateMap.commit(transactionId), executor);
    }

    @Override
    public CompletableFuture<Void> rollback(TransactionId transactionId) {
        return Tools.asyncFuture(delegateMap.rollback(transactionId), executor);
    }

    @Override
    public CompletableFuture<Boolean> prepareAndCommit(TransactionLog<MapUpdate<K, V>> transactionLog) {
        return Tools.asyncFuture(delegateMap.prepareAndCommit(transactionLog), executor);
    }

    @Override
    public CompletableFuture<Void> addListener(MapEventListener<K, V> listener) {
        return addListener(listener, executor);
    }

    @Override
    public CompletableFuture<Void> addListener(MapEventListener<K, V> listener, Executor executor) {
        return Tools.asyncFuture(delegateMap.addListener(listener, executor), this.executor);
    }

    @Override
    public CompletableFuture<Void> removeListener(MapEventListener<K, V> listener) {
        return Tools.asyncFuture(delegateMap.removeListener(listener), executor);
    }
}
