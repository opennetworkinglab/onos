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

import org.onosproject.store.service.AsyncAtomicCounter;

/**
 * {@link AsyncAtomicCounter} that executes asynchronous callbacks on a user provided
 * {@link Executor}.
 */
public class ExecutingAsyncAtomicCounter extends ExecutingDistributedPrimitive implements AsyncAtomicCounter {
    private final AsyncAtomicCounter delegateCounter;

    public ExecutingAsyncAtomicCounter(
            AsyncAtomicCounter delegateCounter, Executor orderedExecutor, Executor threadPoolExecutor) {
        super(delegateCounter, orderedExecutor, threadPoolExecutor);
        this.delegateCounter = delegateCounter;
    }

    @Override
    public CompletableFuture<Long> incrementAndGet() {
        return asyncFuture(delegateCounter.incrementAndGet());
    }

    @Override
    public CompletableFuture<Long> getAndIncrement() {
        return asyncFuture(delegateCounter.getAndIncrement());
    }

    @Override
    public CompletableFuture<Long> getAndAdd(long delta) {
        return asyncFuture(delegateCounter.getAndAdd(delta));
    }

    @Override
    public CompletableFuture<Long> addAndGet(long delta) {
        return asyncFuture(delegateCounter.addAndGet(delta));
    }

    @Override
    public CompletableFuture<Long> get() {
        return asyncFuture(delegateCounter.get());
    }

    @Override
    public CompletableFuture<Void> set(long value) {
        return asyncFuture(delegateCounter.set(value));
    }

    @Override
    public CompletableFuture<Boolean> compareAndSet(long expectedValue, long updateValue) {
        return asyncFuture(delegateCounter.compareAndSet(expectedValue, updateValue));
    }
}
