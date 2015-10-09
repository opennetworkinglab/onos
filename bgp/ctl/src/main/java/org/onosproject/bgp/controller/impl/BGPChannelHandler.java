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

package org.onosproject.bgp.controller.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutException;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.onlab.packet.IpAddress;
import org.onosproject.bgp.controller.BGPCfg;
import org.onosproject.bgp.controller.BGPId;
import org.onosproject.bgp.controller.BGPPeer;
import org.onosproject.bgp.controller.BGPPeerCfg;
import org.onosproject.bgp.controller.impl.BGPControllerImpl.BGPPeerManager;
import org.onosproject.bgpio.exceptions.BGPParseException;
import org.onosproject.bgpio.protocol.BGPMessage;
//import org.onosproject.bgpio.protocol.BGPOpenMsg;
import org.onosproject.bgpio.protocol.BGPType;
import org.onosproject.bgpio.protocol.BGPVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Channel handler deals with the bgp peer connection and dispatches messages from peer to the appropriate locations.
 */
class BGPChannelHandler extends IdleStateAwareChannelHandler {

    private static final Logger log = LoggerFactory.getLogger(BGPChannelHandler.class);

    static final int BGP_MAX_KEEPALIVE_INTERVAL = 3;
    private BGPPeer bgpPeer;
    private BGPId thisbgpId;
    Channel channel;
    private BGPKeepAliveTimer keepAliveTimer = null;
    private short peerHoldTime = 0;
    private short negotiatedHoldTime = 0;
    private short peerAsNum;
    private int peerIdentifier;
    private BGPPacketStatsImpl bgpPacketStats;
    static final int MAX_WRONG_COUNT_PACKET = 5;

    // State needs to be volatile because the HandshakeTimeoutHandler
    // needs to check if the handshake is complete
    private volatile ChannelState state;

    // When a bgp peer with a ip addresss is found (i.e we already have a
    // connected peer with the same ip), the new peer is immediately
    // disconnected. At that point netty callsback channelDisconnected() which
    // proceeds to cleaup peer state - we need to ensure that it does not cleanup
    // peer state for the older (still connected) peer
    private volatile Boolean duplicateBGPIdFound;
    // Indicates the bgp version used by this bgp peer
    protected BGPVersion bgpVersion;
    private BGPControllerImpl bgpControllerImpl;
    private BGPPeerManager peerManager;
    private InetSocketAddress inetAddress;
    private IpAddress ipAddress;
    private SocketAddress address;
    private String peerAddr;
    private BGPCfg bgpconfig;

    /**
     * Create a new unconnected BGPChannelHandler.
     *
     * @param bgpCtrlImpl bgp controller implementation object
     */
    BGPChannelHandler(BGPControllerImpl bgpCtrlImpl) {
        this.bgpControllerImpl = bgpCtrlImpl;
        this.peerManager = bgpCtrlImpl.getPeerManager();
        this.state = ChannelState.IDLE;
        this.duplicateBGPIdFound = Boolean.FALSE;
        this.bgpPacketStats = new BGPPacketStatsImpl();
        this.bgpconfig = bgpCtrlImpl.getConfig();
    }

    // To disconnect peer session.
    public void disconnectPeer() {
        bgpPeer.disconnectPeer();
    }

    // *************************
    // Channel State Machine
    // *************************

    /**
     * The state machine for handling the peer/channel state. All state transitions should happen from within the state
     * machine (and not from other parts of the code)
     */
    enum ChannelState {
        /**
         * Initial state before channel is connected.
         */
        IDLE(false) {

        },

