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
package org.onosproject.openflow.controller;

import org.onosproject.net.Device;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMeterFeatures;
import org.projectfloodlight.openflow.protocol.OFPortDesc;

import java.util.List;

/**
 * Represents to provider facing side of a switch.
 */
public interface OpenFlowSwitch {

    /**
     * Writes the message to the driver.
     * <p>
     * Note: Messages may be silently dropped/lost due to IOExceptions or
     * role. If this is a concern, then a caller should use barriers.
     * </p>
     *
     * @param msg the message to write
     */
    void sendMsg(OFMessage msg);

    /**
     * Writes the OFMessage list to the driver.
     * <p>
     * Note: Messages may be silently dropped/lost due to IOExceptions or
     * role. If this is a concern, then a caller should use barriers.
     * </p>
     *
     * @param msgs the messages to be written
     */
    void sendMsg(List<OFMessage> msgs);

    /**
     * Handle a message from the switch.
     * @param fromSwitch the message to handle
     */
    void handleMessage(OFMessage fromSwitch);

    /**
     * Sets the role for this switch.
     * @param role the role to set.
     */
    void setRole(RoleState role);

    /**
     * Fetch the role for this switch.
     * @return the role.
     */
    RoleState getRole();

    /**
     * Fetches the ports of this switch.
     * @return unmodifiable list of the ports.
     */
    List<OFPortDesc> getPorts();

    /**
     * Fetches the meter features of this switch.
     * @return unmodifiable meter features
     */
    OFMeterFeatures getMeterFeatures();

    /**
     * Provides the factory for this OF version.
     * @return OF version specific factory.
     */
    OFFactory factory();

    /**
     * Gets a string version of the ID for this switch.
     *
     * @return string version of the ID
     */
    String getStringId();

    /**
     * Gets the datapathId of the switch.
     *
     * @return the switch dpid in long format
     */
    long getId();

    /**
     * Gets the datapathId of the switch.
     *
     * @return the switch dpid
     */
    default Dpid getDpid() {
        return new Dpid(getId());
    }

    /**
     * fetch the manufacturer description.
     * @return the description
     */
    String manufacturerDescription();

    /**
     * fetch the datapath description.
     * @return the description
     */
    String datapathDescription();

    /**
     * fetch the hardware description.
     * @return the description
     */
    String hardwareDescription();

    /**
     * fetch the software description.
     * @return the description
     */
    String softwareDescription();

    /**
     * fetch the serial number.
     * @return the serial
     */
    String serialNumber();

    /**
     * Checks if the switch is still connected.
     *
     * @return whether the switch is still connected
     */
    boolean isConnected();

    /**
     * Disconnects the switch by closing the TCP connection. Results in a call
     * to the channel handler's channelDisconnected method for cleanup
     */
    void disconnectSwitch();

    /**
     * Notifies the controller that the device has responded to a set-role request.
     *
     * @param requested the role requested by the controller
     * @param response the role set at the device
     */
    void returnRoleReply(RoleState requested, RoleState response);

    /**
     * Returns the switch device type.
     *
     * @return device type
     */
    Device.Type deviceType();

    /**
     * Identifies the channel used to communicate with the switch.
     *
     * @return string representation of the connection to the device
     */
    String channelId();
}
