/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.l2lb.api;

import org.onosproject.net.DeviceId;

import java.util.Objects;

/**
 * L2 load balancer identifier.
 * It is used to identify a load balancer across the entire system and therefore has to be unique system-wide.
 */
public class L2LbId {
    private final DeviceId deviceId;

    /**
     * L2 load balancer key.
     * It is used to identify a load balancer on a specific device and therefore has to be unique device-wide.
     */
    private final int key;

    /**
     * Constructs L2 load balancer ID.
     *
     * @param deviceId device ID
     * @param key L2 load balancer key
     */
    public L2LbId(DeviceId deviceId, int key) {
        this.deviceId = deviceId;
        this.key = key;
    }

    /**
     * Returns L2 load balancer device ID.
     *
     * @return L2 load balancer device ID
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Returns L2 load balancer key.
     *
     * @return L2 load balancer key
     */
    public int key() {
        return key;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, key);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof L2LbId)) {
            return false;
        }
        final L2LbId other = (L2LbId) obj;

        return Objects.equals(this.deviceId, other.deviceId) &&
                Objects.equals(this.key, other.key);
    }

    @Override
    public String toString() {
        return deviceId.toString() + ":" + key;
    }
}