        OPENSENT(false) {
            @Override
            void processBGPMessage(BGPChannelHandler h, BGPMessage m) throws IOException, BGPParseException {
                log.debug("message received in OPENSENT state");
                // check for OPEN message
                if (m.getType() != BGPType.OPEN) {
                    // When the message type is not keep alive message increment the wrong packet statistics
                    h.processUnknownMsg();
                    log.debug("Message is not OPEN message");
                } else {
                    log.debug("Sending keep alive message in OPENSENT state");
                    h.bgpPacketStats.addInPacket();

                    // TODO: initialize openmessage BGPOpenMsg pOpenmsg = (BGPOpenMsg) m;
                    // TODO: initialize identifier from open messgae h.peerIdentifier = pOpenmsg.getBgpId();

                    // validate capabilities and open msg
                    if (h.openMsgValidation(h)) {
                        log.debug("Sending handshake OPEN message");

                        /*
                         * RFC 4271, section 4.2: Upon receipt of an OPEN message, a BGP speaker MUST calculate the
                         * value of the Hold Timer by using the smaller of its configured Hold Time and the Hold Time
                         * received in the OPEN message
                         */
                        // TODO: initialize holdtime from open message h.peerHoldTime = pOpenmsg.getHoldTime();
                        if (h.peerHoldTime < h.bgpconfig.getHoldTime()) {
                            h.channel.getPipeline().replace("holdTime",
                                                            "holdTime",
                                                            new ReadTimeoutHandler(BGPPipelineFactory.TIMER,
                                                                                   h.peerHoldTime));
                        }

                        log.info("Hold Time : " + h.peerHoldTime);

                        // TODO: get AS number for open message update AS number
                    }

                    // Send keepalive message to peer.
                    h.sendKeepAliveMessage();
                    h.bgpPacketStats.addOutPacket();
                    h.setState(OPENCONFIRM);
                    h.bgpconfig.setPeerConnState(h.peerAddr, BGPPeerCfg.State.OPENCONFIRM);
                }
            }
        },

        OPENWAIT(false) {
            @Override
            void processBGPMessage(BGPChannelHandler h, BGPMessage m) throws IOException, BGPParseException {
                log.debug("Message received in OPEN WAIT State");

                // check for open message
                if (m.getType() != BGPType.OPEN) {
                    // When the message type is not open message increment the wrong packet statistics
                    h.processUnknownMsg();
                    log.debug("Message is not OPEN message");
                } else {
                    h.bgpPacketStats.addInPacket();

                    // TODO: initialize open message BGPOpenMsg pOpenmsg = (BGPOpenMsg) m;

                    // Validate open message
                    if (h.openMsgValidation(h)) {
                        log.debug("Sending handshake OPEN message");

                        /*
                         * RFC 4271, section 4.2: Upon receipt of an OPEN message, a BGP speaker MUST calculate the
                         * value of the Hold Timer by using the smaller of its configured Hold Time and the Hold Time
                         * received in the OPEN message
                         */
                        // TODO: get hold time from open message h.peerHoldTime = pOpenmsg.getHoldTime();
                        if (h.peerHoldTime < h.bgpconfig.getHoldTime()) {
                            h.channel.getPipeline().replace("holdTime",
                                                            "holdTime",
                                                            new ReadTimeoutHandler(BGPPipelineFactory.TIMER,
                                                                                   h.peerHoldTime));
                        }

                        log.debug("Hold Time : " + h.peerHoldTime);

                        //TODO: update AS number form open messsage update AS number

                        h.sendHandshakeOpenMessage();
                        h.bgpPacketStats.addOutPacket();
                        h.setState(OPENCONFIRM);
                    }
                }
            }
        },

