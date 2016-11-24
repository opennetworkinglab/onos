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

import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpMessage;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstraction of an BGP controller. Serves as a one stop shop for obtaining BGP devices and (un)register listeners on
 * bgp events
 */
public interface BgpController {

    /**
     * Returns list of bgp peers connected to this BGP controller.
     *
     * @return Iterable of BGPPeer elements
     */
    Iterable<BgpPeer> getPeers();

    /**
     * Returns the actual bgp peer for the given ip address.
     *
     * @param bgpId the id of the bgp peer to fetch
     * @return the interface to this bgp peer
     */
    BgpPeer getPeer(BgpId bgpId);

    /**
     * Register a listener for BGP message events.
     *
     * @param listener the listener to notify
     */
    void addListener(BgpNodeListener listener);

    /**
     * Unregister a listener.
     *
     * @param listener the listener to unregister
     */
    void removeListener(BgpNodeListener listener);

    /**
     * Send a message to a particular bgp peer.
     *
     * @param bgpId the id of the peer to send message.
     * @param msg   the message to send
     */
    void writeMsg(BgpId bgpId, BgpMessage msg);

    /**
     * Process a message and notify the appropriate listeners.
     *
     * @param bgpId id of the peer the message arrived on
     * @param msg   the message to process.
     * @throws BgpParseException on data processing error
     */
    void processBgpPacket(BgpId bgpId, BgpMessage msg) throws BgpParseException;

    /**
     * Close all connected BGP peers.
     */
    void closeConnectedPeers();

    /**
     * Get the BGPConfig class to the caller.
     *
     * @return configuration object
     */
    BgpCfg getConfig();

    /**
     * Get the BGP connected peers to this controller.
     *
     * @return the integer number
     */
    int connectedPeerCount();

    /**
     * Return BGP local RIB instance with VPN.
     *
     * @return BGPLocalRibImpl local RIB with VPN
     */
    BgpLocalRib bgpLocalRibVpn();

    /**
     * Return BGP local RIB instance.
     *
     * @return BGPLocalRibImpl local RIB
     */
    BgpLocalRib bgpLocalRib();

    /**
     * Return BGP peer manager.
     *
     * @return BGPPeerManager peer manager instance
     */
    BgpPeerManager peerManager();

    /**
     * Return BGP connected peers.
     *
     * @return connectedPeers connected peers
     */
    Map<BgpId, BgpPeer> connectedPeers();

    /**
     * Return BGP node listener.
     *
     * @return node listener
     */
    Set<BgpNodeListener> listener();

    /**
     * Register a listener for BGP message events.
     *
     * @param listener the listener to notify
     */
    void addLinkListener(BgpLinkListener listener);

    /**
     * Unregister a listener.
     *
     * @param listener the listener to unregister
     */
    void removeLinkListener(BgpLinkListener listener);

    /**
     * Return BGP link listener.
     *
     * @return link listener
     */
    Set<BgpLinkListener> linkListener();

    /**
     * Stores the exceptions occured during an active session.
     *
     * @param peerId BGP peer id
     * @param exception exceptions based on the peer id.
     */
    void activeSessionExceptionAdd(String peerId, String exception);

    /**
     * Stores the exceptions occured during an closed session.
     *
     * @param peerId BGP peer id
     * @param exception exceptions based on the peer id
     */
    void closedSessionExceptionAdd(String peerId, String exception);

    /**
     * Return active session exceptions.
     *
     * @return activeSessionMap
     */
    Map<String, List<String>> activeSessionMap();

    /**
     * Return closed session exceptions.
     *
     * @return closedSessionMap
     */
    Map<String, List<String>> closedSessionMap();

}
