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

package org.onosproject.segmentrouting.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Configuration object for Segment Routing Application.
 */
public class SegmentRoutingDeviceConfig extends Config<DeviceId> {
    private static final String NAME = "name";
    private static final String IP4 = "ipv4Loopback";
    private static final String IP6 = "ipv6Loopback";
    private static final String MAC = "routerMac";
    private static final String IP4_SID = "ipv4NodeSid";
    private static final String IP6_SID = "ipv6NodeSid";
    private static final String EDGE = "isEdgeRouter";
    /**
     * Adjancency SIDs config.
     *
     * @deprecated in Loon (1.11). We are not using and do not plan to use it.
     */
    @Deprecated
    private static final String ADJSIDS = "adjacencySids";

    /**
     * Adjancency SID config.
     *
     * @deprecated in Loon (1.11). We are not using and do not plan to use it.
     */
    @Deprecated
    private static final String ADJSID = "adjSid";

    /**
     * Adjancency port config.
     *
     * @deprecated in Loon (1.11). We are not using and do not plan to use it.
     */
    @Deprecated
    private static final String PORTS = "ports";

    private static final String PAIR_DEVICE_ID = "pairDeviceId";
    private static final String PAIR_LOCAL_PORT = "pairLocalPort";

    @Override
    public boolean isValid() {
        return hasOnlyFields(NAME, IP4, IP6, MAC, IP4_SID, IP6_SID, EDGE, ADJSIDS, ADJSID, PORTS,
                             PAIR_DEVICE_ID, PAIR_LOCAL_PORT) &&
                name() != null &&
                routerIpv4() != null && (!hasField(IP6) || routerIpv6() != null) &&
                routerMac() != null &&
                nodeSidIPv4() != -1 && (!hasField(IP6_SID) || nodeSidIPv6() != -1) &&
                isEdgeRouter() != null &&
                adjacencySids() != null &&
                // pairDeviceId and pairLocalPort must be both configured or both omitted
                !(hasField(PAIR_DEVICE_ID) ^ hasField(PAIR_LOCAL_PORT)) &&
                (!hasField(PAIR_DEVICE_ID) || pairDeviceId() != null) &&
                (!hasField(PAIR_LOCAL_PORT) || pairLocalPort() != null);
    }

    /**
     * Gets the name of the router.
     *
     * @return Optional name of the router. May be empty if not configured.
     */
    public Optional<String> name() {
        String name = get(NAME, null);
        return name != null ? Optional.of(name) : Optional.empty();
    }

    /**
     * Sets the name of the router.
     *
     * @param name name of the router.
     * @return the config of the router.
     */
    public SegmentRoutingDeviceConfig setName(String name) {
        return (SegmentRoutingDeviceConfig) setOrClear(NAME, name);
    }

    /**
     * Gets the IPv4 address of the router.
     *
     * @return IP address of the router. Or null if not configured.
     */
    public Ip4Address routerIpv4() {
        String ip = get(IP4, null);
        return ip != null ? Ip4Address.valueOf(ip) : null;
    }

    /**
     * Gets the IPv6 address of the router.
     *
     * @return IP address of the router. Or null if not configured.
     */
    public Ip6Address routerIpv6() {
        String ip = get(IP6, null);
        return ip != null ? Ip6Address.valueOf(ip) : null;
    }

    /**
     * Sets the IPv4 address of the router.
     *
     * @param ip IPv4 address of the router.
     * @return the config of the router.
     */
    public SegmentRoutingDeviceConfig setRouterIpv4(String ip) {
        return (SegmentRoutingDeviceConfig) setOrClear(IP4, ip);
    }

    /**
     * Sets the IPv6 address of the router.
     *
     * @param ip IPv6 address of the router.
     * @return the config of the router.
     */
    public SegmentRoutingDeviceConfig setRouterIpv6(String ip) {
        return (SegmentRoutingDeviceConfig) setOrClear(IP6, ip);
    }

    /**
     * Gets the MAC address of the router.
     *
     * @return MAC address of the router. Or null if not configured.
     */
    public MacAddress routerMac() {
        String mac = get(MAC, null);
        return mac != null ? MacAddress.valueOf(mac) : null;
    }

    /**
     * Sets the MAC address of the router.
     *
     * @param mac MAC address of the router.
     * @return the config of the router.
     */
    public SegmentRoutingDeviceConfig setRouterMac(String mac) {
        return (SegmentRoutingDeviceConfig) setOrClear(MAC, mac);
    }

    /**
     * Gets the IPv4 node SID of the router.
     *
     * @return node SID of the router. Or -1 if not configured.
     */
    public int nodeSidIPv4() {
        return get(IP4_SID, -1);
    }

    /**
     * Gets the IPv6 node SID of the router.
     *
     * @return node SID of the router. Or -1 if not configured.
     */
    public int nodeSidIPv6() {
        return get(IP6_SID, -1);
    }

