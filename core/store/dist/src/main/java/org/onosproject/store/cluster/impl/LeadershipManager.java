/*
 * Copyright 2014 Open Networking Laboratory
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

import static com.google.common.base.Preconditions.checkArgument;
import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.service.Lock;
import org.onosproject.store.service.LockService;
import org.onosproject.store.service.impl.DistributedLockManager;
import org.onlab.util.KryoNamespace;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Distributed implementation of LeadershipService that is based on the primitives exposed by
 * LockService.
 */
@Component(enabled = false)
@Service
public class LeadershipManager implements LeadershipService {

    private final Logger log = getLogger(getClass());

    // TODO: Remove this dependency
    private static final int TERM_DURATION_MS =
            DistributedLockManager.DEAD_LOCK_TIMEOUT_MS;

    // Time to wait before retrying leadership after
    // a unexpected error.
    private static final int WAIT_BEFORE_RETRY_MS = 2000;

    // TODO: Make Thread pool size configurable.
    private final ScheduledExecutorService threadPool =
            Executors.newScheduledThreadPool(25, namedThreads("leadership-manager-%d"));

    private static final MessageSubject LEADERSHIP_UPDATES =
            new MessageSubject("leadership-contest-updates");

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private LockService lockService;

    private final Map<String, Leadership> leaderBoard = Maps.newHashMap();

    private final Map<String, Lock> openContests = Maps.newConcurrentMap();
    private final Set<LeadershipEventListener> listeners = Sets.newIdentityHashSet();
    private NodeId localNodeId;

    private final LeadershipEventListener peerAdvertiser = new PeerAdvertiser();
    private final LeadershipEventListener leaderBoardUpdater = new LeaderBoardUpdater();

    public static final KryoSerializer SERIALIZER = new KryoSerializer() {
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
        localNodeId = clusterService.getLocalNode().id();

        addListener(peerAdvertiser);
        addListener(leaderBoardUpdater);

        clusterCommunicator.addSubscriber(
                LEADERSHIP_UPDATES,
                new PeerAdvertisementHandler());

        log.info("Started.");
    }

    @Deactivate
    public void deactivate() {
        removeListener(peerAdvertiser);
        removeListener(leaderBoardUpdater);

        clusterCommunicator.removeSubscriber(LEADERSHIP_UPDATES);

        threadPool.shutdown();

        log.info("Stopped.");
    }


    @Override
    public NodeId getLeader(String path) {
        synchronized (leaderBoard) {
            Leadership leadership = leaderBoard.get(path);
            if (leadership != null) {
                return leadership.leader();
            }
        }
        return null;
    }

