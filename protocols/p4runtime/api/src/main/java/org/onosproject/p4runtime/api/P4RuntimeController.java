/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.p4runtime.api;

import com.google.common.annotations.Beta;
import org.onosproject.event.ListenerService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceAgentListener;

/**
 * Controller of P4Runtime devices.
 */
@Beta
public interface P4RuntimeController
        extends ListenerService<P4RuntimeEvent, P4RuntimeEventListener> {

    /**
     * Instantiates a new client to operate on a P4Runtime device identified by
     * the given information. As a result of this method, a {@link
     * P4RuntimeClient} can be later obtained by invoking {@link
     * #getClient(DeviceId)}. Returns true if the client was created and the
     * channel to the device is open, false otherwise.
     * <p>
     * Only one client can exist for the same device ID. Calls to this method
     * are idempotent for the same [device ID, address, port, p4DeviceId]
     * triplet, i.e. returns true if such client already exists but a new one is
     * not created. Throws an {@link IllegalStateException} if a client for
     * device ID already exists but for different [address, port, p4DeviceId].
     *
     * @param deviceId   device identifier
     * @param serverAddr address of the P4Runtime server
     * @param serverPort port of the P4Runtime server
     * @param p4DeviceId P4Runtime-specific device identifier
     * @return true if the client was created and the channel to the device is
     * open
     * @throws IllegalStateException if a client already exists for this device
     *                               ID but for different [address, port,
     *                               p4DeviceId] triplet.
     */
    boolean createClient(DeviceId deviceId, String serverAddr, int serverPort,
                         long p4DeviceId);

    /**
     * Returns a client to operate on the given device, or null if a client for
     * such device does not exist in this controller.
     *
     * @param deviceId device identifier
     * @return client instance or null
     */
    P4RuntimeClient getClient(DeviceId deviceId);

    /**
     * Removes the client for the given device. If no client exists for the
     * given device identifier, the result is a no-op.
     *
     * @param deviceId device identifier
     */
    void removeClient(DeviceId deviceId);

    /**
     * Returns true if a client exists for the given device identifier, false
     * otherwise.
     *
     * @param deviceId device identifier
     * @return true if client exists, false otherwise.
     */
    boolean hasClient(DeviceId deviceId);

    /**
     * Returns true if the P4Runtime server running on the given device is
     * reachable, i.e. the channel is open and the server is able to respond to
     * RPCs, false otherwise. Reachability can be tested only if a client was
     * previously created using {@link #createClient(DeviceId, String, int,
     * long)}, otherwise this method returns false.
     *
     * @param deviceId device identifier.
     * @return true if a client was created and is able to contact the P4Runtime
     * server, false otherwise.
     */
    boolean isReachable(DeviceId deviceId);

    /**
     * Adds a listener for device agent events.
     *
     * @param deviceId device identifier
     * @param listener the device agent listener
     */
    void addDeviceAgentListener(DeviceId deviceId, DeviceAgentListener listener);

    /**
     * Removes the listener for device agent events.
     *
     * @param deviceId device identifier
     * @param listener the device agent listener
     */
    void removeDeviceAgentListener(DeviceId deviceId, DeviceAgentListener listener);
}
