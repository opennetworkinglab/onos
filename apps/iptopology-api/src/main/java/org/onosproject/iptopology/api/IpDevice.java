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
package org.onosproject.iptopology.api;

import org.onosproject.net.DeviceId;
import org.onosproject.net.Element;

/**
 * Abstraction of Ip Device.
 */
public interface IpDevice extends Element {
    /**
     ** Enum type to store Device Type.
     */
    enum Type {
        /**
         * Signifies that the device is pseudo device.
         */
        PSEUDO,

        /**
         * Signifies that the device is non-pseudo device.
         */
        NONPSEUDO
    }

    /**
     * Obtains device id.
     *
     * @return device id
     */
    @Override
    DeviceId id();

    /**
     * Obtains device type.
     *
     * @return device type
     */
    Type type();

    /**
     * Obtains Device identifier details.
     *
     * @return identifier of the device
     */
    IpDeviceIdentifier deviceIdentifier();

    /**
     * Obtains the traffic engineering parameters of the device.
     *
     * @return traffic engineering parameters of the device
     */
    DeviceTed deviceTed();
}