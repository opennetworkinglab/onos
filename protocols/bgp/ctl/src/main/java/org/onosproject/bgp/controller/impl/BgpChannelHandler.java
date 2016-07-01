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

package org.onosproject.bgp.controller.impl;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutException;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onosproject.bgp.controller.BgpCfg;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.bgp.controller.BgpId;
import org.onosproject.bgp.controller.BgpPeer;
import org.onosproject.bgp.controller.BgpPeerCfg;
import org.onosproject.bgp.controller.impl.BgpControllerImpl.BgpPeerManagerImpl;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpFactory;
import org.onosproject.bgpio.protocol.BgpMessage;
import org.onosproject.bgpio.protocol.BgpOpenMsg;
import org.onosproject.bgpio.protocol.BgpType;
import org.onosproject.bgpio.protocol.BgpVersion;
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.FourOctetAsNumCapabilityTlv;
import org.onosproject.bgpio.types.MultiProtocolExtnCapabilityTlv;
import org.onosproject.bgpio.types.RpdCapabilityTlv;
import org.onosproject.bgpio.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;

/**
 * Channel handler deals with the bgp peer connection and dispatches messages from peer to the appropriate locations.
 */
class BgpChannelHandler extends IdleStateAwareChannelHandler {

    private static final Logger log = LoggerFactory.getLogger(BgpChannelHandler.class);
    static final int BGP_MIN_HOLDTIME = 3;
    static final int BGP_MAX_KEEPALIVE_INTERVAL = 3;
    private BgpPeer bgpPeer;
    private BgpId thisbgpId;
    private Channel channel;
    private BgpKeepAliveTimer keepAliveTimer = null;
    private short peerHoldTime = 0;
    private short negotiatedHoldTime = 0;
    private long peerAsNum;
    private int peerIdentifier;
    private BgpPacketStatsImpl bgpPacketStats;
    static final int MAX_WRONG_COUNT_PACKET = 5;
    static final byte MULTI_PROTOCOL_EXTN_CAPA_TYPE = 1;
    static final byte FOUR_OCTET_AS_NUM_CAPA_TYPE = 65;
    static final int AS_TRANS = 23456;
    static final int MAX_AS2_NUM = 65535;
    static final short AFI = 16388;
    static final byte RES = 0;
    static final byte SAFI = 71;
    static final byte MAX_UNSUPPORTED_CAPABILITY = 5;

    // State needs to be volatile because the HandshakeTimeoutHandler
    // needs to check if the handshake is complete
    private volatile ChannelState state;

    // When a bgp peer with a ip addresss is found (i.e we already have a
    // connected peer with the same ip), the new peer is immediately
    // disconnected. At that point netty callsback channelDisconnected() which
    // proceeds to cleaup peer state - we need to ensure that it does not
    // cleanup
    // peer state for the older (still connected) peer
    private volatile Boolean duplicateBgpIdFound;
    // Indicates the bgp version used by this bgp peer
    protected BgpVersion bgpVersion;
    private BgpController bgpController;
    protected BgpFactory factory4;
    private boolean isIbgpSession;
    private BgpSessionInfoImpl sessionInfo;
    private BgpPeerManagerImpl peerManager;
    private InetSocketAddress inetAddress;
    private IpAddress ipAddress;
    private SocketAddress address;
    private String peerAddr;
    private BgpCfg bgpconfig;
    List<BgpValueType> remoteBgpCapability;

