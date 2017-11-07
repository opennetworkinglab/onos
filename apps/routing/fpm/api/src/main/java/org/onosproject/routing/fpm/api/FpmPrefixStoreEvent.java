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

package org.onosproject.routing.fpm.api;

import org.onosproject.event.AbstractEvent;

/**
 * Event class for FPM prefix store.
 */
public class FpmPrefixStoreEvent extends AbstractEvent<FpmPrefixStoreEvent.Type, FpmRecord> {

    /**
     * Types of the event.
     */
    public enum Type {
        /**
         * A Fpm record has been added.
         */
        ADD,

        /**
         * A Fpm record has been removed.
         */
        REMOVE
    }

    /**
     * Creates a Fpm prefix store event with given data.
     *
     * @param type is the type of event
     * @param subject is the Fpm record of this event
     */
    public FpmPrefixStoreEvent(Type type, FpmRecord subject) {
        super(type, subject);
    }
}
