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

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

/**
 * Service for administering the mapping management.
 */
public interface MappingAdminService extends MappingService {

    /**
     * Stores a mapping entry.
     *
     * @param type  mapping store type
     * @param entry mapping entry to be stored
     */
    void storeMappingEntry(MappingStore.Type type, MappingEntry entry);

    /**
     * Removes the specified mapping entries from their respective devices and
     * mapping store.
     *
     * @param type     mapping store type
     * @param entries  one or more mapping entries
     */
    void removeMappingEntries(MappingStore.Type type, MappingEntry... entries);

    /**
     * Removes all mapping entries submitted by a particular application.
     *
     * @param type  mapping store type
     * @param appId identifier of application whose mapping entries will be removed
     */
    void removeMappingEntriesByAppId(MappingStore.Type type, ApplicationId appId);

    /**
     * Purges all mappings on the specified device and mapping store.
     * Note that the mappings will only be removed from storage, the mappings
     * are still remaining in the device.
     *
     * @param type     mapping store type
     * @param deviceId device identifier
     */
    void purgeMappings(MappingStore.Type type, DeviceId deviceId);
}
