/*
 * Copyright 2017-present Open Networking Foundation
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
 *
 */

package org.onosproject.dhcprelay;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.BasePacket;
import org.onlab.packet.DHCP6;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onlab.packet.dhcp.Dhcp6RelayOption;
import org.onlab.packet.dhcp.Dhcp6InterfaceIdOption;
import org.onlab.packet.dhcp.Dhcp6Option;
import org.onlab.packet.dhcp.Dhcp6IaNaOption;
import org.onlab.packet.dhcp.Dhcp6IaTaOption;
import org.onlab.packet.dhcp.Dhcp6IaPdOption;
import org.onlab.packet.dhcp.Dhcp6IaAddressOption;
import org.onlab.packet.dhcp.Dhcp6IaPrefixOption;
import org.onlab.util.HexString;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.dhcprelay.api.DhcpHandler;
import org.onosproject.dhcprelay.api.DhcpServerInfo;
import org.onosproject.dhcprelay.config.IgnoreDhcpConfig;
import org.onosproject.dhcprelay.store.DhcpRelayStore;
import org.onosproject.dhcprelay.store.DhcpFpmPrefixStore;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.routing.fpm.api.FpmRecord;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteStore;
import org.onosproject.dhcprelay.config.DhcpServerConfig;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;


import java.nio.ByteBuffer;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;


import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.onosproject.net.flowobjective.Objective.Operation.ADD;
import static org.onosproject.net.flowobjective.Objective.Operation.REMOVE;

@Component
@Service
@Property(name = "version", value = "6")
public class Dhcp6HandlerImpl implements DhcpHandler, HostProvider {
    public static final String DHCP_V6_RELAY_APP = "org.onosproject.Dhcp6HandlerImpl";
    public static final ProviderId PROVIDER_ID = new ProviderId("dhcp6", DHCP_V6_RELAY_APP);
    private static final int IGNORE_CONTROL_PRIORITY = PacketPriority.CONTROL.priorityValue() + 1000;

    private static final TrafficSelector CLIENT_SERVER_SELECTOR = DefaultTrafficSelector.builder()
            .matchEthType(Ethernet.TYPE_IPV6)
            .matchIPProtocol(IPv6.PROTOCOL_UDP)
            .matchIPv6Src(IpPrefix.IPV6_LINK_LOCAL_PREFIX)
            .matchIPv6Dst(Ip6Address.ALL_DHCP_RELAY_AGENTS_AND_SERVERS.toIpPrefix())
            .matchUdpSrc(TpPort.tpPort(UDP.DHCP_V6_CLIENT_PORT))
            .matchUdpDst(TpPort.tpPort(UDP.DHCP_V6_SERVER_PORT))
            .build();
    private static final TrafficSelector SERVER_RELAY_SELECTOR = DefaultTrafficSelector.builder()
            .matchEthType(Ethernet.TYPE_IPV6)
            .matchIPProtocol(IPv6.PROTOCOL_UDP)
            .matchUdpSrc(TpPort.tpPort(UDP.DHCP_V6_SERVER_PORT))
            .matchUdpDst(TpPort.tpPort(UDP.DHCP_V6_SERVER_PORT))
            .build();
    static final Set<TrafficSelector> DHCP_SELECTORS = ImmutableSet.of(
            CLIENT_SERVER_SELECTOR,
            SERVER_RELAY_SELECTOR
    );
    private static Logger log = LoggerFactory.getLogger(Dhcp6HandlerImpl.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DhcpRelayStore dhcpRelayStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouteStore routeStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DhcpFpmPrefixStore dhcpFpmPrefixStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    protected HostProviderService providerService;
    protected ApplicationId appId;
    protected Multimap<DeviceId, VlanId> ignoredVlans = HashMultimap.create();
    private InternalHostListener hostListener = new InternalHostListener();

    private Boolean dhcpFpmEnabled = false;

    private List<DhcpServerInfo> defaultServerInfoList = Lists.newArrayList();
    private List<DhcpServerInfo> indirectServerInfoList = Lists.newArrayList();


    // CLIENT message types
    public static final Set<Byte> MSG_TYPE_FROM_CLIENT =
            ImmutableSet.of(DHCP6.MsgType.SOLICIT.value(),
                            DHCP6.MsgType.REQUEST.value(),
                            DHCP6.MsgType.REBIND.value(),
                            DHCP6.MsgType.RENEW.value(),
                            DHCP6.MsgType.RELEASE.value(),
                            DHCP6.MsgType.DECLINE.value(),
                            DHCP6.MsgType.CONFIRM.value(),
                            DHCP6.MsgType.RELAY_FORW.value());
    // SERVER message types
    public static final Set<Byte> MSG_TYPE_FROM_SERVER =
            ImmutableSet.of(DHCP6.MsgType.RELAY_REPL.value());

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(DHCP_V6_RELAY_APP);
        providerService = providerRegistry.register(this);
        hostService.addListener(hostListener);
    }

    @Deactivate
    protected void deactivate() {
        providerRegistry.unregister(this);
        hostService.removeListener(hostListener);
        defaultServerInfoList.forEach(this::stopMonitoringIps);
        defaultServerInfoList.clear();
        indirectServerInfoList.forEach(this::stopMonitoringIps);
        indirectServerInfoList.clear();
    }

    private void stopMonitoringIps(DhcpServerInfo serverInfo) {
        serverInfo.getDhcpGatewayIp6().ifPresent(gatewayIp -> {
            hostService.stopMonitoringIp(gatewayIp);
        });
        serverInfo.getDhcpServerIp6().ifPresent(serverIp -> {
            hostService.stopMonitoringIp(serverIp);
        });
    }

    @Override
    public List<DhcpServerInfo> getDefaultDhcpServerInfoList() {
        return defaultServerInfoList;
    }

    @Override
    public List<DhcpServerInfo> getIndirectDhcpServerInfoList() {
        return indirectServerInfoList;
    }

    @Override
    public void updateIgnoreVlanConfig(IgnoreDhcpConfig config) {
        if (config == null) {
            ignoredVlans.forEach(((deviceId, vlanId) -> {
                processIgnoreVlanRule(deviceId, vlanId, REMOVE);
            }));
            return;
        }
        config.ignoredVlans().forEach((deviceId, vlanId) -> {
            if (ignoredVlans.get(deviceId).contains(vlanId)) {
                // don't need to process if it already ignored
                return;
            }
            processIgnoreVlanRule(deviceId, vlanId, ADD);
        });

        ignoredVlans.forEach((deviceId, vlanId) -> {
            if (!config.ignoredVlans().get(deviceId).contains(vlanId)) {
                // not contains in new config, remove it
                processIgnoreVlanRule(deviceId, vlanId, REMOVE);
            }
        });
    }

    @Override
    public void processDhcpPacket(PacketContext context, BasePacket payload) {
        checkNotNull(payload, "DHCP6 payload can't be null");
        checkState(payload instanceof DHCP6, "Payload is not a DHCP6");
        DHCP6 dhcp6Payload = (DHCP6) payload;
        Ethernet receivedPacket = context.inPacket().parsed();

        if (!configured()) {
            log.warn("Missing DHCP6 relay server config. Abort packet processing");
            log.warn("dhcp6 payload {}", dhcp6Payload);

            return;
        }

        byte msgType = dhcp6Payload.getMsgType();
        log.warn("msgType is {}", msgType);

        ConnectPoint inPort = context.inPacket().receivedFrom();
        if (inPort == null) {
            log.warn("incommin ConnectPoint is null");
        }
        Set<Interface> receivingInterfaces = interfaceService.getInterfacesByPort(inPort);
        //ignore the packets if dhcp client interface is not configured on onos.
        if (receivingInterfaces.isEmpty()) {
            log.warn("Virtual interface is not configured on {}", inPort);
            return;
        }


        if (MSG_TYPE_FROM_CLIENT.contains(msgType)) {

            InternalPacket ethernetClientPacket =
                    processDhcp6PacketFromClient(context, receivedPacket, receivingInterfaces);
            if (ethernetClientPacket != null) {
                forwardPacket(ethernetClientPacket);
            }

        } else if (MSG_TYPE_FROM_SERVER.contains(msgType)) {
            log.warn("calling processDhcp6PacketFromServer with RELAY_REPL", msgType);
            InternalPacket ethernetPacketReply =
                    processDhcp6PacketFromServer(context, receivedPacket, receivingInterfaces);
            if (ethernetPacketReply != null) {
                forwardPacket(ethernetPacketReply);
            }
        } else {
            log.warn("Not so fast, packet type {} not supported yet", msgType);
        }
    }


