/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.openstacknetworking.routing;

import com.google.common.collect.Maps;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstackinterface.OpenstackInterfaceService;
import org.onosproject.openstackinterface.OpenstackPort;
import org.onosproject.openstacknetworking.OpenstackNetworkingConfig;
import org.onosproject.openstacknetworking.OpenstackPortInfo;
import org.onosproject.openstacknetworking.OpenstackSwitchingService;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * Handle ICMP packet sent from Openstack Gateway nodes.
 */
public class OpenstackIcmpHandler {
    protected final Logger log = getLogger(getClass());

    private final PacketService packetService;
    private final DeviceService deviceService;
    private final Map<String, OpenstackPortInfo> icmpInfoMap = Maps.newHashMap();
    private final OpenstackSwitchingService openstackSwitchingService;
    private final OpenstackInterfaceService openstackService;
    private final OpenstackNetworkingConfig config;
    private static final MacAddress GATEWAY_MAC = MacAddress.valueOf("1f:1f:1f:1f:1f:1f");
    private static final String NETWORK_ROUTER_INTERFACE = "network:router_interface";
    private static final String PORTNAME = "portName";
    private static final String NETWORK_ROUTER_GATEWAY = "network:router_gateway";
    private static final String NETWORK_FLOATING_IP = "network:floatingip";

    /**
     * Default constructor.
     *
     * @param packetService             packet service
     * @param deviceService             device service
     * @param openstackService          openstackInterface service
     * @param config                    openstackRoutingConfig
     * @param openstackSwitchingService openstackSwitching service
     */
    OpenstackIcmpHandler(PacketService packetService, DeviceService deviceService,
                         OpenstackInterfaceService openstackService, OpenstackNetworkingConfig config,
                         OpenstackSwitchingService openstackSwitchingService) {
        this.packetService = packetService;
        this.deviceService = deviceService;
        this.openstackService = checkNotNull(openstackService);
        this.config = checkNotNull(config);
        this.openstackSwitchingService = checkNotNull(openstackSwitchingService);
    }

    /**
     * Requests ICMP packet.
     *
     * @param appId Application Id
     */
    public void requestPacket(ApplicationId appId) {
        TrafficSelector icmpSelector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_ICMP)
                .build();

