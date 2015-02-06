package org.onosproject.store.consistent.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * A simple Partitioner that uses the key hashCode to map
 * key to a partition.
 *
 * @param <K> key type.
 */
public class SimpleKeyHashPartitioner<K> implements Partitioner<K> {

    private final Map<String, Database> partitionMap;
    private final List<String> sortedPartitionNames;

    public SimpleKeyHashPartitioner(Map<String, Database> partitionMap) {
        this.partitionMap = ImmutableMap.copyOf(partitionMap);
        sortedPartitionNames = Lists.newArrayList(this.partitionMap.keySet());
        Collections.sort(sortedPartitionNames);
    }

    @Override
    public Database getPartition(String tableName, K key) {
        return partitionMap.get(sortedPartitionNames.get(Math.abs(key.hashCode()) % partitionMap.size()));
    }
}
