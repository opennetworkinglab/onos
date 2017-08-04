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

package org.onosproject.openflow.controller.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * Trigger a timeout if a switch fails to complete handshake soon enough.
 */
public class HandshakeTimeoutHandler
    extends ChannelDuplexHandler {

    private static final Logger log = getLogger(HandshakeTimeoutHandler.class);

    final OFChannelHandler channelHandler;
    final long timeoutMillis;
    volatile long deadline;

    public HandshakeTimeoutHandler(OFChannelHandler channelHandler,
                                   long timeoutSeconds) {
        super();
        this.channelHandler = channelHandler;
        this.timeoutMillis = TimeUnit.SECONDS.toMillis(timeoutSeconds);
        this.deadline = System.currentTimeMillis() + timeoutMillis;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (timeoutMillis > 0) {
            // set Handshake deadline
            deadline = System.currentTimeMillis() + timeoutMillis;
        }
        super.channelActive(ctx);
    }

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        checkTimeout(ctx);
        super.read(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg,
                      ChannelPromise promise)
            throws Exception {
        checkTimeout(ctx);
        super.write(ctx, msg, promise);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx,
                                   Object evt)
            throws Exception {

        // expecting idle event
        checkTimeout(ctx);
        super.userEventTriggered(ctx, evt);
    }

    void checkTimeout(ChannelHandlerContext ctx) {
        if (channelHandler.isHandshakeComplete()) {
            // handshake complete, Handshake monitoring timeout no-longer needed
            ctx.channel().pipeline().remove(this);
            return;
        }

        if (!ctx.channel().isActive()) {
            return;
        }

        if (System.currentTimeMillis() > deadline) {
            log.info("Handshake time out {}", channelHandler);
            ctx.fireExceptionCaught(new HandshakeTimeoutException());
        }
    }
}
