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

import org.onosproject.net.driver.HandlerBehaviour;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowSession;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMeterFeaturesStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFVersion;

import java.util.List;

/**
 * Represents the driver side of an OpenFlow switch.
 * This interface should never be exposed to consumers.
 *
 */
public interface OpenFlowSwitchDriver extends OpenFlowSwitch, HandlerBehaviour {

    /**
     * Sets the OpenFlow agent to be used. This method
     * can only be called once.
     * @param agent the agent to set.
     */
    void setAgent(OpenFlowAgent agent);

    /**
     * Sets the Role handler object.
     * This method can only be called once.
     * @param roleHandler the roleHandler class
     */
    void setRoleHandler(RoleHandler roleHandler);

    /**
     * Reasserts this controllers role to the switch.
     * Useful in cases where the switch no longer agrees
     * that this controller has the role it claims.
     */
    void reassertRole();

    /**
     * Handle the situation where the role request triggers an error.
     * @param error the error to handle.
     * @return true if handled, false if not.
     */
    boolean handleRoleError(OFErrorMsg error);

    /**
     * If this driver know of Nicira style role messages, these should
     * be handled here.
     * @param m the role message to handle.
     * @throws SwitchStateException if the message received was
     *  not a nicira role or was malformed.
     */
    void handleNiciraRole(OFMessage m) throws SwitchStateException;

    /**
     * Handle OF 1.x (where x &gt; 0) role messages.
     * @param m the role message to handle
     * @throws SwitchStateException if the message received was
     *  not a nicira role or was malformed.
     */
    void handleRole(OFMessage m) throws SwitchStateException;

    /**
     * Announce to the OpenFlow agent that this switch has connected.
     * @return true if successful, false if duplicate switch.
     */
    boolean connectSwitch();

    /**
     * Activate this MASTER switch-controller relationship in the OF agent.
     * @return true is successful, false is switch has not
     * connected or is unknown to the system.
     */
    boolean activateMasterSwitch();

    /**
     * Activate this EQUAL switch-controller relationship in the OF agent.
     * @return true is successful, false is switch has not
     * connected or is unknown to the system.
     */
    boolean activateEqualSwitch();

    /**
     * Transition this switch-controller relationship to an EQUAL state.
     */
    void transitionToEqualSwitch();

    /**
     * Transition this switch-controller relationship to an Master state.
     */
    void transitionToMasterSwitch();

    /**
     * Remove this switch from the openflow agent.
     */
    void removeConnectedSwitch();

    /**
     * Sets the ports on this switch.
     * @param portDescReply the port set and descriptions
     */
    void setPortDescReply(OFPortDescStatsReply portDescReply);

    /**
     * Sets the ports on this switch.
     * @param portDescReplies list of port set and descriptions
     */
    void setPortDescReplies(List<OFPortDescStatsReply> portDescReplies);

    /**
     * Sets the features reply for this switch.
     * @param featuresReply the features to set.
     */
    void setFeaturesReply(OFFeaturesReply featuresReply);

    /**
     *  Sets the meter features reply for this switch.
     * @param meterFeaturesReply the meter features to set.
     */
    void setMeterFeaturesReply(OFMeterFeaturesStatsReply meterFeaturesReply);

    /**
     * Sets the switch description.
     * @param desc the descriptions
     */
    void setSwitchDescription(OFDescStatsReply desc);

    /**
     * Gets the next transaction id to use.
     * @return the xid
     */
    int getNextTransactionId();


    /**
     * Sets the OF version for this switch.
     * @param ofV the version to set.
     */
    void setOFVersion(OFVersion ofV);

    /**
     * Sets this switch has having a full flowtable.
     * @param full true if full, false otherswise.
     */
    void setTableFull(boolean full);

    /**
     * Sets the associated OpenFlow session for this switch.
     *
     * @param session the OpenFlow session
     */
    void setChannel(OpenFlowSession session);

    /**
     * Sets whether the switch is connected.
     *
     * @param connected whether the switch is connected
     */
    void setConnected(boolean connected);

    /**
     * Initialises the behaviour.
     * @param dpid a dpid
     * @param desc a switch description
     * @param ofv OpenFlow version
     */
    void init(Dpid dpid, OFDescStatsReply desc, OFVersion ofv);

    /**
     * Does this switch support Nicira Role messages.
     * <p>
     * Only relevant if this Device is OpenFlow 1.0.
     *
     * @return true if supports, false otherwise.
     */
    Boolean supportNxRole();


    /**
     * Starts the driver specific handshake process.
     */
    void startDriverHandshake();

    /**
     * Checks whether the driver specific handshake is complete.
     * @return true is finished, false if not.
     */
    boolean isDriverHandshakeComplete();

    /**
     * Process a message during the driver specific handshake.
     * @param m the message to process.
     */
    void processDriverHandshakeMessage(OFMessage m);

    /**
     * Sends only role request messages.
     *
     * @param message a role request message.
     */
    void sendRoleRequest(OFMessage message);

    /**
     * Allows the handshaker behaviour to send messages during the
     * handshake phase only.
     *
     * @param message an OpenFlow message
     */
    void sendHandshakeMessage(OFMessage message);

}
