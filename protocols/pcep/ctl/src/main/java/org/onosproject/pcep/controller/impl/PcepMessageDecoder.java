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
package org.onosproject.pcep.controller.impl;

import java.util.LinkedList;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.onosproject.pcepio.exceptions.PcepOutOfBoundMessageException;
import org.onosproject.pcepio.protocol.PcepFactories;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.onosproject.pcepio.protocol.PcepMessageReader;
import org.onosproject.pcepio.util.HexDump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decode an pcep message from a Channel, for use in a netty pipeline.
 */
public class PcepMessageDecoder extends FrameDecoder {

    protected static final Logger log = LoggerFactory.getLogger(PcepMessageDecoder.class);

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel,
            ChannelBuffer buffer) throws Exception {
        log.debug("Message received.");
        if (!channel.isConnected()) {
            log.info("Channel is not connected.");
            // In testing, I see decode being called AFTER decode last.
            // This check avoids that from reading corrupted frames
            return null;
        }

        HexDump.pcepHexDump(buffer);

        // Buffer can contain multiple messages, also may contain out of bound message.
        // Read the message one by one from buffer and parse it. If it encountered out of bound message,
        // then mark the reader index and again take the next chunk of messages from the channel
        // and parse again from the marked reader index.
        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        List<PcepMessage> msgList = (List<PcepMessage>) ctx.getAttachment();

        if (msgList == null) {
            msgList = new LinkedList<>();
        }

        try {
            while (buffer.readableBytes() > 0) {
                buffer.markReaderIndex();
                PcepMessage message = reader.readFrom(buffer);
                msgList.add(message);
            }
            ctx.setAttachment(null);
            return msgList;
        } catch (PcepOutOfBoundMessageException e) {
            log.debug("PCEP message decode error");
            buffer.resetReaderIndex();
            buffer.discardReadBytes();
            ctx.setAttachment(msgList);
        }
        return null;
    }
}
