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
package org.onosproject.store.atomix.primitives.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import io.atomix.core.election.LeadershipEventListener;
import org.onosproject.cluster.Leader;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.Change;
import org.onosproject.store.service.AsyncLeaderElector;

import static org.onosproject.store.atomix.primitives.impl.AtomixFutures.adaptFuture;

/**
 * Atomix leader elector.
 */
public class AtomixLeaderElector implements AsyncLeaderElector {
    private final io.atomix.core.election.AsyncLeaderElector<NodeId> atomixElector;
    private final NodeId localNodeId;
    private final Map<Consumer<Change<Leadership>>, LeadershipEventListener<NodeId>> listenerMap =
        Maps.newIdentityHashMap();

    public AtomixLeaderElector(io.atomix.core.election.AsyncLeaderElector<NodeId> atomixElector, NodeId localNodeId) {
        this.atomixElector = atomixElector;
        this.localNodeId = localNodeId;
    }

    @Override
    public String name() {
        return atomixElector.name();
    }

    @Override
    public CompletableFuture<Leadership> run(String topic, NodeId nodeId) {
        return adaptFuture(atomixElector.run(topic, nodeId)).thenApply(leadership -> toLeadership(topic, leadership));
    }

    @Override
    public CompletableFuture<Void> withdraw(String topic) {
        return adaptFuture(atomixElector.withdraw(topic, localNodeId));
    }

    @Override
    public CompletableFuture<Boolean> anoint(String topic, NodeId nodeId) {
        return adaptFuture(atomixElector.anoint(topic, nodeId));
    }

    @Override
    public CompletableFuture<Void> evict(NodeId nodeId) {
        return adaptFuture(atomixElector.evict(nodeId));
    }

    @Override
    public CompletableFuture<Boolean> promote(String topic, NodeId nodeId) {
        return adaptFuture(atomixElector.promote(topic, nodeId));
    }

    @Override
    public CompletableFuture<Leadership> getLeadership(String topic) {
        return adaptFuture(atomixElector.getLeadership(topic)).thenApply(leadership -> toLeadership(topic, leadership));
    }

    @Override
    public CompletableFuture<Map<String, Leadership>> getLeaderships() {
        return adaptFuture(atomixElector.getLeaderships())
            .thenApply(leaderships -> leaderships.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> toLeadership(e.getKey(), e.getValue()))));
    }

    @Override
    public CompletableFuture<Void> addChangeListener(Consumer<Change<Leadership>> consumer) {
        LeadershipEventListener<NodeId> atomixListener = event ->
            consumer.accept(new Change<>(
                toLeadership(event.topic(), event.oldLeadership()),
                toLeadership(event.topic(), event.newLeadership())));
        listenerMap.put(consumer, atomixListener);
        return adaptFuture(atomixElector.addListener(atomixListener));
    }

    @Override
    public CompletableFuture<Void> removeChangeListener(Consumer<Change<Leadership>> consumer) {
        LeadershipEventListener<NodeId> atomixListener = listenerMap.remove(consumer);
        if (atomixListener != null) {
            return adaptFuture(atomixElector.removeListener(atomixListener));
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> demote(String topic, NodeId nodeId) {
        return adaptFuture(atomixElector.demote(topic, nodeId));
    }

    private Leadership toLeadership(String topic, io.atomix.core.election.Leadership<NodeId> leadership) {
        return leadership != null
            ? new Leadership(topic, toLeader(leadership.leader()), leadership.candidates())
            : null;
    }

    private Leader toLeader(io.atomix.core.election.Leader<NodeId> leader) {
        return leader != null ? new Leader(leader.id(), leader.term(), leader.timestamp()) : null;
    }
}
