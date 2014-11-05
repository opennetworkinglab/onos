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

    // put regardless of previous value
    public WriteRequest(String tableName, String key, byte[] newValue) {
        this(tableName, key, newValue, -1, null);
    }

    // put if version matches
    public WriteRequest(String tableName, String key, byte[] newValue, long previousVersion) {
        this(tableName, key, newValue, previousVersion, null);
        checkArgument(previousVersion >= 0);
    }

    // put if value matches
    public WriteRequest(String tableName, String key, byte[] newValue, byte[] oldValue) {
        this(tableName, key, newValue, -1, oldValue);
    }

    // hidden constructor
    private WriteRequest(String tableName, String key, byte[] newValue, long previousVersion, byte[] oldValue) {

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
