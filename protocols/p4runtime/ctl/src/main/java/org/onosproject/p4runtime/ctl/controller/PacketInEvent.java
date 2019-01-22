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

package org.onosproject.p4runtime.ctl.controller;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.p4runtime.api.P4RuntimePacketIn;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * P4Runtime packet-in.
 */
public final class PacketInEvent implements P4RuntimePacketIn {

    private final DeviceId deviceId;
    private final PiPacketOperation operation;

    public PacketInEvent(DeviceId deviceId, PiPacketOperation operation) {
        this.deviceId = checkNotNull(deviceId);
        this.operation = checkNotNull(operation);
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public PiPacketOperation packetOperation() {
        return operation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PacketInEvent that = (PacketInEvent) o;
        return Objects.equal(deviceId, that.deviceId) &&
                Objects.equal(operation, that.operation);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId, operation);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("deviceId", deviceId)
                .add("operation", operation)
                .toString();
    }
}
