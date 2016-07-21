/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteAdminService;
import org.onosproject.routing.fpm.protocol.FpmHeader;
import org.onosproject.routing.fpm.protocol.Netlink;
import org.onosproject.routing.fpm.protocol.RouteAttribute;
import org.onosproject.routing.fpm.protocol.RouteAttributeDst;
import org.onosproject.routing.fpm.protocol.RouteAttributeGateway;
import org.onosproject.routing.fpm.protocol.RtNetlink;
import org.onosproject.routing.fpm.protocol.RtProtocol;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Forwarding Plane Manager (FPM) route source.
 */
@Service
@Component(immediate = true, enabled = false)
public class FpmManager implements FpmInfoService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int FPM_PORT = 2620;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouteAdminService routeService;

    private ServerBootstrap serverBootstrap;
    private Channel serverChannel;
    private ChannelGroup allChannels = new DefaultChannelGroup();

    private Map<SocketAddress, Long> peers = new ConcurrentHashMap<>();

    private Map<IpPrefix, Route> fpmRoutes = new ConcurrentHashMap<>();

    @Property(name = "clearRoutes", boolValue = true,
            label = "Whether to clear routes when the FPM connection goes down")
    private boolean clearRoutes = true;

    @Activate
    protected void activate(ComponentContext context) {
        componentConfigService.registerProperties(getClass());
        modified(context);
        startServer();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
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
        ChannelFactory channelFactory = new NioServerSocketChannelFactory(
                newCachedThreadPool(groupedThreads("onos/fpm", "sm-boss-%d", log)),
                newCachedThreadPool(groupedThreads("onos/fpm", "sm-worker-%d", log)));
        ChannelPipelineFactory pipelineFactory = () -> {
            // Allocate a new session per connection
            FpmSessionHandler fpmSessionHandler =
                    new FpmSessionHandler(new InternalFpmListener());
            FpmFrameDecoder fpmFrameDecoder =
                    new FpmFrameDecoder();

            // Setup the processing pipeline
            ChannelPipeline pipeline = Channels.pipeline();
            pipeline.addLast("FpmFrameDecoder", fpmFrameDecoder);
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
            clearRoutes();
        }
    }

    private void fpmMessage(FpmHeader fpmMessage) {
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
            route = new Route(Route.Source.FPM, prefix, gateway);

            fpmRoutes.put(prefix, route);

            updates.add(route);
            break;
        case RTM_DELROUTE:
            Route existing = fpmRoutes.remove(prefix);
            if (existing == null) {
                log.warn("Got delete for non-existent prefix");
                return;
            }

            route = new Route(Route.Source.FPM, prefix, existing.nextHop());

            withdraws.add(route);
            break;
        case RTM_GETROUTE:
        default:
            break;
        }

        routeService.withdraw(withdraws);
        routeService.update(updates);
    }


    private void clearRoutes() {
        log.info("Clearing all routes");
        routeService.withdraw(ImmutableList.copyOf(fpmRoutes.values()));
    }

    @Override
    public Map<SocketAddress, Long> peers() {
        return ImmutableMap.copyOf(peers);
    }

    private class InternalFpmListener implements FpmListener {
        @Override
        public void fpmMessage(FpmHeader fpmMessage) {
            FpmManager.this.fpmMessage(fpmMessage);
        }

        @Override
        public boolean peerConnected(SocketAddress address) {
            if (peers.keySet().contains(address)) {
                return false;
            }

            peers.put(address, System.currentTimeMillis());
            return true;
        }

        @Override
        public void peerDisconnected(SocketAddress address) {
            log.info("FPM connection to {} went down", address);

            if (clearRoutes) {
                clearRoutes();
            }

            peers.remove(address);
        }
    }

}
