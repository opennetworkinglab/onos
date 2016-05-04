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

package org.onosproject.store.primitives.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.onosproject.core.ApplicationId;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.MapTransaction;
import org.onosproject.store.service.Versioned;

import com.google.common.base.MoreObjects;

/**
 * {@code AsyncConsistentMap} that merely delegates control to
 * another AsyncConsistentMap.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class DelegatingAsyncConsistentMap<K, V> implements AsyncConsistentMap<K, V> {

    private final AsyncConsistentMap<K, V> delegateMap;

    DelegatingAsyncConsistentMap(AsyncConsistentMap<K, V> delegateMap) {
        this.delegateMap = checkNotNull(delegateMap, "delegate map cannot be null");
    }

    @Override
    public String name() {
        return delegateMap.name();
    }

    @Override
    public ApplicationId applicationId() {
        return delegateMap.applicationId();
    }

    @Override
    public CompletableFuture<Integer> size() {
        return delegateMap.size();
    }

    @Override
    public CompletableFuture<Boolean> containsKey(K key) {
        return delegateMap.containsKey(key);
    }

    @Override
    public CompletableFuture<Boolean> containsValue(V value) {
        return delegateMap.containsValue(value);
    }

    @Override
    public CompletableFuture<Versioned<V>> get(K key) {
        return delegateMap.get(key);
    }

    @Override
    public CompletableFuture<Versioned<V>> computeIf(K key,
            Predicate<? super V> condition,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return delegateMap.computeIf(key, condition, remappingFunction);
    }

    @Override
    public CompletableFuture<Versioned<V>> put(K key, V value) {
        return delegateMap.put(key, value);
    }

    @Override
    public CompletableFuture<Versioned<V>> putAndGet(K key, V value) {
        return delegateMap.putAndGet(key, value);
    }

    @Override
    public CompletableFuture<Versioned<V>> remove(K key) {
        return delegateMap.remove(key);
    }

    @Override
    public CompletableFuture<Void> clear() {
        return delegateMap.clear();
    }

    @Override
    public CompletableFuture<Set<K>> keySet() {
        return delegateMap.keySet();
    }

    @Override
    public CompletableFuture<Collection<Versioned<V>>> values() {
        return delegateMap.values();
    }

    @Override
    public CompletableFuture<Set<Entry<K, Versioned<V>>>> entrySet() {
        return delegateMap.entrySet();
    }

    @Override
    public CompletableFuture<Versioned<V>> putIfAbsent(K key, V value) {
        return delegateMap.putIfAbsent(key, value);
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, V value) {
        return delegateMap.remove(key, value);
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, long version) {
        return delegateMap.remove(key, version);
    }

    @Override
    public CompletableFuture<Versioned<V>> replace(K key, V value) {
        return delegateMap.replace(key, value);
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, V oldValue, V newValue) {
        return delegateMap.replace(key, oldValue, newValue);
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, long oldVersion, V newValue) {
        return delegateMap.replace(key, oldVersion, newValue);
    }

    @Override
    public CompletableFuture<Void> addListener(MapEventListener<K, V> listener, Executor executor) {
        return delegateMap.addListener(listener, executor);
    }

    @Override
    public CompletableFuture<Void> removeListener(MapEventListener<K, V> listener) {
        return delegateMap.removeListener(listener);
    }

    @Override
    public CompletableFuture<Boolean> prepare(MapTransaction<K, V> transaction) {
        return delegateMap.prepare(transaction);
    }

    @Override
    public CompletableFuture<Void> commit(TransactionId transactionId) {
        return delegateMap.commit(transactionId);
    }

    @Override
    public CompletableFuture<Void> rollback(TransactionId transactionId) {
        return delegateMap.rollback(transactionId);
    }

    @Override
    public CompletableFuture<Boolean> prepareAndCommit(MapTransaction<K, V> transaction) {
        return delegateMap.prepareAndCommit(transaction);
    }

    @Override
    public void addStatusChangeListener(Consumer<Status> listener) {
        delegateMap.addStatusChangeListener(listener);
    }

    @Override
    public void removeStatusChangeListener(Consumer<Status> listener) {
        delegateMap.removeStatusChangeListener(listener);
    }

    @Override
    public Collection<Consumer<Status>> statusChangeListeners() {
        return delegateMap.statusChangeListeners();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                          .add("delegateMap", delegateMap)
                          .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegateMap);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof DelegatingAsyncConsistentMap) {
            DelegatingAsyncConsistentMap<K, V> that = (DelegatingAsyncConsistentMap) other;
            return this.delegateMap.equals(that.delegateMap);
        }
        return false;
    }
}
