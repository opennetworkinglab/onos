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

import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.collect.ImmutableSet;
import io.atomix.copycat.server.session.ServerSession;
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
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.onosproject.cluster.Leader;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.Change;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands.Anoint;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands.Evict;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands.GetAllLeaderships;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands.GetElectedTopics;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands.GetLeadership;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands.Listen;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands.Promote;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands.Run;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands.Unlisten;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands.Withdraw;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.slf4j.Logger;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
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
    private final Map<Long, Commit<? extends Listen>> listeners = new LinkedHashMap<>();
    private final Serializer serializer = Serializer.using(Arrays.asList(KryoNamespaces.API),
                                                           ElectionState.class,
                                                           Registration.class);

    public AtomixLeaderElectorState(Properties properties) {
        super(properties);
    }

    @Override
    protected void configure(StateMachineExecutor executor) {
        // Notification
        executor.register(Listen.class, this::listen);
        executor.register(Unlisten.class, this::unlisten);
        // Commands
        executor.register(Run.class, this::run);
        executor.register(Withdraw.class, this::withdraw);
        executor.register(Anoint.class, this::anoint);
        executor.register(Promote.class, this::promote);
        executor.register(Evict.class, this::evict);
        // Queries
        executor.register(GetLeadership.class, this::leadership);
        executor.register(GetAllLeaderships.class, this::allLeaderships);
        executor.register(GetElectedTopics.class, this::electedTopics);
    }

    private void notifyLeadershipChange(Leadership previousLeadership, Leadership newLeadership) {
        notifyLeadershipChanges(Lists.newArrayList(new Change<>(previousLeadership, newLeadership)));
    }

    private void notifyLeadershipChanges(List<Change<Leadership>> changes) {
        if (changes.isEmpty()) {
            return;
        }
        listeners.values()
                 .forEach(listener -> listener.session()
                                              .publish(AtomixLeaderElector.CHANGE_SUBJECT, changes));
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
    public void listen(Commit<? extends Listen> commit) {
        if (listeners.putIfAbsent(commit.session().id(), commit) != null) {
            commit.close();
        }
    }

    /**
     * Applies unlisten commits.
     *
     * @param commit unlisten commit
     */
    public void unlisten(Commit<? extends Unlisten> commit) {
        try {
            Commit<? extends Listen> listener = listeners.remove(commit.session().id());
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
    public Leadership run(Commit<? extends Run> commit) {
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
        } catch (Exception e) {
            log.error("State machine operation failed", e);
            throw Throwables.propagate(e);
        } finally {
            commit.close();
        }
    }

    /**
     * Applies an {@link AtomixLeaderElectorCommands.Withdraw} commit.
     * @param commit withdraw commit
     */
    public void withdraw(Commit<? extends Withdraw> commit) {
        try {
            String topic = commit.operation().topic();
            Leadership oldLeadership = leadership(topic);
            elections.computeIfPresent(topic, (k, v) -> v.cleanup(commit.session(),
                                        termCounter(topic)::incrementAndGet));
            Leadership newLeadership = leadership(topic);
            if (!Objects.equal(oldLeadership, newLeadership)) {
                notifyLeadershipChange(oldLeadership, newLeadership);
            }
        } catch (Exception e) {
            log.error("State machine operation failed", e);
            throw Throwables.propagate(e);
        } finally {
            commit.close();
        }
    }

    /**
     * Applies an {@link AtomixLeaderElectorCommands.Anoint} commit.
     * @param commit anoint commit
     * @return {@code true} if changes were made and the transfer occurred; {@code false} if it did not.
     */
    public boolean anoint(Commit<? extends Anoint> commit) {
        try {
            String topic = commit.operation().topic();
            NodeId nodeId = commit.operation().nodeId();
            Leadership oldLeadership = leadership(topic);
            ElectionState electionState = elections.computeIfPresent(topic,
                    (k, v) -> v.transferLeadership(nodeId, termCounter(topic)));
            Leadership newLeadership = leadership(topic);
            if (!Objects.equal(oldLeadership, newLeadership)) {
                notifyLeadershipChange(oldLeadership, newLeadership);
            }
            return (electionState != null &&
                    electionState.leader() != null &&
                    commit.operation().nodeId().equals(electionState.leader().nodeId()));
        } catch (Exception e) {
            log.error("State machine operation failed", e);
            throw Throwables.propagate(e);
        } finally {
            commit.close();
        }
    }

    /**
     * Applies an {@link AtomixLeaderElectorCommands.Promote} commit.
     * @param commit promote commit
     * @return {@code true} if changes desired end state is achieved.
     */
    public boolean promote(Commit<? extends Promote> commit) {
        try {
            String topic = commit.operation().topic();
            NodeId nodeId = commit.operation().nodeId();
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
            log.error("State machine operation failed", e);
            throw Throwables.propagate(e);
        } finally {
            commit.close();
        }
    }

    /**
     * Applies an {@link AtomixLeaderElectorCommands.Evict} commit.
     * @param commit evict commit
     */
    public void evict(Commit<? extends Evict> commit) {
        try {
            List<Change<Leadership>> changes = Lists.newArrayList();
            NodeId nodeId = commit.operation().nodeId();
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
            log.error("State machine operation failed", e);
            throw Throwables.propagate(e);
        } finally {
            commit.close();
        }
    }

    /**
     * Applies an {@link AtomixLeaderElectorCommands.GetLeadership} commit.
     * @param commit GetLeadership commit
     * @return leader
     */
    public Leadership leadership(Commit<? extends GetLeadership> commit) {
        String topic = commit.operation().topic();
        try {
            return leadership(topic);
        } catch (Exception e) {
            log.error("State machine operation failed", e);
            throw Throwables.propagate(e);
        } finally {
            commit.close();
        }
    }

    /**
     * Applies an {@link AtomixLeaderElectorCommands.GetElectedTopics} commit.
     * @param commit commit entry
     * @return set of topics for which the node is the leader
     */
    public Set<String> electedTopics(Commit<? extends GetElectedTopics> commit) {
        try {
            NodeId nodeId = commit.operation().nodeId();
            return ImmutableSet.copyOf(Maps.filterEntries(elections, e -> {
                Leader leader = leadership(e.getKey()).leader();
                return leader != null && leader.nodeId().equals(nodeId);
            }).keySet());
        } catch (Exception e) {
            log.error("State machine operation failed", e);
            throw Throwables.propagate(e);
        } finally {
            commit.close();
        }
    }

    /**
     * Applies an {@link AtomixLeaderElectorCommands.GetAllLeaderships} commit.
     * @param commit GetAllLeaderships commit
     * @return topic to leader mapping
     */
    public Map<String, Leadership> allLeaderships(Commit<? extends GetAllLeaderships> commit) {
        Map<String, Leadership> result = new HashMap<>();
        try {
            result.putAll(Maps.transformEntries(elections, (k, v) -> leadership(k)));
            return result;
        } catch (Exception e) {
            log.error("State machine operation failed", e);
            throw Throwables.propagate(e);
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

    private void onSessionEnd(ServerSession session) {
        Commit<? extends AtomixLeaderElectorCommands.Listen> listener = listeners.remove(session.id());
        if (listener != null) {
            listener.close();
        }
        Set<String> topics = elections.keySet();
        List<Change<Leadership>> changes = Lists.newArrayList();
        topics.forEach(topic -> {
            Leadership oldLeadership = leadership(topic);
            elections.compute(topic, (k, v) -> v.cleanup(session, termCounter(topic)::incrementAndGet));
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

        public ElectionState cleanup(ServerSession session, Supplier<Long> termCounter) {
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

        public ElectionState evict(NodeId nodeId, Supplier<Long> termCounter) {
            Optional<Registration> registration =
                    registrations.stream().filter(r -> r.nodeId.equals(nodeId)).findFirst();
            if (registration.isPresent()) {
                List<Registration> updatedRegistrations =
                        registrations.stream()
                        .filter(r -> !r.nodeId().equals(nodeId))
                        .collect(Collectors.toList());
                if (leader.nodeId().equals(nodeId)) {
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
                                    termStartTime);

        }
    }

    @Override
    public void register(ServerSession session) {
    }

    @Override
    public void unregister(ServerSession session) {
        onSessionEnd(session);
    }

    @Override
    public void expire(ServerSession session) {
        onSessionEnd(session);
    }

    @Override
    public void close(ServerSession session) {
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
        log.debug("Took state machine snapshot");
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
        log.debug("Reinstated state machine from snapshot");
    }

    private AtomicLong termCounter(String topic) {
        return termCounters.computeIfAbsent(topic, k -> new AtomicLong(0));
    }
}
