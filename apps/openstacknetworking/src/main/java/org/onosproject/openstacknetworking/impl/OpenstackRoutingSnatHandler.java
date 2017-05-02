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
package org.onosproject.openstacknetworking.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TCP;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknode.OpenstackNodeService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.openstack4j.model.network.ExternalGateway;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.NetworkType;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.Subnet;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handle packets needs SNAT.
 */
@Component(immediate = true)
public class OpenstackRoutingSnatHandler {

    private final Logger log = getLogger(getClass());

    private static final String ERR_PACKETIN = "Failed to handle packet in: ";
    private static final String ERR_UNSUPPORTED_NET_TYPE = "Unsupported network type";
    private static final int TIME_OUT_SNAT_RULE = 120;
    private static final long TIME_OUT_SNAT_PORT_MS = 120 * 1000;
    private static final int TP_PORT_MINIMUM_NUM = 65000;
    private static final int TP_PORT_MAXIMUM_NUM = 65535;

    private static final KryoNamespace.Builder NUMBER_SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackRouterService osRouterService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final InternalPacketProcessor packetProcessor = new InternalPacketProcessor();

    private ConsistentMap<Integer, Long> allocatedPortNumMap;
    private DistributedSet<Integer> unUsedPortNumSet;
    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);

        allocatedPortNumMap = storageService.<Integer, Long>consistentMapBuilder()
                .withSerializer(Serializer.using(NUMBER_SERIALIZER.build()))
                .withName("openstackrouting-allocatedportnummap")
                .withApplicationId(appId)
                .build();

        unUsedPortNumSet = storageService.<Integer>setBuilder()
                .withName("openstackrouting-unusedportnumset")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build()
                .asDistributedSet();

        initializeUnusedPortNumSet();

        packetService.addProcessor(packetProcessor, PacketProcessor.director(1));
        log.info("Started");
    }

    private void initializeUnusedPortNumSet() {
        for (int i = TP_PORT_MINIMUM_NUM; i < TP_PORT_MAXIMUM_NUM; i++) {
            if (!allocatedPortNumMap.containsKey(i)) {
                unUsedPortNumSet.add(i);
            }
        }

        clearPortNumMap();
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(packetProcessor);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    private void processSnatPacket(PacketContext context, Ethernet eth) {
        IPv4 iPacket = (IPv4) eth.getPayload();
        InboundPacket packetIn = context.inPacket();

        int patPort = getPortNum();

        InstancePort srcInstPort = instancePortService.instancePort(eth.getSourceMAC());
        if (srcInstPort == null) {
            log.error(ERR_PACKETIN + "source host(MAC:{}) does not exist",
                    eth.getSourceMAC());
            return;
        }

        IpAddress srcIp = IpAddress.valueOf(iPacket.getSourceAddress());
        Subnet srcSubnet = getSourceSubnet(srcInstPort, srcIp);
        IpAddress externalGatewayIp = getExternalIp(srcSubnet);
        if (externalGatewayIp == null) {
            return;
        }

        populateSnatFlowRules(context.inPacket(),
                srcInstPort,
                TpPort.tpPort(patPort),
                externalGatewayIp);

        packetOut((Ethernet) eth.clone(),
                packetIn.receivedFrom().deviceId(),
                patPort,
                externalGatewayIp);
    }

    private Subnet getSourceSubnet(InstancePort instance, IpAddress srcIp) {
        Port osPort = osNetworkService.port(instance.portId());
        IP fixedIp = osPort.getFixedIps().stream()
                .filter(ip -> IpAddress.valueOf(ip.getIpAddress()).equals(srcIp))
                .findAny().orElse(null);
        if (fixedIp == null) {
            return null;
        }
        return osNetworkService.subnet(fixedIp.getSubnetId());
    }

    private IpAddress getExternalIp(Subnet srcSubnet) {
        RouterInterface osRouterIface = osRouterService.routerInterfaces().stream()
                .filter(i -> Objects.equals(i.getSubnetId(), srcSubnet.getId()))
                .findAny().orElse(null);
        if (osRouterIface == null) {
            // this subnet is not connected to the router
            log.trace(ERR_PACKETIN + "source subnet(ID:{}, CIDR:{}) has no router",
                    srcSubnet.getId(), srcSubnet.getCidr());
            return null;
        }

        Router osRouter = osRouterService.router(osRouterIface.getId());
        if (osRouter.getExternalGatewayInfo() == null) {
            // this router does not have external connectivity
            log.trace(ERR_PACKETIN + "router({}) has no external gateway",
                    osRouter.getName());
            return null;
        }

        ExternalGateway exGatewayInfo = osRouter.getExternalGatewayInfo();
        if (!exGatewayInfo.isEnableSnat()) {
            // SNAT is disabled in this router
            log.trace(ERR_PACKETIN + "router({}) SNAT is disabled", osRouter.getName());
            return null;
        }

        // TODO fix openstack4j for ExternalGateway provides external fixed IP list
        Port exGatewayPort = osNetworkService.ports(exGatewayInfo.getNetworkId())
                .stream()
                .filter(port -> Objects.equals(port.getDeviceId(), osRouter.getId()))
                .findAny().orElse(null);
        if (exGatewayPort == null) {
            log.trace(ERR_PACKETIN + "no external gateway port for router({})",
                    osRouter.getName());
            return null;
        }

        return IpAddress.valueOf(exGatewayPort.getFixedIps().stream()
                .findFirst().get().getIpAddress());
    }

    private void populateSnatFlowRules(InboundPacket packetIn, InstancePort srcInstPort,
                                       TpPort patPort, IpAddress externalIp) {
        Network osNet = osNetworkService.network(srcInstPort.networkId());
        if (osNet == null) {
            final String error = String.format(ERR_PACKETIN + "network %s not found",
                    srcInstPort.networkId());
            throw new IllegalStateException(error);
        }

        setDownstreamRules(srcInstPort,
                osNet.getProviderSegID(),
                osNet.getNetworkType(),
                externalIp,
                patPort,
                packetIn);

        setUpstreamRules(osNet.getProviderSegID(),
                osNet.getNetworkType(),
                externalIp,
                patPort,
                packetIn);
    }

    private void setDownstreamRules(InstancePort srcInstPort, String segmentId, NetworkType networkType,
                                    IpAddress externalIp, TpPort patPort,
                                    InboundPacket packetIn) {
        IPv4 iPacket = (IPv4) packetIn.parsed().getPayload();
        IpAddress internalIp = IpAddress.valueOf(iPacket.getSourceAddress());

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(iPacket.getProtocol())
                .matchIPDst(IpPrefix.valueOf(externalIp, 32))
                .matchIPSrc(IpPrefix.valueOf(iPacket.getDestinationAddress(), 32));

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setEthDst(packetIn.parsed().getSourceMAC())
                .setIpDst(internalIp);

        switch (networkType) {
            case VXLAN:
                tBuilder.setTunnelId(Long.parseLong(segmentId));
                break;
            case VLAN:
                tBuilder.pushVlan()
                        .setVlanId(VlanId.vlanId(segmentId))
                        .setEthSrc(DEFAULT_GATEWAY_MAC);
                break;
            default:
                final String error = String.format(
                        ERR_UNSUPPORTED_NET_TYPE + "%s",
                        networkType.toString());
                throw new IllegalStateException(error);
        }


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

        osNodeService.gatewayDeviceIds().forEach(deviceId -> {
            DeviceId srcDeviceId = srcInstPort.deviceId();
            TrafficTreatment.Builder tmpBuilder =
                    DefaultTrafficTreatment.builder(tBuilder.build());
            switch (networkType) {
                case VXLAN:
                    tmpBuilder.extension(RulePopulatorUtil.buildExtension(
                            deviceService,
                            deviceId,
                            osNodeService.dataIp(srcDeviceId).get().getIp4Address()), deviceId)
                            .setOutput(osNodeService.tunnelPort(deviceId).get());
                    break;
                case VLAN:
                    tmpBuilder.setOutput(osNodeService.vlanPort(deviceId).get());
                    break;
                default:
                    final String error = String.format(
                            ERR_UNSUPPORTED_NET_TYPE + "%s",
                            networkType.toString());
                    throw new IllegalStateException(error);
            }

            ForwardingObjective fo = DefaultForwardingObjective.builder()
                    .withSelector(sBuilder.build())
                    .withTreatment(tmpBuilder.build())
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(PRIORITY_SNAT_RULE)
                    .makeTemporary(TIME_OUT_SNAT_RULE)
                    .fromApp(appId)
                    .add();

            flowObjectiveService.forward(deviceId, fo);
        });
    }

    private void setUpstreamRules(String segmentId, NetworkType networkType, IpAddress externalIp, TpPort patPort,
                                  InboundPacket packetIn) {
        IPv4 iPacket = (IPv4) packetIn.parsed().getPayload();

        TrafficSelector.Builder sBuilder =  DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(iPacket.getProtocol())
                .matchIPSrc(IpPrefix.valueOf(iPacket.getSourceAddress(), 32))
                .matchIPDst(IpPrefix.valueOf(iPacket.getDestinationAddress(), 32));

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        switch (networkType) {
            case VXLAN:
                sBuilder.matchTunnelId(Long.parseLong(segmentId));
                break;
            case VLAN:
                sBuilder.matchVlanId(VlanId.vlanId(segmentId));
                tBuilder.popVlan();
                break;
            default:
                final String error = String.format(
                        ERR_UNSUPPORTED_NET_TYPE + "%s",
                        networkType.toString());
                throw new IllegalStateException(error);
        }

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
        osNodeService.gatewayDeviceIds().forEach(deviceId -> {
            TrafficTreatment.Builder tmpBuilder =
                    DefaultTrafficTreatment.builder(tBuilder.build());
            tmpBuilder.setOutput(osNodeService.externalPort(deviceId).get());
            ForwardingObjective fo = DefaultForwardingObjective.builder()
                    .withSelector(sBuilder.build())
                    .withTreatment(tmpBuilder.build())
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(PRIORITY_SNAT_RULE)
                    .makeTemporary(TIME_OUT_SNAT_RULE)
                    .fromApp(appId)
                    .add();

            flowObjectiveService.forward(deviceId, fo);
        });
    }

    private void packetOut(Ethernet ethPacketIn, DeviceId srcDevice, int patPort,
                           IpAddress externalIp) {
        IPv4 iPacket = (IPv4) ethPacketIn.getPayload();

        switch (iPacket.getProtocol()) {
            case IPv4.PROTOCOL_TCP:
                TCP tcpPacket = (TCP) iPacket.getPayload();
                tcpPacket.setSourcePort(patPort);
                tcpPacket.resetChecksum();
                tcpPacket.setParent(iPacket);
                iPacket.setPayload(tcpPacket);
                break;
            case IPv4.PROTOCOL_UDP:
                UDP udpPacket = (UDP) iPacket.getPayload();
                udpPacket.setSourcePort(patPort);
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
        iPacket.setParent(ethPacketIn);
        ethPacketIn.setDestinationMACAddress(DEFAULT_EXTERNAL_ROUTER_MAC);
        ethPacketIn.setPayload(iPacket);
        ethPacketIn.resetChecksum();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(osNodeService.externalPort(srcDevice).get()).build();

        packetService.emit(new DefaultOutboundPacket(
                srcDevice,
                treatment,
                ByteBuffer.wrap(ethPacketIn.serialize())));
    }

    private int getPortNum() {
        if (unUsedPortNumSet.isEmpty()) {
            clearPortNumMap();
        }

        int portNum = findUnusedPortNum();
        if (portNum != 0) {
            unUsedPortNumSet.remove(portNum);
            allocatedPortNumMap.put(portNum, System.currentTimeMillis());
        }

        return portNum;
    }

    private int findUnusedPortNum() {
        return unUsedPortNumSet.stream().findAny().orElse(0);
    }

    private void clearPortNumMap() {
        allocatedPortNumMap.entrySet().forEach(e -> {
            if (System.currentTimeMillis() - e.getValue().value() > TIME_OUT_SNAT_PORT_MS) {
                allocatedPortNumMap.remove(e.getKey());
                unUsedPortNumSet.add(e.getKey());
            }
        });
    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            } else if (!osNodeService.gatewayDeviceIds().contains(
                    context.inPacket().receivedFrom().deviceId())) {
                // return if the packet is not from gateway nodes
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet eth = pkt.parsed();
            if (eth == null || eth.getEtherType() == Ethernet.TYPE_ARP) {
                return;
            }

            IPv4 iPacket = (IPv4) eth.getPayload();
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
                    eventExecutor.execute(() -> processSnatPacket(context, eth));
                    break;
            }
        }
    }
}
