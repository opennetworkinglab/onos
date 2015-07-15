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

import java.util.Map;

/**
 * A persistent store for an eventually consistent map.
 */
interface PersistentStore<K, V> {

    /**
     * Read the contents of the disk into the given maps.
     *
     * @param items items map
     */
    void readInto(Map<K, MapValue<V>> items);

    /**
     * Updates a key,value pair in the persistent store.
     *
     * @param key the key
     * @param value the value
     */
    void update(K key, MapValue<V> value);

    /**
     * Removes a key from persistent store.
     *
     * @param key the key to remove
     */
    void remove(K key);
}
