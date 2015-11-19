/*
 * Copyright 2015 Open Networking Laboratory
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
import java.util.HashSet;
import java.util.Set;

/**
 * Basic configuration for network end-station hosts.
 */
public class BasicHostConfig extends BasicElementConfig<HostId> {
    private static final String IPS = "ips";
    private static final String LOCATION = "location";

    @Override
    public boolean isValid() {
        return hasOnlyFields(IPS, LOCATION) &&
                this.location() != null &&
                this.ipAddresses() != null;
    }

    /**
     * Gets location of the host.
     *
     * @return location of the host. Or null if not specified with correct format.
     */
    public ConnectPoint location() {
        String location = get(LOCATION, null);

        if (location != null) {
            try {
                return ConnectPoint.deviceConnectPoint(location);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Sets the location of the host.
     *
     * @param location location of the host.
     * @return the config of the host.
     */
    public BasicHostConfig setLocation(String location) {
        return (BasicHostConfig) setOrClear(LOCATION, location);
    }

    /**
     * Gets IP addresses of the host.
     *
     * @return IP addresses of the host. Or null if not specified with correct format.
     */
    public Set<IpAddress> ipAddresses() {
        HashSet<IpAddress> ipAddresses = new HashSet<>();
        if (object.has(IPS)) {
            ArrayNode ipNodes = (ArrayNode) object.path(IPS);
            try {
                ipNodes.forEach(ipNode -> {
                    ipAddresses.add(IpAddress.valueOf(ipNode.asText()));
                });
                return ipAddresses;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Sets the IP addresses of the host.
     *
     * @param ipAddresses IP addresses of the host.
     * @return the config of the host.
     */
    public BasicHostConfig setIps(Set<IpAddress> ipAddresses) {
        return (BasicHostConfig) setOrClear(IPS, ipAddresses);
    }
}
