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

package org.onosproject.grpc.ctl;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Striped;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;

import org.onosproject.event.AbstractListenerManager;
import org.onosproject.event.Event;
import org.onosproject.event.EventListener;
import org.onosproject.grpc.api.GrpcChannelController;
import org.onosproject.grpc.api.GrpcChannelId;
import org.onosproject.grpc.api.GrpcClient;
import org.onosproject.grpc.api.GrpcClientController;
import org.onosproject.grpc.api.GrpcClientKey;
import org.onosproject.net.DeviceId;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstract class of a gRPC based client controller for specific gRPC client
 * which provides basic gRPC client management and thread safe mechanism.
 *
 * @param <C> the gRPC client type
 * @param <K> the key type of the gRPC client
 * @param <E> the event type of the gRPC client
 * @param <L> the event listener of event {@link E}
 */
@Component
public abstract class AbstractGrpcClientController
        <K extends GrpcClientKey, C extends GrpcClient, E extends Event, L extends EventListener<E>>
        extends AbstractListenerManager<E, L>
        implements GrpcClientController<K, C> {

    /**
     * The default max inbound message size (MB).
     */
    private static final int DEFAULT_MAX_INBOUND_MSG_SIZE = 256; // Megabytes.
    private static final int MEGABYTES = 1024 * 1024;
    private static final int DEFAULT_DEVICE_LOCK_SIZE = 30;

    private final Logger log = getLogger(getClass());
    private final Map<DeviceId, K> clientKeys = Maps.newHashMap();
    private final Map<K, C> clients = Maps.newHashMap();
    private final Map<DeviceId, GrpcChannelId> channelIds = Maps.newHashMap();
    private final Striped<Lock> stripedLocks = Striped.lock(DEFAULT_DEVICE_LOCK_SIZE);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private GrpcChannelController grpcChannelController;

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        clientKeys.keySet().forEach(this::removeClient);
        clientKeys.clear();
        clients.clear();
        channelIds.clear();
        log.info("Stopped");
    }

    @Override
    public boolean createClient(K clientKey) {
        checkNotNull(clientKey);
        return withDeviceLock(() -> doCreateClient(clientKey), clientKey.deviceId());
    }

    private boolean doCreateClient(K clientKey) {
        DeviceId deviceId = clientKey.deviceId();
        String serverAddr = clientKey.serverAddr();
        int serverPort = clientKey.serverPort();

        if (clientKeys.containsKey(deviceId)) {
            final GrpcClientKey existingKey = clientKeys.get(deviceId);
            if (clientKey.equals(existingKey)) {
                log.debug("Not creating client for {} as it already exists (key={})...",
                        deviceId, clientKey);
                return true;
            } else {
                log.info("Requested client for {} with new " +
                                "endpoint, removing old client (key={})...",
                        deviceId, clientKey);
                doRemoveClient(deviceId);
            }
        }
        log.info("Creating client for {} (server={}:{})...",
                deviceId, serverAddr, serverPort);
        GrpcChannelId channelId = GrpcChannelId.of(clientKey.deviceId(), clientKey.toString());
        ManagedChannelBuilder channelBuilder = NettyChannelBuilder
                .forAddress(serverAddr, serverPort)
                .maxInboundMessageSize(DEFAULT_MAX_INBOUND_MSG_SIZE * MEGABYTES)
                .usePlaintext();

        ManagedChannel channel;
        try {
            channel = grpcChannelController.connectChannel(channelId, channelBuilder);
        } catch (IOException e) {
            log.warn("Unable to connect to gRPC server of {}: {}",
                    clientKey.deviceId(), e.getMessage());
            return false;
        }

        C client = createClientInstance(clientKey, channel);
        if (client == null) {
            log.warn("Cannot create client for {} (key={})", deviceId, clientKey);
            return false;
        }
        clientKeys.put(deviceId, clientKey);
        clients.put(clientKey, client);
        channelIds.put(deviceId, channelId);

        return true;
    }

    protected abstract C createClientInstance(K clientKey, ManagedChannel channel);

    @Override
    public C getClient(DeviceId deviceId) {
        checkNotNull(deviceId);
        return withDeviceLock(() -> doGetClient(deviceId), deviceId);
    }

    protected C doGetClient(DeviceId deviceId) {
        if (!clientKeys.containsKey(deviceId)) {
            return null;
        }
        return clients.get(clientKeys.get(deviceId));
    }

    @Override
    public void removeClient(DeviceId deviceId) {
        checkNotNull(deviceId);
        withDeviceLock(() -> doRemoveClient(deviceId), deviceId);
    }

    private Void doRemoveClient(DeviceId deviceId) {
        if (clientKeys.containsKey(deviceId)) {
            final K clientKey = clientKeys.get(deviceId);
            clients.get(clientKey).shutdown();
            grpcChannelController.disconnectChannel(channelIds.get(deviceId));
            clientKeys.remove(deviceId);
            clients.remove(clientKey);
            channelIds.remove(deviceId);
        }
        return null;
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        checkNotNull(deviceId);
        return withDeviceLock(() -> doIsReachable(deviceId), deviceId);
    }

    protected boolean doIsReachable(DeviceId deviceId) {
        // Default behaviour checks only the gRPC channel, should
        // check according to different gRPC service
        if (!clientKeys.containsKey(deviceId)) {
            log.debug("No client for {}, can't check for reachability", deviceId);
            return false;
        }
        return grpcChannelController.isChannelOpen(channelIds.get(deviceId));
    }

    private <U> U withDeviceLock(Supplier<U> task, DeviceId deviceId) {
        final Lock lock = stripedLocks.get(deviceId);
        lock.lock();
        try {
            return task.get();
        } finally {
            lock.unlock();
        }
    }
}
