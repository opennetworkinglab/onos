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

package org.onosproject.pcep.controller.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.timeout.IdleState;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutException;
import org.onlab.packet.IpAddress;
import org.onosproject.pcep.controller.ClientCapability;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepCfg;
import org.onosproject.pcep.controller.driver.PcepClientDriver;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepError;
import org.onosproject.pcepio.protocol.PcepErrorInfo;
import org.onosproject.pcepio.protocol.PcepErrorMsg;
import org.onosproject.pcepio.protocol.PcepErrorObject;
import org.onosproject.pcepio.protocol.PcepFactory;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.onosproject.pcepio.protocol.PcepOpenMsg;
import org.onosproject.pcepio.protocol.PcepOpenObject;
import org.onosproject.pcepio.protocol.PcepType;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.onosproject.pcepio.types.IPv4RouterIdOfLocalNodeSubTlv;
import org.onosproject.pcepio.types.NodeAttributesTlv;
import org.onosproject.pcepio.types.PceccCapabilityTlv;
import org.onosproject.pcepio.types.SrPceCapabilityTlv;
import org.onosproject.pcepio.types.StatefulPceCapabilityTlv;
import org.onosproject.pcepio.types.PcepErrorDetailInfo;
import org.onosproject.pcepio.types.PcepValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onosproject.pcep.controller.PcepSyncStatus.NOT_SYNCED;

/**
 * Channel handler deals with the pcc client connection and dispatches
 * messages from client to the appropriate locations.
 */
class PcepChannelHandler extends IdleStateAwareChannelHandler {
    static final byte DEADTIMER_MAXIMUM_VALUE = (byte) 0xFF;
    static final byte KEEPALIVE_MULTIPLE_FOR_DEADTIMER = 4;
    private static final Logger log = LoggerFactory.getLogger(PcepChannelHandler.class);
    private final Controller controller;
    private PcepClientDriver pc;
    private PccId thispccId;
    private Channel channel;
    private byte sessionId = 0;
    private byte keepAliveTime;
    private byte deadTime;
    private ClientCapability capability;
    private PcepPacketStatsImpl pcepPacketStats;
    static final int MAX_WRONG_COUNT_PACKET = 5;
    static final int BYTE_MASK = 0xFF;

    // State needs to be volatile because the HandshakeTimeoutHandler
    // needs to check if the handshake is complete
    private volatile ChannelState state;
    private String peerAddr;
    private SocketAddress address;
    private InetSocketAddress inetAddress;
    // When a pcc client with a ip addresss is found (i.e we already have a
    // connected client with the same ip), the new client is immediately
    // disconnected. At that point netty callsback channelDisconnected() which
    // proceeds to cleaup client state - we need to ensure that it does not cleanup
    // client state for the older (still connected) client
    private volatile Boolean duplicatePccIdFound;

    //Indicates the pcep version used by this pcc client
    protected PcepVersion pcepVersion;
    protected PcepFactory factory1;

    /**
     * Create a new unconnected PcepChannelHandler.
     * @param controller parent controller
     */
    PcepChannelHandler(Controller controller) {
        this.controller = controller;
        this.state = ChannelState.INIT;
        factory1 = controller.getPcepMessageFactory1();
        duplicatePccIdFound = Boolean.FALSE;
        pcepPacketStats = new PcepPacketStatsImpl();
    }

    /**
     * To disconnect a PCC.
     */
    public void disconnectClient() {
        pc.disconnectClient();
    }

    //*************************
    //  Channel State Machine
    //*************************

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        channel = e.getChannel();
        log.info("PCC connected from {}", channel.getRemoteAddress());

        address = channel.getRemoteAddress();
        if (!(address instanceof InetSocketAddress)) {
            throw new IOException("Invalid peer connection.");
        }

        inetAddress = (InetSocketAddress) address;
        peerAddr = IpAddress.valueOf(inetAddress.getAddress()).toString();

        // Wait for open message from pcc client
        setState(ChannelState.OPENWAIT);
        controller.peerStatus(peerAddr, PcepCfg.State.OPENWAIT.toString(), sessionId);
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        log.info("Pcc disconnected callback for pc:{}. Cleaning up ...", getClientInfoString());
        controller.peerStatus(peerAddr, PcepCfg.State.DOWN.toString(), sessionId);

