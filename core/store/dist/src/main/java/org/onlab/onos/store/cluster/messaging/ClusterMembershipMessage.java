package org.onlab.onos.store.cluster.messaging;

import org.onlab.onos.cluster.NodeId;
import org.onlab.packet.IpPrefix;

/**
 * Base for cluster membership messages.
 */
public abstract class ClusterMembershipMessage extends ClusterMessage {

    private NodeId nodeId;
    private IpPrefix ipAddress;
    private int tcpPort;

    // For serialization
    protected ClusterMembershipMessage() {
        super(MessageSubject.HELLO);
        nodeId = null;
        ipAddress = null;
        tcpPort = 0;
    }

    /**
     * Creates a new membership message for the specified end-point data.
     *
     * @param subject   message subject
     * @param nodeId    sending node identification
     * @param ipAddress sending node IP address
     * @param tcpPort   sending node TCP port
     */
    protected ClusterMembershipMessage(MessageSubject subject, NodeId nodeId,
                                       IpPrefix ipAddress, int tcpPort) {
        super(subject);
        this.nodeId = nodeId;
        this.ipAddress = ipAddress;
        this.tcpPort = tcpPort;
    }

    /**
     * Returns the sending node identifer.
     *
     * @return node identifier
     */
    public NodeId nodeId() {
        return nodeId;
    }

    /**
     * Returns the sending node IP address.
     *
     * @return node IP address
     */
    public IpPrefix ipAddress() {
        return ipAddress;
    }

    /**
     * Returns the sending node TCP listen port.
     *
     * @return TCP listen port
     */
    public int tcpPort() {
        return tcpPort;
    }

}
