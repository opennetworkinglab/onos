/*
 * Copyright 2014-present Open Networking Foundation
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
 * See the License for  the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.packetstats;
import com.codahale.metrics.Counter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.ICMP6;
import org.onlab.packet.UDP;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.onlab.metrics.MetricsComponent;
import org.onlab.metrics.MetricsFeature;
import org.onlab.metrics.MetricsService;

/**
 * Application for Packet Statistics.
 */
@Component(immediate = true)
public class PacketStatistics {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;


    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;


    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MetricsService metricService;

    private PacketCounter processor = new PacketCounter();
    private final Logger log = getLogger(getClass());

    private ApplicationId appId;


    private MetricsComponent packetStatisticsComponent;
    private MetricsFeature arpFeature;
    private Counter arpCounter;
    private MetricsFeature dhcpFeature;
    private Counter dhcpCounter;
    private MetricsFeature lldpFeature;
    private Counter lldpCounter;
    private MetricsFeature vlanFeature;
    private Counter vlanCounter;
    private MetricsFeature tcpFeature;
    private Counter tcpCounter;
    private MetricsFeature icmpFeature;
    private Counter icmpCounter;
    private MetricsFeature icmp6Feature;
    private Counter icmp6Counter;
    private MetricsFeature nbrSolicitFeature;
    private Counter nbrSolicitCounter;
    private MetricsFeature nbrAdvertFeature;
    private Counter nbrAdvertCounter;
    private MetricsFeature igmpFeature;
    private Counter igmpCounter;
    private MetricsFeature pimFeature;
    private Counter pimCounter;
    private MetricsFeature bsnFeature;
    private Counter bsnCounter;
    private MetricsFeature rarpFeature;
    private Counter rarpCounter;
    private MetricsFeature mplsFeature;
    private Counter mplsCounter;
    private MetricsFeature unknownFeature;
    private Counter unknownCounter;

    @Activate
    public void activate(ComponentContext context) {
        this.packetStatisticsComponent =
                metricService.registerComponent("packetStatisticsComponent");
        this.arpFeature =
                packetStatisticsComponent.registerFeature("arpFeature");
        this.dhcpFeature =
                packetStatisticsComponent.registerFeature("dhcpFeature");
        this.lldpFeature =
                packetStatisticsComponent.registerFeature("lldpFeature");
        this.vlanFeature =
                packetStatisticsComponent.registerFeature("vlanFeature");
        this.tcpFeature =
                packetStatisticsComponent.registerFeature("tcpFeature");
        this.icmpFeature =
                packetStatisticsComponent.registerFeature("icmpFeature");
        this.icmp6Feature =
                packetStatisticsComponent.registerFeature("icmp6Feature");
        this.nbrSolicitFeature =
                packetStatisticsComponent.registerFeature("nbrSolicitFeature");
        this.nbrAdvertFeature =
                packetStatisticsComponent.registerFeature("nbrAdvertFeature");
        this.igmpFeature =
                packetStatisticsComponent.registerFeature("igmpFeature");
        this.pimFeature =
                packetStatisticsComponent.registerFeature("pimFeature");
        this.bsnFeature =
                packetStatisticsComponent.registerFeature("bsnFeature");
        this.rarpFeature =
                packetStatisticsComponent.registerFeature("rarpFeature");
        this.mplsFeature =
                packetStatisticsComponent.registerFeature("mplsFeature");
        this.unknownFeature =
                packetStatisticsComponent.registerFeature("unknownFeature");
        this.arpCounter =
                metricService.createCounter(packetStatisticsComponent, arpFeature, "arpPC");
        this.dhcpCounter =
                metricService.createCounter(packetStatisticsComponent, dhcpFeature, "dhcpPC");
        this.lldpCounter =
                metricService.createCounter(packetStatisticsComponent, lldpFeature, "lldpPC");
        this.vlanCounter =
                metricService.createCounter(packetStatisticsComponent, vlanFeature, "vlanPC");
        this.tcpCounter =
                metricService.createCounter(packetStatisticsComponent, tcpFeature, "tcpPC");
        this.rarpCounter =
                metricService.createCounter(packetStatisticsComponent, rarpFeature, "rarpPC");
        this.icmpCounter =
                metricService.createCounter(packetStatisticsComponent, icmpFeature, "icmpPC");
        this.icmp6Counter =
                metricService.createCounter(packetStatisticsComponent, icmp6Feature, "icmp6PC");
        this.nbrSolicitCounter =
                metricService.createCounter(packetStatisticsComponent, nbrSolicitFeature, "nbrSolicitPC");
        this.nbrAdvertCounter =
                metricService.createCounter(packetStatisticsComponent, nbrAdvertFeature, "nbrAdvertPC");
        this.igmpCounter =
                metricService.createCounter(packetStatisticsComponent, igmpFeature, "igmpPC");
        this.pimCounter =
                metricService.createCounter(packetStatisticsComponent, pimFeature, "pimPC");
        this.bsnCounter =
                metricService.createCounter(packetStatisticsComponent, bsnFeature, "bsnPC");
        this.mplsCounter =
                metricService.createCounter(packetStatisticsComponent, mplsFeature, "mplsPC");
        this.unknownCounter =
                metricService.createCounter(packetStatisticsComponent, unknownFeature, "unknownPC");

        appId = coreService.registerApplication("org.onosproject.packet-stats");

        packetService.addProcessor(processor, PacketProcessor.director(0));
        log.info("Started", appId.id());
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        flowRuleService.removeFlowRulesById(appId);
        packetService.removeProcessor(processor);
        processor = null;
        log.info("Stopped");
    }


