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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMessageReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Decodes inbound byte buffer from OpenFlow channel to OFMessage.
 */
public final class OFMessageDecoder extends ByteToMessageDecoder {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final OFMessageReader<OFMessage> reader = OFFactories.getGenericReader();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
            throws Exception {
        if (!ctx.channel().isActive()) {
            return;
        }

        try {
            OFMessage message = reader.readFrom(in);
            out.add(message);
        } catch (Throwable cause) {
            log.error("Failed decode OF message for {}", cause.getMessage());
        }
    }
}
