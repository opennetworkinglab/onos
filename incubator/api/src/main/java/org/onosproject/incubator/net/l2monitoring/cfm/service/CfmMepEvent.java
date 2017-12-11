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
package org.onosproject.incubator.net.l2monitoring.cfm.service;

import org.onosproject.event.AbstractEvent;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepKeyId;

/**
 * Event related to the maintenance of CFM MEPs.
 */
public class CfmMepEvent extends AbstractEvent<CfmMepEvent.Type, MepKeyId> {

    /**
     * Type of Mep events.
     */
    public enum Type {
        /**
         * Signifies that a new Mep has been detected.
         */
        MEP_ADDED,

        /**
         * Signifies that a Mep has been removed.
         */
        MEP_REMOVED,

        /**
         * Signifies that a Mep has been updated.
         */
        MEP_UPDATED,

        /**
         * Signifies that the MEP has raised a fault alarm.
         */
        MEP_FAULT_ALARM
    }

    /**
     * Creates an event of a given type and for the specified Mep and the current time.
     *
     * @param type Mep event type
     * @param mepKeyId event Mep subject
     */
    public CfmMepEvent(Type type, MepKeyId mepKeyId) {
        super(type, mepKeyId);
    }
}
