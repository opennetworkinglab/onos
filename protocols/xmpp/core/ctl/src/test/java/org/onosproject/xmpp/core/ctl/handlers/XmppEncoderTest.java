/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.xmpp.core.ctl.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.easymock.EasyMock;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Packet;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test class for XmppEncoder class.
 */
public class XmppEncoderTest {

    private XmppEncoder xmppEncoder;
    private ChannelHandlerContext channelHandlerContext;

    @Before
    public void setUp() throws Exception {
        xmppEncoder = new XmppEncoder();
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
    }

    @After
    public void tearDown() throws Exception {
        xmppEncoder = null;
    }

    @Test
    public void testEncode() throws Exception {
        Packet iq = new IQ();
        ByteBuf buffer = Unpooled.buffer();
        xmppEncoder.encode(channelHandlerContext, iq, buffer);
        assertThat(buffer.hasArray(), Matchers.is(true));
    }

}
