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
package org.onosproject.isis.controller.impl;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.onlab.packet.MacAddress;
import org.onosproject.isis.controller.IsisMessage;
import org.onosproject.isis.io.isispacket.IsisMessageReader;
import org.onosproject.isis.io.util.IsisConstants;
import org.onosproject.isis.io.util.IsisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Decodes an ISIS message from a Channel, for use in a netty pipeline.
 */
public class IsisMessageDecoder extends FrameDecoder {

    private static final Logger log = LoggerFactory.getLogger(IsisMessageDecoder.class);

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        log.debug("IsisMessageDecoder::Message received <:> length {}", buffer.readableBytes());
        if (!channel.isConnected()) {
            log.info("Channel is not connected.");
            return null;
        }
        IsisMessageReader messageReader = new IsisMessageReader();
        List<IsisMessage> isisMessageList = new LinkedList<>();
        int dataLength = buffer.readableBytes();
        while (buffer.readableBytes() >= IsisConstants.MINIMUM_FRAME_LEN) {
            ChannelBuffer payload = buffer.readBytes(IsisConstants.MINIMUM_FRAME_LEN);
            ChannelBuffer ethernetHeader = payload.readBytes(IsisUtil.ETHER_HEADER_LEN);
            //Read the Source MAC address from ethernet header at the 6th position
            MacAddress sourceMac = getSourceMac(ethernetHeader);
            //Strip 17 byte ethernet header and get the ISIS data buffer
            ChannelBuffer isisDataBuffer = payload.readBytes(payload.readableBytes());
            int readableBytes = isisDataBuffer.readableBytes();
            IsisMessage message = messageReader.readFromBuffer(isisDataBuffer);
            //Last 7 bytes is metadata. ie. interface MAC address and interface index.
            if (message != null) {
                if (isisDataBuffer.readableBytes() >= IsisConstants.METADATA_LEN) {
                    //Sets the source MAC
                    message.setSourceMac(sourceMac);
                    isisDataBuffer.readerIndex(readableBytes - IsisConstants.METADATA_LEN);
                    log.debug("IsisMessageDecoder::Reading metadata <:> length {}", isisDataBuffer.readableBytes());
                    byte[] macBytes = new byte[IsisUtil.SIX_BYTES];
                    isisDataBuffer.readBytes(macBytes, 0, IsisUtil.SIX_BYTES);
                    MacAddress macAddress = MacAddress.valueOf(macBytes);
                    int interfaceIndex = isisDataBuffer.readByte();
                    message.setInterfaceMac(macAddress);
                    message.setInterfaceIndex(interfaceIndex);
                }
                isisMessageList.add(message);
            }
        }
        return (!isisMessageList.isEmpty()) ? isisMessageList : null;
    }

    /**
     * Gets the source MAC address from the ethernet header.
     *
     * @param ethHeader ethernet header bytes
     * @return MAC address of the source router
     */
    private MacAddress getSourceMac(ChannelBuffer ethHeader) {
        //Source MAC is at position 6 to 11 (6 bytes)
        ethHeader.skipBytes(IsisUtil.SIX_BYTES);
        MacAddress sourceMac = MacAddress.valueOf(ethHeader.readBytes(IsisUtil.SIX_BYTES).array());

        return sourceMac;
    }
}