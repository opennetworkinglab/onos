/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.grpc.api;

import com.google.common.annotations.Beta;
import io.grpc.ManagedChannel;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceAgentListener;
import org.onosproject.net.provider.ProviderId;

/**
 * Abstraction of controller that manages gRPC clients.
 *
 * @param <C> the gRPC client type
 */
@Beta
public interface GrpcClientController<C extends GrpcClient> {

    /**
     * Instantiates a new client to operate on the given gRPC channel. Returns
     * true if  the client was created successfully, false otherwise. Clients
     * are identified by device IDs and once created they can be obtained by
     * invoking {@link #get(DeviceId)}.
     * <p>
     * Only one client can exist for the same device ID. If a client for the
     * given device ID already exists, throws an exception.
     *
     * @param deviceId device ID
     * @param channel  gRPC managed channel
     * @return true if the client was created, false otherwise
     * @throws IllegalArgumentException if a client for the same device ID
     *                                  already exists.
     */
    boolean create(DeviceId deviceId, ManagedChannel channel);

    /**
     * Returns the gRPC client previously created for the given device ID, or
     * null if such client does not exist.
     *
     * @param deviceId the device ID
     * @return the gRPC client of the device if exists; null otherwise
     */
    C get(DeviceId deviceId);

    /**
     * Removes the gRPC client for the given device and any gRPC channel state
     * associated to it. If no client exists for the given device, the result is
     * a no-op.
     *
     * @param deviceId the device ID
     */
    void remove(DeviceId deviceId);

    /**
     * Adds a listener for device agent events for the given provider. If a
     * listener already exists for the given device ID and provider ID, then it
     * will be replaced by the new one.
     *
     * @param deviceId   device ID
     * @param providerId provider ID
     * @param listener   the device agent listener
     */
    void addDeviceAgentListener(DeviceId deviceId, ProviderId providerId,
                                DeviceAgentListener listener);

    /**
     * Removes the listener for device agent events that was previously
     * registered for the given provider.
     *
     * @param deviceId   device ID
     * @param providerId the provider ID
     */
    void removeDeviceAgentListener(DeviceId deviceId, ProviderId providerId);
}
