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
import org.onlab.packet.IpPrefix;

import java.util.Objects;

/**
 * HostRoute class.
 */
public class KubevirtHostRoute {
    private final IpPrefix destination;
    private final IpAddress nexthop;

    /**
     * Default constructor.
     *
     * @param destination   destination CIDR
     * @param nexthop       nexthop IP address
     */
    public KubevirtHostRoute(IpPrefix destination, IpAddress nexthop) {
        this.destination = destination;
        this.nexthop = nexthop;
    }

    /**
     * Returns the destination CIDR.
     *
     * @return destination CIDR
     */
    public IpPrefix destination() {
        return destination;
    }

    /**
     * Returns the nexthop IP address.
     *
     * @return nexthop IP address
     */
    public IpAddress nexthop() {
        return nexthop;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KubevirtHostRoute kubevirtHostRoute = (KubevirtHostRoute) o;
        return destination.equals(kubevirtHostRoute.destination) &&
                nexthop.equals(kubevirtHostRoute.nexthop);
    }

    @Override
    public int hashCode() {
        return Objects.hash(destination, nexthop);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("destination", destination)
                .add("nexthop", nexthop)
                .toString();
    }
}
