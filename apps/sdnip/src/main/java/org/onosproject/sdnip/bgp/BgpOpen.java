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

        // Optional Parameters
        int optParamLen = message.readUnsignedByte();
        if (message.readableBytes() < optParamLen) {
            log.debug("BGP RX OPEN Error from {}: " +
                      "Invalid Optional Parameter Length field {}. " +
                      "Remaining Optional Parameters {}",
                      bgpSession.getRemoteAddress(), optParamLen,
                      message.readableBytes());
            //
            // ERROR: Invalid Optional Parameter Length field: Unspecific
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
        // NOTE: Parse the optional parameters (if needed)
        message.readBytes(optParamLen);             // NOTE: data ignored

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
        message.writeByte(0);               // No Optional Parameters
        return BgpMessage.prepareBgpMessage(BgpConstants.BGP_TYPE_OPEN,
                                            message);
    }
}
