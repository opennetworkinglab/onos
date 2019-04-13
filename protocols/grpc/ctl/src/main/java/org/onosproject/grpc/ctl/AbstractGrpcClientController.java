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
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.event.Event;
import org.onosproject.event.EventListener;
import org.onosproject.grpc.api.GrpcClient;
import org.onosproject.grpc.api.GrpcClientController;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceAgentEvent;
import org.onosproject.net.device.DeviceAgentListener;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstract class of a controller for gRPC clients which provides means to
 * create clients, associate device agent listeners to them and register other
 * event listeners.
 *
 * @param <C> the gRPC client type
 * @param <E> the event type of the gRPC client
 * @param <L> the event listener of event {@link E}
 */
public abstract class AbstractGrpcClientController
        <C extends GrpcClient, E extends Event, L extends EventListener<E>>
        extends AbstractListenerManager<E, L>
        implements GrpcClientController<C> {

    /**
     * The default max inbound message size (MB).
     */
    private static final int DEFAULT_DEVICE_LOCK_SIZE = 30;

    private final Logger log = getLogger(getClass());
    private final Map<DeviceId, C> clients = Maps.newHashMap();
    private final ConcurrentMap<DeviceId, ConcurrentMap<ProviderId, DeviceAgentListener>>
            deviceAgentListeners = Maps.newConcurrentMap();
    private final Class<E> eventClass;
    private final String serviceName;
    private final Striped<Lock> stripedLocks = Striped.lock(DEFAULT_DEVICE_LOCK_SIZE);

    public AbstractGrpcClientController(Class<E> eventClass, String serviceName) {
        this.eventClass = eventClass;
        this.serviceName = serviceName;
    }

    @Activate
    public void activate() {
        eventDispatcher.addSink(eventClass, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(eventClass);
        clients.clear();
        deviceAgentListeners.clear();
        log.info("Stopped");
    }

    @Override
    public boolean create(DeviceId deviceId, ManagedChannel channel) {
        checkNotNull(deviceId);
        checkNotNull(channel);
        return withDeviceLock(() -> doCreateClient(deviceId, channel), deviceId);
    }

    private boolean doCreateClient(DeviceId deviceId, ManagedChannel channel) {

        if (clients.containsKey(deviceId)) {
            throw new IllegalArgumentException(format(
                    "A %s client already exists for %s", serviceName, deviceId));
        }

        log.info("Creating {}...", clientName(deviceId));

        final C client;
        try {
            client = createClientInstance(deviceId, channel);
        } catch (Throwable e) {
            log.error("Exception while creating {}", clientName(deviceId), e);
            return false;
        }

        if (client == null) {
            log.error("Unable to create {}, implementation returned null...",
                      clientName(deviceId));
            return false;
        }

        clients.put(deviceId, client);
        return true;
    }

    protected abstract C createClientInstance(DeviceId deviceId, ManagedChannel channel);

    @Override
    public C get(DeviceId deviceId) {
        checkNotNull(deviceId);
        return withDeviceLock(() -> clients.get(deviceId), deviceId);
    }

    @Override
    public void remove(DeviceId deviceId) {
        checkNotNull(deviceId);
        withDeviceLock(() -> {
            final C client = clients.remove(deviceId);
            if (client != null) {
                log.info("Removing {}...", clientName(deviceId));
                client.shutdown();
            }
            return null;
        }, deviceId);
    }

    @Override
    public void addDeviceAgentListener(DeviceId deviceId, ProviderId providerId, DeviceAgentListener listener) {
        checkNotNull(deviceId, "deviceId cannot be null");
        checkNotNull(providerId, "providerId cannot be null");
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
        // TODO: extend device agent event to allow delivery of protocol-specific
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

    private String clientName(DeviceId deviceId) {
        return format("%s client for %s", serviceName, deviceId);
    }
}
