package org.onlab.onos.store.cluster.impl;

import org.onlab.onos.cluster.ControllerNode;

//Not used right now
/**
 * Contains information that will be published when a cluster membership event occurs.
 */
public class ClusterMembershipEvent {

    private final ClusterMembershipEventType type;
    private final ControllerNode node;

    public ClusterMembershipEvent(ClusterMembershipEventType type, ControllerNode node) {
        this.type = type;
        this.node = node;
    }

    public ClusterMembershipEventType type() {
        return type;
    }

    public ControllerNode node() {
        return node;
    }
}
