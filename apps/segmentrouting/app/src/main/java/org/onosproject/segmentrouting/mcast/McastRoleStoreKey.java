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
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Key of multicast role store.
 */
public class McastRoleStoreKey {
    // Identify role using group address, deviceId and source
    private final IpAddress mcastIp;
    private final DeviceId deviceId;
    private final ConnectPoint source;

    /**
     * Constructs the key of multicast role store.
     *
     * @param mcastIp multicast group IP address
     * @param deviceId device ID
     * @param source source connect point
     */
    public McastRoleStoreKey(IpAddress mcastIp, DeviceId deviceId, ConnectPoint source) {
        checkNotNull(mcastIp, "mcastIp cannot be null");
        checkNotNull(deviceId, "deviceId cannot be null");
        checkNotNull(source, "source cannot be null");
        checkArgument(mcastIp.isMulticast(), "mcastIp must be a multicast address");
        this.mcastIp = mcastIp;
        this.deviceId = deviceId;
        this.source = source;
    }

    // Constructor for serialization
    private McastRoleStoreKey() {
        this.mcastIp = null;
        this.deviceId = null;
        this.source = null;
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
     * Returns the source connect point of this key.
     *
     * @return the source connect point
     */
    public ConnectPoint source() {
        return source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof McastRoleStoreKey)) {
            return false;
        }
        final McastRoleStoreKey that = (McastRoleStoreKey) o;

        return Objects.equals(this.mcastIp, that.mcastIp) &&
                Objects.equals(this.deviceId, that.deviceId) &&
                Objects.equals(this.source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mcastIp, deviceId, source);
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("mcastIp", mcastIp)
                .add("deviceId", deviceId)
                .add("source", source)
                .toString();
    }
}
