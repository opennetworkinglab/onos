/*
 * Copyright 2014 Open Networking Laboratory
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

import java.util.List;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;

/**
 * Represents to provider facing side of a switch.
 */
public interface OpenFlowSwitch {

    /**
     * Writes the message to the driver.
     *
     * @param msg the message to write
     */
    public void sendMsg(OFMessage msg);

    /**
     * Writes to the OFMessage list to the driver.
     *
     * @param msgs the messages to be written
     */
    public void sendMsg(List<OFMessage> msgs);

    /**
     * Handle a message from the switch.
     * @param fromSwitch the message to handle
     */
    public void handleMessage(OFMessage fromSwitch);

    /**
     * Sets the role for this switch.
     * @param role the role to set.
     */
    public void setRole(RoleState role);

    /**
     * Fetch the role for this switch.
     * @return the role.
     */
    public RoleState getRole();

    /**
     * Fetches the ports of this switch.
     * @return unmodifiable list of the ports.
     */
    public List<OFPortDesc> getPorts();

    /**
     * Provides the factory for this OF version.
     * @return OF version specific factory.
     */
    public OFFactory factory();

    /**
     * Gets a string version of the ID for this switch.
     *
     * @return string version of the ID
     */
    public String getStringId();

    /**
     * Gets the datapathId of the switch.
     *
     * @return the switch dpid in long format
     */
    public long getId();

    /**
     * fetch the manufacturer description.
     * @return the description
     */
    public String manufacturerDescription();

    /**
     * fetch the datapath description.
     * @return the description
     */
    public String datapathDescription();

    /**
     * fetch the hardware description.
     * @return the description
     */
    public String hardwareDescription();

    /**
     * fetch the software description.
     * @return the description
     */
    public String softwareDescription();

    /**
     * fetch the serial number.
     * @return the serial
     */
    public String serialNumber();

    /**
     * Checks if the switch is still connected.
     *
     * @return whether the switch is still connected
     */
    public boolean isConnected();

    /**
     * Disconnects the switch by closing the TCP connection. Results in a call
     * to the channel handler's channelDisconnected method for cleanup
     */
    public void disconnectSwitch();

    /**
     * Notifies the controller that the device has responded to a set-role request.
     *
     * @param requested the role requested by the controller
     * @param response the role set at the device
     */
    public void returnRoleReply(RoleState requested, RoleState response);

    /**
     * Indicates if this switch is optical.
     *
     * @return true if optical
     */
    public boolean isOptical();

    /**
     * Identifies the channel used to communicate with the switch.
     */
    public String channelId();

}
