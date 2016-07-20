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
package org.onosproject.store.primitives.resources.impl;

import io.atomix.variables.DistributedLong;

import java.util.concurrent.CompletableFuture;

import org.onosproject.store.service.AsyncAtomicCounter;

/**
 * {@code AsyncAtomicCounter} implementation backed by Atomix
 * {@link DistributedLong}.
 */
public class AtomixCounter implements AsyncAtomicCounter {

    private final String name;
    private final DistributedLong distLong;

    public AtomixCounter(String name, DistributedLong distLong) {
        this.name = name;
        this.distLong = distLong;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public CompletableFuture<Long> incrementAndGet() {
        return distLong.incrementAndGet();
    }

    @Override
    public CompletableFuture<Long> getAndIncrement() {
        return distLong.getAndIncrement();
    }

    @Override
    public CompletableFuture<Long> getAndAdd(long delta) {
        return distLong.getAndAdd(delta);
    }

    @Override
    public CompletableFuture<Long> addAndGet(long delta) {
        return distLong.addAndGet(delta);
    }

    @Override
    public CompletableFuture<Long> get() {
        return distLong.get();
    }

    @Override
    public CompletableFuture<Void> set(long value) {
        return distLong.set(value);
    }

    @Override
    public CompletableFuture<Boolean> compareAndSet(long expectedValue, long updateValue) {
        return distLong.compareAndSet(expectedValue, updateValue);
    }
}