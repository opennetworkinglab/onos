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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.atomix.protocols.raft.service.AbstractRaftService;
import io.atomix.protocols.raft.service.Commit;
import io.atomix.protocols.raft.service.RaftServiceExecutor;
import io.atomix.protocols.raft.session.RaftSession;
import io.atomix.protocols.raft.storage.snapshot.SnapshotReader;
import io.atomix.protocols.raft.storage.snapshot.SnapshotWriter;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.Leader;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.Change;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.Anoint;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.Evict;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.GetElectedTopics;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.GetLeadership;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.Promote;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.Run;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorOperations.Withdraw;
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
 * State machine for {@link AtomixLeaderElector} resource.
 */
public class AtomixLeaderElectorService extends AbstractRaftService {

    private static final Serializer SERIALIZER = Serializer.using(KryoNamespace.newBuilder()
            .register(AtomixLeaderElectorOperations.NAMESPACE)
            .register(AtomixLeaderElectorEvents.NAMESPACE)
            .register(ElectionState.class)
            .register(Registration.class)
            .register(new LinkedHashMap<>().keySet().getClass())
            .build());

    private Map<String, AtomicLong> termCounters = new HashMap<>();
    private Map<String, ElectionState> elections = new HashMap<>();
    private Map<Long, RaftSession> listeners = new LinkedHashMap<>();

    @Override
    public void snapshot(SnapshotWriter writer) {
        writer.writeObject(Sets.newHashSet(listeners.keySet()), SERIALIZER::encode);
        writer.writeObject(termCounters, SERIALIZER::encode);
        writer.writeObject(elections, SERIALIZER::encode);
        logger().debug("Took state machine snapshot");
    }

    @Override
    public void install(SnapshotReader reader) {
        listeners = new LinkedHashMap<>();
        for (Long sessionId : reader.<Set<Long>>readObject(SERIALIZER::decode)) {
            listeners.put(sessionId, sessions().getSession(sessionId));
        }
        termCounters = reader.readObject(SERIALIZER::decode);
        elections = reader.readObject(SERIALIZER::decode);
        elections.values().forEach(e -> e.elections = elections);
        logger().debug("Reinstated state machine from snapshot");
    }

    @Override
    protected void configure(RaftServiceExecutor executor) {
        // Notification
        executor.register(ADD_LISTENER, this::listen);
        executor.register(REMOVE_LISTENER, this::unlisten);
        // Commands
        executor.register(RUN, SERIALIZER::decode, this::run, SERIALIZER::encode);
        executor.register(WITHDRAW, SERIALIZER::decode, this::withdraw);
        executor.register(ANOINT, SERIALIZER::decode, this::anoint, SERIALIZER::encode);
        executor.register(PROMOTE, SERIALIZER::decode, this::promote, SERIALIZER::encode);
        executor.register(EVICT, SERIALIZER::decode, this::evict);
        // Queries
        executor.register(GET_LEADERSHIP, SERIALIZER::decode, this::getLeadership, SERIALIZER::encode);
        executor.register(GET_ALL_LEADERSHIPS, this::allLeaderships, SERIALIZER::encode);
        executor.register(GET_ELECTED_TOPICS, SERIALIZER::decode, this::electedTopics, SERIALIZER::encode);
    }

    private void notifyLeadershipChange(Leadership previousLeadership, Leadership newLeadership) {
        notifyLeadershipChanges(Lists.newArrayList(new Change<>(previousLeadership, newLeadership)));
    }

    private void notifyLeadershipChanges(List<Change<Leadership>> changes) {
        if (changes.isEmpty()) {
            return;
        }
        listeners.values().forEach(session -> session.publish(CHANGE, SERIALIZER::encode, changes));
    }

    /**
     * Applies listen commits.
     *
     * @param commit listen commit
     */
    public void listen(Commit<Void> commit) {
        listeners.put(commit.session().sessionId().id(), commit.session());
    }

    /**
     * Applies unlisten commits.
     *
     * @param commit unlisten commit
     */
    public void unlisten(Commit<Void> commit) {
        listeners.remove(commit.session().sessionId().id());
    }

