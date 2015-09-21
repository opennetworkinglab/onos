/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.bgp.controller.impl;

import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.onosproject.bgpio.protocol.BGPMessage;
import org.onlab.util.HexDump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encode an bgp message for output into a ChannelBuffer, for use in a
 * netty pipeline.
 */
public class BGPMessageEncoder extends OneToOneEncoder {
    protected static final Logger log = LoggerFactory.getLogger(BGPMessageEncoder.class);

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        log.debug("BGPMessageEncoder::encode");
        if (!(msg instanceof List)) {
            log.debug("Invalid msg.");
            return msg;
        }

        @SuppressWarnings("unchecked")
        List<BGPMessage> msglist = (List<BGPMessage>) msg;

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        log.debug("SENDING MESSAGE");
        for (BGPMessage pm : msglist) {
            pm.writeTo(buf);
        }

        HexDump.dump(buf);

        return buf;
    }
}