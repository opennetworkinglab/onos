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
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

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

    // TODO make configurable
    private static final int NUM_PARTITIONS = 100;

    private static final String ELECTION_PREFIX = "intent-partition-";

    private LeadershipEventListener leaderListener = new InternalLeadershipListener();

    private Set<PartitionId> myPartitions;

    @Activate
    public void activate() {
        myPartitions = Collections.newSetFromMap(new ConcurrentHashMap<>());

        leadershipService.addListener(leaderListener);

        for (int i = 0; i < NUM_PARTITIONS; i++) {
            leadershipService.runForLeadership(ELECTION_PREFIX + i);
        }
    }

    @Deactivate
    public void deactivate() {
        leadershipService.removeListener(leaderListener);
    }

    private PartitionId getPartitionForKey(String intentKey) {
        return new PartitionId(intentKey.hashCode() % NUM_PARTITIONS);
    }

    @Override
    public boolean isMine(String intentKey) {
        return checkNotNull(
                myPartitions.contains(getPartitionForKey(intentKey)));
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

                if (event.type() == LeadershipEvent.Type.LEADER_ELECTED) {
                    myPartitions.add(new PartitionId(partitionId));
                } else if (event.type() == LeadershipEvent.Type.LEADER_BOOTED) {
                    myPartitions.remove(new PartitionId(partitionId));
                }
            }

        }
    }
}
