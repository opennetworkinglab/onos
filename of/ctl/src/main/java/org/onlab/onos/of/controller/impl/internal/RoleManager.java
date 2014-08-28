package org.onlab.onos.of.controller.impl.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.onlab.onos.of.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFControllerRole;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFErrorType;
import org.projectfloodlight.openflow.protocol.OFExperimenter;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFNiciraControllerRole;
import org.projectfloodlight.openflow.protocol.OFNiciraControllerRoleReply;
import org.projectfloodlight.openflow.protocol.OFRoleReply;
import org.projectfloodlight.openflow.protocol.OFRoleRequest;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.errormsg.OFBadRequestErrorMsg;
import org.projectfloodlight.openflow.protocol.errormsg.OFRoleRequestFailedErrorMsg;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
class RoleManager {
    protected static final long NICIRA_EXPERIMENTER = 0x2320;

    private static Logger log = LoggerFactory.getLogger(RoleManager.class);
    // indicates that a request is currently pending
    // needs to be volatile to allow correct double-check idiom
    private volatile boolean requestPending;
    // the transaction Id of the pending request
    private int pendingXid;
    // the role that's pending
    private RoleState pendingRole;

    // the expectation set by the caller for the returned role
    private RoleRecvStatus expectation;
    private AtomicInteger xidCounter;
    private AbstractOpenFlowSwitch sw;


    public RoleManager(AbstractOpenFlowSwitch sw) {
        this.requestPending = false;
        this.pendingXid = -1;
        this.pendingRole = null;
        this.xidCounter = new AtomicInteger(0);
        this.expectation = RoleRecvStatus.MATCHED_CURRENT_ROLE;
        this.sw = sw;
    }

    /**
     * Send NX role request message to the switch requesting the specified
     * role.
     *
     * @param sw switch to send the role request message to
     * @param role role to request
     */
    private int sendNxRoleRequest(RoleState role) throws IOException {
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
        int xid = xidCounter.getAndIncrement();
        OFExperimenter roleRequest = OFFactories.getFactory(OFVersion.OF_10)
                .buildNiciraControllerRoleRequest()
                .setXid(xid)
                .setRole(roleToSend)
                .build();
        sw.write(Collections.<OFMessage>singletonList(roleRequest));
        return xid;
    }

