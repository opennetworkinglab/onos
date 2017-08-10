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

package org.onosproject.evpnopenflow.rsc.baseport;

import org.onosproject.event.AbstractEvent;
import org.onosproject.evpnopenflow.rsc.BasePort;

/**
 * Describes base port event.
 */
public class BasePortEvent extends AbstractEvent<BasePortEvent.Type,
        BasePort> {
    /**
     * Type of base port events.
     */
    public enum Type {
        /**
         * Signifies that base port has been created.
         */
        BASE_PORT_PUT,
        /**
         * Signifies that base port has been deleted.
         */
        BASE_PORT_DELETE,
        /**
         * Signifies that base port has been updated.
         */
        BASE_PORT_UPDATE
    }

    /**
     * Creates an event of a given type and for the specified base port.
     *
     * @param type     base port event type
     * @param basePort base port subject
     */
    public BasePortEvent(Type type, BasePort basePort) {
        super(type, basePort);
    }

    /**
     * Creates an event of a given type and for the specified base port.
     *
     * @param type     base port event type
     * @param basePort base port subject
     * @param time     occurrence time
     */
    public BasePortEvent(Type type, BasePort basePort, long time) {
        super(type, basePort, time);
    }
}
