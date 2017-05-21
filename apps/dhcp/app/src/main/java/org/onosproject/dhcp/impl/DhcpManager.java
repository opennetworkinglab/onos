/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.dhcp.impl;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

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
import org.onlab.util.SharedScheduledExecutors;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.dhcp.DhcpService;
import org.onosproject.dhcp.DhcpStore;
import org.onosproject.dhcp.IpAssignment;
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
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.onlab.packet.DHCP.DHCPOptionCode.OptionCode_DHCPServerIp;
import static org.onlab.packet.DHCP.DHCPOptionCode.OptionCode_MessageType;
import static org.onlab.packet.DHCP.DHCPOptionCode.OptionCode_RequestedIP;
import static org.onlab.packet.DHCPPacketType.DHCPACK;
import static org.onlab.packet.DHCPPacketType.DHCPNAK;
import static org.onlab.packet.DHCPPacketType.DHCPOFFER;
import static org.onlab.packet.MacAddress.valueOf;
import static org.onosproject.dhcp.IpAssignment.AssignmentStatus.Option_RangeNotEnforced;
import static org.onosproject.dhcp.IpAssignment.AssignmentStatus.Option_Requested;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;

/**
 * Skeletal ONOS DHCP Server application.
 */
@Component(immediate = true)
@Service
public class DhcpManager implements DhcpService {

    private static final ProviderId PID = new ProviderId("of", "org.onosproject.dhcp", true);
    private static final String ALLOW_HOST_DISCOVERY = "allowHostDiscovery";
    private static final boolean DEFAULT_ALLOW_HOST_DISCOVERY = false;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final InternalConfigListener cfgListener = new InternalConfigListener();

    private final Set<ConfigFactory> factories = ImmutableSet.of(
            new ConfigFactory<ApplicationId, DhcpConfig>(APP_SUBJECT_FACTORY,
                    DhcpConfig.class,
                    "dhcp") {
                @Override
                public DhcpConfig createConfig() {
                    return new DhcpConfig();
                }
            }
    );
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    private DhcpPacketProcessor processor = new DhcpPacketProcessor();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DhcpStore dhcpStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostProviderRegistry hostProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    @Property(name = ALLOW_HOST_DISCOVERY, boolValue = DEFAULT_ALLOW_HOST_DISCOVERY,
            label = "Allow host discovery from DHCP request")
    private boolean allowHostDiscovery = DEFAULT_ALLOW_HOST_DISCOVERY;

    protected HostProviderService hostProviderService;
    private final HostProvider hostProvider = new InternalHostProvider();
    private ApplicationId appId;

    // Hardcoded values are default values.
    /**
     * leaseTime - 10 mins or 600s.
     * renewalTime - 5 mins or 300s.
     * rebindingTime - 6 mins or 360s.
     */
    private static int leaseTime = 600;
    private static int renewalTime = 300;
    private static int rebindingTime = 360;
    private static byte packetTTL = (byte) 127;
    private static Ip4Address subnetMask = Ip4Address.valueOf("255.0.0.0");
    private static Ip4Address broadcastAddress = Ip4Address.valueOf("10.255.255.255");
    private static Ip4Address routerAddress = Ip4Address.valueOf("10.0.0.2");
    private static Ip4Address domainServer = Ip4Address.valueOf("10.0.0.2");
    private static Ip4Address myIP = Ip4Address.valueOf("10.0.0.2");
    private static MacAddress myMAC = valueOf("4e:4f:4f:4f:4f:4f");
    private static final Ip4Address IP_BROADCAST = Ip4Address.valueOf("255.255.255.255");

    protected ScheduledFuture<?> timeout;
    protected static int timerDelay = 2;

    @Activate
    protected void activate() {
        // start the dhcp server
        appId = coreService.registerApplication("org.onosproject.dhcp");

        componentConfigService.registerProperties(getClass());
        cfgService.addListener(cfgListener);
        factories.forEach(cfgService::registerConfigFactory);
        cfgListener.reconfigureNetwork(cfgService.getConfig(appId, DhcpConfig.class));
        hostProviderService = hostProviderRegistry.register(hostProvider);
        packetService.addProcessor(processor, PacketProcessor.director(1));
        requestPackets();
        timeout = SharedScheduledExecutors.newTimeout(new PurgeListTask(), timerDelay, TimeUnit.MINUTES);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.removeListener(cfgListener);
        factories.forEach(cfgService::unregisterConfigFactory);
        packetService.removeProcessor(processor);
        hostProviderRegistry.unregister(hostProvider);
        hostProviderService = null;
        cancelPackets();
        timeout.cancel(true);
        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        String updatedConfig = Tools.get(properties, ALLOW_HOST_DISCOVERY);
        if (!Strings.isNullOrEmpty(updatedConfig)) {
            allowHostDiscovery = Boolean.valueOf(updatedConfig);
            log.info("Host discovery is set to {}", updatedConfig);
        }

        log.info("Modified");
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

        selectorServer = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_ARP);
        packetService.requestPackets(selectorServer.build(), PacketPriority.CONTROL, appId);
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

