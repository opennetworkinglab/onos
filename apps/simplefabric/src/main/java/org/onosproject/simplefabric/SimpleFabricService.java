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
package org.onosproject.simplefabric;

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

    // App symbols
    static final String APP_ID = "org.onosproject.simplefabric";
    static final String L2FORWARD_APP_ID = "org.onosproject.simplefabric.l2forward";
    static final String REACTIVE_APP_ID = "org.onosproject.simplefabric.reactive";

    // Priority for l2NetworkRouting: L2NETWORK_UNICAST or L2NETWORK_BROADCAST
    static final int PRI_L2NETWORK_UNICAST = 601;
    static final int PRI_L2NETWORK_BROADCAST = 600;

    // Reactive Routing within Local Subnets
    // ASSUME: local subnets NEVER overlaps each other
    static final int PRI_REACTIVE_LOCAL_FORWARD = 501;
    static final int PRI_REACTIVE_LOCAL_INTERCEPT = 500;
    // Reactive Routing for Border Routes with local subnet
    // Priority: REACTIVE_BROUTE_BASE + routeIpPrefix * REACTIVE_BROUTE_STEP
    //           + REACTIVE_BROUTE_FORWARD or REACTIVE_BROUTE_INTERCEPT
    static final int PRI_REACTIVE_BORDER_BASE = 100;
    static final int PRI_REACTIVE_BORDER_STEP = 2;
    static final int PRI_REACTIVE_BORDER_FORWARD = 1;
    static final int PRI_REACTIVE_BORDER_INTERCEPT = 0;

    // Simple fabric event related timers
    static final long IDLE_INTERVAL_MSEC = 5000;

    // Feature control parameters
    static final boolean ALLOW_IPV6 = false;
    static final boolean ALLOW_ETH_ADDRESS_SELECTOR = true;
    static final boolean REACTIVE_SINGLE_TO_SINGLE = false;
    static final boolean REACTIVE_ALLOW_LINK_CP = false;  // MUST BE false (yjlee, 2017-10-18)
    static final boolean REACTIVE_HASHED_PATH_SELECTION = false;
    static final boolean REACTIVE_MATCH_IP_PROTO = false;

    /**
     * Gets appId.
     *
     * @return appId of simple fabric app
     */
    ApplicationId getAppId();

    /**
     * Gets all the l2Networks.
     *
     * @return all the l2Networks
     */
    Collection<L2Network> getL2Networks();

    /**
     * Retrieves the entire set of ipSubnets configuration.
     *
     * @return all the ipSubnets
     */
    Set<IpSubnet> getIpSubnets();

    /**
     * Retrieves the entire set of static routes to outer networks.
     *
     * @return the set of static routes to outer networks.
     */
    Set<Route> getBorderRoutes();

    /**
     * Evaluates whether a mac is of Virtual Gateway Mac Addresses.
     *
     * @param mac the MacAddress to evaluate
     * @return true if the mac is of any Vitrual Gateway Mac Address of ipSubnets
     */
    boolean isVMac(MacAddress mac);

    /**
     * Evaluates whether an Interface belongs to l2Networks.
     *
     * @param intf the interface to evaluate
     * @return true if the inteface belongs to l2Networks configed, otherwise false
     */
    boolean isL2NetworkInterface(Interface intf);

    /**
     * Find Virtual Gateway Mac Address for Local Subnet Virtual Gateway Ip.
     *
     * @param ip the ip to check for Virtual Gateway Ip
     * @return mac address of virtual gateway
     */
    MacAddress findVMacForIp(IpAddress ip);

    /**
     * Finds the L2 Network with given port and vlanId.
     *
     * @param port the port to be matched
     * @param vlanId the vlanId to be matched
     * @return the L2 Network for specific port and vlanId or null
     */
    L2Network findL2Network(ConnectPoint port, VlanId vlanId);

    /**
     * Finds the L2 Network of the name.
     *
     * @param name the name to be matched
     * @return the L2 Network for specific name
     */
    L2Network findL2Network(String name);

    /**
     * Finds the IpSubnet containing the ipAddress.
     *
     * @param ipAddress the ipAddress to be matched
     * @return the IpSubnet for specific ipAddress
     */
    IpSubnet findIpSubnet(IpAddress ipAddress);

    /**
     * Finds the Border Route containing the ipAddress.
     * ASSUME: ipAddress is out of ipSubnets
     *
     * @param ipAddress the ipAddress to be matched
     * @return the IpSubnet for specific ipAddress
     */
    Route findBorderRoute(IpAddress ipAddress);

    /**
     * Finds the network interface related to the host.
     *
     * @param host the host
     * @return the interface related to the host
     */
    Interface findHostInterface(Host host);

    /**
     * Sends Neighbour Query (ARP or NDP) to Find Host Location.
     *
     * @param ip the ip address to resolve
     * @return true if request mac packets are emitted. otherwise false
     */
    boolean requestMac(IpAddress ip);

    /**
     * Sends Dump Event to all SimpleFabricListeners to Dump Info on the Subject.
     *
     * @param subject the subject to dump
     * @param out the output stream to dump
     */
    void dumpToStream(String subject, OutputStream out);

    /**
     * Triggers to send Refresh Notification to all sub modules.
     */
    void triggerRefresh();

    /**
     * Triggers to send Flush Notification to all sub modules.
     */
    void triggerFlush();

}
