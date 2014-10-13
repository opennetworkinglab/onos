package org.onlab.onos.store.cluster.messaging;

// FIXME: not used any more? remove
/**
 * Service for encoding &amp; decoding intra-cluster message payload.
 */
public interface SerializationService {

    /**
     * Decodes the specified byte buffer to obtain the message within.
     *
     * @param buffer byte buffer with message(s)
     * @return parsed message
     */
    <T> T decode(byte[] data);

    /**
     * Encodes the specified message into the given byte buffer.
     *
     * @param message message to be encoded
     * @param buffer byte buffer to receive the message data
     */
    byte[] encode(Object message);

}
