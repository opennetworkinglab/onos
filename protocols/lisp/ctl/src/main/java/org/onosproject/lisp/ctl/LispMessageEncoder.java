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
package org.onosproject.lisp.ctl;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.onosproject.lisp.msg.protocols.LispMessage;

import java.util.List;

/**
 * Encode a LISP message for output into a ByteBuffer,
 * for use in a netty pipeline.
 */
public class LispMessageEncoder extends MessageToByteEncoder {

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (!(msg instanceof List)) {
            ((LispMessage) msg).writeTo(out);
            return;
        }

        List<LispMessage> msgList = (List<LispMessage>) msg;

        for (LispMessage message : msgList) {
            if (message != null) {
                message.writeTo(out);
            }
        }
    }
}
