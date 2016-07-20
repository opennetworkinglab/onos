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
import org.onlab.packet.MacAddress;

/**
 * Immutable representation of a allowed address pair.
 */
public final class AllowedAddressPair {
    private final IpAddress ip;
    private final MacAddress mac;
    // Public construction is prohibited
    private AllowedAddressPair(IpAddress ip, MacAddress mac) {
        checkNotNull(ip, "IpAddress cannot be null");
        checkNotNull(mac, "MacAddress cannot be null");
        this.ip = ip;
        this.mac = mac;
    }
    /**
     * Returns the AllowedAddressPair ip address.
     *
     * @return ip address
     */
    public IpAddress ip() {
        return ip;
    }

    /**
     * Returns the AllowedAddressPair MAC address.
     *
     * @return MAC address
     */
    public MacAddress mac() {
        return mac;
    }


    /**
     * Creates a allowedAddressPair using the supplied ipAddress &amp;
     * macAddress.
     *
     * @param ip IP address
     * @param mac MAC address
     * @return AllowedAddressPair
     */
    public static AllowedAddressPair allowedAddressPair(IpAddress ip,
                                                        MacAddress mac) {
        return new AllowedAddressPair(ip, mac);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, mac);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AllowedAddressPair) {
            final AllowedAddressPair that = (AllowedAddressPair) obj;
            return Objects.equals(this.ip, that.ip)
                    && Objects.equals(this.mac, that.mac);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("ip", ip).add("mac", mac).toString();
    }

}
