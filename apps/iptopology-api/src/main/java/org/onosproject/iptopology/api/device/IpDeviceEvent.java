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
package org.onosproject.iptopology.api.device;

import org.joda.time.LocalDateTime;
import org.onosproject.event.AbstractEvent;
import org.onosproject.iptopology.api.DeviceIntf;
import org.onosproject.iptopology.api.DevicePrefix;
import org.onosproject.iptopology.api.IpDevice;


import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Describes ip device event.
 */
public class IpDeviceEvent extends AbstractEvent<IpDeviceEvent.Type, IpDevice> {

    private final DeviceIntf devInterface;
    private final DevicePrefix devicePrefix;

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
         * Signifies that an interface has been added.
         */
        INTERFACE_ADDED,

        /**
         * Signifies that an interface has been updated.
         */
        INTERFACE_UPDATED,

        /**
         * Signifies that an interface has been removed.
         */
        INTERFACE_REMOVED,

        /**
         * Signifies that a prefix has been added.
         */
        PREFIX_ADDED,

        /**
         * Signifies that a prefix has been updated.
         */
        PREFIX_UPDATED,

        /**
         * Signifies that a prefix has been removed.
         */
        PREFIX_REMOVED,

    }

    /**
     * Creates an event of a given type and for the specified ip device.
     *
     * @param type   device event type
     * @param device event device subject
     */
    public IpDeviceEvent(Type type, IpDevice device) {
        this(type, device, null, null);
    }

    /**
     * Creates an event of a given type and for the specified device and interface.
     *
     * @param type   device event type
     * @param device event device subject
     * @param devInterface   optional interface subject
     */
    public IpDeviceEvent(Type type, IpDevice device, DeviceIntf devInterface) {
        this(type, device, devInterface, null);
    }

    /**
     * Creates an event of a given type and for the specified device and interface.
     *
     * @param type   device event type
     * @param device event device subject
     * @param devicePrefix   optional prefix subject
     */
    public IpDeviceEvent(Type type, IpDevice device, DevicePrefix devicePrefix) {
        this(type, device, null, devicePrefix);
    }


    /**
     * Creates an event of a given type and for the specified device, interface and prefix.
     *
     * @param type   device event type
     * @param device event device subject
     * @param devInterface   optional interface subject
     * @param devicePrefix   optional prefix subject
     */
    public IpDeviceEvent(Type type, IpDevice device, DeviceIntf devInterface,  DevicePrefix devicePrefix) {
        super(type, device);
        this.devInterface = devInterface;
        this.devicePrefix = devicePrefix;
    }


    /**
     * Creates an event of a given type and for the specified device, interface and time.
     *
     * @param type   device event type
     * @param device event device subject
     * @param devInterface   optional interface subject
     * @param devicePrefix   optional prefix subject
     * @param time   occurrence time
     */

    public IpDeviceEvent(Type type, IpDevice device, DeviceIntf devInterface,  DevicePrefix devicePrefix, long time) {
        super(type, device, time);
        this.devInterface = devInterface;
        this.devicePrefix = devicePrefix;
    }


    /**
     * Returns the interface subject.
     *
     * @return interface subject or null if the event is not interface specific.
     */
    public DeviceIntf deviceInterface() {
        return devInterface;
    }

    /**
     * Returns the prefix subject.
     *
     * @return prefix subject or null if the event is not prefix specific.
     */
    public DevicePrefix prefix() {
        return devicePrefix;
    }

    @Override
    public String toString() {
        if (devInterface == null || devicePrefix == null) {
            return super.toString();
        }
        return toStringHelper(this)
                .add("time", new LocalDateTime(time()))
                .add("type", type())
                .add("subject", subject())
                .add("interface", devInterface)
                .add("prefix", devicePrefix)
                .toString();
     }
}
