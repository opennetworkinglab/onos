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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultTrafficTreatment;
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
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.openstack4j.model.network.ExternalGateway;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.openstack.networking.domain.NeutronIP;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.DEFAULT_GATEWAY_MAC;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
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
public class OpenstackRoutingIcmpHandler {

    protected final Logger log = getLogger(getClass());

    private static final String ERR_REQ = "Failed to handle ICMP request: ";
    private static final String ERR_DUPLICATE = " already exists";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackRouterService osRouterService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final InternalPacketProcessor packetProcessor = new InternalPacketProcessor();
    private ConsistentMap<String, InstancePort> icmpInfoMap;

    private static final KryoNamespace SERIALIZER_ICMP_MAP = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(InstancePort.class)
            .register(HostBasedInstancePort.class)
            .build();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        packetService.addProcessor(packetProcessor, PacketProcessor.director(1));

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

        log.info("Stopped");
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
            case ICMP.TYPE_ECHO_REQUEST:
                handleEchoRequest(
                        context.inPacket().receivedFrom().deviceId(),
                        ethernet.getSourceMAC(),
                        ipPacket,
                        icmp);
                context.block();
                break;
            case ICMP.TYPE_ECHO_REPLY:
                handleEchoReply(ipPacket, icmp);
                context.block();
                break;
            default:
                break;
        }
    }

    private void handleEchoRequest(DeviceId srcDevice, MacAddress srcMac, IPv4 ipPacket,
                                   ICMP icmp) {
        InstancePort instPort = instancePortService.instancePort(srcMac);
        if (instPort == null) {
            log.info(ERR_REQ + "unknown source host(MAC:{})", srcMac);
            return;
        }

        IpAddress srcIp = IpAddress.valueOf(ipPacket.getSourceAddress());
        Subnet srcSubnet = getSourceSubnet(instPort, srcIp);
        if (srcSubnet == null) {
            log.info(ERR_REQ + "unknown source subnet(IP:{})", srcIp);
            return;
        }
        if (Strings.isNullOrEmpty(srcSubnet.getGateway())) {
            log.info(ERR_REQ + "source subnet(ID:{}, CIDR:{}) has no gateway",
                    srcSubnet.getId(), srcSubnet.getCidr());
            return;
        }

        if (isForSubnetGateway(IpAddress.valueOf(ipPacket.getDestinationAddress()),
                srcSubnet)) {
            // this is a request for the subnet gateway
            processRequestForGateway(ipPacket, instPort);
        } else {
            ExternalPeerRouter externalPeerRouter = externalPeerRouter(srcSubnet);
            if (externalPeerRouter == null) {
                log.info(ERR_REQ + "failed to get external peer router");
                return;
            }
            // this is a request for the external network
            IpAddress externalIp = getExternalIp(srcSubnet);
            if (externalIp == null) {
                return;
            }

            sendRequestForExternal(ipPacket, srcDevice, externalIp, externalPeerRouter);
            String icmpInfoKey = String.valueOf(getIcmpId(icmp))
                    .concat(String.valueOf(externalIp.getIp4Address().toInt()))
                    .concat(String.valueOf(ipPacket.getDestinationAddress()));
            try {
                icmpInfoMap.compute(icmpInfoKey, (id, existing) -> {
                    checkArgument(existing == null, ERR_DUPLICATE);
                    return instPort;
                });
            } catch (IllegalArgumentException e) {
                log.warn("Exception occurred because of {}", e.toString());
            }

        }
    }

    private ExternalPeerRouter externalPeerRouter(Subnet subnet) {
        RouterInterface osRouterIface = osRouterService.routerInterfaces().stream()
                .filter(i -> Objects.equals(i.getSubnetId(), subnet.getId()))
                .findAny().orElse(null);
        if (osRouterIface == null) {
            return null;
        }

        Router osRouter = osRouterService.router(osRouterIface.getId());
        if (osRouter == null) {
            return null;
        }
        if (osRouter.getExternalGatewayInfo() == null) {
            return null;
        }

        ExternalGateway exGatewayInfo = osRouter.getExternalGatewayInfo();

        return osNetworkService.externalPeerRouter(exGatewayInfo);
    }

    private void handleEchoReply(IPv4 ipPacket, ICMP icmp) {
        String icmpInfoKey = String.valueOf(getIcmpId(icmp))
                .concat(String.valueOf(ipPacket.getDestinationAddress()))
                .concat(String.valueOf(ipPacket.getSourceAddress()));

        if (icmpInfoMap.get(icmpInfoKey) != null) {
            processReplyFromExternal(ipPacket, icmpInfoMap.get(icmpInfoKey).value());
            icmpInfoMap.remove(icmpInfoKey);
        } else {
            log.warn("No ICMP Info for ICMP packet");
        }
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
        Set<IpAddress> routableGateways = osRouterService.routerInterfaces(osRouter.getId())
                .stream()
                .map(iface -> osNetworkService.subnet(iface.getSubnetId()).getGateway())
                .map(IpAddress::valueOf)
                .collect(Collectors.toSet());

        return routableGateways.contains(dstIp);
    }

    private IpAddress getExternalIp(Subnet srcSubnet) {
        RouterInterface osRouterIface = osRouterService.routerInterfaces().stream()
                .filter(i -> Objects.equals(i.getSubnetId(), srcSubnet.getId()))
                .findAny().orElse(null);
        if (osRouterIface == null) {
            final String error = String.format(ERR_REQ +
                    "subnet(ID:%s, CIDR:%s) is not connected to any router",
                    srcSubnet.getId(), srcSubnet.getCidr());
            throw new IllegalStateException(error);
        }

        Router osRouter = osRouterService.router(osRouterIface.getId());
        if (osRouter.getExternalGatewayInfo() == null) {
            final String error = String.format(ERR_REQ +
                    "router(ID:%s, name:%s) does not have external gateway",
                    osRouter.getId(), osRouter.getName());
            throw new IllegalStateException(error);
        }

        // TODO fix openstack4j for ExternalGateway provides external fixed IP list
        ExternalGateway exGatewayInfo = osRouter.getExternalGatewayInfo();
        Port exGatewayPort = osNetworkService.ports(exGatewayInfo.getNetworkId())
                .stream()
                .filter(port -> Objects.equals(port.getDeviceId(), osRouter.getId()))
                .findAny().orElse(null);
        if (exGatewayPort == null) {
            final String error = String.format(ERR_REQ +
                    "no external gateway port for router (ID:%s, name:%s)",
                    osRouter.getId(), osRouter.getName());
            throw new IllegalStateException(error);
        }
        Optional<NeutronIP> externalIpAddress = (Optional<NeutronIP>) exGatewayPort.getFixedIps().stream().findFirst();
        if (!externalIpAddress.isPresent() || externalIpAddress.get().getIpAddress() == null) {
            final String error = String.format(ERR_REQ +
                            "no external gateway IP address for router (ID:%s, name:%s)",
                    osRouter.getId(), osRouter.getName());
            throw new IllegalStateException(error);
        }

        return IpAddress.valueOf(externalIpAddress.get().getIpAddress());
    }

    private void processRequestForGateway(IPv4 ipPacket, InstancePort instPort) {
        ICMP icmpReq = (ICMP) ipPacket.getPayload();
        icmpReq.setChecksum((short) 0);
        icmpReq.setIcmpType(ICMP.TYPE_ECHO_REPLY).resetChecksum();

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

    private void sendRequestForExternal(IPv4 ipPacket, DeviceId srcDevice,
                                        IpAddress srcNatIp, ExternalPeerRouter externalPeerRouter) {
        ICMP icmpReq = (ICMP) ipPacket.getPayload();
        icmpReq.resetChecksum();
        ipPacket.setSourceAddress(srcNatIp.getIp4Address().toInt()).resetChecksum();
        ipPacket.setPayload(icmpReq);

        Ethernet icmpRequestEth = new Ethernet();
        icmpRequestEth.setEtherType(Ethernet.TYPE_IPV4)
                .setSourceMACAddress(DEFAULT_GATEWAY_MAC)
                .setDestinationMACAddress(externalPeerRouter.externalPeerRouterMac());

        if (!externalPeerRouter.externalPeerRouterVlanId().equals(VlanId.NONE)) {
            icmpRequestEth.setVlanID(externalPeerRouter.externalPeerRouterVlanId().toShort());
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
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(instPort.portNumber())
                .build();

        OutboundPacket packet = new DefaultOutboundPacket(
                instPort.deviceId(),
                treatment,
                ByteBuffer.wrap(icmpReply.serialize()));

        packetService.emit(packet);
    }

    private short getIcmpId(ICMP icmp) {
        return ByteBuffer.wrap(icmp.serialize(), 4, 2).getShort();
    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            Set<DeviceId> gateways = osNodeService.completeNodes(GATEWAY)
                    .stream().map(OpenstackNode::intgBridge)
                    .collect(Collectors.toSet());

            if (context.isHandled()) {
                return;
            } else if (!gateways.contains(context.inPacket().receivedFrom().deviceId())) {
                // return if the packet is not from gateway nodes
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethernet = pkt.parsed();
            if (ethernet == null || ethernet.getEtherType() == Ethernet.TYPE_ARP) {
                return;
            }

            IPv4 iPacket = (IPv4) ethernet.getPayload();
            if (iPacket.getProtocol() == IPv4.PROTOCOL_ICMP) {
                eventExecutor.execute(() -> processIcmpPacket(context, ethernet));
            }
        }
    }
}
