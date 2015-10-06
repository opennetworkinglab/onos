/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.routing.config.impl;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.onlab.packet.MacAddress;
import org.onosproject.routing.config.BgpPeer;
import org.onosproject.routing.config.BgpSpeaker;
import org.onosproject.routing.config.LocalIpPrefixEntry;

import java.util.Collections;
import java.util.List;

/**
 * Contains the configuration data for SDN-IP that has been read from a
 * JSON-formatted configuration file.
 */
public class Configuration {
    // We call the BGP routers in our SDN network the BGP speakers, and call
    // the BGP routers outside our SDN network the BGP peers.
    private List<BgpSpeaker> bgpSpeakers = Collections.emptyList();
    private List<BgpPeer> peers = Collections.emptyList();
    private MacAddress virtualGatewayMacAddress;

    // All IP prefixes from the configuration are local
    private List<LocalIpPrefixEntry> localIp4PrefixEntries =
            Collections.emptyList();
    private List<LocalIpPrefixEntry> localIp6PrefixEntries =
            Collections.emptyList();

    /**
     * Default constructor.
     */
    public Configuration() {
    }

    /**
     * Gets a list of bgpSpeakers in the system, represented by
     * {@link BgpSpeaker} objects.
     *
     * @return the list of BGP speakers
     */
    public List<BgpSpeaker> getBgpSpeakers() {
        return Collections.unmodifiableList(bgpSpeakers);
    }

    /**
     * Sets a list of bgpSpeakers in the system.
     *
     * @param bgpSpeakers the list of BGP speakers
     */
    @JsonProperty("bgpSpeakers")
    public void setBgpSpeakers(List<BgpSpeaker> bgpSpeakers) {
        this.bgpSpeakers = bgpSpeakers;
    }

    /**
     * Gets a list of BGP peers we are configured to peer with. Peers are
     * represented by {@link BgpPeer} objects.
     *
     * @return the list of BGP peers
     */
    public List<BgpPeer> getPeers() {
        return Collections.unmodifiableList(peers);
    }

    /**
     * Sets a list of BGP peers we configured to peer with.
     *
     * @param peers the list of BGP peers
     */
    @JsonProperty("bgpPeers")
    public void setPeers(List<BgpPeer> peers) {
        this.peers = peers;
    }

    /**
     * Gets the MAC address we configured for virtual gateway
     * in SDN network.
     *
     * @return the MAC address of virtual gateway
     */
    public MacAddress getVirtualGatewayMacAddress() {
        return virtualGatewayMacAddress;
    }

    /**
     * Sets the MAC address for virtual gateway in SDN network.
     *
     * @param virtualGatewayMacAddress the MAC address of virtual gateway
     */
    @JsonProperty("virtualGatewayMacAddress")
    public void setVirtualGatewayMacAddress(MacAddress virtualGatewayMacAddress) {
        this.virtualGatewayMacAddress = virtualGatewayMacAddress;
    }

    /**
     * Gets a list of local IPv4 prefix entries configured for local
     * SDN network.
     * <p>
     * IP prefix entries are represented by {@link LocalIpPrefixEntry}
     * objects.
     * </p>
     *
     * @return the list of local IPv4 prefix entries
     */
    public List<LocalIpPrefixEntry> getLocalIp4PrefixEntries() {
        return Collections.unmodifiableList(localIp4PrefixEntries);
    }

    /**
     * Sets a list of IPv4 prefix entries configured for local SDN network.
     *
     * @param ip4PrefixEntries the list of Ipv4 prefix entries
     */
    @JsonProperty("ip4LocalPrefixes")
    public void setLocalIp4PrefixEntries(List<LocalIpPrefixEntry> ip4PrefixEntries) {
        this.localIp4PrefixEntries = ip4PrefixEntries;
    }

    /**
     * Gets a list of IPv6 prefix entries configured for local SDN network.
     * <p>
     * IP prefix entries are represented by {@link LocalIpPrefixEntry}
     * objects.
     * </p>
     *
     * @return the list of IPv6 prefix entries
     */
    public List<LocalIpPrefixEntry> getLocalIp6PrefixEntries() {
        return Collections.unmodifiableList(localIp6PrefixEntries);
    }

    /**
     * Sets a list of IPv6 prefix entries configured for local SDN network.
     *
     * @param ip6PrefixEntries the list of Ipv6 prefix entries
     */
    @JsonProperty("ip6LocalPrefixes")
    public void setLocalIp6PrefixEntries(List<LocalIpPrefixEntry> ip6PrefixEntries) {
        this.localIp6PrefixEntries = ip6PrefixEntries;
    }

}
