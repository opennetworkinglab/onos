/*
 * Copyright 2015 Open Networking Laboratory
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