    /**
     * Applies an {@link AtomixLeaderElectorOperations.Run} commit.
     *
     * @param commit commit entry
     * @return topic leader. If no previous leader existed this is the node that just entered the race.
     */
    public Leadership run(Commit<? extends Run> commit) {
        try {
            String topic = commit.value().topic();
            Leadership oldLeadership = leadership(topic);
            Registration registration = new Registration(commit.value().nodeId(), commit.session().sessionId().id());
            elections.compute(topic, (k, v) -> {
                if (v == null) {
                    return new ElectionState(registration, termCounter(topic)::incrementAndGet, elections);
                } else {
                    if (!v.isDuplicate(registration)) {
                        return new ElectionState(v).addRegistration(
                                topic, registration, termCounter(topic)::incrementAndGet);
                    } else {
                        return v;
                    }
                }
            });
            Leadership newLeadership = leadership(topic);

            if (!Objects.equal(oldLeadership, newLeadership)) {
                notifyLeadershipChange(oldLeadership, newLeadership);
            }
            return newLeadership;
        } catch (Exception e) {
            logger().error("State machine operation failed", e);
            throw Throwables.propagate(e);
        }
    }

    /**
     * Applies an {@link AtomixLeaderElectorOperations.Withdraw} commit.
     *
     * @param commit withdraw commit
     */
    public void withdraw(Commit<? extends Withdraw> commit) {
        try {
            String topic = commit.value().topic();
            Leadership oldLeadership = leadership(topic);
            elections.computeIfPresent(topic, (k, v) -> v.cleanup(
                    topic, commit.session(), termCounter(topic)::incrementAndGet));
            Leadership newLeadership = leadership(topic);
            if (!Objects.equal(oldLeadership, newLeadership)) {
                notifyLeadershipChange(oldLeadership, newLeadership);
            }
        } catch (Exception e) {
            logger().error("State machine operation failed", e);
            throw Throwables.propagate(e);
        }
    }

    /**
     * Applies an {@link AtomixLeaderElectorOperations.Anoint} commit.
     *
     * @param commit anoint commit
     * @return {@code true} if changes were made and the transfer occurred; {@code false} if it did not.
     */
    public boolean anoint(Commit<? extends Anoint> commit) {
        try {
            String topic = commit.value().topic();
            NodeId nodeId = commit.value().nodeId();
            Leadership oldLeadership = leadership(topic);
            ElectionState electionState = elections.computeIfPresent(topic,
                    (k, v) -> v.transferLeadership(nodeId, termCounter(topic)));
            Leadership newLeadership = leadership(topic);
            if (!Objects.equal(oldLeadership, newLeadership)) {
                notifyLeadershipChange(oldLeadership, newLeadership);
            }
            return (electionState != null &&
                    electionState.leader() != null &&
                    commit.value().nodeId().equals(electionState.leader().nodeId()));
        } catch (Exception e) {
            logger().error("State machine operation failed", e);
            throw Throwables.propagate(e);
        }
    }

    /**
     * Applies an {@link AtomixLeaderElectorOperations.Promote} commit.
     *
     * @param commit promote commit
     * @return {@code true} if changes desired end state is achieved.
     */
    public boolean promote(Commit<? extends Promote> commit) {
        try {
            String topic = commit.value().topic();
            NodeId nodeId = commit.value().nodeId();
            Leadership oldLeadership = leadership(topic);
            if (oldLeadership == null || !oldLeadership.candidates().contains(nodeId)) {
                return false;
            }
            elections.computeIfPresent(topic, (k, v) -> v.promote(nodeId));
            Leadership newLeadership = leadership(topic);
            if (!Objects.equal(oldLeadership, newLeadership)) {
                notifyLeadershipChange(oldLeadership, newLeadership);
            }
            return true;
        } catch (Exception e) {
            logger().error("State machine operation failed", e);
            throw Throwables.propagate(e);
        }
    }

    /**
     * Applies an {@link AtomixLeaderElectorOperations.Evict} commit.
     *
     * @param commit evict commit
     */
    public void evict(Commit<? extends Evict> commit) {
        try {
            List<Change<Leadership>> changes = Lists.newArrayList();
            NodeId nodeId = commit.value().nodeId();
            Set<String> topics = Maps.filterValues(elections, e -> e.candidates().contains(nodeId)).keySet();
            topics.forEach(topic -> {
                Leadership oldLeadership = leadership(topic);
                elections.compute(topic, (k, v) -> v.evict(nodeId, termCounter(topic)::incrementAndGet));
                Leadership newLeadership = leadership(topic);
                if (!Objects.equal(oldLeadership, newLeadership)) {
                    changes.add(new Change<>(oldLeadership, newLeadership));
                }
            });
            notifyLeadershipChanges(changes);
        } catch (Exception e) {
            logger().error("State machine operation failed", e);
            throw Throwables.propagate(e);
        }
    }

    /**
     * Applies an {@link AtomixLeaderElectorOperations.GetLeadership} commit.
     *
     * @param commit GetLeadership commit
     * @return leader
     */
    public Leadership getLeadership(Commit<? extends GetLeadership> commit) {
        String topic = commit.value().topic();
        try {
            return leadership(topic);
        } catch (Exception e) {
            logger().error("State machine operation failed", e);
            throw Throwables.propagate(e);
        }
    }

