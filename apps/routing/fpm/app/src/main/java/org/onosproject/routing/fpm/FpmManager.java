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
 */

package org.onosproject.routing.fpm;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteAdminService;
import org.onosproject.routing.fpm.api.FpmPrefixStore;
import org.onosproject.routing.fpm.api.FpmPrefixStoreEvent;
import org.onosproject.routing.fpm.api.FpmRecord;
import org.onosproject.routing.fpm.protocol.FpmHeader;
import org.onosproject.routing.fpm.protocol.Netlink;
import org.onosproject.routing.fpm.protocol.NetlinkMessageType;
import org.onosproject.routing.fpm.protocol.RouteAttribute;
import org.onosproject.routing.fpm.protocol.RouteAttributeDst;
import org.onosproject.routing.fpm.protocol.RouteAttributeGateway;
import org.onosproject.routing.fpm.protocol.RtNetlink;
import org.onosproject.routing.fpm.protocol.RtProtocol;
import org.onosproject.store.StoreDelegate;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncDistributedLock;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.routing.fpm.OsgiPropertyConstants.CLEAR_ROUTES;
import static org.onosproject.routing.fpm.OsgiPropertyConstants.CLEAR_ROUTES_DEFAULT;
import static org.onosproject.routing.fpm.OsgiPropertyConstants.PD_PUSH_ENABLED;
import static org.onosproject.routing.fpm.OsgiPropertyConstants.PD_PUSH_ENABLED_DEFAULT;
import static org.onosproject.routing.fpm.OsgiPropertyConstants.PD_PUSH_NEXT_HOP_IPV4;
import static org.onosproject.routing.fpm.OsgiPropertyConstants.PD_PUSH_NEXT_HOP_IPV4_DEFAULT;
import static org.onosproject.routing.fpm.OsgiPropertyConstants.PD_PUSH_NEXT_HOP_IPV6;
import static org.onosproject.routing.fpm.OsgiPropertyConstants.PD_PUSH_NEXT_HOP_IPV6_DEFAULT;

/**
 * Forwarding Plane Manager (FPM) route source.
 */
