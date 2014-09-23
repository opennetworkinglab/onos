package org.onlab.onos.store.cluster.impl;

import com.google.common.collect.ImmutableSet;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ClusterStore;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.store.StoreService;
import org.onlab.packet.IpPrefix;
import org.slf4j.Logger;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Distributed implementation of the cluster nodes store.
 */
@Component(immediate = true)
@Service
public class DistributedClusterStore implements ClusterStore {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StoreService storeService;

    private HazelcastInstance theInstance;

    // FIXME: experimental implementation; enhance to assure persistence and
    // visibility to nodes that are not currently in the cluster

    @Activate
    public void activate() {
        log.info("Started");
        theInstance = storeService.getHazelcastInstance();

    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public ControllerNode getLocalNode() {
        return node(theInstance.getCluster().getLocalMember());
    }

    @Override
    public Set<ControllerNode> getNodes() {
        ImmutableSet.Builder<ControllerNode> builder = ImmutableSet.builder();
        for (Member member : theInstance.getCluster().getMembers()) {
            builder.add(node(member));
        }
        return builder.build();
    }

    @Override
    public ControllerNode getNode(NodeId nodeId) {
        for (Member member : theInstance.getCluster().getMembers()) {
            if (member.getUuid().equals(nodeId.toString())) {
                return node(member);
            }
        }
        return null;
    }

    @Override
    public ControllerNode.State getState(NodeId nodeId) {
        return ControllerNode.State.ACTIVE;
    }

    // Creates a controller node descriptor from the Hazelcast member.
    private ControllerNode node(Member member) {
        return new DefaultControllerNode(new NodeId(member.getUuid()),
                                         IpPrefix.valueOf(member.getSocketAddress().getAddress().getAddress()));
    }
}
