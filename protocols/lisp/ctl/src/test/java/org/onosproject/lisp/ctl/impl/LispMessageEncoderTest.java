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
package org.onosproject.lisp.ctl.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.junit.Test;
import org.onosproject.lisp.msg.protocols.LispType;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for LISP message encoder.
 */
public class LispMessageEncoderTest {

    static class MockLispMessage extends LispMessageAdapter {
        LispType type;

        public MockLispMessage(LispType type) {
            super(type);
            this.type = type;
        }

        @Override
        public void writeTo(ByteBuf buffer) {
            String message = "LISP message [" + type.toString() + "] ";
            buffer.writeBytes(message.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testEncodeOneEntry() throws Exception {
        LispMessageEncoder encoder = new LispMessageEncoder();
        MockLispMessage message = new MockLispMessage(LispType.LISP_MAP_REQUEST);

        List<DatagramPacket> list = Lists.newArrayList();
        encoder.encode(null, message, list);

        assertThat(list, notNullValue());

        String expected = "LISP message [LISP_MAP_REQUEST] ";
        String returned = new String(list.get(0).content().array(),
                        StandardCharsets.UTF_8).substring(0, expected.length());
        assertThat(returned, is(expected));
    }

    @Test
    public void testEncode() throws Exception {
        LispMessageEncoder encoder = new LispMessageEncoder();
        MockLispMessage request = new MockLispMessage(LispType.LISP_MAP_REQUEST);
        MockLispMessage reply = new MockLispMessage(LispType.LISP_MAP_REPLY);
        MockLispMessage register = new MockLispMessage(LispType.LISP_MAP_REGISTER);
        MockLispMessage notify = new MockLispMessage(LispType.LISP_MAP_NOTIFY);

        ByteBuf buff = Unpooled.buffer();
        List<DatagramPacket> list = Lists.newArrayList();
        List<MockLispMessage> messages = ImmutableList.of(request, reply, register, notify);
        encoder.encode(null, messages, list);

        list.forEach(p -> {
            byte[] tmp = new byte[p.content().writerIndex()];
            p.content().readBytes(tmp);
            buff.writeBytes(tmp);
        });

        assertThat(buff, notNullValue());

        StringBuilder expBuilder = new StringBuilder();
        expBuilder.append("LISP message [LISP_MAP_REQUEST] ");
        expBuilder.append("LISP message [LISP_MAP_REPLY] ");
        expBuilder.append("LISP message [LISP_MAP_REGISTER] ");
        expBuilder.append("LISP message [LISP_MAP_NOTIFY] ");

        String expected = expBuilder.toString();
        String returned = new String(buff.array(),
                        StandardCharsets.UTF_8).substring(0, expected.length());
        assertThat(returned, is(expected));
    }
}
