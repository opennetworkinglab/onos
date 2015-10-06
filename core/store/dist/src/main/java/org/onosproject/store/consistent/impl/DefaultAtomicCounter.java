/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.consistent.impl;

import org.onosproject.store.service.AsyncAtomicCounter;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.StorageException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Default implementation for a distributed AtomicCounter backed by
 * partitioned Raft DB.
 * <p>
 * The initial value will be zero.
 */
public class DefaultAtomicCounter implements AtomicCounter {

    private static final int OPERATION_TIMEOUT_MILLIS = 5000;

    private final AsyncAtomicCounter asyncCounter;

    public DefaultAtomicCounter(String name,
                                Database database,
                                boolean meteringEnabled) {
        asyncCounter = new DefaultAsyncAtomicCounter(name, database, meteringEnabled);
    }

    @Override
    public long incrementAndGet() {
        return complete(asyncCounter.incrementAndGet());
    }

    @Override
    public long getAndIncrement() {
        return complete(asyncCounter.getAndIncrement());
    }

    @Override
    public long getAndAdd(long delta) {
        return complete(asyncCounter.getAndAdd(delta));
    }

    @Override
    public long addAndGet(long delta) {
        return complete(asyncCounter.getAndAdd(delta));
    }

    @Override
    public void set(long value) {
        complete(asyncCounter.set(value));
    }

    @Override
    public boolean compareAndSet(long expectedValue, long updateValue) {
        return complete(asyncCounter.compareAndSet(expectedValue, updateValue));
    }

    @Override
    public long get() {
        return complete(asyncCounter.get());
    }

    private static <T> T complete(CompletableFuture<T> future) {
        try {
            return future.get(OPERATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StorageException.Interrupted();
        } catch (TimeoutException e) {
            throw new StorageException.Timeout();
        } catch (ExecutionException e) {
            throw new StorageException(e.getCause());
        }
    }
}
