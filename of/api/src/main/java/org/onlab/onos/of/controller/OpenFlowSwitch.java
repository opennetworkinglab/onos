package org.onlab.onos.of.controller;

import org.projectfloodlight.openflow.protocol.OFMessage;

/**
 * Abstract model of an OpenFlow Switch.
 *
 */
public interface OpenFlowSwitch {

    /**
     * Writes the message to this switch.
     *
     * @param msg the message to write
     */
    public void write(OFMessage msg);

    /**
     * Handle a message from the switch.
     * @param fromSwitch the message to handle
     */
    public void handleMessage(OFMessage fromSwitch);
}
