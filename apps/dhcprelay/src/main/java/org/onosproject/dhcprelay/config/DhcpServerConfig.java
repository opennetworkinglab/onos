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
 *
 */

package org.onosproject.dhcprelay.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onosproject.net.ConnectPoint;

import java.util.Optional;

/**
 * DHCP server configuration.
 */
public class DhcpServerConfig {
    private static final String DHCP_CONNECT_POINT = "dhcpServerConnectPoint";
    private static final String DHCP_SERVER_IP = "serverIps";
    private static final String DHCP_GATEWAY_IP = "gatewayIps";
    private static final String RELAY_AGENT_IP = "relayAgentIps";

    private ConnectPoint connectPoint;
    private Ip4Address serverIp4Addr;
    private Ip4Address gatewayIp4Addr;
    private Ip4Address relayAgentIp4Addr;
    private Ip6Address serverIp6Addr;
    private Ip6Address gatewayIp6Addr;
    private Ip6Address relayAgentIp6Addr;

    protected DhcpServerConfig() {
        // empty config not allowed here
    }

    public DhcpServerConfig(JsonNode config) {
        if (!config.has(DHCP_CONNECT_POINT)) {
            // connect point doesn't exist
            throw new IllegalArgumentException("Missing " + DHCP_CONNECT_POINT);
        }
        connectPoint = ConnectPoint.deviceConnectPoint(config.path(DHCP_CONNECT_POINT).asText());

        if (!config.has(DHCP_SERVER_IP)) {
            // server ip doesn't exist
            throw new IllegalArgumentException("Missing " + DHCP_SERVER_IP);
        }
        ArrayNode serverIps = (ArrayNode) config.path(DHCP_SERVER_IP);
        serverIps.forEach(node -> {
            if (node.isTextual()) {
                IpAddress ip = IpAddress.valueOf(node.asText());
                if (ip.isIp4() && serverIp4Addr == null) {
                    serverIp4Addr = ip.getIp4Address();
                }
                if (ip.isIp6() && serverIp6Addr == null) {
                    serverIp6Addr = ip.getIp6Address();
                }
            }
        });

        if (config.has(DHCP_GATEWAY_IP)) {
            ArrayNode gatewayIps = (ArrayNode) config.path(DHCP_GATEWAY_IP);
            gatewayIps.forEach(node -> {
                if (node.isTextual()) {
                    IpAddress ip = IpAddress.valueOf(node.asText());
                    if (ip.isIp4() && gatewayIp4Addr == null) {
                        gatewayIp4Addr = ip.getIp4Address();
                    }
                    if (ip.isIp6() && gatewayIp6Addr == null) {
                        gatewayIp6Addr = ip.getIp6Address();
                    }
                }
            });
        }
        if (config.has(RELAY_AGENT_IP)) {
            ArrayNode relayAgentIps = (ArrayNode) config.path(RELAY_AGENT_IP);
            relayAgentIps.forEach(node -> {
                if (node.isTextual()) {
                    IpAddress ip = IpAddress.valueOf(node.asText());
                    if (ip.isIp4() && relayAgentIp4Addr == null) {
                        relayAgentIp4Addr = ip.getIp4Address();
                    }
                    if (ip.isIp6() && relayAgentIp6Addr == null) {
                        relayAgentIp6Addr = ip.getIp6Address();
                    }
                }
            });
        }
    }

    /**
     * Verify a json config is a valid DHCP server config.
     *
     * @param jsonConfig the json config
     * @return true if valid; false otherwise
     */
    public static boolean isValid(JsonNode jsonConfig) {
        return jsonConfig.has(DHCP_CONNECT_POINT) && jsonConfig.has(DHCP_SERVER_IP);
    }

    /**
     * Returns the dhcp server connect point.
     *
     * @return dhcp server connect point
     */
    public Optional<ConnectPoint> getDhcpServerConnectPoint() {
        return Optional.ofNullable(connectPoint);
    }

    /**
     * Returns the IPv4 address of DHCP server.
     *
     * @return IPv4 address of server; empty value if not set
     */
    public Optional<Ip4Address> getDhcpServerIp4() {
        return Optional.ofNullable(serverIp4Addr);
    }

    /**
     * Returns the optional IPv4 address of dhcp gateway, if configured.
     * This option is typically used if the dhcp server is not directly attached
     * to a switch; For example, the dhcp server may be reached via an external
     * gateway connected to the dhcpserverConnectPoint.
     *
     * @return IPv4 address of gateway; empty value if not set
     */
    public Optional<Ip4Address> getDhcpGatewayIp4() {
        return Optional.ofNullable(gatewayIp4Addr);
    }

    /**
     * Returns the IPv6 address of DHCP server.
     *
     * @return IPv6 address of server ; empty value if not set
     */
    public Optional<Ip6Address> getDhcpServerIp6() {
        return Optional.ofNullable(serverIp6Addr);
    }

    /**
     * Returns the optional IPv6 address of dhcp gateway, if configured.
     * This option is typically used if the dhcp server is not directly attached
     * to a switch; For example, the dhcp server may be reached via an external
     * gateway connected to the dhcpserverConnectPoint.
     *
     * @return IPv6 address of gateway; empty value if not set
     */
    public Optional<Ip6Address> getDhcpGatewayIp6() {
        return Optional.ofNullable(gatewayIp6Addr);
    }

    /**
     * Returns the optional IPv4 address for relay agent, if configured.
     * This option is used if we want to replace the giaddr field in DHCPv4
     * payload.
     *
     * @return the giaddr; empty value if not set
     */
    public Optional<Ip4Address> getRelayAgentIp4() {
        return Optional.ofNullable(relayAgentIp4Addr);
    }

    /**
     * Returns the optional IPv6 address for relay agent, if configured.
     * This option is used if we want to replace the link-address field in DHCPv6
     * payload.
     *
     * @return the giaddr; empty value if not set
     */
    public Optional<Ip6Address> getRelayAgentIp6() {
        return Optional.ofNullable(relayAgentIp6Addr);
    }
}
