package org.onosproject.store.primitives.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Task;
import org.onosproject.store.service.WorkQueue;
import org.onosproject.store.service.WorkQueueStats;

import com.google.common.collect.Collections2;

/**
 * Default implementation of {@link WorkQueue}.
 *
 * @param <E> task payload type.
 */
public class DefaultDistributedWorkQueue<E> implements WorkQueue<E> {

    private final WorkQueue<byte[]> backingQueue;
    private final Serializer serializer;

    public DefaultDistributedWorkQueue(WorkQueue<byte[]> backingQueue, Serializer serializer) {
        this.backingQueue = backingQueue;
        this.serializer = serializer;
    }

    @Override
    public String name() {
        return backingQueue.name();
    }

    @Override
    public CompletableFuture<Void> addMultiple(Collection<E> items) {
        return backingQueue.addMultiple(items.stream()
                                             .map(serializer::encode)
                                             .collect(Collectors.toCollection(ArrayList::new)));
    }

    private Collection<Task<E>> decodeCollection(Collection<Task<byte[]>> tasks) {
        return Collections2.transform(tasks, task -> task.map(serializer::decode));
    }

    @Override
    public CompletableFuture<Collection<Task<E>>> take(int maxTasks) {
        return backingQueue.take(maxTasks)
                           .thenApply(this::decodeCollection);
    }

    @Override
    public CompletableFuture<Void> complete(Collection<String> ids) {
        return backingQueue.complete(ids);
    }

    @Override
    public CompletableFuture<WorkQueueStats> stats() {
        return backingQueue.stats();
    }

    @Override
    public CompletableFuture<Void> registerTaskProcessor(Consumer<E> callback,
                                                         int parallelism,
                                                         Executor executor) {
        Consumer<byte[]> backingQueueCallback = payload -> callback.accept(serializer.decode(payload));
        return backingQueue.registerTaskProcessor(backingQueueCallback, parallelism, executor);
    }

    @Override
    public CompletableFuture<Void> stopProcessing() {
        return backingQueue.stopProcessing();
    }

    @Override
    public CompletableFuture<Void> destroy() {
        return backingQueue.destroy();
    }
}
