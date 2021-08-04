/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.net.meter;

import org.onosproject.net.DeviceId;

import java.util.Objects;

/**
 * MeterTableKey is used to represent a single meter table in each device uniquely.
 */
public final class MeterTableKey {
    private final DeviceId deviceId;
    private final MeterScope scope;

    private MeterTableKey(DeviceId deviceId, MeterScope scope) {
        this.deviceId = deviceId;
        this.scope = scope;
    }

    public static MeterTableKey key(DeviceId did, MeterScope scope) {
        return new MeterTableKey(did, scope);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MeterTableKey mtk = (MeterTableKey) obj;
        return Objects.equals(deviceId, mtk.deviceId()) && Objects.equals(scope, mtk.scope());
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, scope);
    }

    @Override
    public String toString() {
        return "mtk@" + deviceId.toString() + " scope:" + scope.toString();
    }

    public DeviceId deviceId() {
        return deviceId;
    }

    public MeterScope scope() {
        return scope;
    }
}
