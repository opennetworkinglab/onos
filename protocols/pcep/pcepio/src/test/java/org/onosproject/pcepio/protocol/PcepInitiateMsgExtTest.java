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

public class PcepInitiateMsgExtTest {

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv,
     * SymbolicPathNameTlv, StatefulLspDbVerTlv, StatefulLspErrorCodeTlv, StatefulRsvpErrorSpecTlv),
     * END-POINTS, ERO, LSPA, BANDWIDTH, METRIC-LIST objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest1() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv, StatefulLspDbVerTlv,
        // StatefulLspErrorCodeTlv, StatefulRsvpErrorSpecTlv), END-POINTS, ERO, LSPA, BANDWIDTH, METRIC-LIST.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0xA4,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x38, 0x00, 0x00, 0x10, 0x03,
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x04, 0x54, 0x31, 0x32, 0x33, //SymbolicPathNameTlv
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, //ERO object
                0x01, 0x08, 0x0C, 0x01, 0x01, 0x01, 0x00, 0x00,
                0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01, //Metric object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20}; //Metric object

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv,
     * SymbolicPathNameTlv, StatefulLspDbVerTlv, StatefulLspErrorCodeTlv,
     * StatefulRsvpErrorSpecTlv), END-POINTS, ERO, LSPA, BANDWIDTH, METRIC OBJECT
     * objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest2() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv, StatefulLspDbVerTlv,
        // StatefulLspErrorCodeTlv, StatefulRsvpErrorSpecTlv), END-POINTS, ERO, LSPA, BANDWIDTH, METRIC OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0xA8,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x48, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x04, 0x54, 0x31, 0x32, 0x33, //SymbolicPathNameTlv
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x00, 0x15, 0x00, 0x0c, //StatefulRsvpErrorSpecTlv
                0x00, 0x0c, 0x06, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x18, 0x00, 0x05,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00,
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv,
     * StatefulLspDbVerTlv, StatefulLspErrorCodeTlv, StatefulRsvpErrorSpecTlv), END-POINTS,
     * ERO, LSPA, BANDWIDTH objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest3() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv, StatefulLspDbVerTlv,
        // StatefulLspErrorCodeTlv, StatefulRsvpErrorSpecTlv), END-POINTS, ERO, LSPA, BANDWIDTH.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x8c,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x38, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x04, 0x54, 0x31, 0x32, 0x33, //SymbolicPathNameTlv
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                // 0x00, 0x15, 0x00, 0x0c, //StatefulRsvpErrorSpecTlv
                //0x00, 0x0c, 0x06, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x18, 0x00, 0x05,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv,
     * SymbolicPathNameTlv, StatefulLspDbVerTlv, StatefulLspErrorCodeTlv, StatefulRsvpErrorSpecTlv),
     * END-POINTS, ERO, LSPA objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest4() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv, StatefulLspDbVerTlv,
        // StatefulLspErrorCodeTlv, StatefulRsvpErrorSpecTlv), END-POINTS, ERO, LSPA.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x84,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x38, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x04, 0x54, 0x31, 0x32, 0x33, //SymbolicPathNameTlv
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                // 0x00, 0x15, 0x00, 0x0c, //StatefulRsvpErrorSpecTlv
                // 0x00, 0x0c, 0x06, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x18, 0x00, 0x05,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv,
     * SymbolicPathNameTlv, StatefulLspDbVerTlv, StatefulLspErrorCodeTlv), END-POINTS, ERO, LSPA
     * objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest5() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv, StatefulLspDbVerTlv,
        // StatefulLspErrorCodeTlv), END-POINTS, ERO, LSPA.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x84,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x38, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x04, 0x54, 0x31, 0x32, 0x33, //SymbolicPathNameTlv
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv,
     * SymbolicPathNameTlv, StatefulLspDbVerTlv, StatefulLspErrorCodeTlv), END-POINTS, ERO, LSPA,
     * BANDWIDTH OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest6() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv, StatefulLspDbVerTlv,
        // StatefulLspErrorCodeTlv), END-POINTS, ERO, LSPA, BANDWIDTH OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x8c,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x38, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x04, 0x54, 0x31, 0x32, 0x33, //SymbolicPathNameTlv
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv,
     * SymbolicPathNameTlv, StatefulLspDbVerTlv, StatefulLspErrorCodeTlv), END-POINTS, ERO,
     * LSPA, BANDWIDTH, METRIC OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest7() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv, StatefulLspDbVerTlv,
        // StatefulLspErrorCodeTlv), END-POINTS, ERO, LSPA, BANDWIDTH, METRIC OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x98,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x38, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x04, 0x54, 0x31, 0x32, 0x33, //SymbolicPathNameTlv
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00,
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv,
     * SymbolicPathNameTlv, StatefulLspDbVerTlv), END-POINTS, ERO, LSPA, BANDWIDTH, METRIC OBJECT
     * objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest8() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv, StatefulLspDbVerTlv),
        // END-POINTS, ERO, LSPA, BANDWIDTH, METRIC OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x90,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x30, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x04, 0x54, 0x31, 0x32, 0x33, //SymbolicPathNameTlv
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00,
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv,
     * SymbolicPathNameTlv, StatefulLspDbVerTlv), END-POINTS, ERO, LSPA, BANDWIDTH OBJECT
     * objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest9() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv, StatefulLspDbVerTlv),
        // END-POINTS, ERO, LSPA, BANDWIDTH OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x84,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x30, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x04, 0x54, 0x31, 0x32, 0x33, //SymbolicPathNameTlv
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00}; //Bandwidth object

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv,
     * SymbolicPathNameTlv), END-POINTS, ERO, LSPA OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest10() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv),
        // END-POINTS, ERO, LSPA OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x70,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x10, 0x03,
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x04, 0x54, 0x31, 0x32, 0x33, //SymbolicPathNameTlv
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv,
     * SymbolicPathNameTlv, StatefulLspDbVerTlv), END-POINTS, ERO, LSPA OBJECT
     * objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest11() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv, StatefulLspDbVerTlv),
        // END-POINTS, ERO, LSPA OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x7C,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x30, 0x00, 0x00, 0x10, 0x03,
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x04, 0x54, 0x31, 0x32, 0x33, //SymbolicPathNameTlv
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv,
     * SymbolicPathNameTlv), END-POINTS, ERO, LSPA, BANDWIDTH OBJECT
     * objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest12() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv),
        // END-POINTS, ERO, LSPA, BANDWIDTH OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x78,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x10, 0x03,
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x04, 0x54, 0x31, 0x32, 0x33, //SymbolicPathNameTlv
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00}; //Bandwidth object

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv,
     * SymbolicPathNameTlv, StatefulLspDbVerTlv), END-POINTS, ERO, LSPA, BANDWIDTH , METRIC OBJECT
     * objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest13() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv, SymbolicPathNameTlv, StatefulLspDbVerTlv),
        // END-POINTS, ERO, LSPA, BANDWIDTH , METRIC OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x84,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x10, 0x03,
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x04, 0x54, 0x31, 0x32, 0x33, //SymbolicPathNameTlv
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv),
     * END-POINTS, ERO, LSPA, BANDWIDTH , METRIC OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest14() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv),
        // END-POINTS, ERO, LSPA, BANDWIDTH , METRIC OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x7c,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x1c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv),
     * END-POINTS, ERO, LSPA, BANDWIDTH OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest15() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv),
        // END-POINTS, ERO, LSPA, BANDWIDTH OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x70,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x1c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00}; //Bandwidth object

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv),
     * END-POINTS, ERO, LSPA OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest16() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv),
        // END-POINTS, ERO, LSPA OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x68,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x1c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv), END-POINTS, ERO, LSPA OBJECT
     * objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest17() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (StatefulIPv4LspIdentidiersTlv), END-POINTS, ERO, LSPA OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x60,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x1c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv), END-POINTS, ERO, LSPA,
     * BANDWIDTH OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest18() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (StatefulIPv4LspIdentidiersTlv), END-POINTS, ERO, LSPA, BANDWIDTH OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x68,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x1c, 0x00, 0x00, 0x10, 0x03,
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00}; //Bandwidth object

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;


        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv), END-POINTS, ERO, LSPA,
     * BANDWIDTH, METRIC OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest19() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (StatefulIPv4LspIdentidiersTlv), END-POINTS, ERO, LSPA, BANDWIDTH, METRIC OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x74,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x1c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv), END-POINTS, ERO, LSPA,
     * BANDWIDTH, METRIC OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest20() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (StatefulIPv4LspIdentidiersTlv), END-POINTS, ERO, LSPA, BANDWIDTH, METRIC OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x64,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x1c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x04, //ERO object
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv), END-POINTS, ERO, LSPA,
     * BANDWIDTH OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest21() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (StatefulIPv4LspIdentidiersTlv), END-POINTS, ERO, LSPA, BANDWIDTH OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x58,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x1c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x04,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00}; //Bandwidth object

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (StatefulIPv4LspIdentidiersTlv), END-POINTS, ERO,
     * LSPA OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest22() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (StatefulIPv4LspIdentidiersTlv), END-POINTS, ERO, LSPA OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x50,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x1c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x04, //ERO object
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv),
     * END-POINTS, ERO, LSPA OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest23() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv), END-POINTS, ERO, LSPA OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x58,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x1c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x04, //ERO object
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv),
     * END-POINTS, ERO, LSPA BANDWIDTH OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest25() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv), END-POINTS, ERO, LSPA BANDWIDTH OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x60,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x1c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x04, //ERO object
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00}; //Bandwidth object

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv), END-POINTS,
     * ERO, LSPA, BANDWIDTH, METRIC OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest26() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, StatefulIPv4LspIdentidiersTlv), END-POINTS,
        // ERO, LSPA, BANDWIDTH, METRIC OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x6C,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x1c, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x04, //ERO object
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01}; //Metric object

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, SymbolicPathNameTlv), END-POINTS, ERO, LSPA,
     * BANDWIDTH, METRIC OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest27() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, SymbolicPathNameTlv), END-POINTS, ERO, LSPA, BANDWIDTH, METRIC OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x60,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x04, //ERO object
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01}; //Metric object

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, SymbolicPathNameTlv), END-POINTS, ERO,
     * LSPA, BANDWIDTH OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest28() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, SymbolicPathNameTlv), END-POINTS, ERO, LSPA, BANDWIDTH OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x54,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x04, //ERO object
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00}; //Bandwidth object

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, SymbolicPathNameTlv),
     * END-POINTS, ERO, LSPA OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest29() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, SymbolicPathNameTlv), END-POINTS, ERO, LSPA OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x4C,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x04, //ERO object
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv, SymbolicPathNameTlv),
     * END-POINTS, ERO, LSPA OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest30() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv, SymbolicPathNameTlv), END-POINTS, ERO, LSPA OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x5C,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP (SymbolicPathNameTlv), END-POINTS, ERO, LSPA OBJECT
     * objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest31() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP (SymbolicPathNameTlv), END-POINTS, ERO, LSPA OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x54,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP ( StatefulLspDbVerTlv), END-POINTS,
     * ERO, LSPA, BANDWIDTH, METRIC OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest32() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP ( StatefulLspDbVerTlv), END-POINTS,
        // ERO, LSPA, BANDWIDTH, METRIC OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x64,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x14, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x04, //ERO object
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01}; //Metric object

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP ( StatefulLspDbVerTlv), END-POINTS,
     * ERO, LSPA, BANDWIDTH OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest33() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP ( StatefulLspDbVerTlv), END-POINTS,
        // ERO, LSPA, BANDWIDTH OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x58,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x14, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x04, //ERO object
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00}; //Bandwidth object

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP ( StatefulLspDbVerTlv), END-POINTS,
     * ERO, LSPA OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest34() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP ( StatefulLspDbVerTlv), END-POINTS,
        // ERO, LSPA OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x50,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x14, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x04, //ERO object
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP ( StatefulLspDbVerTlv), END-POINTS,
     * ERO, LSPA OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest35() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP ( StatefulLspDbVerTlv), END-POINTS,
        // ERO, LSPA OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x60,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x14, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }

    /**
     * This test case checks for SRP, LSP ( StatefulLspDbVerTlv), END-POINTS,
     * ERO, LSPA OBJECT objects in PcInitiate message.
     */
    @Test
    public void initiateMessageTest36() throws PcepParseException, PcepOutOfBoundMessageException {

        // SRP, LSP ( StatefulLspDbVerTlv), END-POINTS,
        // ERO, LSPA OBJECT.
        //
        byte[] initiateCreationMsg = new byte[]{0x20, 0x0C, 0x00, (byte) 0x58,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x14, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x04, 0x12, 0x00, 0x0C, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, //Endpoints Object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x0C, 0x01, //ERO object
                0x01, 0x01, 0x00, 0x00, 0x01, 0x08, 0x0C, 0x01, 0x01, 0x02, 0x00, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00};

        byte[] testInitiateCreationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(initiateCreationMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        assertThat(message, instanceOf(PcepInitiateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        message.writeTo(buf);

        testInitiateCreationMsg = buf.array();

        int iReadLen = buf.writerIndex();
        testInitiateCreationMsg = new byte[iReadLen];
        buf.readBytes(testInitiateCreationMsg, 0, iReadLen);

        assertThat(testInitiateCreationMsg, is(initiateCreationMsg));
    }
}

