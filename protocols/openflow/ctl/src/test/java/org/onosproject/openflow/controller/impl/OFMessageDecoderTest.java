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


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.openflow.ChannelAdapter;
import org.onosproject.openflow.ChannelHandlerContextAdapter;
import org.projectfloodlight.openflow.protocol.OFHello;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the OpenFlow message decoder.
 */
public class OFMessageDecoderTest {

    private ByteBuf buf;

    private ByteBuf getHelloMessageBuffer() {
        // OFHello, OF version 1, xid of 0, total of 8 bytes
        byte[] messageData = {0x1, 0x0, 0x0, 0x8, 0x0, 0x0, 0x0, 0x0};
        buf.writeBytes(messageData);
        return buf;
    }

    @Before
    public void setUp() {
        buf = ByteBufAllocator.DEFAULT.buffer();
    }

    @After
    public void tearDown() {
        buf.release();
    }


    /**
     * Tests decoding a message on a closed channel.
     *
     * @throws Exception when an exception is thrown from the decoder
     */
    @Test
    public void testDecodeNoChannel() throws Exception {
        OFMessageDecoder decoder = OFMessageDecoder.getInstance();
        ByteBuf channelBuffer = getHelloMessageBuffer();
        List<Object> out = new ArrayList<>();
        decoder.decode(new ChannelHandlerContextAdapter(),
                       channelBuffer,
                       out);
        assertThat(out.size(), is(0));
    }

    /**
     * Tests decoding a message.
     *
     * @throws Exception when an exception is thrown from the decoder
     */
    @Test
    public void testDecode() throws Exception {
        OFMessageDecoder decoder = OFMessageDecoder.getInstance();
        ByteBuf channelBuffer = getHelloMessageBuffer();
        List<Object> out = new ArrayList<>();
        decoder.decode(new ActiveChannelHandlerContextAdapter(),
                       channelBuffer,
                       out);
        assertThat(out.size(), is(1));
        assertThat(out.get(0), instanceOf(OFHello.class));
    }

    public class ActiveChannelHandlerContextAdapter
            extends ChannelHandlerContextAdapter {

        @Override
        public Channel channel() {
            return new ChannelAdapter() {
                @Override
                public boolean isActive() {
                    return true;
                }
            };
        }

    }

}
