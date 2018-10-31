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

/**
 * Abstraction of a gRPC controller which controls specific gRPC client {@link
 * C} with specific client key {@link K}.
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
     * Only one client can exist for the same device ID. Calls to this method
     * are idempotent fot the same client key, i.e. returns true if such client
     * already exists but a new one is not created. If there exists a client
     * with same device ID but different address and port, removes old one and
     * recreate new one.
     *
     * @param clientKey the client key
     * @return true if the client was created and the channel to the server is
     * open; false otherwise
     */
    boolean createClient(K clientKey);

    /**
     * Retrieves the gRPC client to operate on the given device.
     *
     * @param deviceId the device identifier
     * @return the gRPC client of the device if exists; null otherwise
     */
    C getClient(DeviceId deviceId);

    /**
     * Removes the gRPC client for the given device. If no client exists for the
     * given device, the result is a no-op.
     *
     * @param deviceId the device identifier
     */
    void removeClient(DeviceId deviceId);

    /**
     * Check reachability of the gRPC server running on the given device.
     * Reachability can be tested only if a client is previously created using
     * {@link #createClient(GrpcClientKey)}. Note that this only checks the
     * reachability instead of checking service availability, different
     * service-specific gRPC clients might check service availability in a
     * different way.
     *
     * @param deviceId the device identifier
     * @return true if client was created and is able to contact the gNMI
     * server; false otherwise
     */
    boolean isReachable(DeviceId deviceId);
}
