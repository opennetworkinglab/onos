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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import com.google.common.collect.Maps;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.Change;
import org.onosproject.store.service.AsyncLeaderElector;

/**
 * {@link AsyncLeaderElector} that executes asynchronous callbacks on a user provided
 * {@link Executor}.
 */
public class ExecutingAsyncLeaderElector extends ExecutingDistributedPrimitive implements AsyncLeaderElector {
    private final AsyncLeaderElector delegateElector;
    private final Executor orderedExecutor;
    private final Map<Consumer<Change<Leadership>>, Consumer<Change<Leadership>>> listenerMap = Maps.newConcurrentMap();

    public ExecutingAsyncLeaderElector(
            AsyncLeaderElector delegateElector, Executor orderedExecutor, Executor threadPoolExecutor) {
        super(delegateElector, orderedExecutor, threadPoolExecutor);
        this.delegateElector = delegateElector;
        this.orderedExecutor = orderedExecutor;
    }

    @Override
    public CompletableFuture<Leadership> run(String topic, NodeId nodeId) {
        return asyncFuture(delegateElector.run(topic, nodeId));
    }

    @Override
    public CompletableFuture<Void> withdraw(String topic) {
        return asyncFuture(delegateElector.withdraw(topic));
    }

    @Override
    public CompletableFuture<Boolean> anoint(String topic, NodeId nodeId) {
        return asyncFuture(delegateElector.anoint(topic, nodeId));
    }

    @Override
    public CompletableFuture<Void> evict(NodeId nodeId) {
        return asyncFuture(delegateElector.evict(nodeId));
    }

    @Override
    public CompletableFuture<Boolean> promote(String topic, NodeId nodeId) {
        return asyncFuture(delegateElector.promote(topic, nodeId));
    }

    @Override
    public CompletableFuture<Leadership> getLeadership(String topic) {
        return asyncFuture(delegateElector.getLeadership(topic));
    }

    @Override
    public CompletableFuture<Map<String, Leadership>> getLeaderships() {
        return asyncFuture(delegateElector.getLeaderships());
    }

    @Override
    public CompletableFuture<Void> addChangeListener(Consumer<Change<Leadership>> listener) {
        Consumer<Change<Leadership>> wrappedListener = e -> orderedExecutor.execute(() -> listener.accept(e));
        listenerMap.put(listener, wrappedListener);
        return asyncFuture(delegateElector.addChangeListener(wrappedListener));
    }

    @Override
    public CompletableFuture<Void> removeChangeListener(Consumer<Change<Leadership>> listener) {
        Consumer<Change<Leadership>> wrappedListener = listenerMap.remove(listener);
        if (wrappedListener != null) {
            return asyncFuture(delegateElector.removeChangeListener(wrappedListener));
        }
        return CompletableFuture.completedFuture(null);
    }
}
