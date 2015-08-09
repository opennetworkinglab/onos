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

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Representation of a ConsistentMap update notification.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class MapEvent<K, V> {

    /**
     * MapEvent type.
     */
    public enum Type {
        /**
         * Entry inserted into the map.
         */
        INSERT,

        /**
         * Existing map entry updated.
         */
        UPDATE,

        /**
         * Entry removed from map.
         */
        REMOVE
    }

    private final String name;
    private final Type type;
    private final K key;
    private final Versioned<V> value;

    /**
     * Creates a new event object.
     *
     * @param name map name
     * @param type type of event
     * @param key key the event concerns
     * @param value value key is mapped to
     */
    public MapEvent(String name, Type type, K key, Versioned<V> value) {
        this.name = name;
        this.type = type;
        this.key = key;
        this.value = value;
    }

    /**
     * Returns the map name.
     *
     * @return name of map
     */
    public String name() {
        return name;
    }

    /**
     * Returns the type of the event.
     *
     * @return the type of event
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
     * Returns the value associated with this event. If type is REMOVE,
     * this is the value that was removed. If type is INSERT/UPDATE, this is
     * the new value.
     *
     * @return the value
     */
    public Versioned<V> value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MapEvent)) {
            return false;
        }

        MapEvent<K, V> that = (MapEvent) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.key, that.key) &&
                Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, key, value);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("name", name)
                .add("type", type)
                .add("key", key)
                .add("value", value)
                .toString();
    }
}
