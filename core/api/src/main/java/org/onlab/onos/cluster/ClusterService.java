package org.onlab.onos.cluster;

import java.util.Set;

/**
 * Service for obtaining information about the individual nodes within
 * the controller cluster.
 */
public interface ClusterService {

    /**
     * Returns the local controller node.
     *
     * @return local controller node
     */
    ControllerNode getLocalNode();

    /**
     * Returns the set of current cluster members.
     *
     * @return set of cluster members
     */
    Set<ControllerNode> getNodes();

    /**
     * Returns the specified controller node.
     *
     * @param nodeId controller node identifier
     * @return controller node
     */
    ControllerNode getNode(NodeId nodeId);

    /**
     * Returns the availability state of the specified controller node.
     *
     * @param nodeId controller node identifier
     * @return availability state
     */
    ControllerNode.State getState(NodeId nodeId);

    /**
     * Adds the specified cluster event listener.
     *
     * @param listener the cluster listener
     */
    void addListener(ClusterEventListener listener);

    /**
     * Removes the specified cluster event listener.
     *
     * @param listener the cluster listener
     */
    void removeListener(ClusterEventListener listener);

}
