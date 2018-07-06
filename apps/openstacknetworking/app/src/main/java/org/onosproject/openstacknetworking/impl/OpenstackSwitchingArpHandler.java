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

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ARP;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortEvent;
import org.onosproject.openstacknetworking.api.InstancePortListener;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkEvent;
import org.onosproject.openstacknetworking.api.OpenstackNetworkListener;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.NetworkType;
import org.openstack4j.model.network.Subnet;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.openstacknetworking.api.Constants.ARP_BROADCAST_MODE;
import static org.onosproject.openstacknetworking.api.Constants.ARP_PROXY_MODE;
import static org.onosproject.openstacknetworking.api.Constants.DEFAULT_ARP_MODE_STR;
import static org.onosproject.openstacknetworking.api.Constants.DEFAULT_GATEWAY_MAC_STR;
import static org.onosproject.openstacknetworking.api.Constants.DHCP_ARP_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ARP_CONTROL_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ARP_GATEWAY_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ARP_REPLY_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ARP_REQUEST_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ARP_SUBNET_RULE;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.buildExtension;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;

/**
 * Handles ARP packet from VMs.
 */
@Component(immediate = true)
public final class OpenstackSwitchingArpHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String GATEWAY_MAC = "gatewayMac";
    private static final String ARP_MODE = "arpMode";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    OpenstackFlowRuleService osFlowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService osNodeService;

    @Property(name = GATEWAY_MAC, value = DEFAULT_GATEWAY_MAC_STR,
            label = "Fake MAC address for virtual network subnet gateway")
    private String gatewayMac = DEFAULT_GATEWAY_MAC_STR;

    @Property(name = ARP_MODE, value = DEFAULT_ARP_MODE_STR,
            label = "ARP processing mode, broadcast | proxy (default)")
    protected String arpMode = DEFAULT_ARP_MODE_STR;

    private final InternalPacketProcessor packetProcessor = new InternalPacketProcessor();
    private final InternalOpenstackNetworkListener osNetworkListener =
            new InternalOpenstackNetworkListener();
    private final InstancePortListener instancePortListener = new InternalInstancePortListener();
    private final OpenstackNodeListener osNodeListener = new InternalNodeEventListener();

    private final Set<IpAddress> gateways = Sets.newConcurrentHashSet();

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

        osNetworkService.networks().forEach(n -> {
            if (n.getNetworkType() != NetworkType.FLAT) {
                osNetworkService.subnets().forEach(s -> {
                    if (s.getNetworkId().equals(n.getId())) {
                        addSubnetGateway(s);
                    }
                });
            }
        });

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

        log.info("Stopped");
    }

    @Modified
    void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        String updatedMac;

        updatedMac = Tools.get(properties, GATEWAY_MAC);
        if (!Strings.isNullOrEmpty(updatedMac) && !updatedMac.equals(gatewayMac)) {
            gatewayMac = updatedMac;
        }

        String updateArpMode;

        updateArpMode = Tools.get(properties, ARP_MODE);
        if (!Strings.isNullOrEmpty(updateArpMode) && !updateArpMode.equals(arpMode)) {
            arpMode = updateArpMode;
        }

        log.info("Modified");
    }

    private void addSubnetGateway(Subnet osSubnet) {
        if (Strings.isNullOrEmpty(osSubnet.getGateway())) {
            return;
        }
        IpAddress gatewayIp = IpAddress.valueOf(osSubnet.getGateway());
        gateways.add(gatewayIp);
        log.debug("Added ARP proxy entry IP:{}", gatewayIp);
    }

    private void removeSubnetGateway(Subnet osSubnet) {
        if (Strings.isNullOrEmpty(osSubnet.getGateway())) {
            return;
        }
        IpAddress gatewayIp = IpAddress.valueOf(osSubnet.getGateway());
        gateways.remove(gatewayIp);
        log.debug("Removed ARP proxy entry IP:{}", gatewayIp);
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
        if (arpMode.equals(ARP_BROADCAST_MODE)) {
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
        MacAddress replyMac = gateways.contains(targetIp) ? MacAddress.valueOf(gatewayMac) :
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
     * @param install   flag which indicates whether to install rule or remove rule
     */
    private void setFakeGatewayArpRule(Subnet osSubnet, boolean install, OpenstackNode osNode) {

        if (arpMode.equals(ARP_BROADCAST_MODE)) {
            String gateway = osSubnet.getGateway();

            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                    .matchArpOp(ARP.OP_REQUEST)
                    .matchArpTpa(Ip4Address.valueOf(gateway))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setArpOp(ARP.OP_REPLY)
                    .setArpSha(MacAddress.valueOf(gatewayMac))
                    .setArpSpa(Ip4Address.valueOf(gateway))
                    .setOutput(PortNumber.IN_PORT)
                    .build();

            if (osNode == null) {
                osNodeService.completeNodes(COMPUTE).forEach(n ->
                        osFlowRuleService.setRule(
                                appId,
                                n.intgBridge(),
                                selector,
                                treatment,
                                PRIORITY_ARP_GATEWAY_RULE,
                                DHCP_ARP_TABLE,
                                install
                        )
                );
            } else {
                osFlowRuleService.setRule(
                        appId,
                        osNode.intgBridge(),
                        selector,
                        treatment,
                        PRIORITY_ARP_GATEWAY_RULE,
                        DHCP_ARP_TABLE,
                        install
                );
            }

        }
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
            processPacketIn(context, ethPacket);
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
            Subnet osSubnet = event.subnet();
            if (osSubnet == null) {
                return false;
            }

            Network network = osNetworkService.network(osSubnet.getNetworkId());

            if (network == null) {
                log.warn("Network is not specified.");
                return false;
            } else {
                if (network.getNetworkType().equals(NetworkType.FLAT)) {
                    return false;
                }
            }

            // do not allow to proceed without leadership
            NodeId leader = leadershipService.getLeader(appId.name());
            if (!Objects.equals(localNodeId, leader)) {
                return false;
            }

            return !Strings.isNullOrEmpty(osSubnet.getGateway());
        }

        @Override
        public void event(OpenstackNetworkEvent event) {
            switch (event.type()) {
                case OPENSTACK_SUBNET_CREATED:
                case OPENSTACK_SUBNET_UPDATED:
                    addSubnetGateway(event.subnet());
                    setFakeGatewayArpRule(event.subnet(), true, null);
                    break;
                case OPENSTACK_SUBNET_REMOVED:
                    removeSubnetGateway(event.subnet());
                    setFakeGatewayArpRule(event.subnet(), false, null);
                    break;
                case OPENSTACK_NETWORK_CREATED:
                case OPENSTACK_NETWORK_UPDATED:
                case OPENSTACK_NETWORK_REMOVED:
                case OPENSTACK_PORT_CREATED:
                case OPENSTACK_PORT_UPDATED:
                case OPENSTACK_PORT_REMOVED:
                default:
                    // do nothing for the other events
                    break;
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
            // do not allow to proceed without leadership
            NodeId leader = leadershipService.getLeader(appId.name());
            return Objects.equals(localNodeId, leader) && event.subject().type() == COMPUTE;
        }

        @Override
        public void event(OpenstackNodeEvent event) {
            OpenstackNode osNode = event.subject();
            switch (event.type()) {
                case OPENSTACK_NODE_COMPLETE:
                    setDefaultArpRule(osNode, true);
                    break;
                case OPENSTACK_NODE_INCOMPLETE:
                    setDefaultArpRule(osNode, false);
                    break;
                default:
                    break;
            }
        }

        private void setDefaultArpRule(OpenstackNode osNode, boolean install) {
            switch (arpMode) {
                case ARP_PROXY_MODE:
                    setDefaultArpRuleForProxyMode(osNode, install);
                    break;
                case ARP_BROADCAST_MODE:
                    setDefaultArpRuleForBroadcastMode(osNode, install);

                    // we do not add fake gateway ARP rules for FLAT network
                    // ARP packets generated by FLAT typed VM should not be
                    // delegated to switch to handle
                    osNetworkService.subnets().stream().filter(subnet ->
                            osNetworkService.network(subnet.getNetworkId()) != null &&
                                    osNetworkService.network(subnet.getNetworkId())
                                            .getNetworkType() == NetworkType.FLAT)
                                            .forEach(subnet ->
                        setFakeGatewayArpRule(subnet, install, osNode));
                    break;
                default:
                    log.warn("Invalid ARP mode {}. Please use either " +
                            "broadcast or proxy mode.", arpMode);
                    break;
            }
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
                    DHCP_ARP_TABLE,
                    install
            );
        }

        private void setDefaultArpRuleForBroadcastMode(OpenstackNode osNode, boolean install) {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                    .matchArpOp(ARP.OP_REQUEST)
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.FLOOD)
                    .build();

            osFlowRuleService.setRule(
                    appId,
                    osNode.intgBridge(),
                    selector,
                    treatment,
                    PRIORITY_ARP_SUBNET_RULE,
                    DHCP_ARP_TABLE,
                    install
            );
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

            if (arpMode.equals(ARP_PROXY_MODE)) {
                return false;
            }

            InstancePort instPort = event.subject();
            return mastershipService.isLocalMaster(instPort.deviceId());
        }

        @Override
        public void event(InstancePortEvent event) {
            switch (event.type()) {
                case OPENSTACK_INSTANCE_PORT_UPDATED:
                case OPENSTACK_INSTANCE_PORT_DETECTED:
                    setArpRequestRule(event.subject(), true);
                    setArpReplyRule(event.subject(), true);
                    break;
                case OPENSTACK_INSTANCE_PORT_VANISHED:
                    setArpRequestRule(event.subject(), false);
                    setArpReplyRule(event.subject(), false);
                    break;
                case OPENSTACK_INSTANCE_MIGRATION_ENDED:
                    setArpRequestRule(event.subject(), false);
                    break;
                default:
                    break;
            }
        }

        /**
         * Installs flow rules to match ARP request packets.
         *
         * @param port      instance port
         * @param install   installation flag
         */
        private void setArpRequestRule(InstancePort port, boolean install) {
            NetworkType type = osNetworkService.network(port.networkId()).getNetworkType();

            switch (type) {
                case VXLAN:
                    setRemoteArpRequestRuleForVxlan(port, install);
                    break;
                case VLAN:
                    // since VLAN ARP packet can be broadcasted to all hosts that connected with L2 network,
                    // there is no need to add any flow rules to handle ARP request
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
            NetworkType type = osNetworkService.network(port.networkId()).getNetworkType();

            switch (type) {
                case VXLAN:
                    setArpReplyRuleForVxlan(port, install);
                    break;
                case VLAN:
                    setArpReplyRuleForVlan(port, install);
                    break;
                default:
                    break;
            }
        }

        /**
         * Installs flow rules to match ARP request packets only for VxLAN.
         *
         * @param port      instance port
         * @param install   installation flag
         */
        private void setRemoteArpRequestRuleForVxlan(InstancePort port, boolean install) {

            OpenstackNode localNode = osNodeService.node(port.deviceId());

            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                    .matchArpOp(ARP.OP_REQUEST)
                    .matchArpTpa(port.ipAddress().getIp4Address())
                    .build();

            setRemoteArpTreatmentForVxlan(selector, port, localNode, install);
        }

        /**
         * Installs flow rules to match ARP reply packets only for VxLAN.
         *
         * @param port      instance port
         * @param install   installation flag
         */
        private void setArpReplyRuleForVxlan(InstancePort port, boolean install) {

            OpenstackNode localNode = osNodeService.node(port.deviceId());

            TrafficSelector selector = setArpReplyRuleForVnet(port, install);
            setRemoteArpTreatmentForVxlan(selector, port, localNode, install);
        }

        /**
         * Installs flow rules to match ARP reply packets only for VLAN.
         *
         * @param port      instance port
         * @param install   installation flag
         */
        private void setArpReplyRuleForVlan(InstancePort port, boolean install) {

            TrafficSelector selector = setArpReplyRuleForVnet(port, install);
            setRemoteArpTreatmentForVlan(selector, port, install);
        }

        // a helper method
        private TrafficSelector setArpReplyRuleForVnet(InstancePort port, boolean install) {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                    .matchArpOp(ARP.OP_REPLY)
                    .matchArpTpa(port.ipAddress().getIp4Address())
                    .matchArpTha(port.macAddress())
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(port.portNumber())
                    .build();

            osFlowRuleService.setRule(
                    appId,
                    port.deviceId(),
                    selector,
                    treatment,
                    PRIORITY_ARP_REPLY_RULE,
                    DHCP_ARP_TABLE,
                    install
            );

            return selector;
        }

        // a helper method
        private void setRemoteArpTreatmentForVxlan(TrafficSelector selector,
                                                   InstancePort port,
                                                   OpenstackNode localNode,
                                                   boolean install) {
            for (OpenstackNode remoteNode : osNodeService.completeNodes(COMPUTE)) {
                if (!remoteNode.intgBridge().equals(port.deviceId())) {
                    TrafficTreatment treatmentToRemote = DefaultTrafficTreatment.builder()
                            .extension(buildExtension(
                                    deviceService,
                                    remoteNode.intgBridge(),
                                    localNode.dataIp().getIp4Address()),
                                    remoteNode.intgBridge())
                            .setOutput(remoteNode.tunnelPortNum())
                            .build();

                    osFlowRuleService.setRule(
                            appId,
                            remoteNode.intgBridge(),
                            selector,
                            treatmentToRemote,
                            PRIORITY_ARP_REQUEST_RULE,
                            DHCP_ARP_TABLE,
                            install
                    );
                }
            }
        }

        // a helper method
        private void setRemoteArpTreatmentForVlan(TrafficSelector selector,
                                                  InstancePort port,
                                                  boolean install) {
            for (OpenstackNode remoteNode : osNodeService.completeNodes(COMPUTE)) {
                if (!remoteNode.intgBridge().equals(port.deviceId()) && remoteNode.vlanIntf() != null) {
                    TrafficTreatment treatmentToRemote = DefaultTrafficTreatment.builder()
                            .setOutput(remoteNode.vlanPortNum())
                            .build();

                    osFlowRuleService.setRule(
                            appId,
                            remoteNode.intgBridge(),
                            selector,
                            treatmentToRemote,
                            PRIORITY_ARP_REQUEST_RULE,
                            DHCP_ARP_TABLE,
                            install);
                }
            }
        }
    }
}
