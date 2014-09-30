package org.onlab.onos.store.cluster.impl;

import org.onlab.onos.cluster.DefaultControllerNode;

/**
 * Service for administering communications manager.
 */
public interface ClusterCommunicationAdminService {

    /**
     * Adds the node to the list of monitored nodes.
     *
     * @param node node to be added
     */
    void addNode(DefaultControllerNode node);

    /**
     * Removes the node from the list of monitored nodes.
     *
     * @param node node to be removed
     */
    void removeNode(DefaultControllerNode node);

    /**
     * Starts-up the communications engine.
     *
     * @param localNode local controller node
     * @param delegate nodes delegate
     */
    void startUp(DefaultControllerNode localNode, ClusterNodesDelegate delegate);

    /**
     * Clears all nodes and streams as part of leaving the cluster.
     */
    void clearAllNodesAndStreams();
}
