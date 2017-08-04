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

import java.util.Objects;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;

import com.google.common.base.MoreObjects;

/**
 * Identifier for PortDescription from a Provider.
 */
public final class PortFragmentId {
    public final ProviderId providerId;
    public final DeviceId deviceId;
    public final PortNumber portNumber;

    public PortFragmentId(DeviceId deviceId, ProviderId providerId,
                          PortNumber portNumber) {
        this.providerId = providerId;
        this.deviceId = deviceId;
        this.portNumber = portNumber;
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
        if (!(obj instanceof PortFragmentId)) {
            return false;
        }
        PortFragmentId that = (PortFragmentId) obj;
        return Objects.equals(this.deviceId, that.deviceId) &&
               Objects.equals(this.portNumber, that.portNumber) &&
               Objects.equals(this.providerId, that.providerId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("providerId", providerId)
                .add("deviceId", deviceId)
                .add("portNumber", portNumber)
                .toString();
    }

    // for serializer
    @SuppressWarnings("unused")
    private PortFragmentId() {
        this.providerId = null;
        this.deviceId = null;
        this.portNumber = null;
    }
}
