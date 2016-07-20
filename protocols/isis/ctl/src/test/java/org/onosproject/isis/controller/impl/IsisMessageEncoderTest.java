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

import com.google.common.primitives.Bytes;
import org.easymock.EasyMock;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.isis.io.util.IsisUtil;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for IsisMessageEncoder.
 */
public class IsisMessageEncoderTest {

    private final String id = "127.0.0.1";
    private final byte[] hello = {
            -125, 20, 1, 0, 17, 1, 0, 0,
            2, 51, 51, 51, 51, 51, 51, 0, 100, 5, -39, -126, 1, 4, 3,
            73, 0, 0, -127, 1, -52, -124, 4, -64, -88, 56, 102
    };
    private IsisMessageEncoder isisMessageEncoder;
    private ChannelHandlerContext ctx;
    private Channel channel;
    private SocketAddress socketAddress;
    private ChannelBuffer channelBuffer;

    @Before
    public void setUp() throws Exception {
        channel = EasyMock.createMock(Channel.class);
        isisMessageEncoder = new IsisMessageEncoder();
    }

    @After
    public void tearDown() throws Exception {
        channel = null;
        isisMessageEncoder = null;
    }

    /**
     * Tests encode() method.
     */
    @Test
    public void testEncode() throws Exception {
        socketAddress = InetSocketAddress.createUnresolved(id, 7000);
        byte[] array = IsisUtil.getPaddingTlvs(hello.length);
        channelBuffer = ChannelBuffers.copiedBuffer(Bytes.concat(hello, array));
        assertThat(isisMessageEncoder.encode(ctx, channel, channelBuffer.array()),
                   is(notNullValue()));
    }
}