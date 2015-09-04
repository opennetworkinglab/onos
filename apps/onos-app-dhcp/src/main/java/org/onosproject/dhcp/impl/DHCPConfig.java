/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.dhcp.impl;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.basics.BasicElementConfig;

/**
 * DHCP Config class.
 */
public class DHCPConfig extends Config<ApplicationId> {

    public static final String MY_IP = "ip";
    public static final String MY_MAC = "mac";
    public static final String SUBNET_MASK = "subnet";
    public static final String BROADCAST_ADDRESS = "broadcast";
    public static final String ROUTER_ADDRESS = "router";
    public static final String DOMAIN_SERVER = "domain";
    public static final String TTL = "ttl";
    public static final String LEASE_TIME = "lease";
    public static final String RENEW_TIME = "renew";
    public static final String REBIND_TIME = "rebind";

    /**
     * Returns the dhcp server ip.
     *
     * @return ip address or null if not set
     */
    public String ip() {
        return get(MY_IP, null);
    }

    /**
     * Sets the dhcp server ip.
     *
     * @param ip new ip address; null to clear
     * @return self
     */
    public BasicElementConfig ip(String ip) {
        return (BasicElementConfig) setOrClear(MY_IP, ip);
    }

    /**
     * Returns the dhcp server mac.
     *
     * @return server mac or null if not set
     */
    public String mac() {
        return get(MY_MAC, null);
    }

    /**
     * Sets the dhcp server mac.
     *
     * @param mac new mac address; null to clear
     * @return self
     */
    public BasicElementConfig mac(String mac) {
        return (BasicElementConfig) setOrClear(MY_MAC, mac);
    }

    /**
     * Returns the subnet mask.
     *
     * @return subnet mask or null if not set
     */
    public String subnetMask() {
        return get(SUBNET_MASK, null);
    }

    /**
     * Sets the subnet mask.
     *
     * @param subnet new subnet mask; null to clear
     * @return self
     */
    public BasicElementConfig subnetMask(String subnet) {
        return (BasicElementConfig) setOrClear(SUBNET_MASK, subnet);
    }

    /**
     * Returns the broadcast address.
     *
     * @return broadcast address or null if not set
     */
    public String broadcastAddress() {
        return get(BROADCAST_ADDRESS, null);
    }

    /**
     * Sets the broadcast address.
     *
     * @param broadcast new broadcast address; null to clear
     * @return self
     */
    public BasicElementConfig broadcastAddress(String broadcast) {
        return (BasicElementConfig) setOrClear(BROADCAST_ADDRESS, broadcast);
    }

    /**
     * Returns the Time To Live for the reply packets.
     *
     * @return ttl or null if not set
     */
    public String ttl() {
        return get(TTL, null);
    }

    /**
     * Sets the Time To Live for the reply packets.
     *
     * @param ttl new ttl; null to clear
     * @return self
     */
    public BasicElementConfig ttl(String ttl) {
        return (BasicElementConfig) setOrClear(TTL, ttl);
    }

    /**
     * Returns the Lease Time offered by the DHCP Server.
     *
     * @return lease time or null if not set
     */
    public String leaseTime() {
        return get(LEASE_TIME, null);
    }

    /**
     * Sets the Lease Time offered by the DHCP Server.
     *
     * @param lease new lease time; null to clear
     * @return self
     */
    public BasicElementConfig leaseTime(String lease) {
        return (BasicElementConfig) setOrClear(LEASE_TIME, lease);
    }

    /**
     * Returns the Renew Time offered by the DHCP Server.
     *
     * @return renew time or null if not set
     */
    public String renewTime() {
        return get(RENEW_TIME, null);
    }

    /**
     * Sets the Renew Time offered by the DHCP Server.
     *
     * @param renew new renew time; null to clear
     * @return self
     */
    public BasicElementConfig renewTime(String renew) {
        return (BasicElementConfig) setOrClear(RENEW_TIME, renew);
    }

    /**
     * Returns the Rebind Time offered by the DHCP Server.
     *
     * @return rebind time or null if not set
     */
    public String rebindTime() {
        return get(REBIND_TIME, null);
    }

    /**
     * Sets the Rebind Time offered by the DHCP Server.
     *
     * @param rebind new rebind time; null to clear
     * @return self
     */
    public BasicElementConfig rebindTime(String rebind) {
        return (BasicElementConfig) setOrClear(REBIND_TIME, rebind);
    }

    /**
     * Returns the Router Address.
     *
     * @return router address or null if not set
     */
    public String routerAddress() {
        return get(ROUTER_ADDRESS, null);
    }

    /**
     * Sets the Router Address.
     *
     * @param router new router address; null to clear
     * @return self
     */
    public BasicElementConfig routerAddress(String router) {
        return (BasicElementConfig) setOrClear(ROUTER_ADDRESS, router);
    }

    /**
     * Returns the Domain Server Address.
     *
     * @return domain server address or null if not set
     */
    public String domainServer() {
        return get(DOMAIN_SERVER, null);
    }

    /**
     * Sets the Domain Server Address.
     *
     * @param domain new domain server address; null to clear
     * @return self
     */
    public BasicElementConfig domainServer(String domain) {
        return (BasicElementConfig) setOrClear(DOMAIN_SERVER, domain);
    }
}
