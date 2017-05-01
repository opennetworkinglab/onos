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

    public ExecutingAsyncConsistentTreeMap(
            AsyncConsistentTreeMap<V> delegateMap, Executor orderedExecutor, Executor threadPoolExecutor) {
        super(delegateMap, orderedExecutor, threadPoolExecutor);
        this.delegateMap = delegateMap;
    }

    @Override
    public CompletableFuture<String> firstKey() {
        return asyncFuture(delegateMap.firstKey());
    }

    @Override
    public CompletableFuture<String> lastKey() {
        return asyncFuture(delegateMap.lastKey());
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> ceilingEntry(String key) {
        return asyncFuture(delegateMap.ceilingEntry(key));
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> floorEntry(String key) {
        return asyncFuture(delegateMap.floorEntry(key));
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> higherEntry(String key) {
        return asyncFuture(delegateMap.higherEntry(key));
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> lowerEntry(String key) {
        return asyncFuture(delegateMap.lowerEntry(key));
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> firstEntry() {
        return asyncFuture(delegateMap.firstEntry());
    }

    @Override
    public CompletableFuture<Integer> size() {
        return asyncFuture(delegateMap.size());
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> lastEntry() {
        return asyncFuture(delegateMap.lastEntry());
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> pollFirstEntry() {
        return asyncFuture(delegateMap.pollFirstEntry());
    }

    @Override
    public CompletableFuture<Boolean> containsKey(String key) {
        return asyncFuture(delegateMap.containsKey(key));
    }

    @Override
    public CompletableFuture<Map.Entry<String, Versioned<V>>> pollLastEntry() {
        return asyncFuture(delegateMap.pollLastEntry());
    }

    @Override
    public CompletableFuture<String> lowerKey(String key) {
        return asyncFuture(delegateMap.lowerKey(key));
    }

    @Override
    public CompletableFuture<Boolean> containsValue(V value) {
        return asyncFuture(delegateMap.containsValue(value));
    }

    @Override
    public CompletableFuture<String> floorKey(String key) {
        return asyncFuture(delegateMap.floorKey(key));
    }

    @Override
    public CompletableFuture<String> ceilingKey(String key) {
        return asyncFuture(delegateMap.ceilingKey(key));
    }

    @Override
    public CompletableFuture<Versioned<V>> get(String key) {
        return asyncFuture(delegateMap.get(key));
    }

    @Override
    public CompletableFuture<Versioned<V>> getOrDefault(String key, V defaultValue) {
        return asyncFuture(delegateMap.getOrDefault(key, defaultValue));
    }

    @Override
    public CompletableFuture<String> higherKey(String key) {
        return asyncFuture(delegateMap.higherKey(key));
    }

    @Override
    public CompletableFuture<NavigableSet<String>> navigableKeySet() {
        return asyncFuture(delegateMap.navigableKeySet());
    }

    @Override
    public CompletableFuture<NavigableMap<String, V>> subMap(
            String upperKey, String lowerKey, boolean inclusiveUpper, boolean inclusiveLower) {
        return asyncFuture(delegateMap.subMap(upperKey, lowerKey, inclusiveUpper, inclusiveLower));
    }

    @Override
    public CompletableFuture<Versioned<V>> computeIf(
            String key, Predicate<? super V> condition,
            BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
        return asyncFuture(delegateMap.computeIf(key, condition, remappingFunction));
    }

    @Override
    public CompletableFuture<Versioned<V>> put(String key, V value) {
        return asyncFuture(delegateMap.put(key, value));
    }

    @Override
    public CompletableFuture<Versioned<V>> putAndGet(String key, V value) {
        return asyncFuture(delegateMap.putAndGet(key, value));
    }

    @Override
    public CompletableFuture<Versioned<V>> remove(String key) {
        return asyncFuture(delegateMap.remove(key));
    }

    @Override
    public CompletableFuture<Void> clear() {
        return asyncFuture(delegateMap.clear());
    }

    @Override
    public CompletableFuture<Set<String>> keySet() {
        return asyncFuture(delegateMap.keySet());
    }

    @Override
    public CompletableFuture<Collection<Versioned<V>>> values() {
        return asyncFuture(delegateMap.values());
    }

    @Override
    public CompletableFuture<Set<Map.Entry<String, Versioned<V>>>> entrySet() {
        return asyncFuture(delegateMap.entrySet());
    }

    @Override
    public CompletableFuture<Versioned<V>> putIfAbsent(String key, V value) {
        return asyncFuture(delegateMap.putIfAbsent(key, value));
    }

    @Override
    public CompletableFuture<Boolean> remove(String key, V value) {
        return asyncFuture(delegateMap.remove(key, value));
    }

    @Override
    public CompletableFuture<Boolean> remove(String key, long version) {
        return asyncFuture(delegateMap.remove(key, version));
    }

    @Override
    public CompletableFuture<Versioned<V>> replace(String key, V value) {
        return asyncFuture(delegateMap.replace(key, value));
    }

    @Override
    public CompletableFuture<Boolean> replace(String key, V oldValue, V newValue) {
        return asyncFuture(delegateMap.replace(key, oldValue, newValue));
    }

    @Override
    public CompletableFuture<Boolean> replace(String key, long oldVersion, V newValue) {
        return asyncFuture(delegateMap.replace(key, oldVersion, newValue));
    }

    @Override
    public CompletableFuture<Version> begin(TransactionId transactionId) {
        return asyncFuture(delegateMap.begin(transactionId));
    }

    @Override
    public CompletableFuture<Boolean> prepare(TransactionLog<MapUpdate<String, V>> transactionLog) {
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
    public CompletableFuture<Boolean> prepareAndCommit(TransactionLog<MapUpdate<String, V>> transactionLog) {
        return asyncFuture(delegateMap.prepareAndCommit(transactionLog));
    }

    @Override
    public CompletableFuture<Void> addListener(MapEventListener<String, V> listener) {
        return addListener(listener);
    }

    @Override
    public CompletableFuture<Void> addListener(MapEventListener<String, V> listener, Executor executor) {
        return asyncFuture(delegateMap.addListener(listener, executor));
    }

    @Override
    public CompletableFuture<Void> removeListener(MapEventListener<String, V> listener) {
        return asyncFuture(delegateMap.removeListener(listener));
    }
}
