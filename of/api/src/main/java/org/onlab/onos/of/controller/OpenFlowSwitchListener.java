package org.onlab.onos.of.controller;

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

}
