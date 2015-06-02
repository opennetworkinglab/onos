package org.onosproject.store.consistent.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEvent.Type;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.ConsistentMapException;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.cluster.ControllerNode.State.ACTIVE;
import static org.onosproject.cluster.ControllerNode.State.INACTIVE;

/**
 * Distributed Lock Manager implemented on top of ConsistentMap.
 * <p>
 * This implementation makes use of ClusterService's failure
 * detection capabilities to detect and purge stale locks.
 * TODO: Ensure lock safety and liveness.
 */
@Component(immediate = true, enabled = true)
@Service
public class DistributedLeadershipManager implements LeadershipService {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    private static final MessageSubject LEADERSHIP_EVENT_MESSAGE_SUBJECT =
            new MessageSubject("distributed-leadership-manager-events");

    private final Logger log = getLogger(getClass());
    private ExecutorService messageHandlingExecutor;
    private ScheduledExecutorService electionRunner;
    private ScheduledExecutorService lockExecutor;
    private ScheduledExecutorService staleLeadershipPurgeExecutor;
    private ScheduledExecutorService leadershipStatusBroadcaster;

    private ConsistentMap<String, NodeId> leaderMap;
    private ConsistentMap<String, List<NodeId>> candidateMap;

    private ListenerRegistry<LeadershipEvent, LeadershipEventListener> listenerRegistry;
    private final Map<String, Leadership> leaderBoard = Maps.newConcurrentMap();
    private final Map<String, Leadership> candidateBoard = Maps.newConcurrentMap();
    private final ClusterEventListener clusterEventListener = new InternalClusterEventListener();

    private NodeId localNodeId;
    private Set<String> activeTopics = Sets.newConcurrentHashSet();
    private Map<String, CompletableFuture<Leadership>> pendingFutures = Maps.newConcurrentMap();

    // The actual delay is randomly chosen between the interval [0, WAIT_BEFORE_RETRY_MILLIS)
    private static final int WAIT_BEFORE_RETRY_MILLIS = 150;
    private static final int DELAY_BETWEEN_LEADER_LOCK_ATTEMPTS_SEC = 2;
    private static final int LEADERSHIP_STATUS_UPDATE_INTERVAL_SEC = 2;
    private static final int DELAY_BETWEEN_STALE_LEADERSHIP_PURGE_ATTEMPTS_SEC = 2;

    private final AtomicBoolean staleLeadershipPurgeScheduled = new AtomicBoolean(false);

    private static final Serializer SERIALIZER = Serializer.using(
            new KryoNamespace.Builder().register(KryoNamespaces.API).build());

    @Activate
    public void activate() {
        leaderMap = storageService.<String, NodeId>consistentMapBuilder()
                .withName("onos-topic-leaders")
                .withSerializer(SERIALIZER)
                .withPartitionsDisabled().build();
        candidateMap = storageService.<String, List<NodeId>>consistentMapBuilder()
                .withName("onos-topic-candidates")
                .withSerializer(SERIALIZER)
                .withPartitionsDisabled().build();

        localNodeId = clusterService.getLocalNode().id();

        messageHandlingExecutor = Executors.newSingleThreadExecutor(
                groupedThreads("onos/store/leadership", "message-handler"));
        electionRunner = Executors.newSingleThreadScheduledExecutor(
                groupedThreads("onos/store/leadership", "election-runner"));
        lockExecutor = Executors.newScheduledThreadPool(
                4, groupedThreads("onos/store/leadership", "election-thread-%d"));
        staleLeadershipPurgeExecutor = Executors.newSingleThreadScheduledExecutor(
                groupedThreads("onos/store/leadership", "stale-leadership-evictor"));
        leadershipStatusBroadcaster = Executors.newSingleThreadScheduledExecutor(
                groupedThreads("onos/store/leadership", "peer-updater"));
        clusterCommunicator.addSubscriber(
                LEADERSHIP_EVENT_MESSAGE_SUBJECT,
                SERIALIZER::decode,
                this::onLeadershipEvent,
                messageHandlingExecutor);

        clusterService.addListener(clusterEventListener);

        electionRunner.scheduleWithFixedDelay(
                this::electLeaders, 0, DELAY_BETWEEN_LEADER_LOCK_ATTEMPTS_SEC, TimeUnit.SECONDS);

        leadershipStatusBroadcaster.scheduleWithFixedDelay(
                this::sendLeadershipStatus, 0, LEADERSHIP_STATUS_UPDATE_INTERVAL_SEC, TimeUnit.SECONDS);

        listenerRegistry = new ListenerRegistry<>();
        eventDispatcher.addSink(LeadershipEvent.class, listenerRegistry);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        leaderBoard.forEach((topic, leadership) -> {
            if (localNodeId.equals(leadership.leader())) {
                withdraw(topic);
            }
        });

        clusterService.removeListener(clusterEventListener);
        eventDispatcher.removeSink(LeadershipEvent.class);
        clusterCommunicator.removeSubscriber(LEADERSHIP_EVENT_MESSAGE_SUBJECT);

        electionRunner.shutdown();
        messageHandlingExecutor.shutdown();
        lockExecutor.shutdown();
        staleLeadershipPurgeExecutor.shutdown();
        leadershipStatusBroadcaster.shutdown();

        log.info("Stopped");
    }

