/*
 * Copyright 2015-present Open Networking Laboratory
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
 * Key for DeviceDescriptions in ECDeviceStore.
 */
public class DeviceKey {
    private final ProviderId providerId;
    private final DeviceId deviceId;

    public DeviceKey(ProviderId providerId, DeviceId deviceId) {
        this.providerId = providerId;
        this.deviceId = deviceId;
    }

    public ProviderId providerId() {
        return providerId;
    }

    public DeviceId deviceId() {
        return deviceId;
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
        if (!(obj instanceof DeviceKey)) {
            return false;
        }
        DeviceKey that = (DeviceKey) obj;
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
}
