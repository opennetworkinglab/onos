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
package org.onosproject.net.edge;

import org.onosproject.event.AbstractEvent;
import org.onosproject.net.ConnectPoint;

/**
 * Describes an event pertaining to edge-port inventory.
 */
public class EdgePortEvent extends AbstractEvent<EdgePortEvent.Type, ConnectPoint> {

    public enum Type {
        /**
         * Signifies that a new edge port was detected.
         */
        EDGE_PORT_ADDED,

        /**
         * Signifies that a new edge port vanished.
         */
        EDGE_PORT_REMOVED
    }

    /**
     * Creates a new edge port event.
     *
     * @param type    event type
     * @param subject connection point subject
     */
    public EdgePortEvent(Type type, ConnectPoint subject) {
        super(type, subject);
    }

    /**
     * Creates a new edge port event.
     *
     * @param type    event type
     * @param subject connection point subject
     * @param time    occurrence time
     */
    public EdgePortEvent(Type type, ConnectPoint subject, long time) {
        super(type, subject, time);
    }

}
