/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.routing.bgp;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a route entry for an IP prefix.
 */
public class RouteEntry {
    private final IpPrefix prefix;              // The IP prefix
    private final IpAddress nextHop;            // Next-hop IP address

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
     * Returns the IP version of the route.
     *
     * @return the IP version of the route
     */
    public IpAddress.Version version() {
        return nextHop.version();
    }

    /**
     * Tests whether the IP version of this address is IPv4.
     *
     * @return true if the IP version of this address is IPv4, otherwise false.
     */
    public boolean isIp4() {
        return nextHop.isIp4();
    }

    /**
     * Tests whether the IP version of this address is IPv6.
     *
     * @return true if the IP version of this address is IPv6, otherwise false.
     */
    public boolean isIp6() {
        return nextHop.isIp6();
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
