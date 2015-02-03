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
package org.onosproject.store.impl;

import com.google.common.base.MoreObjects;
import org.onosproject.store.Timestamp;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Describes a single put event in an EventuallyConsistentMap.
 */
final class PutEntry<K, V> {
    private final K key;
    private final V value;
    private final Timestamp timestamp;

    /**
     * Creates a new put entry.
     *
     * @param key key of the entry
     * @param value value of the entry
     * @param timestamp timestamp of the put event
     */
    public PutEntry(K key, V value, Timestamp timestamp) {
        this.key = checkNotNull(key);
        this.value = checkNotNull(value);
        this.timestamp = checkNotNull(timestamp);
    }

    // Needed for serialization.
    @SuppressWarnings("unused")
    private PutEntry() {
        this.key = null;
        this.value = null;
        this.timestamp = null;
    }

    /**
     * Returns the key of the entry.
     *
     * @return the key
     */
    public K key() {
        return key;
    }

    /**
     * Returns the value of the entry.
     *
     * @return the value
     */
    public V value() {
        return value;
    }

    /**
     * Returns the timestamp of the event.
     *
     * @return the timestamp
     */
    public Timestamp timestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("key", key)
                .add("value", value)
                .add("timestamp", timestamp)
                .toString();
    }
}
