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
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.onlab.packet.Ip4Address;

import java.util.concurrent.CountDownLatch;

/**
 * Class for handling the decoding of the BGP messages at the remote
 * BGP peer session.
 */
class TestBgpPeerFrameDecoder extends FrameDecoder {
    final BgpSessionInfo remoteInfo = new BgpSessionInfo();

    final CountDownLatch receivedOpenMessageLatch = new CountDownLatch(1);
    final CountDownLatch receivedKeepaliveMessageLatch = new CountDownLatch(1);

    @Override
    protected Object decode(ChannelHandlerContext ctx,
                            Channel channel,
                            ChannelBuffer buf) throws Exception {
        // Test for minimum length of the BGP message
        if (buf.readableBytes() < BgpConstants.BGP_HEADER_LENGTH) {
            // No enough data received
            return null;
        }

        //
        // Mark the current buffer position in case we haven't received
        // the whole message.
        //
        buf.markReaderIndex();

        //
        // Read and check the BGP message Marker field: it must be all ones
        //
        byte[] marker = new byte[BgpConstants.BGP_HEADER_MARKER_LENGTH];
        buf.readBytes(marker);
        for (int i = 0; i < marker.length; i++) {
            if (marker[i] != (byte) 0xff) {
                // ERROR: Connection Not Synchronized. Close the channel.
                ctx.getChannel().close();
                return null;
            }
        }

        //
        // Read and check the BGP message Length field
        //
        int length = buf.readUnsignedShort();
        if ((length < BgpConstants.BGP_HEADER_LENGTH) ||
            (length > BgpConstants.BGP_MESSAGE_MAX_LENGTH)) {
            // ERROR: Bad Message Length. Close the channel.
            ctx.getChannel().close();
            return null;
        }

        //
        // Test whether the rest of the message is received:
        // So far we have read the Marker (16 octets) and the
        // Length (2 octets) fields.
        //
        int remainingMessageLen =
            length - BgpConstants.BGP_HEADER_MARKER_LENGTH - 2;
        if (buf.readableBytes() < remainingMessageLen) {
            // No enough data received
            buf.resetReaderIndex();
            return null;
        }

        //
        // Read the BGP message Type field, and process based on that type
        //
        int type = buf.readUnsignedByte();
        remainingMessageLen--;      // Adjust after reading the type
        ChannelBuffer message = buf.readBytes(remainingMessageLen);

        //
        // Process the remaining of the message based on the message type
        //
        switch (type) {
        case BgpConstants.BGP_TYPE_OPEN:
            processBgpOpen(ctx, message);
            break;
        case BgpConstants.BGP_TYPE_UPDATE:
            // NOTE: Not used as part of the test, because ONOS does not
            // originate UPDATE messages.
            break;
        case BgpConstants.BGP_TYPE_NOTIFICATION:
            // NOTE: Not used as part of the testing (yet)
            break;
        case BgpConstants.BGP_TYPE_KEEPALIVE:
            processBgpKeepalive(ctx, message);
            break;
        default:
            // ERROR: Bad Message Type. Close the channel.
            ctx.getChannel().close();
            return null;
        }

        return null;
    }

    /**
     * Processes BGP OPEN message.
     *
     * @param ctx the Channel Handler Context.
     * @param message the message to process.
     */
    private void processBgpOpen(ChannelHandlerContext ctx,
                                ChannelBuffer message) {
        int minLength =
            BgpConstants.BGP_OPEN_MIN_LENGTH - BgpConstants.BGP_HEADER_LENGTH;
        if (message.readableBytes() < minLength) {
            // ERROR: Bad Message Length. Close the channel.
            ctx.getChannel().close();
            return;
        }

        //
        // Parse the OPEN message
        //
        remoteInfo.setBgpVersion(message.readUnsignedByte());
        remoteInfo.setAsNumber(message.readUnsignedShort());
        remoteInfo.setHoldtime(message.readUnsignedShort());
        remoteInfo.setBgpId(Ip4Address.valueOf((int) message.readUnsignedInt()));
        // Optional Parameters
        int optParamLen = message.readUnsignedByte();
        if (message.readableBytes() < optParamLen) {
            // ERROR: Bad Message Length. Close the channel.
            ctx.getChannel().close();
            return;
        }
        message.readBytes(optParamLen);             // NOTE: data ignored

        // BGP OPEN message successfully received
        receivedOpenMessageLatch.countDown();
    }

    /**
     * Processes BGP KEEPALIVE message.
     *
     * @param ctx the Channel Handler Context.
     * @param message the message to process.
     */
    private void processBgpKeepalive(ChannelHandlerContext ctx,
                                     ChannelBuffer message) {
        if (message.readableBytes() + BgpConstants.BGP_HEADER_LENGTH !=
            BgpConstants.BGP_KEEPALIVE_EXPECTED_LENGTH) {
            // ERROR: Bad Message Length. Close the channel.
            ctx.getChannel().close();
            return;
        }
        // BGP KEEPALIVE message successfully received
        receivedKeepaliveMessageLatch.countDown();
    }
}
