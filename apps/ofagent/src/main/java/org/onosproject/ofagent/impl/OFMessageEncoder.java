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
package org.onosproject.ofagent.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.projectfloodlight.openflow.protocol.OFMessage;

/**
 * Encodes OFMessage to a byte buffer.
 */
public final class OFMessageEncoder extends MessageToByteEncoder<Iterable<OFMessage>> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Iterable<OFMessage> msgList, ByteBuf out) {
        if (!ctx.channel().isActive()) {
            return;
        }

        for (OFMessage ofm :  msgList) {
            ofm.writeTo(out);
        }
    }
}
