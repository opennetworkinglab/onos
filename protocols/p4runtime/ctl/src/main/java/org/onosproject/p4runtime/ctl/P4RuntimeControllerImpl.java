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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.grpc.ctl.AbstractGrpcClientController;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceAgentEvent;
import org.onosproject.net.device.DeviceAgentListener;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeClientKey;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.onosproject.p4runtime.api.P4RuntimeEvent;
import org.onosproject.p4runtime.api.P4RuntimeEventListener;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * P4Runtime controller implementation.
 */
@Component(immediate = true)
@Service
public class P4RuntimeControllerImpl
        extends AbstractGrpcClientController
        <P4RuntimeClientKey, P4RuntimeClient, P4RuntimeEvent, P4RuntimeEventListener>
        implements P4RuntimeController {

    private final Logger log = getLogger(getClass());

    private final ConcurrentMap<DeviceId, ConcurrentMap<ProviderId, DeviceAgentListener>>
            deviceAgentListeners = Maps.newConcurrentMap();
    private final Striped<Lock> stripedLocks = Striped.lock(30);

    private DistributedElectionIdGenerator electionIdGenerator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private StorageService storageService;

    @Activate
    public void activate() {
        super.activate();
        eventDispatcher.addSink(P4RuntimeEvent.class, listenerRegistry);
        electionIdGenerator = new DistributedElectionIdGenerator(storageService);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        deviceAgentListeners.clear();
        electionIdGenerator.destroy();
        electionIdGenerator = null;
        log.info("Stopped");
    }

    @Override
    protected P4RuntimeClient createClientInstance(P4RuntimeClientKey clientKey, ManagedChannel channel) {
        return new P4RuntimeClientImpl(clientKey, channel, this);
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
            case PERMISSION_DENIED:
                handlePermissionDenied(event);
                break;
            default:
                post(event);
                break;
        }
    }

    private void handlePermissionDenied(P4RuntimeEvent event) {
        postDeviceAgentEvent(event.subject().deviceId(), new DeviceAgentEvent(
                DeviceAgentEvent.Type.NOT_MASTER, event.subject().deviceId()));
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
            deviceAgentListeners.get(deviceId).values().forEach(l -> l.event(event));
        }
    }
}
