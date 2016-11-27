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
package org.onosproject.segmentrouting.config;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.List;
import java.util.Map;

/**
 * Mechanism through which group handler module retrieves
 * the device specific attributes such as segment ID,
 * Mac address...etc from group handler applications.
 */
public interface DeviceProperties {
    /**
     * Checks if the device is configured.
     *
     * @param deviceId device identifier
     * @return true if the device is configured
     */
    boolean isConfigured(DeviceId deviceId);

    /**
     * Returns the IPv4 segment id of a device to be used in group creation.
     *
     * @param deviceId device identifier
     * @throws DeviceConfigNotFoundException if the device configuration is not found
     * @return segment id of a device
     */
    int getIPv4SegmentId(DeviceId deviceId) throws DeviceConfigNotFoundException;

    /**
     * Returns the IPv6 segment id of a device to be used in group creation.
     *
     * @param deviceId device identifier
     * @throws DeviceConfigNotFoundException if the device configuration is not found
     * @return segment id of a device
     */
    int getIPv6SegmentId(DeviceId deviceId) throws DeviceConfigNotFoundException;

    /**
     * Returns the Mac address of a device to be used in group creation.
     *
     * @param deviceId device identifier
     * @throws DeviceConfigNotFoundException if the device configuration is not found
     * @return mac address of a device
     */
    MacAddress getDeviceMac(DeviceId deviceId) throws DeviceConfigNotFoundException;

    /**
     * Returns the router ipv4 address of a segment router.
     *
     * @param deviceId device identifier
     * @throws DeviceConfigNotFoundException if the device configuration is not found
     * @return router ip address
     */
    IpAddress getRouterIpv4(DeviceId deviceId) throws DeviceConfigNotFoundException;

    /**
     * Returns the router ipv6 address of a segment router.
     *
     * @param deviceId device identifier
     * @throws DeviceConfigNotFoundException if the device configuration is not found
     * @return router ip address
     */
    IpAddress getRouterIpv6(DeviceId deviceId) throws DeviceConfigNotFoundException;

    /**
     * Indicates whether a device is edge device or transit/core device.
     *
     * @param deviceId device identifier
     * @throws DeviceConfigNotFoundException if the device configuration is not found
     * @return boolean
     */
    boolean isEdgeDevice(DeviceId deviceId) throws DeviceConfigNotFoundException;

    /**
     * Returns all segment IDs to be considered in building auto
     *
     * created groups.
     * @return list of segment IDs
     */
    List<Integer> getAllDeviceSegmentIds();

    /**
     * Returns subnet-to-ports mapping of given device.
     *
     * For each entry of the map
     * Key: a subnet
     * Value: a list of ports, which are bound to the subnet
     *
     * @param deviceId device identifier
     * @throws DeviceConfigNotFoundException if the device configuration is not found
     * @return a map that contains all subnet-to-ports mapping of given device
     */
    Map<IpPrefix, List<PortNumber>> getSubnetPortsMap(DeviceId deviceId)
            throws DeviceConfigNotFoundException;
}
