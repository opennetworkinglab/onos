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

import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;

import java.util.Objects;

/**
 * Key of VLAN cross-connect next objective store.
 */
public class XConnectStoreKey {
    private final DeviceId deviceId;
    private final VlanId vlanId;

    /**
     * Constructs the key of cross-connect next objective store.
     *
     * @param deviceId device ID of the VLAN cross-connection
     * @param vlanId VLAN ID of the VLAN cross-connection
     */
    public XConnectStoreKey(DeviceId deviceId, VlanId vlanId) {
        this.deviceId = deviceId;
        this.vlanId = vlanId;
    }

    /**
     * Returns the device ID of this key.
     *
     * @return device ID
     */
    public DeviceId deviceId() {
        return this.deviceId;
    }

    /**
     * Returns the VLAN ID of this key.
     *
     * @return VLAN ID
     */
    public VlanId vlanId() {
        return this.vlanId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof XConnectStoreKey)) {
            return false;
        }
        XConnectStoreKey that =
                (XConnectStoreKey) o;
        return (Objects.equals(this.deviceId, that.deviceId) &&
                Objects.equals(this.vlanId, that.vlanId));
    }

    // The list of neighbor ids and label are used for comparison.
    @Override
    public int hashCode() {
         return Objects.hash(deviceId, vlanId);
    }

    @Override
    public String toString() {
        return "Device: " + deviceId + " VlanId: " + vlanId;
    }
}
