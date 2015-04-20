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
package org.onosproject.store.ecmap;

import java.util.Objects;

import org.onosproject.store.Timestamp;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class for events in an EventuallyConsistentMap.
 */
public abstract class AbstractEntry<K, V> implements Comparable<AbstractEntry<K, V>> {
    private final K key;
    private final Timestamp timestamp;

    /**
     * Creates a new put entry.
     *
     * @param key key of the entry
     * @param timestamp timestamp of the put event
     */
    public AbstractEntry(K key, Timestamp timestamp) {
        this.key = checkNotNull(key);
        this.timestamp = checkNotNull(timestamp);
    }

    // Needed for serialization.
    @SuppressWarnings("unused")
    protected AbstractEntry() {
        this.key = null;
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
     * Returns the timestamp of the event.
     *
     * @return the timestamp
     */
    public Timestamp timestamp() {
        return timestamp;
    }

    @Override
    public int compareTo(AbstractEntry<K, V> o) {
        return this.timestamp.compareTo(o.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof AbstractEntry) {
            final AbstractEntry that = (AbstractEntry) o;
            return this.timestamp.equals(that.timestamp);
        }
        return false;
    }
}
