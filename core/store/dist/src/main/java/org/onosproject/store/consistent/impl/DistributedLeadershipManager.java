package org.onosproject.store.consistent.impl;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkArgument;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.AbstractListenerRegistry;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Distributed Lock Manager implemented on top of ConsistentMap.
 * <p>
 * This implementation makes use of cluster manager's failure
 * detection capabilities to detect and purge stale locks.
 * TODO: Ensure lock safety and liveness.
 */
@Component(immediate = true, enabled = false)
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
    private ScheduledExecutorService retryLeaderLockExecutor;
    private ScheduledExecutorService deadLockDetectionExecutor;
    private ScheduledExecutorService leadershipStatusBroadcaster;

    private ConsistentMap<String, NodeId> lockMap;
    private AbstractListenerRegistry<LeadershipEvent, LeadershipEventListener>
        listenerRegistry;
    private final Map<String, Leadership> leaderBoard = Maps.newConcurrentMap();
    private NodeId localNodeId;

    private Set<String> activeTopics = Sets.newConcurrentHashSet();

    private static final int DELAY_BETWEEN_LEADER_LOCK_ATTEMPTS_SEC = 2;
    private static final int DEADLOCK_DETECTION_INTERVAL_SEC = 2;
    private static final int LEADERSHIP_STATUS_UPDATE_INTERVAL_SEC = 2;

    private static final KryoSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .build()
                .populate(1);
        }
    };

    @Activate
    public void activate() {
        lockMap = storageService.createConsistentMap("onos-leader-locks", new Serializer() {
            KryoNamespace kryo = new KryoNamespace.Builder()
                        .register(KryoNamespaces.API).build();

            @Override
            public <T> byte[] encode(T object) {
                return kryo.serialize(object);
            }

            @Override
            public <T> T decode(byte[] bytes) {
                return kryo.deserialize(bytes);
            }
        });

        localNodeId = clusterService.getLocalNode().id();

        messageHandlingExecutor = Executors.newSingleThreadExecutor(
                groupedThreads("onos/store/leadership", "message-handler"));
        retryLeaderLockExecutor = Executors.newScheduledThreadPool(
                4, groupedThreads("onos/store/leadership", "election-thread-%d"));
        deadLockDetectionExecutor = Executors.newSingleThreadScheduledExecutor(
                groupedThreads("onos/store/leadership", "dead-lock-detector"));
        leadershipStatusBroadcaster = Executors.newSingleThreadScheduledExecutor(
                groupedThreads("onos/store/leadership", "peer-updater"));
        clusterCommunicator.addSubscriber(
                LEADERSHIP_EVENT_MESSAGE_SUBJECT,
                new InternalLeadershipEventListener(),
                messageHandlingExecutor);

        deadLockDetectionExecutor.scheduleWithFixedDelay(
                this::purgeStaleLocks, 0, DEADLOCK_DETECTION_INTERVAL_SEC, TimeUnit.SECONDS);
        leadershipStatusBroadcaster.scheduleWithFixedDelay(
                this::sendLeadershipStatus, 0, LEADERSHIP_STATUS_UPDATE_INTERVAL_SEC, TimeUnit.SECONDS);

        listenerRegistry = new AbstractListenerRegistry<>();
        eventDispatcher.addSink(LeadershipEvent.class, listenerRegistry);

        log.info("Started.");
    }

    @Deactivate
    public void deactivate() {
        leaderBoard.forEach((topic, leadership) -> {
            if (localNodeId.equals(leadership.leader())) {
                withdraw(topic);
            }
        });

        eventDispatcher.removeSink(LeadershipEvent.class);
        clusterCommunicator.removeSubscriber(LEADERSHIP_EVENT_MESSAGE_SUBJECT);

        messageHandlingExecutor.shutdown();
        retryLeaderLockExecutor.shutdown();
        deadLockDetectionExecutor.shutdown();
        leadershipStatusBroadcaster.shutdown();

        log.info("Stopped.");
    }

    @Override
    public Map<String, Leadership> getLeaderBoard() {
        return ImmutableMap.copyOf(leaderBoard);
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
    public void runForLeadership(String path) {
        log.debug("Running for leadership for topic: {}", path);
        activeTopics.add(path);
        tryLeaderLock(path);
    }

    @Override
    public void withdraw(String path) {
        activeTopics.remove(path);
        try {
            if (lockMap.remove(path, localNodeId)) {
                log.info("Gave up leadership for {}", path);
            }
            // else we are not the current owner.
        } catch (Exception e) {
            log.debug("Failed to verify (and clear) any lock this node might be holding for {}", path, e);
        }
    }

    @Override
    public void addListener(LeadershipEventListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(LeadershipEventListener listener) {
        listenerRegistry.removeListener(listener);
    }

    private void tryLeaderLock(String path) {
        if (!activeTopics.contains(path)) {
            return;
        }
        try {
            Versioned<NodeId> currentLeader = lockMap.get(path);
            if (currentLeader != null) {
                if (localNodeId.equals(currentLeader.value())) {
                    log.info("Already has leadership for {}", path);
                    notifyNewLeader(path, localNodeId, currentLeader.version());
                } else {
                    // someone else has leadership. will retry after sometime.
                    retry(path);
                }
            } else {
                if (lockMap.putIfAbsent(path, localNodeId) == null) {
                    log.info("Assumed leadership for {}", path);
                    // do a get again to get the version (epoch)
                    Versioned<NodeId> newLeader = lockMap.get(path);
                    notifyNewLeader(path, localNodeId, newLeader.version());
                } else {
                    // someone beat us to it.
                    retry(path);
                }
            }
        } catch (Exception e) {
            log.debug("Attempt to acquire leadership lock for topic {} failed", path, e);
            retry(path);
        }
    }

    private void notifyNewLeader(String path, NodeId leader, long epoch) {
        Leadership newLeadership = new Leadership(path, leader, epoch);
        boolean updatedLeader = false;
        synchronized (leaderBoard) {
            Leadership currentLeader = leaderBoard.get(path);
            if (currentLeader == null || currentLeader.epoch() < epoch) {
                leaderBoard.put(path, newLeadership);
                updatedLeader = true;
            }
        }

        if (updatedLeader) {
            LeadershipEvent event = new LeadershipEvent(LeadershipEvent.Type.LEADER_ELECTED, newLeadership);
            eventDispatcher.post(event);
            clusterCommunicator.broadcast(
                    new ClusterMessage(
                            clusterService.getLocalNode().id(),
                            LEADERSHIP_EVENT_MESSAGE_SUBJECT,
                            SERIALIZER.encode(event)));
        }
    }

    private void notifyRemovedLeader(String path, NodeId leader, long epoch) {
        Leadership oldLeadership = new Leadership(path, leader, epoch);
        boolean updatedLeader = false;
        synchronized (leaderBoard) {
            Leadership currentLeader = leaderBoard.get(path);
            if (currentLeader != null && currentLeader.epoch() == oldLeadership.epoch()) {
                leaderBoard.remove(path);
                updatedLeader = true;
            }
        }

        if (updatedLeader) {
            LeadershipEvent event = new LeadershipEvent(LeadershipEvent.Type.LEADER_BOOTED, oldLeadership);
            eventDispatcher.post(event);
            clusterCommunicator.broadcast(
                    new ClusterMessage(
                            clusterService.getLocalNode().id(),
                            LEADERSHIP_EVENT_MESSAGE_SUBJECT,
                            SERIALIZER.encode(event)));
        }
    }

    private class InternalLeadershipEventListener implements ClusterMessageHandler {

        @Override
        public void handle(ClusterMessage message) {
            LeadershipEvent leadershipEvent =
                    SERIALIZER.decode(message.payload());

            log.debug("Leadership Event: time = {} type = {} event = {}",
                    leadershipEvent.time(), leadershipEvent.type(),
                    leadershipEvent);

            Leadership leadershipUpdate = leadershipEvent.subject();
            LeadershipEvent.Type eventType = leadershipEvent.type();
            String topic = leadershipUpdate.topic();

            boolean updateAccepted = false;

            synchronized (leaderBoard) {
                Leadership currentLeadership = leaderBoard.get(topic);
                if (eventType.equals(LeadershipEvent.Type.LEADER_ELECTED)) {
                    if (currentLeadership == null || currentLeadership.epoch() < leadershipUpdate.epoch()) {
                        leaderBoard.put(topic, leadershipUpdate);
                        updateAccepted = true;
                    }
                } else if (eventType.equals(LeadershipEvent.Type.LEADER_BOOTED)) {
                    if (currentLeadership != null && currentLeadership.epoch() == leadershipUpdate.epoch()) {
                        leaderBoard.remove(topic);
                        updateAccepted = true;
                    }
                } else {
                    throw new IllegalStateException("Unknown event type.");
                }
                if (updateAccepted) {
                    eventDispatcher.post(leadershipEvent);
                }
            }
        }
    }

    private void retry(String path) {
        retryLeaderLockExecutor.schedule(
                () -> tryLeaderLock(path),
                DELAY_BETWEEN_LEADER_LOCK_ATTEMPTS_SEC,
                TimeUnit.SECONDS);
    }

    private void purgeStaleLocks() {
        try {
            Set<Entry<String, Versioned<NodeId>>> entries = lockMap.entrySet();
            entries.forEach(entry -> {
                String path = entry.getKey();
                NodeId nodeId = entry.getValue().value();
                long epoch = entry.getValue().version();
                if (clusterService.getState(nodeId) == ControllerNode.State.INACTIVE) {
                    log.info("Lock for {} is held by {} which is currently inactive", path, nodeId);
                    try {
                        if (lockMap.remove(path, epoch)) {
                            log.info("Purged stale lock held by {} for {}", nodeId, path);
                            notifyRemovedLeader(path, nodeId, epoch);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to purge stale lock held by {} for {}", nodeId, path, e);
                    }
                }
                if (localNodeId.equals(nodeId) && !activeTopics.contains(path)) {
                    log.debug("Lock for {} is held by {} when it not running for leadership.", path, nodeId);
                    try {
                        if (lockMap.remove(path, epoch)) {
                            log.info("Purged stale lock held by {} for {}", nodeId, path);
                            notifyRemovedLeader(path, nodeId, epoch);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to purge stale lock held by {} for {}", nodeId, path, e);
                    }
                }
            });
        } catch (Exception e) {
            log.debug("Failed cleaning up stale locks", e);
        }
    }

    private void sendLeadershipStatus() {
        try {
            leaderBoard.forEach((path, leadership) -> {
                if (leadership.leader().equals(localNodeId)) {
                    LeadershipEvent event = new LeadershipEvent(LeadershipEvent.Type.LEADER_ELECTED, leadership);
                    clusterCommunicator.broadcast(
                            new ClusterMessage(
                                    clusterService.getLocalNode().id(),
                                    LEADERSHIP_EVENT_MESSAGE_SUBJECT,
                                    SERIALIZER.encode(event)));
                }
            });
        } catch (Exception e) {
            log.debug("Failed to send leadership updates", e);
        }
    }
}
