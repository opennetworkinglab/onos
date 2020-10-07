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

import com.google.common.collect.Lists;
import org.onlab.packet.ARP;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.GroupId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortEvent;
import org.onosproject.openstacknetworking.api.InstancePortListener;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;
import org.onosproject.openstacknetworking.api.OpenstackGroupRuleService;
import org.onosproject.openstacknetworking.api.OpenstackNetwork.Type;
import org.onosproject.openstacknetworking.api.OpenstackNetworkEvent;
import org.onosproject.openstacknetworking.api.OpenstackNetworkListener;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.model.common.IdEntity;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.NetworkType;
import org.openstack4j.model.network.Subnet;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.group.GroupDescription.Type.ALL;
import static org.onosproject.openstacknetworking.api.Constants.ARP_BROADCAST_MODE;
import static org.onosproject.openstacknetworking.api.Constants.ARP_PROXY_MODE;
import static org.onosproject.openstacknetworking.api.Constants.ARP_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ARP_CONTROL_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ARP_GATEWAY_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ARP_GROUP_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ARP_REPLY_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ARP_REQUEST_RULE;
import static org.onosproject.openstacknetworking.api.InstancePort.State.ACTIVE;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.FLAT;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.GENEVE;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.GRE;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.VLAN;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.VXLAN;
import static org.onosproject.openstacknetworking.impl.OsgiPropertyConstants.ARP_MODE;
import static org.onosproject.openstacknetworking.impl.OsgiPropertyConstants.ARP_MODE_DEFAULT;
import static org.onosproject.openstacknetworking.impl.OsgiPropertyConstants.GATEWAY_MAC;
import static org.onosproject.openstacknetworking.impl.OsgiPropertyConstants.GATEWAY_MAC_DEFAULT;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getPropertyValue;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.swapStaleLocation;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.tunnelPortNumByNetId;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.buildExtension;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.buildGroupBucket;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.buildMoveArpShaToThaExtension;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.buildMoveArpSpaToTpaExtension;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.buildMoveEthSrcToDstExtension;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;

/**
 * Handles ARP packet from VMs.
 */
