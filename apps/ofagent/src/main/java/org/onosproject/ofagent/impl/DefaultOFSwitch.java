/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.ofagent.impl;

import com.google.common.collect.ImmutableSet;
import io.netty.channel.Channel;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.ofagent.api.OFSwitch;
import org.onosproject.ofagent.api.OFSwitchCapabilities;
import org.onosproject.ofagent.api.OFSwitchService;
import org.projectfloodlight.openflow.protocol.OFBarrierReply;
import org.projectfloodlight.openflow.protocol.OFControllerRole;
import org.projectfloodlight.openflow.protocol.OFEchoReply;
import org.projectfloodlight.openflow.protocol.OFEchoRequest;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFGetConfigReply;
import org.projectfloodlight.openflow.protocol.OFHello;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMeterFeatures;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortReason;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFRoleReply;
import org.projectfloodlight.openflow.protocol.OFRoleRequest;
import org.projectfloodlight.openflow.protocol.OFSetConfig;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.projectfloodlight.openflow.protocol.OFControllerRole.ROLE_EQUAL;

/**
 * Implementation of the default OpenFlow switch.
 */
public final class DefaultOFSwitch implements OFSwitch {

    private static final String ERR_CH_DUPLICATE = "Channel already exists: ";
    private static final String ERR_CH_NOT_FOUND = "Channel not found: ";
    private static final long NUM_BUFFERS = 1024;
    private static final short NUM_TABLES = 3;

    private final Logger log;

    private final OFSwitchService ofSwitchService;

    private final DatapathId dpId;
    private final OFSwitchCapabilities capabilities;
    private final NetworkId networkId;
    private final DeviceId deviceId;

    // miss_send_len field (in OFSetConfig and OFGetConfig messages) indicates the max
    // bytes of a packet that the switch sends to the controller
    private int missSendLen = 0xffff;

    private final ConcurrentHashMap<Channel, OFControllerRole> controllerRoleMap
            = new ConcurrentHashMap<>();
    private static final OFFactory FACTORY = OFFactories.getFactory(OFVersion.OF_13);

    private int handshakeTransactionIds = -1;

    private DefaultOFSwitch(DatapathId dpid, OFSwitchCapabilities capabilities,
                            NetworkId networkId, DeviceId deviceId,
                            OFSwitchService ofSwitchService) {
        this.dpId = dpid;
        this.capabilities = capabilities;
        this.networkId = networkId;
        this.deviceId = deviceId;
        this.ofSwitchService = ofSwitchService;
        log = LoggerFactory.getLogger(getClass().getName() + " : " + dpid);
    }

    public static DefaultOFSwitch of(DatapathId dpid, OFSwitchCapabilities capabilities,
                                     NetworkId networkId, DeviceId deviceId,
                                     ServiceDirectory serviceDirectory) {
        checkNotNull(dpid, "DPID cannot be null");
        checkNotNull(capabilities, "OF capabilities cannot be null");
        return new DefaultOFSwitch(dpid, capabilities, networkId, deviceId,
                                   serviceDirectory.get(OFSwitchService.class));
    }

    @Override
    public DatapathId dpid() {
        return this.dpId;
    }

    @Override
    public OFSwitchCapabilities capabilities() {
        return this.capabilities;
    }

    @Override
    public void addControllerChannel(Channel channel) {
        controllerRoleMap.compute(channel, (ch, existing) -> {
            final String error = ERR_CH_DUPLICATE + channel.remoteAddress();
            checkArgument(existing == null, error);
            return ROLE_EQUAL;
        });
    }

    @Override
    public void deleteControllerChannel(Channel channel) {
        if (controllerRoleMap.remove(channel) == null) {
            final String error = ERR_CH_NOT_FOUND + channel.remoteAddress();
            throw new IllegalStateException(error);
        }
    }

