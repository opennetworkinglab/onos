/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.bgp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.onosproject.bgpio.exceptions.BGPParseException;
import org.onosproject.bgpio.protocol.BGPFactories;
import org.onosproject.bgpio.protocol.BGPMessage;
import org.onosproject.bgpio.protocol.BGPMessageReader;
import org.onosproject.bgpio.protocol.BGPNotificationMsg;
import org.onosproject.bgpio.types.BGPHeader;

/**
 * Test for Notification message.
 */
public class BgpNotificationMsgTest {

    /**
     * Notification message with error code, error subcode and data.
     *
     * @throws BGPParseException while decoding and encoding notification message
     */
    @Test
    public void bgpNotificationMessageTest1() throws BGPParseException {
        byte[] notificationMsg = new byte[] {(byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff, 0x00,
                                             0x17, 0x03, 0x02, 0x02,
                                             (byte) 0xfe, (byte) 0xb0};

        byte[] testNotificationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(notificationMsg);

        BGPMessageReader<BGPMessage> reader = BGPFactories.getGenericReader();
        BGPMessage message = null;
        BGPHeader bgpHeader = new BGPHeader();

        message = reader.readFrom(buffer, bgpHeader);
        assertThat(message, instanceOf(BGPNotificationMsg.class));

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testNotificationMsg = buf.array();

        int iReadLen = buf.writerIndex() - 0;
        testNotificationMsg = new byte[iReadLen];
        buf.readBytes(testNotificationMsg, 0, iReadLen);
        assertThat(testNotificationMsg, is(notificationMsg));
    }

    /**
     * Notification message without data.
     *
     * @throws BGPParseException  while decoding and encoding notification message
     */
    @Test
    public void bgpNotificationMessageTest2() throws BGPParseException {
        byte[] notificationMsg = new byte[] {(byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff, 0x00,
                                             0x15, 0x03, 0x02, 0x00};

        byte[] testNotificationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(notificationMsg);

        BGPMessageReader<BGPMessage> reader = BGPFactories.getGenericReader();
        BGPMessage message = null;
        BGPHeader bgpHeader = new BGPHeader();

        message = reader.readFrom(buffer, bgpHeader);
        assertThat(message, instanceOf(BGPNotificationMsg.class));

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testNotificationMsg = buf.array();

        int iReadLen = buf.writerIndex() - 0;
        testNotificationMsg = new byte[iReadLen];
        buf.readBytes(testNotificationMsg, 0, iReadLen);
        assertThat(testNotificationMsg, is(notificationMsg));
    }

    //Negative scenarios
    /**
     * Notification message with wrong maker value.
     *
     * @throws BGPParseException while decoding and encoding notification message
     */
    @Test(expected = BGPParseException.class)
    public void bgpNotificationMessageTest3() throws BGPParseException {
        byte[] notificationMsg = new byte[] {(byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                              0x01, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff, 0x00,
                                             0x15, 0x03, 0x02, 0x00};

        byte[] testNotificationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(notificationMsg);

        BGPMessageReader<BGPMessage> reader = BGPFactories.getGenericReader();
        BGPMessage message = null;
        BGPHeader bgpHeader = new BGPHeader();

        message = reader.readFrom(buffer, bgpHeader);
        assertThat(message, instanceOf(BGPNotificationMsg.class));

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testNotificationMsg = buf.array();

        int iReadLen = buf.writerIndex() - 0;
        testNotificationMsg = new byte[iReadLen];
        buf.readBytes(testNotificationMsg, 0, iReadLen);
        assertThat(testNotificationMsg, is(notificationMsg));
    }

    /**
     * Notification message without error subcode.
     *
     * @throws BGPParseException while decoding and encoding notification message
     */
    @Test(expected = BGPParseException.class)
    public void bgpNotificationMessageTest4() throws BGPParseException {
        byte[] notificationMsg = new byte[] {(byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff, 0x00,
                                             0x14, 0x03, 0x02};

        byte[] testNotificationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(notificationMsg);

        BGPMessageReader<BGPMessage> reader = BGPFactories.getGenericReader();
        BGPMessage message = null;
        BGPHeader bgpHeader = new BGPHeader();

        message = reader.readFrom(buffer, bgpHeader);
        assertThat(message, instanceOf(BGPNotificationMsg.class));

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testNotificationMsg = buf.array();

        int iReadLen = buf.writerIndex() - 0;
        testNotificationMsg = new byte[iReadLen];
        buf.readBytes(testNotificationMsg, 0, iReadLen);
        assertThat(testNotificationMsg, is(notificationMsg));
    }

    /**
     * Notification message with wrong message length.
     *
     * @throws BGPParseException while decoding and encoding notification message
     */
    @Test(expected = BGPParseException.class)
    public void bgpNotificationMessageTest5() throws BGPParseException {
        byte[] notificationMsg = new byte[] {(byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff,
                                             (byte) 0xff, (byte) 0xff, 0x00,
                                             0x14, 0x03, 0x02, 0x02};

        byte[] testNotificationMsg = {0};
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(notificationMsg);

        BGPMessageReader<BGPMessage> reader = BGPFactories.getGenericReader();
        BGPMessage message = null;
        BGPHeader bgpHeader = new BGPHeader();

        message = reader.readFrom(buffer, bgpHeader);
        assertThat(message, instanceOf(BGPNotificationMsg.class));

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);
        testNotificationMsg = buf.array();

        int iReadLen = buf.writerIndex() - 0;
        testNotificationMsg = new byte[iReadLen];
        buf.readBytes(testNotificationMsg, 0, iReadLen);
        assertThat(testNotificationMsg, is(notificationMsg));
    }
}