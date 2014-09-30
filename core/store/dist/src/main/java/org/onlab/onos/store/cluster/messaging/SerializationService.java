package org.onlab.onos.store.cluster.messaging;

import java.nio.ByteBuffer;

/**
 * Service for serializing/deserializing intra-cluster messages.
 */
public interface SerializationService {

    /**
     * Decodes the specified byte buffer to obtain a message within.
     *
     * @param buffer byte buffer with message(s)
     * @return parsed message
     */
    ClusterMessage decode(ByteBuffer buffer);

    /**
     * Encodes the specified message into the given byte buffer.
     *
     * @param message message to be encoded
     * @param buffer byte buffer to receive the message data
     */
    void encode(ClusterMessage message, ByteBuffer buffer);

}
