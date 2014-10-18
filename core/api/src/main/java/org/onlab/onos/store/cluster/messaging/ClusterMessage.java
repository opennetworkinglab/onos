package org.onlab.onos.store.cluster.messaging;

import java.io.IOException;

import org.onlab.onos.cluster.NodeId;

// TODO: Should payload type be ByteBuffer?
/**
 * Base message for cluster-wide communications.
 */
public class ClusterMessage {

    private final NodeId sender;
    private final MessageSubject subject;
    private final byte[] payload;

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

    /**
     * Sends a response to the sender.
     *
     * @param data payload response.
     * @throws IOException
     */
    public void respond(byte[] data) throws IOException {
        throw new IllegalStateException("One can only repond to message recived from others.");
    }
}
