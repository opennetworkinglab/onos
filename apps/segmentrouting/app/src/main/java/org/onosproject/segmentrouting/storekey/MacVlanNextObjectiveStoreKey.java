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
package org.onosproject.segmentrouting.storekey;

import org.onlab.packet.VlanId;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Key of Device/Vlan/MacAddr to NextObjective store.
 */
public class MacVlanNextObjectiveStoreKey {
    private final DeviceId deviceId;
    private final MacAddress macAddr;
    private final VlanId vlanId;

    /**
     * Constructs the key of the next objective store.
     *
     * @param deviceId device ID
     * @param macAddr mac of host
     * @param vlanId vlan of host
     */
    public MacVlanNextObjectiveStoreKey(DeviceId deviceId, MacAddress macAddr, VlanId vlanId) {
        this.deviceId = deviceId;
        this.macAddr = macAddr;
        this.vlanId = vlanId;
    }

    /**
     * Gets device id in this MacVlanNextObjectiveStoreKey.
     *
     * @return device id
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Gets vlan information in this MacVlanNextObjectiveStoreKey.
     *
     * @return vlan information
     */
    public VlanId vlanId() {
        return vlanId;
    }

    /**
     * Gets mac information in this MacVlanNextObjectiveStoreKey.
     *
     * @return mac information
     */
    public MacAddress macAddr() {
        return macAddr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MacVlanNextObjectiveStoreKey)) {
            return false;
        }
        MacVlanNextObjectiveStoreKey that =
                (MacVlanNextObjectiveStoreKey) o;
        return (Objects.equals(this.deviceId, that.deviceId) &&
                Objects.equals(this.vlanId, that.vlanId) &&
                Objects.equals(this.macAddr, that.macAddr));
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, vlanId, macAddr);
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("deviceId", deviceId)
                .add("vlanId", vlanId)
                .add("macAddr", macAddr)
                .toString();
    }
}
