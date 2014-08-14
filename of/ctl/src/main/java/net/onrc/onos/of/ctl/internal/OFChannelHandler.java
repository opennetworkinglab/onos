package net.onrc.onos.of.ctl.internal;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;

import net.onrc.onos.of.ctl.IOFSwitch;
import net.onrc.onos.of.ctl.IOFSwitch.PortChangeEvent;
import net.onrc.onos.of.ctl.Role;
import net.onrc.onos.of.ctl.annotations.LogMessageDoc;
import net.onrc.onos.of.ctl.annotations.LogMessageDocs;
import net.onrc.onos.of.ctl.debugcounter.IDebugCounterService.CounterException;
import net.onrc.onos.of.ctl.internal.Controller.Counters;
import net.onrc.onos.of.ctl.internal.OFChannelHandler.ChannelState.RoleReplyInfo;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.jboss.netty.handler.timeout.ReadTimeoutException;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFAsyncGetReply;
import org.projectfloodlight.openflow.protocol.OFBadRequestCode;
import org.projectfloodlight.openflow.protocol.OFBarrierReply;
import org.projectfloodlight.openflow.protocol.OFBarrierRequest;
import org.projectfloodlight.openflow.protocol.OFControllerRole;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFDescStatsRequest;
import org.projectfloodlight.openflow.protocol.OFEchoReply;
import org.projectfloodlight.openflow.protocol.OFEchoRequest;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFErrorType;
import org.projectfloodlight.openflow.protocol.OFExperimenter;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFFlowModFailedCode;
import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.protocol.OFGetConfigReply;
import org.projectfloodlight.openflow.protocol.OFGetConfigRequest;
import org.projectfloodlight.openflow.protocol.OFHello;
import org.projectfloodlight.openflow.protocol.OFHelloElem;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFNiciraControllerRole;
import org.projectfloodlight.openflow.protocol.OFNiciraControllerRoleReply;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsRequest;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFQueueGetConfigReply;
import org.projectfloodlight.openflow.protocol.OFRoleReply;
import org.projectfloodlight.openflow.protocol.OFRoleRequest;
import org.projectfloodlight.openflow.protocol.OFSetConfig;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsReplyFlags;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.errormsg.OFBadRequestErrorMsg;
import org.projectfloodlight.openflow.protocol.errormsg.OFFlowModFailedErrorMsg;
import org.projectfloodlight.openflow.protocol.errormsg.OFRoleRequestFailedErrorMsg;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Channel handler deals with the switch connection and dispatches
 * switch messages to the appropriate locations.
 */
class OFChannelHandler extends IdleStateAwareChannelHandler {
    private static final Logger log = LoggerFactory.getLogger(OFChannelHandler.class);
    private static final long DEFAULT_ROLE_TIMEOUT_MS = 2 * 1000; // 10 sec
    private final Controller controller;
    private final Counters counters;
    private IOFSwitch sw;
    private long thisdpid; // channelHandler cached value of connected switch id
    private Channel channel;
    // State needs to be volatile because the HandshakeTimeoutHandler
    // needs to check if the handshake is complete
    private volatile ChannelState state;

    // All role messaging is handled by the roleChanger. The channel state machine
    // coordinates between the roleChanger and the controller-global-registry-service
    // to determine controller roles per switch.
    private RoleChanger roleChanger;
    // Used to coordinate between the controller and the cleanup thread(?)
    // for access to the global registry on a per switch basis.
    volatile Boolean controlRequested;
    // When a switch with a duplicate dpid is found (i.e we already have a
    // connected switch with the same dpid), the new switch is immediately
    // disconnected. At that point netty callsback channelDisconnected() which
    // proceeds to cleaup switch state - we need to ensure that it does not cleanup
    // switch state for the older (still connected) switch
    private volatile Boolean duplicateDpidFound;

    // Temporary storage for switch-features and port-description
    private OFFeaturesReply featuresReply;
    private OFPortDescStatsReply portDescReply;
    // a concurrent ArrayList to temporarily store port status messages
    // before we are ready to deal with them
    private final CopyOnWriteArrayList<OFPortStatus> pendingPortStatusMsg;

    //Indicates the openflow version used by this switch
    protected OFVersion ofVersion;
    protected OFFactory factory13;
    protected OFFactory factory10;

    /** transaction Ids to use during handshake. Since only one thread
     * calls into an OFChannelHandler instance, we don't need atomic.
     * We will count down
     */
    private int handshakeTransactionIds = -1;

    /**
     * Create a new unconnected OFChannelHandler.
     * @param controller
     */
    OFChannelHandler(Controller controller) {
        this.controller = controller;
        this.counters = controller.getCounters();
        this.roleChanger = new RoleChanger(DEFAULT_ROLE_TIMEOUT_MS);
        this.state = ChannelState.INIT;
        this.pendingPortStatusMsg = new CopyOnWriteArrayList<OFPortStatus>();
        factory13 = controller.getOFMessageFactory13();
        factory10 = controller.getOFMessageFactory10();
        controlRequested = Boolean.FALSE;
        duplicateDpidFound = Boolean.FALSE;
    }

    //*******************
    //  Role Handling
    //*******************

    /**
     * When we remove a pending role request we use this enum to indicate how we
     * arrived at the decision. When we send a role request to the switch, we
     * also use  this enum to indicate what we expect back from the switch, so the
     * role changer can match the reply to our expectation.
     */
    public enum RoleRecvStatus {
        /** The switch returned an error indicating that roles are not.
         * supported*/
        UNSUPPORTED,
        /** The request timed out. */
        NO_REPLY,
        /** The reply was old, there is a newer request pending. */
        OLD_REPLY,
        /**
         *  The reply's role matched the role that this controller set in the
         *  request message - invoked either initially at startup or to reassert
         *  current role.
         */
        MATCHED_CURRENT_ROLE,
        /**
         *  The reply's role matched the role that this controller set in the
         *  request message - this is the result of a callback from the
         *  global registry, followed by a role request sent to the switch.
         */
        MATCHED_SET_ROLE,
        /**
         * The reply's role was a response to the query made by this controller.
         */
        REPLY_QUERY,
        /** We received a role reply message from the switch
         *  but the expectation was unclear, or there was no expectation.
         */
        OTHER_EXPECTATION,
    }

    /**
     * Forwards to RoleChanger. See there.
     * @param role
     */
    public void sendRoleRequest(Role role, RoleRecvStatus expectation) {
        try {
            roleChanger.sendRoleRequest(role, expectation);
        } catch (IOException e) {
            log.error("Disconnecting switch {} due to IO Error: {}",
                    getSwitchInfoString(), e.getMessage());
            channel.close();
        }
    }

    // XXX S consider if necessary
    public void disconnectSwitch() {
        sw.disconnectSwitch();
    }

    /**
     * A utility class to handle role requests and replies for this channel.
     * After a role request is submitted the role changer keeps track of the
     * pending request, collects the reply (if any) and times out the request
     * if necessary.
     *
     * To simplify role handling we only keep track of the /last/ pending
     * role reply send to the switch. If multiple requests are pending and
     * we receive replies for earlier requests we ignore them. However, this
     * way of handling pending requests implies that we could wait forever if
     * a new request is submitted before the timeout triggers. If necessary
     * we could work around that though.
     */
    private class RoleChanger {
        // indicates that a request is currently pending
        // needs to be volatile to allow correct double-check idiom
        private volatile boolean requestPending;
        // the transaction Id of the pending request
        private int pendingXid;
        // the role that's pending
        private Role pendingRole;
        // system time in MS when we send the request
        private long roleSubmitTime;
        // the timeout to use
        private final long roleTimeoutMs;
        // the expectation set by the caller for the returned role
        private RoleRecvStatus expectation;

        public RoleChanger(long roleTimeoutMs) {
            this.requestPending = false;
            this.roleSubmitTime = 0;
            this.pendingXid = -1;
            this.pendingRole = null;
            this.roleTimeoutMs = roleTimeoutMs;
            this.expectation = RoleRecvStatus.MATCHED_CURRENT_ROLE;
        }

        /**
         * Send NX role request message to the switch requesting the specified
         * role.
         *
         * @param sw switch to send the role request message to
         * @param role role to request
         */
        private int sendNxRoleRequest(Role role) throws IOException {
            // Convert the role enum to the appropriate role to send
            OFNiciraControllerRole roleToSend = OFNiciraControllerRole.ROLE_OTHER;
            switch (role) {
            case MASTER:
                roleToSend = OFNiciraControllerRole.ROLE_MASTER;
                break;
            case SLAVE:
            case EQUAL:
            default:
                // ensuring that the only two roles sent to 1.0 switches with
                // Nicira role support, are MASTER and SLAVE
                roleToSend = OFNiciraControllerRole.ROLE_SLAVE;
                log.warn("Sending Nx Role.SLAVE to switch {}.", sw);
            }
            int xid = sw.getNextTransactionId();
            OFExperimenter roleRequest = factory10
                    .buildNiciraControllerRoleRequest()
                    .setXid(xid)
                    .setRole(roleToSend)
                    .build();
            sw.write(Collections.<OFMessage>singletonList(roleRequest));
            return xid;
        }

