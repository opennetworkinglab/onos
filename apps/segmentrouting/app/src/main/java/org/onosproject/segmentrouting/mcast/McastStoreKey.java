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

package org.onosproject.segmentrouting.mcast;

import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Objects;

/**
 * Key of multicast next objective store.
 */
public class McastStoreKey {
    // Identify a flow using group address, deviceId, and assigned vlan
    private final IpAddress mcastIp;
    private final DeviceId deviceId;
    private final VlanId vlanId;

    /**
     * Constructs the key of multicast next objective store.
     *
     * @param mcastIp multicast group IP address
     * @param deviceId device ID
     *
     * @deprecated in 1.12 ("Magpie") release.
     */
    @Deprecated
    public McastStoreKey(IpAddress mcastIp, DeviceId deviceId) {
        checkNotNull(mcastIp, "mcastIp cannot be null");
        checkNotNull(deviceId, "deviceId cannot be null");
        checkArgument(mcastIp.isMulticast(), "mcastIp must be a multicast address");
        this.mcastIp = mcastIp;
        this.deviceId = deviceId;
        this.vlanId = null;
    }

    /**
     * Constructs the key of multicast next objective store.
     *
     * @param mcastIp multicast group IP address
     * @param deviceId device ID
     * @param vlanId vlan id
     */
    public McastStoreKey(IpAddress mcastIp, DeviceId deviceId, VlanId vlanId) {
        checkNotNull(mcastIp, "mcastIp cannot be null");
        checkNotNull(deviceId, "deviceId cannot be null");
        checkNotNull(vlanId, "vlan id cannot be null");
        checkArgument(mcastIp.isMulticast(), "mcastIp must be a multicast address");
        this.mcastIp = mcastIp;
        this.deviceId = deviceId;
        // FIXME probably we should avoid not valid values
        this.vlanId = vlanId;
    }

    // Constructor for serialization
    private McastStoreKey() {
        this.mcastIp = null;
        this.deviceId = null;
        this.vlanId = null;
    }

    /**
     * Returns the multicast IP address of this key.
     *
     * @return multicast IP
     */
    public IpAddress mcastIp() {
        return mcastIp;
    }

    /**
     * Returns the device ID of this key.
     *
     * @return device ID
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Returns the vlan ID of this key.
     *
     * @return vlan ID
     */
    public VlanId vlanId() {
        return vlanId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof McastStoreKey)) {
            return false;
        }
        McastStoreKey that =
                (McastStoreKey) o;
        return (Objects.equals(this.mcastIp, that.mcastIp) &&
                Objects.equals(this.deviceId, that.deviceId) &&
                Objects.equals(this.vlanId, that.vlanId));
    }

    @Override
    public int hashCode() {
         return Objects.hash(mcastIp, deviceId, vlanId);
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("mcastIp", mcastIp)
                .add("deviceId", deviceId)
                .add("vlanId", vlanId)
                .toString();
    }
}