        OPENCONFIRM(false) {
            @Override
            void processBGPMessage(BGPChannelHandler h, BGPMessage m) throws IOException, BGPParseException {
                log.debug("Message received in OPENCONFIRM state");
                // check for keep alive message
                if (m.getType() != BGPType.KEEP_ALIVE) {
                    // When the message type is not keep alive message handle the wrong packet
                    h.processUnknownMsg();
                    log.debug("Message is not KEEPALIVE message");
                } else {

                    // Set the peer connected status
                    h.bgpPacketStats.addInPacket();
                    log.debug("Sending keep alive message in OPENCONFIRM state");

                    final InetSocketAddress inetAddress = (InetSocketAddress) h.address;
                    h.thisbgpId = BGPId.bgpId(IpAddress.valueOf(inetAddress.getAddress()));

                    h.bgpPeer = h.peerManager.getBGPPeerInstance(h.thisbgpId, h.bgpVersion, h.bgpPacketStats);
                    // set the status fo bgp as connected
                    h.bgpPeer.setConnected(true);
                    h.bgpPeer.setChannel(h.channel);

                    // set specific parameters to bgp peer
                    h.bgpPeer.setBgpPeerVersion(h.bgpVersion);
                    h.bgpPeer.setBgpPeerASNum(h.peerAsNum);
                    h.bgpPeer.setBgpPeerHoldTime(h.peerHoldTime);
                    h.bgpPeer.setBgpPeerIdentifier(h.peerIdentifier);

                    h.negotiatedHoldTime = (h.peerHoldTime < h.bgpconfig.getHoldTime()) ? h.peerHoldTime : h.bgpconfig
                            .getHoldTime();
                    h.bgpPeer.setNegotiatedHoldTime(h.negotiatedHoldTime);
                    /*
                     * RFC 4271, When an OPEN message is received, sends a KEEPALIVE message, If the negotiated hold
                     * time value is zero, then the HoldTimer and KeepaliveTimer are not started. A reasonable maximum
                     * time between KEEPALIVE messages would be one third of the Hold Time interval.
                     */
                    h.sendKeepAliveMessage();

                    if (h.negotiatedHoldTime != 0) {
                        h.keepAliveTimer
                            = new BGPKeepAliveTimer(h, (h.negotiatedHoldTime / BGP_MAX_KEEPALIVE_INTERVAL));
                    }

                    h.bgpPacketStats.addOutPacket();

                    // set the state handshake completion.
                    h.setHandshakeComplete(true);

                    if (!h.peerManager.addConnectedPeer(h.thisbgpId, h.bgpPeer)) {
                        /*
                         * RFC 4271, Section 6.8, Based on the value of the BGP identifier, a convention is established
                         * for detecting which BGP connection is to be preserved when a collision occurs. The convention
                         * is to compare the BGP Identifiers of the peers involved in the collision and to retain only
                         * the connection initiated by the BGP speaker with the higher-valued BGP Identifier..
                         */
                        // TODO: Connection collision handling.
                        disconnectDuplicate(h);
                    } else {
                        h.setState(ESTABLISHED);
                        h.bgpconfig.setPeerConnState(h.peerAddr, BGPPeerCfg.State.ESTABLISHED);
                    }
                }
            }
        },

        ESTABLISHED(true) {
            @Override
            void processBGPMessage(BGPChannelHandler h, BGPMessage m) throws IOException, BGPParseException {
                log.debug("Message received in established state " + m.getType());
                // dispatch the message
                h.dispatchMessage(m);
            }
        };

        private boolean handshakeComplete;

        ChannelState(boolean handshakeComplete) {
            this.handshakeComplete = handshakeComplete;
        }

        /**
         * Is this a state in which the handshake has completed?
         *
         * @return true if the handshake is complete
         */
        public boolean isHandshakeComplete() {
            return this.handshakeComplete;
        }

        /**
         * Disconnect duplicate peer connection.
         *
         * @param h channel handler
         */
        protected void disconnectDuplicate(BGPChannelHandler h) {
            log.error("Duplicated BGP IP or incompleted cleanup - " + "" + "disconnecting channel {}",
                      h.getPeerInfoString());
            h.duplicateBGPIdFound = Boolean.TRUE;
            h.channel.disconnect();
        }

        // set handshake completion status
        public void setHandshakeComplete(boolean handshakeComplete) {
            this.handshakeComplete = handshakeComplete;
        }

        void processBGPMessage(BGPChannelHandler bgpChannelHandler, BGPMessage pm)
                throws IOException, BGPParseException {
            // TODO Auto-generated method stub
            log.debug("BGP message stub");
        }

    }

    // *************************
    // Channel handler methods
    // *************************

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

        channel = e.getChannel();
        log.info("BGP connected from {}", channel.getRemoteAddress());

        address = channel.getRemoteAddress();
        if (!(address instanceof InetSocketAddress)) {
            throw new IOException("Invalid peer connection.");
        }

        // Connection should establish only if local ip and Autonomous system number is configured.
        if (bgpconfig.getState() != BGPCfg.State.IP_AS_CONFIGURED) {
            channel.close();
            log.info("BGP local AS and router ID not configured");
            return;
        }

        inetAddress = (InetSocketAddress) address;
        ipAddress = IpAddress.valueOf(inetAddress.getAddress());
        peerAddr = ipAddress.toString();

