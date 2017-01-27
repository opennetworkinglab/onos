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

package org.onosproject.routing.fpm;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.onosproject.routing.fpm.protocol.FpmHeader;

/**
 * Frame decoder for FPM connections.
 */
public class FpmFrameDecoder extends FrameDecoder {

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer)
            throws Exception {

        if (!channel.isConnected()) {
            return null;
        }

        if (buffer.readableBytes() < FpmHeader.FPM_HEADER_LENGTH) {
            return null;
        }

        buffer.markReaderIndex();

        short version = buffer.readUnsignedByte();
        short type = buffer.readUnsignedByte();
        int length = buffer.readUnsignedShort();

        buffer.resetReaderIndex();

        if (buffer.readableBytes() < length) {
            // Not enough bytes to read a whole message
            return null;
        }

        byte[] fpmMessage = new byte[length];
        buffer.readBytes(fpmMessage);

        return FpmHeader.decode(fpmMessage, 0, fpmMessage.length);
    }
}
