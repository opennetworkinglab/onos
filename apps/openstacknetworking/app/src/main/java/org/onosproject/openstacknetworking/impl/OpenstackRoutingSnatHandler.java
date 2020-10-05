/*
 * Copyright 2016-present Open Networking Foundation
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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TCP;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.ExternalPeerRouter;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortEvent;
import org.onosproject.openstacknetworking.api.InstancePortListener;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;
import org.onosproject.openstacknetworking.api.OpenstackNetwork.Type;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkEvent;
import org.onosproject.openstacknetworking.api.OpenstackNetworkListener;
import org.onosproject.openstacknetworking.api.OpenstackRouterEvent;
import org.onosproject.openstacknetworking.api.OpenstackRouterListener;
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.onosproject.openstacknetworking.util.RulePopulatorUtil;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.openstack4j.model.network.ExternalGateway;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.Subnet;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.DEFAULT_GATEWAY_MAC;
import static org.onosproject.openstacknetworking.api.Constants.GW_COMMON_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_EXTERNAL_ROUTING_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_SNAT_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_STATEFUL_SNAT_RULE;
import static org.onosproject.openstacknetworking.api.Constants.ROUTING_TABLE;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.FLAT;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.VLAN;
import static org.onosproject.openstacknetworking.impl.OsgiPropertyConstants.USE_STATEFUL_SNAT;
import static org.onosproject.openstacknetworking.impl.OsgiPropertyConstants.USE_STATEFUL_SNAT_DEFAULT;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.deriveResourceName;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.externalGatewayIpSnatEnabled;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.externalPeerRouterFromSubnet;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getPropertyValueAsBoolean;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getRouterFromSubnet;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.tunnelPortNumByNetType;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.CT_NAT_SRC_FLAG;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.buildExtension;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.GATEWAY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handle packets needs SNAT.
 */
@Component(
    immediate = true,
    property = {
            USE_STATEFUL_SNAT + ":Boolean=" + USE_STATEFUL_SNAT_DEFAULT
    }
)
public class OpenstackRoutingSnatHandler {

    private final Logger log = getLogger(getClass());

    private static final String ERR_PACKET_IN = "Failed to handle packet in: ";
    private static final String ERR_UNSUPPORTED_NET_TYPE = "Unsupported network type";
    private static final long TIME_OUT_SNAT_PORT_MS = 120L * 1000L;
    private static final int TP_PORT_MINIMUM_NUM = 1025;
    private static final int TP_PORT_MAXIMUM_NUM = 65535;
    private static final int VM_PREFIX = 32;
    private static final String DEVICE_OWNER_ROUTER_GW = "network:router_gateway";

    private static final String MSG_ENABLED = "Enabled ";
    private static final String MSG_DISABLED = "Disabled ";

    /** Use Stateful SNAT for source NATing. */
    private boolean useStatefulSnat = USE_STATEFUL_SNAT_DEFAULT;

    private static final KryoNamespace.Builder NUMBER_SERIALIZER =
            KryoNamespace.newBuilder()
            .register(KryoNamespaces.API);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNetworkAdminService osNetworkAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackRouterService osRouterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackFlowRuleService osFlowRuleService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final PacketProcessor packetProcessor = new InternalPacketProcessor();
    private final InstancePortListener instancePortListener = new InternalInstancePortListener();
    private final OpenstackRouterListener osRouterListener = new InternalRouterEventListener();
    private final OpenstackNodeListener osNodeListener = new InternalNodeEventListener();
    private final OpenstackNetworkListener osNetworkListener = new InternalNetworkEventListener();

