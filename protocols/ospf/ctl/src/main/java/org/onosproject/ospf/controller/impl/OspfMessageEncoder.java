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

package org.onosproject.ospf.controller.impl;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encodes an OSPF message for output into a ChannelBuffer, for use in a netty pipeline.
 */
public class OspfMessageEncoder extends OneToOneEncoder {

    private static final Logger log = LoggerFactory.getLogger(OspfMessageEncoder.class);

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {

        byte[] byteMsg = (byte[]) msg;
        log.debug("Encoding ospfMessage of length {}", byteMsg.length);
        ChannelBuffer channelBuffer = ChannelBuffers.buffer(byteMsg.length);
        channelBuffer.writeBytes(byteMsg);

        return channelBuffer;
    }
}
