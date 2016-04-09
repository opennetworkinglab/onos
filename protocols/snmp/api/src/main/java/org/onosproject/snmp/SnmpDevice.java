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

package org.onosproject.snmp;

import org.onosproject.net.DeviceId;

/**
 * Abstraction a default Snmp Device.
 */
public interface SnmpDevice {

    /**
     * Returns host IP and host Port, used by this particular SNMP Device.
     *
     * @return Device Information.
     */
    String deviceInfo();

    /**
     * Terminates the device connection.
     */
    void disconnect();

    /**
     * Retrieves the device state.
     *
     * @return true if connected
     */
    boolean isReachable();

    /**
     * Returns the IP used connect ssh on the device.
     *
     * @return SNMP Device IP
     */
    String getSnmpHost();

    /**
     * Returns the SSH Port used connect the device.
     *
     * @return SSH Port number
     */
    int getSnmpPort();

    /**
     * Retrieves the username of the device.
     *
     * @return username
     */
    String getUsername();

    /**
     * Retrieves the community (password) of the device.
     *
     * @return password
     */
    String getCommunity();

    /**
     * Return the SNMP device deviceID.
     *
     * @return DeviceId
     */
    DeviceId deviceId();
}
