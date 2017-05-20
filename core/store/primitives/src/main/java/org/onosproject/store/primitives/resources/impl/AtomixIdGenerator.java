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
package org.onosproject.store.primitives.resources.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import io.atomix.variables.DistributedLong;
import org.onosproject.store.service.AsyncAtomicIdGenerator;

/**
 * {@code AsyncAtomicIdGenerator} implementation backed by Atomix
 * {@link DistributedLong}.
 */
public class AtomixIdGenerator implements AsyncAtomicIdGenerator {

    private static final long DEFAULT_BATCH_SIZE = 1000;
    private final String name;
    private final DistributedLong distLong;
    private final long batchSize;
    private CompletableFuture<Long> reserveFuture;
    private long base;
    private final AtomicLong delta = new AtomicLong();

    public AtomixIdGenerator(String name, DistributedLong distLong) {
        this(name, distLong, DEFAULT_BATCH_SIZE);
    }

    AtomixIdGenerator(String name, DistributedLong distLong, long batchSize) {
        this.name = name;
        this.distLong = distLong;
        this.batchSize = batchSize;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public synchronized CompletableFuture<Long> nextId() {
        long nextDelta = delta.incrementAndGet();
        if ((base == 0 && reserveFuture == null) || nextDelta > batchSize) {
            delta.set(0);
            long delta = this.delta.incrementAndGet();
            return reserve().thenApply(base -> base + delta);
        } else {
            return reserveFuture.thenApply(base -> base + nextDelta);
        }
    }

    private CompletableFuture<Long> reserve() {
        if (reserveFuture == null || reserveFuture.isDone()) {
            reserveFuture = distLong.getAndAdd(batchSize);
        } else {
            reserveFuture = reserveFuture.thenCompose(v -> distLong.getAndAdd(batchSize));
        }
        reserveFuture = reserveFuture.thenApply(base -> {
            this.base = base;
            return base;
        });
        return reserveFuture;
    }
}