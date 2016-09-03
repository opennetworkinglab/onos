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

import static org.slf4j.LoggerFactory.getLogger;
import io.atomix.copycat.server.Commit;
import io.atomix.copycat.server.Snapshottable;
import io.atomix.copycat.server.StateMachineExecutor;
import io.atomix.copycat.server.session.ServerSession;
import io.atomix.copycat.server.session.SessionListener;
import io.atomix.copycat.server.storage.snapshot.SnapshotReader;
import io.atomix.copycat.server.storage.snapshot.SnapshotWriter;
import io.atomix.resource.ResourceStateMachine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.onlab.util.CountDownCompleter;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueueCommands.Add;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueueCommands.Clear;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueueCommands.Complete;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueueCommands.Register;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueueCommands.Stats;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueueCommands.Take;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueueCommands.Unregister;
import org.onosproject.store.service.Task;
import org.onosproject.store.service.WorkQueueStats;
import org.slf4j.Logger;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.AtomicLongMap;

/**
 * State machine for {@link AtomixWorkQueue} resource.
 */
public class AtomixWorkQueueState  extends ResourceStateMachine implements SessionListener, Snapshottable {

    private final Logger log = getLogger(getClass());

    private final AtomicLong totalCompleted = new AtomicLong(0);

    private final Queue<TaskHolder> unassignedTasks = Queues.newArrayDeque();
    private final Map<String, TaskAssignment> assignments = Maps.newHashMap();
    private final Map<Long, Commit<? extends Register>> registeredWorkers = Maps.newHashMap();
    private final AtomicLongMap<Long> activeTasksPerSession = AtomicLongMap.create();

    protected AtomixWorkQueueState(Properties config) {
        super(config);
    }

    @Override
    protected void configure(StateMachineExecutor executor) {
        executor.register(Stats.class, this::stats);
        executor.register(Register.class, (Consumer<Commit<Register>>) this::register);
        executor.register(Unregister.class, (Consumer<Commit<Unregister>>) this::unregister);
        executor.register(Add.class, (Consumer<Commit<Add>>) this::add);
        executor.register(Take.class, this::take);
        executor.register(Complete.class, (Consumer<Commit<Complete>>) this::complete);
        executor.register(Clear.class, (Consumer<Commit<Clear>>) this::clear);
    }

    protected WorkQueueStats stats(Commit<? extends Stats> commit) {
        try {
            return WorkQueueStats.builder()
                    .withTotalCompleted(totalCompleted.get())
                    .withTotalPending(unassignedTasks.size())
                    .withTotalInProgress(assignments.size())
                    .build();
        } finally {
            commit.close();
        }
    }

    protected void clear(Commit<? extends Clear> commit) {
        try {
            unassignedTasks.forEach(TaskHolder::complete);
            unassignedTasks.clear();
            assignments.values().forEach(TaskAssignment::markComplete);
            assignments.clear();
            registeredWorkers.values().forEach(Commit::close);
            registeredWorkers.clear();
            activeTasksPerSession.clear();
            totalCompleted.set(0);
        } finally {
            commit.close();
        }
    }

    protected void register(Commit<? extends Register> commit) {
        long sessionId = commit.session().id();
        if (registeredWorkers.putIfAbsent(sessionId, commit) != null) {
            commit.close();
        }
    }

    protected void unregister(Commit<? extends Unregister> commit) {
        try {
            Commit<? extends Register> registerCommit = registeredWorkers.remove(commit.session().id());
            if (registerCommit != null) {
                registerCommit.close();
            }
        } finally {
            commit.close();
        }
    }

    protected void add(Commit<? extends Add> commit) {
        Collection<byte[]> items = commit.operation().items();

        // Create a CountDownCompleter that will close the commit when all tasks
        // submitted as part of it are completed.
        CountDownCompleter<Commit<? extends Add>> referenceTracker =
                new CountDownCompleter<>(commit, items.size(), Commit::close);

        AtomicInteger itemIndex = new AtomicInteger(0);
        items.forEach(item -> {
            String taskId = String.format("%d:%d:%d", commit.session().id(),
                                                      commit.index(),
                                                      itemIndex.getAndIncrement());
            unassignedTasks.add(new TaskHolder(new Task<>(taskId, item), referenceTracker));
        });

        // Send an event to all sessions that have expressed interest in task processing
        // and are not actively processing a task.
        registeredWorkers.values()
                         .stream()
                         .map(Commit::session)
                         .forEach(session -> session.publish(AtomixWorkQueue.TASK_AVAILABLE));
        // FIXME: This generates a lot of event traffic.
    }