        private int sendOF13RoleRequest(Role role) throws IOException {
            // Convert the role enum to the appropriate role to send
            OFControllerRole roleToSend = OFControllerRole.ROLE_NOCHANGE;
            switch (role) {
            case EQUAL:
                roleToSend = OFControllerRole.ROLE_EQUAL;
                break;
            case MASTER:
                roleToSend = OFControllerRole.ROLE_MASTER;
                break;
            case SLAVE:
                roleToSend = OFControllerRole.ROLE_SLAVE;
                break;
            default:
                log.warn("Sending default role.noChange to switch {}."
                        + " Should only be used for queries.", sw);
            }

            int xid = sw.getNextTransactionId();
            OFRoleRequest rrm = factory13
                    .buildRoleRequest()
                    .setRole(roleToSend)
                    .setXid(xid)
                    .setGenerationId(sw.getNextGenerationId())
                    .build();
            sw.write(rrm);
            return xid;
        }

        /**
         * Send a role request with the given role to the switch and update
         * the pending request and timestamp.
         * Sends an OFPT_ROLE_REQUEST to an OF1.3 switch, OR
         * Sends an NX_ROLE_REQUEST to an OF1.0 switch if configured to support it
         * in the IOFSwitch driver. If not supported, this method sends nothing
         * and returns 'false'. The caller should take appropriate action.
         *
         * One other optimization we do here is that for OF1.0 switches with
         * Nicira role message support, we force the Role.EQUAL to become
         * Role.SLAVE, as there is no defined behavior for the Nicira role OTHER.
         * We cannot expect it to behave like SLAVE. We don't have this problem with
         * OF1.3 switches, because Role.EQUAL is well defined and we can simulate
         * SLAVE behavior by using ASYNC messages.
         *
         * @param role
         * @throws IOException
         * @returns false if and only if the switch does not support role-request
         * messages, according to the switch driver; true otherwise.
         */
        synchronized boolean sendRoleRequest(Role role, RoleRecvStatus exp)
                throws IOException {
            this.expectation = exp;

            if (ofVersion == OFVersion.OF_10) {
                Boolean supportsNxRole = (Boolean)
                        sw.getAttribute(IOFSwitch.SWITCH_SUPPORTS_NX_ROLE);
                if (!supportsNxRole) {
                    log.debug("Switch driver indicates no support for Nicira "
                            + "role request messages. Not sending ...");
                    state.handleUnsentRoleMessage(OFChannelHandler.this, role,
                            expectation);
                    return false;
                }
                // OF1.0 switch with support for NX_ROLE_REQUEST vendor extn.
                // make Role.EQUAL become Role.SLAVE
                role = (role == Role.EQUAL) ? Role.SLAVE : role;
                pendingXid = sendNxRoleRequest(role);
                pendingRole = role;
                roleSubmitTime = System.currentTimeMillis();
                requestPending = true;
            } else {
                // OF1.3 switch, use OFPT_ROLE_REQUEST message
                pendingXid = sendOF13RoleRequest(role);
                pendingRole = role;
                roleSubmitTime = System.currentTimeMillis();
                requestPending = true;
            }
            return true;
        }

        /**
         * Deliver a received role reply.
         *
         * Check if a request is pending and if the received reply matches the
         * the expected pending reply (we check both role and xid) we set
         * the role for the switch/channel.
         *
         * If a request is pending but doesn't match the reply we ignore it, and
         * return
         *
         * If no request is pending we disconnect with a SwitchStateException
         *
         * @param RoleReplyInfo information about role-reply in format that
         *                      controller can understand.
         * @throws SwitchStateException if no request is pending
         */
        synchronized RoleRecvStatus deliverRoleReply(RoleReplyInfo rri)
                throws SwitchStateException {
            if (!requestPending) {
                Role currentRole = (sw != null) ? sw.getRole() : null;
                if (currentRole != null) {
                    if (currentRole == rri.getRole()) {
                        // Don't disconnect if the role reply we received is
                        // for the same role we are already in.
                        log.debug("Received unexpected RoleReply from "
                                + "Switch: {} in State: {}. "
                                + "Role in reply is same as current role of this "
                                + "controller for this sw. Ignoring ...",
                                getSwitchInfoString(), state.toString());
                        return RoleRecvStatus.OTHER_EXPECTATION;
                    } else {
                        String msg = String.format("Switch: [%s], State: [%s], "
                                + "received unexpected RoleReply[%s]. "
                                + "No roles are pending, and this controller's "
                                + "current role:[%s] does not match reply. "
                                + "Disconnecting switch ... ",
                                OFChannelHandler.this.getSwitchInfoString(),
                                OFChannelHandler.this.state.toString(),
                                rri, currentRole);
                        throw new SwitchStateException(msg);
                    }
                }
                log.debug("Received unexpected RoleReply {} from "
                        + "Switch: {} in State: {}. "
                        + "This controller has no current role for this sw. "
                        + "Ignoring ...", new Object[] {rri,
                                getSwitchInfoString(), state});
                return RoleRecvStatus.OTHER_EXPECTATION;
            }

            int xid = (int) rri.getXid();
            Role role = rri.getRole();
            // XXX S should check generation id meaningfully and other cases of expectations
            // U64 genId = rri.getGenId();

            if (pendingXid != xid) {
                log.debug("Received older role reply from " +
                        "switch {} ({}). Ignoring. " +
                        "Waiting for {}, xid={}",
                        new Object[] {getSwitchInfoString(), rri,
                        pendingRole, pendingXid });
                return RoleRecvStatus.OLD_REPLY;
            }

            if (pendingRole == role) {
                log.debug("Received role reply message from {} that matched "
                        + "expected role-reply {} with expectations {}",
                        new Object[] {getSwitchInfoString(), role, expectation});
                counters.roleReplyReceived.updateCounterWithFlush();
                //setSwitchRole(role, RoleRecvStatus.RECEIVED_REPLY); dont want to set state here
                if (expectation == RoleRecvStatus.MATCHED_CURRENT_ROLE ||
                        expectation == RoleRecvStatus.MATCHED_SET_ROLE) {
                    return expectation;
                } else {
                    return RoleRecvStatus.OTHER_EXPECTATION;
                }
            }

            // if xids match but role's don't, perhaps its a query (OF1.3)
            if (expectation == RoleRecvStatus.REPLY_QUERY) {
                return expectation;
            }

            return RoleRecvStatus.OTHER_EXPECTATION;
        }

        /**
         * Called if we receive an  error message. If the xid matches the
         * pending request we handle it otherwise we ignore it.
         *
         * Note: since we only keep the last pending request we might get
         * error messages for earlier role requests that we won't be able
         * to handle
         */
        synchronized RoleRecvStatus deliverError(OFErrorMsg error)
                throws SwitchStateException {
            if (!requestPending) {
                log.debug("Received an error msg from sw {}, but no pending "
                        + "requests in role-changer; not handling ...",
                        getSwitchInfoString());
                return RoleRecvStatus.OTHER_EXPECTATION;
            }
            if (pendingXid != error.getXid()) {
                if (error.getErrType() == OFErrorType.ROLE_REQUEST_FAILED) {
                    log.debug("Received an error msg from sw {} for a role request,"
                            + " but not for pending request in role-changer; "
                            + " ignoring error {} ...",
                            getSwitchInfoString(), error);
                }
                return RoleRecvStatus.OTHER_EXPECTATION;
            }
            // it is an error related to a currently pending role request message
            if (error.getErrType() == OFErrorType.BAD_REQUEST) {
                counters.roleReplyErrorUnsupported.updateCounterWithFlush();
                log.error("Received a error msg {} from sw {} in state {} for "
                        + "pending role request {}. Switch driver indicates "
                        + "role-messaging is supported. Possible issues in "
                        + "switch driver configuration?", new Object[] {
                                ((OFBadRequestErrorMsg) error).toString(),
                                getSwitchInfoString(), state, pendingRole
                        });
                return RoleRecvStatus.UNSUPPORTED;
            }

            if (error.getErrType() == OFErrorType.ROLE_REQUEST_FAILED) {
                OFRoleRequestFailedErrorMsg rrerr =
                        (OFRoleRequestFailedErrorMsg) error;
                switch (rrerr.getCode()) {
                case BAD_ROLE:
                    // switch says that current-role-req has bad role?
                    // for now we disconnect
                    // fall-thru
                case STALE:
                    // switch says that current-role-req has stale gen-id?
                    // for now we disconnect
                    // fall-thru
                case UNSUP:
                    // switch says that current-role-req has role that
                    // cannot be supported? for now we disconnect
                    String msgx = String.format("Switch: [%s], State: [%s], "
                            + "received Error to for pending role request [%s]. "
                            + "Error:[%s]. Disconnecting switch ... ",
                            OFChannelHandler.this.getSwitchInfoString(),
                            OFChannelHandler.this.state.toString(),
                            pendingRole, rrerr);
                    throw new SwitchStateException(msgx);
                default:
                    break;
                }
            }

            // This error message was for a role request message but we dont know
            // how to handle errors for nicira role request messages
            return RoleRecvStatus.OTHER_EXPECTATION;
        }

        /**
         * Check if a pending role request has timed out.
         */
        void checkTimeout() {
            if (!requestPending) {
                return;
            }
            synchronized (this) {
                if (!requestPending) {
                    return;
                }
                long now = System.currentTimeMillis();
                if (now - roleSubmitTime > roleTimeoutMs) {
                    // timeout triggered.
                    counters.roleReplyTimeout.updateCounterWithFlush();
                    //setSwitchRole(pendingRole, RoleRecvStatus.NO_REPLY);
                    // XXX S come back to this
                }
            }
        }

    }

    //*************************
    //  Channel State Machine
    //*************************

