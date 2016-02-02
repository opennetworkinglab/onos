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

import static org.slf4j.LoggerFactory.getLogger;
import io.atomix.copycat.client.session.Session;
import io.atomix.copycat.server.Commit;
import io.atomix.copycat.server.Snapshottable;
import io.atomix.copycat.server.StateMachineExecutor;
import io.atomix.copycat.server.session.SessionListener;
import io.atomix.copycat.server.storage.snapshot.SnapshotReader;
import io.atomix.copycat.server.storage.snapshot.SnapshotWriter;
import io.atomix.resource.ResourceStateMachine;

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

import org.onosproject.cluster.Leader;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.Change;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.slf4j.Logger;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * State machine for {@link AtomixLeaderElector} resource.
 */
public class AtomixLeaderElectorState extends ResourceStateMachine
    implements SessionListener, Snapshottable {

    private final Logger log = getLogger(getClass());
    private Map<String, AtomicLong> termCounters = new HashMap<>();
    private Map<String, ElectionState> elections = new HashMap<>();
    private final Map<Long, Commit<? extends AtomixLeaderElectorCommands.Listen>> listeners = new LinkedHashMap<>();
    private final Serializer serializer = Serializer.using(Arrays.asList(KryoNamespaces.API),
                                                           ElectionState.class,
                                                           Registration.class);

    @Override
    protected void configure(StateMachineExecutor executor) {
        // Notification
        executor.register(AtomixLeaderElectorCommands.Listen.class, this::listen);
        executor.register(AtomixLeaderElectorCommands.Unlisten.class, this::unlisten);
        // Commands
        executor.register(AtomixLeaderElectorCommands.Run.class, this::run);
        executor.register(AtomixLeaderElectorCommands.Withdraw.class, this::withdraw);
        executor.register(AtomixLeaderElectorCommands.Anoint.class, this::anoint);
        // Queries
        executor.register(AtomixLeaderElectorCommands.GetLeadership.class, this::leadership);
        executor.register(AtomixLeaderElectorCommands.GetAllLeaderships.class, this::allLeaderships);
        executor.register(AtomixLeaderElectorCommands.GetElectedTopics.class, this::electedTopics);
    }

    private void notifyLeadershipChange(Leadership previousLeadership, Leadership newLeadership) {
        Change<Leadership> change = new Change<>(previousLeadership, newLeadership);
        listeners.values().forEach(listener -> listener.session().publish("change", change));
    }

    @Override
    public void delete() {
      // Close and clear Listeners
      listeners.values().forEach(Commit::close);
      listeners.clear();
    }

    /**
     * Applies listen commits.
     *
     * @param commit listen commit
     */
    public void listen(Commit<? extends AtomixLeaderElectorCommands.Listen> commit) {
        if (listeners.putIfAbsent(commit.session().id(), commit) != null) {
            commit.close();
        }
    }

    /**
     * Applies unlisten commits.
     *
     * @param commit unlisten commit
     */
    public void unlisten(Commit<? extends AtomixLeaderElectorCommands.Unlisten> commit) {
        try {
            Commit<? extends AtomixLeaderElectorCommands.Listen> listener = listeners.remove(commit.session().id());
            if (listener != null) {
                listener.close();
            }
        } finally {
            commit.close();
        }
    }

    /**
     * Applies an {@link AtomixLeaderElectorCommands.Run} commit.
     * @param commit commit entry
     * @return topic leader. If no previous leader existed this is the node that just entered the race.
     */
    public Leadership run(Commit<? extends AtomixLeaderElectorCommands.Run> commit) {
        try {
            String topic = commit.operation().topic();
            Leadership oldLeadership = leadership(topic);
            Registration registration = new Registration(commit.operation().nodeId(), commit.session().id());
            elections.compute(topic, (k, v) -> {
                if (v == null) {
                    return new ElectionState(registration, termCounter(topic)::incrementAndGet);
                } else {
                    if (!v.isDuplicate(registration)) {
                        return new ElectionState(v).addRegistration(registration, termCounter(topic)::incrementAndGet);
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
        } finally {
            commit.close();
        }
    }

    /**
     * Applies an {@link AtomixLeaderElectorCommands.Withdraw} commit.
     * @param commit withdraw commit
     */
    public void withdraw(Commit<? extends AtomixLeaderElectorCommands.Withdraw> commit) {
        try {
            String topic = commit.operation().topic();
            Leadership oldLeadership = leadership(topic);
            elections.computeIfPresent(topic, (k, v) -> v.cleanup(commit.session(),
                                        termCounter(topic)::incrementAndGet));
            Leadership newLeadership = leadership(topic);
            if (!Objects.equal(oldLeadership, newLeadership)) {
                notifyLeadershipChange(oldLeadership, newLeadership);
            }
        } finally {
            commit.close();
        }
    }

    /**
     * Applies an {@link AtomixLeaderElectorCommands.Anoint} commit.
     * @param commit anoint commit
     * @return {@code true} if changes were made and the transfer occurred; {@code false} if it did not.
     */
    public boolean anoint(Commit<? extends AtomixLeaderElectorCommands.Anoint> commit) {
        try {
            String topic = commit.operation().topic();
            Leadership oldLeadership = leadership(topic);
            ElectionState electionState = elections.computeIfPresent(topic,
                    (k, v) -> new ElectionState(v).transferLeadership(commit.operation().nodeId(), termCounter(topic)));
            Leadership newLeadership = leadership(topic);
            if (!Objects.equal(oldLeadership, newLeadership)) {
                notifyLeadershipChange(oldLeadership, newLeadership);
            }
            return (electionState != null &&
                    electionState.leader() != null &&
                    commit.operation().nodeId().equals(electionState.leader().nodeId()));
        } finally {
            commit.close();
        }
    }

    /**
     * Applies an {@link AtomixLeaderElectorCommands.GetLeadership} commit.
     * @param commit GetLeadership commit
     * @return leader
     */
    public Leadership leadership(Commit<? extends AtomixLeaderElectorCommands.GetLeadership> commit) {
        String topic = commit.operation().topic();
        try {
            return leadership(topic);
        } finally {
            commit.close();
        }
    }

    /**
     * Applies an {@link AtomixLeaderElectorCommands.GetElectedTopics} commit.
     * @param commit commit entry
     * @return set of topics for which the node is the leader
     */
    public Set<String> electedTopics(Commit<? extends AtomixLeaderElectorCommands.GetElectedTopics> commit) {
        try {
            NodeId nodeId = commit.operation().nodeId();
            return Maps.filterEntries(elections, e -> {
                Leader leader = leadership(e.getKey()).leader();
                return leader != null && leader.nodeId().equals(nodeId);
            }).keySet();
        } finally {
            commit.close();
        }
    }

    /**
     * Applies an {@link AtomixLeaderElectorCommands.GetAllLeaderships} commit.
     * @param commit GetAllLeaderships commit
     * @return topic to leader mapping
     */
    public Map<String, Leadership> allLeaderships(
            Commit<? extends AtomixLeaderElectorCommands.GetAllLeaderships> commit) {
        try {
            return Maps.transformEntries(elections, (k, v) -> leadership(k));
        } finally {
            commit.close();
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

    private void onSessionEnd(Session session) {
        Commit<? extends AtomixLeaderElectorCommands.Listen> listener = listeners.remove(session);
        if (listener != null) {
            listener.close();
        }
        Set<String> topics = elections.keySet();
        topics.forEach(topic -> {
            Leadership oldLeadership = leadership(topic);
            elections.compute(topic, (k, v) -> v.cleanup(session, termCounter(topic)::incrementAndGet));
            Leadership newLeadership = leadership(topic);
            if (!Objects.equal(oldLeadership, newLeadership)) {
                notifyLeadershipChange(oldLeadership, newLeadership);
            }
        });
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

    private static class ElectionState {
        final Registration leader;
        final long term;
        final long termStartTime;
        final List<Registration> registrations;

        public ElectionState(Registration registration, Supplier<Long> termCounter) {
            registrations = Arrays.asList(registration);
            term = termCounter.get();
            termStartTime = System.currentTimeMillis();
            leader = registration;
        }

        public ElectionState(ElectionState other) {
            registrations = Lists.newArrayList(other.registrations);
            leader = other.leader;
            term = other.term;
            termStartTime = other.termStartTime;
        }

        public ElectionState(List<Registration> registrations,
                Registration leader,
                long term,
                long termStartTime) {
            this.registrations = Lists.newArrayList(registrations);
            this.leader = leader;
            this.term = term;
            this.termStartTime = termStartTime;
        }

        public ElectionState cleanup(Session session, Supplier<Long> termCounter) {
            Optional<Registration> registration =
                    registrations.stream().filter(r -> r.sessionId() == session.id()).findFirst();
            if (registration.isPresent()) {
                List<Registration> updatedRegistrations =
                        registrations.stream()
                        .filter(r -> r.sessionId() != session.id())
                        .collect(Collectors.toList());
                if (leader.sessionId() == session.id()) {
                    if (updatedRegistrations.size() > 0) {
                        return new ElectionState(updatedRegistrations,
                                updatedRegistrations.get(0),
                                termCounter.get(),
                                System.currentTimeMillis());
                    } else {
                        return new ElectionState(updatedRegistrations, null, term, termStartTime);
                    }
                } else {
                    return new ElectionState(updatedRegistrations, leader, term, termStartTime);
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

        public ElectionState addRegistration(Registration registration, Supplier<Long> termCounter) {
            if (!registrations.stream().anyMatch(r -> r.sessionId() == registration.sessionId())) {
                List<Registration> updatedRegistrations = new LinkedList<>(registrations);
                updatedRegistrations.add(registration);
                boolean newLeader = leader == null;
                return new ElectionState(updatedRegistrations,
                        newLeader ? registration : leader,
                        newLeader ? termCounter.get() : term,
                        newLeader ? System.currentTimeMillis() : termStartTime);
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
                                         System.currentTimeMillis());
            } else {
                return this;
            }
        }
    }

    @Override
    public void register(Session session) {
    }

    @Override
    public void unregister(Session session) {
        onSessionEnd(session);
    }

    @Override
    public void expire(Session session) {
        onSessionEnd(session);
    }

    @Override
    public void close(Session session) {
        onSessionEnd(session);
    }

    @Override
    public void snapshot(SnapshotWriter writer) {
        byte[] encodedTermCounters = serializer.encode(termCounters);
        writer.writeInt(encodedTermCounters.length);
        writer.write(encodedTermCounters);
        byte[] encodedElections  = serializer.encode(elections);
        writer.writeInt(encodedElections.length);
        writer.write(encodedElections);
        log.info("Took state machine snapshot");
    }

    @Override
    public void install(SnapshotReader reader) {
        int encodedTermCountersSize = reader.readInt();
        byte[] encodedTermCounters = new byte[encodedTermCountersSize];
        reader.read(encodedTermCounters);
        termCounters = serializer.decode(encodedTermCounters);
        int encodedElectionsSize = reader.readInt();
        byte[] encodedElections = new byte[encodedElectionsSize];
        reader.read(encodedElections);
        elections = serializer.decode(encodedElections);
        log.info("Reinstated state machine from snapshot");
    }

    private AtomicLong termCounter(String topic) {
        return termCounters.computeIfAbsent(topic, k -> new AtomicLong(0));
    }
}
