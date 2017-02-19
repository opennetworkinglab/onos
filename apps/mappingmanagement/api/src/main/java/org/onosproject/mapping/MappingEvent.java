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

import org.onosproject.event.AbstractEvent;

/**
 * Describes mapping event.
 */
public class MappingEvent extends AbstractEvent<MappingEvent.Type, Mapping> {

    /**
     * Type of mapping events.
     */
    public enum Type {

        /**
         * Signifies that a new mapping has been detected.
         */
        MAPPING_ADDED,

        /**
         * Signifies that a new mapping has been removed.
         */
        MAPPING_REMOVED,

        /**
         * Signifies that a mapping has been updated.
         */
        MAPPING_UPDATED,

        // internal event between Manager <-> Store

        /**
         * Signifies that a request to add mapping has been added to the store.
         */
        MAPPING_ADD_REQUESTED,

        /**
         * Signifies that a request to update mapping has been added to the store.
         */
        MAPPING_UPDATE_REQUESTED,

        /**
         * Signifies that a request to remove flow rule has been added to the store.
         */
        MAPPING_REMOVE_REQUESTED
    }

    /**
     * Creates an event of a given type and for the specified mapping and the
     * current time.
     *
     * @param type    mapping event type
     * @param mapping event mapping subject
     */
    public MappingEvent(Type type, Mapping mapping) {
        super(type, mapping);
    }

    /**
     * Creates an event of a given type and for the specified mapping and time.
     *
     * @param type    mapping event type
     * @param mapping event mapping subject
     * @param time    occurrence time
     */
    public MappingEvent(Type type, Mapping mapping, long time) {
        super(type, mapping, time);
    }
}
