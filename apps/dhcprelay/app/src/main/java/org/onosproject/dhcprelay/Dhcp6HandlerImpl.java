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
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableSet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
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
import org.onlab.packet.dhcp.Dhcp6ClientDataOption;
import org.onlab.packet.dhcp.Dhcp6LeaseQueryOption;
import org.onlab.packet.dhcp.Dhcp6RelayOption;
import org.onlab.packet.dhcp.Dhcp6InterfaceIdOption;
import org.onlab.packet.dhcp.Dhcp6Option;
import org.onlab.packet.dhcp.Dhcp6IaNaOption;
import org.onlab.packet.dhcp.Dhcp6IaTaOption;
import org.onlab.packet.dhcp.Dhcp6IaPdOption;
import org.onlab.packet.dhcp.Dhcp6IaAddressOption;
import org.onlab.packet.dhcp.Dhcp6IaPrefixOption;
import org.onlab.packet.dhcp.Dhcp6ClientIdOption;
import org.onlab.packet.dhcp.Dhcp6Duid;
import org.onlab.packet.DHCP6.MsgType;
import org.onlab.util.HexString;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.dhcprelay.api.DhcpHandler;
import org.onosproject.dhcprelay.api.DhcpServerInfo;
import org.onosproject.dhcprelay.config.IgnoreDhcpConfig;
import org.onosproject.dhcprelay.store.DhcpRelayStore;
import org.onosproject.dhcprelay.store.DhcpRecord;
import org.onosproject.dhcprelay.store.DhcpFpmPrefixStore;
import org.onosproject.dhcprelay.store.DhcpRelayCountersStore;
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
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.dhcprelay.OsgiPropertyConstants.LEARN_ROUTE_FROM_LEASE_QUERY;
import static org.onosproject.dhcprelay.OsgiPropertyConstants.LEARN_ROUTE_FROM_LEASE_QUERY_DEFAULT;
import static org.onosproject.net.flowobjective.Objective.Operation.ADD;
import static org.onosproject.net.flowobjective.Objective.Operation.REMOVE;
import java.util.concurrent.Semaphore;

@Component(
    service = { DhcpHandler.class, HostProvider.class },
    property = {
        "_version:Integer = 6",
        LEARN_ROUTE_FROM_LEASE_QUERY + ":Boolean=" + LEARN_ROUTE_FROM_LEASE_QUERY_DEFAULT
    }
)

public class Dhcp6HandlerImpl implements DhcpHandler, HostProvider {
    public static final String DHCP_V6_RELAY_APP = "org.onosproject.Dhcp6HandlerImpl";
    public static final ProviderId PROVIDER_ID = new ProviderId("dhcp6", DHCP_V6_RELAY_APP);
    private static final int IGNORE_CONTROL_PRIORITY = PacketPriority.CONTROL.priorityValue() + 1000;
    private String gCount = "global";
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
    // lease query reply is from server to client (no relay in between) - so we need to
    // catch that scenario also ..
    private static final TrafficSelector LEASE_QUERY_RESPONSE_SELECTOR = DefaultTrafficSelector.builder()
            .matchEthType(Ethernet.TYPE_IPV6)
            .matchIPProtocol(IPv6.PROTOCOL_UDP)
            .matchUdpSrc(TpPort.tpPort(UDP.DHCP_V6_SERVER_PORT))
            .matchUdpDst(TpPort.tpPort(UDP.DHCP_V6_CLIENT_PORT))
            .build();
    static final Set<TrafficSelector> DHCP_SELECTORS = ImmutableSet.of(
            CLIENT_SERVER_SELECTOR,
            SERVER_RELAY_SELECTOR,
            LEASE_QUERY_RESPONSE_SELECTOR
    );
    private static Logger log = LoggerFactory.getLogger(Dhcp6HandlerImpl.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DhcpRelayStore dhcpRelayStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DhcpRelayCountersStore dhcpRelayCountersStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected RouteStore routeStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DhcpFpmPrefixStore dhcpFpmPrefixStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    /** Enable learning routing information from LQ. */
    private Boolean learnRouteFromLeasequery = LEARN_ROUTE_FROM_LEASE_QUERY_DEFAULT;

    protected HostProviderService providerService;
    protected ApplicationId appId;
    protected Multimap<DeviceId, VlanId> ignoredVlans = Multimaps.synchronizedMultimap(HashMultimap.create());
    private InternalHostListener hostListener = new InternalHostListener();
    private Boolean dhcpFpmEnabled = false;
    private List<DhcpServerInfo> defaultServerInfoList = new CopyOnWriteArrayList<>();
    private List<DhcpServerInfo> indirectServerInfoList = new CopyOnWriteArrayList<>();

    private Executor hostEventExecutor = newSingleThreadExecutor(
        groupedThreads("dhcp6-event-host", "%d", log));

    private class IpAddressInfo {
        Ip6Address ip6Address;
        long    prefTime;
    }
    private class PdPrefixInfo {
        IpPrefix pdPrefix;
        long    prefTime;
    }
    protected int dhcp6PollInterval = 24 * 3600; // 24 hr period

    // max 1 thread
    static Semaphore recordSemaphore = new Semaphore(1);

    // CLIENT message types
    public static final Set<Byte> MSG_TYPE_FROM_CLIENT =
            ImmutableSet.of(DHCP6.MsgType.SOLICIT.value(),
                            DHCP6.MsgType.REQUEST.value(),
                            DHCP6.MsgType.REBIND.value(),
                            DHCP6.MsgType.RENEW.value(),
                            DHCP6.MsgType.RELEASE.value(),
                            DHCP6.MsgType.DECLINE.value(),
                            DHCP6.MsgType.CONFIRM.value(),
                            DHCP6.MsgType.RELAY_FORW.value(),
                            DHCP6.MsgType.LEASEQUERY.value());
    // SERVER message types
    public static final Set<Byte> MSG_TYPE_FROM_SERVER =
            ImmutableSet.of(DHCP6.MsgType.RELAY_REPL.value(),
                            DHCP6.MsgType.LEASEQUERY_REPLY.value());

    @Activate
    protected void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        modified(context);
        appId = coreService.registerApplication(DHCP_V6_RELAY_APP);
        providerService = providerRegistry.register(this);
        hostService.addListener(hostListener);
    }

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        providerRegistry.unregister(this);
        hostService.removeListener(hostListener);
        defaultServerInfoList.forEach(this::stopMonitoringIps);
        defaultServerInfoList.forEach(info -> info.getDhcpServerIp6().ifPresent(this::cancelDhcpPacket));
        defaultServerInfoList.clear();
        indirectServerInfoList.forEach(this::stopMonitoringIps);
        indirectServerInfoList.forEach(info -> info.getDhcpServerIp6().ifPresent(this::cancelDhcpPacket));
        indirectServerInfoList.clear();
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Boolean flag;
        flag = Tools.isPropertyEnabled(properties, LEARN_ROUTE_FROM_LEASE_QUERY);
        if (flag != null) {
            learnRouteFromLeasequery = flag;
            log.info("Learning routes from DHCP leasequery is {}",
                    learnRouteFromLeasequery ? "enabled" : "disabled");
        }
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
    public void removeIgnoreVlanState(IgnoreDhcpConfig config) {
        if (config == null) {
            ignoredVlans.clear();
            return;
        }
        config.ignoredVlans().forEach((deviceId, vlanId) -> {
            ignoredVlans.remove(deviceId, vlanId);
        });
    }

    public DhcpRecord getDhcpRelayRecordFor(Ip6Address clientAddress) {

        Collection<DhcpRecord> records = dhcpRelayStore.getDhcpRecords();
        DhcpRecord dr = null;
        for (DhcpRecord e:records) {
            if (e.ip6Address().isPresent()) {
                if (e.ip6Address().get().equals(clientAddress)) {
                    dr = e;
                    break;
                }
            }
        }
        return dr;
    }

    public MacAddress findNextHopMacForIp6FromRelayStore(Ip6Address clientAddress,
                                                         MacAddress clientMacAddress, VlanId vlanID) {

        DhcpRecord dr = getDhcpRelayRecordFor(clientAddress);

        if (dr != null) {
           Optional<MacAddress> nextHopTempMac = dr.nextHopTemp();
            if (nextHopTempMac.isPresent()) {
                log.info("findNextHopForIp6FromRelayStore " + clientAddress + " got mac " + nextHopTempMac.toString());
                return nextHopTempMac.get();
            }
        } else {
            log.warn("findNextHopMacForIp6FromRelayStore could NOT find next hop for " + clientAddress);
            return null;
        }
        return null;
    }

