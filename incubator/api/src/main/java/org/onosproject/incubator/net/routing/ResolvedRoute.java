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

package org.onosproject.incubator.net.routing;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents a route with the next hop MAC address resolved.
 */
public class ResolvedRoute {

    private final IpPrefix prefix;
    private final IpAddress nextHop;
    private final MacAddress nextHopMac;
    private final ConnectPoint location;

    /**
     * Creates a new resolved route.
     *
     * @param route input route
     * @param nextHopMac next hop MAC address
     * @param location connect point where the next hop connects to
     */
    public ResolvedRoute(Route route, MacAddress nextHopMac, ConnectPoint location) {
        this.prefix = route.prefix();
        this.nextHop = route.nextHop();
        this.nextHopMac = nextHopMac;
        this.location = location;
    }

    /**
     * Creates a new resolved route.
     *
     * @param prefix route prefix
     * @param nextHop route next hop IP address
     * @param nextHopMac next hop MAC address
     * @param location connect point where the next hop connects to
     */
    public ResolvedRoute(IpPrefix prefix, IpAddress nextHop, MacAddress nextHopMac,
                         ConnectPoint location) {
        this.prefix = prefix;
        this.nextHop = nextHop;
        this.nextHopMac = nextHopMac;
        this.location = location;
    }

    /**
     * Returns the IP prefix.
     *
     * @return IP prefix
     */
    public IpPrefix prefix() {
        return prefix;
    }

    /**
     * Returns the next hop IP address.
     *
     * @return IP address
     */
    public IpAddress nextHop() {
        return nextHop;
    }

    /**
     * Returns the next hop MAC address.
     *
     * @return MAC address
     */
    public MacAddress nextHopMac() {
        return nextHopMac;
    }

    /**
     * Returns the next hop location.
     *
     * @return connect point where the next hop attaches to
     */
    public ConnectPoint location() {
        return location;
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, nextHop, nextHopMac, location);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof ResolvedRoute)) {
            return false;
        }

        ResolvedRoute that = (ResolvedRoute) other;

        return Objects.equals(this.prefix, that.prefix) &&
                Objects.equals(this.nextHop, that.nextHop) &&
                Objects.equals(this.nextHopMac, that.nextHopMac) &&
                Objects.equals(this.location, that.location);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("prefix", prefix)
                .add("nextHop", nextHop)
                .add("nextHopMac", nextHopMac)
                .add("location", location)
                .toString();
    }
}
