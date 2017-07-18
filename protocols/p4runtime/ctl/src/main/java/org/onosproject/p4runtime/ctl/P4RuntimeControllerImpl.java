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

package org.onosproject.p4runtime.ctl;

import com.google.common.collect.Maps;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolverProvider;
import io.grpc.internal.DnsNameResolverProvider;
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
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.onosproject.p4runtime.api.P4RuntimeEvent;
import org.onosproject.p4runtime.api.P4RuntimeEventListener;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
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
    private final Map<DeviceId, P4RuntimeClient> clients = Maps.newHashMap();
    private final Map<DeviceId, GrpcChannelId> channelIds = Maps.newHashMap();
    // TODO: should use a cache to delete unused locks.
    private final Map<DeviceId, ReadWriteLock> deviceLocks = Maps.newConcurrentMap();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GrpcController grpcController;

    @Activate
    public void activate() {
        log.info("Started");
    }


    @Deactivate
    public void deactivate() {
        grpcController = null;
        log.info("Stopped");
    }


    @Override
    public boolean createClient(DeviceId deviceId, int p4DeviceId, ManagedChannelBuilder channelBuilder) {
        checkNotNull(deviceId);
        checkNotNull(channelBuilder);

        deviceLocks.putIfAbsent(deviceId, new ReentrantReadWriteLock());
        deviceLocks.get(deviceId).writeLock().lock();

        log.info("Creating client for {} (with internal device id {})...", deviceId, p4DeviceId);

        try {
            if (clients.containsKey(deviceId)) {
                throw new IllegalStateException(format("A client already exists for %s", deviceId));
            } else {
                return doCreateClient(deviceId, p4DeviceId, channelBuilder);
            }
        } finally {
            deviceLocks.get(deviceId).writeLock().unlock();
        }
    }

    private boolean doCreateClient(DeviceId deviceId, int p4DeviceId, ManagedChannelBuilder channelBuilder) {

        GrpcChannelId channelId = GrpcChannelId.of(deviceId, "p4runtime");

        // Channel defaults.
        channelBuilder.nameResolverFactory(nameResolverProvider);

        ManagedChannel channel;
        try {
            channel = grpcController.connectChannel(channelId, channelBuilder);
        } catch (IOException e) {
            log.warn("Unable to connect to gRPC server of {}: {}", deviceId, e.getMessage());
            return false;
        }

        P4RuntimeClient client = new P4RuntimeClientImpl(deviceId, p4DeviceId, channel, this);

        channelIds.put(deviceId, channelId);
        clients.put(deviceId, client);

        return true;
    }

    @Override
    public P4RuntimeClient getClient(DeviceId deviceId) {

        deviceLocks.putIfAbsent(deviceId, new ReentrantReadWriteLock());
        deviceLocks.get(deviceId).readLock().lock();

        try {
            return clients.get(deviceId);
        } finally {
            deviceLocks.get(deviceId).readLock().unlock();
        }
    }

    @Override
    public void removeClient(DeviceId deviceId) {

        deviceLocks.putIfAbsent(deviceId, new ReentrantReadWriteLock());
        deviceLocks.get(deviceId).writeLock().lock();

        try {
            if (clients.containsKey(deviceId)) {
                clients.get(deviceId).shutdown();
                grpcController.disconnectChannel(channelIds.get(deviceId));
                clients.remove(deviceId);
                channelIds.remove(deviceId);
            }
        } finally {
            deviceLocks.get(deviceId).writeLock().unlock();
        }
    }

    @Override
    public boolean hasClient(DeviceId deviceId) {

        deviceLocks.putIfAbsent(deviceId, new ReentrantReadWriteLock());
        deviceLocks.get(deviceId).readLock().lock();

        try {
            return clients.containsKey(deviceId);
        } finally {
            deviceLocks.get(deviceId).readLock().unlock();
        }
    }

    @Override
    public boolean isReacheable(DeviceId deviceId) {

        deviceLocks.putIfAbsent(deviceId, new ReentrantReadWriteLock());
        deviceLocks.get(deviceId).readLock().lock();

        try {
            if (!clients.containsKey(deviceId)) {
                log.warn("No client for {}, can't check for reachability", deviceId);
                return false;
            }

            return grpcController.isChannelOpen(channelIds.get(deviceId));
        } finally {
            deviceLocks.get(deviceId).readLock().unlock();
        }
    }

    void postEvent(P4RuntimeEvent event) {
        post(event);
    }
}
