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
 * Test case for BGP Route Refresh Message.
 */
public class BgpRouteRefreshMsgTest {

    /**
     * This test case checks BGP Route Refresh message.
     */
    @Test
    public void routeRefreshMessageTest1() throws BgpParseException {

        // BGP RouteRefresh Message
        byte[] rrMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                          0x00, 0x17, 0x05, 0x00, 0x02, 0x00, 0x01};

        byte[] testRouteRefreshMsg;
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(rrMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpRouteRefreshMsg.class));

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testRouteRefreshMsg = new byte[readLen];
        buf.readBytes(testRouteRefreshMsg, 0, readLen);

        assertThat(testRouteRefreshMsg, is(rrMsg));
    }
}
