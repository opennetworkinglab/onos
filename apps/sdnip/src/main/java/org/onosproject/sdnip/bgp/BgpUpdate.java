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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onosproject.sdnip.bgp.BgpConstants.Notifications.UpdateMessageError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class for handling BGP UPDATE messages.
 */
final class BgpUpdate {
    private static final Logger log = LoggerFactory.getLogger(BgpUpdate.class);

    /**
     * Default constructor.
     * <p>
     * The constructor is private to prevent creating an instance of
     * this utility class.
     */
    private BgpUpdate() {
    }

    /**
     * Processes BGP UPDATE message.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param message the message to process
     */
    static void processBgpUpdate(BgpSession bgpSession,
                                 ChannelHandlerContext ctx,
                                 ChannelBuffer message) {
        Collection<BgpRouteEntry> addedRoutes = null;
        Map<Ip4Prefix, BgpRouteEntry> deletedRoutes = new HashMap<>();

        int minLength =
            BgpConstants.BGP_UPDATE_MIN_LENGTH - BgpConstants.BGP_HEADER_LENGTH;
        if (message.readableBytes() < minLength) {
            log.debug("BGP RX UPDATE Error from {}: " +
                      "Message length {} too short. Must be at least {}",
                      bgpSession.getRemoteAddress(), message.readableBytes(),
                      minLength);
            //
            // ERROR: Bad Message Length
            //
            // Send NOTIFICATION and close the connection
            ChannelBuffer txMessage =
                BgpNotification.prepareBgpNotificationBadMessageLength(
                message.readableBytes() + BgpConstants.BGP_HEADER_LENGTH);
            ctx.getChannel().write(txMessage);
            bgpSession.closeSession(ctx);
            return;
        }

        log.debug("BGP RX UPDATE message from {}",
                  bgpSession.getRemoteAddress());

        //
        // Parse the UPDATE message
        //

        //
        // Parse the Withdrawn Routes
        //
        int withdrawnRoutesLength = message.readUnsignedShort();
        if (withdrawnRoutesLength > message.readableBytes()) {
            // ERROR: Malformed Attribute List
            actionsBgpUpdateMalformedAttributeList(bgpSession, ctx);
            return;
        }
        Collection<Ip4Prefix> withdrawnPrefixes = null;
        try {
            withdrawnPrefixes = parsePackedPrefixes(withdrawnRoutesLength,
                                                    message);
        } catch (BgpParseException e) {
            // ERROR: Invalid Network Field
            log.debug("Exception parsing Withdrawn Prefixes from BGP peer {}: ",
                      bgpSession.getRemoteBgpId(), e);
            actionsBgpUpdateInvalidNetworkField(bgpSession, ctx);
            return;
        }
        for (Ip4Prefix prefix : withdrawnPrefixes) {
            log.debug("BGP RX UPDATE message WITHDRAWN from {}: {}",
                      bgpSession.getRemoteAddress(), prefix);
            BgpRouteEntry bgpRouteEntry = bgpSession.bgpRibIn().get(prefix);
            if (bgpRouteEntry != null) {
                deletedRoutes.put(prefix, bgpRouteEntry);
            }
        }

        //
        // Parse the Path Attributes
        //
        try {
            addedRoutes = parsePathAttributes(bgpSession, ctx, message);
        } catch (BgpParseException e) {
            log.debug("Exception parsing Path Attributes from BGP peer {}: ",
                      bgpSession.getRemoteBgpId(), e);
            // NOTE: The session was already closed, so nothing else to do
            return;
        }
        // Ignore WITHDRAWN routes that are ADDED
        for (BgpRouteEntry bgpRouteEntry : addedRoutes) {
            deletedRoutes.remove(bgpRouteEntry.prefix());
        }

        // Update the BGP RIB-IN
        for (BgpRouteEntry bgpRouteEntry : deletedRoutes.values()) {
            bgpSession.bgpRibIn().remove(bgpRouteEntry.prefix());
        }
        for (BgpRouteEntry bgpRouteEntry : addedRoutes) {
            bgpSession.bgpRibIn().put(bgpRouteEntry.prefix(), bgpRouteEntry);
        }

        // Push the updates to the BGP Merged RIB
        BgpSessionManager.BgpRouteSelector bgpRouteSelector =
            bgpSession.getBgpSessionManager().getBgpRouteSelector();
        bgpRouteSelector.routeUpdates(bgpSession, addedRoutes,
                                      deletedRoutes.values());

        // Start the Session Timeout timer
        bgpSession.restartSessionTimeoutTimer(ctx);
    }

