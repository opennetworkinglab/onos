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
package org.onosproject.proxyarp;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.proxyarp.ProxyArpService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.packet.Ethernet.TYPE_ARP;
import static org.onlab.packet.Ethernet.TYPE_IPV6;
import static org.onlab.packet.ICMP6.NEIGHBOR_ADVERTISEMENT;
import static org.onlab.packet.ICMP6.NEIGHBOR_SOLICITATION;
import static org.onlab.packet.IPv6.PROTOCOL_ICMP6;
import static org.onosproject.net.packet.PacketPriority.CONTROL;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Sample reactive proxy arp application.
 */
@Component(immediate = true)
public class ProxyArp {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ProxyArpService proxyArpService;

    private ProxyArpProcessor processor = new ProxyArpProcessor();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private ApplicationId appId;

    @Property(name = "ipv6NeighborDiscovery", boolValue = false,
            label = "Enable IPv6 Neighbor Discovery; default is false")
    protected boolean ipv6NeighborDiscovery = false;

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        appId = coreService.registerApplication("org.onosproject.proxyarp");

        packetService.addProcessor(processor, PacketProcessor.director(1));
        readComponentConfiguration(context);
        requestPackests();

        log.info("Started with Application ID {}", appId.id());
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        withdrawIntercepts();
        packetService.removeProcessor(processor);
        processor = null;
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        readComponentConfiguration(context);
        requestPackests();
    }

    /**
     * Request packet in via PacketService.
     */
    private void requestPackests() {
        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(TYPE_ARP);
        packetService.requestPackets(selectorBuilder.build(),
                                     CONTROL, appId);

        selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(TYPE_IPV6);
        selectorBuilder.matchIPProtocol(PROTOCOL_ICMP6);
        selectorBuilder.matchIcmpv6Type(NEIGHBOR_SOLICITATION);
        if (ipv6NeighborDiscovery) {
            // IPv6 Neighbor Solicitation packet.
            packetService.requestPackets(selectorBuilder.build(),
                                         CONTROL, appId);
        } else {
            packetService.cancelPackets(selectorBuilder.build(),
                                        CONTROL, appId);
        }

        // IPv6 Neighbor Advertisement packet.
        selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(TYPE_IPV6);
        selectorBuilder.matchIPProtocol(PROTOCOL_ICMP6);
        selectorBuilder.matchIcmpv6Type(NEIGHBOR_ADVERTISEMENT);
        if (ipv6NeighborDiscovery) {
            packetService.requestPackets(selectorBuilder.build(),
                                         CONTROL, appId);
        } else {
            packetService.cancelPackets(selectorBuilder.build(),
                                        CONTROL, appId);
        }


    }

    /**
     * Cancel requested packet in via packet service.
     */
    private void withdrawIntercepts() {
        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(TYPE_ARP);
        packetService.cancelPackets(selectorBuilder.build(), CONTROL, appId);
        selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(TYPE_IPV6);
        selectorBuilder.matchIPProtocol(PROTOCOL_ICMP6);
        selectorBuilder.matchIcmpv6Type(NEIGHBOR_SOLICITATION);
        packetService.cancelPackets(selectorBuilder.build(), CONTROL, appId);
        selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(TYPE_IPV6);
        selectorBuilder.matchIPProtocol(PROTOCOL_ICMP6);
        selectorBuilder.matchIcmpv6Type(NEIGHBOR_ADVERTISEMENT);
        packetService.cancelPackets(selectorBuilder.build(), CONTROL, appId);

    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Boolean flag;

        flag = isPropertyEnabled(properties, "ipv6NeighborDiscovery");
        if (flag == null) {
            log.info("IPv6 Neighbor Discovery is not configured, " +
                             "using current value of {}", ipv6NeighborDiscovery);
        } else {
            ipv6NeighborDiscovery = flag;
            log.info("Configured. IPv6 Neighbor Discovery is {}",
                     ipv6NeighborDiscovery ? "enabled" : "disabled");
        }
    }

    /**
     * Check property name is defined and set to true.
     *
     * @param properties   properties to be looked up
     * @param propertyName the name of the property to look up
     * @return value when the propertyName is defined or return null
     */
    private static Boolean isPropertyEnabled(Dictionary<?, ?> properties,
                                             String propertyName) {
        Boolean value = null;
        try {
            String s = (String) properties.get(propertyName);
            value = isNullOrEmpty(s) ? null : s.trim().equals("true");
        } catch (ClassCastException e) {
            // No propertyName defined.
            value = null;
        }
        return value;
    }

    /**
     * Packet processor responsible for forwarding packets along their paths.
     */
    private class ProxyArpProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            // Stop processing if the packet has been handled, since we
            // can't do any more to it.
            if (context.isHandled()) {
                return;
            }
            // If IPv6 NDP is disabled, don't handle IPv6 frames.
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
            if (ethPkt == null) {
                return;
            }
            if (!ipv6NeighborDiscovery && (ethPkt.getEtherType() == TYPE_IPV6)) {
                return;
            }

            // Do not ARP for multicast packets.  Let mfwd handle them.
            if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4) {
                if (ethPkt.getDestinationMAC().isMulticast()) {
                    return;
                }
            }

            //handle the arp packet.
            proxyArpService.handlePacket(context);
        }
    }
}


