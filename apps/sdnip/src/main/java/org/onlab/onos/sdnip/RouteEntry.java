/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.onos.sdnip;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;

import com.google.common.base.MoreObjects;

/**
 * Represents a route entry for an IP prefix.
 */
public class RouteEntry {
    private final IpPrefix prefix;             // The IP prefix
    private final IpAddress nextHop;           // Next-hop IP address

    /**
     * Class constructor.
     *
     * @param prefix the IP prefix of the route
     * @param nextHop the next hop IP address for the route
     */
    public RouteEntry(IpPrefix prefix, IpAddress nextHop) {
        this.prefix = checkNotNull(prefix);
        this.nextHop = checkNotNull(nextHop);
    }

    /**
     * Returns the IP prefix of the route.
     *
     * @return the IP prefix of the route
     */
    public IpPrefix prefix() {
        return prefix;
    }

    /**
     * Returns the next hop IP address for the route.
     *
     * @return the next hop IP address for the route
     */
    public IpAddress nextHop() {
        return nextHop;
    }

    /**
     * Creates the binary string representation of an IPv4 prefix.
     * The string length is equal to the prefix length.
     *
     * @param ip4Prefix the IPv4 prefix to use
     * @return the binary string representation
     */
    static String createBinaryString(IpPrefix ip4Prefix) {
        if (ip4Prefix.prefixLength() == 0) {
            return "";
        }

        StringBuilder result = new StringBuilder(ip4Prefix.prefixLength());
        long value = ip4Prefix.toInt();
        for (int i = 0; i < ip4Prefix.prefixLength(); i++) {
            long mask = 1 << (IpAddress.MAX_INET_MASK - 1 - i);
            result.append(((value & mask) == 0) ? "0" : "1");
        }
        return result.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        //
        // NOTE: Subclasses are considered as change of identity, hence
        // equals() will return false if the class type doesn't match.
        //
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        RouteEntry otherRoute = (RouteEntry) other;
        return Objects.equals(this.prefix, otherRoute.prefix) &&
            Objects.equals(this.nextHop, otherRoute.nextHop);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, nextHop);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("prefix", prefix)
            .add("nextHop", nextHop)
            .toString();
    }
}
