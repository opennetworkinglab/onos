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

import com.google.common.base.MoreObjects;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.provider.ProviderId;

/**
 * Remnant of ConfigProvider.
 * @deprecated in Hummingbird(1.7.0)
 */
@Deprecated
public class DeviceInjectedEvent {
    private final ProviderId providerId;
    private final DeviceId deviceId;
    private final DeviceDescription deviceDescription;

    protected DeviceInjectedEvent(
            ProviderId providerId,
            DeviceId deviceId,
            DeviceDescription deviceDescription) {
        this.providerId = providerId;
        this.deviceId = deviceId;
        this.deviceDescription = deviceDescription;
    }

    public DeviceId deviceId() {
        return deviceId;
    }

    public ProviderId providerId() {
        return providerId;
    }

    public DeviceDescription deviceDescription() {
        return deviceDescription;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("providerId", providerId)
                .add("deviceId", deviceId)
                .add("deviceDescription", deviceDescription)
                .toString();
    }

    // for serializer
    protected DeviceInjectedEvent() {
        this.providerId = null;
        this.deviceId = null;
        this.deviceDescription = null;
    }
}
