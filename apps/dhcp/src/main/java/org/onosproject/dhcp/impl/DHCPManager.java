/*
 * Copyright 2014 Open Networking Laboratory
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

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
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
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.dhcp.DHCPService;
import org.onosproject.dhcp.DHCPStore;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.onlab.packet.MacAddress.valueOf;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;

/**
 * Skeletal ONOS DHCP Server application.
 */
@Component(immediate = true)
@Service
public class DHCPManager implements DHCPService {

    private static final ProviderId PID = new ProviderId("of", "org.onosproject.dhcp", true);
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final NetworkConfigListener cfgListener = new InternalConfigListener();

    private final Set<ConfigFactory> factories = ImmutableSet.of(
            new ConfigFactory<ApplicationId, DHCPConfig>(APP_SUBJECT_FACTORY,
                    DHCPConfig.class,
                    "dhcp") {
                @Override
                public DHCPConfig createConfig() {
                    return new DHCPConfig();
                }
            },
            new ConfigFactory<ApplicationId, DHCPStoreConfig>(APP_SUBJECT_FACTORY,
                    DHCPStoreConfig.class,
                    "dhcpstore") {
                @Override
                public DHCPStoreConfig createConfig() {
                    return new DHCPStoreConfig();
                }
            }
    );
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    private DHCPPacketProcessor processor = new DHCPPacketProcessor();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DHCPStore dhcpStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostProviderRegistry hostProviderRegistry;

    protected HostProviderService hostProviderService;

    private ApplicationId appId;

    // Hardcoded values are default values.

    private static String myIP = "10.0.0.2";

    private static MacAddress myMAC = valueOf("4f:4f:4f:4f:4f:4f");

    /**
     * leaseTime - 10 mins or 600s.
     * renewalTime - 5 mins or 300s.
     * rebindingTime - 6 mins or 360s.
     */

    private static int leaseTime = 600;

    private static int renewalTime = 300;

    private static int rebindingTime = 360;

    private static byte packetTTL = (byte) 127;

    private static String subnetMask = "255.0.0.0";

    private static String broadcastAddress = "10.255.255.255";

    private static String routerAddress = "10.0.0.2";

    private static String domainServer = "10.0.0.2";
    private final HostProvider hostProvider = new InternalHostProvider();

