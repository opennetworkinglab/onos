/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.openstacknetworking.impl;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.DHCP;
import org.onlab.packet.DHCPOption;
import org.onlab.packet.DHCPPacketType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Subnet;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.List;

import static org.onlab.packet.DHCP.DHCPOptionCode.*;
import static org.onlab.packet.DHCPPacketType.DHCPACK;
import static org.onlab.packet.DHCPPacketType.DHCPOFFER;
import static org.onosproject.openstacknetworking.api.Constants.DEFAULT_GATEWAY_MAC_STR;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles DHCP requests for the virtual instances.
 */
@Component(immediate = true)
public class OpenstackSwitchingDhcpHandler {
    protected final Logger log = getLogger(getClass());

    private static final String DHCP_SERVER_MAC = "dhcpServerMac";
    private static final Ip4Address DEFAULT_DNS = Ip4Address.valueOf("8.8.8.8");
    private static final byte PACKET_TTL = (byte) 127;
    // TODO add MTU, static route option codes to ONOS DHCP and remove here
    private static final byte DHCP_OPTION_MTU = (byte) 26;
    private static final byte[] DHCP_DATA_LEASE_INFINITE =
            ByteBuffer.allocate(4).putInt(-1).array();
    private static final byte[] DHCP_DATA_MTU_DEFAULT =
            ByteBuffer.allocate(2).putShort((short) 1450).array();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNetworkService osNetworkService;

    @Property(name = DHCP_SERVER_MAC, value = DEFAULT_GATEWAY_MAC_STR,
            label = "Fake MAC address for virtual network subnet gateway")
    private String dhcpServerMac = DEFAULT_GATEWAY_MAC_STR;

    private final PacketProcessor packetProcessor = new InternalPacketProcessor();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(Constants.OPENSTACK_NETWORKING_APP_ID);
        configService.registerProperties(getClass());
        packetService.addProcessor(packetProcessor, PacketProcessor.director(0));
        requestPackets();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        cancelPackets();
        packetService.removeProcessor(packetProcessor);
        configService.unregisterProperties(getClass(), false);

        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        String updatedMac;

        updatedMac = Tools.get(properties, DHCP_SERVER_MAC);
        if (!Strings.isNullOrEmpty(updatedMac) && !updatedMac.equals(dhcpServerMac)) {
            dhcpServerMac = updatedMac;
        }

