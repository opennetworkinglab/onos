package org.onlab.onos.store.cluster.impl;

import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.packet.IpPrefix;

// Not used right now
/**
 * Simple back interface through which connection manager can interact with
 * the cluster store.
 */
public interface ClusterNodesDelegate {

    /**
     * Notifies about cluster node coming online.
     *
     * @param nodeId  newly detected cluster node id
     * @param ip      node IP listen address
     * @param tcpPort node TCP listen port
     * @return the controller node
     */
    DefaultControllerNode nodeDetected(NodeId nodeId, IpPrefix ip, int tcpPort);

    /**
     * Notifies about cluster node going offline.
     *
     * @param nodeId identifier of the cluster node that vanished
     */
    void nodeVanished(NodeId nodeId);

    /**
     * Notifies about remote request to remove node from cluster.
     *
     * @param nodeId identifier of the cluster node that was removed
     */
    void nodeRemoved(NodeId nodeId);

}
