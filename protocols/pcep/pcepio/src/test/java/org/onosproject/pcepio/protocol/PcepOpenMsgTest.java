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
import org.junit.Test;
import org.onosproject.pcepio.exceptions.PcepOutOfBoundMessageException;
import org.onosproject.pcepio.exceptions.PcepParseException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
/**
 * Test cases for PCEP OPEN Message.
 */
public class PcepOpenMsgTest {

    /**
     * This test case checks open object with STATEFUL-PCE-CAPABILITY, GMPLS-CAPABILITY-TLV,
     * PCECC-CAPABILITY-TLV in Pcep Open message.
     */
    @Test
    public void openMessageTest1() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] openMsg = new byte[] {0x20, 0x01, 0x00, 0x24, 0x01, 0x10, 0x00, 0x20, 0x20, 0x1e, 0x78, (byte) 0xbd,
                0x00, 0x10, 0x00, 0x04, 0x00, 0x00, 0x00, 0x0f, //STATEFUL-PCE-CAPABILITY
                0x00, 0x0e, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, //GMPLS-CAPABILITY-TLV
                (byte) 0xff, 0x07, 0x00, 0x04, 0x00, 0x00, 0x00, 0x07, //PCECC-CAPABILITY-TLV
        };

        byte[] testOpenMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepOpenMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testOpenMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);

        assertThat(testOpenMsg, is(openMsg));

    }

    /**
     * This test case checks open object with STATEFUL-PCE-CAPABILITY-TLV in Pcep Open message.
     */
    @Test
    public void openMessageTest2() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] openMsg = new byte[] {0x20, 0x01, 0x00, 0x14, // common header
                0x01, 0x10, 0x00, 0x10, // common object header
                0x20, 0x1E, 0x78, 0x01, // OPEN object
                0x00, 0x10, 0x00, 0x04, 0x00, 0x00, 0x00, 0x0f}; // STATEFUL-PCE-CAPABILITY
        byte[] testOpenMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepOpenMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testOpenMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);

        assertThat(testOpenMsg, is(openMsg));

    }

    /**
     * This test case checks open object with GmplsCapability tlv in Pcep Open message.
     */
    @Test
    public void openMessageTest3() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] openMsg = new byte[] {0x20, 0x01, 0x00, 0x14, // common header
                0x01, 0x10, 0x00, 0x10, // common object header
                0x20, 0x1E, 0x78, 0x01, // OPEN object
                0x00, 0x0e, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00}; //GMPLS-CAPABILITY-TLV

        byte[] testOpenMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepOpenMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testOpenMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);


        assertThat(testOpenMsg, is(openMsg));

    }

    /**
     * This test case checks open object with StatefulLspDbVer Tlv in Pcep Open message.
     */
    @Test
    public void openMessageTest4() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] openMsg = new byte[] {0x20, 0x01, 0x00, 0x18,
                0x01, 0x10, 0x00, 0x14, 0x20, 0x1e, 0x78, 0x20,
                0x00, 0x17, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02 }; //StatefulLspDbVerTlv

        byte[] testOpenMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepOpenMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testOpenMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);

        assertThat(testOpenMsg, is(openMsg));

    }

    /**
     * This test case checks open object with no tlv's in Pcep Open message.
     */
    @Test
    public void openMessageTest5() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] openMsg = new byte[] {0x20, 0x01, 0x00, 0x0C,
                0x01, 0x10, 0x00, 0x08, 0x20, 0x1e, 0x78, (byte) 0xbd }; // no Tlvs in open messsage

        byte[] testOpenMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepOpenMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testOpenMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);

        assertThat(testOpenMsg, is(openMsg));

    }

    /**
     * This test case checks open object with STATEFUL-PCE-CAPABILITY, GMPLS-CAPABILITY-TLV, PCECC-CAPABILITY-TLV
     * with I bit set in Pcep Open message.
     */
    @Test
    public void openMessageTest6() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] openMsg = new byte[] {0x20, 0x01, 0x00, 0x24, 0x01, 0x11, 0x00, 0x20, //p bit not set & i bit set
                0x20, 0x1e, 0x78, (byte) 0xbd,
                0x00, 0x10, 0x00, 0x04, 0x00, 0x00, 0x00, 0x0f, // STATEFUL-PCE-CAPABILITY
                0x00, 0x0e, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, //GMPLS-CAPABILITY-TLV
                (byte) 0xff, 0x07, 0x00, 0x04, 0x00, 0x00, 0x00, 0x07, //PCECC-CAPABILITY-TLV
        };

        byte[] testOpenMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepOpenMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testOpenMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);

        assertThat(testOpenMsg, is(openMsg));

    }

    /**
     * This test case checks open object with STATEFUL-PCE-CAPABILITY, GMPLS-CAPABILITY-TLV, PCECC-CAPABILITY-TLV
     * with P bit set in Pcep Open message.
     */
    @Test
    public void openMessageTest7() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] openMsg = new byte[] {0x20, 0x01, 0x00, 0x24, 0x01, 0x12, 0x00, 0x20, //p bit set & i bit not set
                0x20, 0x1e, 0x78, (byte) 0xbd,
                0x00, 0x10, 0x00, 0x04, 0x00, 0x00, 0x00, 0x0f, //STATEFUL-PCE-CAPABILITY
                0x00, 0x0e, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, //GMPLS-CAPABILITY-TLV
                (byte) 0xff, 0x07, 0x00, 0x04, 0x00, 0x00, 0x00, 0x07, //PCECC-CAPABILITY-TLV
        };

        byte[] testOpenMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepOpenMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testOpenMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);

        assertThat(testOpenMsg, is(openMsg));

    }

    /**
     * This test case checks open object with STATEFUL-PCE-CAPABILITY, GMPLS-CAPABILITY-TLV, PCECC-CAPABILITY-TLV
     * with P & I bits set in Pcep Open message.
     */
    @Test
    public void openMessageTest8() throws PcepParseException, PcepOutOfBoundMessageException {

        /* OPEN OBJECT (STATEFUL-PCE-CAPABILITY, GMPLS-CAPABILITY-TLV, PCECC-CAPABILITY-TLV)
        with p bit set & i bit set.
         */
        byte[] openMsg = new byte[] {0x20, 0x01, 0x00, 0x24, 0x01, 0x13, 0x00, 0x20, //p bit set & i bit set
                0x20, 0x1e, 0x78, (byte) 0xbd,
                0x00, 0x10, 0x00, 0x04, 0x00, 0x00, 0x00, 0x0f, //STATEFUL-PCE-CAPABILITY
                0x00, 0x0e, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, //GMPLS-CAPABILITY-TLV
                (byte) 0xff, 0x07, 0x00, 0x04, 0x00, 0x00, 0x00, 0x07, //PCECC-CAPABILITY-TLV
        };

        byte[] testOpenMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepOpenMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testOpenMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);

        assertThat(testOpenMsg, is(openMsg));

    }

    /**
     * This test case checks open object with STATEFUL-PCE-CAPABILITY, GMPLS-CAPABILITY-TLV, PCECC-CAPABILITY-TLV
     * with P & I bits set and invalid session id in Pcep Open message.
     */
    @Test
    public void openMessageTest9() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] openMsg = new byte[] {0x20, 0x01, 0x00, 0x24, 0x01, 0x13, 0x00, 0x20, //p bit set & i bit set
                0x20, 0x1e, 0x78, 0x00, //invalid sessionID
                0x00, 0x10, 0x00, 0x04, 0x00, 0x00, 0x00, 0x0f, //STATEFUL-PCE-CAPABILITY
                0x00, 0x0e, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, //GMPLS-CAPABILITY-TLV
                (byte) 0xff, 0x07, 0x00, 0x04, 0x00, 0x00, 0x00, 0x07, //PCECC-CAPABILITY-TLV
        };

        byte[] testOpenMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepOpenMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testOpenMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);


        assertThat(testOpenMsg, is(openMsg));

    }

    /**
     * This test case checks open object with STATEFUL-PCE-CAPABILITY, GMPLS-CAPABILITY-TLV
     * in Pcep Open message.
     */
    @Test
    public void openMessageTest10() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] openMsg = new byte[] {0x20, 0x01, 0x00, 0x1C, // common header
                0x01, 0x10, 0x00, 0x18, // common object header
                0x20, 0x05, 0x1E, 0x01, // OPEN object
                0x00, 0x10, 0x00, 0x04, // STATEFUL-PCE-CAPABILITY
                0x00, 0x00, 0x00, 0x05,
                0x00, 0x0E, 0x00, 0x04, // GMPLS-CAPABILITY-TLV
                0x00, 0x00, 0x00, 0x00};

        byte[] testOpenMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepOpenMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testOpenMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);

        assertThat(testOpenMsg, is(openMsg));

    }

    /**
     * This test case checks open object with STATEFUL-PCE-CAPABILITY, GMPLS-CAPABILITY-TLV,
     * PCECC-CAPABILITY-TLV, TED Capability TLV in Pcep Open message.
     */
    @Test
    public void openMessageTest11() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] openMsg = new byte[] {0x20, 0x01, 0x00, 0x2C, // common header
                0x01, 0x10, 0x00, 0x28, // common object header
                0x20, 0x05, 0x1E, 0x01, // OPEN object
                0x00, 0x10, 0x00, 0x04, // STATEFUL-PCE-CAPABILITY
                0x00, 0x00, 0x00, 0x05, 0x00, 0x0E, 0x00, 0x04, // GMPLS-CAPABILITY-TLV
                0x00, 0x00, 0x00, 0x00, (byte) 0xff, 0x07, 0x00, 0x04, // PCECC-CAPABILITY-TLV
                0x00, 0x00, 0x00, 0x03, (byte) 0xFF, (byte) 0x00, 0x00, 0x04, // LS Capability TLV
                0x00, 0x00, 0x00, 0x00 };

        byte[] testOpenMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepOpenMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testOpenMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);

        assertThat(testOpenMsg, is(openMsg));
    }

    /**
     * This test case checks open object with STATEFUL-PCE-CAPABILITY, GMPLS-CAPABILITY-TLV,
     * PCECC-CAPABILITY-TLV in Pcep Open message.
     */
    @Test
    public void openMessageTest12() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] openMsg = new byte[] {0x20, 0x01, 0x00, 0x24, // common header
                0x01, 0x10, 0x00, 0x20, // common object header
                0x20, 0x05, 0x1E, 0x01, // OPEN object
                0x00, 0x10, 0x00, 0x04, // STATEFUL-PCE-CAPABILITY
                0x00, 0x00, 0x00, 0x05, 0x00, 0x0E, 0x00, 0x04, // GMPLS-CAPABILITY-TLV
                0x00, 0x00, 0x00, 0x00, (byte) 0xff, 0x07, 0x00, 0x04, // PCECC-CAPABILITY-TLV
                0x00, 0x00, 0x00, 0x03};

        byte[] testOpenMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepOpenMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testOpenMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);

        assertThat(testOpenMsg, is(openMsg));

    }

    /**
     * This test case checks open object with STATEFUL-PCE-CAPABILITY, GMPLS-CAPABILITY-TLV
     * in Pcep Open message.
     */
    @Test
    public void openMessageTest13() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] openMsg = new byte[] {0x20, 0x01, 0x00, 0x1c, // common header
                0x01, 0x10, 0x00, 0x18, // common object header
                0x20, 0x05, 0x1E, 0x01, // OPEN object
                0x00, 0x10, 0x00, 0x04, // STATEFUL-PCE-CAPABILITY
                0x00, 0x00, 0x00, 0x05, 0x00, 0x0E, 0x00, 0x04, // GMPLS-CAPABILITY-TLV
                0x00, 0x00, 0x00, 0x00};

        byte[] testOpenMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepOpenMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testOpenMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);


        assertThat(testOpenMsg, is(openMsg));

    }

    /**
     * This test case checks open object with STATEFUL-PCE-CAPABILITY in Pcep Open message.
     */
    @Test
    public void openMessageTest14() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] openMsg = new byte[] {0x20, 0x01, 0x00, 0x14, // common header
                0x01, 0x10, 0x00, 0x10, // common object header
                0x20, 0x05, 0x1E, 0x01, // OPEN object
                0x00, 0x10, 0x00, 0x04, // STATEFUL-PCE-CAPABILITY
                0x00, 0x00, 0x00, 0x05};

        byte[] testOpenMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepOpenMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testOpenMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);

        assertThat(testOpenMsg, is(openMsg));

    }

    /**
     * This test case checks open object with no tlv Pcep Open message.
     */
    @Test
    public void openMessageTest15() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] openMsg = new byte[] {0x20, 0x01, 0x00, 0x0c, // common header
                0x01, 0x10, 0x00, 0x08, // common object header
                0x20, 0x05, 0x1E, 0x01 // OPEN object
        };

        byte[] testOpenMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepOpenMsg.class));

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testOpenMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);
        assertThat(testOpenMsg, is(openMsg));

    }

    /**
     * This test case checks open object with LSR id encoded.
     */
    @Test
    public void openMessageTest16() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] openMsg = new byte[] {0x20, 0x01, 0x00, 0x18, // common header
                0x01, 0x10, 0x00, 0x14, // common object header
                0x20, 0x05, 0x1E, 0x01, // OPEN object
                (byte) 0xFF, 0x05, 0x00, 0x08, // Node attribute TLV
                0x00, 0x11, 0x00, 0x04,  // PCEP-LS-IPv4-ROUTER-ID sub tlv
                0x02, 0x02, 0x02, 0x02
        };

        byte[] testOpenMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepOpenMsg.class));

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testOpenMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);
        assertThat(testOpenMsg, is(openMsg));

    }
}