    private int sendOF13RoleRequest(RoleState role) throws IOException {
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

        int xid = xidCounter.getAndIncrement();
        OFRoleRequest rrm = OFFactories.getFactory(OFVersion.OF_13)
                .buildRoleRequest()
                .setRole(roleToSend)
                .setXid(xid)
                //FIXME fix below when we actually use generation ids
                .setGenerationId(U64.ZERO)
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
    synchronized boolean sendRoleRequest(RoleState role, RoleRecvStatus exp)
            throws IOException {
        this.expectation = exp;

        if (sw.factory().getVersion() == OFVersion.OF_10) {
            Boolean supportsNxRole = (Boolean)
                    sw.supportNxRole();
            if (!supportsNxRole) {
                log.debug("Switch driver indicates no support for Nicira "
                        + "role request messages. Not sending ...");
                handleUnsentRoleMessage(role,
                        expectation);
                return false;
            }
            // OF1.0 switch with support for NX_ROLE_REQUEST vendor extn.
            // make Role.EQUAL become Role.SLAVE
            role = (role == RoleState.EQUAL) ? RoleState.SLAVE : role;
            pendingXid = sendNxRoleRequest(role);
            pendingRole = role;
            requestPending = true;
        } else {
            // OF1.3 switch, use OFPT_ROLE_REQUEST message
            pendingXid = sendOF13RoleRequest(role);
            pendingRole = role;
            requestPending = true;
        }
        return true;
    }

    private void handleUnsentRoleMessage(RoleState role,
            RoleRecvStatus exp) throws IOException {
        // typically this is triggered for a switch where role messages
        // are not supported - we confirm that the role being set is
        // master
        if (exp != RoleRecvStatus.MATCHED_SET_ROLE) {

            log.error("Expected MASTER role from registry for switch "
                    + "which has no support for role-messages."
                    + "Received {}. It is possible that this switch "
                    + "is connected to other controllers, in which "
                    + "case it should support role messages - not "
                    + "moving forward.", role);

        }

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
            RoleState currentRole = (sw != null) ? sw.getRole() : null;
            if (currentRole != null) {
                if (currentRole == rri.getRole()) {
                    // Don't disconnect if the role reply we received is
                    // for the same role we are already in.
                    log.debug("Received unexpected RoleReply from "
                            + "Switch: {}. "
                            + "Role in reply is same as current role of this "
                            + "controller for this sw. Ignoring ...",
                            sw.getStringId());
                    return RoleRecvStatus.OTHER_EXPECTATION;
                } else {
                    String msg = String.format("Switch: [%s], "
                            + "received unexpected RoleReply[%s]. "
                            + "No roles are pending, and this controller's "
                            + "current role:[%s] does not match reply. "
                            + "Disconnecting switch ... ",
                            sw.getStringId(),
                            rri, currentRole);
                    throw new SwitchStateException(msg);
                }
            }
            log.debug("Received unexpected RoleReply {} from "
                    + "Switch: {}. "
                    + "This controller has no current role for this sw. "
                    + "Ignoring ...", new Object[] {rri,
                            sw.getStringId(), });
            return RoleRecvStatus.OTHER_EXPECTATION;
        }

        int xid = (int) rri.getXid();
        RoleState role = rri.getRole();
        // XXX S should check generation id meaningfully and other cases of expectations
        // U64 genId = rri.getGenId();

        if (pendingXid != xid) {
            log.debug("Received older role reply from " +
                    "switch {} ({}). Ignoring. " +
                    "Waiting for {}, xid={}",
                    new Object[] {sw.getStringId(), rri,
                    pendingRole, pendingXid });
            return RoleRecvStatus.OLD_REPLY;
        }

        if (pendingRole == role) {
            log.debug("Received role reply message from {} that matched "
                    + "expected role-reply {} with expectations {}",
                    new Object[] {sw.getStringId(), role, expectation});

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
                   sw.getStringId());
            return RoleRecvStatus.OTHER_EXPECTATION;
        }
        if (pendingXid != error.getXid()) {
            if (error.getErrType() == OFErrorType.ROLE_REQUEST_FAILED) {
                log.debug("Received an error msg from sw {} for a role request,"
                        + " but not for pending request in role-changer; "
                        + " ignoring error {} ...",
                        sw.getStringId(), error);
            }
            return RoleRecvStatus.OTHER_EXPECTATION;
        }
        // it is an error related to a currently pending role request message
        if (error.getErrType() == OFErrorType.BAD_REQUEST) {
            log.error("Received a error msg {} from sw {} for "
                    + "pending role request {}. Switch driver indicates "
                    + "role-messaging is supported. Possible issues in "
                    + "switch driver configuration?", new Object[] {
                            ((OFBadRequestErrorMsg) error).toString(),
                            sw.getStringId(), pendingRole
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
                String msgx = String.format("Switch: [%s], "
                        + "received Error to for pending role request [%s]. "
                        + "Error:[%s]. Disconnecting switch ... ",
                        sw.getStringId(),
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
    protected RoleState extractNiciraRoleReply(OFExperimenter experimenterMsg)
            throws SwitchStateException {
        int vendor = (int) experimenterMsg.getExperimenter();
        if (vendor != 0x2320) {
            return null;
        }
        OFNiciraControllerRoleReply nrr =
                (OFNiciraControllerRoleReply) experimenterMsg;

        RoleState role = null;
        OFNiciraControllerRole ncr = nrr.getRole();
        switch(ncr) {
        case ROLE_MASTER:
            role = RoleState.MASTER;
            break;
        case ROLE_OTHER:
            role = RoleState.EQUAL;
            break;
        case ROLE_SLAVE:
            role = RoleState.SLAVE;
            break;
        default: //handled below
        }

        if (role == null) {
            String msg = String.format("Switch: [%s], "
                    + "received NX_ROLE_REPLY with invalid role "
                    + "value %s",
                    sw.getStringId(),
                    nrr.getRole());
            throw new SwitchStateException(msg);
        }
        return role;
    }

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
     * Helper class returns role reply information in the format understood
     * by the controller.
     */
    protected static class RoleReplyInfo {
        private RoleState role;
        private U64 genId;
        private long xid;

        RoleReplyInfo(RoleState role, U64 genId, long xid) {
            this.role = role;
            this.genId = genId;
            this.xid = xid;
        }
        public RoleState getRole() { return role; }
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
    protected RoleReplyInfo extractOFRoleReply(OFRoleReply rrmsg)
            throws SwitchStateException {
        OFControllerRole cr = rrmsg.getRole();
        RoleState role = null;
        switch(cr) {
        case ROLE_EQUAL:
            role = RoleState.EQUAL;
            break;
        case ROLE_MASTER:
            role = RoleState.MASTER;
            break;
        case ROLE_SLAVE:
            role = RoleState.SLAVE;
            break;
        case ROLE_NOCHANGE: // switch should send current role
        default:
            String msg = String.format("Unknown controller role %s "
                    + "received from switch %s", cr, sw);
            throw new SwitchStateException(msg);
        }

        return new RoleReplyInfo(role, rrmsg.getGenerationId(), rrmsg.getXid());
    }

}


///**
// * We are waiting for a role reply message in response to a role request
// * sent after hearing back from the registry service -- OR -- we are
// * just waiting to hear back from the registry service in the case that
// * the switch does not support role messages. If completed successfully,
// * the controller's role for this switch will be set here.
// * Before we move to the state corresponding to the role, we allow the
// * switch specific driver to complete its configuration. This configuration
// * typically depends on the role the controller is playing for this switch.
// * And so we set the switch role (for 'this' controller) before we start
// * the driver-sub-handshake.
// * Next State: WAIT_SWITCH_DRIVER_SUB_HANDSHAKE
// */
//WAIT_INITIAL_ROLE(false) {
//    @Override
//    void processOFError(OFChannelHandler h, OFErrorMsg m)
//            throws SwitchStateException {
//        // role changer will ignore the error if it isn't for it
//        RoleRecvStatus rrstatus = h.roleChanger.deliverError(m);
//        if (rrstatus == RoleRecvStatus.OTHER_EXPECTATION) {
//            logError(h, m);
//        }
//    }
//
//    @Override
//    void processOFExperimenter(OFChannelHandler h, OFExperimenter m)
//            throws IOException, SwitchStateException {
//        Role role = extractNiciraRoleReply(h, m);
//        // If role == null it means the vendor (experimenter) message
//        // wasn't really a Nicira role reply. We ignore this case.
//        if (role != null) {
//            RoleReplyInfo rri = new RoleReplyInfo(role, null, m.getXid());
//            RoleRecvStatus rrs = h.roleChanger.deliverRoleReply(rri);
//            if (rrs == RoleRecvStatus.MATCHED_SET_ROLE) {
//                setRoleAndStartDriverHandshake(h, rri.getRole());
//            } // else do nothing - wait for the correct expected reply
//        } else {
//            unhandledMessageReceived(h, m);
//        }
//    }
//
//    @Override
//    void processOFRoleReply(OFChannelHandler h, OFRoleReply m)
//            throws SwitchStateException, IOException {
//        RoleReplyInfo rri = extractOFRoleReply(h, m);
//        RoleRecvStatus rrs = h.roleChanger.deliverRoleReply(rri);
//        if (rrs == RoleRecvStatus.MATCHED_SET_ROLE) {
//            setRoleAndStartDriverHandshake(h, rri.getRole());
//        } // else do nothing - wait for the correct expected reply
//    }
//
//    @Override
//    void handleUnsentRoleMessage(OFChannelHandler h, Role role,
//            RoleRecvStatus expectation) throws IOException {
//        // typically this is triggered for a switch where role messages
//        // are not supported - we confirm that the role being set is
//        // master and move to the next state
//        if (expectation == RoleRecvStatus.MATCHED_SET_ROLE) {
//            if (role == Role.MASTER) {
//                setRoleAndStartDriverHandshake(h, role);
//            } else {
//                log.error("Expected MASTER role from registry for switch "
//                        + "which has no support for role-messages."
//                        + "Received {}. It is possible that this switch "
//                        + "is connected to other controllers, in which "
//                        + "case it should support role messages - not "
//                        + "moving forward.", role);
//            }
//        } // else do nothing - wait to hear back from registry
//
//    }
//
//    private void setRoleAndStartDriverHandshake(OFChannelHandler h,
//            Role role) throws IOException {
//        h.setSwitchRole(role);
//        h.sw.startDriverHandshake();
//        if (h.sw.isDriverHandshakeComplete()) {
//            Role mySwitchRole = h.sw.getRole();
//            if (mySwitchRole == Role.MASTER) {
//                log.info("Switch-driver sub-handshake complete. "
//                        + "Activating switch {} with Role: MASTER",
//                        h.sw.getStringId());
//                handlePendingPortStatusMessages(h); //before activation
//                boolean success = h.sw.addActivatedMasterSwitch();
//                if (!success) {
//                    disconnectDuplicate(h);
//                    return;
//                }
//                h.setState(MASTER);
//            } else {
//                log.info("Switch-driver sub-handshake complete. "
//                        + "Activating switch {} with Role: EQUAL",
//                        h.sw.getStringId());
//                handlePendingPortStatusMessages(h); //before activation
//                boolean success = h.sw.addActivatedEqualSwitch();
//                if (!success) {
//                    disconnectDuplicate(h);
//                    return;
//                }
//                h.setState(EQUAL);
//            }
//        } else {
//            h.setState(WAIT_SWITCH_DRIVER_SUB_HANDSHAKE);
//        }
//    }
//
//    @Override
//    void processOFFeaturesReply(OFChannelHandler h, OFFeaturesReply  m)
//            throws IOException, SwitchStateException {
//        illegalMessageReceived(h, m);
//    }
//
//    @Override
//    void processOFStatisticsReply(OFChannelHandler h, OFStatsReply m)
//            throws SwitchStateException {
//        illegalMessageReceived(h, m);
//    }
//
//    @Override
//    void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
//            throws IOException, SwitchStateException {
//        h.pendingPortStatusMsg.add(m);
//
//    }
//},







///**
// * This controller is in EQUAL role for this switch. We enter this state
// * after some /other/ controller instance wins mastership-role over this
// * switch. The EQUAL role can be considered the same as the SLAVE role
// * if this controller does NOT send commands or packets to the switch.
// * This should always be true for OF1.0 switches. XXX S need to enforce.
// *
// * For OF1.3 switches, choosing this state as EQUAL instead of SLAVE,
// * gives us the flexibility that if an app wants to send commands/packets
// * to switches, it can, even thought it is running on a controller instance
// * that is not in a MASTER role for this switch. Of course, it is the job
// * of the app to ensure that commands/packets sent by this (EQUAL) controller
// * instance does not clash/conflict with commands/packets sent by the MASTER
// * controller for this switch. Neither the controller instances, nor the
// * switch provides any kind of resolution mechanism should conflicts occur.
// */
//EQUAL(true) {
//    @Override
//    void processOFError(OFChannelHandler h, OFErrorMsg m)
//            throws IOException, SwitchStateException {
//        // role changer will ignore the error if it isn't for it
//        RoleRecvStatus rrstatus = h.roleChanger.deliverError(m);
//        if (rrstatus == RoleRecvStatus.OTHER_EXPECTATION) {
//            logError(h, m);
//            h.dispatchMessage(m);
//        }
//    }
//
//    @Override
//    void processOFStatisticsReply(OFChannelHandler h,
//            OFStatsReply m) {
//        h.sw.handleMessage(m);
//    }
//
//    @Override
//    void processOFExperimenter(OFChannelHandler h, OFExperimenter m)
//            throws IOException, SwitchStateException {
//        Role role = extractNiciraRoleReply(h, m);
//        // If role == null it means the message wasn't really a
//        // Nicira role reply. We ignore it in this state.
//        if (role != null) {
//            RoleRecvStatus rrs = h.roleChanger.deliverRoleReply(
//                    new RoleReplyInfo(role, null, m.getXid()));
//            if (rrs == RoleRecvStatus.MATCHED_SET_ROLE) {
//                checkAndSetRoleTransition(h, role);
//            }
//        } else {
//            unhandledMessageReceived(h, m);
//        }
//    }
//
//    @Override
//    void processOFRoleReply(OFChannelHandler h, OFRoleReply m)
//            throws SwitchStateException, IOException {
//        RoleReplyInfo rri = extractOFRoleReply(h, m);
//        RoleRecvStatus rrs = h.roleChanger.deliverRoleReply(rri);
//        if (rrs == RoleRecvStatus.MATCHED_SET_ROLE) {
//            checkAndSetRoleTransition(h, rri.getRole());
//        }
//    }
//
//    // XXX S needs more handlers for 1.3 switches in equal role
//
//    @Override
//    void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
//            throws IOException, SwitchStateException {
//        handlePortStatusMessage(h, m, true);
//    }
//
//    @Override
//    @LogMessageDoc(level = "WARN",
//    message = "Received PacketIn from switch {} while "
//            + "being slave. Reasserting slave role.",
//            explanation = "The switch has receive a PacketIn despite being "
//                    + "in slave role indicating inconsistent controller roles",
//                    recommendation = "This situation can occurs transiently during role"
//                            + " changes. If, however, the condition persists or happens"
//                            + " frequently this indicates a role inconsistency. "
//                            + LogMessageDoc.CHECK_CONTROLLER)
//    void processOFPacketIn(OFChannelHandler h, OFPacketIn m) throws IOException {
//        // we don't expect packetIn while slave, reassert we are slave
//        h.counters.packetInWhileSwitchIsSlave.updateCounterNoFlush();
//        log.warn("Received PacketIn from switch {} while" +
//                "being slave. Reasserting slave role.", h.sw);
//        //h.controller.reassertRole(h, Role.SLAVE);
//        // XXX reassert in role changer
//    }
//};
