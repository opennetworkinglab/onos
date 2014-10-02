package org.onlab.netty;

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
    Object decode(byte[] data);

    /**
     * Encodes the specified POJO into a byte array.
     *
     * @param data POJO to be encoded
     * @return byte array.
     */
    byte[] encode(Object message);

}