    /**
     * Applies an {@link AtomixLeaderElectorOperations.GetElectedTopics} commit.
     *
     * @param commit commit entry
     * @return set of topics for which the node is the leader
     */
    public Set<String> electedTopics(Commit<? extends GetElectedTopics> commit) {
        try {
            NodeId nodeId = commit.value().nodeId();
            return ImmutableSet.copyOf(Maps.filterEntries(elections, e -> {
                Leader leader = leadership(e.getKey()).leader();
                return leader != null && leader.nodeId().equals(nodeId);
            }).keySet());
        } catch (Exception e) {
            logger().error("State machine operation failed", e);
            throw Throwables.propagate(e);
        }
    }

    /**
     * Applies an {@link AtomixLeaderElectorOperations#GET_ALL_LEADERSHIPS} commit.
     *
     * @param commit GetAllLeaderships commit
     * @return topic to leader mapping
     */
    public Map<String, Leadership> allLeaderships(Commit<Void> commit) {
        Map<String, Leadership> result = new HashMap<>();
        try {
            result.putAll(Maps.transformEntries(elections, (k, v) -> leadership(k)));
            return result;
        } catch (Exception e) {
            logger().error("State machine operation failed", e);
            throw Throwables.propagate(e);
        }
    }

    private Leadership leadership(String topic) {
        return new Leadership(topic,
                leader(topic),
                candidates(topic));
    }

    private Leader leader(String topic) {
        ElectionState electionState = elections.get(topic);
        return electionState == null ? null : electionState.leader();
    }

    private List<NodeId> candidates(String topic) {
        ElectionState electionState = elections.get(topic);
        return electionState == null ? new LinkedList<>() : electionState.candidates();
    }

    private void onSessionEnd(RaftSession session) {
        listeners.remove(session.sessionId().id());
        Set<String> topics = elections.keySet();
        List<Change<Leadership>> changes = Lists.newArrayList();
        topics.forEach(topic -> {
            Leadership oldLeadership = leadership(topic);
            elections.compute(topic, (k, v) -> v.cleanup(topic, session, termCounter(topic)::incrementAndGet));
            Leadership newLeadership = leadership(topic);
            if (!Objects.equal(oldLeadership, newLeadership)) {
                changes.add(new Change<>(oldLeadership, newLeadership));
            }
        });
        notifyLeadershipChanges(changes);
    }

    private static class Registration {
        private final NodeId nodeId;
        private final long sessionId;

        public Registration(NodeId nodeId, long sessionId) {
            this.nodeId = nodeId;
            this.sessionId = sessionId;
        }

        public NodeId nodeId() {
            return nodeId;
        }

        public long sessionId() {
            return sessionId;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("nodeId", nodeId)
                    .add("sessionId", sessionId)
                    .toString();
        }
    }

    private class ElectionState {
        final Registration leader;
        final long term;
        final long termStartTime;
        final List<Registration> registrations;
        transient Map<String, ElectionState> elections;

        public ElectionState(Registration registration, Supplier<Long> termCounter,
                             Map<String, ElectionState> elections) {
            registrations = Arrays.asList(registration);
            term = termCounter.get();
            termStartTime = System.currentTimeMillis();
            leader = registration;
            this.elections = elections;
        }

        public ElectionState(ElectionState other) {
            registrations = Lists.newArrayList(other.registrations);
            leader = other.leader;
            term = other.term;
            termStartTime = other.termStartTime;
            elections = other.elections;
        }

        public ElectionState(List<Registration> registrations,
                Registration leader,
                long term,
                long termStartTime,
                Map<String, ElectionState> elections) {
            this.registrations = Lists.newArrayList(registrations);
            this.leader = leader;
            this.term = term;
            this.termStartTime = termStartTime;
            this.elections = elections;
        }

        private void sortRegistrations(String topic, List<Registration> registrations) {
            registrations.sort((a, b) -> ComparisonChain.start()
                    .compare(countLeaders(topic, a), countLeaders(topic, b))
                    .compare(a.sessionId, b.sessionId)
                    .result());
        }

        private long countLeaders(String topic, Registration registration) {
            return elections.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(topic))
                    .filter(entry -> entry.getValue().leader != null)
                    .filter(entry -> entry.getValue().leader.nodeId.id()
                            .equals(sessions().getSession(registration.sessionId).memberId().id()))
                    .count();
        }

