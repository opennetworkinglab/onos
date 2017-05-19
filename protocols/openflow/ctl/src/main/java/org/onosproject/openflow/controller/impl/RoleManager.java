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
package org.onosproject.openflow.controller.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.onosproject.openflow.controller.RoleState;
import org.onosproject.openflow.controller.driver.OpenFlowSwitchDriver;
import org.onosproject.openflow.controller.driver.RoleHandler;
import org.onosproject.openflow.controller.driver.RoleRecvStatus;
import org.onosproject.openflow.controller.driver.RoleReplyInfo;
import org.onosproject.openflow.controller.driver.SwitchStateException;
import org.projectfloodlight.openflow.protocol.OFControllerRole;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFErrorType;
import org.projectfloodlight.openflow.protocol.OFExperimenter;
import org.projectfloodlight.openflow.protocol.OFFactories;
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

import java.io.IOException;
import java.util.concurrent.TimeUnit;


/**
 * A utility class to handle role requests and replies for this channel.
 * After a role request is submitted the role changer keeps track of the
 * pending request, collects the reply (if any) and times out the request
 * if necessary.
 */
class RoleManager implements RoleHandler {
    protected static final long NICIRA_EXPERIMENTER = 0x2320;

    private static Logger log = LoggerFactory.getLogger(RoleManager.class);

    // The time until cached XID is evicted. Arbitrary for now.
    private final int pendingXidTimeoutSeconds = 60;

    // The cache for pending expected RoleReplies keyed on expected XID
    private Cache<Integer, RoleState> pendingReplies =
            CacheBuilder.newBuilder()
                .expireAfterWrite(pendingXidTimeoutSeconds, TimeUnit.SECONDS)
                .build();

    // the expectation set by the caller for the returned role
    private RoleRecvStatus expectation;
    private final OpenFlowSwitchDriver sw;


    public RoleManager(OpenFlowSwitchDriver sw) {
        this.expectation = RoleRecvStatus.MATCHED_CURRENT_ROLE;
        this.sw = sw;
    }