        channel = e.getChannel();
        address = channel.getRemoteAddress();
        if (!(address instanceof InetSocketAddress)) {
            throw new IOException("Invalid peer connection.");
        }

        inetAddress = (InetSocketAddress) address;
        peerAddr = IpAddress.valueOf(inetAddress.getAddress()).toString();

        if (thispccId != null) {
            if (!duplicatePccIdFound) {
                // if the disconnected client (on this ChannelHandler)
                // was not one with a duplicate-dpid, it is safe to remove all
                // state for it at the controller. Notice that if the disconnected
                // client was a duplicate-ip, calling the method below would clear
                // all state for the original client (with the same ip),
                // which we obviously don't want.
                log.debug("{}:removal called", getClientInfoString());
                if (pc != null) {
                    pc.removeConnectedClient();
                }
            } else {
                // A duplicate was disconnected on this ChannelHandler,
                // this is the same client reconnecting, but the original state was
                // not cleaned up - XXX check liveness of original ChannelHandler
                log.debug("{}:duplicate found", getClientInfoString());
                duplicatePccIdFound = Boolean.FALSE;
            }
        } else {
            log.warn("no pccip in channelHandler registered for " + "disconnected client {}", getClientInfoString());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        PcepErrorMsg errMsg;
        log.info("exceptionCaught: " + e.toString());

        if (e.getCause() instanceof ReadTimeoutException) {
            if (ChannelState.OPENWAIT == state) {
                // When ReadTimeout timer is expired in OPENWAIT state, it is considered
                // OpenWait timer.
                errMsg = getErrorMsg(PcepErrorDetailInfo.ERROR_TYPE_1, PcepErrorDetailInfo.ERROR_VALUE_2);
                log.debug("Sending PCEP-ERROR message to PCC.");
                controller.peerExceptions(peerAddr, e.getCause().toString());
                channel.write(Collections.singletonList(errMsg));
                channel.close();
                state = ChannelState.INIT;
                return;
            } else if (ChannelState.KEEPWAIT == state) {
                // When ReadTimeout timer is expired in KEEPWAIT state, is is considered
                // KeepWait timer.
                errMsg = getErrorMsg(PcepErrorDetailInfo.ERROR_TYPE_1, PcepErrorDetailInfo.ERROR_VALUE_7);
                log.debug("Sending PCEP-ERROR message to PCC.");
                controller.peerExceptions(peerAddr, e.getCause().toString());
                channel.write(Collections.singletonList(errMsg));
                channel.close();
                state = ChannelState.INIT;
                return;
            }
        } else if (e.getCause() instanceof ClosedChannelException) {
            controller.peerExceptions(peerAddr, e.getCause().toString());
            log.debug("Channel for pc {} already closed", getClientInfoString());
        } else if (e.getCause() instanceof IOException) {
            controller.peerExceptions(peerAddr, e.getCause().toString());
            log.error("Disconnecting client {} due to IO Error: {}", getClientInfoString(), e.getCause().getMessage());
            if (log.isDebugEnabled()) {
                // still print stack trace if debug is enabled
                log.debug("StackTrace for previous Exception: ", e.getCause());
            }
            channel.close();
        } else if (e.getCause() instanceof PcepParseException) {
            controller.peerExceptions(peerAddr, e.getCause().toString());
            PcepParseException errMsgParse = (PcepParseException) e.getCause();
            byte errorType = errMsgParse.getErrorType();
            byte errorValue = errMsgParse.getErrorValue();

            if ((errorType == (byte) 0x0) && (errorValue == (byte) 0x0)) {
                processUnknownMsg();
            } else {
                errMsg = getErrorMsg(errorType, errorValue);
                log.debug("Sending PCEP-ERROR message to PCC.");
                channel.write(Collections.singletonList(errMsg));
            }
        } else if (e.getCause() instanceof RejectedExecutionException) {
            log.warn("Could not process message: queue full");
            controller.peerExceptions(peerAddr, e.getCause().toString());
        } else {
            log.error("Error while processing message from client " + getClientInfoString() + "state " + this.state);
            controller.peerExceptions(peerAddr, e.getCause().toString());
            channel.close();
        }
    }

