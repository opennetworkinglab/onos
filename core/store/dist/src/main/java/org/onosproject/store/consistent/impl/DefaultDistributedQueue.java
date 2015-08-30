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

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;

import org.onlab.util.SharedExecutors;
import org.onosproject.store.service.DistributedQueue;
import org.onosproject.store.service.Serializer;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.store.consistent.impl.StateMachineUpdate.Target.QUEUE_PUSH;

/**
 * DistributedQueue implementation that provides FIFO ordering semantics.
 *
 * @param <E> queue entry type
 */
public class DefaultDistributedQueue<E>  implements DistributedQueue<E> {

    private final String name;
    private final Database database;
    private final Serializer serializer;
    private final Set<CompletableFuture<E>> pendingFutures = Sets.newIdentityHashSet();

    private static final String PRIMITIVE_NAME = "distributedQueue";
    private static final String SIZE = "size";
    private static final String PUSH = "push";
    private static final String POP = "pop";
    private static final String PEEK = "peek";

    private static final String ERROR_NULL_ENTRY = "Null entries are not allowed";
    private final MeteringAgent monitor;

    public DefaultDistributedQueue(String name,
                                   Database database,
                                   Serializer serializer,
                                   boolean meteringEnabled) {
        this.name = checkNotNull(name, "queue name cannot be null");
        this.database = checkNotNull(database, "database cannot be null");
        this.serializer = checkNotNull(serializer, "serializer cannot be null");
        this.monitor = new MeteringAgent(PRIMITIVE_NAME, name, meteringEnabled);
        this.database.registerConsumer(update -> {
            SharedExecutors.getSingleThreadExecutor().execute(() -> {
                if (update.target() == QUEUE_PUSH) {
                    List<Object> input = update.input();
                    String queueName = (String) input.get(0);
                    if (queueName.equals(name)) {
                        tryPoll();
                    }
                }
            });
        });
    }

    @Override
    public long size() {
        final MeteringAgent.Context timer = monitor.startTimer(SIZE);
        return Futures.getUnchecked(database.queueSize(name).whenComplete((r, e) -> timer.stop(e)));
    }

    @Override
    public void push(E entry) {
        checkNotNull(entry, ERROR_NULL_ENTRY);
        final MeteringAgent.Context timer = monitor.startTimer(PUSH);
        Futures.getUnchecked(database.queuePush(name, serializer.encode(entry))
                                     .whenComplete((r, e) -> timer.stop(e)));
    }

    @Override
    public CompletableFuture<E> pop() {
        final MeteringAgent.Context timer = monitor.startTimer(POP);
        return database.queuePop(name)
                       .whenComplete((r, e) -> timer.stop(e))
                       .thenCompose(v -> {
                           if (v != null) {
                               return CompletableFuture.<E>completedFuture(serializer.decode(v));
                           }
                           CompletableFuture<E> newPendingFuture = new CompletableFuture<>();
                           pendingFutures.add(newPendingFuture);
                           return newPendingFuture;
                       });

    }

    @Override
    public E peek() {
        final MeteringAgent.Context timer = monitor.startTimer(PEEK);
        return Futures.getUnchecked(database.queuePeek(name)
                                            .thenApply(v -> v != null ? serializer.<E>decode(v) : null)
                                            .whenComplete((r, e) -> timer.stop(e)));
    }

    public String name() {
        return name;
    }

    protected void tryPoll() {
        Set<CompletableFuture<E>> completedFutures = Sets.newHashSet();
        for (CompletableFuture<E> future : pendingFutures) {
            E entry = Futures.getUnchecked(database.queuePop(name)
                                                   .thenApply(v -> v != null ? serializer.decode(v) : null));
            if (entry != null) {
                future.complete(entry);
                completedFutures.add(future);
            } else {
                break;
            }
        }
        pendingFutures.removeAll(completedFutures);
    }
}