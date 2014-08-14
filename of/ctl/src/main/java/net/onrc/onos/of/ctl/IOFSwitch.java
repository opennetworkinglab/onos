/**
 *    Copyright 2011, Big Switch Networks, Inc.
 *    Originally created by David Erickson, Stanford University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package net.onrc.onos.of.ctl;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import net.onrc.onos.of.ctl.debugcounter.IDebugCounterService;
import net.onrc.onos.of.ctl.debugcounter.IDebugCounterService.CounterException;
import net.onrc.onos.of.ctl.util.OrderedCollection;

import org.jboss.netty.channel.Channel;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFCapabilities;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.U64;


public interface IOFSwitch {

    /**
     * OF1.3 switches should support role-request messages as in the 1.3 spec.
     * OF1.0 switches may or may not support the Nicira role request extensions.
     * To indicate the support, this property should be set by the associated
     * OF1.0 switch driver in the net.onrc.onos.core.drivermanager package.
     * The property will be ignored for OF1.3 switches.
     */
    public static final String SWITCH_SUPPORTS_NX_ROLE = "supportsNxRole";


    //************************
    // Channel related
    //************************

    /**
     * Disconnects the switch by closing the TCP connection. Results in a call
     * to the channel handler's channelDisconnected method for cleanup
     * @throws IOException
     */
    public void disconnectSwitch();

    /**
     * Writes to the OFMessage to the output stream.
     * The message will be handed to the floodlightProvider for possible filtering
     * and processing by message listeners
     *
     * @param m
     * @param bc
     * @throws IOException
     */
    public void write(OFMessage m) throws IOException;

    /**
     * Writes the list of messages to the output stream.
     * The message will be handed to the floodlightProvider for possible filtering
     * and processing by message listeners.
     *
     * @param msglist
     * @param bc
     * @throws IOException
     */
    public void write(List<OFMessage> msglist) throws IOException;

    /**
     * Gets the date the switch connected to this controller.
     *
     * @return the date
     */
    public Date getConnectedSince();

    /**
     * Gets the next available transaction id.
     *
     * @return the next transaction ID
     */
    public int getNextTransactionId();

    /**
     * Checks if the switch is still connected.
     * Only call while holding processMessageLock
     *
     * @return whether the switch is still disconnected
     */
    public boolean isConnected();

    /**
     * Sets whether the switch is connected.
     * Only call while holding modifySwitchLock
     *
     * @param connected whether the switch is connected
     */
    public void setConnected(boolean connected);

    /**
     * Flushes all flows queued for this switch in the current thread.
     * NOTE: The contract is limited to the current thread
     */
    public void flush();

    /**
     * Sets the Netty Channel this switch instance is associated with.
     * <p>
     * Called immediately after instantiation
     *
     * @param channel the channel
     */
    public void setChannel(Channel channel);

    //************************
    // Switch features related
    //************************

    /**
     * Gets the datapathId of the switch.
     *
     * @return the switch buffers
     */
    public long getId();

    /**
     * Gets a string version of the ID for this switch.
     *
     * @return string version of the ID
     */
    public String getStringId();

    /**
     * Gets the number of buffers.
     *
     * @return the number of buffers
     */
    public int getNumBuffers();

    public Set<OFCapabilities> getCapabilities();

    public byte getNumTables();

    /**
     * Returns an OFDescStatsReply message object. Use the methods contained
     * to retrieve switch descriptions for Manufacturer, Hw/Sw version etc.
     */
    public OFDescStatsReply getSwitchDescription();

    /**
     * Cancel features reply with a specific transaction ID.
     * @param transactionId the transaction ID
     */
    public void cancelFeaturesReply(int transactionId);

    /**
     * Gets the OFActionType set.
     * <p>
     * getActions has relevance only for an OpenFlow 1.0 switch.
     * For OF1.3, each table can support different actions
     *
     * @return the action set
     */
    public Set<OFActionType> getActions();

    public void setOFVersion(OFVersion ofv);

    public OFVersion getOFVersion();


    //************************
    //  Switch port related
    //************************

    /**
     * the type of change that happened to an open flow port.
     */
    public enum PortChangeType {
        /** Either a new port has been added by the switch, or we are
         * adding a port we just deleted (via a prior notification) due to
         * a change in the portNumber-portName mapping.
         */
        ADD,
        /** some other feature of the port has changed (eg. speed)*/
        OTHER_UPDATE,
        /** Either a port has been deleted by the switch, or we are deleting
         * a port whose portNumber-portName mapping has changed. Note that in
         * the latter case, a subsequent notification will be sent out to add a
         * port with the new portNumber-portName mapping.
         */
        DELETE,
        /** Port is up (i.e. enabled). Presumably an earlier notification had
         * indicated that it was down. To be UP implies that the port is
         * administratively considered UP (see ofp_port_config) AND the port
         * link is up AND the port is no longer blocked (see ofp_port_state).
         */
        UP,
        /** Port is down (i.e. disabled). Presumably an earlier notification had
         * indicated that it was up, or the port was always up.
         * To be DOWN implies that the port has been either
         * administratively brought down (see ofp_port_config) OR the port
         * link is down OR the port is blocked (see ofp_port_state).
         */
        DOWN,
    }

    /**
     * Describes a change of an open flow port.
     */
    public static class PortChangeEvent {
        public final OFPortDesc port;
        public final PortChangeType type;
        /**
         * @param port
         * @param type
         */
        public PortChangeEvent(OFPortDesc port,
                               PortChangeType type) {
            this.port = port;
            this.type = type;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((port == null) ? 0 : port.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            PortChangeEvent other = (PortChangeEvent) obj;
            if (port == null) {
                if (other.port != null) {
                    return false;
                }
            } else if (!port.equals(other.port)) {
                return false;
            }
            if (type != other.type) {
                return false;
            }
            return true;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "[" + type + " " + port.toString() + "]";
        }
    }


    /**
     * Get list of all enabled ports. This will typically be different from
     * the list of ports in the OFFeaturesReply, since that one is a static
     * snapshot of the ports at the time the switch connected to the controller
     * whereas this port list also reflects the port status messages that have
     * been received.
     *
     * @return Unmodifiable list of ports not backed by the underlying collection
     */
    public Collection<OFPortDesc> getEnabledPorts();

    /**
     * Get list of the port numbers of all enabled ports. This will typically
     * be different from the list of ports in the OFFeaturesReply, since that
     * one is a static snapshot of the ports at the time the switch connected
     * to the controller whereas this port list also reflects the port status
     * messages that have been received.
     *
     * @return Unmodifiable list of ports not backed by the underlying collection
     */
    public Collection<Integer> getEnabledPortNumbers();

    /**
     * Retrieve the port object by the port number. The port object
     * is the one that reflects the port status updates that have been
     * received, not the one from the features reply.
     *
     * @param portNumber
     * @return port object
     */
    public OFPortDesc getPort(int portNumber);

    /**
     * Retrieve the port object by the port name. The port object
     * is the one that reflects the port status updates that have been
     * received, not the one from the features reply.
     *
     * @param portName
     * @return port object
     */
    public OFPortDesc getPort(String portName);

    /**
     * Add or modify a switch port. This is called by the core controller
     * code in response to a OFPortStatus message. It should not typically be
     * called by other floodlight applications.
     *
     * OFPPR_MODIFY and OFPPR_ADD will be treated as equivalent. The OpenFlow
     * spec is not clear on whether portNames are portNumbers are considered
     * authoritative identifiers. We treat portNames <-> portNumber mappings
     * as fixed. If they change, we delete all previous conflicting ports and
     * add all new ports.
     *
     * @param ps the port status message
     * @return the ordered Collection of changes "applied" to the old ports
     * of the switch according to the PortStatus message. A single PortStatus
     * message can result in multiple changes.
     * If portName <-> portNumber mappings have
     * changed, the iteration order ensures that delete events for old
     * conflicting appear before before events adding new ports
     */
    public OrderedCollection<PortChangeEvent> processOFPortStatus(OFPortStatus ps);

    /**
     * Get list of all ports. This will typically be different from
     * the list of ports in the OFFeaturesReply, since that one is a static
     * snapshot of the ports at the time the switch connected to the controller
     * whereas this port list also reflects the port status messages that have
     * been received.
     *
     * @return Unmodifiable list of ports
     */
    public Collection<OFPortDesc> getPorts();

    /**
     * @param portName
     * @return Whether a port is enabled per latest port status message
     * (not configured down nor link down nor in spanning tree blocking state)
     */
    public boolean portEnabled(int portName);

    /**
     * @param portNumber
     * @return Whether a port is enabled per latest port status message
     * (not configured down nor link down nor in spanning tree blocking state)
     */
    public boolean portEnabled(String portName);

    /**
     * Compute the changes that would be required to replace the old ports
     * of this switch with the new ports.
     * @param ports new ports to set
     * @return the ordered collection of changes "applied" to the old ports
     * of the switch in order to set them to the new set.
     * If portName <-> portNumber mappings have
     * changed, the iteration order ensures that delete events for old
     * conflicting appear before before events adding new ports
     */
    public OrderedCollection<PortChangeEvent>
            comparePorts(Collection<OFPortDesc> ports);

    /**
     * Replace the ports of this switch with the given ports.
     * @param ports new ports to set
     * @return the ordered collection of changes "applied" to the old ports
     * of the switch in order to set them to the new set.
     * If portName <-> portNumber mappings have
     * changed, the iteration order ensures that delete events for old
     * conflicting appear before before events adding new ports
     */
    public OrderedCollection<PortChangeEvent>
            setPorts(Collection<OFPortDesc> ports);

//  XXX S The odd use of providing an API call to 'set ports' (above) would
//  logically suggest that there should be a way to delete or unset the ports.
//  Right now we forbid this. We should probably not use setPorts too.
//
//  /**
//   * Delete a port for the switch. This is called by the core controller
//   * code in response to a OFPortStatus message. It should not typically be
//   * called by other floodlight applications.
//   *
//   * @param portNumber
//   */
//  public void deletePort(short portNumber);
//
//  /**
//   * Delete a port for the switch. This is called by the core controller
//   * code in response to a OFPortStatus message. It should not typically be
//   * called by other floodlight applications.
//   *
//   * @param portName
//   */
//  public void deletePort(String portName);


    //*******************************************
    //  IOFSwitch object attributes
    //************************

    /**
     * Gets attributes of this switch.
     *
     * @return attributes of the switch
     */
    public Map<Object, Object> getAttributes();

    /**
     * Checks if a specific switch property exists for this switch.
     *
     * @param name name of property
     * @return value for name
     */
    boolean hasAttribute(String name);

    /**
     * Gets properties for switch specific behavior.
     *
     * @param name name of property
     * @return 'value' for 'name', or null if no entry for 'name' exists
     */
    Object getAttribute(String name);

    /**
     * Sets properties for switch specific behavior.
     *
     * @param name  name of property
     * @param value value for name
     */
    void setAttribute(String name, Object value);

    /**
     * Removes properties for switch specific behavior.
     *
     * @param name name of property
     * @return current value for name or null (if not present)
     */
    Object removeAttribute(String name);

    //************************
    //  Switch statistics
    //************************

    /**
     * Delivers the statistics future reply.
     *
     * @param reply the reply to deliver
     */
    public void deliverStatisticsReply(OFMessage reply);

    /**
     * Cancels the statistics reply with the given transaction ID.
     *
     * @param transactionId the transaction ID
     */
    public void cancelStatisticsReply(int transactionId);

    /**
     * Cancels all statistics replies.
     */
    public void cancelAllStatisticsReplies();

    /**
     * Gets a Future object that can be used to retrieve the asynchronous.
     * OFStatisticsReply when it is available.
     *
     * @param request statistics request
     * @return Future object wrapping OFStatisticsReply
     * @throws IOException
     */
    public Future<List<OFStatsReply>> getStatistics(OFStatsRequest<?> request)
            throws IOException;

    //************************
    //  Switch other utilities
    //************************

    /**
     * Clears all flowmods on this switch.
     */
    public void clearAllFlowMods();

    /**
     * Gets the current role of this controller for this IOFSwitch.
     */
    public Role getRole();

    /**
     * Sets this controller's Role for this IOFSwitch to role.
     *
     * @param role
     */
    public void setRole(Role role);

    /**
     * Gets the next generation ID.
     * <p>
     * Note: relevant for role request messages in OF1.3
     *
     * @return next generation ID
     */
    public U64 getNextGenerationId();


    /**
     * Set debug counter service for per-switch counters.
     * Called immediately after instantiation.
     * @param debugCounters
     * @throws CounterException
     */
    public void setDebugCounterService(IDebugCounterService debugCounter)
            throws CounterException;

    /**
     * Start this switch driver's sub handshake. This might be a no-op but
     * this method must be called at least once for the switch to be become
     * ready.
     * This method must only be called from the I/O thread
     * @throws IOException
     * @throws SwitchDriverSubHandshakeAlreadyStarted if the sub-handshake has
     * already been started
     */
    public void startDriverHandshake() throws IOException;

    /**
     * Check if the sub-handshake for this switch driver has been completed.
     * This method can only be called after startDriverHandshake()
     *
     * This methods must only be called from the I/O thread
     * @return true if the sub-handshake has been completed. False otherwise
     * @throws SwitchDriverSubHandshakeNotStarted if startDriverHandshake() has
     * not been called yet.
     */
    public boolean isDriverHandshakeComplete();

    /**
     * Pass the given OFMessage to the driver as part of this driver's
     * sub-handshake. Must not be called after the handshake has been completed
     * This methods must only be called from the I/O thread
     * @param m The message that the driver should process
     * @throws SwitchDriverSubHandshakeCompleted if isDriverHandshake() returns
     * false before this method call
     * @throws SwitchDriverSubHandshakeNotStarted if startDriverHandshake() has
     * not been called yet.
     */
    public void processDriverHandshakeMessage(OFMessage m);

    /**
     * Set the flow table full flag in the switch.
     * XXX S Rethink this for multiple tables
     */
    public void setTableFull(boolean isFull);

    /**
     * Save the features reply for this switch.
     *
     * @param featuresReply
     */
    public void setFeaturesReply(OFFeaturesReply featuresReply);

    /**
     * Save the portset for this switch.
     *
     * @param portDescReply
     */
    public void setPortDescReply(OFPortDescStatsReply portDescReply);

    //************************
    //  Message handling
    //************************
    /**
     * Handle the message coming from the dataplane.
     *
     * @param m the actual message
     */
    public void handleMessage(OFMessage m);


}
