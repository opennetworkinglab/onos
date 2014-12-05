/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.store.service;

import java.util.Arrays;

import org.onlab.util.ByteArraySizeHashPrinter;

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
                .add("value", ByteArraySizeHashPrinter.orNull(value))
                .toString();
    }
}
