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
import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * DHCP server configuration.
 */
public class DhcpServerConfig {
    private final Logger log = getLogger(getClass());

    private static final String DHCP_CONNECT_POINT = "dhcpServerConnectPoint";
    private static final String DHCP_SERVER_IP = "serverIps";
    private static final String DHCP_GATEWAY_IP = "gatewayIps";
    private static final String RELAY_AGENT_IP = "relayAgentIps";
    private static final String IPV4 = "ipv4";
    private static final String IPV6 = "ipv6";

    protected ConnectPoint connectPoint;
    protected Ip4Address serverIp4Addr;
    protected Ip4Address gatewayIp4Addr;
    protected Ip6Address serverIp6Addr;
    protected Ip6Address gatewayIp6Addr;
    protected Map<DeviceId, Pair<Ip4Address, Ip6Address>> relayAgentIps = Maps.newHashMap();

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
                  try {
                      serverIp4Addr = ip.getIp4Address();
                  } catch (IllegalArgumentException iae) {
                      log.warn("Invalid IPv4 address {} found in DHCP server config. Ignored.", ip.toString());
                  }
                }
                if (ip.isIp6() && serverIp6Addr == null) {
                  try {
                    serverIp6Addr = ip.getIp6Address();
                  } catch (IllegalArgumentException iae) {
                      log.warn("Invalid IPv6 address {} found in DHCP server config. Ignored.", ip.toString());
                  }
                }
            }
        });

        if (config.has(DHCP_GATEWAY_IP)) {
            ArrayNode gatewayIps = (ArrayNode) config.path(DHCP_GATEWAY_IP);
            gatewayIps.forEach(node -> {
                if (node.isTextual()) {
                    IpAddress ip = IpAddress.valueOf(node.asText());
                    if (ip.isIp4() && gatewayIp4Addr == null) {
                      try {
                          gatewayIp4Addr = ip.getIp4Address();
                      } catch (IllegalArgumentException iae) {
                          log.warn("Invalid IPv4 address {} found in DHCP gateway config. Ignored.", ip.toString());
                      }
                    }
                    if (ip.isIp6() && gatewayIp6Addr == null) {
                      try {
                          gatewayIp6Addr = ip.getIp6Address();
                      } catch (IllegalArgumentException iae) {
                          log.warn("Invalid IPv6 address {} found in DHCP gateway config. Ignored.", ip.toString());
                      }
                    }
                }
            });
        }
        if (config.has(RELAY_AGENT_IP)) {
            JsonNode relayAgentIpsNode = config.path(RELAY_AGENT_IP);
            relayAgentIpsNode.fields().forEachRemaining(e -> {
                DeviceId deviceId = DeviceId.deviceId(e.getKey());
                JsonNode ips = e.getValue();
                Ip4Address ipv4 = null;
                Ip6Address ipv6 = null;
                if (ips.has(IPV4)) {
                    String ipv4Str = ips.get(IPV4).asText();
                    try {
                        ipv4 = Ip4Address.valueOf(ipv4Str);
                    } catch (IllegalArgumentException iae) {
                        log.warn("Invalid IPv4 address {} found in DHCP relay config. Ignored.", ipv4Str);
                    }
                }
                if (ips.has(IPV6)) {
                    String ipv6Str = ips.get(IPV6).asText();
                    try {
                        ipv6 = Ip6Address.valueOf(ipv6Str);
                    } catch (IllegalArgumentException iae) {
                        log.warn("Invalid IPv6 address {} found in DHCP relay config. Ignored.", ipv6Str);
                    }
                }
                relayAgentIps.put(deviceId, Pair.of(ipv4, ipv6));
            });
        }

        checkNotNull(connectPoint, "Connect point of DHCP server can't be null");
        checkState(serverIp4Addr != null || serverIp6Addr != null,
                   "Should exist at least one server IP for DHCPv4 or DHCPv6");

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
     * Returns the optional IPv4 address for relay agent for given device,
     * if configured.
     * This option is used if we want to replace the giaddr field in DHCPv4
     * payload.
     *
     * @param deviceId the device
     * @return the giaddr; empty value if not set
     */
    public Optional<Ip4Address> getRelayAgentIp4(DeviceId deviceId) {
        Pair<Ip4Address, Ip6Address> relayAgentIp = relayAgentIps.get(deviceId);
        if (relayAgentIp == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(relayAgentIp.getLeft());
    }

    /**
     * Returns the optional IPv6 address for relay agent for given device,
     * if configured.
     * This option is used if we want to replace the link-address field in DHCPv6
     * payload.
     *
     * @param deviceId the device
     * @return the link-addr; empty value if not set
     */
    public Optional<Ip6Address> getRelayAgentIp6(DeviceId deviceId) {
        Pair<Ip4Address, Ip6Address> relayAgentIp = relayAgentIps.get(deviceId);
        if (relayAgentIp == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(relayAgentIp.getRight());
    }

    /**
     * Gets all relay agent ips and device mapping.
     *
     * @return the mapping
     */
    public Map<DeviceId, Pair<Ip4Address, Ip6Address>> getRelayAgentIps() {
        return relayAgentIps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DhcpServerConfig that = (DhcpServerConfig) o;
        return Objects.equal(connectPoint, that.connectPoint) &&
                Objects.equal(serverIp4Addr, that.serverIp4Addr) &&
                Objects.equal(gatewayIp4Addr, that.gatewayIp4Addr) &&
                Objects.equal(serverIp6Addr, that.serverIp6Addr) &&
                Objects.equal(gatewayIp6Addr, that.gatewayIp6Addr) &&
                Objects.equal(relayAgentIps, that.relayAgentIps);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(connectPoint, serverIp4Addr, gatewayIp4Addr,
                                serverIp6Addr, gatewayIp6Addr, relayAgentIps);
    }
}
