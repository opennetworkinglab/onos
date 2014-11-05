package org.onlab.onos.store.service;

/**
 * Database read request.
 */
public class ReadRequest {

    private final String tableName;
    private final String key;

    public ReadRequest(String tableName, String key) {
        this.tableName = tableName;
        this.key = key;
    }

    /**
     * Return the name of the table.
     * @return table name.
     */
    public String tableName() {
        return tableName;
    }

    /**
     * Returns the key.
     * @return key.
     */
    public String key() {
        return key;
    }

    @Override
    public String toString() {
        return "ReadRequest [tableName=" + tableName + ", key=" + key + "]";
    }
}