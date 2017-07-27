/*
 * Copyright 2017-present Open Networking Laboratory
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
 * Abstraction of a gRPC controller. Serves as a one stop shop for obtaining
 * gRPC ManagedChannels to interact with devices and (un)register observers on event streams from the device.
 */
@Beta
public interface GrpcController {

    /**
     * Adds a StreamObserver on a channel specific for a device.
     *
     * @param observerId          the id of the observer
     * @param grpcObserverHandler the manager for the stream.
     */
    void addObserver(GrpcStreamObserverId observerId, GrpcObserverHandler grpcObserverHandler);

    /**
     * Removes the StreamObserver on a channel specific for a device.
     *
     * @param observerId the id of the observer
     */
    void removeObserver(GrpcStreamObserverId observerId);

    /**
     * If present returns the stream observer manager previously added for the given device.
     *
     * @param observerId the id of the observer.
     * @return the ObserverManager
     */
    Optional<GrpcObserverHandler> getObserverManager(GrpcStreamObserverId observerId);

    /**
     * Tries to connect to a specific gRPC server, if the connection is successful
     * returns the ManagedChannel. This method blocks until the channel is setup or a timeout expires.
     * By default the timeout is 20 seconds. If the timeout expires and thus the channel can't be set up
     * a IOException is thrown.
     *
     * @param channelId      the id of the channel
     * @param channelBuilder the builder for the managed channel.
     * @return the ManagedChannel created.
     * @throws IOException if channel can't be setup.
     */
    ManagedChannel connectChannel(GrpcChannelId channelId, ManagedChannelBuilder<?> channelBuilder) throws IOException;

    /**
     * Disconnects a gRPC device by removing it's ManagedChannel from this controller.
     *
     * @param channelId id of the service to remove
     */
    void disconnectChannel(GrpcChannelId channelId);

    /**
     * Gets all ManagedChannels for the gRPC devices.
     *
     * @return Map of all the ManagedChannels with their identifier saved in this controller
     */
    Map<GrpcChannelId, ManagedChannel> getChannels();

    /**
     * Returns true if the channel associated with the given identifier is open, i.e. the server is able to successfully
     * responds to RPCs.
     *
     * @param channelId channel identifier
     * @return true if channel is open, false otherwise.
     */
    boolean isChannelOpen(GrpcChannelId channelId);

    /**
     * Returns all ManagedChannels associated to the given device identifier.
     *
     * @param deviceId the device for which we are interested.
     * @return collection of all the ManagedChannels saved in this controller
     */
    Collection<ManagedChannel> getChannels(DeviceId deviceId);

    /**
     * If present, returns the managed channel associated with the given identifier.
     *
     * @param channelId the id of the channel
     * @return the ManagedChannel of the device.
     */
    Optional<ManagedChannel> getChannel(GrpcChannelId channelId);

}
