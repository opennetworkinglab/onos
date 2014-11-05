package org.onlab.onos.store.service;

import java.util.Arrays;

import com.google.common.base.MoreObjects;

/**
 * Wrapper object that holds the object (as byte array) and its version.
 */
public class VersionedValue {

    private final byte[] value;
    private final long version;

    /**
     * Creates a new instance with the specified value and version.
     * @param value value
     * @param version version
     */
    public VersionedValue(byte[] value, long version) {
        this.value = value;
        this.version = version;
    }

    /**
     * Returns the value.
     * @return value.
     */
    public byte[] value() {
        return value;
    }

    /**
     * Returns the version.
     * @return version.
     */
    public long version() {
        return version;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("version", version)
                .add("value", Arrays.toString(value))
                .toString();
    }
}
