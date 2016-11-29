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
package org.onosproject.segmentrouting.storekey;

import org.onlab.packet.IpPrefix;
import org.onosproject.net.DeviceId;

import java.util.Objects;

/**
 * Key of assigned VLAN ID store.
 */
public class SubnetAssignedVidStoreKey {
    private final DeviceId deviceId;
    private final IpPrefix subnet;

    /**
     * Constructs the key of per subnet VLAN ID store.
     *
     * @param deviceId device ID of the VLAN cross-connection
     * @param subnet subnet information
     */
    public SubnetAssignedVidStoreKey(DeviceId deviceId, IpPrefix subnet) {
        this.deviceId = deviceId;
        this.subnet = subnet;
    }

    /**
     * Default constructor for Kryo.
     */
    protected SubnetAssignedVidStoreKey() {
        this.deviceId = null;
        this.subnet = null;
    }

    /**
     * Returns the device identification used to create this key.
     *
     * @return the device identifier
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Returns the subnet information used to create this key.
     *
     * @return the subnet
     */
    public IpPrefix subnet() {
        return subnet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SubnetAssignedVidStoreKey)) {
            return false;
        }
        SubnetAssignedVidStoreKey that =
                (SubnetAssignedVidStoreKey) o;
        return (Objects.equals(this.deviceId, that.deviceId) &&
                Objects.equals(this.subnet, that.subnet));
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + Objects.hashCode(deviceId)
                + Objects.hashCode(subnet);
        return result;
    }

    @Override
    public String toString() {
        return "Device: " + deviceId + " Subnet: " + subnet;
    }

}