    @Override
    public Map<String, Leadership> getLeaderBoard() {
        return ImmutableMap.copyOf(leaderBoard);
    }

    @Override
    public Map<String, List<NodeId>> getCandidates() {
        return Maps.toMap(candidateBoard.keySet(), this::getCandidates);
    }

    @Override
    public List<NodeId> getCandidates(String path) {
        Leadership current = candidateBoard.get(path);
        return current == null ? ImmutableList.of() : ImmutableList.copyOf(current.candidates());
    }

    @Override
    public NodeId getLeader(String path) {
        Leadership leadership = leaderBoard.get(path);
        return leadership != null ? leadership.leader() : null;
    }

    @Override
    public Leadership getLeadership(String path) {
        checkArgument(path != null);
        return leaderBoard.get(path);
    }

    @Override
    public Set<String> ownedTopics(NodeId nodeId) {
        checkArgument(nodeId != null);
        return leaderBoard.entrySet()
                    .stream()
                    .filter(entry -> nodeId.equals(entry.getValue().leader()))
                    .map(Entry::getKey)
                    .collect(Collectors.toSet());
    }

    @Override
    public CompletableFuture<Leadership> runForLeadership(String path) {
        log.debug("Running for leadership for topic: {}", path);
        CompletableFuture<Leadership> resultFuture = new CompletableFuture<>();
        doRunForLeadership(path, resultFuture);
        return resultFuture;
    }

    private void doRunForLeadership(String path, CompletableFuture<Leadership> future) {
        try {
            Versioned<List<NodeId>> candidates = candidateMap.computeIf(path,
                    currentList -> currentList == null || !currentList.contains(localNodeId),
                    (topic, currentList) -> {
                        if (currentList == null) {
                            return ImmutableList.of(localNodeId);
                        } else {
                            List<NodeId> newList = Lists.newLinkedList();
                            newList.addAll(currentList);
                            newList.add(localNodeId);
                            return newList;
                        }
                    });
            publish(new LeadershipEvent(
                    LeadershipEvent.Type.CANDIDATES_CHANGED,
                    new Leadership(path,
                            candidates.value(),
                            candidates.version(),
                            candidates.creationTime())));
            log.debug("In the leadership race for topic {} with candidates {}", path, candidates);
            activeTopics.add(path);
            Leadership leadership = electLeader(path, candidates.value());
            if (leadership == null) {
                pendingFutures.put(path, future);
            } else {
                future.complete(leadership);
            }
        } catch (ConsistentMapException e) {
            log.debug("Failed to enter topic leader race for {}. Retrying.", path, e);
            rerunForLeadership(path, future);
        }
    }

    @Override
    public CompletableFuture<Void> withdraw(String path) {
        activeTopics.remove(path);
        CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        doWithdraw(path, resultFuture);
        return resultFuture;
    }


