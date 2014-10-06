package org.onlab.onos.store.cluster.messaging;

import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.store.cluster.impl.ClusterNodesDelegate;

// TODO: This service interface can be removed, once we properly start
// using ClusterService
/**
 * Service for administering communications manager.
 */
public interface ClusterCommunicationAdminService {

    /**
     * Initialize.
     */
    void initialize(ControllerNode localNode, ClusterNodesDelegate nodesDelegate);

    /**
     * Adds the node to the list of monitored nodes.
     *
     * @param node node to be added
     */
    void addNode(ControllerNode node);

    /**
     * Removes the node from the list of monitored nodes.
     *
     * @param node node to be removed
     */
    void removeNode(ControllerNode node);
}