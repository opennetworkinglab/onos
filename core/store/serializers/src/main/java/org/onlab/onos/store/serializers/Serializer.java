package org.onlab.onos.store.serializers;

import java.nio.ByteBuffer;

// TODO: To be replaced with SerializationService from IOLoop activity
/**
 * Service to serialize Objects into byte array.
 */
public interface Serializer {

    /**
     * Serializes the specified object into bytes.
     *
     * @param obj object to be serialized
     * @return serialized bytes
     */
    public byte[] encode(final Object obj);

    /**
     * Serializes the specified object into bytes.
     *
     * @param obj object to be serialized
     * @param buffer to write serialized bytes
     */
    public void encode(final Object obj, ByteBuffer buffer);

    /**
     * Deserializes the specified bytes into an object.
     *
     * @param bytes bytes to be deserialized
     * @return deserialized object
     */
    public <T> T decode(final byte[] bytes);

    /**
     * Deserializes the specified bytes into an object.
     *
     * @param buffer bytes to be deserialized
     * @return deserialized object
     */
    public <T> T decode(final ByteBuffer buffer);
}
