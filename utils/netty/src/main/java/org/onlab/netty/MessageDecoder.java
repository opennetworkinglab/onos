/*
 * Copyright 2014 Open Networking Laboratory
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

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decoder for inbound messages.
 */
public class MessageDecoder extends ReplayingDecoder<DecoderState> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final NettyMessagingService messagingService;

    private static final KryoSerializer SERIALIZER = new KryoSerializer();

    private int contentLength;

    public MessageDecoder(NettyMessagingService messagingService) {
        super(DecoderState.READ_HEADER_VERSION);
        this.messagingService = messagingService;
    }

    @Override
    protected void decode(
            ChannelHandlerContext context,
            ByteBuf buffer,
            List<Object> out) throws Exception {

        switch(state()) {
        case READ_HEADER_VERSION:
            int headerVersion = buffer.readInt();
            checkState(headerVersion == MessageEncoder.HEADER_VERSION, "Unexpected header version");
            checkpoint(DecoderState.READ_PREAMBLE);
        case READ_PREAMBLE:
            byte[] preamble = new byte[MessageEncoder.PREAMBLE.length];
            buffer.readBytes(preamble);
            checkState(Arrays.equals(MessageEncoder.PREAMBLE, preamble), "Message has wrong preamble");
            checkpoint(DecoderState.READ_CONTENT_LENGTH);
        case READ_CONTENT_LENGTH:
            contentLength = buffer.readInt();
            checkpoint(DecoderState.READ_SERIALIZER_VERSION);
        case READ_SERIALIZER_VERSION:
            int serializerVersion = buffer.readInt();
            checkState(serializerVersion == MessageEncoder.SERIALIZER_VERSION, "Unexpected serializer version");
            checkpoint(DecoderState.READ_CONTENT);
        case READ_CONTENT:
            InternalMessage message = SERIALIZER.decode(buffer.readBytes(contentLength).nioBuffer());
            message.setMessagingService(messagingService);
            out.add(message);
            checkpoint(DecoderState.READ_HEADER_VERSION);
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
