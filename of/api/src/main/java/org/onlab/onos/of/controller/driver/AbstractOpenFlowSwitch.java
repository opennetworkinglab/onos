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

package org.onlab.onos.of.controller.driver;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.Channel;
import org.onlab.onos.of.controller.Dpid;
import org.onlab.onos.of.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFExperimenter;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFRoleReply;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract representation of an OpenFlow switch. Can be extended by others
 * to serve as a base for their vendor specific representation of a switch.
 */
public abstract class AbstractOpenFlowSwitch implements OpenFlowSwitchDriver {

    private static Logger log =
            LoggerFactory.getLogger(AbstractOpenFlowSwitch.class);

    protected Channel channel;

    private boolean connected;
    protected boolean startDriverHandshakeCalled = false;
    private final Dpid dpid;
    private OpenFlowAgent agent;
    private final AtomicInteger xidCounter = new AtomicInteger(0);

    private OFVersion ofVersion;

    protected OFPortDescStatsReply ports;

    protected boolean tableFull;

    private RoleHandler roleMan;

    protected RoleState role;

    protected OFFeaturesReply features;
    protected OFDescStatsReply desc;

    /**
     * Given a dpid build this switch.
     * @param dp the dpid
     */
    protected AbstractOpenFlowSwitch(Dpid dp) {
        this.dpid = dp;
    }

    //************************
    // Channel related
    //************************

    @Override
    public final void disconnectSwitch() {
        this.channel.close();
    }

    @Override
    public final void sendMsg(OFMessage m) {
        this.write(m);
    }

    @Override
    public final void sendMsg(List<OFMessage> msgs) {
        this.write(msgs);
    }

    @Override
    public abstract void write(OFMessage msg);

    @Override
    public abstract void write(List<OFMessage> msgs);

    @Override
    public final boolean isConnected() {
        return this.connected;
    }

    @Override
    public final void setConnected(boolean connected) {
        this.connected = connected;
    };

    @Override
    public final void setChannel(Channel channel) {
        this.channel = channel;
    };

    //************************
    // Switch features related
    //************************

    @Override
    public final long getId() {
        return this.dpid.value();
    };

    @Override
    public final String getStringId() {
        return this.dpid.toString();
    }

    @Override
    public final void setOFVersion(OFVersion ofV) {
        this.ofVersion = ofV;
    }

    @Override
    public void setTableFull(boolean full) {
        this.tableFull = full;
    }

    @Override
    public void setFeaturesReply(OFFeaturesReply featuresReply) {
        this.features = featuresReply;
    }

    @Override
    public abstract Boolean supportNxRole();

    //************************
    //  Message handling
    //************************
    /**
     * Handle the message coming from the dataplane.
     *
     * @param m the actual message
     */
    @Override
    public final void handleMessage(OFMessage m) {
        this.agent.processMessage(m);
    }

    @Override
    public RoleState getRole() {
        return role;
    };

    @Override
    public final boolean connectSwitch() {
        return this.agent.addConnectedSwitch(dpid, this);
    }

    @Override
    public final boolean activateMasterSwitch() {
        return this.agent.addActivatedMasterSwitch(dpid, this);
    }

    @Override
    public final boolean activateEqualSwitch() {
        return this.agent.addActivatedEqualSwitch(dpid, this);
    }

    @Override
    public final void transitionToEqualSwitch() {
        this.agent.transitionToEqualSwitch(dpid);
    }

    @Override
    public final void transitionToMasterSwitch() {
        this.agent.transitionToMasterSwitch(dpid);
    }

    @Override
    public final void removeConnectedSwitch() {
        this.agent.removeConnectedSwitch(dpid);
    }

    @Override
    public OFFactory factory() {
        return OFFactories.getFactory(ofVersion);
    }

    @Override
    public void setPortDescReply(OFPortDescStatsReply portDescReply) {
        this.ports = portDescReply;
    }

    @Override
    public abstract void startDriverHandshake();

    @Override
    public abstract boolean isDriverHandshakeComplete();

    @Override
    public abstract void processDriverHandshakeMessage(OFMessage m);

    @Override
    public void setRole(RoleState role) {
        try {
            log.info("Sending role {} to switch {}", role, getStringId());
            if (this.roleMan.sendRoleRequest(role, RoleRecvStatus.MATCHED_SET_ROLE)) {
                this.role = role;
            }
        } catch (IOException e) {
            log.error("Unable to write to switch {}.", this.dpid);
        }
    }

    // Role Handling

    @Override
    public void handleRole(OFMessage m) throws SwitchStateException {
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

    @Override
    public void handleNiciraRole(OFMessage m) throws SwitchStateException {
        RoleState r = this.roleMan.extractNiciraRoleReply((OFExperimenter) m);
        if (r == null) {
            // The message wasn't really a Nicira role reply. We just
            // dispatch it to the OFMessage listeners in this case.
            this.handleMessage(m);
        }

        RoleRecvStatus rrs = this.roleMan.deliverRoleReply(
                new RoleReplyInfo(r, null, m.getXid()));
        if (rrs == RoleRecvStatus.MATCHED_SET_ROLE) {
            if (r == RoleState.MASTER) {
                this.transitionToMasterSwitch();
            } else if (r == RoleState.EQUAL ||
                    r == RoleState.SLAVE) {
                this.transitionToEqualSwitch();
            }
        }
    }

    @Override
    public boolean handleRoleError(OFErrorMsg error) {
        try {
            return RoleRecvStatus.OTHER_EXPECTATION != this.roleMan.deliverError(error);
        } catch (SwitchStateException e) {
            this.disconnectSwitch();
        }
        return true;
    }

    @Override
    public void reassertRole() {
        if (this.getRole() == RoleState.MASTER) {
            this.setRole(RoleState.MASTER);
        }
    }

    @Override
    public final void setAgent(OpenFlowAgent ag) {
        if (this.agent == null) {
            this.agent = ag;
        }
    }

    @Override
    public final void setRoleHandler(RoleHandler roleHandler) {
        if (this.roleMan == null) {
            this.roleMan = roleHandler;
        }
    }

    @Override
    public void setSwitchDescription(OFDescStatsReply d) {
        this.desc = d;
    }

    @Override
    public int getNextTransactionId() {
        return this.xidCounter.getAndIncrement();
    }

    @Override
    public List<OFPortDesc> getPorts() {
        return Collections.unmodifiableList(ports.getEntries());
    }

}
