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
import static org.hamcrest.Matchers.is;

public class PcepLabelUpdateMsgTest {

    /**
     * This test case checks for
     * <pce-label-download> SRP, LSP, LABEL Object.
     * in PcepLabelUpdate message.
     */
    @Test
    public void labelUpdateMessageTest1() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] labelUpdate = new byte[]{0x20, (byte) 0xE2, 0x00, 0x24, // common header
                0x21, 0x10, 0x00, 0x0C, // SRP Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x10,
                0x20, 0x10, 0x00, 0x08, // LSP Object Header
                0x00, 0x01, 0x00, 0x00,
                (byte) 0xE1, 0x10, 0x00, 0x0C, // LABEL Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x10, 0x10, 0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(labelUpdate);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testLabelUpdateMsg = {0};
        assertThat(message, instanceOf(PcepLabelUpdateMsg.class));


        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testLabelUpdateMsg = new byte[readLen];
        buf.readBytes(testLabelUpdateMsg, 0, readLen);

        assertThat(testLabelUpdateMsg, is(labelUpdate));
    }

    /**
     * This test case checks for
     * <pce-label-download> SRP, LSP, LABEL Object, LABEL Object.
     * in PcepLabelUpdate message.
     */
    @Test
    public void labelUpdateMessageTest2() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] labelUpdate = new byte[]{0x20, (byte) 0xE2, 0x00, 0x30, // common header
                0x21, 0x10, 0x00, 0x0C, // SRP Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x10,
                0x20, 0x10, 0x00, 0x08, // LSP Object Header
                0x00, 0x01, 0x00, 0x00,
                (byte) 0xE1, 0x10, 0x00, 0x0C, // LABEL Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x44, 0x00, 0x00,
                (byte) 0xE1, 0x10, 0x00, 0x0C, // LABEL Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x79, 0x00, 0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(labelUpdate);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testLabelUpdateMsg = {0};
        assertThat(message, instanceOf(PcepLabelUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testLabelUpdateMsg = new byte[readLen];
        buf.readBytes(testLabelUpdateMsg, 0, readLen);

        assertThat(testLabelUpdateMsg, is(labelUpdate));
    }

    /**
     * This test case checks for
     * <pce-label-map> SRP, LABEL, FEC Object.
     * in PcepLabelUpdate message.
     */
    @Test
    public void labelUpdateMessageTest3() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] labelUpdate = new byte[]{0x20, (byte) 0xE2, 0x00, 0x24, // common header
                0x21, 0x10, 0x00, 0x0C, // SRP Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x10,
                (byte) 0xE1, 0x10, 0x00, 0x0C, // LABEL Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x79, 0x00, 0x00,
                (byte) 0xE2, 0x10, 0x00, 0x08, // FEC Object Header
                0x0A, 0x0A, 0x0B, 0x0B};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(labelUpdate);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testLabelUpdateMsg = {0};
        assertThat(message, instanceOf(PcepLabelUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testLabelUpdateMsg = new byte[readLen];
        buf.readBytes(testLabelUpdateMsg, 0, readLen);

        assertThat(testLabelUpdateMsg, is(labelUpdate));
    }

    /**
     * This test case checks for
     * <pce-label-download> SRP, LSP, LABEL, LABEL, <pce-label-download> SRP, LSP, LABEL
     * in PcepLabelUpdate message.
     */
    @Test
    public void labelUpdateMessageTest4() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] labelUpdate = new byte[]{0x20, (byte) 0xE2, 0x00, 0x50, // common header
                0x21, 0x10, 0x00, 0x0C, // SRP Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x10,
                0x20, 0x10, 0x00, 0x08, // LSP Object Header
                0x00, 0x01, 0x00, 0x00,
                (byte) 0xE1, 0x10, 0x00, 0x0C, // LABEL Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x66, 0x00, 0x00,
                (byte) 0xE1, 0x10, 0x00, 0x0C, // LABEL Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x68, 0x00, 0x00,
                0x21, 0x10, 0x00, 0x0C, // SRP Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x11,
                0x20, 0x10, 0x00, 0x08, // LSP Object Header
                0x00, 0x02, 0x00, 0x00,
                (byte) 0xE1, 0x10, 0x00, 0x0C, // LABEL Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x44, 0x00, 0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(labelUpdate);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testLabelUpdateMsg = {0};
        assertThat(message, instanceOf(PcepLabelUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testLabelUpdateMsg = new byte[readLen];
        buf.readBytes(testLabelUpdateMsg, 0, readLen);

        assertThat(testLabelUpdateMsg, is(labelUpdate));
    }

    /**
     * This test case checks for
     * <pce-label-map> SRP, LABEL, FEC, <pce-label-map> SRP, LABEL, FEC.
     * in PcepLabelUpdate message.
     */
    @Test
    public void labelUpdateMessageTest5() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] labelUpdate = new byte[]{0x20, (byte) 0xE2, 0x00, 0x44, // common header
                0x21, 0x10, 0x00, 0x0C, // SRP Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x10,
                (byte) 0xE1, 0x10, 0x00, 0x0C, // LABEL Object Header
                0x00, 0x00, 0x00, 0x01,
                0x00, 0x44, 0x00, 0x00,
                (byte) 0xE2, 0x10, 0x00, 0x08, // FEC Object Header
                0x0A, 0x0A, 0x0B, 0x0B,
                0x21, 0x10, 0x00, 0x0C, // SRP Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x11,
                (byte) 0xE1, 0x10, 0x00, 0x0C, // LABEL Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x44, 0x00, 0x00,
                (byte) 0xE2, 0x10, 0x00, 0x08, // FEC Object Header
                0x0A, 0x0A, 0x0C, 0x0C};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(labelUpdate);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testLabelUpdateMsg = {0};
        assertThat(message, instanceOf(PcepLabelUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testLabelUpdateMsg = new byte[readLen];
        buf.readBytes(testLabelUpdateMsg, 0, readLen);

        assertThat(testLabelUpdateMsg, is(labelUpdate));
    }

    /**
     * This test case checks for
     * <pce-label-download> SRP, LSP, LABEL, LABEL, <pce-label-download> SRP, LABEL, FEC.
     * in PcepLabelUpdate message.
     */
    @Test
    public void labelUpdateMessageTest6() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] labelUpdate = new byte[]{0x20, (byte) 0xE2, 0x00, 0x50, // common header
                0x21, 0x10, 0x00, 0x0C, // SRP Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x10,
                0x20, 0x10, 0x00, 0x08, // LSP Object Header
                0x00, 0x01, 0x00, 0x00,
                (byte) 0xE1, 0x10, 0x00, 0x0C, // LABEL Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x44, 0x00, 0x00,
                (byte) 0xE1, 0x10, 0x00, 0x0C, // LABEL Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x44, 0x00, 0x00,
                0x21, 0x10, 0x00, 0x0C, // SRP Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x12,
                (byte) 0xE1, 0x10, 0x00, 0x0C, // LABEL Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x44, 0x00, 0x00,
                (byte) 0xE2, 0x10, 0x00, 0x08, // FEC Object Header
                0x0A, 0x0A, 0x0D, 0x0D};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(labelUpdate);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testLabelUpdateMsg = {0};
        assertThat(message, instanceOf(PcepLabelUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testLabelUpdateMsg = new byte[readLen];
        buf.readBytes(testLabelUpdateMsg, 0, readLen);

        assertThat(testLabelUpdateMsg, is(labelUpdate));
    }

    /**
     * This test case checks for
     * <pce-label-download> SRP, LABEL, FEC, <pce-label-download> SRP, LSP, LABEL, LABEL.
     * in PcepLabelUpdate message.
     */
    @Test
    public void labelUpdateMessageTest7() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] labelUpdate = new byte[]{0x20, (byte) 0xE2, 0x00, 0x50, // common header
                0x21, 0x10, 0x00, 0x0C, // SRP Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x12,
                (byte) 0xE1, 0x10, 0x00, 0x0C, // LABEL Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x44, 0x00, 0x00,
                (byte) 0xE2, 0x10, 0x00, 0x08, // FEC Object Header
                0x0A, 0x0A, 0x0D, 0x0D,
                0x21, 0x10, 0x00, 0x0C, // SRP Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x10,
                0x20, 0x10, 0x00, 0x08, // LSP Object Header
                0x00, 0x01, 0x00, 0x00,
                (byte) 0xE1, 0x10, 0x00, 0x0C, // LABEL Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x44, 0x00, 0x00,
                (byte) 0xE1, 0x10, 0x00, 0x0C, // LABEL Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x44, 0x00, 0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(labelUpdate);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testLabelUpdateMsg = {0};
        assertThat(message, instanceOf(PcepLabelUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testLabelUpdateMsg = new byte[readLen];
        buf.readBytes(testLabelUpdateMsg, 0, readLen);

        assertThat(testLabelUpdateMsg, is(labelUpdate));
    }

    /**
     * This test case checks for
     * <pce-label-download> SRP, LABEL, FEC, <pce-label-download> SRP, LSP, LABEL, LABEL.
     * <pce-label-download> SRP, LSP, LABEL, LABEL.
     * in PcepLabelUpdate message.
     */
    @Test
    public void labelUpdateMessageTest8() throws PcepParseException, PcepOutOfBoundMessageException {

        byte[] labelUpdate = new byte[]{0x20, (byte) 0xE2, 0x00, 0x7C, // common header
                0x21, 0x10, 0x00, 0x0C, // SRP Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x12,
                (byte) 0xE1, 0x10, 0x00, 0x0C, // LABEL Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x44, 0x00, 0x00,
                (byte) 0xE2, 0x10, 0x00, 0x08, // FEC Object Header
                0x0A, 0x0A, 0x0D, 0x0D,
                0x21, 0x10, 0x00, 0x0C, // SRP Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x10,
                0x20, 0x10, 0x00, 0x08, // LSP Object Header
                0x00, 0x01, 0x00, 0x00,
                (byte) 0xE1, 0x10, 0x00, 0x0C, // LABEL Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x44, 0x00, 0x00,
                (byte) 0xE1, 0x10, 0x00, 0x0C, // LABEL Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x44, 0x00, 0x00,
                0x21, 0x10, 0x00, 0x0C, // SRP Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x10,
                0x20, 0x10, 0x00, 0x08, // LSP Object Header
                0x00, 0x01, 0x00, 0x00,
                (byte) 0xE1, 0x10, 0x00, 0x0C, // LABEL Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x44, 0x00, 0x00,
                (byte) 0xE1, 0x10, 0x00, 0x0C, // LABEL Object Header
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x44, 0x00, 0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(labelUpdate);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = null;

        message = reader.readFrom(buffer);

        byte[] testLabelUpdateMsg = {0};

        assertThat(message, instanceOf(PcepLabelUpdateMsg.class));
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testLabelUpdateMsg = new byte[readLen];
        buf.readBytes(testLabelUpdateMsg, 0, readLen);

        assertThat(testLabelUpdateMsg, is(labelUpdate));
    }
}
