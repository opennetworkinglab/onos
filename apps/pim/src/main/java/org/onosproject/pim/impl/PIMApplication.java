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
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.incubator.net.config.basics.InterfaceConfig;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.mcast.MulticastRouteService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * The main PIM controller class.
 */
@Component(immediate = true)
public class PIMApplication {
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
    protected PIMPacketHandler pimPacketHandler;

    // Get the network configuration updates
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    // Access defined network (IP) interfaces
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    // Internal class used to listen for network configuration changes
    private InternalConfigListener configListener = new InternalConfigListener();

    // Provide interfaces to the pimInterface manager as a result of Netconfig updates.
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PIMInterfaceService pimInterfaceManager;

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
        // PIMPacketService is an inner class defined below
        PIMPacketProcessor processor = new PIMPacketProcessor();
        packetService.addProcessor(processor, PacketProcessor.director(5));

        // Register for notifications from the Network config & Interface services.
        // We'll use these services to represent "PIMInterfaces"

        // Get a copy of the PIM Packet Handler
        pimPacketHandler = new PIMPacketHandler();

        // Listen for network configuration changes
        configService.addListener(configListener);

        log.info("Started");
    }

    /**
     * Deactivate the PIM component.
     */
    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    /**
     * The class that will receive PIM packets, sanitize them, determine the PIMInterface
     * they arrived on, then forward them on to be processed by the appropriate entity.
     */
    public class PIMPacketProcessor implements PacketProcessor {
        private final Logger log = getLogger(getClass());

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
                log.debug("Could not retrieve ethnernet packet from the parsed packet");
                return;
            }

            // Get the PIM Interface the packet was received on.
            PIMInterface pimi = pimInterfaceManager.getPIMInterface(pkt.receivedFrom());
            if (pimi == null) {
                log.debug("We received PIM packet from a non PIM interface: " + pkt.receivedFrom().toString());
                return;
            }

            /*
             * Pass the packet processing off to the PIMInterface for processing.
             *
             * TODO: Is it possible that PIM interface processing should move to the
             * PIMInterfaceManager directly?
             */
            PIMPacketHandler ph = new PIMPacketHandler();
            ph.processPacket(eth, pimi);
        }
    }

    /*
     * This class receives all events from the network config services, then hands the
     * event off to the PIMInterfaceManager for proper handling.
     *
     * TODO: should this move to PIMInterfaceManager?
     */
    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {

            log.debug(event.toString());
            switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:

                    if (event.configClass() == InterfaceConfig.class) {
                        InterfaceConfig config = configService.getConfig(
                                (ConnectPoint) event.subject(),
                                InterfaceConfig.class);

                        log.debug("Got a network configuration event");

                        // Walk the interfaces and feed them to the PIMInterfaceManager
                        Set<Interface> intfs;
                        try {
                            intfs = config.getInterfaces();
                            for (Interface intf : intfs) {
                                pimInterfaceManager.updateInterface(intf);
                            }
                        } catch (ConfigException e) {
                            log.error(e.toString());
                            return;
                        }
                    }
                    break;

                case CONFIG_REMOVED:
                    if (event.configClass() == InterfaceConfig.class) {
                        ConnectPoint cp = (ConnectPoint) event.subject();
                        //assertNotNull(cp);
                        pimInterfaceManager.deleteInterface(cp);
                    }
                    break;

                case CONFIG_REGISTERED:
                case CONFIG_UNREGISTERED:
                default:
                    log.debug("\tWe are not handling this event type");
                    break;
            }
        }
    }
}
