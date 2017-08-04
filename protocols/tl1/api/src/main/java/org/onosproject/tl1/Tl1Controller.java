/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.tl1;

import com.google.common.annotations.Beta;
import org.onosproject.net.DeviceId;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Beta
public interface Tl1Controller {
    /**
     * Sends the message to the device asynchronously.
     *
     * @param deviceId the device to write to
     * @param msg the message to write
     * @return the device response
     */
    // TODO: return CompletableFuture<Tl1Message> once we have appropriate builders
    CompletableFuture<String> sendMsg(DeviceId deviceId, Tl1Command msg);

    /**
     * Returns the device identified by given ID.
     *
     * @param deviceId the device ID to lookup
     * @return optional Tl1Device
     */
    Optional<Tl1Device> getDevice(DeviceId deviceId);

    /**
    /**
     * Adds a device to the controller.
     * @param deviceId the device ID to add
     * @param device the device to add
     * @return true if device added, false if already known
     */
    boolean addDevice(DeviceId deviceId, Tl1Device device);

    /**
     * Disconnects the device and removes it from the controller.
     * @param deviceId the device to remove
     */
    void removeDevice(DeviceId deviceId);

    /**
     * Connects the controller to the device.
     * @param deviceId the device to disconnect to
     */
    void connectDevice(DeviceId deviceId);

    /**
     * Disconnects the device from the controller.
     * @param deviceId the device to disconnect from
     */
    void disconnectDevice(DeviceId deviceId);

    /**
     * Returns a set of all devices IDs for this TL1 controller.
     * @return set of device IDs
     */
    Set<DeviceId> getDeviceIds();

    /**
     * Returns a set of all devices for this TL1 controller.
     * @return collection of TL1 devices
     */
    Collection<Tl1Device> getDevices();

    /**
     * Registers a listener for TL1 events.
     *
     * @param listener the listener to notify
     */
    void addListener(Tl1Listener listener);

    /**
     * Unregisters a listener for TL1 events.
     *
     * @param listener the listener to unregister
     */
    void removeListener(Tl1Listener listener);
}
