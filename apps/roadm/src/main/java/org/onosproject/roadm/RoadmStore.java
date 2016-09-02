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
package org.onosproject.roadm;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

/**
 * Interface for the ROADM store. Currently used to store target power only.
 * This should be removed if target power could be read port annotations.
 */
public interface RoadmStore {

    /**
     * Adds the device to the store.
     *
     * <p>The device needs to be added to the store
     * before setTargetPower and getTargetPower can be used. This does not initialize
     * any of the target powers.
     *
     * @param deviceId DeviceId of the device to add
     */
    void addDevice(DeviceId deviceId);

    /**
     * Returns true if the device has been added to the store.
     *
     * @param deviceId DeviceId of the device to check
     * @return true if device has been added to the store, false otherwise
     */
    boolean deviceAvailable(DeviceId deviceId);

    /**
     * Stores the targetPower for a port on a device. The device needs to be added
     * to the store before this can be called. This does nothing if the device is
     * not added.
     *
     * @param deviceId DeviceId of the device
     * @param portNumber PortNumber of the port
     * @param targetPower target port power to store
     */
    void setTargetPower(DeviceId deviceId, PortNumber portNumber, long targetPower);

    /**
     * Returns the targetPower for a port on a device. The device needs to be added
     * to the store before this can be called. Returns null if the port's target
     * power has not yet been initialized using setTargetPower.
     *
     * @param deviceId DeviceId of the device
     * @param portNumber PortNumber of the port
     * @return target power if target power has already been set, null otherwise
     */
    Long getTargetPower(DeviceId deviceId, PortNumber portNumber);
}
