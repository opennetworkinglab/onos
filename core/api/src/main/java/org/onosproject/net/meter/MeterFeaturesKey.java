/*
 * Copyright 2016-present Open Networking Laboratory
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
 * A meter features key represents a meter features uniquely.
 * Right now only deviceId is used but this class might be useful in
 * virtualization in which a unique deviceId could have multiple features (guess).
 */
public final class MeterFeaturesKey {
    private final DeviceId deviceId;

    private MeterFeaturesKey(DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    public static MeterFeaturesKey key(DeviceId did) {
        return new MeterFeaturesKey(did);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MeterFeaturesKey mfk = (MeterFeaturesKey) obj;
        return Objects.equals(deviceId, mfk.deviceId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId);
    }

    @Override
    public String toString() {
        return "mfk@" + deviceId.toString();
    }

    public DeviceId deviceId() {
        return deviceId;
    }
}