    /**
     * Checks if this app has been configured.
     *
     * @return true if all information we need have been initialized
     */
    public boolean configured() {
        return !defaultServerInfoList.isEmpty();
    }

    @Override
    public ProviderId id() {
        return PROVIDER_ID;
    }

    @Override
    public void triggerProbe(Host host) {
        // Do nothing here
    }

    // the new class the contains Ethernet packet and destination port, kind of like adding
    // internal header to the packet
    private class InternalPacket {
        Ethernet packet;
        ConnectPoint destLocation;
        public InternalPacket(Ethernet newPacket, ConnectPoint newLocation) {
            packet = newPacket;
            destLocation = newLocation;
        }
        void setLocation(ConnectPoint newLocation) {
            destLocation = newLocation;
        }
    }

    //forward the packet to ConnectPoint where the DHCP server is attached.
    private void forwardPacket(InternalPacket packet) {
        //send Packetout to dhcp server connectpoint.
        if (packet.destLocation != null) {
            TrafficTreatment t = DefaultTrafficTreatment.builder()
                    .setOutput(packet.destLocation.port()).build();
            OutboundPacket o = new DefaultOutboundPacket(
                    packet.destLocation.deviceId(), t, ByteBuffer.wrap(packet.packet.serialize()));
            if (log.isTraceEnabled()) {
                log.trace("Relaying packet to destination {}", packet.destLocation);
            }
            packetService.emit(o);
        } // if
    }

    /**
     * Check if the host is directly connected to the network or not.
     *
     * @param dhcp6Payload the dhcp6 payload
     * @return true if the host is directly connected to the network; false otherwise
     */
    private boolean directlyConnected(DHCP6 dhcp6Payload) {
        log.debug("directlyConnected enters");

        if (dhcp6Payload.getMsgType() != DHCP6.MsgType.RELAY_FORW.value() &&
                dhcp6Payload.getMsgType() != DHCP6.MsgType.RELAY_REPL.value()) {
            log.debug("directlyConnected true. MsgType {}", dhcp6Payload.getMsgType());

            return true;
        }

        // Regardless of relay-forward or relay-replay, check if we see another relay message
        DHCP6 dhcp6Payload2 = dhcp6PacketFromRelayPacket(dhcp6Payload);
        if (dhcp6Payload2 != null) {
            if (dhcp6Payload.getMsgType() == DHCP6.MsgType.RELAY_FORW.value()) {
                log.debug("directlyConnected  false. 1st realy-foward, 2nd MsgType {}", dhcp6Payload2.getMsgType());
                return false;
            } else {
                // relay-reply
                if (dhcp6Payload2.getMsgType() != DHCP6.MsgType.RELAY_REPL.value()) {
                    log.debug("directlyConnected  true. 2nd MsgType {}", dhcp6Payload2.getMsgType());
                    return true;  // must be directly connected
                } else {
                    log.debug("directlyConnected  false. 1st relay-reply, 2nd relay-reply MsgType {}",
                               dhcp6Payload2.getMsgType());
                    return false;  // must be indirectly connected
                }
            }
        } else {
            log.warn("directlyConnected  true.");
            return true;
        }
    }

    /**
     * extract DHCP6 payload from dhcp6 relay message within relay-forwrd/reply.
     *
     * @param dhcp6 dhcp6 relay-reply or relay-foward
     * @return dhcp6Packet dhcp6 packet extracted from relay-message
     */
    private DHCP6 dhcp6PacketFromRelayPacket(DHCP6 dhcp6) {
        log.debug("dhcp6PacketFromRelayPacket  enters. dhcp6 {}", dhcp6);

        // extract the relay message if exist
        DHCP6 dhcp6Payload = dhcp6.getOptions().stream()
                    .filter(opt -> opt instanceof Dhcp6RelayOption)
                    .map(BasePacket::getPayload)
                    .map(pld -> (DHCP6) pld)
                    .findFirst()
                    .orElse(null);


        if (dhcp6Payload == null) {
            // Can't find dhcp payload
            log.debug("Can't find dhcp6 payload from relay message");
        } else {
            log.debug("dhcp6 payload found from relay message {}", dhcp6Payload);
        }

        return dhcp6Payload;
    }

    /**
     * find the leaf DHCP6 packet from multi-level relay packet.
     *
     * @param relayPacket dhcp6 relay packet
     * @return leafPacket non-relay dhcp6 packet
     */
    private DHCP6 getDhcp6Leaf(DHCP6 relayPacket) {
        DHCP6 dhcp6Parent = relayPacket;
        DHCP6 dhcp6Child = null;

        log.debug("getDhcp6Leaf entered.");
        while (dhcp6Parent != null) {
            dhcp6Child = dhcp6PacketFromRelayPacket(dhcp6Parent);

            if (dhcp6Child != null) {
                if (dhcp6Child.getMsgType() != DHCP6.MsgType.RELAY_FORW.value() &&
                        dhcp6Child.getMsgType() != DHCP6.MsgType.RELAY_REPL.value()) {
                    log.debug("leaf dhcp6 packet found.");
                    break;
                } else {
                    // found another relay
                    // go for another loop
                    dhcp6Parent = dhcp6Child;
                }
            } else {
                log.warn("malformed pkt! Expected dhcp6 within relay pkt, but no dhcp6 leaf found.");
                break;
            }
        }
        return dhcp6Child;
    }

    /**
     * check if DHCP6 relay-reply is reply.
     *
     * @param relayPacket dhcp6 relay-reply
     * @return boolean relay-reply contains ack
     */
    private boolean isDhcp6Reply(DHCP6 relayPacket) {
        log.debug("isDhcp6Reply  entered.");

        DHCP6 leafDhcp6 = getDhcp6Leaf(relayPacket);

        if (leafDhcp6 != null) {
            if (leafDhcp6.getMsgType() == DHCP6.MsgType.REPLY.value()) {
                log.debug("isDhcp6Reply  true.");
                return true;  // must be directly connected
            } else {
                log.debug("isDhcp6Reply false. leaf dhcp6 is not replay. MsgType {}", leafDhcp6.getMsgType());
            }
        } else {
            log.debug("isDhcp6Reply false. Expected dhcp6 within relay pkt but not found.");
        }
        log.debug("isDhcp6Reply  false.");
        return false;
    }

    /**
     * check if DHCP6 is release or relay-forward contains release.
     *
     * @param dhcp6Payload dhcp6 packet
     * @return boolean dhcp6 contains release
     */
    private boolean isDhcp6Release(DHCP6 dhcp6Payload) {

        log.debug("isDhcp6Release  entered.");

        if (dhcp6Payload.getMsgType() ==  DHCP6.MsgType.RELEASE.value()) {
            log.debug("isDhcp6Release  true.");
            return true;  // must be directly connected
        } else {
            DHCP6 dhcp6Leaf = getDhcp6Leaf(dhcp6Payload);
            if (dhcp6Leaf != null) {
                if (dhcp6Leaf.getMsgType() ==  DHCP6.MsgType.RELEASE.value()) {
                    log.debug("isDhcp6Release  true. indirectlry connected");
                    return true;
                } else {
                    log.debug("leaf dhcp6 is not release. MsgType {}",  dhcp6Leaf.getMsgType());
                    return false;
                }
            } else {
                log.debug("isDhcp6Release  false. dhcp6 is niether relay nor release.");
                return false;
            }
        }
    }

