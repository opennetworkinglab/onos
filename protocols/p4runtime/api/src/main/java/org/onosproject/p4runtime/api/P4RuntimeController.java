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
import io.grpc.ManagedChannelBuilder;
import org.onosproject.event.ListenerService;
import org.onosproject.net.DeviceId;

/**
 * Controller of P4Runtime devices.
 */
@Beta
public interface P4RuntimeController extends ListenerService<P4RuntimeEvent, P4RuntimeEventListener> {

    /**
     * Instantiates a new client to operate on the device identified by the given information and reachable using the
     * given gRPC channel builder. As a result of this method, a {@link P4RuntimeClient} can be later obtained by
     * invoking {@link #getClient(DeviceId)}. Only one client can exist for the same device identifier. Returns true if
     * the client was created and the channel to the device is open, false otherwise.
     *
     * @param deviceId       device identifier
     * @param p4DeviceId     P4Runtime-specific device identifier
     * @param channelBuilder gRPC channel builder pointing at the P4Runtime server in execution on the device
     * @return true if the client was created and the channel to the device is open
     * @throws IllegalStateException if a client already exists for the given device identifier
     */
    boolean createClient(DeviceId deviceId, long p4DeviceId, ManagedChannelBuilder channelBuilder);

    /**
     * Returns a client to operate on the given device.
     *
     * @param deviceId device identifier
     * @return client instance
     * @throws IllegalStateException if no client exists for the given device identifier
     */
    P4RuntimeClient getClient(DeviceId deviceId);

    /**
     * Removes the client for the given device. If no client exists for the given device identifier, the
     * result is a no-op.
     *
     * @param deviceId device identifier
     */
    void removeClient(DeviceId deviceId);

    /**
     * Returns true if a client exists for the given device identifier, false otherwise.
     *
     * @param deviceId device identifier
     * @return true if client exists, false otherwise.
     */
    boolean hasClient(DeviceId deviceId);

    /**
     * Returns true if the P4Runtime server running on the given device is reachable, i.e. the channel is open and the
     * server is able to respond to RPCs, false otherwise. Reachability can be tested only if a client was previously
     * created using {@link #createClient(DeviceId, long, ManagedChannelBuilder)}, otherwise this method returns false.
     *
     * @param deviceId device identifier.
     * @return true if a client was created and is able to contact the P4Runtime server, false otherwise.
     */
    boolean isReacheable(DeviceId deviceId);

    /**
     * Gets new election id for device arbitration request.
     *
     * @return the election id
     */
    long getNewMasterElectionId();
}
