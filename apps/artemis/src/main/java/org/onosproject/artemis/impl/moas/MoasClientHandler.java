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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.Beta;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.artemis.ArtemisPacketProcessor;
import org.onosproject.artemis.impl.objects.ArtemisMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * MOAS Client channel handler.
 */
@Sharable
@Beta
public class MoasClientHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log =
            LoggerFactory.getLogger(MoasClientHandler.class);

    private IpAddress localIp;
    private IpPrefix localPrefix;
    private ArtemisPacketProcessor packetProcessor;

    MoasClientHandler(IpAddress localIp, IpPrefix localPrefix, ArtemisPacketProcessor packetProcessor) {
        this.localIp = localIp;
        this.packetProcessor = packetProcessor;
        this.localPrefix = localPrefix;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Connected to server {}", ctx.channel().remoteAddress());

        ArtemisMessage message = new ArtemisMessage();
        message.setType(ArtemisMessage.Type.INITIATE_FROM_CLIENT);
        message.setLocalIp(localIp.toString());
        message.setLocalPrefix(localPrefix.toString());

        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonInString = mapper.writeValueAsString(message);
            ByteBuf buffer = Unpooled.copiedBuffer(jsonInString, CharsetUtil.UTF_8);
            ctx.writeAndFlush(buffer);
        } catch (JsonProcessingException e) {
            log.warn("channelActive()", e);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
        ByteBuf in = (ByteBuf) msg;
        String strMsg = in.toString(io.netty.util.CharsetUtil.US_ASCII);

        ObjectMapper mapper = new ObjectMapper();
        ArtemisMessage actObj = mapper.readValue(strMsg, ArtemisMessage.class);

        packetProcessor.processMoasPacket(actObj, ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("exceptionCaught()", cause);
        ctx.close();
    }

}