    @Activate
    protected void activate() {
        // start the dhcp server
        appId = coreService.registerApplication("org.onosproject.dhcp");

        cfgService.addListener(cfgListener);
        factories.forEach(cfgService::registerConfigFactory);
        hostProviderService = hostProviderRegistry.register(hostProvider);
        packetService.addProcessor(processor, PacketProcessor.observer(1));
        requestPackets();
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
        log.info("Stopped");
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
    public Map<MacAddress, Ip4Address> listMapping() {

        return dhcpStore.listMapping();
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
    public boolean setStaticMapping(MacAddress macID, Ip4Address ipAddress) {
        return dhcpStore.assignStaticIP(macID, ipAddress);
    }

    @Override
    public boolean removeStaticMapping(MacAddress macID) {
        return dhcpStore.removeStaticIP(macID);
    }

    @Override
    public Iterable<Ip4Address> getAvailableIPs() {
        return dhcpStore.getAvailableIPs();
    }

    private class DHCPPacketProcessor implements PacketProcessor {

        /**
         * Builds the DHCP Reply packet.
         *
         * @param packet the incoming Ethernet frame
         * @param ipOffered the IP offered by the DHCP Server
         * @param outgoingMessageType the message type of the outgoing packet
         * @return the Ethernet reply frame
         */
        private Ethernet buildReply(Ethernet packet, String ipOffered, byte outgoingMessageType) {
            Ip4Address myIPAddress = Ip4Address.valueOf(myIP);
            Ip4Address ipAddress;

            // Ethernet Frame.
            Ethernet ethReply = new Ethernet();
            ethReply.setSourceMACAddress(myMAC);
            ethReply.setDestinationMACAddress(packet.getSourceMAC());
            ethReply.setEtherType(Ethernet.TYPE_IPV4);
            ethReply.setVlanID(packet.getVlanID());

            // IP Packet
            IPv4 ipv4Packet = (IPv4) packet.getPayload();
            IPv4 ipv4Reply = new IPv4();
            ipv4Reply.setSourceAddress(myIPAddress.toInt());
            ipAddress = Ip4Address.valueOf(ipOffered);
            ipv4Reply.setDestinationAddress(ipAddress.toInt());
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

            ipAddress = Ip4Address.valueOf(ipOffered);
            dhcpReply.setYourIPAddress(ipAddress.toInt());
            dhcpReply.setServerIPAddress(myIPAddress.toInt());

            dhcpReply.setTransactionId(dhcpPacket.getTransactionId());
            dhcpReply.setClientHardwareAddress(dhcpPacket.getClientHardwareAddress());
            dhcpReply.setHardwareType(DHCP.HWTYPE_ETHERNET);
            dhcpReply.setHardwareAddressLength((byte) 6);

            // DHCP Options.
            DHCPOption option = new DHCPOption();
            List<DHCPOption> optionList = new ArrayList<>();

            // DHCP Message Type.
            option.setCode(DHCP.DHCPOptionCode.OptionCode_MessageType.getValue());
            option.setLength((byte) 1);
            byte[] optionData = {outgoingMessageType};
            option.setData(optionData);
            optionList.add(option);

            // DHCP Server Identifier.
            option = new DHCPOption();
            option.setCode(DHCP.DHCPOptionCode.OptionCode_DHCPServerIp.getValue());
            option.setLength((byte) 4);
            option.setData(myIPAddress.toOctets());
            optionList.add(option);

            // IP Address Lease Time.
            option = new DHCPOption();
            option.setCode(DHCP.DHCPOptionCode.OptionCode_LeaseTime.getValue());
            option.setLength((byte) 4);
            option.setData(ByteBuffer.allocate(4).putInt(leaseTime).array());
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
            ipAddress = Ip4Address.valueOf(subnetMask);
            option.setData(ipAddress.toOctets());
            optionList.add(option);

            // Broadcast Address.
            option = new DHCPOption();
            option.setCode(DHCP.DHCPOptionCode.OptionCode_BroadcastAddress.getValue());
            option.setLength((byte) 4);
            ipAddress = Ip4Address.valueOf(broadcastAddress);
            option.setData(ipAddress.toOctets());
            optionList.add(option);

            // Router Address.
            option = new DHCPOption();
            option.setCode(DHCP.DHCPOptionCode.OptionCode_RouterAddress.getValue());
            option.setLength((byte) 4);
            ipAddress = Ip4Address.valueOf(routerAddress);
            option.setData(ipAddress.toOctets());
            optionList.add(option);

            // DNS Server Address.
            option = new DHCPOption();
            option.setCode(DHCP.DHCPOptionCode.OptionCode_DomainServer.getValue());
            option.setLength((byte) 4);
            ipAddress = Ip4Address.valueOf(domainServer);
            option.setData(ipAddress.toOctets());
            optionList.add(option);

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
        private void processDHCPPacket(PacketContext context, DHCP dhcpPayload) {

            Ethernet packet = context.inPacket().parsed();
            boolean flagIfRequestedIP = false;
            boolean flagIfServerIP = false;
            Ip4Address requestedIP = Ip4Address.valueOf("0.0.0.0");
            Ip4Address serverIP = Ip4Address.valueOf("0.0.0.0");

            if (dhcpPayload != null) {

                // TODO Convert this to enum value.
                byte incomingPacketType = 0;
                for (DHCPOption option : dhcpPayload.getOptions()) {
                    if (option.getCode() == DHCP.DHCPOptionCode.OptionCode_MessageType.getValue()) {
                        byte[] data = option.getData();
                        incomingPacketType = data[0];
                    }
                    if (option.getCode() == DHCP.DHCPOptionCode.OptionCode_RequestedIP.getValue()) {
                        byte[] data = option.getData();
                        requestedIP = Ip4Address.valueOf(data);
                        flagIfRequestedIP = true;
                    }
                    if (option.getCode() == DHCP.DHCPOptionCode.OptionCode_DHCPServerIp.getValue()) {
                        byte[] data = option.getData();
                        serverIP = Ip4Address.valueOf(data);
                        flagIfServerIP = true;
                    }
                }

                String ipOffered = "";
                DHCPPacketType outgoingPacketType;
                MacAddress clientMAC = new MacAddress(dhcpPayload.getClientHardwareAddress());

                if (incomingPacketType == DHCPPacketType.DHCPDISCOVER.getValue()) {

                    outgoingPacketType = DHCPPacketType.DHCPOFFER;
                    ipOffered = dhcpStore.suggestIP(clientMAC, requestedIP).toString();

                    Ethernet ethReply = buildReply(packet, ipOffered, (byte) outgoingPacketType.getValue());
                    sendReply(context, ethReply);

                } else if (incomingPacketType == DHCPPacketType.DHCPREQUEST.getValue()) {

                    outgoingPacketType = DHCPPacketType.DHCPACK;

                    if (flagIfServerIP && flagIfRequestedIP) {
                        // SELECTING state
                        if (myIP.equals(serverIP.toString()) &&
                                dhcpStore.assignIP(clientMAC, requestedIP, leaseTime)) {

                            Ethernet ethReply = buildReply(packet, requestedIP.toString(),
                                    (byte) outgoingPacketType.getValue());
                            sendReply(context, ethReply);
                            discoverHost(context, requestedIP);
                        }
                    } else if (flagIfRequestedIP) {
                        // INIT-REBOOT state
                        if (dhcpStore.assignIP(clientMAC, requestedIP, leaseTime)) {
                            Ethernet ethReply = buildReply(packet, requestedIP.toString(),
                                    (byte) outgoingPacketType.getValue());
                            sendReply(context, ethReply);
                            discoverHost(context, requestedIP);
                        }
                    } else {
                        // RENEWING and REBINDING state
                        int ciaadr = dhcpPayload.getClientIPAddress();
                        if (ciaadr != 0) {
                            Ip4Address clientIaddr = Ip4Address.valueOf(ciaadr);
                            if (dhcpStore.assignIP(clientMAC, clientIaddr, leaseTime)) {
                                Ethernet ethReply = buildReply(packet, clientIaddr.toString(),
                                        (byte) outgoingPacketType.getValue());
                                sendReply(context, ethReply);
                                discoverHost(context, clientIaddr);
                            }
                        }
                    }
                } else if (incomingPacketType == DHCPPacketType.DHCPRELEASE.getValue()) {

                    dhcpStore.releaseIP(clientMAC);
                }
            }
        }

        /**
         * Processes the ARP Payload and initiates a reply to the client.
         *
         * @param context context of the incoming message
         * @param packet the ethernet payload
         */
        private void processARPPacket(PacketContext context, Ethernet packet) {

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
            Ethernet packet = context.inPacket().parsed();
            MacAddress mac = packet.getSourceMAC();
            VlanId vlanId = VlanId.vlanId(packet.getVlanID());
            HostLocation hostLocation = new HostLocation(context.inPacket().receivedFrom(), 0);

            Set<IpAddress> ips = new HashSet<>();
            ips.add(ipAssigned);

            HostId hostId = HostId.hostId(mac, vlanId);
            DefaultHostDescription desc = new DefaultHostDescription(mac, vlanId, hostLocation, ips);
            hostProviderService.hostDetected(hostId, desc);
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
                        processDHCPPacket(context, dhcpPayload);
                    }
                }
            } else if (packet.getEtherType() == Ethernet.TYPE_ARP) {
                ARP arpPacket = (ARP) packet.getPayload();

                if ((arpPacket.getOpCode() == ARP.OP_REQUEST) &&
                        (Ip4Address.valueOf(arpPacket.getTargetProtocolAddress()).toString().equals(myIP))) {

                    processARPPacket(context, packet);

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
        private void reconfigureNetwork(DHCPConfig cfg) {

            if (cfg.ip() != null) {
                myIP = cfg.ip();
            }
            if (cfg.mac() != null) {
                myMAC = MacAddress.valueOf(cfg.mac());
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
            if (cfg.ttl() != null) {
                packetTTL = Byte.valueOf(cfg.ttl());
            }
            if (cfg.leaseTime() != null) {
                leaseTime = Integer.valueOf(cfg.leaseTime());
            }
            if (cfg.renewTime() != null) {
                renewalTime = Integer.valueOf(cfg.renewTime());
            }
            if (cfg.rebindTime() != null) {
                rebindingTime = Integer.valueOf(cfg.rebindTime());
            }
        }

        /**
         * Reconfigures the DHCP Store according to the configuration parameters passed.
         *
         * @param cfg configuration object
         */
        private void reconfigureStore(DHCPStoreConfig cfg) {

            if (cfg.defaultTimeout() != null) {
                dhcpStore.setDefaultTimeoutForPurge(Integer.valueOf(cfg.defaultTimeout()));
            }
            if (cfg.timerDelay() != null) {
                dhcpStore.setTimerDelay(Integer.valueOf(cfg.defaultTimeout()));
            }
            if ((cfg.startIP() != null) && (cfg.endIP() != null)) {
                dhcpStore.populateIPPoolfromRange(Ip4Address.valueOf(cfg.startIP()),
                        Ip4Address.valueOf(cfg.endIP()));
            }
        }

        @Override
        public void event(NetworkConfigEvent event) {

            if ((event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                    event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED)) {
                if (event.configClass().equals(DHCPConfig.class)) {
                    DHCPConfig cfg = cfgService.getConfig(appId, DHCPConfig.class);
                    reconfigureNetwork(cfg);
                    log.info("Reconfigured Manager");
                }
                if (event.configClass().equals(DHCPStoreConfig.class)) {
                    DHCPStoreConfig cfg = cfgService.getConfig(appId, DHCPStoreConfig.class);
                    reconfigureStore(cfg);
                    log.info("Reconfigured Store");
                }
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
}