        public ElectionState cleanup(String topic, RaftSession session, Supplier<Long> termCounter) {
            Optional<Registration> registration =
                    registrations.stream().filter(r -> r.sessionId() == session.sessionId().id()).findFirst();
            if (registration.isPresent()) {
                List<Registration> updatedRegistrations =
                        registrations.stream()
                                .filter(r -> r.sessionId() != session.sessionId().id())
                                .collect(Collectors.toList());
                if (leader.sessionId() == session.sessionId().id()) {
                    if (!updatedRegistrations.isEmpty()) {
                        sortRegistrations(topic, updatedRegistrations);
                        return new ElectionState(updatedRegistrations,
                                updatedRegistrations.get(0),
                                termCounter.get(),
                                System.currentTimeMillis(),
                                elections);
                    } else {
                        return new ElectionState(updatedRegistrations, null, term, termStartTime, elections);
                    }
                } else {
                    return new ElectionState(updatedRegistrations, leader, term, termStartTime, elections);
                }
            } else {
                return this;
            }
        }

        public ElectionState evict(NodeId nodeId, Supplier<Long> termCounter) {
            Optional<Registration> registration =
                    registrations.stream().filter(r -> r.nodeId.equals(nodeId)).findFirst();
            if (registration.isPresent()) {
                List<Registration> updatedRegistrations =
                        registrations.stream()
                                .filter(r -> !r.nodeId().equals(nodeId))
                                .collect(Collectors.toList());
                if (leader.nodeId().equals(nodeId)) {
                    if (!updatedRegistrations.isEmpty()) {
                        return new ElectionState(updatedRegistrations,
                                updatedRegistrations.get(0),
                                termCounter.get(),
                                System.currentTimeMillis(),
                                elections);
                    } else {
                        return new ElectionState(updatedRegistrations, null, term, termStartTime, elections);
                    }
                } else {
                    return new ElectionState(updatedRegistrations, leader, term, termStartTime, elections);
                }
            } else {
                return this;
            }
        }

        public boolean isDuplicate(Registration registration) {
            return registrations.stream().anyMatch(r -> r.sessionId() == registration.sessionId());
        }

        public Leader leader() {
            if (leader == null) {
                return null;
            } else {
                NodeId leaderNodeId = leader.nodeId();
                return new Leader(leaderNodeId, term, termStartTime);
            }
        }

        public List<NodeId> candidates() {
            return registrations.stream().map(registration -> registration.nodeId()).collect(Collectors.toList());
        }

        public ElectionState addRegistration(String topic, Registration registration, Supplier<Long> termCounter) {
            if (!registrations.stream().anyMatch(r -> r.sessionId() == registration.sessionId())) {
                List<Registration> updatedRegistrations = new LinkedList<>(registrations);
                updatedRegistrations.add(registration);
                sortRegistrations(topic, updatedRegistrations);
                Registration firstRegistration = updatedRegistrations.get(0);
                Registration leader = this.leader;
                long term = this.term;
                long termStartTime = this.termStartTime;
                if (leader == null || !leader.equals(firstRegistration)) {
                    leader = firstRegistration;
                    term = termCounter.get();
                    termStartTime = System.currentTimeMillis();
                }
                return new ElectionState(updatedRegistrations,
                        leader,
                        term,
                        termStartTime,
                        elections);
            }
            return this;
        }

        public ElectionState transferLeadership(NodeId nodeId, AtomicLong termCounter) {
            Registration newLeader = registrations.stream()
                    .filter(r -> r.nodeId().equals(nodeId))
                    .findFirst()
                    .orElse(null);
            if (newLeader != null) {
                return new ElectionState(registrations,
                        newLeader,
                        termCounter.incrementAndGet(),
                        System.currentTimeMillis(),
                        elections);
            } else {
                return this;
            }
        }

        public ElectionState promote(NodeId nodeId) {
            Registration registration = registrations.stream()
                    .filter(r -> r.nodeId().equals(nodeId))
                    .findFirst()
                    .orElse(null);
            List<Registration> updatedRegistrations = Lists.newArrayList();
            updatedRegistrations.add(registration);
            registrations.stream()
                    .filter(r -> !r.nodeId().equals(nodeId))
                    .forEach(updatedRegistrations::add);
            return new ElectionState(updatedRegistrations,
                    leader,
                    term,
                    termStartTime,
                    elections);

        }
    }

    @Override
    public void onExpire(RaftSession session) {
        onSessionEnd(session);
    }

    @Override
    public void onClose(RaftSession session) {
        onSessionEnd(session);
    }

    private AtomicLong termCounter(String topic) {
        return termCounters.computeIfAbsent(topic, k -> new AtomicLong(0));
    }
}