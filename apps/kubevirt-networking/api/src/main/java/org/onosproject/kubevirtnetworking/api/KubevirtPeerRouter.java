/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

import java.util.Objects;

/**
 * External peer router class.
 */
public class KubevirtPeerRouter {

    private final IpAddress ipAddress;
    private final MacAddress macAddress;

    /**
     * A default constructor.
     *
     * @param ipAddress  IP address
     * @param macAddress MAC address
     */
    public KubevirtPeerRouter(IpAddress ipAddress, MacAddress macAddress) {
        this.ipAddress = ipAddress;
        this.macAddress = macAddress;
    }

    /**
     * Obtains the peer router IP address.
     * @return IP address
     */
    public IpAddress ipAddress() {
        return ipAddress;
    }

    /**
     * Obtains the peer router MAC address.
     *
     * @return MAC address
     */
    public MacAddress macAddress() {
        return macAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KubevirtPeerRouter that = (KubevirtPeerRouter) o;
        return ipAddress.equals(that.ipAddress) && macAddress.equals(that.macAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress, macAddress);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("ipAddress", ipAddress)
                .add("macAddress", macAddress)
                .toString();
    }
}