    /**
     * Sets the node IPv4 node SID of the router.
     *
     * @param sid node SID of the router.
     * @return the config of the router.
     */
    public SegmentRoutingDeviceConfig setNodeSidIPv4(int sid) {
        return (SegmentRoutingDeviceConfig) setOrClear(IP4_SID, sid);
    }

    /**
     * Sets the node IPv6 node SID of the router.
     *
     * @param sid node SID of the router.
     * @return the config of the router.
     */
    public SegmentRoutingDeviceConfig setNodeSidIPv6(int sid) {
        return (SegmentRoutingDeviceConfig) setOrClear(IP6_SID, sid);
    }

    /**
     * Checks if the router is an edge router.
     *
     * @return true if the router is an edge router.
     *         false if the router is not an edge router.
     *         null if the value is not configured.
     */
    public Boolean isEdgeRouter() {
        String isEdgeRouter = get(EDGE, null);
        return isEdgeRouter != null ?
                Boolean.valueOf(isEdgeRouter) :
                null;
    }

    /**
     * Specifies if the router is an edge router.
     *
     * @param isEdgeRouter true if the router is an edge router.
     * @return the config of the router.
     */
    public SegmentRoutingDeviceConfig setIsEdgeRouter(boolean isEdgeRouter) {
        return (SegmentRoutingDeviceConfig) setOrClear(EDGE, isEdgeRouter);
    }

    /**
     * Gets the adjacency SIDs of the router.
     *
     * @return adjacency SIDs of the router. Or null if not configured.
     */
    public Map<Integer, Set<Integer>> adjacencySids() {
        if (!object.has(ADJSIDS)) {
            return null;
        }

        Map<Integer, Set<Integer>> adjacencySids = new HashMap<>();
        ArrayNode adjacencySidsNode = (ArrayNode) object.path(ADJSIDS);
        for (JsonNode adjacencySidNode : adjacencySidsNode) {
            int asid = adjacencySidNode.path(ADJSID).asInt(-1);
            if (asid == -1) {
                return null;
            }

            HashSet<Integer> ports = new HashSet<>();
            ArrayNode portsNode = (ArrayNode) adjacencySidNode.path(PORTS);
            for (JsonNode portNode : portsNode) {
                int port = portNode.asInt(-1);
                if (port == -1) {
                    return null;
                }
                ports.add(port);
            }
            adjacencySids.put(asid, ports);
        }

        return ImmutableMap.copyOf(adjacencySids);
    }

    /**
     * Sets the adjacency SIDs of the router.
     *
     * @param adjacencySids adjacency SIDs of the router.
     * @return the config of the router.
     */
    public SegmentRoutingDeviceConfig setAdjacencySids(Map<Integer, Set<Integer>> adjacencySids) {
        if (adjacencySids == null) {
            object.remove(ADJSIDS);
        } else {
            ArrayNode adjacencySidsNode = mapper.createArrayNode();

            adjacencySids.forEach((sid, ports) -> {
                ObjectNode adjacencySidNode = mapper.createObjectNode();

                adjacencySidNode.put(ADJSID, sid);

                ArrayNode portsNode = mapper.createArrayNode();
                ports.forEach(port -> {
                    portsNode.add(port.toString());
                });
                adjacencySidNode.set(PORTS, portsNode);

                adjacencySidsNode.add(adjacencySidNode);
            });

            object.set(ADJSIDS, adjacencySidsNode);
        }

        return this;
    }

    /**
     * Gets the pair device id.
     *
     * @return pair device id; Or null if not configured.
     */
    public DeviceId pairDeviceId() {
        String pairDeviceId = get(PAIR_DEVICE_ID, null);
        return pairDeviceId != null ? DeviceId.deviceId(pairDeviceId) : null;
    }

    /**
     * Sets the pair device id.
     *
     * @param deviceId pair device id
     * @return this configuration
     */
    public SegmentRoutingDeviceConfig setPairDeviceId(DeviceId deviceId) {
        return (SegmentRoutingDeviceConfig) setOrClear(PAIR_DEVICE_ID, deviceId.toString());
    }

    /**
     * Gets the pair local port.
     *
     * @return pair local port; Or null if not configured.
     */
    public PortNumber pairLocalPort() {
        long pairLocalPort = get(PAIR_LOCAL_PORT, -1L);
        return pairLocalPort != -1L ? PortNumber.portNumber(pairLocalPort) : null;
    }

    /**
     * Sets the pair local port.
     *
     * @param portNumber pair local port
     * @return this configuration
     */
    public SegmentRoutingDeviceConfig setPairLocalPort(PortNumber portNumber) {
        return (SegmentRoutingDeviceConfig) setOrClear(PAIR_LOCAL_PORT, portNumber.toLong());
    }
}
