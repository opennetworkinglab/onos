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

import com.google.common.collect.Maps;
import com.hazelcast.config.TopicConfig;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.AbstractListenerRegistry;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.store.hz.StoreService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.KryoSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onlab.util.Tools.namedThreads;

/**
 * Distributed implementation of LeadershipService that is based on Hazelcast.
 * <p>
 * The election is eventually-consistent: if there is Hazelcast partitioning,
 * and the partitioning is healed, there could be a short window of time
 * until the leaders in each partition discover each other. If this happens,
 * the leaders release the leadership and run again for election.
 * </p>
 * <p>
 * The leader election is based on Hazelcast's Global Lock, which is stongly
 * consistent. In addition, each leader periodically advertises events
 * (using a Hazelcast Topic) that it is the elected leader. Those events are
 * used for two purposes: (1) Discover multi-leader collisions (in case of
 * healed Hazelcast partitions), and (2) Inform all listeners who is
 * the current leader (e.g., for informational purpose).
 * </p>
 */
@Component(immediate = true)
@Service
public class HazelcastLeadershipService implements LeadershipService,
                                        MessageListener<byte[]> {
    private static final Logger log =
        LoggerFactory.getLogger(HazelcastLeadershipService.class);

    private static final KryoSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .build()
                .populate(1);
        }
    };

    private static final long LEADERSHIP_PERIODIC_INTERVAL_MS = 5 * 1000; // 5s
    private static final long LEADERSHIP_REMOTE_TIMEOUT_MS = 15 * 1000;  // 15s
    private static final String TOPIC_HZ_ID = "LeadershipService/AllTopics";

    // indicates there is no term value yet
    private static final long NO_TERM = 0;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StoreService storeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    private AbstractListenerRegistry<LeadershipEvent, LeadershipEventListener>
        listenerRegistry;
    private final Map<String, Topic> topics = Maps.newConcurrentMap();
    private NodeId localNodeId;

    private ITopic<byte[]> leaderTopic;
    private String leaderTopicRegistrationId;

    @Activate
    protected void activate() {
        localNodeId = clusterService.getLocalNode().id();
        listenerRegistry = new AbstractListenerRegistry<>();
        eventDispatcher.addSink(LeadershipEvent.class, listenerRegistry);

        TopicConfig topicConfig = new TopicConfig();
        topicConfig.setGlobalOrderingEnabled(true);
        topicConfig.setName(TOPIC_HZ_ID);
        storeService.getHazelcastInstance().getConfig().addTopicConfig(topicConfig);
        leaderTopic = storeService.getHazelcastInstance().getTopic(TOPIC_HZ_ID);
        leaderTopicRegistrationId = leaderTopic.addMessageListener(this);

        log.info("Hazelcast Leadership Service started");
    }

    @Deactivate
    protected void deactivate() {
        eventDispatcher.removeSink(LeadershipEvent.class);
        leaderTopic.removeMessageListener(leaderTopicRegistrationId);

        for (Topic topic : topics.values()) {
            topic.stop();
        }
        topics.clear();

        log.info("Hazelcast Leadership Service stopped");
    }

    @Override
    public NodeId getLeader(String path) {
        Topic topic = topics.get(path);
        if (topic == null) {
            return null;
        }
        return topic.leader();
    }

    @Override
    public void runForLeadership(String path) {
        checkArgument(path != null);
        Topic topic = new Topic(path);
        Topic oldTopic = topics.putIfAbsent(path, topic);
        if (oldTopic == null) {
            topic.start();
            topic.runForLeadership();
        } else {
            oldTopic.runForLeadership();
        }
    }

    @Override
    public void withdraw(String path) {
        checkArgument(path != null);
        Topic topic = topics.get(path);
        if (topic != null) {
            topic.stop();
            topics.remove(path, topic);
        }
    }

    @Override
    public Map<String, Leadership> getLeaderBoard() {
        Map<String, Leadership> result = new HashMap<>();

        for (Topic topic : topics.values()) {
            Leadership leadership = new Leadership(topic.topicName(),
                                                   topic.leader(),
                                                   topic.term());
            result.put(topic.topicName(), leadership);
        }
        return result;
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
    public void onMessage(Message<byte[]> message) {
        LeadershipEvent leadershipEvent =
            SERIALIZER.decode(message.getMessageObject());

        log.debug("Leadership Event: time = {} type = {} event = {}",
                  leadershipEvent.time(), leadershipEvent.type(),
                  leadershipEvent);

        //
        // If there is no entry for the topic, then create a new one to
        // keep track of the leadership, but don't run for leadership itself.
        //
        String topicName = leadershipEvent.subject().topic();
        Topic topic = topics.get(topicName);
        if (topic == null) {
            topic = new Topic(topicName);
            Topic oldTopic = topics.putIfAbsent(topicName, topic);
            if (oldTopic == null) {
                topic.start();
            } else {
                topic = oldTopic;
            }
        }
        topic.receivedLeadershipEvent(leadershipEvent);
        eventDispatcher.post(leadershipEvent);
    }

    /**
     * Class for keeping per-topic information.
     */
    private final class Topic {
        private final String topicName;
        private volatile boolean isShutdown = true;
        private volatile boolean isRunningForLeadership = false;
        private volatile long lastLeadershipUpdateMs = 0;
        private ExecutorService leaderElectionExecutor;

        private volatile IAtomicLong term;
        // This is local state, recording the term number for the last time
        // this instance was leader for this topic. The current term could be
        // higher if the mastership has changed any times.
        private long myLastLeaderTerm = NO_TERM;

        private NodeId leader;
        private Lock leaderLock;
        private Future<?> getLockFuture;
        private Future<?> periodicProcessingFuture;

        /**
         * Constructor.
         *
         * @param topicName the topic name
         */
        private Topic(String topicName) {
            this.topicName = topicName;
        }

        /**
         * Gets the topic name.
         *
         * @return the topic name
         */
        private String topicName() {
            return topicName;
        }

        /**
         * Gets the leader for the topic.
         *
         * @return the leader for the topic
         */
        private NodeId leader() {
            return leader;
        }

        /**
         * Gets the current term for the topic.
         *
         * @return the term for the topic
         */
        private long term() {
            if (term == null) {
                return NO_TERM;
            }
            return term.get();
        }

        /**
         * Starts operation.
         */
        private void start() {
            isShutdown = false;
            String threadPoolName = "onos-leader-election-" + topicName + "-%d";
            leaderElectionExecutor = Executors.newScheduledThreadPool(2,
                                        namedThreads(threadPoolName));

            periodicProcessingFuture =
                leaderElectionExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        doPeriodicProcessing();
                    }
                });
        }

        /**
         * Runs for leadership.
         */
        private void runForLeadership() {
            if (isRunningForLeadership) {
                return;         // Nothing to do: already running
            }
            if (isShutdown) {
                start();
            }
            String lockHzId = "LeadershipService/" + topicName + "/lock";
            String termHzId = "LeadershipService/" + topicName + "/term";
            leaderLock = storeService.getHazelcastInstance().getLock(lockHzId);
            term = storeService.getHazelcastInstance().getAtomicLong(termHzId);

            getLockFuture = leaderElectionExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        doLeaderElectionThread();
                    }
                });
        }

        /**
         * Stops leadership election for the topic.
         */
        private void stop() {
            isShutdown = true;
            isRunningForLeadership = false;
            // getLockFuture.cancel(true);
            // periodicProcessingFuture.cancel(true);
            leaderElectionExecutor.shutdownNow();
        }

        /**
         * Received a Leadership Event.
         *
         * @param leadershipEvent the received Leadership Event
         */
        private void receivedLeadershipEvent(LeadershipEvent leadershipEvent) {
            NodeId eventLeaderId = leadershipEvent.subject().leader();
            if (!leadershipEvent.subject().topic().equals(topicName)) {
                return;         // Not our topic: ignore
            }
            if (eventLeaderId.equals(localNodeId)) {
                return;         // My own message: ignore
            }

            synchronized (this) {
                switch (leadershipEvent.type()) {
                case LEADER_ELECTED:
                    // FALLTHROUGH
                case LEADER_REELECTED:
                    //
                    // Another leader: if we are also a leader, then give up
                    // leadership and run for re-election.
                    //
                    if ((leader != null) && leader.equals(localNodeId)) {
                        if (getLockFuture != null) {
                            getLockFuture.cancel(true);
                        }
                    } else {
                        // Just update the current leader
                        leader = leadershipEvent.subject().leader();
                        lastLeadershipUpdateMs = System.currentTimeMillis();
                    }
                    break;
                case LEADER_BOOTED:
                    // Remove the state for the current leader
                    if ((leader != null) && eventLeaderId.equals(leader)) {
                        leader = null;
                    }
                    break;
                default:
                    break;
                }
            }
        }

        private void doPeriodicProcessing() {

            while (!isShutdown) {

                //
                // Periodic tasks:
                // (a) Advertise ourselves as the leader
                //   OR
                // (b) Expire a stale (remote) leader
                //
                synchronized (this) {
                    LeadershipEvent leadershipEvent;
                    if (leader != null) {
                        if (leader.equals(localNodeId)) {
                            //
                            // Advertise ourselves as the leader
                            //
                            leadershipEvent = new LeadershipEvent(
                                LeadershipEvent.Type.LEADER_REELECTED,
                                new Leadership(topicName, localNodeId, myLastLeaderTerm));
                            // Dispatch to all instances
                            leaderTopic.publish(SERIALIZER.encode(leadershipEvent));
                        } else {
                            //
                            // Test if time to expire a stale leader
                            //
                            long delta = System.currentTimeMillis() -
                                lastLeadershipUpdateMs;
                            if (delta > LEADERSHIP_REMOTE_TIMEOUT_MS) {
                                leadershipEvent = new LeadershipEvent(
                                        LeadershipEvent.Type.LEADER_BOOTED,
                                        new Leadership(topicName, leader, myLastLeaderTerm));
                                // Dispatch only to the local listener(s)
                                eventDispatcher.post(leadershipEvent);
                                leader = null;
                            }
                        }
                    }
                }

                // Sleep before re-advertising
                try {
                    Thread.sleep(LEADERSHIP_PERIODIC_INTERVAL_MS);
                } catch (InterruptedException e) {
                    log.debug("Leader Election periodic thread interrupted");
                }
            }
        }

        /**
         * Performs the leader election by using Hazelcast.
         */
        private void doLeaderElectionThread() {

            while (!isShutdown) {
                LeadershipEvent leadershipEvent;
                //
                // Try to acquire the lock and keep it until the instance is
                // shutdown.
                //
                log.debug("Leader Election begin for topic {}",
                          topicName);
                try {
                    // Block until it becomes the leader
                    leaderLock.lockInterruptibly();
                } catch (InterruptedException e) {
                    //
                    // Thread interrupted. Either shutdown or run for
                    // re-election.
                    //
                    log.debug("Election interrupted for topic {}",
                              topicName);
                    continue;
                }

                synchronized (this) {
                    //
                    // This instance is now the leader
                    //
                    log.info("Leader Elected for topic {}", topicName);

                    updateTerm();

                    leader = localNodeId;
                    leadershipEvent = new LeadershipEvent(
                        LeadershipEvent.Type.LEADER_ELECTED,
                        new Leadership(topicName, localNodeId, myLastLeaderTerm));
                    leaderTopic.publish(SERIALIZER.encode(leadershipEvent));
                }

                try {
                    // Sleep forever until interrupted
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    //
                    // Thread interrupted. Either shutdown or run for
                    // re-election.
                    //
                    log.debug("Leader Interrupted for topic {}",
                              topicName);
                }

                synchronized (this) {
                    // If we reach here, we should release the leadership
                    log.debug("Leader Lock Released for topic {}", topicName);
                    if ((leader != null) &&
                        leader.equals(localNodeId)) {
                        leader = null;
                    }
                    leadershipEvent = new LeadershipEvent(
                                LeadershipEvent.Type.LEADER_BOOTED,
                                new Leadership(topicName, localNodeId, myLastLeaderTerm));
                    leaderTopic.publish(SERIALIZER.encode(leadershipEvent));
                    leaderLock.unlock();
                }
            }

        }

        // Globally guarded by the leadership lock for this term
        // Locally guarded by synchronized (this)
        private void updateTerm() {
            long oldTerm = term.get();
            long newTerm = term.incrementAndGet();
            myLastLeaderTerm = newTerm;
            log.debug("Topic {} updated term from {} to {}", topicName,
                      oldTerm, newTerm);
        }
    }
}
