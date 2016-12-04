/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.store.primitives.resources.impl;

import java.util.function.Function;

import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.Versioned;

import com.google.common.base.MoreObjects;

/**
 * Result of a map entry update operation.
 * <p>
 * Both old and new values are accessible along with a flag that indicates if the
 * the value was updated. If flag is false, oldValue and newValue both
 * point to the same unmodified value.
 *
 * @param <K> key type
 * @param <V> result type
 */
public class MapEntryUpdateResult<K, V> {

    public enum Status {

        /**
         * Indicates a successful update.
         */
        OK,

        /**
         * Indicates a noop i.e. existing and new value are both null.
         */
        NOOP,

        /**
         * Indicates a failed update due to a write lock.
         */
        WRITE_LOCK,

        /**
         * Indicates a failed update due to a precondition check failure.
         */
        PRECONDITION_FAILED
    }

    private final String mapName;
    private Status status;
    private final K key;
    private final Versioned<V> oldValue;
    private final Versioned<V> newValue;

    public MapEntryUpdateResult(Status status, String mapName, K key, Versioned<V> oldValue, Versioned<V> newValue) {
        this.status = status;
        this.mapName = mapName;
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * Returns {@code true} if the update was successful.
     * @return {@code true} if yes, {@code false} otherwise
     */
    public boolean updated() {
        return status == Status.OK;
    }

    /**
     * Returns the map name.
     * @return map name
     */
    public String mapName() {
        return mapName;
    }

    /**
     * Returns the update status.
     * @return update status
     */
    public Status status() {
        return status;
    }

    /**
     * Returns the map key.
     * @return key
     */
    public K key() {
        return key;
    }

    /**
     * Returns the old value.
     * @return the previous value associated with key if updated was successful, otherwise current value
     */
    public Versioned<V> oldValue() {
        return oldValue;
    }

    /**
     * Returns the new value after update.
     * @return if updated was unsuccessful, this is same as old value
     */
    public Versioned<V> newValue() {
        return newValue;
    }

    /**
     * Maps to another instance with different key and value types.
     * @param keyTransform transformer to use for transcoding keys
     * @param valueMapper mapper to use for transcoding values
     * @return new instance
     * @param <K1> key type of returned {@code MapEntryUpdateResult}
     * @param <V1> value type of returned {@code MapEntryUpdateResult}
     */
    public <K1, V1> MapEntryUpdateResult<K1, V1> map(Function<K, K1> keyTransform, Function<V, V1> valueMapper) {
        return new MapEntryUpdateResult<>(status,
                mapName,
                keyTransform.apply(key),
                oldValue == null ? null : oldValue.map(valueMapper),
                newValue == null ? null : newValue.map(valueMapper));
    }

    /**
     * Return the map event that will be generated as a result of this update.
     * @return map event. if update was unsuccessful, this returns {@code null}
     */
    public MapEvent<K, V> toMapEvent() {
        if (!updated()) {
            return null;
        } else {
            return new MapEvent<>(mapName(), key(), newValue, oldValue);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(MapEntryUpdateResult.class)
                .add("mapName", mapName)
                .add("status", status)
                .add("key", key)
                .add("oldValue", oldValue)
                .add("newValue", newValue)
                .toString();
    }
}