    /**
     * Packet processor responsible for counting various types of packets.
     */
    private class PacketCounter implements PacketProcessor {
        @Override
        public void process(PacketContext context) {
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

            //Indicates whether this is an ARP Packet
            if (ethPkt.getEtherType() == Ethernet.TYPE_ARP) {
                arpCounter.inc();

            } else if (ethPkt.getEtherType() == Ethernet.TYPE_LLDP) {
                lldpCounter.inc();

            } else if (ethPkt.getEtherType() == Ethernet.TYPE_VLAN) {
                vlanCounter.inc();

            } else if (ethPkt.getEtherType() == Ethernet.TYPE_BSN) {
                bsnCounter.inc();

            } else if (ethPkt.getEtherType() == Ethernet.TYPE_RARP) {
                rarpCounter.inc();

            } else if (ethPkt.getEtherType() == Ethernet.MPLS_UNICAST
                    || ethPkt.getEtherType() == Ethernet.MPLS_MULTICAST) {
                mplsCounter.inc();

            } else if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4) {
                IPv4 ipv4Packet = (IPv4) ethPkt.getPayload();
                //Indicates whether this is a TCP Packet
                if (ipv4Packet.getProtocol() == IPv4.PROTOCOL_TCP) {
                    tcpCounter.inc();

                } else if (ipv4Packet.getProtocol() == IPv4.PROTOCOL_ICMP) {
                    icmpCounter.inc();

                } else if (ipv4Packet.getProtocol() == IPv4.PROTOCOL_IGMP) {
                    igmpCounter.inc();

                } else if (ipv4Packet.getProtocol() == IPv4.PROTOCOL_PIM) {
                    pimCounter.inc();

                } else if (ipv4Packet.getProtocol() == IPv4.PROTOCOL_UDP) {
                    UDP udpPacket = (UDP) ipv4Packet.getPayload();
                        //Indicates whether this packet is a DHCP Packet
                        if (udpPacket.getSourcePort() == UDP.DHCP_CLIENT_PORT
                                || udpPacket.getSourcePort() == UDP.DHCP_SERVER_PORT) {
                            dhcpCounter.inc();
                        }
                    }
            } else if (ethPkt.getEtherType() == Ethernet.TYPE_IPV6) {
                   IPv6 ipv6Pkt = (IPv6) ethPkt.getPayload();
                   if (ipv6Pkt.getNextHeader() == IPv6.PROTOCOL_ICMP6) {
                       icmp6Counter.inc();
                       ICMP6 icmpv6 = (ICMP6) ipv6Pkt.getPayload();
                       if (icmpv6.getIcmpType() == ICMP6.NEIGHBOR_SOLICITATION) {
                          nbrSolicitCounter.inc();
                       } else if (icmpv6.getIcmpType() == ICMP6.NEIGHBOR_ADVERTISEMENT) {
                          nbrAdvertCounter.inc();
                       }
                   }
            } else {
                    log.debug("Packet is unknown.");
                    unknownCounter.inc();
            }
        }
    }
}
