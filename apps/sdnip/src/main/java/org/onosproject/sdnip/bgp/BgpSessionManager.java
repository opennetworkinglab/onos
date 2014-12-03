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
import java.util.LinkedList;
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
import org.onosproject.sdnip.RouteListener;
import org.onosproject.sdnip.RouteUpdate;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
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

    private BgpRouteSelector bgpRouteSelector = new BgpRouteSelector();
    private ConcurrentMap<Ip4Prefix, BgpRouteEntry> bgpRoutes =
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
     * Gets the BGP sessions.
     *
     * @return the BGP sessions
     */
    public Collection<BgpSession> getBgpSessions() {
        return bgpSessions.values();
    }

    /**
     * Gets the BGP routes.
     *
     * @return the BGP routes
     */
    public Collection<BgpRouteEntry> getBgpRoutes() {
        return bgpRoutes.values();
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

        ChannelFactory channelFactory =
            new NioServerSocketChannelFactory(Executors.newCachedThreadPool(namedThreads("BGP-SM-boss-%d")),
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

    /**
     * Class to receive and process the BGP routes from each BGP Session/Peer.
     */
    class BgpRouteSelector {
        /**
         * Processes route entry updates: added/updated and deleted route
         * entries.
         *
         * @param bgpSession the BGP session the route entry updates were
         * received on
         * @param addedBgpRouteEntries the added/updated route entries to
         * process
         * @param deletedBgpRouteEntries the deleted route entries to process
         */
        synchronized void routeUpdates(BgpSession bgpSession,
                        Collection<BgpRouteEntry> addedBgpRouteEntries,
                        Collection<BgpRouteEntry> deletedBgpRouteEntries) {
            Collection<RouteUpdate> routeUpdates = new LinkedList<>();
            RouteUpdate routeUpdate;

            if (isShutdown) {
                return;         // Ignore any leftover updates if shutdown
            }
            // Process the deleted route entries
            for (BgpRouteEntry bgpRouteEntry : deletedBgpRouteEntries) {
                routeUpdate = processDeletedRoute(bgpSession, bgpRouteEntry);
                if (routeUpdate != null) {
                    routeUpdates.add(routeUpdate);
                }
            }

            // Process the added/updated route entries
            for (BgpRouteEntry bgpRouteEntry : addedBgpRouteEntries) {
                routeUpdate = processAddedRoute(bgpSession, bgpRouteEntry);
                if (routeUpdate != null) {
                    routeUpdates.add(routeUpdate);
                }
            }
            routeListener.update(routeUpdates);
        }

        /**
         * Processes an added/updated route entry.
         *
         * @param bgpSession the BGP session the route entry update was
         * received on
         * @param bgpRouteEntry the added/updated route entry
         * @return the result route update that should be forwarded to the
         * Route Listener, or null if no route update should be forwarded
         */
        private RouteUpdate processAddedRoute(BgpSession bgpSession,
                                              BgpRouteEntry bgpRouteEntry) {
            RouteUpdate routeUpdate;
            BgpRouteEntry bestBgpRouteEntry =
                bgpRoutes.get(bgpRouteEntry.prefix());

            //
            // Install the new route entry if it is better than the
            // current best route.
            //
            if ((bestBgpRouteEntry == null) ||
                bgpRouteEntry.isBetterThan(bestBgpRouteEntry)) {
                bgpRoutes.put(bgpRouteEntry.prefix(), bgpRouteEntry);
                routeUpdate =
                    new RouteUpdate(RouteUpdate.Type.UPDATE, bgpRouteEntry);
                return routeUpdate;
            }

            //
            // If the route entry arrived on the same BGP Session as
            // the current best route, then elect the next best route
            // and install it.
            //
            if (bestBgpRouteEntry.getBgpSession() !=
                bgpRouteEntry.getBgpSession()) {
                return null;            // Nothing to do
            }

            // Find the next best route
            bestBgpRouteEntry = findBestBgpRoute(bgpRouteEntry.prefix());
            if (bestBgpRouteEntry == null) {
                //
                // TODO: Shouldn't happen. Install the new route as a
                // pre-caution.
                //
                log.debug("BGP next best route for prefix {} is missing. " +
                          "Adding the route that is currently processed.",
                          bgpRouteEntry.prefix());
                bestBgpRouteEntry = bgpRouteEntry;
            }
            // Install the next best route
            bgpRoutes.put(bestBgpRouteEntry.prefix(), bestBgpRouteEntry);
            routeUpdate = new RouteUpdate(RouteUpdate.Type.UPDATE,
                                          bestBgpRouteEntry);
            return routeUpdate;
        }

        /**
         * Processes a deleted route entry.
         *
         * @param bgpSession the BGP session the route entry update was
         * received on
         * @param bgpRouteEntry the deleted route entry
         * @return the result route update that should be forwarded to the
         * Route Listener, or null if no route update should be forwarded
         */
        private RouteUpdate processDeletedRoute(BgpSession bgpSession,
                                                BgpRouteEntry bgpRouteEntry) {
            RouteUpdate routeUpdate;
            BgpRouteEntry bestBgpRouteEntry =
                bgpRoutes.get(bgpRouteEntry.prefix());

            //
            // Remove the route entry only if it was the best one.
            // Install the the next best route if it exists.
            //
            // NOTE: We intentionally use "==" instead of method equals(),
            // because we need to check whether this is same object.
            //
            if (bgpRouteEntry != bestBgpRouteEntry) {
                return null;            // Nothing to do
            }

            //
            // Find the next best route
            //
            bestBgpRouteEntry = findBestBgpRoute(bgpRouteEntry.prefix());
            if (bestBgpRouteEntry != null) {
                // Install the next best route
                bgpRoutes.put(bestBgpRouteEntry.prefix(),
                              bestBgpRouteEntry);
                routeUpdate = new RouteUpdate(RouteUpdate.Type.UPDATE,
                                              bestBgpRouteEntry);
                return routeUpdate;
            }

            //
            // No route found. Remove the route entry
            //
            bgpRoutes.remove(bgpRouteEntry.prefix());
            routeUpdate = new RouteUpdate(RouteUpdate.Type.DELETE,
                                          bgpRouteEntry);
            return routeUpdate;
        }

        /**
         * Finds the best route entry among all BGP Sessions.
         *
         * @param prefix the prefix of the route
         * @return the best route if found, otherwise null
         */
        private BgpRouteEntry findBestBgpRoute(Ip4Prefix prefix) {
            BgpRouteEntry bestRoute = null;

            // Iterate across all BGP Sessions and select the best route
            for (BgpSession bgpSession : bgpSessions.values()) {
                BgpRouteEntry route = bgpSession.findBgpRouteEntry(prefix);
                if (route == null) {
                    continue;
                }
                if ((bestRoute == null) || route.isBetterThan(bestRoute)) {
                    bestRoute = route;
                }
            }
            return bestRoute;
        }
    }
}
