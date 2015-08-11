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

import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepFactories;
import org.onosproject.pcepio.protocol.PcepKeepaliveMsg;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.onosproject.pcepio.protocol.PcepMessageReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PcepKeepaliveMsgTest {

    protected static final Logger log = LoggerFactory.getLogger(PcepKeepaliveMsgTest.class);

    @Before
    public void startUp() {

    }

    @After
    public void tearDown() {

    }

    @Test
    public void keepaliveMessageTest1() throws PcepParseException {

        byte[] keepaliveMsg = new byte[] {0x20, 0x02, 0x00, 0x04 };

        byte[] testKeepaliveMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(keepaliveMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepKeepaliveMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testKeepaliveMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testKeepaliveMsg = new byte[iReadLen];
            buf.readBytes(testKeepaliveMsg, 0, iReadLen);

            if (Arrays.equals(keepaliveMsg, testKeepaliveMsg)) {
                Assert.assertArrayEquals(keepaliveMsg, testKeepaliveMsg);
                log.debug("keepaliveMsg are equal :" + keepaliveMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }
}