    /**
     * extract from dhcp6 packet client ipv6 address of given by dhcp server.
     *
     * @param dhcp6 the dhcp6 packet
     * @return Ip6Address  Ip6Address given by dhcp server, or null if not exists
     */
    private Ip6Address extractIpAddress(DHCP6 dhcp6) {
        Ip6Address ip = null;

        log.debug("extractIpAddress  enters dhcp6 {}.", dhcp6);
        // Extract IPv6 address from IA NA ot IA TA option
        Optional<Dhcp6IaNaOption> iaNaOption = dhcp6.getOptions()
                .stream()
                .filter(opt -> opt instanceof Dhcp6IaNaOption)
                .map(opt -> (Dhcp6IaNaOption) opt)
                .findFirst();
        Optional<Dhcp6IaTaOption> iaTaOption = dhcp6.getOptions()
                .stream()
                .filter(opt -> opt instanceof Dhcp6IaTaOption)
                .map(opt -> (Dhcp6IaTaOption) opt)
                .findFirst();
        Optional<Dhcp6IaAddressOption> iaAddressOption;
        if (iaNaOption.isPresent()) {
            log.debug("Found IPv6 address from iaNaOption {}", iaNaOption);

            iaAddressOption = iaNaOption.get().getOptions().stream()
                    .filter(opt -> opt instanceof Dhcp6IaAddressOption)
                    .map(opt -> (Dhcp6IaAddressOption) opt)
                    .findFirst();
        } else if (iaTaOption.isPresent()) {
            log.debug("Found IPv6 address from iaTaOption {}", iaTaOption);

            iaAddressOption = iaTaOption.get().getOptions().stream()
                    .filter(opt -> opt instanceof Dhcp6IaAddressOption)
                    .map(opt -> (Dhcp6IaAddressOption) opt)
                    .findFirst();
        } else {
            iaAddressOption = Optional.empty();
        }
        if (iaAddressOption.isPresent()) {
            ip = iaAddressOption.get().getIp6Address();
            log.debug("Found IPv6 address from iaAddressOption {}", iaAddressOption);


        } else {
            log.debug("Can't find IPv6 address from DHCPv6 {}", dhcp6);
        }

        return ip;
    }
    /**
     * extract from dhcp6 packet Prefix prefix provided by dhcp server.
     *
     * @param dhcp6 the dhcp6 payload
     * @return IpPrefix Prefix Delegation prefix, or null if not exists.
     */
    private IpPrefix extractPrefix(DHCP6 dhcp6) {
        log.warn("extractPrefix  enters {}", dhcp6);

        // extract prefix
        IpPrefix  prefixPrefix = null;

        Ip6Address prefixAddress = null;

        // Extract IPv6 prefix from IA PD option
        Optional<Dhcp6IaPdOption> iaPdOption = dhcp6.getOptions()
                .stream()
                .filter(opt -> opt instanceof Dhcp6IaPdOption)
                .map(opt -> (Dhcp6IaPdOption) opt)
                .findFirst();

        Optional<Dhcp6IaPrefixOption> iaPrefixOption;
        if (iaPdOption.isPresent()) {
            log.warn("IA_PD option found {}", iaPdOption);

            iaPrefixOption = iaPdOption.get().getOptions().stream()
                    .filter(opt -> opt instanceof Dhcp6IaPrefixOption)
                    .map(opt -> (Dhcp6IaPrefixOption) opt)
                    .findFirst();
        } else {
            log.warn("IA_PD option NOT found");

            iaPrefixOption = Optional.empty();
        }
        if (iaPrefixOption.isPresent()) {
            log.warn("IAPrefix Option within IA_PD option found {}", iaPrefixOption);

            prefixAddress = iaPrefixOption.get().getIp6Prefix();
            int prefixLen = (int) iaPrefixOption.get().getPrefixLength();
            log.warn("Prefix length is  {} bits", prefixLen);
            prefixPrefix = IpPrefix.valueOf(prefixAddress, prefixLen);

        } else {
            log.warn("Can't find IPv6 prefix from DHCPv6 {}", dhcp6);
        }

        return prefixPrefix;
    }

    /**
     * remove host or route.
     *
     * @param directConnFlag  flag to show that packet is from directly connected client
     * @param dhcp6Packet the dhcp6 payload
     * @param clientPacket client's ethernet packet
     * @param clientIpv6 client's Ipv6 packet
     * @param clientInterface client interfaces
     */
    private void removeHostOrRoute(boolean directConnFlag, DHCP6 dhcp6Packet,
                                   Ethernet clientPacket, IPv6 clientIpv6,
                                   Interface clientInterface) {
        log.debug("extractPrefix  enters {}", dhcp6Packet);
        VlanId vlanId = clientInterface.vlan();
        MacAddress clientMac = clientPacket.getSourceMAC();
        log.debug("client mac {} client vlan {}", HexString.toHexString(clientMac.toBytes(), ":"), vlanId);

        // add host or route
        if (isDhcp6Release(dhcp6Packet)) {
            IpAddress ip = null;
            if (directConnFlag) {
                // Add to host store if it is connected to network directly
                ip = extractIpAddress(dhcp6Packet);
                if (ip != null) {

                    HostId hostId = HostId.hostId(clientMac, vlanId);
                    log.debug("remove Host {} ip for directly connected.", hostId.toString());
                    // Remove host's ip of  when dhcp release msg is received
                    providerService.removeIpFromHost(hostId, ip);
                } else {
                    log.debug("ipAddress not found. Do not add Host for directly connected.");
                }
            } else {
                // Remove from route store if it is not connected to network directly
                // pick out the first link-local ip address
                IpAddress nextHopIp = getFirstIpByHost(clientMac, vlanId);
                if (nextHopIp == null) {
                    log.warn("Can't find link-local IP address of gateway mac {} vlanId {}",
                            clientMac, vlanId);
                    return;
                }

                DHCP6 leafDhcp = getDhcp6Leaf(dhcp6Packet);
                ip = extractIpAddress(leafDhcp);
                if (ip == null) {
                    log.debug("ip is null");
                } else {
                    Route routeForIP = new Route(Route.Source.STATIC, ip.toIpPrefix(), nextHopIp);
                    log.debug("removing route of 128 address for indirectly connected.");
                    log.debug("128 ip {}, nexthop {}", HexString.toHexString(ip.toOctets(), ":"),
                            HexString.toHexString(nextHopIp.toOctets(), ":"));
                    routeStore.removeRoute(routeForIP);
                }

                IpPrefix ipPrefix = extractPrefix(leafDhcp);
                if (ipPrefix == null) {
                    log.debug("ipPrefix is null ");
                } else {
                    Route routeForPrefix = new Route(Route.Source.STATIC, ipPrefix, nextHopIp);
                    log.debug("removing route of PD for indirectly connected.");
                    log.debug("pd ip {}, nexthop {}", HexString.toHexString(ipPrefix.address().toOctets(), ":"),
                            HexString.toHexString(nextHopIp.toOctets(), ":"));

                    routeStore.removeRoute(routeForPrefix);
                    if (this.dhcpFpmEnabled) {
                        dhcpFpmPrefixStore.removeFpmRecord(ipPrefix);
                    }
                }
            }
        }
    }

