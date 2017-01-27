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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Prefix;
import org.onlab.packet.IpPrefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onlab.util.Tools.groupedThreads;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Class for handling the BGP peer sessions.
 * There is one instance per each BGP peer session.
 */
public class BgpSession extends SimpleChannelHandler {
    private static final Logger log =
        LoggerFactory.getLogger(BgpSession.class);

    private final BgpSessionManager bgpSessionManager;

    // Local flag to indicate the session is closed.
    // It is used to avoid the Netty's asynchronous closing of a channel.
    private boolean isClosed = false;

    // BGP session info: local and remote
    private final BgpSessionInfo localInfo;     // BGP session local info
    private final BgpSessionInfo remoteInfo;    // BGP session remote info

    // Timers state
    private Timer timer = new HashedWheelTimer(groupedThreads("BgpSession", "timer-%d", log));
    private volatile Timeout keepaliveTimeout;  // Periodic KEEPALIVE
    private volatile Timeout sessionTimeout;    // Session timeout

    // BGP RIB-IN routing entries from this peer
    private ConcurrentMap<Ip4Prefix, BgpRouteEntry> bgpRibIn4 =
        new ConcurrentHashMap<>();
    private ConcurrentMap<Ip6Prefix, BgpRouteEntry> bgpRibIn6 =
        new ConcurrentHashMap<>();

    /**
     * Constructor for a given BGP Session Manager.
     *
     * @param bgpSessionManager the BGP Session Manager to use
     */
    BgpSession(BgpSessionManager bgpSessionManager) {
        this.bgpSessionManager = bgpSessionManager;
        this.localInfo = new BgpSessionInfo();
        this.remoteInfo = new BgpSessionInfo();

        // NOTE: We support only BGP4
        this.localInfo.setBgpVersion(BgpConstants.BGP_VERSION);
    }

    /**
     * Gets the BGP Session Manager.
     *
     * @return the BGP Session Manager
     */
    BgpSessionManager getBgpSessionManager() {
        return bgpSessionManager;
    }

    /**
     * Gets the BGP Session local information.
     *
     * @return the BGP Session local information.
     */
    public BgpSessionInfo localInfo() {
        return localInfo;
    }

    /**
     * Gets the BGP Session remote information.
     *
     * @return the BGP Session remote information.
     */
    public BgpSessionInfo remoteInfo() {
        return remoteInfo;
    }

    /**
     * Gets the BGP Multiprotocol Extensions for the session.
     *
     * @return true if the BGP Multiprotocol Extensions are enabled for the
     * session, otherwise false
     */
    public boolean mpExtensions() {
        return remoteInfo.mpExtensions() && localInfo.mpExtensions();
    }

    /**
     * Gets the BGP session 4 octet AS path capability.
     *
     * @return true when the BGP session is 4 octet AS path capable
     */
    public boolean isAs4OctetCapable() {
        return remoteInfo.as4OctetCapability() &&
            localInfo.as4OctetCapability();
    }

    /**
     * Gets the IPv4 BGP RIB-IN routing entries.
     *
     * @return the IPv4 BGP RIB-IN routing entries
     */
    public Collection<BgpRouteEntry> getBgpRibIn4() {
        return bgpRibIn4.values();
    }

    /**
     * Gets the IPv6 BGP RIB-IN routing entries.
     *
     * @return the IPv6 BGP RIB-IN routing entries
     */
    public Collection<BgpRouteEntry> getBgpRibIn6() {
        return bgpRibIn6.values();
    }

    /**
     * Finds an IPv4 BGP routing entry for a prefix in the IPv4 BGP RIB-IN.
     *
     * @param prefix the IPv4 prefix of the route to search for
     * @return the IPv4 BGP routing entry if found, otherwise null
     */
    public BgpRouteEntry findBgpRoute(Ip4Prefix prefix) {
        return bgpRibIn4.get(prefix);
    }

    /**
     * Finds an IPv6 BGP routing entry for a prefix in the IPv6 BGP RIB-IN.
     *
     * @param prefix the IPv6 prefix of the route to search for
     * @return the IPv6 BGP routing entry if found, otherwise null
     */
    public BgpRouteEntry findBgpRoute(Ip6Prefix prefix) {
        return bgpRibIn6.get(prefix);
    }

    /**
     * Finds a BGP routing entry for a prefix in the BGP RIB-IN. The prefix
     * can be either IPv4 or IPv6.
     *
     * @param prefix the IP prefix of the route to search for
     * @return the BGP routing entry if found, otherwise null
     */
    public BgpRouteEntry findBgpRoute(IpPrefix prefix) {
        if (prefix.isIp4()) {
            // IPv4 prefix
            Ip4Prefix ip4Prefix = prefix.getIp4Prefix();
            return bgpRibIn4.get(ip4Prefix);
        }

        // IPv6 prefix
        Ip6Prefix ip6Prefix = prefix.getIp6Prefix();
        return bgpRibIn6.get(ip6Prefix);
    }

