package org.onlab.onos.store.cluster.messaging;

import org.onlab.onos.cluster.NodeId;

/**l
 * Echo heart-beat message that nodes send to each other.
 */
public class EchoMessage extends ClusterMessage {

    private NodeId nodeId;

    // For serialization
    private EchoMessage() {
        super(MessageSubject.HELLO);
        nodeId = null;
    }

    /**
     * Creates a new heart-beat echo message.
     *
     * @param nodeId    sending node identification
     */
    public EchoMessage(NodeId nodeId) {
        super(MessageSubject.HELLO);
        nodeId = nodeId;
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
