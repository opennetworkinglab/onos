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

package org.onosproject.routing.fpm;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.onosproject.routing.fpm.protocol.FpmHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Session handler for FPM protocol.
 */
public class FpmSessionHandler extends SimpleChannelHandler {

    private static Logger log = LoggerFactory.getLogger(FpmSessionHandler.class);

    private final FpmListener fpmListener;

    private Channel channel;

    /**
     * Class constructor.
     *
     * @param fpmListener listener for FPM messages
     */
    public FpmSessionHandler(FpmListener fpmListener) {
        this.fpmListener = checkNotNull(fpmListener);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        FpmHeader fpmMessage = (FpmHeader) e.getMessage();
        fpmListener.fpmMessage(fpmMessage);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        log.error("Exception thrown while handling FPM message", e.getCause());
        if (channel != null) {
            channel.close();
        }
        handleDisconnect();
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
        if (!fpmListener.peerConnected(ctx.getChannel().getRemoteAddress())) {
            log.error("Received new FPM connection while already connected");
            ctx.getChannel().close();
            return;
        }

        channel = ctx.getChannel();
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
        handleDisconnect();
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
    }

    private void handleDisconnect() {
        fpmListener.peerDisconnected(channel.getRemoteAddress());
        channel = null;
    }
}
