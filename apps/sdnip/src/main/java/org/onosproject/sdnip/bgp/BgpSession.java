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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
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
import org.onosproject.sdnip.bgp.BgpConstants.Notifications.MessageHeaderError;
import org.onosproject.sdnip.bgp.BgpConstants.Notifications.OpenMessageError;
import org.onosproject.sdnip.bgp.BgpConstants.Notifications.UpdateMessageError;
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
    private long remoteHoldtime;                // 2 octets
    private Ip4Address remoteBgpId;             // 4 octets -> IPv4 address
    //
    private SocketAddress localAddress;         // Local IP addr/port
    private Ip4Address localIp4Address;         // Local IPv4 address
    private int localBgpVersion;                // 1 octet
    private long localAs;                       // 2 octets
    private long localHoldtime;                 // 2 octets
    private Ip4Address localBgpId;              // 4 octets -> IPv4 address
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
    }

    /**
     * Gets the BGP RIB-IN routing entries.
     *
     * @return the BGP RIB-IN routing entries
     */
    public Collection<BgpRouteEntry> getBgpRibIn() {
        return bgpRibIn.values();
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
     * Gets the BGP session remote AS number.
     *
     * @return the BGP session remote AS number
     */
    public long getRemoteAs() {
        return remoteAs;
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
     * Gets the BGP session remote BGP Identifier as an IPv4 address.
     *
     * @return the BGP session remote BGP Identifier as an IPv4 address
     */
    public Ip4Address getRemoteBgpId() {
        return remoteBgpId;
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
     * Processes BGP OPEN message.
     *
     * @param ctx the Channel Handler Context
     * @param message the message to process
     */
    void processBgpOpen(ChannelHandlerContext ctx, ChannelBuffer message) {
        int minLength =
            BgpConstants.BGP_OPEN_MIN_LENGTH - BgpConstants.BGP_HEADER_LENGTH;
        if (message.readableBytes() < minLength) {
            log.debug("BGP RX OPEN Error from {}: " +
                      "Message length {} too short. Must be at least {}",
                      remoteAddress, message.readableBytes(), minLength);
            //
            // ERROR: Bad Message Length
            //
            // Send NOTIFICATION and close the connection
            ChannelBuffer txMessage = prepareBgpNotificationBadMessageLength(
                message.readableBytes() + BgpConstants.BGP_HEADER_LENGTH);
            ctx.getChannel().write(txMessage);
            closeSession(ctx);
            return;
        }

        //
        // Parse the OPEN message
        //
        // Remote BGP version
        remoteBgpVersion = message.readUnsignedByte();
        if (remoteBgpVersion != BgpConstants.BGP_VERSION) {
            log.debug("BGP RX OPEN Error from {}: " +
                      "Unsupported BGP version {}. Should be {}",
                      remoteAddress, remoteBgpVersion,
                      BgpConstants.BGP_VERSION);
            //
            // ERROR: Unsupported Version Number
            //
            // Send NOTIFICATION and close the connection
            int errorCode = OpenMessageError.ERROR_CODE;
            int errorSubcode = OpenMessageError.UNSUPPORTED_VERSION_NUMBER;
            ChannelBuffer data = ChannelBuffers.buffer(2);
            data.writeShort(BgpConstants.BGP_VERSION);
            ChannelBuffer txMessage =
                prepareBgpNotification(errorCode, errorSubcode, data);
            ctx.getChannel().write(txMessage);
            closeSession(ctx);
            return;
        }

        // Remote AS number
        remoteAs = message.readUnsignedShort();
        //
        // Verify that the AS number is same for all other BGP Sessions
        // NOTE: This check applies only for our use-case where all BGP
        // sessions are iBGP.
        //
        for (BgpSession bgpSession : bgpSessionManager.getBgpSessions()) {
            if (remoteAs != bgpSession.getRemoteAs()) {
                log.debug("BGP RX OPEN Error from {}: Bad Peer AS {}. " +
                          "Expected {}",
                          remoteAddress, remoteAs, bgpSession.getRemoteAs());
                //
                // ERROR: Bad Peer AS
                //
                // Send NOTIFICATION and close the connection
                int errorCode = OpenMessageError.ERROR_CODE;
                int errorSubcode = OpenMessageError.BAD_PEER_AS;
                ChannelBuffer txMessage =
                    prepareBgpNotification(errorCode, errorSubcode, null);
                ctx.getChannel().write(txMessage);
                closeSession(ctx);
                return;
            }
        }

        // Remote Hold Time
        remoteHoldtime = message.readUnsignedShort();
        if ((remoteHoldtime != 0) &&
            (remoteHoldtime < BgpConstants.BGP_KEEPALIVE_MIN_HOLDTIME)) {
            log.debug("BGP RX OPEN Error from {}: " +
                      "Unacceptable Hold Time field {}. " +
                      "Should be 0 or at least {}",
                      remoteAddress, remoteHoldtime,
                      BgpConstants.BGP_KEEPALIVE_MIN_HOLDTIME);
            //
            // ERROR: Unacceptable Hold Time
            //
            // Send NOTIFICATION and close the connection
            int errorCode = OpenMessageError.ERROR_CODE;
            int errorSubcode = OpenMessageError.UNACCEPTABLE_HOLD_TIME;
            ChannelBuffer txMessage =
                prepareBgpNotification(errorCode, errorSubcode, null);
            ctx.getChannel().write(txMessage);
            closeSession(ctx);
            return;
        }

        // Remote BGP Identifier
        remoteBgpId = Ip4Address.valueOf((int) message.readUnsignedInt());

        // Optional Parameters
        int optParamLen = message.readUnsignedByte();
        if (message.readableBytes() < optParamLen) {
            log.debug("BGP RX OPEN Error from {}: " +
                      "Invalid Optional Parameter Length field {}. " +
                      "Remaining Optional Parameters {}",
                      remoteAddress, optParamLen, message.readableBytes());
            //
            // ERROR: Invalid Optional Parameter Length field: Unspecific
            //
            // Send NOTIFICATION and close the connection
            int errorCode = OpenMessageError.ERROR_CODE;
            int errorSubcode = Notifications.ERROR_SUBCODE_UNSPECIFIC;
            ChannelBuffer txMessage =
                prepareBgpNotification(errorCode, errorSubcode, null);
            ctx.getChannel().write(txMessage);
            closeSession(ctx);
            return;
        }
        // NOTE: Parse the optional parameters (if needed)
        message.readBytes(optParamLen);             // NOTE: data ignored

        //
        // Copy some of the remote peer's state/setup to the local setup:
        //  - BGP version
        //  - AS number (NOTE: the peer setup is always iBGP)
        //  - Holdtime
        // Also, assign the local BGP ID based on the local setup
        //
        localBgpVersion = remoteBgpVersion;
        localAs = remoteAs;
        localHoldtime = remoteHoldtime;
        localBgpId = bgpSessionManager.getMyBgpId();

        // Set the Keepalive interval
        if (localHoldtime == 0) {
            localKeepaliveInterval = 0;
        } else {
            localKeepaliveInterval = Math.max(localHoldtime /
                         BgpConstants.BGP_KEEPALIVE_PER_HOLD_INTERVAL,
                         BgpConstants.BGP_KEEPALIVE_MIN_INTERVAL);
        }

        log.debug("BGP RX OPEN message from {}: " +
                  "BGPv{} AS {} BGP-ID {} Holdtime {}",
                  remoteAddress, remoteBgpVersion, remoteAs,
                  remoteBgpId, remoteHoldtime);

        // Send my OPEN followed by KEEPALIVE
        ChannelBuffer txMessage = prepareBgpOpen();
        ctx.getChannel().write(txMessage);
        //
        txMessage = prepareBgpKeepalive();
        ctx.getChannel().write(txMessage);

        // Start the KEEPALIVE timer
        restartKeepaliveTimer(ctx);

        // Start the Session Timeout timer
        restartSessionTimeoutTimer(ctx);
    }

    /**
     * Processes BGP UPDATE message.
     *
     * @param ctx the Channel Handler Context
     * @param message the message to process
     */
    void processBgpUpdate(ChannelHandlerContext ctx, ChannelBuffer message) {
        Collection<BgpRouteEntry> addedRoutes = null;
        Map<Ip4Prefix, BgpRouteEntry> deletedRoutes = new HashMap<>();

        int minLength =
            BgpConstants.BGP_UPDATE_MIN_LENGTH - BgpConstants.BGP_HEADER_LENGTH;
        if (message.readableBytes() < minLength) {
            log.debug("BGP RX UPDATE Error from {}: " +
                      "Message length {} too short. Must be at least {}",
                      remoteAddress, message.readableBytes(), minLength);
            //
            // ERROR: Bad Message Length
            //
            // Send NOTIFICATION and close the connection
            ChannelBuffer txMessage = prepareBgpNotificationBadMessageLength(
                message.readableBytes() + BgpConstants.BGP_HEADER_LENGTH);
            ctx.getChannel().write(txMessage);
            closeSession(ctx);
            return;
        }

        log.debug("BGP RX UPDATE message from {}", remoteAddress);

        //
        // Parse the UPDATE message
        //

        //
        // Parse the Withdrawn Routes
        //
        int withdrawnRoutesLength = message.readUnsignedShort();
        if (withdrawnRoutesLength > message.readableBytes()) {
            // ERROR: Malformed Attribute List
            actionsBgpUpdateMalformedAttributeList(ctx);
            return;
        }
        Collection<Ip4Prefix> withdrawnPrefixes = null;
        try {
            withdrawnPrefixes = parsePackedPrefixes(withdrawnRoutesLength,
                                                    message);
        } catch (BgpParseException e) {
            // ERROR: Invalid Network Field
            log.debug("Exception parsing Withdrawn Prefixes from BGP peer {}: ",
                      remoteBgpId, e);
            actionsBgpUpdateInvalidNetworkField(ctx);
            return;
        }
        for (Ip4Prefix prefix : withdrawnPrefixes) {
            log.debug("BGP RX UPDATE message WITHDRAWN from {}: {}",
                      remoteAddress, prefix);
            BgpRouteEntry bgpRouteEntry = bgpRibIn.get(prefix);
            if (bgpRouteEntry != null) {
                deletedRoutes.put(prefix, bgpRouteEntry);
            }
        }

        //
        // Parse the Path Attributes
        //
        try {
            addedRoutes = parsePathAttributes(ctx, message);
        } catch (BgpParseException e) {
            log.debug("Exception parsing Path Attributes from BGP peer {}: ",
                      remoteBgpId, e);
            // NOTE: The session was already closed, so nothing else to do
            return;
        }
        // Ignore WITHDRAWN routes that are ADDED
        for (BgpRouteEntry bgpRouteEntry : addedRoutes) {
            deletedRoutes.remove(bgpRouteEntry.prefix());
        }

        // Update the BGP RIB-IN
        for (BgpRouteEntry bgpRouteEntry : deletedRoutes.values()) {
            bgpRibIn.remove(bgpRouteEntry.prefix());
        }
        for (BgpRouteEntry bgpRouteEntry : addedRoutes) {
            bgpRibIn.put(bgpRouteEntry.prefix(), bgpRouteEntry);
        }

        // Push the updates to the BGP Merged RIB
        BgpSessionManager.BgpRouteSelector bgpRouteSelector =
            bgpSessionManager.getBgpRouteSelector();
        bgpRouteSelector.routeUpdates(this, addedRoutes,
                                      deletedRoutes.values());

        // Start the Session Timeout timer
        restartSessionTimeoutTimer(ctx);
    }

    /**
     * Parse BGP Path Attributes from the BGP UPDATE message.
     *
     * @param ctx the Channel Handler Context
     * @param message the message to parse
     * @return a collection of the result BGP Route Entries
     * @throws BgpParseException
     */
    private Collection<BgpRouteEntry> parsePathAttributes(
                                ChannelHandlerContext ctx,
                                ChannelBuffer message)
        throws BgpParseException {
        Map<Ip4Prefix, BgpRouteEntry> addedRoutes = new HashMap<>();

        //
        // Parsed values
        //
        Short origin = -1;                      // Mandatory
        BgpRouteEntry.AsPath asPath = null;     // Mandatory
        Ip4Address nextHop = null;              // Mandatory
        long multiExitDisc =                    // Optional
            BgpConstants.Update.MultiExitDisc.LOWEST_MULTI_EXIT_DISC;
        Long localPref = null;                  // Mandatory
        Long aggregatorAsNumber = null;         // Optional: unused
        Ip4Address aggregatorIpAddress = null;  // Optional: unused

        //
        // Get and verify the Path Attributes Length
        //
        int pathAttributeLength = message.readUnsignedShort();
        if (pathAttributeLength > message.readableBytes()) {
            // ERROR: Malformed Attribute List
            actionsBgpUpdateMalformedAttributeList(ctx);
            String errorMsg = "Malformed Attribute List";
            throw new BgpParseException(errorMsg);
        }
        if (pathAttributeLength == 0) {
            return addedRoutes.values();
        }

        //
        // Parse the Path Attributes
        //
        int pathAttributeEnd = message.readerIndex() + pathAttributeLength;
        while (message.readerIndex() < pathAttributeEnd) {
            int attrFlags = message.readUnsignedByte();
            if (message.readerIndex() >= pathAttributeEnd) {
                // ERROR: Malformed Attribute List
                actionsBgpUpdateMalformedAttributeList(ctx);
                String errorMsg = "Malformed Attribute List";
                throw new BgpParseException(errorMsg);
            }
            int attrTypeCode = message.readUnsignedByte();

            // The Attribute Flags
            boolean optionalBit =       ((0x80 & attrFlags) != 0);
            boolean transitiveBit =     ((0x40 & attrFlags) != 0);
            boolean partialBit =        ((0x20 & attrFlags) != 0);
            boolean extendedLengthBit = ((0x10 & attrFlags) != 0);

            // The Attribute Length
            int attrLen = 0;
            int attrLenOctets = 1;
            if (extendedLengthBit) {
                attrLenOctets = 2;
            }
            if (message.readerIndex() + attrLenOctets > pathAttributeEnd) {
                // ERROR: Malformed Attribute List
                actionsBgpUpdateMalformedAttributeList(ctx);
                String errorMsg = "Malformed Attribute List";
                throw new BgpParseException(errorMsg);
            }
            if (extendedLengthBit) {
                attrLen = message.readUnsignedShort();
            } else {
                attrLen = message.readUnsignedByte();
            }
            if (message.readerIndex() + attrLen > pathAttributeEnd) {
                // ERROR: Malformed Attribute List
                actionsBgpUpdateMalformedAttributeList(ctx);
                String errorMsg = "Malformed Attribute List";
                throw new BgpParseException(errorMsg);
            }

            //
            // Verify the Attribute Flags
            //
            verifyBgpUpdateAttributeFlags(ctx, attrTypeCode, attrLen,
                                          attrFlags, message);

            //
            // Extract the Attribute Value based on the Attribute Type Code
            //
            switch (attrTypeCode) {

            case BgpConstants.Update.Origin.TYPE:
                // Attribute Type Code ORIGIN
                origin = parseAttributeTypeOrigin(ctx, attrTypeCode, attrLen,
                                                  attrFlags, message);
                break;

            case BgpConstants.Update.AsPath.TYPE:
                // Attribute Type Code AS_PATH
                asPath = parseAttributeTypeAsPath(ctx, attrTypeCode, attrLen,
                                                  attrFlags, message);
                break;

            case BgpConstants.Update.NextHop.TYPE:
                // Attribute Type Code NEXT_HOP
                nextHop = parseAttributeTypeNextHop(ctx, attrTypeCode, attrLen,
                                                    attrFlags, message);
                break;

            case BgpConstants.Update.MultiExitDisc.TYPE:
                // Attribute Type Code MULTI_EXIT_DISC
                multiExitDisc =
                    parseAttributeTypeMultiExitDisc(ctx, attrTypeCode, attrLen,
                                                    attrFlags, message);
                break;

            case BgpConstants.Update.LocalPref.TYPE:
                // Attribute Type Code LOCAL_PREF
                localPref =
                    parseAttributeTypeLocalPref(ctx, attrTypeCode, attrLen,
                                                attrFlags, message);
                break;

            case BgpConstants.Update.AtomicAggregate.TYPE:
                // Attribute Type Code ATOMIC_AGGREGATE
                parseAttributeTypeAtomicAggregate(ctx, attrTypeCode, attrLen,
                                                  attrFlags, message);
                // Nothing to do: this attribute is primarily informational
                break;

            case BgpConstants.Update.Aggregator.TYPE:
                // Attribute Type Code AGGREGATOR
                Pair<Long, Ip4Address> aggregator =
                    parseAttributeTypeAggregator(ctx, attrTypeCode, attrLen,
                                                 attrFlags, message);
                aggregatorAsNumber = aggregator.getLeft();
                aggregatorIpAddress = aggregator.getRight();
                break;

            default:
                // NOTE: Parse any new Attribute Types if needed
                if (!optionalBit) {
                    // ERROR: Unrecognized Well-known Attribute
                    actionsBgpUpdateUnrecognizedWellKnownAttribute(
                        ctx, attrTypeCode, attrLen, attrFlags, message);
                    String errorMsg = "Unrecognized Well-known Attribute: " +
                        attrTypeCode;
                    throw new BgpParseException(errorMsg);
                }

                // Skip the data from the unrecognized attribute
                log.debug("BGP RX UPDATE message from {}: " +
                          "Unrecognized Attribute Type {}",
                          remoteAddress, attrTypeCode);
                message.skipBytes(attrLen);
                break;
            }
        }

        //
        // Verify the Well-known Attributes
        //
        verifyBgpUpdateWellKnownAttributes(ctx, origin, asPath, nextHop,
                                           localPref);

        //
        // Parse the NLRI (Network Layer Reachability Information)
        //
        Collection<Ip4Prefix> addedPrefixes = null;
        int nlriLength = message.readableBytes();
        try {
            addedPrefixes = parsePackedPrefixes(nlriLength, message);
        } catch (BgpParseException e) {
            // ERROR: Invalid Network Field
            log.debug("Exception parsing NLRI from BGP peer {}: ",
                      remoteBgpId, e);
            actionsBgpUpdateInvalidNetworkField(ctx);
            // Rethrow the exception
            throw e;
        }

        // Generate the added routes
        for (Ip4Prefix prefix : addedPrefixes) {
            BgpRouteEntry bgpRouteEntry =
                new BgpRouteEntry(this, prefix, nextHop,
                                  origin.byteValue(), asPath, localPref);
            bgpRouteEntry.setMultiExitDisc(multiExitDisc);
            if (bgpRouteEntry.hasAsPathLoop(localAs)) {
                log.debug("BGP RX UPDATE message IGNORED from {}: {} " +
                          "nextHop {}: contains AS Path loop",
                          remoteAddress, prefix, nextHop);
                continue;
            } else {
                log.debug("BGP RX UPDATE message ADDED from {}: {} nextHop {}",
                          remoteAddress, prefix, nextHop);
            }
            addedRoutes.put(prefix, bgpRouteEntry);
        }

        return addedRoutes.values();
    }

    /**
     * Verifies BGP UPDATE Well-known Attributes.
     *
     * @param ctx the Channel Handler Context
     * @param origin the ORIGIN well-known mandatory attribute
     * @param asPath the AS_PATH well-known mandatory attribute
     * @param nextHop the NEXT_HOP well-known mandatory attribute
     * @param localPref the LOCAL_PREF required attribute
     * @throws BgpParseException
     */
    private void verifyBgpUpdateWellKnownAttributes(
                        ChannelHandlerContext ctx,
                        Short origin,
                        BgpRouteEntry.AsPath asPath,
                        Ip4Address nextHop,
                        Long localPref)
        throws BgpParseException {
        //
        // Check for Missing Well-known Attributes
        //
        if ((origin == null) || (origin == -1)) {
            // Missing Attribute Type Code ORIGIN
            int type = BgpConstants.Update.Origin.TYPE;
            actionsBgpUpdateMissingWellKnownAttribute(ctx, type);
            String errorMsg = "Missing Well-known Attribute: ORIGIN";
            throw new BgpParseException(errorMsg);
        }
        if (asPath == null) {
            // Missing Attribute Type Code AS_PATH
            int type = BgpConstants.Update.AsPath.TYPE;
            actionsBgpUpdateMissingWellKnownAttribute(ctx, type);
            String errorMsg = "Missing Well-known Attribute: AS_PATH";
            throw new BgpParseException(errorMsg);
        }
        if (nextHop == null) {
            // Missing Attribute Type Code NEXT_HOP
            int type = BgpConstants.Update.NextHop.TYPE;
            actionsBgpUpdateMissingWellKnownAttribute(ctx, type);
            String errorMsg = "Missing Well-known Attribute: NEXT_HOP";
            throw new BgpParseException(errorMsg);
        }
        if (localPref == null) {
            // Missing Attribute Type Code LOCAL_PREF
            // NOTE: Required for iBGP
            int type = BgpConstants.Update.LocalPref.TYPE;
            actionsBgpUpdateMissingWellKnownAttribute(ctx, type);
            String errorMsg = "Missing Well-known Attribute: LOCAL_PREF";
            throw new BgpParseException(errorMsg);
        }
    }

    /**
     * Verifies the BGP UPDATE Attribute Flags.
     *
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message to parse
     * @throws BgpParseException
     */
    private void verifyBgpUpdateAttributeFlags(
                        ChannelHandlerContext ctx,
                        int attrTypeCode,
                        int attrLen,
                        int attrFlags,
                        ChannelBuffer message)
        throws BgpParseException {

        //
        // Assign the Attribute Type Name and the Well-known flag
        //
        String typeName = "UNKNOWN";
        boolean isWellKnown = false;
        switch (attrTypeCode) {
        case BgpConstants.Update.Origin.TYPE:
            isWellKnown = true;
            typeName = "ORIGIN";
            break;
        case BgpConstants.Update.AsPath.TYPE:
            isWellKnown = true;
            typeName = "AS_PATH";
            break;
        case BgpConstants.Update.NextHop.TYPE:
            isWellKnown = true;
            typeName = "NEXT_HOP";
            break;
        case BgpConstants.Update.MultiExitDisc.TYPE:
            isWellKnown = false;
            typeName = "MULTI_EXIT_DISC";
            break;
        case BgpConstants.Update.LocalPref.TYPE:
            isWellKnown = true;
            typeName = "LOCAL_PREF";
            break;
        case BgpConstants.Update.AtomicAggregate.TYPE:
            isWellKnown = true;
            typeName = "ATOMIC_AGGREGATE";
            break;
        case BgpConstants.Update.Aggregator.TYPE:
            isWellKnown = false;
            typeName = "AGGREGATOR";
            break;
        default:
            isWellKnown = false;
            typeName = "UNKNOWN(" + attrTypeCode + ")";
            break;
        }

        //
        // Verify the Attribute Flags
        //
        boolean optionalBit =       ((0x80 & attrFlags) != 0);
        boolean transitiveBit =     ((0x40 & attrFlags) != 0);
        boolean partialBit =        ((0x20 & attrFlags) != 0);
        if ((isWellKnown && optionalBit) ||
            (isWellKnown && (!transitiveBit)) ||
            (isWellKnown && partialBit) ||
            (optionalBit && (!transitiveBit) && partialBit)) {
            //
            // ERROR: The Optional bit cannot be set for Well-known attributes
            // ERROR: The Transtive bit MUST be 1 for well-known attributes
            // ERROR: The Partial bit MUST be 0 for well-known attributes
            // ERROR: The Partial bit MUST be 0 for optional non-transitive
            //        attributes
            //
            actionsBgpUpdateAttributeFlagsError(
                ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Attribute Flags Error for " + typeName + ": " +
                attrFlags;
            throw new BgpParseException(errorMsg);
        }
    }

    /**
     * Parses BGP UPDATE Attribute Type ORIGIN.
     *
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message to parse
     * @return the parsed ORIGIN value
     * @throws BgpParseException
     */
    private short parseAttributeTypeOrigin(
                        ChannelHandlerContext ctx,
                        int attrTypeCode,
                        int attrLen,
                        int attrFlags,
                        ChannelBuffer message)
        throws BgpParseException {

        // Check the Attribute Length
        if (attrLen != BgpConstants.Update.Origin.LENGTH) {
            // ERROR: Attribute Length Error
            actionsBgpUpdateAttributeLengthError(
                        ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Attribute Length Error";
            throw new BgpParseException(errorMsg);
        }

        message.markReaderIndex();
        short origin = message.readUnsignedByte();
        switch (origin) {
        case BgpConstants.Update.Origin.IGP:
            // FALLTHROUGH
        case BgpConstants.Update.Origin.EGP:
            // FALLTHROUGH
        case BgpConstants.Update.Origin.INCOMPLETE:
            break;
        default:
            // ERROR: Invalid ORIGIN Attribute
            message.resetReaderIndex();
            actionsBgpUpdateInvalidOriginAttribute(
                ctx, attrTypeCode, attrLen, attrFlags, message, origin);
            String errorMsg = "Invalid ORIGIN Attribute: " + origin;
            throw new BgpParseException(errorMsg);
        }

        return origin;
    }

    /**
     * Parses BGP UPDATE Attribute AS Path.
     *
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message to parse
     * @return the parsed AS Path
     * @throws BgpParseException
     */
    private BgpRouteEntry.AsPath parseAttributeTypeAsPath(
                        ChannelHandlerContext ctx,
                        int attrTypeCode,
                        int attrLen,
                        int attrFlags,
                        ChannelBuffer message)
        throws BgpParseException {
        ArrayList<BgpRouteEntry.PathSegment> pathSegments = new ArrayList<>();

        //
        // Parse the message
        //
        while (attrLen > 0) {
            if (attrLen < 2) {
                // ERROR: Malformed AS_PATH
                actionsBgpUpdateMalformedAsPath(ctx);
                String errorMsg = "Malformed AS Path";
                throw new BgpParseException(errorMsg);
            }
            // Get the Path Segment Type and Length (in number of ASes)
            short pathSegmentType = message.readUnsignedByte();
            short pathSegmentLength = message.readUnsignedByte();
            attrLen -= 2;

            // Verify the Path Segment Type
            switch (pathSegmentType) {
            case BgpConstants.Update.AsPath.AS_SET:
                // FALLTHROUGH
            case BgpConstants.Update.AsPath.AS_SEQUENCE:
                // FALLTHROUGH
            case BgpConstants.Update.AsPath.AS_CONFED_SEQUENCE:
                // FALLTHROUGH
            case BgpConstants.Update.AsPath.AS_CONFED_SET:
                break;
            default:
                // ERROR: Invalid Path Segment Type
                //
                // NOTE: The BGP Spec (RFC 4271) doesn't contain Error Subcode
                // for "Invalid Path Segment Type", hence we return
                // the error as "Malformed AS_PATH".
                //
                actionsBgpUpdateMalformedAsPath(ctx);
                String errorMsg =
                    "Invalid AS Path Segment Type: " + pathSegmentType;
                throw new BgpParseException(errorMsg);
            }

            // Parse the AS numbers
            if (2 * pathSegmentLength > attrLen) {
                // ERROR: Malformed AS_PATH
                actionsBgpUpdateMalformedAsPath(ctx);
                String errorMsg = "Malformed AS Path";
                throw new BgpParseException(errorMsg);
            }
            attrLen -= (2 * pathSegmentLength);
            ArrayList<Long> segmentAsNumbers = new ArrayList<>();
            while (pathSegmentLength-- > 0) {
                long asNumber = message.readUnsignedShort();
                segmentAsNumbers.add(asNumber);
            }

            BgpRouteEntry.PathSegment pathSegment =
                new BgpRouteEntry.PathSegment((byte) pathSegmentType,
                                              segmentAsNumbers);
            pathSegments.add(pathSegment);
        }

        return new BgpRouteEntry.AsPath(pathSegments);
    }

    /**
     * Parses BGP UPDATE Attribute Type NEXT_HOP.
     *
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message to parse
     * @return the parsed NEXT_HOP value
     * @throws BgpParseException
     */
    private Ip4Address parseAttributeTypeNextHop(
                        ChannelHandlerContext ctx,
                        int attrTypeCode,
                        int attrLen,
                        int attrFlags,
                        ChannelBuffer message)
        throws BgpParseException {

        // Check the Attribute Length
        if (attrLen != BgpConstants.Update.NextHop.LENGTH) {
            // ERROR: Attribute Length Error
            actionsBgpUpdateAttributeLengthError(
                        ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Attribute Length Error";
            throw new BgpParseException(errorMsg);
        }

        message.markReaderIndex();
        Ip4Address nextHopAddress =
            Ip4Address.valueOf((int) message.readUnsignedInt());
        //
        // Check whether the NEXT_HOP IP address is semantically correct.
        // As per RFC 4271, Section 6.3:
        //
        //  a) It MUST NOT be the IP address of the receiving speaker
        //  b) In the case of an EBGP ....
        //
        // Here we check only (a), because (b) doesn't apply for us: all our
        // peers are iBGP.
        //
        if (nextHopAddress.equals(localIp4Address)) {
            // ERROR: Invalid NEXT_HOP Attribute
            message.resetReaderIndex();
            actionsBgpUpdateInvalidNextHopAttribute(
                        ctx, attrTypeCode, attrLen, attrFlags, message,
                        nextHopAddress);
            String errorMsg = "Invalid NEXT_HOP Attribute: " + nextHopAddress;
            throw new BgpParseException(errorMsg);
        }

        return nextHopAddress;
    }

    /**
     * Parses BGP UPDATE Attribute Type MULTI_EXIT_DISC.
     *
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message to parse
     * @return the parsed MULTI_EXIT_DISC value
     * @throws BgpParseException
     */
    private long parseAttributeTypeMultiExitDisc(
                        ChannelHandlerContext ctx,
                        int attrTypeCode,
                        int attrLen,
                        int attrFlags,
                        ChannelBuffer message)
        throws BgpParseException {

        // Check the Attribute Length
        if (attrLen != BgpConstants.Update.MultiExitDisc.LENGTH) {
            // ERROR: Attribute Length Error
            actionsBgpUpdateAttributeLengthError(
                        ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Attribute Length Error";
            throw new BgpParseException(errorMsg);
        }

        long multiExitDisc = message.readUnsignedInt();
        return multiExitDisc;
    }

    /**
     * Parses BGP UPDATE Attribute Type LOCAL_PREF.
     *
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message to parse
     * @return the parsed LOCAL_PREF value
     * @throws BgpParseException
     */
    private long parseAttributeTypeLocalPref(
                        ChannelHandlerContext ctx,
                        int attrTypeCode,
                        int attrLen,
                        int attrFlags,
                        ChannelBuffer message)
        throws BgpParseException {

        // Check the Attribute Length
        if (attrLen != BgpConstants.Update.LocalPref.LENGTH) {
            // ERROR: Attribute Length Error
            actionsBgpUpdateAttributeLengthError(
                        ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Attribute Length Error";
            throw new BgpParseException(errorMsg);
        }

        long localPref = message.readUnsignedInt();
        return localPref;
    }

    /**
     * Parses BGP UPDATE Attribute Type ATOMIC_AGGREGATE.
     *
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message to parse
     * @throws BgpParseException
     */
    private void parseAttributeTypeAtomicAggregate(
                        ChannelHandlerContext ctx,
                        int attrTypeCode,
                        int attrLen,
                        int attrFlags,
                        ChannelBuffer message)
        throws BgpParseException {

        // Check the Attribute Length
        if (attrLen != BgpConstants.Update.AtomicAggregate.LENGTH) {
            // ERROR: Attribute Length Error
            actionsBgpUpdateAttributeLengthError(
                        ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Attribute Length Error";
            throw new BgpParseException(errorMsg);
        }

        // Nothing to do: this attribute is primarily informational
    }

    /**
     * Parses BGP UPDATE Attribute Type AGGREGATOR.
     *
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message to parse
     * @return the parsed AGGREGATOR value: a tuple of <AS-Number, IP-Address>
     * @throws BgpParseException
     */
    private Pair<Long, Ip4Address> parseAttributeTypeAggregator(
                        ChannelHandlerContext ctx,
                        int attrTypeCode,
                        int attrLen,
                        int attrFlags,
                        ChannelBuffer message)
        throws BgpParseException {

        // Check the Attribute Length
        if (attrLen != BgpConstants.Update.Aggregator.LENGTH) {
            // ERROR: Attribute Length Error
            actionsBgpUpdateAttributeLengthError(
                        ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Attribute Length Error";
            throw new BgpParseException(errorMsg);
        }

        // The AGGREGATOR AS number
        long aggregatorAsNumber = message.readUnsignedShort();
        // The AGGREGATOR IP address
        Ip4Address aggregatorIpAddress =
            Ip4Address.valueOf((int) message.readUnsignedInt());

        Pair<Long, Ip4Address> aggregator = Pair.of(aggregatorAsNumber,
                                                    aggregatorIpAddress);
        return aggregator;
    }

    /**
     * Parses a message that contains encoded IPv4 network prefixes.
     * <p>
     * The IPv4 prefixes are encoded in the form:
     * <Length, Prefix> where Length is the length in bits of the IPv4 prefix,
     * and Prefix is the IPv4 prefix (padded with trailing bits to the end
     * of an octet).
     *
     * @param totalLength the total length of the data to parse
     * @param message the message with data to parse
     * @return a collection of parsed IPv4 network prefixes
     * @throws BgpParseException
     */
    private Collection<Ip4Prefix> parsePackedPrefixes(int totalLength,
                                                      ChannelBuffer message)
        throws BgpParseException {
        Collection<Ip4Prefix> result = new ArrayList<>();

        if (totalLength == 0) {
            return result;
        }

        // Parse the data
        int dataEnd = message.readerIndex() + totalLength;
        while (message.readerIndex() < dataEnd) {
            int prefixBitlen = message.readUnsignedByte();
            int prefixBytelen = (prefixBitlen + 7) / 8;     // Round-up
            if (message.readerIndex() + prefixBytelen > dataEnd) {
                String errorMsg = "Malformed Network Prefixes";
                throw new BgpParseException(errorMsg);
            }

            long address = 0;
            long extraShift = (4 - prefixBytelen) * 8;
            while (prefixBytelen > 0) {
                address <<= 8;
                address |= message.readUnsignedByte();
                prefixBytelen--;
            }
            address <<= extraShift;
            Ip4Prefix prefix =
                Ip4Prefix.valueOf(Ip4Address.valueOf((int) address),
                                  prefixBitlen);
            result.add(prefix);
        }

        return result;
    }

    /**
     * Applies the appropriate actions after detecting BGP UPDATE
     * Invalid Network Field Error: send NOTIFICATION and close the channel.
     *
     * @param ctx the Channel Handler Context
     */
    private void actionsBgpUpdateInvalidNetworkField(
                        ChannelHandlerContext ctx) {
        log.debug("BGP RX UPDATE Error from {}: Invalid Network Field",
                  remoteAddress);

        //
        // ERROR: Invalid Network Field
        //
        // Send NOTIFICATION and close the connection
        int errorCode = UpdateMessageError.ERROR_CODE;
        int errorSubcode = UpdateMessageError.INVALID_NETWORK_FIELD;
        ChannelBuffer txMessage =
            prepareBgpNotification(errorCode, errorSubcode, null);
        ctx.getChannel().write(txMessage);
        closeSession(ctx);
    }

    /**
     * Applies the appropriate actions after detecting BGP UPDATE
     * Malformed Attribute List Error: send NOTIFICATION and close the channel.
     *
     * @param ctx the Channel Handler Context
     */
    private void actionsBgpUpdateMalformedAttributeList(
                        ChannelHandlerContext ctx) {
        log.debug("BGP RX UPDATE Error from {}: Malformed Attribute List",
                  remoteAddress);

        //
        // ERROR: Malformed Attribute List
        //
        // Send NOTIFICATION and close the connection
        int errorCode = UpdateMessageError.ERROR_CODE;
        int errorSubcode = UpdateMessageError.MALFORMED_ATTRIBUTE_LIST;
        ChannelBuffer txMessage =
            prepareBgpNotification(errorCode, errorSubcode, null);
        ctx.getChannel().write(txMessage);
        closeSession(ctx);
    }

    /**
     * Applies the appropriate actions after detecting BGP UPDATE
     * Missing Well-known Attribute Error: send NOTIFICATION and close the
     * channel.
     *
     * @param ctx the Channel Handler Context
     * @param missingAttrTypeCode the missing attribute type code
     */
    private void actionsBgpUpdateMissingWellKnownAttribute(
                        ChannelHandlerContext ctx,
                        int missingAttrTypeCode) {
        log.debug("BGP RX UPDATE Error from {}: Missing Well-known Attribute: {}",
                  remoteAddress, missingAttrTypeCode);

        //
        // ERROR: Missing Well-known Attribute
        //
        // Send NOTIFICATION and close the connection
        int errorCode = UpdateMessageError.ERROR_CODE;
        int errorSubcode = UpdateMessageError.MISSING_WELL_KNOWN_ATTRIBUTE;
        ChannelBuffer data = ChannelBuffers.buffer(1);
        data.writeByte(missingAttrTypeCode);
        ChannelBuffer txMessage =
            prepareBgpNotification(errorCode, errorSubcode, data);
        ctx.getChannel().write(txMessage);
        closeSession(ctx);
    }

    /**
     * Applies the appropriate actions after detecting BGP UPDATE
     * Invalid ORIGIN Attribute Error: send NOTIFICATION and close the channel.
     *
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message with the data
     * @param origin the ORIGIN attribute value
     */
    private void actionsBgpUpdateInvalidOriginAttribute(
                        ChannelHandlerContext ctx,
                        int attrTypeCode,
                        int attrLen,
                        int attrFlags,
                        ChannelBuffer message,
                        short origin) {
        log.debug("BGP RX UPDATE Error from {}: Invalid ORIGIN Attribute",
                  remoteAddress);

        //
        // ERROR: Invalid ORIGIN Attribute
        //
        // Send NOTIFICATION and close the connection
        int errorCode = UpdateMessageError.ERROR_CODE;
        int errorSubcode = UpdateMessageError.INVALID_ORIGIN_ATTRIBUTE;
        ChannelBuffer data =
            prepareBgpUpdateNotificationDataPayload(attrTypeCode, attrLen,
                                                    attrFlags, message);
        ChannelBuffer txMessage =
            prepareBgpNotification(errorCode, errorSubcode, data);
        ctx.getChannel().write(txMessage);
        closeSession(ctx);
    }

    /**
     * Applies the appropriate actions after detecting BGP UPDATE
     * Attribute Flags Error: send NOTIFICATION and close the channel.
     *
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message with the data
     */
    private void actionsBgpUpdateAttributeFlagsError(
                        ChannelHandlerContext ctx,
                        int attrTypeCode,
                        int attrLen,
                        int attrFlags,
                        ChannelBuffer message) {
        log.debug("BGP RX UPDATE Error from {}: Attribute Flags Error",
                  remoteAddress);

        //
        // ERROR: Attribute Flags Error
        //
        // Send NOTIFICATION and close the connection
        int errorCode = UpdateMessageError.ERROR_CODE;
        int errorSubcode = UpdateMessageError.ATTRIBUTE_FLAGS_ERROR;
        ChannelBuffer data =
            prepareBgpUpdateNotificationDataPayload(attrTypeCode, attrLen,
                                                    attrFlags, message);
        ChannelBuffer txMessage =
            prepareBgpNotification(errorCode, errorSubcode, data);
        ctx.getChannel().write(txMessage);
        closeSession(ctx);
    }

    /**
     * Applies the appropriate actions after detecting BGP UPDATE
     * Invalid NEXT_HOP Attribute Error: send NOTIFICATION and close the
     * channel.
     *
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message with the data
     * @param nextHop the NEXT_HOP attribute value
     */
    private void actionsBgpUpdateInvalidNextHopAttribute(
                        ChannelHandlerContext ctx,
                        int attrTypeCode,
                        int attrLen,
                        int attrFlags,
                        ChannelBuffer message,
                        Ip4Address nextHop) {
        log.debug("BGP RX UPDATE Error from {}: Invalid NEXT_HOP Attribute {}",
                  remoteAddress, nextHop);

        //
        // ERROR: Invalid ORIGIN Attribute
        //
        // Send NOTIFICATION and close the connection
        int errorCode = UpdateMessageError.ERROR_CODE;
        int errorSubcode = UpdateMessageError.INVALID_NEXT_HOP_ATTRIBUTE;
        ChannelBuffer data =
            prepareBgpUpdateNotificationDataPayload(attrTypeCode, attrLen,
                                                    attrFlags, message);
        ChannelBuffer txMessage =
            prepareBgpNotification(errorCode, errorSubcode, data);
        ctx.getChannel().write(txMessage);
        closeSession(ctx);
    }

    /**
     * Applies the appropriate actions after detecting BGP UPDATE
     * Unrecognized Well-known Attribute Error: send NOTIFICATION and close
     * the channel.
     *
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message with the data
     */
    private void actionsBgpUpdateUnrecognizedWellKnownAttribute(
                        ChannelHandlerContext ctx,
                        int attrTypeCode,
                        int attrLen,
                        int attrFlags,
                        ChannelBuffer message) {
        log.debug("BGP RX UPDATE Error from {}: " +
                  "Unrecognized Well-known Attribute Error: {}",
                  remoteAddress, attrTypeCode);

        //
        // ERROR: Unrecognized Well-known Attribute
        //
        // Send NOTIFICATION and close the connection
        int errorCode = UpdateMessageError.ERROR_CODE;
        int errorSubcode =
            UpdateMessageError.UNRECOGNIZED_WELL_KNOWN_ATTRIBUTE;
        ChannelBuffer data =
            prepareBgpUpdateNotificationDataPayload(attrTypeCode, attrLen,
                                                    attrFlags, message);
        ChannelBuffer txMessage =
            prepareBgpNotification(errorCode, errorSubcode, data);
        ctx.getChannel().write(txMessage);
        closeSession(ctx);
    }

    /**
     * Applies the appropriate actions after detecting BGP UPDATE
     * Attribute Length Error: send NOTIFICATION and close the channel.
     *
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message with the data
     */
    private void actionsBgpUpdateAttributeLengthError(
                        ChannelHandlerContext ctx,
                        int attrTypeCode,
                        int attrLen,
                        int attrFlags,
                        ChannelBuffer message) {
        log.debug("BGP RX UPDATE Error from {}: Attribute Length Error",
                  remoteAddress);

        //
        // ERROR: Attribute Length Error
        //
        // Send NOTIFICATION and close the connection
        int errorCode = UpdateMessageError.ERROR_CODE;
        int errorSubcode = UpdateMessageError.ATTRIBUTE_LENGTH_ERROR;
        ChannelBuffer data =
            prepareBgpUpdateNotificationDataPayload(attrTypeCode, attrLen,
                                                    attrFlags, message);
        ChannelBuffer txMessage =
            prepareBgpNotification(errorCode, errorSubcode, data);
        ctx.getChannel().write(txMessage);
        closeSession(ctx);
    }

    /**
     * Applies the appropriate actions after detecting BGP UPDATE
     * Malformed AS_PATH Error: send NOTIFICATION and close the channel.
     *
     * @param ctx the Channel Handler Context
     */
    private void actionsBgpUpdateMalformedAsPath(
                        ChannelHandlerContext ctx) {
        log.debug("BGP RX UPDATE Error from {}: Malformed AS Path",
                  remoteAddress);

        //
        // ERROR: Malformed AS_PATH
        //
        // Send NOTIFICATION and close the connection
        int errorCode = UpdateMessageError.ERROR_CODE;
        int errorSubcode = UpdateMessageError.MALFORMED_AS_PATH;
        ChannelBuffer txMessage =
            prepareBgpNotification(errorCode, errorSubcode, null);
        ctx.getChannel().write(txMessage);
        closeSession(ctx);
    }

    /**
     * Processes BGP NOTIFICATION message.
     *
     * @param ctx the Channel Handler Context
     * @param message the message to process
     */
    void processBgpNotification(ChannelHandlerContext ctx,
                                ChannelBuffer message) {
        int minLength =
            BgpConstants.BGP_NOTIFICATION_MIN_LENGTH - BgpConstants.BGP_HEADER_LENGTH;
        if (message.readableBytes() < minLength) {
            log.debug("BGP RX NOTIFICATION Error from {}: " +
                      "Message length {} too short. Must be at least {}",
                      remoteAddress, message.readableBytes(), minLength);
            //
            // ERROR: Bad Message Length
            //
            // NOTE: We do NOT send NOTIFICATION in response to a notification
            return;
        }

        //
        // Parse the NOTIFICATION message
        //
        int errorCode = message.readUnsignedByte();
        int errorSubcode = message.readUnsignedByte();
        int dataLength = message.readableBytes();

        log.debug("BGP RX NOTIFICATION message from {}: Error Code {} " +
                  "Error Subcode {} Data Length {}",
                  remoteAddress, errorCode, errorSubcode, dataLength);

        //
        // NOTE: If the peer sent a NOTIFICATION, we leave it to the peer to
        // close the connection.
        //

        // Start the Session Timeout timer
        restartSessionTimeoutTimer(ctx);
    }

    /**
     * Processes BGP KEEPALIVE message.
     *
     * @param ctx the Channel Handler Context
     * @param message the message to process
     */
    void processBgpKeepalive(ChannelHandlerContext ctx,
                             ChannelBuffer message) {
        if (message.readableBytes() + BgpConstants.BGP_HEADER_LENGTH !=
            BgpConstants.BGP_KEEPALIVE_EXPECTED_LENGTH) {
            log.debug("BGP RX KEEPALIVE Error from {}: " +
                      "Invalid total message length {}. Expected {}",
                      remoteAddress,
                      message.readableBytes() + BgpConstants.BGP_HEADER_LENGTH,
                      BgpConstants.BGP_KEEPALIVE_EXPECTED_LENGTH);
            //
            // ERROR: Bad Message Length
            //
            // Send NOTIFICATION and close the connection
            ChannelBuffer txMessage = prepareBgpNotificationBadMessageLength(
                message.readableBytes() + BgpConstants.BGP_HEADER_LENGTH);
            ctx.getChannel().write(txMessage);
            closeSession(ctx);
            return;
        }

        //
        // Parse the KEEPALIVE message: nothing to do
        //
        log.trace("BGP RX KEEPALIVE message from {}", remoteAddress);

        // Start the Session Timeout timer
        restartSessionTimeoutTimer(ctx);
    }

    /**
     * Prepares BGP OPEN message.
     *
     * @return the message to transmit (BGP header included)
     */
    private ChannelBuffer prepareBgpOpen() {
        ChannelBuffer message =
            ChannelBuffers.buffer(BgpConstants.BGP_MESSAGE_MAX_LENGTH);

        //
        // Prepare the OPEN message payload
        //
        message.writeByte(localBgpVersion);
        message.writeShort((int) localAs);
        message.writeShort((int) localHoldtime);
        message.writeInt(bgpSessionManager.getMyBgpId().toInt());
        message.writeByte(0);               // No Optional Parameters
        return prepareBgpMessage(BgpConstants.BGP_TYPE_OPEN, message);
    }

    /**
     * Prepares BGP KEEPALIVE message.
     *
     * @return the message to transmit (BGP header included)
     */
    private ChannelBuffer prepareBgpKeepalive() {
        ChannelBuffer message =
            ChannelBuffers.buffer(BgpConstants.BGP_MESSAGE_MAX_LENGTH);

        //
        // Prepare the KEEPALIVE message payload: nothing to do
        //
        return prepareBgpMessage(BgpConstants.BGP_TYPE_KEEPALIVE, message);
    }

    /**
     * Prepares BGP NOTIFICATION message.
     *
     * @param errorCode the BGP NOTIFICATION Error Code
     * @param errorSubcode the BGP NOTIFICATION Error Subcode if applicable,
     * otherwise BgpConstants.Notifications.ERROR_SUBCODE_UNSPECIFIC
     * @param data the BGP NOTIFICATION Data if applicable, otherwise null
     * @return the message to transmit (BGP header included)
     */
    ChannelBuffer prepareBgpNotification(int errorCode, int errorSubcode,
                                         ChannelBuffer data) {
        ChannelBuffer message =
            ChannelBuffers.buffer(BgpConstants.BGP_MESSAGE_MAX_LENGTH);

        //
        // Prepare the NOTIFICATION message payload
        //
        message.writeByte(errorCode);
        message.writeByte(errorSubcode);
        if (data != null) {
            message.writeBytes(data);
        }
        return prepareBgpMessage(BgpConstants.BGP_TYPE_NOTIFICATION, message);
    }

    /**
     * Prepares BGP NOTIFICATION message: Bad Message Length.
     *
     * @param length the erroneous Length field
     * @return the message to transmit (BGP header included)
     */
    ChannelBuffer prepareBgpNotificationBadMessageLength(int length) {
        int errorCode = MessageHeaderError.ERROR_CODE;
        int errorSubcode = MessageHeaderError.BAD_MESSAGE_LENGTH;
        ChannelBuffer data = ChannelBuffers.buffer(2);
        data.writeShort(length);

        return prepareBgpNotification(errorCode, errorSubcode, data);
    }

    /**
     * Prepares BGP UPDATE Notification data payload.
     *
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message with the data
     * @return the buffer with the data payload for the BGP UPDATE Notification
     */
    private ChannelBuffer prepareBgpUpdateNotificationDataPayload(
                        int attrTypeCode,
                        int attrLen,
                        int attrFlags,
                        ChannelBuffer message) {
        // Compute the attribute length field octets
        boolean extendedLengthBit = ((0x10 & attrFlags) != 0);
        int attrLenOctets = 1;
        if (extendedLengthBit) {
            attrLenOctets = 2;
        }
        ChannelBuffer data =
            ChannelBuffers.buffer(attrLen + attrLenOctets + 1);
        data.writeByte(attrTypeCode);
        if (extendedLengthBit) {
            data.writeShort(attrLen);
        } else {
            data.writeByte(attrLen);
        }
        data.writeBytes(message, attrLen);
        return data;
    }

    /**
     * Prepares BGP message.
     *
     * @param type the BGP message type
     * @param payload the message payload to transmit (BGP header excluded)
     * @return the message to transmit (BGP header included)
     */
    private ChannelBuffer prepareBgpMessage(int type, ChannelBuffer payload) {
        ChannelBuffer message =
            ChannelBuffers.buffer(BgpConstants.BGP_HEADER_LENGTH +
                                  payload.readableBytes());

        // Write the marker
        for (int i = 0; i < BgpConstants.BGP_HEADER_MARKER_LENGTH; i++) {
            message.writeByte(0xff);
        }

        // Write the rest of the BGP header
        message.writeShort(BgpConstants.BGP_HEADER_LENGTH +
                           payload.readableBytes());
        message.writeByte(type);

        // Write the payload
        message.writeBytes(payload);
        return message;
    }

    /**
     * Restarts the BGP KeepaliveTimer.
     */
    private void restartKeepaliveTimer(ChannelHandlerContext ctx) {
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
            ChannelBuffer txMessage = prepareBgpKeepalive();
            ctx.getChannel().write(txMessage);

            // Restart the KEEPALIVE timer
            restartKeepaliveTimer(ctx);
        }
    }

    /**
     * Restarts the BGP Session Timeout Timer.
     */
    private void restartSessionTimeoutTimer(ChannelHandlerContext ctx) {
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
                prepareBgpNotification(errorCode, errorSubcode, null);
            ctx.getChannel().write(txMessage);
            closeChannel(ctx);
        }
    }

    /**
     * An exception indicating a parsing error of the BGP message.
     */
    private static class BgpParseException extends Exception {
        /**
         * Default constructor.
         */
        public BgpParseException() {
            super();
        }

        /**
         * Constructor for a specific exception details message.
         *
         * @param message the message with the exception details
         */
         public BgpParseException(String message) {
             super(message);
         }
    }
}
