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
 */

package org.onosproject.dhcprelay.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.dhcprelay.config.DhcpServerConfig;

import java.util.Optional;

/**
 * class contains DHCP server information.
 */
public class DhcpServerInfo extends DhcpServerConfig {
    public enum Version {
        DHCP_V4,
        DHCP_V6
    }
    private MacAddress dhcpConnectMac;
    private VlanId dhcpConnectVlan;
    private Version version;

    /**
     * Creates DHCP server information from config.
     *
     * @param config DHCP server config
     * @param version DHCP version for the server
     */
    public DhcpServerInfo(DhcpServerConfig config, Version version) {
        this.relayAgentIps = Maps.newHashMap(config.getRelayAgentIps());
        this.connectPoint = config.getDhcpServerConnectPoint().orElse(null);
        this.version = version;

        switch (version) {
            case DHCP_V4:
                this.serverIp4Addr = config.getDhcpServerIp4().orElse(null);
                this.gatewayIp4Addr = config.getDhcpGatewayIp4().orElse(null);
                break;
            case DHCP_V6:
                this.serverIp6Addr = config.getDhcpServerIp6().orElse(null);
                this.gatewayIp6Addr = config.getDhcpGatewayIp6().orElse(null);
                break;
            default:
                break;
        }
    }

    /**
     * Sets DHCP server or gateway mac address.
     *
     * @param dhcpConnectMac the mac address
     */
    public void setDhcpConnectMac(MacAddress dhcpConnectMac) {
        this.dhcpConnectMac = dhcpConnectMac;
    }

    /**
     * Sets DHCP server or gateway vlan id.
     *
     * @param dhcpConnectVlan the vlan id
     */
    public void setDhcpConnectVlan(VlanId dhcpConnectVlan) {
        this.dhcpConnectVlan = dhcpConnectVlan;
    }

    /**
     * Gets DHCP server or gateway mac address.
     *
     * @return the mac address
     */
    public Optional<MacAddress> getDhcpConnectMac() {
        return Optional.ofNullable(dhcpConnectMac);
    }

    /**
     * Gets DHCP server or gateway vlan id.
     *
     * @return the vlan id.
     */
    public Optional<VlanId> getDhcpConnectVlan() {
        return Optional.ofNullable(dhcpConnectVlan);
    }

    /**
     * Get DHCP version of the DHCP server.
     *
     * @return the version; can be DHCP_V4 or DHCP_V6
     */
    public Version getVersion() {
        return version;
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper toStringHelper = MoreObjects.toStringHelper(this);
        toStringHelper
                .add("dhcpConnectMac", dhcpConnectMac)
                .add("dhcpConnectVlan", dhcpConnectVlan)
                .add("connectPoint", connectPoint)
                .add("version", version);
        switch (version) {
            case DHCP_V4:
                toStringHelper
                        .add("serverIp4Addr", serverIp4Addr)
                        .add("gatewayIp4Addr", gatewayIp4Addr);
                break;
            case DHCP_V6:
                toStringHelper
                        .add("serverIp6Addr", serverIp6Addr)
                        .add("gatewayIp6Addr", gatewayIp6Addr);
                break;
            default:
                break;
        }
        return toStringHelper.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DhcpServerInfo)) {
            return false;
        }
        DhcpServerInfo that = (DhcpServerInfo) o;
        return super.equals(o) &&
                Objects.equal(dhcpConnectMac, that.dhcpConnectMac) &&
                Objects.equal(dhcpConnectVlan, that.dhcpConnectVlan) &&
                version == that.version;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), dhcpConnectMac, dhcpConnectVlan, version);
    }
}
