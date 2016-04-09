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

package org.onosproject.protocol.rest;

import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;

/**
 * Represents an abstraction of a Rest Device in ONOS.
 */
public interface RestSBDevice {
    /**
     * Returns the ip of this device.
     *
     * @return ip
     */
    IpAddress ip();

    /**
     * Returns the password of this device.
     *
     * @return port
     */
    int port();

    /**
     * Returns the username of this device.
     *
     * @return username
     */
    String username();

    /**
     * Returns the password of this device.
     *
     * @return password
     */
    String password();

    /**
     * Returns the ONOS deviceID for this device.
     *
     * @return DeviceId
     */
    DeviceId deviceId();

    /**
     * Sets or unsets the state of the device.
     *
     * @param active boolean
     */
    void setActive(boolean active);

    /**
     * Returns the state of this device.
     *
     * @return state
     */
    boolean isActive();

    /**
     * Returns the protocol for the REST request, usually HTTP o HTTPS.
     *
     * @return protocol
     */
    String protocol();

    /**
     * Returns the url for the REST requests, to be used instead of IP and PORT.
     *
     * @return url
     */
    String url();
}
