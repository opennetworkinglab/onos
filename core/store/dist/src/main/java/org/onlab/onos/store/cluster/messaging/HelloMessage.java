package org.onlab.onos.store.cluster.messaging;

import org.onlab.onos.cluster.NodeId;
import org.onlab.packet.IpPrefix;

/**
 * Hello message that nodes use to greet each other.
 */
public class HelloMessage extends ClusterMembershipMessage {

    // For serialization
    private HelloMessage() {
    }

    /**
     * Creates a new hello message for the specified end-point data.
     *
     * @param nodeId    sending node identification
     * @param ipAddress sending node IP address
     * @param tcpPort   sending node TCP port
     */
    public HelloMessage(NodeId nodeId, IpPrefix ipAddress, int tcpPort) {
        super(MessageSubject.HELLO, nodeId, ipAddress, tcpPort);
    }

}
