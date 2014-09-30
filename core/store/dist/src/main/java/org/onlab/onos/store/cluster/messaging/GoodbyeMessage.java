package org.onlab.onos.store.cluster.messaging;

import org.onlab.onos.cluster.NodeId;

/**
 * Goodbye message that nodes use to leave the cluster for good.
 */
public class GoodbyeMessage extends ClusterMessage {

    private NodeId nodeId;

    // For serialization
    private GoodbyeMessage() {
        super(MessageSubject.GOODBYE);
        nodeId = null;
    }

    /**
     * Creates a new goodbye message.
     *
     * @param nodeId sending node identification
     */
    public GoodbyeMessage(NodeId nodeId) {
        super(MessageSubject.HELLO);
        this.nodeId = nodeId;
    }

    /**
     * Returns the sending node identifer.
     *
     * @return node identifier
     */
    public NodeId nodeId() {
        return nodeId;
    }

}
