package org.onlab.onos.cluster;

/**
 * Service for administering the cluster node membership.
 */
public interface ClusterAdminService {

    /**
     * Removes the specified node from the cluster node list.
     *
     * @param nodeId controller node identifier
     */
    void removeNode(NodeId nodeId);

}
