/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.mapping;

import com.google.common.base.MoreObjects;
import org.onosproject.mapping.addresses.MappingAddress;

import java.util.Objects;

/**
 * Default mapping key implementation.
 */
public final class DefaultMappingKey implements MappingKey {

    private final MappingAddress address;

    /**
     * Create a new mapping key from the specified mapping address.
     *
     * @param address a mapping address
     */
    private DefaultMappingKey(MappingAddress address) {
        this.address = address;
    }

    @Override
    public MappingAddress address() {
        return address;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultMappingKey) {
            DefaultMappingKey that = (DefaultMappingKey) obj;
            return Objects.equals(address, that.address);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("address", address)
                .toString();
    }

    /**
     * Returns a new mapping key builder.
     *
     * @return mapping key builder
     */
    public static MappingKey.Builder builder() {
        return new Builder();
    }

    /**
     * Returns a new mapping key builder.
     *
     * @param key mapping key
     * @return mapping key builder
     */
    public static MappingKey.Builder builder(MappingKey key) {
        return new Builder(key);
    }

    /**
     * Builds a mapping key.
     */
    public static final class Builder implements MappingKey.Builder {

        private MappingAddress address;

        // creates a new builder
        private Builder() {
        }

        // creates a new builder based off an existing mapping key
        private Builder(MappingKey key) {
            this.address = key.address();
        }

        @Override
        public MappingKey.Builder withAddress(MappingAddress address) {
            this.address = address;
            return this;
        }

        @Override
        public MappingKey build() {

            return new DefaultMappingKey(address);
        }
    }
}
