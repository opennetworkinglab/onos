package org.onosproject.store.consistent.impl;

/**
 * Partitioner is responsible for mapping keys to individual database partitions.
 *
 * @param <K> key type.
 */
public interface Partitioner<K> {

    /**
     * Returns the database partition.
     * @param tableName table name
     * @param key key
     * @return Database partition
     */
    Database getPartition(String tableName, K key);
}
