package org.onosproject.store.service;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Database read request.
 */
public class ReadRequest {

    private final String tableName;
    private final String key;

    /**
     * Creates a read request,
     * which will retrieve the specified key from the table.
     *
     * @param tableName name of the table
     * @param key       key in the table
     * @return ReadRequest
     */
    public static ReadRequest get(String tableName, String key) {
        return new ReadRequest(tableName, key);
    }

    public ReadRequest(String tableName, String key) {
        this.tableName = checkNotNull(tableName);
        this.key = checkNotNull(key);
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
        return MoreObjects.toStringHelper(getClass())
                .add("tableName", tableName)
                .add("key", key)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, tableName);
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
        ReadRequest other = (ReadRequest) obj;
        return Objects.equals(this.key, other.key) &&
                Objects.equals(this.tableName, other.tableName);
    }
}