    /**
     * The state machine for handling the switch/channel state. All state
     * transitions should happen from within the state machine (and not from other
     * parts of the code)
     */
    enum ChannelState {
        /**
         * Initial state before channel is connected.
         */
        INIT(false) {
            @Override
            void processOFMessage(OFChannelHandler h, OFMessage m)
                    throws IOException, SwitchStateException {
                illegalMessageReceived(h, m);
            }

            @Override
            void processOFError(OFChannelHandler h, OFErrorMsg m)
                    throws IOException {
                // need to implement since its abstract but it will never
                // be called
            }

            @Override
            void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                    throws IOException {
                unhandledMessageReceived(h, m);
            }
        },

        /**
         * We send a OF 1.3 HELLO to the switch and wait for a Hello from the switch.
         * Once we receive the reply, we decide on OF 1.3 or 1.0 switch - no other
         * protocol version is accepted.
         * We send an OFFeaturesRequest depending on the protocol version selected
         * Next state is WAIT_FEATURES_REPLY
         */
        WAIT_HELLO(false) {
            @Override
            void processOFHello(OFChannelHandler h, OFHello m)
                    throws IOException {
                // TODO We could check for the optional bitmap, but for now
                // we are just checking the version number.
                if (m.getVersion() == OFVersion.OF_13) {
                    log.info("Received {} Hello from {}", m.getVersion(),
                            h.channel.getRemoteAddress());
                    h.ofVersion = OFVersion.OF_13;
                } else if (m.getVersion() == OFVersion.OF_10) {
                    log.info("Received {} Hello from {} - switching to OF "
                            + "version 1.0", m.getVersion(),
                            h.channel.getRemoteAddress());
                    h.ofVersion = OFVersion.OF_10;
                } else {
                    log.error("Received Hello of version {} from switch at {}. "
                            + "This controller works with OF1.0 and OF1.3 "
                            + "switches. Disconnecting switch ...",
                            m.getVersion(), h.channel.getRemoteAddress());
                    h.channel.disconnect();
                    return;
                }
                h.sendHandshakeFeaturesRequestMessage();
                h.setState(WAIT_FEATURES_REPLY);
            }
            @Override
            void processOFFeaturesReply(OFChannelHandler h, OFFeaturesReply  m)
                    throws IOException, SwitchStateException {
                illegalMessageReceived(h, m);
            }
            @Override
            void processOFStatisticsReply(OFChannelHandler h,
                    OFStatsReply  m)
                            throws IOException, SwitchStateException {
                illegalMessageReceived(h, m);
            }
            @Override
            void processOFError(OFChannelHandler h, OFErrorMsg m) {
                logErrorDisconnect(h, m);
            }

            @Override
            void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                    throws IOException {
                unhandledMessageReceived(h, m);
            }
        },


        /**
         * We are waiting for a features reply message. Once we receive it, the
         * behavior depends on whether this is a 1.0 or 1.3 switch. For 1.0,
         * we send a SetConfig request, barrier, and GetConfig request and the
         * next state is WAIT_CONFIG_REPLY. For 1.3, we send a Port description
         * request and the next state is WAIT_PORT_DESC_REPLY.
         */
        WAIT_FEATURES_REPLY(false) {
            @Override
            void processOFFeaturesReply(OFChannelHandler h, OFFeaturesReply  m)
                    throws IOException {
                h.thisdpid = m.getDatapathId().getLong();
                log.info("Received features reply for switch at {} with dpid {}",
                        h.getSwitchInfoString(), h.thisdpid);
                //update the controller about this connected switch
                boolean success = h.controller.addConnectedSwitch(
                        h.thisdpid, h);
                if (!success) {
                    disconnectDuplicate(h);
                    return;
                }

                h.featuresReply = m; //temp store
                if (h.ofVersion == OFVersion.OF_10) {
                    h.sendHandshakeSetConfig();
                    h.setState(WAIT_CONFIG_REPLY);
                } else {
                    //version is 1.3, must get switchport information
                    h.sendHandshakeOFPortDescRequest();
                    h.setState(WAIT_PORT_DESC_REPLY);
                }
            }
            @Override
            void processOFStatisticsReply(OFChannelHandler h,
                    OFStatsReply  m)
                            throws IOException, SwitchStateException {
                illegalMessageReceived(h, m);
            }
            @Override
            void processOFError(OFChannelHandler h, OFErrorMsg m) {
                logErrorDisconnect(h, m);
            }

            @Override
            void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                    throws IOException {
                unhandledMessageReceived(h, m);
            }
        },

        /**
         * We are waiting for a description of the 1.3 switch ports.
         * Once received, we send a SetConfig request
         * Next State is WAIT_CONFIG_REPLY
         */
        WAIT_PORT_DESC_REPLY(false) {

            @Override
            void processOFStatisticsReply(OFChannelHandler h, OFStatsReply m)
                    throws SwitchStateException {
                // Read port description
                if (m.getStatsType() != OFStatsType.PORT_DESC) {
                    log.warn("Expecting port description stats but received stats "
                            + "type {} from {}. Ignoring ...", m.getStatsType(),
                            h.channel.getRemoteAddress());
                    return;
                }
                if (m.getFlags().contains(OFStatsReplyFlags.REPLY_MORE)) {
                    log.warn("Stats reply indicates more stats from sw {} for "
                            + "port description - not currently handled",
                            h.getSwitchInfoString());
                }
                h.portDescReply = (OFPortDescStatsReply) m; // temp store
                log.info("Received port desc reply for switch at {}",
                        h.getSwitchInfoString());
                try {
                    h.sendHandshakeSetConfig();
                } catch (IOException e) {
                    log.error("Unable to send setConfig after PortDescReply. "
                            + "Error: {}", e.getMessage());
                }
                h.setState(WAIT_CONFIG_REPLY);
            }

            @Override
            void processOFError(OFChannelHandler h, OFErrorMsg m)
                    throws IOException, SwitchStateException {
                logErrorDisconnect(h, m);

            }

            @Override
            void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                    throws IOException, SwitchStateException {
                unhandledMessageReceived(h, m);

            }
        },

        /**
         * We are waiting for a config reply message. Once we receive it
         * we send a DescriptionStatsRequest to the switch.
         * Next state: WAIT_DESCRIPTION_STAT_REPLY
         */
        WAIT_CONFIG_REPLY(false) {
            @Override
            @LogMessageDocs({
                @LogMessageDoc(level = "WARN",
                        message = "Config Reply from {switch} has "
                                + "miss length set to {length}",
                                explanation = "The controller requires that the switch "
                                        + "use a miss length of 0xffff for correct "
                                        + "function",
                                        recommendation = "Use a different switch to ensure "
                                                + "correct function")
            })
            void processOFGetConfigReply(OFChannelHandler h, OFGetConfigReply m)
                    throws IOException {
                if (m.getMissSendLen() == 0xffff) {
                    log.trace("Config Reply from switch {} confirms "
                            + "miss length set to 0xffff",
                            h.getSwitchInfoString());
                } else {
                    // FIXME: we can't really deal with switches that don't send
                    // full packets. Shouldn't we drop the connection here?
                    log.warn("Config Reply from switch {} has"
                            + "miss length set to {}",
                            h.getSwitchInfoString(),
                            m.getMissSendLen());
                }
                h.sendHandshakeDescriptionStatsRequest();
                h.setState(WAIT_DESCRIPTION_STAT_REPLY);
            }

            @Override
            void processOFBarrierReply(OFChannelHandler h, OFBarrierReply m) {
                // do nothing;
            }

            @Override
            void processOFFeaturesReply(OFChannelHandler h, OFFeaturesReply  m)
                    throws IOException, SwitchStateException {
                illegalMessageReceived(h, m);
            }
            @Override
            void processOFStatisticsReply(OFChannelHandler h,
                    OFStatsReply  m)
                            throws IOException, SwitchStateException {
                log.error("Received multipart(stats) message sub-type {}",
                        m.getStatsType());
                illegalMessageReceived(h, m);
            }

            @Override
            void processOFError(OFChannelHandler h, OFErrorMsg m) {
                logErrorDisconnect(h, m);
            }

            @Override
            void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                    throws IOException {
                h.pendingPortStatusMsg.add(m);
            }
        },