    /**
     * add host or route.
     *
     * @param directConnFlag  flag to show that packet is from directly connected client
     * @param dhcp6Relay the dhcp6 payload
     * @param embeddedDhcp6 client's ethernet packetthe dhcp6 payload within relay
     * @param clientMac client macAddress
     * @param clientInterface client interface
     */
    private void addHostOrRoute(boolean directConnFlag, DHCP6 dhcp6Relay,
                                   DHCP6 embeddedDhcp6,
                                   MacAddress clientMac,
                                   Interface clientInterface) {
        log.debug("addHostOrRoute entered.");
        VlanId vlanId = clientInterface.vlan();
        // add host or route
        if (isDhcp6Reply(dhcp6Relay)) {
            IpAddress ip = null;
            if (directConnFlag) {
                // Add to host store if it connect to network directly
                ip = extractIpAddress(embeddedDhcp6);
                if (ip != null) {
                    Set<IpAddress> ips = Sets.newHashSet(ip);

                    // FIXME: we should use vlan id from original packet (solicit, request)
                    HostId hostId = HostId.hostId(clientMac, vlanId);
                    Host host = hostService.getHost(hostId);
                    HostLocation hostLocation = new HostLocation(clientInterface.connectPoint(),
                                                                 System.currentTimeMillis());
                    Set<HostLocation> hostLocations = Sets.newHashSet(hostLocation);

                    if (host != null) {
                        // Dual homing support:
                        // if host exists, use old locations and new location
                        hostLocations.addAll(host.locations());
                    }
                    HostDescription desc = new DefaultHostDescription(clientMac, vlanId,
                                                                      hostLocations, ips,
                                                                      false);
                    log.debug("adding Host for directly connected.");
                    log.debug("client mac {} client vlan {} hostlocation {}",
                            HexString.toHexString(clientMac.toBytes(), ":"),
                            vlanId, hostLocation.toString());

                    // Replace the ip when dhcp server give the host new ip address
                    providerService.hostDetected(hostId, desc, false);
                } else {
                    log.warn("ipAddress not found. Do not add Host for directly connected.");
                }
            } else {
                // Add to route store if it does not connect to network directly
                // pick out the first link-local ip address
                IpAddress nextHopIp = getFirstIpByHost(clientMac, vlanId);
                if (nextHopIp == null) {
                    log.warn("Can't find link-local IP address of gateway mac {} vlanId {}",
                            clientMac, vlanId);
                    return;
                }

                DHCP6 leafDhcp = getDhcp6Leaf(embeddedDhcp6);
                ip = extractIpAddress(leafDhcp);
                if (ip == null) {
                    log.warn("ip is null");
                } else {
                    Route routeForIP = new Route(Route.Source.STATIC, ip.toIpPrefix(), nextHopIp);
                    log.warn("adding Route of 128 address for indirectly connected.");
                    routeStore.updateRoute(routeForIP);
                }

                IpPrefix ipPrefix = extractPrefix(leafDhcp);
                if (ipPrefix == null) {
                    log.warn("ipPrefix is null ");
                } else {
                    Route routeForPrefix = new Route(Route.Source.STATIC, ipPrefix, nextHopIp);
                    log.warn("adding Route of PD for indirectly connected.");
                    routeStore.updateRoute(routeForPrefix);
                    if (this.dhcpFpmEnabled) {
                        FpmRecord record = new FpmRecord(ipPrefix, nextHopIp, FpmRecord.Type.DHCP_RELAY);
                        dhcpFpmPrefixStore.addFpmRecord(ipPrefix, record);
                    }
                }
            }
        }
    }

