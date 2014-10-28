package org.onlab.onos.store.cluster.impl;

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
import org.onlab.onos.cluster.ClusterEvent;
import org.onlab.onos.cluster.ClusterStore;
import org.onlab.onos.cluster.ClusterStoreDelegate;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.store.hz.AbsentInvalidatingLoadingCache;
import org.onlab.onos.store.hz.AbstractHazelcastStore;
import org.onlab.onos.store.hz.OptionalCacheLoader;
import org.onlab.packet.IpAddress;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static org.onlab.onos.cluster.ClusterEvent.Type.INSTANCE_ACTIVATED;
import static org.onlab.onos.cluster.ClusterEvent.Type.INSTANCE_DEACTIVATED;
import static org.onlab.onos.cluster.ControllerNode.State;

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

    @Override
    @Activate
    public void activate() {
        super.activate();
        listenerId = theInstance.getCluster().addMembershipListener(listener);

        rawNodes = theInstance.getMap("nodes");
        OptionalCacheLoader<NodeId, DefaultControllerNode> nodeLoader
                = new OptionalCacheLoader<>(serializer, rawNodes);
        nodes = new AbsentInvalidatingLoadingCache<>(newBuilder().build(nodeLoader));
        rawNodes.addEntryListener(new RemoteCacheEventHandler<>(nodes), true);

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
        byte[] address = member.getSocketAddress().getAddress().getAddress();
        return IpAddress.valueOf(address);
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
