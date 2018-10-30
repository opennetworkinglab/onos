/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.simplefabric.api;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.event.ListenerService;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;

import java.io.OutputStream;
import java.util.Set;
import java.util.Collection;

/**
 * Provides information about the routing configuration.
 */
public interface SimpleFabricService
        extends ListenerService<SimpleFabricEvent, SimpleFabricListener> {

    /**
     * Gets appId.
     *
     * @return appId of simple fabric app
     */
    ApplicationId appId();

    /**
     * Gets all the fabric networks.
     *
     * @return all the fabric networks
     */
    Collection<FabricNetwork> fabricNetworks();

    /**
     * Retrieves the entire set of fabricSubnets configuration.
     *
     * @return all the fabricSubnets
     */
    Set<FabricSubnet> defaultFabricSubnets();

    /**
     * Retrieves the entire set of static routes to outer networks.
     *
     * @return the set of static routes to outer networks.
     */
    Set<FabricRoute> fabricRoutes();

    /**
     * Evaluates whether a MAC is of virtual gateway MAC addresses.
     *
     * @param mac the MAC address to evaluate
     * @return true if the mac is of any virtual gateway MAC address of fabricSubnets
     */
    boolean isVirtualGatewayMac(MacAddress mac);

    /**
     * Evaluates whether an interface belongs to fabric network or not.
     *
     * @param intf the interface to evaluate
     * @return true if the interface belongs to fabric network, otherwise false
     */
    boolean isFabricNetworkInterface(Interface intf);

    /**
     * Finds virtual gateway MAC address for local subnet virtual gateway IP.
     *
     * @param ip the IP to check for virtual gateway IP
     * @return MAC address of virtual gateway
     */
    MacAddress vMacForIp(IpAddress ip);

    /**
     * Finds the L2 fabric network with given port and vlanId.
     *
     * @param port the port to be matched
     * @param vlanId the vlanId to be matched
     * @return the L2 Network for specific port and vlanId or null
     */
    FabricNetwork fabricNetwork(ConnectPoint port, VlanId vlanId);

    /**
     * Finds the fabric network by its name.
     *
     * @param name the name to be matched
     * @return the fabric network
     */
    FabricNetwork fabricNetwork(String name);

    /**
     * Finds the FabricSubnet which contains the given IP address.
     *
     * @param ipAddress the IP address to be matched
     * @return the FabricSubnet
     */
    FabricSubnet fabricSubnet(IpAddress ipAddress);

    /**
     * Finds the FabricRoute which contains the given IP address.
     *
     * @param ipAddress the IP address to be matched
     * @return the FabricRoute
     */
    FabricRoute fabricRoute(IpAddress ipAddress);

    /**
     * Finds the network interface which associated with the host.
     *
     * @param host the host
     * @return the interface associated with the host
     */
    Interface hostInterface(Host host);

    /**
     * Sends neighbour query (ARP or NDP) to find host location.
     *
     * @param ip the IP address to resolve
     * @return true if request MAC packets are emitted, false otherwise
     */
    boolean requestMac(IpAddress ip);

    /**
     * Sends dump event to all SimpleFabricListeners to dump info on the subject.
     *
     * @param subject the subject to dump
     * @param out the output stream to dump
     */
    void dumpToStream(String subject, OutputStream out);

    /**
     * Triggers to send refresh notification to all sub modules.
     */
    void triggerRefresh();

    /**
     * Triggers to send flush notification to all sub modules.
     */
    void triggerFlush();
}
