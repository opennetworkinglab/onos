/*
 * Copyright 2014-present Open Networking Foundation
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

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

/**
 * Service for administering the inventory of infrastructure devices.
 */
public interface DeviceAdminService extends DeviceService {

    /**
     * Removes the device with the specified identifier.
     *
     * @param deviceId device identifier
     */
    void removeDevice(DeviceId deviceId);

    // TODO: add ability to administratively suspend/resume device

    /**
     * Administratively enables or disables a port on a device.
     *
     * @param deviceId  device identifier
     * @param portNumber port identifier
     * @param enable true if port is to be enabled, false to disable
     */
    void changePortState(DeviceId deviceId, PortNumber portNumber, boolean enable);

    /**
     * Removes the ports of a device with the specified identifier. The device
     * must be presently unavailable, i.e. offline.
     *
     * @param deviceId device identifier
     */
    default void removeDevicePorts(DeviceId deviceId) {
    }
}
