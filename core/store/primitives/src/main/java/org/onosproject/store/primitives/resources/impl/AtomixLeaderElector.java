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

import io.atomix.copycat.client.CopycatClient;
import io.atomix.resource.AbstractResource;
import io.atomix.resource.ResourceTypeInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

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

import com.google.common.collect.ImmutableSet;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;

/**
 * Distributed resource providing the {@link AsyncLeaderElector} primitive.
 */
@ResourceTypeInfo(id = -152, factory = AtomixLeaderElectorFactory.class)
public class AtomixLeaderElector extends AbstractResource<AtomixLeaderElector>
    implements AsyncLeaderElector {
    private final Set<Consumer<Status>> statusChangeListeners =
            Sets.newCopyOnWriteArraySet();
    private final Set<Consumer<Change<Leadership>>> leadershipChangeListeners =
            Sets.newCopyOnWriteArraySet();
    private final Consumer<Change<Leadership>> cacheUpdater;
    private final Consumer<Status> statusListener;

    public static final String CHANGE_SUBJECT = "leadershipChangeEvents";
    private final LoadingCache<String, CompletableFuture<Leadership>> cache;

    Function<CopycatClient.State, Status> mapper = state -> {
        switch (state) {
            case CONNECTED:
                return Status.ACTIVE;
            case SUSPENDED:
                return Status.SUSPENDED;
            case CLOSED:
                return Status.INACTIVE;
            default:
                throw new IllegalStateException("Unknown state " + state);
        }
    };

    public AtomixLeaderElector(CopycatClient client, Properties properties) {
        super(client, properties);
        cache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .build(CacheLoader.from(topic -> this.client.submit(new GetLeadership(topic))));

        cacheUpdater = change -> {
            Leadership leadership = change.newValue();
            cache.put(leadership.topic(), CompletableFuture.completedFuture(leadership));
        };
        statusListener = status -> {
            if (status == Status.SUSPENDED || status == Status.INACTIVE) {
                cache.invalidateAll();
            }
        };
        addStatusChangeListener(statusListener);
        client.onStateChange(this::handleStateChange);
    }

    @Override
    public CompletableFuture<Void> destroy() {
        removeStatusChangeListener(statusListener);
        return removeChangeListener(cacheUpdater);
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public CompletableFuture<AtomixLeaderElector> open() {
        return super.open().thenApply(result -> {
            client.onStateChange(state -> {
                if (state == CopycatClient.State.CONNECTED && isListening()) {
                    client.submit(new Listen());
                }
            });
            client.onEvent(CHANGE_SUBJECT, this::handleEvent);
            return result;
        });
    }

    public CompletableFuture<AtomixLeaderElector> setupCache() {
        return addChangeListener(cacheUpdater).thenApply(v -> this);
    }

    private void handleEvent(List<Change<Leadership>> changes) {
        changes.forEach(change -> leadershipChangeListeners.forEach(l -> l.accept(change)));
    }

    @Override
    public CompletableFuture<Leadership> run(String topic, NodeId nodeId) {
        return client.submit(new Run(topic, nodeId)).whenComplete((r, e) -> cache.invalidate(topic));
    }

    @Override
    public CompletableFuture<Void> withdraw(String topic) {
        return client.submit(new Withdraw(topic)).whenComplete((r, e) -> cache.invalidate(topic));
    }

    @Override
    public CompletableFuture<Boolean> anoint(String topic, NodeId nodeId) {
        return client.submit(new Anoint(topic, nodeId)).whenComplete((r, e) -> cache.invalidate(topic));
    }

    @Override
    public CompletableFuture<Boolean> promote(String topic, NodeId nodeId) {
        return client.submit(new Promote(topic, nodeId)).whenComplete((r, e) -> cache.invalidate(topic));
    }

    @Override
    public CompletableFuture<Void> evict(NodeId nodeId) {
        return client.submit(new AtomixLeaderElectorCommands.Evict(nodeId));
    }

    @Override
    public CompletableFuture<Leadership> getLeadership(String topic) {
        return cache.getUnchecked(topic)
                .whenComplete((r, e) -> {
                    if (e != null) {
                        cache.invalidate(topic);
                    }
                });
    }

    @Override
    public CompletableFuture<Map<String, Leadership>> getLeaderships() {
        return client.submit(new GetAllLeaderships());
    }

    public CompletableFuture<Set<String>> getElectedTopics(NodeId nodeId) {
        return client.submit(new GetElectedTopics(nodeId));
    }

    @Override
    public synchronized CompletableFuture<Void> addChangeListener(Consumer<Change<Leadership>> consumer) {
        if (leadershipChangeListeners.isEmpty()) {
            return client.submit(new Listen()).thenRun(() -> leadershipChangeListeners.add(consumer));
        } else {
            leadershipChangeListeners.add(consumer);
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public synchronized CompletableFuture<Void> removeChangeListener(Consumer<Change<Leadership>> consumer) {
        if (leadershipChangeListeners.remove(consumer) && leadershipChangeListeners.isEmpty()) {
            return client.submit(new Unlisten()).thenApply(v -> null);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void addStatusChangeListener(Consumer<Status> listener) {
        statusChangeListeners.add(listener);
    }

    @Override
    public void removeStatusChangeListener(Consumer<Status> listener) {
        statusChangeListeners.remove(listener);
    }

    @Override
    public Collection<Consumer<Status>> statusChangeListeners() {
        return ImmutableSet.copyOf(statusChangeListeners);
    }

    private boolean isListening() {
        return !leadershipChangeListeners.isEmpty();
    }

    private void handleStateChange(CopycatClient.State state) {
        statusChangeListeners().forEach(listener -> listener.accept(mapper.apply(state)));
    }
}
