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
package org.onosproject.store.cluster.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipStore;
import org.onosproject.cluster.LeadershipStoreDelegate;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.Change;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.service.LeaderElector;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

/**
 * Implementation of {@code LeadershipStore} that makes use of a {@link LeaderElector}
 * primitive.
 */
@Service
@Component(immediate = true, enabled = true)
public class DistributedLeadershipStore
    extends AbstractStore<LeadershipEvent, LeadershipStoreDelegate>
    implements LeadershipStore {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private NodeId localNodeId;
    private LeaderElector leaderElector;

    private final Consumer<Change<Leadership>> leadershipChangeListener =
            change -> {
                Leadership oldValue = change.oldValue();
                Leadership newValue = change.newValue();
                boolean leaderChanged = !Objects.equals(oldValue.leader(), newValue.leader());
                boolean candidatesChanged = !Objects.equals(oldValue.candidates(), newValue.candidates());
                LeadershipEvent.Type eventType = null;
                if (leaderChanged && candidatesChanged) {
                    eventType = LeadershipEvent.Type.LEADER_AND_CANDIDATES_CHANGED;
                }
                if (leaderChanged && !candidatesChanged) {
                    eventType = LeadershipEvent.Type.LEADER_CHANGED;
                }
                if (!leaderChanged && candidatesChanged) {
                    eventType = LeadershipEvent.Type.CANDIDATES_CHANGED;
                }
                notifyDelegate(new LeadershipEvent(eventType, change.newValue()));
            };

    @Activate
    public void activate() {
        localNodeId = clusterService.getLocalNode().id();
        leaderElector = storageService.leaderElectorBuilder()
                      .withName("onos-leadership-elections")
                      .build()
                      .asLeaderElector();
        leaderElector.addChangeListener(leadershipChangeListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        leaderElector.removeChangeListener(leadershipChangeListener);
        log.info("Stopped");
    }

    @Override
    public Leadership addRegistration(String topic) {
        return leaderElector.run(topic, localNodeId);
    }

    @Override
    public void removeRegistration(String topic) {
        leaderElector.withdraw(topic);
    }

    @Override
    public void removeRegistration(NodeId nodeId) {
        leaderElector.evict(nodeId);
    }

    @Override
    public boolean moveLeadership(String topic, NodeId toNodeId) {
        return leaderElector.anoint(topic, toNodeId);
    }

    @Override
    public boolean makeTopCandidate(String topic, NodeId nodeId) {
        return leaderElector.promote(topic, nodeId);
    }

    @Override
    public Leadership getLeadership(String topic) {
        return leaderElector.getLeadership(topic);
    }

    @Override
    public Map<String, Leadership> getLeaderships() {
        return leaderElector.getLeaderships();
    }
}