    @Override
    public String toString() {
        return getClientInfoString();
    }

    @Override
    public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e) throws Exception {
        if (!isHandshakeComplete()) {
            return;
        }

        if (e.getState() == IdleState.READER_IDLE) {
            // When no message is received on channel for read timeout, then close
            // the channel
            log.info("Disconnecting client {} due to read timeout", getClientInfoString());
            ctx.getChannel().close();
        } else if (e.getState() == IdleState.WRITER_IDLE) {
            // Send keep alive message
            log.debug("Sending keep alive message due to IdleState timeout " + pc.toString());
            pc.sendMessage(Collections.singletonList(pc.factory().buildKeepaliveMsg().build()));
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof List) {
            @SuppressWarnings("unchecked")
            List<PcepMessage> msglist = (List<PcepMessage>) e.getMessage();
            for (PcepMessage pm : msglist) {
                // Do the actual packet processing
                state.processPcepMessage(this, pm);
            }
        } else {
            state.processPcepMessage(this, (PcepMessage) e.getMessage());
        }
    }

    /**
     * Is this a state in which the handshake has completed.
     *
     * @return true if the handshake is complete
     */
    public boolean isHandshakeComplete() {
        return this.state.isHandshakeComplete();
    }

    /**
     * To set the handshake status.
     *
     * @param handshakeComplete value is handshake status
     */
    public void setHandshakeComplete(boolean handshakeComplete) {
        this.state.setHandshakeComplete(handshakeComplete);
    }

    /**
     * To handle the pcep message.
     *
     * @param m pcep message
     */
    private void dispatchMessage(PcepMessage m) {
        pc.handleMessage(m);
    }

    /**
     * Adds PCEP device configuration with capabilities once session is established.
     */
    private void addNode() {
        pc.addNode(pc);
    }

    /**
     * Deletes PCEP device configuration when session is disconnected.
     */
    private void deleteNode() {
        pc.deleteNode(pc.getPccId());
    }

    /**
     * Return a string describing this client based on the already available
     * information (ip address and/or remote socket).
     *
     * @return display string
     */
    private String getClientInfoString() {
        if (pc != null) {
            return pc.toString();
        }
        String channelString;
        if (channel == null || channel.getRemoteAddress() == null) {
            channelString = "?";
        } else {
            channelString = channel.getRemoteAddress().toString();
        }
        String pccIpString;
        // TODO : implement functionality to get pcc id string
        pccIpString = "?";
        return String.format("[%s PCCIP[%s]]", channelString, pccIpString);
    }

    /**
     * Update the channels state. Only called from the state machine.
     *
     * @param state
     */
    private void setState(ChannelState state) {
        this.state = state;
    }

    /**
     * Send handshake open message.
     *
     * @throws IOException,PcepParseException
     */
    private void sendHandshakeOpenMessage() throws IOException, PcepParseException {
        PcepOpenObject pcepOpenobj = factory1.buildOpenObject()
                .setSessionId(sessionId)
                .setKeepAliveTime(keepAliveTime)
                .setDeadTime(deadTime)
                .build();
        PcepMessage msg = factory1.buildOpenMsg()
                .setPcepOpenObj(pcepOpenobj)
                .build();
        log.debug("Sending OPEN message to {}", channel.getRemoteAddress());
        channel.write(Collections.singletonList(msg));
    }

    //Capability negotiation
    private void capabilityNegotiation(PcepOpenMsg pOpenmsg) {
        LinkedList<PcepValueType> tlvList = pOpenmsg.getPcepOpenObject().getOptionalTlv();
        boolean pceccCapability = false;
        boolean statefulPceCapability = false;
        boolean pcInstantiationCapability = false;
        boolean labelStackCapability = false;
        boolean srCapability = false;

        ListIterator<PcepValueType> listIterator = tlvList.listIterator();
        while (listIterator.hasNext()) {
            PcepValueType tlv = listIterator.next();

            switch (tlv.getType()) {
                case PceccCapabilityTlv.TYPE:
                    pceccCapability = true;
                    if (((PceccCapabilityTlv) tlv).sBit()) {
                        labelStackCapability = true;
                    }
                    break;
                case StatefulPceCapabilityTlv.TYPE:
                    statefulPceCapability = true;
                    StatefulPceCapabilityTlv stetefulPcCapTlv = (StatefulPceCapabilityTlv) tlv;
                    if (stetefulPcCapTlv.getIFlag()) {
                        pcInstantiationCapability = true;
                    }
                    break;
                case SrPceCapabilityTlv.TYPE:
                    srCapability = true;
                    break;
                default:
                    continue;
            }
        }
        this.capability = new ClientCapability(pceccCapability, statefulPceCapability, pcInstantiationCapability,
                labelStackCapability, srCapability);
    }

    /**
     * Send keep alive message.
     *
     * @throws IOException        when channel is disconnected
     * @throws PcepParseException while building keep alive message
     */
    private void sendKeepAliveMessage() throws IOException, PcepParseException {
        PcepMessage msg = factory1.buildKeepaliveMsg().build();
        log.debug("Sending KEEPALIVE message to {}", channel.getRemoteAddress());
        channel.write(Collections.singletonList(msg));
    }

    /**
     * Send error message and close channel with pcc.
     */
    private void sendErrMsgAndCloseChannel() {
        // TODO send error message
        //Remove PCEP device from topology
        deleteNode();
        channel.close();
    }

    /**
     * Send error message when an invalid message is received.
     *
     * @throws PcepParseException while building error message
     */
    private void sendErrMsgForInvalidMsg() throws PcepParseException {
        byte errorType = 0x02;
        byte errorValue = 0x00;
        PcepErrorMsg errMsg = getErrorMsg(errorType, errorValue);
        channel.write(Collections.singletonList(errMsg));
    }

    /**
     * Builds pcep error message based on error value and error type.
     *
     * @param errorType  pcep error type
     * @param errorValue pcep error value
     * @return pcep error message
     * @throws PcepParseException while bulding error message
     */
    public PcepErrorMsg getErrorMsg(byte errorType, byte errorValue) throws PcepParseException {
        LinkedList<PcepErrorObject> llerrObj = new LinkedList<>();
        PcepErrorMsg errMsg;

        PcepErrorObject errObj = factory1.buildPcepErrorObject()
                .setErrorValue(errorValue)
                .setErrorType(errorType)
                .build();

        llerrObj.add(errObj);

        //If Error caught in other than Openmessage
        LinkedList<PcepError> llPcepErr = new LinkedList<>();

        PcepError pcepErr = factory1.buildPcepError()
                .setErrorObjList(llerrObj)
                .build();

        llPcepErr.add(pcepErr);

        PcepErrorInfo errInfo = factory1.buildPcepErrorInfo()
                .setPcepErrorList(llPcepErr)
                .build();

        errMsg = factory1.buildPcepErrorMsg()
                .setPcepErrorInfo(errInfo)
                .build();
        return errMsg;
    }

    /**
     * Process unknown pcep message received.
     *
     * @throws PcepParseException while building pcep error message
     */
    public void processUnknownMsg() throws PcepParseException {
        Date now = null;
        if (pcepPacketStats.wrongPacketCount() == 0) {
            now = new Date();
            pcepPacketStats.setTime(now.getTime());
            pcepPacketStats.addWrongPacket();
            sendErrMsgForInvalidMsg();
        }

        if (pcepPacketStats.wrongPacketCount() > 1) {
            Date lastest = new Date();
            pcepPacketStats.addWrongPacket();
            //converting to seconds
            if (((lastest.getTime() - pcepPacketStats.getTime()) / 1000) > 60) {
                now = lastest;
                pcepPacketStats.setTime(now.getTime());
                pcepPacketStats.resetWrongPacket();
                pcepPacketStats.addWrongPacket();
            } else if (((int) (lastest.getTime() - now.getTime()) / 1000) < 60) {
                if (MAX_WRONG_COUNT_PACKET <= pcepPacketStats.wrongPacketCount()) {
                    //reset once wrong packet count reaches MAX_WRONG_COUNT_PACKET
                    pcepPacketStats.resetWrongPacket();
                    // max wrong packets received send error message and close the session
                    sendErrMsgAndCloseChannel();
                }
            }
        }
    }

    /**
     * The state machine for handling the client/channel state. All state
     * transitions should happen from within the state machine (and not from other
     * parts of the code)
     */
    enum ChannelState {
        /**
         * Initial state before channel is connected.
         */
        INIT(false) {

        },
        /**
         * Once the session is established, wait for open message.
         */
        OPENWAIT(false) {
            @Override
            void processPcepMessage(PcepChannelHandler h, PcepMessage m) throws IOException, PcepParseException {

                log.info("Message received in OPEN WAIT State");

                //check for open message
                if (m.getType() != PcepType.OPEN) {
                    // When the message type is not open message increment the wrong packet statistics
                    h.processUnknownMsg();
                    log.debug("Message is not OPEN message");
                } else {

                    h.pcepPacketStats.addInPacket();
                    PcepOpenMsg pOpenmsg = (PcepOpenMsg) m;
                    //Do Capability negotiation.
                    h.capabilityNegotiation(pOpenmsg);
                    log.debug("Sending handshake OPEN message");
                    h.sessionId = pOpenmsg.getPcepOpenObject().getSessionId();
                    h.pcepVersion = pOpenmsg.getPcepOpenObject().getVersion();

                    //setting keepalive and deadTimer
                    byte yKeepalive = pOpenmsg.getPcepOpenObject().getKeepAliveTime();
                    byte yDeadTimer = pOpenmsg.getPcepOpenObject().getDeadTime();
                    h.keepAliveTime = yKeepalive;
                    if (yKeepalive < yDeadTimer) {
                        h.deadTime = yDeadTimer;
                    } else {
                        if (DEADTIMER_MAXIMUM_VALUE > (yKeepalive * KEEPALIVE_MULTIPLE_FOR_DEADTIMER)) {
                            h.deadTime = (byte) (yKeepalive * KEEPALIVE_MULTIPLE_FOR_DEADTIMER);
                        } else {
                            h.deadTime = DEADTIMER_MAXIMUM_VALUE;
                        }
                    }

                        /*
                         * If MPLS LSR id and PCEP session socket IP addresses are not same,
                         * the MPLS LSR id will be encoded in separate TLV.
                         * We always maintain session information based on LSR ids.
                         * The socket IP is stored in channel.
                         */
                    LinkedList<PcepValueType> optionalTlvs = pOpenmsg.getPcepOpenObject().getOptionalTlv();
                    if (optionalTlvs != null) {
                        for (PcepValueType optionalTlv : optionalTlvs) {
                            if (optionalTlv instanceof NodeAttributesTlv) {
                                List<PcepValueType> subTlvs = ((NodeAttributesTlv) optionalTlv)
                                        .getllNodeAttributesSubTLVs();
                                if (subTlvs == null) {
                                    break;
                                }
                                for (PcepValueType subTlv : subTlvs) {
                                    if (subTlv instanceof IPv4RouterIdOfLocalNodeSubTlv) {
                                        h.thispccId = PccId.pccId(IpAddress
                                                .valueOf(((IPv4RouterIdOfLocalNodeSubTlv) subTlv).getInt()));
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                    }

                    if (h.thispccId == null) {
                        final SocketAddress address = h.channel.getRemoteAddress();
                        if (!(address instanceof InetSocketAddress)) {
                            throw new IOException("Invalid client connection. Pcc is indentifed based on IP");
                        }

                        final InetSocketAddress inetAddress = (InetSocketAddress) address;
                        h.thispccId = PccId.pccId(IpAddress.valueOf(inetAddress.getAddress()));
                    }

                    h.sendHandshakeOpenMessage();
                    h.pcepPacketStats.addOutPacket();
                    h.setState(KEEPWAIT);
                    h.controller.peerStatus(h.peerAddr.toString(), PcepCfg.State.KEEPWAIT.toString(), h.sessionId);
                }
            }
        },
        /**
         * Once the open messages are exchanged, wait for keep alive message.
         */
        KEEPWAIT(false) {
            @Override
            void processPcepMessage(PcepChannelHandler h, PcepMessage m) throws IOException, PcepParseException {
                log.info("Message received in KEEPWAIT state");
                //check for keep alive message
                if (m.getType() != PcepType.KEEP_ALIVE) {
                    // When the message type is not keep alive message increment the wrong packet statistics
                    h.processUnknownMsg();
                    log.error("Message is not KEEPALIVE message");
                } else {
                    // Set the client connected status
                    h.pcepPacketStats.addInPacket();
                    log.debug("sending keep alive message in KEEPWAIT state");
                    h.pc = h.controller.getPcepClientInstance(h.thispccId, h.sessionId, h.pcepVersion,
                            h.pcepPacketStats);
                    //Get pc instance and set capabilities
                    h.pc.setCapability(h.capability);

                    // Initilialize DB sync status.
                    h.pc.setLspDbSyncStatus(NOT_SYNCED);
                    h.pc.setLabelDbSyncStatus(NOT_SYNCED);

                    // set the status of pcc as connected
                    h.pc.setConnected(true);
                    h.pc.setChannel(h.channel);

                    // set any other specific parameters to the pcc
                    h.pc.setPcVersion(h.pcepVersion);
                    h.pc.setPcSessionId(h.sessionId);
                    h.pc.setPcKeepAliveTime(h.keepAliveTime);
                    h.pc.setPcDeadTime(h.deadTime);
                    int keepAliveTimer = h.keepAliveTime & BYTE_MASK;
                    int deadTimer = h.deadTime & BYTE_MASK;
                    if (0 == h.keepAliveTime) {
                        h.deadTime = 0;
                    }
                    // handle keep alive and dead time
                    if (keepAliveTimer != PcepPipelineFactory.DEFAULT_KEEP_ALIVE_TIME
                            || deadTimer != PcepPipelineFactory.DEFAULT_DEAD_TIME) {

                        h.channel.getPipeline().replace("idle", "idle",
                                new IdleStateHandler(PcepPipelineFactory.TIMER, deadTimer, keepAliveTimer, 0));
                    }
                    log.debug("Dead timer : " + deadTimer);
                    log.debug("Keep alive time : " + keepAliveTimer);

                    //set the state handshake completion.

                    h.sendKeepAliveMessage();
                    h.pcepPacketStats.addOutPacket();
                    h.setHandshakeComplete(true);

                    if (!h.pc.connectClient()) {
                        disconnectDuplicate(h);
                    } else {
                        h.setState(ESTABLISHED);
                     h.controller.peerStatus(h.peerAddr.toString(), PcepCfg.State.ESTABLISHED.toString(), h.sessionId);
                        //Session is established, add a network configuration with LSR id and device capabilities.
                        h.addNode();
                    }
                }
            }
        },
        /**
         * Once the keep alive messages are exchanged, the state is established.
         */
        ESTABLISHED(true) {
            @Override
            void processPcepMessage(PcepChannelHandler h, PcepMessage m) throws IOException, PcepParseException {

                //h.channel.getPipeline().remove("waittimeout");
                log.debug("Message received in established state " + m.getType());
                //dispatch the message
                h.dispatchMessage(m);
            }
        };
        private boolean handshakeComplete;

        ChannelState(boolean handshakeComplete) {
            this.handshakeComplete = handshakeComplete;
        }

        void processPcepMessage(PcepChannelHandler h, PcepMessage m) throws IOException, PcepParseException {
            // do nothing
        }

        /**
         * Is this a state in which the handshake has completed.
         *
         * @return true if the handshake is complete
         */
        public boolean isHandshakeComplete() {
            return this.handshakeComplete;
        }

        /**
         * Sets handshake complete status.
         *
         * @param handshakeComplete status of handshake
         */
        public void setHandshakeComplete(boolean handshakeComplete) {
            this.handshakeComplete = handshakeComplete;
        }

        protected void disconnectDuplicate(PcepChannelHandler h) {
            log.error("Duplicated Pcc IP or incompleted cleanup - " + "disconnecting channel {}",
                    h.getClientInfoString());
            h.duplicatePccIdFound = Boolean.TRUE;
            h.channel.disconnect();
        }

    }
}
