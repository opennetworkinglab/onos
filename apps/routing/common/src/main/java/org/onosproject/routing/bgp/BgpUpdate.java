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

import org.apache.commons.lang3.tuple.Pair;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.Ip6Prefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
        DecodedBgpRoutes decodedBgpRoutes = new DecodedBgpRoutes();

        int minLength =
            BgpConstants.BGP_UPDATE_MIN_LENGTH - BgpConstants.BGP_HEADER_LENGTH;
        if (message.readableBytes() < minLength) {
            log.debug("BGP RX UPDATE Error from {}: " +
                      "Message length {} too short. Must be at least {}",
                      bgpSession.remoteInfo().address(),
                      message.readableBytes(), minLength);
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
                  bgpSession.remoteInfo().address());

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
            withdrawnPrefixes = parsePackedIp4Prefixes(withdrawnRoutesLength,
                                                       message);
        } catch (BgpMessage.BgpParseException e) {
            // ERROR: Invalid Network Field
            log.debug("Exception parsing Withdrawn Prefixes from BGP peer {}: ",
                      bgpSession.remoteInfo().bgpId(), e);
            actionsBgpUpdateInvalidNetworkField(bgpSession, ctx);
            return;
        }
        for (Ip4Prefix prefix : withdrawnPrefixes) {
            log.debug("BGP RX UPDATE message WITHDRAWN from {}: {}",
                      bgpSession.remoteInfo().address(), prefix);
            BgpRouteEntry bgpRouteEntry = bgpSession.findBgpRoute(prefix);
            if (bgpRouteEntry != null) {
                decodedBgpRoutes.deletedUnicastRoutes4.put(prefix,
                                                           bgpRouteEntry);
            }
        }

        //
        // Parse the Path Attributes
        //
        try {
            parsePathAttributes(bgpSession, ctx, message, decodedBgpRoutes);
        } catch (BgpMessage.BgpParseException e) {
            log.debug("Exception parsing Path Attributes from BGP peer {}: ",
                      bgpSession.remoteInfo().bgpId(), e);
            // NOTE: The session was already closed, so nothing else to do
            return;
        }

        //
        // Update the BGP RIB-IN
        //
        for (Ip4Prefix ip4Prefix :
                 decodedBgpRoutes.deletedUnicastRoutes4.keySet()) {
            bgpSession.removeBgpRoute(ip4Prefix);
        }
        //
        for (BgpRouteEntry bgpRouteEntry :
                 decodedBgpRoutes.addedUnicastRoutes4.values()) {
            bgpSession.addBgpRoute(bgpRouteEntry);
        }
        //
        for (Ip6Prefix ip6Prefix :
                 decodedBgpRoutes.deletedUnicastRoutes6.keySet()) {
            bgpSession.removeBgpRoute(ip6Prefix);
        }
        //
        for (BgpRouteEntry bgpRouteEntry :
                 decodedBgpRoutes.addedUnicastRoutes6.values()) {
            bgpSession.addBgpRoute(bgpRouteEntry);
        }

        //
        // Push the updates to the BGP Merged RIB
        //
        BgpRouteSelector bgpRouteSelector =
            bgpSession.getBgpSessionManager().getBgpRouteSelector();
        bgpRouteSelector.routeUpdates(
                                decodedBgpRoutes.addedUnicastRoutes4.values(),
                                decodedBgpRoutes.deletedUnicastRoutes4.values());
        bgpRouteSelector.routeUpdates(
                                decodedBgpRoutes.addedUnicastRoutes6.values(),
                                decodedBgpRoutes.deletedUnicastRoutes6.values());

        // Start the Session Timeout timer
        bgpSession.restartSessionTimeoutTimer(ctx);
    }

    /**
     * Parse BGP Path Attributes from the BGP UPDATE message.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param message the message to parse
     * @param decodedBgpRoutes the container to store the decoded BGP Route
     * Entries. It might already contain some route entries such as withdrawn
     * IPv4 prefixes
     * @throws BgpMessage.BgpParseException
     */
    // CHECKSTYLE IGNORE MethodLength FOR NEXT 300 LINES
    private static void parsePathAttributes(
                                        BgpSession bgpSession,
                                        ChannelHandlerContext ctx,
                                        ChannelBuffer message,
                                        DecodedBgpRoutes decodedBgpRoutes)
        throws BgpMessage.BgpParseException {

        //
        // Parsed values
        //
        Short origin = -1;                      // Mandatory
        BgpRouteEntry.AsPath asPath = null;     // Mandatory
        // Legacy NLRI (RFC 4271). Mandatory NEXT_HOP if legacy NLRI is used
        MpNlri legacyNlri = new MpNlri(
                BgpConstants.Open.Capabilities.MultiprotocolExtensions.AFI_IPV4,
                                       BgpConstants.Open.Capabilities.MultiprotocolExtensions.SAFI_UNICAST);
        long multiExitDisc =                    // Optional
            BgpConstants.Update.MultiExitDisc.LOWEST_MULTI_EXIT_DISC;
        Long localPref = null;                  // Mandatory
        Long aggregatorAsNumber = null;         // Optional: unused
        Ip4Address aggregatorIpAddress = null;  // Optional: unused
        Collection<MpNlri> mpNlriReachList = new ArrayList<>();   // Optional
        Collection<MpNlri> mpNlriUnreachList = new ArrayList<>(); // Optional

        //
        // Get and verify the Path Attributes Length
        //
        int pathAttributeLength = message.readUnsignedShort();
        if (pathAttributeLength > message.readableBytes()) {
            // ERROR: Malformed Attribute List
            actionsBgpUpdateMalformedAttributeList(bgpSession, ctx);
            String errorMsg = "Malformed Attribute List";
            throw new BgpMessage.BgpParseException(errorMsg);
        }
        if (pathAttributeLength == 0) {
            return;
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
                throw new BgpMessage.BgpParseException(errorMsg);
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
                throw new BgpMessage.BgpParseException(errorMsg);
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
                throw new BgpMessage.BgpParseException(errorMsg);
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
                legacyNlri.nextHop4 =
                    parseAttributeTypeNextHop(bgpSession, ctx,
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

            case BgpConstants.Update.MpReachNlri.TYPE:
                // Attribute Type Code MP_REACH_NLRI
                MpNlri mpNlriReach =
                    parseAttributeTypeMpReachNlri(bgpSession, ctx,
                                                  attrTypeCode,
                                                  attrLen,
                                                  attrFlags, message);
                if (mpNlriReach != null) {
                    mpNlriReachList.add(mpNlriReach);
                }
                break;

            case BgpConstants.Update.MpUnreachNlri.TYPE:
                // Attribute Type Code MP_UNREACH_NLRI
                MpNlri mpNlriUnreach =
                    parseAttributeTypeMpUnreachNlri(bgpSession, ctx,
                                                    attrTypeCode, attrLen,
                                                    attrFlags, message);
                if (mpNlriUnreach != null) {
                    mpNlriUnreachList.add(mpNlriUnreach);
                }
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
                    throw new BgpMessage.BgpParseException(errorMsg);
                }

                // Skip the data from the unrecognized attribute
                log.debug("BGP RX UPDATE message from {}: " +
                          "Unrecognized Attribute Type {}",
                          bgpSession.remoteInfo().address(), attrTypeCode);
                message.skipBytes(attrLen);
                break;
            }
        }

        //
        // Parse the NLRI (Network Layer Reachability Information)
        //
        int nlriLength = message.readableBytes();
        try {
            Collection<Ip4Prefix> addedPrefixes4 =
                parsePackedIp4Prefixes(nlriLength, message);
            // Store it inside the legacy NLRI wrapper
            legacyNlri.nlri4 = addedPrefixes4;
        } catch (BgpMessage.BgpParseException e) {
            // ERROR: Invalid Network Field
            log.debug("Exception parsing NLRI from BGP peer {}: ",
                      bgpSession.remoteInfo().bgpId(), e);
            actionsBgpUpdateInvalidNetworkField(bgpSession, ctx);
            // Rethrow the exception
            throw e;
        }

        // Verify the Well-known Attributes
        verifyBgpUpdateWellKnownAttributes(bgpSession, ctx, origin, asPath,
                                           localPref, legacyNlri,
                                           mpNlriReachList);

        //
        // Generate the deleted routes
        //
        for (MpNlri mpNlri : mpNlriUnreachList) {
            BgpRouteEntry bgpRouteEntry;

            // The deleted IPv4 routes
            for (Ip4Prefix prefix : mpNlri.nlri4) {
                bgpRouteEntry = bgpSession.findBgpRoute(prefix);
                if (bgpRouteEntry != null) {
                    decodedBgpRoutes.deletedUnicastRoutes4.put(prefix,
                                                               bgpRouteEntry);
                }
            }

            // The deleted IPv6 routes
            for (Ip6Prefix prefix : mpNlri.nlri6) {
                bgpRouteEntry = bgpSession.findBgpRoute(prefix);
                if (bgpRouteEntry != null) {
                    decodedBgpRoutes.deletedUnicastRoutes6.put(prefix,
                                                               bgpRouteEntry);
                }
            }
        }

        //
        // Generate the added routes
        //
        mpNlriReachList.add(legacyNlri);
        for (MpNlri mpNlri : mpNlriReachList) {
            BgpRouteEntry bgpRouteEntry;

            // The added IPv4 routes
            for (Ip4Prefix prefix : mpNlri.nlri4) {
                bgpRouteEntry =
                    new BgpRouteEntry(bgpSession, prefix, mpNlri.nextHop4,
                                      origin.byteValue(), asPath, localPref);
                bgpRouteEntry.setMultiExitDisc(multiExitDisc);
                if (bgpRouteEntry.hasAsPathLoop(bgpSession.localInfo().asNumber())) {
                    log.debug("BGP RX UPDATE message IGNORED from {}: {} " +
                              "nextHop {}: contains AS Path loop",
                              bgpSession.remoteInfo().address(), prefix,
                              mpNlri.nextHop4);
                    continue;
                } else {
                    log.debug("BGP RX UPDATE message ADDED from {}: {} nextHop {}",
                              bgpSession.remoteInfo().address(), prefix,
                              mpNlri.nextHop4);
                }
                // Remove from the collection of deleted routes
                decodedBgpRoutes.deletedUnicastRoutes4.remove(prefix);
                decodedBgpRoutes.addedUnicastRoutes4.put(prefix,
                                                         bgpRouteEntry);
            }

            // The added IPv6 routes
            for (Ip6Prefix prefix : mpNlri.nlri6) {
                bgpRouteEntry =
                    new BgpRouteEntry(bgpSession, prefix, mpNlri.nextHop6,
                                      origin.byteValue(), asPath, localPref);
                bgpRouteEntry.setMultiExitDisc(multiExitDisc);
                if (bgpRouteEntry.hasAsPathLoop(bgpSession.localInfo().asNumber())) {
                    log.debug("BGP RX UPDATE message IGNORED from {}: {} " +
                              "nextHop {}: contains AS Path loop",
                              bgpSession.remoteInfo().address(), prefix,
                              mpNlri.nextHop6);
                    continue;
                } else {
                    log.debug("BGP RX UPDATE message ADDED from {}: {} nextHop {}",
                              bgpSession.remoteInfo().address(), prefix,
                              mpNlri.nextHop6);
                }
                // Remove from the collection of deleted routes
                decodedBgpRoutes.deletedUnicastRoutes6.remove(prefix);
                decodedBgpRoutes.addedUnicastRoutes6.put(prefix,
                                                         bgpRouteEntry);
            }
        }
    }

    /**
     * Verifies BGP UPDATE Well-known Attributes.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param origin the ORIGIN well-known mandatory attribute
     * @param asPath the AS_PATH well-known mandatory attribute
     * @param localPref the LOCAL_PREF required attribute
     * @param legacyNlri the legacy NLRI. Encapsulates the NEXT_HOP well-known
     * mandatory attribute (mandatory if legacy NLRI is used).
     * @param mpNlriReachList the Multiprotocol NLRI attributes
     * @throws BgpMessage.BgpParseException
     */
    private static void verifyBgpUpdateWellKnownAttributes(
                                BgpSession bgpSession,
                                ChannelHandlerContext ctx,
                                Short origin,
                                BgpRouteEntry.AsPath asPath,
                                Long localPref,
                                MpNlri legacyNlri,
                                Collection<MpNlri> mpNlriReachList)
        throws BgpMessage.BgpParseException {
        boolean hasNlri = false;
        boolean hasLegacyNlri = false;

        //
        // Convenience flags that are used to check for missing attributes.
        //
        // NOTE: The hasLegacyNlri flag is always set to true if the
        // Multiprotocol Extensions are not enabled, even if the UPDATE
        // message doesn't contain the legacy NLRI (per RFC 4271).
        //
        if (!bgpSession.mpExtensions()) {
            hasNlri = true;
            hasLegacyNlri = true;
        } else {
            if (!legacyNlri.nlri4.isEmpty()) {
                hasNlri = true;
                hasLegacyNlri = true;
            }
            if (!mpNlriReachList.isEmpty()) {
                hasNlri = true;
            }
        }

        //
        // Check for Missing Well-known Attributes
        //
        if (hasNlri && ((origin == null) || (origin == -1))) {
            // Missing Attribute Type Code ORIGIN
            int type = BgpConstants.Update.Origin.TYPE;
            actionsBgpUpdateMissingWellKnownAttribute(bgpSession, ctx, type);
            String errorMsg = "Missing Well-known Attribute: ORIGIN";
            throw new BgpMessage.BgpParseException(errorMsg);
        }
        if (hasNlri && (asPath == null)) {
            // Missing Attribute Type Code AS_PATH
            int type = BgpConstants.Update.AsPath.TYPE;
            actionsBgpUpdateMissingWellKnownAttribute(bgpSession, ctx, type);
            String errorMsg = "Missing Well-known Attribute: AS_PATH";
            throw new BgpMessage.BgpParseException(errorMsg);
        }
        if (hasNlri && (localPref == null)) {
            // Missing Attribute Type Code LOCAL_PREF
            // NOTE: Required for iBGP
            int type = BgpConstants.Update.LocalPref.TYPE;
            actionsBgpUpdateMissingWellKnownAttribute(bgpSession, ctx, type);
            String errorMsg = "Missing Well-known Attribute: LOCAL_PREF";
            throw new BgpMessage.BgpParseException(errorMsg);
        }
        if (hasLegacyNlri && (legacyNlri.nextHop4 == null)) {
            // Missing Attribute Type Code NEXT_HOP
            int type = BgpConstants.Update.NextHop.TYPE;
            actionsBgpUpdateMissingWellKnownAttribute(bgpSession, ctx, type);
            String errorMsg = "Missing Well-known Attribute: NEXT_HOP";
            throw new BgpMessage.BgpParseException(errorMsg);
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
     * @throws BgpMessage.BgpParseException
     */
    private static void verifyBgpUpdateAttributeFlags(
                                BgpSession bgpSession,
                                ChannelHandlerContext ctx,
                                int attrTypeCode,
                                int attrLen,
                                int attrFlags,
                                ChannelBuffer message)
        throws BgpMessage.BgpParseException {

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
        case BgpConstants.Update.MpReachNlri.TYPE:
            isWellKnown = false;
            typeName = "MP_REACH_NLRI";
            break;
        case BgpConstants.Update.MpUnreachNlri.TYPE:
            isWellKnown = false;
            typeName = "MP_UNREACH_NLRI";
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
            throw new BgpMessage.BgpParseException(errorMsg);
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
     * @throws BgpMessage.BgpParseException
     */
    private static short parseAttributeTypeOrigin(
                                BgpSession bgpSession,
                                ChannelHandlerContext ctx,
                                int attrTypeCode,
                                int attrLen,
                                int attrFlags,
                                ChannelBuffer message)
        throws BgpMessage.BgpParseException {

        // Check the Attribute Length
        if (attrLen != BgpConstants.Update.Origin.LENGTH) {
            // ERROR: Attribute Length Error
            actionsBgpUpdateAttributeLengthError(
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Attribute Length Error";
            throw new BgpMessage.BgpParseException(errorMsg);
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
            throw new BgpMessage.BgpParseException(errorMsg);
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
     * @throws BgpMessage.BgpParseException
     */
    private static BgpRouteEntry.AsPath parseAttributeTypeAsPath(
                                                BgpSession bgpSession,
                                                ChannelHandlerContext ctx,
                                                int attrTypeCode,
                                                int attrLen,
                                                int attrFlags,
                                                ChannelBuffer message)
        throws BgpMessage.BgpParseException {
        ArrayList<BgpRouteEntry.PathSegment> pathSegments = new ArrayList<>();

        //
        // Parse the message
        //
        while (attrLen > 0) {
            if (attrLen < 2) {
                // ERROR: Malformed AS_PATH
                actionsBgpUpdateMalformedAsPath(bgpSession, ctx);
                String errorMsg = "Malformed AS Path";
                throw new BgpMessage.BgpParseException(errorMsg);
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
                throw new BgpMessage.BgpParseException(errorMsg);
            }

            // 4-octet AS number handling.
            int asPathLen;
            if (bgpSession.isAs4OctetCapable()) {
                asPathLen = BgpConstants.Update.AS_4OCTET_LENGTH;
            } else {
                asPathLen = BgpConstants.Update.AS_LENGTH;
            }

            // Parse the AS numbers
            if (asPathLen * pathSegmentLength > attrLen) {
                // ERROR: Malformed AS_PATH
                actionsBgpUpdateMalformedAsPath(bgpSession, ctx);
                String errorMsg = "Malformed AS Path";
                throw new BgpMessage.BgpParseException(errorMsg);
            }
            attrLen -= (asPathLen * pathSegmentLength);
            ArrayList<Long> segmentAsNumbers = new ArrayList<>();
            while (pathSegmentLength-- > 0) {
                long asNumber;
                if (asPathLen == BgpConstants.Update.AS_4OCTET_LENGTH) {
                    asNumber = message.readUnsignedInt();
                } else {
                    asNumber = message.readUnsignedShort();
                }
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
     * @throws BgpMessage.BgpParseException
     */
    private static Ip4Address parseAttributeTypeNextHop(
                                        BgpSession bgpSession,
                                        ChannelHandlerContext ctx,
                                        int attrTypeCode,
                                        int attrLen,
                                        int attrFlags,
                                        ChannelBuffer message)
        throws BgpMessage.BgpParseException {

        // Check the Attribute Length
        if (attrLen != BgpConstants.Update.NextHop.LENGTH) {
            // ERROR: Attribute Length Error
            actionsBgpUpdateAttributeLengthError(
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Attribute Length Error";
            throw new BgpMessage.BgpParseException(errorMsg);
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
        if (nextHopAddress.equals(bgpSession.localInfo().ip4Address())) {
            // ERROR: Invalid NEXT_HOP Attribute
            message.resetReaderIndex();
            actionsBgpUpdateInvalidNextHopAttribute(
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message,
                nextHopAddress);
            String errorMsg = "Invalid NEXT_HOP Attribute: " + nextHopAddress;
            throw new BgpMessage.BgpParseException(errorMsg);
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
     * @throws BgpMessage.BgpParseException
     */
    private static long parseAttributeTypeMultiExitDisc(
                                BgpSession bgpSession,
                                ChannelHandlerContext ctx,
                                int attrTypeCode,
                                int attrLen,
                                int attrFlags,
                                ChannelBuffer message)
        throws BgpMessage.BgpParseException {

        // Check the Attribute Length
        if (attrLen != BgpConstants.Update.MultiExitDisc.LENGTH) {
            // ERROR: Attribute Length Error
            actionsBgpUpdateAttributeLengthError(
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Attribute Length Error";
            throw new BgpMessage.BgpParseException(errorMsg);
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
     * @throws BgpMessage.BgpParseException
     */
    private static long parseAttributeTypeLocalPref(
                                BgpSession bgpSession,
                                ChannelHandlerContext ctx,
                                int attrTypeCode,
                                int attrLen,
                                int attrFlags,
                                ChannelBuffer message)
        throws BgpMessage.BgpParseException {

        // Check the Attribute Length
        if (attrLen != BgpConstants.Update.LocalPref.LENGTH) {
            // ERROR: Attribute Length Error
            actionsBgpUpdateAttributeLengthError(
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Attribute Length Error";
            throw new BgpMessage.BgpParseException(errorMsg);
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
     * @throws BgpMessage.BgpParseException
     */
    private static void parseAttributeTypeAtomicAggregate(
                                BgpSession bgpSession,
                                ChannelHandlerContext ctx,
                                int attrTypeCode,
                                int attrLen,
                                int attrFlags,
                                ChannelBuffer message)
        throws BgpMessage.BgpParseException {

        // Check the Attribute Length
        if (attrLen != BgpConstants.Update.AtomicAggregate.LENGTH) {
            // ERROR: Attribute Length Error
            actionsBgpUpdateAttributeLengthError(
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Attribute Length Error";
            throw new BgpMessage.BgpParseException(errorMsg);
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
     * @throws BgpMessage.BgpParseException
     */
    private static Pair<Long, Ip4Address> parseAttributeTypeAggregator(
                                                BgpSession bgpSession,
                                                ChannelHandlerContext ctx,
                                                int attrTypeCode,
                                                int attrLen,
                                                int attrFlags,
                                                ChannelBuffer message)
        throws BgpMessage.BgpParseException {
        int expectedAttrLen;

        if (bgpSession.isAs4OctetCapable()) {
            expectedAttrLen = BgpConstants.Update.Aggregator.AS4_LENGTH;
        } else {
            expectedAttrLen = BgpConstants.Update.Aggregator.AS2_LENGTH;
        }

        // Check the Attribute Length
        if (attrLen != expectedAttrLen) {
            // ERROR: Attribute Length Error
            actionsBgpUpdateAttributeLengthError(
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Attribute Length Error";
            throw new BgpMessage.BgpParseException(errorMsg);
        }

        // The AGGREGATOR AS number
        long aggregatorAsNumber;
        if (bgpSession.isAs4OctetCapable()) {
            aggregatorAsNumber = message.readUnsignedInt();
        } else {
            aggregatorAsNumber = message.readUnsignedShort();
        }
        // The AGGREGATOR IP address
        Ip4Address aggregatorIpAddress =
            Ip4Address.valueOf((int) message.readUnsignedInt());

        Pair<Long, Ip4Address> aggregator = Pair.of(aggregatorAsNumber,
                                                    aggregatorIpAddress);
        return aggregator;
    }

    /**
     * Parses BGP UPDATE Attribute Type MP_REACH_NLRI.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message to parse
     * @return the parsed MP_REACH_NLRI information if recognized, otherwise
     * null
     * @throws BgpMessage.BgpParseException
     */
    private static MpNlri parseAttributeTypeMpReachNlri(
                                                BgpSession bgpSession,
                                                ChannelHandlerContext ctx,
                                                int attrTypeCode,
                                                int attrLen,
                                                int attrFlags,
                                                ChannelBuffer message)
        throws BgpMessage.BgpParseException {
        int attributeEnd = message.readerIndex() + attrLen;

        // Check the Attribute Length
        if (attrLen < BgpConstants.Update.MpReachNlri.MIN_LENGTH) {
            // ERROR: Attribute Length Error
            actionsBgpUpdateAttributeLengthError(
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Attribute Length Error";
            throw new BgpMessage.BgpParseException(errorMsg);
        }

        message.markReaderIndex();
        int afi = message.readUnsignedShort();
        int safi = message.readUnsignedByte();
        int nextHopLen = message.readUnsignedByte();

        //
        // Verify the AFI/SAFI, and skip the attribute if not recognized.
        // NOTE: Currently, we support only IPv4/IPv6 UNICAST
        //
        if (((afi != BgpConstants.Open.Capabilities.MultiprotocolExtensions.AFI_IPV4) &&
             (afi != BgpConstants.Open.Capabilities.MultiprotocolExtensions.AFI_IPV6)) ||
            (safi != BgpConstants.Open.Capabilities.MultiprotocolExtensions.SAFI_UNICAST)) {
            // Skip the attribute
            message.resetReaderIndex();
            message.skipBytes(attrLen);
            return null;
        }

        //
        // Verify the next-hop length
        //
        int expectedNextHopLen = 0;
        switch (afi) {
        case BgpConstants.Open.Capabilities.MultiprotocolExtensions.AFI_IPV4:
            expectedNextHopLen = Ip4Address.BYTE_LENGTH;
            break;
        case BgpConstants.Open.Capabilities.MultiprotocolExtensions.AFI_IPV6:
            expectedNextHopLen = Ip6Address.BYTE_LENGTH;
            break;
        default:
            // UNREACHABLE
            break;
        }
        if (nextHopLen != expectedNextHopLen) {
            // ERROR: Optional Attribute Error
            message.resetReaderIndex();
            actionsBgpUpdateOptionalAttributeError(
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Invalid next-hop network address length. " +
                "Received " + nextHopLen + " expected " + expectedNextHopLen;
            throw new BgpMessage.BgpParseException(errorMsg);
        }
        // NOTE: We use "+ 1" to take into account the Reserved field (1 octet)
        if (message.readerIndex() + nextHopLen + 1 >= attributeEnd) {
            // ERROR: Optional Attribute Error
            message.resetReaderIndex();
            actionsBgpUpdateOptionalAttributeError(
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Malformed next-hop network address";
            throw new BgpMessage.BgpParseException(errorMsg);
        }

        //
        // Get the Next-hop address, skip the Reserved field, and get the NLRI
        //
        byte[] nextHopBuffer = new byte[nextHopLen];
        message.readBytes(nextHopBuffer, 0, nextHopLen);
        int reserved = message.readUnsignedByte();
        MpNlri mpNlri = new MpNlri(afi, safi);
        try {
            switch (afi) {
            case BgpConstants.Open.Capabilities.MultiprotocolExtensions.AFI_IPV4:
                // The next-hop address
                mpNlri.nextHop4 = Ip4Address.valueOf(nextHopBuffer);
                // The NLRI
                mpNlri.nlri4 = parsePackedIp4Prefixes(
                                        attributeEnd - message.readerIndex(),
                                        message);
                break;
            case BgpConstants.Open.Capabilities.MultiprotocolExtensions.AFI_IPV6:
                // The next-hop address
                mpNlri.nextHop6 = Ip6Address.valueOf(nextHopBuffer);
                // The NLRI
                mpNlri.nlri6 = parsePackedIp6Prefixes(
                                        attributeEnd - message.readerIndex(),
                                        message);
                break;
            default:
                // UNREACHABLE
                break;
            }
        } catch (BgpMessage.BgpParseException e) {
            // ERROR: Optional Attribute Error
            message.resetReaderIndex();
            actionsBgpUpdateOptionalAttributeError(
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Malformed network layer reachability information";
            throw new BgpMessage.BgpParseException(errorMsg);
        }

        return mpNlri;
    }

    /**
     * Parses BGP UPDATE Attribute Type MP_UNREACH_NLRI.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message to parse
     * @return the parsed MP_UNREACH_NLRI information if recognized, otherwise
     * null
     * @throws BgpMessage.BgpParseException
     */
    private static MpNlri parseAttributeTypeMpUnreachNlri(
                                                BgpSession bgpSession,
                                                ChannelHandlerContext ctx,
                                                int attrTypeCode,
                                                int attrLen,
                                                int attrFlags,
                                                ChannelBuffer message)
        throws BgpMessage.BgpParseException {
        int attributeEnd = message.readerIndex() + attrLen;

        // Check the Attribute Length
        if (attrLen < BgpConstants.Update.MpUnreachNlri.MIN_LENGTH) {
            // ERROR: Attribute Length Error
            actionsBgpUpdateAttributeLengthError(
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Attribute Length Error";
            throw new BgpMessage.BgpParseException(errorMsg);
        }

        message.markReaderIndex();
        int afi = message.readUnsignedShort();
        int safi = message.readUnsignedByte();

        //
        // Verify the AFI/SAFI, and skip the attribute if not recognized.
        // NOTE: Currently, we support only IPv4/IPv6 UNICAST
        //
        if (((afi != BgpConstants.Open.Capabilities.MultiprotocolExtensions.AFI_IPV4) &&
             (afi != BgpConstants.Open.Capabilities.MultiprotocolExtensions.AFI_IPV6)) ||
            (safi != BgpConstants.Open.Capabilities.MultiprotocolExtensions.SAFI_UNICAST)) {
            // Skip the attribute
            message.resetReaderIndex();
            message.skipBytes(attrLen);
            return null;
        }

        //
        // Get the Withdrawn Routes
        //
        MpNlri mpNlri = new MpNlri(afi, safi);
        try {
            switch (afi) {
            case BgpConstants.Open.Capabilities.MultiprotocolExtensions.AFI_IPV4:
                // The Withdrawn Routes
                mpNlri.nlri4 = parsePackedIp4Prefixes(
                                        attributeEnd - message.readerIndex(),
                                        message);
                break;
            case BgpConstants.Open.Capabilities.MultiprotocolExtensions.AFI_IPV6:
                // The Withdrawn Routes
                mpNlri.nlri6 = parsePackedIp6Prefixes(
                                        attributeEnd - message.readerIndex(),
                                        message);
                break;
            default:
                // UNREACHABLE
                break;
            }
        } catch (BgpMessage.BgpParseException e) {
            // ERROR: Optional Attribute Error
            message.resetReaderIndex();
            actionsBgpUpdateOptionalAttributeError(
                bgpSession, ctx, attrTypeCode, attrLen, attrFlags, message);
            String errorMsg = "Malformed withdrawn routes";
            throw new BgpMessage.BgpParseException(errorMsg);
        }

        return mpNlri;
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
     * @throws BgpMessage.BgpParseException
     */
    private static Collection<Ip4Prefix> parsePackedIp4Prefixes(
                                                int totalLength,
                                                ChannelBuffer message)
        throws BgpMessage.BgpParseException {
        Collection<Ip4Prefix> result = new ArrayList<>();

        if (totalLength == 0) {
            return result;
        }

        // Parse the data
        byte[] buffer = new byte[Ip4Address.BYTE_LENGTH];
        int dataEnd = message.readerIndex() + totalLength;
        while (message.readerIndex() < dataEnd) {
            int prefixBitlen = message.readUnsignedByte();
            int prefixBytelen = (prefixBitlen + 7) / 8;     // Round-up
            if (message.readerIndex() + prefixBytelen > dataEnd) {
                String errorMsg = "Malformed Network Prefixes";
                throw new BgpMessage.BgpParseException(errorMsg);
            }

            message.readBytes(buffer, 0, prefixBytelen);
            Ip4Prefix prefix = Ip4Prefix.valueOf(Ip4Address.valueOf(buffer),
                                                 prefixBitlen);
            result.add(prefix);
        }

        return result;
    }

    /**
     * Parses a message that contains encoded IPv6 network prefixes.
     * <p>
     * The IPv6 prefixes are encoded in the form:
     * <Length, Prefix> where Length is the length in bits of the IPv6 prefix,
     * and Prefix is the IPv6 prefix (padded with trailing bits to the end
     * of an octet).
     *
     * @param totalLength the total length of the data to parse
     * @param message the message with data to parse
     * @return a collection of parsed IPv6 network prefixes
     * @throws BgpMessage.BgpParseException
     */
    private static Collection<Ip6Prefix> parsePackedIp6Prefixes(
                                                int totalLength,
                                                ChannelBuffer message)
        throws BgpMessage.BgpParseException {
        Collection<Ip6Prefix> result = new ArrayList<>();

        if (totalLength == 0) {
            return result;
        }

        // Parse the data
        byte[] buffer = new byte[Ip6Address.BYTE_LENGTH];
        int dataEnd = message.readerIndex() + totalLength;
        while (message.readerIndex() < dataEnd) {
            int prefixBitlen = message.readUnsignedByte();
            int prefixBytelen = (prefixBitlen + 7) / 8;     // Round-up
            if (message.readerIndex() + prefixBytelen > dataEnd) {
                String errorMsg = "Malformed Network Prefixes";
                throw new BgpMessage.BgpParseException(errorMsg);
            }

            message.readBytes(buffer, 0, prefixBytelen);
            Ip6Prefix prefix = Ip6Prefix.valueOf(Ip6Address.valueOf(buffer),
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
                  bgpSession.remoteInfo().address());

        //
        // ERROR: Invalid Network Field
        //
        // Send NOTIFICATION and close the connection
        int errorCode = BgpConstants.Notifications.UpdateMessageError.ERROR_CODE;
        int errorSubcode = BgpConstants.Notifications.UpdateMessageError.INVALID_NETWORK_FIELD;
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
                  bgpSession.remoteInfo().address());

        //
        // ERROR: Malformed Attribute List
        //
        // Send NOTIFICATION and close the connection
        int errorCode = BgpConstants.Notifications.UpdateMessageError.ERROR_CODE;
        int errorSubcode = BgpConstants.Notifications.UpdateMessageError.MALFORMED_ATTRIBUTE_LIST;
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
                  bgpSession.remoteInfo().address(), missingAttrTypeCode);

        //
        // ERROR: Missing Well-known Attribute
        //
        // Send NOTIFICATION and close the connection
        int errorCode = BgpConstants.Notifications.UpdateMessageError.ERROR_CODE;
        int errorSubcode = BgpConstants.Notifications.UpdateMessageError.MISSING_WELL_KNOWN_ATTRIBUTE;
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
                  bgpSession.remoteInfo().address());

        //
        // ERROR: Invalid ORIGIN Attribute
        //
        // Send NOTIFICATION and close the connection
        int errorCode = BgpConstants.Notifications.UpdateMessageError.ERROR_CODE;
        int errorSubcode = BgpConstants.Notifications.UpdateMessageError.INVALID_ORIGIN_ATTRIBUTE;
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
                  bgpSession.remoteInfo().address());

        //
        // ERROR: Attribute Flags Error
        //
        // Send NOTIFICATION and close the connection
        int errorCode = BgpConstants.Notifications.UpdateMessageError.ERROR_CODE;
        int errorSubcode = BgpConstants.Notifications.UpdateMessageError.ATTRIBUTE_FLAGS_ERROR;
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
                  bgpSession.remoteInfo().address(), nextHop);

        //
        // ERROR: Invalid NEXT_HOP Attribute
        //
        // Send NOTIFICATION and close the connection
        int errorCode = BgpConstants.Notifications.UpdateMessageError.ERROR_CODE;
        int errorSubcode = BgpConstants.Notifications.UpdateMessageError.INVALID_NEXT_HOP_ATTRIBUTE;
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
                  bgpSession.remoteInfo().address(), attrTypeCode);

        //
        // ERROR: Unrecognized Well-known Attribute
        //
        // Send NOTIFICATION and close the connection
        int errorCode = BgpConstants.Notifications.UpdateMessageError.ERROR_CODE;
        int errorSubcode =
            BgpConstants.Notifications.UpdateMessageError.UNRECOGNIZED_WELL_KNOWN_ATTRIBUTE;
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
     * Optional Attribute Error: send NOTIFICATION and close
     * the channel.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param attrTypeCode the attribute type code
     * @param attrLen the attribute length (in octets)
     * @param attrFlags the attribute flags
     * @param message the message with the data
     */
    private static void actionsBgpUpdateOptionalAttributeError(
                                BgpSession bgpSession,
                                ChannelHandlerContext ctx,
                                int attrTypeCode,
                                int attrLen,
                                int attrFlags,
                                ChannelBuffer message) {
        log.debug("BGP RX UPDATE Error from {}: Optional Attribute Error: {}",
                  bgpSession.remoteInfo().address(), attrTypeCode);

        //
        // ERROR: Optional Attribute Error
        //
        // Send NOTIFICATION and close the connection
        int errorCode = BgpConstants.Notifications.UpdateMessageError.ERROR_CODE;
        int errorSubcode =
            BgpConstants.Notifications.UpdateMessageError.OPTIONAL_ATTRIBUTE_ERROR;
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
                  bgpSession.remoteInfo().address());

        //
        // ERROR: Attribute Length Error
        //
        // Send NOTIFICATION and close the connection
        int errorCode = BgpConstants.Notifications.UpdateMessageError.ERROR_CODE;
        int errorSubcode = BgpConstants.Notifications.UpdateMessageError.ATTRIBUTE_LENGTH_ERROR;
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
                  bgpSession.remoteInfo().address());

        //
        // ERROR: Malformed AS_PATH
        //
        // Send NOTIFICATION and close the connection
        int errorCode = BgpConstants.Notifications.UpdateMessageError.ERROR_CODE;
        int errorSubcode = BgpConstants.Notifications.UpdateMessageError.MALFORMED_AS_PATH;
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
     * Helper class for storing Multiprotocol Network Layer Reachability
     * information.
     */
    private static final class MpNlri {
        private final int afi;
        private final int safi;
        private Ip4Address nextHop4;
        private Ip6Address nextHop6;
        private Collection<Ip4Prefix> nlri4 = new ArrayList<>();
        private Collection<Ip6Prefix> nlri6 = new ArrayList<>();

        /**
         * Constructor.
         *
         * @param afi the Address Family Identifier
         * @param safi the Subsequent Address Family Identifier
         */
        private MpNlri(int afi, int safi) {
            this.afi = afi;
            this.safi = safi;
        }
    }

    /**
     * Helper class for storing decoded BGP routing information.
     */
    private static final class DecodedBgpRoutes {
        private final Map<Ip4Prefix, BgpRouteEntry> addedUnicastRoutes4 =
            new HashMap<>();
        private final Map<Ip6Prefix, BgpRouteEntry> addedUnicastRoutes6 =
            new HashMap<>();
        private final Map<Ip4Prefix, BgpRouteEntry> deletedUnicastRoutes4 =
            new HashMap<>();
        private final Map<Ip6Prefix, BgpRouteEntry> deletedUnicastRoutes6 =
            new HashMap<>();
    }
}
