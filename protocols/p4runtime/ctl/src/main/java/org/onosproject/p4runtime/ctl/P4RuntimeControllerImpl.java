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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    private static final int DEVICE_LOCK_EXPIRE_TIME_IN_MIN = 10;
    private final Logger log = getLogger(getClass());
    private final NameResolverProvider nameResolverProvider = new DnsNameResolverProvider();
    private final Map<DeviceId, ClientKey> deviceIdToClientKey = Maps.newHashMap();
    private final Map<ClientKey, P4RuntimeClient> clientKeyToClient = Maps.newHashMap();
    private final Map<DeviceId, GrpcChannelId> channelIds = Maps.newHashMap();
    private final ConcurrentMap<DeviceId, List<DeviceAgentListener>> deviceAgentListeners = Maps.newConcurrentMap();
    private final LoadingCache<DeviceId, ReadWriteLock> deviceLocks = CacheBuilder.newBuilder()
            .expireAfterAccess(DEVICE_LOCK_EXPIRE_TIME_IN_MIN, TimeUnit.MINUTES)
            .build(new CacheLoader<DeviceId, ReadWriteLock>() {
                @Override
                public ReadWriteLock load(DeviceId deviceId) {
                    return new ReentrantReadWriteLock();
                }
            });
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

        ClientKey newKey = new ClientKey(deviceId, serverAddr, serverPort, p4DeviceId);

        ManagedChannelBuilder channelBuilder = NettyChannelBuilder
                .forAddress(serverAddr, serverPort)
                .usePlaintext(true);

        deviceLocks.getUnchecked(deviceId).writeLock().lock();

        try {
            if (deviceIdToClientKey.containsKey(deviceId)) {
                final ClientKey existingKey = deviceIdToClientKey.get(deviceId);
                if (newKey.equals(existingKey)) {
                    log.info("Not creating client for {} as it already exists (server={}:{}, p4DeviceId={})...",
                             deviceId, serverAddr, serverPort, p4DeviceId);
                    return true;
                } else {
                    throw new IllegalStateException(
                            "A client for the same device ID but different " +
                                    "server endpoints already exists");
                }
            } else {
                log.info("Creating client for {} (server={}:{}, p4DeviceId={})...",
                         deviceId, serverAddr, serverPort, p4DeviceId);
                return doCreateClient(newKey, channelBuilder);
            }
        } finally {
            deviceLocks.getUnchecked(deviceId).writeLock().unlock();
        }
    }

    private boolean doCreateClient(ClientKey clientKey, ManagedChannelBuilder channelBuilder) {

        GrpcChannelId channelId = GrpcChannelId.of(clientKey.deviceId(),
                                                   "p4runtime-" + clientKey.p4DeviceId());

        // Channel defaults.
        channelBuilder.nameResolverFactory(nameResolverProvider);

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

        channelIds.put(clientKey.deviceId(), channelId);
        deviceIdToClientKey.put(clientKey.deviceId(), clientKey);
        clientKeyToClient.put(clientKey, client);

        return true;
    }

    @Override
    public P4RuntimeClient getClient(DeviceId deviceId) {

        deviceLocks.getUnchecked(deviceId).readLock().lock();

        try {
            if (!deviceIdToClientKey.containsKey(deviceId)) {
                return null;
            } else {
                return clientKeyToClient.get(deviceIdToClientKey.get(deviceId));
            }
        } finally {
            deviceLocks.getUnchecked(deviceId).readLock().unlock();
        }
    }

    @Override
    public void removeClient(DeviceId deviceId) {

        deviceLocks.getUnchecked(deviceId).writeLock().lock();
        try {
            if (deviceIdToClientKey.containsKey(deviceId)) {
                final ClientKey clientKey = deviceIdToClientKey.get(deviceId);
                clientKeyToClient.remove(clientKey).shutdown();
                grpcController.disconnectChannel(channelIds.get(deviceId));
                deviceIdToClientKey.remove(deviceId);
                channelIds.remove(deviceId);
            }
        } finally {
           deviceLocks.getUnchecked(deviceId).writeLock().unlock();
        }
    }

    @Override
    public boolean hasClient(DeviceId deviceId) {
        deviceLocks.getUnchecked(deviceId).readLock().lock();

        try {
            return deviceIdToClientKey.containsKey(deviceId);
        } finally {
            deviceLocks.getUnchecked(deviceId).readLock().unlock();
        }
    }

    @Override
    public boolean isReacheable(DeviceId deviceId) {

        deviceLocks.getUnchecked(deviceId).readLock().lock();

        try {
            if (!deviceIdToClientKey.containsKey(deviceId)) {
                log.debug("No client for {}, can't check for reachability", deviceId);
                return false;
            }
            // FIXME: we're not checking for a P4Runtime server, it could be any gRPC service
            return grpcController.isChannelOpen(channelIds.get(deviceId));
        } finally {
            deviceLocks.getUnchecked(deviceId).readLock().unlock();
        }
    }

    @Override
    public void addDeviceAgentListener(DeviceId deviceId, DeviceAgentListener listener) {
        deviceAgentListeners.putIfAbsent(deviceId, new CopyOnWriteArrayList<>());
        deviceAgentListeners.get(deviceId).add(listener);
    }

    @Override
    public void removeDeviceAgentListener(DeviceId deviceId, DeviceAgentListener listener) {
        deviceAgentListeners.computeIfPresent(deviceId, (did, listeners) -> {
            listeners.remove(listener);
            return listeners;
        });
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
                agentEventType = !isReacheable(deviceId)
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
