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

package org.onosproject.artemis.impl.moas;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.Beta;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.onlab.packet.IpAddress;
import org.onosproject.artemis.impl.objects.ArtemisMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * MOAS Server channel handler.
 */
@Sharable
@Beta
public class MoasServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log =
            LoggerFactory.getLogger(MoasServerHandler.class);

    private MoasServerController controller;

    MoasServerHandler(MoasServerController controller) {
        this.controller = controller;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        final SocketAddress address = ctx.channel().remoteAddress();
        if (!(address instanceof InetSocketAddress)) {
            log.warn("Invalid client connection. MOAS is identified based on IP");
            ctx.close();
            return;
        }

        final InetSocketAddress inetAddress = (InetSocketAddress) address;
        final String host = inetAddress.getHostString();
        log.info("New client connected to the Server: {}", host);

        controller.deviceAgent.addMoas(IpAddress.valueOf(host), ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
        ByteBuf in = (ByteBuf) msg;
        String strMsg = in.toString(io.netty.util.CharsetUtil.US_ASCII);

        ObjectMapper mapper = new ObjectMapper();
        ArtemisMessage actObj = mapper.readValue(strMsg, ArtemisMessage.class);

        controller.packetAgent.processMoasPacket(actObj, ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("exceptionCaught()", cause);
        ctx.close();
    }

}