    /**
     * Build the DHCP6 solicit/request packet with gatewayip.
     * TODO: method too long, need to be refactored.
     *
     * @param context packet context
     * @param clientPacket client ethernet packet
     * @param clientInterfaces set of client side interfaces
     */
    private InternalPacket processDhcp6PacketFromClient(PacketContext context,
                                                        Ethernet clientPacket, Set<Interface> clientInterfaces) {
        ConnectPoint receivedFrom = context.inPacket().receivedFrom();
        DeviceId receivedFromDevice = receivedFrom.deviceId();
        DhcpServerInfo serverInfo;
        Ip6Address dhcpServerIp = null;
        ConnectPoint dhcpServerConnectPoint = null;
        MacAddress dhcpConnectMac = null;
        VlanId dhcpConnectVlan = null;
        Ip6Address dhcpGatewayIp = null;
        Ip6Address indirectDhcpServerIp = null;
        ConnectPoint indirectDhcpServerConnectPoint = null;
        MacAddress indirectDhcpConnectMac = null;
        VlanId indirectDhcpConnectVlan = null;
        Ip6Address indirectDhcpGatewayIp = null;
        Ip6Address indirectRelayAgentIpFromCfg = null;
        if (!defaultServerInfoList.isEmpty()) {
            serverInfo = defaultServerInfoList.get(0);
            dhcpConnectMac = serverInfo.getDhcpConnectMac().orElse(null);
            dhcpGatewayIp = serverInfo.getDhcpGatewayIp6().orElse(null);
            dhcpServerIp = serverInfo.getDhcpServerIp6().orElse(null);
            dhcpServerConnectPoint = serverInfo.getDhcpServerConnectPoint().orElse(null);
            dhcpConnectVlan = serverInfo.getDhcpConnectVlan().orElse(null);
        }
        if (!indirectServerInfoList.isEmpty()) {
            serverInfo = indirectServerInfoList.get(0);
            indirectDhcpConnectMac = serverInfo.getDhcpConnectMac().orElse(null);
            indirectDhcpGatewayIp = serverInfo.getDhcpGatewayIp6().orElse(null);
            indirectDhcpServerIp = serverInfo.getDhcpServerIp6().orElse(null);
            indirectDhcpServerConnectPoint = serverInfo.getDhcpServerConnectPoint().orElse(null);
            indirectDhcpConnectVlan = serverInfo.getDhcpConnectVlan().orElse(null);
            indirectRelayAgentIpFromCfg = serverInfo.getRelayAgentIp6(receivedFromDevice).orElse(null);
        }
        Ip6Address relayAgentIp = getRelayAgentIPv6Address(clientInterfaces);
        MacAddress relayAgentMac = clientInterfaces.iterator().next().mac();
        if (relayAgentIp == null || relayAgentMac == null) {
            log.warn("Missing DHCP relay agent interface Ipv6 addr config for "
                             + "packet from client on port: {}. Aborting packet processing",
                     clientInterfaces.iterator().next().connectPoint());
            return null;
        }
        // get dhcp6 header.
        IPv6 clientIpv6 = (IPv6) clientPacket.getPayload();
        UDP clientUdp = (UDP) clientIpv6.getPayload();
        DHCP6 clientDhcp6 = (DHCP6) clientUdp.getPayload();
        boolean directConnFlag = directlyConnected(clientDhcp6);
        Interface serverInterface;
        if (directConnFlag) {
            serverInterface = getServerInterface();
        } else {
            serverInterface = getIndirectServerInterface();
            if (serverInterface == null) {
                // Indirect server interface not found, use default server interface
                serverInterface = getServerInterface();
            }
        }
        if (serverInterface == null) {
            log.warn("Can't get {} server interface, ignore", directConnFlag ? "direct" : "indirect");
            return null;
        }
        Ip6Address ipFacingServer = getFirstIpFromInterface(serverInterface);
        MacAddress macFacingServer = serverInterface.mac();
        if (ipFacingServer == null || macFacingServer == null) {
            log.warn("No IP v6 address for server Interface {}", serverInterface);
            return null;
        }
        Ethernet etherReply = (Ethernet) clientPacket.duplicate();
        etherReply.setSourceMACAddress(macFacingServer);
        if ((directConnFlag && dhcpConnectMac == null)  ||
                !directConnFlag && indirectDhcpConnectMac == null && dhcpConnectMac == null)   {
            log.warn("Packet received from {} connected client.", directConnFlag ? "directly" : "indirectly");
            log.warn("DHCP6 {} not yet resolved .. Aborting DHCP packet processing from client on port: {}",
                     (dhcpGatewayIp == null) ? "server IP " + dhcpServerIp
                             : "gateway IP " + dhcpGatewayIp,
                     clientInterfaces.iterator().next().connectPoint());
            return null;
        }
        if (dhcpServerConnectPoint == null) {
            log.warn("DHCP6 server connection point direct {} directConn {} indirectConn {} is not set up yet",
                     directConnFlag, dhcpServerConnectPoint, indirectDhcpServerConnectPoint);
            return null;
        }

        etherReply.setDestinationMACAddress(dhcpConnectMac);
        etherReply.setVlanID(dhcpConnectVlan.toShort());
        IPv6 ipv6Packet = (IPv6) etherReply.getPayload();
        byte[] peerAddress = clientIpv6.getSourceAddress();
        ipv6Packet.setSourceAddress(ipFacingServer.toOctets());
        ipv6Packet.setDestinationAddress(dhcpServerIp.toOctets());
        UDP udpPacket = (UDP) ipv6Packet.getPayload();
        udpPacket.setSourcePort(UDP.DHCP_V6_SERVER_PORT);
        DHCP6 dhcp6Packet = (DHCP6) udpPacket.getPayload();
        byte[] dhcp6PacketByte = dhcp6Packet.serialize();

        ConnectPoint clientConnectionPoint = context.inPacket().receivedFrom();
        VlanId vlanIdInUse = VlanId.vlanId(clientPacket.getVlanID());
        Interface clientInterface = interfaceService.getInterfacesByPort(clientConnectionPoint)
                .stream().filter(iface -> interfaceContainsVlan(iface, vlanIdInUse))
                .findFirst().orElse(null);

        removeHostOrRoute(directConnFlag, dhcp6Packet, clientPacket, clientIpv6, clientInterface);

        DHCP6 dhcp6Relay = new DHCP6();
        dhcp6Relay.setMsgType(DHCP6.MsgType.RELAY_FORW.value());
        if (directConnFlag) {
            dhcp6Relay.setLinkAddress(relayAgentIp.toOctets());
            log.debug("direct connection: relayAgentIp obtained dynamically {}",
                      HexString.toHexString(relayAgentIp.toOctets(), ":"));

        } else {
            if (indirectDhcpServerIp == null) {
                log.warn("indirect DhcpServerIp not available, use default DhcpServerIp {}",
                         HexString.toHexString(dhcpServerIp.toOctets()));
             } else {
                 // Indirect case, replace destination to indirect dhcp server if exist
                 // Check if mac is obtained for valid server ip
                 if (indirectDhcpConnectMac == null) {
                     log.warn("DHCP6 {} not yet resolved .. Aborting DHCP "
                                      + "packet processing from client on port: {}",
                              (indirectDhcpGatewayIp == null) ? "server IP " + indirectDhcpServerIp
                                      : "gateway IP " + indirectDhcpGatewayIp,
                              clientInterfaces.iterator().next().connectPoint());
                     return null;
                 }
                 etherReply.setDestinationMACAddress(indirectDhcpConnectMac);
                 etherReply.setVlanID(indirectDhcpConnectVlan.toShort());
                 ipv6Packet.setDestinationAddress(indirectDhcpServerIp.toOctets());
             }
             if (indirectRelayAgentIpFromCfg == null) {
                 dhcp6Relay.setLinkAddress(relayAgentIp.toOctets());
                 log.warn("indirect connection: relayAgentIp NOT availale from config file! Use dynamic. {}",
                          HexString.toHexString(relayAgentIp.toOctets(), ":"));
             } else {
                 dhcp6Relay.setLinkAddress(indirectRelayAgentIpFromCfg.toOctets());
                 log.debug("indirect connection: relayAgentIp from config file is available! {}",
                           HexString.toHexString(indirectRelayAgentIpFromCfg.toOctets(), ":"));
             }
         }
         // peer address: address of the client or relay agent from which
         // the message to be relayed was received.
         dhcp6Relay.setPeerAddress(peerAddress);
         List<Dhcp6Option> options = new ArrayList<>();
         // directly connected case, hop count is zero; otherwise, hop count + 1
         if (directConnFlag) {
             dhcp6Relay.setHopCount((byte) 0);
         } else {
             dhcp6Relay.setHopCount((byte) (dhcp6Packet.getHopCount() + 1));
         }
         // create relay message option
         Dhcp6Option relayMessage = new Dhcp6Option();
         relayMessage.setCode(DHCP6.OptionCode.RELAY_MSG.value());
         relayMessage.setLength((short) dhcp6PacketByte.length);
         relayMessage.setData(dhcp6PacketByte);
         options.add(relayMessage);
         // create interfaceId option
         String inPortString = "-" + context.inPacket().receivedFrom().toString() + ":";
         Dhcp6Option interfaceId = new Dhcp6Option();
         interfaceId.setCode(DHCP6.OptionCode.INTERFACE_ID.value());
         byte[] clientSoureMacBytes = clientPacket.getSourceMACAddress();
         byte[] inPortStringBytes = inPortString.getBytes();
         byte[] vlanIdBytes = new byte[2];
         vlanIdBytes[0] = (byte) (clientPacket.getVlanID() & 0xff);
         vlanIdBytes[1] = (byte) ((clientPacket.getVlanID() >> 8) & 0xff);
         byte[] interfaceIdBytes = new byte[clientSoureMacBytes.length +
                 inPortStringBytes.length + vlanIdBytes.length];
         log.debug("Length: interfaceIdBytes  {} clientSoureMacBytes {} inPortStringBytes {} vlan {}",
                   interfaceIdBytes.length, clientSoureMacBytes.length, inPortStringBytes.length,
                   vlanIdBytes.length);
         System.arraycopy(clientSoureMacBytes, 0, interfaceIdBytes, 0, clientSoureMacBytes.length);
         System.arraycopy(inPortStringBytes, 0, interfaceIdBytes, clientSoureMacBytes.length, inPortStringBytes.length);
         System.arraycopy(vlanIdBytes, 0, interfaceIdBytes, clientSoureMacBytes.length + inPortStringBytes.length,
                          vlanIdBytes.length);
         interfaceId.setData(interfaceIdBytes);
         interfaceId.setLength((short) interfaceIdBytes.length);
         options.add(interfaceId);
         log.debug("interfaceId write srcMac {} portString {}",
                   HexString.toHexString(clientSoureMacBytes, ":"), inPortString);
         dhcp6Relay.setOptions(options);
         udpPacket.setPayload(dhcp6Relay);
         udpPacket.resetChecksum();
         ipv6Packet.setPayload(udpPacket);
         ipv6Packet.setHopLimit((byte) 64);
         etherReply.setPayload(ipv6Packet);
         if (directConnFlag || indirectDhcpServerIp == null) {
             return new InternalPacket(etherReply, dhcpServerConnectPoint);
         } else {
             return new InternalPacket(etherReply, indirectDhcpServerConnectPoint);
         }
     }

