/*
 *Copyright 2014 Open Networking Laboratory
 *
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing, software
 *distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *See the License for the specific language governing permissions and
 *limitations under the License.
 */
package org.onosproject.app.vtnrsc;

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
    public enum Mode {
        DHCPV6_STATEFUL, DHCPV6_STATELESS, SLAAC
    }

    /**
     * Returns the ID of the subnet.
     *
     * @return id
     */
    SubnetId id();

    /**
     * Returns the name of the subnet.
     *
     * @return subnetName
     */
    String subnetName();

    /**
     * Returns the ID of the attached network.
     *
     * @return networkID
     */
    TenantNetworkId networkId();

    /**
     * Returns the The ID of the tenant who owns the network. Only
     * administrative users can specify a tenant ID other than their own. You
     * cannot change this value through authorization policies.
     *
     * @return tenantID
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
     * Returns the gateway IP address..
     *
     * @return gatewayIP
     */
    IpAddress gatewayIp();

    /**
     * Returns true if DHCP is enabled and return false if DHCP is disabled.
     *
     * @return dhcpEnabled
     */
    boolean dhcpEnabled();

    /**
     * Indicates whether this tenantNetwork is shared across all tenants. By
     * default,only administrative user can change this value.
     *
     * @return shared
     */
    boolean shared();

    /**
     * Returns an iterable collections of hostRoutes.
     *
     * @return hostRoutes collection
     */
    Iterable<HostRoute> hostRoutes();

    /**
     * Returns the ipV6AddressMode. A valid value is dhcpv6-stateful,
     * dhcpv6-stateless, or slaac.
     *
     * @return ipV6AddressMode
     */
    Mode ipV6AddressMode();

    /**
     * Returns the ipV6RaMode.A valid value is dhcpv6-stateful,
     * dhcpv6-stateless, or slaac.
     *
     * @return ipV6RaMode
     */
    Mode ipV6RaMode();

    /**
     * Returns an iterable collection of allocation_pools.
     *
     * @return allocationPools collection
     */

    Iterable<AllocationPool> allocationPools();
}
