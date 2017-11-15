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

package org.onosproject.dhcprelay.api;

import org.onlab.packet.BasePacket;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.dhcprelay.config.DhcpServerConfig;
import org.onosproject.dhcprelay.config.IgnoreDhcpConfig;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.packet.PacketContext;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * DHCP relay handler.
 */
public interface DhcpHandler {
    /**
     * Process the DHCP packet before sending to server or client.
     *
     * @param context the packet context
     * @param dhcpPayload the DHCP payload
     */
    void processDhcpPacket(PacketContext context, BasePacket dhcpPayload);

    /**
     * Gets DHCP server IP.
     *
     * @return IP address of DHCP server; empty value if not exist
     * @deprecated 1.12 get the address from config service
     */
    @Deprecated
    default Optional<IpAddress> getDhcpServerIp() {
        throw new UnsupportedOperationException("Method deprecated");
    }

    /**
     * Gets DHCP gateway IP.
     *
     * @return IP address of DHCP gateway; empty value if not exist
     * @deprecated 1.12 get the address from config service
     */
    @Deprecated
    default Optional<IpAddress> getDhcpGatewayIp() {
        throw new UnsupportedOperationException("Method deprecated");
    }

    /**
     * Gets DHCP connect Mac address.
     *
     * @return the connect Mac address of server or gateway
     * @deprecated 1.12 get host mac from host service
     */
    @Deprecated
    default Optional<MacAddress> getDhcpConnectMac() {
        throw new UnsupportedOperationException("Method deprecated");
    }

    /**
     * Sets DHCP gateway IP.
     *
     * @param dhcpGatewayIp the DHCP gateway IP
     * @deprecated 1.12 use setDefaultDhcpServerConfigs or setindirectDhcpServerConfigs
     */
    @Deprecated
    default void setDhcpGatewayIp(IpAddress dhcpGatewayIp) {
        throw new UnsupportedOperationException("Method deprecated");
    }

    /**
     * Sets DHCP connect vlan.
     *
     * @param dhcpConnectVlan the DHCP connect vlan
     * @deprecated 1.12 use setDefaultDhcpServerConfigs or setindirectDhcpServerConfigs
     */
    @Deprecated
    default void setDhcpConnectVlan(VlanId dhcpConnectVlan) {
        throw new UnsupportedOperationException("Method deprecated");
    }

    /**
     * Sets DHCP connect Mac address.
     *
     * @param dhcpConnectMac the connect Mac address
     * @deprecated 1.12 use setDefaultDhcpServerConfigs or setindirectDhcpServerConfigs
     */
    @Deprecated
    default void setDhcpConnectMac(MacAddress dhcpConnectMac) {
        throw new UnsupportedOperationException("Method deprecated");
    }

    /**
     * Sets DHCP server connect point.
     *
     * @param dhcpServerConnectPoint the server connect point
     * @deprecated 1.12 use setDefaultDhcpServerConfigs or setindirectDhcpServerConfigs
     */
    @Deprecated
    default void setDhcpServerConnectPoint(ConnectPoint dhcpServerConnectPoint) {
        throw new UnsupportedOperationException("Method deprecated");
    }

    /**
     * Sets DHCP server IP.
     *
     * @param dhcpServerIp the DHCP server IP
     * @deprecated 1.12 use setDefaultDhcpServerConfigs or setindirectDhcpServerConfigs
     */
    @Deprecated
    default void setDhcpServerIp(IpAddress dhcpServerIp) {
        throw new UnsupportedOperationException("Method deprecated");
    }

    /**
     * Gets list of default DHCP server information.
     *
     * @return list of default DHCP server information
     */
    default List<DhcpServerInfo> getDefaultDhcpServerInfoList() {
        return Collections.emptyList();
    }

    /**
     * Gets list of indirect DHCP server information.
     *
     * @return list of indirect DHCP server information
     */
    default List<DhcpServerInfo> getIndirectDhcpServerInfoList() {
        return Collections.emptyList();
    }

    /**
     * Sets DHCP server config for default case.
     *
     * @param configs the config
     */
    void setDefaultDhcpServerConfigs(Collection<DhcpServerConfig> configs);

    /**
     * Sets DHCP server config for indirect case.
     *
     * @param configs the config
     */
    void setIndirectDhcpServerConfigs(Collection<DhcpServerConfig> configs);

    /**
     * Push IgnoreDhcpConfig to the handler.
     *
     * @param config the config
     */
    void updateIgnoreVlanConfig(IgnoreDhcpConfig config);

    /**
     * Remove internal state for IgnoreDhcp.
     *
     * @param config the config
     */
    void removeIgnoreVlanState(IgnoreDhcpConfig config);

    /**
     * Hander for Dhcp expiration poll timer.
     *
     */
    default void timeTick() { }

    /**
     * Update Dhcp expiration poll timer value.
     *
     * @param val the timer interval value
     */
    default void setDhcp6PollInterval(int val) { }

    /**
     * Sets DHCP FPM Enable state.
     *
     * @param dhcpFpmFlag flag indicating dhcpFpmEnable state
     */
    default void setDhcpFpmEnabled(Boolean dhcpFpmFlag) { }
}
