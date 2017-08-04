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
package org.onosproject.lisp.ctl.impl;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.junit.Test;
import org.onosproject.lisp.msg.protocols.LispMapNotify;
import org.onosproject.lisp.msg.protocols.LispMapRegister;
import org.onosproject.lisp.msg.protocols.LispMapReply;
import org.onosproject.lisp.msg.protocols.LispMapRequest;

import java.net.InetSocketAddress;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * Tests for LISP message decoder.
 */
public class LispMessageDecoderTest {

    private static final int TYPE_SHIFT_BIT = 4;
    private static final byte MAP_REQUEST = 1;
    private static final byte MAP_REPLY = 2;
    private static final byte MAP_REGISTER = 3;
    private static final byte MAP_NOTIFY = 4;


    private ByteBuf getLispMapRequestBuffer() {
        ByteBuf buffer = Unpooled.buffer();

        // specify message type
        buffer.writeByte(MAP_REQUEST << TYPE_SHIFT_BIT);

        // fill up message payload
        // second byte denotes the number of RLOCs
        byte[] messageData = {0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0};
        byte[] eidData = {0x0, 0x1, 0x0, 0x0, 0x0, 0x0};
        byte[] rlocData = {0x0, 0x1, 0x0, 0x0, 0x0, 0x0};
        byte[] replyRecord = {0x0, 0x0, 0x0, 0x1};
        buffer.writeBytes(messageData);
        buffer.writeBytes(eidData);
        buffer.writeBytes(rlocData);
        buffer.writeBytes(replyRecord);
        return buffer;
    }

    private ByteBuf getLispMapReplyBuffer() {
        ByteBuf buffer = Unpooled.buffer();

        // specify message type
        buffer.writeByte(MAP_REPLY << TYPE_SHIFT_BIT);

        // fill up message payload
        byte[] messageData = {0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0};
        buffer.writeBytes(messageData);
        return buffer;
    }

    private ByteBuf getLispMapRegisterBuffer() {
        ByteBuf buffer = Unpooled.buffer();

        // specify message type
        buffer.writeByte(MAP_REGISTER << TYPE_SHIFT_BIT);

        // fill up message payload
        byte[] messageData = {0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0};
        byte[] keyId = {0x0, 0x1};

        // assume that we have auth data which has 2 bytes size
        byte[] authDataLength = {0x0, 0x2};
        byte[] authData = {0x0, 0x0};

        buffer.writeBytes(messageData);
        buffer.writeBytes(keyId);
        buffer.writeBytes(authDataLength);
        buffer.writeBytes(authData);
        return buffer;
    }

    private ByteBuf getLispMapNotifyBuffer() {
        ByteBuf buffer = Unpooled.buffer();

        // specify message type
        buffer.writeByte(MAP_NOTIFY << TYPE_SHIFT_BIT);

        // fill up message payload
        byte[] messageData = {0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0};
        byte[] keyId = {0x0, 0x1};

        // assume that we have auth data which has 2 bytes size
        byte[] authDataLength = {0x0, 0x2};
        byte[] authData = {0x0, 0x0};

        buffer.writeBytes(messageData);
        buffer.writeBytes(keyId);
        buffer.writeBytes(authDataLength);
        buffer.writeBytes(authData);

        return buffer;
    }

    private DatagramPacket convToDatagram(ByteBuf byteBuf) {
        InetSocketAddress source = new InetSocketAddress(0);
        return new DatagramPacket(byteBuf, source);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeNoChannel() throws Exception {
        LispMessageDecoder decoder = new LispMessageDecoder();

        List<Object> list = Lists.newArrayList();
        decoder.decode(new ChannelHandlerContextAdapter(),
                convToDatagram(Unpooled.buffer()), list);
    }

    @Test
    public void testDecode() throws Exception {
        LispMessageDecoder decoder = new LispMessageDecoder();
        DatagramPacket requestBuff = convToDatagram(getLispMapRequestBuffer());
        DatagramPacket replyBuff = convToDatagram(getLispMapReplyBuffer());
        DatagramPacket registerBuff = convToDatagram(getLispMapRegisterBuffer());
        DatagramPacket notifyBuff = convToDatagram(getLispMapNotifyBuffer());

        List<Object> list = Lists.newArrayList();
        decoder.decode(new ChannelHandlerContextAdapter(), requestBuff, list);
        decoder.decode(new ChannelHandlerContextAdapter(), replyBuff, list);
        decoder.decode(new ChannelHandlerContextAdapter(), registerBuff, list);
        decoder.decode(new ChannelHandlerContextAdapter(), notifyBuff, list);

        assertThat(list.size(), is(4));
        assertThat(list.get(0), is(instanceOf(LispMapRequest.class)));
        assertThat(list.get(1), is(instanceOf(LispMapReply.class)));
        assertThat(list.get(2), is(instanceOf(LispMapRegister.class)));
        assertThat(list.get(3), is(instanceOf(LispMapNotify.class)));
    }
}