    /**
     * Create a new unconnected BGPChannelHandler.
     *
     * @param bgpController bgp controller
     */
    BgpChannelHandler(BgpController bgpController) {
        this.bgpController = bgpController;
        this.peerManager = (BgpPeerManagerImpl) bgpController.peerManager();
        this.state = ChannelState.IDLE;
        this.factory4 = Controller.getBgpMessageFactory4();
        this.duplicateBgpIdFound = Boolean.FALSE;
        this.bgpPacketStats = new BgpPacketStatsImpl();
        this.bgpconfig = bgpController.getConfig();
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
            void processBgpMessage(BgpChannelHandler h, BgpMessage m) throws IOException, BgpParseException {
                log.debug("message received in OPENSENT state");
                // check for OPEN message
                if (m.getType() != BgpType.OPEN) {
                    // When the message type is not keep alive message increment the wrong packet statistics
                    h.processUnknownMsg(BgpErrorType.FINITE_STATE_MACHINE_ERROR,
                                        BgpErrorType.RECEIVE_UNEXPECTED_MESSAGE_IN_OPENSENT_STATE,
                                        m.getType().getType());
                    log.debug("Message is not OPEN message");
                } else {
                    log.debug("Sending keep alive message in OPENSENT state");
                    h.bgpPacketStats.addInPacket();

                    BgpOpenMsg pOpenmsg = (BgpOpenMsg) m;
                    h.peerIdentifier = pOpenmsg.getBgpId();

                    // validate capabilities and open msg
                    if (h.openMsgValidation(h, pOpenmsg)) {
                        if (h.connectionCollisionDetection(BgpPeerCfg.State.OPENCONFIRM,
                                                           h.peerIdentifier, h.peerAddr)) {
                            h.channel.close();
                            return;
                        }
                        log.debug("Sending handshake OPEN message");
                        h.remoteBgpCapability = pOpenmsg.getCapabilityTlv();

                        /*
                         * RFC 4271, section 4.2: Upon receipt of an OPEN message, a BGP speaker MUST calculate the
                         * value of the Hold Timer by using the smaller of its configured Hold Time and the Hold Time
                         * received in the OPEN message
                         */
                        h.peerHoldTime = pOpenmsg.getHoldTime();
                        if (h.peerHoldTime < h.bgpconfig.getHoldTime()) {
                            h.channel.getPipeline().replace("holdTime",
                                                            "holdTime",
                                                            new ReadTimeoutHandler(BgpPipelineFactory.TIMER,
                                                                                   h.peerHoldTime));
                        }

                        log.info("Hold Time : " + h.peerHoldTime);

                        // update AS number
                        h.peerAsNum = pOpenmsg.getAsNumber();
                    }

                    // Send keepalive message to peer.
                    h.sendKeepAliveMessage();
                    h.bgpPacketStats.addOutPacket();
                    h.setState(OPENCONFIRM);
                    h.bgpconfig.setPeerConnState(h.peerAddr, BgpPeerCfg.State.OPENCONFIRM);
                }
            }
        },

        OPENWAIT(false) {
            @Override
            void processBgpMessage(BgpChannelHandler h, BgpMessage m) throws IOException, BgpParseException {
                log.debug("Message received in OPEN WAIT State");

                // check for open message
                if (m.getType() != BgpType.OPEN) {
                    // When the message type is not open message increment the wrong packet statistics
                    h.processUnknownMsg(BgpErrorType.FINITE_STATE_MACHINE_ERROR, BgpErrorType.UNSPECIFIED_ERROR,
                                        m.getType().getType());
                    log.debug("Message is not OPEN message");
                } else {
                    h.bgpPacketStats.addInPacket();

                    BgpOpenMsg pOpenmsg = (BgpOpenMsg) m;
                    h.peerIdentifier = pOpenmsg.getBgpId();

                    // Validate open message
                    if (h.openMsgValidation(h, pOpenmsg)) {
                        if (h.connectionCollisionDetection(BgpPeerCfg.State.OPENSENT,
                                                           h.peerIdentifier, h.peerAddr)) {
                            h.channel.close();
                            return;
                        }
                        log.debug("Sending handshake OPEN message");
                        h.remoteBgpCapability = pOpenmsg.getCapabilityTlv();

                        /*
                         * RFC 4271, section 4.2: Upon receipt of an OPEN message, a BGP speaker MUST calculate the
                         * value of the Hold Timer by using the smaller of its configured Hold Time and the Hold Time
                         * received in the OPEN message
                         */
                        h.peerHoldTime = pOpenmsg.getHoldTime();
                        if (h.peerHoldTime < h.bgpconfig.getHoldTime()) {
                            h.channel.getPipeline().replace("holdTime",
                                                            "holdTime",
                                                            new ReadTimeoutHandler(BgpPipelineFactory.TIMER,
                                                                                   h.peerHoldTime));
                        }

                        log.debug("Hold Time : " + h.peerHoldTime);

                        // update AS number
                        h.peerAsNum = pOpenmsg.getAsNumber();

                        h.sendHandshakeOpenMessage();
                        h.bgpPacketStats.addOutPacket();
                        h.setState(OPENCONFIRM);
                        h.bgpconfig.setPeerConnState(h.peerAddr, BgpPeerCfg.State.OPENCONFIRM);
                    }
                }
            }
        },

