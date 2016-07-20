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
package org.onosproject.store.primitives.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.PartitionId;
import org.onosproject.event.Change;
import org.onosproject.store.service.AsyncLeaderElector;
import com.google.common.collect.Maps;

/**
 * {@link AsyncLeaderElector} that has its topics partitioned horizontally across
 * several {@link AsyncLeaderElector leader electors}.
 */
public class PartitionedAsyncLeaderElector implements AsyncLeaderElector {

    private final String name;
    private final TreeMap<PartitionId, AsyncLeaderElector> partitions = Maps.newTreeMap();
    private final Hasher<String> topicHasher;

    public PartitionedAsyncLeaderElector(String name,
            Map<PartitionId, AsyncLeaderElector> partitions,
            Hasher<String> topicHasher) {
        this.name = name;
        this.partitions.putAll(checkNotNull(partitions));
        this.topicHasher = checkNotNull(topicHasher);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public CompletableFuture<Leadership> run(String topic, NodeId nodeId) {
        return getLeaderElector(topic).run(topic, nodeId);
    }

    @Override
    public CompletableFuture<Void> withdraw(String topic) {
        return getLeaderElector(topic).withdraw(topic);
    }

    @Override
    public CompletableFuture<Boolean> anoint(String topic, NodeId nodeId) {
        return getLeaderElector(topic).anoint(topic, nodeId);
    }

    @Override
    public CompletableFuture<Boolean> promote(String topic, NodeId nodeId) {
        return getLeaderElector(topic).promote(topic, nodeId);
    }

    @Override
    public CompletableFuture<Void> evict(NodeId nodeId) {
        return CompletableFuture.allOf(getLeaderElectors().stream()
                                                          .map(le -> le.evict(nodeId))
                                                          .toArray(CompletableFuture[]::new));
    }

    @Override
    public CompletableFuture<Leadership> getLeadership(String topic) {
        return getLeaderElector(topic).getLeadership(topic);
    }

    @Override
    public CompletableFuture<Map<String, Leadership>> getLeaderships() {
        Map<String, Leadership> leaderships = Maps.newConcurrentMap();
        return CompletableFuture.allOf(getLeaderElectors().stream()
                                                          .map(le -> le.getLeaderships()
                                                                       .thenAccept(m -> leaderships.putAll(m)))
                                                          .toArray(CompletableFuture[]::new))
                                .thenApply(v -> leaderships);
    }

    @Override
    public CompletableFuture<Void> addChangeListener(Consumer<Change<Leadership>> listener) {
        return CompletableFuture.allOf(getLeaderElectors().stream()
                                                          .map(map -> map.addChangeListener(listener))
                                                          .toArray(CompletableFuture[]::new));
    }

    @Override
    public CompletableFuture<Void> removeChangeListener(Consumer<Change<Leadership>> listener) {
        return CompletableFuture.allOf(getLeaderElectors().stream()
                                                          .map(map -> map.removeChangeListener(listener))
                                                          .toArray(CompletableFuture[]::new));
    }

    /**
     * Returns the leaderElector (partition) to which the specified topic maps.
     * @param topic topic name
     * @return AsyncLeaderElector to which topic maps
     */
    private AsyncLeaderElector getLeaderElector(String topic) {
        return partitions.get(topicHasher.hash(topic));
    }

    /**
     * Returns all the constituent leader electors.
     * @return collection of leader electors.
     */
    private Collection<AsyncLeaderElector> getLeaderElectors() {
        return partitions.values();
    }

    @Override
    public void addStatusChangeListener(Consumer<Status> listener) {
        partitions.values().forEach(elector -> elector.addStatusChangeListener(listener));
    }

    @Override
    public void removeStatusChangeListener(Consumer<Status> listener) {
        partitions.values().forEach(elector -> elector.removeStatusChangeListener(listener));
    }

    @Override
    public Collection<Consumer<Status>> statusChangeListeners() {
        throw new UnsupportedOperationException();
    }
}