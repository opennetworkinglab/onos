package org.onosproject.store.service;

/**
 * Storage service.
 * <p>
 * This service provides operations for creating key-value stores.
 * One can chose to create key-value stores with varying properties such
 * as strongly consistent vs eventually consistent, durable vs volatile.
 * <p>
 * Various store implementations should leverage the data structures provided
 * by this service
 */
public interface StorageService {

    /**
     * Creates a ConsistentMap.
     *
     * @param name map name
     * @param serializer serializer to use for serializing keys and values.
     * @return consistent map.
     * @param <K> key type
     * @param <V> value type
     */
    <K, V> ConsistentMap<K , V> createConsistentMap(String name, Serializer serializer);

    // TODO: add API for creating Eventually Consistent Map.
}