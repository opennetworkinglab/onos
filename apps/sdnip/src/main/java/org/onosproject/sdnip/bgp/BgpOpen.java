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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.onlab.packet.Ip4Address;
import org.onosproject.sdnip.bgp.BgpConstants.Notifications;
import org.onosproject.sdnip.bgp.BgpConstants.Notifications.OpenMessageError;
import org.onosproject.sdnip.bgp.BgpConstants.Open.Capabilities;
import org.onosproject.sdnip.bgp.BgpConstants.Open.Capabilities.MultiprotocolExtensions;
import org.onosproject.sdnip.bgp.BgpConstants.Open.Capabilities.As4Octet;
import org.onosproject.sdnip.bgp.BgpMessage.BgpParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class for handling BGP OPEN messages.
 */
final class BgpOpen {
    private static final Logger log = LoggerFactory.getLogger(BgpOpen.class);

    /**
     * Default constructor.
     * <p>
     * The constructor is private to prevent creating an instance of
     * this utility class.
     */
    private BgpOpen() {
    }

    /**
     * Processes BGP OPEN message.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param message the message to process
     */
    static void processBgpOpen(BgpSession bgpSession,
                               ChannelHandlerContext ctx,
                               ChannelBuffer message) {
        int minLength =
            BgpConstants.BGP_OPEN_MIN_LENGTH - BgpConstants.BGP_HEADER_LENGTH;
        if (message.readableBytes() < minLength) {
            log.debug("BGP RX OPEN Error from {}: " +
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

        //
        // Parse the OPEN message
        //
        // Remote BGP version
        int remoteBgpVersion = message.readUnsignedByte();
        if (remoteBgpVersion != BgpConstants.BGP_VERSION) {
            log.debug("BGP RX OPEN Error from {}: " +
                      "Unsupported BGP version {}. Should be {}",
                      bgpSession.getRemoteAddress(), remoteBgpVersion,
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
                BgpNotification.prepareBgpNotification(errorCode, errorSubcode,
                                                       data);
            ctx.getChannel().write(txMessage);
            bgpSession.closeSession(ctx);
            return;
        }
        bgpSession.setRemoteBgpVersion(remoteBgpVersion);

        // Remote AS number
        long remoteAs = message.readUnsignedShort();
        //
        // Verify that the AS number is same for all other BGP Sessions
        // NOTE: This check applies only for our use-case where all BGP
        // sessions are iBGP.
        //
        for (BgpSession bs : bgpSession.getBgpSessionManager().getBgpSessions()) {
            if ((bs.getRemoteAs() != 0) && (remoteAs != bs.getRemoteAs())) {
                log.debug("BGP RX OPEN Error from {}: Bad Peer AS {}. " +
                          "Expected {}",
                          bgpSession.getRemoteAddress(), remoteAs,
                          bs.getRemoteAs());
                //
                // ERROR: Bad Peer AS
                //
                // Send NOTIFICATION and close the connection
                int errorCode = OpenMessageError.ERROR_CODE;
                int errorSubcode = OpenMessageError.BAD_PEER_AS;
                ChannelBuffer txMessage =
                    BgpNotification.prepareBgpNotification(errorCode,
                                                           errorSubcode, null);
                ctx.getChannel().write(txMessage);
                bgpSession.closeSession(ctx);
                return;
            }
        }
        bgpSession.setRemoteAs(remoteAs);

        // Remote Hold Time
        long remoteHoldtime = message.readUnsignedShort();
        if ((remoteHoldtime != 0) &&
            (remoteHoldtime < BgpConstants.BGP_KEEPALIVE_MIN_HOLDTIME)) {
            log.debug("BGP RX OPEN Error from {}: " +
                      "Unacceptable Hold Time field {}. " +
                      "Should be 0 or at least {}",
                      bgpSession.getRemoteAddress(), remoteHoldtime,
                      BgpConstants.BGP_KEEPALIVE_MIN_HOLDTIME);
            //
            // ERROR: Unacceptable Hold Time
            //
            // Send NOTIFICATION and close the connection
            int errorCode = OpenMessageError.ERROR_CODE;
            int errorSubcode = OpenMessageError.UNACCEPTABLE_HOLD_TIME;
            ChannelBuffer txMessage =
                BgpNotification.prepareBgpNotification(errorCode, errorSubcode,
                                                       null);
            ctx.getChannel().write(txMessage);
            bgpSession.closeSession(ctx);
            return;
        }
        bgpSession.setRemoteHoldtime(remoteHoldtime);

        // Remote BGP Identifier
        Ip4Address remoteBgpId =
            Ip4Address.valueOf((int) message.readUnsignedInt());
        bgpSession.setRemoteBgpId(remoteBgpId);

        // Parse the Optional Parameters
        try {
            parseOptionalParameters(bgpSession, ctx, message);
        } catch (BgpParseException e) {
            // ERROR: Error parsing optional parameters
            log.debug("BGP RX OPEN Error from {}: " +
                      "Exception parsing Optional Parameters: {}",
                      bgpSession.getRemoteAddress(), e);
            //
            // ERROR: Invalid Optional Parameters: Unspecific
            //
            // Send NOTIFICATION and close the connection
            int errorCode = OpenMessageError.ERROR_CODE;
            int errorSubcode = Notifications.ERROR_SUBCODE_UNSPECIFIC;
            ChannelBuffer txMessage =
                BgpNotification.prepareBgpNotification(errorCode, errorSubcode,
                                                       null);
            ctx.getChannel().write(txMessage);
            bgpSession.closeSession(ctx);
            return;
        }

        log.debug("BGP RX OPEN message from {}: " +
                  "BGPv{} AS {} BGP-ID {} Holdtime {}",
                  bgpSession.getRemoteAddress(), remoteBgpVersion, remoteAs,
                  remoteBgpId, remoteHoldtime);

        // Send my OPEN followed by KEEPALIVE
        ChannelBuffer txMessage = prepareBgpOpen(bgpSession);
        ctx.getChannel().write(txMessage);
        //
        txMessage = BgpKeepalive.prepareBgpKeepalive();
        ctx.getChannel().write(txMessage);

        // Start the KEEPALIVE timer
        bgpSession.restartKeepaliveTimer(ctx);

        // Start the Session Timeout timer
        bgpSession.restartSessionTimeoutTimer(ctx);
    }

    /**
     * Prepares BGP OPEN message.
     *
     * @param bgpSession the BGP Session to use
     * @return the message to transmit (BGP header included)
     */
    private static ChannelBuffer prepareBgpOpen(BgpSession bgpSession) {
        ChannelBuffer message =
            ChannelBuffers.buffer(BgpConstants.BGP_MESSAGE_MAX_LENGTH);

        //
        // Prepare the OPEN message payload
        //
        message.writeByte(bgpSession.getLocalBgpVersion());
        message.writeShort((int) bgpSession.getLocalAs());
        message.writeShort((int) bgpSession.getLocalHoldtime());
        message.writeInt(bgpSession.getLocalBgpId().toInt());

        // Prepare the optional BGP Capabilities
        ChannelBuffer capabilitiesMessage =
            prepareBgpOpenCapabilities(bgpSession);
        message.writeByte(capabilitiesMessage.readableBytes());
        message.writeBytes(capabilitiesMessage);

        return BgpMessage.prepareBgpMessage(BgpConstants.BGP_TYPE_OPEN,
                                            message);
    }

    /**
     * Parses BGP OPEN Optional Parameters.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param message the message to process
     * @throws BgpParseException
     */
    private static void parseOptionalParameters(BgpSession bgpSession,
                                                ChannelHandlerContext ctx,
                                                ChannelBuffer message)
        throws BgpParseException {

        //
        // Get and verify the Optional Parameters Length
        //
        int optParamLength = message.readUnsignedByte();
        if (optParamLength > message.readableBytes()) {
            // ERROR: Invalid Optional Parameter Length
            String errorMsg = "Invalid Optional Parameter Length field " +
                optParamLength + ". Remaining Optional Parameters " +
                message.readableBytes();
            throw new BgpParseException(errorMsg);
        }
        if (optParamLength == 0) {
            return;                     // No Optional Parameters
        }

        //
        // Parse the Optional Parameters
        //
        int optParamEnd = message.readerIndex() + optParamLength;
        while (message.readerIndex() < optParamEnd) {
            int paramType = message.readUnsignedByte();
            if (message.readerIndex() >= optParamEnd) {
                // ERROR: Malformed Optional Parameters
                String errorMsg = "Malformed Optional Parameters";
                throw new BgpParseException(errorMsg);
            }
            int paramLen = message.readUnsignedByte();
            if (message.readerIndex() + paramLen > optParamEnd) {
                // ERROR: Malformed Optional Parameters
                String errorMsg = "Malformed Optional Parameters";
                throw new BgpParseException(errorMsg);
            }

            //
            // Extract the Optional Parameter Value based on the Parameter Type
            //
            switch (paramType) {
            case Capabilities.TYPE:
                // Optional Parameter Type: Capabilities
                if (paramLen < Capabilities.MIN_LENGTH) {
                    // ERROR: Malformed Capability
                    String errorMsg = "Malformed Capability Type " + paramType;
                    throw new BgpParseException(errorMsg);
                }
                int capabEnd = message.readerIndex() + paramLen;
                int capabCode = message.readUnsignedByte();
                int capabLen = message.readUnsignedByte();
                if (message.readerIndex() + capabLen > capabEnd) {
                    // ERROR: Malformed Capability
                    String errorMsg = "Malformed Capability Type " + paramType;
                    throw new BgpParseException(errorMsg);
                }

                switch (capabCode) {
                case MultiprotocolExtensions.CODE:
                    // Multiprotocol Extensions Capabilities (RFC 4760)
                    if (capabLen != MultiprotocolExtensions.LENGTH) {
                        // ERROR: Multiprotocol Extension Length Error
                        String errorMsg = "Multiprotocol Extension Length Error";
                        throw new BgpParseException(errorMsg);
                    }
                    // Decode the AFI (2 octets) and SAFI (1 octet)
                    int afi = message.readUnsignedShort();
                    int reserved = message.readUnsignedByte();
                    int safi = message.readUnsignedByte();
                    log.debug("BGP RX OPEN Capability: AFI = {} SAFI = {}",
                              afi, safi);
                    //
                    // Setup the AFI/SAFI in the BgpSession
                    //
                    if (afi == MultiprotocolExtensions.AFI_IPV4 &&
                        safi == MultiprotocolExtensions.SAFI_UNICAST) {
                        bgpSession.setRemoteIpv4Unicast();
                    } else if (afi == MultiprotocolExtensions.AFI_IPV4 &&
                               safi == MultiprotocolExtensions.SAFI_MULTICAST) {
                        bgpSession.setRemoteIpv4Multicast();
                    } else if (afi == MultiprotocolExtensions.AFI_IPV6 &&
                               safi == MultiprotocolExtensions.SAFI_UNICAST) {
                        bgpSession.setRemoteIpv6Unicast();
                    } else if (afi == MultiprotocolExtensions.AFI_IPV6 &&
                               safi == MultiprotocolExtensions.SAFI_MULTICAST) {
                        bgpSession.setRemoteIpv6Multicast();
                    } else {
                        log.debug("BGP RX OPEN Capability: Unknown AFI = {} SAFI = {}",
                                  afi, safi);
                    }
                    break;

                case Capabilities.As4Octet.CODE:
                    // Support for 4-octet AS Number Capabilities (RFC 6793)
                    if (capabLen != Capabilities.As4Octet.LENGTH) {
                        // ERROR: 4-octet AS Number Capability Length Error
                        String errorMsg = "4-octet AS Number Capability Length Error";
                        throw new BgpParseException(errorMsg);
                    }
                    long as4Number = message.readUnsignedInt();

                    bgpSession.setRemoteAs4OctetCapability();
                    bgpSession.setRemoteAs4Octet(as4Number);

                    // Copy remote 4-octet AS Number Capabilities and AS Number.
                    // This is temporary setting until local AS number configuration is supported.
                    bgpSession.setLocalAs4OctetCapability();
                    bgpSession.setRemoteAs(as4Number);
                    log.debug("BGP RX OPEN Capability:  AS4 Number = {}",
                              as4Number);
                    break;

                default:
                    // Unknown Capability: ignore it
                    log.debug("BGP RX OPEN Capability Code = {} Length = {}",
                              capabCode, capabLen);
                    message.readBytes(capabLen);
                    break;
                }

                break;

            default:
                // Unknown Parameter Type: ignore it
                log.debug("BGP RX OPEN Parameter Type = {} Length = {}",
                          paramType, paramLen);
                message.readBytes(paramLen);
                break;
            }
        }
    }

    /**
     * Prepares the Capabilities for the BGP OPEN message.
     *
     * @param bgpSession the BGP Session to use
     * @return the buffer with the BGP Capabilities to transmit
     */
    private static ChannelBuffer prepareBgpOpenCapabilities(
                                        BgpSession bgpSession) {
        ChannelBuffer message =
            ChannelBuffers.buffer(BgpConstants.BGP_MESSAGE_MAX_LENGTH);

        //
        // Write the Multiprotocol Extensions Capabilities
        //

        // IPv4 unicast
        if (bgpSession.getLocalIpv4Unicast()) {
            message.writeByte(Capabilities.TYPE);               // Param type
            message.writeByte(Capabilities.MIN_LENGTH +
                              MultiprotocolExtensions.LENGTH);  // Param len
            message.writeByte(MultiprotocolExtensions.CODE);    // Capab. code
            message.writeByte(MultiprotocolExtensions.LENGTH);  // Capab. len
            message.writeShort(MultiprotocolExtensions.AFI_IPV4);
            message.writeByte(0);               // Reserved field
            message.writeByte(MultiprotocolExtensions.SAFI_UNICAST);
        }
        // IPv4 multicast
        if (bgpSession.getLocalIpv4Multicast()) {
            message.writeByte(Capabilities.TYPE);               // Param type
            message.writeByte(Capabilities.MIN_LENGTH +
                              MultiprotocolExtensions.LENGTH);  // Param len
            message.writeByte(MultiprotocolExtensions.CODE);    // Capab. code
            message.writeByte(MultiprotocolExtensions.LENGTH);  // Capab. len
            message.writeShort(MultiprotocolExtensions.AFI_IPV4);
            message.writeByte(0);               // Reserved field
            message.writeByte(MultiprotocolExtensions.SAFI_MULTICAST);
        }
        // IPv6 unicast
        if (bgpSession.getLocalIpv6Unicast()) {
            message.writeByte(Capabilities.TYPE);               // Param type
            message.writeByte(Capabilities.MIN_LENGTH +
                              MultiprotocolExtensions.LENGTH);  // Param len
            message.writeByte(MultiprotocolExtensions.CODE);    // Capab. code
            message.writeByte(MultiprotocolExtensions.LENGTH);  // Capab. len
            message.writeShort(MultiprotocolExtensions.AFI_IPV6);
            message.writeByte(0);               // Reserved field
            message.writeByte(MultiprotocolExtensions.SAFI_UNICAST);
        }
        // IPv6 multicast
        if (bgpSession.getLocalIpv6Multicast()) {
            message.writeByte(Capabilities.TYPE);               // Param type
            message.writeByte(Capabilities.MIN_LENGTH +
                              MultiprotocolExtensions.LENGTH);  // Param len
            message.writeByte(MultiprotocolExtensions.CODE);    // Capab. code
            message.writeByte(MultiprotocolExtensions.LENGTH);  // Capab. len
            message.writeShort(MultiprotocolExtensions.AFI_IPV6);
            message.writeByte(0);               // Reserved field
            message.writeByte(MultiprotocolExtensions.SAFI_MULTICAST);
        }

        // 4 octet AS path capability
        if (bgpSession.getLocalAs4OctetCapability()) {
            message.writeByte(Capabilities.TYPE);               // Param type
            message.writeByte(Capabilities.MIN_LENGTH +
                              As4Octet.LENGTH);                 // Param len
            message.writeByte(As4Octet.CODE);                   // Capab, code
            message.writeByte(As4Octet.LENGTH);                 // Capab, len
            message.writeInt((int) bgpSession.getLocalAs());
        }
        return message;
    }
}