        // if peer is not configured disconnect session
        if (!bgpconfig.isPeerConfigured(peerAddr)) {
            log.debug("Peer is not configured {}", peerAddr);
            channel.close();
            return;
        }

        // if connection is already established close channel
        if (peerManager.isPeerConnected(peerAddr)) {
            log.debug("Duplicate connection received, peer {}", peerAddr);
            channel.close();
            return;
        }

        if (null != channel.getPipeline().get("PassiveHandler")) {
            log.info("BGP handle connection request from peer");
            // Wait for open message from bgp peer
            setState(ChannelState.OPENWAIT);
        } else if (null != channel.getPipeline().get("ActiveHandler")) {
            log.info("BGP handle connection response from peer");

            sendHandshakeOpenMessage();
            bgpPacketStats.addOutPacket();
            setState(ChannelState.OPENSENT);
            bgpconfig.setPeerConnState(peerAddr, BGPPeerCfg.State.OPENSENT);
        }
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

        channel = e.getChannel();
        log.info("BGP disconnected callback for bgp:{}. Cleaning up ...", getPeerInfoString());

        address = channel.getRemoteAddress();
        if (!(address instanceof InetSocketAddress)) {
            throw new IOException("Invalid peer connection.");
        }

        inetAddress = (InetSocketAddress) address;
        ipAddress = IpAddress.valueOf(inetAddress.getAddress());
        peerAddr = ipAddress.toString();

        if (thisbgpId != null) {
            if (!duplicateBGPIdFound) {
                // if the disconnected peer (on this ChannelHandler)
                // was not one with a duplicate, it is safe to remove all
                // state for it at the controller. Notice that if the disconnected
                // peer was a duplicate-ip, calling the method below would clear
                // all state for the original peer (with the same ip),
                // which we obviously don't want.
                log.debug("{}:removal called", getPeerInfoString());
                if (bgpPeer != null) {
                    peerManager.removeConnectedPeer(thisbgpId);
                }
            } else {
                // A duplicate was disconnected on this ChannelHandler,
                // this is the same peer reconnecting, but the original state was
                // not cleaned up - XXX check liveness of original ChannelHandler
                log.debug("{}:duplicate found", getPeerInfoString());
                duplicateBGPIdFound = Boolean.FALSE;
            }

            if (null != keepAliveTimer) {
                keepAliveTimer.getKeepAliveTimer().cancel();
            }
        } else {
            log.warn("No bgp ip in channelHandler registered for " + "disconnected peer {}", getPeerInfoString());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {

        log.info("[exceptionCaught]: " + e.toString());

        if (e.getCause() instanceof ReadTimeoutException) {
            if ((ChannelState.OPENWAIT == state) || (ChannelState.OPENSENT == state)) {

                // When ReadTimeout timer is expired in OPENWAIT/OPENSENT state, it is considered
                // TODO: Send notification
                channel.close();
                state = ChannelState.IDLE;
                return;
            } else if (ChannelState.OPENCONFIRM == state) {

                // When ReadTimeout timer is expired in OPENCONFIRM state.
                // TODO: Send Notification
                channel.close();
                state = ChannelState.IDLE;
                return;
            }
        } else if (e.getCause() instanceof ClosedChannelException) {
            log.debug("Channel for bgp {} already closed", getPeerInfoString());
        } else if (e.getCause() instanceof IOException) {
            log.error("Disconnecting peer {} due to IO Error: {}", getPeerInfoString(), e.getCause().getMessage());
            if (log.isDebugEnabled()) {
                // still print stack trace if debug is enabled
                log.debug("StackTrace for previous Exception: ", e.getCause());
            }
            channel.close();
        } else if (e.getCause() instanceof BGPParseException) {
            // TODO: SEND NOTIFICATION
            log.debug("BGP Parse Exception: ", e.getCause());
        } else if (e.getCause() instanceof RejectedExecutionException) {
            log.warn("Could not process message: queue full");
        } else {
            log.error("Error while processing message from peer " + getPeerInfoString() + "state " + this.state);
            channel.close();
        }
    }

    @Override
    public String toString() {
        return getPeerInfoString();
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof List) {
            @SuppressWarnings("Unchecked")
            List<BGPMessage> msglist = (List<BGPMessage>) e.getMessage();
            for (BGPMessage pm : msglist) {
                // Do the actual packet processing
                state.processBGPMessage(this, pm);
            }
        } else {
            state.processBGPMessage(this, (BGPMessage) e.getMessage());
        }
    }

