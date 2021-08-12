/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.impl;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onlab.packet.DHCP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.packet.dhcp.DhcpOption;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.KubevirtFlowRuleService;
import org.onosproject.kubevirtnetworking.api.KubevirtHostRoute;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkService;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtPortService;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeEvent;
import org.onosproject.kubevirtnode.api.KubevirtNodeListener;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
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
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.packet.DHCP.DHCPOptionCode.OptionCode_BroadcastAddress;
import static org.onlab.packet.DHCP.DHCPOptionCode.OptionCode_Classless_Static_Route;
import static org.onlab.packet.DHCP.DHCPOptionCode.OptionCode_DHCPServerIp;
import static org.onlab.packet.DHCP.DHCPOptionCode.OptionCode_DomainServer;
import static org.onlab.packet.DHCP.DHCPOptionCode.OptionCode_END;
import static org.onlab.packet.DHCP.DHCPOptionCode.OptionCode_LeaseTime;
import static org.onlab.packet.DHCP.DHCPOptionCode.OptionCode_MessageType;
import static org.onlab.packet.DHCP.DHCPOptionCode.OptionCode_RouterAddress;
import static org.onlab.packet.DHCP.DHCPOptionCode.OptionCode_SubnetMask;
import static org.onlab.packet.DHCP.MsgType.DHCPACK;
import static org.onlab.packet.DHCP.MsgType.DHCPOFFER;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.Constants.DHCP_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_DHCP_RULE;
import static org.onosproject.kubevirtnetworking.impl.OsgiPropertyConstants.DHCP_SERVER_MAC;
import static org.onosproject.kubevirtnetworking.impl.OsgiPropertyConstants.DHCP_SERVER_MAC_DEFAULT;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.getBroadcastAddr;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.WORKER;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles DHCP requests for the virtual instances.
 */
@Component(
        immediate = true,
        property = {
                DHCP_SERVER_MAC + "=" + DHCP_SERVER_MAC_DEFAULT
        }
)
public class KubevirtDhcpHandler {
    protected final Logger log = getLogger(getClass());

    private static final Ip4Address DEFAULT_PRIMARY_DNS = Ip4Address.valueOf("8.8.8.8");
    private static final Ip4Address DEFAULT_SECONDARY_DNS = Ip4Address.valueOf("8.8.4.4");
    private static final byte PACKET_TTL = (byte) 127;

    // TODO add MTU, static route option codes to ONOS DHCP and remove here
    private static final byte DHCP_OPTION_MTU = (byte) 26;
    private static final byte[] DHCP_DATA_LEASE_INFINITE =
            ByteBuffer.allocate(4).putInt(-1).array();

    private static final int OCTET_BIT_LENGTH = 8;
    private static final int V4_BYTE_SIZE = 4;
    private static final int V4_CIDR_LOWER_BOUND = -1;
    private static final int V4_CIDR_UPPER_BOUND = 33;
    private static final int PADDING_SIZE = 4;

    private static final byte HARDWARE_ADDR_LENGTH = (byte) 6;
    private static final byte DHCP_OPTION_DATA_LENGTH = (byte) 4;
    private static final int DHCP_OPTION_DNS_LENGTH = 8;
    private static final int DHCP_OPTION_MTU_LENGTH = 2;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNodeService nodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtPortService portService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNetworkService networkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtFlowRuleService flowService;

    /** Fake MAC address for virtual network subnet gateway. */
    private String dhcpServerMac = DHCP_SERVER_MAC_DEFAULT;

    private final PacketProcessor packetProcessor = new InternalPacketProcessor();
    private final KubevirtNodeListener nodeListener = new InternalNodeEventListener();

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        nodeService.addListener(nodeListener);
        configService.registerProperties(getClass());
        packetService.addProcessor(packetProcessor, PacketProcessor.director(0));
        leadershipService.runForLeadership(appId.name());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(packetProcessor);
        nodeService.removeListener(nodeListener);
        configService.unregisterProperties(getClass(), false);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

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

