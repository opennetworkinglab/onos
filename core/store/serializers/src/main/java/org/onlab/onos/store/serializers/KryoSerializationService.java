package org.onlab.onos.store.serializers;

import java.nio.ByteBuffer;

// TODO: To be replaced with SerializationService from IOLoop activity
/**
 * Service to serialize Objects into byte array.
 */
public interface KryoSerializationService {

    /**
     * Serializes the specified object into bytes using one of the
     * pre-registered serializers.
     *
     * @param obj object to be serialized
     * @return serialized bytes
     */
    public byte[] serialize(final Object obj);

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
     * @param bytes bytes to be deserialized
     * @return deserialized object
     */
    public <T> T deserialize(final byte[] bytes);

    /**
     * Deserializes the specified bytes into an object using one of the
     * pre-registered serializers.
     *
     * @param buffer bytes to be deserialized
     * @return deserialized object
     */
    public <T> T deserialize(final ByteBuffer buffer);
}