    protected Collection<Task<byte[]>> take(Commit<? extends Take> commit) {
        try {
            if (unassignedTasks.isEmpty()) {
                return ImmutableList.of();
            }
            long sessionId = commit.session().id();
            int maxTasks = commit.operation().maxTasks();
            return IntStream.range(0, Math.min(maxTasks, unassignedTasks.size()))
                            .mapToObj(i -> {
                                TaskHolder holder = unassignedTasks.poll();
                                String taskId = holder.task().taskId();
                                TaskAssignment assignment = new TaskAssignment(sessionId, holder);

                                // bookkeeping
                                assignments.put(taskId, assignment);
                                activeTasksPerSession.incrementAndGet(sessionId);

                                return holder.task();
                            })
                            .collect(Collectors.toCollection(ArrayList::new));
        } catch (Exception e) {
            log.warn("State machine update failed", e);
            throw Throwables.propagate(e);
        } finally {
            commit.close();
        }
    }

    protected void complete(Commit<? extends Complete> commit) {
        long sessionId = commit.session().id();
        try {
            commit.operation().taskIds().forEach(taskId -> {
                TaskAssignment assignment = assignments.get(taskId);
                if (assignment != null && assignment.sessionId() == sessionId) {
                    assignments.remove(taskId).markComplete();
                    // bookkeeping
                    totalCompleted.incrementAndGet();
                    activeTasksPerSession.decrementAndGet(sessionId);
                }
            });
        } catch (Exception e) {
            log.warn("State machine update failed", e);
            throw Throwables.propagate(e);
        } finally {
            commit.close();
        }
    }

    @Override
    public void register(ServerSession session) {
    }

    @Override
    public void unregister(ServerSession session) {
        evictWorker(session.id());
    }

    @Override
    public void expire(ServerSession session) {
        evictWorker(session.id());
    }

    @Override
    public void close(ServerSession session) {
        evictWorker(session.id());
    }

    @Override
    public void snapshot(SnapshotWriter writer) {
        writer.writeLong(totalCompleted.get());
    }

    @Override
    public void install(SnapshotReader reader) {
        totalCompleted.set(reader.readLong());
    }

    private void evictWorker(long sessionId) {
        Commit<? extends Register> commit = registeredWorkers.remove(sessionId);
        if (commit != null) {
            commit.close();
        }

        // TODO: Maintain an index of tasks by session for efficient access.
        Iterator<Map.Entry<String, TaskAssignment>> iter = assignments.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, TaskAssignment> entry = iter.next();
            TaskAssignment assignment = entry.getValue();
            if (assignment.sessionId() == sessionId) {
                unassignedTasks.add(assignment.taskHolder());
                iter.remove();
            }
        }

        // Bookkeeping
        activeTasksPerSession.remove(sessionId);
        activeTasksPerSession.removeAllZeros();
    }

    private class TaskHolder {

        private final Task<byte[]> task;
        private final CountDownCompleter<Commit<? extends Add>> referenceTracker;

        public TaskHolder(Task<byte[]> delegate, CountDownCompleter<Commit<? extends Add>> referenceTracker) {
            this.task = delegate;
            this.referenceTracker = referenceTracker;
        }

        public Task<byte[]> task() {
            return task;
        }

        public void complete() {
            referenceTracker.countDown();
        }
    }

    private class TaskAssignment {
        private final long sessionId;
        private final TaskHolder taskHolder;

        public TaskAssignment(long sessionId, TaskHolder taskHolder) {
            this.sessionId = sessionId;
            this.taskHolder = taskHolder;
        }

        public long sessionId() {
            return sessionId;
        }

        public TaskHolder taskHolder() {
            return taskHolder;
        }

        public void markComplete() {
            taskHolder.complete();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                              .add("sessionId", sessionId)
                              .add("taskHolder", taskHolder)
                              .toString();
        }
    }
}