            eventExecutor.execute(() -> processDhcp(context, dhcpPacket));
        }

        private void processDhcp(PacketContext context, DHCP dhcpPacket) {
            if (dhcpPacket == null) {
                log.trace("DHCP packet without payload received, do nothing");
                return;
            }

            DHCP.MsgType inPacketType = getPacketType(dhcpPacket);
            if (inPacketType == null || dhcpPacket.getClientHardwareAddress() == null) {
                log.trace("Malformed DHCP packet received, ignore it");
                return;
            }

            MacAddress clientMac = MacAddress.valueOf(dhcpPacket.getClientHardwareAddress());

            KubevirtPort port = portService.ports().stream()
                    .filter(p -> p.macAddress().equals(clientMac))
                    .findAny().orElse(null);

            if (port == null) {
                log.debug("Ignore DHCP request, since it comes from unmanaged VM {}", clientMac);
                return;
            }

            IpAddress fixedIp = port.ipAddress();

            if (fixedIp == null) {
                log.warn("There is no IP addresses are assigned with the port {}", port.macAddress());
                return;
            }

            Ethernet ethPacket = context.inPacket().parsed();
            switch (inPacketType) {
                case DHCPDISCOVER:
                    processDhcpDiscover(context, clientMac, port, ethPacket);
                    break;
                case DHCPREQUEST:
                    processDhcpRequest(context, clientMac, port, ethPacket);
                    break;
                case DHCPRELEASE:
                    log.trace("DHCP RELEASE received from {}", clientMac);
                    // do nothing
                    break;
                default:
                    break;
            }
        }

        private void processDhcpDiscover(PacketContext context, MacAddress clientMac,
                                         KubevirtPort port, Ethernet ethPacket) {
            log.trace("DHCP DISCOVER received from {}", clientMac);
            Ethernet discoverReply = buildReply(ethPacket,
                    (byte) DHCPOFFER.getValue(),
                    port);
            sendReply(context, discoverReply);
            log.trace("DHCP OFFER({}) is sent for {}", port.ipAddress().toString(), clientMac);
        }

        private void processDhcpRequest(PacketContext context, MacAddress clientMac,
                                        KubevirtPort port, Ethernet ethPacket) {
            log.trace("DHCP REQUEST received from {}", clientMac);
            Ethernet requestReply = buildReply(ethPacket,
                    (byte) DHCPACK.getValue(),
                    port);
            sendReply(context, requestReply);
            log.trace("DHCP ACK({}) is sent for {}", port.ipAddress(), clientMac);
        }

        private DHCP.MsgType getPacketType(DHCP dhcpPacket) {
            DhcpOption optType = dhcpPacket.getOption(OptionCode_MessageType);
            if (optType == null) {
                log.trace("DHCP packet with no message type, ignore it");
                return null;
            }

            DHCP.MsgType inPacketType = DHCP.MsgType.getType(optType.getData()[0]);
            if (inPacketType == null) {
                log.trace("DHCP packet with no packet type, ignore it");
            }
            return inPacketType;
        }

        private Ethernet buildReply(Ethernet ethRequest, byte packetType,
                                    KubevirtPort port) {
            log.trace("Build for DHCP reply msg for openstack port {}", port.toString());

            // pick one IP address to make a reply
            // since we check the validity of fixed IP address at parent method,
            // so no need to double check the fixed IP existence here
            IpAddress fixedIp = port.ipAddress();

            KubevirtNetwork network = networkService.network(port.networkId());

            Ethernet ethReply = new Ethernet();
            ethReply.setSourceMACAddress(dhcpServerMac);
            ethReply.setDestinationMACAddress(ethRequest.getSourceMAC());
            ethReply.setEtherType(Ethernet.TYPE_IPV4);

            IPv4 ipv4Request = (IPv4) ethRequest.getPayload();
            IPv4 ipv4Reply = new IPv4();

            ipv4Reply.setSourceAddress(
                    clusterService.getLocalNode().ip().getIp4Address().toString());
            ipv4Reply.setDestinationAddress(fixedIp.toString());
            ipv4Reply.setTtl(PACKET_TTL);

            UDP udpRequest = (UDP) ipv4Request.getPayload();
            UDP udpReply = new UDP();
            udpReply.setSourcePort((byte) UDP.DHCP_SERVER_PORT);
            udpReply.setDestinationPort((byte) UDP.DHCP_CLIENT_PORT);

            DHCP dhcpRequest = (DHCP) udpRequest.getPayload();
            DHCP dhcpReply = buildDhcpReply(
                    dhcpRequest,
                    packetType,
                    Ip4Address.valueOf(fixedIp.toString()), port, network);

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
                                    KubevirtPort port, KubevirtNetwork network) {
            Ip4Address gatewayIp = clusterService.getLocalNode().ip().getIp4Address();
            int subnetPrefixLen = IpPrefix.valueOf(network.cidr()).prefixLength();

            DHCP dhcpReply = new DHCP();
            dhcpReply.setOpCode(DHCP.OPCODE_REPLY);
            dhcpReply.setHardwareType(DHCP.HWTYPE_ETHERNET);
            dhcpReply.setHardwareAddressLength(HARDWARE_ADDR_LENGTH);
            dhcpReply.setTransactionId(request.getTransactionId());
            dhcpReply.setFlags(request.getFlags());
            dhcpReply.setYourIPAddress(yourIp.toInt());
            dhcpReply.setServerIPAddress(gatewayIp.toInt());
            dhcpReply.setClientHardwareAddress(request.getClientHardwareAddress());

            List<DhcpOption> options = Lists.newArrayList();

            // message type
            options.add(doMsgType(msgType));

            // server identifier
            options.add(doServerId(gatewayIp));

            // lease time
            options.add(doLeaseTime());

            // subnet mask
            options.add(doSubnetMask(subnetPrefixLen));

            // broadcast address
            options.add(doBroadcastAddr(yourIp, subnetPrefixLen));

            // domain server
            options.add(doDomainServer(network));

            // mtu
            options.add(doMtu(network));

            // classless static route
            if (!network.hostRoutes().isEmpty()) {
                options.add(doClasslessSr(network));
            }

            // Sets the default router address up.
            // Performs only if the gateway is set and default route is configure to true.
            if (network.gatewayIp() != null && network.defaultRoute()) {
                options.add(doRouterAddr(network));
            }

            // end option
            options.add(doEnd());

            dhcpReply.setOptions(options);
            return dhcpReply;
        }


        private DhcpOption doMsgType(byte msgType) {
            DhcpOption option = new DhcpOption();
            option.setCode(OptionCode_MessageType.getValue());
            option.setLength((byte) 1);
            byte[] optionData = {msgType};
            option.setData(optionData);
            return option;
        }

        private DhcpOption doServerId(IpAddress gatewayIp) {
            DhcpOption option = new DhcpOption();
            option.setCode(OptionCode_DHCPServerIp.getValue());
            option.setLength(DHCP_OPTION_DATA_LENGTH);
            option.setData(gatewayIp.toOctets());
            return option;
        }

        private DhcpOption doLeaseTime() {
            DhcpOption option = new DhcpOption();
            option.setCode(OptionCode_LeaseTime.getValue());
            option.setLength(DHCP_OPTION_DATA_LENGTH);
            option.setData(DHCP_DATA_LEASE_INFINITE);
            return option;
        }

        private DhcpOption doSubnetMask(int subnetPrefixLen) {
            Ip4Address subnetMask = Ip4Address.makeMaskPrefix(subnetPrefixLen);
            DhcpOption option = new DhcpOption();
            option.setCode(OptionCode_SubnetMask.getValue());
            option.setLength(DHCP_OPTION_DATA_LENGTH);
            option.setData(subnetMask.toOctets());
            return option;
        }

        private DhcpOption doBroadcastAddr(Ip4Address yourIp, int subnetPrefixLen) {
            String broadcast = getBroadcastAddr(yourIp.toString(), subnetPrefixLen);

            DhcpOption option = new DhcpOption();
            option.setCode(OptionCode_BroadcastAddress.getValue());
            option.setLength(DHCP_OPTION_DATA_LENGTH);
            option.setData(IpAddress.valueOf(broadcast).toOctets());

            return option;
        }

        private DhcpOption doDomainServer(KubevirtNetwork network) {
            DhcpOption option = new DhcpOption();

            option.setCode(OptionCode_DomainServer.getValue());

            if (network.dnses().isEmpty()) {
                option.setLength((byte) DHCP_OPTION_DNS_LENGTH);
                ByteBuffer dnsByteBuf = ByteBuffer.allocate(DHCP_OPTION_DNS_LENGTH);
                dnsByteBuf.put(DEFAULT_PRIMARY_DNS.toOctets());
                dnsByteBuf.put(DEFAULT_SECONDARY_DNS.toOctets());

                option.setData(dnsByteBuf.array());
            } else {
                int dnsLength = 4 * network.dnses().size();

                option.setLength((byte) dnsLength);

                ByteBuffer dnsByteBuf = ByteBuffer.allocate(DHCP_OPTION_DNS_LENGTH);

                for (IpAddress dnsServer : network.dnses()) {
                    dnsByteBuf.put(dnsServer.toOctets());
                }
                option.setData(dnsByteBuf.array());
            }

            return option;
        }

        private DhcpOption doMtu(KubevirtNetwork network) {
            DhcpOption option = new DhcpOption();
            option.setCode(DHCP_OPTION_MTU);
            option.setLength((byte) DHCP_OPTION_MTU_LENGTH);
            checkNotNull(network);
            checkNotNull(network.mtu());

            option.setData(ByteBuffer.allocate(DHCP_OPTION_MTU_LENGTH)
                    .putShort(network.mtu().shortValue()).array());

            return option;
        }

        private DhcpOption doClasslessSr(KubevirtNetwork network) {
            DhcpOption option = new DhcpOption();
            option.setCode(OptionCode_Classless_Static_Route.getValue());

            int hostRoutesSize = hostRoutesSize(ImmutableList.copyOf(network.hostRoutes()));
            if (hostRoutesSize == 0) {
                throw new IllegalArgumentException("Illegal CIDR hostRoutesSize value!");
            }

            log.trace("hostRouteSize: {}", hostRoutesSize);

            option.setLength((byte) hostRoutesSize);
            ByteBuffer hostRouteByteBuf = ByteBuffer.allocate(hostRoutesSize);

            network.hostRoutes().forEach(h -> {
                log.debug("processing host route information: {}", h.toString());

                IpPrefix ipPrefix = h.destination();

                hostRouteByteBuf.put(Objects.requireNonNull(bytesDestinationDescriptor(ipPrefix)));
                hostRouteByteBuf.put(h.nexthop().toOctets());
            });

            option.setData(hostRouteByteBuf.array());
            return option;
        }

        private DhcpOption doRouterAddr(KubevirtNetwork network) {
            DhcpOption option = new DhcpOption();
            option.setCode(OptionCode_RouterAddress.getValue());
            option.setLength(DHCP_OPTION_DATA_LENGTH);
            option.setData(Ip4Address.valueOf(network.gatewayIp().toString()).toOctets());
            return option;
        }

        private DhcpOption doEnd() {
            DhcpOption option = new DhcpOption();
            option.setCode(OptionCode_END.getValue());
            option.setLength((byte) 1);
            return option;
        }

        private int hostRoutesSize(List<KubevirtHostRoute> hostRoutes) {
            int size = 0;
            int preFixLen;

            for (KubevirtHostRoute h : hostRoutes) {
                preFixLen = h.destination().prefixLength();
                if (Math.max(V4_CIDR_LOWER_BOUND, preFixLen) == V4_CIDR_LOWER_BOUND ||
                        Math.min(preFixLen, V4_CIDR_UPPER_BOUND) == V4_CIDR_UPPER_BOUND) {
                    throw new IllegalArgumentException("Illegal CIDR length value!");
                }

                for (int i = 0; i <= V4_BYTE_SIZE; i++) {
                    if (preFixLen == Math.min(preFixLen, i * OCTET_BIT_LENGTH)) {
                        size = size + i + 1 + PADDING_SIZE;
                        break;
                    }
                }
            }
            return size;
        }

        private byte[] bytesDestinationDescriptor(IpPrefix ipPrefix) {
            ByteBuffer byteBuffer;
            int prefixLen = ipPrefix.prefixLength();

            // retrieve ipPrefix to the destination descriptor format
            // ex) 10.1.1.0/24 -> [10,1,1,0]
            String[] ipPrefixString = ipPrefix.getIp4Prefix().toString()
                    .split("/")[0]
                    .split("\\.");

            // retrieve destination descriptor and put this to byte buffer
            // according to RFC 3442
            // ex) 0.0.0.0/0 -> 0
            // ex) 10.0.0.0/8 -> 8.10
            // ex) 10.17.0.0/16 -> 16.10.17
            // ex) 10.27.129.0/24 -> 24.10.27.129
            // ex) 10.229.0.128/25 -> 25.10.229.0.128
            for (int i = 0; i <= V4_BYTE_SIZE; i++) {
                if (prefixLen == Math.min(prefixLen, i * OCTET_BIT_LENGTH)) {
                    byteBuffer = ByteBuffer.allocate(i + 1);
                    byteBuffer.put((byte) prefixLen);

                    for (int j = 0; j < i; j++) {
                        byteBuffer.put((byte) Integer.parseInt(ipPrefixString[j]));
                    }
                    return byteBuffer.array();
                }
            }

            return null;
        }
    }

    private class InternalNodeEventListener implements KubevirtNodeListener {

        @Override
        public boolean isRelevant(KubevirtNodeEvent event) {
            return event.subject().type() == WORKER;
        }

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtNodeEvent event) {
            KubevirtNode node = event.subject();
            switch (event.type()) {
                case KUBEVIRT_NODE_COMPLETE:
                    eventExecutor.execute(() -> processNodeCompletion(node));
                    break;
                case KUBEVIRT_NODE_CREATED:
                case KUBEVIRT_NODE_INCOMPLETE:
                case KUBEVIRT_NODE_REMOVED:
                case KUBEVIRT_NODE_UPDATED:
                default:
                    break;
            }
        }

        private void processNodeCompletion(KubevirtNode node) {
            if (!isRelevantHelper()) {
                return;
            }
            setDhcpRule(node, true);
        }

        private void setDhcpRule(KubevirtNode node, boolean install) {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPProtocol(IPv4.PROTOCOL_UDP)
                    .matchUdpDst(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
                    .matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .punt()
                    .build();

            flowService.setRule(
                    appId,
                    node.intgBridge(),
                    selector,
                    treatment,
                    PRIORITY_DHCP_RULE,
                    DHCP_TABLE,
                    install);
        }
    }
}
