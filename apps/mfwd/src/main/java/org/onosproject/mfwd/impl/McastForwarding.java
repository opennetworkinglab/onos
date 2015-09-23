/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.mfwd.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IPv4;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;

/**
 * WORK-IN-PROGRESS: The multicast forwarding application using intent framework.
 */
@Component(immediate = true)
public class McastForwarding {

    private final Logger log = getLogger(getClass());
    private final IpPrefix mcast = IpPrefix.valueOf("224.0.0.0/4");

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private ReactivePacketProcessor processor = new ReactivePacketProcessor();
    private McastRouteTable mrib;
    private static ApplicationId appId;

    /**
     * Active MulticastForwardingIntent.
     */
    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.mfwd");

        packetService.addProcessor(processor, PacketProcessor.director(2));

        // Build a traffic selector for all multicast traffic
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPDst(mcast);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);

        mrib = McastRouteTable.getInstance();
        log.info("Started");
    }

    /**
     * Deactivate Multicast Forwarding Intent.
     */
    @Deactivate
    public void deactivate() {
        packetService.removeProcessor(processor);
        processor = null;
        log.info("Stopped");
    }

    /**
     * Get the application ID, used by the McastIntentManager.
     *
     * @return the application ID
     */
    public static ApplicationId getAppId() {
        return appId;
    }

    /**
     * Packet processor responsible for forwarding packets along their paths.
     */
    private class ReactivePacketProcessor implements PacketProcessor {

        /**
         * Process incoming packets.
         *
         * @param context packet processing context
         */
        @Override
        public void process(PacketContext context) {
            // Stop processing if the packet has been handled, since we
            // can't do any more to it.
            if (context.isHandled()) {
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

            if (ethPkt == null) {
                return;
            }

            if (ethPkt.getEtherType() != Ethernet.TYPE_IPV4 &&
                    ethPkt.getEtherType() != Ethernet.TYPE_IPV6) {
                return;
            }

            if (ethPkt.getEtherType() == Ethernet.TYPE_IPV6) {
                // Ignore ipv6 at the moment.
                return;
            }

            IPv4 ip = (IPv4) ethPkt.getPayload();
            IpAddress gaddr = IpAddress.valueOf(ip.getDestinationAddress());
            IpAddress saddr = Ip4Address.valueOf(ip.getSourceAddress());

            log.debug("Packet ({}, {}) has been punted\n" +
                            "\tingress port: {}\n",
                    saddr.toString(),
                    gaddr.toString(),
                    context.inPacket().receivedFrom().toString());

            if (!mcast.contains(gaddr)) {
                // Yikes, this is a bad group address
                return;
            }

            if (mcast.contains(saddr)) {
                // Yikes, the source address is multicast
                return;
            }

            IpPrefix spfx = IpPrefix.valueOf(saddr, 32);
            IpPrefix gpfx = IpPrefix.valueOf(gaddr, 32);

            /*
             * Do a best match lookup on the (s, g) of the packet. If an entry does
             * not exist create one and store it's incoming connect point.
             *
             * The connect point is deviceId / portId that the packet entered
             * the SDN network.  This differs from traditional mcast where the
             * ingress port would be a specific device.
             */
            McastRoute entry = mrib.findBestMatch(spfx, gpfx);
            if (entry == null || entry.getSaddr().equals(IPv4.fromIPv4Address(0))) {

                /*
                 * Create an entry that we can fast drop.
                 */
                entry = mrib.addRoute(spfx, gpfx);
                entry.addIngressPoint(context.inPacket().receivedFrom());
            }

            /*
             * TODO: If we do not have an ingress or any egress connect points we
             * should set up a fast drop entry.
             */
            if (entry.getIngressPoint() == null) {
                return;
            }

            if (entry.getEgressPoints().isEmpty()) {
                return;
            }

            /*
             * This is odd, we should not have received a punted packet if an
             * intent was installed unless the intent was not installed
             * correctly.  However, we are seeing packets get punted after
             * the intent has been installed.
             *
             * Therefore we are going to forward the packets even if they
             * should have already been forwarded by the intent fabric.
             */
            if (entry.getIntentKey() != null) {
                return;
            }

            entry.setIntent();
            McastIntentManager im = McastIntentManager.getInstance();
            im.setIntent(entry);

            entry.incrementPuntCount();

            // Send the pack out each of the egress devices & port
            forwardPacketToDst(context, entry);
        }
    }

    /**
     * Forward the packet to it's multicast destinations.
     *
     * @param context The packet context
     * @param entry The multicast route entry matching this packet
     */
    private void forwardPacketToDst(PacketContext context, McastRoute entry) {

        // Send the pack out each of the respective egress ports
        for (ConnectPoint egress : entry.getEgressPoints()) {
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(egress.port()).build();

            OutboundPacket packet = new DefaultOutboundPacket(
                    egress.deviceId(),
                    treatment,
                    context.inPacket().unparsed());

            packetService.emit(packet);
        }
    }
}
