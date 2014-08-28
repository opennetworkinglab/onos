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

package org.onlab.onos.of.controller.impl.internal;

import java.io.IOException;
import java.util.List;

import org.jboss.netty.channel.Channel;
import org.onlab.onos.of.controller.Dpid;
import org.onlab.onos.of.controller.OpenFlowSwitch;
import org.onlab.onos.of.controller.RoleState;
import org.onlab.onos.of.controller.impl.internal.OpenFlowControllerImpl.OpenFlowSwitchAgent;
import org.onlab.onos.of.controller.impl.internal.RoleManager.RoleRecvStatus;
import org.onlab.onos.of.controller.impl.internal.RoleManager.RoleReplyInfo;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFExperimenter;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFRoleReply;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractOpenFlowSwitch implements OpenFlowSwitch {

    private static Logger log =
            LoggerFactory.getLogger(AbstractOpenFlowSwitch.class);

    private Channel channel;
    private boolean connected;
    private Dpid dpid;
    private OpenFlowSwitchAgent agent;

    private OFVersion ofVersion;

    protected OFPortDescStatsReply ports;

    protected boolean tableFull;

    private final RoleManager roleMan = new RoleManager(this);

    protected AbstractOpenFlowSwitch(long dpid) {
        this.dpid = new Dpid(dpid);
    }

    //************************
    // Channel related
    //************************

    /**
     * Disconnects the switch by closing the TCP connection. Results in a call
     * to the channel handler's channelDisconnected method for cleanup
     * @throws IOException
     */
    public final void disconnectSwitch() {
        this.channel.close();
    }

    /**
     * Writes to the OFMessage to the output stream.
     *
     * @param m the message to be written
     */
    public abstract void write(OFMessage m);

    /**
     * Writes to the OFMessage list to the output stream.
     *
     * @param msgs the messages to be written
     */
    public abstract void write(List<OFMessage> msgs);


    /**
     * Checks if the switch is still connected.
     * Only call while holding processMessageLock
     *
     * @return whether the switch is still disconnected
     */
    public final boolean isConnected() {
        return this.connected;
    }

    /**
     * Sets whether the switch is connected.
     * Only call while holding modifySwitchLock
     *
     * @param connected whether the switch is connected
     */
    final void setConnected(boolean connected) {
        this.connected = connected;
    };

    /**
     * Sets the Netty Channel this switch instance is associated with.
     * <p>
     * Called immediately after instantiation
     *
     * @param channel the channel
     */
    public final void setChannel(Channel channel) {
        this.channel = channel;
    };

    //************************
    // Switch features related
    //************************

    /**
     * Gets the datapathId of the switch.
     *
     * @return the switch buffers
     */
    public final long getId() {
        return this.dpid.value();
    };

    /**
     * Gets a string version of the ID for this switch.
     *
     * @return string version of the ID
     */
    public final String getStringId() {
        return this.dpid.toString();
    }

    public final void setOFVersion(OFVersion ofV) {
        this.ofVersion = ofV;
    }

    void setTableFull(boolean full) {
        this.tableFull = full;
    }

    public abstract void setFeaturesReply(OFFeaturesReply featuresReply);

    /**
     * Let peoeple know if you support Nicira style role requests.
     *
     * @return support Nicira roles or not.
     */
    public abstract Boolean supportNxRole();

    //************************
    //  Message handling
    //************************
    /**
     * Handle the message coming from the dataplane.
     *
     * @param m the actual message
     */
    public final void handleMessage(OFMessage m) {
        this.agent.processMessage(m);
    }

    public abstract RoleState getRole();

    final boolean addConnectedSwitch() {
        return this.agent.addConnectedSwitch(this.getId(), this);
    }

    final boolean addActivatedMasterSwitch() {
        return this.agent.addActivatedMasterSwitch(this.getId(), this);
    }

    final boolean addActivatedEqualSwitch() {
        return this.agent.addActivatedEqualSwitch(this.getId(), this);
    }

    final void transitionToEqualSwitch() {
        this.agent.transitionToEqualSwitch(this.getId());
    }

    final void transitionToMasterSwitch() {
        this.agent.transitionToMasterSwitch(this.getId());
    }

    final void removeConnectedSwitch() {
        this.agent.removeConnectedSwitch(this.getId());
    }

    protected OFFactory factory() {
        return OFFactories.getFactory(ofVersion);
    }

    public void setPortDescReply(OFPortDescStatsReply portDescReply) {
        this.ports = portDescReply;
    }

    public abstract void startDriverHandshake();

    public abstract boolean isDriverHandshakeComplete();

    public abstract void processDriverHandshakeMessage(OFMessage m);

    public void setRole(RoleState role) {
        try {
            this.roleMan.sendRoleRequest(role, RoleRecvStatus.MATCHED_SET_ROLE);
        } catch (IOException e) {
           log.error("Unable to write to switch {}.", this.dpid);
        }
    }

    // Role Handling

    void handleRole(OFMessage m) throws SwitchStateException {
        RoleReplyInfo rri = roleMan.extractOFRoleReply((OFRoleReply) m);
        RoleRecvStatus rrs = roleMan.deliverRoleReply(rri);
        if (rrs == RoleRecvStatus.MATCHED_SET_ROLE) {
            if (rri.getRole() == RoleState.MASTER) {
                this.transitionToMasterSwitch();
            } else if (rri.getRole() == RoleState.EQUAL ||
                    rri.getRole() == RoleState.MASTER) {
                this.transitionToEqualSwitch();
            }
        }
    }

    void handleNiciraRole(OFMessage m) throws SwitchStateException {
        RoleState role = this.roleMan.extractNiciraRoleReply((OFExperimenter) m);
      if (role == null) {
          // The message wasn't really a Nicira role reply. We just
          // dispatch it to the OFMessage listeners in this case.
          this.handleMessage(m);
      }

      RoleRecvStatus rrs = this.roleMan.deliverRoleReply(
              new RoleReplyInfo(role, null, m.getXid()));
          if (rrs == RoleRecvStatus.MATCHED_SET_ROLE) {
              if (role == RoleState.MASTER) {
                  this.transitionToMasterSwitch();
              } else if (role == RoleState.EQUAL ||
                      role == RoleState.SLAVE) {
                  this.transitionToEqualSwitch();
              }
          }
    }

    boolean handleRoleError(OFErrorMsg error) {
        try {
            return RoleRecvStatus.OTHER_EXPECTATION != this.roleMan.deliverError(error);
        } catch (SwitchStateException e) {
            this.disconnectSwitch();
        }
        return true;
    }

    void reassertRole() {
        if (this.getRole() == RoleState.MASTER) {
            this.setRole(RoleState.MASTER);
        }
    }

    void setAgent(OpenFlowSwitchAgent ag) {
        this.agent = ag;
    }



}
