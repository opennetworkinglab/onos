package org.onlab.onos.cluster;

import java.util.Set;

/**
 * Test adapter for the cluster service.
 */
public class ClusterServiceAdapter implements ClusterService {
    @Override
    public ControllerNode getLocalNode() {
        return null;
    }

    @Override
    public Set<ControllerNode> getNodes() {
        return null;
    }

    @Override
    public ControllerNode getNode(NodeId nodeId) {
        return null;
    }

    @Override
    public ControllerNode.State getState(NodeId nodeId) {
        return null;
    }

    @Override
    public void addListener(ClusterEventListener listener) {
    }

    @Override
    public void removeListener(ClusterEventListener listener) {
    }
}