        log.info("Modified");
    }

    private void requestPackets() {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpDst(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
                .matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT))
                .build();
        packetService.requestPackets(selector, PacketPriority.CONTROL, appId);
    }

    private void cancelPackets() {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpDst(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
                .matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT))
                .build();
        packetService.cancelPackets(selector, PacketPriority.CONTROL, appId);
    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            }

            Ethernet ethPacket = context.inPacket().parsed();
            if (ethPacket == null || ethPacket.getEtherType() != Ethernet.TYPE_IPV4) {
                return;
            }
            IPv4 ipv4Packet = (IPv4) ethPacket.getPayload();
            if (ipv4Packet.getProtocol() != IPv4.PROTOCOL_UDP) {
                return;
            }
            UDP udpPacket = (UDP) ipv4Packet.getPayload();
            if (udpPacket.getDestinationPort() != UDP.DHCP_SERVER_PORT ||
                    udpPacket.getSourcePort() != UDP.DHCP_CLIENT_PORT) {
                return;
            }

            DHCP dhcpPacket = (DHCP) udpPacket.getPayload();
            processDhcp(context, dhcpPacket);
        }

        private void processDhcp(PacketContext context, DHCP dhcpPacket) {
            if (dhcpPacket == null) {
                log.trace("DHCP packet without payload received, do nothing");
                return;
            }

            DHCPPacketType inPacketType = getPacketType(dhcpPacket);
            if (inPacketType == null || dhcpPacket.getClientHardwareAddress() == null) {
                log.trace("Malformed DHCP packet received, ignore it");
                return;
            }

            MacAddress clientMac = MacAddress.valueOf(dhcpPacket.getClientHardwareAddress());
            InstancePort reqInstPort = instancePortService.instancePort(clientMac);
            if (reqInstPort == null) {
                log.trace("Failed to find host(MAC:{})", clientMac);
                return;
            }
            Ethernet ethPacket = context.inPacket().parsed();
            switch (inPacketType) {
                case DHCPDISCOVER:
                    log.trace("DHCP DISCOVER received from {}", clientMac);
                    Ethernet discoverReply = buildReply(
                            ethPacket,
                            (byte) DHCPOFFER.getValue(),
                            reqInstPort);
                    sendReply(context, discoverReply);
                    log.trace("DHCP OFFER({}) is sent for {}",
                              reqInstPort.ipAddress(), clientMac);
                    break;
                case DHCPREQUEST:
                    log.trace("DHCP REQUEST received from {}", clientMac);
                    Ethernet requestReply = buildReply(
                            ethPacket,
                            (byte) DHCPACK.getValue(),
                            reqInstPort);
                    sendReply(context, requestReply);
                    log.trace("DHCP ACK({}) is sent for {}",
                              reqInstPort.ipAddress(), clientMac);
                    break;
                case DHCPRELEASE:
                    log.trace("DHCP RELEASE received from {}", clientMac);
                    // do nothing
                    break;
                default:
                    break;
            }
        }

        private DHCPPacketType getPacketType(DHCP dhcpPacket) {
            DHCPOption optType = dhcpPacket.getOption(OptionCode_MessageType);
            if (optType == null) {
                log.trace("DHCP packet with no message type, ignore it");
                return null;
            }

            DHCPPacketType inPacketType = DHCPPacketType.getType(optType.getData()[0]);
            if (inPacketType == null) {
                log.trace("DHCP packet with no packet type, ignore it");
            }
            return inPacketType;
        }

        private Ethernet buildReply(Ethernet ethRequest, byte packetType,
                                    InstancePort reqInstPort) {
            Port osPort = osNetworkService.port(reqInstPort.portId());
            // pick one IP address to make a reply
            IP fixedIp = osPort.getFixedIps().stream().findFirst().get();
            Subnet osSubnet = osNetworkService.subnet(fixedIp.getSubnetId());

            Ethernet ethReply = new Ethernet();
            ethReply.setSourceMACAddress(dhcpServerMac);
            ethReply.setDestinationMACAddress(ethRequest.getSourceMAC());
            ethReply.setEtherType(Ethernet.TYPE_IPV4);

            IPv4 ipv4Request = (IPv4) ethRequest.getPayload();
            IPv4 ipv4Reply = new IPv4();
            ipv4Reply.setSourceAddress(Ip4Address.valueOf(osSubnet.getGateway()).toInt());
            ipv4Reply.setDestinationAddress(reqInstPort.ipAddress().getIp4Address().toInt());
            ipv4Reply.setTtl(PACKET_TTL);

            UDP udpRequest = (UDP) ipv4Request.getPayload();
            UDP udpReply = new UDP();
            udpReply.setSourcePort((byte) UDP.DHCP_SERVER_PORT);
            udpReply.setDestinationPort((byte) UDP.DHCP_CLIENT_PORT);

            DHCP dhcpRequest = (DHCP) udpRequest.getPayload();
            DHCP dhcpReply = buildDhcpReply(
                    dhcpRequest,
                    packetType,
                    reqInstPort.ipAddress().getIp4Address(),
                    osSubnet);

            udpReply.setPayload(dhcpReply);
            ipv4Reply.setPayload(udpReply);
            ethReply.setPayload(ipv4Reply);

            return ethReply;
        }

        private void sendReply(PacketContext context, Ethernet ethReply) {
            if (ethReply == null) {
                return;
            }
            ConnectPoint srcPoint = context.inPacket().receivedFrom();
            TrafficTreatment treatment = DefaultTrafficTreatment
                    .builder()
                    .setOutput(srcPoint.port())
                    .build();

            packetService.emit(new DefaultOutboundPacket(
                    srcPoint.deviceId(),
                    treatment,
                    ByteBuffer.wrap(ethReply.serialize())));
            context.block();
        }

        private DHCP buildDhcpReply(DHCP request, byte msgType, Ip4Address yourIp,
                                    Subnet osSubnet) {
            Ip4Address gatewayIp = Ip4Address.valueOf(osSubnet.getGateway());
            int subnetPrefixLen = IpPrefix.valueOf(osSubnet.getCidr()).prefixLength();

            DHCP dhcpReply = new DHCP();
            dhcpReply.setOpCode(DHCP.OPCODE_REPLY);
            dhcpReply.setHardwareType(DHCP.HWTYPE_ETHERNET);
            dhcpReply.setHardwareAddressLength((byte) 6);
            dhcpReply.setTransactionId(request.getTransactionId());
            dhcpReply.setFlags(request.getFlags());
            dhcpReply.setYourIPAddress(yourIp.toInt());
            dhcpReply.setServerIPAddress(gatewayIp.toInt());
            dhcpReply.setClientHardwareAddress(request.getClientHardwareAddress());

            List<DHCPOption> options = Lists.newArrayList();
            // message type
            DHCPOption option = new DHCPOption();
            option.setCode(OptionCode_MessageType.getValue());
            option.setLength((byte) 1);
            byte[] optionData = {msgType};
            option.setData(optionData);
            options.add(option);

            // server identifier
            option = new DHCPOption();
            option.setCode(OptionCode_DHCPServerIp.getValue());
            option.setLength((byte) 4);
            option.setData(gatewayIp.toOctets());
            options.add(option);

            // lease time
            option = new DHCPOption();
            option.setCode(OptionCode_LeaseTime.getValue());
            option.setLength((byte) 4);
            option.setData(DHCP_DATA_LEASE_INFINITE);
            options.add(option);

            // subnet mask
            Ip4Address subnetMask = Ip4Address.makeMaskPrefix(subnetPrefixLen);
            option = new DHCPOption();
            option.setCode(OptionCode_SubnetMask.getValue());
            option.setLength((byte) 4);
            option.setData(subnetMask.toOctets());
            options.add(option);

            // broadcast address
            Ip4Address broadcast = Ip4Address.makeMaskedAddress(yourIp, subnetPrefixLen);
            option = new DHCPOption();
            option.setCode(OptionCode_BroadcastAddress.getValue());
            option.setLength((byte) 4);
            option.setData(broadcast.toOctets());
            options.add(option);

            // domain server
            option = new DHCPOption();
            option.setCode(OptionCode_DomainServer.getValue());
            option.setLength((byte) 4);
            option.setData(DEFAULT_DNS.toOctets());
            options.add(option);

            // TODO fix MTU value to be configurable
            option = new DHCPOption();
            option.setCode(DHCP_OPTION_MTU);
            option.setLength((byte) 2);
            option.setData(DHCP_DATA_MTU_DEFAULT);
            options.add(option);

            // router address
            option = new DHCPOption();
            option.setCode(OptionCode_RouterAddress.getValue());
            option.setLength((byte) 4);
            option.setData(gatewayIp.toOctets());
            options.add(option);

            // end option
            option = new DHCPOption();
            option.setCode(OptionCode_END.getValue());
            option.setLength((byte) 1);
            options.add(option);

            dhcpReply.setOptions(options);
            return dhcpReply;
        }
    }
}
