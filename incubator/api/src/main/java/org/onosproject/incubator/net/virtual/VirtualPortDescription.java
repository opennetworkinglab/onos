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

package org.onosproject.incubator.net.virtual;

import org.onosproject.net.device.PortDescription;

/**
 * Information about a virtual port.
 */
public interface VirtualPortDescription extends PortDescription {

    // TODO: Add something about a requirement of virtual port.

    /**
     * Representation of the virtual port.
     */
    enum State {
        /**
         * Signifies that a virtual port is currently start.
         */
        START,

        /**
         * Signifies that a virtual port is currently stop.
         */
        STOP,

        /**
         * Signifies that a virtual port is pause for a moment.
         */
        PAUSE
    }

    /**
     * Starts the virtual port.
     */
    void start();

    /**
     * Stops the virtual port.
     */
    void stop();

    /**
     * Pauses the virtual port for stopping a network or device.
     * e.g. snapshot.
     */
    void pause();

    /**
     * Returns the virtual port state.
     *
     * @return state of virtual port
     */
    State state();
}
