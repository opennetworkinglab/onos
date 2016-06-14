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

import org.easymock.EasyMock;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for OspfMessageDecoder.
 */
public class OspfMessageDecoderTest {

    private final byte[] hellopacket = {0, 0, 0, 0, 0, 2, 1, 0, 44, -64, -88, -86, 8, 0, 0, 0, 1, 39, 59,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, 0, 0, 10, 2, 1, 0, 0, 0,
            40, -64, -88, -86, 8, 0, 0, 0, 0};
    private final byte[] ddpacket = {0, 0, 0, 0, 2, 2, 0, 32, -64, -88, -86, 8, 0, 0, 0, 1, -96, 82,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, -36, 2, 7, 65, 119, -87, 126};
    private final byte[] ddpacket44 = {0, 0, 0, 0, 2, 2, 0, 10, -64, -88};
    private final byte[] lsAckpacket = {0, 0, 0, 0, 2, 5, 0, 44, -64, -88, -86, 8, 0, 0, 0, 1, -30, -12,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 14, 16, 2, 1, -64, -88, -86, 2, -64,
            -88, -86, 2, -128, 0, 0, 1, 74, -114, 0, 48};
    private final byte[] lsUpdatePacket = {0, 0, 0, 0, 2, 4, 0, 76, -64, -88, -86, 3, 0, 0, 0, 1, 7, 111,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 14, 16, 2, 1, -64, -88,
            -86, 2, -64, -88, -86, 2, -128, 0, 0, 1, 74, -114, 0, 48, 2, 0, 0, 2,
            -64, -88, -86, 0, -1, -1, -1, 0, 3, 0, 0, 10, -64, -88, -86, 0, -1, -1, -1, 0, 3, 0, 0, 10};
    private final byte[] lsRequestPacket = {0, 0, 0, 0, 2, 3, 0, 36, -64, -88, -86, 3, 0, 0, 0, 1, -67, -57,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -64, -88, -86, 8, -64, -88, -86, 8};
    private OspfMessageDecoder ospfMessageDecoder;
    private ChannelHandlerContext ctx;
    private Channel channel;
    private SocketAddress socketAddress;
    private ChannelBuffer channelBuffer;

    @Before
    public void setUp() throws Exception {
        ospfMessageDecoder = new OspfMessageDecoder();
    }

    @After
    public void tearDown() throws Exception {
        ospfMessageDecoder = null;
        channel = null;
        socketAddress = null;
        channelBuffer = null;
    }

    /**
     * Tests decode() method.
     */
    @Test
    public void testDecode() throws Exception {
        channel = EasyMock.createMock(Channel.class);
        socketAddress = InetSocketAddress.createUnresolved("127.0.0.1", 7000);
        channelBuffer = ChannelBuffers.copiedBuffer(hellopacket);
        ospfMessageDecoder.decode(ctx, channel, channelBuffer);
        assertThat(ospfMessageDecoder, is(notNullValue()));
    }
}