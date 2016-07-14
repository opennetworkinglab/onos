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

import java.util.Objects;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;

/**
 * Host route dictionaries for the subnet.
 */
public final class DefaultHostRoute implements HostRoute {

    private final IpAddress nexthop;
    private final IpPrefix destination;

    /**
     *
     * Creates a DefaultHostRoute by using the next hop and the destination.
     *
     * @param nexthop of the DefaultHostRoute
     * @param destination of the DefaultHostRoute
     */
    public DefaultHostRoute(IpAddress nexthop, IpPrefix destination) {
        this.nexthop = nexthop;
        this.destination = destination;
    }

    @Override
    public IpAddress nexthop() {
        return nexthop;
    }

    @Override
    public IpPrefix destination() {
        return destination;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("nexthop", nexthop)
                .add("destination", destination).toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(nexthop, destination);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultHostRoute) {
            final DefaultHostRoute other = (DefaultHostRoute) obj;
            return Objects.equals(this.nexthop, other.nexthop)
                    && Objects.equals(this.destination, other.destination);
        }
        return false;
    }

}
