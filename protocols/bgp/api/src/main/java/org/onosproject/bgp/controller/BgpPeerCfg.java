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

/**
 * BGP Peer configuration information.
 */
public interface BgpPeerCfg {

    enum State {

        /**
         * Signifies that peer connection is idle.
         */
        IDLE,

        /**
         * Signifies that connection is initiated.
         */
        CONNECT,

        /**
         * Signifies that state is active and connection can be established.
         */
        ACTIVE,

        /**
         * Signifies that open is sent and anticipating reply.
         */
        OPENSENT,

        /**
         * Signifies that peer sent the open message as reply.
         */
        OPENCONFIRM,

        /**
         * Signifies that all the negotiation is successful and ready to exchange other messages.
         */
        ESTABLISHED,

        /**
         * Signifies that invalid state.
         */
        INVALID
    }

    /**
     * Returns the connection State information of the peer.
     *
     * @return
     *          enum state is returned
     */
    State getState();

    /**
     * Set the connection state information of the peer.
     *
     * @param state
     *          enum state
     */
    void setState(State state);

    /**
     * Returns the connection is initiated from us or not.
     *
     * @return
     *          true if the connection is initiated by this peer, false if it has been received.
     */
    boolean getSelfInnitConnection();

    /**
     * Set the connection is initiated from us or not.
     *
     * @param selfInit
     *          true if the connection is initiated by this peer, false if it has been received.
     */
    void setSelfInnitConnection(boolean selfInit);

    /**
     * Returns the AS number to which this peer belongs.
     *
     * @return
     *          AS number
     */
    int getAsNumber();

    /**
     * Set the AS number to which this peer belongs.
     *
     * @param asNumber
     *          AS number
     */
    void setAsNumber(int asNumber);

    /**
     * Get the keep alive timer value configured.
     *
     * @return
     *          keep alive timer value in seconds
     */
    short getHoldtime();

    /**
     * Set the keep alive timer value.
     *
     * @param holdTime
     *          keep alive timer value in seconds
     */
    void setHoldtime(short holdTime);

    /**
     * Return the connection type eBGP or iBGP.
     *
     * @return
     *          true if iBGP, false if it is eBGP
     */
    boolean getIsIBgp();

    /**
     * Set the connection type eBGP or iBGP.
     *
     * @param isIBgp
     *          true if iBGP, false if it is eBGP
     */
    void setIsIBgp(boolean isIBgp);

    /**
     * Return the peer router IP address.
     *
     * @return
     *          IP address in string format
     */
    String getPeerRouterId();

    /**
     * Set the peer router IP address.
     *
     * @param peerId
     *          IP address in string format
     */
    void setPeerRouterId(String peerId);

    /**
     * Set the peer router IP address and AS number.
     *
     * @param peerId
     *          IP address in string format
     * @param asNumber
     *          AS number
     */
    void setPeerRouterId(String peerId, int asNumber);

    /**
     * Set the peer connect instance.
     *
     * @param connectpeer connect peer instance
     */
    void setConnectPeer(BgpConnectPeer connectpeer);

    /**
     * Get the peer connect instance.
     *
     * @return peer connect instance
     */
    BgpConnectPeer connectPeer();
}
