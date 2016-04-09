/*
 * Copyright 2014-present Open Networking Laboratory
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

import org.joda.time.LocalDateTime;
import org.onosproject.event.AbstractEvent;
import org.onosproject.net.Device;
import org.onosproject.net.Port;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Describes infrastructure device event.
 */
public class DeviceEvent extends AbstractEvent<DeviceEvent.Type, Device> {

    private final Port port;

    /**
     * Type of device events.
     */
    public enum Type {
        /**
         * Signifies that a new device has been detected.
         */
        DEVICE_ADDED,

        /**
         * Signifies that some device attributes have changed; excludes
         * availability changes.
         */
        DEVICE_UPDATED,

        /**
         * Signifies that a device has been removed.
         */
        DEVICE_REMOVED,

        /**
         * Signifies that a device has been administratively suspended.
         */
        DEVICE_SUSPENDED,

        /**
         * Signifies that a device has come online or has gone offline.
         */
        DEVICE_AVAILABILITY_CHANGED,

        /**
         * Signifies that a port has been added.
         */
        PORT_ADDED,

        /**
         * Signifies that a port has been updated.
         */
        PORT_UPDATED,

        /**
         * Signifies that a port has been removed.
         */
        PORT_REMOVED,

        /**
         * Signifies that port statistics has been updated.
         */
        PORT_STATS_UPDATED
    }

    /**
     * Creates an event of a given type and for the specified device and the
     * current time.
     *
     * @param type   device event type
     * @param device event device subject
     */
    public DeviceEvent(Type type, Device device) {
        this(type, device, null);
    }

    /**
     * Creates an event of a given type and for the specified device, port
     * and the current time.
     *
     * @param type   device event type
     * @param device event device subject
     * @param port   optional port subject
     */
    public DeviceEvent(Type type, Device device, Port port) {
        super(type, device);
        this.port = port;
    }

    /**
     * Creates an event of a given type and for the specified device and time.
     *
     * @param type   device event type
     * @param device event device subject
     * @param port   optional port subject
     * @param time   occurrence time
     */
    public DeviceEvent(Type type, Device device, Port port, long time) {
        super(type, device, time);
        this.port = port;
    }

    /**
     * Returns the port subject.
     *
     * @return port subject or null if the event is not port specific.
     */
    public Port port() {
        return port;
    }

    @Override
    public String toString() {
        if (port == null) {
            return super.toString();
        }
        return toStringHelper(this)
                .add("time", new LocalDateTime(time()))
                .add("type", type())
                .add("subject", subject())
                .add("port", port)
                .toString();
     }
}
