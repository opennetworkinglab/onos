package org.onosproject.store.consistent.impl;

import java.util.Map;

/**
 * A simple Partitioner for mapping keys to database partitions.
 * <p>
 * This class uses a md5 hash based hashing scheme for hashing the key to
 * a partition.
 *
 */
public class SimpleKeyHashPartitioner extends DatabasePartitioner {

    public SimpleKeyHashPartitioner(Map<String, Database> partitionMap) {
        super(partitionMap);
    }

    @Override
    public Database getPartition(String tableName, String key) {
        return sortedPartitions[hash(key) % sortedPartitions.length];
    }
}