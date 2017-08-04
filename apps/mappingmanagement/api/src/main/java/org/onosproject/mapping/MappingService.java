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
import org.onosproject.event.ListenerService;
import org.onosproject.mapping.MappingStore.Type;
import org.onosproject.net.DeviceId;

/**
 * Interface of mapping management service.
 */
public interface MappingService
        extends ListenerService<MappingEvent, MappingListener> {

    /**
     * Obtains the number of mappings in the system.
     *
     * @param type mapping store type
     * @return mapping count
     */
    int getMappingCount(Type type);

    /**
     * Obtains the collection of mapping entries of all devices.
     * This will include mapping which may not yet have been applied to device.
     *
     * @param type      mapping store type
     * @return collection of mapping entries
     */
    Iterable<MappingEntry> getAllMappingEntries(Type type);

    /**
     * Obtains the collection of mapping entries applied on the specific device.
     * The will include mapping which may not yet have been applied to device.
     *
     * @param type     mapping store type
     * @param deviceId device identifier
     * @return collection of mapping entries
     */
    Iterable<MappingEntry> getMappingEntries(Type type, DeviceId deviceId);

    /**
     * Obtains the collection of mapping entries with a given application ID.
     *
     * @param type  mapping store type
     * @param appId application identifier
     * @return collection of mapping entries
     */
    Iterable<MappingEntry> getMappingEntriesByAppId(Type type, ApplicationId appId);
}