    /**
     * Adds a BGP route. The route can be either IPv4 or IPv6.
     *
     * @param bgpRouteEntry the BGP route entry to use
     */
    void addBgpRoute(BgpRouteEntry bgpRouteEntry) {
        if (bgpRouteEntry.isIp4()) {
            // IPv4 route
            Ip4Prefix ip4Prefix = bgpRouteEntry.prefix().getIp4Prefix();
            bgpRibIn4.put(ip4Prefix, bgpRouteEntry);
        } else {
            // IPv6 route
            Ip6Prefix ip6Prefix = bgpRouteEntry.prefix().getIp6Prefix();
            bgpRibIn6.put(ip6Prefix, bgpRouteEntry);
        }
    }

    /**
     * Removes an IPv4 BGP route for a prefix.
     *
     * @param prefix the prefix to use
     * @return true if the route was found and removed, otherwise false
     */
    boolean removeBgpRoute(Ip4Prefix prefix) {
        return (bgpRibIn4.remove(prefix) != null);
    }

    /**
     * Removes an IPv6 BGP route for a prefix.
     *
     * @param prefix the prefix to use
     * @return true if the route was found and removed, otherwise false
     */
    boolean removeBgpRoute(Ip6Prefix prefix) {
        return (bgpRibIn6.remove(prefix) != null);
    }

    /**
     * Removes a BGP route for a prefix. The prefix can be either IPv4 or IPv6.
     *
     * @param prefix the prefix to use
     * @return true if the route was found and removed, otherwise false
     */
    boolean removeBgpRoute(IpPrefix prefix) {
        if (prefix.isIp4()) {
            return (bgpRibIn4.remove(prefix.getIp4Prefix()) != null);   // IPv4
        }
        return (bgpRibIn6.remove(prefix.getIp6Prefix()) != null);       // IPv6
    }

    /**
     * Tests whether the session is closed.
     * <p>
     * NOTE: We use this method to avoid the Netty's asynchronous closing
     * of a channel.
     * </p>
     * @return true if the session is closed
     */
    boolean isClosed() {
        return isClosed;
    }

    /**
     * Closes the session.
     *
     * @param ctx the Channel Handler Context
     */
    void closeSession(ChannelHandlerContext ctx) {
        timer.stop();
        closeChannel(ctx);
    }

    /**
     * Closes the Netty channel.
     *
     * @param ctx the Channel Handler Context
     */
    void closeChannel(ChannelHandlerContext ctx) {
        isClosed = true;
        ctx.getChannel().close();
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx,
                            ChannelStateEvent channelEvent) {
        bgpSessionManager.addSessionChannel(channelEvent.getChannel());
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx,
                              ChannelStateEvent channelEvent) {
        bgpSessionManager.removeSessionChannel(channelEvent.getChannel());
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx,
                                 ChannelStateEvent channelEvent) {
        localInfo.setAddress(ctx.getChannel().getLocalAddress());
        remoteInfo.setAddress(ctx.getChannel().getRemoteAddress());

        // Assign the local and remote IPv4 addresses
        InetAddress inetAddr;
        if (localInfo.address() instanceof InetSocketAddress) {
            inetAddr = ((InetSocketAddress) localInfo.address()).getAddress();
            localInfo.setIp4Address(Ip4Address.valueOf(inetAddr.getAddress()));
        }
        if (remoteInfo.address() instanceof InetSocketAddress) {
            inetAddr = ((InetSocketAddress) remoteInfo.address()).getAddress();
            remoteInfo.setIp4Address(Ip4Address.valueOf(inetAddr.getAddress()));
        }

        log.debug("BGP Session Connected from {} on {}",
                  remoteInfo.address(), localInfo.address());
        if (!bgpSessionManager.peerConnected(this)) {
            log.debug("Cannot setup BGP Session Connection from {}. Closing...",
                      remoteInfo.address());
            ctx.getChannel().close();
        }

        //
        // Assign the local BGP ID
        // NOTE: This should be configuration-based
        //
        localInfo.setBgpId(bgpSessionManager.getMyBgpId());
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx,
                                    ChannelStateEvent channelEvent) {
        log.debug("BGP Session Disconnected from {} on {}",
                  ctx.getChannel().getRemoteAddress(),
                  ctx.getChannel().getLocalAddress());
        processChannelDisconnected();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        log.debug("BGP Session Exception Caught from {} on {}: {}",
                  ctx.getChannel().getRemoteAddress(),
                  ctx.getChannel().getLocalAddress(),
                  e);
        log.debug("Exception:", e.getCause());
        processChannelDisconnected();
    }

