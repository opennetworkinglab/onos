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

/**
 * Service that allows for the creation of local disk backed map for instance specific values that persist across
 * restarts. Empty maps and sets are deleted on shutdown.
 */
public interface PersistenceService {
    /**
     * A builder for the creation of local persistent maps backed by disk.
     *
     * @param <K> the type of keys in this map
     * @param <V> the type of values in this map
     * @return a persistent map builder
     */
    <K, V> PersistentMapBuilder<K, V> persistentMapBuilder();

    /**
     * A builder for the creation of local persistent sets backed by disk.
     *
     * @param <E> the type of the elements
     * @return a persistent set builder
     */
    <E> PersistentSetBuilder<E> persistentSetBuilder();
}