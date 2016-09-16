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
import org.onlab.packet.MacAddress;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Describes a routing next hop.
 */
public class NextHop {

    private final IpAddress ip;
    private final MacAddress mac;

    /**
     * Creates a new next hop.
     *
     * @param ip IP address
     * @param mac MAC address
     */
    public NextHop(IpAddress ip, MacAddress mac) {
        this.ip = ip;
        this.mac = mac;
    }

    /**
     * Returns the IP address of the next hop.
     *
     * @return IP address
     */
    public IpAddress ip() {
        return ip;
    }

    /**
     * Returns the MAC address of the next hop.
     *
     * @return MAC address
     */
    public MacAddress mac() {
        return mac;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, mac);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof NextHop)) {
            return false;
        }

        NextHop that = (NextHop) other;

        return Objects.equals(this.ip, that.ip) &&
                Objects.equals(this.mac, that.mac);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("ip", ip)
                .add("mac", mac)
                .toString();
    }
}
