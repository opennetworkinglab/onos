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
package org.onosproject.ofagent.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encodes OFMessage to a byte buffer.
 */
public final class OFMessageEncoder extends MessageToByteEncoder<Iterable<OFMessage>> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void encode(ChannelHandlerContext ctx, Iterable<OFMessage> msgList, ByteBuf out)
            throws Exception {

        if (!ctx.channel().isActive()) {
            return;
        }

        if (msgList instanceof Iterable) {
            msgList.forEach(msg -> {
                try {
                    ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer();
                    msg.writeTo(byteBuf);

                    ctx.writeAndFlush(byteBuf);
                } catch (Exception e) {
                    log.error("error occured because of {}", e.getMessage());
                }
            });
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof EncoderException) {
            log.error("Connection closed because of EncoderException {}", cause.getMessage());
            ctx.close();
        } else {
            log.error("Exception occured while processing encoding because of {}", cause.getMessage());
            ctx.close();
        }
    }
}
