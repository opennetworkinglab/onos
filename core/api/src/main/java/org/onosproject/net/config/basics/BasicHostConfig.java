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
package org.onosproject.net.config.basics;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.onlab.packet.IpAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;

import java.util.HashSet;
import java.util.Set;

/**
 * Basic configuration for network end-station hosts.
 */
public final class BasicHostConfig extends BasicElementConfig<HostId> {

    private static final String IPS = "ips";
    private static final String LOCATIONS = "locations";
    private static final String DASH = "-";

    @Override
    public boolean isValid() {
        // Location and IP addresses can be absent, but if present must be valid.
        this.locations();
        this.ipAddresses();
        return hasOnlyFields(ALLOWED, NAME, LOC_TYPE, LATITUDE, LONGITUDE,
                GRID_Y, GRID_Y, UI_TYPE, RACK_ADDRESS, OWNER, IPS, LOCATIONS);
    }

    @Override
    public String name() {
        // NOTE:
        // We don't want to default to host ID if friendly name is not set;
        // (it isn't particularly friendly, e.g. "00:00:00:00:00:01/None").
        // We'd prefer to clear the annotation, but if we pass null, then the
        // value won't get set (see BasicElementOperator). So, instead we will
        // return a DASH to signify "use the default friendly name".
        return get(NAME, DASH);
    }

    /**
     * Returns the location of the host.
     *
     * @return location of the host or null if not set
     * @throws IllegalArgumentException if not specified with correct format
     */
    public Set<HostLocation> locations() {
        HashSet<HostLocation> locations = new HashSet<>();
        if (object.has(LOCATIONS)) {
            ArrayNode locationNodes = (ArrayNode) object.path(LOCATIONS);
            locationNodes.forEach(n -> {
                ConnectPoint cp = ConnectPoint.deviceConnectPoint((n.asText()));
                locations.add(new HostLocation(cp, 0));
            });
            return locations;
        }
        return null;
    }

    /**
     * Sets the location of the host.
     *
     * @param locations location of the host or null to unset
     * @return the config of the host
     */
    public BasicHostConfig setLocations(Set<HostLocation> locations) {
        return (BasicHostConfig) setOrClear(LOCATIONS, locations);
    }

    /**
     * Returns IP addresses of the host.
     *
     * @return IP addresses of the host or null if not set
     * @throws IllegalArgumentException if not specified with correct format
     */
    public Set<IpAddress> ipAddresses() {
        HashSet<IpAddress> ipAddresses = new HashSet<>();
        if (object.has(IPS)) {
            ArrayNode ipNodes = (ArrayNode) object.path(IPS);
            ipNodes.forEach(n -> ipAddresses.add(IpAddress.valueOf(n.asText())));
            return ipAddresses;
        }
        return null;
    }

    /**
     * Sets the IP addresses of the host.
     *
     * @param ipAddresses IP addresses of the host or null to unset
     * @return the config of the host
     */
    public BasicHostConfig setIps(Set<IpAddress> ipAddresses) {
        return (BasicHostConfig) setOrClear(IPS, ipAddresses);
    }
}
