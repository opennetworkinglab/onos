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

package org.onosproject.routeservice;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents a route with the next hop MAC address resolved.
 */
// TODO Remove location from ResolvedRoute
public class ResolvedRoute {

    private final Route route;
    private final MacAddress nextHopMac;
    private final VlanId nextHopVlan;

    /**
     * Creates a new resolved route.
     *
     * @param route input route
     * @param nextHopMac next hop MAC address
     */
    public ResolvedRoute(Route route, MacAddress nextHopMac) {
        this(route, nextHopMac, VlanId.NONE);
    }

    /**
     * Creates a new resolved route.
     *
     * @param route input route
     * @param nextHopMac next hop MAC address
     * @param nextHopVlan next hop VLAN ID
     */
    public ResolvedRoute(Route route, MacAddress nextHopMac, VlanId nextHopVlan) {
        this.route = route;
        this.nextHopMac = nextHopMac;
        this.nextHopVlan = nextHopVlan;
    }

    /**
     * Returns the original route.
     *
     * @return route
     */
    public Route route() {
        return route;
    }

    /**
     * Returns the IP prefix.
     *
     * @return IP prefix
     */
    public IpPrefix prefix() {
        return route.prefix();
    }

    /**
     * Returns the next hop IP address.
     *
     * @return IP address
     */
    public IpAddress nextHop() {
        return route.nextHop();
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
     * Returns the next hop VLAN ID.
     *
     * @return VLAN ID
     */
    public VlanId nextHopVlan() {
        return nextHopVlan;
    }

    @Override
    public int hashCode() {
        return Objects.hash(route, nextHopMac, nextHopVlan);
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

        return Objects.equals(this.route, that.route) &&
                Objects.equals(this.nextHopMac, that.nextHopMac) &&
                Objects.equals(this.nextHopVlan, that.nextHopVlan);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("route", route)
                .add("nextHopMac", nextHopMac)
                .add("nextHopVlan", nextHopVlan)
                .toString();
    }
}
