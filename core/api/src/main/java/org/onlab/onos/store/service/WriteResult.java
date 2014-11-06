package org.onlab.onos.store.service;

import com.google.common.base.MoreObjects;


/**
 * Database write result.
 */
public class WriteResult {

    private final String tableName;
    private final String key;
    private final VersionedValue previousValue;

    public WriteResult(String tableName, String key, VersionedValue previousValue) {
        this.tableName = tableName;
        this.key = key;
        this.previousValue = previousValue;
    }

    public String tableName() {
        return tableName;
    }

    public String key() {
        return key;
    }

    public VersionedValue previousValue() {
        return previousValue;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("tableName", tableName)
                .add("key", key)
                .add("previousValue", previousValue)
                .toString();
    }
}