@Component(
       immediate = true,
       service = FpmInfoService.class,
       property = {
           CLEAR_ROUTES + ":Boolean=" + CLEAR_ROUTES_DEFAULT,
           PD_PUSH_ENABLED + ":Boolean=" + PD_PUSH_ENABLED_DEFAULT,
           PD_PUSH_NEXT_HOP_IPV4 + "=" + PD_PUSH_NEXT_HOP_IPV4_DEFAULT,
           PD_PUSH_NEXT_HOP_IPV6 + "=" + PD_PUSH_NEXT_HOP_IPV6_DEFAULT,
       }
)
public class FpmManager implements FpmInfoService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int FPM_PORT = 2620;
    private static final String APP_NAME = "org.onosproject.fpm";
    private static final int IDLE_TIMEOUT_SECS = 5;
    private static final String LOCK_NAME = "fpm-manager-lock";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected RouteAdminService routeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
               bind = "bindRipStore",
               unbind = "unbindRipStore",
               policy = ReferencePolicy.DYNAMIC,
               target = "(_fpm_type=RIP)")
    protected volatile FpmPrefixStore ripStore;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
               bind = "bindDhcpStore",
               unbind = "unbindDhcpStore",
               policy = ReferencePolicy.DYNAMIC,
               target = "(_fpm_type=DHCP)")
    protected volatile FpmPrefixStore dhcpStore;

    private final StoreDelegate<FpmPrefixStoreEvent> fpmPrefixStoreDelegate
                            = new FpmPrefixStoreDelegate();

    private ApplicationId appId;
    private ServerBootstrap serverBootstrap;
    private Channel serverChannel;
    private ChannelGroup allChannels = new DefaultChannelGroup();
    private final InternalClusterListener clusterListener = new InternalClusterListener();
    private AsyncDistributedLock asyncLock;

    private ExecutorService clusterEventExecutor;

    private ConsistentMap<FpmPeer, Set<FpmConnectionInfo>> peers;

    private Map<FpmPeer, Map<IpPrefix, Route>> fpmRoutes = new ConcurrentHashMap<>();

    //Local cache for peers to be used in case of cluster partition.
    private Map<FpmPeer, Set<FpmConnectionInfo>> localPeers = new ConcurrentHashMap<>();

    /** Whether to clear routes when the FPM connection goes down. */
    private boolean clearRoutes = CLEAR_ROUTES_DEFAULT;

    /** Whether to push prefixes to Quagga over fpm connection. */
    private boolean pdPushEnabled = PD_PUSH_ENABLED_DEFAULT;

    /** IPv4 next-hop address for PD Pushing. */
    private List<Ip4Address> pdPushNextHopIPv4 = null;

    /** IPv6 next-hop address for PD Pushing. */
    private List<Ip6Address> pdPushNextHopIPv6 = null;

    protected void bindRipStore(FpmPrefixStore store) {
        if ((ripStore == null) && (store != null)) {
            ripStore = store;
            ripStore.setDelegate(fpmPrefixStoreDelegate);
            for (Channel ch : allChannels) {
                processRipStaticRoutes(ch);
            }
        }
    }

    protected void unbindRipStore(FpmPrefixStore store) {
        if (ripStore == store) {
            ripStore.unsetDelegate(fpmPrefixStoreDelegate);
            ripStore = null;
        }
    }

    protected void bindDhcpStore(FpmPrefixStore store) {
        if ((dhcpStore == null) && (store != null)) {
            dhcpStore = store;
            dhcpStore.setDelegate(fpmPrefixStoreDelegate);
            for (Channel ch : allChannels) {
                processDhcpStaticRoutes(ch);
            }
        }
    }

    protected void unbindDhcpStore(FpmPrefixStore store) {
        if (dhcpStore == store) {
            dhcpStore.unsetDelegate(fpmPrefixStoreDelegate);
            dhcpStore = null;
        }
    }

    @Activate
    protected void activate(ComponentContext context) {
        componentConfigService.preSetProperty(
                "org.onosproject.incubator.store.routing.impl.RouteStoreImpl",
                "distributed", "true");

        componentConfigService.registerProperties(getClass());

        KryoNamespace serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(FpmPeer.class)
                .register(FpmConnectionInfo.class)
                .build();
        peers = storageService.<FpmPeer, Set<FpmConnectionInfo>>consistentMapBuilder()
                .withName("fpm-connections")
                .withSerializer(Serializer.using(serializer))
                .build();

        modified(context);
        startServer();

        appId = coreService.registerApplication(APP_NAME, peers::destroy);

        asyncLock = storageService.lockBuilder().withName(LOCK_NAME).build();

        clusterEventExecutor = Executors.newSingleThreadExecutor(groupedThreads("fpm-event-main", "%d", log));
        clusterService.addListener(clusterListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        componentConfigService.preSetProperty(
                "org.onosproject.incubator.store.routing.impl.RouteStoreImpl",
                "distributed", "false");

        stopServer();
        fpmRoutes.clear();
        componentConfigService.unregisterProperties(getClass(), false);

        clusterService.removeListener(clusterListener);
        clusterEventExecutor.shutdown();
        asyncLock.unlock();

        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Ip4Address rurIPv4Address;
        Ip6Address rurIPv6Address;
        Dictionary<?, ?> properties = context.getProperties();
        if (properties == null) {
            return;
        }
        String strClearRoutes = Tools.get(properties, CLEAR_ROUTES);
        if (strClearRoutes != null) {
            clearRoutes = Boolean.parseBoolean(strClearRoutes);
            log.info("clearRoutes is {}", clearRoutes);
        }

        String strPdPushEnabled = Tools.get(properties, PD_PUSH_ENABLED);
        if (strPdPushEnabled != null) {
            boolean oldValue = pdPushEnabled;
            pdPushEnabled = Boolean.parseBoolean(strPdPushEnabled);
            if (pdPushEnabled) {

                pdPushNextHopIPv4 = new ArrayList<Ip4Address>();
                pdPushNextHopIPv6 = new ArrayList<Ip6Address>();

                String strPdPushNextHopIPv4 = Tools.get(properties, PD_PUSH_NEXT_HOP_IPV4);
                if (strPdPushNextHopIPv4 != null) {
                    List<String> strPdPushNextHopIPv4List = Arrays.asList(strPdPushNextHopIPv4.split(","));
                    for (String nextHop : strPdPushNextHopIPv4List) {
                        log.trace("IPv4 next hop added is:" + nextHop);
                        pdPushNextHopIPv4.add(Ip4Address.valueOf(nextHop));
                    }
                }
                String strPdPushNextHopIPv6 = Tools.get(properties, PD_PUSH_NEXT_HOP_IPV6);
                if (strPdPushNextHopIPv6 != null) {
                    List<String> strPdPushNextHopIPv6List = Arrays.asList(strPdPushNextHopIPv6.split(","));
                    for (String nextHop : strPdPushNextHopIPv6List) {
                        log.trace("IPv6 next hop added is:" + nextHop);
                        pdPushNextHopIPv6.add(Ip6Address.valueOf(nextHop));
                    }
                }

                if (pdPushNextHopIPv4.size() == 0) {
                    rurIPv4Address = interfaceService.getInterfaces()
                        .stream()
                        .filter(iface -> iface.name().contains("RUR"))
                        .map(Interface::ipAddressesList)
                        .flatMap(Collection::stream)
                        .map(InterfaceIpAddress::ipAddress)
                        .filter(IpAddress::isIp4)
                        .map(IpAddress::getIp4Address)
                        .findFirst()
                        .orElse(null);
                    log.debug("RUR IPv4 address extracted from netcfg is: {}", rurIPv4Address);
                    if (rurIPv4Address != null) {
                        pdPushNextHopIPv4.add(rurIPv4Address);
                    } else {
                        log.debug("Unable to extract RUR IPv4 address from netcfg");
                    }

                }

                if (pdPushNextHopIPv6 == null || pdPushNextHopIPv6.size() == 0) {
                    rurIPv6Address = interfaceService.getInterfaces()
                        .stream()
                        .filter(iface -> iface.name().contains("RUR"))
                        .map(Interface::ipAddressesList)
                        .flatMap(Collection::stream)
                        .map(InterfaceIpAddress::ipAddress)
                        .filter(IpAddress::isIp6)
                        .map(IpAddress::getIp6Address)
                        .findFirst()
                        .orElse(null);
                    log.debug("RUR IPv6 address extracted from netcfg is: {}", rurIPv6Address);
                    if (rurIPv6Address != null) {
                        pdPushNextHopIPv6.add(rurIPv6Address);
                    } else {
                        log.debug("Unable to extract RUR IPv6 address from netcfg");
                    }
                }

                log.info("PD pushing is enabled.");
                if (pdPushNextHopIPv4.size() != 0) {
                    log.info("ipv4 next-hop {} with {} items", pdPushNextHopIPv4.toString(), pdPushNextHopIPv4.size());
                } else {
                    log.info("ipv4 next-hop is null");
                }
                if (pdPushNextHopIPv6.size() != 0) {
                    log.info("ipv6 next-hop={} with {} items", pdPushNextHopIPv6.toString(), pdPushNextHopIPv6.size());
                } else {
                    log.info("ipv6 next-hop is null");
                }
                processStaticRoutes();
            } else {
                log.info("PD pushing is disabled.");
            }
        }
    }

    private void startServer() {
        HashedWheelTimer timer = new HashedWheelTimer(
                groupedThreads("onos/fpm", "fpm-timer-%d", log));

        ChannelFactory channelFactory = new NioServerSocketChannelFactory(
                newCachedThreadPool(groupedThreads("onos/fpm", "sm-boss-%d", log)),
                newCachedThreadPool(groupedThreads("onos/fpm", "sm-worker-%d", log)));
        ChannelPipelineFactory pipelineFactory = () -> {
            // Allocate a new session per connection
            IdleStateHandler idleHandler =
                    new IdleStateHandler(timer, IDLE_TIMEOUT_SECS, 0, 0);
            FpmSessionHandler fpmSessionHandler =
                    new FpmSessionHandler(this, new InternalFpmListener());
            FpmFrameDecoder fpmFrameDecoder = new FpmFrameDecoder();

            // Setup the processing pipeline
            ChannelPipeline pipeline = Channels.pipeline();
            pipeline.addLast("FpmFrameDecoder", fpmFrameDecoder);
            pipeline.addLast("idle", idleHandler);
            pipeline.addLast("FpmSession", fpmSessionHandler);
            return pipeline;
        };

        InetSocketAddress listenAddress = new InetSocketAddress(FPM_PORT);

        serverBootstrap = new ServerBootstrap(channelFactory);
        serverBootstrap.setOption("child.reuseAddr", true);
        serverBootstrap.setOption("child.keepAlive", true);
        serverBootstrap.setOption("child.tcpNoDelay", true);
        serverBootstrap.setPipelineFactory(pipelineFactory);
        try {
            serverChannel = serverBootstrap.bind(listenAddress);
            allChannels.add(serverChannel);
        } catch (ChannelException e) {
            log.debug("Exception binding to FPM port {}: ",
                    listenAddress.getPort(), e);
            stopServer();
        }
    }

    private void stopServer() {
        allChannels.close().awaitUninterruptibly();
        allChannels.clear();
        if (serverBootstrap != null) {
            serverBootstrap.releaseExternalResources();
        }

        if (clearRoutes) {
            log.debug("Clearing routes for the peer");
            peers.keySet().forEach(this::clearRoutes);
        }
    }

    private boolean routeInDhcpStore(IpPrefix prefix) {

        if (dhcpStore != null) {
            Collection<FpmRecord> dhcpRecords = dhcpStore.getFpmRecords();
            return dhcpRecords.stream().anyMatch(record -> record.ipPrefix().equals(prefix));
        }
        return false;
    }

    private boolean routeInRipStore(IpPrefix prefix) {

        if (ripStore != null) {
            Collection<FpmRecord> ripRecords = ripStore.getFpmRecords();
            return ripRecords.stream().anyMatch(record -> record.ipPrefix().equals(prefix));
        }
        return false;
    }

    private void fpmMessage(FpmPeer peer, FpmHeader fpmMessage) {
        if (fpmMessage.type() == FpmHeader.FPM_TYPE_KEEPALIVE) {
            return;
        }

        Netlink netlink = fpmMessage.netlink();
        RtNetlink rtNetlink = netlink.rtNetlink();

        if (log.isTraceEnabled()) {
            log.trace("Received FPM message: {}", fpmMessage);
        }

        if (!(rtNetlink.protocol() == RtProtocol.ZEBRA ||
                rtNetlink.protocol() == RtProtocol.UNSPEC)) {
            log.trace("Ignoring non-zebra route");
            return;
        }

        IpAddress dstAddress = null;
        IpAddress gateway = null;

        for (RouteAttribute attribute : rtNetlink.attributes()) {
            if (attribute.type() == RouteAttribute.RTA_DST) {
                RouteAttributeDst raDst = (RouteAttributeDst) attribute;
                dstAddress = raDst.dstAddress();
            } else if (attribute.type() == RouteAttribute.RTA_GATEWAY) {
                RouteAttributeGateway raGateway = (RouteAttributeGateway) attribute;
                gateway = raGateway.gateway();
            }
        }

        if (dstAddress == null) {
            log.error("Dst address missing!");
            return;
        }

        IpPrefix prefix = IpPrefix.valueOf(dstAddress, rtNetlink.dstLength());

        // Ignore routes that we sent.
        if (gateway != null && (
                (prefix.isIp4() && pdPushNextHopIPv4 != null &&
                        pdPushNextHopIPv4.contains(gateway.getIp4Address())) ||
                (prefix.isIp6() && pdPushNextHopIPv6 != null &&
                        pdPushNextHopIPv6.contains(gateway.getIp6Address())))) {
            if (routeInDhcpStore(prefix) || routeInRipStore(prefix)) {
                return;
            }
        }

        List<Route> updates = new LinkedList<>();
        List<Route> withdraws = new LinkedList<>();

        Route route;
        switch (netlink.type()) {
        case RTM_NEWROUTE:
            if (gateway == null) {
                // We ignore interface routes with no gateway for now.
                return;
            }
            route = new Route(Route.Source.FPM, prefix, gateway, clusterService.getLocalNode().id());


            Route oldRoute = fpmRoutes.get(peer).put(prefix, route);

            if (oldRoute != null) {
                log.trace("Swapping {} with {}", oldRoute, route);
                withdraws.add(oldRoute);
            }
            updates.add(route);
            break;
        case RTM_DELROUTE:
            Route existing = fpmRoutes.get(peer).remove(prefix);
            if (existing == null) {
                log.warn("Got delete for non-existent prefix");
                return;
            }

            route = new Route(Route.Source.FPM, prefix, existing.nextHop(), clusterService.getLocalNode().id());

            withdraws.add(route);
            break;
        case RTM_GETROUTE:
        default:
            break;
        }

        updateRouteStore(updates, withdraws);
    }

    private synchronized void updateRouteStore(Collection<Route> routesToAdd, Collection<Route> routesToRemove) {
        routeService.withdraw(routesToRemove);
        routeService.update(routesToAdd);
    }

    private void clearRoutes(FpmPeer peer) {
        log.info("Clearing all routes for peer {}", peer);
        Map<IpPrefix, Route> routes = fpmRoutes.remove(peer);
        if (routes != null) {
            updateRouteStore(Lists.newArrayList(), routes.values());
        }
    }

    public void processStaticRoutes() {
        log.debug("processStaticRoutes function is called");
        for (Channel ch : allChannels) {
            processStaticRoutes(ch);
        }
    }

    public void processStaticRoutes(Channel ch) {
        processRipStaticRoutes(ch);
        processDhcpStaticRoutes(ch);
    }

    private void processRipStaticRoutes(Channel ch) {

        /* Get RIP static routes. */
        if (ripStore != null) {
            Collection<FpmRecord> ripRecords = ripStore.getFpmRecords();
            log.info("RIP store size is {}", ripRecords.size());

            ripRecords.forEach(record -> sendRouteUpdateToChannel(true,
                               record.ipPrefix(), ch));
        }
    }

    private void processDhcpStaticRoutes(Channel ch) {

        /* Get Dhcp static routes. */
        if (dhcpStore != null) {
            Collection<FpmRecord> dhcpRecords = dhcpStore.getFpmRecords();
            log.info("Dhcp store size is {}", dhcpRecords.size());

            dhcpRecords.forEach(record -> sendRouteUpdateToChannel(true,
                                record.ipPrefix(), ch));
        }
    }

    private void updateRoute(IpAddress pdPushNextHop, boolean isAdd, IpPrefix prefix,
                             Channel ch, int raLength, short addrFamily) {
        try {
            RouteAttributeDst raDst = RouteAttributeDst.builder()
                    .length(raLength)
                    .type(RouteAttribute.RTA_DST)
                    .dstAddress(prefix.address())
                    .build();

            RouteAttributeGateway raGateway = RouteAttributeGateway.builder()
                    .length(raLength)
                    .type(RouteAttribute.RTA_GATEWAY)
                    .gateway(pdPushNextHop)
                    .build();

            // Build RtNetlink.
            RtNetlink rtNetlink = RtNetlink.builder()
                    .addressFamily(addrFamily)
                    .dstLength(prefix.prefixLength())
                    .routeAttribute(raDst)
                    .routeAttribute(raGateway)
                    .build();

            // Build Netlink.
            int messageLength = raDst.length() + raGateway.length() +
                    RtNetlink.RT_NETLINK_LENGTH + Netlink.NETLINK_HEADER_LENGTH;
            Netlink netLink = Netlink.builder()
                    .length(messageLength)
                    .type(isAdd ? NetlinkMessageType.RTM_NEWROUTE : NetlinkMessageType.RTM_DELROUTE)
                    .flags(Netlink.NETLINK_REQUEST | Netlink.NETLINK_CREATE)
                    .rtNetlink(rtNetlink)
                    .build();

            // Build FpmHeader.
            messageLength += FpmHeader.FPM_HEADER_LENGTH;
            FpmHeader fpmMessage = FpmHeader.builder()
                    .version(FpmHeader.FPM_VERSION_1)
                    .type(FpmHeader.FPM_TYPE_NETLINK)
                    .length(messageLength)
                    .netlink(netLink)
                    .build();

            // Encode message in a channel buffer and transmit.
            ch.write(fpmMessage.encode());
            log.debug("Fpm Message for updated route {}", fpmMessage.toString());
        } catch (RuntimeException e) {
            log.info("Route not sent over fpm connection.");
        }
    }

    private void sendRouteUpdateToChannel(boolean isAdd, IpPrefix prefix, Channel ch) {

        if (!pdPushEnabled) {
            return;
        }
        int raLength;
        short addrFamily;

        // Build route attributes.
        if (prefix.isIp4()) {
            List<Ip4Address> pdPushNextHopList;
            if (pdPushNextHopIPv4 == null || pdPushNextHopIPv4.size() == 0) {
                log.info("Prefix not pushed because ipv4 next-hop is null.");
                return;
            }
            pdPushNextHopList = pdPushNextHopIPv4;
            raLength =  Ip4Address.BYTE_LENGTH + RouteAttribute.ROUTE_ATTRIBUTE_HEADER_LENGTH;
            addrFamily = RtNetlink.RT_ADDRESS_FAMILY_INET;
            for (Ip4Address pdPushNextHop: pdPushNextHopList) {
                log.trace("IPv4 next hop is:" + pdPushNextHop);
                updateRoute(pdPushNextHop, isAdd, prefix, ch, raLength, addrFamily);
            }
        } else {
            List<Ip6Address> pdPushNextHopList;
            if (pdPushNextHopIPv6 == null || pdPushNextHopIPv6.size() == 0) {
                log.info("Prefix not pushed because ipv6 next-hop is null.");
                return;
            }
            pdPushNextHopList = pdPushNextHopIPv6;
            raLength =  Ip6Address.BYTE_LENGTH + RouteAttribute.ROUTE_ATTRIBUTE_HEADER_LENGTH;
            addrFamily = RtNetlink.RT_ADDRESS_FAMILY_INET6;
            for (Ip6Address pdPushNextHop: pdPushNextHopList) {
                log.trace("IPv6 next hop is:" + pdPushNextHop);
                updateRoute(pdPushNextHop, isAdd, prefix, ch, raLength, addrFamily);
            }
        }
    }

    private void sendRouteUpdate(boolean isAdd, IpPrefix prefix) {

         for (Channel ch : allChannels) {
            sendRouteUpdateToChannel(isAdd, prefix, ch);
        }
    }

    public boolean isPdPushEnabled() {
        return pdPushEnabled;
    }

    private FpmPeerInfo toFpmInfo(FpmPeer peer, Collection<FpmConnectionInfo> connections) {
        return new FpmPeerInfo(connections,
                fpmRoutes.getOrDefault(peer, Collections.emptyMap()).size());
    }

    @Override
    public Map<FpmPeer, FpmPeerInfo> peers() {
        return peers.asJavaMap().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> toFpmInfo(e.getKey(), e.getValue())));
    }

    @Override
    public void updateAcceptRouteFlag(Collection<FpmPeerAcceptRoutes> modifiedPeers) {
        modifiedPeers.forEach(modifiedPeer -> {
            log.debug("FPM connection to {} is disabled", modifiedPeer);
            NodeId localNode = clusterService.getLocalNode().id();
            log.debug("Peer Flag {}", modifiedPeer.isAcceptRoutes());
            peers.compute(modifiedPeer.peer(), (p, infos) -> {
                if (infos == null) {
                    return null;
                }
                Iterator<FpmConnectionInfo> iterator = infos.iterator();
                if (iterator.hasNext()) {
                    FpmConnectionInfo connectionInfo = iterator.next();
                    if (connectionInfo.isAcceptRoutes() == modifiedPeer.isAcceptRoutes()) {
                        return null;
                    }
                    localPeers.remove(modifiedPeer.peer());
                    infos.remove(connectionInfo);
                    infos.add(new FpmConnectionInfo(localNode, modifiedPeer.peer(),
                            System.currentTimeMillis(), modifiedPeer.isAcceptRoutes()));
                    localPeers.put(modifiedPeer.peer(), infos);
                }
                Map<IpPrefix, Route> routes = fpmRoutes.get(modifiedPeer.peer());
                if (routes != null && !modifiedPeer.isAcceptRoutes()) {
                    updateRouteStore(Lists.newArrayList(), routes.values());
                } else {
                    updateRouteStore(routes.values(), Lists.newArrayList());
                }

                return infos;
            });
        });

    }

    private class InternalFpmListener implements FpmListener {
        @Override
        public void fpmMessage(FpmPeer peer, FpmHeader fpmMessage) {
            FpmManager.this.fpmMessage(peer, fpmMessage);
        }

        @Override
        public boolean peerConnected(FpmPeer peer) {
            log.info("FPM connection to {} was connected", peer);
            if (peers.keySet().contains(peer)) {
                return false;
            }

            NodeId localNode = clusterService.getLocalNode().id();
            peers.compute(peer, (p, infos) -> {
                if (infos == null) {
                    infos = new HashSet<>();
                }

                infos.add(new FpmConnectionInfo(localNode, peer, System.currentTimeMillis(), true));
                localPeers.put(peer, infos);
                return infos;
            });

            fpmRoutes.computeIfAbsent(peer, p -> new ConcurrentHashMap<>());
            return true;
        }

        @Override
        public void peerDisconnected(FpmPeer peer) {
            log.info("FPM connection to {} went down", peer);

            if (clearRoutes) {
                clearRoutes(peer);
            }

            peers.compute(peer, (p, infos) -> {
                if (infos == null) {
                    return null;
                }

                infos.stream()
                        .filter(i -> i.connectedTo().equals(clusterService.getLocalNode().id()))
                        .findAny()
                        .ifPresent(i -> {
                            infos.remove(i);
                            localPeers.get(peer).remove(i);
                        });

                if (infos.isEmpty()) {
                    return null;
                }

                return infos;
            });
        }
    }

    /**
     * Adds a channel to the channel group.
     *
     * @param channel the channel to add
     */
    public void addSessionChannel(Channel channel) {
        allChannels.add(channel);
    }

    /**
     * Removes a channel from the channel group.
     *
     * @param channel the channel to remove
     */
    public void removeSessionChannel(Channel channel) {
        allChannels.remove(channel);
    }

   /**
     * Store delegate for Fpm Prefix store.
     * Handles Fpm prefix store event.
     */
    class FpmPrefixStoreDelegate implements StoreDelegate<FpmPrefixStoreEvent> {

        @Override
        public void notify(FpmPrefixStoreEvent e) {

            log.trace("FpmPrefixStoreEvent notify");

            FpmRecord record = e.subject();
            switch (e.type()) {
                case ADD:
                    sendRouteUpdate(true, record.ipPrefix());
                    break;
                case REMOVE:
                    sendRouteUpdate(false, record.ipPrefix());
                    break;
                default:
                    log.warn("unsupported store event type", e.type());
                    return;
            }
        }
    }

    private class InternalClusterListener implements ClusterEventListener {
        @Override
        public void event(ClusterEvent event) {
            clusterEventExecutor.execute(() -> {
                if (event.instanceType() == ClusterEvent.InstanceType.STORAGE) {
                    log.debug("Skipping cluster event for {}", event.subject().id().id());
                    return;
                }

                log.info("Receives ClusterEvent {} for {}", event.type(), event.subject().id());
                switch (event.type()) {
                    case INSTANCE_READY:
                        // When current node is healing from a network partition,
                        // seeing INSTANCE_READY means current node has the ability to read from the cluster,
                        // but it is possible that current node still can't write to the cluster at this moment.
                        // The AsyncDistributedLock is introduced to ensure we attempt to push FPM routes
                        // after current node can write.
                        // Adding 15 seconds retry for the current node to be able to write.
                        asyncLock.tryLock(Duration.ofSeconds(15)).whenComplete((result, error) -> {
                            if (result != null && result.isPresent()) {
                                log.debug("Lock obtained. Push local FPM routes to route store");
                                // All FPM routes on current node will be pushed again even when current node is not
                                // the one that becomes READY. A better way is to do this only on the minority nodes.
                                pushFpmRoutes();
                                localPeers.forEach((key, value) -> peers.put(key, value));
                                asyncLock.unlock();
                            } else {
                                log.debug("Fail to obtain lock. Abort.");
                            }
                        });
                        break;
                    case INSTANCE_DEACTIVATED:
                    case INSTANCE_REMOVED:
                        ImmutableMap.copyOf(peers.asJavaMap()).forEach((key, value) -> {
                            if (value != null) {
                                value.stream()
                                    .filter(i -> i.connectedTo().equals(event.subject().id()))
                                    .findAny()
                                    .ifPresent(value::remove);
                                log.info("Connection {} removed for disabled peer {}", value, key);
                                if (value.isEmpty()) {
                                    peers.remove(key);
                                }
                            }
                        });
                        break;
                    case INSTANCE_ADDED:
                    case INSTANCE_ACTIVATED:
                    default:
                        break;
                }
            });
        }
    }

    @Override
    public void pushFpmRoutes() {
        Set<Route> routes = fpmRoutes.values().stream()
                .map(Map::entrySet).flatMap(Set::stream).map(Map.Entry::getValue)
                .collect(Collectors.toSet());
        updateRouteStore(routes, Lists.newArrayList());
        log.info("{} FPM routes have been updated to route store", routes.size());
    }
}
