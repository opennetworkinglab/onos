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
package org.onosproject.vtnrsc;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onlab.packet.IpAddress;

/**
 * Immutable representation of a IP address for the port, Include the IP address
 * and subnet identity.
 */
public final class FixedIp {
    private final SubnetId subnetId;
    private final IpAddress ip;
    // Public construction is prohibited
    private FixedIp(SubnetId subnetId, IpAddress ip) {
        checkNotNull(subnetId, "SubnetId cannot be null");
        checkNotNull(ip, "IpAddress cannot be null");
        this.subnetId = subnetId;
        this.ip = ip;
    }

    /**
     * Returns the FixedIp subnet identifier.
     *
     * @return subnet identifier
     */
    public SubnetId subnetId() {
        return subnetId;
    }

    /**
     * Returns the FixedIp IP address.
     *
     * @return IP address
     */
    public IpAddress ip() {
        return ip;
    }

    /**
     * Creates a fixed ip using the supplied fixedIp.
     *
     * @param subnetId subnet identity
     * @param ip IP address
     * @return FixedIp
     */
    public static FixedIp fixedIp(SubnetId subnetId, IpAddress ip) {
        return new FixedIp(subnetId, ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subnetId, ip);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FixedIp) {
            final FixedIp that = (FixedIp) obj;
            return Objects.equals(this.subnetId, that.subnetId)
                    && Objects.equals(this.ip, that.ip);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("subnetId", subnetId).add("ip", ip)
                .toString();
    }

}
