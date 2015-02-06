package org.onosproject.store.consistent.impl;

import static com.google.common.base.Preconditions.checkState;

import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.hash.Hashing;

/**
 * Partitioner for mapping table entries to individual database partitions.
 * <p>
 * By default a md5 hash of the hash key (key or table name) is used to pick a
 * partition.
 */
public abstract class DatabasePartitioner implements Partitioner<String> {
    // Database partitions sorted by their partition name.
    protected final Database[] sortedPartitions;

    public DatabasePartitioner(Map<String, Database> partitionMap) {
        checkState(partitionMap != null && !partitionMap.isEmpty(), "Partition map cannot be null or empty");
        sortedPartitions = ImmutableSortedMap.<String, Database>copyOf(partitionMap).values().toArray(new Database[]{});
    }

    protected int hash(String key) {
        return Math.abs(Hashing.md5().newHasher().putBytes(key.getBytes(Charsets.UTF_8)).hash().asInt());
    }

}