        packetService.requestPackets(icmpSelector,
                PacketPriority.CONTROL,
                appId,
                Optional.of(DeviceId.deviceId(config.gatewayBridgeId())));
    }

    /**
     * Handles ICMP packet.
     *
     * @param context  packet context
     * @param ethernet ethernet
     */
    public void processIcmpPacket(PacketContext context, Ethernet ethernet) {
        checkNotNull(context, "context can not be null");
        checkNotNull(ethernet, "ethernet can not be null");

        IPv4 ipPacket = (IPv4) ethernet.getPayload();

        log.debug("icmpEvent called from ip {}, mac {}", Ip4Address.valueOf(ipPacket.getSourceAddress()).toString(),
                ethernet.getSourceMAC().toString());

        ICMP icmp = (ICMP) ipPacket.getPayload();
        short icmpId = getIcmpId(icmp);

        DeviceId deviceId = context.inPacket().receivedFrom().deviceId();
        PortNumber portNumber = context.inPacket().receivedFrom().port();
        if (icmp.getIcmpType() == ICMP.TYPE_ECHO_REQUEST) {
            //TODO: Considers icmp between internal subnets which are belonged to the same router.

            OpenstackPortInfo openstackPortInfo =
                    getOpenstackPortInfo(Ip4Address.valueOf(ipPacket.getSourceAddress()), ethernet.getSourceMAC());

            //checkNotNull(openstackPortInfo, "openstackPortInfo can not be null");
            if (requestToOpenstackRoutingNetwork(ipPacket.getDestinationAddress())) {
                if (openstackPortInfo == null) {
                    if (config.gatewayBridgeId().equals(context.inPacket().receivedFrom().deviceId().toString())) {
                         if (portNumber.equals(getPortForAnnotationPortName(deviceId,
                                        config.gatewayExternalInterfaceName()))) {
                            processIcmpPacketSentToExtenal(ipPacket, icmp, ipPacket.getSourceAddress(),
                                    ethernet.getSourceMAC(), deviceId, portNumber);
                            return;
                        }
                    }
                    return;
                } else {
                    processIcmpPacketSentToGateway(ipPacket, icmp, openstackPortInfo);
                    return;
                }
            }

            if (ipPacket.getDestinationAddress() == openstackPortInfo.gatewayIP().toInt()) {
                processIcmpPacketSentToGateway(ipPacket, icmp, openstackPortInfo);
            } else {
                Ip4Address pNatIpAddress = pNatIpForPort(openstackPortInfo);
                checkNotNull(pNatIpAddress, "pNatIpAddress can not be null");

                sendRequestPacketToExt(ipPacket, icmp, deviceId, pNatIpAddress);

                String icmpInfoKey = String.valueOf(icmpId)
                        .concat(String.valueOf(pNatIpAddress.toInt()))
                        .concat(String.valueOf(ipPacket.getDestinationAddress()));
                icmpInfoMap.putIfAbsent(icmpInfoKey, openstackPortInfo);
            }
        } else if (icmp.getIcmpType() == ICMP.TYPE_ECHO_REPLY) {
            String icmpInfoKey = String.valueOf(icmpId)
                    .concat(String.valueOf(ipPacket.getDestinationAddress()))
                    .concat(String.valueOf(ipPacket.getSourceAddress()));

            processResponsePacketFromExternalToHost(ipPacket, icmp, icmpInfoMap.get(icmpInfoKey));

            icmpInfoMap.remove(icmpInfoKey);
        }
    }

    private void processIcmpPacketSentToExtenal(IPv4 icmpRequestIpv4, ICMP icmpRequest,
                                                int destAddr, MacAddress destMac,
                                                DeviceId deviceId, PortNumber portNumber) {
        icmpRequest.setChecksum((short) 0);
        icmpRequest.setIcmpType(ICMP.TYPE_ECHO_REPLY).resetChecksum();
        icmpRequestIpv4.setSourceAddress(icmpRequestIpv4.getDestinationAddress())
                .setDestinationAddress(destAddr).resetChecksum();
        icmpRequestIpv4.setPayload(icmpRequest);
        Ethernet icmpResponseEth = new Ethernet();
        icmpResponseEth.setEtherType(Ethernet.TYPE_IPV4)
                .setSourceMACAddress(config.gatewayExternalInterfaceMac())
                .setDestinationMACAddress(destMac).setPayload(icmpRequestIpv4);
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(portNumber).build();
        OutboundPacket packet = new DefaultOutboundPacket(deviceId,
                treatment, ByteBuffer.wrap(icmpResponseEth.serialize()));
        packetService.emit(packet);
    }

    private void processIcmpPacketSentToGateway(IPv4 icmpRequestIpv4, ICMP icmpRequest,
                                                OpenstackPortInfo openstackPortInfo) {
        icmpRequest.setChecksum((short) 0);
        icmpRequest.setIcmpType(ICMP.TYPE_ECHO_REPLY)
                .resetChecksum();

        icmpRequestIpv4.setSourceAddress(icmpRequestIpv4.getDestinationAddress())
                .setDestinationAddress(openstackPortInfo.ip().toInt())
                .resetChecksum();

        icmpRequestIpv4.setPayload(icmpRequest);

        Ethernet icmpResponseEth = new Ethernet();

        icmpResponseEth.setEtherType(Ethernet.TYPE_IPV4)
                .setSourceMACAddress(GATEWAY_MAC)
                .setDestinationMACAddress(openstackPortInfo.mac())
                .setPayload(icmpRequestIpv4);

        sendResponsePacketToHost(icmpResponseEth, openstackPortInfo);
    }

    private void sendRequestPacketToExt(IPv4 icmpRequestIpv4, ICMP icmpRequest, DeviceId deviceId,
                                        Ip4Address pNatIpAddress) {
        icmpRequest.resetChecksum();
        icmpRequestIpv4.setSourceAddress(pNatIpAddress.toInt())
                .resetChecksum();
        icmpRequestIpv4.setPayload(icmpRequest);

        Ethernet icmpRequestEth = new Ethernet();

        icmpRequestEth.setEtherType(Ethernet.TYPE_IPV4)
                .setSourceMACAddress(MacAddress.valueOf(config.gatewayExternalInterfaceMac()))
                .setDestinationMACAddress(MacAddress.valueOf(config.physicalRouterMac()))
                .setPayload(icmpRequestIpv4);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(getPortForAnnotationPortName(DeviceId.deviceId(config.gatewayBridgeId()),
                        config.gatewayExternalInterfaceName()))
                .build();

        OutboundPacket packet = new DefaultOutboundPacket(deviceId,
                treatment, ByteBuffer.wrap(icmpRequestEth.serialize()));

        packetService.emit(packet);
    }

    private void processResponsePacketFromExternalToHost(IPv4 icmpResponseIpv4, ICMP icmpResponse,
                                                         OpenstackPortInfo openstackPortInfo) {
        icmpResponse.resetChecksum();

        icmpResponseIpv4.setDestinationAddress(openstackPortInfo.ip().toInt())
                .resetChecksum();
        icmpResponseIpv4.setPayload(icmpResponse);

        Ethernet icmpResponseEth = new Ethernet();

        icmpResponseEth.setEtherType(Ethernet.TYPE_IPV4)
                .setSourceMACAddress(GATEWAY_MAC)
                .setDestinationMACAddress(openstackPortInfo.mac())
                .setPayload(icmpResponseIpv4);

        sendResponsePacketToHost(icmpResponseEth, openstackPortInfo);
    }

    private void sendResponsePacketToHost(Ethernet icmpResponseEth, OpenstackPortInfo openstackPortInfo) {
        Map.Entry<String, OpenstackPortInfo> entry = openstackSwitchingService.openstackPortInfo().entrySet().stream()
                .filter(e -> e.getValue().mac().equals(openstackPortInfo.mac()))
                .findAny().orElse(null);

        if (entry == null) {
            return;
        }

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(getPortForAnnotationPortName(openstackPortInfo.deviceId(), entry.getKey()))
                .build();

        OutboundPacket packet = new DefaultOutboundPacket(openstackPortInfo.deviceId(),
                treatment, ByteBuffer.wrap(icmpResponseEth.serialize()));

        packetService.emit(packet);
    }

    private OpenstackPortInfo getOpenstackPortInfo(Ip4Address sourceIp, MacAddress sourceMac) {
        checkNotNull(openstackSwitchingService.openstackPortInfo(), "openstackportinfo collection can not be null");

        return openstackSwitchingService.openstackPortInfo().values()
                .stream().filter(p -> p.ip().equals(sourceIp) && p.mac().equals(sourceMac))
                .findAny().orElse(null);
    }

    private short getIcmpId(ICMP icmp) {
        return ByteBuffer.wrap(icmp.serialize(), 4, 2).getShort();
    }

    private Ip4Address pNatIpForPort(OpenstackPortInfo openstackPortInfo) {

        OpenstackPort openstackPort = openstackService.ports().stream()
                .filter(p -> p.deviceOwner().equals(NETWORK_ROUTER_INTERFACE) &&
                        p.networkId().equals(openstackPortInfo.networkId()))
                .findAny().orElse(null);

        checkNotNull(openstackPort, "openstackPort can not be null");

        return openstackService.router(openstackPort.deviceId())
                .gatewayExternalInfo().externalFixedIps().values()
                .stream().findAny().orElse(null);
    }

    private PortNumber getPortForAnnotationPortName(DeviceId deviceId, String match) {
        Port port = deviceService.getPorts(deviceId).stream()
                .filter(p -> p.annotations().value(PORTNAME).equals(match))
                .findAny().orElse(null);

        checkNotNull(port, "port cannot be null");

        return port.number();
    }

    private boolean requestToOpenstackRoutingNetwork(int destAddr) {
        OpenstackPort port = openstackService.ports().stream()
                .filter(p -> p.deviceOwner().equals(NETWORK_ROUTER_GATEWAY) ||
                        p.deviceOwner().equals(NETWORK_FLOATING_IP))
                .filter(p -> p.fixedIps().containsValue(
                        Ip4Address.valueOf(destAddr)))
                .findAny().orElse(null);
        if (port == null) {
            return false;
        }
        return true;
    }
}