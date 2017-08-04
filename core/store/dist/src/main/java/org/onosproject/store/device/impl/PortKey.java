/*
 * Copyright 2015-present Open Networking Foundation
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

import java.util.Objects;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;

import com.google.common.base.MoreObjects;

/**
 * Key for PortDescriptions in ECDeviceStore.
 */
public class PortKey {
    private final ProviderId providerId;
    private final DeviceId deviceId;
    private final PortNumber portNumber;

    public PortKey(ProviderId providerId, DeviceId deviceId, PortNumber portNumber) {
        this.providerId = providerId;
        this.deviceId = deviceId;
        this.portNumber = portNumber;
    }

    public ProviderId providerId() {
        return providerId;
    }

    public DeviceId deviceId() {
        return deviceId;
    }

    public PortNumber portNumber() {
        return portNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId, deviceId, portNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PortKey)) {
            return false;
        }
        PortKey that = (PortKey) obj;
        return Objects.equals(this.deviceId, that.deviceId) &&
               Objects.equals(this.providerId, that.providerId) &&
               Objects.equals(this.portNumber, that.portNumber);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("providerId", providerId)
                .add("deviceId", deviceId)
                .add("portNumber", portNumber)
                .toString();
    }
}
