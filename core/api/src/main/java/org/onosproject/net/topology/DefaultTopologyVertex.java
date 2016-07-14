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
package org.onosproject.net.topology;

import org.onosproject.net.DeviceId;

import java.util.Objects;

/**
 * Implementation of the topology vertex backed by a device id.
 */
public class DefaultTopologyVertex implements TopologyVertex {

    private final DeviceId deviceId;

    /**
     * Creates a new topology vertex.
     *
     * @param deviceId backing infrastructure device identifier
     */
    public DefaultTopologyVertex(DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public int hashCode() {
        return deviceId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultTopologyVertex) {
            final DefaultTopologyVertex other = (DefaultTopologyVertex) obj;
            return Objects.equals(this.deviceId, other.deviceId);
        }
        return false;
    }

    @Override
    public String toString() {
        return deviceId.toString();
    }

}

