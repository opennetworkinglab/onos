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
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.ARP;
import org.onlab.packet.DHCP;
import org.onlab.packet.DHCP6;
import org.onlab.packet.IPacket;
import org.onlab.packet.IPv6;
import org.onlab.packet.dhcp.DhcpOption;
import org.onlab.packet.dhcp.CircuitId;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onlab.packet.dhcp.DhcpRelayAgentOption;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.dhcprelay.store.DhcpRecord;
import org.onosproject.dhcprelay.store.DhcpRelayStore;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteStore;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.HostStore;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import static org.onlab.packet.DHCP.DHCPOptionCode.OptionCode_CircuitID;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.packet.dhcp.DhcpRelayAgentOption.RelayAgentInfoOptions.CIRCUIT_ID;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;
import static org.onlab.packet.DHCP.DHCPOptionCode.OptionCode_MessageType;
import static org.onlab.packet.MacAddress.valueOf;
/**
 * DHCP Relay Agent Application Component.
 */
@Component(immediate = true)
@Service
public class DhcpRelayManager implements DhcpRelayService {
    public static final String DHCP_RELAY_APP = "org.onosproject.dhcp-relay";
    protected static final ProviderId PROVIDER_ID = new ProviderId("dhcp", DHCP_RELAY_APP);
    public static final String HOST_LOCATION_PROVIDER =
            "org.onosproject.provider.host.impl.HostLocationProvider";
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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostStore hostStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouteStore routeStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DhcpRelayStore dhcpRelayStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService compCfgService;

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

