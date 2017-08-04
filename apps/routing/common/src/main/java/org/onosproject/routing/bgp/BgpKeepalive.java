/*
 * Copyright 2017-present Open Networking Foundation
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
 * A class for handling BGP KEEPALIVE messages.
 */
final class BgpKeepalive {
    private static final Logger log =
        LoggerFactory.getLogger(BgpKeepalive.class);

    /**
     * Default constructor.
     * <p>
     * The constructor is private to prevent creating an instance of
     * this utility class.
     */
    private BgpKeepalive() {
    }

    /**
     * Processes BGP KEEPALIVE message.
     *
     * @param bgpSession the BGP Session to use
     * @param ctx the Channel Handler Context
     * @param message the message to process
     */
    static void processBgpKeepalive(BgpSession bgpSession,
                                    ChannelHandlerContext ctx,
                                    ChannelBuffer message) {
        if (message.readableBytes() + BgpConstants.BGP_HEADER_LENGTH !=
            BgpConstants.BGP_KEEPALIVE_EXPECTED_LENGTH) {
            log.debug("BGP RX KEEPALIVE Error from {}: " +
                      "Invalid total message length {}. Expected {}",
                      bgpSession.remoteInfo().address(),
                      message.readableBytes() + BgpConstants.BGP_HEADER_LENGTH,
                      BgpConstants.BGP_KEEPALIVE_EXPECTED_LENGTH);
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
        // Parse the KEEPALIVE message: nothing to do
        //
        log.trace("BGP RX KEEPALIVE message from {}",
                  bgpSession.remoteInfo().address());

        // Start the Session Timeout timer
        bgpSession.restartSessionTimeoutTimer(ctx);
    }

    /**
     * Prepares BGP KEEPALIVE message.
     *
     * @return the message to transmit (BGP header included)
     */
    static ChannelBuffer prepareBgpKeepalive() {
        ChannelBuffer message =
            ChannelBuffers.buffer(BgpConstants.BGP_MESSAGE_MAX_LENGTH);

        //
        // Prepare the KEEPALIVE message payload: nothing to do
        //
        return BgpMessage.prepareBgpMessage(BgpConstants.BGP_TYPE_KEEPALIVE,
                                            message);
    }
}