    /**
     *
     * process the DHCP6 relay-reply packet from dhcp server.
     *
     * @param context packet context
     * @param receivedPacket server ethernet packet
     * @param recevingInterfaces set of server side interfaces
     */
    private InternalPacket processDhcp6PacketFromServer(PacketContext context,
                                                        Ethernet receivedPacket, Set<Interface> recevingInterfaces) {

        ConnectPoint receivedFrom = context.inPacket().receivedFrom();
        DeviceId receivedFromDevice = receivedFrom.deviceId();

        // TODO: refactor
        DhcpServerInfo serverInfo;
        Ip6Address dhcpServerIp = null;
        ConnectPoint dhcpServerConnectPoint = null;
        MacAddress dhcpConnectMac = null;
        VlanId dhcpConnectVlan = null;
        Ip6Address dhcpGatewayIp = null;

        Ip6Address indirectDhcpServerIp = null;
        ConnectPoint indirectDhcpServerConnectPoint = null;
        MacAddress indirectDhcpConnectMac = null;
        VlanId indirectDhcpConnectVlan = null;
        Ip6Address indirectDhcpGatewayIp = null;
        Ip6Address indirectRelayAgentIpFromCfg = null;

        if (!defaultServerInfoList.isEmpty()) {
            serverInfo = defaultServerInfoList.get(0);
            dhcpConnectMac = serverInfo.getDhcpConnectMac().orElse(null);
            dhcpGatewayIp = serverInfo.getDhcpGatewayIp6().orElse(null);
            dhcpServerIp = serverInfo.getDhcpServerIp6().orElse(null);
            dhcpServerConnectPoint = serverInfo.getDhcpServerConnectPoint().orElse(null);
            dhcpConnectVlan = serverInfo.getDhcpConnectVlan().orElse(null);
        }

        if (!indirectServerInfoList.isEmpty()) {
            serverInfo = indirectServerInfoList.get(0);
            indirectDhcpConnectMac = serverInfo.getDhcpConnectMac().orElse(null);
            indirectDhcpGatewayIp = serverInfo.getDhcpGatewayIp6().orElse(null);
            indirectDhcpServerIp = serverInfo.getDhcpServerIp6().orElse(null);
            indirectDhcpServerConnectPoint = serverInfo.getDhcpServerConnectPoint().orElse(null);
            indirectDhcpConnectVlan = serverInfo.getDhcpConnectVlan().orElse(null);
            indirectRelayAgentIpFromCfg = serverInfo.getRelayAgentIp6(receivedFromDevice).orElse(null);
        }

        // get dhcp6 header.
        Ethernet etherReply = receivedPacket.duplicate();
        IPv6 ipv6Packet = (IPv6) etherReply.getPayload();
        UDP udpPacket = (UDP) ipv6Packet.getPayload();
        DHCP6 dhcp6Relay = (DHCP6) udpPacket.getPayload();

        Boolean directConnFlag = directlyConnected(dhcp6Relay);
        ConnectPoint inPort = context.inPacket().receivedFrom();
        if ((directConnFlag || (!directConnFlag && indirectDhcpServerIp == null))
             && !inPort.equals(dhcpServerConnectPoint)) {
            log.warn("Receiving port {} is not the same as server connect point {} for direct or indirect-null",
                    inPort, dhcpServerConnectPoint);
            return null;
        }

        if (!directConnFlag && indirectDhcpServerIp != null &&
                                !inPort.equals(indirectDhcpServerConnectPoint)) {
            log.warn("Receiving port {} is not the same as server connect point {} for indirect",
                    inPort, indirectDhcpServerConnectPoint);
            return null;
        }


        Dhcp6InterfaceIdOption interfaceIdOption = dhcp6Relay.getOptions().stream()
                .filter(opt -> opt instanceof Dhcp6InterfaceIdOption)
                .map(opt -> (Dhcp6InterfaceIdOption) opt)
                .findFirst()
                .orElse(null);

        if (interfaceIdOption == null) {
            log.warn("Interface Id option is not present, abort packet...");
            return null;
        }

        MacAddress peerMac = interfaceIdOption.getMacAddress();
        String clientConnectionPointStr = new String(interfaceIdOption.getInPort());

        ConnectPoint clientConnectionPoint = ConnectPoint.deviceConnectPoint(clientConnectionPointStr);
        VlanId vlanIdInUse = VlanId.vlanId(interfaceIdOption.getVlanId());
        Interface clientInterface = interfaceService.getInterfacesByPort(clientConnectionPoint)
                .stream()
                .filter(iface -> interfaceContainsVlan(iface, vlanIdInUse))
                .findFirst()
                .orElse(null);
        if (clientInterface == null) {
            log.warn("Cannot get client interface for from packet, abort... vlan {}", vlanIdInUse.toString());
            return null;
        }
        MacAddress relayAgentMac = clientInterface.mac();
        if (relayAgentMac == null) {
            log.warn("Can not get interface mac, abort packet..");
            return null;
        }
        etherReply.setSourceMACAddress(relayAgentMac);

        // find destMac
        MacAddress clientMac = null;
        Ip6Address peerAddress = Ip6Address.valueOf(dhcp6Relay.getPeerAddress());
        Set<Host> clients = hostService.getHostsByIp(peerAddress);
        if (clients.isEmpty()) {
            log.warn("There's no host found for this address {}",
                    HexString.toHexString(dhcp6Relay.getPeerAddress(), ":"));
            log.warn("Let's look up interfaceId {}", HexString.toHexString(peerMac.toBytes(), ":"));
            clientMac = peerMac;
        } else {
            clientMac = clients.iterator().next().mac();
            if (clientMac == null) {
                log.warn("No client mac address found, abort packet...");
                return null;
            }
            log.warn("Client mac address found from getHostByIp");

        }
        etherReply.setDestinationMACAddress(clientMac);

        // ip header
        ipv6Packet.setSourceAddress(dhcp6Relay.getLinkAddress());
        ipv6Packet.setDestinationAddress(dhcp6Relay.getPeerAddress());
        // udp header
        udpPacket.setSourcePort(UDP.DHCP_V6_SERVER_PORT);
        if (directConnFlag) {
            udpPacket.setDestinationPort(UDP.DHCP_V6_CLIENT_PORT);
        } else {
            udpPacket.setDestinationPort(UDP.DHCP_V6_SERVER_PORT);
        }

        DHCP6 embeddedDhcp6 = dhcp6Relay.getOptions().stream()
                    .filter(opt -> opt instanceof Dhcp6RelayOption)
                    .map(BasePacket::getPayload)
                    .map(pld -> (DHCP6) pld)
                    .findFirst()
                    .orElse(null);


        // add host or route
        addHostOrRoute(directConnFlag, dhcp6Relay, embeddedDhcp6, clientMac, clientInterface);

        udpPacket.setPayload(embeddedDhcp6);
        udpPacket.resetChecksum();
        ipv6Packet.setPayload(udpPacket);
        etherReply.setPayload(ipv6Packet);

        return new InternalPacket(etherReply, clientConnectionPoint);
    }

    // Returns the first v6 interface ip out of a set of interfaces or null.
    // Checks all interfaces, and ignores v6 interface ips
    private Ip6Address getRelayAgentIPv6Address(Set<Interface> intfs) {
        for (Interface intf : intfs) {
            for (InterfaceIpAddress ip : intf.ipAddressesList()) {
                Ip6Address relayAgentIp = ip.ipAddress().getIp6Address();
                if (relayAgentIp != null) {
                    return relayAgentIp;
                }
            }
        }
        return null;
    }

    @Override
    public void setDhcpFpmEnabled(Boolean enabled) {
       dhcpFpmEnabled = enabled;
    }

    @Override
    public void setDefaultDhcpServerConfigs(Collection<DhcpServerConfig> configs) {
        setDhcpServerConfigs(configs, defaultServerInfoList);
    }

    @Override
    public void setIndirectDhcpServerConfigs(Collection<DhcpServerConfig> configs) {
        setDhcpServerConfigs(configs, indirectServerInfoList);
    }

