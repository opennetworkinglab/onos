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
package org.onosproject.ovsdb.controller.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import org.onosproject.ovsdb.rfc.jsonrpc.JsonReadContext;
import org.onosproject.ovsdb.rfc.utils.JsonRpcReaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decoder for inbound messages.
 */
public class MessageDecoder extends ByteToMessageDecoder {

    private final Logger log = LoggerFactory.getLogger(MessageDecoder.class);
    private final JsonReadContext context = new JsonReadContext();

    /**
     * Default constructor.
     */
    public MessageDecoder() {
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf,
                          List<Object> out) throws Exception {
        log.debug("Message decoder");
        JsonRpcReaderUtil.readToJsonNode(buf, out, context);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        log.error("Exception inside channel handling pipeline.", cause);
        context.close();
    }
}
