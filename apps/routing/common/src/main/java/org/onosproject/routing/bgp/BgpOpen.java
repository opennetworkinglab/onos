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
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.onlab.packet.Ip4Address;
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

        //
        // Parse the OPEN message
        //
        // Remote BGP version
        int remoteBgpVersion = message.readUnsignedByte();
        if (remoteBgpVersion != BgpConstants.BGP_VERSION) {
            log.debug("BGP RX OPEN Error from {}: " +
                      "Unsupported BGP version {}. Should be {}",
                      bgpSession.remoteInfo().address(), remoteBgpVersion,
                      BgpConstants.BGP_VERSION);
            //
            // ERROR: Unsupported Version Number
            //
            // Send NOTIFICATION and close the connection
            int errorCode = BgpConstants.Notifications.OpenMessageError.ERROR_CODE;
            int errorSubcode = BgpConstants.Notifications.OpenMessageError.UNSUPPORTED_VERSION_NUMBER;
            ChannelBuffer data = ChannelBuffers.buffer(2);
            data.writeShort(BgpConstants.BGP_VERSION);
            ChannelBuffer txMessage =
                BgpNotification.prepareBgpNotification(errorCode, errorSubcode,
                                                       data);
            ctx.getChannel().write(txMessage);
            bgpSession.closeSession(ctx);
            return;
        }
        bgpSession.remoteInfo().setBgpVersion(remoteBgpVersion);

        // Remote AS number
        long remoteAs = message.readUnsignedShort();
        bgpSession.remoteInfo().setAsNumber(remoteAs);
        //
        // NOTE: Currently, the local AS number is always set to the remote AS.
        // This is done, because the peer setup is always iBGP.
        // In the future, the local AS number should be configured as part
        // of an explicit BGP peering configuration.
        //
        bgpSession.localInfo().setAsNumber(remoteAs);

        // Remote Hold Time
        long remoteHoldtime = message.readUnsignedShort();
        if ((remoteHoldtime != 0) &&
            (remoteHoldtime < BgpConstants.BGP_KEEPALIVE_MIN_HOLDTIME)) {
            log.debug("BGP RX OPEN Error from {}: " +
                      "Unacceptable Hold Time field {}. " +
                      "Should be 0 or at least {}",
                      bgpSession.remoteInfo().address(), remoteHoldtime,
                      BgpConstants.BGP_KEEPALIVE_MIN_HOLDTIME);
            //
            // ERROR: Unacceptable Hold Time
            //
            // Send NOTIFICATION and close the connection
            int errorCode = BgpConstants.Notifications.OpenMessageError.ERROR_CODE;
            int errorSubcode = BgpConstants.Notifications.OpenMessageError.UNACCEPTABLE_HOLD_TIME;
            ChannelBuffer txMessage =
                BgpNotification.prepareBgpNotification(errorCode, errorSubcode,
                                                       null);
            ctx.getChannel().write(txMessage);
            bgpSession.closeSession(ctx);
            return;
        }
        bgpSession.remoteInfo().setHoldtime(remoteHoldtime);
        //
        // NOTE: Currently. the local BGP Holdtime is always set to the remote
        // BGP holdtime.
        // In the future, the local BGP Holdtime should be configured as part
        // of an explicit BGP peering configuration.
        //
        bgpSession.localInfo().setHoldtime(remoteHoldtime);

        // Remote BGP Identifier
        Ip4Address remoteBgpId =
            Ip4Address.valueOf((int) message.readUnsignedInt());
        bgpSession.remoteInfo().setBgpId(remoteBgpId);

        // Parse the Optional Parameters
        try {
            parseOptionalParameters(bgpSession, ctx, message);
        } catch (BgpMessage.BgpParseException e) {
            // ERROR: Error parsing optional parameters
            log.debug("BGP RX OPEN Error from {}: " +
                      "Exception parsing Optional Parameters: {}",
                      bgpSession.remoteInfo().address(), e);
            //
            // ERROR: Invalid Optional Parameters: Unspecific
            //
            // Send NOTIFICATION and close the connection
            int errorCode = BgpConstants.Notifications.OpenMessageError.ERROR_CODE;
            int errorSubcode = BgpConstants.Notifications.ERROR_SUBCODE_UNSPECIFIC;
            ChannelBuffer txMessage =
                BgpNotification.prepareBgpNotification(errorCode, errorSubcode,
                                                       null);
            ctx.getChannel().write(txMessage);
            bgpSession.closeSession(ctx);
            return;
        }

        //
        // NOTE: Prepare the BGP OPEN message before the original local AS
        // is overwritten by the 4-octet AS number
        //
        ChannelBuffer txOpenMessage = prepareBgpOpen(bgpSession.localInfo());

        //
        // Use the 4-octet AS number in lieu of the "My AS" field
        // See RFC 6793, Section 4.1, second paragraph.
        //
        if (bgpSession.remoteInfo().as4OctetCapability()) {
            long as4Number = bgpSession.remoteInfo().as4Number();
            bgpSession.remoteInfo().setAsNumber(as4Number);
            bgpSession.localInfo().setAsNumber(as4Number);
        }

        //
        // Verify that the AS number is same for all other BGP Sessions
        // NOTE: This check applies only for our use-case where all BGP
        // sessions are iBGP.
        //
        for (BgpSession bs : bgpSession.getBgpSessionManager().getBgpSessions()) {
            if ((bs.remoteInfo().asNumber() != 0) &&
                (bgpSession.remoteInfo().asNumber() !=
                 bs.remoteInfo().asNumber())) {
                log.debug("BGP RX OPEN Error from {}: Bad Peer AS {}. " +
                          "Expected {}",
                          bgpSession.remoteInfo().address(),
                          bgpSession.remoteInfo().asNumber(),
                          bs.remoteInfo().asNumber());
                //
                // ERROR: Bad Peer AS
                //
                // Send NOTIFICATION and close the connection
                int errorCode = BgpConstants.Notifications.OpenMessageError.ERROR_CODE;
                int errorSubcode = BgpConstants.Notifications.OpenMessageError.BAD_PEER_AS;
                ChannelBuffer txMessage =
                    BgpNotification.prepareBgpNotification(errorCode,
                                                           errorSubcode, null);
                ctx.getChannel().write(txMessage);
                bgpSession.closeSession(ctx);
                return;
            }
        }

        log.debug("BGP RX OPEN message from {}: " +
                  "BGPv{} AS {} BGP-ID {} Holdtime {}",
                  bgpSession.remoteInfo().address(),
                  bgpSession.remoteInfo().bgpVersion(),
                  bgpSession.remoteInfo().asNumber(),
                  bgpSession.remoteInfo().bgpId(),
                  bgpSession.remoteInfo().holdtime());

        // Send my OPEN followed by KEEPALIVE
        ctx.getChannel().write(txOpenMessage);
        //
        ChannelBuffer txMessage = BgpKeepalive.prepareBgpKeepalive();
        ctx.getChannel().write(txMessage);

        // Start the KEEPALIVE timer
        bgpSession.restartKeepaliveTimer(ctx);

        // Start the Session Timeout timer
        bgpSession.restartSessionTimeoutTimer(ctx);
    }

    /**
     * Prepares BGP OPEN message.
     *
     * @param localInfo the BGP Session local information to use
     * @return the message to transmit (BGP header included)
     */
    static ChannelBuffer prepareBgpOpen(BgpSessionInfo localInfo) {
        ChannelBuffer message =
            ChannelBuffers.buffer(BgpConstants.BGP_MESSAGE_MAX_LENGTH);

        //
        // Prepare the OPEN message payload
        //
        message.writeByte(localInfo.bgpVersion());
        message.writeShort((int) localInfo.asNumber());
        message.writeShort((int) localInfo.holdtime());
        message.writeInt(localInfo.bgpId().toInt());

        // Prepare the optional BGP Capabilities
        ChannelBuffer capabilitiesMessage =
            prepareBgpOpenCapabilities(localInfo);
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
     * @throws BgpMessage.BgpParseException
     */
    private static void parseOptionalParameters(BgpSession bgpSession,
                                                ChannelHandlerContext ctx,
                                                ChannelBuffer message)
        throws BgpMessage.BgpParseException {

        //
        // Get and verify the Optional Parameters Length
        //
        int optParamLength = message.readUnsignedByte();
        if (optParamLength > message.readableBytes()) {
            // ERROR: Invalid Optional Parameter Length
            String errorMsg = "Invalid Optional Parameter Length field " +
                optParamLength + ". Remaining Optional Parameters " +
                message.readableBytes();
            throw new BgpMessage.BgpParseException(errorMsg);
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
                throw new BgpMessage.BgpParseException(errorMsg);
            }
            int paramLen = message.readUnsignedByte();
            if (message.readerIndex() + paramLen > optParamEnd) {
                // ERROR: Malformed Optional Parameters
                String errorMsg = "Malformed Optional Parameters";
                throw new BgpMessage.BgpParseException(errorMsg);
            }

            //
            // Extract the Optional Parameter Value based on the Parameter Type
            //
            switch (paramType) {
            case BgpConstants.Open.Capabilities.TYPE:
                // Optional Parameter Type: Capabilities
                if (paramLen < BgpConstants.Open.Capabilities.MIN_LENGTH) {
                    // ERROR: Malformed Param Type
                    String errorMsg = "Malformed Capabilities Optional "
                            + "Parameter Type " + paramType;
                    throw new BgpMessage.BgpParseException(errorMsg);
                }
                int paramEnd = message.readerIndex() + paramLen;
                // Parse Capabilities
                while (message.readerIndex() < paramEnd) {
                    if (paramEnd - message.readerIndex() <
                            BgpConstants.Open.Capabilities.MIN_LENGTH) {
                        String errorMsg = "Malformed Capabilities";
                        throw new BgpMessage.BgpParseException(errorMsg);
                    }
                    int capabCode = message.readUnsignedByte();
                    int capabLen = message.readUnsignedByte();
                    if (message.readerIndex() + capabLen > paramEnd) {
                        // ERROR: Malformed Capability
                        String errorMsg = "Malformed Capability instance with "
                                + "code " + capabCode;
                        throw new BgpMessage.BgpParseException(errorMsg);
                    }

                    switch (capabCode) {
                    case BgpConstants.Open.Capabilities.MultiprotocolExtensions.CODE:
                        // Multiprotocol Extensions Capabilities (RFC 4760)
                        if (capabLen != BgpConstants.Open.Capabilities.MultiprotocolExtensions.LENGTH) {
                            // ERROR: Multiprotocol Extension Length Error
                            String errorMsg = "Multiprotocol Extension Length Error";
                            throw new BgpMessage.BgpParseException(errorMsg);
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
                        // NOTE: For now we just copy the remote AFI/SAFI setting
                        // to the local configuration.
                        //
                        if (afi == BgpConstants.Open.Capabilities.MultiprotocolExtensions.AFI_IPV4 &&
                            safi == BgpConstants.Open.Capabilities.MultiprotocolExtensions.SAFI_UNICAST) {
                            bgpSession.remoteInfo().setIpv4Unicast();
                            bgpSession.localInfo().setIpv4Unicast();
                        } else if (afi == BgpConstants.Open.Capabilities.MultiprotocolExtensions.AFI_IPV4 &&
                                   safi == BgpConstants.Open.Capabilities.MultiprotocolExtensions.SAFI_MULTICAST) {
                            bgpSession.remoteInfo().setIpv4Multicast();
                            bgpSession.localInfo().setIpv4Multicast();
                        } else if (afi == BgpConstants.Open.Capabilities.MultiprotocolExtensions.AFI_IPV6 &&
                                   safi == BgpConstants.Open.Capabilities.MultiprotocolExtensions.SAFI_UNICAST) {
                            bgpSession.remoteInfo().setIpv6Unicast();
                            bgpSession.localInfo().setIpv6Unicast();
                        } else if (afi == BgpConstants.Open.Capabilities.MultiprotocolExtensions.AFI_IPV6 &&
                                   safi == BgpConstants.Open.Capabilities.MultiprotocolExtensions.SAFI_MULTICAST) {
                            bgpSession.remoteInfo().setIpv6Multicast();
                            bgpSession.localInfo().setIpv6Multicast();
                        } else {
                            log.debug("BGP RX OPEN Capability: Unknown AFI = {} SAFI = {}",
                                      afi, safi);
                        }
                        break;

                    case BgpConstants.Open.Capabilities.As4Octet.CODE:
                        // Support for 4-octet AS Number Capabilities (RFC 6793)
                        if (capabLen != BgpConstants.Open.Capabilities.As4Octet.LENGTH) {
                            // ERROR: 4-octet AS Number Capability Length Error
                            String errorMsg = "4-octet AS Number Capability Length Error";
                            throw new BgpMessage.BgpParseException(errorMsg);
                        }
                        long as4Number = message.readUnsignedInt();

                        bgpSession.remoteInfo().setAs4OctetCapability();
                        bgpSession.remoteInfo().setAs4Number(as4Number);

                        //
                        // Copy remote 4-octet AS Number Capabilities and AS
                        // Number. This is a temporary setting until local AS
                        // number configuration is supported.
                        //
                        bgpSession.localInfo().setAs4OctetCapability();
                        bgpSession.localInfo().setAs4Number(as4Number);
                        log.debug("BGP RX OPEN Capability: AS4 Number = {}",
                                  as4Number);
                        break;

                    default:
                        // Unknown Capability: ignore it
                        log.debug("BGP RX OPEN Capability Code = {} Length = {}",
                                  capabCode, capabLen);
                        message.readBytes(capabLen);
                        break;
                    }


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
     * @param localInfo the BGP Session local information to use
     * @return the buffer with the BGP Capabilities to transmit
     */
    private static ChannelBuffer prepareBgpOpenCapabilities(
                                        BgpSessionInfo localInfo) {
        ChannelBuffer message =
            ChannelBuffers.buffer(BgpConstants.BGP_MESSAGE_MAX_LENGTH);

        //
        // Write the Multiprotocol Extensions Capabilities
        //

        // IPv4 unicast
        if (localInfo.ipv4Unicast()) {
            message.writeByte(BgpConstants.Open.Capabilities.TYPE);               // Param type
            message.writeByte(BgpConstants.Open.Capabilities.MIN_LENGTH +
                              BgpConstants.Open.Capabilities.MultiprotocolExtensions.LENGTH);  // Param len
            message.writeByte(
                    BgpConstants.Open.Capabilities.MultiprotocolExtensions.CODE);    // Capab. code
            message.writeByte(
                    BgpConstants.Open.Capabilities.MultiprotocolExtensions.LENGTH);  // Capab. len
            message.writeShort(
                    BgpConstants.Open.Capabilities.MultiprotocolExtensions.AFI_IPV4);
            message.writeByte(0);               // Reserved field
            message.writeByte(
                    BgpConstants.Open.Capabilities.MultiprotocolExtensions.SAFI_UNICAST);
        }
        // IPv4 multicast
        if (localInfo.ipv4Multicast()) {
            message.writeByte(BgpConstants.Open.Capabilities.TYPE);               // Param type
            message.writeByte(BgpConstants.Open.Capabilities.MIN_LENGTH +
                              BgpConstants.Open.Capabilities.MultiprotocolExtensions.LENGTH);  // Param len
            message.writeByte(
                    BgpConstants.Open.Capabilities.MultiprotocolExtensions.CODE);    // Capab. code
            message.writeByte(
                    BgpConstants.Open.Capabilities.MultiprotocolExtensions.LENGTH);  // Capab. len
            message.writeShort(
                    BgpConstants.Open.Capabilities.MultiprotocolExtensions.AFI_IPV4);
            message.writeByte(0);               // Reserved field
            message.writeByte(
                    BgpConstants.Open.Capabilities.MultiprotocolExtensions.SAFI_MULTICAST);
        }
        // IPv6 unicast
        if (localInfo.ipv6Unicast()) {
            message.writeByte(BgpConstants.Open.Capabilities.TYPE);               // Param type
            message.writeByte(BgpConstants.Open.Capabilities.MIN_LENGTH +
                              BgpConstants.Open.Capabilities.MultiprotocolExtensions.LENGTH);  // Param len
            message.writeByte(
                    BgpConstants.Open.Capabilities.MultiprotocolExtensions.CODE);    // Capab. code
            message.writeByte(
                    BgpConstants.Open.Capabilities.MultiprotocolExtensions.LENGTH);  // Capab. len
            message.writeShort(
                    BgpConstants.Open.Capabilities.MultiprotocolExtensions.AFI_IPV6);
            message.writeByte(0);               // Reserved field
            message.writeByte(
                    BgpConstants.Open.Capabilities.MultiprotocolExtensions.SAFI_UNICAST);
        }
        // IPv6 multicast
        if (localInfo.ipv6Multicast()) {
            message.writeByte(BgpConstants.Open.Capabilities.TYPE);               // Param type
            message.writeByte(BgpConstants.Open.Capabilities.MIN_LENGTH +
                              BgpConstants.Open.Capabilities.MultiprotocolExtensions.LENGTH);  // Param len
            message.writeByte(
                    BgpConstants.Open.Capabilities.MultiprotocolExtensions.CODE);    // Capab. code
            message.writeByte(
                    BgpConstants.Open.Capabilities.MultiprotocolExtensions.LENGTH);  // Capab. len
            message.writeShort(
                    BgpConstants.Open.Capabilities.MultiprotocolExtensions.AFI_IPV6);
            message.writeByte(0);               // Reserved field
            message.writeByte(
                    BgpConstants.Open.Capabilities.MultiprotocolExtensions.SAFI_MULTICAST);
        }

        // 4 octet AS path capability
        if (localInfo.as4OctetCapability()) {
            message.writeByte(BgpConstants.Open.Capabilities.TYPE);               // Param type
            message.writeByte(BgpConstants.Open.Capabilities.MIN_LENGTH +
                              BgpConstants.Open.Capabilities.As4Octet.LENGTH);                 // Param len
            message.writeByte(BgpConstants.Open.Capabilities.As4Octet.CODE);                   // Capab. code
            message.writeByte(BgpConstants.Open.Capabilities.As4Octet.LENGTH);                 // Capab. len
            message.writeInt((int) localInfo.as4Number());
        }
        return message;
    }
}
