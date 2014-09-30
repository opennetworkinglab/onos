package org.onlab.onos.store.serializers;

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
     * Deserializes the specified bytes into an object using one of the
     * pre-registered serializers.
     *
     * @param bytes bytes to be deserialized
     * @return deserialized object
     */
    public <T> T deserialize(final byte[] bytes);

}
