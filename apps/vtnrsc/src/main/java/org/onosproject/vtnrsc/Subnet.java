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

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpAddress.Version;
import org.onlab.packet.IpPrefix;

/**
 * Representation of a subnet.
 */
public interface Subnet {

    /**
     * Coarse classification of the type of the ipV6Mode.
     */
    enum Mode {
        DHCPV6_STATEFUL, DHCPV6_STATELESS, SLAAC
    }

    /**
     * Returns the subnet identifier.
     *
     * @return identifier
     */
    SubnetId id();

    /**
     * Returns the name of the subnet.
     *
     * @return subnetName
     */
    String subnetName();

    /**
     * Returns the network identifier.
     *
     * @return the network identifier
     */
    TenantNetworkId networkId();

    /**
     * Returns tenant identifier.
     *
     * @return the tenant identifier
     */
    TenantId tenantId();

    /**
     * Returns the IP version, which is 4 or 6.
     *
     * @return ipVersion
     */
    Version ipVersion();

    /**
     * Returns the cidr.
     *
     * @return cidr
     */
    IpPrefix cidr();

    /**
     * Returns the gateway IP address.
     *
     * @return gatewayIp
     */
    IpAddress gatewayIp();

    /**
     * Returns true if DHCP is enabled and return false if DHCP is disabled.
     *
     * @return true or false
     */
    boolean dhcpEnabled();

    /**
     * Indicates whether this tenantNetwork is shared across all tenants. By
     * default, only administrative user can change this value.
     *
     * @return true or false
     */
    boolean shared();

    /**
     * Returns a collection of hostRoutes.
     *
     * @return a collection of hostRoutes
     */
    Iterable<HostRoute> hostRoutes();

    /**
     * Returns the ipV6AddressMode. A valid value is dhcpv6-stateful,
     * dhcpv6-stateless, or slaac.
     *
     * @return ipV6AddressMode whose value is dhcpv6-stateful, dhcpv6-stateless
     *         or slaac
     */
    Mode ipV6AddressMode();

    /**
     * Returns the ipV6RaMode.A valid value is dhcpv6-stateful,
     * dhcpv6-stateless, or slaac.
     *
     * @return ipV6RaMode whose value is dhcpv6-stateful, dhcpv6-stateless or
     *         slaac
     */
    Mode ipV6RaMode();

    /**
     * Returns a collection of allocation_pools.
     *
     * @return a collection of allocationPools
     */
    Iterable<AllocationPool> allocationPools();
}