    public void setDhcpServerConfigs(Collection<DhcpServerConfig> configs, List<DhcpServerInfo> serverInfoList) {
        if (configs.size() == 0) {
            // no config to update
            return;
        }

        // TODO: currently we pick up first DHCP server config.
        // Will use other server configs in the future for HA.
        DhcpServerConfig serverConfig = configs.iterator().next();

        if (!serverConfig.getDhcpServerIp6().isPresent()) {
            // not a DHCPv6 config
            return;
        }

        if (!serverInfoList.isEmpty()) {
            // remove old server info
            DhcpServerInfo oldServerInfo = serverInfoList.remove(0);

            // stop monitoring gateway or server
            oldServerInfo.getDhcpGatewayIp6().ifPresent(gatewayIp -> {
                hostService.stopMonitoringIp(gatewayIp);
            });
            oldServerInfo.getDhcpServerIp6().ifPresent(serverIp -> {
                hostService.stopMonitoringIp(serverIp);
                cancelDhcpPacket(serverIp);
            });
        }

        // Create new server info according to the config
        DhcpServerInfo newServerInfo = new DhcpServerInfo(serverConfig,
                                                          DhcpServerInfo.Version.DHCP_V6);
        checkState(newServerInfo.getDhcpServerConnectPoint().isPresent(),
                   "Connect point not exists");
        checkState(newServerInfo.getDhcpServerIp6().isPresent(),
                   "IP of DHCP server not exists");

        log.debug("DHCP server connect point: {}", newServerInfo.getDhcpServerConnectPoint().orElse(null));
        log.debug("DHCP server IP: {}", newServerInfo.getDhcpServerIp6().orElse(null));

        Ip6Address serverIp = newServerInfo.getDhcpServerIp6().get();
        Ip6Address ipToProbe;
        if (newServerInfo.getDhcpGatewayIp6().isPresent()) {
            ipToProbe = newServerInfo.getDhcpGatewayIp6().get();
        } else {
            ipToProbe = newServerInfo.getDhcpServerIp6().orElse(null);
        }
        String hostToProbe = newServerInfo.getDhcpGatewayIp6()
                .map(ip -> "gateway").orElse("server");

        log.debug("Probing to resolve {} IP {}", hostToProbe, ipToProbe);
        hostService.startMonitoringIp(ipToProbe);

        Set<Host> hosts = hostService.getHostsByIp(ipToProbe);
        if (!hosts.isEmpty()) {
            Host host = hosts.iterator().next();
            newServerInfo.setDhcpConnectVlan(host.vlan());
            newServerInfo.setDhcpConnectMac(host.mac());
        }
        // Add new server info
        synchronized (this) {
            serverInfoList.clear();
            serverInfoList.add(0, newServerInfo);
        }
        requestDhcpPacket(serverIp);
    }

    class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            switch (event.type()) {
                case HOST_ADDED:
                case HOST_UPDATED:
                    hostUpdated(event.subject());
                    break;
                case HOST_REMOVED:
                    hostRemoved(event.subject());
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Handle host updated.
     * If the host is DHCP server or gateway, update connect mac and vlan.
     *
     * @param host the host
     */
    private void hostUpdated(Host host) {
        hostUpdated(host, defaultServerInfoList);
        hostUpdated(host, indirectServerInfoList);
    }

    private void hostUpdated(Host host, List<DhcpServerInfo> serverInfoList) {
        DhcpServerInfo serverInfo;
        Ip6Address targetIp;
        if (!serverInfoList.isEmpty()) {
            serverInfo = serverInfoList.get(0);
            Ip6Address serverIp = serverInfo.getDhcpServerIp6().orElse(null);
            targetIp = serverInfo.getDhcpGatewayIp6().orElse(null);

            if (targetIp == null) {
                targetIp = serverIp;
            }

            if (targetIp != null) {
                if (host.ipAddresses().contains(targetIp)) {
                    serverInfo.setDhcpConnectMac(host.mac());
                    serverInfo.setDhcpConnectVlan(host.vlan());
                    requestDhcpPacket(serverIp);
                }
            }
        }
    }

    /**
     * Handle host removed.
     * If the host is DHCP server or gateway, unset connect mac and vlan.
     *
     * @param host the host
     */
    private void hostRemoved(Host host) {
        hostRemoved(host, defaultServerInfoList);
        hostRemoved(host, indirectServerInfoList);
    }

    private void hostRemoved(Host host, List<DhcpServerInfo> serverInfoList) {
        DhcpServerInfo serverInfo;
        Ip6Address targetIp;

        if (!serverInfoList.isEmpty()) {
            serverInfo = serverInfoList.get(0);
            Ip6Address serverIp = serverInfo.getDhcpServerIp6().orElse(null);
            targetIp = serverInfo.getDhcpGatewayIp6().orElse(null);

            if (targetIp == null) {
                targetIp = serverIp;
            }

            if (targetIp != null) {
                if (host.ipAddresses().contains(targetIp)) {
                    serverInfo.setDhcpConnectVlan(null);
                    serverInfo.setDhcpConnectMac(null);
                    cancelDhcpPacket(serverIp);
                }
            }
        }
    }