        OPENCONFIRM(false) {
            @Override
            void processBgpMessage(BgpChannelHandler h, BgpMessage m) throws IOException, BgpParseException {
                log.debug("Message received in OPENCONFIRM state");
                // check for keep alive message
                if (m.getType() != BgpType.KEEP_ALIVE) {
                    // When the message type is not keep alive message handle the wrong packet
                    h.processUnknownMsg(BgpErrorType.FINITE_STATE_MACHINE_ERROR,
                                        BgpErrorType.RECEIVE_UNEXPECTED_MESSAGE_IN_OPENCONFIRM_STATE,
                                        m.getType().getType());
                    log.debug("Message is not KEEPALIVE message");
                } else {

                    // Set the peer connected status
                    h.bgpPacketStats.addInPacket();
                    log.debug("Sending keep alive message in OPENCONFIRM state");

                    final InetSocketAddress inetAddress = (InetSocketAddress) h.address;
                    h.thisbgpId = BgpId.bgpId(IpAddress.valueOf(inetAddress.getAddress()));

                    // set session parameters
                    h.negotiatedHoldTime = (h.peerHoldTime < h.bgpconfig.getHoldTime()) ? h.peerHoldTime
                                                                                        : h.bgpconfig.getHoldTime();
                    h.sessionInfo = new BgpSessionInfoImpl(h.thisbgpId, h.bgpVersion, h.peerAsNum, h.peerHoldTime,
                                                           h.peerIdentifier, h.negotiatedHoldTime, h.isIbgpSession,
                                                           h.remoteBgpCapability);

                    h.bgpPeer = h.peerManager.getBgpPeerInstance(h.bgpController, h.sessionInfo, h.bgpPacketStats);
                    // set the status of bgp as connected
                    h.bgpPeer.setConnected(true);
                    h.bgpPeer.setChannel(h.channel);

                    /*
                     * RFC 4271, When an OPEN message is received, sends a KEEPALIVE message, If the negotiated hold
                     * time value is zero, then the HoldTimer and KeepaliveTimer are not started. A reasonable maximum
                     * time between KEEPALIVE messages would be one third of the Hold Time interval.
                     */

                    if (h.negotiatedHoldTime != 0) {
                        h.keepAliveTimer = new BgpKeepAliveTimer(h,
                                                                (h.negotiatedHoldTime / BGP_MAX_KEEPALIVE_INTERVAL));
                    } else {
                        h.sendKeepAliveMessage();
                    }

                    h.bgpPacketStats.addOutPacket();

                    // set the state handshake completion.
                    h.setHandshakeComplete(true);

                    if (!h.peerManager.addConnectedPeer(h.thisbgpId, h.bgpPeer)) {
                        disconnectDuplicate(h);
                    } else {
                        h.setState(ESTABLISHED);
                        h.bgpconfig.setPeerConnState(h.peerAddr, BgpPeerCfg.State.ESTABLISHED);
                    }
                }
            }
        },

