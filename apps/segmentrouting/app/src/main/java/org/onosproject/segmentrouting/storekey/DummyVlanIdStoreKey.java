/*
 * Copyright 2016-present Open Networking Foundation
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
import org.onosproject.net.ConnectPoint;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Key of VLAN ID to DummyVlanIdStore.
 */
public class DummyVlanIdStoreKey {
    private final ConnectPoint connectPoint;
    private final IpAddress ipAddress;

    /**
     * Construct the key of dummy vlan id key store.
     *
     * @param connectPoint connect point that this vlan id is associated
     * @param ipAddress IP address that this vlan id is associated
     */
    public DummyVlanIdStoreKey(ConnectPoint connectPoint, IpAddress ipAddress) {
        this.connectPoint = connectPoint;
        this.ipAddress = ipAddress;
    }

    /**
     * Returns the connect point in the key.
     *
     * @return connect point
     */
    public ConnectPoint connectPoint() {
        return connectPoint;
    }

    /**
     * Returns the IP address in the key.
     *
     * @return IP address
     */
    public IpAddress ipAddress() {
        return ipAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DummyVlanIdStoreKey)) {
            return false;
        }
        DummyVlanIdStoreKey that = (DummyVlanIdStoreKey) o;
        return (Objects.equals(this.connectPoint, that.connectPoint) &&
                Objects.equals(this.ipAddress, that.ipAddress));
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectPoint, ipAddress);
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("connectPoint", connectPoint)
                .add("ipAddress", ipAddress)
                .toString();
    }
}
