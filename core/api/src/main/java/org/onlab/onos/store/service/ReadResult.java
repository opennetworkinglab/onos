package org.onlab.onos.store.service;

import com.google.common.base.MoreObjects;


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
     * Returns database table name.
     * @return table name
     */
    public String tableName() {
        return tableName;
    }

    /**
     * Returns database table key.
     * @return key
     */
    public String key() {
        return key;
    }

    /**
     * Returns true if database table contained value for the key.
     *
     * @return true if database table contained value for the key
     */
    public boolean valueExists() {
        return value != null;
    }

    /**
     * Returns value associated with the key.
     * @return non-null value if the table contains one, null otherwise.
     */
    public VersionedValue value() {
        return value;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("tableName", tableName)
                .add("key", key)
                .add("value", value)
                .toString();
    }
}