@Component(
    immediate = true,
    property = {
        GATEWAY_MAC + "=" + GATEWAY_MAC_DEFAULT,
        ARP_MODE + "=" + ARP_MODE_DEFAULT
    }
)
public class OpenstackSwitchingArpHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackFlowRuleService osFlowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackGroupRuleService osGroupRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNodeService osNodeService;

    /** Fake MAC address for virtual network subnet gateway. */
    private String gatewayMac = GATEWAY_MAC_DEFAULT;

    /** ARP processing mode, broadcast | proxy (default). */
    protected String arpMode = ARP_MODE_DEFAULT;

    private final InternalPacketProcessor packetProcessor = new InternalPacketProcessor();
    private final InternalOpenstackNetworkListener osNetworkListener =
            new InternalOpenstackNetworkListener();
    private final InstancePortListener instancePortListener = new InternalInstancePortListener();
    private final OpenstackNodeListener osNodeListener = new InternalNodeEventListener();

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));


    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        configService.registerProperties(getClass());
        localNodeId = clusterService.getLocalNode().id();
        osNetworkService.addListener(osNetworkListener);
        osNodeService.addListener(osNodeListener);
        leadershipService.runForLeadership(appId.name());
        packetService.addProcessor(packetProcessor, PacketProcessor.director(0));

        instancePortService.addListener(instancePortListener);

        log.info("Started");
    }

    @Deactivate
    void deactivate() {
        packetService.removeProcessor(packetProcessor);
        osNetworkService.removeListener(osNetworkListener);
        osNodeService.removeListener(osNodeListener);
        instancePortService.removeListener(instancePortListener);
        leadershipService.withdraw(appId.name());
        configService.unregisterProperties(getClass(), false);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Modified
    void modified(ComponentContext context) {
        readComponentConfiguration(context);

        log.info("Modified");
    }

    private String getArpMode() {
        Set<ConfigProperty> properties = configService.getProperties(this.getClass().getName());
        return getPropertyValue(properties, ARP_MODE);
    }

    /**
     * Processes ARP request packets.
     * It checks if the target IP is owned by a known host first and then ask to
     * OpenStack if it's not. This ARP proxy does not support overlapping IP.
     *
     * @param context   packet context
     * @param ethPacket ethernet packet
     */
    private void processPacketIn(PacketContext context, Ethernet ethPacket) {

        // if the ARP mode is configured as broadcast mode, we simply ignore ARP packet_in
        if (ARP_BROADCAST_MODE.equals(getArpMode())) {
            return;
        }

        ARP arpPacket = (ARP) ethPacket.getPayload();
        if (arpPacket.getOpCode() != ARP.OP_REQUEST) {
            return;
        }

        InstancePort srcInstPort = instancePortService.instancePort(ethPacket.getSourceMAC());
        if (srcInstPort == null) {
            log.trace("Failed to find source instance port(MAC:{})",
                    ethPacket.getSourceMAC());
            return;
        }

        IpAddress targetIp = Ip4Address.valueOf(arpPacket.getTargetProtocolAddress());

        MacAddress replyMac = isGatewayIp(targetIp) ? MacAddress.valueOf(gatewayMac) :
                getMacFromHostOpenstack(targetIp, srcInstPort.networkId());
        if (replyMac == MacAddress.NONE) {
            log.trace("Failed to find MAC address for {}", targetIp);
            return;
        }

        Ethernet ethReply = ARP.buildArpReply(
                targetIp.getIp4Address(),
                replyMac,
                ethPacket);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(context.inPacket().receivedFrom().port())
                .build();

        packetService.emit(new DefaultOutboundPacket(
                context.inPacket().receivedFrom().deviceId(),
                treatment,
                ByteBuffer.wrap(ethReply.serialize())));
    }

    /**
     * Denotes whether the given target IP is gateway IP.
     *
     * @param targetIp target IP address
     * @return true if the given targetIP is gateway IP, false otherwise.
     */
    private boolean isGatewayIp(IpAddress targetIp) {
        return osNetworkService.subnets().stream()
                .filter(Objects::nonNull)
                .filter(subnet -> subnet.getGateway() != null)
                .anyMatch(subnet -> subnet.getGateway().equals(targetIp.toString()));
    }

    /**
     * Returns MAC address of a host with a given target IP address by asking to
     * instance port service.
     *
     * @param targetIp target ip
     * @param osNetId  openstack network id of the source instance port
     * @return mac address, or none mac address if it fails to find the mac
     */
    private MacAddress getMacFromHostOpenstack(IpAddress targetIp, String osNetId) {
        checkNotNull(targetIp);

        InstancePort instPort = instancePortService.instancePort(targetIp, osNetId);
        if (instPort != null) {
            log.trace("Found MAC from host service for {}", targetIp);
            return instPort.macAddress();
        } else {
            return MacAddress.NONE;
        }
    }

    /**
     * Installs flow rules which convert ARP request packet into ARP reply
     * by adding a fake gateway MAC address as Source Hardware Address.
     *
     * @param osSubnet  openstack subnet
     * @param network   openstack network
     * @param install   flag which indicates whether to install rule or remove rule
     * @param osNode    openstack node
     */
    private void setFakeGatewayArpRule(Subnet osSubnet, Network network,
                                       boolean install, OpenstackNode osNode) {

        if (ARP_BROADCAST_MODE.equals(getArpMode())) {

            Type netType = osNetworkService.networkType(network.getId());

            String gateway = osSubnet.getGateway();
            if (gateway == null) {
                return;
            }

            TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();

            if (netType == VLAN) {
                sBuilder.matchVlanId(VlanId.vlanId(network.getProviderSegID()));

            } else if (netType == VXLAN || netType == GRE || netType == GENEVE) {
                // do not remove fake gateway ARP rules, if there is another gateway
                // which has the same subnet that to be removed
                // this only occurs if we have duplicated subnets associated with
                // different networks
                if (!install) {
                    long numOfDupGws = osNetworkService.subnets().stream()
                            .filter(s -> !s.getId().equals(osSubnet.getId()))
                            .filter(s -> s.getGateway() != null)
                            .filter(s -> s.getGateway().equals(osSubnet.getGateway()))
                            .count();
                    if (numOfDupGws > 0) {
                        return;
                    }
                }
            }

            sBuilder.matchEthType(EthType.EtherType.ARP.ethType().toShort())
                    .matchArpOp(ARP.OP_REQUEST)
                    .matchArpTpa(Ip4Address.valueOf(gateway));

            if (osNode == null) {
                osNodeService.completeNodes(COMPUTE).forEach(n -> {
                    Device device = deviceService.getDevice(n.intgBridge());

                    TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

                    if (netType == VLAN) {
                        tBuilder.popVlan();
                    }

                    tBuilder.extension(buildMoveEthSrcToDstExtension(device), device.id())
                            .extension(buildMoveArpShaToThaExtension(device), device.id())
                            .extension(buildMoveArpSpaToTpaExtension(device), device.id())
                            .setArpOp(ARP.OP_REPLY)
                            .setArpSha(MacAddress.valueOf(gatewayMac))
                            .setArpSpa(Ip4Address.valueOf(gateway))
                            .setEthSrc(MacAddress.valueOf(gatewayMac))
                            .setOutput(PortNumber.IN_PORT);

                    osFlowRuleService.setRule(
                            appId,
                            n.intgBridge(),
                            sBuilder.build(),
                            tBuilder.build(),
                            PRIORITY_ARP_GATEWAY_RULE,
                            ARP_TABLE,
                            install
                    );
                });
            } else {
                Device device = deviceService.getDevice(osNode.intgBridge());

                TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

                if (netType == VLAN) {
                    tBuilder.popVlan();
                }

                tBuilder.extension(buildMoveEthSrcToDstExtension(device), device.id())
                        .extension(buildMoveArpShaToThaExtension(device), device.id())
                        .extension(buildMoveArpSpaToTpaExtension(device), device.id())
                        .setArpOp(ARP.OP_REPLY)
                        .setArpSha(MacAddress.valueOf(gatewayMac))
                        .setArpSpa(Ip4Address.valueOf(gateway))
                        .setOutput(PortNumber.IN_PORT);

                osFlowRuleService.setRule(
                        appId,
                        osNode.intgBridge(),
                        sBuilder.build(),
                        tBuilder.build(),
                        PRIORITY_ARP_GATEWAY_RULE,
                        ARP_TABLE,
                        install
                );
            }
        }
    }

    /**
     * Installs flow rules to match ARP request packets.
     *
     * @param port      instance port
     * @param install   installation flag
     */
    private void setArpRequestRule(InstancePort port, boolean install) {
        Type netType = osNetworkService.networkType(port.networkId());

        switch (netType) {
            case VXLAN:
            case GRE:
            case GENEVE:
                setRemoteArpRequestRuleForTunnel(port, install);
                setLocalArpRequestRuleForVnet(port, install);
                break;
            case VLAN:
                setArpRequestRuleForVlan(port, install);
                setLocalArpRequestRuleForVnet(port, install);
                break;
            default:
                break;
        }
    }

    /**
     * Installs flow rules to match ARP reply packets.
     *
     * @param port      instance port
     * @param install   installation flag
     */
    private void setArpReplyRule(InstancePort port, boolean install) {
        Type netType = osNetworkService.networkType(port.networkId());

        switch (netType) {
            case VXLAN:
                setArpReplyRuleForVxlan(port, install);
                break;
            case GRE:
                setArpReplyRuleForGre(port, install);
                break;
            case GENEVE:
                setArpReplyRuleForGeneve(port, install);
                break;
            case VLAN:
                setArpReplyRuleForVlan(port, install);
                break;
            default:
                break;
        }
    }

    /**
     * Installs flow rules at remote node to match ARP request packets for Tunnel.
     *
     * @param port      instance port
     * @param install   installation flag
     */
    private void setRemoteArpRequestRuleForTunnel(InstancePort port, boolean install) {

        OpenstackNode localNode = osNodeService.node(port.deviceId());

        String segId = osNetworkService.segmentId(port.networkId());

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                .matchArpOp(ARP.OP_REQUEST)
                .matchArpTpa(port.ipAddress().getIp4Address())
                .matchTunnelId(Long.parseLong(segId))
                .build();

        setRemoteArpTreatmentForTunnel(selector, port, localNode, install);
    }

    /**
     * Installs flow rules at local node to matchA RP request packets for Tunnel.
     *
     * @param port      instance port
     * @param install   installation flag
     */
    private void setLocalArpRequestRuleForVnet(InstancePort port, boolean install) {
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setOutput(port.portNumber());

        List<GroupBucket> bkts = Lists.newArrayList();
        bkts.add(buildGroupBucket(tBuilder.build(), ALL, (short) -1));
        osGroupRuleService.setBuckets(appId, port.deviceId(),
                port.networkId().hashCode(), bkts, install);
    }

    /**
     * Installs flow rules to match ARP request packets for VLAN.
     *
     * @param port      instance port
     * @param install   installation flag
     */
    private void setArpRequestRuleForVlan(InstancePort port, boolean install) {
        TrafficSelector selector = getArpRequestSelectorForVlan(port);

        setLocalArpRequestTreatmentForVlan(selector, port, install);
        setRemoteArpRequestTreatmentForVlan(selector, port, install);
    }

    /**
     * Obtains the ARP request selector for VLAN.
     *
     * @param port instance port
     * @return traffic selector
     */
    private TrafficSelector getArpRequestSelectorForVlan(InstancePort port) {
        String segId = osNetworkService.segmentId(port.networkId());

        return DefaultTrafficSelector.builder()
                .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                .matchArpOp(ARP.OP_REQUEST)
                .matchArpTpa(port.ipAddress().getIp4Address())
                .matchVlanId(VlanId.vlanId(segId))
                .build();
    }

    /**
     * Installs flow rules to match ARP reply packets only for VxLAN.
     *
     * @param port      instance port
     * @param install   installation flag
     */
    private void setArpReplyRuleForVxlan(InstancePort port, boolean install) {

        OpenstackNode localNode = osNodeService.node(port.deviceId());

        TrafficSelector selector = getArpReplySelectorForVxlan(port);

        setLocalArpReplyTreatmentForVxlan(selector, port, install);
        setRemoteArpTreatmentForTunnel(selector, port, localNode, install);
    }

    /**
     * Installs flow rules to match ARP reply packets only for GRE.
     *
     * @param port      instance port
     * @param install   installation flag
     */
    private void setArpReplyRuleForGre(InstancePort port, boolean install) {

        OpenstackNode localNode = osNodeService.node(port.deviceId());

        TrafficSelector selector = getArpReplySelectorForGre(port);

        setLocalArpReplyTreatmentForGre(selector, port, install);
        setRemoteArpTreatmentForTunnel(selector, port, localNode, install);
    }

    /**
     * Installs flow rules to match ARP reply packets only for GENEVE.
     *
     * @param port      instance port
     * @param install   installation flag
     */
    private void setArpReplyRuleForGeneve(InstancePort port, boolean install) {

        OpenstackNode localNode = osNodeService.node(port.deviceId());

        TrafficSelector selector = getArpReplySelectorForGeneve(port);

        setLocalArpReplyTreatmentForGeneve(selector, port, install);
        setRemoteArpTreatmentForTunnel(selector, port, localNode, install);
    }

    /**
     * Installs flow rules to match ARP reply packets only for VLAN.
     *
     * @param port      instance port
     * @param install   installation flag
     */
    private void setArpReplyRuleForVlan(InstancePort port, boolean install) {

        TrafficSelector selector = getArpReplySelectorForVlan(port);

        setLocalArpReplyTreatmentForVlan(selector, port, install);
        setRemoteArpReplyTreatmentForVlan(selector, port, install);
    }

    // a helper method
    private TrafficSelector getArpReplySelectorForVxlan(InstancePort port) {
        return getArpReplySelectorForVnet(port, VXLAN);
    }

    // a helper method
    private TrafficSelector getArpReplySelectorForGre(InstancePort port) {
        return getArpReplySelectorForVnet(port, GRE);
    }

    // a helper method
    private TrafficSelector getArpReplySelectorForGeneve(InstancePort port) {
        return getArpReplySelectorForVnet(port, GENEVE);
    }

    // a helper method
    private TrafficSelector getArpReplySelectorForVlan(InstancePort port) {
        return getArpReplySelectorForVnet(port, VLAN);
    }

    // a helper method
    private TrafficSelector getArpReplySelectorForVnet(InstancePort port,
                                                       Type type) {

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();

        if (type == VLAN) {
            String segId = osNetworkService.network(port.networkId()).getProviderSegID();
            sBuilder.matchVlanId(VlanId.vlanId(segId));
        }

        return sBuilder
                .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                .matchArpOp(ARP.OP_REPLY)
                .matchArpTpa(port.ipAddress().getIp4Address())
                .matchArpTha(port.macAddress())
                .build();
    }

    // a helper method
    private void setLocalArpReplyTreatmentForVxlan(TrafficSelector selector,
                                                   InstancePort port,
                                                   boolean install) {
        setLocalArpReplyTreatmentForVnet(selector, port, VXLAN, install);
    }

    // a helper method
    private void setLocalArpReplyTreatmentForGre(TrafficSelector selector,
                                                 InstancePort port,
                                                 boolean install) {
        setLocalArpReplyTreatmentForVnet(selector, port, GRE, install);
    }

    // a helper method
    private void setLocalArpReplyTreatmentForGeneve(TrafficSelector selector,
                                                    InstancePort port,
                                                    boolean install) {
        setLocalArpReplyTreatmentForVnet(selector, port, GENEVE, install);
    }

    // a helper method
    private void setLocalArpReplyTreatmentForVlan(TrafficSelector selector,
                                                  InstancePort port,
                                                  boolean install) {
        setLocalArpReplyTreatmentForVnet(selector, port, VLAN, install);
    }

    // a helper method
    private void setLocalArpReplyTreatmentForVnet(TrafficSelector selector,
                                                  InstancePort port,
                                                  Type type,
                                                  boolean install) {
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        if (type == VLAN) {
            tBuilder.popVlan();
        }

        tBuilder.setOutput(port.portNumber());

        osFlowRuleService.setRule(
                appId,
                port.deviceId(),
                selector,
                tBuilder.build(),
                PRIORITY_ARP_REPLY_RULE,
                ARP_TABLE,
                install
        );
    }

    // a helper method
    private void setLocalArpRequestTreatmentForVlan(TrafficSelector selector,
                                                    InstancePort port,
                                                    boolean install) {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .popVlan()
                .setOutput(port.portNumber())
                .build();

        osFlowRuleService.setRule(
                appId,
                port.deviceId(),
                selector,
                treatment,
                PRIORITY_ARP_REQUEST_RULE,
                ARP_TABLE,
                install
        );
    }

    // a helper method
    private void setRemoteArpTreatmentForTunnel(TrafficSelector selector,
                                                InstancePort port,
                                                OpenstackNode localNode,
                                                boolean install) {
        for (OpenstackNode remoteNode : osNodeService.completeNodes(COMPUTE)) {
            if (!remoteNode.intgBridge().equals(port.deviceId())) {

                PortNumber portNum = tunnelPortNumByNetId(port.networkId(),
                        osNetworkService, remoteNode);

                TrafficTreatment treatmentToRemote = DefaultTrafficTreatment.builder()
                        .extension(buildExtension(
                                deviceService,
                                remoteNode.intgBridge(),
                                localNode.dataIp().getIp4Address()),
                                remoteNode.intgBridge())
                        .setOutput(portNum)
                        .build();

                osFlowRuleService.setRule(
                        appId,
                        remoteNode.intgBridge(),
                        selector,
                        treatmentToRemote,
                        PRIORITY_ARP_REQUEST_RULE,
                        ARP_TABLE,
                        install
                );
            }
        }
    }

    // a helper method
    private void setRemoteArpRequestTreatmentForVlan(TrafficSelector selector,
                                                     InstancePort port,
                                                     boolean install) {
        setRemoteArpTreatmentForVlan(selector, port, ARP.OP_REQUEST, install);
    }

    // a helper method
    private void setRemoteArpReplyTreatmentForVlan(TrafficSelector selector,
                                                   InstancePort port,
                                                   boolean install) {
        setRemoteArpTreatmentForVlan(selector, port, ARP.OP_REPLY, install);
    }

    // a helper method
    private void setRemoteArpTreatmentForVlan(TrafficSelector selector,
                                              InstancePort port,
                                              short arpOp,
                                              boolean install) {

        int priority;
        if (arpOp == ARP.OP_REQUEST) {
            priority = PRIORITY_ARP_REQUEST_RULE;
        } else if (arpOp == ARP.OP_REPLY) {
            priority = PRIORITY_ARP_REPLY_RULE;
        } else {
            // if ARP op does not match with any operation mode, we simply
            // configure the ARP request rule priority
            priority = PRIORITY_ARP_REQUEST_RULE;
        }

        for (OpenstackNode remoteNode : osNodeService.completeNodes(COMPUTE)) {
            if (!remoteNode.intgBridge().equals(port.deviceId()) &&
                    remoteNode.vlanIntf() != null) {
                TrafficTreatment treatmentToRemote = DefaultTrafficTreatment.builder()
                        .setOutput(remoteNode.vlanPortNum())
                        .build();

                osFlowRuleService.setRule(
                        appId,
                        remoteNode.intgBridge(),
                        selector,
                        treatmentToRemote,
                        priority,
                        ARP_TABLE,
                        install);
            }
        }
    }

    // a helper method
    private void setBaseVnetArpRuleForBroadcastMode(OpenstackNode osNode,
                                                    String segId, String netId,
                                                    boolean isTunnel,
                                                    boolean install) {

        if (install) {
            processGroupTableRules(osNode, netId, true);
            processFlowTableRules(osNode, segId, netId, isTunnel, true);
        } else {
            processFlowTableRules(osNode, segId, netId, isTunnel, false);
            processGroupTableRules(osNode, netId, false);
        }
    }

    private void processGroupTableRules(OpenstackNode osNode,
                                        String netId, boolean install) {
        int groupId = netId.hashCode();
        osGroupRuleService.setRule(appId, osNode.intgBridge(), groupId,
                ALL, Lists.newArrayList(), install);
    }

    private void processFlowTableRules(OpenstackNode osNode,
                                       String segId, String netId,
                                       boolean isTunnel,
                                       boolean install) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                .matchArpOp(ARP.OP_REQUEST);

        if (isTunnel) {
            sBuilder.matchTunnelId(Long.parseLong(segId));
        } else {
            sBuilder.matchVlanId(VlanId.vlanId(segId));
        }

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .group(GroupId.valueOf(netId.hashCode()))
                .build();

        osFlowRuleService.setRule(
                appId,
                osNode.intgBridge(),
                sBuilder.build(),
                treatment,
                PRIORITY_ARP_GROUP_RULE,
                ARP_TABLE,
                install
        );
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        String updatedMac = Tools.get(properties, GATEWAY_MAC);
        gatewayMac = updatedMac != null ? updatedMac : GATEWAY_MAC_DEFAULT;
        log.info("Configured. Gateway MAC is {}", gatewayMac);
    }

    /**
     * An internal packet processor which processes ARP request, and results in
     * packet-out ARP reply.
     */
    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            }

            Ethernet ethPacket = context.inPacket().parsed();
            if (ethPacket == null || ethPacket.getEtherType() != Ethernet.TYPE_ARP) {
                return;
            }

            eventExecutor.execute(() -> processPacketIn(context, ethPacket));
        }
    }

    /**
     * An internal network listener which listens to openstack network event,
     * manages the gateway collection and installs flow rule that handles
     * ARP request in data plane.
     */
    private class InternalOpenstackNetworkListener implements OpenstackNetworkListener {

        @Override
        public boolean isRelevant(OpenstackNetworkEvent event) {
            Network network = event.subject();

            if (network == null) {
                log.debug("Network is not specified.");
                return false;
            } else {
                return network.getProviderSegID() != null;
            }
        }

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(OpenstackNetworkEvent event) {
            switch (event.type()) {
                case OPENSTACK_SUBNET_CREATED:
                case OPENSTACK_SUBNET_UPDATED:
                    eventExecutor.execute(() -> processSubnetCreation(event));
                    break;
                case OPENSTACK_SUBNET_REMOVED:
                    eventExecutor.execute(() -> processSubnetRemoval(event));
                    break;
                case OPENSTACK_NETWORK_CREATED:
                case OPENSTACK_NETWORK_UPDATED:
                    eventExecutor.execute(() -> processNetworkCreation(event));
                    break;
                case OPENSTACK_NETWORK_PRE_REMOVED:
                    eventExecutor.execute(() -> processNetworkRemoval(event));
                    break;
                case OPENSTACK_NETWORK_REMOVED:
                case OPENSTACK_PORT_CREATED:
                case OPENSTACK_PORT_UPDATED:
                case OPENSTACK_PORT_REMOVED:
                default:
                    // do nothing for the other events
                    break;
            }
        }

        private void processSubnetCreation(OpenstackNetworkEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            setFakeGatewayArpRule(event.subnet(), event.subject(),
                    true, null);
        }

        private void processSubnetRemoval(OpenstackNetworkEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            setFakeGatewayArpRule(event.subnet(), event.subject(),
                    false, null);
        }

        private void processNetworkCreation(OpenstackNetworkEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            setVnetArpRule(event.subject(), true);
        }

        private void processNetworkRemoval(OpenstackNetworkEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            setVnetArpRule(event.subject(), false);
        }

        private void setVnetArpRule(Network network, boolean install) {

            if (ARP_PROXY_MODE.equals(getArpMode())) {
                return;
            }

            String netId = network.getId();
            NetworkType netType = network.getNetworkType();

            if (netType != NetworkType.LOCAL && netType != NetworkType.FLAT
                    && netType != NetworkType.VLAN) {
                String segId = network.getProviderSegID();
                osNodeService.completeNodes(COMPUTE)
                        .forEach(node -> {
                            setBaseVnetArpRuleForBroadcastMode(node, segId,
                                    netId, true, install);
                        });
            }
            if (netType == NetworkType.VLAN) {
                String segId = network.getProviderSegID();
                osNodeService.completeNodes(COMPUTE)
                        .forEach(node -> {
                            setBaseVnetArpRuleForBroadcastMode(
                                    node, segId, netId, false, install);
                        });
            }
        }
    }

    /**
     * An internal openstack node listener which is used for listening openstack
     * node activity. As long as a node is in complete state, we will install
     * default ARP rule to handle ARP request.
     */
    private class InternalNodeEventListener implements OpenstackNodeListener {
        @Override
        public boolean isRelevant(OpenstackNodeEvent event) {
            return event.subject().type() == COMPUTE;
        }

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(OpenstackNodeEvent event) {
            OpenstackNode osNode = event.subject();
            switch (event.type()) {
                case OPENSTACK_NODE_COMPLETE:
                    eventExecutor.execute(() -> processNodeCompletion(osNode));
                    break;
                case OPENSTACK_NODE_INCOMPLETE:
                default:
                    break;
            }
        }

        private void processNodeCompletion(OpenstackNode osNode) {
            if (!isRelevantHelper()) {
                return;
            }

            setDefaultArpRule(osNode, true);
            setAllArpRules(osNode, true);
        }

        private void processNodeIncompletion(OpenstackNode osNode) {
            if (!isRelevantHelper()) {
                return;
            }

            setDefaultArpRule(osNode, false);
            setAllArpRules(osNode, false);
        }

        private void setDefaultArpRule(OpenstackNode osNode, boolean install) {

            if (getArpMode() == null) {
                return;
            }

            switch (getArpMode()) {
                case ARP_PROXY_MODE:
                    setDefaultArpRuleForProxyMode(osNode, install);
                    break;
                case ARP_BROADCAST_MODE:
                    processDefaultArpRuleForBroadcastMode(osNode, install);
                    break;
                default:
                    log.warn("Invalid ARP mode {}. Please use either " +
                            "broadcast or proxy mode.", getArpMode());
                    break;
            }
        }

        private void processDefaultArpRuleForBroadcastMode(OpenstackNode osNode,
                                                           boolean install) {
            setVnetArpRuleForBroadcastMode(osNode, install);

            // we do not add fake gateway ARP rules for FLAT network
            // ARP packets generated by FLAT typed VM should not be
            // delegated to switch to handle
            osNetworkService.subnets().stream().filter(subnet ->
                    osNetworkService.network(subnet.getNetworkId()) != null &&
                            osNetworkService.networkType(subnet.getNetworkId()) != FLAT)
                    .forEach(subnet -> {
                        String netId = subnet.getNetworkId();
                        Network net = osNetworkService.network(netId);
                        setFakeGatewayArpRule(subnet, net, install, osNode);
                    });
        }

        private void setDefaultArpRuleForProxyMode(OpenstackNode osNode, boolean install) {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .punt()
                    .build();

            osFlowRuleService.setRule(
                    appId,
                    osNode.intgBridge(),
                    selector,
                    treatment,
                    PRIORITY_ARP_CONTROL_RULE,
                    ARP_TABLE,
                    install
            );
        }

        private void setVnetArpRuleForBroadcastMode(OpenstackNode osNode, boolean install) {
            Set<String> netIds = osNetworkService.networks().stream()
                    .map(IdEntity::getId).collect(Collectors.toSet());

            netIds.stream()
                    .filter(nid -> osNetworkService.networkType(nid) == VXLAN ||
                            osNetworkService.networkType(nid) == GRE ||
                            osNetworkService.networkType(nid) == GENEVE)
                    .forEach(nid -> {
                        String segId = osNetworkService.segmentId(nid);
                        setBaseVnetArpRuleForBroadcastMode(osNode, segId, nid, true, install);
                    });

            netIds.stream()
                    .filter(nid -> osNetworkService.networkType(nid) == VLAN)
                    .forEach(nid -> {
                        String segId = osNetworkService.segmentId(nid);
                        setBaseVnetArpRuleForBroadcastMode(osNode, segId, nid, false, install);
                    });
        }

        private void setAllArpRules(OpenstackNode osNode, boolean install) {
            if (ARP_BROADCAST_MODE.equals(getArpMode())) {
                instancePortService.instancePorts().stream()
                        .filter(p -> p.state() == ACTIVE)
                        .filter(p -> p.deviceId().equals(osNode.intgBridge()))
                        .forEach(p -> {
                            setArpRequestRule(p, install);
                            setArpReplyRule(p, install);
                        });
            } else {
                // we do nothing for proxy mode
            }
        }
    }

    /**
     * An internal instance port listener which listens the port events generated
     * from VM. When ARP a host which located in a remote compute node, we specify
     * both ARP OP mode as REQUEST and Target Protocol Address (TPA) with
     * host IP address. When ARP a host which located in a local compute node,
     * we specify only ARP OP mode as REQUEST.
     */
    private class InternalInstancePortListener implements InstancePortListener {

        @Override
        public boolean isRelevant(InstancePortEvent event) {
            return ARP_BROADCAST_MODE.equals(getArpMode());
        }

        private boolean isRelevantHelper(InstancePortEvent event) {
            return mastershipService.isLocalMaster(event.subject().deviceId());
        }

        @Override
        public void event(InstancePortEvent event) {
            switch (event.type()) {
                case OPENSTACK_INSTANCE_PORT_DETECTED:
                case OPENSTACK_INSTANCE_PORT_UPDATED:
                case OPENSTACK_INSTANCE_MIGRATION_STARTED:
                    eventExecutor.execute(() -> processInstanceMigrationStart(event));
                    break;
                case OPENSTACK_INSTANCE_PORT_VANISHED:
                    eventExecutor.execute(() -> processInstanceRemoval(event));
                    break;
                case OPENSTACK_INSTANCE_MIGRATION_ENDED:
                    eventExecutor.execute(() -> processInstanceMigrationEnd(event));
                    break;
                default:
                    break;
            }
        }

        private void processInstanceMigrationStart(InstancePortEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            setArpRequestRule(event.subject(), true);
            setArpReplyRule(event.subject(), true);
        }

        private void processInstanceMigrationEnd(InstancePortEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            InstancePort revisedInstPort = swapStaleLocation(event.subject());
            setArpRequestRule(revisedInstPort, false);
        }

        private void processInstanceRemoval(InstancePortEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            setArpRequestRule(event.subject(), false);
            setArpReplyRule(event.subject(), false);
        }
    }
}