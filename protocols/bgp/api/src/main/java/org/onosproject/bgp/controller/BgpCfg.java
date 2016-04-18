/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.bgp.controller;

import java.util.TreeMap;

/**
 * Abstraction of an BGP configuration. Manages the BGP configuration from CLI to the BGP controller.
 */
public interface BgpCfg {

    enum State {
        /**
         * Signifies that its just created.
         */
        INIT,

        /**
         * Signifies that only IP Address is configured.
         */
        IP_CONFIGURED,

        /**
         * Signifies that only Autonomous System is configured.
         */
        AS_CONFIGURED,

        /**
         * Signifies that both IP and Autonomous System is configured.
         */
        IP_AS_CONFIGURED
    }

    enum FlowSpec {

        /**
         * Signifies that peer support IPV4 flow specification.
         */
        IPV4,

        /**
         *  Signifies that peer support VPNV4 flow specification.
         */
        VPNV4,

        /**
         *  Signifies that peer support IPV4 and VPNV4 flow specification.
         */
        IPV4_VPNV4,

        /**
         * Signifies that peer flow specification capability disabled.
         */
        NONE
    }

    /**
     * Returns the status of the configuration based on this state certain operations like connection is handled.
     *
     * @return State of the configuration
     */
    State getState();

    /**
     * To set the current state of the configuration.
     *
     * @param state Configuration State enum
     */
    void setState(State state);

    /**
     * Get the status of the link state support for this BGP speaker.
     *
     * @return true if the link state is supported else false
     */
    boolean getLsCapability();

    /**
     * Set the link state support to this BGP speaker.
     *
     * @param lscapability true value if link state is supported else false
     */
    void setLsCapability(boolean lscapability);

    /**
     * Get the status of the 32 bit AS support for this BGP speaker.
     *
     * @return true if the 32 bit AS number is supported else false
     */
    boolean getLargeASCapability();

    /**
     * Set the 32 bit AS support capability to this BGP speaker.
     *
     * @param largeAs true value if the 32 bit AS is supported else false
     */
    void setLargeASCapability(boolean largeAs);

    /**
     * Set the AS number to which this BGP speaker belongs.
     *
     * @param localAs 16 or 32 bit AS number, length is dependent on the capability
     */
    void setAsNumber(int localAs);

    /**
     * Get the AS number to which this BGP speaker belongs.
     *
     * @return 16 or 32 bit AS number, length is dependent on the capability
     */
    int getAsNumber();

    /**
     * Get the connection retry count number.
     *
     * @return connection retry count if there is a connection error
     */
    int getMaxConnRetryCount();

    /**
     * Set the connection retry count.
     *
     * @param retryCount number of times to try to connect if there is any error
     */
    void setMaxConnRetryCout(int retryCount);

    /**
     * Get the connection retry time in seconds.
     *
     * @return connection retry time in seconds
     */
    int getMaxConnRetryTime();

    /**
     * Set the connection retry time in seconds.
     *
     * @param retryTime connection retry times in seconds
     */
    void setMaxConnRetryTime(int retryTime);

    /**
     * Set the keep alive timer for the connection.
     *
     * @param holdTime connection hold timer in seconds
     */
    void setHoldTime(short holdTime);

    /**
     * Returns the connection hold timer in seconds.
     *
     * @return connection hold timer in seconds
     */
    short getHoldTime();

    /**
     * Returns the maximum number of session supported.
     *
     * @return maximum number of session supported
     */
    int getMaxSession();

    /**
     * Set the maximum number of sessions to support.
     *
     * @param maxsession maximum number of session
     */
    void setMaxSession(int maxsession);

    /**
     * Returns the Router ID of this BGP speaker.
     *
     * @return IP address in string format
     */
    String getRouterId();

    /**
     * Set the Router ID of this BGP speaker.
     *
     * @param routerid IP address in string format
     */
    void setRouterId(String routerid);

    /**
     * Add the BGP peer IP address and the AS number to which it belongs.
     *
     * @param routerid IP address in string format
     * @param remoteAs AS number to which it belongs
     *
     * @return true if added successfully else false
     */
    boolean addPeer(String routerid, int remoteAs);

    /**
     * Add the BGP peer IP address and the keep alive time.
     *
     * @param routerid IP address in string format
     * @param holdTime keep alive time for the connection
     *
     * @return true if added successfully else false
     */
    boolean addPeer(String routerid, short holdTime);

    /**
     * Add the BGP peer IP address, the AS number to which it belongs and keep alive time.
     *
     * @param routerid IP address in string format
     * @param remoteAs AS number to which it belongs
     * @param holdTime keep alive time for the connection
     *
     * @return true if added successfully else false
     */
    boolean addPeer(String routerid, int remoteAs, short holdTime);

    /**
     * Remove the BGP peer with this IP address.
     *
     * @param routerid router IP address
     *
     * @return true if removed successfully else false
     */
    boolean removePeer(String routerid);

    /**
     * Connect to BGP peer with this IP address.
     *
     * @param routerid router IP address
     *
     * @return true of the configuration is found and able to connect else false
     */
    boolean connectPeer(String routerid);

    /**
     * Disconnect this BGP peer with this IP address.
     *
     * @param routerid router IP address in string format
     *
     * @return true if the configuration is found and able to disconnect else false
     */
    boolean disconnectPeer(String routerid);

    /**
     * Returns the peer tree information.
     *
     * @return return the tree map with IP as key and BGPPeerCfg as object
     */
    TreeMap<String, BgpPeerCfg> displayPeers();

    /**
     * Return the BGP Peer information with this matching IP.
     *
     * @param routerid router IP address in string format
     *
     * @return BGPPeerCfg object
     */
    BgpPeerCfg displayPeers(String routerid);

    /**
     * Check if this BGP peer is configured.
     *
     * @param routerid router IP address in string format
     *
     * @return true if configured exists else false
     */
    boolean isPeerConfigured(String routerid);

    /**
     * Check if this BGP speaker is having connection with the peer.
     *
     * @param routerid router IP address in string format
     *
     * @return true if the connection exists else false
     */
    boolean isPeerConnected(String routerid);

    /**
     * Return the peer tree map.
     *
     * @return return the tree map with IP as key and BGPPeerCfg as object
     */
    TreeMap<String, BgpPeerCfg> getPeerTree();

    /**
     * Set the current connection state information.
     *
     * @param routerid router IP address in string format
     * @param state state information
     */
    void setPeerConnState(String routerid, BgpPeerCfg.State state);

    /**
     * Check if the peer can be connected or not.
     *
     * @param routerid router IP address in string format
     *
     * @return true if the peer can be connected else false
     */
    boolean isPeerConnectable(String routerid);

    /**
     * Get the current peer connection state information.
     *
     * @param routerid router IP address in string format
     *
     * @return state information
     */
    BgpPeerCfg.State getPeerConnState(String routerid);

    /**
     * Gets the flow specification capability.
     *
     * @return flow specification capability
     */
    FlowSpec flowSpecCapability();

    /**
     * Sets the flow specification capability.
     *
     * @param flowSpec flow specification capability
     */
    void setFlowSpecCapability(FlowSpec flowSpec);

    /**
     * Returns the flow specification route policy distribution capability.
     *
     * @return RDP flow specification capability
     */
    boolean flowSpecRpdCapability();

    /**
     * Sets the flow specification route policy distribution capability.
     *
     * @param rpdCapability flow specification RPD capability
     */
    void setFlowSpecRpdCapability(boolean rpdCapability);
}
