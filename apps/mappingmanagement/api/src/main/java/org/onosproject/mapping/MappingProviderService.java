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

import org.onosproject.net.provider.ProviderService;

import java.util.List;

/**
 * Service through which mapping providers can inject mapping information into
 * the core.
 */
public interface MappingProviderService extends ProviderService<MappingProvider> {

    /**
     * Signals that a new mapping has been received.
     *
     * @param mappingEntry newly added mapping entry
     * @param type         indicates that where this map entry should be stored
     */
    void mappingAdded(MappingEntry mappingEntry, MappingStore.Type type);

    /**
     * Signals that a new mapping query has been issued.
     * If no mapping is found, simply returns null.
     *
     * @param mappingKey a mapping key that is used for query a mapping value
     * @return a mapping value associated with a given mapping key
     */
    MappingValue mappingQueried(MappingKey mappingKey);

    /**
     * Signals that a new batch mapping query has been issued.
     * If no mapping is found, simply returns empty list.
     *
     * @param mappingKeys a collection of mapping keys
     * @return a collection of mapping values associated with give mapping keys
     */
    List<MappingValue> mappingQueried(List<MappingKey> mappingKeys);
}
