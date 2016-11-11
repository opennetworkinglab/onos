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

import com.google.common.collect.Lists;
import io.netty.channel.Channel;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.ofagent.api.OFSwitch;
import org.onosproject.ofagent.api.OFSwitchCapabilities;
import org.projectfloodlight.openflow.protocol.OFControllerRole;
import org.projectfloodlight.openflow.protocol.OFEchoReply;
import org.projectfloodlight.openflow.protocol.OFEchoRequest;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFHello;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.DatapathId;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.projectfloodlight.openflow.protocol.OFControllerRole.*;

/**
 * Implementation of OF switch.
 */
public final class DefaultOFSwitch implements OFSwitch {

    private static final String ERR_CH_DUPLICATE = "Channel already exists: ";
    private static final String ERR_CH_NOT_FOUND = "Channel not found: ";
    private static final long NUM_BUFFERS = 1024;
    private static final short NUM_TABLES = 3;

    private final Device device;
    private final OFSwitchCapabilities capabilities;
    private final DatapathId datapathId;

    private final ConcurrentHashMap<Channel, OFControllerRole> controllerRoleMap
            = new ConcurrentHashMap<>();

    protected static final OFFactory FACTORY = OFFactories.getFactory(OFVersion.OF_13);
    private int handshakeTransactionIds;

    public DefaultOFSwitch(Device device, OFSwitchCapabilities capabilities) {
        this.device = device;
        this.capabilities = capabilities;
        datapathId = getDpidFromDeviceId(device.id());
        handshakeTransactionIds = -1;

    }

    // TODO add builder

    @Override
    public Device device() {
        return device;
    }

    @Override
    public OFSwitchCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public boolean isConnected() {
        return !controllerChannels().isEmpty();
    }

    @Override
    public void started() {
        // TODO do some initial setups
    }

    @Override
    public void stopped() {
        // TODO implement
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
        return null;
    }

    @Override
    public void processPortAdded(Port port) {
        // TODO generate FEATURES_REPLY message and send it to the controller
    }

    @Override
    public void processPortDown(Port port) {
        // TODO generate PORT_STATUS message and send it to the controller
    }

    @Override
    public void processPortUp(Port port) {
        // TODO generate PORT_STATUS message and send it to the controller
    }

    @Override
    public void processFlowRemoved(FlowRule flowRule) {
        // TODO generate FLOW_REMOVED message and send it to the controller
    }

    @Override
    public void processPacketIn(InboundPacket packet) {
        // TODO generate PACKET_IN message and send it to the controller
    }

    @Override
    public void processControllerCommand(Channel channel, OFMessage msg) {
        // TODO process controller command
    }

    @Override
    public void processStatsRequest(Channel channel, OFMessage msg) {
        // TODO process request and send reply
    }

    @Override
    public void processRoleRequest(Channel channel, OFMessage msg) {
        // TODO process role request and send reply
    }

    @Override
    public void processFeaturesRequest(Channel channel, OFMessage msg) {
        // TODO process features request and send reply
        List<OFMessage> ofMessageList = Lists.newArrayList();

        OFFeaturesReply.Builder frBuilder = FACTORY.buildFeaturesReply()
                .setDatapathId(datapathId)
                .setNBuffers(NUM_BUFFERS)
                .setNTables(NUM_TABLES)
                .setCapabilities(capabilities.ofSwitchCapabilities())
                .setXid(msg.getXid());

        ofMessageList.add(frBuilder.build());
        channel.write(ofMessageList);

    }

    @Override
    public void processLldp(Channel channel, OFMessage msg) {
        // TODO process lldp
    }

    @Override
    public void sendOfHello(Channel channel) {
        List<OFMessage> ofMessageList = Lists.newArrayList();
        OFHello.Builder ofHello = FACTORY.buildHello()
                .setXid(this.handshakeTransactionIds--);

        ofMessageList.add(ofHello.build());
        channel.write(ofMessageList);
    }

    @Override
    public void processEchoRequest(Channel channel, OFMessage msg) {
        List<OFMessage> ofMessageList = Lists.newArrayList();
        OFEchoReply.Builder echoBuilder = FACTORY.buildEchoReply()
                .setXid(msg.getXid())
                .setData(((OFEchoRequest) msg).getData());

        ofMessageList.add(echoBuilder.build());
        channel.write(ofMessageList);
    }

    private DatapathId getDpidFromDeviceId(DeviceId deviceId) {
        String deviceIdToString = deviceId.toString().split(":")[1];

        assert (deviceIdToString.length() == 16);

        String resultedHexString = new String();
        for (int i = 0; i < 8; i++) {
            resultedHexString = resultedHexString + deviceIdToString.charAt(2 * i)
                    + deviceIdToString.charAt(2 * i + 1);
            if (i != 7) {
                resultedHexString += ":";
            }
        }
        return DatapathId.of(resultedHexString);
    }
}
