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

import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Objects;

/**
 * Key of multicast next objective store.
 */
public class McastStoreKey {
    private final IpAddress mcastIp;
    private final DeviceId deviceId;

    /**
     * Constructs the key of multicast next objective store.
     *
     * @param mcastIp multicast group IP address
     * @param deviceId device ID
     */
    public McastStoreKey(IpAddress mcastIp, DeviceId deviceId) {
        checkNotNull(mcastIp, "mcastIp cannot be null");
        checkNotNull(deviceId, "deviceId cannot be null");
        checkArgument(mcastIp.isMulticast(), "mcastIp must be a multicast address");
        this.mcastIp = mcastIp;
        this.deviceId = deviceId;
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
                Objects.equals(this.deviceId, that.deviceId));
    }

    @Override
    public int hashCode() {
         return Objects.hash(mcastIp, deviceId);
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("mcastIp", mcastIp)
                .add("deviceId", deviceId)
                .toString();
    }
}
