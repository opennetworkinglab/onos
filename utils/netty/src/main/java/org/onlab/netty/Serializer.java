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
     * Encodes the specified POJO into a byte buffer.
     *
     * @param data POJO to be encoded
     * @param buffer to write serialized bytes
     */
    public void encode(final Object data, ByteBuffer buffer);

    /**
     * Decodes the specified byte buffer to a POJO.
     *
     * @param buffer bytes to be decoded
     * @return POJO
     */
    public <T> T decode(final ByteBuffer buffer);
}
