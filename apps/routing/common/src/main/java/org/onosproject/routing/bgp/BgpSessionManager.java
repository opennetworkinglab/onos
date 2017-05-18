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

package org.onosproject.routing.bgp;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
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
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Prefix;
import org.onlab.packet.IpPrefix;
import org.onosproject.cluster.ClusterService;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteAdminService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Dictionary;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.onlab.util.Tools.groupedThreads;

/**
 * BGP Session Manager class.
 */
@Component(immediate = true, enabled = false)
@Service
public class BgpSessionManager implements BgpInfoService {
    private static final Logger log =
            LoggerFactory.getLogger(BgpSessionManager.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouteAdminService routeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    boolean isShutdown = true;
    private Channel serverChannel;     // Listener for incoming BGP connections
    private ServerBootstrap serverBootstrap;
    private ChannelGroup allChannels = new DefaultChannelGroup();
    private ConcurrentMap<SocketAddress, BgpSession> bgpSessions =
            new ConcurrentHashMap<>();
    private Ip4Address myBgpId;        // Same BGP ID for all peers

    private BgpRouteSelector bgpRouteSelector;
    private ConcurrentMap<Ip4Prefix, BgpRouteEntry> bgpRoutes4 =
            new ConcurrentHashMap<>();
    private ConcurrentMap<Ip6Prefix, BgpRouteEntry> bgpRoutes6 =
            new ConcurrentHashMap<>();

    private static final int DEFAULT_BGP_PORT = 2000;
    private int bgpPort;

    @Activate
    protected void activate(ComponentContext context) {
        bgpRouteSelector = new BgpRouteSelector(this, clusterService);
        readComponentConfiguration(context);
        start();
        log.info("BgpSessionManager started");
    }

    @Deactivate
    protected void deactivate() {
        stop();
        log.info("BgpSessionManager stopped");
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        try {
            String strPort = (String) properties.get("bgpPort");
            if (strPort != null) {
                bgpPort = Integer.parseInt(strPort);
            } else {
                bgpPort = DEFAULT_BGP_PORT;
            }
        } catch (NumberFormatException | ClassCastException e) {
            bgpPort = DEFAULT_BGP_PORT;
        }
        log.debug("BGP port is set to {}", bgpPort);
    }

    @Modified
    public void modified(ComponentContext context) {
        // Blank @Modified method to catch modifications to the context.
        // If no @Modified method exists, it seems @Activate is called again
        // when the context is modified.
    }

    /**
     * Checks whether the BGP Session Manager is shutdown.
     *
     * @return true if the BGP Session Manager is shutdown, otherwise false
     */
    boolean isShutdown() {
        return this.isShutdown;
    }

    /**
     * Gets the BGP sessions.
     *
     * @return the BGP sessions
     */
    @Override
    public Collection<BgpSession> getBgpSessions() {
        return bgpSessions.values();
    }

    /**
     * Gets the selected IPv4 BGP routes among all BGP sessions.
     *
     * @return the selected IPv4 BGP routes among all BGP sessions
     */
    @Override
    public Collection<BgpRouteEntry> getBgpRoutes4() {
        return bgpRoutes4.values();
    }

    /**
     * Gets the selected IPv6 BGP routes among all BGP sessions.
     *
     * @return the selected IPv6 BGP routes among all BGP sessions
     */
    @Override
    public Collection<BgpRouteEntry> getBgpRoutes6() {
        return bgpRoutes6.values();
    }

    /**
     * Finds a BGP route for a prefix. The prefix can be either IPv4 or IPv6.
     *
     * @param prefix the prefix to use
     * @return the BGP route if found, otherwise null
     */
    BgpRouteEntry findBgpRoute(IpPrefix prefix) {
        if (prefix.isIp4()) {
            return bgpRoutes4.get(prefix.getIp4Prefix());               // IPv4
        }
        return bgpRoutes6.get(prefix.getIp6Prefix());                   // IPv6
    }

    /**
     * Adds a BGP route. The route can be either IPv4 or IPv6.
     *
     * @param bgpRouteEntry the BGP route entry to use
     */
    void addBgpRoute(BgpRouteEntry bgpRouteEntry) {
        if (bgpRouteEntry.isIp4()) {
            bgpRoutes4.put(bgpRouteEntry.prefix().getIp4Prefix(),       // IPv4
                           bgpRouteEntry);
        } else {
            bgpRoutes6.put(bgpRouteEntry.prefix().getIp6Prefix(),       // IPv6
                           bgpRouteEntry);
        }
    }

    /**
     * Removes a BGP route for a prefix. The prefix can be either IPv4 or IPv6.
     *
     * @param prefix the prefix to use
     * @return true if the route was found and removed, otherwise false
     */
    boolean removeBgpRoute(IpPrefix prefix) {
        if (prefix.isIp4()) {
            return (bgpRoutes4.remove(prefix.getIp4Prefix()) != null);  // IPv4
        }
        return (bgpRoutes6.remove(prefix.getIp6Prefix()) != null);      // IPv6
    }

    /**
     * Adds the channel for a BGP session.
     *
     * @param channel the channel to add
     */
    void addSessionChannel(Channel channel) {
        allChannels.add(channel);
    }

    /**
     * Removes the channel for a BGP session.
     *
     * @param channel the channel to remove
     */
    void removeSessionChannel(Channel channel) {
        allChannels.remove(channel);
    }

    /**
     * Processes the connection from a BGP peer.
     *
     * @param bgpSession the BGP session for the peer
     * @return true if the connection can be established, otherwise false
     */
    boolean peerConnected(BgpSession bgpSession) {

        // Test whether there is already a session from the same remote
        if (bgpSessions.get(bgpSession.remoteInfo().address()) != null) {
            return false;               // Duplicate BGP session
        }
        bgpSessions.put(bgpSession.remoteInfo().address(), bgpSession);

        //
        // If the first connection, set my BGP ID to the local address
        // of the socket.
        //
        if (bgpSession.localInfo().address() instanceof InetSocketAddress) {
            InetAddress inetAddr =
                ((InetSocketAddress) bgpSession.localInfo().address()).getAddress();
            Ip4Address ip4Address = Ip4Address.valueOf(inetAddr.getAddress());
            updateMyBgpId(ip4Address);
        }
        return true;
    }

    /**
     * Processes the disconnection from a BGP peer.
     *
     * @param bgpSession the BGP session for the peer
     */
    void peerDisconnected(BgpSession bgpSession) {
        bgpSessions.remove(bgpSession.remoteInfo().address());
    }

    /**
     * Conditionally updates the local BGP ID if it wasn't set already.
     * <p/>
     * NOTE: A BGP instance should use same BGP ID across all BGP sessions.
     *
     * @param ip4Address the IPv4 address to use as BGP ID
     */
    private synchronized void updateMyBgpId(Ip4Address ip4Address) {
        if (myBgpId == null) {
            myBgpId = ip4Address;
            log.debug("BGP: My BGP ID is {}", myBgpId);
        }
    }

    /**
     * Gets the local BGP Identifier as an IPv4 address.
     *
     * @return the local BGP Identifier as an IPv4 address
     */
    Ip4Address getMyBgpId() {
        return myBgpId;
    }

    /**
     * Gets the BGP Route Selector.
     *
     * @return the BGP Route Selector
     */
    BgpRouteSelector getBgpRouteSelector() {
        return bgpRouteSelector;
    }

    /**
     * Sends updates routes to the route service.
     *
     * @param updates routes to update
     */
    void update(Collection<Route> updates) {
        routeService.update(updates);
    }

    /**
     * Sends withdrawn routes to the routes service.
     *
     * @param withdraws routes to withdraw
     */
    void withdraw(Collection<Route> withdraws) {
        routeService.withdraw(withdraws);
    }


    public void start() {
        log.debug("BGP Session Manager start.");
        isShutdown = false;

        ChannelFactory channelFactory = new NioServerSocketChannelFactory(
                newCachedThreadPool(groupedThreads("onos/bgp", "sm-boss-%d", log)),
                newCachedThreadPool(groupedThreads("onos/bgp", "sm-worker-%d", log)));
        ChannelPipelineFactory pipelineFactory = () -> {
            // Allocate a new session per connection
            BgpSession bgpSessionHandler =
                    new BgpSession(BgpSessionManager.this);
            BgpFrameDecoder bgpFrameDecoder =
                    new BgpFrameDecoder(bgpSessionHandler);

            // Setup the processing pipeline
            ChannelPipeline pipeline = Channels.pipeline();
            pipeline.addLast("BgpFrameDecoder", bgpFrameDecoder);
            pipeline.addLast("BgpSession", bgpSessionHandler);
            return pipeline;
        };
        InetSocketAddress listenAddress =
                new InetSocketAddress(bgpPort);

        serverBootstrap = new ServerBootstrap(channelFactory);
        // serverBootstrap.setOptions("reuseAddr", true);
        serverBootstrap.setOption("child.keepAlive", true);
        serverBootstrap.setOption("child.tcpNoDelay", true);
        serverBootstrap.setPipelineFactory(pipelineFactory);
        try {
            serverChannel = serverBootstrap.bind(listenAddress);
            allChannels.add(serverChannel);
        } catch (ChannelException e) {
            log.debug("Exception binding to BGP port {}: ",
                      listenAddress.getPort(), e);
        }
    }

    public void stop() {
        isShutdown = true;
        allChannels.close().awaitUninterruptibly();
        serverBootstrap.releaseExternalResources();
    }
}
