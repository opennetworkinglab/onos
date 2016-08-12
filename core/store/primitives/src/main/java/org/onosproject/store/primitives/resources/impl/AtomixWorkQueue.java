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

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;
import io.atomix.copycat.client.CopycatClient;
import io.atomix.resource.AbstractResource;
import io.atomix.resource.ResourceTypeInfo;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.onlab.util.AbstractAccumulator;
import org.onlab.util.Accumulator;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueueCommands.Add;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueueCommands.Clear;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueueCommands.Complete;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueueCommands.Register;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueueCommands.Stats;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueueCommands.Take;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueueCommands.Unregister;
import org.onosproject.store.service.Task;
import org.onosproject.store.service.WorkQueue;
import org.onosproject.store.service.WorkQueueStats;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;

/**
 * Distributed resource providing the {@link WorkQueue} primitive.
 */
@ResourceTypeInfo(id = -154, factory = AtomixWorkQueueFactory.class)
public class AtomixWorkQueue extends AbstractResource<AtomixWorkQueue>
    implements WorkQueue<byte[]> {

    private final Logger log = getLogger(getClass());
    public static final String TASK_AVAILABLE = "task-available";
    private final ExecutorService executor = newSingleThreadExecutor(groupedThreads("AtomixWorkQueue", "%d", log));
    private final AtomicReference<TaskProcessor> taskProcessor = new AtomicReference<>();
    private final Timer timer = new Timer("atomix-work-queue-completer");
    private final AtomicBoolean isRegistered = new AtomicBoolean(false);

    protected AtomixWorkQueue(CopycatClient client, Properties options) {
        super(client, options);
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public CompletableFuture<Void> destroy() {
        executor.shutdown();
        timer.cancel();
        return client.submit(new Clear());
    }

    @Override
    public CompletableFuture<AtomixWorkQueue> open() {
        return super.open().thenApply(result -> {
            client.onStateChange(state -> {
                if (state == CopycatClient.State.CONNECTED && isRegistered.get()) {
                    client.submit(new Register());
                }
            });
            client.onEvent(TASK_AVAILABLE, this::resumeWork);
            return result;
        });
    }

    @Override
    public CompletableFuture<Void> addMultiple(Collection<byte[]> items) {
        if (items.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return client.submit(new Add(items));
    }

    @Override
    public CompletableFuture<Collection<Task<byte[]>>> take(int maxTasks) {
        if (maxTasks <= 0) {
            return CompletableFuture.completedFuture(ImmutableList.of());
        }
        return client.submit(new Take(maxTasks));
    }

    @Override
    public CompletableFuture<Void> complete(Collection<String> taskIds) {
        if (taskIds.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return client.submit(new Complete(taskIds));
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
        return client.submit(new Stats());
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
        return client.submit(new Register()).thenRun(() -> isRegistered.set(true));
    }

    private CompletableFuture<Void> unregister() {
        return client.submit(new Unregister()).thenRun(() -> isRegistered.set(false));
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
