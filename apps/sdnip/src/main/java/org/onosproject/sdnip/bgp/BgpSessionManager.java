/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.sdnip.bgp;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.namedThreads;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

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
import org.onlab.packet.IpPrefix;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Prefix;
import org.onosproject.sdnip.RouteListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BGP Session Manager class.
 */
public class BgpSessionManager {
    private static final Logger log =
        LoggerFactory.getLogger(BgpSessionManager.class);

    boolean isShutdown = true;
    private Channel serverChannel;     // Listener for incoming BGP connections
    private ServerBootstrap serverBootstrap;
    private ChannelGroup allChannels = new DefaultChannelGroup();
    private ConcurrentMap<SocketAddress, BgpSession> bgpSessions =
        new ConcurrentHashMap<>();
    private Ip4Address myBgpId;        // Same BGP ID for all peers

    private BgpRouteSelector bgpRouteSelector = new BgpRouteSelector(this);
    private ConcurrentMap<Ip4Prefix, BgpRouteEntry> bgpRoutes4 =
        new ConcurrentHashMap<>();
    private ConcurrentMap<Ip6Prefix, BgpRouteEntry> bgpRoutes6 =
        new ConcurrentHashMap<>();

    private final RouteListener routeListener;

    /**
     * Constructor for given route listener.
     *
     * @param routeListener the route listener to use
     */
    public BgpSessionManager(RouteListener routeListener) {
        this.routeListener = checkNotNull(routeListener);
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
     * Gets the route listener.
     *
     * @return the route listener to use
     */
    RouteListener getRouteListener() {
        return routeListener;
    }

    /**
     * Gets the BGP sessions.
     *
     * @return the BGP sessions
     */
    public Collection<BgpSession> getBgpSessions() {
        return bgpSessions.values();
    }

    /**
     * Gets the selected IPv4 BGP routes among all BGP sessions.
     *
     * @return the selected IPv4 BGP routes among all BGP sessions
     */
    public Collection<BgpRouteEntry> getBgpRoutes4() {
        return bgpRoutes4.values();
    }

    /**
     * Gets the selected IPv6 BGP routes among all BGP sessions.
     *
     * @return the selected IPv6 BGP routes among all BGP sessions
     */
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
        if (prefix.version() == Ip4Address.VERSION) {
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
        if (bgpRouteEntry.version() == Ip4Address.VERSION) {
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
        if (prefix.version() == Ip4Address.VERSION) {
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
        if (bgpSessions.get(bgpSession.getRemoteAddress()) != null) {
            return false;               // Duplicate BGP session
        }
        bgpSessions.put(bgpSession.getRemoteAddress(), bgpSession);

        //
        // If the first connection, set my BGP ID to the local address
        // of the socket.
        //
        if (bgpSession.getLocalAddress() instanceof InetSocketAddress) {
            InetAddress inetAddr =
                ((InetSocketAddress) bgpSession.getLocalAddress()).getAddress();
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
        bgpSessions.remove(bgpSession.getRemoteAddress());
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
     * Starts up BGP Session Manager operation.
     *
     * @param listenPortNumber the port number to listen on. By default
     * it should be BgpConstants.BGP_PORT (179)
     */
    public void start(int listenPortNumber) {
        log.debug("BGP Session Manager start.");
        isShutdown = false;

        ChannelFactory channelFactory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(namedThreads("BGP-SM-boss-%d")),
                Executors.newCachedThreadPool(namedThreads("BGP-SM-worker-%d")));
        ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory() {
                @Override
                public ChannelPipeline getPipeline() throws Exception {
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
                }
            };
        InetSocketAddress listenAddress =
            new InetSocketAddress(listenPortNumber);

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

    /**
     * Stops the BGP Session Manager operation.
     */
    public void stop() {
        isShutdown = true;
        allChannels.close().awaitUninterruptibly();
        serverBootstrap.releaseExternalResources();
    }
}
