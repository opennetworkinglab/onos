/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.routing;

import java.util.Objects;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

import com.google.common.base.MoreObjects;

/**
 * Represents a next hop for routing, whose MAC address has already been resolved.
 */
public class NextHop {

    private final IpAddress ip;
    private final MacAddress mac;
    private final NextHopGroupKey group;

    /**
     * Creates a new next hop.
     *
     * @param ip next hop's IP address
     * @param mac next hop's MAC address
     * @param group next hop's group
     */
    public NextHop(IpAddress ip, MacAddress mac, NextHopGroupKey group) {
        this.ip = ip;
        this.mac = mac;
        this.group = group;
    }

    /**
     * Returns the next hop's IP address.
     *
     * @return next hop's IP address
     */
    public IpAddress ip() {
        return ip;
    }

    /**
     * Returns the next hop's MAC address.
     *
     * @return next hop's MAC address
     */
    public MacAddress mac() {
        return mac;
    }

    /**
     * Returns the next hop group.
     *
     * @return group
     */
    public NextHopGroupKey group() {
        return group;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NextHop)) {
            return false;
        }

        NextHop that = (NextHop) o;

        return Objects.equals(this.ip, that.ip) &&
                Objects.equals(this.mac, that.mac) &&
                Objects.equals(this.group, that.group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, mac, group);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("ip", ip)
                .add("mac", mac)
                .add("group", group)
                .toString();
    }
}