    private void doWithdraw(String path, CompletableFuture<Void> future) {
        if (activeTopics.contains(path)) {
            future.completeExceptionally(new CancellationException(String.format("%s is now a active topic", path)));
        }
        try {
            Versioned<NodeId> leader = leaderMap.get(path);
            if (leader != null && Objects.equals(leader.value(), localNodeId)) {
                if (leaderMap.remove(path, leader.version())) {
                    log.debug("Gave up leadership for {}", path);
                    future.complete(null);
                    publish(new LeadershipEvent(
                            LeadershipEvent.Type.LEADER_BOOTED,
                            new Leadership(path,
                                localNodeId,
                                leader.version(),
                                leader.creationTime())));
                }
            }
            // else we are not the current leader, can still be a candidate.
            Versioned<List<NodeId>> candidates = candidateMap.get(path);
            List<NodeId> candidateList = candidates != null
                    ? Lists.newArrayList(candidates.value())
                    : Lists.newArrayList();
            if (!candidateList.remove(localNodeId)) {
                future.complete(null);
                return;
            }
            if (candidateMap.replace(path, candidates.version(), candidateList)) {
                Versioned<List<NodeId>> newCandidates = candidateMap.get(path);
                future.complete(null);
                publish(new LeadershipEvent(
                                LeadershipEvent.Type.CANDIDATES_CHANGED,
                                new Leadership(path,
                                    newCandidates.value(),
                                    newCandidates.version(),
                                    newCandidates.creationTime())));
            } else {
                log.debug("Failed to withdraw from candidates list for {}. Will retry", path);
                retryWithdraw(path, future);
            }
        } catch (Exception e) {
            log.debug("Failed to verify (and clear) any lock this node might be holding for {}", path, e);
            retryWithdraw(path, future);
        }
    }