        /**
         * We are waiting for a OFDescriptionStat message from the switch.
         * Once we receive any stat message we try to parse it. If it's not
         * a description stats message we disconnect. If its the expected
         * description stats message, we:
         *    - use the switch driver to bind the switch and get an IOFSwitch instance
         *    - setup the IOFSwitch instance
         *    - add switch to FloodlightProvider(Controller) and send the initial role
         *      request to the switch.
         * Next state: WAIT_INITIAL_ROLE
         *      In the typical case, where switches support role request messages
         *      the next state is where we expect the role reply message.
         *      In the special case that where the switch does not support any kind
         *      of role request messages, we don't send a role message, but we do
         *      request mastership from the registry service. This controller
         *      should become master once we hear back from the registry service.
         * All following states will have a h.sw instance!
         */
        WAIT_DESCRIPTION_STAT_REPLY(false) {
            @LogMessageDoc(message = "Switch {switch info} bound to class "
                    + "{switch driver}, description {switch description}",
                    explanation = "The specified switch has been bound to "
                            + "a switch driver based on the switch description"
                            + "received from the switch")
            @Override
            void processOFStatisticsReply(OFChannelHandler h, OFStatsReply m)
                    throws SwitchStateException {
                // Read description, if it has been updated
                if (m.getStatsType() != OFStatsType.DESC) {
                    log.warn("Expecting Description stats but received stats "
                            + "type {} from {}. Ignoring ...", m.getStatsType(),
                            h.channel.getRemoteAddress());
                    return;
                }
                log.info("Received switch description reply from switch at {}",
                        h.channel.getRemoteAddress());
                OFDescStatsReply drep = (OFDescStatsReply) m;
                // Here is where we differentiate between different kinds of switches
                h.sw = h.controller.getOFSwitchInstance(drep, h.ofVersion);
                // set switch information
                h.sw.setOFVersion(h.ofVersion);
                h.sw.setFeaturesReply(h.featuresReply);
                h.sw.setPortDescReply(h.portDescReply);
                h.sw.setConnected(true);
                h.sw.setChannel(h.channel);

                try {
                    h.sw.setDebugCounterService(h.controller.getDebugCounter());
                } catch (CounterException e) {
                    h.counters.switchCounterRegistrationFailed
                    .updateCounterNoFlush();
                    log.warn("Could not register counters for switch {} ",
                            h.getSwitchInfoString(), e);
                }

                log.info("Switch {} bound to class {}, description {}",
                        new Object[] {h.sw, h.sw.getClass(), drep });
                //Put switch in EQUAL mode until we hear back from the global registry
                log.debug("Setting new switch {} to EQUAL and sending Role request",
                        h.sw.getStringId());
                h.setSwitchRole(Role.EQUAL);
                try {
                    boolean supportsRRMsg = h.roleChanger.sendRoleRequest(Role.EQUAL,
                            RoleRecvStatus.MATCHED_CURRENT_ROLE);
                    if (!supportsRRMsg) {
                        log.warn("Switch {} does not support role request messages "
                                + "of any kind. No role messages were sent. "
                                + "This controller instance SHOULD become MASTER "
                                + "from the registry process. ",
                                h.getSwitchInfoString());
                    }
                    h.setState(WAIT_INITIAL_ROLE);
                    // request control of switch from global registry -
                    // necessary even if this is the only controller the
                    // switch is connected to.
                    h.controller.submitRegistryRequest(h.sw.getId());
                } catch (IOException e) {
                    log.error("Exception when sending role request: {} ",
                            e.getMessage());
                    // FIXME shouldn't we disconnect?
                }
            }

            @Override
            void processOFError(OFChannelHandler h, OFErrorMsg m) {
                logErrorDisconnect(h, m);
            }

            @Override
            void processOFFeaturesReply(OFChannelHandler h, OFFeaturesReply  m)
                    throws IOException, SwitchStateException {
                illegalMessageReceived(h, m);
            }

            @Override
            void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                    throws IOException {
                h.pendingPortStatusMsg.add(m);
            }
        },

        /**
         * We are waiting for a role reply message in response to a role request
         * sent after hearing back from the registry service -- OR -- we are
         * just waiting to hear back from the registry service in the case that
         * the switch does not support role messages. If completed successfully,
         * the controller's role for this switch will be set here.
         * Before we move to the state corresponding to the role, we allow the
         * switch specific driver to complete its configuration. This configuration
         * typically depends on the role the controller is playing for this switch.
         * And so we set the switch role (for 'this' controller) before we start
         * the driver-sub-handshake.
         * Next State: WAIT_SWITCH_DRIVER_SUB_HANDSHAKE
         */
        WAIT_INITIAL_ROLE(false) {
            @Override
            void processOFError(OFChannelHandler h, OFErrorMsg m)
                    throws SwitchStateException {
                // role changer will ignore the error if it isn't for it
                RoleRecvStatus rrstatus = h.roleChanger.deliverError(m);
                if (rrstatus == RoleRecvStatus.OTHER_EXPECTATION) {
                    logError(h, m);
                }
            }

            @Override
            void processOFExperimenter(OFChannelHandler h, OFExperimenter m)
                    throws IOException, SwitchStateException {
                Role role = extractNiciraRoleReply(h, m);
                // If role == null it means the vendor (experimenter) message
                // wasn't really a Nicira role reply. We ignore this case.
                if (role != null) {
                    RoleReplyInfo rri = new RoleReplyInfo(role, null, m.getXid());
                    RoleRecvStatus rrs = h.roleChanger.deliverRoleReply(rri);
                    if (rrs == RoleRecvStatus.MATCHED_SET_ROLE) {
                        setRoleAndStartDriverHandshake(h, rri.getRole());
                    } // else do nothing - wait for the correct expected reply
                } else {
                    unhandledMessageReceived(h, m);
                }
            }

            @Override
            void processOFRoleReply(OFChannelHandler h, OFRoleReply m)
                    throws SwitchStateException, IOException {
                RoleReplyInfo rri = extractOFRoleReply(h, m);
                RoleRecvStatus rrs = h.roleChanger.deliverRoleReply(rri);
                if (rrs == RoleRecvStatus.MATCHED_SET_ROLE) {
                    setRoleAndStartDriverHandshake(h, rri.getRole());
                } // else do nothing - wait for the correct expected reply
            }

            @Override
            void handleUnsentRoleMessage(OFChannelHandler h, Role role,
                    RoleRecvStatus expectation) throws IOException {
                // typically this is triggered for a switch where role messages
                // are not supported - we confirm that the role being set is
                // master and move to the next state
                if (expectation == RoleRecvStatus.MATCHED_SET_ROLE) {
                    if (role == Role.MASTER) {
                        setRoleAndStartDriverHandshake(h, role);
                    } else {
                        log.error("Expected MASTER role from registry for switch "
                                + "which has no support for role-messages."
                                + "Received {}. It is possible that this switch "
                                + "is connected to other controllers, in which "
                                + "case it should support role messages - not "
                                + "moving forward.", role);
                    }
                } // else do nothing - wait to hear back from registry

            }

            private void setRoleAndStartDriverHandshake(OFChannelHandler h,
                    Role role) throws IOException {
                h.setSwitchRole(role);
                h.sw.startDriverHandshake();
                if (h.sw.isDriverHandshakeComplete()) {
                    Role mySwitchRole = h.sw.getRole();
                    if (mySwitchRole == Role.MASTER) {
                        log.info("Switch-driver sub-handshake complete. "
                                + "Activating switch {} with Role: MASTER",
                                h.getSwitchInfoString());
                        handlePendingPortStatusMessages(h); //before activation
                        boolean success = h.controller.addActivatedMasterSwitch(
                                h.sw.getId(), h.sw);
                        if (!success) {
                            disconnectDuplicate(h);
                            return;
                        }
                        h.setState(MASTER);
                    } else {
                        log.info("Switch-driver sub-handshake complete. "
                                + "Activating switch {} with Role: EQUAL",
                                h.getSwitchInfoString());
                        handlePendingPortStatusMessages(h); //before activation
                        boolean success = h.controller.addActivatedEqualSwitch(
                                h.sw.getId(), h.sw);
                        if (!success) {
                            disconnectDuplicate(h);
                            return;
                        }
                        h.setState(EQUAL);
                    }
                } else {
                    h.setState(WAIT_SWITCH_DRIVER_SUB_HANDSHAKE);
                }
            }

            @Override
            void processOFFeaturesReply(OFChannelHandler h, OFFeaturesReply  m)
                    throws IOException, SwitchStateException {
                illegalMessageReceived(h, m);
            }

            @Override
            void processOFStatisticsReply(OFChannelHandler h, OFStatsReply m)
                    throws SwitchStateException {
                illegalMessageReceived(h, m);
            }

            @Override
            void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                    throws IOException, SwitchStateException {
                h.pendingPortStatusMsg.add(m);

            }
        },

        /**
         * We are waiting for the respective switch driver to complete its
         * configuration. Notice that we do not consider this to be part of the main
         * switch-controller handshake. But we do consider it as a step that comes
         * before we declare the switch as available to the controller.
         * Next State: depends on the role of this controller for this switch - either
         * MASTER or EQUAL.
         */
        WAIT_SWITCH_DRIVER_SUB_HANDSHAKE(true) {

            @Override
            void processOFError(OFChannelHandler h, OFErrorMsg m)
                    throws IOException {
                // will never be called. We override processOFMessage
            }

            @Override
            void processOFMessage(OFChannelHandler h, OFMessage m)
                    throws IOException {
                if (m.getType() == OFType.ECHO_REQUEST) {
                    processOFEchoRequest(h, (OFEchoRequest) m);
                } else {
                    // FIXME: other message to handle here?
                    h.sw.processDriverHandshakeMessage(m);
                    if (h.sw.isDriverHandshakeComplete()) {
                        // consult the h.sw role and goto that state
                        Role mySwitchRole = h.sw.getRole();
                        if (mySwitchRole == Role.MASTER) {
                            log.info("Switch-driver sub-handshake complete. "
                                    + "Activating switch {} with Role: MASTER",
                                    h.getSwitchInfoString());
                            handlePendingPortStatusMessages(h); //before activation
                            boolean success = h.controller.addActivatedMasterSwitch(
                                    h.sw.getId(), h.sw);
                            if (!success) {
                                disconnectDuplicate(h);
                                return;
                            }
                            h.setState(MASTER);
                        } else {
                            log.info("Switch-driver sub-handshake complete. "
                                    + "Activating switch {} with Role: EQUAL",
                                    h.getSwitchInfoString());
                            handlePendingPortStatusMessages(h); //before activation
                            boolean success = h.controller.addActivatedEqualSwitch(
                                    h.sw.getId(), h.sw);
                            if (!success) {
                                disconnectDuplicate(h);
                                return;
                            }
                            h.setState(EQUAL);
                        }
                    }
                }
            }

            @Override
            void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                    throws IOException, SwitchStateException {
                h.pendingPortStatusMsg.add(m);
            }
        },


