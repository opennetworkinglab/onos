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
package org.onosproject.pcepio;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Assert;
import org.junit.Test;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepFactories;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.onosproject.pcepio.protocol.PcepMessageReader;
import org.onosproject.pcepio.protocol.PcepCloseMsg;

public class PcepCloseMsgTest {

    /**
     * Common header, reason to close.
     *
     * @throws PcepParseException while parsing the PCEP message.
     */
    @Test
    public void closeMessageTest1() throws PcepParseException {

        byte[] closeMsg = new byte[] {0x20, 0x07, 0x00, 0x0C,
                0x0f, 0x10, 0x00, 0x08, 0x00, 0x00, 0x00, 0x02 };

        byte[] testCloseMsg = {0 };
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(closeMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);
        Assert.assertTrue("PcepMessage is not instance of PcepCloseMsg", message instanceof PcepCloseMsg);

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testCloseMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testCloseMsg = new byte[readLen];
        buf.readBytes(testCloseMsg, 0, readLen);
        Assert.assertArrayEquals("PcTERpt messages are not equal", closeMsg, testCloseMsg);
    }
}
