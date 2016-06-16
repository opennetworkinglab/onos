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
package org.onosproject.dhcprelay;

import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.DHCP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;
/**
 * DHCP Relay Agent Application Component.
 */
@Component(immediate = true)
public class DhcpRelay {

    public static final String DHCP_RELAY_APP = "org.onosproject.dhcp-relay";
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final InternalConfigListener cfgListener = new InternalConfigListener();

    private final Set<ConfigFactory> factories = ImmutableSet.of(
            new ConfigFactory<ApplicationId, DhcpRelayConfig>(APP_SUBJECT_FACTORY,
                    DhcpRelayConfig.class,
                    "dhcprelay") {
                @Override
                public DhcpRelayConfig createConfig() {
                    return new DhcpRelayConfig();
                }
            }
    );

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    private DhcpRelayPacketProcessor dhcpRelayPacketProcessor = new DhcpRelayPacketProcessor();
    private ConnectPoint dhcpServerConnectPoint = null;
    private ApplicationId appId;

    @Activate
    protected void activate() {
        //start the dhcp relay agent

        appId = coreService.registerApplication(DHCP_RELAY_APP);

        cfgService.addListener(cfgListener);
        factories.forEach(cfgService::registerConfigFactory);
        //update the dhcp server configuration.
        updateConfig();
        //add the packet services.
        packetService.addProcessor(dhcpRelayPacketProcessor, PacketProcessor.director(0));
        requestPackets();
        log.info("DHCP-RELAY Started");
        log.info("started the apps dhcp relay");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.removeListener(cfgListener);
        factories.forEach(cfgService::unregisterConfigFactory);
        packetService.removeProcessor(dhcpRelayPacketProcessor);
        cancelPackets();
        log.info("DHCP-RELAY Stopped");
    }

    private void updateConfig() {
        DhcpRelayConfig cfg = cfgService.getConfig(appId, DhcpRelayConfig.class);

        if (cfg == null) {
            log.warn("Dhcp Server info not available");
            return;
        }

        dhcpServerConnectPoint = cfg.getDhcpServerConnectPoint();
        log.info("Reconfigured the dhcp server info");
        log.info("dhcp server connect points are " + dhcpServerConnectPoint);
    }

    /**
     * Request packet in via PacketService.
     */
    private void requestPackets() {

        TrafficSelector.Builder selectorServer = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpDst(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
                .matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT));
        packetService.requestPackets(selectorServer.build(), PacketPriority.CONTROL, appId);

        TrafficSelector.Builder selectorClient = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpDst(TpPort.tpPort(UDP.DHCP_CLIENT_PORT))
                .matchUdpSrc(TpPort.tpPort(UDP.DHCP_SERVER_PORT));
        packetService.requestPackets(selectorClient.build(), PacketPriority.CONTROL, appId);
    }

    /**
     * Cancel requested packets in via packet service.
     */
    private void cancelPackets() {
        TrafficSelector.Builder selectorServer = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpDst(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
                .matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT));
        packetService.cancelPackets(selectorServer.build(), PacketPriority.CONTROL, appId);

        TrafficSelector.Builder selectorClient = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpDst(TpPort.tpPort(UDP.DHCP_CLIENT_PORT))
                .matchUdpSrc(TpPort.tpPort(UDP.DHCP_SERVER_PORT));
        packetService.cancelPackets(selectorClient.build(), PacketPriority.CONTROL, appId);
    }

    private class DhcpRelayPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            // process the packet and get the payload
            Ethernet packet = context.inPacket().parsed();

            if (packet == null) {
                return;
            }

            if (packet.getEtherType() == Ethernet.TYPE_IPV4) {
                IPv4 ipv4Packet = (IPv4) packet.getPayload();

                if (ipv4Packet.getProtocol() == IPv4.PROTOCOL_UDP) {
                    UDP udpPacket = (UDP) ipv4Packet.getPayload();
                    DHCP dhcpPayload = (DHCP) udpPacket.getPayload();
                    if (udpPacket.getDestinationPort() == UDP.DHCP_SERVER_PORT &&
                        udpPacket.getSourcePort() == UDP.DHCP_CLIENT_PORT) {
                        //This packet is dhcp client request.
                        forwardPacket(context, dhcpPayload);
                    } else {
                        //This packet is a dhcp reply from DHCP server.
                        sendReply(context, dhcpPayload);
                    }
                }
            }
        }

        //forward the packet to ConnectPoint where the DHCP server is attached.
        private void forwardPacket(PacketContext context, DHCP dhcpPayload) {
            if (dhcpPayload == null) {
                log.debug("DHCP packet without payload, do nothing");
                return;
            }

            //send Packetout to dhcp server connectpoint.
            if (dhcpServerConnectPoint != null) {
                TrafficTreatment t = DefaultTrafficTreatment.builder()
                        .setOutput(dhcpServerConnectPoint.port()).build();
                OutboundPacket o = new DefaultOutboundPacket(
                        dhcpServerConnectPoint.deviceId(), t, context.inPacket().unparsed());
                packetService.emit(o);
            }
        }

        /*//process the dhcp packet before sending to server
        private void processDhcpPacket(PacketContext context, DHCP dhcpPayload) {
            if (dhcpPayload == null) {
                return;
            }
            Ethernet packet = context.inPacket().parsed();
            DHCPPacketType incomingPacketType = null;
            for (DHCPOption option : dhcpPayload.getOptions()) {
                if (option.getCode() == OptionCode_MessageType.getValue()) {
                    byte[] data = option.getData();
                    incomingPacketType = DHCPPacketType.getType(data[0]);
                }
            }
            switch (incomingPacketType) {
            case DHCPDISCOVER:
                break;
            default:
                break;
            }
        }*/

        //send the response to the requestor host.
        private void sendReply(PacketContext context, DHCP dhcpPayload) {
            if (dhcpPayload == null) {
                log.debug("DHCP packet without payload, do nothing");
                return;
            }
            //get the host info
            Ethernet packet = context.inPacket().parsed();
            Host host = hostService.getHost(HostId.hostId(packet.getDestinationMAC()));
            ConnectPoint dhcpRequestor = new ConnectPoint(host.location().elementId(),
                                                    host.location().port());

            //send Packetout to requestor host.
            if (dhcpRequestor != null) {
                TrafficTreatment t = DefaultTrafficTreatment.builder()
                        .setOutput(dhcpRequestor.port()).build();
                OutboundPacket o = new DefaultOutboundPacket(
                        dhcpRequestor.deviceId(), t, context.inPacket().unparsed());
                packetService.emit(o);
            }
        }
    }

    /**
     * Listener for network config events.
     */
    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {

            if ((event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                    event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED) &&
                    event.configClass().equals(DhcpRelayConfig.class)) {
                updateConfig();
                log.info("Reconfigured");
            }
        }
    }
}