        ESTABLISHED(true) {
            @Override
            void processBgpMessage(BgpChannelHandler h, BgpMessage m) throws IOException, BgpParseException {
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
        protected void disconnectDuplicate(BgpChannelHandler h) {
            log.error("Duplicated BGP IP or incompleted cleanup - " + "" + "disconnecting channel {}",
                      h.getPeerInfoString());
            h.duplicateBgpIdFound = Boolean.TRUE;
            h.channel.disconnect();
        }

        // set handshake completion status
        public void setHandshakeComplete(boolean handshakeComplete) {
            this.handshakeComplete = handshakeComplete;
        }

        void processBgpMessage(BgpChannelHandler bgpChannelHandler, BgpMessage pm)
                throws IOException, BgpParseException {
            // TODO Auto-generated method stub
            log.debug("BGP message stub");
        }

    }

    //Stop keepalive timer
    private void stopKeepAliveTimer() {
        if ((keepAliveTimer != null) && (keepAliveTimer.getKeepAliveTimer() != null)) {
            keepAliveTimer.getKeepAliveTimer().cancel();
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
        if (bgpconfig.getState() != BgpCfg.State.IP_AS_CONFIGURED) {
            sendNotification(BgpErrorType.CEASE, BgpErrorType.CONNECTION_REJECTED, null);
            channel.close();
            log.info("BGP local AS and router ID not configured");
            return;
        }

        inetAddress = (InetSocketAddress) address;
        peerAddr = IpAddress.valueOf(inetAddress.getAddress()).toString();

        // if peer is not configured disconnect session
        if (!bgpconfig.isPeerConfigured(peerAddr)) {
            log.debug("Peer is not configured {}", peerAddr);
            sendNotification(BgpErrorType.CEASE, BgpErrorType.CONNECTION_REJECTED, null);
            channel.close();
            return;
        }

        // if connection is already established close channel
        if (peerManager.isPeerConnected(BgpId.bgpId(IpAddress.valueOf(peerAddr)))) {
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
            bgpconfig.setPeerConnState(peerAddr, BgpPeerCfg.State.OPENSENT);
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
        peerAddr = IpAddress.valueOf(inetAddress.getAddress()).toString();

        if (thisbgpId != null) {
            if (!duplicateBgpIdFound) {
                // if the disconnected peer (on this ChannelHandler)
                // was not one with a duplicate, it is safe to remove all
                // state for it at the controller. Notice that if the disconnected
                // peer was a duplicate-ip, calling the method below would clear
                // all state for the original peer (with the same ip),
                // which we obviously don't want.
                log.debug("{}:removal called", getPeerInfoString());
                if (bgpPeer != null) {
                    BgpPeerImpl peer = (BgpPeerImpl) bgpPeer;
                    peerManager.removeConnectedPeer(thisbgpId);
                    peer.updateLocalRibOnPeerDisconnect();
                }

                // Retry connection if connection is lost to bgp speaker/peer
                if ((channel != null) && (null != channel.getPipeline().get("ActiveHandler"))) {
                    BgpConnectPeerImpl connectPeer;
                    BgpPeerCfg.State peerCfgState;

                    peerCfgState = bgpconfig.getPeerConnState(peerAddr);
                    // on session disconnect using configuration, do not retry
                    if (!peerCfgState.equals(BgpPeerCfg.State.IDLE)) {
                        log.debug("Connection reset by peer, retry, STATE:{}", peerCfgState);
                        BgpPeerConfig peerConfig = (BgpPeerConfig) bgpconfig.displayPeers(peerAddr);

                        bgpconfig.setPeerConnState(peerAddr, BgpPeerCfg.State.IDLE);
                        connectPeer = new BgpConnectPeerImpl(bgpController, peerAddr, Controller.getBgpPortNum());
                        peerConfig.setConnectPeer(connectPeer);
                    }
                } else {
                    bgpconfig.setPeerConnState(peerAddr, BgpPeerCfg.State.IDLE);
                }
            } else {
                // A duplicate was disconnected on this ChannelHandler,
                // this is the same peer reconnecting, but the original state was
                // not cleaned up - XXX check liveness of original ChannelHandler
                log.debug("{}:duplicate found", getPeerInfoString());
                duplicateBgpIdFound = Boolean.FALSE;
            }

           stopKeepAliveTimer();
        } else {
            bgpconfig.setPeerConnState(peerAddr, BgpPeerCfg.State.IDLE);
            log.warn("No bgp ip in channelHandler registered for " + "disconnected peer {}", getPeerInfoString());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {

        log.error("[exceptionCaught]: " + e.toString());

        if (e.getCause() instanceof ReadTimeoutException) {
            // device timeout
            log.error("Disconnecting device {} due to read timeout", getPeerInfoString());
            sendNotification(BgpErrorType.HOLD_TIMER_EXPIRED, (byte) 0, null);
            state = ChannelState.IDLE;
            stopKeepAliveTimer();
            ctx.getChannel().close();
            return;
        } else if (e.getCause() instanceof ClosedChannelException) {
            log.debug("Channel for bgp {} already closed", getPeerInfoString());
        } else if (e.getCause() instanceof IOException) {
            log.error("Disconnecting peer {} due to IO Error: {}", getPeerInfoString(), e.getCause().getMessage());
            if (log.isDebugEnabled()) {
                // still print stack trace if debug is enabled
                log.debug("StackTrace for previous Exception: ", e.getCause());
            }
            stopKeepAliveTimer();
            ctx.getChannel().close();
        } else if (e.getCause() instanceof BgpParseException) {
            byte[] data = new byte[] {};
            BgpParseException errMsg = (BgpParseException) e.getCause();
            byte errorCode = errMsg.getErrorCode();
            byte errorSubCode = errMsg.getErrorSubCode();
            ChannelBuffer tempCb = errMsg.getData();
            if (tempCb != null) {
                int dataLength = tempCb.readableBytes();
                data = new byte[dataLength];
                tempCb.readBytes(data, 0, dataLength);
            }
            sendNotification(errorCode, errorSubCode, data);
        } else if (e.getCause() instanceof RejectedExecutionException) {
            log.warn("Could not process message: queue full");
        } else {
            stopKeepAliveTimer();
            log.error("Error while processing message from peer " + getPeerInfoString() + "state " + this.state);
            ctx.getChannel().close();
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
            List<BgpMessage> msglist = (List<BgpMessage>) e.getMessage();
            for (BgpMessage pm : msglist) {
                // Do the actual packet processing
                state.processBgpMessage(this, pm);
            }
        } else {
            state.processBgpMessage(this, (BgpMessage) e.getMessage());
        }
    }

    /**
     * Check for connection collision.
     *
     * @param state connection state
     * @param peerIdentifier BGP peer identifier
     * @param peerAddr BGP peer address
     * @return true if bgp spreakers initiated connection
     * @throws BgpParseException on error while procession collision detection
     * @throws IOException on error while procession collision detection
     */
    public boolean connectionCollisionDetection(BgpPeerCfg.State state, int peerIdentifier, String peerAddr)
            throws IOException, BgpParseException {
        /*
         * RFC 4271, Section 6.8, Based on the value of the BGP identifier, a convention is established for detecting
         * which BGP connection is to be preserved when a collision occurs. The convention is to compare the BGP
         * Identifiers of the peers involved in the collision and to retain only the connection initiated by the BGP
         * speaker with the higher-valued BGP Identifier..
         */
        BgpPeerCfg.State currentState = bgpconfig.getPeerConnState(peerAddr);
        if (currentState.equals(state)) {
            if (((Ip4Address.valueOf(bgpconfig.getRouterId())).compareTo(Ip4Address.valueOf(peerIdentifier))) > 0) {
                // send notification
                sendNotification(BgpErrorType.CEASE, BgpErrorType.CONNECTION_COLLISION_RESOLUTION, null);
                log.debug("Connection collision detected, local id: {},  peer id: {}, peer state:{}, in state:{}",
                          (Ip4Address.valueOf(bgpconfig.getRouterId())), (Ip4Address.valueOf(peerIdentifier)),
                          currentState, state);
                return true;
            }
        }

        return false;
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
     * @param m bgp message
     * @throws BgpParseException throw exception
     */
    private void dispatchMessage(BgpMessage m) throws BgpParseException {
        bgpPacketStats.addInPacket();
        bgpController.processBgpPacket(thisbgpId, m);
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
    public BgpPacketStatsImpl getBgpPacketStats() {
        return bgpPacketStats;
    }

    /**
     * Send handshake open message to the peer.
     *
     * @throws IOException, BgpParseException
     */
    private void sendHandshakeOpenMessage() throws IOException, BgpParseException {
        int bgpId;
        BgpCfg.FlowSpec flowSpec = bgpconfig.flowSpecCapability();
        boolean flowSpecStatus = false;
        boolean vpnFlowSpecStatus = false;

        bgpId = Ip4Address.valueOf(bgpconfig.getRouterId()).toInt();

        if (flowSpec == BgpCfg.FlowSpec.IPV4) {
            flowSpecStatus = true;
        } else if (flowSpec == BgpCfg.FlowSpec.VPNV4) {
            vpnFlowSpecStatus = true;
        } else if (flowSpec == BgpCfg.FlowSpec.IPV4_VPNV4) {
            flowSpecStatus = true;
            vpnFlowSpecStatus = true;
        }

        BgpMessage msg = factory4.openMessageBuilder().setAsNumber((short) bgpconfig.getAsNumber())
                .setHoldTime(bgpconfig.getHoldTime()).setBgpId(bgpId)
                .setLsCapabilityTlv(bgpconfig.getLsCapability())
                .setLargeAsCapabilityTlv(bgpconfig.getLargeASCapability())
                .setFlowSpecCapabilityTlv(flowSpecStatus)
                .setVpnFlowSpecCapabilityTlv(vpnFlowSpecStatus)
                .setFlowSpecRpdCapabilityTlv(bgpconfig.flowSpecRpdCapability()).build();
        log.debug("Sending open message to {}", channel.getRemoteAddress());
        channel.write(Collections.singletonList(msg));

    }

    /**
     * Send notification message to peer.
     *
     * @param errorCode error code send in notification
     * @param errorSubCode sub error code send in notification
     * @param data data to send in notification
     * @throws IOException, BgpParseException while building message
     */
    private void sendNotification(byte errorCode, byte errorSubCode, byte[] data)
                                                                           throws IOException, BgpParseException {
        BgpMessage msg = factory4.notificationMessageBuilder().setErrorCode(errorCode)
                                                              .setErrorSubCode(errorSubCode).setData(data).build();
        log.debug("Sending notification message to {}", channel.getRemoteAddress());
        channel.write(Collections.singletonList(msg));
    }

    /**
     * Send keep alive message.
     *
     * @throws IOException when channel is disconnected
     * @throws BgpParseException while building keep alive message
     */
    synchronized void sendKeepAliveMessage() throws IOException, BgpParseException {

        BgpMessage msg = factory4.keepaliveMessageBuilder().build();
        log.debug("Sending keepalive message to {}", channel.getRemoteAddress());
        channel.write(Collections.singletonList(msg));
    }

    /**
     * Process unknown BGP message received.
     *
     * @param errorCode error code
     * @param errorSubCode error sub code
     * @param data message type
     * @throws BgpParseException while processing error messsage
     * @throws IOException while processing error message
     */
    public void processUnknownMsg(byte errorCode, byte errorSubCode, byte data) throws BgpParseException, IOException {
        log.debug("UNKNOWN message received");
        byte[] byteArray = new byte[1];
        byteArray[0] = data;
        sendNotification(errorCode, errorSubCode, byteArray);
        channel.close();
    }

    /**
     * BGP open message validation.
     *
     * @param h channel handler
     * @param openMsg open message
     * @return true if valid message, otherwise false
     * @throws BgpParseException throw exception
     */
    public boolean openMsgValidation(BgpChannelHandler h, BgpOpenMsg openMsg) throws BgpParseException {
        boolean result;

        // Validate BGP ID
        result = bgpIdValidation(openMsg);
        if (!result) {
            throw new BgpParseException(BgpErrorType.OPEN_MESSAGE_ERROR, BgpErrorType.BAD_BGP_IDENTIFIER, null);
        }

        // Validate AS number
        result = asNumberValidation(h, openMsg);
        if (!result) {
            throw new BgpParseException(BgpErrorType.OPEN_MESSAGE_ERROR, BgpErrorType.BAD_PEER_AS, null);
        }

        // Validate hold timer
        if ((openMsg.getHoldTime() != 0) && (openMsg.getHoldTime() < BGP_MIN_HOLDTIME)) {
            throw new BgpParseException(BgpErrorType.OPEN_MESSAGE_ERROR, BgpErrorType.UNACCEPTABLE_HOLD_TIME, null);
        }

        // Validate capabilities
        result = capabilityValidation(h, openMsg);
        return result;
    }

    /**
     * Capability Validation.
     *
     * @param h channel handler
     * @param openmsg open message
     * @return success or failure
     * @throws BgpParseException
     */
    private boolean capabilityValidation(BgpChannelHandler h, BgpOpenMsg openmsg) throws BgpParseException {
        log.debug("capabilityValidation");

        boolean isFourOctetCapabilityExits = false;
        boolean isRpdCapabilityExits = false;
        int capAsNum = 0;
        byte sendReceive = 0;

        List<BgpValueType> capabilityTlv = openmsg.getCapabilityTlv();
        ListIterator<BgpValueType> listIterator = capabilityTlv.listIterator();
        List<BgpValueType> unSupportedCapabilityTlv = new CopyOnWriteArrayList<BgpValueType>();
        ListIterator<BgpValueType> unSupportedCaplistIterator = unSupportedCapabilityTlv.listIterator();
        BgpValueType tempTlv;
        boolean isLargeAsCapabilityCfg = h.bgpconfig.getLargeASCapability();
        boolean isFlowSpecRpdCapabilityCfg = h.bgpconfig.flowSpecRpdCapability();
        boolean isLsCapabilityCfg = h.bgpconfig.getLsCapability();
        boolean isFlowSpecIpv4CapabilityCfg = false;
        boolean isFlowSpecVpnv4CapabilityCfg = false;
        MultiProtocolExtnCapabilityTlv tempCapability;
        boolean isMultiProtocolLsCapability = false;
        boolean isMultiProtocolFlowSpecCapability = false;
        boolean isMultiProtocolVpnFlowSpecCapability = false;
        BgpCfg.FlowSpec flowSpec = h.bgpconfig.flowSpecCapability();

        if (flowSpec == BgpCfg.FlowSpec.IPV4) {
            isFlowSpecIpv4CapabilityCfg = true;
        } else if (flowSpec == BgpCfg.FlowSpec.VPNV4) {
            isFlowSpecVpnv4CapabilityCfg = true;
        } else if (flowSpec == BgpCfg.FlowSpec.IPV4_VPNV4) {
            isFlowSpecIpv4CapabilityCfg = true;
            isFlowSpecVpnv4CapabilityCfg = true;
        }

        while (listIterator.hasNext()) {
            BgpValueType tlv = listIterator.next();
            if (tlv.getType() == MULTI_PROTOCOL_EXTN_CAPA_TYPE) {
                tempCapability = (MultiProtocolExtnCapabilityTlv) tlv;
                if (Constants.SAFI_FLOWSPEC_VALUE == tempCapability.getSafi()) {
                    isMultiProtocolFlowSpecCapability = true;
                }

                if (Constants.VPN_SAFI_FLOWSPEC_VALUE == tempCapability.getSafi()) {
                    isMultiProtocolVpnFlowSpecCapability = true;
                }

                if (SAFI == tempCapability.getSafi()) {
                    isMultiProtocolLsCapability = true;
                }
            }
            if (tlv.getType() == FOUR_OCTET_AS_NUM_CAPA_TYPE) {
                isFourOctetCapabilityExits = true;
                capAsNum = ((FourOctetAsNumCapabilityTlv) tlv).getInt();
            }

            if (tlv.getType() == RpdCapabilityTlv.TYPE) {
                isRpdCapabilityExits = true;
                sendReceive = ((RpdCapabilityTlv) tlv).sendReceive();
            }
        }

        if (isFourOctetCapabilityExits) {
            if (capAsNum > MAX_AS2_NUM) {
                if (openmsg.getAsNumber() != AS_TRANS) {
                    throw new BgpParseException(BgpErrorType.OPEN_MESSAGE_ERROR, BgpErrorType.BAD_PEER_AS, null);
                }
            } else {
                if (capAsNum != openmsg.getAsNumber()) {
                    throw new BgpParseException(BgpErrorType.OPEN_MESSAGE_ERROR, BgpErrorType.BAD_PEER_AS, null);
                }
            }
        }

        if (isRpdCapabilityExits) {
            if (sendReceive > 2) {
                throw new BgpParseException(BgpErrorType.OPEN_MESSAGE_ERROR, BgpErrorType.UNSUPPORTED_CAPABILITY, null);
            }
        }

        if ((isLsCapabilityCfg)) {
            if (!isMultiProtocolLsCapability) {
                tempTlv = new MultiProtocolExtnCapabilityTlv(AFI, RES, SAFI);
                unSupportedCapabilityTlv.add(tempTlv);
            }
        }

        if (isFlowSpecIpv4CapabilityCfg) {
            if (!isMultiProtocolFlowSpecCapability) {
                tempTlv = new MultiProtocolExtnCapabilityTlv(Constants.AFI_FLOWSPEC_VALUE,
                                                             RES, Constants.SAFI_FLOWSPEC_VALUE);
                unSupportedCapabilityTlv.add(tempTlv);
            }
        }

        if (isFlowSpecVpnv4CapabilityCfg) {
            if (!isMultiProtocolVpnFlowSpecCapability) {
                tempTlv = new MultiProtocolExtnCapabilityTlv(Constants.AFI_FLOWSPEC_VALUE,
                                                             RES, Constants.VPN_SAFI_FLOWSPEC_VALUE);
                unSupportedCapabilityTlv.add(tempTlv);
            }
        }

        if ((isLargeAsCapabilityCfg)) {
            if (!isFourOctetCapabilityExits) {
                tempTlv = new FourOctetAsNumCapabilityTlv(h.bgpconfig.getAsNumber());
                unSupportedCapabilityTlv.add(tempTlv);
            }
        }

        if ((isFlowSpecRpdCapabilityCfg)) {
            if (!isRpdCapabilityExits) {
                tempTlv = new RpdCapabilityTlv(Constants.RPD_CAPABILITY_SEND_VALUE);
                unSupportedCapabilityTlv.add(tempTlv);
            }
        }

        if (unSupportedCapabilityTlv.size() == MAX_UNSUPPORTED_CAPABILITY) {
            ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
            while (unSupportedCaplistIterator.hasNext()) {
                BgpValueType tlv = unSupportedCaplistIterator.next();
                tlv.write(buffer);
            }
            throw new BgpParseException(BgpErrorType.OPEN_MESSAGE_ERROR, BgpErrorType.UNSUPPORTED_CAPABILITY, buffer);
        } else {
            return true;
        }
    }

    /**
     * AS Number Validation.
     *
     * @param h channel Handler
     * @param openMsg open message
     * @return true or false
     */
    private boolean asNumberValidation(BgpChannelHandler h, BgpOpenMsg openMsg) {
        log.debug("AS Num validation");

        int capAsNum = 0;
        boolean isFourOctetCapabilityExits = false;

        BgpPeerCfg peerCfg = h.bgpconfig.displayPeers(peerAddr);
        List<BgpValueType> capabilityTlv = openMsg.getCapabilityTlv();
        ListIterator<BgpValueType> listIterator = capabilityTlv.listIterator();

        while (listIterator.hasNext()) {
            BgpValueType tlv = listIterator.next();
            if (tlv.getType() == FOUR_OCTET_AS_NUM_CAPA_TYPE) {
                isFourOctetCapabilityExits = true;
                capAsNum = ((FourOctetAsNumCapabilityTlv) tlv).getInt();
            }
        }

        if (peerCfg.getAsNumber() > MAX_AS2_NUM) {
            if (openMsg.getAsNumber() != AS_TRANS) {
                return false;
            }

            if (!isFourOctetCapabilityExits) {
                return false;
            }

            if (peerCfg.getAsNumber() != capAsNum) {
                return false;
            }

            isIbgpSession = peerCfg.getIsIBgp();
            if (isIbgpSession) {
                // IBGP - AS number should be same for Peer and local if it is IBGP
                if (h.bgpconfig.getAsNumber() != capAsNum) {
                    return false;
                }
            }
        } else {

            if (openMsg.getAsNumber() != peerCfg.getAsNumber()) {
                return false;
            }

            if (isFourOctetCapabilityExits) {
                if (capAsNum != peerCfg.getAsNumber()) {
                    return false;
                }
            }

            isIbgpSession = peerCfg.getIsIBgp();
            if (isIbgpSession) {
                // IBGP - AS number should be same for Peer and local if it is IBGP
                if (openMsg.getAsNumber() != h.bgpconfig.getAsNumber()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Validates BGP ID.
     *
     * @param openMsg open message
     * @return true or false
     */
    private boolean bgpIdValidation(BgpOpenMsg openMsg) {
        String openMsgBgpId = Ip4Address.valueOf(openMsg.getBgpId()).toString();

        InetAddress ipAddress;
        try {
            ipAddress = InetAddress.getByName(openMsgBgpId);
            if (ipAddress.isMulticastAddress()) {
                return false;
            }
        } catch (UnknownHostException e) {
            log.debug("InetAddress convertion failed");
        }
        return true;
    }
}
