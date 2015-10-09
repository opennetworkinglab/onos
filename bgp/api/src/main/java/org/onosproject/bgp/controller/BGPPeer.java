/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onosproject.bgpio.protocol.BGPMessage;
import org.onosproject.bgpio.protocol.BGPVersion;

/**
 * Represents the peer side of an bgp peer.
 *
 */
public interface BGPPeer {

    /**
     * Sets the BGP version for this bgp peer.
     *
     * @param bgpVersion the version to set.
     */
    void setBgpPeerVersion(BGPVersion bgpVersion);

    /**
     * Gets the BGP version for this bgp peer.
     *
     * @return bgp identifier.
     */
    int getBgpPeerIdentifier();

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
     * Sets the AS Number for this bgp peer.
     *
     * @param peerASNum the autonomous system number value to set.
     */
    void setBgpPeerASNum(short peerASNum);

    /**
     * Sets the hold time for this bgp peer.
     *
     * @param peerHoldTime the hold timer value to set.
     */
    void setBgpPeerHoldTime(short peerHoldTime);

    /**
     * Sets the peer identifier value.
     *
     * @param peerIdentifier the bgp peer identifier value.
     */
    void setBgpPeerIdentifier(int peerIdentifier);

    /**
     * Sets whether the bgp peer is connected.
     *
     * @param connected whether the bgp peer is connected
     */
    void setConnected(boolean connected);

    /**
     * Initialises the behaviour.
     *
     * @param bgpId id of bgp peer
     * @param bgpVersion BGP version
     * @param pktStats packet statistics
     */
    void init(BGPId bgpId, BGPVersion bgpVersion, BGPPacketStats pktStats);

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
    void sendMessage(BGPMessage msg);

    /**
     * Writes the BGPMessage list to the peer.
     *
     * @param msgs the messages to be written
     */
    void sendMessage(List<BGPMessage> msgs);

    /**
     * Gets a string version of the ID for this bgp peer.
     *
     * @return string version of the ID
     */
    String getStringId();

    /**
     * Gets the ipAddress of the peer.
     *
     * @return the peer bgpId in IPAddress format
     */
    BGPId getBGPId();

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
     * Gets the negotiated hold time.
     *
     * @return the negotiated hold time
     */
    int getNegotiatedHoldTime();

    /**
     * Sets negotiated hold time for the peer.
     *
     * @param negotiatedHoldTime negotiated hold time
     */
    void setNegotiatedHoldTime(short negotiatedHoldTime);
}
