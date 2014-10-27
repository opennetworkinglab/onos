/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Encode InternalMessage out into a byte buffer.
 */
@Sharable
public class MessageEncoder extends MessageToByteEncoder<InternalMessage> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // onosiscool in ascii
    public static final byte[] PREAMBLE = "onosiscool".getBytes();
    public static final int HEADER_VERSION = 1;
    public static final int SERIALIZER_VERSION = 1;


    private static final KryoSerializer SERIALIZER = new KryoSerializer();

    @Override
    protected void encode(
            ChannelHandlerContext context,
            InternalMessage message,
            ByteBuf out) throws Exception {

        // write version
        out.writeInt(HEADER_VERSION);

        // write preamble
        out.writeBytes(PREAMBLE);

        byte[] payload = SERIALIZER.encode(message);

        // write payload length
        out.writeInt(payload.length);

        // write payloadSerializer version
        out.writeInt(SERIALIZER_VERSION);

        // write payload.
        out.writeBytes(payload);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        log.error("Exception inside channel handling pipeline.", cause);
        context.close();
    }
}
