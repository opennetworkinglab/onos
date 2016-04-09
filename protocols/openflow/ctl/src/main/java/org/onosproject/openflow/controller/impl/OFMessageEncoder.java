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

package org.onosproject.openflow.controller.impl;

import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.projectfloodlight.openflow.protocol.OFMessage;

/**
 * Encode an openflow message for output into a ChannelBuffer, for use in a
 * netty pipeline.
 */
public class OFMessageEncoder extends OneToOneEncoder {

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel,
                            Object msg) throws Exception {
        if (!(msg instanceof List)) {
            return msg;
        }

        @SuppressWarnings("unchecked")
        List<OFMessage> msglist = (List<OFMessage>) msg;
        /* XXX S can't get length of OFMessage in loxigen's openflowj??
        int size = 0;
        for (OFMessage ofm : msglist) {
            size += ofm.getLengthU();
        }*/

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        for (OFMessage ofm : msglist) {
            if (ofm != null) {
                ofm.writeTo(buf);
            }
        }
        return buf;
    }

}
