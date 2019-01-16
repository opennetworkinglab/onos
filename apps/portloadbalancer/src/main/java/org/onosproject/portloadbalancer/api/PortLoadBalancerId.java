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
package org.onosproject.portloadbalancer.api;

import org.onosproject.net.DeviceId;

import java.util.Objects;

/**
 * Port load balancer identifier.
 * It is used to identify a load balancer across the entire system and therefore has to be unique system-wide.
 */
public class PortLoadBalancerId {
    private final DeviceId deviceId;

    /**
     * Port load balancer key.
     * It is used to identify a load balancer on a specific device and therefore has to be unique device-wide.
     */
    private final int key;

    /**
     * Constructs port load balancer ID.
     *
     * @param deviceId device ID
     * @param key port load balancer key
     */
    public PortLoadBalancerId(DeviceId deviceId, int key) {
        this.deviceId = deviceId;
        this.key = key;
    }

    /**
     * Returns port load balancer device ID.
     *
     * @return port load balancer device ID
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Returns port load balancer key.
     *
     * @return port load balancer key
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
        if (!(obj instanceof PortLoadBalancerId)) {
            return false;
        }
        final PortLoadBalancerId other = (PortLoadBalancerId) obj;

        return Objects.equals(this.deviceId, other.deviceId) &&
                Objects.equals(this.key, other.key);
    }

    @Override
    public String toString() {
        return deviceId.toString() + ":" + key;
    }
}
