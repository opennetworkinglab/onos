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
import io.grpc.StatusRuntimeException;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Abstraction of a gRPC controller that stores and manages gRPC channels.
 */
@Beta
public interface GrpcChannelController {

    int CONNECTION_TIMEOUT_SECONDS = 20;

    /**
     * Creates a gRPC managed channel from the given builder and opens the
     * connection. If the connection is successful, returns the managed channel
     * object and stores the channel internally, associated with the given
     * channel ID.
     * <p>
     * This method blocks until the channel is open or a timeout expires. By
     * default the timeout is {@link #CONNECTION_TIMEOUT_SECONDS} seconds. If
     * the timeout expires, a {@link StatusRuntimeException} is thrown. If
     * another channel with the same ID already exists, an {@link
     * IllegalArgumentException} is thrown.
     *
     * @param channelId      ID of the channel
     * @param channelBuilder builder of the managed channel
     * @return the managed channel created
     * @throws StatusRuntimeException   if the channel cannot be opened
     * @throws IllegalArgumentException if a channel with the same ID already
     *                                  exists
     */
    ManagedChannel connectChannel(GrpcChannelId channelId,
                                  ManagedChannelBuilder<?> channelBuilder);

    /**
     * Closes the gRPC managed channel (i.e., disconnects from the gRPC server)
     * and removes any internal state associated to it.
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
     * If present, returns the channel associated with the given ID.
     *
     * @param channelId channel ID
     * @return optional channel
     */
    Optional<ManagedChannel> getChannel(GrpcChannelId channelId);

    /**
     * Probes the server at the endpoint of the given channel. Returns true if
     * the server responded to the probe, false otherwise or if the channel does
     * not exist.
     *
     * @param channelId channel ID
     * @return completable future eventually true if the gRPC server responded
     * to the probe; false otherwise
     */
    CompletableFuture<Boolean> probeChannel(GrpcChannelId channelId);
}
