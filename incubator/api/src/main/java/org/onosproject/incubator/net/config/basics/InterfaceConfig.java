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

package org.onosproject.incubator.net.config.basics;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.config.Config;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.host.InterfaceIpAddress;

import java.util.Set;

/**
 * Configuration for interfaces.
 */
public class InterfaceConfig extends Config<ConnectPoint> {
    public static final String INTERFACES = "interfaces";
    public static final String IPS = "ips";
    public static final String MAC = "mac";
    public static final String VLAN = "vlan";

    public static final String IP_MISSING_ERROR = "Must have at least one IP address";
    public static final String MAC_MISSING_ERROR = "Must have a MAC address for each interface";
    public static final String CONFIG_VALUE_ERROR = "Error parsing config value";

    /**
     * Retrieves all interfaces configured on this port.
     *
     * @return set of interfaces
     * @throws ConfigException if there is any error in the JSON config
     */
    public Set<Interface> getInterfaces() throws ConfigException {
        Set<Interface> interfaces = Sets.newHashSet();

        try {
            // TODO: rework this to take advantage of ArrayNode backing
            for (JsonNode intfNode : object.path(INTERFACES)) {
                Set<InterfaceIpAddress> ips = getIps(intfNode);
                if (ips.isEmpty()) {
                    throw new ConfigException(IP_MISSING_ERROR);
                }

                if (intfNode.path(MAC).isMissingNode()) {
                    throw new ConfigException(MAC_MISSING_ERROR);
                }

                MacAddress mac = MacAddress.valueOf(intfNode.path(MAC).asText());

                VlanId vlan = VlanId.NONE;
                if (!intfNode.path(VLAN).isMissingNode()) {
                    vlan = VlanId.vlanId(Short.valueOf(intfNode.path(VLAN).asText()));
                }

                interfaces.add(new Interface(subject, ips, mac, vlan));
            }
        } catch (IllegalArgumentException e) {
            throw new ConfigException(CONFIG_VALUE_ERROR, e);
        }

        return interfaces;
    }

    private Set<InterfaceIpAddress> getIps(JsonNode node) {
        Set<InterfaceIpAddress> ips = Sets.newHashSet();

        JsonNode ipsNode = node.get(IPS);
        ipsNode.forEach(jsonNode -> ips.add(InterfaceIpAddress.valueOf(jsonNode.asText())));

        return ips;
    }

}
