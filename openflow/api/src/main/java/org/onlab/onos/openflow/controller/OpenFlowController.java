package org.onlab.onos.openflow.controller;

import org.projectfloodlight.openflow.protocol.OFMessage;

/**
 * Abstraction of an OpenFlow controller. Serves as a one stop
 * shop for obtaining OpenFlow devices and (un)register listeners
 * on OpenFlow events
 */
public interface OpenFlowController {

    /**
     * Returns all switches known to this OF controller.
     * @return Iterable of dpid elements
     */
    public Iterable<OpenFlowSwitch> getSwitches();

    /**
     * Returns all master switches known to this OF controller.
     * @return Iterable of dpid elements
     */
    public Iterable<OpenFlowSwitch> getMasterSwitches();

    /**
     * Returns all equal switches known to this OF controller.
     * @return Iterable of dpid elements
     */
    public Iterable<OpenFlowSwitch> getEqualSwitches();


    /**
     * Returns the actual switch for the given Dpid.
     * @param dpid the switch to fetch
     * @return the interface to this switch
     */
    public OpenFlowSwitch getSwitch(Dpid dpid);

    /**
     * Returns the actual master switch for the given Dpid, if one exists.
     * @param dpid the switch to fetch
     * @return the interface to this switch
     */
    public OpenFlowSwitch getMasterSwitch(Dpid dpid);

    /**
     * Returns the actual equal switch for the given Dpid, if one exists.
     * @param dpid the switch to fetch
     * @return the interface to this switch
     */
    public OpenFlowSwitch getEqualSwitch(Dpid dpid);

    /**
     * Register a listener for meta events that occur to OF
     * devices.
     * @param listener the listener to notify
     */
    public void addListener(OpenFlowSwitchListener listener);

    /**
     * Unregister a listener.
     *
     * @param listener the listener to unregister
     */
    public void removeListener(OpenFlowSwitchListener listener);

    /**
     * Register a listener for packet events.
     * @param priority the importance of this listener, lower values are more important
     * @param listener the listener to notify
     */
    public void addPacketListener(int priority, PacketListener listener);

    /**
     * Unregister a listener.
     *
     * @param listener the listener to unregister
     */
    public void removePacketListener(PacketListener listener);

    /**
     * Register a listener for OF msg events.
     *
     * @param listener the listener to notify
     */
    public void addEventListener(OpenFlowEventListener listener);

    /**
     * Unregister a listener.
     *
     * @param listener the listener to unregister
     */
    public void removeEventListener(OpenFlowEventListener listener);

    /**
     * Send a message to a particular switch.
     * @param dpid the switch to send to.
     * @param msg the message to send
     */
    public void write(Dpid dpid, OFMessage msg);

    /**
     * Process a message and notify the appropriate listeners.
     *
     * @param dpid the dpid the message arrived on
     * @param msg the message to process.
     */
    public void processPacket(Dpid dpid, OFMessage msg);

    /**
     * Sets the role for a given switch.
     * @param role the desired role
     * @param dpid the switch to set the role for.
     */
    public void setRole(Dpid dpid, RoleState role);
}
