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
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.event.Event;
import org.onosproject.event.EventListener;
import org.onosproject.grpc.api.GrpcChannelController;
import org.onosproject.grpc.api.GrpcChannelId;
import org.onosproject.grpc.api.GrpcClient;
import org.onosproject.grpc.api.GrpcClientController;
import org.onosproject.grpc.api.GrpcClientKey;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceAgentEvent;
import org.onosproject.net.device.DeviceAgentListener;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import javax.net.ssl.SSLException;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
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
    private final ConcurrentMap<DeviceId, ConcurrentMap<ProviderId, DeviceAgentListener>>
            deviceAgentListeners = Maps.newConcurrentMap();
    private final Class<E> eventClass;
    private final Striped<Lock> stripedLocks = Striped.lock(DEFAULT_DEVICE_LOCK_SIZE);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected GrpcChannelController grpcChannelController;

    public AbstractGrpcClientController(Class<E> eventClass) {
        this.eventClass = eventClass;
    }

    @Activate
    public void activate() {
        eventDispatcher.addSink(eventClass, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(eventClass);
        clientKeys.keySet().forEach(this::removeClient);
        clientKeys.clear();
        clients.clear();
        channelIds.clear();
        deviceAgentListeners.clear();
        log.info("Stopped");
    }

    @Override
    public boolean createClient(K clientKey) {
        checkNotNull(clientKey);
        return withDeviceLock(() -> doCreateClient(clientKey),
                              clientKey.deviceId());
    }

    private boolean doCreateClient(K clientKey) {
        DeviceId deviceId = clientKey.deviceId();

        if (clientKeys.containsKey(deviceId)) {
            final GrpcClientKey existingKey = clientKeys.get(deviceId);
            if (clientKey.equals(existingKey)) {
                log.debug("Not creating {} as it already exists... (key={})",
                          clientName(clientKey), clientKey);
                return true;
            } else {
                throw new IllegalArgumentException(format(
                        "A client already exists for device %s (%s)",
                        deviceId, clientKey));
            }
        }

        final String method = clientKey.requiresSecureChannel()
                ? "TLS" : "plaintext TCP";

        log.info("Connecting {} client for {} to server at {} using {}...",
                 clientKey.serviceName(), deviceId, clientKey.serveUri(), method);

        SslContext sslContext = null;
        if (clientKey.requiresSecureChannel()) {
            try {
                // FIXME: Accept any server certificate; this is insecure and
                //  should not be used in production
                sslContext = GrpcSslContexts.forClient().trustManager(
                        InsecureTrustManagerFactory.INSTANCE).build();
            } catch (SSLException e) {
                log.error("Failed to build SSL Context", e);
                return false;
            }
        }

        GrpcChannelId channelId = GrpcChannelId.of(clientKey.toString());
        NettyChannelBuilder channelBuilder = NettyChannelBuilder
                .forAddress(clientKey.serveUri().getHost(),
                            clientKey.serveUri().getPort())
                .maxInboundMessageSize(DEFAULT_MAX_INBOUND_MSG_SIZE * MEGABYTES);

        if (sslContext != null) {
            channelBuilder
                    .sslContext(sslContext)
                    .useTransportSecurity();
        } else {
            channelBuilder.usePlaintext();
        }

        final ManagedChannel channel;

        try {
            channel = grpcChannelController.connectChannel(channelId, channelBuilder);
        } catch (Throwable e) {
            log.warn("Failed to connect to {} ({}) using {}: {}",
                     deviceId, clientKey.serveUri(), method, e.toString());
            log.debug("gRPC client connection exception", e);
            return false;
        }

        final C client;
        try {
            client = createClientInstance(clientKey, channel);
        } catch (Throwable e) {
            log.error("Exception while creating {}", clientName(clientKey), e);
            grpcChannelController.disconnectChannel(channelId);
            return false;
        }

        if (client == null) {
            log.error("Unable to create {}, implementation returned null... (key={})",
                      clientName(clientKey), clientKey);
            grpcChannelController.disconnectChannel(channelId);
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

    private C doGetClient(DeviceId deviceId) {
        if (!clientKeys.containsKey(deviceId)) {
            return null;
        }
        return clients.get(clientKeys.get(deviceId));
    }

    @Override
    public C getClient(K clientKey) {
        checkNotNull(clientKey);
        return clients.get(clientKey);
    }

    @Override
    public void removeClient(DeviceId deviceId) {
        checkNotNull(deviceId);
        withDeviceLock(() -> doRemoveClient(deviceId), deviceId);
    }

    @Override
    public void removeClient(K clientKey) {
        checkNotNull(clientKey);
        withDeviceLock(() -> doRemoveClient(clientKey), clientKey.deviceId());
    }

    private Void doRemoveClient(DeviceId deviceId) {
        if (clientKeys.containsKey(deviceId)) {
            doRemoveClient(clientKeys.get(deviceId));
        }
        return null;
    }

    private Void doRemoveClient(K clientKey) {
        if (clients.containsKey(clientKey)) {
            clients.get(clientKey).shutdown();
        }
        if (channelIds.containsKey(clientKey.deviceId())) {
            grpcChannelController.disconnectChannel(
                    channelIds.get(clientKey.deviceId()));
        }
        clientKeys.remove(clientKey.deviceId());
        clients.remove(clientKey);
        channelIds.remove(clientKey.deviceId());
        return null;
    }

    @Override
    public void addDeviceAgentListener(DeviceId deviceId, ProviderId providerId, DeviceAgentListener listener) {
        checkNotNull(deviceId, "deviceId cannot be null");
        checkNotNull(deviceId, "providerId cannot be null");
        checkNotNull(listener, "listener cannot be null");
        deviceAgentListeners.putIfAbsent(deviceId, Maps.newConcurrentMap());
        deviceAgentListeners.get(deviceId).put(providerId, listener);
    }

    @Override
    public void removeDeviceAgentListener(DeviceId deviceId, ProviderId providerId) {
        checkNotNull(deviceId, "deviceId cannot be null");
        checkNotNull(providerId, "listener cannot be null");
        deviceAgentListeners.computeIfPresent(deviceId, (did, listeners) -> {
            listeners.remove(providerId);
            return listeners.isEmpty() ? null : listeners;
        });
    }

    public void postEvent(E event) {
        checkNotNull(event);
        post(event);
    }

    public void postEvent(DeviceAgentEvent event) {
        // We should have only one event delivery mechanism. We have two now
        // because we have two different types of events, DeviceAgentEvent and
        // controller/protocol specific ones (e.g. P4Runtime or gNMI).
        // TODO: extend device agent event to allow delivery protocol-specific
        //  events, e.g. packet-in
        checkNotNull(event);
        if (deviceAgentListeners.containsKey(event.subject())) {
            deviceAgentListeners.get(event.subject()).values()
                    .forEach(l -> l.event(event));
        }
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

    private String clientName(GrpcClientKey key) {
        return format("%s client for %s", key.serviceName(), key.deviceId());
    }
}
