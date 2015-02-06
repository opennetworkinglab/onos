package org.onosproject.store.consistent.impl;

import org.onosproject.store.serializers.StoreSerializer;

/**
 * Database service.
 */
public interface DatabaseService {

    /**
     * Creates a ConsistentMap.
     *
     * @param name map name
     * @param serializer serializer to use for serializing keys and values.
     * @return consistent map.
     */
    <K, V> ConsistentMap<K , V> createConsistentMap(String name, StoreSerializer serializer);
}