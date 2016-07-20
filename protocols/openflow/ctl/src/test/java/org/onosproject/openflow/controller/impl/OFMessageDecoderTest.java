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


import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.onosproject.core.netty.ChannelAdapter;
import org.onosproject.openflow.ChannelHandlerContextAdapter;
import org.projectfloodlight.openflow.protocol.OFHello;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for the OpenFlow message decoder.
 */
public class OFMessageDecoderTest {

    static class ConnectedChannel extends ChannelAdapter {
        @Override
        public boolean isConnected() {
            return true;
        }
    }

    private ChannelBuffer getHelloMessageBuffer() {
        // OFHello, OF version 1, xid of 0, total of 8 bytes
        byte[] messageData = {0x1, 0x0, 0x0, 0x8, 0x0, 0x0, 0x0, 0x0};
        ChannelBuffer channelBuffer = ChannelBuffers.dynamicBuffer();
        channelBuffer.writeBytes(messageData);
        return channelBuffer;
    }

    /**
     * Tests decoding a message on a closed channel.
     *
     * @throws Exception when an exception is thrown from the decoder
     */
    @Test
    public void testDecodeNoChannel() throws Exception {
        OFMessageDecoder decoder = new OFMessageDecoder();
        ChannelBuffer channelBuffer = getHelloMessageBuffer();
        Object message =
                decoder.decode(new ChannelHandlerContextAdapter(),
                               new ChannelAdapter(),
                               channelBuffer);
        assertThat(message, nullValue());
    }

    /**
     * Tests decoding a message.
     *
     * @throws Exception when an exception is thrown from the decoder
     */
    @Test
    public void testDecode() throws Exception {
        OFMessageDecoder decoder = new OFMessageDecoder();
        ChannelBuffer channelBuffer = getHelloMessageBuffer();
        Object message =
                decoder.decode(new ChannelHandlerContextAdapter(),
                               new ConnectedChannel(),
                               channelBuffer);
        assertThat(message, notNullValue());
        assertThat(message, instanceOf(OFHello.class));
    }

}
