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

    /**
     * Creates a copy of given VersionedValue.
     *
     * @param original VersionedValue to create a copy
     * @return same as original if original or it's value is null,
     *         otherwise creates a copy.
     */
    public static VersionedValue copy(VersionedValue original) {
        if (original == null) {
            return null;
        }
        if (original.value == null) {
            // immutable, no need to copy
            return original;
        } else {
            return new VersionedValue(
                                      Arrays.copyOf(original.value,
                                                    original.value.length),
                                      original.version);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("version", version)
                .add("value", value != null ? "[" + value.length + " bytes]" : value)
                .toString();
    }
}
