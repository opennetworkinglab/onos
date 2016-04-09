/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnrsc.portpair;

import org.onosproject.event.AbstractEvent;
import org.onosproject.vtnrsc.PortPair;

/**
 * Describes network Port-Pair event.
 */
public class PortPairEvent extends AbstractEvent<PortPairEvent.Type, PortPair> {
    /**
     * Type of port-pair events.
     */
    public enum Type {
        /**
         * Signifies that port-pair has been created.
         */
        PORT_PAIR_PUT,
        /**
         * Signifies that port-pair has been deleted.
         */
        PORT_PAIR_DELETE,
        /**
         * Signifies that port-pair has been updated.
         */
        PORT_PAIR_UPDATE
    }

    /**
     * Creates an event of a given type and for the specified Port-Pair.
     *
     * @param type Port-Pair event type
     * @param portPair Port-Pair subject
     */
    public PortPairEvent(Type type, PortPair portPair) {
        super(type, portPair);
    }

    /**
     * Creates an event of a given type and for the specified Port-Pair.
     *
     * @param type Port-Pair event type
     * @param portPair Port-Pair subject
     * @param time occurrence time
     */
    public PortPairEvent(Type type, PortPair portPair, long time) {
        super(type, portPair, time);
    }
}
