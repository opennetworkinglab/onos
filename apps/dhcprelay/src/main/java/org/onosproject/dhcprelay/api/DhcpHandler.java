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
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.packet.PacketContext;

import java.util.Collection;
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
    Optional<IpAddress> getDhcpServerIp();

    /**
     * Gets DHCP gateway IP.
     *
     * @return IP address of DHCP gateway; empty value if not exist
     * @deprecated 1.12 get the address from config service
     */
    @Deprecated
    Optional<IpAddress> getDhcpGatewayIp();

    /**
     * Gets DHCP connect Mac address.
     *
     * @return the connect Mac address of server or gateway
     * @deprecated 1.12 get host mac from host service
     */
    @Deprecated
    Optional<MacAddress> getDhcpConnectMac();

    /**
     * Sets DHCP gateway IP.
     *
     * @param dhcpGatewayIp the DHCP gateway IP
     * @deprecated 1.12 use setDefaultDhcpServerConfigs or setindirectDhcpServerConfigs
     */
    @Deprecated
    void setDhcpGatewayIp(IpAddress dhcpGatewayIp);

    /**
     * Sets DHCP connect vlan.
     *
     * @param dhcpConnectVlan the DHCP connect vlan
     * @deprecated 1.12 use setDefaultDhcpServerConfigs or setindirectDhcpServerConfigs
     */
    @Deprecated
    void setDhcpConnectVlan(VlanId dhcpConnectVlan);

    /**
     * Sets DHCP connect Mac address.
     *
     * @param dhcpConnectMac the connect Mac address
     * @deprecated 1.12 use setDefaultDhcpServerConfigs or setindirectDhcpServerConfigs
     */
    @Deprecated
    void setDhcpConnectMac(MacAddress dhcpConnectMac);

    /**
     * Sets DHCP server connect point.
     *
     * @param dhcpServerConnectPoint the server connect point
     * @deprecated 1.12 use setDefaultDhcpServerConfigs or setindirectDhcpServerConfigs
     */
    @Deprecated
    void setDhcpServerConnectPoint(ConnectPoint dhcpServerConnectPoint);

    /**
     * Sets DHCP server IP.
     *
     * @param dhcpServerIp the DHCP server IP
     * @deprecated 1.12 use setDefaultDhcpServerConfigs or setindirectDhcpServerConfigs
     */
    @Deprecated
    void setDhcpServerIp(IpAddress dhcpServerIp);

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
}
