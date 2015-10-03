/*
 * Copyright 2015 Open Networking Laboratory
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

import com.google.common.base.MoreObjects;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.provider.ProviderId;

import java.util.List;

public class PortInjectedEvent {

    private ProviderId providerId;
    private DeviceId deviceId;
    private List<PortDescription> portDescriptions;

    protected PortInjectedEvent(ProviderId providerId, DeviceId deviceId, List<PortDescription> portDescriptions) {
        this.providerId = providerId;
        this.deviceId = deviceId;
        this.portDescriptions = portDescriptions;
    }

    public DeviceId deviceId() {
        return deviceId;
    }

    public ProviderId providerId() {
        return providerId;
    }

    public List<PortDescription> portDescriptions() {
        return portDescriptions;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("providerId", providerId)
                .add("deviceId", deviceId)
                .add("portDescriptions", portDescriptions)
                .toString();
    }

    // for serializer
    protected PortInjectedEvent() {
        this.providerId = null;
        this.deviceId = null;
        this.portDescriptions = null;
    }

}
