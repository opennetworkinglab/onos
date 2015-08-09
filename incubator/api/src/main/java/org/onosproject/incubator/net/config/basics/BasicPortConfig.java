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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Sets;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.config.Config;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.host.InterfaceIpAddress;

import java.util.Iterator;
import java.util.Set;

/**
 * Basic configuration for a port on a device.
 */
public class BasicPortConfig extends Config<ConnectPoint> {
    public static final String IPS = "ips";
    public static final String MAC = "mac";
    public static final String VLAN = "vlan";

    /**
     * Returns the set of IP addresses assigned to the port.
     *
     * @return set ip IP addresses
     */
    public Set<InterfaceIpAddress> ips() {
        Set<InterfaceIpAddress> ips = Sets.newHashSet();

        JsonNode ipsNode = node.get(IPS);
        ipsNode.forEach(jsonNode -> ips.add(InterfaceIpAddress.valueOf(jsonNode.asText())));

        return ips;
    }

    /**
     * Adds an IP address to configuration of the port.
     *
     * @param ip ip address to add
     * @return this
     */
    public BasicPortConfig addIp(InterfaceIpAddress ip) {
        ArrayNode ipsNode = (ArrayNode) node.get(IPS);
        if (ipsNode == null) {
            ipsNode = node.putArray(IPS);
        }

        // Check if the value is already there
        if (ipsNode.findValue(ip.toString()) != null) {
            ipsNode.add(ip.toString());
        }

        return this;
    }

    /**
     * Removes an IP address from the configuration of the port.
     *
     * @param ip ip address to remove
     * @return this
     */
    public BasicPortConfig removeIp(InterfaceIpAddress ip) {
        ArrayNode ipsNode = (ArrayNode) node.get(IPS);

        if (ipsNode != null) {
            if (ipsNode.size() == 1) {
                node.remove(IPS);
            } else {
                Iterator<JsonNode> it = ipsNode.iterator();
                while (it.hasNext()) {
                    if (it.next().asText().equals(ip.toString())) {
                        it.remove();
                        break;
                    }
                }
            }
        }

        return this;
    }

    /**
     * Clear all IP addresses from the configuration.
     *
     * @return this
     */
    public BasicPortConfig clearIps() {
        node.remove(IPS);
        return this;
    }

    /**
     * Returns the MAC address configured on the port.
     *
     * @return MAC address
     */
    public MacAddress mac() {
        JsonNode macNode = node.get(MAC);
        if (macNode == null) {
            return null;
        }

        return MacAddress.valueOf(macNode.asText());
    }

    /**
     * Sets the MAC address configured on the port.
     *
     * @param mac MAC address
     * @return this
     */
    public BasicPortConfig mac(MacAddress mac) {
        String macString = (mac == null) ? null : mac.toString();
        return (BasicPortConfig) setOrClear(MAC, macString);
    }

    /**
     * Returns the VLAN configured on the port.
     *
     * @return VLAN ID
     */
    public VlanId vlan() {
        JsonNode macNode = node.get(VLAN);
        if (macNode == null) {
            return null;
        }

        return VlanId.vlanId(Short.parseShort(macNode.asText()));
    }

    /**
     * Sets the VLAN configured on the port.
     *
     * @param vlan VLAN ID
     * @return this
     */
    public BasicPortConfig vlan(VlanId vlan) {
        Integer vlanId = (vlan == null) ? null : Integer.valueOf(vlan.toShort());
        return (BasicPortConfig) setOrClear(VLAN, vlanId);
    }
}