    /**
     * Parse BGP Path Attributes from the BGP UPDATE message.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param message the message to parse
     * @return a collection of the result BGP Route Entries
     * @throws BgpParseException
     */
    private static Collection<BgpRouteEntry> parsePathAttributes(
                                                BgpSession bgpSession,
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
            actionsBgpUpdateMalformedAttributeList(bgpSession, ctx);
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
                actionsBgpUpdateMalformedAttributeList(bgpSession, ctx);
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
                actionsBgpUpdateMalformedAttributeList(bgpSession, ctx);
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
                actionsBgpUpdateMalformedAttributeList(bgpSession, ctx);
                String errorMsg = "Malformed Attribute List";
                throw new BgpParseException(errorMsg);
            }

            // Verify the Attribute Flags
            verifyBgpUpdateAttributeFlags(bgpSession, ctx, attrTypeCode,
                                          attrLen, attrFlags, message);

            //
            // Extract the Attribute Value based on the Attribute Type Code
            //
            switch (attrTypeCode) {

            case BgpConstants.Update.Origin.TYPE:
                // Attribute Type Code ORIGIN
                origin = parseAttributeTypeOrigin(bgpSession, ctx,
                                                  attrTypeCode, attrLen,
                                                  attrFlags, message);
                break;

            case BgpConstants.Update.AsPath.TYPE:
                // Attribute Type Code AS_PATH
                asPath = parseAttributeTypeAsPath(bgpSession, ctx,
                                                  attrTypeCode, attrLen,
                                                  attrFlags, message);
                break;

            case BgpConstants.Update.NextHop.TYPE:
                // Attribute Type Code NEXT_HOP
                nextHop = parseAttributeTypeNextHop(bgpSession, ctx,
                                                    attrTypeCode, attrLen,
                                                    attrFlags, message);
                break;

            case BgpConstants.Update.MultiExitDisc.TYPE:
                // Attribute Type Code MULTI_EXIT_DISC
                multiExitDisc =
                    parseAttributeTypeMultiExitDisc(bgpSession, ctx,
                                                    attrTypeCode, attrLen,
                                                    attrFlags, message);
                break;

            case BgpConstants.Update.LocalPref.TYPE:
                // Attribute Type Code LOCAL_PREF
                localPref =
                    parseAttributeTypeLocalPref(bgpSession, ctx,
                                                attrTypeCode, attrLen,
                                                attrFlags, message);
                break;

            case BgpConstants.Update.AtomicAggregate.TYPE:
                // Attribute Type Code ATOMIC_AGGREGATE
                parseAttributeTypeAtomicAggregate(bgpSession, ctx,
                                                  attrTypeCode, attrLen,
                                                  attrFlags, message);
                // Nothing to do: this attribute is primarily informational
                break;

            case BgpConstants.Update.Aggregator.TYPE:
                // Attribute Type Code AGGREGATOR
                Pair<Long, Ip4Address> aggregator =
                    parseAttributeTypeAggregator(bgpSession, ctx,
                                                 attrTypeCode, attrLen,
                                                 attrFlags, message);
                aggregatorAsNumber = aggregator.getLeft();
                aggregatorIpAddress = aggregator.getRight();
                break;

            default:
                // NOTE: Parse any new Attribute Types if needed
                if (!optionalBit) {
                    // ERROR: Unrecognized Well-known Attribute
                    actionsBgpUpdateUnrecognizedWellKnownAttribute(
                        bgpSession, ctx, attrTypeCode, attrLen, attrFlags,
                        message);
                    String errorMsg = "Unrecognized Well-known Attribute: " +
                        attrTypeCode;
                    throw new BgpParseException(errorMsg);
                }

                // Skip the data from the unrecognized attribute
                log.debug("BGP RX UPDATE message from {}: " +
                          "Unrecognized Attribute Type {}",
                          bgpSession.getRemoteAddress(), attrTypeCode);
                message.skipBytes(attrLen);
                break;
            }
        }

        // Verify the Well-known Attributes
        verifyBgpUpdateWellKnownAttributes(bgpSession, ctx, origin, asPath,
                                           nextHop, localPref);

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
                      bgpSession.getRemoteBgpId(), e);
            actionsBgpUpdateInvalidNetworkField(bgpSession, ctx);
            // Rethrow the exception
            throw e;
        }

        // Generate the added routes
        for (Ip4Prefix prefix : addedPrefixes) {
            BgpRouteEntry bgpRouteEntry =
                new BgpRouteEntry(bgpSession, prefix, nextHop,
                                  origin.byteValue(), asPath, localPref);
            bgpRouteEntry.setMultiExitDisc(multiExitDisc);
            if (bgpRouteEntry.hasAsPathLoop(bgpSession.getLocalAs())) {
                log.debug("BGP RX UPDATE message IGNORED from {}: {} " +
                          "nextHop {}: contains AS Path loop",
                          bgpSession.getRemoteAddress(), prefix, nextHop);
                continue;
            } else {
                log.debug("BGP RX UPDATE message ADDED from {}: {} nextHop {}",
                          bgpSession.getRemoteAddress(), prefix, nextHop);
            }
            addedRoutes.put(prefix, bgpRouteEntry);
        }

        return addedRoutes.values();
    }

    /**
     * Verifies BGP UPDATE Well-known Attributes.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param origin the ORIGIN well-known mandatory attribute
     * @param asPath the AS_PATH well-known mandatory attribute
     * @param nextHop the NEXT_HOP well-known mandatory attribute
     * @param localPref the LOCAL_PREF required attribute
     * @throws BgpParseException
     */
    private static void verifyBgpUpdateWellKnownAttributes(
                                BgpSession bgpSession,
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
            actionsBgpUpdateMissingWellKnownAttribute(bgpSession, ctx, type);
            String errorMsg = "Missing Well-known Attribute: ORIGIN";
            throw new BgpParseException(errorMsg);
        }
        if (asPath == null) {
            // Missing Attribute Type Code AS_PATH
            int type = BgpConstants.Update.AsPath.TYPE;
            actionsBgpUpdateMissingWellKnownAttribute(bgpSession, ctx, type);
            String errorMsg = "Missing Well-known Attribute: AS_PATH";
            throw new BgpParseException(errorMsg);
        }
        if (nextHop == null) {
            // Missing Attribute Type Code NEXT_HOP
            int type = BgpConstants.Update.NextHop.TYPE;
            actionsBgpUpdateMissingWellKnownAttribute(bgpSession, ctx, type);
            String errorMsg = "Missing Well-known Attribute: NEXT_HOP";
            throw new BgpParseException(errorMsg);
        }
        if (localPref == null) {
            // Missing Attribute Type Code LOCAL_PREF
            // NOTE: Required for iBGP
            int type = BgpConstants.Update.LocalPref.TYPE;
            actionsBgpUpdateMissingWellKnownAttribute(bgpSession, ctx, type);
            String errorMsg = "Missing Well-known Attribute: LOCAL_PREF";
            throw new BgpParseException(errorMsg);
        }
    }

    /**
     * Verifies the BGP UPDATE Attribute Flags.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message to parse
     * @throws BgpParseException
     */
    private static void verifyBgpUpdateAttributeFlags(
                                BgpSession bgpSession,
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
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Attribute Flags Error for " + typeName + ": " +
                attrFlags;
            throw new BgpParseException(errorMsg);
        }
    }

    /**
     * Parses BGP UPDATE Attribute Type ORIGIN.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message to parse
     * @return the parsed ORIGIN value
     * @throws BgpParseException
     */
    private static short parseAttributeTypeOrigin(
                                BgpSession bgpSession,
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
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message);
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
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message,
                origin);
            String errorMsg = "Invalid ORIGIN Attribute: " + origin;
            throw new BgpParseException(errorMsg);
        }

        return origin;
    }

    /**
     * Parses BGP UPDATE Attribute AS Path.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message to parse
     * @return the parsed AS Path
     * @throws BgpParseException
     */
    private static BgpRouteEntry.AsPath parseAttributeTypeAsPath(
                                                BgpSession bgpSession,
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
                actionsBgpUpdateMalformedAsPath(bgpSession, ctx);
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
                actionsBgpUpdateMalformedAsPath(bgpSession, ctx);
                String errorMsg =
                    "Invalid AS Path Segment Type: " + pathSegmentType;
                throw new BgpParseException(errorMsg);
            }

            // Parse the AS numbers
            if (2 * pathSegmentLength > attrLen) {
                // ERROR: Malformed AS_PATH
                actionsBgpUpdateMalformedAsPath(bgpSession, ctx);
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
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message to parse
     * @return the parsed NEXT_HOP value
     * @throws BgpParseException
     */
    private static Ip4Address parseAttributeTypeNextHop(
                                        BgpSession bgpSession,
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
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message);
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
        if (nextHopAddress.equals(bgpSession.getLocalIp4Address())) {
            // ERROR: Invalid NEXT_HOP Attribute
            message.resetReaderIndex();
            actionsBgpUpdateInvalidNextHopAttribute(
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message,
                nextHopAddress);
            String errorMsg = "Invalid NEXT_HOP Attribute: " + nextHopAddress;
            throw new BgpParseException(errorMsg);
        }

        return nextHopAddress;
    }

    /**
     * Parses BGP UPDATE Attribute Type MULTI_EXIT_DISC.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message to parse
     * @return the parsed MULTI_EXIT_DISC value
     * @throws BgpParseException
     */
    private static long parseAttributeTypeMultiExitDisc(
                                BgpSession bgpSession,
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
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Attribute Length Error";
            throw new BgpParseException(errorMsg);
        }

        long multiExitDisc = message.readUnsignedInt();
        return multiExitDisc;
    }

    /**
     * Parses BGP UPDATE Attribute Type LOCAL_PREF.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message to parse
     * @return the parsed LOCAL_PREF value
     * @throws BgpParseException
     */
    private static long parseAttributeTypeLocalPref(
                                BgpSession bgpSession,
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
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Attribute Length Error";
            throw new BgpParseException(errorMsg);
        }

        long localPref = message.readUnsignedInt();
        return localPref;
    }

    /**
     * Parses BGP UPDATE Attribute Type ATOMIC_AGGREGATE.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message to parse
     * @throws BgpParseException
     */
    private static void parseAttributeTypeAtomicAggregate(
                                BgpSession bgpSession,
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
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Attribute Length Error";
            throw new BgpParseException(errorMsg);
        }

        // Nothing to do: this attribute is primarily informational
    }

    /**
     * Parses BGP UPDATE Attribute Type AGGREGATOR.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message to parse
     * @return the parsed AGGREGATOR value: a tuple of <AS-Number, IP-Address>
     * @throws BgpParseException
     */
    private static Pair<Long, Ip4Address> parseAttributeTypeAggregator(
                                                BgpSession bgpSession,
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
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message);
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
    private static Collection<Ip4Prefix> parsePackedPrefixes(
                                                int totalLength,
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
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     */
    private static void actionsBgpUpdateInvalidNetworkField(
                                BgpSession bgpSession,
                                ChannelHandlerContext ctx) {
        log.debug("BGP RX UPDATE Error from {}: Invalid Network Field",
                  bgpSession.getRemoteAddress());

        //
        // ERROR: Invalid Network Field
        //
        // Send NOTIFICATION and close the connection
        int errorCode = UpdateMessageError.ERROR_CODE;
        int errorSubcode = UpdateMessageError.INVALID_NETWORK_FIELD;
        ChannelBuffer txMessage =
            BgpNotification.prepareBgpNotification(errorCode, errorSubcode,
                                                   null);
        ctx.getChannel().write(txMessage);
        bgpSession.closeSession(ctx);
    }

    /**
     * Applies the appropriate actions after detecting BGP UPDATE
     * Malformed Attribute List Error: send NOTIFICATION and close the channel.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     */
    private static void actionsBgpUpdateMalformedAttributeList(
                                BgpSession bgpSession,
                                ChannelHandlerContext ctx) {
        log.debug("BGP RX UPDATE Error from {}: Malformed Attribute List",
                  bgpSession.getRemoteAddress());

        //
        // ERROR: Malformed Attribute List
        //
        // Send NOTIFICATION and close the connection
        int errorCode = UpdateMessageError.ERROR_CODE;
        int errorSubcode = UpdateMessageError.MALFORMED_ATTRIBUTE_LIST;
        ChannelBuffer txMessage =
            BgpNotification.prepareBgpNotification(errorCode, errorSubcode,
                                                   null);
        ctx.getChannel().write(txMessage);
        bgpSession.closeSession(ctx);
    }

    /**
     * Applies the appropriate actions after detecting BGP UPDATE
     * Missing Well-known Attribute Error: send NOTIFICATION and close the
     * channel.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param missingAttrTypeCode the missing attribute type code
     */
    private static void actionsBgpUpdateMissingWellKnownAttribute(
                                BgpSession bgpSession,
                                ChannelHandlerContext ctx,
                                int missingAttrTypeCode) {
        log.debug("BGP RX UPDATE Error from {}: Missing Well-known Attribute: {}",
                  bgpSession.getRemoteAddress(), missingAttrTypeCode);

        //
        // ERROR: Missing Well-known Attribute
        //
        // Send NOTIFICATION and close the connection
        int errorCode = UpdateMessageError.ERROR_CODE;
        int errorSubcode = UpdateMessageError.MISSING_WELL_KNOWN_ATTRIBUTE;
        ChannelBuffer data = ChannelBuffers.buffer(1);
        data.writeByte(missingAttrTypeCode);
        ChannelBuffer txMessage =
            BgpNotification.prepareBgpNotification(errorCode, errorSubcode,
                                                   data);
        ctx.getChannel().write(txMessage);
        bgpSession.closeSession(ctx);
    }

    /**
     * Applies the appropriate actions after detecting BGP UPDATE
     * Invalid ORIGIN Attribute Error: send NOTIFICATION and close the channel.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message with the data
     * @param origin the ORIGIN attribute value
     */
    private static void actionsBgpUpdateInvalidOriginAttribute(
                                BgpSession bgpSession,
                                ChannelHandlerContext ctx,
                                int attrTypeCode,
                                int attrLen,
                                int attrFlags,
                                ChannelBuffer message,
                                short origin) {
        log.debug("BGP RX UPDATE Error from {}: Invalid ORIGIN Attribute",
                  bgpSession.getRemoteAddress());

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
            BgpNotification.prepareBgpNotification(errorCode, errorSubcode,
                                                   data);
        ctx.getChannel().write(txMessage);
        bgpSession.closeSession(ctx);
    }

    /**
     * Applies the appropriate actions after detecting BGP UPDATE
     * Attribute Flags Error: send NOTIFICATION and close the channel.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message with the data
     */
    private static void actionsBgpUpdateAttributeFlagsError(
                                BgpSession bgpSession,
                                ChannelHandlerContext ctx,
                                int attrTypeCode,
                                int attrLen,
                                int attrFlags,
                                ChannelBuffer message) {
        log.debug("BGP RX UPDATE Error from {}: Attribute Flags Error",
                  bgpSession.getRemoteAddress());

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
            BgpNotification.prepareBgpNotification(errorCode, errorSubcode,
                                                   data);
        ctx.getChannel().write(txMessage);
        bgpSession.closeSession(ctx);
    }

    /**
     * Applies the appropriate actions after detecting BGP UPDATE
     * Invalid NEXT_HOP Attribute Error: send NOTIFICATION and close the
     * channel.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message with the data
     * @param nextHop the NEXT_HOP attribute value
     */
    private static void actionsBgpUpdateInvalidNextHopAttribute(
                                BgpSession bgpSession,
                                ChannelHandlerContext ctx,
                                int attrTypeCode,
                                int attrLen,
                                int attrFlags,
                                ChannelBuffer message,
                                Ip4Address nextHop) {
        log.debug("BGP RX UPDATE Error from {}: Invalid NEXT_HOP Attribute {}",
                  bgpSession.getRemoteAddress(), nextHop);

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
            BgpNotification.prepareBgpNotification(errorCode, errorSubcode,
                                                   data);
        ctx.getChannel().write(txMessage);
        bgpSession.closeSession(ctx);
    }

    /**
     * Applies the appropriate actions after detecting BGP UPDATE
     * Unrecognized Well-known Attribute Error: send NOTIFICATION and close
     * the channel.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message with the data
     */
    private static void actionsBgpUpdateUnrecognizedWellKnownAttribute(
                                BgpSession bgpSession,
                                ChannelHandlerContext ctx,
                                int attrTypeCode,
                                int attrLen,
                                int attrFlags,
                                ChannelBuffer message) {
        log.debug("BGP RX UPDATE Error from {}: " +
                  "Unrecognized Well-known Attribute Error: {}",
                  bgpSession.getRemoteAddress(), attrTypeCode);

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
            BgpNotification.prepareBgpNotification(errorCode, errorSubcode,
                                                   data);
        ctx.getChannel().write(txMessage);
        bgpSession.closeSession(ctx);
    }

    /**
     * Applies the appropriate actions after detecting BGP UPDATE
     * Attribute Length Error: send NOTIFICATION and close the channel.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message with the data
     */
    private static void actionsBgpUpdateAttributeLengthError(
                                BgpSession bgpSession,
                                ChannelHandlerContext ctx,
                                int attrTypeCode,
                                int attrLen,
                                int attrFlags,
                                ChannelBuffer message) {
        log.debug("BGP RX UPDATE Error from {}: Attribute Length Error",
                  bgpSession.getRemoteAddress());

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
            BgpNotification.prepareBgpNotification(errorCode, errorSubcode,
                                                   data);
        ctx.getChannel().write(txMessage);
        bgpSession.closeSession(ctx);
    }

    /**
     * Applies the appropriate actions after detecting BGP UPDATE
     * Malformed AS_PATH Error: send NOTIFICATION and close the channel.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     */
    private static void actionsBgpUpdateMalformedAsPath(
                                BgpSession bgpSession,
                                ChannelHandlerContext ctx) {
        log.debug("BGP RX UPDATE Error from {}: Malformed AS Path",
                  bgpSession.getRemoteAddress());

        //
        // ERROR: Malformed AS_PATH
        //
        // Send NOTIFICATION and close the connection
        int errorCode = UpdateMessageError.ERROR_CODE;
        int errorSubcode = UpdateMessageError.MALFORMED_AS_PATH;
        ChannelBuffer txMessage =
            BgpNotification.prepareBgpNotification(errorCode, errorSubcode,
                                                   null);
        ctx.getChannel().write(txMessage);
        bgpSession.closeSession(ctx);
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
    private static ChannelBuffer prepareBgpUpdateNotificationDataPayload(
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
     * An exception indicating a parsing error of the BGP message.
     */
    private static final class BgpParseException extends Exception {
        /**
         * Default constructor.
         */
        private BgpParseException() {
            super();
        }

        /**
         * Constructor for a specific exception details message.
         *
         * @param message the message with the exception details
         */
         private BgpParseException(String message) {
             super(message);
         }
    }
}