        /**
         * This controller is in MASTER role for this switch. We enter this state
         * after requesting and winning control from the global registry.
         * The main handshake as well as the switch-driver sub-handshake
         * is complete at this point.
         * // XXX S reconsider below
         * In the (near) future we may deterministically assign controllers to
         * switches at startup.
         * We only leave this state if the switch disconnects or
         * if we send a role request for SLAVE /and/ receive the role reply for
         * SLAVE.
         */
        MASTER(true) {
            @LogMessageDoc(level = "WARN",
                    message = "Received permission error from switch {} while"
                            + "being master. Reasserting master role.",
                            explanation = "The switch has denied an operation likely "
                                    + "indicating inconsistent controller roles",
                                    recommendation = "This situation can occurs transiently during role"
                                            + " changes. If, however, the condition persists or happens"
                                            + " frequently this indicates a role inconsistency. "
                                            + LogMessageDoc.CHECK_CONTROLLER)
            @Override
            void processOFError(OFChannelHandler h, OFErrorMsg m)
                    throws IOException, SwitchStateException {
                // first check if the error msg is in response to a role-request message
                RoleRecvStatus rrstatus = h.roleChanger.deliverError(m);
                if (rrstatus != RoleRecvStatus.OTHER_EXPECTATION) {
                    // rolechanger has handled the error message - we are done
                    return;
                }

                // if we get here, then the error message is for something else
                if (m.getErrType() == OFErrorType.BAD_REQUEST &&
                        ((OFBadRequestErrorMsg) m).getCode() ==
                        OFBadRequestCode.EPERM) {
                    // We are the master controller and the switch returned
                    // a permission error. This is a likely indicator that
                    // the switch thinks we are slave. Reassert our
                    // role
                    // FIXME: this could be really bad during role transitions
                    // if two controllers are master (even if its only for
                    // a brief period). We might need to see if these errors
                    // persist before we reassert
                    h.counters.epermErrorWhileSwitchIsMaster.updateCounterWithFlush();
                    log.warn("Received permission error from switch {} while" +
                            "being master. Reasserting master role.",
                            h.getSwitchInfoString());
                    //h.controller.reassertRole(h, Role.MASTER);
                    // XXX S reassert in role changer or reconsider if all this
                    // stuff is really needed
                } else if (m.getErrType() == OFErrorType.FLOW_MOD_FAILED &&
                        ((OFFlowModFailedErrorMsg) m).getCode() ==
                        OFFlowModFailedCode.ALL_TABLES_FULL) {
                    h.sw.setTableFull(true);
                } else {
                    logError(h, m);
                }
                h.dispatchMessage(m);
            }

            @Override
            void processOFStatisticsReply(OFChannelHandler h,
                    OFStatsReply m) {
                h.sw.deliverStatisticsReply(m);
            }

            @Override
            void processOFExperimenter(OFChannelHandler h, OFExperimenter m)
                    throws IOException, SwitchStateException {
                Role role = extractNiciraRoleReply(h, m);
                if (role == null) {
                    // The message wasn't really a Nicira role reply. We just
                    // dispatch it to the OFMessage listeners in this case.
                    h.dispatchMessage(m);
                    return;
                }

                RoleRecvStatus rrs = h.roleChanger.deliverRoleReply(
                        new RoleReplyInfo(role, null, m.getXid()));
                if (rrs == RoleRecvStatus.MATCHED_SET_ROLE) {
                    checkAndSetRoleTransition(h, role);
                }
            }

            @Override
            void processOFRoleReply(OFChannelHandler h, OFRoleReply m)
                    throws SwitchStateException, IOException {
                RoleReplyInfo rri = extractOFRoleReply(h, m);
                RoleRecvStatus rrs = h.roleChanger.deliverRoleReply(rri);
                if (rrs == RoleRecvStatus.MATCHED_SET_ROLE) {
                    checkAndSetRoleTransition(h, rri.getRole());
                }
            }

            @Override
            void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                    throws IOException, SwitchStateException {
                handlePortStatusMessage(h, m, true);
                h.dispatchMessage(m);
            }

            @Override
            void processOFPacketIn(OFChannelHandler h, OFPacketIn m)
                    throws IOException {
                h.dispatchMessage(m);
            }

            @Override
            void processOFFlowRemoved(OFChannelHandler h,
                    OFFlowRemoved m) throws IOException {
                h.dispatchMessage(m);
            }

            @Override
            void processOFBarrierReply(OFChannelHandler h, OFBarrierReply m)
                    throws IOException {
                h.dispatchMessage(m);
            }

        },

        /**
         * This controller is in EQUAL role for this switch. We enter this state
         * after some /other/ controller instance wins mastership-role over this
         * switch. The EQUAL role can be considered the same as the SLAVE role
         * if this controller does NOT send commands or packets to the switch.
         * This should always be true for OF1.0 switches. XXX S need to enforce.
         *
         * For OF1.3 switches, choosing this state as EQUAL instead of SLAVE,
         * gives us the flexibility that if an app wants to send commands/packets
         * to switches, it can, even thought it is running on a controller instance
         * that is not in a MASTER role for this switch. Of course, it is the job
         * of the app to ensure that commands/packets sent by this (EQUAL) controller
         * instance does not clash/conflict with commands/packets sent by the MASTER
         * controller for this switch. Neither the controller instances, nor the
         * switch provides any kind of resolution mechanism should conflicts occur.
         */
        EQUAL(true) {
            @Override
            void processOFError(OFChannelHandler h, OFErrorMsg m)
                    throws IOException, SwitchStateException {
                // role changer will ignore the error if it isn't for it
                RoleRecvStatus rrstatus = h.roleChanger.deliverError(m);
                if (rrstatus == RoleRecvStatus.OTHER_EXPECTATION) {
                    logError(h, m);
                    h.dispatchMessage(m);
                }
            }

            @Override
            void processOFStatisticsReply(OFChannelHandler h,
                    OFStatsReply m) {
                h.sw.deliverStatisticsReply(m);
            }

            @Override
            void processOFExperimenter(OFChannelHandler h, OFExperimenter m)
                    throws IOException, SwitchStateException {
                Role role = extractNiciraRoleReply(h, m);
                // If role == null it means the message wasn't really a
                // Nicira role reply. We ignore it in this state.
                if (role != null) {
                    RoleRecvStatus rrs = h.roleChanger.deliverRoleReply(
                            new RoleReplyInfo(role, null, m.getXid()));
                    if (rrs == RoleRecvStatus.MATCHED_SET_ROLE) {
                        checkAndSetRoleTransition(h, role);
                    }
                } else {
                    unhandledMessageReceived(h, m);
                }
            }

            @Override
            void processOFRoleReply(OFChannelHandler h, OFRoleReply m)
                    throws SwitchStateException, IOException {
                RoleReplyInfo rri = extractOFRoleReply(h, m);
                RoleRecvStatus rrs = h.roleChanger.deliverRoleReply(rri);
                if (rrs == RoleRecvStatus.MATCHED_SET_ROLE) {
                    checkAndSetRoleTransition(h, rri.getRole());
                }
            }

            // XXX S needs more handlers for 1.3 switches in equal role

            @Override
            void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                    throws IOException, SwitchStateException {
                handlePortStatusMessage(h, m, true);
            }

            @Override
            @LogMessageDoc(level = "WARN",
            message = "Received PacketIn from switch {} while "
                    + "being slave. Reasserting slave role.",
                    explanation = "The switch has receive a PacketIn despite being "
                            + "in slave role indicating inconsistent controller roles",
                            recommendation = "This situation can occurs transiently during role"
                                    + " changes. If, however, the condition persists or happens"
                                    + " frequently this indicates a role inconsistency. "
                                    + LogMessageDoc.CHECK_CONTROLLER)
            void processOFPacketIn(OFChannelHandler h, OFPacketIn m) throws IOException {
                // we don't expect packetIn while slave, reassert we are slave
                h.counters.packetInWhileSwitchIsSlave.updateCounterNoFlush();
                log.warn("Received PacketIn from switch {} while" +
                        "being slave. Reasserting slave role.", h.sw);
                //h.controller.reassertRole(h, Role.SLAVE);
                // XXX reassert in role changer
            }
        };

        private final boolean handshakeComplete;
        ChannelState(boolean handshakeComplete) {
            this.handshakeComplete = handshakeComplete;
        }

        /**
         * Is this a state in which the handshake has completed?
         * @return true if the handshake is complete
         */
        public boolean isHandshakeComplete() {
            return handshakeComplete;
        }

        /**
         * Get a string specifying the switch connection, state, and
         * message received. To be used as message for SwitchStateException
         * or log messages
         * @param h The channel handler (to get switch information_
         * @param m The OFMessage that has just been received
         * @param details A string giving more details about the exact nature
         * of the problem.
         * @return
         */
        // needs to be protected because enum members are actually subclasses
        protected String getSwitchStateMessage(OFChannelHandler h,
                OFMessage m,
                String details) {
            return String.format("Switch: [%s], State: [%s], received: [%s]"
                    + ", details: %s",
                    h.getSwitchInfoString(),
                    this.toString(),
                    m.getType().toString(),
                    details);
        }

