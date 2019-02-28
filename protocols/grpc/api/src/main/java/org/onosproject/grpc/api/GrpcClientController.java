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
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceAgentListener;
import org.onosproject.net.provider.ProviderId;

/**
 * Abstraction of controller that manages gRPC clients.
 *
 * @param <K> the gRPC client key
 * @param <C> the gRPC client type
 */
@Beta
public interface GrpcClientController<K extends GrpcClientKey, C extends GrpcClient> {

    /**
     * Instantiates a new client to operate on a gRPC server identified by the
     * given information. As a result of this method, a client can be later
     * obtained by invoking {@link #getClient(DeviceId)}.
     * <p>
     * Upon creation, a connection to the server is automatically started, which
     * blocks execution. If the connection is successful, the client is created
     * and this method returns true, otherwise (e.g., socket error) any state
     * associated with this client is destroyed and returns false.
     * <p>
     * Only one client can exist for the same device ID. Calls to this method
     * are idempotent fot the same client key, i.e. returns true if such client
     * already exists. Otherwise, if a client for the same device ID but
     * different client key already exists, throws an exception.
     *
     * @param clientKey the client key
     * @return true if the client was created and the channel to the server is
     * open; false otherwise
     * @throws IllegalArgumentException if a client for the same device ID but
     *                                  different client key already exists.
     */
    boolean createClient(K clientKey);

    /**
     * Returns the gRPC client previously created for the given device, or null
     * if such client does not exist.
     *
     * @param deviceId the device identifier
     * @return the gRPC client of the device if exists; null otherwise
     */
    C getClient(DeviceId deviceId);

    /**
     * Returns the gRPC client previously created for the given client key, or
     * null if such client does not exist.
     *
     * @param clientKey client key
     * @return the gRPC client of the device if exists; null otherwise
     */
    C getClient(K clientKey);

    /**
     * Removes the gRPC client for the given device and any gRPC channel state
     * associated to it. If no client exists for the given device, the result is
     * a no-op.
     *
     * @param deviceId the device identifier
     */
    void removeClient(DeviceId deviceId);

    /**
     * Similar to {@link #removeClient(DeviceId)} but uses the client key to
     * identify the client to remove.
     *
     * @param clientKey the client key
     */
    void removeClient(K clientKey);

    /**
     * Adds a listener for device agent events for the given provider.
     *
     * @param deviceId device identifier
     * @param providerId provider ID
     * @param listener the device agent listener
     */
    void addDeviceAgentListener(DeviceId deviceId, ProviderId providerId,
                                DeviceAgentListener listener);

    /**
     * Removes the listener for device agent events that was previously
     * registered for the given provider.
     *
     * @param deviceId   device identifier
     * @param providerId the provider ID
     */
    void removeDeviceAgentListener(DeviceId deviceId, ProviderId providerId);
}
