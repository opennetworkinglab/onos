/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.routing.bgp;

import org.onlab.packet.Ip4Address;

import java.net.SocketAddress;

/**
 * Class for keeping information about a BGP session.
 *
 * There are two instances per each BGP peer session: one to keep the local
 * information about the BGP session, and another to keep information about
 * the remote BGP peer.
 */
public class BgpSessionInfo {
    private SocketAddress address;              // IP addr/port
    private Ip4Address ip4Address;              // IPv4 address
    private int bgpVersion;                     // 1 octet
    private long asNumber;                      // AS number: 2 octets
    private long as4Number;                     // AS4 number: 4 octets
    private long holdtime;                      // 2 octets
    private Ip4Address bgpId;                   // 4 octets -> IPv4 address
    private boolean mpExtensions;               // Multiprotocol Extensions
                                                // enabled: RFC 4760
    private boolean ipv4Unicast;                // IPv4/UNICAST AFI/SAFI
    private boolean ipv4Multicast;              // IPv4/MULTICAST AFI/SAFI
    private boolean ipv6Unicast;                // IPv6/UNICAST AFI/SAFI
    private boolean ipv6Multicast;              // IPv6/MULTICAST AFI/SAFI
    private boolean as4OctetCapability;         // AS 4 octet path capability

    /**
     * Gets the BGP session address: local or remote.
     *
     * @return the BGP session address
     */
    public SocketAddress address() {
        return this.address;
    }

    /**
     * Sets the BGP session address: local or remote.
     *
     * @param address the BGP session address to set
     */
    public void setAddress(SocketAddress address) {
        this.address = address;
    }

    /**
     * Gets the BGP session IPv4 address: local or remote.
     *
     * @return the BGP session IPv4 address
     */
    public Ip4Address ip4Address() {
        return this.ip4Address;
    }

    /**
     * Sets the BGP session IPv4 address: local or remote.
     *
     * @param ip4Address the BGP session IPv4 address to set
     */
    public void setIp4Address(Ip4Address ip4Address) {
        this.ip4Address = ip4Address;
    }

    /**
     * Gets the BGP session BGP version: local or remote.
     *
     * @return the BGP session BGP version
     */
    public int bgpVersion() {
        return this.bgpVersion;
    }

    /**
     * Sets the BGP session BGP version: local or remote.
     *
     * @param bgpVersion the BGP session BGP version to set
     */
    public void setBgpVersion(int bgpVersion) {
        this.bgpVersion = bgpVersion;
    }

    /**
     * Gets the BGP session AS number: local or remote.
     *
     * @return the BGP session AS number
     */
    public long asNumber() {
        return this.asNumber;
    }

    /**
     * Sets the BGP session AS number: local or remote.
     *
     * @param asNumber the BGP session AS number to set
     */
    public void setAsNumber(long asNumber) {
        this.asNumber = asNumber;
    }

    /**
     * Gets the BGP session AS4 number: local or remote.
     *
     * @return the BGP session AS4 number
     */
    public long as4Number() {
        return this.as4Number;
    }

    /**
     * Sets the BGP session AS4 number: local or remote.
     *
     * @param as4Number the BGP session AS4 number to set
     */
    public void setAs4Number(long as4Number) {
        this.as4Number = as4Number;
    }

    /**
     * Gets the BGP session holdtime: local or remote.
     *
     * @return the BGP session holdtime
     */
    public long holdtime() {
        return this.holdtime;
    }

    /**
     * Sets the BGP session holdtime: local or remote.
     *
     * @param holdtime the BGP session holdtime to set
     */
    public void setHoldtime(long holdtime) {
        this.holdtime = holdtime;
    }

    /**
     * Gets the BGP session BGP Identifier as an IPv4 address: local or remote.
     *
     * @return the BGP session BGP Identifier as an IPv4 address
     */
    public Ip4Address bgpId() {
        return this.bgpId;
    }

    /**
     * Sets the BGP session BGP Identifier as an IPv4 address: local or remote.
     *
     * @param bgpId the BGP session BGP Identifier to set
     */
    public void setBgpId(Ip4Address bgpId) {
        this.bgpId = bgpId;
    }

    /**
     * Gets the BGP Multiprotocol Extensions: local or remote.
     *
     * @return true if the BGP Multiprotocol Extensions are enabled, otherwise
     * false
     */
    public boolean mpExtensions() {
        return this.mpExtensions;
    }

    /**
     * Gets the BGP session AFI/SAFI configuration for IPv4 unicast: local or
     * remote.
     *
     * @return the BGP session AFI/SAFI configuration for IPv4 unicast
     */
    public boolean ipv4Unicast() {
        return ipv4Unicast;
    }

    /**
     * Sets the BGP session AFI/SAFI configuration for IPv4 unicast: local or
     * remote.
     */
    public void setIpv4Unicast() {
        this.mpExtensions = true;
        this.ipv4Unicast = true;
    }

    /**
     * Gets the BGP session AFI/SAFI configuration for IPv4 multicast: local or
     * remote.
     *
     * @return the BGP session AFI/SAFI configuration for IPv4 multicast
     */
    public boolean ipv4Multicast() {
        return ipv4Multicast;
    }

    /**
     * Sets the BGP session AFI/SAFI configuration for IPv4 multicast: local or
     * remote.
     */
    public void setIpv4Multicast() {
        this.mpExtensions = true;
        this.ipv4Multicast = true;
    }

    /**
     * Gets the BGP session AFI/SAFI configuration for IPv6 unicast: local or
     * remote.
     *
     * @return the BGP session AFI/SAFI configuration for IPv6 unicast
     */
    public boolean ipv6Unicast() {
        return ipv6Unicast;
    }

    /**
     * Sets the BGP session AFI/SAFI configuration for IPv6 unicast: local or
     * remote.
     */
    void setIpv6Unicast() {
        this.mpExtensions = true;
        this.ipv6Unicast = true;
    }

    /**
     * Gets the BGP session AFI/SAFI configuration for IPv6 multicast: local or
     * remote.
     *
     * @return the BGP session AFI/SAFI configuration for IPv6 multicast
     */
    public boolean ipv6Multicast() {
        return ipv6Multicast;
    }

    /**
     * Sets the BGP session AFI/SAFI configuration for IPv6 multicast: local or
     * remote.
     */
    public void setIpv6Multicast() {
        this.mpExtensions = true;
        this.ipv6Multicast = true;
    }

    /**
     * Gets the BGP session 4 octet AS path capability: local or remote.
     *
     * @return true when the BGP session has 4 octet AS path capability
     */
    public boolean as4OctetCapability() {
        return this.as4OctetCapability;
    }

    /**
     * Sets the BGP session 4 octet AS path capability.
     */
    public void setAs4OctetCapability() {
        this.as4OctetCapability = true;
    }
}
