/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onlab.netty;

import static com.google.common.base.Preconditions.checkState;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpAddress.Version;
import org.onosproject.store.cluster.messaging.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

/**
 * Decoder for inbound messages.
 */
public class MessageDecoder extends ReplayingDecoder<DecoderState> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final int correctPreamble;
    private long messageId;
    private int preamble;
    private Version ipVersion;
    private IpAddress senderIp;
    private int senderPort;
    private int messageTypeLength;
    private String messageType;
    private int contentLength;

    public MessageDecoder(int correctPreamble) {
        super(DecoderState.READ_MESSAGE_PREAMBLE);
        this.correctPreamble = correctPreamble;
    }

    @Override
    protected void decode(
            ChannelHandlerContext context,
            ByteBuf buffer,
            List<Object> out) throws Exception {

        switch (state()) {
        case READ_MESSAGE_PREAMBLE:
            preamble = buffer.readInt();
            if (preamble != correctPreamble) {
                throw new IllegalStateException("This message had an incorrect preamble.");
            }
            checkpoint(DecoderState.READ_MESSAGE_ID);
        case READ_MESSAGE_ID:
            messageId = buffer.readLong();
            checkpoint(DecoderState.READ_SENDER_IP_VERSION);
        case READ_SENDER_IP_VERSION:
            ipVersion = buffer.readByte() == 0x0 ? Version.INET : Version.INET6;
            checkpoint(DecoderState.READ_SENDER_IP);
        case READ_SENDER_IP:
            byte[] octets = new byte[IpAddress.byteLength(ipVersion)];
            buffer.readBytes(octets);
            senderIp = IpAddress.valueOf(ipVersion, octets);
            checkpoint(DecoderState.READ_SENDER_PORT);
        case READ_SENDER_PORT:
            senderPort = buffer.readInt();
            checkpoint(DecoderState.READ_MESSAGE_TYPE_LENGTH);
        case READ_MESSAGE_TYPE_LENGTH:
            messageTypeLength = buffer.readInt();
            checkpoint(DecoderState.READ_MESSAGE_TYPE);
        case READ_MESSAGE_TYPE:
            byte[] messageTypeBytes = new byte[messageTypeLength];
            buffer.readBytes(messageTypeBytes);
            messageType = new String(messageTypeBytes, Charsets.UTF_8);
            checkpoint(DecoderState.READ_CONTENT_LENGTH);
        case READ_CONTENT_LENGTH:
            contentLength = buffer.readInt();
            checkpoint(DecoderState.READ_CONTENT);
        case READ_CONTENT:
            //TODO Perform a sanity check on the size before allocating
            byte[] payload = new byte[contentLength];
            buffer.readBytes(payload);
            InternalMessage message = new InternalMessage(messageId,
                    new Endpoint(senderIp, senderPort),
                    messageType,
                    payload);
            out.add(message);
            checkpoint(DecoderState.READ_MESSAGE_PREAMBLE);
            break;
         default:
            checkState(false, "Must not be here");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        log.error("Exception inside channel handling pipeline.", cause);
        context.close();
    }
}
