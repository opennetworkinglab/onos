/*
 * Copyright 2017-present Open Networking Foundation
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

import org.onosproject.net.DeviceId;
import org.onosproject.store.Store;

/**
 * Interface of a distributed store for managing mapping information.
 */
public interface MappingStore extends Store<MappingEvent, MappingStoreDelegate> {

    /**
     * Represents the type of mapping store.
     */
    enum Type {

        /**
         * Signifies that mapping information should be stored in map database.
         */
        MAP_DATABASE,

        /**
         * Signifies that mapping information should be stored in map cache.
         */
        MAP_CACHE
    }

    /**
     * Obtains the number of mapping in the specified store.
     *
     * @param type store type
     * @return number of mapping
     */
    int getMappingCount(Type type);

    /**
     * Obtains all mapping entries from the specified store.
     *
     * @param type store type
     * @return the mapping entries
     */
    Iterable<MappingEntry> getAllMappingEntries(Type type);

    /**
     * Obtains the stored mapping from the specified store.
     *
     * @param type    store type
     * @param mapping the mapping to look for
     * @return a mapping
     */
    MappingEntry getMappingEntry(Type type, Mapping mapping);

    /**
     * Obtains the mapping entries associated with a device from the
     * specified store.
     *
     * @param type     store type
     * @param deviceId device identifier
     * @return the mapping entries
     */
    Iterable<MappingEntry> getMappingEntries(Type type, DeviceId deviceId);

    /**
     * Stores a new mapping.
     *
     * @param type    store type
     * @param mapping the mapping to add
     */
    void storeMapping(Type type, MappingEntry mapping);

    /**
     * Marks a mapping for deletion. Actual deletion will occur when the
     * provider indicates that the mapping has been removed.
     *
     * @param type    store type
     * @param mapping the mapping to be marked as delete
     */
    void pendingDeleteMapping(Type type, Mapping mapping);

    /**
     * Removes an existing mapping from the specified store.
     *
     * @param type    store type
     * @param mapping the mapping to remove
     * @return mapping_removed event, or null if nothing removed
     */
    MappingEvent removeMapping(Type type, Mapping mapping);

    /**
     * Stores a new mapping or updates an existing entry from/to the
     * specified store.
     *
     * @param type  store type
     * @param entry the mapping to add or update
     * @return mapping_added event, or null if just an update
     */
    MappingEvent addOrUpdateMappingEntry(Type type, MappingEntry entry);

    /**
     * Marks a mapping as PENDING_ADD during retry.
     * <p>
     * Emits mapping_update event if the state is changed
     *
     * @param type  store type
     * @param entry the mapping that is retrying
     * @return mapping_updated event, or null if nothing updated
     */
    MappingEvent pendingMappingEntry(Type type, MappingEntry entry);

    /**
     * Removes all mapping entries of given device from the specified store.
     *
     * @param type     store type
     * @param deviceId device identifier
     */
    default void purgeMappingEntry(Type type, DeviceId deviceId) {
    }

    /**
     * Removes all mapping entries from the specified store.
     *
     * @param type store type
     */
    void purgeMappingEntries(Type type);
}
