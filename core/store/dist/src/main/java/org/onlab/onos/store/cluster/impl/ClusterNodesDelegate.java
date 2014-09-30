package org.onlab.onos.store.cluster.impl;

import org.onlab.onos.cluster.DefaultControllerNode;

/**
 * Simple back interface through which connection manager can interact with
 * the cluster store.
 */
public interface ClusterNodesDelegate {

    /**
     * Notifies about a new cluster node being detected.
     *
     * @param node newly detected cluster node
     */
    void nodeDetected(DefaultControllerNode node);

    /**
     * Notifies about cluster node going offline.
     *
     * @param node cluster node that vanished
     */
    void nodeVanished(DefaultControllerNode node);

}
