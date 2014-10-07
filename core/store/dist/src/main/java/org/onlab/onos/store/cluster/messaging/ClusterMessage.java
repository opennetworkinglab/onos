package org.onlab.onos.store.cluster.messaging;

import org.onlab.onos.cluster.NodeId;

// TODO: ClusterMessage should be aware about how to serialize the payload
// TODO: Should payload type be made generic?
/**
 * Base message for cluster-wide communications.
 */
public class ClusterMessage {

    private final NodeId sender;
    private final MessageSubject subject;
    private final byte[] payload;
    // TODO: add field specifying Serializer for payload

    /**
     * Creates a cluster message.
     *
     * @param subject message subject
     */
    public ClusterMessage(NodeId sender, MessageSubject subject, byte[] payload) {
        this.sender = sender;
        this.subject = subject;
        this.payload = payload;
    }

    /**
     * Returns the id of the controller sending this message.
     *
     * @return message sender id.
     */
     public NodeId sender() {
         return sender;
     }

    /**
     * Returns the message subject indicator.
     *
     * @return message subject
     */
    public MessageSubject subject() {
        return subject;
    }

    /**
     * Returns the message payload.
     *
     * @return message payload.
     */
    public byte[] payload() {
        return payload;
    }
}
