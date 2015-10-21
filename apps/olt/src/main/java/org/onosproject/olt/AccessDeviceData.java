/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.olt;

import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Information about an access device.
 */
public class AccessDeviceData {
    private static final String DEVICE_ID_MISSING = "Device ID cannot be null";
    private static final String UPLINK_MISSING = "Uplink cannot be null";
    private static final String VLAN_MISSING = "VLAN ID cannot be null";

    private final DeviceId deviceId;
    private final PortNumber uplink;
    private final VlanId vlan;

    /**
     * Class constructor.
     *
     * @param deviceId access device ID
     * @param uplink uplink port number
     * @param vlan device VLAN ID
     */
    public AccessDeviceData(DeviceId deviceId, PortNumber uplink, VlanId vlan) {
        this.deviceId = checkNotNull(deviceId, DEVICE_ID_MISSING);
        this.uplink = checkNotNull(uplink, UPLINK_MISSING);
        this.vlan = checkNotNull(vlan, VLAN_MISSING);
    }

    /**
     * Retrieves the access device ID.
     *
     * @return device ID
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Retrieves the uplink port number.
     *
     * @return port number
     */
    public PortNumber uplink() {
        return uplink;
    }

    /**
     * Retrieves the VLAN ID assigned to the device.
     *
     * @return vlan ID
     */
    public VlanId vlan() {
        return vlan;
    }
}