    private ConsistentMap<Integer, Long> allocatedPortNumMap;
    private DistributedSet<Integer> unUsedPortNumSet;
    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);

        allocatedPortNumMap = storageService.<Integer, Long>consistentMapBuilder()
                .withSerializer(Serializer.using(NUMBER_SERIALIZER.build()))
                .withName("openstackrouting-allocated-portnummap")
                .withApplicationId(appId)
                .build();

        unUsedPortNumSet = storageService.<Integer>setBuilder()
                .withName("openstackrouting-unused-portnumset")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build()
                .asDistributedSet();

        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        packetService.addProcessor(packetProcessor, PacketProcessor.director(1));

        configService.registerProperties(getClass());
        instancePortService.addListener(instancePortListener);
        osRouterService.addListener(osRouterListener);
        osNodeService.addListener(osNodeListener);
        osNetworkAdminService.addListener(osNetworkListener);

        eventExecutor.execute(this::initializeUnusedPortNumSet);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        osNetworkAdminService.removeListener(osNetworkListener);
        osRouterService.removeListener(osRouterListener);
        osNodeService.removeListener(osNodeListener);
        instancePortService.removeListener(instancePortListener);
        configService.unregisterProperties(getClass(), false);
        packetService.removeProcessor(packetProcessor);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Boolean flag;

        flag = Tools.isPropertyEnabled(properties, USE_STATEFUL_SNAT);
        if (flag == null) {
            log.info("useStatefulSnat is not configured, " +
                    "using current value of {}", useStatefulSnat);
        } else {
            useStatefulSnat = flag;
            log.info("Configured. useStatefulSnat is {}",
                    useStatefulSnat ? "enabled" : "disabled");
        }

        resetSnatRules();
    }

    private void processSnatPacket(PacketContext context, Ethernet eth) {

        if (getStatefulSnatFlag()) {
            return;
        }

        IPv4 iPacket = (IPv4) eth.getPayload();
        InboundPacket packetIn = context.inPacket();

        int patPort = getPortNum();

        InstancePort srcInstPort = instancePortService.instancePort(eth.getSourceMAC());
        if (srcInstPort == null) {
            log.error(ERR_PACKET_IN + "source host(MAC:{}) does not exist",
                    eth.getSourceMAC());
            return;
        }

        IpAddress srcIp = IpAddress.valueOf(iPacket.getSourceAddress());
        Subnet srcSubnet = getSourceSubnet(srcInstPort, srcIp);

        Router osRouter = getRouterFromSubnet(srcSubnet, osRouterService);

        if (osRouter == null || osRouter.getExternalGatewayInfo() == null) {
            // this router does not have external connectivity
            log.warn("No router is associated with the given subnet {}", srcSubnet);
            return;
        }

        IpAddress externalGatewayIp =
                externalGatewayIpSnatEnabled(osRouter, osNetworkAdminService);

        if (externalGatewayIp == null) {
            return;
        }

        ExternalPeerRouter externalPeerRouter = externalPeerRouterFromSubnet(
                srcSubnet, osRouterService, osNetworkService);
        if (externalPeerRouter == null) {
            return;
        }

        populateSnatFlowRules(context.inPacket(),
                srcInstPort,
                TpPort.tpPort(patPort),
                externalGatewayIp, externalPeerRouter);


        packetOut(eth.duplicate(),
                packetIn.receivedFrom().deviceId(),
                patPort,
                externalGatewayIp, externalPeerRouter);
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

    private void populateSnatFlowRules(InboundPacket packetIn,
                                       InstancePort srcInstPort,
                                       TpPort patPort, IpAddress externalIp,
                                       ExternalPeerRouter externalPeerRouter) {
        Network osNet = osNetworkService.network(srcInstPort.networkId());
        Type netType = osNetworkService.networkType(srcInstPort.networkId());

        if (osNet == null) {
            final String error = String.format("%s network %s not found",
                    ERR_PACKET_IN, srcInstPort.networkId());
            throw new IllegalStateException(error);
        }

        setStatelessSnatDownstreamRules(srcInstPort,
                osNet.getProviderSegID(),
                netType,
                externalIp,
                externalPeerRouter,
                patPort,
                packetIn);

        setStatelessSnatUpstreamRules(osNet.getProviderSegID(),
                netType,
                externalIp,
                externalPeerRouter,
                patPort,
                packetIn);
    }

    private void setStatelessSnatDownstreamRules(InstancePort srcInstPort,
                                                 String segmentId,
                                                 Type networkType,
                                                 IpAddress externalIp,
                                                 ExternalPeerRouter externalPeerRouter,
                                                 TpPort patPort,
                                                 InboundPacket packetIn) {
        IPv4 iPacket = (IPv4) packetIn.parsed().getPayload();
        IpAddress internalIp = IpAddress.valueOf(iPacket.getSourceAddress());

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(iPacket.getProtocol())
                .matchIPDst(IpPrefix.valueOf(externalIp.getIp4Address(), VM_PREFIX))
                .matchIPSrc(IpPrefix.valueOf(iPacket.getDestinationAddress(), VM_PREFIX));

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setEthDst(packetIn.parsed().getSourceMAC())
                .setIpDst(internalIp);

        if (!externalPeerRouter.vlanId().equals(VlanId.NONE)) {
            sBuilder.matchVlanId(externalPeerRouter.vlanId());
            tBuilder.popVlan();
        }

        switch (networkType) {
            case VXLAN:
            case GRE:
            case GENEVE:
                tBuilder.setTunnelId(Long.parseLong(segmentId));
                break;
            case VLAN:
                tBuilder.pushVlan()
                        .setVlanId(VlanId.vlanId(segmentId));
                break;
            default:
                final String error = String.format("%s %s",
                        ERR_UNSUPPORTED_NET_TYPE, networkType.toString());
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

        OpenstackNode srcNode = osNodeService.node(srcInstPort.deviceId());
        osNodeService.completeNodes(GATEWAY).forEach(gNode -> {
            TrafficTreatment treatment =
                    getDownstreamTreatment(networkType, tBuilder, gNode, srcNode);
            osFlowRuleService.setRule(
                    appId,
                    gNode.intgBridge(),
                    sBuilder.build(),
                    treatment,
                    PRIORITY_SNAT_RULE,
                    GW_COMMON_TABLE,
                    true);
        });
    }

    private TrafficTreatment getDownstreamTreatment(Type networkType,
                                                    TrafficTreatment.Builder tBuilder,
                                                    OpenstackNode gNode,
                                                    OpenstackNode srcNode) {
        TrafficTreatment.Builder tmpBuilder =
                DefaultTrafficTreatment.builder(tBuilder.build());
        switch (networkType) {
            case VXLAN:
            case GRE:
            case GENEVE:
                PortNumber portNum = tunnelPortNumByNetType(networkType, gNode);
                tmpBuilder.extension(RulePopulatorUtil.buildExtension(
                        deviceService,
                        gNode.intgBridge(),
                        srcNode.dataIp().getIp4Address()), gNode.intgBridge())
                        .setOutput(portNum);
                break;
            case VLAN:
                tmpBuilder.setOutput(gNode.vlanPortNum());
                break;
            default:
                final String error = String.format("%s %s",
                        ERR_UNSUPPORTED_NET_TYPE, networkType.toString());
                throw new IllegalStateException(error);
        }

        return tmpBuilder.build();
    }

    private void setStatelessSnatUpstreamRules(String segmentId,
                                               Type networkType,
                                               IpAddress externalIp,
                                               ExternalPeerRouter externalPeerRouter,
                                               TpPort patPort,
                                               InboundPacket packetIn) {
        IPv4 iPacket = (IPv4) packetIn.parsed().getPayload();

        TrafficSelector.Builder sBuilder =  DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(iPacket.getProtocol())
                .matchIPSrc(IpPrefix.valueOf(iPacket.getSourceAddress(), VM_PREFIX))
                .matchIPDst(IpPrefix.valueOf(iPacket.getDestinationAddress(), VM_PREFIX));

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        switch (networkType) {
            case VXLAN:
            case GRE:
            case GENEVE:
                sBuilder.matchTunnelId(Long.parseLong(segmentId));
                break;
            case VLAN:
                sBuilder.matchVlanId(VlanId.vlanId(segmentId));
                tBuilder.popVlan();
                break;
            default:
                final String error = String.format("%s %s",
                        ERR_UNSUPPORTED_NET_TYPE, networkType.toString());
                throw new IllegalStateException(error);
        }

        switch (iPacket.getProtocol()) {
            case IPv4.PROTOCOL_TCP:
                TCP tcpPacket = (TCP) iPacket.getPayload();
                sBuilder.matchTcpSrc(TpPort.tpPort(tcpPacket.getSourcePort()))
                        .matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()));
                tBuilder.setTcpSrc(patPort).setEthDst(externalPeerRouter.macAddress());
                break;
            case IPv4.PROTOCOL_UDP:
                UDP udpPacket = (UDP) iPacket.getPayload();
                sBuilder.matchUdpSrc(TpPort.tpPort(udpPacket.getSourcePort()))
                        .matchUdpDst(TpPort.tpPort(udpPacket.getDestinationPort()));
                tBuilder.setUdpSrc(patPort).setEthDst(externalPeerRouter.macAddress());
                break;
            default:
                log.debug("Unsupported IPv4 protocol {}");
                break;
        }

        if (!externalPeerRouter.vlanId().equals(VlanId.NONE)) {
            tBuilder.pushVlan().setVlanId(externalPeerRouter.vlanId());
        }

        tBuilder.setIpSrc(externalIp);
        osNodeService.completeNodes(GATEWAY).forEach(gNode -> {
            TrafficTreatment.Builder tmpBuilder =
                    DefaultTrafficTreatment.builder(tBuilder.build());
            tmpBuilder.setOutput(gNode.uplinkPortNum());

            osFlowRuleService.setRule(
                    appId,
                    gNode.intgBridge(),
                    sBuilder.build(),
                    tmpBuilder.build(),
                    PRIORITY_SNAT_RULE,
                    GW_COMMON_TABLE,
                    true);
        });
    }

    private void packetOut(Ethernet ethPacketIn, DeviceId srcDevice, int patPort,
                           IpAddress externalIp, ExternalPeerRouter externalPeerRouter) {
        IPv4 iPacket = (IPv4) ethPacketIn.getPayload();
        switch (iPacket.getProtocol()) {
            case IPv4.PROTOCOL_TCP:
                iPacket.setPayload(buildPacketOutTcp(iPacket, patPort));
                break;
            case IPv4.PROTOCOL_UDP:
                iPacket.setPayload(buildPacketOutUdp(iPacket, patPort));
                break;
            default:
                log.trace("Temporally, this method can process UDP and TCP protocol.");
                return;
        }

        iPacket.setSourceAddress(externalIp.toString());
        iPacket.resetChecksum();
        iPacket.setParent(ethPacketIn);
        ethPacketIn.setSourceMACAddress(DEFAULT_GATEWAY_MAC);
        ethPacketIn.setDestinationMACAddress(externalPeerRouter.macAddress());
        ethPacketIn.setPayload(iPacket);

        if (!externalPeerRouter.vlanId().equals(VlanId.NONE)) {
            ethPacketIn.setVlanID(externalPeerRouter.vlanId().toShort());
        }

        ethPacketIn.resetChecksum();

        OpenstackNode srcNode = osNodeService.node(srcDevice);
        if (srcNode == null) {
            final String error = String.format("Cannot find openstack node for %s",
                    srcDevice);
            throw new IllegalStateException(error);
        }

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        packetService.emit(new DefaultOutboundPacket(
                srcDevice,
                tBuilder.setOutput(srcNode.uplinkPortNum()).build(),
                ByteBuffer.wrap(ethPacketIn.serialize())));
    }

    private TCP buildPacketOutTcp(IPv4 iPacket, int patPort) {
        TCP tcpPacket = (TCP) iPacket.getPayload();
        tcpPacket.setSourcePort(patPort);
        tcpPacket.resetChecksum();
        tcpPacket.setParent(iPacket);

        return tcpPacket;
    }

    private UDP buildPacketOutUdp(IPv4 iPacket, int patPort) {
        UDP udpPacket = (UDP) iPacket.getPayload();
        udpPacket.setSourcePort(patPort);
        udpPacket.resetChecksum();
        udpPacket.setParent(iPacket);

        return udpPacket;
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
            if (System.currentTimeMillis() -
                    e.getValue().value() > TIME_OUT_SNAT_PORT_MS) {
                allocatedPortNumMap.remove(e.getKey());
                unUsedPortNumSet.add(e.getKey());
            }
        });
    }

    private void initializeUnusedPortNumSet() {
        for (int i = TP_PORT_MINIMUM_NUM; i < TP_PORT_MAXIMUM_NUM; i++) {
            if (!allocatedPortNumMap.containsKey(i)) {
                unUsedPortNumSet.add(i);
            }
        }

        clearPortNumMap();
    }

    private void resetSnatRules() {
        if (getStatefulSnatFlag()) {
            osRouterService.routerInterfaces().forEach(
                    routerIface -> {
                        setReactiveSnatRules(routerIface, false);
                        setStatefulSnatRules(routerIface, true);
                    }
            );
        } else {
            osRouterService.routerInterfaces().forEach(
                    routerIface -> {
                        setStatefulSnatRules(routerIface, false);
                        setReactiveSnatRules(routerIface, true);
                    }
            );
        }
    }

    private void setRulesToGateway(OpenstackNode osNode,
                                   String segmentId,
                                   IpPrefix srcSubnet,
                                   Type networkType,
                                   boolean install) {
        OpenstackNode sourceNatGateway = osNodeService.completeNodes(GATEWAY)
                .stream().findFirst().orElse(null);

        if (sourceNatGateway == null) {
            return;
        }

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(srcSubnet.getIp4Prefix())
                .matchEthDst(Constants.DEFAULT_GATEWAY_MAC);

        switch (networkType) {
            case VXLAN:
            case GRE:
            case GENEVE:
                sBuilder.matchTunnelId(Long.parseLong(segmentId));
                break;
            case VLAN:
                sBuilder.matchVlanId(VlanId.vlanId(segmentId));
                break;
            default:
                final String error = String.format("%s %s",
                        ERR_UNSUPPORTED_NET_TYPE,
                        networkType.toString());
                throw new IllegalStateException(error);
        }

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        switch (networkType) {
            case VXLAN:
            case GRE:
            case GENEVE:
                PortNumber portNum = tunnelPortNumByNetType(networkType, osNode);
                tBuilder.extension(buildExtension(
                        deviceService,
                        osNode.intgBridge(),
                        sourceNatGateway.dataIp().getIp4Address()),
                        osNode.intgBridge())
                        .setOutput(portNum);
                break;

            case VLAN:
                tBuilder.setOutput(osNode.vlanPortNum());
                break;

            default:
                break;
        }

        osFlowRuleService.setRule(
                appId,
                osNode.intgBridge(),
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_EXTERNAL_ROUTING_RULE,
                ROUTING_TABLE,
                install);
    }


    private void routerUpdated(Router osRouter) {
        ExternalGateway exGateway = osRouter.getExternalGatewayInfo();

        ExternalPeerRouter externalPeerRouter =
                osNetworkAdminService.externalPeerRouter(exGateway);
        VlanId vlanId = externalPeerRouter == null ?
                                    VlanId.NONE : externalPeerRouter.vlanId();

        if (exGateway == null) {
            deleteUnassociatedExternalPeerRouter();
            osRouterService.routerInterfaces(osRouter.getId()).forEach(iface ->
                    setSourceNat(iface, false));
        } else {
            osNetworkAdminService.deriveExternalPeerRouterMac(exGateway, osRouter, vlanId);
            osRouterService.routerInterfaces(osRouter.getId()).forEach(iface ->
                    setSourceNat(iface, exGateway.isEnableSnat()));
        }
    }

    private void deleteUnassociatedExternalPeerRouter() {
        log.trace("Deleting unassociated external peer router");

        try {
            Set<String> routerIps = Sets.newConcurrentHashSet();

            osRouterService.routers().stream()
                    .filter(router -> getGatewayIpAddress(router) != null)
                    .map(router -> getGatewayIpAddress(router).toString())
                    .forEach(routerIps::add);

            osNetworkAdminService.externalPeerRouters().stream()
                    .filter(externalPeerRouter ->
                            !routerIps.contains(externalPeerRouter.ipAddress().toString()))
                    .forEach(externalPeerRouter -> {
                        osNetworkAdminService
                                .deleteExternalPeerRouter(
                                        externalPeerRouter.ipAddress().toString());
                        log.trace("Deleted unassociated external peer router {}",
                                externalPeerRouter.ipAddress().toString());
                    });
        } catch (Exception e) {
            log.error("Exception occurred because of {}", e.toString());
        }
    }

    private void routerIfaceAdded(Router osRouter, RouterInterface osRouterIface) {
        ExternalGateway exGateway = osRouter.getExternalGatewayInfo();
        if (exGateway != null && exGateway.isEnableSnat()) {
            setSourceNat(osRouterIface, true);
        }
    }

    private void routerIfaceRemoved(Router osRouter, RouterInterface osRouterIface) {
        ExternalGateway exGateway = osRouter.getExternalGatewayInfo();
        if (exGateway != null && exGateway.isEnableSnat()) {
            setSourceNat(osRouterIface, false);
        }
    }

    private void setSourceNat(RouterInterface routerIface, boolean install) {
        Subnet osSubnet = osNetworkAdminService.subnet(routerIface.getSubnetId());
        Network osNet = osNetworkAdminService.network(osSubnet.getNetworkId());
        Type netType = osNetworkAdminService.networkType(osSubnet.getNetworkId());

        osNodeService.completeNodes(COMPUTE).forEach(cNode -> {
            setRulesToGateway(cNode, osNet.getProviderSegID(),
                    IpPrefix.valueOf(osSubnet.getCidr()), netType, install);
        });

        if (getStatefulSnatFlag()) {
            setStatefulSnatRules(routerIface, install);
        } else {
            setReactiveSnatRules(routerIface, install);
        }

        final String updateStr = install ? MSG_ENABLED : MSG_DISABLED;
        log.info(updateStr + "external access for subnet({})", osSubnet.getCidr());
    }

    private void setStatefulSnatRules(RouterInterface routerIface, boolean install) {
        Subnet osSubnet = osNetworkAdminService.subnet(routerIface.getSubnetId());
        Network osNet = osNetworkAdminService.network(osSubnet.getNetworkId());
        Type netType = osNetworkAdminService.networkType(osSubnet.getNetworkId());

        if (netType == FLAT) {
            log.warn("FLAT typed network does not need SNAT rules");
            return;
        }

        Optional<Router> osRouter = osRouterService.routers().stream()
                .filter(router -> routerIface.getId().equals(router.getId()))
                .findAny();

        if (!osRouter.isPresent()) {
            log.warn("Cannot find a router attached with the given router interface {} ", routerIface);
            return;
        }

        IpAddress natAddress = externalGatewayIpSnatEnabled(osRouter.get(), osNetworkAdminService);
        if (natAddress == null) {
            log.debug("NAT address is not found");
            return;
        }

        IpAddress extRouterAddress = getGatewayIpAddress(osRouter.get());
        if (extRouterAddress == null) {
            log.warn("External router address is not found");
            return;
        }

        ExternalPeerRouter externalPeerRouter =
                osNetworkService.externalPeerRouter(extRouterAddress);
        if (externalPeerRouter == null) {
            log.warn("External peer router not found");
            return;
        }

        Map<OpenstackNode, PortRange> gwPortRangeMap = getAssignedPortsForGateway(
                ImmutableList.copyOf(osNodeService.nodes(GATEWAY)));

        osNodeService.completeNodes(GATEWAY).forEach(gwNode -> {
            if (install) {
                PortRange gwPortRange = gwPortRangeMap.get(gwNode);

                Map<String, PortRange> netPortRangeMap =
                        getAssignedPortsForNet(getNetIdByRouterId(routerIface.getId()),
                                gwPortRange.min(), gwPortRange.max());

                PortRange netPortRange = netPortRangeMap.get(osNet.getId());

                setStatefulSnatUpstreamRule(gwNode, natAddress,
                        Long.parseLong(osNet.getProviderSegID()),
                        externalPeerRouter, netPortRange.min(),
                        netPortRange.max(), install);
            } else {
                setStatefulSnatUpstreamRule(gwNode, natAddress,
                        Long.parseLong(osNet.getProviderSegID()),
                        externalPeerRouter, 0, 0, install);
            }
        });
    }

    private void setStatefulDownstreamRules(Router osRouter, boolean install) {

        if (!getStatefulSnatFlag()) {
            return;
        }

        IpAddress natAddress = externalGatewayIpSnatEnabled(osRouter, osNetworkAdminService);
        if (natAddress == null) {
            return;
        }

        setStatefulDownstreamRules(natAddress, install);
    }

    private void setStatefulDownstreamRules(IpAddress natAddress, boolean install) {
        osNodeService.completeNodes(GATEWAY)
                .forEach(gwNode -> {
                    setStatefulSnatDownstreamRule(gwNode.intgBridge(),
                            IpPrefix.valueOf(natAddress, VM_PREFIX), install);
                });
    }

    private List<String> getNetIdByRouterId(String routerId) {
        return osRouterService.routerInterfaces(routerId)
                .stream()
                .filter(ri -> osRouterService.router(ri.getId())
                                .getExternalGatewayInfo().isEnableSnat())
                .map(RouterInterface::getSubnetId)
                .map(si -> osNetworkAdminService.subnet(si))
                .map(Subnet::getNetworkId)
                .collect(Collectors.toList());
    }

    private Map<OpenstackNode, PortRange>
                        getAssignedPortsForGateway(List<OpenstackNode> gateways) {

        Map<OpenstackNode, PortRange> gwPortRangeMap = Maps.newConcurrentMap();

        int portRangeNumPerGwNode =
                (TP_PORT_MAXIMUM_NUM - TP_PORT_MINIMUM_NUM + 1) / gateways.size();

        for (int i = 0; i < gateways.size(); i++) {
            int gwPortRangeMin = TP_PORT_MINIMUM_NUM + i * portRangeNumPerGwNode;
            int gwPortRangeMax = TP_PORT_MINIMUM_NUM + (i + 1) * portRangeNumPerGwNode - 1;

            gwPortRangeMap.put(gateways.get(i),
                    new PortRange(gwPortRangeMin, gwPortRangeMax));
        }

        return gwPortRangeMap;
    }

    private Map<String, PortRange> getAssignedPortsForNet(List<String> netIds,
                                                          int min, int max) {

        Map<String, PortRange> netPortRangeMap = Maps.newConcurrentMap();

        int portRangeNumPerNet = (max - min + 1) / netIds.size();

        for (int i = 0; i < netIds.size(); i++) {
            int netPortRangeMin = min + i * portRangeNumPerNet;
            int netPortRangeMax = min + (i + 1) * portRangeNumPerNet - 1;

            netPortRangeMap.put(netIds.get(i),
                    new PortRange(netPortRangeMin, netPortRangeMax));
        }

        return netPortRangeMap;
    }

    private IpAddress getGatewayIpAddress(Router osRouter) {

        if (osRouter.getExternalGatewayInfo() == null) {
            return null;
        }
        String extNetId = osNetworkAdminService.network(
                osRouter.getExternalGatewayInfo().getNetworkId()).getId();
        Optional<Subnet> extSubnet = osNetworkAdminService.subnets().stream()
                .filter(subnet -> subnet.getNetworkId().equals(extNetId))
                .findAny();

        if (!extSubnet.isPresent()) {
            log.error("Cannot find external subnet for the router");
            return null;
        }

        return IpAddress.valueOf(extSubnet.get().getGateway());
    }

    private void setReactiveSnatRules(RouterInterface routerIface, boolean install) {
        Subnet osSubnet = osNetworkAdminService.subnet(routerIface.getSubnetId());
        Network osNet = osNetworkAdminService.network(osSubnet.getNetworkId());
        Type netType = osNetworkAdminService.networkType(osSubnet.getNetworkId());

        osNodeService.completeNodes(GATEWAY)
                .forEach(gwNode -> setRulesToController(
                        gwNode.intgBridge(),
                        osNet.getProviderSegID(),
                        IpPrefix.valueOf(osSubnet.getCidr()),
                        netType,
                        install));
    }

    private void setGatewayToInstanceDownstreamRule(OpenstackNode gwNode,
                                                    InstancePort instPort,
                                                    boolean install) {

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(IpPrefix.valueOf(instPort.ipAddress(), VM_PREFIX))
                .build();

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setEthDst(instPort.macAddress());

        Type netType = osNetworkAdminService.networkType(instPort.networkId());
        String segId = osNetworkAdminService.segmentId(instPort.networkId());

        switch (netType) {
            case VXLAN:
            case GRE:
            case GENEVE:
                tBuilder.setTunnelId(Long.valueOf(segId));
                break;
            case VLAN:
            default:
                final String error = String.format("%s %s",
                        ERR_UNSUPPORTED_NET_TYPE, netType.name());
                throw new IllegalStateException(error);
        }

        OpenstackNode srcNode = osNodeService.node(instPort.deviceId());
        TrafficTreatment treatment =
                    getDownstreamTreatment(netType, tBuilder, gwNode, srcNode);

        osFlowRuleService.setRule(
                appId,
                gwNode.intgBridge(),
                selector,
                treatment,
                PRIORITY_STATEFUL_SNAT_RULE,
                GW_COMMON_TABLE,
                install);
    }

    private void setStatefulSnatDownstreamRule(DeviceId deviceId,
                                               IpPrefix gatewayIp,
                                               boolean install) {

        Set<TrafficSelector> selectors = Sets.newConcurrentHashSet();

        ImmutableSet<Byte> ipv4Proto = ImmutableSet.of(IPv4.PROTOCOL_TCP, IPv4.PROTOCOL_UDP);

        ipv4Proto.forEach(proto -> {
            TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
            sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPDst(gatewayIp)
                    .matchIPProtocol(proto);
            selectors.add(sBuilder.build());
        });


        ExtensionTreatment natTreatment = RulePopulatorUtil
                .niciraConnTrackTreatmentBuilder(driverService, deviceId)
                .commit(false)
                .natAction(true)
                .table((short) 0)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .extension(natTreatment, deviceId)
                .build();

        selectors.forEach(s -> {
            osFlowRuleService.setRule(
                    appId,
                    deviceId,
                    s,
                    treatment,
                    PRIORITY_STATEFUL_SNAT_RULE,
                    GW_COMMON_TABLE,
                    install);
        });
    }

    private void setStatefulSnatUpstreamRule(OpenstackNode gwNode,
                                             IpAddress gatewayIp,
                                             long vni,
                                             ExternalPeerRouter extPeerRouter,
                                             int minPortNum,
                                             int maxPortNum,
                                             boolean install) {

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchEthDst(DEFAULT_GATEWAY_MAC)
                .matchTunnelId(vni)
                .build();

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        // we do not consider to much like port range on removing the rules...
        if (install) {
            ExtensionTreatment natTreatment = RulePopulatorUtil
                    .niciraConnTrackTreatmentBuilder(driverService, gwNode.intgBridge())
                    .commit(true)
                    .natFlag(CT_NAT_SRC_FLAG)
                    .natAction(true)
                    .natIp(gatewayIp)
                    .natPortMin(TpPort.tpPort(minPortNum))
                    .natPortMax(TpPort.tpPort(maxPortNum))
                    .build();

            tBuilder.extension(natTreatment, gwNode.intgBridge())
                    .setEthDst(extPeerRouter.macAddress())
                    .setEthSrc(DEFAULT_GATEWAY_MAC)
                    .setOutput(gwNode.uplinkPortNum());
        }

        osFlowRuleService.setRule(
                appId,
                gwNode.intgBridge(),
                selector,
                tBuilder.build(),
                PRIORITY_STATEFUL_SNAT_RULE,
                GW_COMMON_TABLE,
                install);
    }

    private void setRulesToController(DeviceId deviceId,
                                      String segmentId,
                                      IpPrefix srcSubnet,
                                      Type networkType,
                                      boolean install) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(srcSubnet)
                .matchEthDst(Constants.DEFAULT_GATEWAY_MAC);

        switch (networkType) {
            case VXLAN:
            case GRE:
            case GENEVE:
                sBuilder.matchTunnelId(Long.parseLong(segmentId));
                break;
            case VLAN:
                sBuilder.matchVlanId(VlanId.vlanId(segmentId));
                break;
            default:
                final String error = String.format("%s %s",
                        ERR_UNSUPPORTED_NET_TYPE,
                        networkType.toString());
                throw new IllegalStateException(error);
        }

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        if (networkType == VLAN) {
            tBuilder.popVlan();
        }

        tBuilder.punt();

        osFlowRuleService.setRule(
                appId,
                deviceId,
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_EXTERNAL_ROUTING_RULE,
                GW_COMMON_TABLE,
                install);
    }

    private boolean getStatefulSnatFlag() {
        Set<ConfigProperty> properties =
                configService.getProperties(getClass().getName());
        return getPropertyValueAsBoolean(properties, USE_STATEFUL_SNAT);
    }

    private class InternalInstancePortListener implements InstancePortListener {

        private boolean isRelevantHelper(InstancePortEvent event) {
            return mastershipService.isLocalMaster(event.subject().deviceId());
        }

        @Override
        public void event(InstancePortEvent event) {
            InstancePort instPort = event.subject();
            switch (event.type()) {
                case OPENSTACK_INSTANCE_PORT_DETECTED:
                case OPENSTACK_INSTANCE_PORT_UPDATED:
                    eventExecutor.execute(() ->
                            processInstancePortDetection(event, instPort));
                    break;
                case OPENSTACK_INSTANCE_PORT_VANISHED:
                    eventExecutor.execute(() ->
                            processInstancePortRemoval(event, instPort));
                    break;
                case OPENSTACK_INSTANCE_MIGRATION_STARTED:
                    eventExecutor.execute(() ->
                            processInstanceMigrationStart(event, instPort));
                    break;
                case OPENSTACK_INSTANCE_MIGRATION_ENDED:
                    eventExecutor.execute(() ->
                            processInstanceMigrationEnd(event, instPort));
                    break;
                default:
                    break;
            }
        }

        private void processInstancePortDetection(InstancePortEvent event,
                                                  InstancePort instPort) {
            if (!isRelevantHelper(event)) {
                return;
            }

            log.info("RoutingHandler: Instance port detected MAC:{} IP:{}",
                    instPort.macAddress(),
                    instPort.ipAddress());

            instPortDetected(event.subject());
        }

        private void processInstancePortRemoval(InstancePortEvent event,
                                                InstancePort instPort) {
            if (!isRelevantHelper(event)) {
                return;
            }

            log.info("RoutingHandler: Instance port vanished MAC:{} IP:{}",
                    instPort.macAddress(),
                    instPort.ipAddress());

            instPortRemoved(event.subject());
        }

        private void processInstanceMigrationStart(InstancePortEvent event,
                                                   InstancePort instPort) {
            if (!isRelevantHelper(event)) {
                return;
            }

            log.info("RoutingHandler: Migration started for MAC:{} IP:{}",
                    instPort.macAddress(),
                    instPort.ipAddress());

            instPortDetected(instPort);
        }

        private void processInstanceMigrationEnd(InstancePortEvent event,
                                                 InstancePort instPort) {
            log.info("RoutingHandler: Migration finished for MAC:{} IP:{}",
                    instPort.macAddress(),
                    instPort.ipAddress());
            // TODO: need to reconfigure rules to point to update VM
        }

        private void instPortDetected(InstancePort instPort) {
            Type netType = osNetworkAdminService.networkType(instPort.networkId());

            if (netType == FLAT) {
                return;
            }

            if (getStatefulSnatFlag()) {
                osNodeService.completeNodes(GATEWAY).forEach(gwNode ->
                        setGatewayToInstanceDownstreamRule(gwNode, instPort, true));
            }
        }

        private void instPortRemoved(InstancePort instPort) {
            Type netType = osNetworkAdminService.networkType(instPort.networkId());

            if (netType == FLAT) {
                return;
            }

            if (getStatefulSnatFlag()) {
                osNodeService.completeNodes(GATEWAY).forEach(gwNode ->
                        setGatewayToInstanceDownstreamRule(gwNode, instPort, false));
            }
        }
    }

    private class InternalNetworkEventListener implements OpenstackNetworkListener {

        @Override
        public boolean isRelevant(OpenstackNetworkEvent event) {
            Port osPort = event.port();
            if (osPort == null || osPort.getFixedIps() == null) {
                return false;
            }

            return DEVICE_OWNER_ROUTER_GW.equals(osPort.getDeviceOwner()) &&
                    getStatefulSnatFlag();
        }

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(OpenstackNetworkEvent event) {
            IpAddress ipAddress = externalIp(event.port());
            switch (event.type()) {
                case OPENSTACK_PORT_CREATED:
                case OPENSTACK_PORT_UPDATED:
                    eventExecutor.execute(() -> processPortCreation(ipAddress));
                    break;
                case OPENSTACK_PORT_REMOVED:
                    eventExecutor.execute(() -> processPortRemoval(ipAddress));
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        private void processPortCreation(IpAddress ipAddress) {
            if (!isRelevantHelper() || ipAddress == null) {
                return;
            }

            setStatefulDownstreamRules(ipAddress, true);
        }

        private void processPortRemoval(IpAddress ipAddress) {
            if (!isRelevantHelper() || ipAddress == null) {
                return;
            }

            setStatefulDownstreamRules(ipAddress, false);
        }

        private IpAddress externalIp(Port port) {
            IP ip = port.getFixedIps().stream().findAny().orElse(null);

            if (ip != null && ip.getIpAddress() != null) {
                return IpAddress.valueOf(ip.getIpAddress());
            }

            return null;
        }
    }

    private class InternalRouterEventListener implements OpenstackRouterListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(OpenstackRouterEvent event) {
            switch (event.type()) {
                case OPENSTACK_ROUTER_CREATED:
                    eventExecutor.execute(() -> processRouterCreation(event));
                    break;
                case OPENSTACK_ROUTER_UPDATED:
                    eventExecutor.execute(() -> processRouterUpdate(event));
                    break;
                case OPENSTACK_ROUTER_INTERFACE_ADDED:
                    eventExecutor.execute(() -> processRouterIntfCreation(event));
                    break;
                case OPENSTACK_ROUTER_INTERFACE_REMOVED:
                    eventExecutor.execute(() -> processRouterIntfRemoval(event));
                    break;
                default:
                    break;
            }
        }

        private void processRouterCreation(OpenstackRouterEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            log.debug("Router(name:{}, ID:{}) is created",
                    deriveResourceName(event.subject()), event.subject().getId());

            routerUpdated(event.subject());
        }

        private void processRouterUpdate(OpenstackRouterEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            log.debug("Router(name:{}, ID:{}) is updated",
                    deriveResourceName(event.subject()), event.subject().getId());

            routerUpdated(event.subject());
        }

        private void processRouterIntfCreation(OpenstackRouterEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            log.debug("Router interface {} added to router {}",
                    event.routerIface().getPortId(),
                    event.routerIface().getId());

            routerIfaceAdded(event.subject(), event.routerIface());
        }

        private void processRouterIntfRemoval(OpenstackRouterEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            log.debug("Router interface {} removed from router {}",
                    event.routerIface().getPortId(),
                    event.routerIface().getId());

            routerIfaceRemoved(event.subject(), event.routerIface());
        }
    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {

            if (context.isHandled()) {
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
                        break; // don't process DHCP
                    }
                default:
                    eventExecutor.execute(() -> {
                        if (!isRelevantHelper(context)) {
                            return;
                        }
                        processSnatPacket(context, eth);
                    });
                    break;
            }
        }

        private boolean isRelevantHelper(PacketContext context) {
            Set<DeviceId> gateways = osNodeService.completeNodes(GATEWAY)
                    .stream().map(OpenstackNode::intgBridge)
                    .collect(Collectors.toSet());

            return gateways.contains(context.inPacket().receivedFrom().deviceId());
        }
    }

    private class InternalNodeEventListener implements OpenstackNodeListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(OpenstackNodeEvent event) {
            OpenstackNode osNode = event.subject();
            switch (event.type()) {
                case OPENSTACK_NODE_COMPLETE:
                    eventExecutor.execute(() -> processGatewayCompletion(osNode));
                    eventExecutor.execute(() -> reconfigureRouters(osNode));
                    break;
                case OPENSTACK_NODE_REMOVED:
                    eventExecutor.execute(() -> processGatewayRemoval(osNode));
                    eventExecutor.execute(() -> reconfigureRouters(osNode));
                    break;
                case OPENSTACK_NODE_UPDATED:
                    eventExecutor.execute(() -> reconfigureRouters(osNode));
                    break;
                case OPENSTACK_NODE_INCOMPLETE:
                case OPENSTACK_NODE_CREATED:
                default:
                    break;
            }
        }

        private void processGatewayCompletion(OpenstackNode osNode) {
            if (!isRelevantHelper()) {
                return;
            }

            if (getStatefulSnatFlag() && osNode.type() == GATEWAY) {
                instancePortService.instancePorts().forEach(instPort -> {
                    Type netType = osNetworkAdminService.networkType(instPort.networkId());

                    if (netType == FLAT) {
                        return;
                    }
                    setGatewayToInstanceDownstreamRule(osNode, instPort, true);
                });
            }
        }

        private void processGatewayRemoval(OpenstackNode osNode) {
            if (!isRelevantHelper()) {
                return;
            }

            if (getStatefulSnatFlag() && osNode.type() == GATEWAY) {
                instancePortService.instancePorts().forEach(instPort -> {
                    Type netType = osNetworkAdminService.networkType(instPort.networkId());

                    if (netType == FLAT) {
                        return;
                    }

                    setGatewayToInstanceDownstreamRule(osNode, instPort, false);
                });
            }
        }

        private void reconfigureRouters(OpenstackNode osNode) {
            if (!isRelevantHelper()) {
                return;
            }

            osRouterService.routers().forEach(osRouter -> {
                routerUpdated(osRouter);
                osRouterService.routerInterfaces(osRouter.getId()).forEach(iface -> {
                    routerIfaceAdded(osRouter, iface);
                });

                setStatefulDownstreamRules(osRouter, true);
            });
            log.debug("Reconfigure routers for {}", osNode.hostname());
        }
    }

    private class PortRange {
        private int min;
        private int max;

        /**
         * A default constructor.
         *
         * @param min min port num
         * @param max max port num
         */
        public PortRange(int min, int max) {
            this.min = min;
            this.max = max;
        }

        /**
         * Obtains min port num.
         *
         * @return min port num
         */
        int min() {
            return min;
        }

        /**
         * Obtains max port num.
         *
         * @return max port num
         */
        int max() {
            return max;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("min", min)
                    .add("max", max)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PortRange portRange = (PortRange) o;
            return min == portRange.min &&
                    max == portRange.max;
        }

        @Override
        public int hashCode() {
            return Objects.hash(min, max);
        }
    }
}
