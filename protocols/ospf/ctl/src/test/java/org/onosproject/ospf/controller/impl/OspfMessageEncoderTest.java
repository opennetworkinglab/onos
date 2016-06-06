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
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by sdn on 13/1/16.
 */
public class OspfMessageEncoderTest {

    private final byte[] object = {2, 1, 0, 44, -64, -88, -86, 8, 0, 0, 0, 1, 39,
            59, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, 0, 0, 10, 2, 1, 0, 0,
            0, 40, -64, -88, -86, 8, 0, 0, 0, 0};
    private OspfMessageEncoder ospfMessageEncoder;
    private ChannelHandlerContext channelHandlerContext;
    private Channel channel;

    @Before
    public void setUp() throws Exception {
        ospfMessageEncoder = new OspfMessageEncoder();
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        channel = EasyMock.createMock(Channel.class);
    }

    @After
    public void tearDown() throws Exception {
        ospfMessageEncoder = null;
    }

    /**
     * Tests encode() method.
     */
    @Test
    public void testEncode() throws Exception {
        assertThat(ospfMessageEncoder.encode(channelHandlerContext, channel, object), is(notNullValue()));
    }
}