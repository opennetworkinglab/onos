package org.onlab.onos.store.cluster.messaging;

import org.onlab.onos.cluster.NodeId;
import org.onlab.packet.IpPrefix;

/**
 * Announcement message that nodes use to gossip about new arrivals.
 */
public class NewMemberMessage extends ClusterMembershipMessage {

    // For serialization
    private NewMemberMessage() {
    }

    /**
     * Creates a new gossip message for the specified end-point data.
     *
     * @param nodeId    sending node identification
     * @param ipAddress sending node IP address
     * @param tcpPort   sending node TCP port
     */
    public NewMemberMessage(NodeId nodeId, IpPrefix ipAddress, int tcpPort) {
        super(MessageSubject.NEW_MEMBER, nodeId, ipAddress, tcpPort);
    }

}
