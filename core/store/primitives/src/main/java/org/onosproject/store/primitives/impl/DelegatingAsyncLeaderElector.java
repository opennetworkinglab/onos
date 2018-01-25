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
package org.onosproject.store.primitives.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.Change;
import org.onosproject.store.service.AsyncLeaderElector;

/**
 * Delegating leader elector.
 */
public class DelegatingAsyncLeaderElector extends DelegatingDistributedPrimitive implements AsyncLeaderElector {

    private final AsyncLeaderElector delegateLeaderElector;

    public DelegatingAsyncLeaderElector(AsyncLeaderElector delegateLeaderElector) {
        super(delegateLeaderElector);
        this.delegateLeaderElector = delegateLeaderElector;
    }

    @Override
    public CompletableFuture<Leadership> run(String topic, NodeId nodeId) {
        return delegateLeaderElector.run(topic, nodeId);
    }

    @Override
    public CompletableFuture<Void> withdraw(String topic) {
        return delegateLeaderElector.withdraw(topic);
    }

    @Override
    public CompletableFuture<Boolean> anoint(String topic, NodeId nodeId) {
        return delegateLeaderElector.anoint(topic, nodeId);
    }

    @Override
    public CompletableFuture<Void> evict(NodeId nodeId) {
        return delegateLeaderElector.evict(nodeId);
    }

    @Override
    public CompletableFuture<Boolean> promote(String topic, NodeId nodeId) {
        return delegateLeaderElector.promote(topic, nodeId);
    }

    @Override
    public CompletableFuture<Leadership> getLeadership(String topic) {
        return delegateLeaderElector.getLeadership(topic);
    }

    @Override
    public CompletableFuture<Map<String, Leadership>> getLeaderships() {
        return delegateLeaderElector.getLeaderships();
    }

    @Override
    public CompletableFuture<Void> addChangeListener(Consumer<Change<Leadership>> consumer) {
        return delegateLeaderElector.addChangeListener(consumer);
    }

    @Override
    public CompletableFuture<Void> removeChangeListener(Consumer<Change<Leadership>> consumer) {
        return delegateLeaderElector.removeChangeListener(consumer);
    }
}
