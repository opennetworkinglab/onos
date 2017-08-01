/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.store.cluster.messaging.impl;

import com.google.common.base.Charsets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpAddress.Version;
import org.onosproject.core.HybridLogicalTime;
import org.onosproject.store.cluster.messaging.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkState;

/**
 * Decoder for inbound messages.
 */
public class MessageDecoder extends ReplayingDecoder<DecoderState> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Version ipVersion;
    private IpAddress senderIp;
    private int senderPort;

    private InternalMessage.Type type;
    private int preamble;
    private long logicalTime;
    private long logicalCounter;
    private long messageId;
    private int contentLength;
    private byte[] content;
    private int subjectLength;
    private String subject;
    private InternalReply.Status status;

    public MessageDecoder() {
        super(DecoderState.READ_SENDER_IP_VERSION);
    }

    @Override
    @SuppressWarnings("squid:S128") // suppress switch fall through warning
    protected void decode(
            ChannelHandlerContext context,
            ByteBuf buffer,
            List<Object> out) throws Exception {

        switch (state()) {
            case READ_SENDER_IP_VERSION:
                ipVersion = buffer.readByte() == 0x0 ? Version.INET : Version.INET6;
                checkpoint(DecoderState.READ_SENDER_IP);
                // FALLTHROUGH
            case READ_SENDER_IP:
                byte[] octets = new byte[IpAddress.byteLength(ipVersion)];
                buffer.readBytes(octets);
                senderIp = IpAddress.valueOf(ipVersion, octets);
                checkpoint(DecoderState.READ_SENDER_PORT);
                // FALLTHROUGH
            case READ_SENDER_PORT:
                senderPort = buffer.readInt();
                checkpoint(DecoderState.READ_TYPE);
                // FALLTHROUGH
            case READ_TYPE:
                type = InternalMessage.Type.forId(buffer.readByte());
                checkpoint(DecoderState.READ_PREAMBLE);
                // FALLTHROUGH
            case READ_PREAMBLE:
                preamble = buffer.readInt();
                checkpoint(DecoderState.READ_LOGICAL_TIME);
                // FALLTHROUGH
            case READ_LOGICAL_TIME:
                logicalTime = buffer.readLong();
                checkpoint(DecoderState.READ_LOGICAL_COUNTER);
                // FALLTHROUGH
            case READ_LOGICAL_COUNTER:
                logicalCounter = buffer.readLong();
                checkpoint(DecoderState.READ_MESSAGE_ID);
                // FALLTHROUGH
            case READ_MESSAGE_ID:
                messageId = buffer.readLong();
                checkpoint(DecoderState.READ_CONTENT_LENGTH);
                // FALLTHROUGH
            case READ_CONTENT_LENGTH:
                contentLength = buffer.readInt();
                checkpoint(DecoderState.READ_CONTENT);
                // FALLTHROUGH
            case READ_CONTENT:
                if (contentLength > 0) {
                    //TODO Perform a sanity check on the size before allocating
                    content = new byte[contentLength];
                    buffer.readBytes(content);
                } else {
                    content = new byte[0];
                }

                switch (type) {
                    case REQUEST:
                        checkpoint(DecoderState.READ_SUBJECT_LENGTH);
                        break;
                    case REPLY:
                        checkpoint(DecoderState.READ_STATUS);
                        break;
                    default:
                        checkState(false, "Must not be here");
                }
                break;
            default:
                break;
        }

        switch (type) {
            case REQUEST:
                switch (state()) {
                    case READ_SUBJECT_LENGTH:
                        subjectLength = buffer.readShort();
                        checkpoint(DecoderState.READ_SUBJECT);
                        // FALLTHROUGH
                    case READ_SUBJECT:
                        byte[] messageTypeBytes = new byte[subjectLength];
                        buffer.readBytes(messageTypeBytes);
                        subject = new String(messageTypeBytes, Charsets.UTF_8);
                        InternalRequest message = new InternalRequest(preamble,
                                new HybridLogicalTime(logicalTime, logicalCounter),
                                messageId,
                                new Endpoint(senderIp, senderPort),
                                subject,
                                content);
                        out.add(message);
                        checkpoint(DecoderState.READ_TYPE);
                        break;
                    default:
                        break;
                }
                break;
            case REPLY:
                switch (state()) {
                    case READ_STATUS:
                        status = InternalReply.Status.forId(buffer.readByte());
                        InternalReply message = new InternalReply(preamble,
                                new HybridLogicalTime(logicalTime, logicalCounter),
                                messageId,
                                content,
                                status);
                        out.add(message);
                        checkpoint(DecoderState.READ_TYPE);
                        break;
                    default:
                        break;
                }
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