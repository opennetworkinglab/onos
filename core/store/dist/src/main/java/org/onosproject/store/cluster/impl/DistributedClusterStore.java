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

import com.google.common.base.Optional;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterStore;
import org.onosproject.cluster.ClusterStoreDelegate;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.hz.AbsentInvalidatingLoadingCache;
import org.onosproject.store.hz.AbstractHazelcastStore;
import org.onosproject.store.hz.OptionalCacheLoader;
import org.onlab.packet.IpAddress;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static org.onosproject.cluster.ClusterEvent.Type.INSTANCE_ACTIVATED;
import static org.onosproject.cluster.ClusterEvent.Type.INSTANCE_DEACTIVATED;
import static org.onosproject.cluster.ControllerNode.State;

/**
 * Distributed implementation of the cluster nodes store.
 */
@Component(immediate = true)
@Service
public class DistributedClusterStore
        extends AbstractHazelcastStore<ClusterEvent, ClusterStoreDelegate>
        implements ClusterStore {

    private IMap<byte[], byte[]> rawNodes;
    private LoadingCache<NodeId, Optional<DefaultControllerNode>> nodes;

    private String listenerId;
    private final MembershipListener listener = new InternalMembershipListener();
    private final Map<NodeId, State> states = new ConcurrentHashMap<>();

    private String nodesListenerId;

    @Override
    @Activate
    public void activate() {
        super.activate();
        listenerId = theInstance.getCluster().addMembershipListener(listener);

        rawNodes = theInstance.getMap("nodes");
        OptionalCacheLoader<NodeId, DefaultControllerNode> nodeLoader
                = new OptionalCacheLoader<>(serializer, rawNodes);
        nodes = new AbsentInvalidatingLoadingCache<>(newBuilder().build(nodeLoader));
        nodesListenerId = rawNodes.addEntryListener(new RemoteCacheEventHandler<>(nodes), true);

        loadClusterNodes();

        log.info("Started");
    }

    // Loads the initial set of cluster nodes
    private void loadClusterNodes() {
        for (Member member : theInstance.getCluster().getMembers()) {
            addNode(node(member));
        }
    }

    @Deactivate
    public void deactivate() {
        rawNodes.removeEntryListener(nodesListenerId);
        theInstance.getCluster().removeMembershipListener(listenerId);
        log.info("Stopped");
    }

    @Override
    public ControllerNode getLocalNode() {
        return node(theInstance.getCluster().getLocalMember());
    }

    @Override
    public Set<ControllerNode> getNodes() {
        ImmutableSet.Builder<ControllerNode> builder = ImmutableSet.builder();
        for (Optional<DefaultControllerNode> optional : nodes.asMap().values()) {
            builder.add(optional.get());
        }
        return builder.build();
    }

    @Override
    public ControllerNode getNode(NodeId nodeId) {
        return nodes.getUnchecked(nodeId).orNull();
    }

    @Override
    public State getState(NodeId nodeId) {
        State state = states.get(nodeId);
        return state == null ? State.INACTIVE : state;
    }

    @Override
    public ControllerNode addNode(NodeId nodeId, IpAddress ip, int tcpPort) {
        return addNode(new DefaultControllerNode(nodeId, ip, tcpPort));
    }

    @Override
    public void removeNode(NodeId nodeId) {
        synchronized (this) {
            rawNodes.remove(serialize(nodeId));
            nodes.invalidate(nodeId);
        }
    }

    // Adds a new node based on the specified member
    private synchronized ControllerNode addNode(DefaultControllerNode node) {
        rawNodes.put(serialize(node.id()), serialize(node));
        nodes.put(node.id(), Optional.of(node));
        states.put(node.id(), State.ACTIVE);
        return node;
    }

    // Creates a controller node descriptor from the Hazelcast member.
    private DefaultControllerNode node(Member member) {
        IpAddress ip = memberAddress(member);
        return new DefaultControllerNode(new NodeId(ip.toString()), ip);
    }

    private IpAddress memberAddress(Member member) {
        return IpAddress.valueOf(member.getSocketAddress().getAddress());
    }

    // Interceptor for membership events.
    private class InternalMembershipListener implements MembershipListener {
        @Override
        public void memberAdded(MembershipEvent membershipEvent) {
            log.info("Member {} added", membershipEvent.getMember());
            ControllerNode node = addNode(node(membershipEvent.getMember()));
            notifyDelegate(new ClusterEvent(INSTANCE_ACTIVATED, node));
        }

        @Override
        public void memberRemoved(MembershipEvent membershipEvent) {
            log.info("Member {} removed", membershipEvent.getMember());
            NodeId nodeId = new NodeId(memberAddress(membershipEvent.getMember()).toString());
            states.put(nodeId, State.INACTIVE);
            notifyDelegate(new ClusterEvent(INSTANCE_DEACTIVATED, getNode(nodeId)));
        }

        @Override
        public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent) {
            log.info("Member {} attribute {} changed to {}",
                     memberAttributeEvent.getMember(),
                     memberAttributeEvent.getKey(),
                     memberAttributeEvent.getValue());
        }
    }
}
