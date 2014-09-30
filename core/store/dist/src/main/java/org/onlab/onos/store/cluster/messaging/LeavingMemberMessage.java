package org.onlab.onos.store.cluster.messaging;

import org.onlab.onos.cluster.NodeId;

/**
 * Announcement message that nodes use to gossip about team departures.
 */
public class LeavingMemberMessage extends ClusterMembershipMessage {

    // For serialization
    private LeavingMemberMessage() {
        super();
    }

    /**
     * Creates a new goodbye message.
     *
     * @param nodeId sending node identification
     */
    public LeavingMemberMessage(NodeId nodeId) {
        super(MessageSubject.LEAVING_MEMBER, nodeId, null, 0);
    }

}
