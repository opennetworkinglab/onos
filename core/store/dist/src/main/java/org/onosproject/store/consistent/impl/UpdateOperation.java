package org.onosproject.store.consistent.impl;

import com.google.common.base.MoreObjects;

/**
 * Database update operation.
 *
 * @param <K> key type.
 * @param <V> value type.
 */
public class UpdateOperation<K, V> {

    /**
     * Type of database update operation.
     */
    public static enum Type {
        PUT,
        PUT_IF_ABSENT,
        PUT_IF_VERSION_MATCH,
        PUT_IF_VALUE_MATCH,
        REMOVE,
        REMOVE_IF_VERSION_MATCH,
        REMOVE_IF_VALUE_MATCH,
    }

    private Type type;
    private String tableName;
    private K key;
    private V value;
    private V currentValue;
    private long currentVersion;

    /**
     * Returns the type of update operation.
     * @return type of update.
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the tableName being updated.
     * @return table name.
     */
    public String tableName() {
        return tableName;
    }

    /**
     * Returns the item key being updated.
     * @return item key
     */
    public K key() {
        return key;
    }

    /**
     * Returns the new value.
     * @return item's target value.
     */
    public V value() {
        return value;
    }

    /**
     * Returns the expected current value in the database value for the key.
     * @return current value in database.
     */
    public V currentValue() {
        return currentValue;
    }

    /**
     * Returns the expected current version in the database for the key.
     * @return expected version.
     */
    public long currentVersion() {
        return currentVersion;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("type", type)
            .add("tableName", tableName)
            .add("key", key)
            .add("value", value)
            .add("currentValue", currentValue)
            .add("currentVersion", currentVersion)
            .toString();
    }

    /**
     * UpdatOperation builder.
     *
     * @param <K> key type.
     * @param <V> value type.
     */
    public static final class Builder<K, V> {

        private UpdateOperation<K, V> operation = new UpdateOperation<>();

        /**
         * Creates a new builder instance.
         * @param <K> key type.
         * @param <V> value type.
         *
         * @return builder.
         */
        public static <K, V> Builder<K, V> builder() {
            return new Builder<>();
        }

        private Builder() {
        }

        public UpdateOperation<K, V> build() {
            return operation;
        }

        public Builder<K, V> withType(Type type) {
            operation.type = type;
            return this;
        }

        public Builder<K, V> withTableName(String tableName) {
            operation.tableName = tableName;
            return this;
        }

        public Builder<K, V> withKey(K key) {
            operation.key = key;
            return this;
        }

        public Builder<K, V> withCurrentValue(V value) {
            operation.currentValue = value;
            return this;
        }

        public Builder<K, V> withValue(V value) {
            operation.value = value;
            return this;
        }

        public Builder<K, V> withCurrentVersion(long version) {
            operation.currentVersion = version;
            return this;
        }
    }
}
