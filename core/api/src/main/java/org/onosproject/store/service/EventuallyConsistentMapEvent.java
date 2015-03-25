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

import java.util.Objects;

/**
 * Event object signalling that the map was modified.
 */
public class EventuallyConsistentMapEvent<K, V> {

    public enum Type {
        PUT,
        REMOVE
    }

    private final Type type;
    private final K key;
    private final V value;

    /**
     * Creates a new event object.
     *
     * @param type the type of the event
     * @param key the key the event concerns
     * @param value the value related to the key, or null for remove events
     */
    public EventuallyConsistentMapEvent(Type type, K key, V value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    /**
     * Returns the type of the event.
     *
     * @return the type of the event
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the key this event concerns.
     *
     * @return the key
     */
    public K key() {
        return key;
    }

    /**
     * Returns the value associated with this event.
     *
     * @return the value, or null if the event was REMOVE
     */
    public V value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EventuallyConsistentMapEvent)) {
            return false;
        }

        EventuallyConsistentMapEvent that = (EventuallyConsistentMapEvent) o;
        return Objects.equals(this.type, that.type) &&
                Objects.equals(this.key, that.key) &&
                Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, key, value);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("type", type)
                .add("key", key)
                .add("value", value)
                .toString();
    }
}
