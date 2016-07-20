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
package org.onosproject.ospf.protocol.ospfpacket;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.ospf.protocol.util.OspfUtil;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for OspfMessageReader.
 */

public class OspfMessageReaderTest {

    private final byte[] packet1 = {2, 1, 0, 44, -64, -88, -86, 8,
            0, 0, 0, 1, 39, 59, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, 0, 0,
            10, 2, 1, 0, 0, 0, 40, -64, -88, -86, 8, 0, 0, 0, 0};
    private final byte[] packet2 = {2, 2, 0, 52, -64, -88, -86, 8, 0,
            0, 0, 1, -96, 82, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, -36, 2, 7, 65, 119,
            -87, 126, 0, 23, 2, 1, 10, 10, 10, 10, 10, 10, 10, 10, -128, 0, 0, 6,
            -69, 26, 0, 36};
    private final byte[] packet3 = {2, 3, 0, 36, -64, -88, -86, 3, 0,
            0, 0, 1, -67, -57, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -64, -88,
            -86, 8, -64, -88, -86, 8};
    private final byte[] packet4 = {2, 4, 1, 36, -64, -88, -86, 3, 0,
            0, 0, 1, 54, 107, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0,
            2, 2, 1, -64, -88, -86, 3, -64, -88, -86, 3, -128, 0,
            0, 1, 58, -100, 0, 48, 2, 0, 0, 2, -64, -88, -86,
            0, -1, -1, -1, 0, 3, 0, 0, 10, -64, -88, -86, 0,
            -1, -1, -1, 0, 3, 0, 0, 10, 0, 3, 2, 5, 80, -44,
            16, 0, -64, -88, -86, 2, -128, 0, 0, 1, 42, 73, 0,
            36, -1, -1, -1, -1, -128, 0, 0, 20, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 3, 2, 5, -108, 121, -85, 0, -64, -88,
            -86, 2, -128, 0, 0, 1, 52, -91, 0, 36, -1, -1, -1,
            0, -128, 0, 0, 20, -64, -88, -86, 1, 0, 0, 0, 0, 0,
            3, 2, 5, -64, -126, 120, 0, -64, -88, -86, 2, -128, 0,
            0, 1, -45, 25, 0, 36, -1, -1, -1, 0, -128, 0, 0, 20,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 2, 5, -64, -88, 0, 0,
            -64, -88, -86, 2, -128, 0, 0, 1, 55, 8, 0, 36, -1, -1,
            -1, 0, -128, 0, 0, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            3, 2, 5, -64, -88, 1, 0, -64, -88, -86, 2, -128, 0, 0,
            1, 44, 18, 0, 36, -1, -1, -1, 0, -128, 0, 0, 20, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 3, 2, 5, -64, -88, -84, 0, -64,
            -88, -86, 2, -128, 0, 0, 1, 51, 65, 0, 36, -1, -1, -1, 0,
            -128, 0, 0, 20, -64, -88, -86, 10, 0, 0, 0, 0};
    private final byte[] packet5 = {2, 5, 0, 44, -64, -88, -86, 8, 0, 0,
            0, 1, -30, -12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 14, 16, 2, 1, -64, -88, -86,
            2, -64, -88, -86, 2, -128, 0, 0, 1, 74, -114, 0, 48};
    private OspfMessageReader ospfMessageReader;
    private ChannelBuffer channelBuffer;

    @Before
    public void setUp() throws Exception {
        ospfMessageReader = new OspfMessageReader();
    }

    @After
    public void tearDown() throws Exception {
        ospfMessageReader = null;
        channelBuffer = null;
    }

    /**
     * Tests readFromBuffer() method.
     */
    @Test
    public void testReadFromBuffer() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(framePacket(packet1));
        ospfMessageReader.readFromBuffer(channelBuffer);

        channelBuffer = ChannelBuffers.copiedBuffer(framePacket(packet2));
        ospfMessageReader.readFromBuffer(channelBuffer);

        channelBuffer = ChannelBuffers.copiedBuffer(framePacket(packet3));
        ospfMessageReader.readFromBuffer(channelBuffer);

        channelBuffer = ChannelBuffers.copiedBuffer(framePacket(packet4));
        ospfMessageReader.readFromBuffer(channelBuffer);

        channelBuffer = ChannelBuffers.copiedBuffer(framePacket(packet5));
        ospfMessageReader.readFromBuffer(channelBuffer);
        assertThat(ospfMessageReader, is(notNullValue()));
    }

    /**
     * Frames the packet to min frame length.
     *
     * @param ospfPacket OSPF packet
     * @return OSPF packet as byte array
     */
    private byte[] framePacket(byte[] ospfPacket) {
        //Set the length of the packet
        //Get the total length of the packet
        int length = ospfPacket.length;
        //PDU_LENGTH + 1 byte for interface index
        if (length < OspfUtil.MINIMUM_FRAME_LEN) {
            byte[] bytes = new byte[OspfUtil.MINIMUM_FRAME_LEN + 5];
            System.arraycopy(ospfPacket, 0, bytes, 0, length);
            return bytes;
        }
        return ospfPacket;
    }
}