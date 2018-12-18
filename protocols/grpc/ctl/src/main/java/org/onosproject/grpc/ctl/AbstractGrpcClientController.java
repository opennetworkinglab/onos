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
import io.grpc.StatusRuntimeException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.NotSslRecordException;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.event.Event;
import org.onosproject.event.EventListener;
import org.onosproject.grpc.api.GrpcChannelController;
import org.onosproject.grpc.api.GrpcChannelId;
import org.onosproject.grpc.api.GrpcClient;
import org.onosproject.grpc.api.GrpcClientController;
import org.onosproject.grpc.api.GrpcClientKey;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;

import javax.net.ssl.SSLException;
import java.util.Map;
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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
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
        /*
            FIXME we might want to move "useTls" and "fallback" to properties of the netcfg and clientKey
                  For now, we will first try to connect with TLS (accepting any cert), then fall back to
                  plaintext for every device
         */
        return withDeviceLock(() -> doCreateClient(clientKey, true, true), clientKey.deviceId());
    }

    private boolean doCreateClient(K clientKey, boolean useTls, boolean fallbackToPlainText) {
        final DeviceId deviceId = clientKey.deviceId();
        final String serverAddr = clientKey.serverAddr();
        final int serverPort = clientKey.serverPort();

        if (clientKeys.containsKey(deviceId)) {
            final GrpcClientKey existingKey = clientKeys.get(deviceId);
            if (clientKey.equals(existingKey)) {
                log.debug("Not creating {} as it already exists... (key={})",
                          clientName(clientKey), clientKey);
                return true;
            } else {
                log.info("Requested new {} with updated key, removing old client... (oldKey={})",
                         clientName(clientKey), existingKey);
                doRemoveClient(deviceId);
            }
        }

        log.info("Creating new {}... (key={}, useTls={}, fallbackToPlainText={})",
                 clientName(clientKey), clientKey, useTls,
                 fallbackToPlainText);

        final GrpcChannelId channelId = GrpcChannelId.of(
                clientKey.deviceId(), clientKey.toString());
        final NettyChannelBuilder channelBuilder = NettyChannelBuilder
                .forAddress(serverAddr, serverPort)
                .maxInboundMessageSize(DEFAULT_MAX_INBOUND_MSG_SIZE * MEGABYTES);

        if (useTls) {
            // FIXME: logic to create/manage SSL properties of a channel builder
            //  should belong to the GrpcChannelController.
            log.debug("Using SSL for {}", clientName(clientKey), deviceId);
            final SslContext sslContext;
            try {
                // Accept any server certificate; this is insecure and should
                // not be used in production
                sslContext = GrpcSslContexts.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
            } catch (SSLException e) {
                log.error("Failed to build SSL context for {}", clientName(clientKey), e);
                return false;
            }
            channelBuilder
                    .sslContext(sslContext)
                    .useTransportSecurity();
        } else {
            log.debug("Using plaintext TCP for {}", clientName(clientKey));
            channelBuilder.usePlaintext();
        }

        final ManagedChannel channel;
        try {
            channel = grpcChannelController.connectChannel(channelId, channelBuilder);
        } catch (Throwable e) {
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                if (useTls && cause instanceof NotSslRecordException) {
                    // Likely root cause is that server is using plaintext
                    log.warn("Failed to connect {} using TLS", clientName(clientKey));
                    log.debug("TLS connection exception", e);
                    if (fallbackToPlainText) {
                        log.info("Falling back to plaintext TCP for {}", clientName(clientKey));
                        return doCreateClient(clientKey, false, false);
                    }
                }
                if (!useTls && "Connection reset by peer".equals(cause.getMessage())) {
                    // Not a great signal, but could indicate the server is expected a TLS connection
                    log.warn("Failed to connect {} using plaintext TCP; " +
                                     "is the server using TLS?",
                             clientName(clientKey));
                    break;
                }
            }
            if (e instanceof StatusRuntimeException) {
                log.warn("Unable to connect {}: {}", clientName(clientKey), e.getMessage());
                log.debug("Connection exception", e);
            } else {
                log.error("Exception while connecting {}", clientName(clientKey), e);
            }
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

    private boolean doIsReachable(DeviceId deviceId) {
        // Default behaviour checks only the gRPC channel, should
        // check according to different gRPC service
        if (!clientKeys.containsKey(deviceId)) {
            log.debug("Missing client for {}, cannot check for reachability", deviceId);
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

    private String clientName(GrpcClientKey key) {
        return format("%s client for %s", key.serviceName(), key.deviceId());
    }
}
