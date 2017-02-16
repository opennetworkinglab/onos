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
package org.onosproject.store.intent.impl;

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
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.intent.WorkPartitionEvent;
import org.onosproject.net.intent.WorkPartitionEventListener;
import org.onosproject.net.intent.WorkPartitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onlab.util.Tools.groupedThreads;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Manages the assignment of work partitions to instances.
 */
@Component(immediate = true)
@Service
public class WorkPartitionManager implements WorkPartitionService {

    private static final Logger log = LoggerFactory.getLogger(WorkPartitionManager.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    protected final AtomicBoolean rebalanceScheduled = new AtomicBoolean(false);

    static final int NUM_PARTITIONS = 14;
    private static final int BACKOFF_TIME = 2;
    private static final int CHECK_PARTITION_BALANCE_PERIOD_SEC = 10;
    private static final int RETRY_AFTER_DELAY_SEC = 5;

    private static final String ELECTION_PREFIX = "work-partition-";

    protected NodeId localNodeId;
    private ListenerRegistry<WorkPartitionEvent, WorkPartitionEventListener> listenerRegistry;
    private LeadershipEventListener leaderListener = new InternalLeadershipListener();

    private ScheduledExecutorService executor = Executors
            .newScheduledThreadPool(1, groupedThreads("work-parition", "balancer-%d", log));

    @Activate
    public void activate() {
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.addListener(leaderListener);

        listenerRegistry = new ListenerRegistry<>();
        eventDispatcher.addSink(WorkPartitionEvent.class, listenerRegistry);

        for (int i = 0; i < NUM_PARTITIONS; i++) {
            leadershipService.runForLeadership(getPartitionPath(i));
            log.debug("Registered to run for {}", getPartitionPath(i));
        }

        executor.scheduleAtFixedRate(() -> scheduleRebalance(0), 0,
                                     CHECK_PARTITION_BALANCE_PERIOD_SEC, TimeUnit.SECONDS);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        executor.shutdownNow();

        eventDispatcher.removeSink(WorkPartitionEvent.class);
        leadershipService.removeListener(leaderListener);
        log.info("Stopped");
    }

    /**
     * Sets the specified executor to be used for scheduling background tasks.
     *
     * @param executor scheduled executor service for background tasks
     * @return this WorkPartitionManager
     */
    WorkPartitionManager withScheduledExecutor(ScheduledExecutorService executor) {
        this.executor = executor;
        return this;
    }

    private String getPartitionPath(int i) {
        return ELECTION_PREFIX + i;
    }

    @Override
    public <K> boolean isMine(K id, Function<K, Long> hasher) {
        return Objects.equals(localNodeId, getLeader(id, hasher));
    }

    @Override
    public <K> NodeId getLeader(K id, Function<K, Long> hasher) {
        int partition = Math.abs(hasher.apply(id).intValue()) % NUM_PARTITIONS;
        PartitionId partitionId = new PartitionId(partition);
        return leadershipService.getLeadership(getPartitionPath(partitionId.value())).leaderNodeId();
    }

    @Override
    public void addListener(WorkPartitionEventListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(WorkPartitionEventListener listener) {
        listenerRegistry.removeListener(listener);
    }

    void doRebalance() {
        rebalanceScheduled.set(false);
        try {
            rebalance();
        } catch (Exception e) {
            log.warn("{} caught during rebalance task. Will retry in " +
                     RETRY_AFTER_DELAY_SEC + " seconds", e.getMessage());
            scheduleRebalance(RETRY_AFTER_DELAY_SEC);
        }
    }

    /**
     * Determine whether we have more than our fair share of partitions, and if
     * so, relinquish leadership of some of them for a little while to let
     * other instances take over.
     */
    private void rebalance() {
        int activeNodes = (int) clusterService.getNodes()
                .stream()
                .filter(node -> clusterService.getState(node.id()).isActive())
                .count();

        int myShare = (int) Math.ceil((double) NUM_PARTITIONS / activeNodes);

        // First make sure this node is a candidate for all partitions.
        IntStream.range(0, NUM_PARTITIONS)
                 .mapToObj(this::getPartitionPath)
                 .map(leadershipService::getLeadership)
                 .filter(leadership -> !leadership.candidates().contains(localNodeId))
                 .map(Leadership::topic)
                 .forEach(leadershipService::runForLeadership);

        List<String> myPartitions = IntStream.range(0, NUM_PARTITIONS)
                                             .mapToObj(this::getPartitionPath)
                                             .map(leadershipService::getLeadership)
                                             .filter(Objects::nonNull)
                                             .filter(leadership -> localNodeId.equals(leadership.leaderNodeId()))
                                             .map(Leadership::topic)
                                             .collect(Collectors.toList());

        int relinquish = myPartitions.size() - myShare;


        for (int i = 0; i < relinquish; i++) {
            String topic = myPartitions.get(i);
            // Wait till all active nodes are in contention for partition ownership.
            // This avoids too many relinquish/reclaim cycles.
            if (leadershipService.getCandidates(topic).size() == activeNodes) {
                leadershipService.withdraw(topic);
                executor.schedule(() -> recontest(topic), BACKOFF_TIME, TimeUnit.SECONDS);
            }
        }
    }

    private void scheduleRebalance(int afterDelaySec) {
        if (rebalanceScheduled.compareAndSet(false, true)) {
            executor.schedule(this::doRebalance, afterDelaySec, TimeUnit.SECONDS);
        }
    }

    /**
     * Try and recontest for leadership of a partition.
     *
     * @param path topic name to recontest
     */
    private void recontest(String path) {
        leadershipService.runForLeadership(path);
    }

    private final class InternalLeadershipListener implements LeadershipEventListener {

        @Override
        public void event(LeadershipEvent event) {
            Leadership leadership = event.subject();

            if (Objects.equals(leadership.leaderNodeId(), localNodeId) &&
                    leadership.topic().startsWith(ELECTION_PREFIX)) {

                eventDispatcher.post(new WorkPartitionEvent(WorkPartitionEvent.Type.LEADER_CHANGED,
                                                        leadership.topic()));
            }

            if (event.type() == LeadershipEvent.Type.CANDIDATES_CHANGED) {
                scheduleRebalance(0);
            }
        }
    }
}
