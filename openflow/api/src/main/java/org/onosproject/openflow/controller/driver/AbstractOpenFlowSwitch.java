/*
 * Copyright 2014-2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.openflow.controller.driver;

import org.jboss.netty.channel.Channel;
import org.onlab.packet.IpAddress;
import org.onosproject.net.Device;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFExperimenter;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFNiciraControllerRoleRequest;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFRoleReply;
import org.projectfloodlight.openflow.protocol.OFRoleRequest;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * An abstract representation of an OpenFlow switch. Can be extended by others
 * to serve as a base for their vendor specific representation of a switch.
 */
public abstract class AbstractOpenFlowSwitch extends AbstractHandlerBehaviour
        implements OpenFlowSwitchDriver {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private Channel channel;
    protected String channelId;

    private boolean connected;
    protected boolean startDriverHandshakeCalled = false;
    private Dpid dpid;
    private OpenFlowAgent agent;
    private final AtomicInteger xidCounter = new AtomicInteger(0);

    private OFVersion ofVersion;

    protected List<OFPortDescStatsReply> ports = new ArrayList<>();

    protected boolean tableFull;

    private RoleHandler roleMan;

    protected RoleState role;

    protected OFFeaturesReply features;
    protected OFDescStatsReply desc;

    List<OFMessage> messagesPendingMastership;

    @Override
    public void init(Dpid dpid, OFDescStatsReply desc, OFVersion ofv) {
        this.dpid = dpid;
        this.desc = desc;
        this.ofVersion = ofv;
    }

    //************************
    // Channel related
    //************************

    @Override
    public final void disconnectSwitch() {
        this.channel.close();
    }

    @Override
    public void sendMsg(OFMessage msg) {
        this.sendMsg(Collections.singletonList(msg));
    }

    @Override
    public final void sendMsg(List<OFMessage> msgs) {
        if (role == RoleState.MASTER && channel.isConnected()) {
            channel.write(msgs);
        } else if (messagesPendingMastership != null) {
            messagesPendingMastership.addAll(msgs);
            log.debug("Enqueue message for switch {}. queue size after is {}",
                      dpid, messagesPendingMastership.size());
        } else {
            log.warn("Dropping message for switch {} (role: {}, connected: {}): {}",
                     dpid, role, channel.isConnected(), msgs);
        }
    }

    @Override
    public final void sendRoleRequest(OFMessage msg) {
        if (msg instanceof OFRoleRequest ||
                msg instanceof OFNiciraControllerRoleRequest) {
            channel.write(Collections.singletonList(msg));
            return;
        }
        throw new IllegalArgumentException("Someone is trying to send " +
                                                   "a non role request message");
    }

    @Override
    public final void sendHandshakeMessage(OFMessage message) {
        if (!this.isDriverHandshakeComplete()) {
            channel.write(Collections.singletonList(message));
        }
    }

    @Override
    public final boolean isConnected() {
        return this.connected;
    }

    @Override
    public final void setConnected(boolean connected) {
        this.connected = connected;
    }

    @Override
    public final void setChannel(Channel channel) {
        this.channel = channel;
        final SocketAddress address = channel.getRemoteAddress();
        if (address instanceof InetSocketAddress) {
            final InetSocketAddress inetAddress = (InetSocketAddress) address;
            final IpAddress ipAddress = IpAddress.valueOf(inetAddress.getAddress());
            if (ipAddress.isIp4()) {
                channelId = ipAddress.toString() + ':' + inetAddress.getPort();
            } else {
                channelId = '[' + ipAddress.toString() + "]:" + inetAddress.getPort();
            }
        }
    }

    @Override
    public String channelId() {
        return channelId;
    }

    //************************
    // Switch features related
    //************************

    @Override
    public final long getId() {
        return this.dpid.value();
    }

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
        if (this.role == RoleState.MASTER || m instanceof OFPortStatus) {
            this.agent.processMessage(dpid, m);
        }
    }

    @Override
    public RoleState getRole() {
        return role;
    }

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
        if (messagesPendingMastership != null) {
            this.sendMsg(messagesPendingMastership);
            log.debug("Sending {} pending messages to switch {}",
                     messagesPendingMastership.size(), dpid);
            messagesPendingMastership = null;
        }
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
        this.ports.add(portDescReply);
    }

    @Override
    public void setPortDescReplies(List<OFPortDescStatsReply> portDescReplies) {
        this.ports.addAll(portDescReplies);
    }

    @Override
    public void returnRoleReply(RoleState requested, RoleState response) {
        this.agent.returnRoleReply(dpid, requested, response);
    }

    @Override
    public abstract void startDriverHandshake();

    @Override
    public abstract boolean isDriverHandshakeComplete();

    @Override
    public abstract void processDriverHandshakeMessage(OFMessage m);


    // Role Handling

    @Override
    public void setRole(RoleState role) {
        try {
            if (this.roleMan.sendRoleRequest(role, RoleRecvStatus.MATCHED_SET_ROLE)) {
                log.debug("Sending role {} to switch {}", role, getStringId());
                if (role == RoleState.SLAVE || role == RoleState.EQUAL) {
                    this.role = role;
                } else {
                    if (messagesPendingMastership == null) {
                        log.debug("Initializing new queue for switch {}", dpid);
                        messagesPendingMastership = new ArrayList<>();
                    }
                }
            } else {
                this.role = role;
            }
        } catch (IOException e) {
            log.error("Unable to write to switch {}.", this.dpid);
        }
    }

    @Override
    public void reassertRole() {
        if (this.getRole() == RoleState.MASTER) {
            log.warn("Received permission error from switch {} while " +
                    "being master. Reasserting master role.",
                    this.getStringId());
            this.setRole(RoleState.MASTER);
        }
    }



    @Override
    public void handleRole(OFMessage m) throws SwitchStateException {
        RoleReplyInfo rri = roleMan.extractOFRoleReply((OFRoleReply) m);
        RoleRecvStatus rrs = roleMan.deliverRoleReply(rri);
        if (rrs == RoleRecvStatus.MATCHED_SET_ROLE) {
            if (rri.getRole() == RoleState.MASTER) {
                this.role = rri.getRole();
                this.transitionToMasterSwitch();
            } else if (rri.getRole() == RoleState.EQUAL ||
                    rri.getRole() == RoleState.SLAVE) {
                this.transitionToEqualSwitch();
            }
        }  else {
            log.warn("Failed to set role for {}", this.getStringId());
        }
    }

    @Override
    public void handleNiciraRole(OFMessage m) throws SwitchStateException {
        RoleState r = this.roleMan.extractNiciraRoleReply((OFExperimenter) m);
        if (r == null) {
            // The message wasn't really a Nicira role reply. We just
            // dispatch it to the OFMessage listeners in this case.
            this.handleMessage(m);
            return;
        }

        RoleRecvStatus rrs = this.roleMan.deliverRoleReply(
                new RoleReplyInfo(r, null, m.getXid()));
        if (rrs == RoleRecvStatus.MATCHED_SET_ROLE) {
            if (r == RoleState.MASTER) {
                this.role = r;
                this.transitionToMasterSwitch();
            } else if (r == RoleState.EQUAL ||
                    r == RoleState.SLAVE) {
                this.transitionToEqualSwitch();
            }
        } else {
            this.disconnectSwitch();
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
        return this.ports.stream()
                  .flatMap((portReply) -> (portReply.getEntries().stream()))
                  .collect(Collectors.toList());
        //return Collections.unmodifiableList(ports.getEntries());
    }

    @Override
    public String manufacturerDescription() {
        return this.desc.getMfrDesc();
    }


    @Override
    public String datapathDescription() {
        return this.desc.getDpDesc();
    }


    @Override
    public String hardwareDescription() {
        return this.desc.getHwDesc();
    }

    @Override
    public String softwareDescription() {
        return this.desc.getSwDesc();
    }

    @Override
    public String serialNumber() {
        return this.desc.getSerialNum();
    }


    @Override
    public Device.Type deviceType() {
        return Device.Type.SWITCH;
    }


    @Override
    public String toString() {
        return this.getClass().getName() + " [" + ((channel != null)
                ? channel.getRemoteAddress() : "?")
                + " DPID[" + ((getStringId() != null) ? getStringId() : "?") + "]]";
    }



}
