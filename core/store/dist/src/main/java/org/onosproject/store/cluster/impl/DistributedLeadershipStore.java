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
package org.onosproject.store.cluster.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.Leader;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipStore;
import org.onosproject.cluster.LeadershipStoreDelegate;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Implementation of {@code LeadershipStore} backed by {@link ConsistentMap}.
 */
@Service
@Component(immediate = true, enabled = true)
public class DistributedLeadershipStore
    extends AbstractStore<LeadershipEvent, LeadershipStoreDelegate>
    implements LeadershipStore {

    private static final Logger log = getLogger(DistributedLeadershipStore.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    protected NodeId localNodeId;
    protected ConsistentMap<String, InternalLeadership> leadershipMap;
    protected Map<String, Versioned<InternalLeadership>> leadershipCache = Maps.newConcurrentMap();

    private final MapEventListener<String, InternalLeadership> leadershipChangeListener =
            event -> {
                Leadership oldValue = InternalLeadership.toLeadership(Versioned.valueOrNull(event.oldValue()));
                Leadership newValue = InternalLeadership.toLeadership(Versioned.valueOrNull(event.newValue()));
                boolean leaderChanged =
                        !Objects.equal(oldValue == null ? null : oldValue.leader(), newValue.leader());
                boolean candidatesChanged =
                        !Sets.symmetricDifference(Sets.newHashSet(oldValue == null ?
                                                    ImmutableSet.<NodeId>of() : oldValue.candidates()),
                                                  Sets.newHashSet(newValue.candidates())).isEmpty();
                LeadershipEvent.Type eventType = null;
                if (leaderChanged && candidatesChanged) {
                    eventType = LeadershipEvent.Type.LEADER_AND_CANDIDATES_CHANGED;
                }
                if (leaderChanged && !candidatesChanged) {
                    eventType = LeadershipEvent.Type.LEADER_CHANGED;
                }
                if (!leaderChanged && candidatesChanged) {
                    eventType = LeadershipEvent.Type.CANDIDATES_CHANGED;
                }
                leadershipCache.compute(event.key(), (k, v) -> {
                    if (v == null || v.version() < event.newValue().version()) {
                        return event.newValue();
                    }
                    return v;
                });
                notifyDelegate(new LeadershipEvent(eventType, newValue));
            };

    @Activate
    public void activate() {
        localNodeId = clusterService.getLocalNode().id();
        leadershipMap = storageService.<String, InternalLeadership>consistentMapBuilder()
                                      .withName("onos-leadership")
                                      .withPartitionsDisabled()
                                      .withRelaxedReadConsistency()
                                      .withSerializer(Serializer.using(KryoNamespaces.API, InternalLeadership.class))
                                      .build();
        leadershipMap.entrySet().forEach(e -> leadershipCache.put(e.getKey(), e.getValue()));
        leadershipMap.addListener(leadershipChangeListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        leadershipMap.removeListener(leadershipChangeListener);
        log.info("Stopped");
    }

    @Override
    public Leadership addRegistration(String topic) {
        Versioned<InternalLeadership> internalLeadership = leadershipMap.computeIf(topic,
                v -> v == null || !v.candidates().contains(localNodeId),
                (k, v) -> {
                    if (v == null || v.candidates().isEmpty()) {
                        return new InternalLeadership(topic,
                                localNodeId,
                                v == null ? 1 : v.term() + 1,
                                System.currentTimeMillis(),
                                ImmutableList.of(localNodeId));
                    }
                    List<NodeId> newCandidates = new ArrayList<>(v.candidates());
                    newCandidates.add(localNodeId);
                    return new InternalLeadership(topic, v.leader(), v.term(), v.termStartTime(), newCandidates);
                });
        return InternalLeadership.toLeadership(Versioned.valueOrNull(internalLeadership));
    }

    @Override
    public void removeRegistration(String topic) {
        removeRegistration(topic, localNodeId);
    }

    private void removeRegistration(String topic, NodeId nodeId) {
        leadershipMap.computeIf(topic,
                v -> v != null && v.candidates().contains(nodeId),
                (k, v) -> {
                    List<NodeId> newCandidates = v.candidates()
                            .stream()
                            .filter(id -> !nodeId.equals(id))
                            .collect(Collectors.toList());
                    NodeId newLeader = nodeId.equals(v.leader()) ?
                            newCandidates.size() > 0 ? newCandidates.get(0) : null : v.leader();
                    long newTerm = newLeader == null || Objects.equal(newLeader, v.leader()) ?
                            v.term() : v.term() + 1;
                    long newTermStartTime = newLeader == null || Objects.equal(newLeader, v.leader()) ?
                            v.termStartTime() : System.currentTimeMillis();
                    return new InternalLeadership(topic, newLeader, newTerm, newTermStartTime, newCandidates);
                });
    }

    @Override
    public void removeRegistration(NodeId nodeId) {
        leadershipMap.entrySet()
                                  .stream()
                                  .filter(e -> e.getValue().value().candidates().contains(nodeId))
                                  .map(e -> e.getKey())
                                  .forEach(topic -> this.removeRegistration(topic, nodeId));
    }

    @Override
    public boolean moveLeadership(String topic, NodeId toNodeId) {
        Versioned<InternalLeadership> internalLeadership = leadershipMap.computeIf(topic,
                v -> v != null &&
                    v.candidates().contains(toNodeId) &&
                    !Objects.equal(v.leader(), toNodeId),
                (k, v) -> {
                    List<NodeId> newCandidates = new ArrayList<>();
                    newCandidates.add(toNodeId);
                    newCandidates.addAll(v.candidates()
                            .stream()
                            .filter(id -> !toNodeId.equals(id))
                            .collect(Collectors.toList()));
                    return new InternalLeadership(topic,
                            toNodeId,
                            v.term() + 1,
                            System.currentTimeMillis(),
                            newCandidates);
                });
        return Objects.equal(toNodeId, Versioned.valueOrNull(internalLeadership).leader());
    }

    @Override
    public boolean makeTopCandidate(String topic, NodeId nodeId) {
        Versioned<InternalLeadership> internalLeadership = leadershipMap.computeIf(topic,
                v -> v != null &&
                v.candidates().contains(nodeId) &&
                !v.candidates().get(0).equals(nodeId),
                (k, v) -> {
                    List<NodeId> newCandidates = new ArrayList<>();
                    newCandidates.add(nodeId);
                    newCandidates.addAll(v.candidates()
                            .stream()
                            .filter(id -> !nodeId.equals(id))
                            .collect(Collectors.toList()));
                    return new InternalLeadership(topic,
                            v.leader(),
                            v.term(),
                            System.currentTimeMillis(),
                            newCandidates);
                });
        return internalLeadership != null && nodeId.equals(internalLeadership.value().candidates().get(0));
    }

    @Override
    public Leadership getLeadership(String topic) {
        InternalLeadership internalLeadership = Versioned.valueOrNull(leadershipMap.get(topic));
        return internalLeadership == null ? null : internalLeadership.asLeadership();
    }

    @Override
    public Map<String, Leadership> getLeaderships() {
        return ImmutableMap.copyOf(Maps.transformValues(leadershipCache, v -> v.value().asLeadership()));
    }

    private static class InternalLeadership {
        private final String topic;
        private final NodeId leader;
        private final long term;
        private final long termStartTime;
        private final List<NodeId> candidates;

        public InternalLeadership(String topic,
                NodeId leader,
                long term,
                long termStartTime,
                List<NodeId> candidates) {
            this.topic = topic;
            this.leader = leader;
            this.term = term;
            this.termStartTime = termStartTime;
            this.candidates = ImmutableList.copyOf(candidates);
        }

        public NodeId leader() {
            return this.leader;
        }

        public long term() {
            return term;
        }

        public long termStartTime() {
            return termStartTime;
        }

        public List<NodeId> candidates() {
            return candidates;
        }

        public Leadership asLeadership() {
            return new Leadership(topic, leader == null ?
                    null : new Leader(leader, term, termStartTime), candidates);
        }

        public static Leadership toLeadership(InternalLeadership internalLeadership) {
            return internalLeadership == null ? null : internalLeadership.asLeadership();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("leader", leader)
                    .add("term", term)
                    .add("termStartTime", termStartTime)
                    .add("candidates", candidates)
                    .toString();
        }
    }
}
