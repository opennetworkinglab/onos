/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.bgp;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CountDownLatch;

/**
 * Class to decode the message received.
 */
public class BgpPeerFrameDecoderTest extends FrameDecoder {
    static final byte OPEN_MSG_TYPE = 0x1;
    static final byte KEEPALIVE_MSG_TYPE = 0x4;
    static final byte UPDATE_MSG_TYPE = 0x2;
    static final byte NOTIFICATION_MSG_TYPE = 0x3;
    static final int MINIMUM_COMMON_HEADER_LENGTH = 19;
    static final int MINIMUM_OPEN_MSG_LENGTH = 29;
    static final int MINIMUM_HEADER_MARKER_LENGTH = 16;
    static final int HEADER_AND_MSG_LEN = 18;

    private static final Logger log = LoggerFactory
            .getLogger(BgpPeerFrameDecoderTest.class);
    final CountDownLatch receivedOpenMessageLatch = new CountDownLatch(1);
    final CountDownLatch receivedKeepaliveMessageLatch = new CountDownLatch(1);
    final CountDownLatch receivedNotificationMessageLatch = new CountDownLatch(1);

    @Override
    protected Object decode(ChannelHandlerContext ctx,
                            Channel channel,
                            ChannelBuffer cb) throws Exception {

        if (cb.readableBytes() < MINIMUM_COMMON_HEADER_LENGTH) {
            log.debug("Error: Packet length is less then minimum length");
            return null;
        }

        byte[] marker = new byte[MINIMUM_HEADER_MARKER_LENGTH];
        cb.readBytes(marker);
        for (int i = 0; i < marker.length; i++) {
            if (marker[i] != (byte) 0xff) {
                log.debug("Error: Marker must be set all ones");
                ctx.getChannel().close();
                return null;
            }
        }

        short length = cb.readShort();
        if (length < MINIMUM_COMMON_HEADER_LENGTH) {
            log.debug("Error: Bad message length");
            ctx.getChannel().close();
            return null;
        }

        if (length != (cb.readableBytes() + HEADER_AND_MSG_LEN)) {
            log.debug("Error: Bad message length");
            ctx.getChannel().close();
            return null;
        }

        byte type = cb.readByte();
        int len = length - MINIMUM_COMMON_HEADER_LENGTH;

        ChannelBuffer message = cb.readBytes(len);

        switch (type) {
        case OPEN_MSG_TYPE:
            processBgpOpen(ctx, message);
            break;
        case UPDATE_MSG_TYPE:
            break;
        case NOTIFICATION_MSG_TYPE:
            processBgpNotification(ctx, message);
            break;
        case KEEPALIVE_MSG_TYPE:
            processBgpKeepalive(ctx, message);
            break;
        default:
            ctx.getChannel().close();
            return null;
        }

        return null;
    }

    /**
     * Processes BGP open message.
     *
     * @param ctx Channel handler context
     * @param message open message
     */
    private void processBgpOpen(ChannelHandlerContext ctx,
                                ChannelBuffer message) {
        int minLength =
            MINIMUM_OPEN_MSG_LENGTH - MINIMUM_COMMON_HEADER_LENGTH;
        if (message.readableBytes() < minLength) {
            log.debug("Error: Bad message length");
            ctx.getChannel().close();
            return;
        }

        message.readByte(); // read version
        message.readShort(); // read AS number
        message.readShort(); // read Hold timer
        message.readInt(); // read BGP Identifier
        // Optional Parameters
        int optParamLen = message.readUnsignedByte();
        if (message.readableBytes() < optParamLen) {
            log.debug("Error: Bad message length");
            ctx.getChannel().close();
            return;
        }
        message.readBytes(optParamLen);

        // Open message received
        receivedOpenMessageLatch.countDown();
    }

    /**
     * Processes BGP keepalive message.
     *
     * @param ctx Channel handler context
     * @param message keepalive message
     */
    private void processBgpKeepalive(ChannelHandlerContext ctx,
                                     ChannelBuffer message) {

        // Keepalive message received
        receivedKeepaliveMessageLatch.countDown();
    }

    /**
     * Processes BGP notification message.
     *
     * @param ctx Channel handler context
     * @param message notification message
     */
    private void processBgpNotification(ChannelHandlerContext ctx,
                                     ChannelBuffer message) {
        byte[] data;
        message.readByte(); //read error code
        message.readByte(); // read error sub code
        if (message.readableBytes() > 0) {
            data = new byte[message.readableBytes()];
            message.readBytes(data, 0, message.readableBytes());
        }

        // Notification message received
        receivedNotificationMessageLatch.countDown();
    }
}