package org.onosproject.store.consistent.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Partitioned database configuration.
 */
public class PartitionedDatabaseConfig {
    private final Map<String, DatabaseConfig> partitions = new HashMap<>();

    /**
     * Returns the configuration for all partitions.
     * @return partition map to configuartion mapping.
     */
    public Map<String, DatabaseConfig> partitions() {
        return Collections.unmodifiableMap(partitions);
    }

    /**
     * Adds the specified partition name and configuration.
     * @param name partition name.
     * @param config partition config
     * @return this instance
     */
    public PartitionedDatabaseConfig withPartition(String name, DatabaseConfig config) {
        partitions.put(name, config);
        return this;
    }
}
