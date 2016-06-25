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
package org.onosproject.incubator.net.virtual;

import org.onlab.packet.ChassisId;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.*;

/**
 * Default representation of a virtual device.
 */
public final class DefaultVirtualDevice extends DefaultDevice implements VirtualDevice {

    private static final String VIRTUAL = "virtual";
    private static final ProviderId PID = new ProviderId(VIRTUAL, VIRTUAL);

    private final NetworkId networkId;

    /**
     * Creates a network element attributed to the specified provider.
     *
     * @param networkId network identifier
     * @param id        device identifier
     */
    public DefaultVirtualDevice(NetworkId networkId, DeviceId id) {
        super(PID, id, Type.VIRTUAL, VIRTUAL, VIRTUAL, VIRTUAL, VIRTUAL,
              new ChassisId(0));
        this.networkId = networkId;
    }

    @Override
    public NetworkId networkId() {
        return networkId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultVirtualDevice) {
            DefaultVirtualDevice that = (DefaultVirtualDevice) obj;
            return super.equals(that) && Objects.equals(this.networkId, that.networkId);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("networkId", networkId).toString();
    }
}
