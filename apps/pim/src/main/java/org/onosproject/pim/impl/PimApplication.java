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
package org.onosproject.pim.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.mcast.MulticastRouteService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;

import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * The main PIM controller class.
 */
@Component(immediate = true)
public class PimApplication {
    private final Logger log = getLogger(getClass());

    // Used to get the appId
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    // Our application ID
    private static ApplicationId appId;

    // Register to receive PIM packets, used to send packets as well
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    // Use the MulticastRouteService to manage incoming PIM Join/Prune state as well as
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MulticastRouteService ms;

    // Create an instance of the PIM packet handler
    protected PimPacketHandler pimPacketHandler;

    // Provide interfaces to the pimInterface manager as a result of Netconfig updates.
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PimInterfaceService pimInterfaceManager;

    private final PimPacketProcessor processor = new PimPacketProcessor();

    /**
     * Activate the PIM component.
     */
    @Activate
    public void activate() {
        // Get our application ID
        appId = coreService.registerApplication("org.onosproject.pim");

        // Build the traffic selector for PIM packets
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPProtocol(IPv4.PROTOCOL_PIM);

        // Use the traffic selector to tell the packet service which packets we want.
        packetService.addProcessor(processor, PacketProcessor.director(5));

        packetService.requestPackets(selector.build(), PacketPriority.CONTROL,
                appId, Optional.empty());

        // Get a copy of the PIM Packet Handler
        pimPacketHandler = new PimPacketHandler();

        log.info("Started");
    }

    /**
     * Deactivate the PIM component.
     */
    @Deactivate
    public void deactivate() {
        packetService.removeProcessor(processor);

        log.info("Stopped");
    }

    /**
     * The class that will receive PIM packets, sanitize them, determine the PIMInterface
     * they arrived on, then forward them on to be processed by the appropriate entity.
     */
    public class PimPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {

            // return if this packet has already been handled
            if (context.isHandled()) {
                return;
            }

            // get the inbound packet
            InboundPacket pkt = context.inPacket();
            if (pkt == null) {
                // problem getting the inbound pkt.  Log it debug to avoid spamming log file
                log.debug("Could not retrieve packet from context");
                return;
            }

            // Get the ethernet header
            Ethernet eth = pkt.parsed();
            if (eth == null) {
                // problem getting the ethernet pkt.  Log it debug to avoid spamming log file
                log.debug("Could not retrieve ethernet packet from the parsed packet");
                return;
            }

            // Get the PIM Interface the packet was received on.
            PimInterface pimi = pimInterfaceManager.getPimInterface(pkt.receivedFrom());
            if (pimi == null) {
                return;
            }

            /*
             * Pass the packet processing off to the PIMInterface for processing.
             *
             * TODO: Is it possible that PIM interface processing should move to the
             * PIMInterfaceManager directly?
             */
            pimPacketHandler.processPacket(eth, pimi);
        }
    }

}
