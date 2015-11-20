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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.RejectedExecutionException;

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
import org.onosproject.bgp.controller.BGPCfg;
import org.onosproject.bgp.controller.BGPController;
import org.onosproject.bgp.controller.BGPId;
import org.onosproject.bgp.controller.BGPPeer;
import org.onosproject.bgp.controller.BGPPeerCfg;
import org.onosproject.bgp.controller.impl.BGPControllerImpl.BGPPeerManagerImpl;
import org.onosproject.bgpio.exceptions.BGPParseException;
import org.onosproject.bgpio.protocol.BGPFactory;
import org.onosproject.bgpio.protocol.BGPMessage;
import org.onosproject.bgpio.protocol.BGPOpenMsg;
import org.onosproject.bgpio.protocol.BGPType;
import org.onosproject.bgpio.protocol.BGPVersion;
import org.onosproject.bgpio.types.BGPErrorType;
import org.onosproject.bgpio.types.BGPValueType;
import org.onosproject.bgpio.types.FourOctetAsNumCapabilityTlv;
import org.onosproject.bgpio.types.MultiProtocolExtnCapabilityTlv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Channel handler deals with the bgp peer connection and dispatches messages from peer to the appropriate locations.
 */
class BGPChannelHandler extends IdleStateAwareChannelHandler {

    private static final Logger log = LoggerFactory.getLogger(BGPChannelHandler.class);
    static final int BGP_MIN_HOLDTIME = 3;
    static final int BGP_MAX_KEEPALIVE_INTERVAL = 3;
    private BGPPeer bgpPeer;
    private BGPId thisbgpId;
    private Channel channel;
    private BGPKeepAliveTimer keepAliveTimer = null;
    private short peerHoldTime = 0;
    private short negotiatedHoldTime = 0;
    private long peerAsNum;
    private int peerIdentifier;
    private BGPPacketStatsImpl bgpPacketStats;
    static final int MAX_WRONG_COUNT_PACKET = 5;
    static final byte MULTI_PROTOCOL_EXTN_CAPA_TYPE = 1;
    static final byte FOUR_OCTET_AS_NUM_CAPA_TYPE = 65;
    static final int AS_TRANS = 23456;
    static final int MAX_AS2_NUM = 65535;
    static final short AFI = 16388;
    static final byte RES = 0;
    static final byte SAFI = 71;

    // State needs to be volatile because the HandshakeTimeoutHandler
    // needs to check if the handshake is complete
    private volatile ChannelState state;

    // When a bgp peer with a ip addresss is found (i.e we already have a
    // connected peer with the same ip), the new peer is immediately
    // disconnected. At that point netty callsback channelDisconnected() which
    // proceeds to cleaup peer state - we need to ensure that it does not
    // cleanup
    // peer state for the older (still connected) peer
    private volatile Boolean duplicateBGPIdFound;
    // Indicates the bgp version used by this bgp peer
    protected BGPVersion bgpVersion;
    private BGPController bgpController;
    protected BGPFactory factory4;
    private boolean isIbgpSession;
    private BgpSessionInfoImpl sessionInfo;
    private BGPPeerManagerImpl peerManager;
    private InetSocketAddress inetAddress;
    private IpAddress ipAddress;
    private SocketAddress address;
    private String peerAddr;
    private BGPCfg bgpconfig;

