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
 * Key of Subnet to NextObjective store.
 */
public class SubnetNextObjectiveStoreKey {
    private final DeviceId deviceId;
    private final IpPrefix prefix;

    /**
     * Constructs the key of subnet next objective store.
     *
     * @param deviceId device ID
     * @param prefix subnet information
     */
    public SubnetNextObjectiveStoreKey(DeviceId deviceId,
                                       IpPrefix prefix) {
        this.deviceId = deviceId;
        this.prefix = prefix;
    }

    /**
     * Gets device id in this SubnetNextObjectiveStoreKey.
     *
     * @return device id
     */
    public DeviceId deviceId() {
        return this.deviceId;
    }

    /**
     * Gets subnet information in this SubnetNextObjectiveStoreKey.
     *
     * @return subnet information
     */
    public IpPrefix prefix() {
        return this.prefix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SubnetNextObjectiveStoreKey)) {
            return false;
        }
        SubnetNextObjectiveStoreKey that =
                (SubnetNextObjectiveStoreKey) o;
        return (Objects.equals(this.deviceId, that.deviceId) &&
                Objects.equals(this.prefix, that.prefix));
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, prefix);
    }

    @Override
    public String toString() {
        return "Device: " + deviceId + " Subnet: " + prefix;
    }
}
