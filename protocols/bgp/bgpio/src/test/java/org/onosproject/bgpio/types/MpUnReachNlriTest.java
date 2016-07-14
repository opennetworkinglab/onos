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
package org.onosproject.bgpio.types;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpFactories;
import org.onosproject.bgpio.protocol.BgpMessage;
import org.onosproject.bgpio.protocol.BgpMessageReader;
import org.onosproject.bgpio.protocol.ver4.BgpUpdateMsgVer4;

/**
 * Test for MP unreach NLRI encoding and decoding.
 */
public class MpUnReachNlriTest {

    /**
     * This testcase checks BGP update message.
     */
    @Test
    public void mpUnReachNlriTest() throws BgpParseException {

        // BGP flow spec  Message
        byte[] flowSpecMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, 0x00, 0x2b, 0x02, 0x00, 0x00, 0x00, 0x14,
                (byte) 0x90, 0x0f, 0x00, 0x10, 0x00, 0x01, (byte) 0x85,
                0x0c, 0x02, 0x20, (byte) 0xc0, (byte) 0xa8, 0x07, 0x36,
                0x03, (byte) 0x81, 0x67, 0x04, (byte) 0x81, 0x01};

        byte[] testFsMsg;
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(flowSpecMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsgVer4.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testFsMsg = new byte[readLen];
        buf.readBytes(testFsMsg, 0, readLen);

        assertThat(testFsMsg, is(flowSpecMsg));
    }
}
