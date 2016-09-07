/*
 * Copyright 2016-present Open Networking Laboratory
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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstackinterface.OpenstackInterfaceService;
import org.onosproject.openstackinterface.OpenstackPort;
import org.onosproject.scalablegateway.api.ScalableGatewayService;
import org.onosproject.openstacknetworking.Constants;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.Constants.DEVICE_OWNER_FLOATING_IP;
import static org.onosproject.openstacknetworking.Constants.DEVICE_OWNER_ROUTER_GATEWAY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handle ARP, ICMP and NAT packets from gateway nodes.
 */
@Component(immediate = true)
public class OpenstackRoutingArpHandler {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackInterfaceService openstackService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ScalableGatewayService gatewayService;

    private final ExecutorService executorService =
            newSingleThreadExecutor(groupedThreads("onos/openstackrouting", "packet-event", log));

    private final InternalPacketProcessor packetProcessor = new InternalPacketProcessor();

    @Activate
    protected void activate() {
        packetService.addProcessor(packetProcessor, PacketProcessor.director(1));
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(packetProcessor);
        log.info("Stopped");
    }

    private void processArpPacket(PacketContext context, Ethernet ethernet) {
        ARP arp = (ARP) ethernet.getPayload();
        log.trace("arpEvent called from {} to {}",
                Ip4Address.valueOf(arp.getSenderProtocolAddress()).toString(),
                Ip4Address.valueOf(arp.getTargetProtocolAddress()).toString());

        if (arp.getOpCode() != ARP.OP_REQUEST) {
            return;
        }

        IpAddress targetIp = Ip4Address.valueOf(arp.getTargetProtocolAddress());
        if (getTargetMacForTargetIp(targetIp.getIp4Address()) == MacAddress.NONE) {
            return;
        }

        MacAddress targetMac = Constants.DEFAULT_EXTERNAL_ROUTER_MAC;
        Ethernet ethReply = ARP.buildArpReply(targetIp.getIp4Address(),
                targetMac, ethernet);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(context.inPacket().receivedFrom().port())
                .build();

        packetService.emit(new DefaultOutboundPacket(
                context.inPacket().receivedFrom().deviceId(),
                treatment,
                ByteBuffer.wrap(ethReply.serialize())));
    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            } else if (!gatewayService.getGatewayDeviceIds().contains(
                    context.inPacket().receivedFrom().deviceId())) {
                // return if the packet is not from gateway nodes
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethernet = pkt.parsed();
            if (ethernet != null &&
                    ethernet.getEtherType() == Ethernet.TYPE_ARP) {
                executorService.execute(() -> processArpPacket(context, ethernet));
            }
        }
    }

    // TODO make a cache for the MAC, not a good idea to REST call every time it gets ARP request
    private MacAddress getTargetMacForTargetIp(Ip4Address targetIp) {
        OpenstackPort port = openstackService.ports().stream()
                .filter(p -> p.deviceOwner().equals(DEVICE_OWNER_ROUTER_GATEWAY) ||
                        p.deviceOwner().equals(DEVICE_OWNER_FLOATING_IP))
                .filter(p -> p.fixedIps().containsValue(targetIp.getIp4Address()))
                .findAny().orElse(null);

        return port == null ? MacAddress.NONE : port.macAddress();
    }
}
