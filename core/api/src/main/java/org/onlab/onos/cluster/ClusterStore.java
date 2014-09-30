package org.onlab.onos.cluster;

import org.onlab.onos.store.Store;
import org.onlab.packet.IpPrefix;

import java.util.Set;

/**
 * Manages inventory of controller cluster nodes; not intended for direct use.
 */
public interface ClusterStore extends Store<ClusterEvent, ClusterStoreDelegate> {

    /**
     * Returns the local controller node.
     *
     * @return local controller instance
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
     * @param nodeId controller instance identifier
     * @return controller instance
     */
    ControllerNode getNode(NodeId nodeId);

    /**
     * Returns the availability state of the specified controller node.
     *
     * @param nodeId controller instance identifier
     * @return availability state
     */
    ControllerNode.State getState(NodeId nodeId);

    /**
     * Adds a new controller node to the cluster.
     *
     * @param nodeId  controller node identifier
     * @param ip      node IP listen address
     * @param tcpPort tcp listen port
     * @return newly added node
     */
    ControllerNode addNode(NodeId nodeId, IpPrefix ip, int tcpPort);

    /**
     * Removes the specified node from the inventory of cluster nodes.
     *
     * @param nodeId controller instance identifier
     */
    void removeNode(NodeId nodeId);

}
