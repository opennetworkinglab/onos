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
package org.onosproject.store.consistent.impl;

import java.util.function.Function;

import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.Versioned;

/**
 * Result of a update operation.
 * <p>
 * Both old and new values are accessible along with a flag that indicates if the
 * the value was updated. If flag is false, oldValue and newValue both
 * point to the same unmodified value.
 * @param <V> result type
 */
public class UpdateResult<K, V> {

    private final boolean updated;
    private final String mapName;
    private final K key;
    private final Versioned<V> oldValue;
    private final Versioned<V> newValue;

    public UpdateResult(boolean updated, String mapName, K key, Versioned<V> oldValue, Versioned<V> newValue) {
        this.updated = updated;
        this.mapName = mapName;
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public boolean updated() {
        return updated;
    }

    public String mapName() {
        return mapName;
    }

    public K key() {
        return key;
    }

    public Versioned<V> oldValue() {
        return oldValue;
    }

    public Versioned<V> newValue() {
        return newValue;
    }

    public <K1, V1> UpdateResult<K1, V1> map(Function<K, K1> keyTransform, Function<V, V1> valueMapper) {
        return new UpdateResult<>(updated,
                mapName,
                keyTransform.apply(key),
                oldValue == null ? null : oldValue.map(valueMapper),
                newValue == null ? null : newValue.map(valueMapper));
    }

    public MapEvent<K, V> toMapEvent() {
        if (!updated) {
            return null;
        } else {
            MapEvent.Type eventType = oldValue == null ?
                    MapEvent.Type.INSERT : newValue == null ? MapEvent.Type.REMOVE : MapEvent.Type.UPDATE;
            Versioned<V> eventValue = eventType == MapEvent.Type.REMOVE ? oldValue : newValue;
            return new MapEvent<>(mapName(), eventType, key(), eventValue);
        }
    }
}