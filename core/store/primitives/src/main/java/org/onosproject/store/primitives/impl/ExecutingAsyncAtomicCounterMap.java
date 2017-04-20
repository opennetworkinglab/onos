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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.onlab.util.Tools;
import org.onosproject.store.service.AsyncAtomicCounterMap;

/**
 * {@link org.onosproject.store.service.AsyncAtomicCounterMap} that executes asynchronous callbacks on a user provided
 * {@link Executor}.
 */
public class ExecutingAsyncAtomicCounterMap<K>
        extends ExecutingDistributedPrimitive implements AsyncAtomicCounterMap<K> {
    private final AsyncAtomicCounterMap<K> delegateMap;
    private final Executor executor;

    public ExecutingAsyncAtomicCounterMap(AsyncAtomicCounterMap<K> delegateMap, Executor executor) {
        super(delegateMap, executor);
        this.delegateMap = delegateMap;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Long> incrementAndGet(K key) {
        return Tools.asyncFuture(delegateMap.incrementAndGet(key), executor);
    }

    @Override
    public CompletableFuture<Long> decrementAndGet(K key) {
        return Tools.asyncFuture(delegateMap.decrementAndGet(key), executor);
    }

    @Override
    public CompletableFuture<Long> getAndIncrement(K key) {
        return Tools.asyncFuture(delegateMap.getAndIncrement(key), executor);
    }

    @Override
    public CompletableFuture<Long> getAndDecrement(K key) {
        return Tools.asyncFuture(delegateMap.getAndDecrement(key), executor);
    }

    @Override
    public CompletableFuture<Long> addAndGet(K key, long delta) {
        return Tools.asyncFuture(delegateMap.addAndGet(key, delta), executor);
    }

    @Override
    public CompletableFuture<Long> getAndAdd(K key, long delta) {
        return Tools.asyncFuture(delegateMap.getAndAdd(key, delta), executor);
    }

    @Override
    public CompletableFuture<Long> get(K key) {
        return Tools.asyncFuture(delegateMap.get(key), executor);
    }

    @Override
    public CompletableFuture<Long> put(K key, long newValue) {
        return Tools.asyncFuture(delegateMap.put(key, newValue), executor);
    }

    @Override
    public CompletableFuture<Long> putIfAbsent(K key, long newValue) {
        return Tools.asyncFuture(delegateMap.putIfAbsent(key, newValue), executor);
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, long expectedOldValue, long newValue) {
        return Tools.asyncFuture(delegateMap.replace(key, expectedOldValue, newValue), executor);
    }

    @Override
    public CompletableFuture<Long> remove(K key) {
        return Tools.asyncFuture(delegateMap.remove(key), executor);
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, long value) {
        return Tools.asyncFuture(delegateMap.remove(key, value), executor);
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
    public CompletableFuture<Void> clear() {
        return Tools.asyncFuture(delegateMap.clear(), executor);
    }
}
