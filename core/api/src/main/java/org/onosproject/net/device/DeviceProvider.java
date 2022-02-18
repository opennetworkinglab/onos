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
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.Provider;

import java.util.concurrent.CompletableFuture;

/**
 * Abstraction of a device information provider.
 */
public interface DeviceProvider extends Provider {

    /**
     * Triggers an asynchronous probe of the specified device, intended to
     * determine whether the device is present or not. An indirect result of this
     * should be invocation of
     * {@link org.onosproject.net.device.DeviceProviderService#deviceConnected} )} or
     * {@link org.onosproject.net.device.DeviceProviderService#deviceDisconnected}
     * at some later point in time.
     *
     * @param deviceId ID of device to be probed
     */
    void triggerProbe(DeviceId deviceId);

    /**
     * Notifies the provider of a mastership role change for the specified
     * device as decided by the core.
     *
     * @param deviceId device identifier
     * @param newRole  newly determined mastership role
     */
    void roleChanged(DeviceId deviceId, MastershipRole newRole);

    /**
     * Checks the reachability (connectivity) of a device from this provider.
     * Reachability, unlike availability, denotes whether THIS particular node
     * can send messages and receive replies from the specified device.
     * <p>
     * Implementations are encouraged to check for reachability by using only
     * internal provider state, i.e., without blocking execution.
     *
     * @param deviceId device identifier
     * @return true if reachable, false otherwise
     */
    boolean isReachable(DeviceId deviceId);

    /**
     * Checks the availability of the device from the provider perspective.
     * Availability denotes whether the device is reachable by
     * this node and able to perform its functions as expected (e.g., forward
     * traffic).
     *
     * <p>
     * Implementations are encouraged to check for availability by using only
     * internal provider state, i.e., without blocking execution.
     *
     * @param deviceId device identifier
     * @return completable future eventually true if available, false otherwise
     */
    default boolean isAvailable(DeviceId deviceId) {
        // For most implementations such as OpenFlow, reachability is equivalent
        // to availability.
        return isReachable(deviceId);
    }

    /**
     * Administratively enables or disables a port.
     *
     * @param deviceId   device identifier
     * @param portNumber port number
     * @param enable     true if port is to be enabled, false to disable
     */
    void changePortState(DeviceId deviceId, PortNumber portNumber,
                         boolean enable);


    /**
     * Administratively triggers 'disconnection' from the device. This is meant
     * purely in logical sense and is intended to apply equally to implementations
     * relying on connectionless control protocols.
     *
     * An indirect result of this should be invocation of
     * {@link org.onosproject.net.device.DeviceProviderService#deviceDisconnected}
     * if the device was presently 'connected' and
     * {@link org.onosproject.net.device.DeviceProviderService#deviceConnected}
     * at some later point in time if the device is available and continues to
     * be permitted to reconnect or if the provider continues to discover it.
     *
     * @param deviceId device identifier
     */
    default void triggerDisconnect(DeviceId deviceId) {
        throw new UnsupportedOperationException(id() + " does not implement this feature");
    }

    /**
     * Probe the reachability of the device from the provider perspective.
     * <p>
     * Implementations are encouraged to provide an async implementation.
     * With the respect of triggerProbe, this method returns whether or
     * not was possible to send a message to the device from this instance.
     * Instead, isReachable asses only the capability to send a message from
     * this node but does not imply sending a message to the device.
     *
     * @param deviceId device identifier
     * @return completable future eventually true if reachable, false otherwise
     */
    default CompletableFuture<Boolean> probeReachability(DeviceId deviceId) {
        return CompletableFuture.completedFuture(isReachable(deviceId));
    }

    /**
     * Return the grace period of this provider in milliseconds. With some devices, it might be
     * required more time to establish a working channel. This method returns a grace period
     * that should be respected before checking the reachability with the device.
     *
     * @return the grace period of the device. 0 if the device is expected to be immediately functional
     */
    default int gracePeriod() {
        return 0;
    }

}
