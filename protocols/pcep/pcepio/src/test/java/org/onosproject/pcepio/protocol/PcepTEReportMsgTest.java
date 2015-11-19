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
package org.onosproject.pcepio.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.onosproject.pcepio.exceptions.PcepParseException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;

public class PcepTEReportMsgTest {

    /**
     * This test case checks for
     * TE Object (Routing Universe TLV, Local TE Node Descriptors TLV(AutonomousSystemTlv)).
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest1() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, 0x28, // common header
                0x0E, 0x10, 0x00, 0x24, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x08, // Local TE Node Descriptors TLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystem Tlv
                0x00, 0x00, 0x00, 0x11};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};

        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for
     * T E Object (Routing Universe TLV, Local TE Node Descriptors TLV(AutonomousSystemTlv)) with different TE-ID.
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest2() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, 0x28, // common header
                0x0E, 0x10, 0x00, 0x24, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x08, // Local TE Node Descriptors TLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for  TE Object (Routing Universe TLV)
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest3() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, 0x1c, // common header
                0x0E, 0x10, 0x00, 0x18, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for
     * TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(AutonomousSystemTlv, BGPLSidentifierTlv.
     * OSPFareaIDsubTlv, RouterIDSubTlv)).
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest4() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, 0x44, // common header
                0x0E, 0x10, 0x00, 0x40, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x24, // Local TE Node Descriptors TLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for
     * TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv))
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest5() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, 0x3C, // common header
                0x0E, 0x10, 0x00, 0x38, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x1C, // Local TE Node Descriptors TLV
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(OSPFareaIDsubTlv,
     * RouterIDSubTlv))
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest6() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, 0x34, // common header
                0x0E, 0x10, 0x00, 0x30, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x14, // Local TE Node Descriptors TLV
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(RouterIDSubTlv)).
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest7() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, 0x2C, // common header
                0x0E, 0x10, 0x00, 0x28, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x0C, // Local TE Node Descriptors TLV
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for TE Object (Routing Universe TLV,Local TE Node Descriptors TLV)
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest8() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, 0x20, // common header
                0x0E, 0x10, 0x00, 0x1C, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x00 // Local TE Node Descriptors TLV
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for
     * TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(AutonomousSystemTlv, BGPLSidentifierTlv.
     * OSPFareaIDsubTlv, RouterIDSubTlv), RemoteTENodeDescriptorsTLV(AutonomousSystemTlv, BGPLSidentifierTlv.
     * OSPFareaIDsubTlv, RouterIDSubTlv)).
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest9() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, 0x6C, // common header
                0x0E, 0x10, 0x00, 0x68, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x24, // Local TE Node Descriptors TLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xEB, 0x00, 0x24, //RemoteTENodeDescriptorsTLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for
     * TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), RemoteTENodeDescriptorsTLV(BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv))
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest10() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, 0x64, // common header
                0x0E, 0x10, 0x00, 0x60, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x24, // Local TE Node Descriptors TLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xEB, 0x00, 0x1C, //RemoteTENodeDescriptorsTLV
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for
     * TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), RemoteTENodeDescriptorsTLV(OSPFareaIDsubTlv, RouterIDSubTlv))
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest11() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, 0x5C, // common header
                0x0E, 0x10, 0x00, 0x58, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x24, // Local TE Node Descriptors TLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xEB, 0x00, 0x14, //RemoteTENodeDescriptorsTLV
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for
     * TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), RemoteTENodeDescriptorsTLV(RouterIDSubTlv))
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest12() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, 0x54, // common header
                0x0E, 0x10, 0x00, 0x50, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x24, // Local TE Node Descriptors TLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xEB, 0x00, 0x0c, //RemoteTENodeDescriptorsTLV
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for
     * TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), RemoteTENodeDescriptorsTLV)
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest13() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, 0x48, // common header
                0x0E, 0x10, 0x00, 0x44, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x24, // Local TE Node Descriptors TLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xEB, 0x00, 0x00, //RemoteTENodeDescriptorsTLV
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for
     * TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), RemoteTENodeDescriptorsTLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), TELinkDescriptorsTLV(LinkLocalRemoteIdentifiersTlv
     * IPv4InterfaceAddressTlv, IPv4NeighborAddressTlv))
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest14() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, (byte) 0x8C, // common header
                0x0E, 0x10, 0x00, (byte) 0x88, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x24, // Local TE Node Descriptors TLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xEB, 0x00, 0x24, //RemoteTENodeDescriptorsTLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x04, 0x2E, 0x00, 0x1C, //TELinkDescriptorsTLV
                0x00, 0x04, 0x00, 0x08, //LinkLocalRemoteIdentifiersTlv
                0x01, 0x11, 0x00, 0x09,
                0x01, 0x21, 0x00, 0x09,
                0x00, 0x06, 0x00, 0x04, //IPv4InterfaceAddressTlv
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressTlv
                0x01, 0x011, 0x01, 0x10
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for
     * TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), RemoteTENodeDescriptorsTLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), TELinkDescriptorsTLV(
     * IPv4InterfaceAddressTlv, IPv4NeighborAddressTlv))
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest15() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, (byte) 0x80, // common header
                0x0E, 0x10, 0x00, (byte) 0x7C, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x24, // Local TE Node Descriptors TLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xEB, 0x00, 0x24, //RemoteTENodeDescriptorsTLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x04, 0x2E, 0x00, 0x10, //TELinkDescriptorsTLV
                0x00, 0x06, 0x00, 0x04, //IPv4InterfaceAddressTlv
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressTlv
                0x01, 0x011, 0x01, 0x10
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for
     * TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), RemoteTENodeDescriptorsTLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), TELinkDescriptorsTLV(IPv4NeighborAddressTlv))
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest16() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, (byte) 0x78, // common header
                0x0E, 0x10, 0x00, (byte) 0x74, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x24, // Local TE Node Descriptors TLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xEB, 0x00, 0x24, //RemoteTENodeDescriptorsTLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x04, 0x2E, 0x00, 0x08, //TELinkDescriptorsTLV
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressTlv
                0x01, 0x011, 0x01, 0x10
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for
     * TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), RemoteTENodeDescriptorsTLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), TELinkDescriptorsTLV)
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest17() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, (byte) 0x70, // common header
                0x0E, 0x10, 0x00, (byte) 0x6C, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x24, // Local TE Node Descriptors TLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xEB, 0x00, 0x24, //RemoteTENodeDescriptorsTLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x04, 0x2E, 0x00, 0x00, //TELinkDescriptorsTLV
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for
     * TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), RemoteTENodeDescriptorsTLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), TELinkDescriptorsTLV(LinkLocalRemoteIdentifiersTlv
     * IPv4InterfaceAddressTlv, IPv4NeighborAddressTlv))
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest18() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, (byte) 0xC0, // common header
                0x0E, 0x10, 0x00, (byte) 0xbC, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x24, // Local TE Node Descriptors TLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xEB, 0x00, 0x24, //RemoteTENodeDescriptorsTLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x04, 0x2E, 0x00, 0x1C, //TELinkDescriptorsTLV
                0x00, 0x04, 0x00, 0x08, //LinkLocalRemoteIdentifiersTlv
                0x01, 0x11, 0x00, 0x09,
                0x01, 0x21, 0x00, 0x09,
                0x00, 0x06, 0x00, 0x04, //IPv4InterfaceAddressTlv
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressTlv
                0x01, 0x011, 0x01, 0x10,
                0x04, (byte) 0xF3, 0x00, 0x30, //TENodeAttributesTlv
                0x00, 0x0E, 0x00, 0x01, //NodeFlagBitsTlv
                (byte) 0x90, 0x00, 0x00, 0x00,
                0x03, (byte) 0xE9, 0x00, 0x04, //OpaqueNodeAttributeTlv
                0x01, 0x011, 0x01, 0x10,
                0x03, (byte) 0xEF, 0x00, 0x08, //NodeNameTlv
                0x08, 0x00, 0x01, 0x09,
                0x08, 0x00, 0x01, 0x09,
                0x00, 0x6B, 0x00, 0x08, //ISISAreaIdentifierTlv
                0x20, 0x01, 0x22, 0x01,
                0x20, 0x01, 0x22, 0x01,
                0x00, (byte) 0x86, 0x00, 0x04, //IPv4TERouterIdOfLocalNodeTlv
                0x00, 0x01, 0x01, 0x02
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for
     * TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), RemoteTENodeDescriptorsTLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), TELinkDescriptorsTLV(LinkLocalRemoteIdentifiersTlv
     * IPv4InterfaceAddressTlv, IPv4NeighborAddressTlv), TENodeAttributesTlv(NodeFlagBitsTlv
     * OpaqueNodeAttributeTlv, NodeNameTlv, ISISAreaIdentifierTlv, IPv4TERouterIdOfLocalNodeTlv))
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest19() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, (byte) 0xC0, // common header
                0x0E, 0x10, 0x00, (byte) 0xBC, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x24, // Local TE Node Descriptors TLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xEB, 0x00, 0x24, //RemoteTENodeDescriptorsTLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x04, 0x2E, 0x00, 0x1C, //TELinkDescriptorsTLV
                0x00, 0x04, 0x00, 0x08, //LinkLocalRemoteIdentifiersTlv
                0x01, 0x11, 0x00, 0x09,
                0x01, 0x21, 0x00, 0x09,
                0x00, 0x06, 0x00, 0x04, //IPv4InterfaceAddressTlv
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressTlv
                0x01, 0x011, 0x01, 0x10,
                0x04, (byte) 0xF3, 0x00, 0x30, //TENodeAttributesTlv
                0x00, 0x0E, 0x00, 0x01, //NodeFlagBitsTlv
                (byte) 0x90, 0x00, 0x00, 0x00,
                0x03, (byte) 0xE9, 0x00, 0x04, //OpaqueNodeAttributeTlv
                0x01, 0x011, 0x01, 0x10,
                0x03, (byte) 0xEF, 0x00, 0x08, //NodeNameTlv
                0x08, 0x00, 0x01, 0x09,
                0x08, 0x00, 0x01, 0x09,
                0x00, 0x6B, 0x00, 0x08, //ISISAreaIdentifierTlv
                0x20, 0x01, 0x22, 0x01,
                0x20, 0x01, 0x22, 0x01,
                0x00, (byte) 0x86, 0x00, 0x04, //IPv4TERouterIdOfLocalNodeTlv
                0x00, 0x01, 0x01, 0x02
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for
     * TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), RemoteTENodeDescriptorsTLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), TELinkDescriptorsTLV(LinkLocalRemoteIdentifiersTlv
     * IPv4InterfaceAddressTlv, IPv4NeighborAddressTlv), TENodeAttributesTlv(OpaqueNodeAttributeTlv
     * NodeNameTlv, ISISAreaIdentifierTlv, IPv4TERouterIdOfLocalNodeTlv))
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest20() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, (byte) 0xB8, // common header
                0x0E, 0x10, 0x00, (byte) 0xB4, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x24, // Local TE Node Descriptors TLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xEB, 0x00, 0x24, //RemoteTENodeDescriptorsTLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x04, 0x2E, 0x00, 0x1C, //TELinkDescriptorsTLV
                0x00, 0x04, 0x00, 0x08, //LinkLocalRemoteIdentifiersTlv
                0x01, 0x11, 0x00, 0x09,
                0x01, 0x21, 0x00, 0x09,
                0x00, 0x06, 0x00, 0x04, //IPv4InterfaceAddressTlv
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressTlv
                0x01, 0x011, 0x01, 0x10,
                0x04, (byte) 0xF3, 0x00, 0x28, //TENodeAttributesTlv
                0x03, (byte) 0xE9, 0x00, 0x04, //OpaqueNodeAttributeTlv
                0x01, 0x011, 0x01, 0x10,
                0x03, (byte) 0xEF, 0x00, 0x08, //NodeNameTlv
                0x08, 0x00, 0x01, 0x09,
                0x08, 0x00, 0x01, 0x09,
                0x00, 0x6B, 0x00, 0x08, //ISISAreaIdentifierTlv
                0x20, 0x01, 0x22, 0x01,
                0x20, 0x01, 0x22, 0x01,
                0x00, (byte) 0x86, 0x00, 0x04, //IPv4TERouterIdOfLocalNodeTlv
                0x00, 0x01, 0x01, 0x02
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for
     * TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), RemoteTENodeDescriptorsTLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), TELinkDescriptorsTLV(LinkLocalRemoteIdentifiersTlv.
     * IPv4InterfaceAddressTlv, IPv4NeighborAddressTlv), TENodeAttributesTlv(OpaqueNodeAttributeTlv
     * ISISAreaIdentifierTlv, IPv4TERouterIdOfLocalNodeTlv))
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest21() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x00, (byte) 0xAC, // common header
                0x0E, 0x10, 0x00, (byte) 0xA8, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x24, // Local TE Node Descriptors TLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xEB, 0x00, 0x24, //RemoteTENodeDescriptorsTLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x04, 0x2E, 0x00, 0x1C, //TELinkDescriptorsTLV
                0x00, 0x04, 0x00, 0x08, //LinkLocalRemoteIdentifiersTlv
                0x01, 0x11, 0x00, 0x09,
                0x01, 0x21, 0x00, 0x09,
                0x00, 0x06, 0x00, 0x04, //IPv4InterfaceAddressTlv
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressTlv
                0x01, 0x011, 0x01, 0x10,
                0x04, (byte) 0xF3, 0x00, 0x1C, //TENodeAttributesTlv
                0x03, (byte) 0xE9, 0x00, 0x04, //OpaqueNodeAttributeTlv
                0x01, 0x011, 0x01, 0x10,
                0x00, 0x6B, 0x00, 0x08, //ISISAreaIdentifierTlv
                0x20, 0x01, 0x22, 0x01,
                0x20, 0x01, 0x22, 0x01,
                0x00, (byte) 0x86, 0x00, 0x04, //IPv4TERouterIdOfLocalNodeTlv
                0x00, 0x01, 0x01, 0x02
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for
     * TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(AutonomousSystemTlv, BGPLSidentifierTlv.
     * OSPFareaIDsubTlv, RouterIDSubTlv), RemoteTENodeDescriptorsTLV(AutonomousSystemTlv, BGPLSidentifierTlv.
     * OSPFareaIDsubTlv, RouterIDSubTlv), TELinkDescriptorsTLV(LinkLocalRemoteIdentifiersTlv.
     * IPv4InterfaceAddressTlv, IPv4NeighborAddressTlv), TENodeAttributesTlv(NodeFlagBitsTlv.
     * OpaqueNodeAttributeTlv, NodeNameTlv, ISISAreaIdentifierTlv, IPv4TERouterIdOfLocalNodeTlv).
     * TELinkAttributesTlv(IPv4TERouterIdOfRemoteNodeTlv, IPv6TERouterIdofRemoteNodeTlv, AdministrativeGroupTlv.
     * MaximumLinkBandwidthTlv, MaximumReservableLinkBandwidthTlv, UnreservedBandwidthTlv, TEDefaultMetricTlv.
     * LinkProtectionTypeTlv, MPLSProtocolMaskTlv, IGPMetricTlv:, SharedRiskLinkGroupTlv.
     * OpaqueLinkAttributeTlv, LinkNameTlv)).
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest22() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x01, (byte) 0x120, // common header
                0x0E, 0x10, 0x01, (byte) 0x1C, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x24, // Local TE Node Descriptors TLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xEB, 0x00, 0x24, //RemoteTENodeDescriptorsTLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x04, 0x2E, 0x00, 0x1C, //TELinkDescriptorsTLV
                0x00, 0x04, 0x00, 0x08, //LinkLocalRemoteIdentifiersTlv
                0x01, 0x11, 0x00, 0x09,
                0x01, 0x21, 0x00, 0x09,
                0x00, 0x06, 0x00, 0x04, //IPv4InterfaceAddressTlv
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressTlv
                0x01, 0x011, 0x01, 0x10,
                0x04, (byte) 0xF3, 0x00, 0x28, //TENodeAttributesTlv
                0x03, (byte) 0xE9, 0x00, 0x04, //OpaqueNodeAttributeTlv
                0x01, 0x011, 0x01, 0x10,
                0x03, (byte) 0xEF, 0x00, 0x08, //NodeNameTlv
                0x08, 0x00, 0x01, 0x09,
                0x08, 0x00, 0x01, 0x09,
                0x00, 0x6B, 0x00, 0x08, //ISISAreaIdentifierTlv
                0x20, 0x01, 0x22, 0x01,
                0x20, 0x01, 0x22, 0x01,
                0x00, (byte) 0x86, 0x00, 0x04, //IPv4TERouterIdOfLocalNodeTlv
                0x00, 0x01, 0x01, 0x02,
                0x07, 0x69, 0x00, 0x64, //TELinkAttributesTlv
                0x05, 0x3C, 0x00, 0x04, //IPv4TERouterIdOfRemoteNodeTlv
                0x00, 0x07, 0x08, 0x00,
                0x00, 0x03, 0x00, 0x04, //AdministrativeGroupTlv
                0x00, 0x09, 0x08, 0x00,
                0x00, 0x09, 0x00, 0x04, //MaximumLinkBandwidthTlv
                0x00, 0x09, 0x00, 0x00,
                0x00, 0x0a, 0x00, 0x04, //MaximumReservableLinkBandwidthTlv
                0x00, 0x10, 0x00, 0x00,
                0x00, 0x0b, 0x00, 0x04, //UnreservedBandwidthTlv
                0x00, 0x00, (byte) 0x90, 0x00,
                0x34, 0x58, 0x00, 0x04, //TEDefaultMetricTlv
                0x00, (byte) 0x99, 0x09, 0x00,
                0x00, 0x14, 0x00, 0x02, //LinkProtectionTypeTlv
                0x09, 0x00, 0x00, 0x00,
                0x04, 0x46, 0x00, 0x01, //MPLSProtocolMaskTlv
                (byte) 0x80, 0x00, 0x00, 0x00,
                0x04, 0x47, 0x00, 0x03, //IGPMetricTlv
                0x09, (byte) 0x89, 0x07, 0x00,
                0x04, 0x48, 0x00, 0x08, //SharedRiskLinkGroupTlv
                0x04, 0x47, 0x00, 0x03,
                0x04, 0x47, 0x00, 0x03, //OpaqueLinkAttributeTlv
                0x04, 0x49, 0x00, 0x04,
                0x04, 0x47, 0x00, 0x03,
                0x04, 0x4A, 0x00, 0x04, //LinkNameTlv
                0x04, 0x47, 0x00, 0x03
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for
     * TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), RemoteTENodeDescriptorsTLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), TELinkDescriptorsTLV(LinkLocalRemoteIdentifiersTlv
     * IPv4InterfaceAddressTlv, IPv4NeighborAddressTlv), TENodeAttributesTlv(NodeFlagBitsTlv
     * OpaqueNodeAttributeTlv, NodeNameTlv, ISISAreaIdentifierTlv, IPv4TERouterIdOfLocalNodeTlv)
     * TELinkAttributesTlv(IPv4TERouterIdOfRemoteNodeTlv, IPv6TERouterIdofRemoteNodeTlv, AdministrativeGroupTlv
     * MaximumLinkBandwidthTlv, MaximumReservableLinkBandwidthTlv, UnreservedBandwidthTlv, TEDefaultMetricTlv
     * LinkProtectionTypeTlv, MPLSProtocolMaskTlv, IGPMetricTlv:, SharedRiskLinkGroupTlv
     * OpaqueLinkAttributeTlv))
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest23() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x01, (byte) 0x118, // common header
                0x0E, 0x10, 0x01, (byte) 0x14, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x24, // Local TE Node Descriptors TLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xEB, 0x00, 0x24, //RemoteTENodeDescriptorsTLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x04, 0x2E, 0x00, 0x1C, //TELinkDescriptorsTLV
                0x00, 0x04, 0x00, 0x08, //LinkLocalRemoteIdentifiersTlv
                0x01, 0x11, 0x00, 0x09,
                0x01, 0x21, 0x00, 0x09,
                0x00, 0x06, 0x00, 0x04, //IPv4InterfaceAddressTlv
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressTlv
                0x01, 0x011, 0x01, 0x10,
                0x04, (byte) 0xF3, 0x00, 0x28, //TENodeAttributesTlv
                0x03, (byte) 0xE9, 0x00, 0x04, //OpaqueNodeAttributeTlv
                0x01, 0x011, 0x01, 0x10,
                0x03, (byte) 0xEF, 0x00, 0x08, //NodeNameTlv
                0x08, 0x00, 0x01, 0x09,
                0x08, 0x00, 0x01, 0x09,
                0x00, 0x6B, 0x00, 0x08, //ISISAreaIdentifierTlv
                0x20, 0x01, 0x22, 0x01,
                0x20, 0x01, 0x22, 0x01,
                0x00, (byte) 0x86, 0x00, 0x04, //IPv4TERouterIdOfLocalNodeTlv
                0x00, 0x01, 0x01, 0x02,
                0x07, 0x69, 0x00, 0x5C, //TELinkAttributesTlv
                0x05, 0x3C, 0x00, 0x04, //IPv4TERouterIdOfRemoteNodeTlv
                0x00, 0x07, 0x08, 0x00,
                0x00, 0x03, 0x00, 0x04, //AdministrativeGroupTlv
                0x00, 0x09, 0x08, 0x00,
                0x00, 0x09, 0x00, 0x04, //MaximumLinkBandwidthTlv
                0x00, 0x09, 0x00, 0x00,
                0x00, 0x0a, 0x00, 0x04, //MaximumReservableLinkBandwidthTlv
                0x00, 0x10, 0x00, 0x00,
                0x00, 0x0b, 0x00, 0x04, //UnreservedBandwidthTlv
                0x00, 0x00, (byte) 0x90, 0x00,
                0x34, 0x58, 0x00, 0x04, //TEDefaultMetricTlv
                0x00, (byte) 0x99, 0x09, 0x00,
                0x00, 0x14, 0x00, 0x02, //LinkProtectionTypeTlv
                0x09, 0x00, 0x00, 0x00,
                0x04, 0x46, 0x00, 0x01, //MPLSProtocolMaskTlv
                (byte) 0x80, 0x00, 0x00, 0x00,
                0x04, 0x47, 0x00, 0x03, //IGPMetricTlv
                0x09, (byte) 0x89, 0x07, 0x00,
                0x04, 0x48, 0x00, 0x08, //SharedRiskLinkGroupTlv
                0x04, 0x47, 0x00, 0x03,
                0x04, 0x47, 0x00, 0x03, //OpaqueLinkAttributeTlv
                0x04, 0x49, 0x00, 0x04,
                0x04, 0x47, 0x00, 0x03
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for
     * TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), RemoteTENodeDescriptorsTLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), TELinkDescriptorsTLV(LinkLocalRemoteIdentifiersTlv
     * IPv4InterfaceAddressTlv, IPv4NeighborAddressTlv), TENodeAttributesTlv(NodeFlagBitsTlv
     * OpaqueNodeAttributeTlv, NodeNameTlv, ISISAreaIdentifierTlv, IPv4TERouterIdOfLocalNodeTlv)
     * TELinkAttributesTlv(IPv4TERouterIdOfRemoteNodeTlv, IPv6TERouterIdofRemoteNodeTlv, AdministrativeGroupTlv
     * MaximumLinkBandwidthTlv, MaximumReservableLinkBandwidthTlv, UnreservedBandwidthTlv, TEDefaultMetricTlv
     * LinkProtectionTypeTlv, MPLSProtocolMaskTlv, IGPMetricTlv:, SharedRiskLinkGroupTlv))
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest24() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x01, (byte) 0x110, // common header
                0x0E, 0x10, 0x01, (byte) 0x0C, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x24, // Local TE Node Descriptors TLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xEB, 0x00, 0x24, //RemoteTENodeDescriptorsTLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x04, 0x2E, 0x00, 0x1C, //TELinkDescriptorsTLV
                0x00, 0x04, 0x00, 0x08, //LinkLocalRemoteIdentifiersTlv
                0x01, 0x11, 0x00, 0x09,
                0x01, 0x21, 0x00, 0x09,
                0x00, 0x06, 0x00, 0x04, //IPv4InterfaceAddressTlv
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressTlv
                0x01, 0x011, 0x01, 0x10,
                0x04, (byte) 0xF3, 0x00, 0x28, //TENodeAttributesTlv
                0x03, (byte) 0xE9, 0x00, 0x04, //OpaqueNodeAttributeTlv
                0x01, 0x011, 0x01, 0x10,
                0x03, (byte) 0xEF, 0x00, 0x08, //NodeNameTlv
                0x08, 0x00, 0x01, 0x09,
                0x08, 0x00, 0x01, 0x09,
                0x00, 0x6B, 0x00, 0x08, //ISISAreaIdentifierTlv
                0x20, 0x01, 0x22, 0x01,
                0x20, 0x01, 0x22, 0x01,
                0x00, (byte) 0x86, 0x00, 0x04, //IPv4TERouterIdOfLocalNodeTlv
                0x00, 0x01, 0x01, 0x02,
                0x07, 0x69, 0x00, 0x54, //TELinkAttributesTlv
                0x05, 0x3C, 0x00, 0x04, //IPv4TERouterIdOfRemoteNodeTlv
                0x00, 0x07, 0x08, 0x00,
                0x00, 0x03, 0x00, 0x04, //AdministrativeGroupTlv
                0x00, 0x09, 0x08, 0x00,
                0x00, 0x09, 0x00, 0x04, //MaximumLinkBandwidthTlv
                0x00, 0x09, 0x00, 0x00,
                0x00, 0x0a, 0x00, 0x04, //MaximumReservableLinkBandwidthTlv
                0x00, 0x10, 0x00, 0x00,
                0x00, 0x0b, 0x00, 0x04, //UnreservedBandwidthTlv
                0x00, 0x00, (byte) 0x90, 0x00,
                0x34, 0x58, 0x00, 0x04, //TEDefaultMetricTlv
                0x00, (byte) 0x99, 0x09, 0x00,
                0x00, 0x14, 0x00, 0x02, //LinkProtectionTypeTlv
                0x09, 0x00, 0x00, 0x00,
                0x04, 0x46, 0x00, 0x01, //MPLSProtocolMaskTlv
                (byte) 0x80, 0x00, 0x00, 0x00,
                0x04, 0x47, 0x00, 0x03, //IGPMetricTlv
                0x09, (byte) 0x89, 0x07, 0x00,
                0x04, 0x48, 0x00, 0x08, //SharedRiskLinkGroupTlv
                0x04, 0x47, 0x00, 0x03,
                0x04, 0x47, 0x00, 0x03
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }

    /**
     * This test case checks for
     * TE Object (Routing Universe TLV,Local TE Node Descriptors TLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), RemoteTENodeDescriptorsTLV(AutonomousSystemTlv, BGPLSidentifierTlv
     * OSPFareaIDsubTlv, RouterIDSubTlv), TELinkDescriptorsTLV(LinkLocalRemoteIdentifiersTlv
     * IPv4InterfaceAddressTlv, IPv4NeighborAddressTlv), TENodeAttributesTlv(NodeFlagBitsTlv
     * OpaqueNodeAttributeTlv, NodeNameTlv, ISISAreaIdentifierTlv, IPv4TERouterIdOfLocalNodeTlv)
     * TELinkAttributesTlv(IPv4TERouterIdOfRemoteNodeTlv, IPv6TERouterIdofRemoteNodeTlv, AdministrativeGroupTlv
     * MaximumLinkBandwidthTlv, MaximumReservableLinkBandwidthTlv, UnreservedBandwidthTlv, TEDefaultMetricTlv
     * LinkProtectionTypeTlv, MPLSProtocolMaskTlv, IGPMetricTlv))
     * in PcTERpt message.
     */
    @Test
    public void teReportMessageTest25() throws PcepParseException {

        byte[] teReportMsg = new byte[]{0x20, 0x0E, 0x01, (byte) 0x104, // common header
                0x0E, 0x10, 0x01, 0x00, // TE Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, // TE-ID
                0x00, 0x0E, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                0x06, 0x65, 0x00, 0x24, // Local TE Node Descriptors TLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xEB, 0x00, 0x24, //RemoteTENodeDescriptorsTLV
                0x00, 0x64, 0x00, 0x04, //AutonomousSystemTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x11, 0x00, 0x04, //BGPLSidentifierTlv
                0x00, 0x00, 0x00, 0x11,
                0x02, 0x58, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x03, (byte) 0xE8, 0x00, 0x08, //RouterIDSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                0x04, 0x2E, 0x00, 0x1C, //TELinkDescriptorsTLV
                0x00, 0x04, 0x00, 0x08, //LinkLocalRemoteIdentifiersTlv
                0x01, 0x11, 0x00, 0x09,
                0x01, 0x21, 0x00, 0x09,
                0x00, 0x06, 0x00, 0x04, //IPv4InterfaceAddressTlv
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressTlv
                0x01, 0x011, 0x01, 0x10,
                0x04, (byte) 0xF3, 0x00, 0x28, //TENodeAttributesTlv
                0x03, (byte) 0xE9, 0x00, 0x04, //OpaqueNodeAttributeTlv
                0x01, 0x011, 0x01, 0x10,
                0x03, (byte) 0xEF, 0x00, 0x08, //NodeNameTlv
                0x08, 0x00, 0x01, 0x09,
                0x08, 0x00, 0x01, 0x09,
                0x00, 0x6B, 0x00, 0x08, //ISISAreaIdentifierTlv
                0x20, 0x01, 0x22, 0x01,
                0x20, 0x01, 0x22, 0x01,
                0x00, (byte) 0x86, 0x00, 0x04, //IPv4TERouterIdOfLocalNodeTlv
                0x00, 0x01, 0x01, 0x02,
                0x07, 0x69, 0x00, 0x48, //TELinkAttributesTlv
                0x05, 0x3C, 0x00, 0x04, //IPv4TERouterIdOfRemoteNodeTlv
                0x00, 0x07, 0x08, 0x00,
                0x00, 0x03, 0x00, 0x04, //AdministrativeGroupTlv
                0x00, 0x09, 0x08, 0x00,
                0x00, 0x09, 0x00, 0x04, //MaximumLinkBandwidthTlv
                0x00, 0x09, 0x00, 0x00,
                0x00, 0x0a, 0x00, 0x04, //MaximumReservableLinkBandwidthTlv
                0x00, 0x10, 0x00, 0x00,
                0x00, 0x0b, 0x00, 0x04, //UnreservedBandwidthTlv
                0x00, 0x00, (byte) 0x90, 0x00,
                0x34, 0x58, 0x00, 0x04, //TEDefaultMetricTlv
                0x00, (byte) 0x99, 0x09, 0x00,
                0x00, 0x14, 0x00, 0x02, //LinkProtectionTypeTlv
                0x09, 0x00, 0x00, 0x00,
                0x04, 0x46, 0x00, 0x01, //MPLSProtocolMaskTlv
                (byte) 0x80, 0x00, 0x00, 0x00,
                0x04, 0x47, 0x00, 0x03, //IGPMetricTlv
                0x09, (byte) 0x89, 0x07, 0x00
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(teReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};

        assertThat(message, instanceOf(PcepTEReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(teReportMsg));
    }
}
