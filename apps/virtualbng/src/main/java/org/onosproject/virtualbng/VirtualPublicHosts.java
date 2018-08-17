/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.virtualbng;

import static org.slf4j.LoggerFactory.getLogger;

import java.nio.ByteBuffer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;

/**
 * When the upstream gateway which is outside local SDN network wants to send
 * packets to our local public IP addresses, it will send out ARP requests to
 * get the MAC address of each public IP address. Actually, there are no hosts
 * configured with those public IP addresses, so this class is to emulate the
 * behavior of the non-existed hosts and return ARP replies.
 * <p>
 * Since we will rewrite the destination MAC address in the switch before
 * traffic packets go to the destination, so the MAC address can be any number.
 * We manually configured a random MAC address for this purpose in the vBNG
 * configuration file.
 * </p>
 */
@Component(immediate = true)
public class VirtualPublicHosts {
    private final Logger log = getLogger(getClass());

    private static final String APP_NAME =
            "org.onosproject.virtualbng.VirtualPublicHosts";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VbngConfigurationService vbngConfigService;

    private ApplicationId appId;
    private ArpRequestProcessor processor = new ArpRequestProcessor();

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_NAME);

        packetService.addProcessor(processor, PacketProcessor.director(6));
        requestIntercepts();
        log.info("vBNG virtual public hosts started");
    }

    @Deactivate
    public void deactivate() {
        withdrawIntercepts();
        packetService.removeProcessor(processor);
        processor = null;
        log.info("vBNG virtual public hosts Stopped");
    }

    /**
     * Request packet in via PacketService.
     */
    private void requestIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        // Only IPv4 is supported in current vBNG.
        selector.matchEthType(Ethernet.TYPE_ARP);
        packetService.requestPackets(selector.build(),
                                     PacketPriority.REACTIVE, appId);
    }

    /**
     * Cancel request for packet in via PacketService.
     */
    private void withdrawIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        // Only IPv4 is supported in current vBNG.
        selector.matchEthType(Ethernet.TYPE_ARP);
        packetService.cancelPackets(selector.build(),
                                    PacketPriority.REACTIVE, appId);
    }

    /**
     * This class filters out the ARP request packets, generates the ARP
     * reply packets, and emits those packets.
     */
    private class ArpRequestProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {

            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

            // Only handle the ARP packets
            if (ethPkt == null || ethPkt.getEtherType() != Ethernet.TYPE_ARP) {
                return;
            }
            ARP arpPacket = (ARP) ethPkt.getPayload();
            // Only handle ARP request packets
            if (arpPacket.getOpCode() != ARP.OP_REQUEST) {
                return;
            }

            Ip4Address targetIpAddress = Ip4Address
                    .valueOf(arpPacket.getTargetProtocolAddress());

            // Only handle an ARP request when the target IP address inside is
            // an assigned public IP address
            if (!vbngConfigService.isAssignedPublicIpAddress(targetIpAddress)) {
                return;
            }

            MacAddress virtualHostMac =
                    vbngConfigService.getPublicFacingMac();
            if (virtualHostMac == null) {
                return;
            }

            ConnectPoint srcConnectPoint = pkt.receivedFrom();
            Ethernet eth = ARP.buildArpReply(targetIpAddress,
                                             virtualHostMac,
                                             ethPkt);

            TrafficTreatment.Builder builder =
                    DefaultTrafficTreatment.builder();
            builder.setOutput(srcConnectPoint.port());
            packetService.emit(new DefaultOutboundPacket(
                    srcConnectPoint.deviceId(),
                    builder.build(),
                    ByteBuffer.wrap(eth.serialize())));
        }
    }
}