    /**
     * Send NX role request message to the switch requesting the specified
     * role.
     *
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
            roleToSend = OFNiciraControllerRole.ROLE_OTHER;
            log.debug("Sending Nx Role.SLAVE to switch {}.", sw);
        }
        int xid = sw.getNextTransactionId();
        OFExperimenter roleRequest = OFFactories.getFactory(OFVersion.OF_10)
                .buildNiciraControllerRoleRequest()
                .setXid(xid)
                .setRole(roleToSend)
                .build();
        sw.sendRoleRequest(roleRequest);
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

        int xid = sw.getNextTransactionId();
        OFRoleRequest rrm = sw.factory()
                .buildRoleRequest()
                .setRole(roleToSend)
                .setXid(xid)
                //FIXME fix below when we actually use generation ids
                .setGenerationId(U64.ZERO)
                .build();

        sw.sendRoleRequest(rrm);
        return xid;
    }

    @Override
    public synchronized boolean sendRoleRequest(RoleState role, RoleRecvStatus exp)
            throws IOException {
        this.expectation = exp;

        if (sw.factory().getVersion() == OFVersion.OF_10) {
            Boolean supportsNxRole = sw.supportNxRole();
            if (!supportsNxRole) {
                log.debug("Switch driver indicates no support for Nicira "
                        + "role request messages. Not sending ...");
                handleUnsentRoleMessage(role,
                        expectation);
                return false;
            }
            // OF1.0 switch with support for NX_ROLE_REQUEST vendor extn.
            // make Role.EQUAL become Role.SLAVE
            RoleState roleToSend = (role == RoleState.EQUAL) ? RoleState.SLAVE : role;
            pendingReplies.put(sendNxRoleRequest(roleToSend), role);
        } else {
            // OF1.3 switch, use OFPT_ROLE_REQUEST message
            pendingReplies.put(sendOF13RoleRequest(role), role);
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


    @Override
    public synchronized RoleRecvStatus deliverRoleReply(RoleReplyInfo rri)
            throws SwitchStateException {
        int xid = (int) rri.getXid();
        RoleState receivedRole = rri.getRole();
        RoleState expectedRole = pendingReplies.getIfPresent(xid);

        if (expectedRole == null) {
            RoleState currentRole = (sw != null) ? sw.getRole() : null;
            if (currentRole != null) {
                if (currentRole == rri.getRole()) {
                    // Don't disconnect if the role reply we received is
                    // for the same role we are already in.
                    // FIXME: but we do from the caller anyways.
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
                    + "Ignoring ...",
                      rri,
                      sw == null ? "(null)" : sw.getStringId());
            return RoleRecvStatus.OTHER_EXPECTATION;
        }

        // XXX Should check generation id meaningfully and other cases of expectations
        //if (pendingXid != xid) {
        //    log.info("Received older role reply from " +
        //            "switch {} ({}). Ignoring. " +
        //            "Waiting for {}, xid={}",
        //            new Object[] {sw.getStringId(), rri,
        //            pendingRole, pendingXid });
        //    return RoleRecvStatus.OLD_REPLY;
        //}
        sw.returnRoleReply(expectedRole, receivedRole);

        if (expectedRole == receivedRole) {
            log.debug("Received role reply message from {} that matched "
                    + "expected role-reply {} with expectations {}",
                    sw.getStringId(), receivedRole, expectation);

            // Done with this RoleReply; Invalidate
            pendingReplies.invalidate(xid);
            if (expectation == RoleRecvStatus.MATCHED_CURRENT_ROLE ||
                    expectation == RoleRecvStatus.MATCHED_SET_ROLE) {
                return expectation;
            } else {
                return RoleRecvStatus.OTHER_EXPECTATION;
            }
        }

        pendingReplies.invalidate(xid);
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
    @Override
    public synchronized RoleRecvStatus deliverError(OFErrorMsg error)
            throws SwitchStateException {
        RoleState errorRole = pendingReplies.getIfPresent(error.getXid());
        if (errorRole == null) {
            if (error.getErrType() == OFErrorType.ROLE_REQUEST_FAILED) {
                log.debug("Received an error msg from sw {} for a role request,"
                        + " but not for pending request in role-changer; "
                        + " ignoring error {} ...",
                        sw.getStringId(), error);
            } else {
                log.debug("Received an error msg from sw {}, but no pending "
                        + "requests in role-changer; not handling ...",
                        sw.getStringId());
            }
            return RoleRecvStatus.OTHER_EXPECTATION;
        }
        // it is an error related to a currently pending role request message
        if (error.getErrType() == OFErrorType.BAD_REQUEST) {
            log.error("Received a error msg {} from sw {} for "
                    + "pending role request {}. Switch driver indicates "
                    + "role-messaging is supported. Possible issues in "
                    + "switch driver configuration?",
                    ((OFBadRequestErrorMsg) error).toString(),
                    sw.getStringId(),
                    errorRole);
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
                        errorRole, rrerr);
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
     * @param experimenterMsg message
     * @return The role in the message if the message is a Nicira role
     * reply, null otherwise.
     * @throws SwitchStateException If the message is a Nicira role reply
     * but the numeric role value is unknown.
     */
    @Override
    public RoleState extractNiciraRoleReply(OFExperimenter experimenterMsg)
            throws SwitchStateException {
        int vendor = (int) experimenterMsg.getExperimenter();
        if (vendor != 0x2320) {
            return null;
        }
        OFNiciraControllerRoleReply nrr =
                (OFNiciraControllerRoleReply) experimenterMsg;

        RoleState role = null;
        OFNiciraControllerRole ncr = nrr.getRole();
        switch (ncr) {
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
     * Extract the role information from an OF1.3 Role Reply Message.
     *
     * @param rrmsg the role message
     * @return RoleReplyInfo object
     * @throws SwitchStateException if the role information could not be extracted.
     */
    @Override
    public RoleReplyInfo extractOFRoleReply(OFRoleReply rrmsg)
            throws SwitchStateException {
        OFControllerRole cr = rrmsg.getRole();
        RoleState role = null;
        switch (cr) {
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

