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

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Set;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ARP;
import org.onlab.packet.DHCP;
import org.onlab.packet.DHCPOption;
import org.onlab.packet.DHCPPacketType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
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
import static org.onlab.packet.DHCP.DHCPOptionCode.OptionCode_MessageType;
import static org.onlab.packet.MacAddress.valueOf;
/**
 * DHCP Relay Agent Application Component.
 */
@Component(immediate = true)
public class DhcpRelay {

    public static final String DHCP_RELAY_APP = "org.onosproject.dhcp-relay";
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final InternalConfigListener cfgListener = new InternalConfigListener();
    private static Ip4Address relayAgentIP = null;
    private static MacAddress relayAgentMAC = null;
    private static MacAddress myMAC = valueOf("4f:4f:4f:4f:4f:4f");

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    private DhcpRelayPacketProcessor dhcpRelayPacketProcessor = new DhcpRelayPacketProcessor();
    private ConnectPoint dhcpServerConnectPoint = null;
    private Ip4Address dhcpServerIp = null;
    private MacAddress dhcpServerMac = null;
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
        dhcpServerIp = cfg.getDhcpServerIp();
        dhcpServerMac = cfg.getDhcpServermac();
        log.info("dhcp server connect points are " + dhcpServerConnectPoint);
        log.info("dhcp server ipaddress " + dhcpServerIp);
        log.info("dhcp server mac address " + dhcpServerMac);
    }

    /**
     * Request packet in via PacketService.
     */
    private void requestPackets() {

        TrafficSelector.Builder selectorServer = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpSrc(TpPort.tpPort(UDP.DHCP_SERVER_PORT));
        packetService.requestPackets(selectorServer.build(), PacketPriority.CONTROL, appId);

        TrafficSelector.Builder selectorClient = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT));
        packetService.requestPackets(selectorClient.build(), PacketPriority.CONTROL, appId);

        TrafficSelector.Builder selectorArpServer = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_ARP);
        packetService.requestPackets(selectorArpServer.build(), PacketPriority.CONTROL, appId);
    }

    /**
     * Cancel requested packets in via packet service.
     */
    private void cancelPackets() {
        TrafficSelector.Builder selectorServer = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpSrc(TpPort.tpPort(UDP.DHCP_SERVER_PORT));
        packetService.cancelPackets(selectorServer.build(), PacketPriority.CONTROL, appId);

        TrafficSelector.Builder selectorClient = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT));
        packetService.cancelPackets(selectorClient.build(), PacketPriority.CONTROL, appId);

        TrafficSelector.Builder selectorArpServer = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_ARP);
        packetService.cancelPackets(selectorArpServer.build(), PacketPriority.CONTROL, appId);
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
                    if (udpPacket.getSourcePort() == UDP.DHCP_CLIENT_PORT ||
                        udpPacket.getSourcePort() == UDP.DHCP_SERVER_PORT) {
                        //This packet is dhcp.
                        processDhcpPacket(context, dhcpPayload);
                    }
                }
            } else if (packet.getEtherType() == Ethernet.TYPE_ARP) {
                ARP arpPacket = (ARP) packet.getPayload();
                Set<Interface> serverInterfaces = interfaceService.
                        getInterfacesByPort(context.inPacket().receivedFrom());
                //ignore the packets if dhcp server interface is not configured on onos.
                if (serverInterfaces.isEmpty()) {
                    log.warn("server virtual interface not configured");
                    return;
                }
                if ((arpPacket.getOpCode() == ARP.OP_REQUEST) &&
                        checkArpRequestFrmDhcpServ(serverInterfaces, arpPacket)) {
                    processArpPacket(context, packet);
                }
            }
        }

        //method to check the arp request is from dhcp server for default-gateway.
        private boolean checkArpRequestFrmDhcpServ(Set<Interface> serverInterfaces, ARP arpPacket) {
            if (Objects.equals(serverInterfaces.iterator().next().ipAddressesList().get(0).
                    ipAddress().getIp4Address(),
                    Ip4Address.valueOf(arpPacket.getTargetProtocolAddress()))) {
                return true;
            }
            return false;
        }

        //forward the packet to ConnectPoint where the DHCP server is attached.
        private void forwardPacket(Ethernet packet) {

            //send Packetout to dhcp server connectpoint.
            if (dhcpServerConnectPoint != null) {
                TrafficTreatment t = DefaultTrafficTreatment.builder()
                        .setOutput(dhcpServerConnectPoint.port()).build();
                OutboundPacket o = new DefaultOutboundPacket(
                        dhcpServerConnectPoint.deviceId(), t, ByteBuffer.wrap(packet.serialize()));
                packetService.emit(o);
            }
        }

        /**
         * Processes the ARP Payload and initiates a reply to the client.
         *
         * @param context context of the incoming message
         * @param packet the ethernet payload
         */
        private void processArpPacket(PacketContext context, Ethernet packet) {

            ARP arpPacket = (ARP) packet.getPayload();

            ARP arpReply = (ARP) arpPacket.clone();
            arpReply.setOpCode(ARP.OP_REPLY);

            arpReply.setTargetProtocolAddress(arpPacket.getSenderProtocolAddress());
            arpReply.setTargetHardwareAddress(arpPacket.getSenderHardwareAddress());
            arpReply.setSenderProtocolAddress(arpPacket.getTargetProtocolAddress());
            arpReply.setSenderHardwareAddress(myMAC.toBytes());

            // Ethernet Frame.
            Ethernet ethReply = new Ethernet();
            ethReply.setSourceMACAddress(myMAC);
            ethReply.setDestinationMACAddress(packet.getSourceMAC());
            ethReply.setEtherType(Ethernet.TYPE_ARP);
            ethReply.setVlanID(packet.getVlanID());

            ethReply.setPayload(arpReply);
            forwardPacket(ethReply);
        }

        //process the dhcp packet before sending to server
        private void processDhcpPacket(PacketContext context, DHCP dhcpPayload) {

            Set<Interface> clientServerInterfaces = interfaceService.
                    getInterfacesByPort(context.inPacket().receivedFrom());
            //ignore the packets if dhcp client interface is not configured on onos.
            if (clientServerInterfaces.isEmpty()) {
                log.warn("client virtual interface not configured");
                return;
            }

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
                //add the gatewayip as virtual interface ip for server to understand the lease to be assigned
                //and forward the packet to dhcp server.
                Ethernet ethernetPacketDiscover = processDhcpPacketFrmClient(context, packet, clientServerInterfaces);
                forwardPacket(ethernetPacketDiscover);
                break;
            case DHCPOFFER:
                //reply to dhcp client.
                Ethernet ethernetPacketOffer = processDhcpPacketFrmServer(packet);
                sendReply(ethernetPacketOffer, dhcpPayload);
                break;
            case DHCPREQUEST:
                //add the gatewayip as virtual interface ip for server to understand the lease to be assigned
                //and forward the packet to dhcp server.
                Ethernet ethernetPacketRequest = processDhcpPacketFrmClient(context, packet, clientServerInterfaces);
                forwardPacket(ethernetPacketRequest);
                break;
            case DHCPACK:
                //reply to dhcp client.
                Ethernet ethernetPacketAck = processDhcpPacketFrmServer(packet);
                sendReply(ethernetPacketAck, dhcpPayload);
                break;
            default:
                break;
            }
        }

        //build the DHCP discover/request packet with gatewayip(unicast packet)
        private Ethernet processDhcpPacketFrmClient(PacketContext context, Ethernet ethernetPacket,
                Set<Interface> clientInterfaces) {

            //assuming one interface per port for now.
            relayAgentIP = clientInterfaces.iterator().next().ipAddressesList().get(0).
                    ipAddress().getIp4Address();
            relayAgentMAC = clientInterfaces.iterator().next().mac();
            // get dhcp header.
            Ethernet etherReply = (Ethernet) ethernetPacket.clone();
            etherReply.setSourceMACAddress(relayAgentMAC);
            etherReply.setDestinationMACAddress(dhcpServerMac);
            IPv4 ipv4Packet = (IPv4) etherReply.getPayload();
            ipv4Packet.setSourceAddress(relayAgentIP.toInt());
            ipv4Packet.setDestinationAddress(dhcpServerIp.toInt());
            UDP udpPacket = (UDP) ipv4Packet.getPayload();
            DHCP dhcpPacket = (DHCP) udpPacket.getPayload();
            dhcpPacket.setGatewayIPAddress(relayAgentIP.toInt());
            udpPacket.setPayload(dhcpPacket);
            ipv4Packet.setPayload(udpPacket);
            etherReply.setPayload(ipv4Packet);
            return etherReply;
        }

        //build the DHCP offer/ack with proper client port.
        private Ethernet processDhcpPacketFrmServer(Ethernet ethernetPacket) {

            // get dhcp header.
            Ethernet etherReply = (Ethernet) ethernetPacket.clone();
            IPv4 ipv4Packet = (IPv4) etherReply.getPayload();
            UDP udpPacket = (UDP) ipv4Packet.getPayload();
            DHCP dhcpPayload = (DHCP) udpPacket.getPayload();
            //set the ethernet frame.
            etherReply.setDestinationMACAddress(dhcpPayload.getClientHardwareAddress());
            udpPacket.setDestinationPort(UDP.DHCP_CLIENT_PORT);
            udpPacket.setPayload(dhcpPayload);
            ipv4Packet.setPayload(udpPacket);
            etherReply.setPayload(ipv4Packet);
            return etherReply;
        }

        //send the response to the requestor host.
        private void sendReply(Ethernet ethPacket, DHCP dhcpPayload) {

            MacAddress descMac = new MacAddress(dhcpPayload.getClientHardwareAddress());
            Host host = hostService.getHost(HostId.hostId(descMac,
                    VlanId.vlanId(ethPacket.getVlanID())));
            //handle the concurrent host offline scenario
            if (host == null) {
                return;
            }
            ConnectPoint dhcpRequestor = new ConnectPoint(host.location().elementId(),
                                                    host.location().port());

            //send Packetout to requestor host.
            if (dhcpRequestor != null) {
                TrafficTreatment t = DefaultTrafficTreatment.builder()
                        .setOutput(dhcpRequestor.port()).build();
                OutboundPacket o = new DefaultOutboundPacket(
                        dhcpRequestor.deviceId(), t, ByteBuffer.wrap(ethPacket.serialize()));
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
