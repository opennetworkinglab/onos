package org.onlab.onos.store.service;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Database write request.
 */
public class WriteRequest {

    private final String tableName;
    private final String key;
    private final byte[] newValue;
    private final long previousVersion;
    private final byte[] oldValue;

    /**
     * Creates a write request, which will
     * put the specified value to the table regardless of the previous value.
     *
     * @param tableName name of the table
     * @param key       key in the table
     * @param newValue  value to write
     * @return WriteRequest
     */
    public static WriteRequest put(String tableName, String key,
                                   byte[] newValue) {
        return new WriteRequest(tableName, key, newValue, -1, null);
    }

    // FIXME: Is there a special version value to realize putIfAbsent?
    /**
     * Creates a write request, which will
     * put the specified value to the table if the previous version matches.
     *
     * @param tableName name of the table
     * @param key       key in the table
     * @param newValue  value to write
     * @param previousVersion previous version expected
     * @return WriteRequest
     */
    public static WriteRequest putIfVersionMatches(String tableName, String key,
                                                   byte[] newValue,
                                                   long previousVersion) {
        checkArgument(previousVersion >= 0);
        return new WriteRequest(tableName, key, newValue, previousVersion, null);
    }

    // FIXME: What is the behavior of oldValue=null? putIfAbsent?
    /**
     * Creates a write request, which will
     * put the specified value to the table if the previous value matches.
     *
     * @param tableName name of the table
     * @param key       key in the table
     * @param newValue  value to write
     * @param oldValue  previous value expected
     * @return WriteRequest
     */
    public static WriteRequest putIfValueMatches(String tableName, String key,
                                                 byte[] newValue,
                                                 byte[] oldValue) {
        return new WriteRequest(tableName, key, newValue, -1, oldValue);
    }

    // FIXME: How do we remove value? newValue=null?

    // hidden constructor
    protected WriteRequest(String tableName, String key, byte[] newValue, long previousVersion, byte[] oldValue) {

        checkArgument(tableName != null);
        checkArgument(key != null);
        checkArgument(newValue != null);

        this.tableName = tableName;
        this.key = key;
        this.newValue = newValue;
        this.previousVersion = previousVersion;
        this.oldValue = oldValue;
    }

    public String tableName() {
        return tableName;
    }

    public String key() {
        return key;
    }

    public byte[] newValue() {
        return newValue;
    }

    public long previousVersion() {
        return previousVersion;
    }

    public byte[] oldValue() {
        return oldValue;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("tableName", tableName)
                .add("key", key)
                .add("newValue", newValue)
                .add("previousVersion", previousVersion)
                .add("oldValue", oldValue)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, tableName, previousVersion);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        WriteRequest other = (WriteRequest) obj;
        return Objects.equals(this.key, other.key) &&
                Objects.equals(this.tableName, other.tableName) &&
                Objects.equals(this.previousVersion, other.previousVersion);
    }
}
