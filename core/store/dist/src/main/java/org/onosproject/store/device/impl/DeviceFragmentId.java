/*
 * Copyright 2014-present Open Networking Laboratory
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
import org.onosproject.net.provider.ProviderId;

import com.google.common.base.MoreObjects;

/**
 * Identifier for DeviceDesctiption from a Provider.
 */
public final class DeviceFragmentId {
    public final ProviderId providerId;
    public final DeviceId deviceId;

    public DeviceFragmentId(DeviceId deviceId, ProviderId providerId) {
        this.providerId = providerId;
        this.deviceId = deviceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId, deviceId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DeviceFragmentId)) {
            return false;
        }
        DeviceFragmentId that = (DeviceFragmentId) obj;
        return Objects.equals(this.deviceId, that.deviceId) &&
               Objects.equals(this.providerId, that.providerId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("providerId", providerId)
                .add("deviceId", deviceId)
                .toString();
    }

    // for serializer
    @SuppressWarnings("unused")
    private DeviceFragmentId() {
        this.providerId = null;
        this.deviceId = null;
    }
}
