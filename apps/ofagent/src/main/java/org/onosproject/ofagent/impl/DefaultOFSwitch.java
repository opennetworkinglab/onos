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
import org.onosproject.net.Port;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.ofagent.api.OFSwitch;
import org.onosproject.ofagent.api.OFSwitchCapabilities;
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
import org.projectfloodlight.openflow.protocol.OFSetConfig;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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

    private final DatapathId dpId;
    private final OFSwitchCapabilities capabilities;

    // miss_send_len field (in OFSetConfig and OFGetConfig messages) indicates the max
    // bytes of a packet that the switch sends to the controller
    private int missSendLen = 0xffff;

    private final ConcurrentHashMap<Channel, OFControllerRole> controllerRoleMap
            = new ConcurrentHashMap<>();
    private static final OFFactory FACTORY = OFFactories.getFactory(OFVersion.OF_13);

    private int handshakeTransactionIds = -1;

    private DefaultOFSwitch(DatapathId dpid, OFSwitchCapabilities capabilities) {
        this.dpId = dpid;
        this.capabilities = capabilities;
        log = LoggerFactory.getLogger(getClass().getName() + " : " + dpid);
    }

    public static DefaultOFSwitch of(DatapathId dpid, OFSwitchCapabilities capabilities) {
        checkNotNull(dpid, "DPID cannot be null");
        checkNotNull(capabilities, "OF capabilities cannot be null");
        return new DefaultOFSwitch(dpid, capabilities);
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
        // TODO generate FEATURES_REPLY message and send it to the controller
        log.debug("Functionality not yet supported for {}", port);
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
                ofStatsReply = FACTORY.buildPortDescStatsReply()
                        .setXid(msg.getXid())
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
        // TODO process role request and send reply
        log.debug("Functionality not yet supported for {}", msg);
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
