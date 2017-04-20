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

import com.google.common.collect.Multiset;
import org.onlab.util.Tools;
import org.onosproject.store.service.AsyncConsistentMultimap;
import org.onosproject.store.service.Versioned;

/**
 * {@link org.onosproject.store.service.AsyncConsistentMultimap} that executes asynchronous callbacks on a provided
 * {@link Executor}.
 */
public class ExecutingAsyncConsistentMultimap<K, V>
        extends ExecutingDistributedPrimitive implements AsyncConsistentMultimap<K, V> {
    private final AsyncConsistentMultimap<K, V> delegateMap;
    private final Executor executor;

    public ExecutingAsyncConsistentMultimap(AsyncConsistentMultimap<K, V> delegateMap, Executor executor) {
        super(delegateMap, executor);
        this.delegateMap = delegateMap;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Integer> size() {
        return Tools.asyncFuture(delegateMap.size(), executor);
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return Tools.asyncFuture(delegateMap.isEmpty(), executor);
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
    public CompletableFuture<Boolean> containsEntry(K key, V value) {
        return Tools.asyncFuture(delegateMap.containsEntry(key, value), executor);
    }

    @Override
    public CompletableFuture<Boolean> put(K key, V value) {
        return Tools.asyncFuture(delegateMap.put(key, value), executor);
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, V value) {
        return Tools.asyncFuture(delegateMap.remove(key, value), executor);
    }

    @Override
    public CompletableFuture<Boolean> removeAll(K key, Collection<? extends V> values) {
        return Tools.asyncFuture(delegateMap.removeAll(key, values), executor);
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends V>>> removeAll(K key) {
        return Tools.asyncFuture(delegateMap.removeAll(key), executor);
    }

    @Override
    public CompletableFuture<Boolean> putAll(K key, Collection<? extends V> values) {
        return Tools.asyncFuture(delegateMap.putAll(key, values), executor);
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends V>>> replaceValues(K key, Collection<V> values) {
        return Tools.asyncFuture(delegateMap.replaceValues(key, values), executor);
    }

    @Override
    public CompletableFuture<Void> clear() {
        return Tools.asyncFuture(delegateMap.clear(), executor);
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends V>>> get(K key) {
        return Tools.asyncFuture(delegateMap.get(key), executor);
    }

    @Override
    public CompletableFuture<Set<K>> keySet() {
        return Tools.asyncFuture(delegateMap.keySet(), executor);
    }

    @Override
    public CompletableFuture<Multiset<K>> keys() {
        return Tools.asyncFuture(delegateMap.keys(), executor);
    }

    @Override
    public CompletableFuture<Multiset<V>> values() {
        return Tools.asyncFuture(delegateMap.values(), executor);
    }

    @Override
    public CompletableFuture<Collection<Map.Entry<K, V>>> entries() {
        return Tools.asyncFuture(delegateMap.entries(), executor);
    }

    @Override
    public CompletableFuture<Map<K, Collection<V>>> asMap() {
        return Tools.asyncFuture(delegateMap.asMap(), executor);
    }
}
