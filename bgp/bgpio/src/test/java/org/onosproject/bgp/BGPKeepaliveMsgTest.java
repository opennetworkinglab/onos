/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.bgp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.onosproject.bgpio.exceptions.BGPParseException;
import org.onosproject.bgpio.protocol.BGPFactories;
import org.onosproject.bgpio.protocol.BGPKeepaliveMsg;
import org.onosproject.bgpio.protocol.BGPMessage;
import org.onosproject.bgpio.protocol.BGPMessageReader;
import org.onosproject.bgpio.types.BGPHeader;

/**
 * Test case for BGP KEEPALIVE Message.
 */
public class BGPKeepaliveMsgTest {

    /**
     * This test case checks BGP Keepalive message.
     */
    @Test
    public void keepaliveMessageTest1() throws BGPParseException {

        // BGP KEEPALIVE Message
        byte[] keepaliveMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                          0x00, 0x13, 0x04};

        byte[] testKeepaliveMsg;
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(keepaliveMsg);

        BGPMessageReader<BGPMessage> reader = BGPFactories.getGenericReader();
        BGPMessage message;
        BGPHeader bgpHeader = new BGPHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BGPKeepaliveMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testKeepaliveMsg = new byte[readLen];
        buf.readBytes(testKeepaliveMsg, 0, readLen);

        assertThat(testKeepaliveMsg, is(keepaliveMsg));
    }
}
