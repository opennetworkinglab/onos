package org.onosproject.store.consistent.impl;

import java.util.Map;

/**
 * A simple Partitioner that uses the table name hash to
 * pick a partition.
 * <p>
 * This class uses a md5 hash based hashing scheme for hashing the table name to
 * a partition. This partitioner maps all keys for a table to the same database
 * partition.
 */
public class SimpleTableHashPartitioner extends DatabasePartitioner {

    public SimpleTableHashPartitioner(Map<String, Database> partitionMap) {
        super(partitionMap);
    }

    @Override
    public Database getPartition(String tableName, String key) {
        return sortedPartitions[hash(tableName) % sortedPartitions.length];
    }
}