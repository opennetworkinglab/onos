package org.onlab.onos.openflow.controller.driver;

import org.onlab.onos.openflow.controller.Dpid;
import org.onlab.onos.openflow.controller.OpenFlowSwitch;
import org.projectfloodlight.openflow.protocol.OFMessage;

/**
 * Responsible for keeping track of the current set of switches
 * connected to the system. As well as whether they are in Master
 * role or not.
 *
 */
public interface OpenFlowAgent {

    /**
     * Add a switch that has just connected to the system.
     * @param dpid the dpid to add
     * @param sw the actual switch object.
     * @return true if added, false otherwise.
     */
    public boolean addConnectedSwitch(Dpid dpid, OpenFlowSwitch sw);

    /**
     * Checks if the activation for this switch is valid.
     * @param dpid the dpid to check
     * @return true if valid, false otherwise
     */
    public boolean validActivation(Dpid dpid);

    /**
     * Called when a switch is activated, with this controller's role as MASTER.
     * @param dpid the dpid to add.
     * @param sw the actual switch
     * @return true if added, false otherwise.
     */
    public boolean addActivatedMasterSwitch(Dpid dpid, OpenFlowSwitch sw);

    /**
     * Called when a switch is activated, with this controller's role as EQUAL.
     * @param dpid the dpid to add.
     * @param sw the actual switch
     * @return true if added, false otherwise.
     */
    public boolean addActivatedEqualSwitch(Dpid dpid, OpenFlowSwitch sw);

    /**
     * Called when this controller's role for a switch transitions from equal
     * to master. For 1.0 switches, we internally refer to the role 'slave' as
     * 'equal' - so this transition is equivalent to 'addActivatedMasterSwitch'.
     * @param dpid the dpid to transistion.
     */
    public void transitionToMasterSwitch(Dpid dpid);

    /**
     * Called when this controller's role for a switch transitions to equal.
     * For 1.0 switches, we internally refer to the role 'slave' as
     * 'equal'.
     * @param dpid the dpid to transistion.
     */
    public void transitionToEqualSwitch(Dpid dpid);

    /**
     * Clear all state in controller switch maps for a switch that has
     * disconnected from the local controller. Also release control for
     * that switch from the global repository. Notify switch listeners.
     * @param dpid the dpid to remove.
     */
    public void removeConnectedSwitch(Dpid dpid);

    /**
     * Process a message coming from a switch.
     *
     * @param dpid the dpid the message came on.
     * @param m the message to process
     */
    public void processMessage(Dpid dpid, OFMessage m);
}
