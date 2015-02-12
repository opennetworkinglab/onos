package org.onosproject.store.service;

import com.google.common.base.MoreObjects;

/**
 * Versioned value.
 *
 * @param <V> value type.
 */
public class Versioned<V> {

    private final V value;
    private final long version;

    /**
     * Constructs a new versioned value.
     * @param value value
     * @param version version
     */
    public Versioned(V value, long version) {
        this.value = value;
        this.version = version;
    }

    /**
     * Returns the value.
     *
     * @return value.
     */
    public V value() {
        return value;
    }

    /**
     * Returns the version.
     *
     * @return version
     */
    public long version() {
        return version;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("value", value)
            .add("version", version)
            .toString();
    }
}
