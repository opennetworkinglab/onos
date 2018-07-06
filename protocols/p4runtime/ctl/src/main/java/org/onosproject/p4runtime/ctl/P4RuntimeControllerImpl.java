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

package org.onosproject.p4runtime.ctl;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Striped;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolverProvider;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.netty.NettyChannelBuilder;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.grpc.api.GrpcChannelId;
import org.onosproject.grpc.api.GrpcController;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceAgentEvent;
import org.onosproject.net.device.DeviceAgentListener;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.onosproject.p4runtime.api.P4RuntimeEvent;
import org.onosproject.p4runtime.api.P4RuntimeEventListener;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * P4Runtime controller implementation.
 */
@Component(immediate = true)
@Service
public class P4RuntimeControllerImpl
        extends AbstractListenerManager<P4RuntimeEvent, P4RuntimeEventListener>
        implements P4RuntimeController {

    private final Logger log = getLogger(getClass());
    private final NameResolverProvider nameResolverProvider = new DnsNameResolverProvider();

    private final Map<DeviceId, ClientKey> clientKeys = Maps.newHashMap();
    private final Map<ClientKey, P4RuntimeClient> clients = Maps.newHashMap();
    private final Map<DeviceId, GrpcChannelId> channelIds = Maps.newHashMap();

    private final ConcurrentMap<DeviceId, List<DeviceAgentListener>> deviceAgentListeners = Maps.newConcurrentMap();
    private final Striped<Lock> stripedLocks = Striped.lock(30);

    private DistributedElectionIdGenerator electionIdGenerator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private GrpcController grpcController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private StorageService storageService;

    @Activate
    public void activate() {
        eventDispatcher.addSink(P4RuntimeEvent.class, listenerRegistry);
        electionIdGenerator = new DistributedElectionIdGenerator(storageService);
        log.info("Started");
    }


    @Deactivate
    public void deactivate() {
        clientKeys.keySet().forEach(this::removeClient);
        clientKeys.clear();
        clients.clear();
        channelIds.clear();
        deviceAgentListeners.clear();
        grpcController = null;
        electionIdGenerator.destroy();
        electionIdGenerator = null;
        eventDispatcher.removeSink(P4RuntimeEvent.class);
        log.info("Stopped");
    }

    @Override
    public boolean createClient(DeviceId deviceId, String serverAddr,
                                int serverPort, long p4DeviceId) {
        checkNotNull(deviceId);
        checkNotNull(serverAddr);
        checkArgument(serverPort > 0, "Invalid server port");

        return withDeviceLock(() -> doCreateClient(
                deviceId, serverAddr, serverPort, p4DeviceId), deviceId);
    }

    private boolean doCreateClient(DeviceId deviceId, String serverAddr,
                                   int serverPort, long p4DeviceId) {

        ClientKey clientKey = new ClientKey(deviceId, serverAddr, serverPort, p4DeviceId);

        if (clientKeys.containsKey(deviceId)) {
            final ClientKey existingKey = clientKeys.get(deviceId);
            if (clientKey.equals(existingKey)) {
                log.info("Not creating client for {} as it already exists (server={}:{}, p4DeviceId={})...",
                         deviceId, serverAddr, serverPort, p4DeviceId);
                return true;
            } else {
                throw new IllegalStateException(
                        "A client for the same device ID but different " +
                                "server endpoints already exists");
            }
        }

        log.info("Creating client for {} (server={}:{}, p4DeviceId={})...",
                 deviceId, serverAddr, serverPort, p4DeviceId);

        GrpcChannelId channelId = GrpcChannelId.of(
                clientKey.deviceId(), "p4runtime-" + clientKey);

        ManagedChannelBuilder channelBuilder = NettyChannelBuilder
                .forAddress(serverAddr, serverPort)
                .usePlaintext(true)
                .nameResolverFactory(nameResolverProvider);

        ManagedChannel channel;
        try {
            channel = grpcController.connectChannel(channelId, channelBuilder);
        } catch (IOException e) {
            log.warn("Unable to connect to gRPC server of {}: {}",
                     clientKey.deviceId(), e.getMessage());
            return false;
        }

        P4RuntimeClient client = new P4RuntimeClientImpl(
                clientKey.deviceId(), clientKey.p4DeviceId(), channel, this);

        clientKeys.put(clientKey.deviceId(), clientKey);
        clients.put(clientKey, client);
        channelIds.put(clientKey.deviceId(), channelId);

        return true;
    }

    @Override
    public P4RuntimeClient getClient(DeviceId deviceId) {
        if (deviceId == null) {
            return null;
        }
        return withDeviceLock(() -> doGetClient(deviceId), deviceId);
    }

    private P4RuntimeClient doGetClient(DeviceId deviceId) {
        if (!clientKeys.containsKey(deviceId)) {
            return null;
        } else {
            return clients.get(clientKeys.get(deviceId));
        }
    }

    @Override
    public void removeClient(DeviceId deviceId) {
        if (deviceId == null) {
            return;
        }
        withDeviceLock(() -> doRemoveClient(deviceId), deviceId);
    }

    private Void doRemoveClient(DeviceId deviceId) {
        if (clientKeys.containsKey(deviceId)) {
            final ClientKey clientKey = clientKeys.get(deviceId);
            clients.get(clientKey).shutdown();
            grpcController.disconnectChannel(channelIds.get(deviceId));
            clientKeys.remove(deviceId);
            clients.remove(clientKey);
            channelIds.remove(deviceId);
        }
        return null;
    }

    @Override
    public boolean hasClient(DeviceId deviceId) {
        return clientKeys.containsKey(deviceId);
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        if (deviceId == null) {
            return false;
        }
        return withDeviceLock(() -> doIsReacheable(deviceId), deviceId);
    }

    private boolean doIsReacheable(DeviceId deviceId) {
        // FIXME: we're not checking for a P4Runtime server, it could be any gRPC service
        if (!clientKeys.containsKey(deviceId)) {
            log.debug("No client for {}, can't check for reachability", deviceId);
            return false;
        }
        return grpcController.isChannelOpen(channelIds.get(deviceId));
    }

    @Override
    public void addDeviceAgentListener(DeviceId deviceId, DeviceAgentListener listener) {
        checkNotNull(deviceId, "deviceId cannot be null");
        checkNotNull(listener, "listener cannot be null");
        deviceAgentListeners.putIfAbsent(deviceId, new CopyOnWriteArrayList<>());
        deviceAgentListeners.get(deviceId).add(listener);
    }

    @Override
    public void removeDeviceAgentListener(DeviceId deviceId, DeviceAgentListener listener) {
        checkNotNull(deviceId, "deviceId cannot be null");
        checkNotNull(listener, "listener cannot be null");
        deviceAgentListeners.computeIfPresent(deviceId, (did, listeners) -> {
            listeners.remove(listener);
            return listeners;
        });
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

    BigInteger newMasterElectionId(DeviceId deviceId) {
        return electionIdGenerator.generate(deviceId);
    }

    void postEvent(P4RuntimeEvent event) {
        switch (event.type()) {
            case CHANNEL_EVENT:
                handleChannelEvent(event);
                break;
            case ARBITRATION_RESPONSE:
                handleArbitrationReply(event);
                break;
            default:
                post(event);
                break;
        }
    }

    private void handleChannelEvent(P4RuntimeEvent event) {
        final ChannelEvent channelEvent = (ChannelEvent) event.subject();
        final DeviceId deviceId = channelEvent.deviceId();
        final DeviceAgentEvent.Type agentEventType;
        switch (channelEvent.type()) {
            case OPEN:
                agentEventType = DeviceAgentEvent.Type.CHANNEL_OPEN;
                break;
            case CLOSED:
                agentEventType = DeviceAgentEvent.Type.CHANNEL_CLOSED;
                break;
            case ERROR:
                agentEventType = !isReachable(deviceId)
                        ? DeviceAgentEvent.Type.CHANNEL_CLOSED
                        : DeviceAgentEvent.Type.CHANNEL_ERROR;
                break;
            default:
                log.warn("Unrecognized channel event type {}", channelEvent.type());
                return;
        }
        postDeviceAgentEvent(deviceId, new DeviceAgentEvent(agentEventType, deviceId));
    }

    private void handleArbitrationReply(P4RuntimeEvent event) {
        final DeviceId deviceId = event.subject().deviceId();
        final ArbitrationResponse response = (ArbitrationResponse) event.subject();
        final DeviceAgentEvent.Type roleType = response.isMaster()
                ? DeviceAgentEvent.Type.ROLE_MASTER
                : DeviceAgentEvent.Type.ROLE_STANDBY;
        postDeviceAgentEvent(deviceId, new DeviceAgentEvent(
                roleType, response.deviceId()));
    }

    private void postDeviceAgentEvent(DeviceId deviceId, DeviceAgentEvent event) {
        if (deviceAgentListeners.containsKey(deviceId)) {
            deviceAgentListeners.get(deviceId).forEach(l -> l.event(event));
        }
    }
}
