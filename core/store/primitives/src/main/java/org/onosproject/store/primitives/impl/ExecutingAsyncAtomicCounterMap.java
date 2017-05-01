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

import org.onosproject.store.service.AsyncAtomicCounterMap;

/**
 * {@link org.onosproject.store.service.AsyncAtomicCounterMap} that executes asynchronous callbacks on a user provided
 * {@link Executor}.
 */
public class ExecutingAsyncAtomicCounterMap<K>
        extends ExecutingDistributedPrimitive implements AsyncAtomicCounterMap<K> {
    private final AsyncAtomicCounterMap<K> delegateMap;

    public ExecutingAsyncAtomicCounterMap(
            AsyncAtomicCounterMap<K> delegateMap, Executor orderedExecutor, Executor threadPoolExecutor) {
        super(delegateMap, orderedExecutor, threadPoolExecutor);
        this.delegateMap = delegateMap;
    }

    @Override
    public CompletableFuture<Long> incrementAndGet(K key) {
        return asyncFuture(delegateMap.incrementAndGet(key));
    }

    @Override
    public CompletableFuture<Long> decrementAndGet(K key) {
        return asyncFuture(delegateMap.decrementAndGet(key));
    }

    @Override
    public CompletableFuture<Long> getAndIncrement(K key) {
        return asyncFuture(delegateMap.getAndIncrement(key));
    }

    @Override
    public CompletableFuture<Long> getAndDecrement(K key) {
        return asyncFuture(delegateMap.getAndDecrement(key));
    }

    @Override
    public CompletableFuture<Long> addAndGet(K key, long delta) {
        return asyncFuture(delegateMap.addAndGet(key, delta));
    }

    @Override
    public CompletableFuture<Long> getAndAdd(K key, long delta) {
        return asyncFuture(delegateMap.getAndAdd(key, delta));
    }

    @Override
    public CompletableFuture<Long> get(K key) {
        return asyncFuture(delegateMap.get(key));
    }

    @Override
    public CompletableFuture<Long> put(K key, long newValue) {
        return asyncFuture(delegateMap.put(key, newValue));
    }

    @Override
    public CompletableFuture<Long> putIfAbsent(K key, long newValue) {
        return asyncFuture(delegateMap.putIfAbsent(key, newValue));
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, long expectedOldValue, long newValue) {
        return asyncFuture(delegateMap.replace(key, expectedOldValue, newValue));
    }

    @Override
    public CompletableFuture<Long> remove(K key) {
        return asyncFuture(delegateMap.remove(key));
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, long value) {
        return asyncFuture(delegateMap.remove(key, value));
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
    public CompletableFuture<Void> clear() {
        return asyncFuture(delegateMap.clear());
    }
}
