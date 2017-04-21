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

package org.onosproject.store.service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;

/**
 * Testing adapter for the AsyncConsistenMap interface.
 */
public class AsyncConsistentMapAdapter<K, V> implements AsyncConsistentMap<K, V> {
    @Override
    public CompletableFuture<Integer> size() {
        return null;
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> containsKey(K key) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> containsValue(V value) {
        return null;
    }

    @Override
    public CompletableFuture<Versioned<V>> get(K key) {
        return null;
    }

    @Override
    public CompletableFuture<Versioned<V>> getOrDefault(K key, V defaultValue) {
        return null;
    }

    @Override
    public CompletableFuture<Versioned<V>>
    computeIf(K key, Predicate<? super V> condition,
              BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return null;
    }

    @Override
    public CompletableFuture<Versioned<V>> put(K key, V value) {
        return null;
    }

    @Override
    public CompletableFuture<Versioned<V>> putAndGet(K key, V value) {
        return null;
    }

    @Override
    public CompletableFuture<Versioned<V>> remove(K key) {
        return null;
    }

    @Override
    public CompletableFuture<Void> clear() {
        return null;
    }

    @Override
    public CompletableFuture<Set<K>> keySet() {
        return null;
    }

    @Override
    public CompletableFuture<Collection<Versioned<V>>> values() {
        return null;
    }

    @Override
    public CompletableFuture<Set<Map.Entry<K, Versioned<V>>>> entrySet() {
        return null;
    }

    @Override
    public CompletableFuture<Versioned<V>> putIfAbsent(K key, V value) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, V value) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, long version) {
        return null;
    }

    @Override
    public CompletableFuture<Versioned<V>> replace(K key, V value) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, V oldValue, V newValue) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, long oldVersion, V newValue) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addListener(MapEventListener<K, V> listener, Executor executor) {
        return null;
    }

    @Override
    public CompletableFuture<Void> removeListener(MapEventListener<K, V> listener) {
        return null;
    }

    @Override
    public CompletableFuture<Version> begin(TransactionId transactionId) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> prepare(TransactionLog<MapUpdate<K, V>> transactionLog) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> prepareAndCommit(TransactionLog<MapUpdate<K, V>> transactionLog) {
        return null;
    }

    @Override
    public CompletableFuture<Void> commit(TransactionId transactionId) {
        return null;
    }

    @Override
    public CompletableFuture<Void> rollback(TransactionId transactionId) {
        return null;
    }
}

