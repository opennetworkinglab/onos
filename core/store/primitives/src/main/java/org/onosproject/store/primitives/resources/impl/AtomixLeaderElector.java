/*
 * Copyright 2016 Open Networking Laboratory
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

import io.atomix.catalyst.util.Listener;
import io.atomix.copycat.client.CopycatClient;
import io.atomix.resource.Resource;
import io.atomix.resource.ResourceTypeInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.Change;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands.Anoint;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands.GetAllLeaderships;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands.GetElectedTopics;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands.GetLeadership;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands.Listen;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands.Promote;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands.Run;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands.Unlisten;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands.Withdraw;
import org.onosproject.store.service.AsyncLeaderElector;

import com.google.common.collect.Sets;

/**
 * Distributed resource providing the {@link AsyncLeaderElector} primitive.
 */
@ResourceTypeInfo(id = -152,
                  stateMachine = AtomixLeaderElectorState.class,
                  typeResolver = AtomixLeaderElectorCommands.TypeResolver.class)
public class AtomixLeaderElector extends Resource<AtomixLeaderElector>
    implements AsyncLeaderElector {
    private final Set<Consumer<Change<Leadership>>> leadershipChangeListeners =
            Sets.newConcurrentHashSet();

    public static final String CHANGE_SUBJECT = "leadershipChangeEvents";
    private Listener<Change<Leadership>> listener;

    public AtomixLeaderElector(CopycatClient client, Resource.Options options) {
        super(client, options);
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public CompletableFuture<AtomixLeaderElector> open() {
        return super.open().thenApply(result -> {
            client.onEvent(CHANGE_SUBJECT, this::handleEvent);
            return result;
        });
    }

    private void handleEvent(List<Change<Leadership>> changes) {
        changes.forEach(change -> leadershipChangeListeners.forEach(l -> l.accept(change)));
    }

    @Override
    public CompletableFuture<Leadership> run(String topic, NodeId nodeId) {
        return submit(new Run(topic, nodeId));
    }

    @Override
    public CompletableFuture<Void> withdraw(String topic) {
        return submit(new Withdraw(topic));
    }

    @Override
    public CompletableFuture<Boolean> anoint(String topic, NodeId nodeId) {
        return submit(new Anoint(topic, nodeId));
    }

    @Override
    public CompletableFuture<Boolean> promote(String topic, NodeId nodeId) {
        return submit(new Promote(topic, nodeId));
    }

    @Override
    public CompletableFuture<Void> evict(NodeId nodeId) {
        return submit(new AtomixLeaderElectorCommands.Evict(nodeId));
    }

    @Override
    public CompletableFuture<Leadership> getLeadership(String topic) {
        return submit(new GetLeadership(topic));
    }

    @Override
    public CompletableFuture<Map<String, Leadership>> getLeaderships() {
        return submit(new GetAllLeaderships());
    }

    public CompletableFuture<Set<String>> getElectedTopics(NodeId nodeId) {
        return submit(new GetElectedTopics(nodeId));
    }

    @Override
    public synchronized CompletableFuture<Void> addChangeListener(Consumer<Change<Leadership>> consumer) {
        if (leadershipChangeListeners.isEmpty()) {
            return submit(new Listen()).thenRun(() -> leadershipChangeListeners.add(consumer));
        } else {
            leadershipChangeListeners.add(consumer);
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public synchronized CompletableFuture<Void> removeChangeListener(Consumer<Change<Leadership>> consumer) {
        if (leadershipChangeListeners.remove(listener) && leadershipChangeListeners.isEmpty()) {
            return submit(new Unlisten()).thenApply(v -> null);
        }
        return CompletableFuture.completedFuture(null);
    }
}
