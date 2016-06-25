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
package org.onosproject.pcepio.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Assert;
import org.junit.Test;
import org.onosproject.pcepio.exceptions.PcepOutOfBoundMessageException;
import org.onosproject.pcepio.exceptions.PcepParseException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;

public class PcepKeepaliveMsgTest {

    /**
     * Common header for keep alive message.
     */
    @Test
    public void keepaliveMessageTest1() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] keepaliveMsg = new byte[] {0x20, 0x02, 0x00, 0x04 };

        byte[] testKeepaliveMsg = {0 };
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(keepaliveMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);
        assertThat(message, instanceOf(PcepKeepaliveMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testKeepaliveMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testKeepaliveMsg = new byte[iReadLen];
        buf.readBytes(testKeepaliveMsg, 0, iReadLen);

        Assert.assertThat(testKeepaliveMsg, is(keepaliveMsg));
    }
}
