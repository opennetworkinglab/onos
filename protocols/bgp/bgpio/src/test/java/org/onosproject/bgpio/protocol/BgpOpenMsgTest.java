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
package org.onosproject.bgpio.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.BgpHeader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;

/**
 * Test cases for BGP Open Message.
 */
public class BgpOpenMsgTest {

    /**
     * This test case checks open message without optional parameter.
     */
    @Test
    public void openMessageTest1() throws BgpParseException {
        //Open message without optional parameter
        byte[] openMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     0x00, 0x1d, 0x01, 0X04, (byte) 0xfe, 0x09, 0x00,
                                     (byte) 0xb4, (byte) 0xc0, (byte) 0xa8, 0x00, 0x0f,
                                     0x00};

        byte[] testOpenMsg;
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();
        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpOpenMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);

        assertThat(testOpenMsg, is(openMsg));
    }

    /**
     * This test case checks open message with Multiprotocol extension
     * capability.
     */
    @Test
    public void openMessageTest2() throws BgpParseException {

        // OPEN Message (MultiProtocolExtension-CAPABILITY).
        byte[] openMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, 0x00, 0x25,
                                     0x01, //BGP Header
                                     0X04, //Version
                                     (byte) 0x00, (byte) 0xc8, // AS Number
                                     0x00, (byte) 0xb4, // Hold time
                                     (byte) 0xb6, (byte) 0x02, 0x5d,
                                     (byte) 0xc8, // BGP Identifier
                                     0x08, 0x02, 0x06, // Opt Parameter length
                                     0x01, 0x04, 0x00, 0x00, 0x00, (byte) 0xc8}; // Multiprotocol CAPABILITY

        byte[] testOpenMsg;
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpOpenMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);

        assertThat(testOpenMsg, is(openMsg));
    }

    /**
     * This test case checks open message with Four-octet AS number
     * capability.
     */
    @Test
    public void openMessageTest3() throws BgpParseException {

        // OPEN Message (Four-Octet AS number capability).
        byte[] openMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, 0x00, 0x25,
                                     0x01, //BGPHeader
                                     0X04, //Version
                                     (byte) 0x00, (byte) 0xc8, //AS Number
                                     0x00, (byte) 0xb4, //Hold Time
                                     (byte) 0xb6, (byte) 0x02, 0x5d,
                                     (byte) 0xc8, //BGP Identifier
                                     0x08, 0x02, 0x06, //Opt Parameter Length
                                     0x41, 0x04, 0x00, 0x01, 0x00, 0x01}; //Four Octet AS Number-CAPABILITY-TLV

        byte[] testOpenMsg;
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpOpenMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);

        assertThat(testOpenMsg, is(openMsg));
    }

    /**
     * This test case checks open message with capabilities.
     */
    @Test
    public void openMessageTest4() throws BgpParseException {

        // OPEN Message with capabilities.
        byte[] openMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, 0x00, 0x2b,
                                     0x01, //BGPHeader
                                     0X04, //Version
                                     (byte) 0x00, (byte) 0xc8, //AS Number
                                     0x00, (byte) 0xb4, //Hold Time
                                     (byte) 0xb6, (byte) 0x02, 0x5d, (byte) 0xc8, //BGP Identifier
                                     0x0e, 0x02, 0x0c, //Opt Parameter Length
                                     0x01, 0x04, 0x00, 0x00, 0x00, (byte) 0xc8, // Multiprotocol extension capability
                                     0x41, 0x04, 0x00, 0x01, 0x00, 0x01}; //Four Octet AS Number-CAPABILITY-TLV

        byte[] testOpenMsg;
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpOpenMsg.class));

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);

        assertThat(testOpenMsg, is(openMsg));
    }

    /**
     * In this test case, Invalid version is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void openMessageTest5() throws BgpParseException {

        // OPEN Message with invalid version number.
        byte[] openMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, 0x00, 0x1d, 0x01, 0X05,
                (byte) 0xfe, 0x09, 0x00, (byte) 0xb4,
                (byte) 0xc0, (byte) 0xa8, 0x00, 0x0f,
                0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();
        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpOpenMsg.class));
    }

    /**
     * In this test case, Marker is set as 0 in input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void openMessageTest6() throws BgpParseException {

        // OPEN Message with marker set to 0.
        byte[] openMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0x00, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff,
                 0x00, 0x00, 0x1d, 0x01, 0X04,
                (byte) 0xfe, 0x09, 0x00, (byte) 0xb4,
                (byte) 0xc0, (byte) 0xa8, 0x00, 0x0f,
                0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();
        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpOpenMsg.class));
    }

    /**
     * In this test case, Invalid message length is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void openMessageTest7() throws BgpParseException {

        // OPEN Message with invalid header length.
        byte[] openMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, 0x00, 0x1e, 0x01, 0X04,
                (byte) 0xfe, 0x09, 0x00, (byte) 0xb4,
                (byte) 0xc0, (byte) 0xa8, 0x00, 0x0f,
                0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();
        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpOpenMsg.class));
    }

    /**
     * In this test case, Invalid message type is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void openMessageTest8() throws BgpParseException {

        // OPEN Message with invalid message type.
        byte[] openMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x00, 0x1d, 0x05, 0X04,
                                     (byte) 0xfe, 0x09, 0x00, (byte) 0xb4, (byte) 0xc0, (byte) 0xa8, 0x00, 0x0f, 0x00 };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();
        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpOpenMsg.class));
    }

    /**
     * This test case checks open message with route policy distribution capability.
     */
    @Test
    public void openMessageTest9() throws BgpParseException {

        // OPEN Message with capabilities.
        byte[] openMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                      (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                      (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                      0x00, 0x3d, 0x01, 0x04, 0x00, (byte) 0xc8, 0x00, (byte) 0xb4, (byte) 0xc0,
                                      (byte) 0xa8, 0x07, 0x35, 0x20, 0x02, 0x1e, 0x01,
                                     0x04, 00, 0x01, 0x00, 0x01, 0x41, 0x04, 0x00, 0x00, 0x00, (byte) 0xc8, 0x01,
                                     0x04, 0x40, 0x04, 0x00, 0x47, 0x01, 0x04, 0x00, 0x01, 0x00, (byte) 0x85,
                                     (byte) 0x81, 0x04, 0x00, 0x01, (byte) 0x85, 0x01 }; //RPD capability

        byte[] testOpenMsg;
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpOpenMsg.class));

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testOpenMsg = new byte[readLen];
        buf.readBytes(testOpenMsg, 0, readLen);

        assertThat(testOpenMsg, is(openMsg));
    }

    /**
     * In this test case, Invalid multiprotocol capability length is given as input and expecting an exception.
     */
    @Test(expected = BgpParseException.class)
    public void openMessageTest10() throws BgpParseException {

        // OPEN Message with invalid message type.
        byte[] openMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                     0x00, 0x3d, 0x01, 0x04, 0x00, (byte) 0xc8, 0x00, (byte) 0xb4, (byte) 0xc0,
                                     (byte) 0xa8, 0x07, 0x35, 0x20, 0x02, 0x1e, 0x01, 0x04, 00, 0x01, 0x00, 0x01, 0x41,
                                     0x04, 0x00, 0x00, 0x00, (byte) 0xc8, 0x01, 0x04, 0x40, 0x04, 0x00,
                                     0x47, 0x01, 0x04, 0x00, 0x01, 0x00, (byte) 0x85,
                                    (byte) 0x81, 0x05, 0x00, 0x01, (byte) 0x85, 0x01 }; //RPD capability

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(openMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();
        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpOpenMsg.class));
    }
}