        /**
         * We have an OFMessage we didn't expect given the current state and
         * we want to treat this as an error.
         * We currently throw an exception that will terminate the connection
         * However, we could be more forgiving
         * @param h the channel handler that received the message
         * @param m the message
         * @throws SwitchStateException
         * @throws SwitchStateExeption we always through the execption
         */
        // needs to be protected because enum members are acutally subclasses
        protected void illegalMessageReceived(OFChannelHandler h, OFMessage m)
                throws SwitchStateException {
            String msg = getSwitchStateMessage(h, m,
                    "Switch should never send this message in the current state");
            throw new SwitchStateException(msg);

        }

        /**
         * We have an OFMessage we didn't expect given the current state and
         * we want to ignore the message.
         * @param h the channel handler the received the message
         * @param m the message
         */
        protected void unhandledMessageReceived(OFChannelHandler h,
                OFMessage m) {
            h.counters.unhandledMessage.updateCounterNoFlush();
            if (log.isDebugEnabled()) {
                String msg = getSwitchStateMessage(h, m,
                        "Ignoring unexpected message");
                log.debug(msg);
            }
        }

        /**
         * Log an OpenFlow error message from a switch.
         * @param sw The switch that sent the error
         * @param error The error message
         */
        @LogMessageDoc(level = "ERROR",
                message = "Error {error type} {error code} from {switch} "
                        + "in state {state}",
                        explanation = "The switch responded with an unexpected error"
                                + "to an OpenFlow message from the controller",
                                recommendation = "This could indicate improper network operation. "
                                        + "If the problem persists restarting the switch and "
                                        + "controller may help."
                )
        protected void logError(OFChannelHandler h, OFErrorMsg error) {
            log.error("{} from switch {} in state {}",
                    new Object[] {
                    error,
                    h.getSwitchInfoString(),
                    this.toString()});
        }

        /**
         * Log an OpenFlow error message from a switch and disconnect the
         * channel.
         *
         * @param h the IO channel for this switch.
         * @param error The error message
         */
        protected void logErrorDisconnect(OFChannelHandler h, OFErrorMsg error) {
            logError(h, error);
            h.channel.disconnect();
        }

        /**
         * log an error message for a duplicate dpid and disconnect this channel.
         * @param h the IO channel for this switch.
         */
        protected void disconnectDuplicate(OFChannelHandler h) {
            log.error("Duplicated dpid or incompleted cleanup - "
                    + "disconnecting channel {}", h.getSwitchInfoString());
            h.duplicateDpidFound = Boolean.TRUE;
            h.channel.disconnect();
        }

        /**
         * Extract the role from an OFVendor message.
         *
         * Extract the role from an OFVendor message if the message is a
         * Nicira role reply. Otherwise return null.
         *
         * @param h The channel handler receiving the message
         * @param vendorMessage The vendor message to parse.
         * @return The role in the message if the message is a Nicira role
         * reply, null otherwise.
         * @throws SwitchStateException If the message is a Nicira role reply
         * but the numeric role value is unknown.
         */
        protected Role extractNiciraRoleReply(OFChannelHandler h,
                OFExperimenter experimenterMsg) throws SwitchStateException {
            int vendor = (int) experimenterMsg.getExperimenter();
            if (vendor != 0x2320) {
                return null;
            }
            OFNiciraControllerRoleReply nrr =
                    (OFNiciraControllerRoleReply) experimenterMsg;

            Role role = null;
            OFNiciraControllerRole ncr = nrr.getRole();
            switch(ncr) {
            case ROLE_MASTER:
                role = Role.MASTER;
                break;
            case ROLE_OTHER:
                role = Role.EQUAL;
                break;
            case ROLE_SLAVE:
                role = Role.SLAVE;
                break;
            default: //handled below
            }

            if (role == null) {
                String msg = String.format("Switch: [%s], State: [%s], "
                        + "received NX_ROLE_REPLY with invalid role "
                        + "value %s",
                        h.getSwitchInfoString(),
                        this.toString(),
                        nrr.getRole());
                throw new SwitchStateException(msg);
            }
            return role;
        }

        /**
         * Helper class returns role reply information in the format understood
         * by the controller.
         */
        protected static class RoleReplyInfo {
            private Role role;
            private U64 genId;
            private long xid;

            RoleReplyInfo(Role role, U64 genId, long xid) {
                this.role = role;
                this.genId = genId;
                this.xid = xid;
            }
            public Role getRole() { return role; }
            public U64 getGenId() { return genId; }
            public long getXid() { return xid; }
            @Override
            public String toString() {
                return "[Role:" + role + " GenId:" + genId + " Xid:" + xid + "]";
            }
        }

        /**
         * Extract the role information from an OF1.3 Role Reply Message.
         * @param h
         * @param rrmsg
         * @return RoleReplyInfo object
         * @throws SwitchStateException
         */
        protected RoleReplyInfo extractOFRoleReply(OFChannelHandler h,
                OFRoleReply rrmsg) throws SwitchStateException {
            OFControllerRole cr = rrmsg.getRole();
            Role role = null;
            switch(cr) {
            case ROLE_EQUAL:
                role = Role.EQUAL;
                break;
            case ROLE_MASTER:
                role = Role.MASTER;
                break;
            case ROLE_SLAVE:
                role = Role.SLAVE;
                break;
            case ROLE_NOCHANGE: // switch should send current role
            default:
                String msg = String.format("Unknown controller role %s "
                        + "received from switch %s", cr, h.sw);
                throw new SwitchStateException(msg);
            }

            return new RoleReplyInfo(role, rrmsg.getGenerationId(), rrmsg.getXid());
        }

        /**
         * Handles all pending port status messages before a switch is declared
         * activated in MASTER or EQUAL role. Note that since this handling
         * precedes the activation (and therefore notification to IOFSwitchListerners)
         * the changes to ports will already be visible once the switch is
         * activated. As a result, no notifications are sent out for these
         * pending portStatus messages.
         * @param h
         * @throws SwitchStateException
         */
        protected void handlePendingPortStatusMessages(OFChannelHandler h) {
            try {
                handlePendingPortStatusMessages(h, 0);
            } catch (SwitchStateException e) {
                log.error(e.getMessage());
            }
        }

        private void handlePendingPortStatusMessages(OFChannelHandler h, int index)
                throws SwitchStateException {
            if (h.sw == null) {
                String msg = "State machine error: switch is null. Should never " +
                        "happen";
                throw new SwitchStateException(msg);
            }
            ArrayList<OFPortStatus> temp  = new ArrayList<OFPortStatus>();
            for (OFPortStatus ps: h.pendingPortStatusMsg) {
                temp.add(ps);
                handlePortStatusMessage(h, ps, false);
            }
            temp.clear();
            // expensive but ok - we don't expect too many port-status messages
            // note that we cannot use clear(), because of the reasons below
            h.pendingPortStatusMsg.removeAll(temp);
            // the iterator above takes a snapshot of the list - so while we were
            // dealing with the pending port-status messages, we could have received
            // newer ones. Handle them recursively, but break the recursion after
            // five steps to avoid an attack.
            if (!h.pendingPortStatusMsg.isEmpty() && ++index < 5) {
                handlePendingPortStatusMessages(h, index);
            }
        }

        /**
         * Handle a port status message.
         *
         * Handle a port status message by updating the port maps in the
         * IOFSwitch instance and notifying Controller about the change so
         * it can dispatch a switch update.
         *
         * @param h The OFChannelHhandler that received the message
         * @param m The PortStatus message we received
         * @param doNotify if true switch port changed events will be
         * dispatched
         * @throws SwitchStateException
         *
         */
        protected void handlePortStatusMessage(OFChannelHandler h, OFPortStatus m,
                boolean doNotify) throws SwitchStateException {
            if (h.sw == null) {
                String msg = getSwitchStateMessage(h, m,
                        "State machine error: switch is null. Should never " +
                        "happen");
                throw new SwitchStateException(msg);
            }

            Collection<PortChangeEvent> changes = h.sw.processOFPortStatus(m);
            if (doNotify) {
                for (PortChangeEvent ev: changes) {
                    h.controller.notifyPortChanged(h.sw.getId(), ev.port, ev.type);
                }
            }
        }

        /**
         * Checks if the role received (from the role-reply msg) is different
         * from the existing role in the IOFSwitch object for this controller.
         * If so, it transitions the controller to the new role. Note that
         * the caller should have already verified that the role-reply msg
         * received was in response to a role-request msg sent out by this
         * controller after hearing from the registry service.
         *
         * @param h the ChannelHandler that received the message
         * @param role the role in the recieved role reply message
         */
        protected void checkAndSetRoleTransition(OFChannelHandler h, Role role) {
            // we received a role-reply in response to a role message
            // sent after hearing from the registry service. It is
            // possible that the role of this controller instance for
            // this switch has changed:
            // for 1.0 switch: from MASTER to SLAVE
            // for 1.3 switch: from MASTER to EQUAL
            if ((h.sw.getRole() == Role.MASTER && role == Role.SLAVE) ||
                    (h.sw.getRole() == Role.MASTER && role == Role.EQUAL)) {
                // the mastership has changed
                h.sw.setRole(role);
                h.setState(EQUAL);
                h.controller.transitionToEqualSwitch(h.sw.getId());
                return;
            }

            // or for both 1.0 and 1.3 switches from EQUAL to MASTER.
            // note that for 1.0, even though we mean SLAVE,
            // internally we call the role EQUAL.
            if (h.sw.getRole() == Role.EQUAL && role == Role.MASTER) {
                // the mastership has changed
                h.sw.setRole(role);
                h.setState(MASTER);
                h.controller.transitionToMasterSwitch(h.sw.getId());
                return;
            }
        }