        // disable dhcp from host location provider
        compCfgService.preSetProperty(HOST_LOCATION_PROVIDER,
                                      "useDhcp", Boolean.FALSE.toString());
        compCfgService.registerProperties(getClass());
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
        compCfgService.unregisterProperties(getClass(), true);
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
        if (dhcpGatewayIp != null && host.ipAddresses().contains(dhcpGatewayIp)) {
            dhcpConnectMac = host.mac();
            dhcpConnectVlan = host.vlan();
            log.info("DHCP gateway {} resolved to Mac/Vlan:{}/{}", dhcpGatewayIp,
                     dhcpConnectMac, dhcpConnectVlan);
        } else if (host.ipAddresses().contains(dhcpServerIp)) {
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

    @Override
    public Optional<DhcpRecord> getDhcpRecord(HostId hostId) {
        return dhcpRelayStore.getDhcpRecord(hostId);
    }

    @Override
    public Collection<DhcpRecord> getDhcpRecords() {
        return dhcpRelayStore.getDhcpRecords();
    }

    /**
     * Gets output interface of a dhcp packet.
     * If option 82 exists in the dhcp packet and the option was sent by
     * ONOS (gateway address exists in ONOS interfaces), use the connect
     * point and vlan id from circuit id; otherwise, find host by destination
     * address and use vlan id from sender (dhcp server).
     *
     * @param ethPacket the ethernet packet
     * @param dhcpPayload the dhcp packet
     * @return an interface represent the output port and vlan; empty value
     *         if the host or circuit id not found
     */
    private Optional<Interface> getOutputInterface(Ethernet ethPacket, DHCP dhcpPayload) {
        VlanId originalPacketVlanId = VlanId.vlanId(ethPacket.getVlanID());
        IpAddress gatewayIpAddress = Ip4Address.valueOf(dhcpPayload.getGatewayIPAddress());
        Set<Interface> gatewayInterfaces = interfaceService.getInterfacesByIp(gatewayIpAddress);
        DhcpRelayAgentOption option = (DhcpRelayAgentOption) dhcpPayload.getOption(OptionCode_CircuitID);

        // Sent by ONOS, and contains circuit id
        if (!gatewayInterfaces.isEmpty() && option != null) {
            DhcpOption circuitIdSubOption = option.getSubOption(CIRCUIT_ID.getValue());
            try {
                CircuitId circuitId = CircuitId.deserialize(circuitIdSubOption.getData());
                ConnectPoint connectPoint = ConnectPoint.deviceConnectPoint(circuitId.connectPoint());
                VlanId vlanId = circuitId.vlanId();
                return Optional.of(new Interface(null, connectPoint, null, null, vlanId));
            } catch (IllegalArgumentException ex) {
                // invalid circuit format, didn't sent by ONOS
                log.debug("Invalid circuit {}, use information from dhcp payload",
                          circuitIdSubOption.getData());
            }
        }

        // Use Vlan Id from DHCP server if DHCP relay circuit id was not
        // sent by ONOS or circuit Id can't be parsed
        MacAddress dstMac = valueOf(dhcpPayload.getClientHardwareAddress());
        Optional<DhcpRecord> dhcpRecord = dhcpRelayStore.getDhcpRecord(HostId.hostId(dstMac, originalPacketVlanId));
        return dhcpRecord
                .map(DhcpRecord::locations)
                .orElse(Collections.emptySet())
                .stream()
                .reduce((hl1, hl2) -> {
                    if (hl1 == null || hl2 == null) {
                        return hl1 == null ? hl2 : hl1;
                    }
                    return hl1.time() > hl2.time() ? hl1 : hl2;
                })
                .map(lastLocation -> new Interface(null, lastLocation, null, null, originalPacketVlanId));
    }

    /**
     * Send the response DHCP to the requester host.
     *
     * @param ethPacket the packet
     * @param dhcpPayload the DHCP data
     */
    private void handleDhcpOffer(Ethernet ethPacket, DHCP dhcpPayload) {
        Optional<Interface> outInterface = getOutputInterface(ethPacket, dhcpPayload);
        outInterface.ifPresent(theInterface -> {
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(theInterface.connectPoint().port())
                    .build();
            OutboundPacket o = new DefaultOutboundPacket(
                    theInterface.connectPoint().deviceId(),
                    treatment,
                    ByteBuffer.wrap(ethPacket.serialize()));
            if (log.isTraceEnabled()) {
                log.trace("Relaying packet to DHCP client {} via {}, vlan {}",
                          ethPacket,
                          theInterface.connectPoint(),
                          theInterface.vlan());
            }
            packetService.emit(o);
        });
    }

    /**
     * Send the DHCP ack to the requester host.
     *
     * @param ethernetPacketAck the packet
     * @param dhcpPayload the DHCP data
     */
    private void handleDhcpAck(Ethernet ethernetPacketAck, DHCP dhcpPayload) {
        Optional<Interface> outInterface = getOutputInterface(ethernetPacketAck, dhcpPayload);
        if (!outInterface.isPresent()) {
            log.warn("Can't find output interface for dhcp: {}", dhcpPayload);
            return;
        }

        Interface outIface = outInterface.get();
        HostLocation hostLocation = new HostLocation(outIface.connectPoint(), System.currentTimeMillis());
        MacAddress macAddress = MacAddress.valueOf(dhcpPayload.getClientHardwareAddress());
        VlanId vlanId = outIface.vlan();
        HostId hostId = HostId.hostId(macAddress, vlanId);
        Ip4Address ip = Ip4Address.valueOf(dhcpPayload.getYourIPAddress());

        if (directlyConnected(dhcpPayload)) {
            // Add to host store if it connect to network directly
            Set<IpAddress> ips = Sets.newHashSet(ip);
            HostDescription desc = new DefaultHostDescription(macAddress, vlanId,
                                                              hostLocation, ips);

            // Replace the ip when dhcp server give the host new ip address
            hostStore.createOrUpdateHost(PROVIDER_ID, hostId, desc, true);
        } else {
            // Add to route store if it does not connect to network directly
            // Get gateway host IP according to host mac address
            DhcpRecord record = dhcpRelayStore.getDhcpRecord(hostId).orElse(null);

            if (record == null) {
                log.warn("Can't find DHCP record of host {}", hostId);
                return;
            }

            MacAddress gwMac = record.nextHop().orElse(null);
            if (gwMac == null) {
                log.warn("Can't find gateway mac address from record {}", record);
                return;
            }

            HostId gwHostId = HostId.hostId(gwMac, record.vlanId());
            Host gwHost = hostService.getHost(gwHostId);

            if (gwHost == null) {
                log.warn("Can't find gateway host {}", gwHostId);
                return;
            }

            Ip4Address nextHopIp = gwHost.ipAddresses()
                    .stream()
                    .filter(IpAddress::isIp4)
                    .map(IpAddress::getIp4Address)
                    .findFirst()
                    .orElse(null);

            if (nextHopIp == null) {
                log.warn("Can't find IP address of gateway {}", gwHost);
                return;
            }

            Route route = new Route(Route.Source.STATIC, ip.toIpPrefix(), nextHopIp);
            routeStore.updateRoute(route);
        }
        handleDhcpOffer(ethernetPacketAck, dhcpPayload);
    }

    /**
     * forward the packet to ConnectPoint where the DHCP server is attached.
     *
     * @param packet the packet
     */
    private void handleDhcpDiscoverAndRequest(Ethernet packet) {
        // send packet to dhcp server connect point.
        if (dhcpServerConnectPoint != null) {
            TrafficTreatment t = DefaultTrafficTreatment.builder()
                    .setOutput(dhcpServerConnectPoint.port()).build();
            OutboundPacket o = new DefaultOutboundPacket(
                    dhcpServerConnectPoint.deviceId(), t, ByteBuffer.wrap(packet.serialize()));
            if (log.isTraceEnabled()) {
                log.trace("Relaying packet to dhcp server {}", packet);
            }
            packetService.emit(o);
        } else {
            log.warn("Can't find DHCP server connect point, abort.");
        }
    }

    /**
     * Gets DHCP data from a packet.
     *
     * @param packet the packet
     * @return the DHCP data; empty if it is not a DHCP packet
     */
    private Optional<DHCP> findDhcp(Ethernet packet) {
        return Stream.of(packet)
                .filter(Objects::nonNull)
                .map(Ethernet::getPayload)
                .filter(p -> p instanceof IPv4)
                .map(IPacket::getPayload)
                .filter(Objects::nonNull)
                .filter(p -> p instanceof UDP)
                .map(IPacket::getPayload)
                .filter(Objects::nonNull)
                .filter(p -> p instanceof DHCP)
                .map(p -> (DHCP) p)
                .findFirst();
    }

    /**
     * Gets DHCPv6 data from a packet.
     *
     * @param packet the packet
     * @return the DHCPv6 data; empty if it is not a DHCPv6 packet
     */
    private Optional<DHCP6> findDhcp6(Ethernet packet) {
        return Stream.of(packet)
                .filter(Objects::nonNull)
                .map(Ethernet::getPayload)
                .filter(p -> p instanceof IPv6)
                .map(IPacket::getPayload)
                .filter(Objects::nonNull)
                .filter(p -> p instanceof UDP)
                .map(IPacket::getPayload)
                .filter(Objects::nonNull)
                .filter(p -> p instanceof DHCP6)
                .map(p -> (DHCP6) p)
                .findFirst();
    }

    /**
     * Check if the host is directly connected to the network or not.
     *
     * @param dhcpPayload the dhcp payload
     * @return true if the host is directly connected to the network; false otherwise
     */
    private boolean directlyConnected(DHCP dhcpPayload) {
        DhcpOption relayAgentOption = dhcpPayload.getOption(OptionCode_CircuitID);

        // Doesn't contains relay option
        if (relayAgentOption == null) {
            return true;
        }

        IpAddress gatewayIp = IpAddress.valueOf(dhcpPayload.getGatewayIPAddress());
        Set<Interface> gatewayInterfaces = interfaceService.getInterfacesByIp(gatewayIp);

        // Contains relay option, and added by ONOS
        if (!gatewayInterfaces.isEmpty()) {
            return true;
        }

        // Relay option added by other relay agent
        return false;
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

            findDhcp(packet).ifPresent(dhcpPayload -> {
                processDhcpPacket(context, dhcpPayload);
            });

            findDhcp6(packet).ifPresent(dhcp6Payload -> {
                // TODO: handle DHCPv6 packet
                log.warn("DHCPv6 unsupported.");
            });

            if (packet.getEtherType() == Ethernet.TYPE_ARP && arpEnabled) {
                ARP arpPacket = (ARP) packet.getPayload();
                VlanId vlanId = VlanId.vlanId(packet.getVlanID());
                Set<Interface> interfaces = interfaceService.
                        getInterfacesByPort(context.inPacket().receivedFrom());
                //ignore the packets if dhcp server interface is not configured on onos.
                if (interfaces.isEmpty()) {
                    log.warn("server virtual interface not configured");
                    return;
                }
                if ((arpPacket.getOpCode() != ARP.OP_REQUEST)) {
                    // handle request only
                    return;
                }
                MacAddress interfaceMac = interfaces.stream()
                        .filter(iface -> iface.vlan().equals(vlanId))
                        .map(Interface::mac)
                        .filter(mac -> !mac.equals(MacAddress.NONE))
                        .findFirst()
                        .orElse(null);

                if (interfaceMac == null) {
                    // can't find interface mac address
                    return;
                }
                processArpPacket(context, packet, interfaceMac);
            }
        }

        /**
         * Processes the ARP Payload and initiates a reply to the client.
         *
         * @param context the packet context
         * @param packet the ethernet payload
         * @param replyMac mac address to be replied
         */
        private void processArpPacket(PacketContext context, Ethernet packet, MacAddress replyMac) {
            ARP arpPacket = (ARP) packet.getPayload();
            ARP arpReply = (ARP) arpPacket.clone();
            arpReply.setOpCode(ARP.OP_REPLY);

            arpReply.setTargetProtocolAddress(arpPacket.getSenderProtocolAddress());
            arpReply.setTargetHardwareAddress(arpPacket.getSenderHardwareAddress());
            arpReply.setSenderProtocolAddress(arpPacket.getTargetProtocolAddress());
            arpReply.setSenderHardwareAddress(replyMac.toBytes());

            // Ethernet Frame.
            Ethernet ethReply = new Ethernet();
            ethReply.setSourceMACAddress(replyMac.toBytes());
            ethReply.setDestinationMACAddress(packet.getSourceMAC());
            ethReply.setEtherType(Ethernet.TYPE_ARP);
            ethReply.setVlanID(packet.getVlanID());
            ethReply.setPayload(arpReply);

            ConnectPoint targetPort = context.inPacket().receivedFrom();
            TrafficTreatment t = DefaultTrafficTreatment.builder()
                    .setOutput(targetPort.port()).build();
            OutboundPacket o = new DefaultOutboundPacket(
                    targetPort.deviceId(), t, ByteBuffer.wrap(ethReply.serialize()));
            if (log.isTraceEnabled()) {
                log.trace("Relaying ARP packet {} to {}", packet, targetPort);
            }
            packetService.emit(o);
        }

        // process the dhcp packet before sending to server
        private void processDhcpPacket(PacketContext context, DHCP dhcpPayload) {
            ConnectPoint inPort = context.inPacket().receivedFrom();
            Set<Interface> clientServerInterfaces = interfaceService.getInterfacesByPort(inPort);
            // ignore the packets if dhcp client interface is not configured on onos.
            if (clientServerInterfaces.isEmpty()) {
                log.warn("Virtual interface is not configured on {}", inPort);
                return;
            }
            checkNotNull(dhcpPayload, "Can't find DHCP payload");
            Ethernet packet = context.inPacket().parsed();
            DHCP.MsgType incomingPacketType = dhcpPayload.getOptions().stream()
                    .filter(dhcpOption -> dhcpOption.getCode() == OptionCode_MessageType.getValue())
                    .map(DhcpOption::getData)
                    .map(data -> DHCP.MsgType.getType(data[0]))
                    .findFirst()
                    .orElse(null);
            checkNotNull(incomingPacketType, "Can't get message type from DHCP payload {}", dhcpPayload);
            switch (incomingPacketType) {
                case DHCPDISCOVER:
                    // add the gatewayip as virtual interface ip for server to understand
                    // the lease to be assigned and forward the packet to dhcp server.
                    Ethernet ethernetPacketDiscover =
                            processDhcpPacketFromClient(context, packet, clientServerInterfaces);

                    if (ethernetPacketDiscover != null) {
                        writeRequestDhcpRecord(inPort, packet, dhcpPayload);
                        handleDhcpDiscoverAndRequest(ethernetPacketDiscover);
                    }
                    break;
                case DHCPOFFER:
                    //reply to dhcp client.
                    Ethernet ethernetPacketOffer = processDhcpPacketFromServer(packet);
                    if (ethernetPacketOffer != null) {
                        writeResponseDhcpRecord(ethernetPacketOffer, dhcpPayload);
                        handleDhcpOffer(ethernetPacketOffer, dhcpPayload);
                    }
                    break;
                case DHCPREQUEST:
                    // add the gateway ip as virtual interface ip for server to understand
                    // the lease to be assigned and forward the packet to dhcp server.
                    Ethernet ethernetPacketRequest =
                            processDhcpPacketFromClient(context, packet, clientServerInterfaces);
                    if (ethernetPacketRequest != null) {
                        writeRequestDhcpRecord(inPort, packet, dhcpPayload);
                        handleDhcpDiscoverAndRequest(ethernetPacketRequest);
                    }
                    break;
                case DHCPACK:
                    // reply to dhcp client.
                    Ethernet ethernetPacketAck = processDhcpPacketFromServer(packet);
                    if (ethernetPacketAck != null) {
                        writeResponseDhcpRecord(ethernetPacketAck, dhcpPayload);
                        handleDhcpAck(ethernetPacketAck, dhcpPayload);
                    }
                    break;
                case DHCPRELEASE:
                    // TODO: release the ip address from client
                    break;
                default:
                    break;
            }
        }

        private void writeRequestDhcpRecord(ConnectPoint location,
                                            Ethernet ethernet,
                                            DHCP dhcpPayload) {
            VlanId vlanId = VlanId.vlanId(ethernet.getVlanID());
            MacAddress macAddress = MacAddress.valueOf(dhcpPayload.getClientHardwareAddress());
            HostId hostId = HostId.hostId(macAddress, vlanId);
            DhcpRecord record = dhcpRelayStore.getDhcpRecord(hostId).orElse(null);
            if (record == null) {
                record = new DhcpRecord(HostId.hostId(macAddress, vlanId));
            } else {
                record = record.clone();
            }
            record.addLocation(new HostLocation(location, System.currentTimeMillis()));
            record.ip4Status(dhcpPayload.getPacketType());
            record.setDirectlyConnected(directlyConnected(dhcpPayload));
            if (!directlyConnected(dhcpPayload)) {
                // Update gateway mac address if the host is not directly connected
                record.nextHop(ethernet.getSourceMAC());
            }
            record.updateLastSeen();
            dhcpRelayStore.updateDhcpRecord(HostId.hostId(macAddress, vlanId), record);
        }

        private void writeResponseDhcpRecord(Ethernet ethernet,
                                             DHCP dhcpPayload) {
            Optional<Interface> outInterface = getOutputInterface(ethernet, dhcpPayload);
            if (!outInterface.isPresent()) {
                log.warn("Failed to determine where to send {}", dhcpPayload.getPacketType());
                return;
            }

            Interface outIface = outInterface.get();
            ConnectPoint location = outIface.connectPoint();
            VlanId vlanId = outIface.vlan();
            MacAddress macAddress = MacAddress.valueOf(dhcpPayload.getClientHardwareAddress());
            HostId hostId = HostId.hostId(macAddress, vlanId);
            DhcpRecord record = dhcpRelayStore.getDhcpRecord(hostId).orElse(null);
            if (record == null) {
                record = new DhcpRecord(HostId.hostId(macAddress, vlanId));
            } else {
                record = record.clone();
            }
            record.addLocation(new HostLocation(location, System.currentTimeMillis()));
            if (dhcpPayload.getPacketType() == DHCP.MsgType.DHCPACK) {
                record.ip4Address(Ip4Address.valueOf(dhcpPayload.getYourIPAddress()));
            }
            record.ip4Status(dhcpPayload.getPacketType());
            record.setDirectlyConnected(directlyConnected(dhcpPayload));
            record.updateLastSeen();
            dhcpRelayStore.updateDhcpRecord(HostId.hostId(macAddress, vlanId), record);
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

            // If there is no relay agent option(option 82), add one to DHCP payload
            boolean containsRelayAgentOption = dhcpPacket.getOptions().stream()
                    .map(DhcpOption::getCode)
                    .anyMatch(code -> code == OptionCode_CircuitID.getValue());

            if (!containsRelayAgentOption) {
                ConnectPoint inPort = context.inPacket().receivedFrom();
                VlanId vlanId = VlanId.vlanId(ethernetPacket.getVlanID());
                // add connected in port and vlan
                CircuitId cid = new CircuitId(inPort.toString(), vlanId);
                byte[] circuitId = cid.serialize();

                if (circuitId != null) {
                    DhcpOption circuitIdSubOpt = new DhcpOption();
                    circuitIdSubOpt
                            .setCode(CIRCUIT_ID.getValue())
                            .setLength((byte) circuitId.length)
                            .setData(circuitId);

                    DhcpRelayAgentOption newRelayAgentOpt = new DhcpRelayAgentOption();
                    newRelayAgentOpt.setCode(OptionCode_CircuitID.getValue());
                    newRelayAgentOpt.addSubOption(circuitIdSubOpt);

                    // push new circuit id to last
                    List<DhcpOption> options = dhcpPacket.getOptions();
                    options.add(newRelayAgentOpt);

                    // make sure option 255(End) is the last option
                    options.sort((opt1, opt2) -> opt2.getCode() - opt1.getCode());
                    dhcpPacket.setOptions(options);

                    dhcpPacket.setGatewayIPAddress(relayAgentIp.toInt());
                } else {
                    log.warn("Can't generate circuit id for port {}, vlan {}", inPort, vlanId);
                }
            } else {
                // TODO: if it contains a relay agent information, sets gateway ip
                // according to the information it provided
            }

            udpPacket.setPayload(dhcpPacket);
            udpPacket.setSourcePort(UDP.DHCP_CLIENT_PORT);
            udpPacket.setDestinationPort(UDP.DHCP_SERVER_PORT);
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
            Interface outInterface = getOutputInterface(ethernetPacket, dhcpPayload).orElse(null);

            if (outInterface == null) {
                log.warn("Cannot find the interface for the DHCP {}", dhcpPayload);
                return null;
            }

            etherReply.setDestinationMACAddress(dhcpPayload.getClientHardwareAddress());
            etherReply.setVlanID(outInterface.vlan().toShort());
            // we leave the srcMac from the original packet

            // figure out the relay agent IP corresponding to the original request
            Ip4Address relayAgentIP = getRelayAgentIPv4Address(
                          interfaceService.getInterfacesByPort(outInterface.connectPoint()));
            if (relayAgentIP == null) {
                log.warn("Cannot determine relay agent interface Ipv4 addr for host {}/{}. "
                        + "Aborting relay for dhcp packet from server {}",
                         etherReply.getDestinationMAC(), outInterface.vlan(),
                         ethernetPacket);
                return null;
            }
            // SRC_IP: relay agent IP
            // DST_IP: offered IP
            ipv4Packet.setSourceAddress(relayAgentIP.toInt());
            ipv4Packet.setDestinationAddress(dhcpPayload.getYourIPAddress());
            udpPacket.setSourcePort(UDP.DHCP_SERVER_PORT);
            if (directlyConnected(dhcpPayload)) {
                udpPacket.setDestinationPort(UDP.DHCP_CLIENT_PORT);
            } else {
                // forward to another dhcp relay
                udpPacket.setDestinationPort(UDP.DHCP_SERVER_PORT);
            }

            udpPacket.setPayload(dhcpPayload);
            ipv4Packet.setPayload(udpPacket);
            etherReply.setPayload(ipv4Packet);
            return etherReply;
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