    /**
     * Returns the first interface ip from interface.
     *
     * @param iface interface of one connect point
     * @return the first interface IP; null if not exists an IP address in
     *         these interfaces
     */
    private Ip6Address getFirstIpFromInterface(Interface iface) {
        checkNotNull(iface, "Interface can't be null");
        return iface.ipAddressesList().stream()
                .map(InterfaceIpAddress::ipAddress)
                .filter(IpAddress::isIp6)
                .map(IpAddress::getIp6Address)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets Interface facing to the server for default host.
     *
     * @return the Interface facing to the server; null if not found
     */
    private Interface getServerInterface() {
        DhcpServerInfo serverInfo;
        ConnectPoint dhcpServerConnectPoint;
        VlanId dhcpConnectVlan;

        if (!defaultServerInfoList.isEmpty()) {
            serverInfo = defaultServerInfoList.get(0);
            dhcpServerConnectPoint = serverInfo.getDhcpServerConnectPoint().orElse(null);
            dhcpConnectVlan = serverInfo.getDhcpConnectVlan().orElse(null);
        } else {
            return null;
        }
        if (dhcpServerConnectPoint == null || dhcpConnectVlan == null) {
            log.info("Default DHCP server {} not resolve yet", serverInfo.getDhcpGatewayIp6());
            return null;
        }
        return interfaceService.getInterfacesByPort(dhcpServerConnectPoint)
                .stream()
                .filter(iface -> interfaceContainsVlan(iface, dhcpConnectVlan))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets Interface facing to the server for indirect hosts.
     * Use default server Interface if indirect server not configured.
     *
     * @return the Interface facing to the server; null if not found
     */
    private Interface getIndirectServerInterface() {
        DhcpServerInfo serverInfo;

        ConnectPoint indirectDhcpServerConnectPoint;
        VlanId indirectDhcpConnectVlan;

        if (!indirectServerInfoList.isEmpty()) {
            serverInfo = indirectServerInfoList.get(0);
            indirectDhcpServerConnectPoint = serverInfo.getDhcpServerConnectPoint().orElse(null);
            indirectDhcpConnectVlan = serverInfo.getDhcpConnectVlan().orElse(null);
        } else {
            return getServerInterface();
        }
        if (indirectDhcpServerConnectPoint == null || indirectDhcpConnectVlan == null) {
            log.info("Indirect DHCP server {} not resolve yet", serverInfo.getDhcpGatewayIp6());
            return null;
        }
        return interfaceService.getInterfacesByPort(indirectDhcpServerConnectPoint)
                .stream()
                .filter(iface -> interfaceContainsVlan(iface, indirectDhcpConnectVlan))
                .findFirst()
                .orElse(null);
    }

    /**
     * Determind if an Interface contains a vlan id.
     *
     * @param iface the Interface
     * @param vlanId the vlan id
     * @return true if the Interface contains the vlan id
     */
    private boolean interfaceContainsVlan(Interface iface, VlanId vlanId) {
        if (vlanId.equals(VlanId.NONE)) {
            // untagged packet, check if vlan untagged or vlan native is not NONE
            return !iface.vlanUntagged().equals(VlanId.NONE) ||
                    !iface.vlanNative().equals(VlanId.NONE);
        }
        // tagged packet, check if the interface contains the vlan
        return iface.vlanTagged().contains(vlanId);
    }

    private void requestDhcpPacket(Ip6Address serverIp) {
        requestServerDhcpPacket(serverIp);
        requestClientDhcpPacket(serverIp);
    }

    private void cancelDhcpPacket(Ip6Address serverIp) {
        cancelServerDhcpPacket(serverIp);
        cancelClientDhcpPacket(serverIp);
    }

    private void cancelServerDhcpPacket(Ip6Address serverIp) {
        TrafficSelector serverSelector =
                DefaultTrafficSelector.builder(SERVER_RELAY_SELECTOR)
                        .matchIPv6Src(serverIp.toIpPrefix())
                        .build();
        packetService.cancelPackets(serverSelector,
                                    PacketPriority.CONTROL,
                                    appId);
    }

    private void requestServerDhcpPacket(Ip6Address serverIp) {
        TrafficSelector serverSelector =
                DefaultTrafficSelector.builder(SERVER_RELAY_SELECTOR)
                        .matchIPv6Src(serverIp.toIpPrefix())
                        .build();
        packetService.requestPackets(serverSelector,
                                     PacketPriority.CONTROL,
                                     appId);
    }

    private void cancelClientDhcpPacket(Ip6Address serverIp) {
        // Packet comes from relay
        TrafficSelector indirectClientSelector =
                DefaultTrafficSelector.builder(SERVER_RELAY_SELECTOR)
                        .matchIPv6Dst(serverIp.toIpPrefix())
                        .build();
        packetService.cancelPackets(indirectClientSelector,
                                    PacketPriority.CONTROL,
                                    appId);
        indirectClientSelector =
                DefaultTrafficSelector.builder(SERVER_RELAY_SELECTOR)
                        .matchIPv6Dst(Ip6Address.ALL_DHCP_RELAY_AGENTS_AND_SERVERS.toIpPrefix())
                        .build();
        packetService.cancelPackets(indirectClientSelector,
                                    PacketPriority.CONTROL,
                                    appId);
        indirectClientSelector =
                DefaultTrafficSelector.builder(SERVER_RELAY_SELECTOR)
                        .matchIPv6Dst(Ip6Address.ALL_DHCP_SERVERS.toIpPrefix())
                        .build();
        packetService.cancelPackets(indirectClientSelector,
                                    PacketPriority.CONTROL,
                                    appId);

        // Packet comes from client
        packetService.cancelPackets(CLIENT_SERVER_SELECTOR,
                                    PacketPriority.CONTROL,
                                    appId);
    }

    private void requestClientDhcpPacket(Ip6Address serverIp) {
        // Packet comes from relay
        TrafficSelector indirectClientSelector =
                DefaultTrafficSelector.builder(SERVER_RELAY_SELECTOR)
                        .matchIPv6Dst(serverIp.toIpPrefix())
                        .build();
        packetService.requestPackets(indirectClientSelector,
                                     PacketPriority.CONTROL,
                                     appId);
        indirectClientSelector =
                DefaultTrafficSelector.builder(SERVER_RELAY_SELECTOR)
                        .matchIPv6Dst(Ip6Address.ALL_DHCP_RELAY_AGENTS_AND_SERVERS.toIpPrefix())
                        .build();
        packetService.requestPackets(indirectClientSelector,
                                     PacketPriority.CONTROL,
                                     appId);
        indirectClientSelector =
                DefaultTrafficSelector.builder(SERVER_RELAY_SELECTOR)
                        .matchIPv6Dst(Ip6Address.ALL_DHCP_SERVERS.toIpPrefix())
                        .build();
        packetService.requestPackets(indirectClientSelector,
                                     PacketPriority.CONTROL,
                                     appId);

        // Packet comes from client
        packetService.requestPackets(CLIENT_SERVER_SELECTOR,
                                     PacketPriority.CONTROL,
                                     appId);
    }

    /**
     * Process the ignore rules.
     *
     * @param deviceId the device id
     * @param vlanId the vlan to be ignored
     * @param op the operation, ADD to install; REMOVE to uninstall rules
     */
    private void processIgnoreVlanRule(DeviceId deviceId, VlanId vlanId, Objective.Operation op) {
        AtomicInteger installedCount = new AtomicInteger(DHCP_SELECTORS.size());
        DHCP_SELECTORS.forEach(trafficSelector -> {
            TrafficSelector selector = DefaultTrafficSelector.builder(trafficSelector)
                    .matchVlanId(vlanId)
                    .build();

            ForwardingObjective.Builder builder = DefaultForwardingObjective.builder()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withSelector(selector)
                    .withPriority(IGNORE_CONTROL_PRIORITY)
                    .withTreatment(DefaultTrafficTreatment.emptyTreatment())
                    .fromApp(appId);


            ObjectiveContext objectiveContext = new ObjectiveContext() {
                @Override
                public void onSuccess(Objective objective) {
                    log.info("Ignore rule {} (Vlan id {}, device {}, selector {})",
                             op, vlanId, deviceId, selector);
                    int countDown = installedCount.decrementAndGet();
                    if (countDown != 0) {
                        return;
                    }
                    switch (op) {
                        case ADD:
                            ignoredVlans.put(deviceId, vlanId);
                            break;
                        case REMOVE:
                            ignoredVlans.remove(deviceId, vlanId);
                            break;
                        default:
                            log.warn("Unsupported objective operation {}", op);
                            break;
                    }
                }

                @Override
                public void onError(Objective objective, ObjectiveError error) {
                    log.warn("Can't {} ignore rule (vlan id {}, selector {}, device {}) due to {}",
                             op, vlanId, selector, deviceId, error);
                }
            };

            ForwardingObjective fwd;
            switch (op) {
                case ADD:
                    fwd = builder.add(objectiveContext);
                    break;
                case REMOVE:
                    fwd = builder.remove(objectiveContext);
                    break;
                default:
                    log.warn("Unsupported objective operation {}", op);
                    return;
            }

            Device device = deviceService.getDevice(deviceId);
            if (device == null || !device.is(Pipeliner.class)) {
                log.warn("Device {} is not available now, wait until device is available", deviceId);
                return;
            }
            flowObjectiveService.apply(deviceId, fwd);
        });
    }

    /**
     * Find first ipaddress for a given Host info i.e.  mac and vlan.
     *
     * @param clientMac client mac
     * @param vlanId  packet's vlan
     * @return next-hop link-local ipaddress for a given host
     */
    private IpAddress getFirstIpByHost(MacAddress clientMac, VlanId vlanId) {
        IpAddress nextHopIp;
        // pick out the first link-local ip address
        HostId gwHostId = HostId.hostId(clientMac, vlanId);
        Host gwHost = hostService.getHost(gwHostId);
        if (gwHost == null) {
            log.warn("Can't find gateway host for hostId {}", gwHostId);
            return null;
        }
        nextHopIp = gwHost.ipAddresses()
                .stream()
                .filter(IpAddress::isIp6)
                .filter(ip6 -> ip6.isLinkLocal())
                .map(IpAddress::getIp6Address)
                .findFirst()
                .orElse(null);
        return nextHopIp;
    }
}