    @Override
    public void runForLeadership(String path) {
        checkArgument(path != null);

        if (openContests.containsKey(path)) {
            log.info("Already in the leadership contest for {}", path);
            return;
        } else {
            Lock lock = lockService.create(path);
            openContests.put(path, lock);
            threadPool.schedule(new TryLeadership(lock), 0, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void withdraw(String path) {
        checkArgument(path != null);

        Lock lock = openContests.remove(path);

        if (lock != null && lock.isLocked()) {
            lock.unlock();
            notifyListeners(
                    new LeadershipEvent(
                            LeadershipEvent.Type.LEADER_BOOTED,
                            new Leadership(lock.path(), localNodeId, lock.epoch())));
        }
    }

    @Override
    public Map<String, Leadership> getLeaderBoard() {
        return ImmutableMap.copyOf(leaderBoard);
    }

    @Override
    public void addListener(LeadershipEventListener listener) {
        checkArgument(listener != null);
        listeners.add(listener);
    }

    @Override
    public void removeListener(LeadershipEventListener listener) {
        checkArgument(listener != null);
        listeners.remove(listener);
    }

    private void notifyListeners(LeadershipEvent event) {
        for (LeadershipEventListener listener : listeners) {
            try {
                listener.event(event);
            } catch (Exception e) {
                log.error("Notifying listener failed with exception.", e);
            }
        }
    }

    private void tryAcquireLeadership(String path) {
        Lock lock = openContests.get(path);
        if (lock == null) {
            // withdrew from race.
            return;
        }
        lock.lockAsync(TERM_DURATION_MS).whenComplete((response, error) -> {
            if (error == null) {
                threadPool.schedule(
                        new ReelectionTask(lock),
                        TERM_DURATION_MS / 2,
                        TimeUnit.MILLISECONDS);
                notifyListeners(
                        new LeadershipEvent(
                                LeadershipEvent.Type.LEADER_ELECTED,
                                new Leadership(lock.path(), localNodeId, lock.epoch())));
                return;
            } else {
                log.warn("Failed to acquire lock for {}. Will retry in {} ms", path, WAIT_BEFORE_RETRY_MS, error);
                threadPool.schedule(new TryLeadership(lock), WAIT_BEFORE_RETRY_MS, TimeUnit.MILLISECONDS);
            }
        });
    }

    private class ReelectionTask implements Runnable {

        private final Lock lock;

        public ReelectionTask(Lock lock) {
           this.lock = lock;
        }

        @Override
        public void run() {
            if (!openContests.containsKey(lock.path())) {
                log.debug("Node withdrew from leadership race for {}. Cancelling reelection task.", lock.path());
                return;
            }

            boolean lockExtended = false;
            try {
                lockExtended = lock.extendExpiration(TERM_DURATION_MS);
            } catch (Exception e) {
                log.warn("Attempt to extend lock failed with an exception.", e);
            }

            if (lockExtended) {
                notifyListeners(
                        new LeadershipEvent(
                                LeadershipEvent.Type.LEADER_REELECTED,
                                new Leadership(lock.path(), localNodeId, lock.epoch())));
                threadPool.schedule(this, TERM_DURATION_MS / 2, TimeUnit.MILLISECONDS);
            } else {
                // Check if this node already withdrew from the contest, in which case
                // we don't need to notify here.
                if (openContests.containsKey(lock.path())) {
                    notifyListeners(
                            new LeadershipEvent(
                                    LeadershipEvent.Type.LEADER_BOOTED,
                                    new Leadership(lock.path(), localNodeId, lock.epoch())));
                    // Retry leadership after a brief wait.
                    threadPool.schedule(new TryLeadership(lock), WAIT_BEFORE_RETRY_MS, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    private class TryLeadership implements Runnable {
        private final Lock lock;

        public TryLeadership(Lock lock) {
           this.lock = lock;
        }

        @Override
        public void run() {
            tryAcquireLeadership(lock.path());
        }
    }

    private class PeerAdvertiser implements LeadershipEventListener {
        @Override
        public void event(LeadershipEvent event) {
            // publish events originating on this host.
            if (event.subject().leader().equals(localNodeId)) {
                try {
                    clusterCommunicator.broadcast(
                            new ClusterMessage(
                                    localNodeId,
                                    LEADERSHIP_UPDATES,
                                    SERIALIZER.encode(event)));
                } catch (IOException e) {
                    log.error("Failed to broadcast leadership update message", e);
                }
            }
        }
    }

    private class PeerAdvertisementHandler implements ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {
            LeadershipEvent event = SERIALIZER.decode(message.payload());
            log.trace("Received {} from {}", event, message.sender());
            notifyListeners(event);
        }
    }

    private class LeaderBoardUpdater implements LeadershipEventListener {
        @Override
        public void event(LeadershipEvent event) {
            Leadership leadershipUpdate = event.subject();
            synchronized (leaderBoard) {
                Leadership currentLeadership = leaderBoard.get(leadershipUpdate.topic());
                switch (event.type()) {
                case LEADER_ELECTED:
                case LEADER_REELECTED:
                        if (currentLeadership == null || currentLeadership.epoch() < leadershipUpdate.epoch()) {
                            leaderBoard.put(leadershipUpdate.topic(), leadershipUpdate);
                        }
                    break;
                case LEADER_BOOTED:
                    if (currentLeadership != null && currentLeadership.epoch() <= leadershipUpdate.epoch()) {
                        leaderBoard.remove(leadershipUpdate.topic());
                    }
                    break;
                default:
                    break;
                }
            }
        }
    }
}
