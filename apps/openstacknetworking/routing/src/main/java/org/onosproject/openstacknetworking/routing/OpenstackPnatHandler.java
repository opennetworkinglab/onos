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
package org.onosproject.openstacknetworking.routing;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TCP;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstackinterface.OpenstackInterfaceService;
import org.onosproject.openstackinterface.OpenstackPort;
import org.onosproject.openstackinterface.OpenstackRouter;
import org.onosproject.openstacknetworking.RulePopulatorUtil;
import org.onosproject.openstacknode.OpenstackNodeService;
import org.onosproject.scalablegateway.api.ScalableGatewayService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.Constants.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handle NAT packet processing for managing flow rules in openstack nodes.
 */
@Component(immediate = true)
public class OpenstackPnatHandler {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackInterfaceService openstackService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService nodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ScalableGatewayService gatewayService;

    private static final KryoNamespace.Builder NUMBER_SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API);

    private static final int PNAT_PORT_EXPIRE_TIME = 1200 * 1000;
    private static final int TP_PORT_MINIMUM_NUM = 1024;
    private static final int TP_PORT_MAXIMUM_NUM = 65535;

    private final ExecutorService eventExecutor = newSingleThreadScheduledExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final InternalPacketProcessor packetProcessor = new InternalPacketProcessor();

    private ConsistentMap<Integer, String> tpPortNumMap;
    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(ROUTING_APP_ID);
        tpPortNumMap = storageService.<Integer, String>consistentMapBuilder()
                .withSerializer(Serializer.using(NUMBER_SERIALIZER.build()))
                .withName("openstackrouting-tpportnum")
                .withApplicationId(appId)
                .build();

        packetService.addProcessor(packetProcessor, PacketProcessor.director(1));
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(packetProcessor);
        log.info("Stopped");
    }

    private void processPnatPacket(PacketContext context, Ethernet ethernet) {
        IPv4 iPacket = (IPv4) ethernet.getPayload();
        InboundPacket inboundPacket = context.inPacket();

        int srcPort = getPortNum(ethernet.getSourceMAC(), iPacket.getDestinationAddress());
        OpenstackPort osPort = getOpenstackPort(ethernet.getSourceMAC());
        if (osPort == null) {
            return;
        }
        Ip4Address externalGatewayIp = getExternalGatewayIp(osPort);
        if (externalGatewayIp == null) {
            return;
        }

        populatePnatFlowRules(context.inPacket(),
                osPort,
                TpPort.tpPort(srcPort),
                externalGatewayIp);

        packetOut((Ethernet) ethernet.clone(),
                  inboundPacket.receivedFrom().deviceId(),
                  srcPort,
                  externalGatewayIp);
    }

    private void packetOut(Ethernet ethernet, DeviceId deviceId, int portNum, Ip4Address externalIp) {
        IPv4 iPacket = (IPv4) ethernet.getPayload();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        switch (iPacket.getProtocol()) {
            case IPv4.PROTOCOL_TCP:
                TCP tcpPacket = (TCP) iPacket.getPayload();
                tcpPacket.setSourcePort(portNum);
                tcpPacket.resetChecksum();
                tcpPacket.setParent(iPacket);
                iPacket.setPayload(tcpPacket);
                break;
            case IPv4.PROTOCOL_UDP:
                UDP udpPacket = (UDP) iPacket.getPayload();
                udpPacket.setSourcePort(portNum);
                udpPacket.resetChecksum();
                udpPacket.setParent(iPacket);
                iPacket.setPayload(udpPacket);
                break;
            default:
                log.trace("Temporally, this method can process UDP and TCP protocol.");
                return;
        }

        iPacket.setSourceAddress(externalIp.toString());
        iPacket.resetChecksum();
        iPacket.setParent(ethernet);
        ethernet.setDestinationMACAddress(DEFAULT_EXTERNAL_ROUTER_MAC);
        ethernet.setPayload(iPacket);

        treatment.setOutput(gatewayService.getUplinkPort(deviceId));
        ethernet.resetChecksum();
        packetService.emit(new DefaultOutboundPacket(
                deviceId,
                treatment.build(),
                ByteBuffer.wrap(ethernet.serialize())));
    }

    private int getPortNum(MacAddress sourceMac, int destinationAddress) {
        int portNum = findUnusedPortNum();
        if (portNum == 0) {
            clearPortNumMap();
            portNum = findUnusedPortNum();
        }
        tpPortNumMap.put(portNum, sourceMac.toString().concat(":").concat(String.valueOf(destinationAddress)));
        return portNum;
    }

    private int findUnusedPortNum() {
        for (int i = TP_PORT_MINIMUM_NUM; i < TP_PORT_MAXIMUM_NUM; i++) {
            if (!tpPortNumMap.containsKey(i)) {
                return i;
            }
        }
        return 0;
    }

    private void clearPortNumMap() {
        tpPortNumMap.entrySet().forEach(e -> {
            if (System.currentTimeMillis() - e.getValue().creationTime() > PNAT_PORT_EXPIRE_TIME) {
                tpPortNumMap.remove(e.getKey());
            }
        });
    }

    // TODO there can be multiple routers connected to a particular openstack port
    // TODO cache router information
    private Ip4Address getExternalGatewayIp(OpenstackPort osPort) {
        Optional<OpenstackPort> routerPort = openstackService.ports().stream()
                .filter(p -> p.deviceOwner().equals(DEVICE_OWNER_ROUTER_INTERFACE))
                .filter(p -> checkSameSubnet(p, osPort))
                .findAny();
        if (!routerPort.isPresent()) {
            log.warn("No router is connected to network {}", osPort.networkId());
            return null;
        }

        OpenstackRouter osRouter = openstackService.router(routerPort.get().deviceId());
        if (osRouter == null) {
            log.warn("Failed to get OpenStack router {}",
                     routerPort.get().deviceId());
            return null;
        }

        return osRouter.gatewayExternalInfo().externalFixedIps().values()
                .stream().findAny().orElse(null);
    }

    private OpenstackPort getOpenstackPort(MacAddress srcMac) {
        Optional<Host> host = hostService.getHostsByMac(srcMac).stream()
                .filter(h -> h.annotations().value(PORT_ID) != null)
                .findAny();
        if (!host.isPresent()) {
            log.warn("Failed to find a host with MAC:{}", srcMac);
            return null;
        }
        return openstackService.port(host.get().annotations().value(PORT_ID));
    }

    private boolean checkSameSubnet(OpenstackPort osPortA, OpenstackPort osPortB) {
        return osPortA.fixedIps().keySet().stream()
                .anyMatch(subnetId -> osPortB.fixedIps().keySet().contains(subnetId));
    }

    private void populatePnatFlowRules(InboundPacket inboundPacket,
                                      OpenstackPort osPort,
                                      TpPort patPort,
                                      Ip4Address externalIp) {
        long vni = getVni(osPort.networkId());
        populatePnatIncomingFlowRules(vni, externalIp, patPort, inboundPacket);
        populatePnatOutgoingFlowRules(vni, externalIp, patPort, inboundPacket);
    }

    private long getVni(String netId) {
        // TODO remove this and use host vxlan annotation if applicable
        return Long.parseLong(openstackService.network(netId).segmentId());
    }

    private void populatePnatOutgoingFlowRules(long vni, Ip4Address externalIp, TpPort patPort,
                                               InboundPacket inboundPacket) {
        IPv4 iPacket = (IPv4) inboundPacket.parsed().getPayload();

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(iPacket.getProtocol())
                .matchTunnelId(vni)
                .matchIPSrc(IpPrefix.valueOf(iPacket.getSourceAddress(), 32))
                .matchIPDst(IpPrefix.valueOf(iPacket.getDestinationAddress(), 32));

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        switch (iPacket.getProtocol()) {
            case IPv4.PROTOCOL_TCP:
                TCP tcpPacket = (TCP) iPacket.getPayload();
                sBuilder.matchTcpSrc(TpPort.tpPort(tcpPacket.getSourcePort()))
                        .matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()));
                tBuilder.setTcpSrc(patPort)
                        .setEthDst(DEFAULT_EXTERNAL_ROUTER_MAC);
                break;
            case IPv4.PROTOCOL_UDP:
                UDP udpPacket = (UDP) iPacket.getPayload();
                sBuilder.matchUdpSrc(TpPort.tpPort(udpPacket.getSourcePort()))
                        .matchUdpDst(TpPort.tpPort(udpPacket.getDestinationPort()));
                tBuilder.setUdpSrc(patPort)
                        .setEthDst(DEFAULT_EXTERNAL_ROUTER_MAC);

                break;
            default:
                log.debug("Unsupported IPv4 protocol {}");
                break;
        }

        tBuilder.setIpSrc(externalIp);
        gatewayService.getGatewayNodes().stream().forEach(gateway -> {
            TrafficTreatment.Builder tmpBuilder = DefaultTrafficTreatment.builder(tBuilder.build());
            tmpBuilder.setOutput(gatewayService.getUplinkPort(gateway.getGatewayDeviceId()));
            ForwardingObjective fo = DefaultForwardingObjective.builder()
                    .withSelector(sBuilder.build())
                    .withTreatment(tmpBuilder.build())
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(PNAT_RULE_PRIORITY)
                    .makeTemporary(PNAT_TIMEOUT)
                    .fromApp(appId)
                    .add();

            flowObjectiveService.forward(gateway.getGatewayDeviceId(), fo);
        });
    }

    private void populatePnatIncomingFlowRules(long vni, Ip4Address externalIp, TpPort patPort,
                                               InboundPacket inboundPacket) {
        IPv4 iPacket = (IPv4) inboundPacket.parsed().getPayload();
        IpAddress internalIp = IpAddress.valueOf(iPacket.getSourceAddress());

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(iPacket.getProtocol())
                .matchIPDst(IpPrefix.valueOf(externalIp, 32))
                .matchIPSrc(IpPrefix.valueOf(iPacket.getDestinationAddress(), 32));

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        tBuilder.setTunnelId(vni)
                .setEthDst(inboundPacket.parsed().getSourceMAC())
                .setIpDst(internalIp);

        switch (iPacket.getProtocol()) {
            case IPv4.PROTOCOL_TCP:
                TCP tcpPacket = (TCP) iPacket.getPayload();
                sBuilder.matchTcpSrc(TpPort.tpPort(tcpPacket.getDestinationPort()))
                        .matchTcpDst(patPort);
                tBuilder.setTcpDst(TpPort.tpPort(tcpPacket.getSourcePort()));
                break;
            case IPv4.PROTOCOL_UDP:
                UDP udpPacket = (UDP) iPacket.getPayload();
                sBuilder.matchUdpSrc(TpPort.tpPort(udpPacket.getDestinationPort()))
                        .matchUdpDst(patPort);
                tBuilder.setUdpDst(TpPort.tpPort(udpPacket.getSourcePort()));
                break;
            default:
                break;
        }

        Optional<Host> srcVm = Tools.stream(hostService.getHostsByIp(internalIp))
                .filter(host -> Objects.equals(
                        host.annotations().value(VXLAN_ID),
                        String.valueOf(vni)))
                .findFirst();
        if (!srcVm.isPresent()) {
            log.warn("Failed to find source VM with IP {}", internalIp);
            return;
        }

        gatewayService.getGatewayDeviceIds().stream().forEach(deviceId -> {
            DeviceId srcDeviceId = srcVm.get().location().deviceId();
            TrafficTreatment.Builder tmpBuilder = DefaultTrafficTreatment.builder(tBuilder.build());
            tmpBuilder.extension(RulePopulatorUtil.buildExtension(
                    deviceService,
                    deviceId,
                    nodeService.dataIp(srcDeviceId).get().getIp4Address()), deviceId)
                    .setOutput(nodeService.tunnelPort(deviceId).get());

            ForwardingObjective fo = DefaultForwardingObjective.builder()
                    .withSelector(sBuilder.build())
                    .withTreatment(tmpBuilder.build())
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(PNAT_RULE_PRIORITY)
                    .makeTemporary(PNAT_TIMEOUT)
                    .fromApp(appId)
                    .add();

            flowObjectiveService.forward(deviceId, fo);
        });
    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            } else if (!gatewayService.getGatewayDeviceIds().contains(
                    context.inPacket().receivedFrom().deviceId())) {
                // return if the packet is not from gateway nodes
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethernet = pkt.parsed();
            if (ethernet == null || ethernet.getEtherType() == Ethernet.TYPE_ARP) {
                return;
            }

            IPv4 iPacket = (IPv4) ethernet.getPayload();
            switch (iPacket.getProtocol()) {
                case IPv4.PROTOCOL_ICMP:
                    break;
                case IPv4.PROTOCOL_UDP:
                    UDP udpPacket = (UDP) iPacket.getPayload();
                    if (udpPacket.getDestinationPort() == UDP.DHCP_SERVER_PORT &&
                            udpPacket.getSourcePort() == UDP.DHCP_CLIENT_PORT) {
                        // don't process DHCP
                        break;
                    }
                default:
                    eventExecutor.execute(() -> processPnatPacket(context, ethernet));
                    break;
            }
        }
    }
}