    // *************************
    // Channel utility methods
    // *************************
    /**
     * Set handshake status.
     *
     * @param handshakeComplete handshake complete status
     */
    public void setHandshakeComplete(boolean handshakeComplete) {
        this.state.setHandshakeComplete(handshakeComplete);
    }

    /**
     * Is this a state in which the handshake has completed?
     *
     * @return true if the handshake is complete
     */
    public boolean isHandshakeComplete() {
        return state.isHandshakeComplete();
    }

    /**
     * To handle the BGP message.
     *
     * @param m BGP message
     */
    private void dispatchMessage(BGPMessage m) throws BGPParseException {
        bgpPacketStats.addInPacket();
        bgpControllerImpl.processBGPPacket(thisbgpId, m);
    }

    /**
     * Return a string describing this peer based on the already available information (ip address and/or remote
     * socket).
     *
     * @return display string
     */
    private String getPeerInfoString() {
        if (bgpPeer != null) {
            return bgpPeer.toString();
        }
        String channelString;
        if (channel == null || channel.getRemoteAddress() == null) {
            channelString = "?";
        } else {
            channelString = channel.getRemoteAddress().toString();
        }
        String bgpIpString;
        // TODO: implement functionality to get bgp id string
        bgpIpString = "?";
        return String.format("[%s BGP-IP[%s]]", channelString, bgpIpString);
    }

    /**
     * Update the channels state. Only called from the state machine. TODO: enforce restricted state transitions
     *
     * @param state
     */
    private void setState(ChannelState state) {
        this.state = state;
    }

    /**
     * get packet statistics.
     *
     * @return packet statistics
     */
    public BGPPacketStatsImpl getBgpPacketStats() {
        return bgpPacketStats;
    }

    /**
     * Send handshake open message to the peer.
     *
     * @throws IOException ,BGPParseException
     */
    private void sendHandshakeOpenMessage() throws IOException, BGPParseException {
        // TODO: send open message.

    }

    /**
     * Send keep alive message.
     *
     * @throws IOException when channel is disconnected
     * @throws BGPParseException while building keep alive message
     */
    synchronized void sendKeepAliveMessage() throws IOException, BGPParseException {

        // TODO: send keep alive message.
    }

    /**
     * Send notification and close channel with peer.
     */
    private void sendErrNotificationAndCloseChannel() {
        // TODO: send notification
        channel.close();
    }

    /**
     * Process unknown BGP message received.
     *
     * @throws BGPParseException when received invalid message
     */
    public void processUnknownMsg() throws BGPParseException {
        log.debug("UNKNOWN message received");
        Date now = null;
        if (bgpPacketStats.wrongPacketCount() == 0) {
            now = new Date();
            bgpPacketStats.setTime(now.getTime());
            bgpPacketStats.addWrongPacket();
            sendErrNotificationAndCloseChannel();
        }
        if (bgpPacketStats.wrongPacketCount() > 1) {
            Date lastest = new Date();
            bgpPacketStats.addWrongPacket();
            // converting to seconds
            if (((lastest.getTime() - bgpPacketStats.getTime()) / 1000) > 60) {
                now = lastest;
                bgpPacketStats.setTime(now.getTime());
                bgpPacketStats.resetWrongPacket();
                bgpPacketStats.addWrongPacket();
            } else if (((int) (lastest.getTime() - now.getTime()) / 1000) < 60) {
                if (MAX_WRONG_COUNT_PACKET <= bgpPacketStats.wrongPacketCount()) {
                    // reset once wrong packet count reaches MAX_WRONG_COUNT_PACKET
                    bgpPacketStats.resetWrongPacket();
                    // max wrong packets received send error message and close the session
                    sendErrNotificationAndCloseChannel();
                }
            }
        }
    }

    /**
     * Open message validation.
     *
     * @param h channel handler
     * @return true if validation succeed, otherwise false
     * @throws BGPParseException when received invalid message
     */
    public boolean openMsgValidation(BGPChannelHandler h) throws BGPParseException {
        // TODO: Open message validation.
        return true;
    }
}
