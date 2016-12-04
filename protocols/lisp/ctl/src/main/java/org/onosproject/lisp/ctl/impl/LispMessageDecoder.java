/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.lisp.ctl.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.onosproject.lisp.msg.protocols.LispMessage;
import org.onosproject.lisp.msg.protocols.LispMessageReader;
import org.onosproject.lisp.msg.protocols.LispMessageReaderFactory;

import java.util.List;

/**
 * Decode a LISP message from a ByteBuffer, for use in a netty pipeline.
 */
public class LispMessageDecoder extends MessageToMessageDecoder<DatagramPacket> {

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg,
                          List<Object> list) throws Exception {
        ByteBuf byteBuf = msg.content();
        LispMessageReader reader = LispMessageReaderFactory.getReader(byteBuf);
        LispMessage message = (LispMessage) reader.readFrom(byteBuf);
        message.configSender(msg.sender());
        list.add(message);
    }
}
