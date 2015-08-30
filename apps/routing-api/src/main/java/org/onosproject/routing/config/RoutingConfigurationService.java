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
package org.onosproject.routing.config;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;

import java.util.Map;
import java.util.Set;

/**
 * Provides information about the routing configuration.
 */
public interface RoutingConfigurationService {

    /**
     * Gets the list of BGP speakers inside the SDN network.
     *
     * @return the map of BGP speaker names to BGP speaker objects
     */
    Map<String, BgpSpeaker> getBgpSpeakers();

    /**
     * Gets the list of configured BGP peers.
     *
     * @return the map from peer IP address to BgpPeer object
     */
    Map<IpAddress, BgpPeer> getBgpPeers();

    /**
     * Gets the MAC address configured for virtual gateway in SDN network.
     *
     * @return the MAC address of virtual gateway
     */
    MacAddress getVirtualGatewayMacAddress();

    /**
     * Evaluates whether an IP address is a virtual gateway IP address.
     *
     * @param ipAddress the IP address to evaluate
     * @return true if the IP address is a virtual gateway address, otherwise false
     */
    boolean isVirtualGatewayIpAddress(IpAddress ipAddress);

    /**
     * Evaluates whether an IP address belongs to local SDN network.
     *
     * @param ipAddress the IP address to evaluate
     * @return true if the IP address belongs to local SDN network, otherwise false
     */
    boolean isIpAddressLocal(IpAddress ipAddress);

    /**
     * Evaluates whether an IP prefix belongs to local SDN network.
     *
     * @param ipPrefix the IP prefix to evaluate
     * @return true if the IP prefix belongs to local SDN network, otherwise false
     */
    boolean isIpPrefixLocal(IpPrefix ipPrefix);

    /**
     * Retrieves the entire set of interfaces in the network.
     *
     * @return the set of interfaces
     * @deprecated in Drake release - use InterfaceService instead
     */
    @Deprecated
    Set<Interface> getInterfaces();

    /**
     * Retrieves the entire set of connect points connected to BGP peers in the
     * network.
     *
     * @return the set of connect points connected to BGP peers
     */
    Set<ConnectPoint> getBgpPeerConnectPoints();

    /**
     * Retrieves the interface associated with the given connect point.
     *
     * @param connectPoint the connect point to retrieve interface information
     * for
     * @return the interface
     * @deprecated in Drake release - use InterfaceService instead
     */
    @Deprecated
    Interface getInterface(ConnectPoint connectPoint);

    /**
     * Retrieves the interface associated with the given IP address.
     *
     * @param ip IP address of the interface
     * @return the interface
     * @deprecated in Drake release - use InterfaceService instead
     */
    @Deprecated
    Interface getInterface(IpAddress ip);

    /**
     * Retrieves the interface that matches the given IP address. Matching
     * means that the IP address is in one of the interface's assigned subnets.
     *
     * @param ipAddress IP address to match
     * @return the matching interface
     * @deprecated in Drake release - use InterfaceService instead
     */
    @Deprecated
    Interface getMatchingInterface(IpAddress ipAddress);

}
