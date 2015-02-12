package org.onosproject.store.service;

/**
 * Interface for serialization for store artifacts.
 */
public interface Serializer {
    /**
     * Serialize the specified object.
     * @param object object to serialize.
     * @return serialized bytes.
     * @param <T> encoded type
     */
    <T> byte[] encode(T object);

    /**
     * Deserialize the specified bytes.
     * @param bytes byte array to deserialize.
     * @return deserialized object.
     * @param <T> decoded type
     */
    <T> T decode(byte[] bytes);
}