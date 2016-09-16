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
package org.onosproject.pcelabelstore.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import org.onosproject.store.service.AsyncAtomicCounter;
import org.onosproject.store.service.AtomicCounterBuilder;

/**
 * Test implementation of atomic counter.
 */
public final class TestAtomicCounter implements AsyncAtomicCounter {
    final AtomicLong value;

    @Override
    public String name() {
        return null;
    }

    private TestAtomicCounter() {
        value = new AtomicLong();
    }

    @Override
    public CompletableFuture<Long> incrementAndGet() {
        return CompletableFuture.completedFuture(value.incrementAndGet());
    }

    @Override
    public CompletableFuture<Long> getAndIncrement() {
        return CompletableFuture.completedFuture(value.getAndIncrement());
    }

    @Override
    public CompletableFuture<Long> getAndAdd(long delta) {
        return CompletableFuture.completedFuture(value.getAndAdd(delta));
    }

    @Override
    public CompletableFuture<Long> addAndGet(long delta) {
        return CompletableFuture.completedFuture(value.addAndGet(delta));
    }

    @Override
    public CompletableFuture<Void> set(long value) {
        this.value.set(value);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> compareAndSet(long expectedValue, long updateValue) {
        return CompletableFuture.completedFuture(value.compareAndSet(expectedValue, updateValue));
    }

    @Override
    public CompletableFuture<Long> get() {
        return CompletableFuture.completedFuture(value.get());
    }

    public static AtomicCounterBuilder builder() {
        return new Builder();
    }

    public static class Builder extends AtomicCounterBuilder {
        @Override
        public AsyncAtomicCounter build() {
            return new TestAtomicCounter();
        }
    }
}
