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

/**
 * Storage service.
 * <p>
 * This service provides operations for creating key-value stores.
 * One can chose to create key-value stores with varying properties such
 * as strongly consistent vs eventually consistent, durable vs volatile.
 * <p>
 * Various store implementations should leverage the data structures provided
 * by this service
 */
public interface StorageService {

    /**
     * Creates a ConsistentMap.
     *
     * @param name map name
     * @param serializer serializer to use for serializing keys and values.
     * @return consistent map.
     * @param <K> key type
     * @param <V> value type
     */
    <K, V> ConsistentMap<K , V> createConsistentMap(String name, Serializer serializer);

    // TODO: add API for creating Eventually Consistent Map.
}