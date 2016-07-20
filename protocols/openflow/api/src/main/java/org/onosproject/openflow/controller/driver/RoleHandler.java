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
package org.onosproject.openflow.controller.driver;

import java.io.IOException;

import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFExperimenter;
import org.projectfloodlight.openflow.protocol.OFRoleReply;

/**
 * Role handling.
 *
 */
public interface RoleHandler {

    /**
     * Extract the role from an OFVendor message.
     *
     * Extract the role from an OFVendor message if the message is a
     * Nicira role reply. Otherwise return null.
     *
     * @param experimenterMsg The vendor message to parse.
     * @return The role in the message if the message is a Nicira role
     * reply, null otherwise.
     * @throws SwitchStateException If the message is a Nicira role reply
     * but the numeric role value is unknown.
     */
    RoleState extractNiciraRoleReply(OFExperimenter experimenterMsg)
            throws SwitchStateException;

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
     * @param role role to request
     * @param exp expectation
     * @throws IOException when I/O exception of some sort has occurred
     * @return false if and only if the switch does not support role-request
     * messages, according to the switch driver; true otherwise.
     */
    boolean sendRoleRequest(RoleState role, RoleRecvStatus exp)
            throws IOException;

    /**
     * Extract the role information from an OF1.3 Role Reply Message.
     * @param rrmsg role reply message
     * @return RoleReplyInfo object
     * @throws SwitchStateException If unknown role encountered
     */
    RoleReplyInfo extractOFRoleReply(OFRoleReply rrmsg)
            throws SwitchStateException;

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
     * @param rri information about role-reply in format that
     *                      controller can understand.
     * @return result comparing expected and received reply
     * @throws SwitchStateException if no request is pending
     */
    RoleRecvStatus deliverRoleReply(RoleReplyInfo rri)
            throws SwitchStateException;


    /**
     * Called if we receive an  error message. If the xid matches the
     * pending request we handle it otherwise we ignore it.
     *
     * Note: since we only keep the last pending request we might get
     * error messages for earlier role requests that we won't be able
     * to handle
     * @param error error message
     * @return result comparing expected and received reply
     * @throws SwitchStateException if switch did not support requested role
     */
    RoleRecvStatus deliverError(OFErrorMsg error)
            throws SwitchStateException;

}
