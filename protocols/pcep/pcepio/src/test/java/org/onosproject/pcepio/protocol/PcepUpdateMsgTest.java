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

public class PcepUpdateMsgTest {
    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv), ERO in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest1() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x30,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x1C, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, (byte) 0xb6, 0x02, 0x4e, 0x1f, //StatefulIPv4LspIdentidiersTlv
                0x00, 0x01, (byte) 0x80, 0x01, (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x07, 0x10, 0x00, 0x04 }; //ERO object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP(SymbolicPathNameTlv), LSP (StatefulIPv4LspIdentidiersTlv,
     * SymbolicPathNameTlv, StatefulLspErrorCodeTlv), ERO, LSPA, Metric-list in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest2() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x94,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x2c, 0x00, 0x00, 0x10, 0x03, //LSP object // lsp object
                0x00, 0x12, 0x00, 0x10, (byte) 0xb6, 0x02, 0x4e, 0x1f, //StatefulIPv4LspIdentidiersTlv
                0x00, 0x01, (byte) 0x80, 0x01, (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x07, 0x10, 0x00, 0x24, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x02, 0x04, 0x00, 0x01, 0x08, 0x12, 0x01,
                0x01, 0x02, 0x04, 0x00, 0x01, 0x08, 0x12, 0x01, 0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01,
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);
        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv),
     * ERO objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest3() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x38,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x1C, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x07, 0x10, 0x00, 0x04 }; //ERO object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv,
     * SymbolicPathNameTlv), ERO objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest4() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x40,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x07, 0x10, 0x00, 0x04 }; //ERO object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv,
     * SymbolicPathNameTlv), ERO objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest5() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x40,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x07, 0x10, 0x00, 0x04 }; //ERO object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv,
     * StatefulLspErrorCodeTlv), ERO objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest6() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x48,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x2c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x07, 0x10, 0x00, 0x04 };

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));

    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv,
     * StatefulLspErrorCodeTlv), ERO objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest7() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x48,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x2c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x07, 0x10, 0x00, 0x04}; //ERO object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);
        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv,
     * StatefulLspErrorCodeTlv), ERO (IPv4SubObject) objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest8() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x50,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x2c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x07, 0x10, 0x00, 0x0c, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00 };

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv,
     * StatefulLspErrorCodeTlv), ERO (IPv4SubObject, IPv4SubObject) objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest9() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x58,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x2c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00 };

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv,
     * StatefulLspErrorCodeTlv), ERO (IPv4SubObject, IPv4SubObject), LSPA objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest10() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x6c,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x2c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00 };

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv,
     * StatefulLspErrorCodeTlv), ERO (IPv4SubObject, IPv4SubObject),LSPA, Metric objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest11() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x78,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x2c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv,
     * StatefulLspErrorCodeTlv), ERO (IPv4SubObject, IPv4SubObject),LSPA, metric objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest12() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x70,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x2c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv,
     * StatefulLspErrorCodeTlv), ERO (IPv4SubObject, IPv4SubObject),LSPA, metric objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest13() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x70,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x2c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv),
     * ERO (IPv4SubObject, IPv4SubObject),LSPA, metric Object objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest14() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x68,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv),
     * ERO (IPv4SubObject, IPv4SubObject),LSPA, metric objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest15() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x68,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv),
     * ERO (IPv4SubObject, IPv4SubObject),LSPA, metric objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest16() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x60,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x1C, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv),
     * ERO (IPv4SubObject, IPv4SubObject),LSPA objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest17() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x54,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x1c, 0x00, 0x00, 0x10, 0x05, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00 };

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv),
     * ERO (IPv4SubObject, IPv4SubObject),Metric objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest18() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x4c,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x1C, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv),
     * ERO (IPv4SubObject, IPv4SubObject),Metric-list objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest19() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x58,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x1C, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20, //Metric object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv,
     * StatefulLspErrorCodeTlv),ERO (IPv4SubObject, IPv4SubObject),LSPA, Bandwidth, Metric objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest20() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x80,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x2c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv),
     * ERO (IPv4SubObject, IPv4SubObject), Bandwidth objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest21() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x48,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x1C, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00 }; //Bandwidth object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv),
     * ERO (IPv4SubObject, IPv4SubObject), LSPA, Bandwidth objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest22() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x5C,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x1C, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00 }; //Bandwidth object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv),
     * ERO (IPv4SubObject, IPv4SubObject), LSPA, Bandwidth, Metric objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest23() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x68,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x1C, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv)
     * ERO (IPv4SubObject, IPv4SubObject), LSPA, Bandwidth, Metric objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest24() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x70,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv)
     * ERO (IPv4SubObject, IPv4SubObject), LSPA, Bandwidth, Metric objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest25() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x70,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv,
     * StatefulLspErrorCodeTlv) ERO (IPv4SubObject, IPv4SubObject), LSPA, Bandwidth,
     * Metric objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest26() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x78,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x2c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);
        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv,
     * StatefulLspErrorCodeTlv) ERO (IPv4SubObject, IPv4SubObject), LSPA, Bandwidth,
     * Metric objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest27() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x78,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x2c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv,
     * SymbolicPathNameTlv, StatefulLspErrorCodeTlv), ERO (IPv4SubObject, IPv4SubObject),
     * LSPA, Bandwidth, Metric objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest28() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x80,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x2c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv),
     * ERO (IPv4SubObject, IPv4SubObject), LSPA, Bandwidth, Metric objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest29() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x68,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x1C, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP,
     * ERO (IPv4SubObject, IPv4SubObject), LSPA, Bandwidth, Metric objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest30() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x54,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x08, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20}; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulLspErrorCodeTlv),
     * ERO (IPv4SubObject, IPv4SubObject), LSPA, Bandwidth, Metric objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest31() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x5c,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP,
     * ERO (IPv4SubObject, IPv4SubObject), LSPA, Bandwidth, Metric objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest32() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x54,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x8, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv),
     * ERO (IPv4SubObject, IPv4SubObject), LSPA, Bandwidth, Metric objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest33() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x5c,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, SymbolicPathNameTlv),
     * ERO (IPv4SubObject, IPv4SubObject), LSPA, Bandwidth, Metric objects in PcUpd message.
     */
    @Test
    public void pcepUpdateMsgTest34() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x64,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testupdateMsg = buf.array();

        int readLen = buf.writerIndex() - 0;
        testupdateMsg = new byte[readLen];
        buf.readBytes(testupdateMsg, 0, readLen);

        assertThat(testupdateMsg, is(updateMsg));
    }
}
