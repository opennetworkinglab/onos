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
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.onlab.util.Tools;
import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.service.AsyncConsistentTreeMap;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.TransactionLog;
import org.onosproject.store.service.Version;
import org.onosproject.store.service.Versioned;

/**
 * {@link org.onosproject.store.service.AsyncConsistentTreeMap} that executes asynchronous callbacks on a provided
 * {@link Executor}.
 */
public class ExecutingAsyncConsistentTreeMap<V>
        extends ExecutingDistributedPrimitive implements AsyncConsistentTreeMap<V> {
    private final AsyncConsistentTreeMap<V> delegateMap;
    private final Executor executor;

    public ExecutingAsyncConsistentTreeMap(AsyncConsistentTreeMap<V> delegateMap, Executor executor) {
        super(delegateMap, executor);
        this.delegateMap = delegateMap;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<String> firstKey() {
        return Tools.asyncFuture(delegateMap.firstKey(), executor);
    }

    @Override
    public CompletableFuture<String> lastKey() {
        return Tools.asyncFuture(delegateMap.lastKey(), executor);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> ceilingEntry(String key) {
        return Tools.asyncFuture(delegateMap.ceilingEntry(key), executor);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> floorEntry(String key) {
        return Tools.asyncFuture(delegateMap.floorEntry(key), executor);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> higherEntry(String key) {
        return Tools.asyncFuture(delegateMap.higherEntry(key), executor);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> lowerEntry(String key) {
        return Tools.asyncFuture(delegateMap.lowerEntry(key), executor);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> firstEntry() {
        return Tools.asyncFuture(delegateMap.firstEntry(), executor);
    }

    @Override
    public CompletableFuture<Integer> size() {
        return Tools.asyncFuture(delegateMap.size(), executor);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> lastEntry() {
        return Tools.asyncFuture(delegateMap.lastEntry(), executor);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> pollFirstEntry() {
        return Tools.asyncFuture(delegateMap.pollFirstEntry(), executor);
    }

    @Override
    public CompletableFuture<Boolean> containsKey(String key) {
        return Tools.asyncFuture(delegateMap.containsKey(key), executor);
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> pollLastEntry() {
        return Tools.asyncFuture(delegateMap.pollLastEntry(), executor);
    }

    @Override
    public CompletableFuture<String> lowerKey(String key) {
        return Tools.asyncFuture(delegateMap.lowerKey(key), executor);
    }

    @Override
    public CompletableFuture<Boolean> containsValue(V value) {
        return Tools.asyncFuture(delegateMap.containsValue(value), executor);
    }

    @Override
    public CompletableFuture<String> floorKey(String key) {
        return Tools.asyncFuture(delegateMap.floorKey(key), executor);
    }

    @Override
    public CompletableFuture<String> ceilingKey(String key) {
        return Tools.asyncFuture(delegateMap.ceilingKey(key), executor);
    }

    @Override
    public CompletableFuture<Versioned<V>> get(String key) {
        return Tools.asyncFuture(delegateMap.get(key), executor);
    }

    @Override
    public CompletableFuture<Versioned<V>> getOrDefault(String key, V defaultValue) {
        return Tools.asyncFuture(delegateMap.getOrDefault(key, defaultValue), executor);
    }

    @Override
    public CompletableFuture<String> higherKey(String key) {
        return Tools.asyncFuture(delegateMap.higherKey(key), executor);
    }

    @Override
    public CompletableFuture<NavigableSet<String>> navigableKeySet() {
        return Tools.asyncFuture(delegateMap.navigableKeySet(), executor);
    }

    @Override
    public CompletableFuture<NavigableMap<String, V>> subMap(
            String upperKey, String lowerKey, boolean inclusiveUpper, boolean inclusiveLower) {
        return Tools.asyncFuture(delegateMap.subMap(upperKey, lowerKey, inclusiveUpper, inclusiveLower), executor);
    }

    @Override
    public CompletableFuture<Versioned<V>> computeIf(
            String key, Predicate<? super V> condition,
            BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
        return Tools.asyncFuture(delegateMap.computeIf(key, condition, remappingFunction), executor);
    }

    @Override
    public CompletableFuture<Versioned<V>> put(String key, V value) {
        return Tools.asyncFuture(delegateMap.put(key, value), executor);
    }

    @Override
    public CompletableFuture<Versioned<V>> putAndGet(String key, V value) {
        return Tools.asyncFuture(delegateMap.putAndGet(key, value), executor);
    }

    @Override
    public CompletableFuture<Versioned<V>> remove(String key) {
        return Tools.asyncFuture(delegateMap.remove(key), executor);
    }

    @Override
    public CompletableFuture<Void> clear() {
        return Tools.asyncFuture(delegateMap.clear(), executor);
    }

    @Override
    public CompletableFuture<Set<String>> keySet() {
        return Tools.asyncFuture(delegateMap.keySet(), executor);
    }

    @Override
    public CompletableFuture<Collection<Versioned<V>>> values() {
        return Tools.asyncFuture(delegateMap.values(), executor);
    }

    @Override
    public CompletableFuture<Set<Map.Entry<String, Versioned<V>>>> entrySet() {
        return Tools.asyncFuture(delegateMap.entrySet(), executor);
    }

    @Override
    public CompletableFuture<Versioned<V>> putIfAbsent(String key, V value) {
        return Tools.asyncFuture(delegateMap.putIfAbsent(key, value), executor);
    }

    @Override
    public CompletableFuture<Boolean> remove(String key, V value) {
        return Tools.asyncFuture(delegateMap.remove(key, value), executor);
    }

    @Override
    public CompletableFuture<Boolean> remove(String key, long version) {
        return Tools.asyncFuture(delegateMap.remove(key, version), executor);
    }

    @Override
    public CompletableFuture<Versioned<V>> replace(String key, V value) {
        return Tools.asyncFuture(delegateMap.replace(key, value), executor);
    }

    @Override
    public CompletableFuture<Boolean> replace(String key, V oldValue, V newValue) {
        return Tools.asyncFuture(delegateMap.replace(key, oldValue, newValue), executor);
    }

    @Override
    public CompletableFuture<Boolean> replace(String key, long oldVersion, V newValue) {
        return Tools.asyncFuture(delegateMap.replace(key, oldVersion, newValue), executor);
    }

    @Override
    public CompletableFuture<Version> begin(TransactionId transactionId) {
        return Tools.asyncFuture(delegateMap.begin(transactionId), executor);
    }

    @Override
    public CompletableFuture<Boolean> prepare(TransactionLog<MapUpdate<String, V>> transactionLog) {
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
    public CompletableFuture<Boolean> prepareAndCommit(TransactionLog<MapUpdate<String, V>> transactionLog) {
        return Tools.asyncFuture(delegateMap.prepareAndCommit(transactionLog), executor);
    }

    @Override
    public CompletableFuture<Void> addListener(MapEventListener<String, V> listener) {
        return addListener(listener, executor);
    }

    @Override
    public CompletableFuture<Void> addListener(MapEventListener<String, V> listener, Executor executor) {
        return Tools.asyncFuture(delegateMap.addListener(listener, executor), this.executor);
    }

    @Override
    public CompletableFuture<Void> removeListener(MapEventListener<String, V> listener) {
        return Tools.asyncFuture(delegateMap.removeListener(listener), executor);
    }
}
