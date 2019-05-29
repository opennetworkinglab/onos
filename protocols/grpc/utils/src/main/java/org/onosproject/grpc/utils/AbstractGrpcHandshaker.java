/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.grpc.utils;

import com.google.common.util.concurrent.Striped;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import org.onosproject.grpc.api.GrpcChannelController;
import org.onosproject.grpc.api.GrpcClient;
import org.onosproject.grpc.api.GrpcClientController;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceAgentListener;
import org.onosproject.net.device.DeviceHandshaker;
import org.onosproject.net.provider.ProviderId;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Abstract implementation of DeviceHandshaker that uses gRPC to establish a
 * connection to the device.
 *
 * @param <CLIENT> gRPC client class
 * @param <CTRL>   gRPC controller class
 */
public abstract class AbstractGrpcHandshaker
        <CLIENT extends GrpcClient, CTRL extends GrpcClientController<CLIENT>>
        extends AbstractGrpcHandlerBehaviour<CLIENT, CTRL>
        implements DeviceHandshaker {

    /**
     * Creates a new instance of this behaviour for the given gRPC controller
     * class.
     *
     * @param controllerClass gRPC controller class
     */
    public AbstractGrpcHandshaker(Class<CTRL> controllerClass) {
        super(controllerClass);
    }

    private static final Striped<Lock> DEVICE_LOCKS = Striped.lock(10);

    @Override
    public boolean connect() {
        final GrpcChannelController channelController = handler().get(
                GrpcChannelController.class);
        final CTRL clientController = handler().get(controllerClass);
        final DeviceId deviceId = data().deviceId();

        final URI netcfgUri = mgmtUriFromNetcfg();
        if (netcfgUri == null) {
            return false;
        }

        DEVICE_LOCKS.get(deviceId).lock();
        try {
            if (clientController.get(deviceId) != null) {
                throw new IllegalStateException(
                        "A client for this device already exists");
            }

            // Create or get an existing channel. We support sharing the same
            // channel by different drivers for the same device.
            final ManagedChannel channel;
            final URI existingChannelUri = CHANNEL_URIS.get(deviceId);
            if (existingChannelUri != null) {
                if (!existingChannelUri.equals(netcfgUri)) {
                    throw new IllegalStateException(
                            "A gRPC channel with different URI already " +
                                    "exists for this device");
                }
                channel = channelController.get(existingChannelUri)
                        .orElseThrow(() -> new IllegalStateException(
                                "Missing gRPC channel in controller"));
            } else {
                channel = channelController.create(netcfgUri);
                // Store channel URI for future use.
                CHANNEL_URIS.put(deviceId, netcfgUri);
                // Trigger connection.
                channel.getState(true);
            }

            return clientController.create(deviceId, channel);
        } finally {
            DEVICE_LOCKS.get(deviceId).unlock();
        }
    }

    @Override
    public boolean hasConnection() {
        final DeviceId deviceId = data().deviceId();
        final URI netcfgUri = mgmtUriFromNetcfg();
        // If a client already exists for this device, but the netcfg with the
        // server endpoints has changed, this will return false.
        DEVICE_LOCKS.get(deviceId).lock();
        try {
            final URI existingChannelUri = CHANNEL_URIS.get(deviceId);
            return existingChannelUri != null &&
                    existingChannelUri.equals(netcfgUri) &&
                    handler().get(GrpcChannelController.class)
                            .get(existingChannelUri).isPresent() &&
                    handler().get(controllerClass)
                            .get(deviceId) != null;
        } finally {
            DEVICE_LOCKS.get(deviceId).unlock();
        }
    }

    @Override
    public void disconnect() {
        final DeviceId deviceId = data().deviceId();
        final URI netcfgUri = mgmtUriFromNetcfg();
        // This removes any clients and channels associated with this device ID.
        DEVICE_LOCKS.get(deviceId).lock();
        try {
            final URI existingChannelUri = CHANNEL_URIS.remove(deviceId);
            handler().get(controllerClass).remove(deviceId);
            if (existingChannelUri != null) {
                handler().get(GrpcChannelController.class).destroy(existingChannelUri);
            }
            if (netcfgUri != null) {
                // This should not be needed if we are sure there can never be
                // two channels for the same device.
                handler().get(GrpcChannelController.class).destroy(netcfgUri);
            }
        } finally {
            DEVICE_LOCKS.get(deviceId).unlock();
        }
    }

    @Override
    public boolean isReachable() {
        return setupBehaviour("isReachable()") && client.isServerReachable();
    }

    @Override
    public CompletableFuture<Boolean> probeReachability() {
        if (!setupBehaviour("probeReachability()")) {
            return completedFuture(false);
        }
        resetChannelConnectBackoffIfNeeded();
        return client.probeService();
    }

    @Override
    public void addDeviceAgentListener(ProviderId providerId, DeviceAgentListener listener) {
        // Don't use controller/deviceId class variables as they might be uninitialized.
        handler().get(controllerClass)
                .addDeviceAgentListener(data().deviceId(), providerId, listener);
    }

    @Override
    public void removeDeviceAgentListener(ProviderId providerId) {
        // Don't use controller/deviceId class variable as they might be uninitialized.
        handler().get(controllerClass)
                .removeDeviceAgentListener(data().deviceId(), providerId);
    }

    private void resetChannelConnectBackoffIfNeeded() {
        // Stimulate channel reconnect if in failure state.
        final ManagedChannel channel = getExistingChannel();
        if (channel == null) {
            // Where did the channel go?
            return;
        }
        if (channel.getState(false)
                .equals(ConnectivityState.TRANSIENT_FAILURE)) {
            channel.resetConnectBackoff();
        }
    }

    private ManagedChannel getExistingChannel() {
        final DeviceId deviceId = data().deviceId();
        if (CHANNEL_URIS.containsKey(deviceId)) {
            return handler().get(GrpcChannelController.class)
                    .get(CHANNEL_URIS.get(deviceId)).orElse(null);
        }
        return null;
    }
}
