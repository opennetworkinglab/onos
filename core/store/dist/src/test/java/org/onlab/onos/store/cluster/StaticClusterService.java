package org.onlab.onos.store.cluster;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.onlab.onos.cluster.ClusterEventListener;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.cluster.ControllerNode.State;

import com.google.common.collect.Sets;

public abstract class StaticClusterService implements ClusterService {

    protected final Map<NodeId, ControllerNode> nodes = new HashMap<>();
    protected final Map<NodeId, ControllerNode.State> nodeStates = new HashMap<>();
    protected ControllerNode localNode;

    @Override
    public ControllerNode getLocalNode() {
        return localNode;
    }

    @Override
    public Set<ControllerNode> getNodes() {
        return Sets.newHashSet(nodes.values());
    }

    @Override
    public ControllerNode getNode(NodeId nodeId) {
        return nodes.get(nodeId);
    }

    @Override
    public State getState(NodeId nodeId) {
        return nodeStates.get(nodeId);
    }

    @Override
    public void addListener(ClusterEventListener listener) {
    }

    @Override
    public void removeListener(ClusterEventListener listener) {
    }
}