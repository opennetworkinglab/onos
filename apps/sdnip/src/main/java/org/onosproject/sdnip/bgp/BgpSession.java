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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

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
import org.onosproject.sdnip.bgp.BgpConstants.Notifications;
import org.onosproject.sdnip.bgp.BgpConstants.Notifications.HoldTimerExpired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private SocketAddress remoteAddress;        // Peer IP addr/port
    private Ip4Address remoteIp4Address;        // Peer IPv4 address
    private int remoteBgpVersion;               // 1 octet
    private long remoteAs;                      // 2 octets
    private long remoteAs4Octet;                // 4 octets
    private long remoteHoldtime;                // 2 octets
    private Ip4Address remoteBgpId;             // 4 octets -> IPv4 address
    private boolean remoteIpv4Unicast;          // Peer IPv4/UNICAST AFI/SAFI
    private boolean remoteIpv4Multicast;        // Peer IPv4/MULTICAST AFI/SAFI
    private boolean remoteIpv6Unicast;          // Peer IPv6/UNICAST AFI/SAFI
    private boolean remoteIpv6Multicast;        // Peer IPv6/MULTICAST AFI/SAFI
    //
    private SocketAddress localAddress;         // Local IP addr/port
    private Ip4Address localIp4Address;         // Local IPv4 address
    private int localBgpVersion;                // 1 octet
    private long localAs;                       // 2 octets
    private long localHoldtime;                 // 2 octets
    private Ip4Address localBgpId;              // 4 octets -> IPv4 address
    private boolean localIpv4Unicast;        // Local IPv4/UNICAST AFI/SAFI
    private boolean localIpv4Multicast;      // Local IPv4/MULTICAST AFI/SAFI
    private boolean localIpv6Unicast;        // Local IPv6/UNICAST AFI/SAFI
    private boolean localIpv6Multicast;      // Local IPv6/MULTICAST AFI/SAFI
    //
    private long localKeepaliveInterval;        // Keepalive interval

    // Timers state
    private Timer timer = new HashedWheelTimer();
    private volatile Timeout keepaliveTimeout;  // Periodic KEEPALIVE
    private volatile Timeout sessionTimeout;    // Session timeout

    // BGP RIB-IN routing entries from this peer
    private ConcurrentMap<Ip4Prefix, BgpRouteEntry> bgpRibIn =
        new ConcurrentHashMap<>();

    /**
     * Constructor for a given BGP Session Manager.
     *
     * @param bgpSessionManager the BGP Session Manager to use
     */
    BgpSession(BgpSessionManager bgpSessionManager) {
        this.bgpSessionManager = bgpSessionManager;

        // NOTE: We support only BGP4
        this.localBgpVersion = BgpConstants.BGP_VERSION;
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
     * Gets the BGP RIB-IN routing entries.
     *
     * @return the BGP RIB-IN routing entries
     */
    public Map<Ip4Prefix, BgpRouteEntry> bgpRibIn() {
        return bgpRibIn;
    }

    /**
     * Finds a BGP routing entry in the BGP RIB-IN.
     *
     * @param prefix the prefix of the route to search for
     * @return the BGP routing entry if found, otherwise null
     */
    public BgpRouteEntry findBgpRouteEntry(Ip4Prefix prefix) {
        return bgpRibIn.get(prefix);
    }

    /**
     * Gets the BGP session remote address.
     *
     * @return the BGP session remote address
     */
    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Gets the BGP session remote IPv4 address.
     *
     * @return the BGP session remote IPv4 address
     */
    public Ip4Address getRemoteIp4Address() {
        return remoteIp4Address;
    }

    /**
     * Gets the BGP session remote BGP version.
     *
     * @return the BGP session remote BGP version
     */
    public int getRemoteBgpVersion() {
        return remoteBgpVersion;
    }

    /**
     * Sets the BGP session remote BGP version.
     *
     * @param remoteBgpVersion the BGP session remote BGP version to set
     */
     void setRemoteBgpVersion(int remoteBgpVersion) {
         this.remoteBgpVersion = remoteBgpVersion;
     }

    /**
     * Gets the BGP session remote AS number.
     *
     * @return the BGP session remote AS number
     */
    public long getRemoteAs() {
        return remoteAs;
    }

    /**
     * Sets the BGP session remote AS number.
     *
     * @param remoteAs the BGP session remote AS number to set
     */
    void setRemoteAs(long remoteAs) {
        this.remoteAs = remoteAs;

        //
        // NOTE: Currently, the local AS number is always set to the remote AS.
        // This is done, because the peer setup is always iBGP.
        // In the future the local AS number should be configured as part
        // of an explicit BGP peering configuration.
        //
        this.localAs = remoteAs;
    }

    /**
     * Sets the BGP session remote 4 octet AS number.
     *
     * @param remoteAs4Octet the BGP session remote 4 octet AS number to set
     */
    void setRemoteAs4Octet(long remoteAs4Octet) {
        this.remoteAs4Octet = remoteAs4Octet;
    }

    /**
     * Gets the BGP session remote Holdtime.
     *
     * @return the BGP session remote Holdtime
     */
    public long getRemoteHoldtime() {
        return remoteHoldtime;
    }

    /**
     * Sets the BGP session remote Holdtime.
     *
     * @param remoteHoldtime the BGP session remote Holdtime to set
     */
    void setRemoteHoldtime(long remoteHoldtime) {
        this.remoteHoldtime = remoteHoldtime;

        //
        // NOTE: Currently. the local BGP Holdtime is always set to the remote
        // BGP holdtime.
        // In the future, the local BGP Holdtime should be configured as part
        // of an explicit BGP peering configuration.
        //
        this.localHoldtime = remoteHoldtime;

        // Set the local Keepalive interval
        if (localHoldtime == 0) {
            localKeepaliveInterval = 0;
        } else {
            localKeepaliveInterval = Math.max(localHoldtime /
                         BgpConstants.BGP_KEEPALIVE_PER_HOLD_INTERVAL,
                         BgpConstants.BGP_KEEPALIVE_MIN_INTERVAL);
        }
    }

    /**
     * Gets the BGP session remote BGP Identifier as an IPv4 address.
     *
     * @return the BGP session remote BGP Identifier as an IPv4 address
     */
    public Ip4Address getRemoteBgpId() {
        return remoteBgpId;
    }

    /**
     * Sets the BGP session remote BGP Identifier as an IPv4 address.
     *
     * @param remoteBgpId the BGP session remote BGP Identifier to set
     */
    void setRemoteBgpId(Ip4Address remoteBgpId) {
        this.remoteBgpId = remoteBgpId;
    }

    /**
     * Gets the BGP session remote AFI/SAFI configuration for IPv4 unicast.
     *
     * @return the BGP session remote AFI/SAFI configuration for IPv4 unicast
     */
    public boolean getRemoteIpv4Unicast() {
        return remoteIpv4Unicast;
    }

    /**
     * Sets the BGP session remote AFI/SAFI configuration for IPv4 unicast.
     */
    void setRemoteIpv4Unicast() {
        this.remoteIpv4Unicast = true;
        // Copy the remote AFI/SAFI setting to the local configuration
        // NOTE: Uncomment the line below if the AFI/SAFI is supported locally
        // this.localIpv4Unicast = true;
    }

    /**
     * Gets the BGP session remote AFI/SAFI configuration for IPv4 multicast.
     *
     * @return the BGP session remote AFI/SAFI configuration for IPv4 multicast
     */
    public boolean getRemoteIpv4Multicast() {
        return remoteIpv4Multicast;
    }

    /**
     * Sets the BGP session remote AFI/SAFI configuration for IPv4 multicast.
     */
    void setRemoteIpv4Multicast() {
        this.remoteIpv4Multicast = true;
        // Copy the remote AFI/SAFI setting to the local configuration
        // NOTE: Uncomment the line below if the AFI/SAFI is supported locally
        // this.localIpv4Multicast = true;
    }

    /**
     * Gets the BGP session remote AFI/SAFI configuration for IPv6 unicast.
     *
     * @return the BGP session remote AFI/SAFI configuration for IPv6 unicast
     */
    public boolean getRemoteIpv6Unicast() {
        return remoteIpv6Unicast;
    }

    /**
     * Sets the BGP session remote AFI/SAFI configuration for IPv6 unicast.
     */
    void setRemoteIpv6Unicast() {
        this.remoteIpv6Unicast = true;
        // Copy the remote AFI/SAFI setting to the local configuration
        // NOTE: Uncomment the line below if the AFI/SAFI is supported locally
        // this.localIpv6Unicast = true;
    }

    /**
     * Gets the BGP session remote AFI/SAFI configuration for IPv6 multicast.
     *
     * @return the BGP session remote AFI/SAFI configuration for IPv6 multicast
     */
    public boolean getRemoteIpv6Multicast() {
        return remoteIpv6Multicast;
    }

    /**
     * Sets the BGP session remote AFI/SAFI configuration for IPv6 multicast.
     */
    void setRemoteIpv6Multicast() {
        this.remoteIpv6Multicast = true;
        // Copy the remote AFI/SAFI setting to the local configuration
        // NOTE: Uncomment the line below if the AFI/SAFI is supported locally
        // this.localIpv6Multicast = true;
    }

    /**
     * Gets the BGP session local address.
     *
     * @return the BGP session local address
     */
    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    /**
     * Gets the BGP session local IPv4 address.
     *
     * @return the BGP session local IPv4 address
     */
    public Ip4Address getLocalIp4Address() {
        return localIp4Address;
    }

    /**
     * Gets the BGP session local BGP version.
     *
     * @return the BGP session local BGP version
     */
    public int getLocalBgpVersion() {
        return localBgpVersion;
    }

    /**
     * Gets the BGP session local AS number.
     *
     * @return the BGP session local AS number
     */
    public long getLocalAs() {
        return localAs;
    }

    /**
     * Gets the BGP session local Holdtime.
     *
     * @return the BGP session local Holdtime
     */
    public long getLocalHoldtime() {
        return localHoldtime;
    }

    /**
     * Gets the BGP session local BGP Identifier as an IPv4 address.
     *
     * @return the BGP session local BGP Identifier as an IPv4 address
     */
    public Ip4Address getLocalBgpId() {
        return localBgpId;
    }

    /**
     * Gets the BGP session local AFI/SAFI configuration for IPv4 unicast.
     *
     * @return the BGP session local AFI/SAFI configuration for IPv4 unicast
     */
    public boolean getLocalIpv4Unicast() {
        return localIpv4Unicast;
    }

    /**
     * Gets the BGP session local AFI/SAFI configuration for IPv4 multicast.
     *
     * @return the BGP session local AFI/SAFI configuration for IPv4 multicast
     */
    public boolean getLocalIpv4Multicast() {
        return localIpv4Multicast;
    }

    /**
     * Gets the BGP session local AFI/SAFI configuration for IPv6 unicast.
     *
     * @return the BGP session local AFI/SAFI configuration for IPv6 unicast
     */
    public boolean getLocalIpv6Unicast() {
        return localIpv6Unicast;
    }

    /**
     * Gets the BGP session local AFI/SAFI configuration for IPv6 multicast.
     *
     * @return the BGP session local AFI/SAFI configuration for IPv6 multicast
     */
    public boolean getLocalIpv6Multicast() {
        return localIpv6Multicast;
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
        localAddress = ctx.getChannel().getLocalAddress();
        remoteAddress = ctx.getChannel().getRemoteAddress();

        // Assign the local and remote IPv4 addresses
        InetAddress inetAddr;
        if (localAddress instanceof InetSocketAddress) {
            inetAddr = ((InetSocketAddress) localAddress).getAddress();
            localIp4Address = Ip4Address.valueOf(inetAddr.getAddress());
        }
        if (remoteAddress instanceof InetSocketAddress) {
            inetAddr = ((InetSocketAddress) remoteAddress).getAddress();
            remoteIp4Address = Ip4Address.valueOf(inetAddr.getAddress());
        }

        log.debug("BGP Session Connected from {} on {}",
                  remoteAddress, localAddress);
        if (!bgpSessionManager.peerConnected(this)) {
            log.debug("Cannot setup BGP Session Connection from {}. Closing...",
                      remoteAddress);
            ctx.getChannel().close();
        }

        //
        // Assign the local BGP ID
        // NOTE: This should be configuration-based
        //
        localBgpId = bgpSessionManager.getMyBgpId();
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
        Collection<BgpRouteEntry> deletedRoutes = bgpRibIn.values();
        bgpRibIn = new ConcurrentHashMap<>();

        // Push the updates to the BGP Merged RIB
        BgpSessionManager.BgpRouteSelector bgpRouteSelector =
            bgpSessionManager.getBgpRouteSelector();
        Collection<BgpRouteEntry> addedRoutes = Collections.emptyList();
        bgpRouteSelector.routeUpdates(this, addedRoutes, deletedRoutes);

        bgpSessionManager.peerDisconnected(this);
    }

    /**
     * Restarts the BGP KeepaliveTimer.
     */
    void restartKeepaliveTimer(ChannelHandlerContext ctx) {
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
     */
    void restartSessionTimeoutTimer(ChannelHandlerContext ctx) {
        if (remoteHoldtime == 0) {
            return;                 // Nothing to do
        }
        if (sessionTimeout != null) {
            sessionTimeout.cancel();
        }
        sessionTimeout = timer.newTimeout(new SessionTimeoutTask(ctx),
                                          remoteHoldtime,
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

            log.debug("BGP Session Timeout: peer {}", remoteAddress);
            //
            // ERROR: Invalid Optional Parameter Length field: Unspecific
            //
            // Send NOTIFICATION and close the connection
            int errorCode = HoldTimerExpired.ERROR_CODE;
            int errorSubcode = Notifications.ERROR_SUBCODE_UNSPECIFIC;
            ChannelBuffer txMessage =
                BgpNotification.prepareBgpNotification(errorCode, errorSubcode,
                                                       null);
            ctx.getChannel().write(txMessage);
            closeChannel(ctx);
        }
    }
}
