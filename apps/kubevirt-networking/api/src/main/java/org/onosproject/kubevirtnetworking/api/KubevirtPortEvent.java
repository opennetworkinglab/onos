/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

import org.onosproject.event.AbstractEvent;

/**
 * Kubevirt port event class.
 */
public class KubevirtPortEvent extends AbstractEvent<KubevirtPortEvent.Type, KubevirtPort> {

    /**
     * Creates an event of a given type for the specified port.
     *
     * @param type      kubevirt port event type
     * @param subject   kubevirt port subject
     */
    public KubevirtPortEvent(Type type, KubevirtPort subject) {
        super(type, subject);
    }

    /**
     * Kubevirt port events.
     */
    public enum Type {

        /**
         * Signifies that a new kubevirt port is created.
         */
        KUBEVIRT_PORT_CREATED,

        /**
         * Signifies that the kubevirt port is updated.
         */
        KUBEVIRT_PORT_UPDATED,

        /**
         * Signifies that the kubevirt port is removed.
         */
        KUBEVIRT_PORT_REMOVED,
    }
}
