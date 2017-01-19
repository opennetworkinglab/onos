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
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfMessage;
import org.onosproject.ospf.protocol.ospfpacket.OspfMessageReader;
import org.onosproject.ospf.protocol.util.OspfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Decodes an OSPF message from a Channel, for use in a netty pipeline.
 */
public class OspfMessageDecoder extends FrameDecoder {

    private static final Logger log = LoggerFactory.getLogger(OspfMessageDecoder.class);

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        log.debug("OspfMessageDecoder::Message received <:> length {}", buffer.readableBytes());
        if (!channel.isConnected()) {
            log.info("Channel is not connected.");
            return null;
        }
        OspfMessageReader messageReader = new OspfMessageReader();
        List<OspfMessage> ospfMessageList = new LinkedList<>();
        while (buffer.readableBytes() >= OspfUtil.MINIMUM_FRAME_LEN) {
            ChannelBuffer ospfDataBuffer = buffer.readBytes(OspfUtil.MINIMUM_FRAME_LEN);
            int readableBytes = ospfDataBuffer.readableBytes();
            OspfMessage message = messageReader.readFromBuffer(ospfDataBuffer);
            if (message != null) {
                if (ospfDataBuffer.readableBytes() >= OspfUtil.METADATA_LEN) {
                    ospfDataBuffer.readerIndex(readableBytes - OspfUtil.METADATA_LEN);
                    log.debug("IsisMessageDecoder::Reading metadata <:> length {}", ospfDataBuffer.readableBytes());


                    int interfaceIndex = ospfDataBuffer.readByte();
                    byte[] sourceIpBytes = new byte[OspfUtil.FOUR_BYTES];
                    ospfDataBuffer.readBytes(sourceIpBytes, 0, OspfUtil.FOUR_BYTES);
                    Ip4Address sourceIP = Ip4Address.valueOf(sourceIpBytes);

                    message.setSourceIp(sourceIP);
                    message.setInterfaceIndex(interfaceIndex);
                }
                ospfMessageList.add(message);
            }
        }
        return (!ospfMessageList.isEmpty()) ? ospfMessageList : null;
    }
}