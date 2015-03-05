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

import org.onosproject.store.Timestamp;
import org.onosproject.store.impl.Timestamped;

import java.util.Map;

/**
 * A persistent store for an eventually consistent map.
 */
interface PersistentStore<K, V> {

    /**
     * Read the contents of the disk into the given maps.
     *
     * @param items items map
     * @param tombstones tombstones map
     */
    void readInto(Map<K, Timestamped<V>> items, Map<K, Timestamp> tombstones);

    /**
     * Puts a new key,value pair into the map on disk.
     *
     * @param key the key
     * @param value the value
     * @param timestamp the timestamp of the update
     */
    void put(K key, V value, Timestamp timestamp);

    /**
     * Removes a key from the map on disk.
     *
     * @param key the key
     * @param timestamp the timestamp of the update
     */
    void remove(K key, Timestamp timestamp);
}
