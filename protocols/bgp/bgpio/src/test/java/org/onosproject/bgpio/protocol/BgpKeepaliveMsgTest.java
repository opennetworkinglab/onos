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
package org.onosproject.bgpio.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.BgpHeader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;

/**
 * Test case for BGP KEEPALIVE Message.
 */
public class BgpKeepaliveMsgTest {

    /**
     * This test case checks BGP Keepalive message.
     */
    @Test
    public void keepaliveMessageTest1() throws BgpParseException {

        // BGP KEEPALIVE Message
        byte[] keepaliveMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                          0x00, 0x13, 0x04};

        byte[] testKeepaliveMsg;
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(keepaliveMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpKeepaliveMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testKeepaliveMsg = new byte[readLen];
        buf.readBytes(testKeepaliveMsg, 0, readLen);

        assertThat(testKeepaliveMsg, is(keepaliveMsg));
    }
}