    /**
     * Processes the channel being disconnected.
     */
    private void processChannelDisconnected() {
        //
        // Withdraw the routes advertised by this BGP peer
        //
        // NOTE: We must initialize the RIB-IN before propagating the withdraws
        // for further processing. Otherwise, the BGP Decision Process
        // will use those routes again.
        //
        Collection<BgpRouteEntry> deletedRoutes4 = bgpRibIn4.values();
        Collection<BgpRouteEntry> deletedRoutes6 = bgpRibIn6.values();
        bgpRibIn4 = new ConcurrentHashMap<>();
        bgpRibIn6 = new ConcurrentHashMap<>();

        // Push the updates to the BGP Merged RIB
        BgpRouteSelector bgpRouteSelector =
            bgpSessionManager.getBgpRouteSelector();
        Collection<BgpRouteEntry> addedRoutes = Collections.emptyList();
        bgpRouteSelector.routeUpdates(addedRoutes, deletedRoutes4);
        bgpRouteSelector.routeUpdates(addedRoutes, deletedRoutes6);

        bgpSessionManager.peerDisconnected(this);
    }

    /**
     * Restarts the BGP KeepaliveTimer.
     *
     * @param ctx the Channel Handler Context to use
     */
    void restartKeepaliveTimer(ChannelHandlerContext ctx) {
        long localKeepaliveInterval = 0;

        //
        // Compute the local Keepalive interval
        //
        if (localInfo.holdtime() != 0) {
            localKeepaliveInterval = Math.max(localInfo.holdtime() /
                         BgpConstants.BGP_KEEPALIVE_PER_HOLD_INTERVAL,
                         BgpConstants.BGP_KEEPALIVE_MIN_INTERVAL);
        }

        // Restart the Keepalive timer
        if (localKeepaliveInterval == 0) {
            return;                 // Nothing to do
        }
        keepaliveTimeout = timer.newTimeout(new TransmitKeepaliveTask(ctx),
                                            localKeepaliveInterval,
                                            TimeUnit.SECONDS);
    }

    /**
     * Task class for transmitting KEEPALIVE messages.
     */
    private final class TransmitKeepaliveTask implements TimerTask {
        private final ChannelHandlerContext ctx;

        /**
         * Constructor for given Channel Handler Context.
         *
         * @param ctx the Channel Handler Context to use
         */
        TransmitKeepaliveTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            if (timeout.isCancelled()) {
                return;
            }
            if (!ctx.getChannel().isOpen()) {
                return;
            }

            // Transmit the KEEPALIVE
            ChannelBuffer txMessage = BgpKeepalive.prepareBgpKeepalive();
            ctx.getChannel().write(txMessage);

            // Restart the KEEPALIVE timer
            restartKeepaliveTimer(ctx);
        }
    }

    /**
     * Restarts the BGP Session Timeout Timer.
     *
     * @param ctx the Channel Handler Context to use
     */
    void restartSessionTimeoutTimer(ChannelHandlerContext ctx) {
        if (remoteInfo.holdtime() == 0) {
            return;                 // Nothing to do
        }
        if (sessionTimeout != null) {
            sessionTimeout.cancel();
        }
        sessionTimeout = timer.newTimeout(new SessionTimeoutTask(ctx),
                                          remoteInfo.holdtime(),
                                          TimeUnit.SECONDS);
    }

    /**
     * Task class for BGP Session timeout.
     */
    private final class SessionTimeoutTask implements TimerTask {
        private final ChannelHandlerContext ctx;

        /**
         * Constructor for given Channel Handler Context.
         *
         * @param ctx the Channel Handler Context to use
         */
        SessionTimeoutTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            if (timeout.isCancelled()) {
                return;
            }
            if (!ctx.getChannel().isOpen()) {
                return;
            }

            log.debug("BGP Session Timeout: peer {}", remoteInfo.address());
            //
            // ERROR: Invalid Optional Parameter Length field: Unspecific
            //
            // Send NOTIFICATION and close the connection
            int errorCode = BgpConstants.Notifications.HoldTimerExpired.ERROR_CODE;
            int errorSubcode = BgpConstants.Notifications.ERROR_SUBCODE_UNSPECIFIC;
            ChannelBuffer txMessage =
                BgpNotification.prepareBgpNotification(errorCode, errorSubcode,
                                                       null);
            ctx.getChannel().write(txMessage);
            closeChannel(ctx);
        }
    }
}
