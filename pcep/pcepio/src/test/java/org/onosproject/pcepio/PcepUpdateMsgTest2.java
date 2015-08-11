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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepFactories;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.onosproject.pcepio.protocol.PcepMessageReader;
import org.onosproject.pcepio.protocol.PcepUpdateMsg;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Test cases for PCEP update message.
 */
public class PcepUpdateMsgTest2 {

    protected static final Logger log = LoggerFactory.getLogger(PcepUpdateMsgTest2.class);

    @Before
    public void startUp() {
    }

    @After
    public void tearDown() {

    }

    @Test
    public void pcepUpdateMsgTest1() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x8c,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x38, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest2() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x68,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x1C, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSP object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest3() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x54,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x08, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
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
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest4() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x5c,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
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
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest5() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x60,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x14, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
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
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest6() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x5c,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
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
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest7() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x64,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
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
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest8() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x5c,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x1C, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
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
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest9() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x58,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x18, 0x00, 0x00, 0x10, 0x03,
                0x00, 0x15, 0x00, 0x0c, //StatefulRsvpErrorSpecTlv
                0x00, 0x0c, 0x06, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x18, 0x00, 0x05,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
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
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest10() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x50,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPa object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00 }; //Bandwidth object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest11() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x54,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x14, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x05, 0x20, 0x00, 0x04}; //Bandwidth object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest12() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x50,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
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
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest13() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x58,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
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
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest14() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x60,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x1C, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
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
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest15() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x4c,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x08, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //lspa object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest16() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x54,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
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
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest17() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x58,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x14, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
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
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest18() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x54,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
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
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest19() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x5c,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00,
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest20() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x54,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x1C, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest21() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x40,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x08, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest22() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x48,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest23() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x4c,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x14, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest24() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x48,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest25() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x50,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, //Bandwidth object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest26() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x54,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x1C, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x12, 0x00, 0x10, //StatefulIPv4LspIdentidiersTlv
                (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x00, 0x01, (byte) 0x80, 0x01,
                (byte) 0xb6, 0x02, 0x4e, 0x1f, (byte) 0xb6, 0x02, 0x4e, 0x20,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00 };

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest27() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x34,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x08, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00 }; //Bandwidth object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest28() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x40,
                0x21, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x20, 0x10, 0x00, 0x10, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x14, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, //StatefulLspErrorCodeTlv
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest29() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x54,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x14, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x09, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, //LSPA object
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x00, 0x00 };

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest30() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x48,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x14, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x05, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00 }; //Bandwidth object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest31() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x4c,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x14, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
            } else {
                Assert.fail("test case failed");
                log.debug("not equal");
            }
        } else {
            Assert.fail("test case failed");
            log.debug("not equal");
        }
    }

    @Test
    public void pcepUpdateMsgTest32() throws PcepParseException {
        byte[] updateMsg = new byte[] {0x20, 0x0b, 0x00, (byte) 0x64,
                0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, //SRP object
                0x00, 0x11, 0x00, 0x02,  0x54, 0x31, 0x00, 0x00, //SymbolicPathNameTlv
                0x20, 0x10, 0x00, 0x14, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x17, 0x00, 0x08, //StatefulLspDbVerTlv
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                0x07, 0x10, 0x00, 0x14, 0x01, 0x08, 0x11, 0x01, //ERO Object
                0x01, 0x01, 0x04, 0x00, 0x01, 0x08, 0x11, 0x01,
                0x01, 0x01, 0x04, 0x00,
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20, //Metric object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20, //Metric object
                0x06, 0x10, 0x00, 0x0c, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00, 0x00, 0x20 }; //Metric object

        byte[] testupdateMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;
        try {
            message = reader.readFrom(buffer);
        } catch (PcepParseException e) {
            e.printStackTrace();
        }

        if (message instanceof PcepUpdateMsg) {
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            message.writeTo(buf);
            testupdateMsg = buf.array();

            int iReadLen = buf.writerIndex() - 0;
            testupdateMsg = new byte[iReadLen];
            buf.readBytes(testupdateMsg, 0, iReadLen);

            if (Arrays.equals(updateMsg, testupdateMsg)) {
                Assert.assertArrayEquals(updateMsg, testupdateMsg);
                log.debug("updateMsg are equal :" + updateMsg);
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

