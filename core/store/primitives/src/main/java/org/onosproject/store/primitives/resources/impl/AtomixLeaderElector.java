/*
 * Copyright 2016-present Open Networking Foundation
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.google.common.collect.Sets;
import io.atomix.protocols.raft.proxy.RaftProxy;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.Change;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.Anoint;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.GetElectedTopics;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.GetLeadership;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.Promote;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.Run;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.Withdraw;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncLeaderElector;
import org.onosproject.store.service.Serializer;

import static org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorEvents.CHANGE;
import static org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.ADD_LISTENER;
import static org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.ANOINT;
import static org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.EVICT;
import static org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.GET_ALL_LEADERSHIPS;
import static org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.GET_ELECTED_TOPICS;
import static org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.GET_LEADERSHIP;
import static org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.PROMOTE;
import static org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.REMOVE_LISTENER;
import static org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.RUN;
import static org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.WITHDRAW;

/**
 * Distributed resource providing the {@link AsyncLeaderElector} primitive.
 */
public class AtomixLeaderElector extends AbstractRaftPrimitive implements AsyncLeaderElector {
    private static final Serializer SERIALIZER = Serializer.using(KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(AtomixLeaderElectorOperations.NAMESPACE)
            .register(AtomixLeaderElectorEvents.NAMESPACE)
            .build());

    private final Set<Consumer<Change<Leadership>>> leadershipChangeListeners = Sets.newCopyOnWriteArraySet();

    public AtomixLeaderElector(RaftProxy proxy) {
        super(proxy);
        proxy.addStateChangeListener(state -> {
            if (state == RaftProxy.State.CONNECTED && isListening()) {
                proxy.invoke(ADD_LISTENER);
            }
        });
        proxy.addEventListener(CHANGE, SERIALIZER::decode, this::handleEvent);
    }

    private void handleEvent(List<Change<Leadership>> changes) {
        changes.forEach(change -> leadershipChangeListeners.forEach(l -> l.accept(change)));
    }

    @Override
    public CompletableFuture<Leadership> run(String topic, NodeId nodeId) {
        return proxy.<Run, Leadership>invoke(RUN, SERIALIZER::encode, new Run(topic, nodeId), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Void> withdraw(String topic) {
        return proxy.invoke(WITHDRAW, SERIALIZER::encode, new Withdraw(topic));
    }

    @Override
    public CompletableFuture<Boolean> anoint(String topic, NodeId nodeId) {
        return proxy.<Anoint, Boolean>invoke(ANOINT, SERIALIZER::encode, new Anoint(topic, nodeId), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Boolean> promote(String topic, NodeId nodeId) {
        return proxy.<Promote, Boolean>invoke(
                PROMOTE, SERIALIZER::encode, new Promote(topic, nodeId), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Void> evict(NodeId nodeId) {
        return proxy.invoke(EVICT, SERIALIZER::encode, new AtomixLeaderElectorOperations.Evict(nodeId));
    }

    @Override
    public CompletableFuture<Leadership> getLeadership(String topic) {
        return proxy.invoke(GET_LEADERSHIP, SERIALIZER::encode, new GetLeadership(topic), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Map<String, Leadership>> getLeaderships() {
        return proxy.invoke(GET_ALL_LEADERSHIPS, SERIALIZER::decode);
    }

    public CompletableFuture<Set<String>> getElectedTopics(NodeId nodeId) {
        return proxy.invoke(GET_ELECTED_TOPICS, SERIALIZER::encode, new GetElectedTopics(nodeId), SERIALIZER::decode);
    }

    @Override
    public synchronized CompletableFuture<Void> addChangeListener(Consumer<Change<Leadership>> consumer) {
        if (leadershipChangeListeners.isEmpty()) {
            return proxy.invoke(ADD_LISTENER).thenRun(() -> leadershipChangeListeners.add(consumer));
        } else {
            leadershipChangeListeners.add(consumer);
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public synchronized CompletableFuture<Void> removeChangeListener(Consumer<Change<Leadership>> consumer) {
        if (leadershipChangeListeners.remove(consumer) && leadershipChangeListeners.isEmpty()) {
            return proxy.invoke(REMOVE_LISTENER).thenApply(v -> null);
        }
        return CompletableFuture.completedFuture(null);
    }

    private boolean isListening() {
        return !leadershipChangeListeners.isEmpty();
    }
}