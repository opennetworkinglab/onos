/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.store.atomix.primitives.impl;

import java.util.concurrent.CompletableFuture;

import org.onosproject.store.service.AsyncAtomicCounter;

import static org.onosproject.store.atomix.primitives.impl.AtomixFutures.adaptFuture;

/**
 * Atomix atomic counter.
 */
public class AtomixAtomicCounter implements AsyncAtomicCounter {
    private final io.atomix.core.counter.AsyncAtomicCounter atomixCounter;

    public AtomixAtomicCounter(io.atomix.core.counter.AsyncAtomicCounter atomixCounter) {
        this.atomixCounter = atomixCounter;
    }

    @Override
    public String name() {
        return atomixCounter.name();
    }

    @Override
    public CompletableFuture<Long> incrementAndGet() {
        return adaptFuture(atomixCounter.incrementAndGet());
    }

    @Override
    public CompletableFuture<Long> getAndIncrement() {
        return adaptFuture(atomixCounter.getAndIncrement());
    }

    @Override
    public CompletableFuture<Long> getAndAdd(long delta) {
        return adaptFuture(atomixCounter.getAndAdd(delta));
    }

    @Override
    public CompletableFuture<Long> addAndGet(long delta) {
        return adaptFuture(atomixCounter.addAndGet(delta));
    }

    @Override
    public CompletableFuture<Long> get() {
        return adaptFuture(atomixCounter.get());
    }

    @Override
    public CompletableFuture<Void> set(long value) {
        return adaptFuture(atomixCounter.set(value));
    }

    @Override
    public CompletableFuture<Boolean> compareAndSet(long expectedValue, long updateValue) {
        return adaptFuture(atomixCounter.compareAndSet(expectedValue, updateValue));
    }
}
