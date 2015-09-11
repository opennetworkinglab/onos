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
package org.onosproject.vtnrsc;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;
import java.util.Set;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpAddress.Version;
import org.onlab.packet.IpPrefix;

/**
 * Default implementation of Subnet interface .
 */
public final class DefaultSubnet implements Subnet {
    private final SubnetId id;
    private final String subnetName;
    private final TenantNetworkId networkId;
    private final TenantId tenantId;
    private final Version ipVersion;
    private final IpPrefix cidr;
    private final IpAddress gatewayIp;
    private final boolean dhcpEnabled;
    private final boolean shared;
    private final Mode ipV6AddressMode;
    private final Mode ipV6RaMode;
    private final Set<HostRoute> hostRoutes;
    private final Set<AllocationPool> allocationPools;

    /**
     * Creates a subnet object.
     *
     * @param id subnet identifier
     * @param subnetName the name of subnet
     * @param networkId network identifier
     * @param tenantId tenant identifier
     * @param ipVersion Version of ipv4 or ipv6
     * @param cidr the cidr
     * @param gatewayIp gateway ip
     * @param dhcpEnabled dhcp enabled or not
     * @param shared indicates whether this network is shared across all
     *            tenants, By default, only administrative user can change this
     *            value
     * @param hostRoutes a collection of host routes
     * @param ipV6AddressMode ipV6AddressMode
     * @param ipV6RaMode ipV6RaMode
     * @param allocationPoolsIt a collection of allocationPools
     */
    public DefaultSubnet(SubnetId id, String subnetName,
                         TenantNetworkId networkId, TenantId tenantId,
                         Version ipVersion, IpPrefix cidr, IpAddress gatewayIp,
                         boolean dhcpEnabled, boolean shared,
                         Set<HostRoute> hostRoutes, Mode ipV6AddressMode,
                         Mode ipV6RaMode,
                         Set<AllocationPool> allocationPoolsIt) {
        this.id = id;
        this.subnetName = subnetName;
        this.networkId = networkId;
        this.tenantId = tenantId;
        this.ipVersion = ipVersion;
        this.cidr = cidr;
        this.gatewayIp = gatewayIp;
        this.dhcpEnabled = dhcpEnabled;
        this.shared = shared;
        this.ipV6AddressMode = ipV6AddressMode;
        this.ipV6RaMode = ipV6RaMode;
        this.hostRoutes = hostRoutes;
        this.allocationPools = allocationPoolsIt;
    }

    @Override
    public SubnetId id() {
        return id;
    }

    @Override
    public String subnetName() {
        return subnetName;
    }

    @Override
    public TenantNetworkId networkId() {
        return networkId;
    }

    @Override
    public TenantId tenantId() {
        return tenantId;
    }

    @Override
    public Version ipVersion() {
        return ipVersion;
    }

    @Override
    public IpPrefix cidr() {
        return cidr;
    }

    @Override
    public IpAddress gatewayIp() {
        return gatewayIp;
    }

    @Override
    public boolean dhcpEnabled() {
        return dhcpEnabled;
    }

    @Override
    public boolean shared() {
        return shared;
    }

    @Override
    public Iterable<HostRoute> hostRoutes() {
        return hostRoutes;
    }

    @Override
    public Mode ipV6AddressMode() {
        return ipV6AddressMode;
    }

    @Override
    public Mode ipV6RaMode() {
        return ipV6RaMode;
    }

    @Override
    public Iterable<AllocationPool> allocationPools() {
        return allocationPools;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, subnetName, ipVersion, cidr, gatewayIp,
                            dhcpEnabled, shared, tenantId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultSubnet) {
            final DefaultSubnet that = (DefaultSubnet) obj;
            return Objects.equals(this.id, that.id)
                    && Objects.equals(this.subnetName, that.subnetName)
                    && Objects.equals(this.ipVersion, that.ipVersion)
                    && Objects.equals(this.cidr, that.cidr)
                    && Objects.equals(this.shared, that.shared)
                    && Objects.equals(this.gatewayIp, that.gatewayIp)
                    && Objects.equals(this.dhcpEnabled, that.dhcpEnabled);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("id", id).add("subnetName", subnetName)
                .add("ipVersion", ipVersion).add("cidr", cidr)
                .add("shared", shared).add("gatewayIp", gatewayIp)
                .add("dhcpEnabled", dhcpEnabled).toString();
    }

}
