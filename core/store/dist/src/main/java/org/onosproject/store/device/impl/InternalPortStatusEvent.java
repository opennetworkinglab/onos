/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.store.device.impl;

import org.onosproject.net.DeviceId;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.impl.Timestamped;

import com.google.common.base.MoreObjects;

/**
 * Information published by GossipDeviceStore to notify peers of a port
 * status change event.
 */
public class InternalPortStatusEvent {

    private final ProviderId providerId;
    private final DeviceId deviceId;
    private final Timestamped<PortDescription> portDescription;

    protected InternalPortStatusEvent(
            ProviderId providerId,
            DeviceId deviceId,
            Timestamped<PortDescription> portDescription) {
        this.providerId = providerId;
        this.deviceId = deviceId;
        this.portDescription = portDescription;
    }

    public DeviceId deviceId() {
        return deviceId;
    }

    public ProviderId providerId() {
        return providerId;
    }

    public Timestamped<PortDescription> portDescription() {
        return portDescription;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("providerId", providerId)
                .add("deviceId", deviceId)
                .add("portDescription", portDescription)
                .toString();
    }

    // for serializer
    protected InternalPortStatusEvent() {
        this.providerId = null;
        this.deviceId = null;
        this.portDescription = null;
    }
}
