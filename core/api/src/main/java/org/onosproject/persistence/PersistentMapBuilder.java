/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.persistence;


import org.onosproject.store.service.Serializer;

import java.util.Map;

/**
 * The interface for a persistent map builder for use with mapDB.
 */
public interface PersistentMapBuilder<K, V> {

    /**
     * Sets the name of this map.
     * @param name the string name of this map
     * @return a persistent map builder with the name option now set
     */
    PersistentMapBuilder<K, V> withName(String name);

    /**
     * Sets the key serializer to be used to serialize this map, this is a required parameter.
     * @param serializer the serializer to be used for keys
     * @return a persistent map builder with the key serializer set
     */
    PersistentMapBuilder<K, V> withSerializer(Serializer serializer);

    /**
     * Validates the map settings and then builds this map in the database.  Throws an exception if invalid settings
     * are found.
     * @return The map that was created
     */
    Map<K, V> build();
}