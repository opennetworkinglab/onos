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

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.junit.Test;
import org.onosproject.openflow.OfMessageAdapter;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;

import com.google.common.collect.ImmutableList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for the OpenFlow message encoder.
 */
public class OFMessageEncoderTest {

    static class MockOfMessage extends OfMessageAdapter {
        static int nextId = 1;
        final int id;

        MockOfMessage() {
            super(OFType.ERROR);
            id = nextId++;
        }

        @Override
        public void writeTo(ChannelBuffer channelBuffer) {
            String message = "message" + Integer.toString(id) + " ";
            channelBuffer.writeBytes(message.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Tests that encoding a non-list returns the object specified.
     *
     * @throws Exception on exception in the encoder
     */
    @Test
    public void testNoList() throws Exception {
        OFMessageEncoder encoder = new OFMessageEncoder();
        MockOfMessage message = new MockOfMessage();
        OFMessage returnedMessage =
                (OFMessage) encoder.encode(null, null, message);
        assertThat(message, is(returnedMessage));
    }

    /**
     * Tests that encoding a list returns the proper encoded payload.
     *
     * @throws Exception on exception in the encoder
     */
    @Test
    public void testList() throws Exception {
        OFMessageEncoder encoder = new OFMessageEncoder();
        MockOfMessage message1 = new MockOfMessage();
        MockOfMessage message2 = new MockOfMessage();
        MockOfMessage message3 = new MockOfMessage();
        List<MockOfMessage> messages = ImmutableList.of(message1, message2, message3);
        ChannelBuffer returnedChannel =
                (ChannelBuffer) encoder.encode(null, null, messages);
        assertThat(returnedChannel, notNullValue());
        byte[] channelBytes = returnedChannel.array();
        String expectedListMessage = "message1 message2 message3 ";
        String listMessage =
                (new String(channelBytes, StandardCharsets.UTF_8))
                        .substring(0, expectedListMessage.length());
        assertThat(listMessage, is(expectedListMessage));
    }
}
