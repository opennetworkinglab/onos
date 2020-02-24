/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.impl;

import com.google.common.base.Strings;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP;
import org.onlab.packet.ICMPEcho;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.ExternalPeerRouter;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.Subnet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.packet.ICMP.TYPE_ECHO_REPLY;
import static org.onlab.packet.ICMP.TYPE_ECHO_REQUEST;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.DEFAULT_GATEWAY_MAC;
import static org.onosproject.openstacknetworking.api.Constants.GW_COMMON_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_INTERNAL_ROUTING_RULE;
import static org.onosproject.openstacknetworking.impl.OsgiPropertyConstants.USE_STATEFUL_SNAT;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.externalGatewayIp;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.externalPeerRouterFromSubnet;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getPropertyValueAsBoolean;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getRouterFromSubnet;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.GATEWAY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles ICMP packet received from a gateway node.
 * For a request for virtual network subnet gateway, it generates fake ICMP reply.
 * For a request for the external network, it does source NAT with the public IP and
 * forward the request to the external only if the requested virtual subnet has
 * external connectivity.
 */
@Component(immediate = true)
public class OpenstackRoutingSnatIcmpHandler {

    protected final Logger log = getLogger(getClass());

    private static final String ERR_REQ = "Failed to handle ICMP request: ";
    private static final String ERR_DUPLICATE = " already exists";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackRouterService osRouterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackFlowRuleService osFlowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final InternalPacketProcessor packetProcessor = new InternalPacketProcessor();
    private ConsistentMap<String, InstancePort> icmpInfoMap;
    private final OpenstackNodeListener osNodeListener = new InternalNodeEventListener();

    private static final KryoNamespace SERIALIZER_ICMP_MAP = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(InstancePort.class)
            .register(DefaultInstancePort.class)
            .register(InstancePort.State.class)
            .build();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        packetService.addProcessor(packetProcessor, PacketProcessor.director(1));
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        osNodeService.addListener(osNodeListener);

