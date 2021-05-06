/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.cluster.impl;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipAdminService;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.LeadershipStore;
import org.onosproject.cluster.LeadershipStoreDelegate;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.AbstractListenerManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of {@link LeadershipService} and {@link LeadershipAdminService}.
 */
@Component(immediate = true, service = {LeadershipService.class, LeadershipAdminService.class})
public class LeadershipManager
    extends AbstractListenerManager<LeadershipEvent, LeadershipEventListener>
    implements LeadershipService, LeadershipAdminService {

    private final Logger log = getLogger(getClass());

    private LeadershipStoreDelegate delegate = this::post;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipStore store;

    private NodeId localNodeId;

    @Activate
    public void activate() {
        localNodeId = clusterService.getLocalNode().id();
        store.setDelegate(delegate);
        eventDispatcher.addSink(LeadershipEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        Maps.filterValues(store.getLeaderships(), v -> v.candidates().contains(localNodeId))
            .keySet()
            .forEach(this::withdraw);
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(LeadershipEvent.class);
        log.info("Stopped");
    }

    @Override
    public Leadership getLeadership(String topic) {
        return store.getLeadership(topic);
    }

    @Override
    public Set<String> ownedTopics(NodeId nodeId) {
        return Maps.filterValues(store.getLeaderships(), v -> Objects.equal(nodeId, v.leaderNodeId())).keySet();
    }

    @Override
    public Leadership runForLeadership(String topic) {
        return store.addRegistration(topic);
    }

    @Override
    public void withdraw(String topic) {
        store.removeRegistration(topic);
    }

    @Override
    public Map<String, Leadership> getLeaderBoard() {
        return store.getLeaderships();
    }

    @Override
    public boolean demote(String topic, NodeId nodeId) {
        return store.demote(topic, nodeId);
    }

    @Override
    public boolean transferLeadership(String topic, NodeId to) {
        return store.moveLeadership(topic, to);
    }

    @Override
    public void unregister(NodeId nodeId) {
        store.removeRegistration(nodeId);
    }

    @Override
    public boolean promoteToTopOfCandidateList(String topic, NodeId nodeId) {
        return store.makeTopCandidate(topic, nodeId);
    }
}
