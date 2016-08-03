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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstackinterface.OpenstackRouter;
import org.onosproject.openstacknetworking.Constants;
import org.onosproject.openstackinterface.OpenstackInterfaceService;
import org.onosproject.openstackinterface.OpenstackPort;
import org.onosproject.openstacknode.OpenstackNode;
import org.onosproject.openstacknode.OpenstackNodeEvent;
import org.onosproject.openstacknode.OpenstackNodeListener;
import org.onosproject.openstacknode.OpenstackNodeService;
import org.onosproject.scalablegateway.api.GatewayNode;
import org.onosproject.scalablegateway.api.ScalableGatewayService;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.Constants.*;
import static org.onosproject.openstacknode.OpenstackNodeService.NodeType.GATEWAY;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Handle ICMP packet sent from OpenStack Gateway nodes.
 * For a request to any private network gateway IPs, it generates fake reply.
 * For a request to the external network, it does source NAT with a public IP and
 * forward the request to the external only if the request instance has external
 * connection setups.
 */
@Component(immediate = true)
public class OpenstackIcmpHandler {
    protected final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackInterfaceService openstackService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ScalableGatewayService gatewayService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService nodeService;

    private final ExecutorService eventExecutor = newSingleThreadScheduledExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final InternalPacketProcessor packetProcessor = new InternalPacketProcessor();
    private final InternalNodeListener nodeListener = new InternalNodeListener();
    private final Map<String, Host> icmpInfoMap = Maps.newHashMap();

    ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(ROUTING_APP_ID);
        packetService.addProcessor(packetProcessor, PacketProcessor.director(1));
        nodeService.addListener(nodeListener);
        requestPacket(appId);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(packetProcessor);
        log.info("Stopped");
    }

    private void requestPacket(ApplicationId appId) {
        TrafficSelector icmpSelector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_ICMP)
                .build();

        gatewayService.getGatewayDeviceIds().stream().forEach(gateway -> {
            packetService.requestPackets(icmpSelector,
                                         PacketPriority.CONTROL,
                                         appId,
                                         Optional.of(gateway));
            log.debug("Requested ICMP packet on {}", gateway);
        });
    }

    private void processIcmpPacket(PacketContext context, Ethernet ethernet) {
        IPv4 ipPacket = (IPv4) ethernet.getPayload();
        log.trace("Processing ICMP packet from ip {}, mac {}",
                  Ip4Address.valueOf(ipPacket.getSourceAddress()),
                  ethernet.getSourceMAC());

        ICMP icmp = (ICMP) ipPacket.getPayload();
        short icmpId = getIcmpId(icmp);

        DeviceId srcDevice = context.inPacket().receivedFrom().deviceId();
        switch (icmp.getIcmpType()) {
            case ICMP.TYPE_ECHO_REQUEST:
                Optional<Host> reqHost = hostService.getHostsByMac(ethernet.getSourceMAC())
                        .stream().findFirst();
                if (!reqHost.isPresent()) {
                    log.warn("No host found for MAC {}", ethernet.getSourceMAC());
                    return;
                }

                // TODO Considers icmp between internal subnets belong to the same router.
                // TODO do we have to support ICMP reply for non-existing gateway?
                Ip4Address gatewayIp = Ip4Address.valueOf(
                        reqHost.get().annotations().value(Constants.GATEWAY_IP));
                if (Objects.equals(ipPacket.getDestinationAddress(), gatewayIp.toInt())) {
                    processRequestToGateway(ipPacket, reqHost.get());
                } else {
                    Optional<Ip4Address> srcNatIp = getSrcNatIp(reqHost.get());
                    if (!srcNatIp.isPresent()) {
                        log.trace("VM {} has no external connection", reqHost.get());
                        return;
                    }

                    sendRequestToExternal(ipPacket, srcDevice, srcNatIp.get());
                    String icmpInfoKey = String.valueOf(icmpId)
                            .concat(String.valueOf(srcNatIp.get().toInt()))
                            .concat(String.valueOf(ipPacket.getDestinationAddress()));
                    icmpInfoMap.putIfAbsent(icmpInfoKey, reqHost.get());
                }
                break;
            case ICMP.TYPE_ECHO_REPLY:
                String icmpInfoKey = String.valueOf(icmpId)
                        .concat(String.valueOf(ipPacket.getDestinationAddress()))
                        .concat(String.valueOf(ipPacket.getSourceAddress()));

                processReplyFromExternal(ipPacket, icmpInfoMap.get(icmpInfoKey));
                icmpInfoMap.remove(icmpInfoKey);
                break;
            default:
                break;
        }
    }

    // TODO do we have to handle the request to the fake gateway?
    private void processRequestToGateway(IPv4 ipPacket, Host reqHost) {
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
                .setDestinationMACAddress(reqHost.mac())
                .setPayload(ipPacket);

        sendReply(icmpReply, reqHost);
    }

    private void sendRequestToExternal(IPv4 ipPacket, DeviceId srcDevice, Ip4Address srcNatIp) {
        ICMP icmpReq = (ICMP) ipPacket.getPayload();
        icmpReq.resetChecksum();
        ipPacket.setSourceAddress(srcNatIp.toInt()).resetChecksum();
        ipPacket.setPayload(icmpReq);

        Ethernet icmpRequestEth = new Ethernet();
        icmpRequestEth.setEtherType(Ethernet.TYPE_IPV4)
                .setSourceMACAddress(DEFAULT_GATEWAY_MAC)
                .setDestinationMACAddress(DEFAULT_EXTERNAL_ROUTER_MAC)
                .setPayload(ipPacket);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(gatewayService.getUplinkPort(srcDevice))
                .build();

        OutboundPacket packet = new DefaultOutboundPacket(
                srcDevice,
                treatment,
                ByteBuffer.wrap(icmpRequestEth.serialize()));

        packetService.emit(packet);
    }

    private void processReplyFromExternal(IPv4 ipPacket, Host dstHost) {
        ICMP icmpReply = (ICMP) ipPacket.getPayload();
        icmpReply.resetChecksum();

        Ip4Address ipAddress = dstHost.ipAddresses().stream().findFirst().get().getIp4Address();
        ipPacket.setDestinationAddress(ipAddress.toInt())
                .resetChecksum();
        ipPacket.setPayload(icmpReply);

        Ethernet icmpResponseEth = new Ethernet();
        icmpResponseEth.setEtherType(Ethernet.TYPE_IPV4)
                .setSourceMACAddress(Constants.DEFAULT_GATEWAY_MAC)
                .setDestinationMACAddress(dstHost.mac())
                .setPayload(ipPacket);

        sendReply(icmpResponseEth, dstHost);
    }

    private void sendReply(Ethernet icmpReply, Host dstHost) {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(dstHost.location().port())
                .build();

        OutboundPacket packet = new DefaultOutboundPacket(
                dstHost.location().deviceId(),
                treatment,
                ByteBuffer.wrap(icmpReply.serialize()));

        packetService.emit(packet);
    }

    private Optional<Ip4Address> getSrcNatIp(Host host) {
        // TODO cache external gateway IP for each network because
        // asking Neutron for every ICMP request is a bad idea
        Optional<OpenstackPort> osPort = openstackService.ports().stream()
                .filter(port -> port.deviceOwner().equals(DEVICE_OWNER_ROUTER_INTERFACE) &&
                        Objects.equals(host.annotations().value(NETWORK_ID),
                                       port.networkId()))
                .findAny();
        if (!osPort.isPresent()) {
            return Optional.empty();
        }

        OpenstackRouter osRouter = openstackService.router(osPort.get().deviceId());
        if (osRouter == null) {
            return Optional.empty();
        }

        return osRouter.gatewayExternalInfo().externalFixedIps()
                .values().stream().findAny();
    }

    private short getIcmpId(ICMP icmp) {
        return ByteBuffer.wrap(icmp.serialize(), 4, 2).getShort();
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
            if (iPacket.getProtocol() == IPv4.PROTOCOL_ICMP) {
                    eventExecutor.execute(() -> processIcmpPacket(context, ethernet));
            }
        }
    }

    private class InternalNodeListener implements OpenstackNodeListener {

        @Override
        public void event(OpenstackNodeEvent event) {
            OpenstackNode node = event.node();

            switch (event.type()) {
                case COMPLETE:
                    if (node.type() == GATEWAY) {
                        log.info("GATEWAY node {} detected", node.hostname());
                        eventExecutor.execute(() -> {
                            GatewayNode gnode = GatewayNode.builder()
                                    .gatewayDeviceId(node.intBridge())
                                    .dataIpAddress(node.dataIp().getIp4Address())
                                    .uplinkIntf(node.externalPortName().get())
                                    .build();
                            gatewayService.addGatewayNode(gnode);
                            requestPacket(appId);
                        });
                    }
                    break;
                case INIT:
                case DEVICE_CREATED:
                case INCOMPLETE:
                default:
                    break;
            }
        }
    }
}