        icmpInfoMap = storageService.<String, InstancePort>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_ICMP_MAP))
                .withName("openstack-icmpmap")
                .withApplicationId(appId)
                .build();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(packetProcessor);
        eventExecutor.shutdown();
        leadershipService.withdraw(appId.name());
        osNodeService.removeListener(osNodeListener);

        log.info("Stopped");
    }

    private boolean getStatefulSnatFlag() {
        Set<ConfigProperty> properties = configService.getProperties(OpenstackRoutingSnatHandler.class.getName());
        return getPropertyValueAsBoolean(properties, USE_STATEFUL_SNAT);
    }

    private class InternalNodeEventListener implements OpenstackNodeListener {
        @Override
        public boolean isRelevant(OpenstackNodeEvent event) {
            return event.subject().type() == GATEWAY;
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

            setIcmpReplyRules(osNode.intgBridge(), true);
        }

        private void processNodeInCompletion(OpenstackNode osNode) {
            if (!isRelevantHelper()) {
                return;
            }

            setIcmpReplyRules(osNode.intgBridge(), false);
        }

        private void setIcmpReplyRules(DeviceId deviceId, boolean install) {
            // Sends ICMP response to controller for SNATing ingress traffic
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPProtocol(IPv4.PROTOCOL_ICMP)
                    .matchIcmpType(ICMP.TYPE_ECHO_REPLY)
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .punt()
                    .build();

            osFlowRuleService.setRule(
                    appId,
                    deviceId,
                    selector,
                    treatment,
                    PRIORITY_INTERNAL_ROUTING_RULE,
                    GW_COMMON_TABLE,
                    install);
        }
    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            }

            eventExecutor.execute(() -> {

                if (!isRelevantHelper(context)) {
                    return;
                }

                InboundPacket pkt = context.inPacket();
                Ethernet ethernet = pkt.parsed();
                if (ethernet == null || ethernet.getEtherType() != Ethernet.TYPE_IPV4) {
                    return;
                }

                IPv4 iPacket = (IPv4) ethernet.getPayload();

                if (iPacket.getProtocol() == IPv4.PROTOCOL_ICMP) {
                    processIcmpPacket(context, ethernet);
                }
            });
        }

        private boolean isRelevantHelper(PacketContext context) {
            Set<DeviceId> gateways = osNodeService.completeNodes(GATEWAY)
                    .stream().map(OpenstackNode::intgBridge)
                    .collect(Collectors.toSet());

            return gateways.contains(context.inPacket().receivedFrom().deviceId());
        }

        private void processIcmpPacket(PacketContext context, Ethernet ethernet) {
            IPv4 ipPacket = (IPv4) ethernet.getPayload();
            ICMP icmp = (ICMP) ipPacket.getPayload();
            log.trace("Processing ICMP packet source MAC:{}, source IP:{}," +
                            "dest MAC:{}, dest IP:{}",
                    ethernet.getSourceMAC(),
                    IpAddress.valueOf(ipPacket.getSourceAddress()),
                    ethernet.getDestinationMAC(),
                    IpAddress.valueOf(ipPacket.getDestinationAddress()));

            switch (icmp.getIcmpType()) {
                case TYPE_ECHO_REQUEST:
                    if (handleEchoRequest(context.inPacket().receivedFrom().deviceId(),
                            ethernet.getSourceMAC(), ipPacket, icmp)) {
                        context.block();
                    }
                    break;
                case TYPE_ECHO_REPLY:
                    if (handleEchoReply(ipPacket, icmp)) {
                        context.block();
                    }
                    break;
                default:
                    break;
            }
        }

        private boolean handleEchoRequest(DeviceId srcDevice, MacAddress srcMac, IPv4 ipPacket,
                                          ICMP icmp) {
            // we only handles a request from an instance port
            // in case of ehco request to SNAT ip address from an external router,
            // we intentionally ignore it
            InstancePort instPort = instancePortService.instancePort(srcMac);
            if (instPort == null) {
                log.warn(ERR_REQ + "unknown source host(MAC:{})", srcMac);
                return false;
            }

            IpAddress srcIp = IpAddress.valueOf(ipPacket.getSourceAddress());
            IpAddress dstIp = IpAddress.valueOf(ipPacket.getDestinationAddress());

            Subnet srcSubnet = getSourceSubnet(instPort);
            if (srcSubnet == null) {
                log.warn(ERR_REQ + "unknown source subnet(IP:{})", srcIp);
                return false;
            }

            if (Strings.isNullOrEmpty(srcSubnet.getGateway())) {
                log.warn(ERR_REQ + "source subnet(ID:{}, CIDR:{}) has no gateway",
                        srcSubnet.getId(), srcSubnet.getCidr());
                return false;
            }

            if (isForSubnetGateway(IpAddress.valueOf(ipPacket.getDestinationAddress()),
                    srcSubnet)) {
                // this is a request to a subnet gateway
                log.trace("Icmp request to gateway {} from {}", dstIp, srcIp);
                processRequestForGateway(ipPacket, instPort);
            } else {
                // this is a request to an external network
                log.trace("Icmp request to external {} from {}", dstIp, srcIp);

                Router osRouter = getRouterFromSubnet(srcSubnet, osRouterService);

                if (osRouter == null || osRouter.getExternalGatewayInfo() == null) {
                    // this router does not have external connectivity
                    log.warn("No router is associated with the given subnet {}", srcSubnet);
                    return false;
                }

                IpAddress externalGatewayIp = externalGatewayIp(osRouter, osNetworkService);

                if (externalGatewayIp == null) {
                    log.warn(ERR_REQ + "failed to get external ip");
                    return false;
                }

                ExternalPeerRouter externalPeerRouter =
                        externalPeerRouterFromSubnet(srcSubnet,
                                            osRouterService, osNetworkService);
                if (externalPeerRouter == null) {
                    log.warn(ERR_REQ + "failed to get external peer router");
                    return false;
                }

                String icmpInfoKey = icmpInfoKey(icmp, externalGatewayIp.toString(),
                        IPv4.fromIPv4Address(ipPacket.getDestinationAddress()));
                log.trace("Created icmpInfo key is {}", icmpInfoKey);

                sendRequestForExternal(ipPacket, srcDevice, externalGatewayIp, externalPeerRouter);

                try {
                    icmpInfoMap.compute(icmpInfoKey, (id, existing) -> {
                        checkArgument(existing == null, ERR_DUPLICATE);
                        return instPort;
                    });
                } catch (IllegalArgumentException e) {
                    log.warn("IllegalArgumentException occurred because of {}", e.toString());
                    return false;
                }
            }
            return true;
        }

        private String icmpInfoKey(ICMP icmp, String srcIp, String dstIp) {
            return String.valueOf(getIcmpId(icmp))
                    .concat(srcIp)
                    .concat(dstIp);
        }

        private boolean handleEchoReply(IPv4 ipPacket, ICMP icmp) {
            String icmpInfoKey = icmpInfoKey(icmp,
                    IPv4.fromIPv4Address(ipPacket.getDestinationAddress()),
                    IPv4.fromIPv4Address(ipPacket.getSourceAddress()));
            log.trace("Retrieved icmpInfo key is {}", icmpInfoKey);

            if (icmpInfoMap.get(icmpInfoKey) != null) {
                processReplyFromExternal(ipPacket, icmpInfoMap.get(icmpInfoKey).value());
                icmpInfoMap.remove(icmpInfoKey);
                return true;
            } else {
                log.debug("No ICMP Info for ICMP packet");
                return false;
            }
        }

        private Subnet getSourceSubnet(InstancePort instance) {
            checkNotNull(instance);

            Port osPort = osNetworkService.port(instance.portId());
            return osNetworkService.subnets(osPort.getNetworkId())
                    .stream().findAny().orElse(null);
        }

        private boolean isForSubnetGateway(IpAddress dstIp, Subnet srcSubnet) {
            RouterInterface osRouterIface = osRouterService.routerInterfaces().stream()
                    .filter(i -> Objects.equals(i.getSubnetId(), srcSubnet.getId()))
                    .findAny().orElse(null);
            if (osRouterIface == null) {
                log.trace(ERR_REQ + "source subnet(ID:{}, CIDR:{}) has no router",
                        srcSubnet.getId(), srcSubnet.getCidr());
                return false;
            }

            Router osRouter = osRouterService.router(osRouterIface.getId());
            Set<IpAddress> routableGateways =
                    osRouterService.routerInterfaces(osRouter.getId())
                    .stream()
                    .map(iface -> osNetworkService.subnet(iface.getSubnetId()).getGateway())
                    .map(IpAddress::valueOf)
                    .collect(Collectors.toSet());

            return routableGateways.contains(dstIp);
        }

        private void processRequestForGateway(IPv4 ipPacket, InstancePort instPort) {
            ICMP icmpReq = (ICMP) ipPacket.getPayload();
            icmpReq.setChecksum((short) 0);
            icmpReq.setIcmpType(TYPE_ECHO_REPLY);

            int destinationAddress = ipPacket.getSourceAddress();

            ipPacket.setSourceAddress(ipPacket.getDestinationAddress())
                    .setDestinationAddress(destinationAddress)
                    .resetChecksum();

            ipPacket.setPayload(icmpReq);
            Ethernet icmpReply = new Ethernet();
            icmpReply.setEtherType(Ethernet.TYPE_IPV4)
                    .setSourceMACAddress(Constants.DEFAULT_GATEWAY_MAC)
                    .setDestinationMACAddress(instPort.macAddress())
                    .setPayload(ipPacket);

            sendReply(icmpReply, instPort);
        }

        private void sendRequestForExternal(IPv4 ipPacket,
                                            DeviceId srcDevice,
                                            IpAddress srcNatIp,
                                            ExternalPeerRouter externalPeerRouter) {
            ICMP icmpReq = (ICMP) ipPacket.getPayload();
            icmpReq.resetChecksum();
            ipPacket.setSourceAddress(srcNatIp.getIp4Address().toInt()).resetChecksum();
            ipPacket.setPayload(icmpReq);

            Ethernet icmpRequestEth = new Ethernet();
            icmpRequestEth.setEtherType(Ethernet.TYPE_IPV4)
                    .setSourceMACAddress(DEFAULT_GATEWAY_MAC)
                    .setDestinationMACAddress(externalPeerRouter.macAddress());

            if (!externalPeerRouter.vlanId().equals(VlanId.NONE)) {
                icmpRequestEth.setVlanID(externalPeerRouter.vlanId().toShort());
            }

            icmpRequestEth.setPayload(ipPacket);

            OpenstackNode osNode = osNodeService.node(srcDevice);
            if (osNode == null) {
                final String error = String.format("Cannot find openstack node for %s",
                        srcDevice);
                throw new IllegalStateException(error);
            }
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(osNode.uplinkPortNum())
                    .build();

            OutboundPacket packet = new DefaultOutboundPacket(
                    srcDevice,
                    treatment,
                    ByteBuffer.wrap(icmpRequestEth.serialize()));

            packetService.emit(packet);
        }

        private void processReplyFromExternal(IPv4 ipPacket, InstancePort instPort) {

            if (instPort.networkId() == null) {
                return;
            }

            ICMP icmpReply = (ICMP) ipPacket.getPayload();

            icmpReply.resetChecksum();

            ipPacket.setDestinationAddress(instPort.ipAddress().getIp4Address().toInt())
                    .resetChecksum();
            ipPacket.setPayload(icmpReply);

            Ethernet icmpResponseEth = new Ethernet();
            icmpResponseEth.setEtherType(Ethernet.TYPE_IPV4)
                    .setSourceMACAddress(Constants.DEFAULT_GATEWAY_MAC)
                    .setDestinationMACAddress(instPort.macAddress())
                    .setPayload(ipPacket);

            sendReply(icmpResponseEth, instPort);
        }

        private void sendReply(Ethernet icmpReply, InstancePort instPort) {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                    .setOutput(instPort.portNumber());

            String netId = instPort.networkId();
            String segId = osNetworkService.segmentId(netId);

            switch (osNetworkService.networkType(netId)) {
                case VXLAN:
                case GRE:
                case GENEVE:
                    tBuilder.setTunnelId(Long.valueOf(segId));
                    break;
                case VLAN:
                    tBuilder.setVlanId(VlanId.vlanId(segId));
                    break;
                default:
                    break;
            }

            OutboundPacket packet = new DefaultOutboundPacket(
                    instPort.deviceId(),
                    tBuilder.build(),
                    ByteBuffer.wrap(icmpReply.serialize()));

            packetService.emit(packet);
        }

        private short getIcmpId(ICMP icmp) {
            return ((ICMPEcho) icmp.getPayload()).getIdentifier();
        }
    }
}
