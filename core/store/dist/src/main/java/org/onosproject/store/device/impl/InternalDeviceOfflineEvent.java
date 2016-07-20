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

import org.onosproject.net.DeviceId;
import org.onosproject.store.Timestamp;

import com.google.common.base.MoreObjects;

/**
 * Information published by GossipDeviceStore to notify peers of a device
 * going offline.
 */
public class InternalDeviceOfflineEvent {

    private final DeviceId deviceId;
    private final Timestamp timestamp;

    /**
     * Creates a InternalDeviceOfflineEvent.
     * @param deviceId identifier of device going offline.
     * @param timestamp timestamp of when the device went offline.
     */
    public InternalDeviceOfflineEvent(DeviceId deviceId, Timestamp timestamp) {
        this.deviceId = deviceId;
        this.timestamp = timestamp;
    }

    public DeviceId deviceId() {
        return deviceId;
    }

    public Timestamp timestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("deviceId", deviceId)
                .add("timestamp", timestamp)
                .toString();
    }

    // for serializer
    @SuppressWarnings("unused")
    private InternalDeviceOfflineEvent() {
        deviceId = null;
        timestamp = null;
    }
}
