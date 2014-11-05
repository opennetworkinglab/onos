package org.onlab.onos.store.service;

import org.onlab.onos.store.service.impl.VersionedValue;

/**
 * Database read result.
 */
public class ReadResult {

    private final String tableName;
    private final String key;
    private final VersionedValue value;

    public ReadResult(String tableName, String key, VersionedValue value) {
        this.tableName = tableName;
        this.key = key;
        this.value = value;
    }

    /**
     * Database table name.
     * @return
     */
    public String tableName() {
        return tableName;
    }

    /**
     * Database table key.
     * @return key.
     */
    public String key() {
        return key;
    }

    /**
     * value associated with the key.
     * @return non-null value if the table contains one, null otherwise.
     */
    public VersionedValue value() {
        return value;
    }
}
