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
import org.onosproject.cluster.NodeId;
import org.onosproject.store.service.DistributedQueue;
import org.onosproject.store.service.Serializer;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * DistributedQueue implementation that provides FIFO ordering semantics.
 *
 * @param <E> queue entry type
 */
public class DefaultDistributedQueue<E>  implements DistributedQueue<E> {

    private final String name;
    private final Database database;
    private final Serializer serializer;
    private final NodeId localNodeId;
    private final Set<CompletableFuture<E>> pendingFutures = Sets.newIdentityHashSet();
    private final Consumer<Set<NodeId>> notifyConsumers;

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
                                   NodeId localNodeId,
                                   boolean meteringEnabled,
                                   Consumer<Set<NodeId>> notifyConsumers) {
        this.name = checkNotNull(name, "queue name cannot be null");
        this.database = checkNotNull(database, "database cannot be null");
        this.serializer = checkNotNull(serializer, "serializer cannot be null");
        this.localNodeId = localNodeId;
        this.notifyConsumers = notifyConsumers;
        this.monitor = new MeteringAgent(PRIMITIVE_NAME, name, meteringEnabled);

    }

    @Override
    public long size() {
        final MeteringAgent.Context timer = monitor.startTimer(SIZE);
        try {
            return Futures.getUnchecked(database.queueSize(name));
        } finally {
            timer.stop();
        }
    }

    @Override
    public void push(E entry) {
        final MeteringAgent.Context timer = monitor.startTimer(PUSH);
        try {
            checkNotNull(entry, ERROR_NULL_ENTRY);
            Futures.getUnchecked(database.queuePush(name, serializer.encode(entry))
                                         .thenAccept(notifyConsumers)
                                         .thenApply(v -> null));
        } finally {
            timer.stop();
        }
    }

    @Override
    public CompletableFuture<E> pop() {
        final MeteringAgent.Context timer = monitor.startTimer(POP);
        return database.queuePop(name, localNodeId)
                       .thenCompose(v -> {
                           if (v != null) {
                               return CompletableFuture.completedFuture(serializer.decode(v));
                           } else {
                               CompletableFuture<E> newPendingFuture = new CompletableFuture<>();
                               pendingFutures.add(newPendingFuture);
                               return newPendingFuture;
                           }
                       })
                .whenComplete((r, e) -> timer.stop());
    }

    @Override
    public E peek() {
        final MeteringAgent.Context timer = monitor.startTimer(PEEK);
        try {
            return Futures.getUnchecked(database.queuePeek(name)
                                                .thenApply(v -> v != null ? serializer.decode(v) : null));
        } finally {
            timer.stop();
        }
    }

    public String name() {
        return name;
    }

    protected void tryPoll() {
        Set<CompletableFuture<E>> completedFutures = Sets.newHashSet();
        for (CompletableFuture<E> future : pendingFutures) {
            E entry = Futures.getUnchecked(database.queuePop(name, localNodeId)
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