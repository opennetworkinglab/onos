/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.net.intent.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages the assignment of intent keyspace partitions to instances.
 */
@Component(immediate = true)
@Service
public class PartitionManager implements PartitionService {

    private static final Logger log = LoggerFactory.getLogger(PartitionManager.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    private static final int NUM_PARTITIONS = 14;
    private static final int BACKOFF_TIME = 2;
    private static final int CHECK_PERIOD = 10;

    private static final String ELECTION_PREFIX = "intent-partition-";

    private LeadershipEventListener leaderListener = new InternalLeadershipListener();
    private ClusterEventListener clusterListener = new InternalClusterEventListener();

    private final Set<PartitionId> myPartitions
            = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private ScheduledExecutorService executor = Executors
            .newScheduledThreadPool(1);

    @Activate
    public void activate() {
        leadershipService.addListener(leaderListener);
        clusterService.addListener(clusterListener);

        for (int i = 0; i < NUM_PARTITIONS; i++) {
            leadershipService.runForLeadership(getPartitionPath(i));
        }

        executor.scheduleAtFixedRate(this::doRelinquish, 0,
                                     CHECK_PERIOD, TimeUnit.SECONDS);
    }

    @Deactivate
    public void deactivate() {
        leadershipService.removeListener(leaderListener);
        clusterService.removeListener(clusterListener);
    }

    private String getPartitionPath(int i) {
        return ELECTION_PREFIX + i;
    }

    private PartitionId getPartitionForKey(Key intentKey) {
        int partition = Math.abs((int) intentKey.hash()) % NUM_PARTITIONS;
        //TODO investigate Guava consistent hash method
        // ... does it add significant computational complexity? is it worth it?
        //int partition = consistentHash(intentKey.hash(), NUM_PARTITIONS);
        PartitionId id = new PartitionId(partition);
        log.debug("Getting partition for {}: {}", intentKey, id); //FIXME debug
        return id;
    }

    @Override
    public boolean isMine(Key intentKey) {
        return myPartitions.contains(getPartitionForKey(intentKey));
    }

    private void doRelinquish() {
        try {
            relinquish();
        } catch (Exception e) {
            log.warn("Exception caught during relinquish task", e);
        }
    }


    /**
     * Determine whether we have more than our fair share of partitions, and if
     * so, relinquish leadership of some of them for a little while to let
     * other instances take over.
     */
    private void relinquish() {
        int activeNodes = (int) clusterService.getNodes()
                .stream()
                .filter(n -> clusterService.getState(n.id())
                        == ControllerNode.State.ACTIVE)
                .count();

        int myShare = (int) Math.ceil((double) NUM_PARTITIONS / activeNodes);

        synchronized (myPartitions) {
            int relinquish = myPartitions.size() - myShare;

            if (relinquish <= 0) {
                return;
            }

            Iterator<PartitionId> it = myPartitions.iterator();
            for (int i = 0; i < relinquish; i++) {
                PartitionId id = it.next();
                it.remove();

                leadershipService.withdraw(getPartitionPath(id.value()));

                executor.schedule(() -> recontest(getPartitionPath(id.value())),
                                  BACKOFF_TIME, TimeUnit.SECONDS);
            }
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
            // update internal state about which partitions I'm leader of
            if (leadership.leader().equals(clusterService.getLocalNode().id()) &&
                    leadership.topic().startsWith(ELECTION_PREFIX)) {

                // Parse out the partition ID
                String[] splitted = leadership.topic().split("-");
                if (splitted.length != 3) {
                    log.warn("Couldn't parse leader election topic {}", leadership.topic());
                    return;
                }

                int partitionId;
                try {
                    partitionId = Integer.parseInt(splitted[2]);
                } catch (NumberFormatException e) {
                    log.warn("Couldn't parse partition ID {}", splitted[2]);
                    return;
                }

                synchronized (myPartitions) {
                    if (event.type() == LeadershipEvent.Type.LEADER_ELECTED) {
                        myPartitions.add(new PartitionId(partitionId));
                    } else if (event.type() == LeadershipEvent.Type.LEADER_BOOTED) {
                        myPartitions.remove(new PartitionId(partitionId));
                    }
                }

                // See if we need to let some partitions go
                relinquish();
            }
        }
    }

    private final class InternalClusterEventListener implements
            ClusterEventListener {

        @Override
        public void event(ClusterEvent event) {
            relinquish();
        }
    }
}
