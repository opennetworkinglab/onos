/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.mapping;

/**
 * Abstraction of mapping entry.
 */
public interface MappingEntry extends Mapping {

    /**
     * Represents the type of mapping entry state.
     */
    enum MappingEntryState {

        /**
         * Indicates that this mapping has been submitted for addition.
         * Not necessarily in the map database or map cache.
         */
        PENDING_ADD,

        /**
         * Mapping has been added which means it is either in map database or
         * in map cache.
         */
        ADDED,

        /**
         * Mapping has been marked for removal, might still be either in map
         * database or in map cache.
         */
        PENDING_REMOVE,

        /**
         * Mapping has been removed from map database or in map cache, and
         * ca be purged.
         */
        REMOVED,

        /**
         * Indicates that the installation of this mapping has failed.
         */
        FAILED
    }

    /**
     * Returns the mapping entry state.
     *
     * @return mapping entry state
     */
    MappingEntryState state();
}
