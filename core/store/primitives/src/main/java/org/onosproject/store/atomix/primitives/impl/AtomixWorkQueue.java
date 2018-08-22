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

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.onosproject.store.service.Task;
import org.onosproject.store.service.WorkQueue;
import org.onosproject.store.service.WorkQueueStats;

import static org.onosproject.store.atomix.primitives.impl.AtomixFutures.adaptFuture;

/**
 * Atomix work queue.
 */
public class AtomixWorkQueue<E> implements WorkQueue<E> {
    private final io.atomix.core.workqueue.AsyncWorkQueue<E> atomixWorkQueue;

    public AtomixWorkQueue(io.atomix.core.workqueue.AsyncWorkQueue<E> atomixWorkQueue) {
        this.atomixWorkQueue = atomixWorkQueue;
    }

    @Override
    public String name() {
        return atomixWorkQueue.name();
    }

    @Override
    public CompletableFuture<Void> addMultiple(Collection<E> items) {
        return adaptFuture(atomixWorkQueue.addMultiple(items));
    }

    @Override
    public CompletableFuture<Collection<Task<E>>> take(int maxItems) {
        return adaptFuture(atomixWorkQueue.take(maxItems))
            .thenApply(tasks -> tasks.stream()
                .map(task -> new Task<>(task.taskId(), task.payload()))
                .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<Void> complete(Collection<String> taskIds) {
        return adaptFuture(atomixWorkQueue.complete(taskIds));
    }

    @Override
    public CompletableFuture<Void> registerTaskProcessor(
        Consumer<E> taskProcessor, int parallelism, Executor executor) {
        return adaptFuture(atomixWorkQueue.registerTaskProcessor(taskProcessor, parallelism, executor));
    }

    @Override
    public CompletableFuture<Void> stopProcessing() {
        return adaptFuture(atomixWorkQueue.stopProcessing());
    }

    @Override
    public CompletableFuture<WorkQueueStats> stats() {
        return adaptFuture(atomixWorkQueue.stats())
            .thenApply(stats -> WorkQueueStats.builder()
                .withTotalCompleted(stats.totalCompleted())
                .withTotalInProgress(stats.totalInProgress())
                .withTotalPending(stats.totalPending())
                .build());
    }

    @Override
    public CompletableFuture<Void> destroy() {
        return adaptFuture(atomixWorkQueue.delete());
    }
}
