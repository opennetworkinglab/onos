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
import java.util.Dictionary;
import java.util.Objects;
import java.util.Set;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ARP;
import org.onlab.packet.DHCP;
import org.onlab.packet.DHCPOption;
import org.onlab.packet.DHCPPacketType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onlab.util.Tools;
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
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.osgi.service.component.ComponentContext;
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

    @Property(name = "arpEnabled", boolValue = true,
            label = "Enable Address resolution protocol")
    protected boolean arpEnabled = true;

    private DhcpRelayPacketProcessor dhcpRelayPacketProcessor = new DhcpRelayPacketProcessor();
    private InternalHostListener hostListener = new InternalHostListener();

    private Ip4Address dhcpServerIp = null;
    // dhcp server may be connected directly to the SDN network or
    // via an external gateway. When connected directly, the dhcpConnectPoint, dhcpConnectMac,
    // and dhcpConnectVlan refer to the server. When connected via the gateway, they refer
    // to the gateway.
    private ConnectPoint dhcpServerConnectPoint = null;
    private MacAddress dhcpConnectMac = null;
    private VlanId dhcpConnectVlan = null;
    private Ip4Address dhcpGatewayIp = null;
    private ApplicationId appId;

    @Activate
    protected void activate(ComponentContext context) {
        //start the dhcp relay agent
        appId = coreService.registerApplication(DHCP_RELAY_APP);

        cfgService.addListener(cfgListener);
        factories.forEach(cfgService::registerConfigFactory);
        //update the dhcp server configuration.
        updateConfig();
        //add the packet services.
        packetService.addProcessor(dhcpRelayPacketProcessor, PacketProcessor.director(0));
        hostService.addListener(hostListener);
        requestDhcpPackets();
        modified(context);

        log.info("DHCP-RELAY Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.removeListener(cfgListener);
        factories.forEach(cfgService::unregisterConfigFactory);
        packetService.removeProcessor(dhcpRelayPacketProcessor);
        hostService.removeListener(hostListener);
        cancelDhcpPackets();
        cancelArpPackets();
        if (dhcpGatewayIp != null) {
            hostService.stopMonitoringIp(dhcpGatewayIp);
        } else {
            hostService.stopMonitoringIp(dhcpServerIp);
        }

        log.info("DHCP-RELAY Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Boolean flag;

        flag = Tools.isPropertyEnabled(properties, "arpEnabled");
        if (flag != null) {
            arpEnabled = flag;
            log.info("Address resolution protocol is {}",
                     arpEnabled ? "enabled" : "disabled");
        }

        if (arpEnabled) {
            requestArpPackets();
        } else {
            cancelArpPackets();
        }
    }

    /**
     * Checks if this app has been configured.
     *
     * @return true if all information we need have been initialized
     */
    private boolean configured() {
        return dhcpServerConnectPoint != null && dhcpServerIp != null;
    }

    private void updateConfig() {
        DhcpRelayConfig cfg = cfgService.getConfig(appId, DhcpRelayConfig.class);
        if (cfg == null) {
            log.warn("Dhcp Server info not available");
            return;
        }

        dhcpServerConnectPoint = cfg.getDhcpServerConnectPoint();
        Ip4Address oldDhcpServerIp = dhcpServerIp;
        Ip4Address oldDhcpGatewayIp = dhcpGatewayIp;
        dhcpServerIp = cfg.getDhcpServerIp();
        dhcpGatewayIp = cfg.getDhcpGatewayIp();
        dhcpConnectMac = null; // reset for updated config
        dhcpConnectVlan = null; // reset for updated config
        log.info("DHCP server connect point: " + dhcpServerConnectPoint);
        log.info("DHCP server ipaddress " + dhcpServerIp);

        IpAddress ipToProbe = dhcpGatewayIp != null ? dhcpGatewayIp : dhcpServerIp;
        String hostToProbe = dhcpGatewayIp != null ? "gateway" : "DHCP server";

        Set<Host> hosts = hostService.getHostsByIp(ipToProbe);
        if (hosts.isEmpty()) {
            log.info("Probing to resolve {} IP {}", hostToProbe, ipToProbe);
            if (oldDhcpGatewayIp != null) {
                hostService.stopMonitoringIp(oldDhcpGatewayIp);
            }
            if (oldDhcpServerIp != null) {
                hostService.stopMonitoringIp(oldDhcpServerIp);
            }
            hostService.startMonitoringIp(ipToProbe);
        } else {
            // Probe target is known; There should be only 1 host with this ip
            hostUpdated(hosts.iterator().next());
        }
    }

    private void hostRemoved(Host host) {
        if (host.ipAddresses().contains(dhcpServerIp)) {
            log.warn("DHCP server {} removed", dhcpServerIp);
            dhcpConnectMac = null;
            dhcpConnectVlan = null;
        }
        if (dhcpGatewayIp != null && host.ipAddresses().contains(dhcpGatewayIp)) {
            log.warn("DHCP gateway {} removed", dhcpGatewayIp);
            dhcpConnectMac = null;
            dhcpConnectVlan = null;
        }
    }

    private void hostUpdated(Host host) {
        if (dhcpGatewayIp != null) {
            if (host.ipAddresses().contains(dhcpGatewayIp)) {
                dhcpConnectMac = host.mac();
                dhcpConnectVlan = host.vlan();
                log.info("DHCP gateway {} resolved to Mac/Vlan:{}/{}", dhcpGatewayIp,
                        dhcpConnectMac, dhcpConnectVlan);
            }
            return;
        }
        if (host.ipAddresses().contains(dhcpServerIp)) {
            dhcpConnectMac = host.mac();
            dhcpConnectVlan = host.vlan();
            log.info("DHCP server {} resolved to Mac/Vlan:{}/{}", dhcpServerIp,
                    dhcpConnectMac, dhcpConnectVlan);
        }
    }

    /**
     * Request DHCP packet in via PacketService.
     */
    private void requestDhcpPackets() {
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
    }

    /**
     * Cancel requested DHCP packets in via packet service.
     */
    private void cancelDhcpPackets() {
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
    }

    /**
     * Request ARP packet in via PacketService.
     */
    private void requestArpPackets() {
        TrafficSelector.Builder selectorArpServer = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_ARP);
        packetService.requestPackets(selectorArpServer.build(), PacketPriority.CONTROL, appId);
    }

    /**
     * Cancel requested ARP packets in via packet service.
     */
    private void cancelArpPackets() {
        TrafficSelector.Builder selectorArpServer = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_ARP);
        packetService.cancelPackets(selectorArpServer.build(), PacketPriority.CONTROL, appId);
    }

    private class DhcpRelayPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            if (!configured()) {
                log.warn("Missing DHCP relay server config. Abort packet processing");
                return;
            }

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
            } else if (packet.getEtherType() == Ethernet.TYPE_ARP && arpEnabled) {
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
                if (log.isTraceEnabled()) {
                    log.trace("Relaying packet to dhcp server {}", packet);
                }
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
            ConnectPoint inPort = context.inPacket().receivedFrom();
            Set<Interface> clientServerInterfaces = interfaceService.getInterfacesByPort(inPort);
            //ignore the packets if dhcp client interface is not configured on onos.
            if (clientServerInterfaces.isEmpty()) {
                log.warn("Virtual interface is not configured on {}", inPort);
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
                // add the gatewayip as virtual interface ip for server to understand
                // the lease to be assigned and forward the packet to dhcp server.
                Ethernet ethernetPacketDiscover =
                    processDhcpPacketFromClient(context, packet, clientServerInterfaces);
                if (ethernetPacketDiscover != null) {
                    forwardPacket(ethernetPacketDiscover);
                }
                break;
            case DHCPOFFER:
                //reply to dhcp client.
                Ethernet ethernetPacketOffer = processDhcpPacketFromServer(packet);
                if (ethernetPacketOffer != null) {
                    sendReply(ethernetPacketOffer, dhcpPayload);
                }
                break;
            case DHCPREQUEST:
                // add the gatewayip as virtual interface ip for server to understand
                // the lease to be assigned and forward the packet to dhcp server.
                Ethernet ethernetPacketRequest =
                    processDhcpPacketFromClient(context, packet, clientServerInterfaces);
                if (ethernetPacketRequest != null) {
                    forwardPacket(ethernetPacketRequest);
                }
                break;
            case DHCPACK:
                //reply to dhcp client.
                Ethernet ethernetPacketAck = processDhcpPacketFromServer(packet);
                if (ethernetPacketAck != null) {
                    sendReply(ethernetPacketAck, dhcpPayload);
                }
                break;
            default:
                break;
            }
        }

        //build the DHCP discover/request packet with gatewayip(unicast packet)
        private Ethernet processDhcpPacketFromClient(PacketContext context,
                             Ethernet ethernetPacket, Set<Interface> clientInterfaces) {
            Ip4Address relayAgentIp = getRelayAgentIPv4Address(clientInterfaces);
            MacAddress relayAgentMac = clientInterfaces.iterator().next().mac();
            if (relayAgentIp == null || relayAgentMac == null) {
                log.warn("Missing DHCP relay agent interface Ipv4 addr config for "
                        + "packet from client on port: {}. Aborting packet processing",
                         clientInterfaces.iterator().next().connectPoint());
                return null;
            }
            if (dhcpConnectMac == null) {
                log.warn("DHCP {} not yet resolved .. Aborting DHCP "
                        + "packet processing from client on port: {}",
                        (dhcpGatewayIp == null) ? "server IP " + dhcpServerIp
                                                : "gateway IP " + dhcpGatewayIp,
                        clientInterfaces.iterator().next().connectPoint());
                return null;
            }
            // get dhcp header.
            Ethernet etherReply = (Ethernet) ethernetPacket.clone();
            etherReply.setSourceMACAddress(relayAgentMac);
            etherReply.setDestinationMACAddress(dhcpConnectMac);
            etherReply.setVlanID(dhcpConnectVlan.toShort());
            IPv4 ipv4Packet = (IPv4) etherReply.getPayload();
            ipv4Packet.setSourceAddress(relayAgentIp.toInt());
            ipv4Packet.setDestinationAddress(dhcpServerIp.toInt());
            UDP udpPacket = (UDP) ipv4Packet.getPayload();
            DHCP dhcpPacket = (DHCP) udpPacket.getPayload();
            dhcpPacket.setGatewayIPAddress(relayAgentIp.toInt());
            udpPacket.setPayload(dhcpPacket);
            ipv4Packet.setPayload(udpPacket);
            etherReply.setPayload(ipv4Packet);
            return etherReply;
        }

        // Returns the first v4 interface ip out of a set of interfaces or null.
        // Checks all interfaces, and ignores v6 interface ips
        private Ip4Address getRelayAgentIPv4Address(Set<Interface> intfs) {
            for (Interface intf : intfs) {
                for (InterfaceIpAddress ip : intf.ipAddressesList()) {
                    Ip4Address relayAgentIp = ip.ipAddress().getIp4Address();
                    if (relayAgentIp != null) {
                        return relayAgentIp;
                    }
                }
            }
            return null;
        }

        //build the DHCP offer/ack with proper client port.
        private Ethernet processDhcpPacketFromServer(Ethernet ethernetPacket) {
            // get dhcp header.
            Ethernet etherReply = (Ethernet) ethernetPacket.clone();
            IPv4 ipv4Packet = (IPv4) etherReply.getPayload();
            UDP udpPacket = (UDP) ipv4Packet.getPayload();
            DHCP dhcpPayload = (DHCP) udpPacket.getPayload();

            // determine the vlanId of the client host - note that this vlan id
            // could be different from the vlan in the packet from the server
            MacAddress dstMac = valueOf(dhcpPayload.getClientHardwareAddress());
            Set<Host> hosts = hostService.getHostsByMac(dstMac);
            if (hosts == null || hosts.isEmpty()) {
                log.warn("Cannot determine host for DHCP client: {}. Aborting "
                        + "relay for dhcp packet from server {}",
                         dhcpPayload.getClientHardwareAddress(), ethernetPacket);
                return null;
            } else if (hosts.size() > 1) {
                // XXX  redo to send reply to all hosts found
                log.warn("Multiple hosts found for mac:{}. Picking one "
                        + "host out of {}", dstMac, hosts);
            }
            Host host = hosts.iterator().next();
            etherReply.setDestinationMACAddress(dstMac);
            etherReply.setVlanID(host.vlan().toShort());
            // we leave the srcMac from the original packet

            // figure out the relay agent IP corresponding to the original request
            Ip4Address relayAgentIP = getRelayAgentIPv4Address(
                          interfaceService.getInterfacesByPort(host.location()));
            if (relayAgentIP == null) {
                log.warn("Cannot determine relay agent interface Ipv4 addr for host {}. "
                        + "Aborting relay for dhcp packet from server {}",
                        host, ethernetPacket);
                return null;
            }
            // SRC_IP: relay agent IP
            // DST_IP: offered IP
            ipv4Packet.setSourceAddress(relayAgentIP.toInt());
            ipv4Packet.setDestinationAddress(dhcpPayload.getYourIPAddress());

            udpPacket.setDestinationPort(UDP.DHCP_CLIENT_PORT);
            udpPacket.setPayload(dhcpPayload);
            ipv4Packet.setPayload(udpPacket);
            etherReply.setPayload(ipv4Packet);
            return etherReply;
        }

        //send the response to the requester host.
        private void sendReply(Ethernet ethPacket, DHCP dhcpPayload) {
            MacAddress descMac = valueOf(dhcpPayload.getClientHardwareAddress());
            Host host = hostService.getHost(HostId.hostId(descMac,
                    VlanId.vlanId(ethPacket.getVlanID())));

            // Send packet out to requester if the host information is available
            if (host != null) {
                TrafficTreatment t = DefaultTrafficTreatment.builder()
                        .setOutput(host.location().port()).build();
                OutboundPacket o = new DefaultOutboundPacket(
                        host.location().deviceId(), t, ByteBuffer.wrap(ethPacket.serialize()));
                if (log.isTraceEnabled()) {
                    log.trace("Relaying packet to dhcp client {}", ethPacket);
                }
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

    /**
     * Internal listener for host events.
     */
    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            switch (event.type()) {
            case HOST_ADDED:
            case HOST_UPDATED:
                hostUpdated(event.subject());
                break;
            case HOST_REMOVED:
                hostRemoved(event.subject());
                break;
            case HOST_MOVED:
                // XXX todo -- moving dhcp server
                break;
            default:
                break;
            }
        }
    }
}
