/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onlab.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.IOException;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpAddress.Version;
import org.onosproject.store.cluster.messaging.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

/**
 * Encode InternalMessage out into a byte buffer.
 */
@Sharable
public class MessageEncoder extends MessageToByteEncoder<InternalMessage> {

    private final int preamble;

    public MessageEncoder(int preamble) {
        super();
        this.preamble = preamble;
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void encode(
            ChannelHandlerContext context,
            InternalMessage message,
            ByteBuf out) throws Exception {

        out.writeInt(this.preamble);

        // write message id
        out.writeLong(message.id());

        Endpoint sender = message.sender();

        IpAddress senderIp = sender.host();
        if (senderIp.version() == Version.INET) {
            out.writeByte(0);
        } else {
            out.writeByte(1);
        }
        out.writeBytes(senderIp.toOctets());

        // write sender port
        out.writeInt(sender.port());

        byte[] messageTypeBytes = message.type().getBytes(Charsets.UTF_8);

        // write length of message type
        out.writeInt(messageTypeBytes.length);

        // write message type bytes
        out.writeBytes(messageTypeBytes);

        byte[] payload = message.payload();

        // write payload length
        out.writeInt(payload.length);

        // write payload.
        out.writeBytes(payload);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        if (cause instanceof IOException) {
            log.debug("IOException inside channel handling pipeline.", cause);
        } else {
            log.error("non-IOException inside channel handling pipeline.", cause);
        }
        context.close();
    }
}
