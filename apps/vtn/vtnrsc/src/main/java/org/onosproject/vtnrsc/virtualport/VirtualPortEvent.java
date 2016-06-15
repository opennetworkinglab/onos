/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.vtnrsc.virtualport;

import org.onosproject.event.AbstractEvent;
import org.onosproject.vtnrsc.VirtualPort;

/**
 * Describes virtual port event.
 */
public class VirtualPortEvent extends AbstractEvent<VirtualPortEvent.Type, VirtualPort> {
    /**
     * Type of virtual port events.
     */
    public enum Type {
        /**
         * Signifies that virtual port has been created.
         */
        VIRTUAL_PORT_PUT,
        /**
         * Signifies that virtual port has been deleted.
         */
        VIRTUAL_PORT_DELETE,
        /**
         * Signifies that virtual port has been updated.
         */
        VIRTUAL_PORT_UPDATE
    }

    /**
     * Creates an event of a given type and for the specified virtual port.
     *
     * @param type virtual port event type
     * @param virtualPort virtual port subject
     */
    public VirtualPortEvent(Type type, VirtualPort virtualPort) {
        super(type, virtualPort);
    }

    /**
     * Creates an event of a given type and for the specified virtual port.
     *
     * @param type virtual port event type
     * @param virtualPort virtual port subject
     * @param time occurrence time
     */
    public VirtualPortEvent(Type type, VirtualPort virtualPort, long time) {
        super(type, virtualPort, time);
    }
}