        /**
         * Process an OF message received on the channel and
         * update state accordingly.
         *
         * The main "event" of the state machine. Process the received message,
         * send follow up message if required and update state if required.
         *
         * Switches on the message type and calls more specific event handlers
         * for each individual OF message type. If we receive a message that
         * is supposed to be sent from a controller to a switch we throw
         * a SwitchStateExeption.
         *
         * The more specific handlers can also throw SwitchStateExceptions
         *
         * @param h The OFChannelHandler that received the message
         * @param m The message we received.
         * @throws SwitchStateException
         * @throws IOException
         */
        void processOFMessage(OFChannelHandler h, OFMessage m)
                throws IOException, SwitchStateException {
            h.roleChanger.checkTimeout();
            switch(m.getType()) {
            case HELLO:
                processOFHello(h, (OFHello) m);
                break;
            case BARRIER_REPLY:
                processOFBarrierReply(h, (OFBarrierReply) m);
                break;
            case ECHO_REPLY:
                processOFEchoReply(h, (OFEchoReply) m);
                break;
            case ECHO_REQUEST:
                processOFEchoRequest(h, (OFEchoRequest) m);
                break;
            case ERROR:
                processOFError(h, (OFErrorMsg) m);
                break;
            case FEATURES_REPLY:
                processOFFeaturesReply(h, (OFFeaturesReply) m);
                break;
            case FLOW_REMOVED:
                processOFFlowRemoved(h, (OFFlowRemoved) m);
                break;
            case GET_CONFIG_REPLY:
                processOFGetConfigReply(h, (OFGetConfigReply) m);
                break;
            case PACKET_IN:
                processOFPacketIn(h, (OFPacketIn) m);
                break;
            case PORT_STATUS:
                processOFPortStatus(h, (OFPortStatus) m);
                break;
            case QUEUE_GET_CONFIG_REPLY:
                processOFQueueGetConfigReply(h, (OFQueueGetConfigReply) m);
                break;
            case STATS_REPLY: // multipart_reply in 1.3
            processOFStatisticsReply(h, (OFStatsReply) m);
            break;
            case EXPERIMENTER:
                processOFExperimenter(h, (OFExperimenter) m);
                break;
            case ROLE_REPLY:
                processOFRoleReply(h, (OFRoleReply) m);
                break;
            case GET_ASYNC_REPLY:
                processOFGetAsyncReply(h, (OFAsyncGetReply) m);
                break;

                // The following messages are sent to switches. The controller
                // should never receive them
            case SET_CONFIG:
            case GET_CONFIG_REQUEST:
            case PACKET_OUT:
            case PORT_MOD:
            case QUEUE_GET_CONFIG_REQUEST:
            case BARRIER_REQUEST:
            case STATS_REQUEST: // multipart request in 1.3
            case FEATURES_REQUEST:
            case FLOW_MOD:
            case GROUP_MOD:
            case TABLE_MOD:
            case GET_ASYNC_REQUEST:
            case SET_ASYNC:
            case METER_MOD:
            default:
                illegalMessageReceived(h, m);
                break;
            }
        }

        /*-----------------------------------------------------------------
         * Default implementation for message handlers in any state.
         *
         * Individual states must override these if they want a behavior
         * that differs from the default.
         *
         * In general, these handlers simply ignore the message and do
         * nothing.
         *
         * There are some exceptions though, since some messages really
         * are handled the same way in every state (e.g., ECHO_REQUST) or
         * that are only valid in a single state (e.g., HELLO, GET_CONFIG_REPLY
         -----------------------------------------------------------------*/

        void processOFHello(OFChannelHandler h, OFHello m)
                throws IOException, SwitchStateException {
            // we only expect hello in the WAIT_HELLO state
            illegalMessageReceived(h, m);
        }

        void processOFBarrierReply(OFChannelHandler h, OFBarrierReply m)
                throws IOException {
            // Silently ignore.
        }

        void processOFEchoRequest(OFChannelHandler h, OFEchoRequest m)
                throws IOException {
            if (h.ofVersion == null) {
                log.error("No OF version set for {}. Not sending Echo REPLY",
                        h.channel.getRemoteAddress());
                return;
            }
            OFFactory factory = (h.ofVersion == OFVersion.OF_13) ?
                    h.controller.getOFMessageFactory13() : h.controller.getOFMessageFactory10();
            OFEchoReply reply = factory
                    .buildEchoReply()
                    .setXid(m.getXid())
                    .setData(m.getData())
                    .build();
            h.channel.write(Collections.singletonList(reply));
        }

        void processOFEchoReply(OFChannelHandler h, OFEchoReply m)
                throws IOException {
            // Do nothing with EchoReplies !!
        }

        // no default implementation for OFError
        // every state must override it
        abstract void processOFError(OFChannelHandler h, OFErrorMsg m)
                throws IOException, SwitchStateException;


        void processOFFeaturesReply(OFChannelHandler h, OFFeaturesReply  m)
                throws IOException, SwitchStateException {
            unhandledMessageReceived(h, m);
        }

        void processOFFlowRemoved(OFChannelHandler h, OFFlowRemoved m)
                throws IOException {
            unhandledMessageReceived(h, m);
        }

        void processOFGetConfigReply(OFChannelHandler h, OFGetConfigReply m)
                throws IOException, SwitchStateException {
            // we only expect config replies in the WAIT_CONFIG_REPLY state
            illegalMessageReceived(h, m);
        }

        void processOFPacketIn(OFChannelHandler h, OFPacketIn m)
                throws IOException {
            unhandledMessageReceived(h, m);
        }

        // no default implementation. Every state needs to handle it.
        abstract void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                throws IOException, SwitchStateException;

        void processOFQueueGetConfigReply(OFChannelHandler h,
                OFQueueGetConfigReply m)
                        throws IOException {
            unhandledMessageReceived(h, m);
        }

        void processOFStatisticsReply(OFChannelHandler h, OFStatsReply m)
                throws IOException, SwitchStateException {
            unhandledMessageReceived(h, m);
        }

        void processOFExperimenter(OFChannelHandler h, OFExperimenter m)
                throws IOException, SwitchStateException {
            // TODO: it might make sense to parse the vendor message here
            // into the known vendor messages we support and then call more
            // specific event handlers
            unhandledMessageReceived(h, m);
        }

        void processOFRoleReply(OFChannelHandler h, OFRoleReply m)
                throws SwitchStateException, IOException {
            unhandledMessageReceived(h, m);
        }

        void processOFGetAsyncReply(OFChannelHandler h,
                OFAsyncGetReply m) {
            unhandledMessageReceived(h, m);
        }

        void handleUnsentRoleMessage(OFChannelHandler h, Role role,
                RoleRecvStatus expectation) throws IOException {
            // do nothing in most states
        }
    }



    //*************************
    //  Channel handler methods
    //*************************

    @Override
    @LogMessageDoc(message = "New switch connection from {ip address}",
    explanation = "A new switch has connected from the "
            + "specified IP address")
    public void channelConnected(ChannelHandlerContext ctx,
            ChannelStateEvent e) throws Exception {
        counters.switchConnected.updateCounterWithFlush();
        channel = e.getChannel();
        log.info("New switch connection from {}",
                channel.getRemoteAddress());
        sendHandshakeHelloMessage();
        setState(ChannelState.WAIT_HELLO);
    }

    @Override
    @LogMessageDoc(message = "Disconnected switch {switch information}",
    explanation = "The specified switch has disconnected.")
    public void channelDisconnected(ChannelHandlerContext ctx,
            ChannelStateEvent e) throws Exception {
        log.info("Switch disconnected callback for sw:{}. Cleaning up ...",
                getSwitchInfoString());
        if (thisdpid != 0) {
            if (!duplicateDpidFound) {
                // if the disconnected switch (on this ChannelHandler)
                // was not one with a duplicate-dpid, it is safe to remove all
                // state for it at the controller. Notice that if the disconnected
                // switch was a duplicate-dpid, calling the method below would clear
                // all state for the original switch (with the same dpid),
                // which we obviously don't want.
                controller.removeConnectedSwitch(thisdpid);
            } else {
                // A duplicate was disconnected on this ChannelHandler,
                // this is the same switch reconnecting, but the original state was
                // not cleaned up - XXX check liveness of original ChannelHandler
                duplicateDpidFound = Boolean.FALSE;
            }
        } else {
            log.warn("no dpid in channelHandler registered for "
                    + "disconnected switch {}", getSwitchInfoString());
        }
    }