    @Override
    public void setRole(Channel channel, OFControllerRole role) {
        controllerRoleMap.compute(channel, (ch, existing) -> {
            final String error = ERR_CH_NOT_FOUND + channel.remoteAddress();
            checkNotNull(existing, error);
            return role;
        });
    }

    @Override
    public OFControllerRole role(Channel channel) {
        OFControllerRole role = controllerRoleMap.get(channel);
        if (role == null) {
            final String error = ERR_CH_NOT_FOUND + channel.remoteAddress();
            throw new IllegalStateException(error);
        }
        return role;
    }

    @Override
    public Set<Channel> controllerChannels() {
        return ImmutableSet.copyOf(controllerRoleMap.keySet());
    }

    @Override
    public void processPortAdded(Port port) {
        sendPortStatus(port, OFPortReason.ADD);
    }

    @Override
    public void processPortRemoved(Port port) {
        sendPortStatus(port, OFPortReason.DELETE);
    }

    @Override
    public void processPortDown(Port port) {
        // TODO generate PORT_STATUS message and send it to the controller
        log.debug("Functionality not yet supported for {}", port);
    }

    @Override
    public void processPortUp(Port port) {
        // TODO generate PORT_STATUS message and send it to the controller
        log.debug("Functionality not yet supported for {}", port);
    }

    @Override
    public void processFlowRemoved(FlowRule flowRule) {
        // TODO generate FLOW_REMOVED message and send it to the controller
        log.debug("Functionality not yet supported for {}", flowRule);
    }

    @Override
    public void processPacketIn(InboundPacket packet) {
        // TODO generate PACKET_IN message and send it to the controller
        log.debug("Functionality not yet supported for {}", packet);
    }

    @Override
    public void processControllerCommand(Channel channel, OFMessage msg) {
        // TODO process controller command
        log.debug("Functionality not yet supported for {}", msg);
    }

    private void sendPortStatus(Port port, OFPortReason ofPortReason) {
        Set<Channel> channels = controllerChannels();
        if (channels.isEmpty()) {
            log.trace("No channels present.  Port status will not be sent.");
            return;
        }
        OFPortDesc ofPortDesc = portDesc(port);
        OFPortStatus ofPortStatus = FACTORY.buildPortStatus()
                .setDesc(ofPortDesc)
                .setReason(ofPortReason)
                .build();
        log.trace("Sending port status {}", ofPortStatus);
        channels.forEach(channel -> {
            channel.writeAndFlush(Collections.singletonList(ofPortStatus));
        });
    }

    private OFPortDesc portDesc(Port port) {
        OFPort ofPort = OFPort.of((int) port.number().toLong());
        // TODO handle port state and other port attributes
        OFPortDesc ofPortDesc = FACTORY.buildPortDesc()
                .setPortNo(ofPort)
                .build();
        return ofPortDesc;
    }

    @Override
    public void processStatsRequest(Channel channel, OFMessage msg) {
        if (msg.getType() != OFType.STATS_REQUEST) {
            log.warn("Ignoring message of type {}.", msg.getType());
            return;
        }

        OFStatsRequest ofStatsRequest = (OFStatsRequest) msg;
        OFStatsReply ofStatsReply = null;
        switch (ofStatsRequest.getStatsType()) {
            case PORT_DESC:
                List<OFPortDesc> portDescs = new ArrayList<>();
                Set<Port> ports = ofSwitchService.ports(networkId, deviceId);
                ports.forEach(port -> {
                    OFPortDesc ofPortDesc = portDesc(port);
                    portDescs.add(ofPortDesc);
                });
                ofStatsReply = FACTORY.buildPortDescStatsReply()
                        .setXid(msg.getXid())
                        .setEntries(portDescs)
                        //TODO add details
                        .build();
                break;
            case METER_FEATURES:
                OFMeterFeatures ofMeterFeatures = FACTORY.buildMeterFeatures()
                        .build();
                ofStatsReply = FACTORY.buildMeterFeaturesStatsReply()
                        .setXid(msg.getXid())
                        .setFeatures(ofMeterFeatures)
                        //TODO add details
                        .build();
                break;
            case DESC:
                ofStatsReply = FACTORY.buildDescStatsReply()
                        .setXid(msg.getXid())
                        .build();
                break;
            default:
                log.debug("Functionality not yet supported for type {} statsType{} msg {}",
                          msg.getType(), ofStatsRequest.getStatsType(), msg);
                break;
        }

        if (ofStatsReply != null) {
            log.trace("request {}; reply {}", msg, ofStatsReply);
            channel.writeAndFlush(Collections.singletonList(ofStatsReply));
        }

    }

