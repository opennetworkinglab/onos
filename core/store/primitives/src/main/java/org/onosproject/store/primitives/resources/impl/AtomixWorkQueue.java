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

import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import io.atomix.protocols.raft.proxy.RaftProxy;
import org.onlab.util.AbstractAccumulator;
import org.onlab.util.Accumulator;
import org.onlab.util.KryoNamespace;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueueOperations.Add;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueueOperations.Complete;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueueOperations.Take;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Task;
import org.onosproject.store.service.WorkQueue;
import org.onosproject.store.service.WorkQueueStats;
import org.slf4j.Logger;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.store.primitives.resources.impl.AtomixWorkQueueEvents.TASK_AVAILABLE;
import static org.onosproject.store.primitives.resources.impl.AtomixWorkQueueOperations.ADD;
import static org.onosproject.store.primitives.resources.impl.AtomixWorkQueueOperations.CLEAR;
import static org.onosproject.store.primitives.resources.impl.AtomixWorkQueueOperations.COMPLETE;
import static org.onosproject.store.primitives.resources.impl.AtomixWorkQueueOperations.REGISTER;
import static org.onosproject.store.primitives.resources.impl.AtomixWorkQueueOperations.STATS;
import static org.onosproject.store.primitives.resources.impl.AtomixWorkQueueOperations.TAKE;
import static org.onosproject.store.primitives.resources.impl.AtomixWorkQueueOperations.UNREGISTER;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Distributed resource providing the {@link WorkQueue} primitive.
 */
public class AtomixWorkQueue extends AbstractRaftPrimitive implements WorkQueue<byte[]> {
    private static final Serializer SERIALIZER = Serializer.using(KryoNamespace.newBuilder()
            .register(KryoNamespaces.BASIC)
            .register(AtomixWorkQueueOperations.NAMESPACE)
            .register(AtomixWorkQueueEvents.NAMESPACE)
            .build());

    private final Logger log = getLogger(getClass());
    private final ExecutorService executor = newSingleThreadExecutor(groupedThreads("AtomixWorkQueue", "%d", log));
    private final AtomicReference<TaskProcessor> taskProcessor = new AtomicReference<>();
    private final Timer timer = new Timer("atomix-work-queue-completer");
    private final AtomicBoolean isRegistered = new AtomicBoolean(false);

    public AtomixWorkQueue(RaftProxy proxy) {
        super(proxy);
        proxy.addStateChangeListener(state -> {
            if (state == RaftProxy.State.CONNECTED && isRegistered.get()) {
                proxy.invoke(REGISTER);
            }
        });
        proxy.addEventListener(TASK_AVAILABLE, this::resumeWork);
    }

    @Override
    public CompletableFuture<Void> destroy() {
        executor.shutdown();
        timer.cancel();
        return proxy.invoke(CLEAR);
    }

    @Override
    public CompletableFuture<Void> addMultiple(Collection<byte[]> items) {
        if (items.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return proxy.invoke(ADD, SERIALIZER::encode, new Add(items));
    }

    @Override
    public CompletableFuture<Collection<Task<byte[]>>> take(int maxTasks) {
        if (maxTasks <= 0) {
            return CompletableFuture.completedFuture(ImmutableList.of());
        }
        return proxy.invoke(TAKE, SERIALIZER::encode, new Take(maxTasks), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Void> complete(Collection<String> taskIds) {
        if (taskIds.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return proxy.invoke(COMPLETE, SERIALIZER::encode, new Complete(taskIds));
    }

    @Override
    public CompletableFuture<Void> registerTaskProcessor(Consumer<byte[]> callback,
            int parallelism,
            Executor executor) {
        Accumulator<String> completedTaskAccumulator =
                new CompletedTaskAccumulator(timer, 50, 50); // TODO: make configurable
        taskProcessor.set(new TaskProcessor(callback,
                parallelism,
                executor,
                completedTaskAccumulator));
        return register().thenCompose(v -> take(parallelism))
                .thenAccept(taskProcessor.get());
    }

    @Override
    public CompletableFuture<Void> stopProcessing() {
        return unregister();
    }

    @Override
    public CompletableFuture<WorkQueueStats> stats() {
        return proxy.invoke(STATS, SERIALIZER::decode);
    }

    private void resumeWork() {
        TaskProcessor activeProcessor = taskProcessor.get();
        if (activeProcessor == null) {
            return;
        }
        this.take(activeProcessor.headRoom())
                .whenCompleteAsync((tasks, e) -> activeProcessor.accept(tasks), executor);
    }

    private CompletableFuture<Void> register() {
        return proxy.invoke(REGISTER).thenRun(() -> isRegistered.set(true));
    }

    private CompletableFuture<Void> unregister() {
        return proxy.invoke(UNREGISTER).thenRun(() -> isRegistered.set(false));
    }

    // TaskId accumulator for paced triggering of task completion calls.
    private class CompletedTaskAccumulator extends AbstractAccumulator<String> {
        CompletedTaskAccumulator(Timer timer, int maxTasksToBatch, int maxBatchMillis) {
            super(timer, maxTasksToBatch, maxBatchMillis, Integer.MAX_VALUE);
        }

        @Override
        public void processItems(List<String> items) {
            complete(items);
        }
    }

    private class TaskProcessor implements Consumer<Collection<Task<byte[]>>> {

        private final AtomicInteger headRoom;
        private final Consumer<byte[]> backingConsumer;
        private final Executor executor;
        private final Accumulator<String> taskCompleter;

        public TaskProcessor(Consumer<byte[]> backingConsumer,
                int parallelism,
                Executor executor,
                Accumulator<String> taskCompleter) {
            this.backingConsumer = backingConsumer;
            this.headRoom = new AtomicInteger(parallelism);
            this.executor = executor;
            this.taskCompleter = taskCompleter;
        }

        public int headRoom() {
            return headRoom.get();
        }

        @Override
        public void accept(Collection<Task<byte[]>> tasks) {
            if (tasks == null) {
                return;
            }
            headRoom.addAndGet(-1 * tasks.size());
            tasks.forEach(task ->
                    executor.execute(() -> {
                        try {
                            backingConsumer.accept(task.payload());
                            taskCompleter.add(task.taskId());
                        } catch (Exception e) {
                            log.debug("Task execution failed", e);
                        } finally {
                            headRoom.incrementAndGet();
                            resumeWork();
                        }
                    }));
        }
    }
}