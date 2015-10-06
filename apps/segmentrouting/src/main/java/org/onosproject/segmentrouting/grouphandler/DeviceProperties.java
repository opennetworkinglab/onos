/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.segmentrouting.grouphandler;

import java.util.List;

import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;

/**
 * Mechanism through which group handler module retrieves
 * the device specific attributes such as segment ID,
 * Mac address...etc from group handler applications.
 */
public interface DeviceProperties {
    /**
     * Returns the segment id of a device to be used in group creation.
     *
     * @param deviceId device identifier
     * @return segment id of a device
     */
    int getSegmentId(DeviceId deviceId);
    /**
     * Returns the Mac address of a device to be used in group creation.
     *
     * @param deviceId device identifier
     * @return mac address of a device
     */
    MacAddress getDeviceMac(DeviceId deviceId);
    /**
     * Indicates whether a device is edge device or transit/core device.
     *
     * @param deviceId device identifier
     * @return boolean
     */
    boolean isEdgeDevice(DeviceId deviceId);
    /**
     * Returns all segment IDs to be considered in building auto
     *
     * created groups.
     * @return list of segment IDs
     */
    List<Integer> getAllDeviceSegmentIds();
}