    @Override
    public void processRoleRequest(Channel channel, OFMessage msg) {
        OFRoleRequest ofRoleRequest = (OFRoleRequest) msg;
        OFControllerRole oldRole = role(channel);
        OFControllerRole newRole = ofRoleRequest.getRole();
        if (oldRole.equals(newRole)) {
            log.trace("No change needed to existing role {}", oldRole);
        } else {
            log.trace("Changing role from {} to {}", oldRole, newRole);
            setRole(channel, newRole);
        }
        OFRoleReply ofRoleReply = FACTORY.buildRoleReply()
                .setRole(role(channel))
                .setXid(msg.getXid())
                .build();
        channel.writeAndFlush(Collections.singletonList(ofRoleReply));
        log.trace("request {}; reply {}", msg, ofRoleReply);
    }

    @Override
    public void processFeaturesRequest(Channel channel, OFMessage msg) {
        OFFeaturesReply ofFeaturesReply = FACTORY.buildFeaturesReply()
                .setDatapathId(dpId)
                .setNBuffers(NUM_BUFFERS)
                .setNTables(NUM_TABLES)
                .setCapabilities(capabilities.ofSwitchCapabilities())
                .setXid(msg.getXid())
                .build();
        channel.writeAndFlush(Collections.singletonList(ofFeaturesReply));
    }

    @Override
    public void processLldp(Channel channel, OFMessage msg) {
        // TODO process lldp
    }

    @Override
    public void sendOfHello(Channel channel) {
        OFHello ofHello = FACTORY.buildHello()
                .setXid(this.handshakeTransactionIds--)
                .build();
        channel.writeAndFlush(Collections.singletonList(ofHello));
    }

    @Override
    public void processEchoRequest(Channel channel, OFMessage msg) {
        OFEchoReply ofEchoReply = FACTORY.buildEchoReply()
                .setXid(msg.getXid())
                .setData(((OFEchoRequest) msg).getData())
                .build();
        channel.writeAndFlush(Collections.singletonList(ofEchoReply));
    }

    @Override
    public void processGetConfigRequest(Channel channel, OFMessage msg) {
        OFGetConfigReply ofGetConfigReply = FACTORY.buildGetConfigReply()
                .setXid(msg.getXid())
                .setMissSendLen(missSendLen)
                .build();
        log.trace("request {}; reply {}", msg, ofGetConfigReply);
        channel.writeAndFlush(Collections.singletonList(ofGetConfigReply));
    }

    @Override
    public void processSetConfigMessage(Channel channel, OFMessage msg) {
        OFSetConfig ofSetConfig = (OFSetConfig) msg;
        if (missSendLen != ofSetConfig.getMissSendLen()) {
            log.trace("Changing missSendLen from {} to {}.",
                      missSendLen, ofSetConfig.getMissSendLen());
            missSendLen = ofSetConfig.getMissSendLen();
        }

        // SetConfig message is not acknowledged
    }

    @Override
    public void processBarrierRequest(Channel channel, OFMessage msg) {
        // TODO check previous state requests have been handled before issuing BarrierReply
        OFBarrierReply ofBarrierReply = FACTORY.buildBarrierReply()
                .setXid(msg.getXid())
                .build();
        log.trace("request {}; reply {}", msg, ofBarrierReply);
        channel.writeAndFlush(Collections.singletonList(ofBarrierReply));
    }
}
