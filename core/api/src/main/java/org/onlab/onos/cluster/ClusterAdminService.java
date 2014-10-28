package org.onlab.onos.cluster;

import org.onlab.packet.IpAddress;

/**
 * Service for administering the cluster node membership.
 */
public interface ClusterAdminService {

    /**
     * Adds a new controller node to the cluster.
     *
     * @param nodeId  controller node identifier
     * @param ip      node IP listen address
     * @param tcpPort tcp listen port
     * @return newly added node
     */
    ControllerNode addNode(NodeId nodeId, IpAddress ip, int tcpPort);

    /**
     * Removes the specified node from the cluster node list.
     *
     * @param nodeId controller node identifier
     */
    void removeNode(NodeId nodeId);

}