    /**
     * Create a new unconnected BGPChannelHandler.
     *
     * @param bgpController bgp controller
     */
    BGPChannelHandler(BGPController bgpController) {
        this.bgpController = bgpController;
        this.peerManager = (BGPPeerManagerImpl) bgpController.peerManager();
        this.state = ChannelState.IDLE;
        this.factory4 = Controller.getBGPMessageFactory4();
        this.duplicateBGPIdFound = Boolean.FALSE;
        this.bgpPacketStats = new BGPPacketStatsImpl();
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
            void processBGPMessage(BGPChannelHandler h, BGPMessage m) throws IOException, BGPParseException {
                log.debug("message received in OPENSENT state");
                // check for OPEN message
                if (m.getType() != BGPType.OPEN) {
                    // When the message type is not keep alive message increment the wrong packet statistics
                    h.processUnknownMsg(BGPErrorType.FINITE_STATE_MACHINE_ERROR,
                                        BGPErrorType.RECEIVE_UNEXPECTED_MESSAGE_IN_OPENSENT_STATE, m.getType()
                                        .getType());
                    log.debug("Message is not OPEN message");
                } else {
                    log.debug("Sending keep alive message in OPENSENT state");
                    h.bgpPacketStats.addInPacket();

                    BGPOpenMsg pOpenmsg = (BGPOpenMsg) m;
                    h.peerIdentifier = pOpenmsg.getBgpId();

                    // validate capabilities and open msg
                    if (h.openMsgValidation(h, pOpenmsg)) {
                        log.debug("Sending handshake OPEN message");

                        /*
                         * RFC 4271, section 4.2: Upon receipt of an OPEN message, a BGP speaker MUST calculate the
                         * value of the Hold Timer by using the smaller of its configured Hold Time and the Hold Time
                         * received in the OPEN message
                         */
                        h.peerHoldTime = pOpenmsg.getHoldTime();
                        if (h.peerHoldTime < h.bgpconfig.getHoldTime()) {
                            h.channel.getPipeline().replace("holdTime",
                                                            "holdTime",
                                                            new ReadTimeoutHandler(BGPPipelineFactory.TIMER,
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
                    h.processUnknownMsg(BGPErrorType.FINITE_STATE_MACHINE_ERROR, BGPErrorType.UNSPECIFIED_ERROR, m
                                        .getType().getType());
                    log.debug("Message is not OPEN message");
                } else {
                    h.bgpPacketStats.addInPacket();

                    BGPOpenMsg pOpenmsg = (BGPOpenMsg) m;
                    h.peerIdentifier = pOpenmsg.getBgpId();

                    // Validate open message
                    if (h.openMsgValidation(h, pOpenmsg)) {
                        log.debug("Sending handshake OPEN message");

                        /*
                         * RFC 4271, section 4.2: Upon receipt of an OPEN message, a BGP speaker MUST calculate the
                         * value of the Hold Timer by using the smaller of its configured Hold Time and the Hold Time
                         * received in the OPEN message
                         */
                        h.peerHoldTime = pOpenmsg.getHoldTime();
                        if (h.peerHoldTime < h.bgpconfig.getHoldTime()) {
                            h.channel.getPipeline().replace("holdTime",
                                                            "holdTime",
                                                            new ReadTimeoutHandler(BGPPipelineFactory.TIMER,
                                                                                   h.peerHoldTime));
                        }

                        log.debug("Hold Time : " + h.peerHoldTime);

                        // update AS number
                        h.peerAsNum = pOpenmsg.getAsNumber();

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
                    h.processUnknownMsg(BGPErrorType.FINITE_STATE_MACHINE_ERROR,
                                        BGPErrorType.RECEIVE_UNEXPECTED_MESSAGE_IN_OPENCONFIRM_STATE, m.getType()
                                        .getType());
                    log.debug("Message is not KEEPALIVE message");
                } else {

                    // Set the peer connected status
                    h.bgpPacketStats.addInPacket();
                    log.debug("Sending keep alive message in OPENCONFIRM state");

                    final InetSocketAddress inetAddress = (InetSocketAddress) h.address;
                    h.thisbgpId = BGPId.bgpId(IpAddress.valueOf(inetAddress.getAddress()));

                    // set session parameters
                    h.negotiatedHoldTime = (h.peerHoldTime < h.bgpconfig.getHoldTime()) ? h.peerHoldTime
                                                                                        : h.bgpconfig.getHoldTime();
                    h.sessionInfo = new BgpSessionInfoImpl(h.thisbgpId, h.bgpVersion, h.peerAsNum, h.peerHoldTime,
                                                           h.peerIdentifier, h.negotiatedHoldTime, h.isIbgpSession);

                    h.bgpPeer = h.peerManager.getBGPPeerInstance(h.bgpController, h.sessionInfo, h.bgpPacketStats);
                    // set the status of bgp as connected
                    h.bgpPeer.setConnected(true);
                    h.bgpPeer.setChannel(h.channel);

                    /*
                     * RFC 4271, When an OPEN message is received, sends a KEEPALIVE message, If the negotiated hold
                     * time value is zero, then the HoldTimer and KeepaliveTimer are not started. A reasonable maximum
                     * time between KEEPALIVE messages would be one third of the Hold Time interval.
                     */

                    if (h.negotiatedHoldTime != 0) {
                        h.keepAliveTimer = new BGPKeepAliveTimer(h,
                                                                 (h.negotiatedHoldTime / BGP_MAX_KEEPALIVE_INTERVAL));
                    } else {
                        h.sendKeepAliveMessage();
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
        peerAddr = IpAddress.valueOf(inetAddress.getAddress()).toString();

        // if peer is not configured disconnect session
        if (!bgpconfig.isPeerConfigured(peerAddr)) {
            log.debug("Peer is not configured {}", peerAddr);
            channel.close();
            return;
        }

        // if connection is already established close channel
        if (peerManager.isPeerConnected(BGPId.bgpId(IpAddress.valueOf(peerAddr)))) {
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
        peerAddr = IpAddress.valueOf(inetAddress.getAddress()).toString();

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
                sendNotification(BGPErrorType.HOLD_TIMER_EXPIRED, (byte) 0, null);
                channel.close();
                state = ChannelState.IDLE;
                return;
            } else if (ChannelState.OPENCONFIRM == state) {

                // When ReadTimeout timer is expired in OPENCONFIRM state.
                sendNotification(BGPErrorType.HOLD_TIMER_EXPIRED, (byte) 0, null);
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
            byte[] data = new byte[] {};
            BGPParseException errMsg = (BGPParseException) e.getCause();
            byte errorCode = errMsg.getErrorCode();
            byte errorSubCode = errMsg.getErrorSubCode();
            ChannelBuffer tempCb = errMsg.getData();
            if (tempCb != null) {
                int dataLength = tempCb.capacity();
                data = new byte[dataLength];
                tempCb.readBytes(data, 0, dataLength);
            }
            sendNotification(errorCode, errorSubCode, data);
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
     * @param m bgp message
     * @throws BGPParseException throw exception
     */
    private void dispatchMessage(BGPMessage m) throws BGPParseException {
        bgpPacketStats.addInPacket();
        bgpController.processBGPPacket(thisbgpId, m);
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
     * @throws IOException, BGPParseException
     */
    private void sendHandshakeOpenMessage() throws IOException, BGPParseException {
        int bgpId;

        bgpId = Ip4Address.valueOf(bgpconfig.getRouterId()).toInt();
        BGPMessage msg = factory4.openMessageBuilder().setAsNumber((short) bgpconfig.getAsNumber())
                .setHoldTime(bgpconfig.getHoldTime()).setBgpId(bgpId)
                .setLsCapabilityTlv(bgpconfig.getLsCapability())
                .setLargeAsCapabilityTlv(bgpconfig.getLargeASCapability())
                .build();
        log.debug("Sending open message to {}", channel.getRemoteAddress());
        channel.write(Collections.singletonList(msg));

    }

    /**
     * Send notification message to peer.
     *
     * @param errorCode error code send in notification
     * @param errorSubCode sub error code send in notification
     * @param data data to send in notification
     * @throws IOException, BGPParseException while building message
     */
    private void sendNotification(byte errorCode, byte errorSubCode, byte[] data)
            throws IOException, BGPParseException {
        BGPMessage msg = factory4.notificationMessageBuilder().setErrorCode(errorCode).setErrorSubCode(errorSubCode)
                .setData(data).build();
        log.debug("Sending notification message to {}", channel.getRemoteAddress());
        channel.write(Collections.singletonList(msg));
    }

    /**
     * Send keep alive message.
     *
     * @throws IOException when channel is disconnected
     * @throws BGPParseException while building keep alive message
     */
    synchronized void sendKeepAliveMessage() throws IOException, BGPParseException {

        BGPMessage msg = factory4.keepaliveMessageBuilder().build();
        log.debug("Sending keepalive message to {}", channel.getRemoteAddress());
        channel.write(Collections.singletonList(msg));
    }

    /**
     * Process unknown BGP message received.
     *
     * @param errorCode error code
     * @param errorSubCode error sub code
     * @param data message type
     * @throws BGPParseException while processing error messsage
     * @throws IOException while processing error message
     */
    public void processUnknownMsg(byte errorCode, byte errorSubCode, byte data) throws BGPParseException, IOException {
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
     * @throws BGPParseException throw exception
     */
    public boolean openMsgValidation(BGPChannelHandler h, BGPOpenMsg openMsg) throws BGPParseException {
        boolean result;

        // Validate BGP ID
        result = bgpIdValidation(openMsg);
        if (!result) {
            throw new BGPParseException(BGPErrorType.OPEN_MESSAGE_ERROR, BGPErrorType.BAD_BGP_IDENTIFIER, null);
        }

        // Validate AS number
        result = asNumberValidation(h, openMsg);
        if (!result) {
            throw new BGPParseException(BGPErrorType.OPEN_MESSAGE_ERROR, BGPErrorType.BAD_PEER_AS, null);
        }

        // Validate hold timer
        if ((openMsg.getHoldTime() != 0) && (openMsg.getHoldTime() < BGP_MIN_HOLDTIME)) {
            throw new BGPParseException(BGPErrorType.OPEN_MESSAGE_ERROR, BGPErrorType.UNACCEPTABLE_HOLD_TIME, null);
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
     * @throws BGPParseException
     */
    private boolean capabilityValidation(BGPChannelHandler h, BGPOpenMsg openmsg) throws BGPParseException {
        log.debug("capabilityValidation");

        boolean isMultiProtocolcapabilityExists = false;
        boolean isFourOctetCapabilityExits = false;
        int capAsNum = 0;

        List<BGPValueType> capabilityTlv = openmsg.getCapabilityTlv();
        ListIterator<BGPValueType> listIterator = capabilityTlv.listIterator();
        List<BGPValueType> unSupportedCapabilityTlv = new LinkedList<>();
        ListIterator<BGPValueType> unSupportedCaplistIterator = unSupportedCapabilityTlv.listIterator();
        BGPValueType tempTlv;
        boolean isLargeAsCapabilityCfg = h.bgpconfig.getLargeASCapability();
        boolean isLsCapabilityCfg = h.bgpconfig.getLsCapability();

        while (listIterator.hasNext()) {
            BGPValueType tlv = listIterator.next();
            if (tlv.getType() == MULTI_PROTOCOL_EXTN_CAPA_TYPE) {
                isMultiProtocolcapabilityExists = true;
            }
            if (tlv.getType() == FOUR_OCTET_AS_NUM_CAPA_TYPE) {
                isFourOctetCapabilityExits = true;
                capAsNum = ((FourOctetAsNumCapabilityTlv) tlv).getInt();
            }
        }

        if (isFourOctetCapabilityExits) {
            if (capAsNum > MAX_AS2_NUM) {
                if (openmsg.getAsNumber() != AS_TRANS) {
                    throw new BGPParseException(BGPErrorType.OPEN_MESSAGE_ERROR, BGPErrorType.BAD_PEER_AS, null);
                }
            } else {
                if (capAsNum != openmsg.getAsNumber()) {
                    throw new BGPParseException(BGPErrorType.OPEN_MESSAGE_ERROR, BGPErrorType.BAD_PEER_AS, null);
                }
            }
        }

        if ((isLsCapabilityCfg)) {
            if (!isMultiProtocolcapabilityExists) {
                tempTlv = new MultiProtocolExtnCapabilityTlv(AFI, RES, SAFI);
                unSupportedCapabilityTlv.add(tempTlv);
            }
        }

        if ((isLargeAsCapabilityCfg)) {
            if (!isFourOctetCapabilityExits) {
                tempTlv = new FourOctetAsNumCapabilityTlv(h.bgpconfig.getAsNumber());
                unSupportedCapabilityTlv.add(tempTlv);
            }
        }

        if (unSupportedCaplistIterator.hasNext()) {
            ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
            while (unSupportedCaplistIterator.hasNext()) {
                BGPValueType tlv = unSupportedCaplistIterator.next();
                tlv.write(buffer);
            }
            throw new BGPParseException(BGPErrorType.OPEN_MESSAGE_ERROR,
                    BGPErrorType.UNSUPPORTED_CAPABILITY, buffer);
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
    private boolean asNumberValidation(BGPChannelHandler h, BGPOpenMsg openMsg) {
        log.debug("AS Num validation");

        int capAsNum = 0;
        boolean isFourOctetCapabilityExits = false;

        BGPPeerCfg peerCfg = h.bgpconfig.displayPeers(peerAddr);
        List<BGPValueType> capabilityTlv = openMsg.getCapabilityTlv();
        ListIterator<BGPValueType> listIterator = capabilityTlv.listIterator();

        while (listIterator.hasNext()) {
            BGPValueType tlv = listIterator.next();
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
    private boolean bgpIdValidation(BGPOpenMsg openMsg) {
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
