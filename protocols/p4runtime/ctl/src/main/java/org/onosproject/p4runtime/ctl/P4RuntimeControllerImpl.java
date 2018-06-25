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
import org.onosproject.net.device.ChannelEvent;
import org.onosproject.net.device.ChannelListener;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.onosproject.p4runtime.api.P4RuntimeEvent;
import org.onosproject.p4runtime.api.P4RuntimeEventListener;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    private static final String P4R_ELECTION = "p4runtime-election";
    private static final int DEVICE_LOCK_EXPIRE_TIME_IN_MIN = 10;
    private final Logger log = getLogger(getClass());
    private final NameResolverProvider nameResolverProvider = new DnsNameResolverProvider();
    private final Map<DeviceId, ClientKey> deviceIdToClientKey = Maps.newHashMap();
    private final Map<ClientKey, P4RuntimeClient> clientKeyToClient = Maps.newHashMap();
    private final Map<DeviceId, GrpcChannelId> channelIds = Maps.newHashMap();
    private final Map<DeviceId, List<ChannelListener>> channelListeners = Maps.newConcurrentMap();
    private final LoadingCache<DeviceId, ReadWriteLock> deviceLocks = CacheBuilder.newBuilder()
            .expireAfterAccess(DEVICE_LOCK_EXPIRE_TIME_IN_MIN, TimeUnit.MINUTES)
            .build(new CacheLoader<DeviceId, ReadWriteLock>() {
                @Override
                public ReadWriteLock load(DeviceId deviceId) {
                    return new ReentrantReadWriteLock();
                }
            });

    private AtomicCounter electionIdGenerator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private GrpcController grpcController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private StorageService storageService;

    @Activate
    public void activate() {
        eventDispatcher.addSink(P4RuntimeEvent.class, listenerRegistry);
        electionIdGenerator = storageService.getAtomicCounter(P4R_ELECTION);

        log.info("Started");
    }


    @Deactivate
    public void deactivate() {
        grpcController = null;
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
        log.info("Creating client for {} (server={}:{}, p4DeviceId={})...",
                 deviceId, serverAddr, serverPort, p4DeviceId);

        try {
            if (deviceIdToClientKey.containsKey(deviceId)) {
                final ClientKey existingKey = deviceIdToClientKey.get(deviceId);
                if (newKey.equals(existingKey)) {
                    return true;
                } else {
                    throw new IllegalStateException(
                            "A client for the same device ID but different " +
                                    "server endpoints already exists");
                }
            } else {
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
                grpcController.disconnectChannel(channelIds.get(deviceId));
                clientKeyToClient.remove(clientKey).shutdown();
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

            return grpcController.isChannelOpen(channelIds.get(deviceId));
        } finally {
            deviceLocks.getUnchecked(deviceId).readLock().unlock();
        }
    }

    @Override
    public long getNewMasterElectionId() {
        return electionIdGenerator.incrementAndGet();
    }

    @Override
    public void addChannelListener(DeviceId deviceId, ChannelListener listener) {
        channelListeners.compute(deviceId, (devId, listeners) -> {
            List<ChannelListener> newListeners;
            if (listeners != null) {
                newListeners = listeners;
            } else {
                newListeners = new ArrayList<>();
            }
            newListeners.add(listener);
            return newListeners;
        });
    }

    @Override
    public void removeChannelListener(DeviceId deviceId, ChannelListener listener) {
        channelListeners.compute(deviceId, (devId, listeners) -> {
            if (listeners != null) {
                listeners.remove(listener);
                return listeners;
            } else {
                log.debug("Device {} has no listener registered", deviceId);
                return null;
            }
        });
    }

    void postEvent(P4RuntimeEvent event) {
        if (event.type().equals(P4RuntimeEvent.Type.CHANNEL_EVENT)) {
            DefaultChannelEvent channelError = (DefaultChannelEvent) event.subject();
            DeviceId deviceId = event.subject().deviceId();
            ChannelEvent channelEvent = null;
            //If disconnection is already known we propagate it.
            if (channelError.type().equals(ChannelEvent.Type.CHANNEL_DISCONNECTED)) {
                channelEvent = new ChannelEvent(ChannelEvent.Type.CHANNEL_DISCONNECTED, channelError.deviceId(),
                        channelError.throwable());
            } else if (channelError.type().equals(ChannelEvent.Type.CHANNEL_ERROR)) {
                //If we don't know what the error is we check for reachability
                if (!isReacheable(deviceId)) {
                    //if false the channel has disconnected
                    channelEvent = new ChannelEvent(ChannelEvent.Type.CHANNEL_DISCONNECTED, channelError.deviceId(),
                            channelError.throwable());
                } else {
                    // else we propagate the event.
                    channelEvent = new ChannelEvent(ChannelEvent.Type.CHANNEL_ERROR, channelError.deviceId(),
                            channelError.throwable());
                }
            }
            //Ignoring CHANNEL_CONNECTED
            if (channelEvent != null && channelListeners.get(deviceId) != null) {
                for (ChannelListener listener : channelListeners.get(deviceId)) {
                    listener.event(channelEvent);
                }
            }
        } else {
            post(event);
        }
    }

}
