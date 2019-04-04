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
package org.onosproject.ofoverlay.impl.util;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Representation of a network address, which consists of IP address and prefix.
 */
public final class NetworkAddress {
    private final IpAddress ip;
    private final IpPrefix prefix;

    /**
     * Constructor for a given IP address and prefix.
     *
     * @param ip ip address
     * @param prefix ip prefix
     */
    private NetworkAddress(IpAddress ip, IpPrefix prefix) {
        this.ip = ip;
        this.prefix = prefix;
    }

    /**
     * Converts a CIDR notation string into a network address.
     *
     * @param cidr cidr
     * @return network address
     * @throws IllegalArgumentException if the cidr is not valid
     */
    public static NetworkAddress valueOf(String cidr) {
        checkArgument(cidr.contains("/"));

        IpAddress ipAddress = IpAddress.valueOf(cidr.split("/")[0]);
        IpPrefix ipPrefix = IpPrefix.valueOf(cidr);

        return new NetworkAddress(ipAddress, ipPrefix);
    }

    /**
     * Returns the IP address value of the network address.
     *
     * @return ip address
     */
    public IpAddress ip() {
        return this.ip;
    }

    /**
     * Returns the IP prefix value of the network address.
     *
     * @return ip prefix
     */
    public IpPrefix prefix() {
        return this.prefix;
    }

    /**
     * Converts a network address to a CIDR notation.
     *
     * @return cidr notation string
     */
    public String cidr() {
        return ip.toString() + "/" + prefix.prefixLength();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof NetworkAddress) {
            NetworkAddress that = (NetworkAddress) obj;
            return Objects.equals(ip, that.ip) && Objects.equals(prefix, that.prefix);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, prefix);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("IpAddress", ip)
                .add("IpPrefix", prefix)
                .toString();
    }
}