    @Override
    public boolean stepdown(String path) {
        if (!activeTopics.contains(path) || !Objects.equals(localNodeId, getLeader(path))) {
            return false;
        }

        try {
            Versioned<NodeId> leader = leaderMap.get(path);
            if (leader != null && Objects.equals(leader.value(), localNodeId)) {
                if (leaderMap.remove(path, leader.version())) {
                    log.debug("Stepped down from leadership for {}", path);
                    publish(new LeadershipEvent(
                            LeadershipEvent.Type.LEADER_BOOTED,
                            new Leadership(path,
                                localNodeId,
                                leader.version(),
                                leader.creationTime())));
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Error executing stepdown for {}", path, e);
        }
        return false;
    }

    @Override
    public void addListener(LeadershipEventListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(LeadershipEventListener listener) {
        listenerRegistry.removeListener(listener);
    }

    @Override
    public boolean makeTopCandidate(String path, NodeId nodeId) {
        Versioned<List<NodeId>> newCandidates = candidateMap.computeIf(path,
                candidates -> candidates != null &&
                              candidates.contains(nodeId) &&
                              !nodeId.equals(Iterables.getFirst(candidates, null)),
                (topic, candidates) -> {
                    List<NodeId> updatedCandidates = new ArrayList<>(candidates.size());
                    updatedCandidates.add(nodeId);
                    candidates.stream().filter(id -> !nodeId.equals(id)).forEach(updatedCandidates::add);
                    return updatedCandidates;
                });
        publish(new LeadershipEvent(
                    LeadershipEvent.Type.CANDIDATES_CHANGED,
                    new Leadership(path,
                        newCandidates.value(),
                        newCandidates.version(),
                        newCandidates.creationTime())));
        return true;
    }

    private Leadership electLeader(String path, List<NodeId> candidates) {
        Leadership currentLeadership = getLeadership(path);
        if (currentLeadership != null) {
            return currentLeadership;
        } else {
            NodeId topCandidate = candidates
                        .stream()
                        .filter(n -> clusterService.getState(n) == ACTIVE)
                        .findFirst()
                        .orElse(null);
            try {
                Versioned<NodeId> leader = localNodeId.equals(topCandidate)
                        ? leaderMap.computeIfAbsent(path, p -> localNodeId) : leaderMap.get(path);
                if (leader != null) {
                    Leadership newLeadership = new Leadership(path,
                            leader.value(),
                            leader.version(),
                            leader.creationTime());
                    publish(new LeadershipEvent(
                            LeadershipEvent.Type.LEADER_ELECTED,
                            newLeadership));
                    return newLeadership;
                }
            } catch (Exception e) {
                log.debug("Failed to elect leader for {}", path, e);
            }
        }
        return null;
    }

    private void electLeaders() {
        try {
            candidateMap.entrySet().forEach(entry -> {
                String path = entry.getKey();
                Versioned<List<NodeId>> candidates = entry.getValue();
                // for active topics, check if this node can become a leader (if it isn't already)
                if (activeTopics.contains(path)) {
                    lockExecutor.submit(() -> {
                        Leadership leadership = electLeader(path, candidates.value());
                        if (leadership != null) {
                            CompletableFuture<Leadership> future = pendingFutures.remove(path);
                            if (future != null) {
                                future.complete(leadership);
                            }
                        }
                    });
                }
                // Raise a CANDIDATES_CHANGED event to force refresh local candidate board
                // and also to update local listeners.
                // Don't worry about duplicate events as they will be suppressed.
                onLeadershipEvent(new LeadershipEvent(LeadershipEvent.Type.CANDIDATES_CHANGED,
                                                      new Leadership(path,
                                                                     candidates.value(),
                                                                     candidates.version(),
                                                                     candidates.creationTime())));
            });
        } catch (Exception e) {
            log.debug("Failure electing leaders", e);
        }
    }

    private void publish(LeadershipEvent event) {
        onLeadershipEvent(event);
        clusterCommunicator.broadcast(event, LEADERSHIP_EVENT_MESSAGE_SUBJECT, SERIALIZER::encode);
    }

    private void onLeadershipEvent(LeadershipEvent leadershipEvent) {
        log.trace("Leadership Event: time = {} type = {} event = {}",
                leadershipEvent.time(), leadershipEvent.type(),
                leadershipEvent);

        Leadership leadershipUpdate = leadershipEvent.subject();
        LeadershipEvent.Type eventType = leadershipEvent.type();
        String topic = leadershipUpdate.topic();

        AtomicBoolean updateAccepted = new AtomicBoolean(false);
        if (eventType.equals(LeadershipEvent.Type.LEADER_ELECTED)) {
            leaderBoard.compute(topic, (k, currentLeadership) -> {
                if (currentLeadership == null || currentLeadership.epoch() < leadershipUpdate.epoch()) {
                    updateAccepted.set(true);
                    return leadershipUpdate;
                }
                return currentLeadership;
            });
        } else if (eventType.equals(LeadershipEvent.Type.LEADER_BOOTED)) {
            leaderBoard.compute(topic, (k, currentLeadership) -> {
                if (currentLeadership == null || currentLeadership.epoch() <= leadershipUpdate.epoch()) {
                    updateAccepted.set(true);
                    return null;
                }
                return currentLeadership;
            });
        } else if (eventType.equals(LeadershipEvent.Type.CANDIDATES_CHANGED)) {
            candidateBoard.compute(topic, (k, currentInfo) -> {
                if (currentInfo == null || currentInfo.epoch() < leadershipUpdate.epoch()) {
                    updateAccepted.set(true);
                    return leadershipUpdate;
                }
                return currentInfo;
            });
        } else {
            throw new IllegalStateException("Unknown event type.");
        }

        if (updateAccepted.get()) {
            eventDispatcher.post(leadershipEvent);
        }
    }

    private void rerunForLeadership(String path, CompletableFuture<Leadership> future) {
        lockExecutor.schedule(
                () -> doRunForLeadership(path, future),
                RandomUtils.nextInt(WAIT_BEFORE_RETRY_MILLIS),
                TimeUnit.MILLISECONDS);
    }

    private void retryWithdraw(String path, CompletableFuture<Void> future) {
        lockExecutor.schedule(
                () -> doWithdraw(path, future),
                RandomUtils.nextInt(WAIT_BEFORE_RETRY_MILLIS),
                TimeUnit.MILLISECONDS);
    }

    private void scheduleStaleLeadershipPurge(int afterDelaySec) {
        if (staleLeadershipPurgeScheduled.compareAndSet(false, true)) {
            staleLeadershipPurgeExecutor.schedule(
                    this::purgeStaleLeadership,
                    afterDelaySec,
                    TimeUnit.SECONDS);
        }
    }

    /**
     * Purges locks held by inactive nodes and evicts inactive nodes from candidacy.
     */
    private void purgeStaleLeadership() {
        AtomicBoolean rerunPurge = new AtomicBoolean(false);
        try {
            staleLeadershipPurgeScheduled.set(false);
            leaderMap.entrySet()
                .stream()
                .filter(e -> clusterService.getState(e.getValue().value()) == INACTIVE)
                .forEach(entry -> {
                    String path = entry.getKey();
                    NodeId nodeId = entry.getValue().value();
                    long epoch = entry.getValue().version();
                    long creationTime = entry.getValue().creationTime();
                    try {
                        if (leaderMap.remove(path, epoch)) {
                            log.debug("Purged stale lock held by {} for {}", nodeId, path);
                            publish(new LeadershipEvent(
                                    LeadershipEvent.Type.LEADER_BOOTED,
                                    new Leadership(path, nodeId, epoch, creationTime)));
                        }
                    } catch (Exception e) {
                        log.debug("Failed to purge stale lock held by {} for {}", nodeId, path, e);
                        rerunPurge.set(true);
                    }
                });

            candidateMap.entrySet()
                .forEach(entry -> {
                    String path = entry.getKey();
                    Versioned<List<NodeId>> candidates = entry.getValue();
                    List<NodeId> candidatesList = candidates != null
                            ? candidates.value() : Collections.emptyList();
                    List<NodeId> activeCandidatesList =
                            candidatesList.stream()
                                          .filter(n -> clusterService.getState(n) == ACTIVE)
                                          .filter(n -> !localNodeId.equals(n) || activeTopics.contains(path))
                                          .collect(Collectors.toList());
                    if (activeCandidatesList.size() < candidatesList.size()) {
                        Set<NodeId> removedCandidates =
                                Sets.difference(Sets.newHashSet(candidatesList),
                                                Sets.newHashSet(activeCandidatesList));
                        try {
                            if (candidateMap.replace(path, entry.getValue().version(), activeCandidatesList)) {
                                log.info("Evicted inactive candidates {} from "
                                        + "candidate list for {}", removedCandidates, path);
                                Versioned<List<NodeId>> updatedCandidates = candidateMap.get(path);
                                publish(new LeadershipEvent(
                                        LeadershipEvent.Type.CANDIDATES_CHANGED,
                                        new Leadership(path,
                                                updatedCandidates.value(),
                                                updatedCandidates.version(),
                                                updatedCandidates.creationTime())));
                            } else {
                                // Conflicting update detected. Rerun purge to make sure
                                // inactive candidates are evicted.
                                rerunPurge.set(true);
                            }
                        } catch (Exception e) {
                            log.debug("Failed to evict inactive candidates {} from "
                                    + "candidate list for {}", removedCandidates, path, e);
                            rerunPurge.set(true);
                        }
                    }
                });
        } catch (Exception e) {
            log.debug("Failure purging state leadership.", e);
            rerunPurge.set(true);
        }

        if (rerunPurge.get()) {
            log.debug("Rescheduling stale leadership purge due to errors encountered in previous run");
            scheduleStaleLeadershipPurge(DELAY_BETWEEN_STALE_LEADERSHIP_PURGE_ATTEMPTS_SEC);
        }
    }

    private void sendLeadershipStatus() {
        try {
            leaderBoard.forEach((path, leadership) -> {
                if (leadership.leader().equals(localNodeId)) {
                    LeadershipEvent event = new LeadershipEvent(LeadershipEvent.Type.LEADER_ELECTED, leadership);
                    clusterCommunicator.broadcast(event,
                            LEADERSHIP_EVENT_MESSAGE_SUBJECT,
                            SERIALIZER::encode);
                }
            });
        } catch (Exception e) {
            log.debug("Failed to send leadership updates", e);
        }
    }

    private class InternalClusterEventListener implements ClusterEventListener {

        @Override
        public void event(ClusterEvent event) {
            if (event.type() == Type.INSTANCE_DEACTIVATED || event.type() == Type.INSTANCE_REMOVED) {
                scheduleStaleLeadershipPurge(0);
            }
        }
    }
}
