/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Set;

/**
 * Representation of virtual port.
 */
public interface KubevirtPort {

    /**
     * Returns the name of VM where the kubevirt port is attached.
     *
     * @return VM name
     */
    String vmName();

    /**
     * Returns the network identifier associated with the port.
     *
     * @return network identifier
     */
    String networkId();

    /**
     * Returns the MAC address of the port.
     *
     * @return MAC address
     */
    MacAddress macAddress();

    /**
     * Returns the IP address of the port.
     *
     * @return IP address
     */
    IpAddress ipAddress();

    /**
     * Returns the device ID of the port.
     *
     * @return device ID
     */
    DeviceId deviceId();

    /**
     * Returns the tenant device ID of the port.
     *
     * @return device ID
     */
    DeviceId tenantDeviceId();

    /**
     * Returns the result whether the port is associated with the tenant network.
     *
     * @return true if the port is associated with tenant network, false otherwise
     */
    boolean isTenant();

    /**
     * Returns the port number of the port.
     *
     * @return port number
     */
    PortNumber portNumber();

    /**
     * Returns new port instance with the given IP address.
     *
     * @param updatedIpAddress updated ip address
     * @return updated port
     */
    KubevirtPort updateIpAddress(IpAddress updatedIpAddress);

    /**
     * Returns new port instance with the given port number.
     *
     * @param updatedPortNumber updated port number
     * @return updated port
     */
    KubevirtPort updatePortNumber(PortNumber updatedPortNumber);

    /**
     * Returns new port instance with the given device ID.
     *
     * @param updatedDeviceId device identifier
     * @return updated port
     */
    KubevirtPort updateDeviceId(DeviceId updatedDeviceId);

    /**
     * Returns the security group IDs.
     *
     * @return security group identifiers.
     */
    Set<String> securityGroups();

    /**
     * Returns new port instance with the given security groups.
     *
     * @param sgs security groups
     * @return updated port
     */
    KubevirtPort updateSecurityGroups(Set<String> sgs);

    /**
     * Builder of new port.
     */
    interface Builder {

        /**
         * Builds an immutable port instance.
         *
         * @return kubernetes port
         */
        KubevirtPort build();

        /**
         * Returns port builder with supplied VM name.
         *
         * @param vmName VM name
         * @return port builder
         */
        Builder vmName(String vmName);

        /**
         * Returns port builder with supplied network identifier.
         *
         * @param networkId network identifier
         * @return port builder
         */
        Builder networkId(String networkId);

        /**
         * Returns port builder with supplied MAC address.
         *
         * @param macAddress MAC address
         * @return port builder
         */
        Builder macAddress(MacAddress macAddress);

        /**
         * Returns port builder with supplied IP address.
         *
         * @param ipAddress IP address
         * @return port builder
         */
        Builder ipAddress(IpAddress ipAddress);

        /**
         * Returns port builder with supplied device ID.
         *
         * @param deviceId device ID
         * @return port builder
         */
        Builder deviceId(DeviceId deviceId);

        /**
         * Returns port builder with supplied port number.
         *
         * @param portNumber port number
         * @return port builder
         */
        Builder portNumber(PortNumber portNumber);

        /**
         * Returns port builder with supplied security group identifiers.
         *
         * @param securityGroups security group identifiers
         * @return port builder
         */
        Builder securityGroups(Set<String> securityGroups);
    }
}