    public Ip6Address findNextHopIp6FromRelayStore(Ip6Address clientAddress) {

        DhcpRecord dr = getDhcpRelayRecordFor(clientAddress);
        if (dr != null) {
            Optional<MacAddress> nextHopMac = dr.nextHop();
            if (nextHopMac.isPresent()) {
                // find the local ip6 from the host store
                HostId gwHostId = HostId.hostId(nextHopMac.get(), dr.vlanId());
                Host gwHost = hostService.getHost(gwHostId);
                if (gwHost == null) {
                    log.warn("Can't find next hop host ID {}", gwHostId);
                    return null;
                }
                Ip6Address nextHopIp = gwHost.ipAddresses()
                        .stream()
                        .filter(IpAddress::isIp6)
                        .filter(IpAddress::isLinkLocal)
                        .map(IpAddress::getIp6Address)
                        .findFirst()
                        .orElse(null);

                log.info("findNextHopIp6FromRelayStore " + clientAddress + " got mac " +
                                 nextHopMac.toString() + " ip6 " + nextHopIp);
                return nextHopIp;
            }
        } else {
            log.warn("findNextHopIp6FromRelayStore could NOT find next hop for " + clientAddress);
            return null;
        }
        return null;
    }

    private void setPotentialNextHopForIp6InRelayStore(Ip6Address clientAddress,
                                                       VlanId vlanId, MacAddress nh) {
        DhcpRecord dr = getDhcpRelayRecordFor(clientAddress);
        if (dr != null) {
            dr.nextHopTemp(nh);
            log.debug("LQ6 potential NH mac " + nh.toString() + " UPDATED in RelayRecord client " + clientAddress);
        } else {
            log.warn("LQ6 potential NH mac" + nh.toString() +
                             " NOT FOUND in RelayRecord for client - LQ rejected" + clientAddress);
        }
    }

    public void handleLeaseQuery6ReplyMsg(PacketContext context, DHCP6 dhcp6Payload) {
        ConnectPoint inPort = context.inPacket().receivedFrom();
        log.info("Got LQV6-REPLY on port {}", inPort);
        List<Dhcp6Option> lopt = dhcp6Payload.getOptions();
        log.info("Options list: {}", lopt);
        // find out if this lease is known is
        Dhcp6ClientDataOption clientDataOption = dhcp6Payload.getOptions()
                .stream()
                .filter(opt -> opt instanceof Dhcp6ClientDataOption)
                .map(pld -> (Dhcp6ClientDataOption) pld)
                .findFirst()
                .orElse(null);

        if (clientDataOption == null) {
            log.warn("clientDataOption option is not present, " +
                             "lease is UNKNOWN - not adding any new route...");
        } else {
            Dhcp6IaAddressOption aiAddressOption = clientDataOption.getOptions()
                    .stream()
                    .filter(opt -> opt instanceof Dhcp6IaAddressOption)
                    .map(pld -> (Dhcp6IaAddressOption) pld)
                    .findFirst()
                    .orElse(null);

            Dhcp6ClientIdOption clientIdOption = clientDataOption.getOptions()
                    .stream()
                    .filter(opt -> opt instanceof Dhcp6ClientIdOption)
                    .map(pld -> (Dhcp6ClientIdOption) pld)
                    .findFirst()
                    .orElse(null);

            if (aiAddressOption == null) {
                log.warn("clientDataOption from DHCP server does not " +
                                 "contains Dhcp6IaAddressOption for the client - giving up...");
            } else {
                Ip6Address clientAddress = aiAddressOption.getIp6Address();
                MacAddress clientMacAddress = MacAddress.valueOf(clientIdOption.getDuid().getLinkLayerAddress());
                Ethernet packet = context.inPacket().parsed();
                VlanId vlanId = VlanId.vlanId(packet.getVlanID());
                MacAddress potentialNextHopMac =
                        findNextHopMacForIp6FromRelayStore(clientAddress, clientMacAddress, vlanId);

                if (potentialNextHopMac == null) {
                    log.warn("Can't find next hop host mac for client {} mac:{}/{}",
                             clientAddress, clientMacAddress, vlanId);
                    return;
                } else {
                    log.info("Next hop mac for {}/{}/{} is {}", clientAddress,
                             clientMacAddress, vlanId, potentialNextHopMac.toString());
                }
                // search the next hop in the hosts store
                HostId gwHostId = HostId.hostId(potentialNextHopMac, vlanId);
                Host gwHost = hostService.getHost(gwHostId);
                if (gwHost == null) {
                    log.warn("Can't find next hop host ID {}", gwHostId);
                    return;
                }
                Ip6Address nextHopIp = gwHost.ipAddresses()
                        .stream()
                        .filter(IpAddress::isIp6)
                        .filter(IpAddress::isLinkLocal)
                        .map(IpAddress::getIp6Address)
                        .findFirst()
                        .orElse(null);
                if (nextHopIp == null) {
                    log.warn("Can't find IP6 address of next hop {}", gwHost);
                    return;
                }
                log.info("client " + clientAddress + " is known !");
                Route routeForIP6 = new Route(Route.Source.DHCP, clientAddress.toIpPrefix(), nextHopIp);
                log.debug("updating route of Client for indirectly connected.");
                log.debug("client ip: " + clientAddress + ", next hop ip6: " + nextHopIp);
                routeStore.updateRoute(routeForIP6);
            }
        }
    }

