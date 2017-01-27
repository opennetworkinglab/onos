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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class for handling BGP NOTIFICATION messages.
 */
final class BgpNotification {
    private static final Logger log =
        LoggerFactory.getLogger(BgpNotification.class);

    /**
     * Default constructor.
     * <p>
     * The constructor is private to prevent creating an instance of
     * this utility class.
     */
    private BgpNotification() {
    }

    /**
     * Processes BGP NOTIFICATION message.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param message the message to process
     */
    static void processBgpNotification(BgpSession bgpSession,
                                       ChannelHandlerContext ctx,
                                       ChannelBuffer message) {
        int minLength =
            BgpConstants.BGP_NOTIFICATION_MIN_LENGTH - BgpConstants.BGP_HEADER_LENGTH;
        if (message.readableBytes() < minLength) {
            log.debug("BGP RX NOTIFICATION Error from {}: " +
                      "Message length {} too short. Must be at least {}",
                      bgpSession.remoteInfo().address(),
                      message.readableBytes(), minLength);
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
                  bgpSession.remoteInfo().address(), errorCode, errorSubcode,
                  dataLength);

        //
        // NOTE: If the peer sent a NOTIFICATION, we leave it to the peer to
        // close the connection.
        //

        // Start the Session Timeout timer
        bgpSession.restartSessionTimeoutTimer(ctx);
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
    static ChannelBuffer prepareBgpNotification(int errorCode,
                                                int errorSubcode,
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
        return BgpMessage.prepareBgpMessage(BgpConstants.BGP_TYPE_NOTIFICATION,
                                            message);
    }

    /**
     * Prepares BGP NOTIFICATION message: Bad Message Length.
     *
     * @param length the erroneous Length field
     * @return the message to transmit (BGP header included)
     */
    static ChannelBuffer prepareBgpNotificationBadMessageLength(int length) {
        int errorCode = BgpConstants.Notifications.MessageHeaderError.ERROR_CODE;
        int errorSubcode = BgpConstants.Notifications.MessageHeaderError.BAD_MESSAGE_LENGTH;
        ChannelBuffer data = ChannelBuffers.buffer(2);
        data.writeShort(length);

        return prepareBgpNotification(errorCode, errorSubcode, data);
    }
}
