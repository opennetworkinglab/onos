/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.openflow.controller.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.EncoderException;
import static org.slf4j.LoggerFactory.getLogger;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.slf4j.Logger;

/**
 * Encode an openflow message for output into a netty channel, for use in a
 * netty pipeline.
 */
@Sharable
public final class OFMessageEncoder extends ChannelOutboundHandlerAdapter {

    private static final Logger log = getLogger(OFMessageEncoder.class);

    private static final OFMessageEncoder INSTANCE = new OFMessageEncoder();

    public static OFMessageEncoder getInstance() {
        return INSTANCE;
    }

    private OFMessageEncoder() {}

    protected final void encode(ChannelHandlerContext ctx,
                          Iterable<OFMessage> msgs,
                          ByteBuf out) throws Exception {

        msgs.forEach(msg -> msg.writeTo(out));
    }

    // MessageToByteEncoder without dependency to TypeParameterMatcher
    @Override
    public void write(ChannelHandlerContext ctx,
                      Object msg,
                      ChannelPromise promise) throws Exception {

        ByteBuf buf = null;
        try {
            if (msg instanceof Iterable) {
                @SuppressWarnings("unchecked")
                Iterable<OFMessage> ofmsgs =  (Iterable<OFMessage>) msg;
                buf = ctx.alloc().ioBuffer();

                encode(ctx, ofmsgs, buf);

                if (buf.isReadable()) {
                    ctx.write(buf, promise);
                } else {
                    log.warn("NOTHING WAS WRITTEN for {}", msg);
                    buf.release();
                    ctx.write(Unpooled.EMPTY_BUFFER, promise);
                }
                buf = null;

            } else {
                log.warn("Attempted to encode unexpected message: {}", msg);
                ctx.write(msg, promise);
            }
        } catch (EncoderException e) {
            log.error("EncoderException handling {}", msg, e);
            throw e;
        } catch (Throwable e) {
            log.error("Exception handling {}", msg, e);
            throw new EncoderException(e);
        } finally {
            if (buf != null) {
                buf.release();
            }
        }
    }

}