    @Override
    @LogMessageDocs({
        @LogMessageDoc(level = "ERROR",
                message = "Disconnecting switch {switch} due to read timeout",
                explanation = "The connected switch has failed to send any "
                        + "messages or respond to echo requests",
                recommendation = LogMessageDoc.CHECK_SWITCH),
                        @LogMessageDoc(level = "ERROR",
                message = "Disconnecting switch {switch}: failed to "
                        + "complete handshake",
                explanation = "The switch did not respond correctly "
                        + "to handshake messages",
                recommendation = LogMessageDoc.CHECK_SWITCH),
        @LogMessageDoc(level = "ERROR",
                message = "Disconnecting switch {switch} due to IO Error: {}",
                explanation = "There was an error communicating with the switch",
                recommendation = LogMessageDoc.CHECK_SWITCH),
        @LogMessageDoc(level = "ERROR",
                message = "Disconnecting switch {switch} due to switch "
                        + "state error: {error}",
                explanation = "The switch sent an unexpected message",
                recommendation = LogMessageDoc.CHECK_SWITCH),
        @LogMessageDoc(level = "ERROR",
                message = "Disconnecting switch {switch} due to "
                          + "message parse failure",
                explanation = "Could not parse a message from the switch",
                recommendation = LogMessageDoc.CHECK_SWITCH),
        @LogMessageDoc(level = "ERROR",
                message = "Terminating controller due to storage exception",
                explanation = Controller.ERROR_DATABASE,
                recommendation = LogMessageDoc.CHECK_CONTROLLER),
        @LogMessageDoc(level = "ERROR",
                message = "Could not process message: queue full",
                explanation = "OpenFlow messages are arriving faster than "
                            + "the controller can process them.",
                recommendation = LogMessageDoc.CHECK_CONTROLLER),
        @LogMessageDoc(level = "ERROR",
                message = "Error while processing message "
                        + "from switch {switch} {cause}",
                explanation = "An error occurred processing the switch message",
                recommendation = LogMessageDoc.GENERIC_ACTION)
    })
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        if (e.getCause() instanceof ReadTimeoutException) {
            // switch timeout
            log.error("Disconnecting switch {} due to read timeout",
                    getSwitchInfoString());
            counters.switchDisconnectReadTimeout.updateCounterWithFlush();
            ctx.getChannel().close();
        } else if (e.getCause() instanceof HandshakeTimeoutException) {
            log.error("Disconnecting switch {}: failed to complete handshake",
                    getSwitchInfoString());
            counters.switchDisconnectHandshakeTimeout.updateCounterWithFlush();
            ctx.getChannel().close();
        } else if (e.getCause() instanceof ClosedChannelException) {
            log.debug("Channel for sw {} already closed", getSwitchInfoString());
        } else if (e.getCause() instanceof IOException) {
            log.error("Disconnecting switch {} due to IO Error: {}",
                    getSwitchInfoString(), e.getCause().getMessage());
            if (log.isDebugEnabled()) {
                // still print stack trace if debug is enabled
                log.debug("StackTrace for previous Exception: ", e.getCause());
            }
            counters.switchDisconnectIOError.updateCounterWithFlush();
            ctx.getChannel().close();
        } else if (e.getCause() instanceof SwitchStateException) {
            log.error("Disconnecting switch {} due to switch state error: {}",
                    getSwitchInfoString(), e.getCause().getMessage());
            if (log.isDebugEnabled()) {
                // still print stack trace if debug is enabled
                log.debug("StackTrace for previous Exception: ", e.getCause());
            }
            counters.switchDisconnectSwitchStateException.updateCounterWithFlush();
            ctx.getChannel().close();
        } else if (e.getCause() instanceof OFParseError) {
            log.error("Disconnecting switch "
                    + getSwitchInfoString() +
                    " due to message parse failure",
                    e.getCause());
            counters.switchDisconnectParseError.updateCounterWithFlush();
            ctx.getChannel().close();
        } else if (e.getCause() instanceof RejectedExecutionException) {
            log.warn("Could not process message: queue full");
            counters.rejectedExecutionException.updateCounterWithFlush();
        } else {
            log.error("Error while processing message from switch "
                    + getSwitchInfoString()
                    + "state " + this.state, e.getCause());
            counters.switchDisconnectOtherException.updateCounterWithFlush();
            ctx.getChannel().close();
        }
    }

    @Override
    public String toString() {
        return getSwitchInfoString();
    }

    @Override
    public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e)
            throws Exception {
        OFFactory factory = (ofVersion == OFVersion.OF_13) ? factory13 : factory10;
        OFMessage m = factory.buildEchoRequest().build();
        log.info("Sending Echo Request on idle channel: {}",
                e.getChannel().getPipeline().getLast().toString());
        e.getChannel().write(Collections.singletonList(m));
        // XXX S some problems here -- echo request has no transaction id, and
        // echo reply is not correlated to the echo request.
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        if (e.getMessage() instanceof List) {
            @SuppressWarnings("unchecked")
            List<OFMessage> msglist = (List<OFMessage>) e.getMessage();


            for (OFMessage ofm : msglist) {
                counters.messageReceived.updateCounterNoFlush();
                // Do the actual packet processing
                state.processOFMessage(this, ofm);
            }
        } else {
            counters.messageReceived.updateCounterNoFlush();
            state.processOFMessage(this, (OFMessage) e.getMessage());
        }
    }



    //*************************
    //  Channel utility methods
    //*************************

    /**
     * Is this a state in which the handshake has completed?
     * @return true if the handshake is complete
     */
    public boolean isHandshakeComplete() {
        return this.state.isHandshakeComplete();
    }

    private void dispatchMessage(OFMessage m) throws IOException {
        sw.handleMessage(m);
    }

    /**
     * Return a string describing this switch based on the already available
     * information (DPID and/or remote socket).
     * @return
     */
    private String getSwitchInfoString() {
        if (sw != null) {
            return sw.toString();
        }
        String channelString;
        if (channel == null || channel.getRemoteAddress() == null) {
            channelString = "?";
        } else {
            channelString = channel.getRemoteAddress().toString();
        }
        String dpidString;
        if (featuresReply == null) {
            dpidString = "?";
        } else {
            dpidString = featuresReply.getDatapathId().toString();
        }
        return String.format("[%s DPID[%s]]", channelString, dpidString);
    }

    /**
     * Update the channels state. Only called from the state machine.
     * TODO: enforce restricted state transitions
     * @param state
     */
    private void setState(ChannelState state) {
        this.state = state;
    }

    /**
     * Send hello message to the switch using the handshake transactions ids.
     * @throws IOException
     */
    private void sendHandshakeHelloMessage() throws IOException {
        // The OF protocol requires us to start things off by sending the highest
        // version of the protocol supported.

        // bitmap represents OF1.0 (ofp_version=0x01) and OF1.3 (ofp_version=0x04)
        // see Sec. 7.5.1 of the OF1.3.4 spec
        U32 bitmap = U32.ofRaw(0x00000012);
        OFHelloElem hem = factory13.buildHelloElemVersionbitmap()
                .setBitmaps(Collections.singletonList(bitmap))
                .build();
        OFMessage.Builder mb = factory13.buildHello()
                .setXid(this.handshakeTransactionIds--)
                .setElements(Collections.singletonList(hem));
        log.info("Sending OF_13 Hello to {}", channel.getRemoteAddress());
        channel.write(Collections.singletonList(mb.build()));
    }

    /**
     * Send featuresRequest msg to the switch using the handshake transactions ids.
     * @throws IOException
     */
    private void sendHandshakeFeaturesRequestMessage() throws IOException {
        OFFactory factory = (ofVersion == OFVersion.OF_13) ? factory13 : factory10;
        OFMessage m = factory.buildFeaturesRequest()
                .setXid(this.handshakeTransactionIds--)
                .build();
        channel.write(Collections.singletonList(m));
    }

    private void setSwitchRole(Role role) {
        sw.setRole(role);
    }

    /**
     * Send the configuration requests to tell the switch we want full
     * packets.
     * @throws IOException
     */
    private void sendHandshakeSetConfig() throws IOException {
        OFFactory factory = (ofVersion == OFVersion.OF_13) ? factory13 : factory10;
        //log.debug("Sending CONFIG_REQUEST to {}", channel.getRemoteAddress());
        List<OFMessage> msglist = new ArrayList<OFMessage>(3);

        // Ensure we receive the full packet via PacketIn
        // FIXME: We don't set the reassembly flags.
        OFSetConfig sc = factory
                .buildSetConfig()
                .setMissSendLen((short) 0xffff)
                .setXid(this.handshakeTransactionIds--)
                .build();
        msglist.add(sc);

        // Barrier
        OFBarrierRequest br = factory
                .buildBarrierRequest()
                .setXid(this.handshakeTransactionIds--)
                .build();
        msglist.add(br);

        // Verify (need barrier?)
        OFGetConfigRequest gcr = factory
                .buildGetConfigRequest()
                .setXid(this.handshakeTransactionIds--)
                .build();
        msglist.add(gcr);
        channel.write(msglist);
    }

    /**
     * send a description state request.
     * @throws IOException
     */
    private void sendHandshakeDescriptionStatsRequest() throws IOException {
        // Get Description to set switch-specific flags
        OFFactory factory = (ofVersion == OFVersion.OF_13) ? factory13 : factory10;
        OFDescStatsRequest dreq = factory
                .buildDescStatsRequest()
                .setXid(handshakeTransactionIds--)
                .build();
        channel.write(Collections.singletonList(dreq));
    }

    private void sendHandshakeOFPortDescRequest() throws IOException {
        // Get port description for 1.3 switch
        OFPortDescStatsRequest preq = factory13
                .buildPortDescStatsRequest()
                .setXid(handshakeTransactionIds--)
                .build();
        channel.write(Collections.singletonList(preq));
    }

    ChannelState getStateForTesting() {
        return state;
    }

    void useRoleChangerWithOtherTimeoutForTesting(long roleTimeoutMs) {
        roleChanger = new RoleChanger(roleTimeoutMs);
    }


}
