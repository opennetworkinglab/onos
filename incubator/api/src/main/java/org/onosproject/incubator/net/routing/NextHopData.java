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

import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Stores next hop information.
 */
public class NextHopData {

    private final MacAddress mac;
    private final ConnectPoint location;

    /**
     * Creates a new instance.
     *
     * @param mac MAC address
     * @param location Connect point
     */
    public NextHopData(MacAddress mac, ConnectPoint location) {
        this.mac = mac;
        this.location = location;
    }

    /**
     * Returns the MAC address.
     *
     * @return MAC address
     */
    public MacAddress mac() {
        return mac;
    }

    /**
     * Returns the location.
     *
     * @return Connect point
     */
    public ConnectPoint location() {
        return location;
    }

    /**
     * Creates a new instance from a host.
     *
     * @param host Host information
     * @return NextHopData instance
     */
    public static NextHopData fromHost(Host host) {
        return new NextHopData(host.mac(), host.location());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mac, location);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof NextHopData)) {
            return false;
        }

        NextHopData that = (NextHopData) other;

        return Objects.equals(this.mac, that.mac) &&
                Objects.equals(this.location, that.location);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("mac", mac)
                .add("location", location)
                .toString();
    }
}
