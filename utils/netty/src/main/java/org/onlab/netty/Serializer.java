package org.onlab.netty;

import java.nio.ByteBuffer;

/**
 * Interface for encoding/decoding message payloads.
 */
public interface Serializer {

    /**
     * Decodes the specified byte array to a POJO.
     *
     * @param data byte array.
     * @return POJO
     */
    public <T> T decode(byte[] data);

    /**
     * Encodes the specified POJO into a byte array.
     *
     * @param data POJO to be encoded
     * @return byte array.
     */
    public byte[] encode(Object data);

    /**
     * Serializes the specified object into bytes using one of the
     * pre-registered serializers.
     *
     * @param obj object to be serialized
     * @param buffer to write serialized bytes
     */
    public void serialize(final Object obj, ByteBuffer buffer);

    /**
     * Deserializes the specified bytes into an object using one of the
     * pre-registered serializers.
     *
     * @param buffer bytes to be deserialized
     * @return deserialized object
     */
    public <T> T deserialize(final ByteBuffer buffer);
}