        selectorServer = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_ARP);
        packetService.cancelPackets(selectorServer.build(), PacketPriority.CONTROL, appId);
    }

    @Override
    public Map<HostId, IpAssignment> listMapping() {
        return dhcpStore.listAssignedMapping();
    }

    @Override
    public int getLeaseTime() {
        return leaseTime;
    }

    @Override
    public int getRenewalTime() {
        return renewalTime;
    }

    @Override
    public int getRebindingTime() {
        return rebindingTime;
    }

    @Override
    public boolean setStaticMapping(MacAddress macAddress, IpAssignment ipAssignment) {
        log.debug("setStaticMapping is called with Mac: {} IpAssignment: {}",
                  macAddress, ipAssignment);
        return dhcpStore.assignStaticIP(macAddress, ipAssignment);
    }

    @Override
    public boolean removeStaticMapping(MacAddress macID) {
        return dhcpStore.removeStaticIP(macID);
    }

    @Override
    public Iterable<Ip4Address> getAvailableIPs() {
        return dhcpStore.getAvailableIPs();
    }

    private class DhcpPacketProcessor implements PacketProcessor {

        /**
         * Builds the DHCP Reply packet.
         *
         * @param packet the incoming Ethernet frame
         * @param ipOffered the IP offered by the DHCP Server
         * @param outgoingMessageType the message type of the outgoing packet
         * @return the Ethernet reply frame
         */
        private Ethernet buildReply(Ethernet packet, Ip4Address ipOffered, byte outgoingMessageType) {

            // mandatory options
            // TODO save and get the information below to/from IP assignment
            Ip4Address dhcpServerReply = myIP;
            Ip4Address subnetMaskReply = subnetMask;
            Ip4Address broadcastReply = broadcastAddress;

            // optional options
            Optional<Ip4Address> routerAddressReply = Optional.of(routerAddress);
            Optional<Ip4Address> domainServerReply = Optional.of(domainServer);

            IpAssignment ipAssignment = dhcpStore.getIpAssignmentFromAllocationMap(
                    HostId.hostId(packet.getSourceMAC()));

            if (ipAssignment != null &&
                    ipAssignment.assignmentStatus().equals(Option_RangeNotEnforced)) {
                subnetMaskReply = ipAssignment.subnetMask();
                broadcastReply = ipAssignment.broadcast();
                routerAddressReply = Optional.ofNullable(ipAssignment.routerAddress());
                domainServerReply = Optional.ofNullable(ipAssignment.domainServer());
            }

            // Ethernet Frame.
            Ethernet ethReply = new Ethernet();
            ethReply.setSourceMACAddress(myMAC);
            ethReply.setDestinationMACAddress(packet.getSourceMAC());
            ethReply.setEtherType(Ethernet.TYPE_IPV4);
            ethReply.setVlanID(packet.getVlanID());

            // IP Packet
            IPv4 ipv4Packet = (IPv4) packet.getPayload();
            IPv4 ipv4Reply = new IPv4();
            ipv4Reply.setSourceAddress(dhcpServerReply.toInt());
            ipv4Reply.setDestinationAddress(ipOffered.toInt());
            ipv4Reply.setTtl(packetTTL);

            // UDP Datagram.
            UDP udpPacket = (UDP) ipv4Packet.getPayload();
            UDP udpReply = new UDP();
            udpReply.setSourcePort((byte) UDP.DHCP_SERVER_PORT);
            udpReply.setDestinationPort((byte) UDP.DHCP_CLIENT_PORT);

            // DHCP Payload.
            DHCP dhcpPacket = (DHCP) udpPacket.getPayload();
            DHCP dhcpReply = new DHCP();
            dhcpReply.setOpCode(DHCP.OPCODE_REPLY);
            dhcpReply.setFlags(dhcpPacket.getFlags());
            dhcpReply.setGatewayIPAddress(dhcpPacket.getGatewayIPAddress());
            dhcpReply.setClientHardwareAddress(dhcpPacket.getClientHardwareAddress());
            dhcpReply.setTransactionId(dhcpPacket.getTransactionId());

            if (outgoingMessageType != DHCPPacketType.DHCPNAK.getValue()) {
                dhcpReply.setYourIPAddress(ipOffered.toInt());
                dhcpReply.setServerIPAddress(dhcpServerReply.toInt());
                if (dhcpPacket.getGatewayIPAddress() == 0) {
                    ipv4Reply.setDestinationAddress(IP_BROADCAST.toInt());
                }
            }
            dhcpReply.setHardwareType(DHCP.HWTYPE_ETHERNET);
            dhcpReply.setHardwareAddressLength((byte) 6);

            // DHCP Options.
            DHCPOption option = new DHCPOption();
            List<DHCPOption> optionList = new ArrayList<>();

            // DHCP Message Type.
            option.setCode(OptionCode_MessageType.getValue());
            option.setLength((byte) 1);
            byte[] optionData = {outgoingMessageType};
            option.setData(optionData);
            optionList.add(option);

            // DHCP Server Identifier.
            option = new DHCPOption();
            option.setCode(OptionCode_DHCPServerIp.getValue());
            option.setLength((byte) 4);
            option.setData(dhcpServerReply.toOctets());
            optionList.add(option);

            if (outgoingMessageType != DHCPPacketType.DHCPNAK.getValue()) {
                // IP Address Lease Time.
                option = new DHCPOption();
                option.setCode(DHCP.DHCPOptionCode.OptionCode_LeaseTime.getValue());
                option.setLength((byte) 4);
                option.setData(ByteBuffer.allocate(4)
                        .putInt(ipAssignment == null ? leaseTime : ipAssignment.leasePeriod()).array());
                optionList.add(option);

                // IP Address Renewal Time.
                option = new DHCPOption();
                option.setCode(DHCP.DHCPOptionCode.OptionCode_RenewalTime.getValue());
                option.setLength((byte) 4);
                option.setData(ByteBuffer.allocate(4).putInt(renewalTime).array());
                optionList.add(option);

                // IP Address Rebinding Time.
                option = new DHCPOption();
                option.setCode(DHCP.DHCPOptionCode.OPtionCode_RebindingTime.getValue());
                option.setLength((byte) 4);
                option.setData(ByteBuffer.allocate(4).putInt(rebindingTime).array());
                optionList.add(option);

                // Subnet Mask.
                option = new DHCPOption();
                option.setCode(DHCP.DHCPOptionCode.OptionCode_SubnetMask.getValue());
                option.setLength((byte) 4);
                option.setData(subnetMaskReply.toOctets());
                optionList.add(option);

                // Broadcast Address.
                option = new DHCPOption();
                option.setCode(DHCP.DHCPOptionCode.OptionCode_BroadcastAddress.getValue());
                option.setLength((byte) 4);
                option.setData(broadcastReply.toOctets());
                optionList.add(option);

                // Router Address.
                if (routerAddressReply.isPresent()) {
                    option = new DHCPOption();
                    option.setCode(DHCP.DHCPOptionCode.OptionCode_RouterAddress.getValue());
                    option.setLength((byte) 4);
                    option.setData(routerAddressReply.get().toOctets());
                    optionList.add(option);
                }

                // DNS Server Address.
                if (domainServerReply.isPresent()) {
                    option = new DHCPOption();
                    option.setCode(DHCP.DHCPOptionCode.OptionCode_DomainServer.getValue());
                    option.setLength((byte) 4);
                    option.setData(domainServerReply.get().toOctets());
                    optionList.add(option);
                }
            }

            // End Option.
            option = new DHCPOption();
            option.setCode(DHCP.DHCPOptionCode.OptionCode_END.getValue());
            option.setLength((byte) 1);
            optionList.add(option);

            dhcpReply.setOptions(optionList);
            udpReply.setPayload(dhcpReply);
            ipv4Reply.setPayload(udpReply);
            ethReply.setPayload(ipv4Reply);

            return ethReply;
        }

        /**
         * Sends the Ethernet reply frame via the Packet Service.
         *
         * @param context the context of the incoming frame
         * @param reply the Ethernet reply frame
         */
        private void sendReply(PacketContext context, Ethernet reply) {
            if (reply != null) {
                TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
                ConnectPoint sourcePoint = context.inPacket().receivedFrom();
                builder.setOutput(sourcePoint.port());
                context.block();
                packetService.emit(new DefaultOutboundPacket(sourcePoint.deviceId(),
                        builder.build(), ByteBuffer.wrap(reply.serialize())));
            }
        }

        /**
         * Processes the DHCP Payload and initiates a reply to the client.
         *
         * @param context context of the incoming message
         * @param dhcpPayload the extracted DHCP payload
         */
        private void processDhcpPacket(PacketContext context, DHCP dhcpPayload) {
            if (dhcpPayload == null) {
                log.debug("DHCP packet without payload, do nothing");
                return;
            }

            Ethernet packet = context.inPacket().parsed();
            DHCPPacketType incomingPacketType = null;
            boolean flagIfRequestedIP = false;
            boolean flagIfServerIP = false;
            Ip4Address requestedIP = Ip4Address.valueOf("0.0.0.0");
            Ip4Address serverIP = Ip4Address.valueOf("0.0.0.0");

            for (DHCPOption option : dhcpPayload.getOptions()) {
                if (option.getCode() == OptionCode_MessageType.getValue()) {
                    byte[] data = option.getData();
                    incomingPacketType = DHCPPacketType.getType(data[0]);
                }
                if (option.getCode() == OptionCode_RequestedIP.getValue()) {
                    byte[] data = option.getData();
                    requestedIP = Ip4Address.valueOf(data);
                    flagIfRequestedIP = true;
                }
                if (option.getCode() == OptionCode_DHCPServerIp.getValue()) {
                    byte[] data = option.getData();
                    serverIP = Ip4Address.valueOf(data);
                    flagIfServerIP = true;
                }
            }

            if (incomingPacketType == null) {
                log.debug("No incoming packet type specified, ignore it");
                return;
            }

            DHCPPacketType outgoingPacketType;
            MacAddress clientMac = new MacAddress(dhcpPayload.getClientHardwareAddress());
            VlanId vlanId = VlanId.vlanId(packet.getVlanID());
            HostId hostId = HostId.hostId(clientMac, vlanId);

            switch (incomingPacketType) {
                case DHCPDISCOVER:
                    log.trace("DHCP DISCOVER received from {}", hostId);
                    Ip4Address ipOffered = dhcpStore.suggestIP(hostId, requestedIP);
                    if (ipOffered != null) {
                        Ethernet ethReply = buildReply(
                                packet,
                                ipOffered,
                                (byte) DHCPOFFER.getValue());
                        sendReply(context, ethReply);
                    }
                    break;
                case DHCPREQUEST:
                    log.trace("DHCP REQUEST received from {}", hostId);
                    if (flagIfServerIP && !myIP.equals(serverIP)) {
                        return;
                    }

                    if (!flagIfRequestedIP) {
                        // this is renew or rebinding request
                        int clientIp = dhcpPayload.getClientIPAddress();
                        requestedIP = Ip4Address.valueOf(clientIp);
                    }

                    IpAssignment ipAssignment = IpAssignment.builder()
                            .ipAddress(requestedIP)
                            .leasePeriod(leaseTime)
                            .timestamp(new Date())
                            .assignmentStatus(Option_Requested).build();

                    if (dhcpStore.assignIP(hostId, ipAssignment)) {
                        outgoingPacketType = DHCPACK;
                        discoverHost(context, requestedIP);
                    } else {
                        outgoingPacketType = DHCPNAK;
                    }

                    Ethernet ethReply = buildReply(packet, requestedIP, (byte) outgoingPacketType.getValue());
                    sendReply(context, ethReply);
                    break;
                case DHCPRELEASE:
                    log.trace("DHCP RELEASE received from {}", hostId);
                    Ip4Address releaseIp = dhcpStore.releaseIP(hostId);
                    if (releaseIp != null) {
                        hostProviderService.removeIpFromHost(hostId, releaseIp);
                    }
                    break;
                default:
                    break;
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
            sendReply(context, ethReply);
        }

        /**
         * Integrates hosts learned through DHCP into topology.
         * @param context context of the incoming message
         * @param ipAssigned IP Address assigned to the host by DHCP Manager
         */
        private void discoverHost(PacketContext context, Ip4Address ipAssigned) {
            if (!allowHostDiscovery) {
                // host discovery is not allowed, do nothing
                return;
            }

            Ethernet packet = context.inPacket().parsed();
            MacAddress mac = packet.getSourceMAC();
            VlanId vlanId = VlanId.vlanId(packet.getVlanID());
            HostLocation hostLocation = new HostLocation(context.inPacket().receivedFrom(), 0);

            Set<IpAddress> ips = new HashSet<>();
            ips.add(ipAssigned);

            HostId hostId = HostId.hostId(mac, vlanId);
            DefaultHostDescription desc = new DefaultHostDescription(mac, vlanId, hostLocation, ips);

            log.info("Discovered host {}", desc);
            hostProviderService.hostDetected(hostId, desc, false);
        }


        @Override
        public void process(PacketContext context) {
            Ethernet packet = context.inPacket().parsed();
            if (packet == null) {
                return;
            }

            if (packet.getEtherType() == Ethernet.TYPE_IPV4) {
                IPv4 ipv4Packet = (IPv4) packet.getPayload();

                if (ipv4Packet.getProtocol() == IPv4.PROTOCOL_UDP) {
                    UDP udpPacket = (UDP) ipv4Packet.getPayload();

                    if (udpPacket.getDestinationPort() == UDP.DHCP_SERVER_PORT &&
                            udpPacket.getSourcePort() == UDP.DHCP_CLIENT_PORT) {
                        // This is meant for the dhcp server so process the packet here.

                        DHCP dhcpPayload = (DHCP) udpPacket.getPayload();
                        processDhcpPacket(context, dhcpPayload);
                    }
                }
            } else if (packet.getEtherType() == Ethernet.TYPE_ARP) {
                ARP arpPacket = (ARP) packet.getPayload();

                if ((arpPacket.getOpCode() == ARP.OP_REQUEST) &&
                        Objects.equals(myIP, Ip4Address.valueOf(arpPacket.getTargetProtocolAddress()))) {

                    processArpPacket(context, packet);

                }
            }
        }
    }

    private class InternalConfigListener implements NetworkConfigListener {

        /**
         * Reconfigures the DHCP Server according to the configuration parameters passed.
         *
         * @param cfg configuration object
         */
        private void reconfigureNetwork(DhcpConfig cfg) {
            if (cfg == null) {
                return;
            }
            if (cfg.ip() != null) {
                myIP = cfg.ip();
            }
            if (cfg.mac() != null) {
                myMAC = cfg.mac();
            }
            if (cfg.subnetMask() != null) {
                subnetMask = cfg.subnetMask();
            }
            if (cfg.broadcastAddress() != null) {
                broadcastAddress = cfg.broadcastAddress();
            }
            if (cfg.routerAddress() != null) {
                routerAddress = cfg.routerAddress();
            }
            if (cfg.domainServer() != null) {
                domainServer = cfg.domainServer();
            }
            if (cfg.ttl() != -1) {
                packetTTL = (byte) cfg.ttl();
            }
            if (cfg.leaseTime() != -1) {
                leaseTime = cfg.leaseTime();
            }
            if (cfg.renewTime() != -1) {
                renewalTime = cfg.renewTime();
            }
            if (cfg.rebindTime() != -1) {
                rebindingTime = cfg.rebindTime();
            }
            if (cfg.defaultTimeout() != -1) {
                dhcpStore.setDefaultTimeoutForPurge(cfg.defaultTimeout());
            }
            if (cfg.timerDelay() != -1) {
                timerDelay = cfg.timerDelay();
            }
            if ((cfg.startIp() != null) && (cfg.endIp() != null)) {
                dhcpStore.populateIPPoolfromRange(cfg.startIp(), cfg.endIp());
            }
        }


        @Override
        public void event(NetworkConfigEvent event) {

            if ((event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                    event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED) &&
                    event.configClass().equals(DhcpConfig.class)) {

                DhcpConfig cfg = cfgService.getConfig(appId, DhcpConfig.class);
                reconfigureNetwork(cfg);
                log.info("Reconfigured");
            }
        }
    }

    private class InternalHostProvider extends AbstractProvider implements HostProvider {

        /**
         * Creates a provider with the supplier identifier.
         */
        protected InternalHostProvider() {
            super(PID);
        }

        @Override
        public void triggerProbe(Host host) {
            // nothing to do
        }
    }

    private class PurgeListTask implements Runnable {

        @Override
        public void run() {
            IpAssignment ipAssignment;
            Date dateNow = new Date();

            Map<HostId, IpAssignment> ipAssignmentMap = dhcpStore.listAllMapping();
            for (Map.Entry<HostId, IpAssignment> entry: ipAssignmentMap.entrySet()) {
                ipAssignment = entry.getValue();

                long timeLapsed = dateNow.getTime() - ipAssignment.timestamp().getTime();
                if ((ipAssignment.assignmentStatus() != IpAssignment.AssignmentStatus.Option_Expired) &&
                        (ipAssignment.leasePeriod() > 0) && (timeLapsed > (ipAssignment.leasePeriodMs()))) {

                    Ip4Address ip4Address = dhcpStore.releaseIP(entry.getKey());
                    if (ip4Address != null) {
                        hostProviderService.removeIpFromHost(entry.getKey(), ipAssignment.ipAddress());
                    }
                }
            }
            timeout = SharedScheduledExecutors.newTimeout(new PurgeListTask(), timerDelay, TimeUnit.MINUTES);
        }
    }
}
