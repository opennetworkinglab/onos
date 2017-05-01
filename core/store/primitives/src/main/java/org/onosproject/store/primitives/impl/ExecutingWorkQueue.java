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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.onosproject.store.service.AsyncAtomicValue;
import org.onosproject.store.service.Task;
import org.onosproject.store.service.WorkQueue;
import org.onosproject.store.service.WorkQueueStats;

/**
 * {@link AsyncAtomicValue} that executes asynchronous callbacks on a user provided
 * {@link Executor}.
 */
public class ExecutingWorkQueue<E> extends ExecutingDistributedPrimitive implements WorkQueue<E> {
    private final WorkQueue<E> delegateQueue;

    public ExecutingWorkQueue(WorkQueue<E> delegateQueue, Executor orderedExecutor, Executor threadPoolExecutor) {
        super(delegateQueue, orderedExecutor, threadPoolExecutor);
        this.delegateQueue = delegateQueue;
    }

    @Override
    public CompletableFuture<Void> addMultiple(Collection<E> items) {
        return asyncFuture(delegateQueue.addMultiple(items));
    }

    @Override
    public CompletableFuture<Collection<Task<E>>> take(int maxItems) {
        return asyncFuture(delegateQueue.take(maxItems));
    }

    @Override
    public CompletableFuture<Void> complete(Collection<String> taskIds) {
        return asyncFuture(delegateQueue.complete(taskIds));
    }

    @Override
    public CompletableFuture<Void> registerTaskProcessor(
            Consumer<E> taskProcessor, int parallelism, Executor executor) {
        return asyncFuture(delegateQueue.registerTaskProcessor(taskProcessor, parallelism, executor));
    }

    @Override
    public CompletableFuture<Void> stopProcessing() {
        return asyncFuture(delegateQueue.stopProcessing());
    }

    @Override
    public CompletableFuture<WorkQueueStats> stats() {
        return asyncFuture(delegateQueue.stats());
    }
}