    @Override
    public void processDhcpPacket(PacketContext context, BasePacket payload) {
        checkNotNull(payload, "DHCP6 payload can't be null");
        checkState(payload instanceof DHCP6, "Payload is not a DHCP6");
        DHCP6 dhcp6Payload = (DHCP6) payload;
        Ethernet receivedPacket = context.inPacket().parsed();

        if (!configured()) {
            log.warn("Missing DHCP6 relay server config. " +
                             "Abort packet processing dhcp6 payload {}", dhcp6Payload);
            return;
        }
        byte msgTypeVal = dhcp6Payload.getMsgType();
        MsgType msgType = DHCP6.MsgType.getType(msgTypeVal);
        log.debug("msgType is {}", msgType);

        ConnectPoint inPort = context.inPacket().receivedFrom();

        if (inPort == null) {
            log.warn("incoming ConnectPoint is null");
        }
        Set<Interface> receivingInterfaces = interfaceService.getInterfacesByPort(inPort);
        //ignore the packets if dhcp client interface is not configured on onos.
        if (receivingInterfaces.isEmpty()) {
            log.warn("Virtual interface is not configured on {}", inPort);
            return;
        }

        if (msgTypeVal == DHCP6.MsgType.LEASEQUERY.value()) {
            List<InternalPacket> ethernetClientPackets =
                    learnRouteFromLeasequery ?
                        processLQ6PacketFromClient(context, receivedPacket, receivingInterfaces, dhcp6Payload) :
                        processDhcp6ForwardOnly(context, receivedPacket, receivingInterfaces, dhcp6Payload);
            for (InternalPacket internalPacket : ethernetClientPackets) {
                forwardPacket(internalPacket);
            }
        } else if (msgTypeVal == DHCP6.MsgType.LEASEQUERY_REPLY.value() && learnRouteFromLeasequery) {
            IPv6 clientIpv6 = (IPv6) receivedPacket.getPayload();
            UDP clientUdp = (UDP) clientIpv6.getPayload();
            DHCP6 clientDhcp6 = (DHCP6) clientUdp.getPayload();
            Interface serverInterface = Dhcp6HandlerUtil.directlyConnected(clientDhcp6) ?
                    getServerInterface() : getIndirectServerInterface();
            InternalPacket ethernetPacketReply =
                    Dhcp6HandlerUtil.processLQ6PacketFromServer(
                            defaultServerInfoList, indirectServerInfoList,
                            serverInterface, interfaceService,
                            hostService,
                            context, receivedPacket, receivingInterfaces);
            if (ethernetPacketReply != null) {
                forwardPacket(ethernetPacketReply);
            }
            handleLeaseQuery6ReplyMsg(context, dhcp6Payload);
        } else if (MSG_TYPE_FROM_CLIENT.contains(msgTypeVal)) {
            List<InternalPacket> ethernetClientPacket =
                    processDhcp6PacketFromClient(context, receivedPacket, receivingInterfaces);
            for (InternalPacket internalPacket : ethernetClientPacket) {
                forwardPacket(internalPacket);
            }
        } else if (MSG_TYPE_FROM_SERVER.contains(msgTypeVal)) {
            log.debug("calling processDhcp6PacketFromServer with RELAY_REPL {}, {}", receivedPacket, dhcp6Payload);
            InternalPacket ethernetPacketReply =
                    processDhcp6PacketFromServer(context, receivedPacket, receivingInterfaces);
            if (ethernetPacketReply != null) {
                forwardPacket(ethernetPacketReply);
            }
        } else {
            log.warn("Not so fast, packet type {} not supported yet", msgTypeVal);
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

    //forward the packet to ConnectPoint where the DHCP server is attached.
    private void forwardPacket(InternalPacket packet) {
        //send Packetout to dhcp server connectpoint.
        if (packet.getDestLocation() != null) {
            TrafficTreatment t = DefaultTrafficTreatment.builder()
                    .setOutput(packet.getDestLocation().port()).build();
            OutboundPacket o = new DefaultOutboundPacket(
                    packet.getDestLocation().deviceId(), t, ByteBuffer.wrap(packet.getPacket().serialize()));
            packetService.emit(o);
            if (log.isTraceEnabled()) {
                IPv6 ip6 = (IPv6) packet.getPacket().getPayload();
                UDP udp = (UDP) ip6.getPayload();
                DHCP6 dhcp6  = (DHCP6) udp.getPayload();
                log.trace("Relaying packet to destination {} eth: {} dhcp: {}",
                        packet.getDestLocation(), packet.getPacket(), dhcp6);
            }

        }
    }

    /**
     * extract from dhcp6 packet client ipv6 address of given by dhcp server.
     *
     * @param dhcp6 the dhcp6 packet
     * @return IpAddressInfo  IpAddressInfo given by dhcp server, or null if not exists
     */
    private IpAddressInfo extractIpAddress(DHCP6 dhcp6) {
        IpAddressInfo ipInfo = new IpAddressInfo();

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
            log.info("No IPv6 address found from iaTaOption {}", iaTaOption);
            iaAddressOption = Optional.empty();
        }
        if (iaAddressOption.isPresent()) {
            ipInfo.ip6Address = iaAddressOption.get().getIp6Address();
            ipInfo.prefTime = iaAddressOption.get().getPreferredLifetime();
            log.debug("Found IPv6 address from iaAddressOption {}", iaAddressOption);
        } else {
            log.debug("Can't find IPv6 address from DHCPv6 {}", dhcp6);
            return null;
        }
        return ipInfo;
    }

    /**
     * extract from dhcp6 packet Prefix prefix provided by dhcp server.
     *
     * @param dhcp6 the dhcp6 payload
     * @return IpPrefix Prefix Delegation prefix, or null if not exists.
     */
    private PdPrefixInfo extractPrefix(DHCP6 dhcp6) {
        log.debug("extractPrefix  enters {}", dhcp6);

        // extract prefix
        PdPrefixInfo  pdPrefixInfo = new PdPrefixInfo();

        Ip6Address prefixAddress = null;

        // Extract IPv6 prefix from IA PD option
        Optional<Dhcp6IaPdOption> iaPdOption = dhcp6.getOptions()
                .stream()
                .filter(opt -> opt instanceof Dhcp6IaPdOption)
                .map(opt -> (Dhcp6IaPdOption) opt)
                .findFirst();

        Optional<Dhcp6IaPrefixOption> iaPrefixOption;
        if (iaPdOption.isPresent()) {
            log.debug("IA_PD option found {}", iaPdOption);

            iaPrefixOption = iaPdOption.get().getOptions().stream()
                    .filter(opt -> opt instanceof Dhcp6IaPrefixOption)
                    .map(opt -> (Dhcp6IaPrefixOption) opt)
                    .findFirst();
        } else {
            log.debug("IA_PD option NOT found");

            iaPrefixOption = Optional.empty();
        }
        if (iaPrefixOption.isPresent()) {
            log.debug("IAPrefix Option within IA_PD option found {}", iaPrefixOption);

            prefixAddress = iaPrefixOption.get().getIp6Prefix();
            int prefixLen = (int) iaPrefixOption.get().getPrefixLength();
            log.debug("Prefix length is  {} bits", prefixLen);
            pdPrefixInfo.pdPrefix = IpPrefix.valueOf(prefixAddress, prefixLen);
            pdPrefixInfo.prefTime = iaPrefixOption.get().getPreferredLifetime();
        } else {
            log.debug("Can't find IPv6 prefix from DHCPv6 {}", dhcp6);
            return null;
        }
        return pdPrefixInfo;
    }

    /**
     * remove host or route and update dhcp relay record attributes.
     *
     * @param directConnFlag  flag to show that packet is from directly connected client
     * @param location  client side connect point
     * @param dhcp6Packet the dhcp6 payload
     * @param clientPacket client's ethernet packet
     * @param clientIpv6 client's Ipv6 packet
     * @param clientInterface client interfaces
     */
    private void removeHostOrRoute(boolean directConnFlag, ConnectPoint location,
                                   DHCP6 dhcp6Packet,
                                   Ethernet clientPacket, IPv6 clientIpv6,
                                   Interface clientInterface) {
        log.debug("removeHostOrRoute  enters {}", dhcp6Packet);
        VlanId vlanId = clientInterface.vlan();
        MacAddress srcMac = clientPacket.getSourceMAC();  // could be gw or host
        MacAddress leafClientMac;
        byte leafMsgType;
        log.debug("client mac {} client vlan {}", HexString.toHexString(srcMac.toBytes(), ":"), vlanId);

        Dhcp6ClientIdOption clientIdOption = Dhcp6HandlerUtil.extractClientId(directConnFlag, dhcp6Packet);
        if (clientIdOption != null) {
            if ((clientIdOption.getDuid().getDuidType() == Dhcp6Duid.DuidType.DUID_LLT) ||
                    (clientIdOption.getDuid().getDuidType() == Dhcp6Duid.DuidType.DUID_LL)) {
                leafClientMac = MacAddress.valueOf(clientIdOption.getDuid().getLinkLayerAddress());
            } else {
                log.warn("Link-Layer Address not supported in CLIENTID option. No DhcpRelay Record created.");
                //dhcpRelayCountersStore.incrementCounter(gCount, DhcpRelayCounters.NO_LINKLOCAL_FAIL);
                return;
            }
        } else {
            log.warn("CLIENTID option NOT found. Don't create DhcpRelay Record.");
            //dhcpRelayCountersStore.incrementCounter(gCount, DhcpRelayCounters.NO_CLIENTID_FAIL);
            return;
        }

        HostId leafHostId = HostId.hostId(leafClientMac, vlanId);
        DhcpRecord record = dhcpRelayStore.getDhcpRecord(leafHostId).orElse(null);
        if (record == null) {
            record = new DhcpRecord(leafHostId);
        }  else {
            record = record.clone();
        }

        Boolean isMsgRelease = Dhcp6HandlerUtil.isDhcp6Release(dhcp6Packet);
        IpAddressInfo ipInfo;
        PdPrefixInfo pdInfo = null;
        if (directConnFlag) {
            // Add to host store if it is connected to network directly
            ipInfo = extractIpAddress(dhcp6Packet);
            if (ipInfo != null) {
                if (isMsgRelease) {
                    HostId hostId = HostId.hostId(srcMac, vlanId);
                    log.debug("remove Host {} ip for directly connected.", hostId.toString());
                    providerService.removeIpFromHost(hostId, ipInfo.ip6Address);
                }
            } else {
                log.debug("ipAddress not found. Do not remove Host {} for directly connected.",
                        HostId.hostId(srcMac, vlanId).toString());
            }
            leafMsgType = dhcp6Packet.getMsgType();
        } else {
            // Remove from route store if it is not connected to network directly
            // pick out the first link-local ip address
            IpAddress nextHopIp = getFirstIpByHost(directConnFlag, srcMac, vlanId);
            if (nextHopIp == null) {
                log.warn("Can't find link-local IP address of gateway mac {} vlanId {}", srcMac, vlanId);
                //dhcpRelayCountersStore.incrementCounter(gCount, DhcpRelayCounters.NO_LINKLOCAL_GW);
                return;
            }

            DHCP6 leafDhcp = Dhcp6HandlerUtil.getDhcp6Leaf(dhcp6Packet);
            ipInfo = extractIpAddress(leafDhcp);
            if (ipInfo == null) {
                log.debug("ip is null");
            } else {
                if (isMsgRelease) {
                    Route routeForIP = new Route(Route.Source.DHCP, ipInfo.ip6Address.toIpPrefix(), nextHopIp);
                    log.debug("removing route of 128 address for indirectly connected.");
                    log.debug("128 ip {}, nexthop {}",
                            HexString.toHexString(ipInfo.ip6Address.toOctets(), ":"),
                            HexString.toHexString(nextHopIp.toOctets(), ":"));
                    routeStore.removeRoute(routeForIP);
                }
            }

            pdInfo = extractPrefix(leafDhcp);
            if (pdInfo == null) {
                log.debug("ipPrefix is null ");
            } else {
                if (isMsgRelease) {
                    Route routeForPrefix = new Route(Route.Source.DHCP, pdInfo.pdPrefix, nextHopIp);
                    log.debug("removing route of PD for indirectly connected.");
                    log.debug("pd ip {}, nexthop {}",
                            HexString.toHexString(pdInfo.pdPrefix.address().toOctets(), ":"),
                            HexString.toHexString(nextHopIp.toOctets(), ":"));

                    routeStore.removeRoute(routeForPrefix);
                    if (this.dhcpFpmEnabled) {
                        dhcpFpmPrefixStore.removeFpmRecord(pdInfo.pdPrefix);
                    }
                }
            }
            leafMsgType = leafDhcp.getMsgType();
       }

        if (isMsgRelease) {
            log.debug("DHCP6 RELEASE msg.");
            if (record != null) {
                if (ipInfo != null) {
                    log.debug("DhcpRelay Record ip6Address is set to null.");
                    record.ip6Address(null);
                }
                if (pdInfo != null) {
                    log.debug("DhcpRelay Record pdPrefix is set to null.");
                }

                if (!record.ip6Address().isPresent() && !record.pdPrefix().isPresent()) {
                    log.warn("IP6 address and IP6 PD both are null. Remove record.");
                    // do not remove a record. Let timer task handler it.
                    //dhcpRelayStore.removeDhcpRecord(HostId.hostId(leafClientMac, vlanId));
                }
            }
        }

        if (record != null) {
            record.getV6Counters().incrementCounter(Dhcp6HandlerUtil.getMsgTypeStr(leafMsgType));
            record.addLocation(new HostLocation(location, System.currentTimeMillis()));
            record.ip6Status(DHCP6.MsgType.getType(leafMsgType));
            record.setDirectlyConnected(directConnFlag);
            if (!directConnFlag) {
                // Update gateway mac address if the host is not directly connected
                record.nextHop(srcMac);
            }
            record.updateLastSeen();
        }
        dhcpRelayStore.updateDhcpRecord(leafHostId, record);
        /*
        // TODO Use AtomicInteger for the counters
        try {
            recordSemaphore.acquire();
            try {
                dhcpRelayCountersStore.incrementCounter(gCount, Dhcp6HandlerUtil.getMsgTypeStr(leafMsgType));
            } finally {
                // calling release() after a successful acquire()
                recordSemaphore.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        */
    }

    /**
     * add host or route and update dhcp relay record.
     *
     * @param directConnFlag  flag to show that packet is from directly connected client
     * @param location  client side connect point
     * @param dhcp6Relay the dhcp6 payload
     * @param embeddedDhcp6 the dhcp6 payload within relay
     * @param srcMac client gw/host macAddress
     * @param clientInterface client interface
     * @param vlanIdInUse vlanid encoded in the interface id Option
     */
    private void addHostOrRoute(boolean directConnFlag, ConnectPoint location, DHCP6 dhcp6Relay,
                                DHCP6 embeddedDhcp6, MacAddress srcMac, Interface clientInterface,
                                VlanId vlanIdInUse) {
        log.debug("addHostOrRoute entered.");
        VlanId vlanId;
        if (clientInterface.vlanTagged().isEmpty()) {
            vlanId = clientInterface.vlan();
        } else {
            // might be multiple vlan in same interface
            vlanId = vlanIdInUse;
        }
        if (vlanId == null) {
            vlanId = VlanId.NONE;
        }

        Boolean isMsgReply = Dhcp6HandlerUtil.isDhcp6Reply(dhcp6Relay);
        MacAddress leafClientMac;
        Byte leafMsgType;

        Dhcp6ClientIdOption clientIdOption = Dhcp6HandlerUtil.extractClientId(directConnFlag, embeddedDhcp6);
        if (clientIdOption != null) {
            log.debug("CLIENTID option found {}", clientIdOption);
            if ((clientIdOption.getDuid().getDuidType() == Dhcp6Duid.DuidType.DUID_LLT) ||
                    (clientIdOption.getDuid().getDuidType() == Dhcp6Duid.DuidType.DUID_LL)) {
                leafClientMac = MacAddress.valueOf(clientIdOption.getDuid().getLinkLayerAddress());
            } else {
                log.warn("Link-Layer Address not supported in CLIENTID option. No DhcpRelay Record created.");
                //dhcpRelayCountersStore.incrementCounter(gCount, DhcpRelayCounters.NO_LINKLOCAL_FAIL);
                return;
            }
        } else {
            log.warn("CLIENTID option NOT found. No DhcpRelay Record created.");
            //dhcpRelayCountersStore.incrementCounter(gCount, DhcpRelayCounters.NO_CLIENTID_FAIL);
            return;
        }
        HostId leafHostId = HostId.hostId(leafClientMac, vlanId);
        DhcpRecord record = dhcpRelayStore.getDhcpRecord(leafHostId).orElse(null);
        if (record == null) {
            record = new DhcpRecord(HostId.hostId(leafClientMac, vlanId));
        } else {
            record = record.clone();
        }

        IpAddressInfo ipInfo;
        PdPrefixInfo pdInfo = null;
        if (directConnFlag) {
            // Add to host store if it connect to network directly
            ipInfo = extractIpAddress(embeddedDhcp6);
            if (ipInfo != null) {
                if (isMsgReply) {
                    Set<IpAddress> ips = Sets.newHashSet(ipInfo.ip6Address);
                    HostId hostId = HostId.hostId(srcMac, vlanId);
                    Host host = hostService.getHost(hostId);
                    HostLocation hostLocation = new HostLocation(clientInterface.connectPoint(),
                            System.currentTimeMillis());
                    Set<HostLocation> hostLocations = Sets.newHashSet(hostLocation);
                    if (host != null) {
                        // Dual homing support:
                        // if host exists, use old locations and new location
                        hostLocations.addAll(host.locations());
                    }
                    HostDescription desc = new DefaultHostDescription(srcMac, vlanId, hostLocations, ips,
                            false);
                    log.debug("adding Host for directly connected.");
                    log.debug("client mac {} client vlan {} hostlocation {}",
                            HexString.toHexString(srcMac.toBytes(), ":"), vlanId, hostLocation.toString());
                    // Replace the ip when dhcp server give the host new ip address
                    providerService.hostDetected(hostId, desc, false);
                }
            } else {
                log.warn("ipAddress not found. Do not add Host {} for directly connected.",
                        HostId.hostId(srcMac, vlanId).toString());
            }
            leafMsgType = embeddedDhcp6.getMsgType();
        } else {
            // Add to route store if it does not connect to network directly
            // pick out the first link-local ip address
            IpAddress nextHopIp = getFirstIpByHost(directConnFlag, srcMac, vlanId);
            if (nextHopIp == null) {
                log.warn("Can't find link-local IP address of gateway mac {} vlanId {}", srcMac, vlanId);
                //dhcpRelayCountersStore.incrementCounter(gCount, DhcpRelayCounters.NO_LINKLOCAL_GW);
                return;
            }

            DHCP6 leafDhcp = Dhcp6HandlerUtil.getDhcp6Leaf(embeddedDhcp6);
            ipInfo = extractIpAddress(leafDhcp);
            if (ipInfo == null) {
                log.debug("ip is null");
            } else {
                if (isMsgReply) {
                    Route routeForIP = new Route(Route.Source.DHCP, ipInfo.ip6Address.toIpPrefix(), nextHopIp);
                    log.debug("adding Route of 128 address for indirectly connected.");
                    routeStore.replaceRoute(routeForIP);
                }
            }

            pdInfo = extractPrefix(leafDhcp);
            if (pdInfo == null) {
                log.debug("ipPrefix is null ");
            } else {
                if (isMsgReply) {
                    Route routeForPrefix = new Route(Route.Source.DHCP, pdInfo.pdPrefix, nextHopIp);
                    log.debug("adding Route of PD for indirectly connected.");
                    routeStore.replaceRoute(routeForPrefix);
                    if (this.dhcpFpmEnabled) {
                        FpmRecord fpmRecord = new FpmRecord(pdInfo.pdPrefix, nextHopIp, FpmRecord.Type.DHCP_RELAY);
                        dhcpFpmPrefixStore.addFpmRecord(pdInfo.pdPrefix, fpmRecord);
                    }
                }
            }
            leafMsgType = leafDhcp.getMsgType();
        }
        if (leafMsgType == DHCP6.MsgType.RELEASE.value() ||
                (leafMsgType == DHCP6.MsgType.REPLY.value()) && ipInfo == null) {
            log.warn("DHCP6 RELEASE/REPLY(null ip) from Server. MsgType {}", leafMsgType);
            //return;
        }

        record.addLocation(new HostLocation(location, System.currentTimeMillis()));

        if (leafMsgType == DHCP6.MsgType.REPLY.value()) {
            if (ipInfo != null) {
                log.debug("IP6 address is being stored into dhcp-relay store.");
                log.debug("Client IP6 address {}", HexString.toHexString(ipInfo.ip6Address.toOctets(), ":"));
                record.ip6Address(ipInfo.ip6Address);
                record.updateAddrPrefTime(ipInfo.prefTime);
                record.updateLastIp6Update();
             } else {
                log.debug("IP6 address is not returned from server. Maybe only PD is returned.");
            }
            if (pdInfo != null) {
                log.debug("IP6 PD address {}",
                        HexString.toHexString(pdInfo.pdPrefix.address().toOctets(), ":"));
                record.pdPrefix(pdInfo.pdPrefix);
                record.updatePdPrefTime(pdInfo.prefTime);
                record.updateLastPdUpdate();
            } else {
                log.debug("IP6 PD address is not returned from server. Maybe only IPAddress is returned.");
            }
        }

        record.getV6Counters().incrementCounter(Dhcp6HandlerUtil.getMsgTypeStr(leafMsgType));
        record.ip6Status(DHCP6.MsgType.getType(leafMsgType));
        record.setDirectlyConnected(directConnFlag);
        record.updateLastSeen();
        dhcpRelayStore.updateDhcpRecord(leafHostId, record);
        /*
        // TODO Use AtomicInteger for the counters
        try {
            recordSemaphore.acquire();
            try {
                dhcpRelayCountersStore.incrementCounter(gCount, Dhcp6HandlerUtil.getMsgTypeStr(leafMsgType));
            } finally {
                // calling release() after a successful acquire()
                recordSemaphore.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        */
    }

    private List<InternalPacket> processDhcp6ForwardOnly(PacketContext context,
                                                         Ethernet clientPacket,
                                                         Set<Interface> clientInterfaces,
                                                         DHCP6 dhcpPacket) {
        ConnectPoint inPort = context.inPacket().receivedFrom();
        log.trace("Got DHCPv6 on port {}", inPort);
        boolean directConnFlag = Dhcp6HandlerUtil.directlyConnected(dhcpPacket);

        List<InternalPacket> internalPackets = new ArrayList<>();
        List<DhcpServerInfo> serverInfoList = findValidServerInfo(directConnFlag);

        for (DhcpServerInfo dhcpServer : serverInfoList) {
            Interface serverInterface = getServerInterface(dhcpServer);
            if (serverInterface == null) {
                log.warn("Can't get server interface, ignore");
                continue;
            }

            Ethernet newPacket = Dhcp6HandlerUtil.buildDhcp6PacketFromClient(context,
                    clientPacket, clientInterfaces, dhcpServer, serverInterface);
            log.trace("Built packet for server {} : {}", dhcpServer, newPacket);
            internalPackets.add(InternalPacket.internalPacket(newPacket,
                    dhcpServer.getDhcpServerConnectPoint().get()));
        }

        return internalPackets;
    }

    private List<InternalPacket> processLQ6PacketFromClient(PacketContext context,
                                                              Ethernet clientPacket,
                                                              Set<Interface> clientInterfaces,
                                                              DHCP6 dhcp6Payload) {
        ConnectPoint inPort = context.inPacket().receivedFrom();
        log.info("Got LQ-REQUEST V6 on port {}", inPort);
        List<Dhcp6Option> lopt = dhcp6Payload.getOptions();
        log.info("Options list: {}", lopt);
        Dhcp6LeaseQueryOption lqoption = dhcp6Payload.getOptions()
                .stream()
                .filter(opt -> opt instanceof Dhcp6LeaseQueryOption)
                .map(pld -> (Dhcp6LeaseQueryOption) pld)
                .findFirst()
                .orElse(null);

        if (lqoption == null) {
            // Can't find dhcp payload
            log.warn("Can't find dhcp6 lease query message - aborting");
            return null;
        } else {
            log.info("dhcp6 lqv6 options found: {}", lqoption);
        }
        log.warn("LQv6 for " + lqoption.linkAddress.toString() + " comes from " + inPort.toString());
        Ethernet packet = context.inPacket().parsed();
        Ip6Address clientAddress = lqoption.linkAddress;
        IPv6 ipv6Packet = (IPv6) packet.getPayload();
        Ip6Address nextHopIp = findNextHopIp6FromRelayStore(clientAddress);

        // 1. only if there is a route to remove - remove it
        if (nextHopIp != null) {
            Route routeForIP6 = new Route(Route.Source.DHCP, clientAddress.toIpPrefix(), nextHopIp);
            log.debug("Removing route of Client " + clientAddress +
                              " for indirectly connected - next hop ip6 " + nextHopIp);
            routeStore.removeRoute(routeForIP6);
        }

        // 2. note the potential NH this packet came from in case it's a known lease
        //    this NH will then be used to build the route
        MacAddress potentialNH = packet.getSourceMAC();
        VlanId vlanId = VlanId.vlanId(packet.getVlanID());
        setPotentialNextHopForIp6InRelayStore(clientAddress, vlanId, potentialNH);
        // 3. route this LQ6 to all relevant servers
        IPv6 clientIpv6 = (IPv6) clientPacket.getPayload();
        UDP clientUdp = (UDP) clientIpv6.getPayload();
        DHCP6 clientDhcp6 = (DHCP6) clientUdp.getPayload();

        boolean directConnFlag = Dhcp6HandlerUtil.directlyConnected(clientDhcp6);
        boolean serverFound = false;
        List<InternalPacket> internalPackets = new ArrayList<>();
        List<DhcpServerInfo> serverInfoList = findValidServerInfo(directConnFlag);
        List<DhcpServerInfo> copyServerInfoList = new ArrayList<DhcpServerInfo>(serverInfoList);

        for (DhcpServerInfo serverInfo : copyServerInfoList) {
            if (!Dhcp6HandlerUtil.checkDhcpServerConnPt(directConnFlag, serverInfo)) {
                log.warn("Can't get server connect point, ignore");
                continue;
            }
            DhcpServerInfo newServerInfo = getHostInfoForServerInfo(serverInfo, serverInfoList);
            if (newServerInfo == null) {
                log.debug("Can't get server interface with host info resolved, ignore serverInfo {} serverInfoList {}",
                            serverInfo, serverInfoList);
                continue;
            }
            Interface serverInterface = getServerInterface(newServerInfo);
            if (serverInterface == null) {
                log.debug("Can't get server interface, ignore for serverInfo {}, serverInfoList {}",
                           serverInfo, serverInfoList);
                continue;
            }

            serverFound = true;
            log.debug("Server Info Found {}", serverInfo.getDhcpConnectMac());
            Ethernet etherRouted = (Ethernet) clientPacket.clone();
            MacAddress macFacingServer = serverInterface.mac();
            if (macFacingServer == null) {
                log.warn("No MAC address for server Interface {}", serverInterface);
                return null;
            }
            etherRouted.setSourceMACAddress(macFacingServer);
            etherRouted.setDestinationMACAddress(newServerInfo.getDhcpConnectMac().get());
            InternalPacket internalPacket =
                    InternalPacket.internalPacket(etherRouted,
                              serverInfo.getDhcpServerConnectPoint().get());
            internalPackets.add(internalPacket);
            log.debug("Sending LQ to DHCP server {}", newServerInfo.getDhcpServerIp6());
        }
        if (!serverFound) {
            log.warn("ProcessDhcp6PacketFromClient No Server Found");
        }
        log.debug("num of client packets to send is{}", internalPackets.size());

        return internalPackets;
    }

    /**
     * build the DHCP6 solicit/request packet with gatewayip.
     *
     * @param context packet context
     * @param clientPacket client ethernet packet
     * @param clientInterfaces set of client side interfaces
     */
    private List<InternalPacket> processDhcp6PacketFromClient(PacketContext context,
                                                              Ethernet clientPacket,
                                                              Set<Interface> clientInterfaces) {
        ConnectPoint receivedFrom = context.inPacket().receivedFrom();
        Ip6Address relayAgentIp = Dhcp6HandlerUtil.getRelayAgentIPv6Address(clientInterfaces);
        MacAddress relayAgentMac = clientInterfaces.iterator().next().mac();
        if (relayAgentIp == null || relayAgentMac == null) {
            log.warn("Missing DHCP relay agent interface Ipv6 addr config for "
                      + "packet from client on port: {}. Aborting packet processing",
                      clientInterfaces.iterator().next().connectPoint());
            //dhcpRelayCountersStore.incrementCounter(gCount, DhcpRelayCounters.NO_CLIENT_INTF_MAC);
            return Lists.newArrayList();
        }

        IPv6 clientIpv6 = (IPv6) clientPacket.getPayload();
        UDP clientUdp = (UDP) clientIpv6.getPayload();
        DHCP6 clientDhcp6 = (DHCP6) clientUdp.getPayload();

        boolean directConnFlag = Dhcp6HandlerUtil.directlyConnected(clientDhcp6);

        ConnectPoint clientConnectionPoint = context.inPacket().receivedFrom();
        VlanId vlanIdInUse = VlanId.vlanId(clientPacket.getVlanID());
        Interface clientInterface = interfaceService.getInterfacesByPort(clientConnectionPoint)
                .stream().filter(iface -> Dhcp6HandlerUtil.interfaceContainsVlan(iface, vlanIdInUse))
                .findFirst()
                .orElse(null);

        List<InternalPacket> internalPackets = new ArrayList<>();
        List<DhcpServerInfo> serverInfoList = findValidServerInfo(directConnFlag);
        List<DhcpServerInfo> copyServerInfoList = new ArrayList<DhcpServerInfo>(serverInfoList);

        for (DhcpServerInfo serverInfo : copyServerInfoList) {
            if (!Dhcp6HandlerUtil.checkDhcpServerConnPt(directConnFlag, serverInfo)) {
                log.warn("Can't get server connect point, ignore");
                continue;
            }
            DhcpServerInfo newServerInfo = getHostInfoForServerInfo(serverInfo, serverInfoList);
            if (newServerInfo == null) {
                log.warn("Can't get server interface with host info resolved, ignore");
                continue;
            }

            Interface serverInterface = getServerInterface(newServerInfo);
            if (serverInterface == null) {
                log.warn("Can't get server interface, ignore");
                continue;
            }

            Ethernet etherReply = Dhcp6HandlerUtil.buildDhcp6PacketFromClient(context, clientPacket,
                                                              clientInterfaces, newServerInfo, serverInterface);
            removeHostOrRoute(directConnFlag, clientConnectionPoint, clientDhcp6, clientPacket,
                    clientIpv6, clientInterface);

            InternalPacket internalPacket = InternalPacket.internalPacket(etherReply,
                    serverInfo.getDhcpServerConnectPoint().get());
            internalPackets.add(internalPacket);
        }
        log.debug("num of client packets to send is{}", internalPackets.size());

        return internalPackets;
    }

    /**
     * process the DHCP6 relay-reply packet from dhcp server.
     *
     * @param context packet context
     * @param receivedPacket server ethernet packet
     * @param recevingInterfaces set of server side interfaces
     * @return internalPacket toward client
     */
    private InternalPacket processDhcp6PacketFromServer(PacketContext context,
                                                        Ethernet receivedPacket, Set<Interface> recevingInterfaces) {
        // get dhcp6 header.
        Ethernet etherReply = receivedPacket.duplicate();
        IPv6 ipv6Packet = (IPv6) etherReply.getPayload();
        UDP udpPacket = (UDP) ipv6Packet.getPayload();
        DHCP6 dhcp6Relay = (DHCP6) udpPacket.getPayload();
        Boolean directConnFlag = Dhcp6HandlerUtil.directlyConnected(dhcp6Relay);

        DHCP6 embeddedDhcp6 = dhcp6Relay.getOptions().stream()
                .filter(opt -> opt instanceof Dhcp6RelayOption)
                .map(BasePacket::getPayload)
                .map(pld -> (DHCP6) pld)
                .findFirst()
                .orElse(null);
        ConnectPoint inPort = context.inPacket().receivedFrom();
        DhcpServerInfo foundServerInfo = findServerInfoFromServer(directConnFlag, inPort);

        if (foundServerInfo == null) {
            log.warn("Cannot find server info for {} server, inPort {}",
                      directConnFlag ? "direct" : "indirect", inPort);
            //dhcpRelayCountersStore.incrementCounter(gCount, DhcpRelayCounters.NO_SERVER_INFO);
            return null;
        } else {
            if (Dhcp6HandlerUtil.isServerIpEmpty(foundServerInfo)) {
                log.warn("Cannot find server info's ipaddress");
                //dhcpRelayCountersStore.incrementCounter(gCount, DhcpRelayCounters.NO_SERVER_IP6ADDR);
                return null;
            }
        }

        Dhcp6InterfaceIdOption interfaceIdOption = dhcp6Relay.getOptions().stream()
                .filter(opt -> opt instanceof Dhcp6InterfaceIdOption)
                .map(opt -> (Dhcp6InterfaceIdOption) opt)
                .findFirst()
                .orElse(null);
        if (interfaceIdOption == null) {
            log.warn("Interface Id option is not present, abort packet...");
            //dhcpRelayCountersStore.incrementCounter(gCount, DhcpRelayCounters.OPTION_MISSING_FAIL);
            return null;
        }

        MacAddress peerMac = interfaceIdOption.getMacAddress();
        String clientConnectionPointStr = new String(interfaceIdOption.getInPort());
        ConnectPoint clientConnectionPoint = ConnectPoint.deviceConnectPoint(clientConnectionPointStr);
        VlanId vlanIdInUse = VlanId.vlanId(interfaceIdOption.getVlanId());

        log.debug("processDhcp6PacketFromServer Interface Id Mac {}, port{}, vlan {}",
                          peerMac, clientConnectionPointStr, vlanIdInUse);
        Interface clientInterface = interfaceService.getInterfacesByPort(clientConnectionPoint)
                .stream().filter(iface -> Dhcp6HandlerUtil.interfaceContainsVlan(iface, vlanIdInUse))
                .findFirst().orElse(null);
        if (clientInterface == null) {
            log.warn("Cannot get client interface for from packet, abort... vlan {}", vlanIdInUse.toString());
            //dhcpRelayCountersStore.incrementCounter(gCount, DhcpRelayCounters.NO_MATCHING_INTF);
            return null;
        }
        etherReply.setVlanID(vlanIdInUse.toShort());

        MacAddress relayAgentMac = clientInterface.mac();
        if (relayAgentMac == null) {
            log.warn("Can not get client interface mac, abort packet..");
            //dhcpRelayCountersStore.incrementCounter(gCount, DhcpRelayCounters.NO_CLIENT_INTF_MAC);
            return null;
        }
        etherReply.setSourceMACAddress(relayAgentMac);

        // find destMac
        MacAddress clientMac;
        Ip6Address peerAddress = Ip6Address.valueOf(dhcp6Relay.getPeerAddress());
        Set<Host> clients = hostService.getHostsByIp(peerAddress);
        if (clients.isEmpty()) {
            log.trace("There's no host found for this address {}",
                    HexString.toHexString(dhcp6Relay.getPeerAddress(), ":"));
            log.trace("Let's look up interfaceId {}", HexString.toHexString(peerMac.toBytes(), ":"));
            clientMac = peerMac;
        } else {
            clientMac = clients.iterator().next().mac();
            if (clientMac == null) {
                log.warn("No client mac address found, abort packet...");
                //dhcpRelayCountersStore.incrementCounter(gCount, DhcpRelayCounters.NO_CLIENT_INTF_MAC);
                return null;
            }
            log.trace("Client mac address found from getHostByIp");
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


        boolean hostOrRouteAllowed = learnRouteFromLeasequery ||
                    Dhcp6HandlerUtil.getDhcp6LeafMessageType(dhcp6Relay) != MsgType.LEASEQUERY_REPLY;
        log.debug("Can add host or route: {}", hostOrRouteAllowed);

        if (hostOrRouteAllowed) {
            // add host or route
            addHostOrRoute(directConnFlag, clientConnectionPoint, dhcp6Relay, embeddedDhcp6,
                    clientMac, clientInterface, vlanIdInUse);
        }

        udpPacket.setPayload(embeddedDhcp6);
        udpPacket.resetChecksum();
        ipv6Packet.setPayload(udpPacket);
        etherReply.setPayload(ipv6Packet);
        return InternalPacket.internalPacket(etherReply, clientConnectionPoint);
    }

    @Override
    public void setDhcpFpmEnabled(Boolean enabled) {
       dhcpFpmEnabled = enabled;
    }

    @Override
    public void setDefaultDhcpServerConfigs(Collection<DhcpServerConfig> configs) {
        log.debug("setDefaultDhcpServerConfigs is called.");
        setDhcpServerConfigs(configs, defaultServerInfoList);
    }

    @Override
    public void setIndirectDhcpServerConfigs(Collection<DhcpServerConfig> configs) {
        log.debug("setIndirectDhcpServerConfigs is called.");
        setDhcpServerConfigs(configs, indirectServerInfoList);
    }

    public void setDhcpServerConfigs(Collection<DhcpServerConfig> configs, List<DhcpServerInfo> serverInfoList) {
        log.debug("config size {}.", configs.size());

        if (configs.size() == 0) {
            // no config to update
            return;
        }
        // TODO: currently we pick up first DHCP server config.
        // Will use other server configs in the future for HA.
        Boolean isConfigValid = false;
        for (DhcpServerConfig serverConfig : configs) {
            if (serverConfig.getDhcpServerIp6().isPresent()) {
                isConfigValid = true;
                break;
            }
        }
        if (!isConfigValid) {
            log.warn("No IP V6 server address found.");
            return;  // No IP V6 address found
        }
        for (DhcpServerInfo oldServerInfo : serverInfoList) {
            // stop monitoring gateway or server
            oldServerInfo.getDhcpGatewayIp6().ifPresent(gatewayIp -> {
                hostService.stopMonitoringIp(gatewayIp);
            });
            oldServerInfo.getDhcpServerIp6().ifPresent(serverIp -> {
                hostService.stopMonitoringIp(serverIp);
                cancelDhcpPacket(serverIp);
            });
        }
        serverInfoList.clear();
        for (DhcpServerConfig serverConfig : configs) {
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

            log.warn("Probing to resolve {} IP {}", hostToProbe, ipToProbe);
            hostService.startMonitoringIp(ipToProbe);

            Set<Host> hosts = hostService.getHostsByIp(ipToProbe);
            if (!hosts.isEmpty()) {
                Host host = hosts.iterator().next();
                newServerInfo.setDhcpConnectVlan(host.vlan());
                newServerInfo.setDhcpConnectMac(host.mac());
                log.warn("Host found host {}", host);

            } else {
                log.warn("No host found host ip {}", ipToProbe);
            }
            // Add new server info
            synchronized (this) {
                serverInfoList.add(newServerInfo);
            }
            if (!hosts.isEmpty()) {
                requestDhcpPacket(serverIp);
            }
        }
    }

    class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            switch (event.type()) {
                case HOST_ADDED:
                case HOST_UPDATED:
                case HOST_MOVED:
                    log.trace("Scheduled host event {}", event);
                    hostEventExecutor.execute(() -> hostUpdated(event.subject()));
                    break;
                case HOST_REMOVED:
                    log.trace("Scheduled host event {}", event);
                    hostEventExecutor.execute(() -> hostRemoved(event.subject()));
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
        serverInfoList.stream().forEach(serverInfo -> {
            Ip6Address serverIp = serverInfo.getDhcpServerIp6().orElse(null);
            Ip6Address targetIp = serverInfo.getDhcpGatewayIp6().orElse(null);

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
        });
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
        serverInfoList.stream().forEach(serverInfo -> {
            Ip6Address serverIp = serverInfo.getDhcpServerIp6().orElse(null);
            Ip6Address targetIp = serverInfo.getDhcpGatewayIp6().orElse(null);

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
        });
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
                .filter(iface -> Dhcp6HandlerUtil.interfaceContainsVlan(iface, dhcpConnectVlan))
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
                .filter(iface -> Dhcp6HandlerUtil.interfaceContainsVlan(iface, indirectDhcpConnectVlan))
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if serverInfo's host info (mac and vlan) is filled in; if not, fills in.
     *
     * @param serverInfo server information
     * @return newServerInfo if host info can be either found or filled in.
     */
    private DhcpServerInfo getHostInfoForServerInfo(DhcpServerInfo serverInfo, List<DhcpServerInfo> sererInfoList) {
        DhcpServerInfo newServerInfo = null;
        MacAddress  dhcpServerConnectMac = serverInfo.getDhcpConnectMac().orElse(null);
        VlanId dhcpConnectVlan = serverInfo.getDhcpConnectVlan().orElse(null);
        ConnectPoint dhcpServerConnectPoint = serverInfo.getDhcpServerConnectPoint().orElse(null);

        if (dhcpServerConnectMac != null && dhcpConnectVlan != null) {
            newServerInfo = serverInfo;
            log.debug("DHCP server {} host info found. ConnectPt{}  Mac {} vlan {}", serverInfo.getDhcpServerIp6(),
                    dhcpServerConnectPoint, dhcpServerConnectMac, dhcpConnectVlan);
        } else {
            log.debug("DHCP server {} not resolve yet connectPt {} mac {} vlan {}", serverInfo.getDhcpServerIp6(),
                    dhcpServerConnectPoint, dhcpServerConnectMac, dhcpConnectVlan);

            Ip6Address ipToProbe;
            if (serverInfo.getDhcpGatewayIp6().isPresent()) {
                ipToProbe = serverInfo.getDhcpGatewayIp6().get();
            } else {
                ipToProbe = serverInfo.getDhcpServerIp6().orElse(null);
            }
            String hostToProbe = serverInfo.getDhcpGatewayIp6()
                    .map(ip -> "gateway").orElse("server");

            log.debug("Dynamically probing to resolve {} IP {}", hostToProbe, ipToProbe);
            hostService.startMonitoringIp(ipToProbe);

            Set<Host> hosts = hostService.getHostsByIp(ipToProbe);
            if (!hosts.isEmpty()) {
                int serverInfoIndex = sererInfoList.indexOf(serverInfo);
                Host host = hosts.iterator().next();
                serverInfo.setDhcpConnectVlan(host.vlan());
                serverInfo.setDhcpConnectMac(host.mac());
                // replace the serverInfo in the list
                sererInfoList.set(serverInfoIndex, serverInfo);
                newServerInfo = serverInfo;
                log.warn("Dynamically host found host {}", host);
            } else {
                log.debug("No host found host ip {} dynamically", ipToProbe);
            }
        }
        return newServerInfo;
    }

    /**
     * Gets Interface facing to the server for default host.
     *
     * @param serverInfo server information
     * @return the Interface facing to the server; null if not found
     */
    private Interface getServerInterface(DhcpServerInfo serverInfo) {
        Interface serverInterface = null;

        ConnectPoint dhcpServerConnectPoint = serverInfo.getDhcpServerConnectPoint().orElse(null);
        VlanId dhcpConnectVlan = serverInfo.getDhcpConnectVlan().orElse(null);

        if (dhcpServerConnectPoint != null && dhcpConnectVlan != null) {
        serverInterface = interfaceService.getInterfacesByPort(dhcpServerConnectPoint)
                    .stream()
                    .filter(iface -> Dhcp6HandlerUtil.interfaceContainsVlan(iface, dhcpConnectVlan))
                    .findFirst()
                    .orElse(null);
        } else {
            log.warn("DHCP server {} not resolve yet connectPoint {} vlan {}", serverInfo.getDhcpServerIp6(),
                    dhcpServerConnectPoint, dhcpConnectVlan);
        }

        return serverInterface;
    }

    private void requestDhcpPacket(Ip6Address serverIp) {
        requestServerDhcpPacket(serverIp);
        requestClientDhcpPacket(serverIp);
        requestServerLQPacket(serverIp);
    }

    private void cancelDhcpPacket(Ip6Address serverIp) {
        cancelServerDhcpPacket(serverIp);
        cancelClientDhcpPacket(serverIp);
        cancelServerLQPacket(serverIp);
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

    private void cancelServerLQPacket(Ip6Address serverIp) {
        TrafficSelector serverSelector =
                DefaultTrafficSelector.builder(LEASE_QUERY_RESPONSE_SELECTOR)
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

    private void requestServerLQPacket(Ip6Address serverIp) {
        TrafficSelector serverSelector =
                DefaultTrafficSelector.builder(LEASE_QUERY_RESPONSE_SELECTOR)
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
    private IpAddress getFirstIpByHost(Boolean directConnFlag, MacAddress clientMac, VlanId vlanId) {
        IpAddress nextHopIp;
        // pick out the first link-local ip address
        HostId gwHostId = HostId.hostId(clientMac, vlanId);
        Host gwHost = hostService.getHost(gwHostId);
        if (gwHost == null) {
            log.warn("Can't find gateway host for hostId {}", gwHostId);
            return null;
        }
        if (directConnFlag) {
            nextHopIp = gwHost.ipAddresses()
                    .stream()
                    .filter(IpAddress::isIp6)
                    .map(IpAddress::getIp6Address)
                    .findFirst()
                    .orElse(null);
        } else {
            nextHopIp = gwHost.ipAddresses()
                    .stream()
                    .filter(IpAddress::isIp6)
                    .filter(ip6 -> ip6.isLinkLocal())
                    .map(IpAddress::getIp6Address)
                    .findFirst()
                    .orElse(null);
        }
        return nextHopIp;
    }

    private List<DhcpServerInfo> findValidServerInfo(boolean directConnFlag) {
        List<DhcpServerInfo> validServerInfo;

        if (directConnFlag || indirectServerInfoList.isEmpty()) {
            validServerInfo = new ArrayList<DhcpServerInfo>(defaultServerInfoList);
        } else {
            validServerInfo = new ArrayList<DhcpServerInfo>(indirectServerInfoList);
        }
        return validServerInfo;
    }

    private DhcpServerInfo findServerInfoFromServer(boolean directConnFlag, ConnectPoint inPort) {
        List<DhcpServerInfo> validServerInfoList = findValidServerInfo(directConnFlag);
        DhcpServerInfo  foundServerInfo = null;
        for (DhcpServerInfo serverInfo : validServerInfoList) {
            if (inPort.equals(serverInfo.getDhcpServerConnectPoint().get())) {
                foundServerInfo = serverInfo;
                log.debug("ServerInfo found for Rcv port {} Server Connect Point {} for {}",
                        inPort, serverInfo.getDhcpServerConnectPoint(), directConnFlag ? "direct" : "indirect");
                break;
            }
        }
        return foundServerInfo;
    }

    /**
     * Set the dhcp6 lease expiry poll interval value.
     *
     * @param val poll interval value in seconds
     */
    @Override
    public void setDhcp6PollInterval(int val) {
        dhcp6PollInterval = val;
    }

    /**
     * get the dhcp6 lease expiry poll interval value.
     * This is a private function
     * @return  poll interval value in seconds
     */
    private int getDhcp6PollInterval() {
        return dhcp6PollInterval;
    }

    /**
     * Find lease-expired ipaddresses and pd prefixes.
     * Removing host/route/fpm entries.
     */
    public void timeTick() {
        long currentTime = System.currentTimeMillis();
        Collection<DhcpRecord> records = dhcpRelayStore.getDhcpRecords();

        log.debug("timeTick called currenttime {} records num {} ", currentTime, records.size());

        records.forEach(record -> {
                    boolean addrOrPdRemoved = false;
                    DHCP6.MsgType ip6Status = record.ip6Status().orElse(null);
                    if (ip6Status == null) {
                        log.debug("record is not valid v6 record.");
                        return;
                    }

                    if ((currentTime - record.getLastIp6Update()) >
                            ((record.addrPrefTime() + getDhcp6PollInterval() / 2) * 1000)) {
                        // remove ipaddress from host/route table
                        IpAddress ip = record.ip6Address().orElse(null);
                        if (ip != null) {
                            if (record.directlyConnected()) {
                                providerService.removeIpFromHost(HostId.hostId(record.macAddress(),
                                        record.vlanId()), ip);
                            } else {
                                MacAddress gwMac = record.nextHop().orElse(null);
                                if (gwMac == null) {
                                    log.warn("Can't find gateway mac address from record {} for ip6Addr", record);
                                    return;
                                }
                                IpAddress nextHopIp = getFirstIpByHost(record.directlyConnected(),
                                        gwMac,
                                        record.vlanId());
                                Route route = new Route(Route.Source.DHCP, ip.toIpPrefix(), nextHopIp);
                                routeStore.removeRoute(route);
                            }
                            record.updateAddrPrefTime(0);
                            record.ip6Address(null);
                            addrOrPdRemoved = true;
                            dhcpRelayStore.updateDhcpRecord(HostId.hostId(record.macAddress(),
                                    record.vlanId()), record);
                            log.warn("IP6 address is set to null. delta {} lastUpdate {} addrPrefTime {}",
                                    (currentTime - record.getLastIp6Update()), record.getLastIp6Update(),
                                    record.addrPrefTime());
                        }
                    }
                    if ((currentTime - record.getLastPdUpdate()) >
                            ((record.pdPrefTime() + getDhcp6PollInterval() / 2) * 1000)) {
                        // remove PD from route/fpm table
                        IpPrefix pdIpPrefix = record.pdPrefix().orElse(null);
                        if (pdIpPrefix != null) {
                            if (record.directlyConnected()) {
                                providerService.removeIpFromHost(HostId.hostId(record.macAddress(), record.vlanId()),
                                        pdIpPrefix.address().getIp6Address());
                            } else {
                                MacAddress gwMac = record.nextHop().orElse(null);
                                if (gwMac == null) {
                                    log.warn("Can't find gateway mac address from record {} for PD prefix", record);
                                    return;
                                }
                                IpAddress nextHopIp = getFirstIpByHost(record.directlyConnected(),
                                        gwMac,
                                        record.vlanId());
                                Route route = new Route(Route.Source.DHCP, pdIpPrefix, nextHopIp);
                                routeStore.removeRoute(route);
                                if (this.dhcpFpmEnabled) {
                                    dhcpFpmPrefixStore.removeFpmRecord(pdIpPrefix);
                                }
                            }
                            record.updatePdPrefTime(0);
                            record.pdPrefix(null);
                            addrOrPdRemoved = true;
                            dhcpRelayStore.updateDhcpRecord(HostId.hostId(record.macAddress(),
                                    record.vlanId()), record);
                            log.warn("PD prefix is set to null.delta {} pdPrefTime {}",
                                    (currentTime - record.getLastPdUpdate()), record.pdPrefTime());
                        }
                    }
                    if (addrOrPdRemoved &&
                            !record.ip6Address().isPresent() && !record.pdPrefix().isPresent()) {
                        log.warn("ip6Status {} IP6 address and IP6 PD both are null. Remove record.", ip6Status);
                        dhcpRelayStore.removeDhcpRecord(HostId.hostId(record.macAddress(), record.vlanId()));
                    }
                }
        );
    }
}
