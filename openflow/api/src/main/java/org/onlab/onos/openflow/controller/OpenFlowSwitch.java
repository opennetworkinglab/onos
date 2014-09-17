package org.onlab.onos.openflow.controller;

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
    public String manfacturerDescription();

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
     * Disconnects the switch by closing the TCP connection. Results in a call
     * to the channel handler's channelDisconnected method for cleanup
     */
    public void disconnectSwitch();

}
