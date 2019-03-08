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

package org.onosproject.drivers.gnmi;

import com.google.common.collect.Maps;
import org.onlab.util.SharedExecutors;
import org.onosproject.gnmi.api.GnmiController;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceAgentEvent;
import org.onosproject.net.device.DeviceAgentListener;
import org.onosproject.net.device.DeviceHandshaker;
import org.onosproject.net.provider.ProviderId;

import java.util.TimerTask;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of DeviceHandshaker that allows using the gNMI driver as a
 * standalone one. Since gNMI does not support mastership, this driver stores a
 * copy of the agent listeners registered by the providers, needed to
 * acknowledge whatever role was requested.
 */
public class GnmiHandshakerStandalone
        extends GnmiHandshaker implements DeviceHandshaker {

    private static final int ROLE_REPLY_LATENCY_MILLIS = 200;
    private static final ConcurrentMap<DeviceId, ConcurrentMap<ProviderId, DeviceAgentListener>>
            AGENT_LISTENERS = Maps.newConcurrentMap();
    private static final ConcurrentMap<DeviceId, MastershipRole>
            ROLES = Maps.newConcurrentMap();

    @Override
    public void roleChanged(MastershipRole newRole) {
        final DeviceAgentEvent.Type eventType;
        switch (newRole) {
            case MASTER:
                eventType = DeviceAgentEvent.Type.ROLE_MASTER;
                break;
            case STANDBY:
                eventType = DeviceAgentEvent.Type.ROLE_STANDBY;
                break;
            case NONE:
                eventType = DeviceAgentEvent.Type.ROLE_NONE;
                break;
            default:
                log.error("Unrecognized mastership role {}", newRole);
                return;
        }
        SharedExecutors.getTimer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (newRole == MastershipRole.NONE) {
                    ROLES.remove(deviceId);
                } else {
                    ROLES.put(data().deviceId(), newRole);
                }
                postAgentEvent(new DeviceAgentEvent(
                        eventType, data().deviceId()));
            }
        }, ROLE_REPLY_LATENCY_MILLIS);
    }

    @Override
    public MastershipRole getRole() {
        return ROLES.getOrDefault(data().deviceId(), MastershipRole.NONE);
    }

    @Override
    public void addDeviceAgentListener(ProviderId providerId, DeviceAgentListener listener) {
        handler().get(GnmiController.class)
                .addDeviceAgentListener(data().deviceId(), providerId, listener);
        AGENT_LISTENERS.putIfAbsent(deviceId, Maps.newConcurrentMap());
        AGENT_LISTENERS.get(deviceId).put(providerId, listener);
    }

    @Override
    public void removeDeviceAgentListener(ProviderId providerId) {
        handler().get(GnmiController.class)
                .removeDeviceAgentListener(data().deviceId(), providerId);
        AGENT_LISTENERS.computeIfPresent(deviceId, (did, listeners) -> {
            listeners.remove(providerId);
            return listeners.isEmpty() ? null : listeners;
        });
    }

    private void postAgentEvent(DeviceAgentEvent event) {
        checkNotNull(event);
        if (AGENT_LISTENERS.containsKey(event.subject())) {
            AGENT_LISTENERS.get(event.subject()).values()
                    .forEach(l -> l.event(event));
        }
    }

    @Override
    public void disconnect() {
        ROLES.remove(data().deviceId());
        AGENT_LISTENERS.remove(data().deviceId());
        super.disconnect();
    }
}
