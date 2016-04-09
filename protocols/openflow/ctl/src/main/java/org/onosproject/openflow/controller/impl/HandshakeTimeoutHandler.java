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

import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;

/**
 * Trigger a timeout if a switch fails to complete handshake soon enough.
 */
public class HandshakeTimeoutHandler
    extends SimpleChannelUpstreamHandler {
    static final HandshakeTimeoutException EXCEPTION =
            new HandshakeTimeoutException();

    final OFChannelHandler channelHandler;
    final Timer timer;
    final long timeoutNanos;
    volatile Timeout timeout;

    public HandshakeTimeoutHandler(OFChannelHandler channelHandler,
                                   Timer timer,
                                   long timeoutSeconds) {
        super();
        this.channelHandler = channelHandler;
        this.timer = timer;
        this.timeoutNanos = TimeUnit.SECONDS.toNanos(timeoutSeconds);

    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
        if (timeoutNanos > 0) {
            timeout = timer.newTimeout(new HandshakeTimeoutTask(ctx),
                                       timeoutNanos, TimeUnit.NANOSECONDS);
        }
        ctx.sendUpstream(e);
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
        if (timeout != null) {
            timeout.cancel();
            timeout = null;
        }
    }

    private final class HandshakeTimeoutTask implements TimerTask {

        private final ChannelHandlerContext ctx;

        HandshakeTimeoutTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run(Timeout t) throws Exception {
            if (t.isCancelled()) {
                return;
            }

            if (!ctx.getChannel().isOpen()) {
                return;
            }
            if (!channelHandler.isHandshakeComplete()) {
                Channels.fireExceptionCaught(ctx, EXCEPTION);
            }
        }
    }
}
