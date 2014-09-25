package org.onlab.onos.openflow.controller;

import org.projectfloodlight.openflow.protocol.OFPortStatus;

/**
 * Allows for providers interested in Switch events to be notified.
 */
public interface OpenFlowSwitchListener {

    /**
     * Notify that the switch was added.
     * @param dpid the switch where the event occurred
     */
    public void switchAdded(Dpid dpid);

    /**
     * Notify that the switch was removed.
     * @param dpid the switch where the event occurred.
     */
    public void switchRemoved(Dpid dpid);

    /**
     * Notify that a port has changed.
     * @param dpid the switch on which the change happened.
     * @param status the new state of the port.
     */
    public void portChanged(Dpid dpid, OFPortStatus status);

    /**
     * Notify that a role imposed on a switch failed to take hold.
     *
     * @param dpid the switch that failed role assertion
     * @param role the role imposed by the controller
     */
    public void roleAssertFailed(Dpid dpid, RoleState role);
}
