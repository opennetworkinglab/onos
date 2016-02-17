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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.Change;
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
            client.session().onEvent("change", this::handleEvent);
            return result;
        });
    }

    private void handleEvent(Change<Leadership> change) {
        leadershipChangeListeners.forEach(l -> l.accept(change));
    }

    @Override
    public CompletableFuture<Leadership> run(String topic, NodeId nodeId) {
        return submit(new AtomixLeaderElectorCommands.Run(topic, nodeId));
    }

    @Override
    public CompletableFuture<Void> withdraw(String topic) {
        return submit(new AtomixLeaderElectorCommands.Withdraw(topic));
    }

    @Override
    public CompletableFuture<Boolean> anoint(String topic, NodeId nodeId) {
        return submit(new AtomixLeaderElectorCommands.Anoint(topic, nodeId));
    }

    @Override
    public CompletableFuture<Leadership> getLeadership(String topic) {
        return submit(new AtomixLeaderElectorCommands.GetLeadership(topic));
    }

    @Override
    public CompletableFuture<Map<String, Leadership>> getLeaderships() {
        return submit(new AtomixLeaderElectorCommands.GetAllLeaderships());
    }

    public CompletableFuture<Set<String>> getElectedTopics(NodeId nodeId) {
        return submit(new AtomixLeaderElectorCommands.GetElectedTopics(nodeId));
    }

    /**
     * Leadership change listener context.
     */
    private final class LeadershipChangeListener implements Listener<Change<Leadership>> {
        private final Consumer<Change<Leadership>> listener;

        private LeadershipChangeListener(Consumer<Change<Leadership>> listener) {
            this.listener = listener;
        }

        @Override
        public void accept(Change<Leadership> change) {
            listener.accept(change);
        }

        @Override
        public void close() {
            synchronized (AtomixLeaderElector.this) {
                submit(new AtomixLeaderElectorCommands.Unlisten());
            }
        }
    }

    @Override
    public CompletableFuture<Void> addChangeListener(Consumer<Change<Leadership>> consumer) {
        leadershipChangeListeners.add(consumer);
        return setupListener();
    }

    @Override
    public CompletableFuture<Void> removeChangeListener(Consumer<Change<Leadership>> consumer) {
        leadershipChangeListeners.remove(consumer);
        return teardownListener();
    }

    private CompletableFuture<Void> setupListener() {
        if (listener == null && !leadershipChangeListeners.isEmpty()) {
            Consumer<Change<Leadership>> changeConsumer = change -> {
                leadershipChangeListeners.forEach(consumer -> consumer.accept(change));
            };
            return submit(new AtomixLeaderElectorCommands.Listen())
                    .thenAccept(v -> listener = new LeadershipChangeListener(changeConsumer));
        }
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> teardownListener() {
        if (listener != null && leadershipChangeListeners.isEmpty()) {
            listener.close();
            listener = null;
            return submit(new AtomixLeaderElectorCommands.Unlisten());
        }
        return CompletableFuture.completedFuture(null);
    }
}
