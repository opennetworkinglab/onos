package org.onlab.onos.store.cluster.messaging;

import org.onlab.onos.cluster.NodeId;
import org.onlab.packet.IpPrefix;

/**
 * Hello message that nodes use to greet each other.
 */
public class HelloMessage extends ClusterMessage {

    private NodeId nodeId;
    private IpPrefix ipAddress;
    private int tcpPort;

    // For serialization
    private HelloMessage() {
        super(MessageSubject.HELLO);
        nodeId = null;
        ipAddress = null;
        tcpPort = 0;
    }

    /**
     * Creates a new hello message for the specified end-point data.
     *
     * @param nodeId    sending node identification
     * @param ipAddress sending node IP address
     * @param tcpPort   sending node TCP port
     */
    public HelloMessage(NodeId nodeId, IpPrefix ipAddress, int tcpPort) {
        super(MessageSubject.HELLO);
        nodeId = nodeId;
        ipAddress = ipAddress;
        tcpPort = tcpPort;
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
