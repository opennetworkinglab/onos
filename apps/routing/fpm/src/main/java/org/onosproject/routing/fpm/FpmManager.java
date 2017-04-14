/*
 * Copyright 2017-present Open Networking Laboratory
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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
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
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteAdminService;
import org.onosproject.routing.fpm.protocol.FpmHeader;
import org.onosproject.routing.fpm.protocol.Netlink;
import org.onosproject.routing.fpm.protocol.RouteAttribute;
import org.onosproject.routing.fpm.protocol.RouteAttributeDst;
import org.onosproject.routing.fpm.protocol.RouteAttributeGateway;
import org.onosproject.routing.fpm.protocol.RtNetlink;
import org.onosproject.routing.fpm.protocol.RtProtocol;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Forwarding Plane Manager (FPM) route source.
 */
@Service
@Component(immediate = true)
public class FpmManager implements FpmInfoService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int FPM_PORT = 2620;
    private static final String APP_NAME = "org.onosproject.fpm";
    private static final int IDLE_TIMEOUT_SECS = 5;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouteAdminService routeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private ServerBootstrap serverBootstrap;
    private Channel serverChannel;
    private ChannelGroup allChannels = new DefaultChannelGroup();

    private ConsistentMap<FpmPeer, Set<FpmConnectionInfo>> peers;

    private Map<FpmPeer, Map<IpPrefix, Route>> fpmRoutes = new ConcurrentHashMap<>();

    @Property(name = "clearRoutes", boolValue = true,
            label = "Whether to clear routes when the FPM connection goes down")
    private boolean clearRoutes = true;

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

        coreService.registerApplication(APP_NAME, peers::destroy);

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
        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        if (properties == null) {
            return;
        }
        String strClearRoutes = Tools.get(properties, "clearRoutes");
        clearRoutes = Boolean.parseBoolean(strClearRoutes);

        log.info("clearRoutes set to {}", clearRoutes);
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
                    new FpmSessionHandler(new InternalFpmListener());
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
            peers.keySet().forEach(this::clearRoutes);
        }
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

        routeService.withdraw(withdraws);
        routeService.update(updates);
    }


    private void clearRoutes(FpmPeer peer) {
        log.info("Clearing all routes for peer {}", peer);
        Map<IpPrefix, Route> routes = fpmRoutes.remove(peer);
        if (routes != null) {
            routeService.withdraw(routes.values());
        }
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

    private class InternalFpmListener implements FpmListener {
        @Override
        public void fpmMessage(FpmPeer peer, FpmHeader fpmMessage) {
            FpmManager.this.fpmMessage(peer, fpmMessage);
        }

        @Override
        public boolean peerConnected(FpmPeer peer) {
            if (peers.keySet().contains(peer)) {
                return false;
            }

            NodeId localNode = clusterService.getLocalNode().id();
            peers.compute(peer, (p, infos) -> {
                if (infos == null) {
                    infos = new HashSet<>();
                }

                infos.add(new FpmConnectionInfo(localNode, peer, System.currentTimeMillis()));
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
                        .ifPresent(i -> infos.remove(i));

                if (infos.isEmpty()) {
                    return null;
                }

                return infos;
            });
        }
    }

}
