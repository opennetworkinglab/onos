/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.net.device;

import org.onosproject.event.AbstractEvent;
import org.onosproject.net.DeviceId;

/**
 * Describes and event related to a protocol agent used to interact with an
 * infrastructure device.
 */
public final class DeviceAgentEvent
        extends AbstractEvent<DeviceAgentEvent.Type, DeviceId> {

    /**
     * Type of device events.
     */
    public enum Type {
        /**
         * Signifies that a channel between the agent and the device is open and
         * the two can communicate.
         */
        CHANNEL_OPEN,

        /**
         * Signifies that a channel between the agent and the device is closed
         * and the two cannot communicate.
         */
        CHANNEL_CLOSED,

        /**
         * Signifies that a channel error has been detected. Further
         * investigation should be performed to check if the channel is still
         * open or closed.
         */
        CHANNEL_ERROR,

        /**
         * Signifies that the agent has acquired master role.
         */
        ROLE_MASTER,

        /**
         * Signifies that the agent has acquired standby/slave mastership role.
         */
        ROLE_STANDBY,

        /**
         * Signifies that the agent doesn't have any valid mastership role for
         * the device.
         */
        ROLE_NONE,

        /**
         * Signifies that the agent tried to perform some operations on the
         * device that requires master role.
         */
        NOT_MASTER,

    }

    /**
     * Creates a new device agent event for the given type and device ID.
     *
     * @param type     event type
     * @param deviceId device ID
     */
    public DeviceAgentEvent(Type type, DeviceId deviceId) {
        super(type, deviceId);
    }
}
