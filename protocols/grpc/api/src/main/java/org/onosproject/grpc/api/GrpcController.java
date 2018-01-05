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

package org.onosproject.grpc.api;

import com.google.common.annotations.Beta;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.onosproject.net.DeviceId;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Abstraction of a gRPC controller that stores and manages gRPC channels.
 */
@Beta
public interface GrpcController {

    int CONNECTION_TIMEOUT_SECONDS = 20;

    /**
     * Creates a gRPC managed channel from the given builder and opens a
     * connection to it. If the connection is successful returns the managed
     * channel object and stores the channel internally, associated with the
     * given channel ID.
     * <p>
     * This method blocks until the channel is open or a timeout expires. By
     * default the timeout is {@link #CONNECTION_TIMEOUT_SECONDS} seconds. If
     * the timeout expires, a IOException is thrown.
     *
     * @param channelId      ID of the channel
     * @param channelBuilder builder of the managed channel
     * @return the managed channel created
     * @throws IOException if the channel cannot be opened
     */
    ManagedChannel connectChannel(GrpcChannelId channelId,
                                  ManagedChannelBuilder<?> channelBuilder)
            throws IOException;

    /**
     * Closes the gRPC managed channel (i.e., disconnects from the gRPC server)
     * and removed the channel from this controller.
     *
     * @param channelId ID of the channel to remove
     */
    void disconnectChannel(GrpcChannelId channelId);

    /**
     * Returns all channels known by this controller, each one mapped to the ID
     * passed at creation time.
     *
     * @return map of all the channels with their ID as stored in this
     * controller
     */
    Map<GrpcChannelId, ManagedChannel> getChannels();

    /**
     * Returns true if the channel associated with the given identifier is open,
     * i.e. the server is able to successfully replies to RPCs, false
     * otherwise.
     *
     * @param channelId channel ID
     * @return true if channel is open, false otherwise.
     */
    boolean isChannelOpen(GrpcChannelId channelId);

    /**
     * Returns all channel associated to the given device ID.
     *
     * @param deviceId device ID
     * @return collection of channels
     */
    Collection<ManagedChannel> getChannels(DeviceId deviceId);

    /**
     * If present, returns the channel associated with the given ID.
     *
     * @param channelId channel ID
     * @return optional channel
     */
    Optional<ManagedChannel> getChannel(GrpcChannelId channelId);

}
