package org.onlab.onos.openflow.controller;

import org.projectfloodlight.openflow.protocol.OFMessage;


/**
 * Notifies providers about openflow msg events.
 */
public interface OpenFlowEventListener {

    /**
     * Handles the message event.
     *
     * @param msg the message
     */
    public void handleMessage(Dpid dpid, OFMessage msg);
}
