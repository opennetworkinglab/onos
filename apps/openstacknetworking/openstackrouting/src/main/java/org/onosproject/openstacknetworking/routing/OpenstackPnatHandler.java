/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.openstacknetworking.routing;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TCP;
import org.onlab.packet.UDP;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstacknetworking.OpenstackPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Handle NAT packet processing for Managing Flow Rules In Openstack Nodes.
 */
public class OpenstackPnatHandler implements Runnable {

    volatile PacketContext context;
    private final Logger log = LoggerFactory.getLogger(getClass());

    protected PacketService packetService;

    private final OpenstackRoutingRulePopulator rulePopulator;
    private final int portNum;
    private final OpenstackPort openstackPort;

    OpenstackPnatHandler(OpenstackRoutingRulePopulator rulePopulator, PacketContext context,
                         int portNum, OpenstackPort openstackPort) {
        this.rulePopulator = checkNotNull(rulePopulator);
        this.context = checkNotNull(context);
        this.portNum = checkNotNull(portNum);
        this.openstackPort = checkNotNull(openstackPort);
    }

    @Override
    public void run() {
        InboundPacket inboundPacket = context.inPacket();
        Ethernet ethernet = checkNotNull(inboundPacket.parsed());

        //TODO: Considers IPV6
        if (ethernet.getEtherType() != Ethernet.TYPE_IPV4) {
            log.warn("Now, we just consider IP version 4");
            return;
        }

        packetOut(inboundPacket, portNum);

        rulePopulator.populatePnatFlowRules(inboundPacket, openstackPort, portNum,
                getExternalInterfaceMacAddress(), getExternalRouterMacAddress());
    }

    private void packetOut(InboundPacket inboundPacket, int portNum) {
        Ethernet ethernet = checkNotNull(inboundPacket.parsed());
        IPv4 iPacket = (IPv4) ethernet.getPayload();

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        switch (iPacket.getProtocol()) {
            case IPv4.PROTOCOL_TCP:
                TCP tcpPacket = (TCP) iPacket.getPayload();
                tcpPacket.setSourcePort(portNum);
                tcpPacket.resetChecksum();
                tcpPacket.setParent(iPacket);
                iPacket.setPayload(tcpPacket);
                break;
            case IPv4.PROTOCOL_UDP:
                UDP udpPacket = (UDP) iPacket.getPayload();
                udpPacket.setSourcePort(portNum);
                udpPacket.resetChecksum();
                udpPacket.setParent(iPacket);
                iPacket.setPayload(udpPacket);
                break;
            default:
                break;
        }

        iPacket.resetChecksum();
        iPacket.setPayload(ethernet);
        ethernet.setSourceMACAddress(getExternalInterfaceMacAddress())
                .setDestinationMACAddress(getExternalRouterMacAddress());
        ethernet.resetChecksum();

        treatment.setOutput(getExternalPort(inboundPacket.receivedFrom().deviceId()));

        packetService.emit(new DefaultOutboundPacket(inboundPacket.receivedFrom().deviceId(),
                treatment.build(), ByteBuffer.wrap(ethernet.serialize())));
    }

    private PortNumber getExternalPort(DeviceId deviceId) {
        // TODO
        return null;
    }
    private MacAddress getExternalInterfaceMacAddress() {
        // TODO
        return null;
    }
    private MacAddress getExternalRouterMacAddress() {
        // TODO
        return null;
    }
}