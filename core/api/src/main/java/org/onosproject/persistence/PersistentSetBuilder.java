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

import java.util.Set;

/**
 * The default interface for the persistent set builder for use with mapDB.
 */
public interface PersistentSetBuilder<E> {

    /**
     * Sets the name of this set.
     * @param name the string name of this set
     * @return a persistent set builder with the name option now set
     */
    PersistentSetBuilder<E> withName(String name);

    /**
     * Sets the serializer to be used to serialize this set, this is a required parameter.
     * @param serializer the serializer to be used
     * @return a persistent set builder with the serializer set
     */
    PersistentSetBuilder<E> withSerializer(Serializer serializer);

    /**
     * Validates the set settings and then builds this map in the database.  Throws an exception if invalid settings
     * are found.
     * @return The set that was created
     */
    Set<E> build();
}