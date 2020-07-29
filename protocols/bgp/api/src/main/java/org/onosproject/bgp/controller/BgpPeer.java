/*
 * Copyright 2015-present Open Networking Foundation
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
import java.util.List;
import org.jboss.netty.channel.Channel;
import org.onlab.packet.IpAddress;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpEvpnNlri;
import org.onosproject.bgpio.protocol.BgpFactory;
import org.onosproject.bgpio.protocol.BgpMessage;
import org.onosproject.bgpio.protocol.flowspec.BgpFlowSpecNlri;
import org.onosproject.bgpio.protocol.flowspec.BgpFlowSpecRouteKey;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.attr.WideCommunity;

/**
 * Represents the peer side of an BGP peer.
 *
 */
public interface BgpPeer {

    enum FlowSpecOperation {

        /**
         * Signifies addition of flow specification rule.
         */
        ADD,

        /**
         *  Signifies updation of flow specification rule.
         */
        UPDATE,

        /**
         * Signifies deletion of flow specification rule.
         */
        DELETE
    }
    /**
     * Sets the associated Netty channel for this bgp peer.
     *
     * @param channel the Netty channel
     */
    void setChannel(Channel channel);

    /**
     * Gets the associated Netty channel handler for this bgp peer.
     *
     * @return Channel channel connected.
     */
    Channel getChannel();

    /**
     * Sets whether the bgp peer is connected.
     *
     * @param connected whether the bgp peer is connected
     */
    void setConnected(boolean connected);

    /**
     * Checks whether the handshake is complete.
     *
     * @return true is finished, false if not.
     */
    boolean isHandshakeComplete();

    /**
     * Writes the message to the peer.
     *
     * @param msg the message to write
     */
    void sendMessage(BgpMessage msg);

    /**
     * Writes the BGPMessage list to the peer.
     *
     * @param msgs the messages to be written
     */
    void sendMessage(List<BgpMessage> msgs);

    /**
     * Provides the factory for BGP version.
     *
     * @return BGP version specific factory.
     */
    BgpFactory factory();

    /**
     * Checks if the bgp peer is still connected.
     *
     * @return whether the bgp peer is still connected
     */
    boolean isConnected();

    /**
     * Disconnects the bgp peer by closing the TCP connection. Results in a call to the channel handler's
     * channelDisconnected method for cleanup
     */
    void disconnectPeer();

    /**
     * Identifies the channel used to communicate with the bgp peer.
     *
     * @return string representation of the connection to the peer
     */
    String channelId();

    /**
     * Maintaining Adj-RIB-In separately for each peer.
     *
     * @param pathAttr list of Bgp path attributes
     * @throws BgpParseException while building Adj-Rib-In
     */
    void buildAdjRibIn(List<BgpValueType> pathAttr) throws BgpParseException;

    /**
     * Return the BGP session info.
     *
     * @return sessionInfo bgp session info
     */
    BgpSessionInfo sessionInfo();

    /**
     * Updates flow specification rule.
     *
     * @param operType operation type add or delete or update
     * @param routeKey flow route key  for the flow rule
     * @param flowSpec BGP flow specification components
     * @param wideCommunity for route policy
     */
    void updateFlowSpec(FlowSpecOperation operType, BgpFlowSpecRouteKey routeKey,
                        BgpFlowSpecNlri flowSpec, WideCommunity wideCommunity);

    /**
     * Updates evpn rule.
     *
     * @param operType     operation type add or delete or update
     * @param nextHop      next Hop
     * @param extcommunity extended community
     * @param evpnNlris    list of evpnNlri
     */
    void updateEvpnNlri(FlowSpecOperation operType, IpAddress nextHop,
                        List<BgpValueType> extcommunity,
                        List<BgpEvpnNlri> evpnNlris);

    /**
     * Send the Route Refresh message to the connected BGP peer.
     */
    void sendRouteRefreshMessage();
}
