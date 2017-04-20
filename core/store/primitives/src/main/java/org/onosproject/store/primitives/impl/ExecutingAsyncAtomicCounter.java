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
import org.onosproject.store.service.AsyncAtomicCounter;

/**
 * {@link AsyncAtomicCounter} that executes asynchronous callbacks on a user provided
 * {@link Executor}.
 */
public class ExecutingAsyncAtomicCounter extends ExecutingDistributedPrimitive implements AsyncAtomicCounter {
    private final AsyncAtomicCounter delegateCounter;
    private final Executor executor;

    public ExecutingAsyncAtomicCounter(AsyncAtomicCounter delegateCounter, Executor executor) {
        super(delegateCounter, executor);
        this.delegateCounter = delegateCounter;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Long> incrementAndGet() {
        return Tools.asyncFuture(delegateCounter.incrementAndGet(), executor);
    }

    @Override
    public CompletableFuture<Long> getAndIncrement() {
        return Tools.asyncFuture(delegateCounter.getAndIncrement(), executor);
    }

    @Override
    public CompletableFuture<Long> getAndAdd(long delta) {
        return Tools.asyncFuture(delegateCounter.getAndAdd(delta), executor);
    }

    @Override
    public CompletableFuture<Long> addAndGet(long delta) {
        return Tools.asyncFuture(delegateCounter.addAndGet(delta), executor);
    }

    @Override
    public CompletableFuture<Long> get() {
        return Tools.asyncFuture(delegateCounter.get(), executor);
    }

    @Override
    public CompletableFuture<Void> set(long value) {
        return Tools.asyncFuture(delegateCounter.set(value), executor);
    }

    @Override
    public CompletableFuture<Boolean> compareAndSet(long expectedValue, long updateValue) {
        return Tools.asyncFuture(delegateCounter.compareAndSet(expectedValue, updateValue), executor);
    }
}
