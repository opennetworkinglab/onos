/*
 * Copyright 2015-present Open Networking Foundation
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

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.openflow.OfMessageAdapter;
import org.projectfloodlight.openflow.protocol.OFType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for the OpenFlow message encoder.
 */
public class OFMessageEncoderTest {

    private ByteBuf buf;
    static class MockOfMessage extends OfMessageAdapter {
        static int nextId = 1;
        final int id;

        MockOfMessage() {
            super(OFType.ERROR);
            id = nextId++;
        }

        @Override
        public void writeTo(ByteBuf byteBuf) {
            String message = "message" + Integer.toString(id) + " ";
            byteBuf.writeBytes(message.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Before
    public void setUp() {
        buf = ByteBufAllocator.DEFAULT.buffer();
    }

    @After
    public void tearDown() {
        buf.release();
    }

    @Test
    public void testEncode() throws Exception {
        OFMessageEncoder encoder = OFMessageEncoder.getInstance();
        MockOfMessage message1 = new MockOfMessage();
        encoder.encode(null, Collections.singletonList(message1), buf);

        assertThat(buf.isReadable(), Matchers.is(true));
        byte[] channelBytes = new byte[buf.readableBytes()];
        buf.readBytes(channelBytes);
        String expectedListMessage = "message1 ";
        assertThat(channelBytes, is(expectedListMessage.getBytes()));
    }
}
