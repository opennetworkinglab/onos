/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.xmpp.core.ctl.handlers;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import io.netty.util.CharsetUtil;
import org.onosproject.xmpp.core.stream.XmppStreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.Packet;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encodes XMPP message and writes XML data to channel.
 */
public class XmppEncoder extends MessageToByteEncoder {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        byte[] bytes = null;

        if (msg instanceof XmppStreamEvent) {
            XmppStreamEvent streamEvent = (XmppStreamEvent) msg;
            logger.info("SENDING: {}", streamEvent.toXml());
            bytes = streamEvent.toXml().getBytes(CharsetUtil.UTF_8);
        }

        if (msg instanceof Packet) {
            Packet pkt = (Packet) msg;
            logger.info("SENDING /n, {}", pkt.toString());
            bytes = pkt.toXML().getBytes(CharsetUtil.UTF_8);
        }

        out.writeBytes(checkNotNull(bytes));
    }
}
