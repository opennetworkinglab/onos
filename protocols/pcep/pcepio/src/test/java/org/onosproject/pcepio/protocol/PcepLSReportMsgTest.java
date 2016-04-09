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
package org.onosproject.pcepio.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.onosproject.pcepio.exceptions.PcepOutOfBoundMessageException;
import org.onosproject.pcepio.exceptions.PcepParseException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;

public class PcepLSReportMsgTest {

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV, Local Node Descriptors TLV(AutonomousSystemSubTlv)).
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest1() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, 0x2C, // common header
                (byte) 0xE0, 0x10, 0x00, 0x28, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x08, // Local Node Descriptors TLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystem Tlv
                0x00, 0x00, 0x00, 0x11};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};

        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV, Local Node Descriptors TLV(AutonomousSystemSubTlv)) with different LS-ID.
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest2() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, 0x2C, // common header
                (byte) 0xE0, 0x10, 0x00, 0x28, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x08, // Local Node Descriptors TLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for  LS Object (Routing Universe TLV)
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest3() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, 0x20, // common header
                (byte) 0xE0, 0x10, 0x00, 0x1C, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV,Local Node Descriptors TLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv.
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv)).
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest4() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, 0x48, // common header
                (byte) 0xE0, 0x10, 0x00, 0x44, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x24, // Local Node Descriptors TLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OspfAreaIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV,Local Node Descriptors TLV(BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv))
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest5() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, 0x40, // common header
                (byte) 0xE0, 0x10, 0x00, 0x3C, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x1C, // Local Node Descriptors TLV
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for LS Object (Routing Universe TLV,Local Node Descriptors TLV(OSPFareaIDsubTlv,
     * IgpRouterIdSubTlv))
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest6() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, 0x38, // common header
                (byte) 0xE0, 0x10, 0x00, 0x34, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x14, // Local Node Descriptors TLV
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for LS Object (Routing Universe TLV,Local Node Descriptors TLV(IgpRouterIdSubTlv)).
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest7() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, 0x30, // common header
                (byte) 0xE0, 0x10, 0x00, 0x2C, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x0C, // Local Node Descriptors TLV
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for LS Object (Routing Universe TLV,Local Node Descriptors TLV)
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest8() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, 0x24, // common header
                (byte) 0xE0, 0x10, 0x00, 0x20, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x00 // Local Node Descriptors TLV
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV,Local Node Descriptors TLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv.
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), RemoteNodeDescriptorsTLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv.
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv)).
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest9() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, 0x70, // common header
                (byte) 0xE0, 0x10, 0x00, 0x6C, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x24, // Local Node Descriptors TLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x03, 0x00, 0x24, //RemoteNodeDescriptorsTLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV,Local Node Descriptors TLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), RemoteNodeDescriptorsTLV(BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv))
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest10() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, 0x68, // common header
                (byte) 0xE0, 0x10, 0x00, 0x64, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x24, // Local Node Descriptors TLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x03, 0x00, 0x1C, //RemoteNodeDescriptorsTLV
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV,Local Node Descriptors TLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), RemoteNodeDescriptorsTLV(OSPFareaIDsubTlv, IgpRouterIdSubTlv))
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest11() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, 0x60, // common header
                (byte) 0xE0, 0x10, 0x00, 0x5C, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x24, // Local Node Descriptors TLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x03, 0x00, 0x14, //RemoteNodeDescriptorsTLV
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV,Local Node Descriptors TLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), RemoteNodeDescriptorsTLV(IgpRouterIdSubTlv))
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest12() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, 0x58, // common header
                (byte) 0xE0, 0x10, 0x00, 0x54, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x24, // Local Node Descriptors TLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x03, 0x00, 0x0c, //RemoteNodeDescriptorsTLV
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV,Local Node Descriptors TLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), RemoteNodeDescriptorsTLV)
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest13() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, 0x4C, // common header
                (byte) 0xE0, 0x10, 0x00, 0x48, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x24, // Local Node Descriptors TLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x03, 0x00, 0x00 //RemoteNodeDescriptorsTLV
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV,Local Node Descriptors TLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), RemoteNodeDescriptorsTLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), LinkDescriptorsTLV(LinkLocalRemoteIdentifiersSubTlv
     * IPv4InterfaceAddressSubTlv, IPv4NeighborAddressSubTlv))
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest14() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, (byte) 0x90, // common header
                (byte) 0xE0, 0x10, 0x00, (byte) 0x8C, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x24, // Local Node Descriptors TLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x03, 0x00, 0x24, //RemoteNodeDescriptorsTLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x04, 0x00, 0x1C, //LinkDescriptorsTLV
                0x00, 0x06, 0x00, 0x08, //LinkLocalRemoteIdentifiersSubTlv
                0x01, 0x11, 0x00, 0x09,
                0x01, 0x21, 0x00, 0x09,
                0x00, 0x07, 0x00, 0x04, //IPv4InterfaceAddressSubTlv
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressSubTlv
                0x01, 0x011, 0x01, 0x10
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV,Local Node Descriptors TLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), RemoteNodeDescriptorsTLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), LinkDescriptorsTLV(
     * IPv4InterfaceAddressSubTlv, IPv4NeighborAddressSubTlv))
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest15() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, (byte) 0x84, // common header
                (byte) 0xE0, 0x10, 0x00, (byte) 0x80, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x24, // Local Node Descriptors TLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x03, 0x00, 0x24, //RemoteNodeDescriptorsTLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x04, 0x00, 0x10, //LinkDescriptorsTLV
                0x00, 0x07, 0x00, 0x04, //IPv4InterfaceAddressSubTlv
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressSubTlv
                0x01, 0x011, 0x01, 0x10
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV,Local Node Descriptors TLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), RemoteNodeDescriptorsTLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), LinkDescriptorsTLV(IPv4NeighborAddressSubTlv))
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest16() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, (byte) 0x7C, // common header
                (byte) 0xE0, 0x10, 0x00, (byte) 0x78, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x24, // Local Node Descriptors TLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x03, 0x00, 0x24, //RemoteNodeDescriptorsTLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x04, 0x00, 0x08, //LinkDescriptorsTLV
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressSubTlv
                0x01, 0x011, 0x01, 0x10
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV,Local Node Descriptors TLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), RemoteNodeDescriptorsTLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), LinkDescriptorsTLV)
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest17() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, (byte) 0x74, // common header
                (byte) 0xE0, 0x10, 0x00, (byte) 0x70, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x24, // Local Node Descriptors TLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x03, 0x00, 0x24, //RemoteNodeDescriptorsTLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x04, 0x00, 0x00, //LinkDescriptorsTLV
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV,Local Node Descriptors TLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), RemoteNodeDescriptorsTLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), LinkDescriptorsTLV(LinkLocalRemoteIdentifiersSubTlv
     * IPv4InterfaceAddressSubTlv, IPv4NeighborAddressSubTlv))
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest18() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, (byte) 0xC4, // common header
                (byte) 0xE0, 0x10, 0x00, (byte) 0xC0, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x24, // Local Node Descriptors TLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x03, 0x00, 0x24, //RemoteNodeDescriptorsTLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x04, 0x00, 0x1C, //LinkDescriptorsTLV
                0x00, 0x06, 0x00, 0x08, //LinkLocalRemoteIdentifiersSubTlv
                0x01, 0x11, 0x00, 0x09,
                0x01, 0x21, 0x00, 0x09,
                0x00, 0x07, 0x00, 0x04, //IPv4InterfaceAddressSubTlv
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressSubTlv
                0x01, 0x011, 0x01, 0x10,
                (byte) 0xFF, 0x05, 0x00, 0x30, //NodeAttributesTlv
                0x00, 0x0D, 0x00, 0x01, //NodeFlagBitsSubTlv
                (byte) 0x90, 0x00, 0x00, 0x00,
                0x00, 0x0E, 0x00, 0x04, //OpaqueNodePropertiesSubTlv
                0x01, 0x011, 0x01, 0x10,
                0x00, 0x0F, 0x00, 0x08, //NodeNameSubTlv
                0x08, 0x00, 0x01, 0x09,
                0x08, 0x00, 0x01, 0x09,
                0x00, 0x10, 0x00, 0x08, //ISISAreaIdentifierSubTlv
                0x20, 0x01, 0x22, 0x01,
                0x20, 0x01, 0x22, 0x01,
                0x00, 0x11, 0x00, 0x04, //IPv4RouterIdOfLocalNodeSubTlv
                0x00, 0x01, 0x01, 0x02
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV,Local Node Descriptors TLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), RemoteNodeDescriptorsTLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), LinkDescriptorsTLV(LinkLocalRemoteIdentifiersSubTlv
     * IPv4InterfaceAddressSubTlv, IPv4NeighborAddressSubTlv), NodeAttributesTlv(NodeFlagBitsSubTlv
     * OpaqueNodePropertiesSubTlv, NodeNameSubTlv, ISISAreaIdentifierSubTlv, IPv4RouterIdOfLocalNodeSubTlv))
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest19() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, (byte) 0xC4, // common header
                (byte) 0xE0, 0x10, 0x00, (byte) 0xC0, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x24, // Local Node Descriptors TLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x03, 0x00, 0x24, //RemoteNodeDescriptorsTLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x04, 0x00, 0x1C, //LinkDescriptorsTLV
                0x00, 0x06, 0x00, 0x08, //LinkLocalRemoteIdentifiersSubTlv
                0x01, 0x11, 0x00, 0x09,
                0x01, 0x21, 0x00, 0x09,
                0x00, 0x07, 0x00, 0x04, //IPv4InterfaceAddressSubTlv
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressSubTlv
                0x01, 0x011, 0x01, 0x10,
                (byte) 0xFF, 0x05, 0x00, 0x30, //NodeAttributesTlv
                0x00, 0x0D, 0x00, 0x01, //NodeFlagBitsSubTlv
                (byte) 0x90, 0x00, 0x00, 0x00,
                0x00, 0x0E, 0x00, 0x04, //OpaqueNodePropertiesSubTlv
                0x01, 0x011, 0x01, 0x10,
                0x00, 0x0F, 0x00, 0x08, //NodeNameSubTlv
                0x08, 0x00, 0x01, 0x09,
                0x08, 0x00, 0x01, 0x09,
                0x00, 0x10, 0x00, 0x08, //ISISAreaIdentifierSubTlv
                0x20, 0x01, 0x22, 0x01,
                0x20, 0x01, 0x22, 0x01,
                0x00, 0x11, 0x00, 0x04, //IPv4RouterIdOfLocalNodeSubTlv
                0x00, 0x01, 0x01, 0x02
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV,Local Node Descriptors TLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), RemoteNodeDescriptorsTLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), LinkDescriptorsTLV(LinkLocalRemoteIdentifiersSubTlv
     * IPv4InterfaceAddressSubTlv, IPv4NeighborAddressSubTlv), NodeAttributesTlv(OpaqueNodePropertiesSubTlv
     * NodeNameSubTlv, ISISAreaIdentifierSubTlv, IPv4RouterIdOfLocalNodeSubTlv))
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest20() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, (byte) 0xBC, // common header
                (byte) 0xE0, 0x10, 0x00, (byte) 0xB8, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x24, // Local Node Descriptors TLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x03, 0x00, 0x24, //RemoteNodeDescriptorsTLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x04, 0x00, 0x1C, //LinkDescriptorsTLV
                0x00, 0x06, 0x00, 0x08, //LinkLocalRemoteIdentifiersSubTlv
                0x01, 0x11, 0x00, 0x09,
                0x01, 0x21, 0x00, 0x09,
                0x00, 0x07, 0x00, 0x04, //IPv4InterfaceAddressSubTlv
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressSubTlv
                0x01, 0x011, 0x01, 0x10,
                (byte) 0xFF, 0x05, 0x00, 0x28, //NodeAttributesTlv
                0x00, 0x0E, 0x00, 0x04, //OpaqueNodePropertiesSubTlv
                0x01, 0x011, 0x01, 0x10,
                0x00, 0x0F, 0x00, 0x08, //NodeNameSubTlv
                0x08, 0x00, 0x01, 0x09,
                0x08, 0x00, 0x01, 0x09,
                0x00, 0x10, 0x00, 0x08, //ISISAreaIdentifierSubTlv
                0x20, 0x01, 0x22, 0x01,
                0x20, 0x01, 0x22, 0x01,
                0x00, 0x11, 0x00, 0x04, //IPv4RouterIdOfLocalNodeSubTlv
                0x00, 0x01, 0x01, 0x02
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV,Local Node Descriptors TLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), RemoteNodeDescriptorsTLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), LinkDescriptorsTLV(LinkLocalRemoteIdentifiersSubTlv.
     * IPv4InterfaceAddressSubTlv, IPv4NeighborAddressSubTlv), NodeAttributesTlv(OpaqueNodePropertiesSubTlv
     * ISISAreaIdentifierSubTlv, IPv4RouterIdOfLocalNodeSubTlv))
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest21() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, (byte) 0xB0, // common header
                (byte) 0xE0, 0x10, 0x00, (byte) 0xAC, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x24, // Local Node Descriptors TLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x03, 0x00, 0x24, //RemoteNodeDescriptorsTLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x04, 0x00, 0x1C, //LinkDescriptorsTLV
                0x00, 0x06, 0x00, 0x08, //LinkLocalRemoteIdentifiersSubTlv
                0x01, 0x11, 0x00, 0x09,
                0x01, 0x21, 0x00, 0x09,
                0x00, 0x07, 0x00, 0x04, //IPv4InterfaceAddressSubTlv
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressSubTlv
                0x01, 0x011, 0x01, 0x10,
                (byte) 0xFF, 0x05, 0x00, 0x1C, //NodeAttributesTlv
                0x00, 0x0E, 0x00, 0x04, //OpaqueNodePropertiesSubTlv
                0x01, 0x011, 0x01, 0x10,
                0x00, 0x10, 0x00, 0x08, //ISISAreaIdentifierSubTlv
                0x20, 0x01, 0x22, 0x01,
                0x20, 0x01, 0x22, 0x01,
                0x00, 0x11, 0x00, 0x04, //IPv4RouterIdOfLocalNodeSubTlv
                0x00, 0x01, 0x01, 0x02
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV,Local Node Descriptors TLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv,
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), RemoteNodeDescriptorsTLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv,
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), LinkDescriptorsTLV(LinkLocalRemoteIdentifiersSubTlv,
     * IPv4InterfaceAddressSubTlv, IPv4NeighborAddressSubTlv), NodeAttributesTlv(NodeFlagBitsSubTlv,
     * OpaqueNodePropertiesSubTlv, NodeNameSubTlv, ISISAreaIdentifierSubTlv, IPv4RouterIdOfLocalNodeSubTlv),
     * LinkAttributesTlv(IPv4RouterIdOfRemoteNodeSubTlv, IPv6LSRouterIdofRemoteNodeTlv, AdministrativeGroupSubTlv,
     * TEDefaultMetricSubTlv, MaximumLinkBandwidthSubTlv, MaximumReservableLinkBandwidthSubTlv,
     * UnreservedBandwidthSubTlv, LinkProtectionTypeSubTlv, MPLSProtocolMaskSubTlv, IgpMetricSubTlv,
     * SharedRiskLinkGroupSubTlv, OpaqueLinkAttributeSubTlv, LinkNameAttributeSubTlv)).
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest22() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x01, 0x18, // common header
                (byte) 0xE0, 0x10, 0x01, 0x14, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x24, // Local Node Descriptors TLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x03, 0x00, 0x24, //RemoteNodeDescriptorsTLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x04, 0x00, 0x1C, //LinkDescriptorsTLV
                0x00, 0x06, 0x00, 0x08, //LinkLocalRemoteIdentifiersSubTlv
                0x01, 0x11, 0x00, 0x09,
                0x01, 0x21, 0x00, 0x09,
                0x00, 0x07, 0x00, 0x04, //IPv4InterfaceAddressSubTlv
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressSubTlv
                0x01, 0x011, 0x01, 0x10,
                (byte) 0xFF, 0x05, 0x00, 0x1C, //NodeAttributesTlv
                0x00, 0x0E, 0x00, 0x04, //OpaqueNodePropertiesSubTlv
                0x01, 0x011, 0x01, 0x10,
                0x00, 0x10, 0x00, 0x08, //ISISAreaIdentifierSubTlv
                0x20, 0x01, 0x22, 0x01,
                0x20, 0x01, 0x22, 0x01,
                0x00, 0x11, 0x00, 0x04, //IPv4RouterIdOfLocalNodeSubTlv
                0x00, 0x01, 0x01, 0x02,
                (byte) 0xFF, 0x06, 0x00, 0x64, //LinkAttributesTlv
                0x00, 0x13, 0x00, 0x04, //IPv4RouterIdOfRemoteNodeSubTlv
                0x00, 0x07, 0x08, 0x00,
                0x00, 0x16, 0x00, 0x04, //AdministrativeGroupSubTlv
                0x00, 0x09, 0x08, 0x00,
                0x00, 0x17, 0x00, 0x04, //MaximumLinkBandwidthSubTlv
                0x00, 0x09, 0x00, 0x00,
                0x00, 0x18, 0x00, 0x04, //MaximumReservableLinkBandwidthSubTlv
                0x00, 0x10, 0x00, 0x00,
                0x00, 0x19, 0x00, 0x04, //UnreservedBandwidthSubTlv
                0x00, 0x00, (byte) 0x90, 0x00,
                0x00, 0x1A, 0x00, 0x04, //TEDefaultMetricSubTlv
                0x00, (byte) 0x99, 0x09, 0x00,
                0x00, 0x1B, 0x00, 0x02, //LinkProtectionTypeSubTlv
                0x09, 0x00, 0x00, 0x00,
                0x00, 0x1C, 0x00, 0x01, //MPLSProtocolMaskSubTlv
                (byte) 0x80, 0x00, 0x00, 0x00,
                0x00, 0x1D, 0x00, 0x04, //IgpMetricSubTlv
                0x09, (byte) 0x89, 0x07, 0x00,
                0x00, 0x1E, 0x00, 0x04, //SharedRiskLinkGroupSubTlv
                0x04, 0x47, 0x00, 0x03,
                0x00, 0x1F, 0x00, 0x08, //OpaqueLinkAttributeSubTlv
                0x04, 0x49, 0x00, 0x04,
                0x04, 0x47, 0x00, 0x03,
                0x00, 0x20, 0x00, 0x04, //LinkNameAttributeSubTlv
                0x04, 0x47, 0x00, 0x03
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV,Local Node Descriptors TLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv,
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), RemoteNodeDescriptorsTLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv,
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), LinkDescriptorsTLV(LinkLocalRemoteIdentifiersSubTlv,
     * IPv4InterfaceAddressSubTlv, IPv4NeighborAddressSubTlv), NodeAttributesTlv(NodeFlagBitsSubTlv,
     * OpaqueNodePropertiesSubTlv, NodeNameSubTlv, ISISAreaIdentifierSubTlv, IPv4RouterIdOfLocalNodeSubTlv),
     * LinkAttributesTlv(IPv4RouterIdOfRemoteNodeSubTlv, IPv6LSRouterIdofRemoteNodeTlv, AdministrativeGroupSubTlv,
     * MaximumLinkBandwidthSubTlv, MaximumReservableLinkBandwidthSubTlv, UnreservedBandwidthSubTlv,
     * TEDefaultMetricSubTlv, LinkProtectionTypeSubTlv, MPLSProtocolMaskSubTlv, IgpMetricSubTlv,
     * SharedRiskLinkGroupSubTlv, OpaqueLinkAttributeSubTlv))
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest23() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x01, 0x10, // common header
                (byte) 0xE0, 0x10, 0x01, 0x0C, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x24, // Local Node Descriptors TLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x03, 0x00, 0x24, //RemoteNodeDescriptorsTLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x04, 0x00, 0x1C, //LinkDescriptorsTLV
                0x00, 0x06, 0x00, 0x08, //LinkLocalRemoteIdentifiersSubTlv
                0x01, 0x11, 0x00, 0x09,
                0x01, 0x21, 0x00, 0x09,
                0x00, 0x07, 0x00, 0x04, //IPv4InterfaceAddressSubTlv
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressSubTlv
                0x01, 0x011, 0x01, 0x10,
                (byte) 0xFF, 0x05, 0x00, 0x1C, //NodeAttributesTlv
                0x00, 0x0E, 0x00, 0x04, //OpaqueNodePropertiesSubTlv
                0x01, 0x011, 0x01, 0x10,
                0x00, 0x10, 0x00, 0x08, //ISISAreaIdentifierSubTlv
                0x20, 0x01, 0x22, 0x01,
                0x20, 0x01, 0x22, 0x01,
                0x00, 0x11, 0x00, 0x04, //IPv4RouterIdOfLocalNodeSubTlv
                0x00, 0x01, 0x01, 0x02,
                (byte) 0xFF, 0x06, 0x00, 0x5C, //LinkAttributesTlv
                0x00, 0x13, 0x00, 0x04, //IPv4RouterIdOfRemoteNodeSubTlv
                0x00, 0x07, 0x08, 0x00,
                0x00, 0x16, 0x00, 0x04, //AdministrativeGroupSubTlv
                0x00, 0x09, 0x08, 0x00,
                0x00, 0x17, 0x00, 0x04, //MaximumLinkBandwidthSubTlv
                0x00, 0x09, 0x00, 0x00,
                0x00, 0x18, 0x00, 0x04, //MaximumReservableLinkBandwidthSubTlv
                0x00, 0x10, 0x00, 0x00,
                0x00, 0x19, 0x00, 0x04, //UnreservedBandwidthSubTlv
                0x00, 0x00, (byte) 0x90, 0x00,
                0x00, 0x1A, 0x00, 0x04, //TEDefaultMetricSubTlv
                0x00, (byte) 0x99, 0x09, 0x00,
                0x00, 0x1B, 0x00, 0x02, //LinkProtectionTypeSubTlv
                0x09, 0x00, 0x00, 0x00,
                0x00, 0x1C, 0x00, 0x01, //MPLSProtocolMaskSubTlv
                (byte) 0x80, 0x00, 0x00, 0x00,
                0x00, 0x1D, 0x00, 0x04, //IgpMetricSubTlv
                0x09, (byte) 0x89, 0x07, 0x00,
                0x00, 0x1E, 0x00, 0x04, //SharedRiskLinkGroupSubTlv
                0x04, 0x47, 0x00, 0x03,
                0x00, 0x1F, 0x00, 0x08, //OpaqueLinkAttributeSubTlv
                0x04, 0x49, 0x00, 0x04,
                0x04, 0x47, 0x00, 0x03
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV,Local Node Descriptors TLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv,
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), RemoteNodeDescriptorsTLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv,
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), LinkDescriptorsTLV(LinkLocalRemoteIdentifiersSubTlv,
     * IPv4InterfaceAddressSubTlv, IPv4NeighborAddressSubTlv), NodeAttributesTlv(NodeFlagBitsSubTlv,
     * OpaqueNodePropertiesSubTlv, NodeNameSubTlv, ISISAreaIdentifierSubTlv, IPv4RouterIdOfLocalNodeSubTlv),
     * LinkAttributesTlv(IPv4RouterIdOfRemoteNodeSubTlv, IPv6LSRouterIdofRemoteNodeTlv, AdministrativeGroupSubTlv,
     * MaximumLinkBandwidthSubTlv, MaximumReservableLinkBandwidthSubTlv, UnreservedBandwidthSubTlv,
     * TEDefaultMetricSubTlv, LinkProtectionTypeSubTlv, MPLSProtocolMaskSubTlv, IgpMetricSubTlv,
     * SharedRiskLinkGroupSubTlv)) in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest24() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x01, 0x08, // common header
                (byte) 0xE0, 0x10, 0x01, 0x04, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x24, // Local Node Descriptors TLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x03, 0x00, 0x24, //RemoteNodeDescriptorsTLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x04, 0x00, 0x1C, //LinkDescriptorsTLV
                0x00, 0x06, 0x00, 0x08, //LinkLocalRemoteIdentifiersSubTlv
                0x01, 0x11, 0x00, 0x09,
                0x01, 0x21, 0x00, 0x09,
                0x00, 0x07, 0x00, 0x04, //IPv4InterfaceAddressSubTlv
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressSubTlv
                0x01, 0x011, 0x01, 0x10,
                (byte) 0xFF, 0x05, 0x00, 0x1C, //NodeAttributesTlv
                0x00, 0x0E, 0x00, 0x04, //OpaqueNodePropertiesSubTlv
                0x01, 0x011, 0x01, 0x10,
                0x00, 0x10, 0x00, 0x08, //ISISAreaIdentifierSubTlv
                0x20, 0x01, 0x22, 0x01,
                0x20, 0x01, 0x22, 0x01,
                0x00, 0x11, 0x00, 0x04, //IPv4RouterIdOfLocalNodeSubTlv
                0x00, 0x01, 0x01, 0x02,
                (byte) 0xFF, 0x06, 0x00, 0x54, //LinkAttributesTlv
                0x00, 0x13, 0x00, 0x04, //IPv4RouterIdOfRemoteNodeSubTlv
                0x00, 0x07, 0x08, 0x00,
                0x00, 0x16, 0x00, 0x04, //AdministrativeGroupSubTlv
                0x00, 0x09, 0x08, 0x00,
                0x00, 0x17, 0x00, 0x04, //MaximumLinkBandwidthSubTlv
                0x00, 0x09, 0x00, 0x00,
                0x00, 0x18, 0x00, 0x04, //MaximumReservableLinkBandwidthSubTlv
                0x00, 0x10, 0x00, 0x00,
                0x00, 0x19, 0x00, 0x04, //UnreservedBandwidthSubTlv
                0x00, 0x00, (byte) 0x90, 0x00,
                0x00, 0x1A, 0x00, 0x04, //TEDefaultMetricSubTlv
                0x00, (byte) 0x99, 0x09, 0x00,
                0x00, 0x1B, 0x00, 0x02, //LinkProtectionTypeSubTlv
                0x09, 0x00, 0x00, 0x00,
                0x00, 0x1C, 0x00, 0x01, //MPLSProtocolMaskSubTlv
                (byte) 0x80, 0x00, 0x00, 0x00,
                0x00, 0x1D, 0x00, 0x04, //IgpMetricSubTlv
                0x09, (byte) 0x89, 0x07, 0x00,
                0x00, 0x1E, 0x00, 0x08, //SharedRiskLinkGroupSubTlv
                0x04, 0x47, 0x00, 0x03,
                0x04, 0x47, 0x00, 0x03
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};
        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }

    /**
     * This test case checks for
     * LS Object (Routing Universe TLV,Local Node Descriptors TLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv,
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), RemoteNodeDescriptorsTLV(AutonomousSystemSubTlv, BGPLSidentifierSubTlv,
     * OSPFareaIDsubTlv, IgpRouterIdSubTlv), LinkDescriptorsTLV(LinkLocalRemoteIdentifiersSubTlv,
     * IPv4InterfaceAddressSubTlv, IPv4NeighborAddressSubTlv), NodeAttributesTlv(NodeFlagBitsSubTlv,
     * OpaqueNodePropertiesSubTlv, NodeNameSubTlv, ISISAreaIdentifierSubTlv, IPv4RouterIdOfLocalNodeSubTlv),
     * LinkAttributesTlv(IPv4RouterIdOfRemoteNodeSubTlv, IPv6LSRouterIdofRemoteNodeTlv, AdministrativeGroupSubTlv,
     * MaximumLinkBandwidthSubTlv, MaximumReservableLinkBandwidthSubTlv, UnreservedBandwidthSubTlv,
     * TEDefaultMetricSubTlv, LinkProtectionTypeSubTlv, MPLSProtocolMaskSubTlv, IgpMetricSubTlv))
     * in PcLSRpt message.
     */
    @Test
    public void lsReportMessageTest25() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] lsReportMsg = new byte[]{0x20, (byte) 0xE0, 0x00, (byte) 0xFC, // common header
                (byte) 0xE0, 0x10, 0x00, (byte) 0xF8, // LS Object Header
                0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // LS-ID
                (byte) 0xFF, 0x01, 0x00, 0x08, // Routing Universe TLV
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01,
                (byte) 0xFF, 0x02, 0x00, 0x24, // Local Node Descriptors TLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x03, 0x00, 0x24, //RemoteNodeDescriptorsTLV
                0x00, 0x01, 0x00, 0x04, //AutonomousSystemSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x02, 0x00, 0x04, //BGPLSidentifierSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x03, 0x00, 0x04, //OSPFareaIDsubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x04, 0x00, 0x08, //IgpRouterIdSubTlv
                0x00, 0x00, 0x00, 0x11,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xFF, 0x04, 0x00, 0x1C, //LinkDescriptorsTLV
                0x00, 0x06, 0x00, 0x08, //LinkLocalRemoteIdentifiersSubTlv
                0x01, 0x11, 0x00, 0x09,
                0x01, 0x21, 0x00, 0x09,
                0x00, 0x07, 0x00, 0x04, //IPv4InterfaceAddressSubTlv
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x08, 0x00, 0x04, //IPv4NeighborAddressSubTlv
                0x01, 0x011, 0x01, 0x10,
                (byte) 0xFF, 0x05, 0x00, 0x1C, //NodeAttributesTlv
                0x00, 0x0E, 0x00, 0x04, //OpaqueNodePropertiesSubTlv
                0x01, 0x011, 0x01, 0x10,
                0x00, 0x10, 0x00, 0x08, //ISISAreaIdentifierSubTlv
                0x20, 0x01, 0x22, 0x01,
                0x20, 0x01, 0x22, 0x01,
                0x00, 0x11, 0x00, 0x04, //IPv4RouterIdOfLocalNodeSubTlv
                0x00, 0x01, 0x01, 0x02,
                (byte) 0xFF, 0x06, 0x00, 0x48, //LinkAttributesTlv
                0x00, 0x13, 0x00, 0x04, //IPv4RouterIdOfRemoteNodeSubTlv
                0x00, 0x07, 0x08, 0x00,
                0x00, 0x16, 0x00, 0x04, //AdministrativeGroupSubTlv
                0x00, 0x09, 0x08, 0x00,
                0x00, 0x17, 0x00, 0x04, //MaximumLinkBandwidthSubTlv
                0x00, 0x09, 0x00, 0x00,
                0x00, 0x18, 0x00, 0x04, //MaximumReservableLinkBandwidthSubTlv
                0x00, 0x10, 0x00, 0x00,
                0x00, 0x19, 0x00, 0x04, //UnreservedBandwidthSubTlv
                0x00, 0x00, (byte) 0x90, 0x00,
                0x00, 0x1A, 0x00, 0x04, //TEDefaultMetricSubTlv
                0x00, (byte) 0x99, 0x09, 0x00,
                0x00, 0x1B, 0x00, 0x02, //LinkProtectionTypeSubTlv
                0x09, 0x00, 0x00, 0x00,
                0x00, 0x1C, 0x00, 0x01, //MPLSProtocolMaskSubTlv
                (byte) 0x80, 0x00, 0x00, 0x00,
                0x00, 0x1D, 0x00, 0x04, //IgpMetricSubTlv
                0x09, (byte) 0x89, 0x07, 0x00
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(lsReportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testReportMsg = {0};

        assertThat(message, instanceOf(PcepLSReportMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testReportMsg = new byte[readLen];
        buf.readBytes(testReportMsg, 0, readLen);

        assertThat(testReportMsg, is(lsReportMsg));
    }
}
