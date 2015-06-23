/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.store.trivial;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEvent.Type;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;

/**
 * A trivial implementation of the leadership service.
 * <p>
 * The service is not distributed, so it can assume there's a single leadership
 * contender. This contender is always granted leadership whenever it asks.
 */
@Component(immediate = true)
@Service
public class SimpleLeadershipManager implements LeadershipService {

    private Set<LeadershipEventListener> listeners = new CopyOnWriteArraySet<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterService clusterService;

    private Map<String, Boolean> elections = new ConcurrentHashMap<>();

    @Override
    public NodeId getLeader(String path) {
        return elections.get(path) ? clusterService.getLocalNode().id() : null;
    }

    @Override
    public Leadership getLeadership(String path) {
        checkArgument(path != null);
        return elections.get(path) ? new Leadership(path, clusterService.getLocalNode().id(), 0, 0) : null;
    }

    @Override
    public Set<String> ownedTopics(NodeId nodeId) {
        checkArgument(nodeId != null);
        return elections.entrySet()
                .stream()
                .filter(Entry::getValue)
                .map(Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public CompletableFuture<Leadership> runForLeadership(String path) {
        elections.put(path, true);
        for (LeadershipEventListener listener : listeners) {
            listener.event(new LeadershipEvent(Type.LEADER_ELECTED,
                    new Leadership(path, clusterService.getLocalNode().id(), 0, 0)));
        }
        return CompletableFuture.completedFuture(new Leadership(path, clusterService.getLocalNode().id(), 0, 0));
    }

    @Override
    public CompletableFuture<Void> withdraw(String path) {
        elections.remove(path);
        for (LeadershipEventListener listener : listeners) {
            listener.event(new LeadershipEvent(Type.LEADER_BOOTED,
                    new Leadership(path, clusterService.getLocalNode().id(), 0, 0)));
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Map<String, Leadership> getLeaderBoard() {
        //FIXME
        throw new UnsupportedOperationException("I don't know what to do." +
                                                        " I wish you luck.");
    }

    @Override
    public void addListener(LeadershipEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(LeadershipEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public Map<String, List<NodeId>> getCandidates() {
        return null;
    }

    @Override
    public List<NodeId> getCandidates(String path) {
        return null;
    }

    @Override
    public boolean stepdown(String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean makeTopCandidate(String path, NodeId nodeId) {
        throw new UnsupportedOperationException();
    }
}
