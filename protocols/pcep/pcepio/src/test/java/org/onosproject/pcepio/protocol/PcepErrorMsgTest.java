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
 * Test cases for PCEP ERROR Message.
 */
public class PcepErrorMsgTest {

    /**
     * This test case checks for
     * PCEP-ERROR Object, OPEN Object (STATEFUL-PCE-CAPABILITY, GMPLS-CAPABILITY-TLV,
     * PCECC-CAPABILITY-TLV, LS Capability TLV)
     * in PcepErrorMsg message.
     */
    @Test
    public void errorMessageTest1() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] errorMsg = new byte[]{0x20, 0x06, 0x00, 0x34, // common header
                0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x01, 0x01, 0x10, 0x00, 0x28, // OPEN object header
                0x20, 0x05, 0x1E, 0x01, // OPEN object
                0x00, 0x10, 0x00, 0x04, // STATEFUL-PCE-CAPABILITY
                0x00, 0x00, 0x00, 0x05, 0x00, 0x0E, 0x00, 0x04, // GMPLS-CAPABILITY-TLV
                0x00, 0x00, 0x00, 0x00, (byte) 0xff, 0x07, 0x00, 0x04, // PCECC-CAPABILITY-TLV
                0x00, 0x00, 0x00, 0x03, (byte) 0xFF, (byte) 0x00, 0x00, 0x04, // LS Capability TLV
                0x00, 0x00, 0x00, 0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errorMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testErrorMsg = {0};
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        assertThat(message, instanceOf(PcepErrorMsg.class));
        message.writeTo(buf);
        int iReadLen = buf.writerIndex();
        testErrorMsg = new byte[iReadLen];
        buf.readBytes(testErrorMsg, 0, iReadLen);

        assertThat(testErrorMsg, is(errorMsg));
    }

    /**
     * This test case checks for
     * PCEP-ERROR Object, PCEP-ERROR Object, OPEN Object (STATEFUL-PCE-CAPABILITY, GMPLS-CAPABILITY-TLV,
     * PCECC-CAPABILITY-TLV, LS Capability TLV)
     * in PcepErrorMsg message.
     */
    @Test
    public void errorMessageTest2() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] errorMsg = new byte[]{0x20, 0x06, 0x00, 0x3C, // common header
                0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x01, 0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x03, 0x01, 0x10, 0x00, 0x28, // OPEN object header
                0x20, 0x05, 0x1E, 0x01, // OPEN object
                0x00, 0x10, 0x00, 0x04, // STATEFUL-PCE-CAPABILITY
                0x00, 0x00, 0x00, 0x05, 0x00, 0x0E, 0x00, 0x04, // GMPLS-CAPABILITY-TLV
                0x00, 0x00, 0x00, 0x00, (byte) 0xff, 0x07, 0x00, 0x04, // PCECC-CAPABILITY-TLV
                0x00, 0x00, 0x00, 0x03, (byte) 0xFF, (byte) 0x00, 0x00, 0x04, // LS Capability TLV
                0x00, 0x00, 0x00, 0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errorMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testErrorMsg = {0};
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        assertThat(message, instanceOf(PcepErrorMsg.class));
        message.writeTo(buf);
        int iReadLen = buf.writerIndex();
        testErrorMsg = new byte[iReadLen];
        buf.readBytes(testErrorMsg, 0, iReadLen);

        assertThat(testErrorMsg, is(errorMsg));
    }

    /**
     * This test case checks for
     * PCEP-ERROR Object, PCEP-ERROR Object, OPEN Object (STATEFUL-PCE-CAPABILITY, GMPLS-CAPABILITY-TLV,
     * PCECC-CAPABILITY-TLV)
     * in PcepErrorMsg message.
     */
    @Test
    public void errorMessageTest3() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] errorMsg = new byte[]{0x20, 0x06, 0x00, 0x34, // common header
                0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x01, 0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x03, 0x01, 0x10, 0x00, 0x20, // OPEN object header
                0x20, 0x05, 0x1E, 0x01, // OPEN object
                0x00, 0x10, 0x00, 0x04, // STATEFUL-PCE-CAPABILITY
                0x00, 0x00, 0x00, 0x05, 0x00, 0x0E, 0x00, 0x04, // GMPLS-CAPABILITY-TLV
                0x00, 0x00, 0x00, 0x00, (byte) 0xff, 0x07, 0x00, 0x04, // PCECC-CAPABILITY-TLV
                0x00, 0x00, 0x00, 0x03};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errorMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testErrorMsg = {0};
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        assertThat(message, instanceOf(PcepErrorMsg.class));
        message.writeTo(buf);
        int iReadLen = buf.writerIndex();
        testErrorMsg = new byte[iReadLen];
        buf.readBytes(testErrorMsg, 0, iReadLen);

        assertThat(testErrorMsg, is(errorMsg));
    }

    /**
     * This test case checks for
     * PCEP-ERROR Object, PCEP-ERROR Object, OPEN Object (STATEFUL-PCE-CAPABILITY, GMPLS-CAPABILITY-TLV)
     * in PcepErrorMsg message.
     */
    @Test
    public void errorMessageTest4() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] errorMsg = new byte[]{0x20, 0x06, 0x00, 0x2c, // common header
                0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x01, 0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x03, 0x01, 0x10, 0x00, 0x18, // OPEN object header
                0x20, 0x05, 0x1E, 0x01, // OPEN object
                0x00, 0x10, 0x00, 0x04, // STATEFUL-PCE-CAPABILITY
                0x00, 0x00, 0x00, 0x05, 0x00, 0x0E, 0x00, 0x04, // GMPLS-CAPABILITY-TLV
                0x00, 0x00, 0x00, 0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errorMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testErrorMsg = {0};
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        assertThat(message, instanceOf(PcepErrorMsg.class));
        message.writeTo(buf);
        int iReadLen = buf.writerIndex();
        testErrorMsg = new byte[iReadLen];
        buf.readBytes(testErrorMsg, 0, iReadLen);

        assertThat(testErrorMsg, is(errorMsg));
    }

    /**
     * This test case checks for
     * PCEP-ERROR Object, PCEP-ERROR Object, OPEN Object (STATEFUL-PCE-CAPABILITY)
     * in PcepErrorMsg message.
     */
    @Test
    public void errorMessageTest5() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] errorMsg = new byte[]{0x20, 0x06, 0x00, 0x24, // common header
                0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x01, 0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x03, 0x01, 0x10, 0x00, 0x10, // OPEN object header
                0x20, 0x05, 0x1E, 0x01, // OPEN object
                0x00, 0x10, 0x00, 0x04, // STATEFUL-PCE-CAPABILITY
                0x00, 0x00, 0x00, 0x05};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errorMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testErrorMsg = {0};
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        assertThat(message, instanceOf(PcepErrorMsg.class));
        message.writeTo(buf);
        int iReadLen = buf.writerIndex();
        testErrorMsg = new byte[iReadLen];
        buf.readBytes(testErrorMsg, 0, iReadLen);

        assertThat(testErrorMsg, is(errorMsg));
    }

    /**
     * This test case checks for
     * PCEP-ERROR Object, PCEP-ERROR Object, OPEN Object
     * in PcepErrorMsg message.
     */
    @Test
    public void errorMessageTest6() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] errorMsg = new byte[]{0x20, 0x06, 0x00, 0x1C, // common header
                0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x01, 0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x03, 0x01, 0x10, 0x00, 0x08, // OPEN object header
                0x20, 0x05, 0x1E, 0x01 // OPEN object
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errorMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testErrorMsg = {0};
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        assertThat(message, instanceOf(PcepErrorMsg.class));
        message.writeTo(buf);
        int iReadLen = buf.writerIndex();
        testErrorMsg = new byte[iReadLen];
        buf.readBytes(testErrorMsg, 0, iReadLen);

        assertThat(testErrorMsg, is(errorMsg));
    }

    /**
     * This test case checks for
     * PCEP-ERROR Object, OPEN Object
     * in PcepErrorMsg message.
     */
    @Test
    public void errorMessageTest7() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] errorMsg = new byte[]{0x20, 0x06, 0x00, 0x14, // common header
                0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x01, 0x01, 0x10, 0x00, 0x08, // OPEN object header
                0x20, 0x05, 0x1E, 0x01 // OPEN object
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errorMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testErrorMsg = {0};
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        assertThat(message, instanceOf(PcepErrorMsg.class));
        message.writeTo(buf);
        int iReadLen = buf.writerIndex();
        testErrorMsg = new byte[iReadLen];
        buf.readBytes(testErrorMsg, 0, iReadLen);

        assertThat(testErrorMsg, is(errorMsg));
    }

    /**
     * This test case checks for
     * PCEP-ERROR Object, RP Object, PCEP-ERROR Object
     * in PcepErrorMsg message.
     */
    @Test
    public void errorMessageTest8() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] errorMsg = new byte[]{0x20, 0x06, 0x00, 0x20, // common header
                0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x01, 0x02, 0x10, 0x00, 0x0C, // RP Object Header
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x03};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errorMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testErrorMsg = {0};
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        assertThat(message, instanceOf(PcepErrorMsg.class));
        message.writeTo(buf);
        int iReadLen = buf.writerIndex();
        testErrorMsg = new byte[iReadLen];
        buf.readBytes(testErrorMsg, 0, iReadLen);

        assertThat(testErrorMsg, is(errorMsg));
    }

    /**
     * This test case checks for
     * PCEP-ERROR Object, PCEP-ERROR Object
     * in PcepErrorMsg message.
     */
    @Test
    public void errorMessageTest9() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] errorMsg = new byte[]{0x20, 0x06, 0x00, 0x14, // common header
                0x0D, 0x10, 0x00, 0x08, // PCEP-ERROR Object Header
                0x00, 0x00, 0x01, 0x01, 0x0D, 0x10, 0x00, 0x08, // PCEP-ERROR Object Header
                0x00, 0x00, 0x01, 0x01};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errorMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testErrorMsg = {0};
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        assertThat(message, instanceOf(PcepErrorMsg.class));
        message.writeTo(buf);
        int iReadLen = buf.writerIndex();
        testErrorMsg = new byte[iReadLen];
        buf.readBytes(testErrorMsg, 0, iReadLen);

        assertThat(testErrorMsg, is(errorMsg));
    }

    /**
     * This test case checks for
     * PCEP-ERROR Object, PCEP-ERROR Object
     * in PcepErrorMsg message.
     */
    @Test
    public void errorMessageTest10() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] errorMsg = new byte[]{0x20, 0x06, 0x00, 0x14, // common header
                0x0D, 0x10, 0x00, 0x08, // PCEP-ERROR Object Header
                0x00, 0x00, 0x01, 0x01, 0x0D, 0x10, 0x00, 0x08, // PCEP-ERROR Object Header
                0x00, 0x00, 0x01, 0x01};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errorMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testErrorMsg = {0};
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        assertThat(message, instanceOf(PcepErrorMsg.class));
        message.writeTo(buf);
        int iReadLen = buf.writerIndex();
        testErrorMsg = new byte[iReadLen];
        buf.readBytes(testErrorMsg, 0, iReadLen);

        assertThat(testErrorMsg, is(errorMsg));
    }

    /**
     * This test case checks for
     * LS Object, PCEP-ERROR Object
     * in PcepErrorMsg message.
     */
    @Test
    public void errorMessageTest11() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] errorMsg = new byte[]{0x20, 0x06, 0x00, 0x1C, // common header
                (byte) 0xE0, 0x13, 0x00, 0x10, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, // LS-ID
                0x00, 0x00, 0x00, 0x10,
                0x0D, 0x10, 0x00, 0x08, // PCEP-ERROR Object Header
                0x00, 0x00, 0x01, 0x01};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errorMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testErrorMsg = {0};
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        assertThat(message, instanceOf(PcepErrorMsg.class));
        message.writeTo(buf);
        int iReadLen = buf.writerIndex();
        testErrorMsg = new byte[iReadLen];
        buf.readBytes(testErrorMsg, 0, iReadLen);

        assertThat(testErrorMsg, is(errorMsg));
    }

    /**
     * This test case checks for
     * RP Object, PCEP-ERROR Object
     * in PcepErrorMsg message.
     */
    @Test
    public void errorMessageTest12() throws PcepParseException, PcepOutOfBoundMessageException {

        //RP Object, PCEP-ERROR Object
        byte[] errorMsg = new byte[]{0x20, 0x06, 0x00, 0x18, // common header
                0x02, 0x10, 0x00, 0x0C, // RP Object Header
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x0D, 0x10, 0x00, 0x08, // PCEP-ERROR Object Header
                0x00, 0x00, 0x01, 0x01};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errorMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testErrorMsg = {0};
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        assertThat(message, instanceOf(PcepErrorMsg.class));
        message.writeTo(buf);
        int iReadLen = buf.writerIndex();
        testErrorMsg = new byte[iReadLen];
        buf.readBytes(testErrorMsg, 0, iReadLen);

        assertThat(testErrorMsg, is(errorMsg));
    }

    /**
     * This test case checks for
     * RP Object, RP Object, PCEP-ERROR Object
     * in PcepErrorMsg message.
     */
    @Test
    public void errorMessageTest13() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] errorMsg = new byte[]{0x20, 0x06, 0x00, 0x24, // common header
                0x02, 0x10, 0x00, 0x0C, // RP Object Header
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x02, 0x10, 0x00, 0x0C, // RP Object Header
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04, 0x0D, 0x10, 0x00, 0x08, // PCEP-ERROR Object Header
                0x00, 0x00, 0x01, 0x01};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errorMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testErrorMsg = {0};
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        assertThat(message, instanceOf(PcepErrorMsg.class));
        message.writeTo(buf);
        int iReadLen = buf.writerIndex();
        testErrorMsg = new byte[iReadLen];
        buf.readBytes(testErrorMsg, 0, iReadLen);

        assertThat(testErrorMsg, is(errorMsg));
    }

    /**
     * This test case checks for
     * LS Object, LS Object, PCEP-ERROR Object
     * in PcepErrorMsg message.
     */
    @Test
    public void errorMessageTest14() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] errorMsg = new byte[]{0x20, 0x06, 0x00, 0x2C, // common header
                (byte) 0xE0, 0x10, 0x00, 0x10, // LS Object Header
                0x01, 0x00, 0x00, 0x03, // LS-ID
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x10,
                (byte) 0xE0, 0x10, 0x00, 0x10, // LS Object Header
                0x01, 0x00, 0x00, 0x03, // LS-ID
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x11,
                0x0D, 0x10, 0x00, 0x08, // PCEP-ERROR Object Header
                0x00, 0x00, 0x01, 0x01};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errorMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testErrorMsg = {0};
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        assertThat(message, instanceOf(PcepErrorMsg.class));
        message.writeTo(buf);
        int iReadLen = buf.writerIndex();
        testErrorMsg = new byte[iReadLen];
        buf.readBytes(testErrorMsg, 0, iReadLen);

        assertThat(testErrorMsg, is(errorMsg));
    }

    /**
     * This test case checks for
     * PCEP-ERROR Object, LS Object, PCEP-ERROR Object
     * in PcepErrorMsg message.
     */
    @Test
    public void errorMessageTest15() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] errorMsg = new byte[]{0x20, 0x06, 0x00, 0x24, // common header
                0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x01,
                (byte) 0xE0, 0x10, 0x00, 0x10, // LS Object Header
                0x01, 0x00, 0x00, 0x03, // LS-ID
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x10,
                0x0D, 0x10, 0x00, 0x08, // PCEP-ERROR Object Header
                0x00, 0x00, 0x01, 0x03};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errorMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testErrorMsg = {0};
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        assertThat(message, instanceOf(PcepErrorMsg.class));
        message.writeTo(buf);
        int iReadLen = buf.writerIndex();
        testErrorMsg = new byte[iReadLen];
        buf.readBytes(testErrorMsg, 0, iReadLen);

        assertThat(testErrorMsg, is(errorMsg));
    }

    /**
     * This test case checks for
     * PCEP-ERROR Object, RP Object, RP Object, PCEP-ERROR Object
     * in PcepErrorMsg message.
     */
    @Test
    public void errorMessageTest16() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] errorMsg = new byte[]{0x20, 0x06, 0x00, 0x2C, // common header
                0x0D, 0x10, 0x00, 0x08, // PCEP-ERROR Object Header
                0x00, 0x00, 0x01, 0x01, 0x02, 0x10, 0x00, 0x0C, // RP Object Header
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x02, 0x10, 0x00, 0x0C, // RP Object Header
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04, 0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x03};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errorMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testErrorMsg = {0};
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        assertThat(message, instanceOf(PcepErrorMsg.class));
        message.writeTo(buf);
        int iReadLen = buf.writerIndex();
        testErrorMsg = new byte[iReadLen];
        buf.readBytes(testErrorMsg, 0, iReadLen);

        assertThat(testErrorMsg, is(errorMsg));
    }

    /**
     * This test case checks for
     * PCEP-ERROR Object, LS Object, LS Object, PCEP-ERROR Object
     * in PcepErrorMsg message.
     */
    @Test
    public void errorMessageTest17() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] errorMsg = new byte[]{0x20, 0x06, 0x00, 0x34, // common header
                0x0D, 0x10, 0x00, 0x08, // PCEP-ERROR Object Header
                0x00, 0x00, 0x01, 0x01,
                (byte) 0xE0, 0x10, 0x00, 0x10, // LS Object Header
                0x01, 0x00, 0x00, 0x03, // LS-ID
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x10,
                (byte) 0xE0, 0x10, 0x00, 0x10, // LS Object Header
                0x01, 0x00, 0x00, 0x03, // LS-ID
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x11,
                0x0D, 0x10, 0x00, 0x08, // PCEP-ERROR Object Header
                0x00, 0x00, 0x01, 0x03};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errorMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testErrorMsg = {0};
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        assertThat(message, instanceOf(PcepErrorMsg.class));
        message.writeTo(buf);
        int iReadLen = buf.writerIndex();
        testErrorMsg = new byte[iReadLen];
        buf.readBytes(testErrorMsg, 0, iReadLen);

        assertThat(testErrorMsg, is(errorMsg));
    }

    /**
     * This test case checks for
     * PCEP-ERROR Object, PCEP-ERROR Object, RP Object, RP Object, PCEP-ERROR Object, PCEP-ERROR Object
     * in PcepErrorMsg message.
     */
    @Test
    public void errorMessageTest18() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] errorMsg = new byte[]{0x20, 0x06, 0x00, 0x3C, // common header
                0x0D, 0x10, 0x00, 0x08, // PCEP-ERROR Object Header
                0x00, 0x00, 0x01, 0x01, 0x0D, 0x10, 0x00, 0x08, // PCEP-ERROR Object Header
                0x00, 0x00, 0x01, 0x03, 0x02, 0x10, 0x00, 0x0C, // RP Object Header
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x02, 0x10, 0x00, 0x0C, // RP Object Header
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04, 0x0D, 0x10, 0x00, 0x08, // PCEP-ERROR Object Header
                0x00, 0x00, 0x01, 0x04, 0x0D, 0x10, 0x00, 0x08, // PCEP-ERROR Object Header
                0x00, 0x00, 0x01, 0x06};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errorMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testErrorMsg = {0};
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        assertThat(message, instanceOf(PcepErrorMsg.class));
        message.writeTo(buf);
        int iReadLen = buf.writerIndex();
        testErrorMsg = new byte[iReadLen];
        buf.readBytes(testErrorMsg, 0, iReadLen);

        assertThat(testErrorMsg, is(errorMsg));
    }

    /**
     * This test case checks for
     * PCEP-ERROR Object, PCEP-ERROR Object, LS Object, LS Object, PCEP-ERROR Object, PCEP-ERROR Object
     * in PcepErrorMsg message.
     */
    @Test
    public void errorMessageTest19() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] errorMsg = new byte[]{0x20, 0x06, 0x00, 0x44, // common header
                0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x01,
                0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x03,
                (byte) 0xE0, 0x10, 0x00, 0x10, // LS Object Header
                0x01, 0x00, 0x00, 0x03, // LS-ID
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x10,
                (byte) 0xE0, 0x10, 0x00, 0x10, // LS Object Header
                0x01, 0x00, 0x00, 0x03, // LS-ID
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x11,
                0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x04, // PCERR Object Header
                0x0D, 0x10, 0x00, 0x08,
                0x00, 0x00, 0x01, 0x06};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errorMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testErrorMsg = {0};
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        assertThat(message, instanceOf(PcepErrorMsg.class));
        message.writeTo(buf);
        int iReadLen = buf.writerIndex();
        testErrorMsg = new byte[iReadLen];
        buf.readBytes(testErrorMsg, 0, iReadLen);

        assertThat(testErrorMsg, is(errorMsg));
    }

    /**
     * This test case checks for
     * PCEP-ERROR Object, RP Object, RP Object, PCEP-ERROR Object, PCEP-ERROR Object,
     * LS Object, PCEP-ERROR Object
     * in PcepErrorMsg message.
     */
    @Test
    public void errorMessageTest20() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] errorMsg = new byte[]{0x20, 0x06, 0x00, 0x4C, // common header
                0x0D, 0x10, 0x00, 0x08, // PCEP-ERROR Object Header
                0x00, 0x00, 0x01, 0x01, 0x02, 0x10, 0x00, 0x0C, // RP Object Header
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x02, 0x10, 0x00, 0x0C, // RP Object Header
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04, 0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x04, 0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x06, (byte) 0xE0, 0x10, 0x00, 0x10, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                0x0D, 0x10, 0x00, 0x08, // PCERR Object Header
                0x00, 0x00, 0x01, 0x06};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(errorMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testErrorMsg = {0};
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        assertThat(message, instanceOf(PcepErrorMsg.class));

        message.writeTo(buf);
        int iReadLen = buf.writerIndex();
        testErrorMsg = new byte[iReadLen];
        buf.readBytes(testErrorMsg, 0, iReadLen);

        assertThat(testErrorMsg, is(errorMsg));
    }
}
