/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.openflow.controller.impl;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMessageReader;
import org.slf4j.Logger;

/**
 * Decode an openflow message from a netty channel, for use in a netty pipeline.
 */
public final class OFMessageDecoder extends ByteToMessageDecoder {

    private static final Logger log = getLogger(OFMessageDecoder.class);

    public static OFMessageDecoder getInstance() {
        // not Sharable
        return new OFMessageDecoder();
    }

    private OFMessageDecoder() {}


    @Override
    protected void decode(ChannelHandlerContext ctx,
                          ByteBuf byteBuf,
                          List<Object> out) throws Exception {

        if (!ctx.channel().isActive()) {
            // In testing, I see decode being called AFTER decode last.
            // This check avoids that from reading corrupted frames
            return;
        }

        // Note that a single call to readFrom results in reading a single
        // OFMessage from the channel buffer, which is passed on to, and processed
        // by, the controller (in OFChannelHandler).
        // This is different from earlier behavior (with the original openflowj),
        // where we parsed all the messages in the buffer, before passing on
        // a list of the parsed messages to the controller.
        // The performance *may or may not* not be as good as before.
        OFMessageReader<OFMessage> reader = OFFactories.getGenericReader();

        OFMessage message = reader.readFrom(byteBuf);
        while (message != null) {
            out.add(message);
            message = reader.readFrom(byteBuf);
        }
    }

}
