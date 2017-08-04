/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onlab.packet;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for IPv4 class.
 */
public class IPv4Test {

    private static Deserializer<IPv4> deserializer;

    private static final byte VERSION = 4;
    private static final byte HEADER_LENGTH = 6;
    private static final byte DIFF_SERV = 2;
    private static final short TOTAL_LENGTH = 60;
    private static final short IDENTIFICATION = 1;
    private static final byte FLAGS = 1;
    private static final short FRAGMENT_OFFSET = 1;
    private static final byte TTL = 60;
    private static final byte PROTOCOL = 4;
    private static final short CHECKSUM = 4;
    private static final int SOURCE_ADDRESS = 1;
    private static final int DESTINATION_ADDRESS = 2;
    private static final byte[] OPTIONS = new byte[] {0x1, 0x2, 0x3, 0x4};

    private byte[] headerBytes;

    @Before
    public void setUp() throws Exception {
        deserializer = IPv4.deserializer();

        ByteBuffer bb = ByteBuffer.allocate(HEADER_LENGTH * 4);

        bb.put((byte) ((VERSION & 0xf) << 4 | HEADER_LENGTH & 0xf));
        bb.put(DIFF_SERV);
        bb.putShort(TOTAL_LENGTH);
        bb.putShort(IDENTIFICATION);
        bb.putShort((short) ((FLAGS & 0x7) << 13 | FRAGMENT_OFFSET & 0x1fff));
        bb.put(TTL);
        bb.put(PROTOCOL);
        bb.putShort(CHECKSUM);
        bb.putInt(SOURCE_ADDRESS);
        bb.putInt(DESTINATION_ADDRESS);
        bb.put(OPTIONS);

        headerBytes = bb.array();
    }

    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(deserializer);
    }

    @Test
    public void testDeserializeTruncated() throws Exception {
        PacketTestUtils.testDeserializeTruncated(deserializer, headerBytes);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() throws Exception {
        IPv4 ipv4 = deserializer.deserialize(headerBytes, 0, headerBytes.length);

        assertEquals(VERSION, ipv4.getVersion());
        assertEquals(HEADER_LENGTH, ipv4.getHeaderLength());
        assertEquals(DIFF_SERV, ipv4.getDiffServ());
        assertEquals(TOTAL_LENGTH, ipv4.getTotalLength());
        assertEquals(IDENTIFICATION, ipv4.getIdentification());
        assertEquals(FLAGS, ipv4.getFlags());
        assertEquals(FRAGMENT_OFFSET, ipv4.getFragmentOffset());
        assertEquals(TTL, ipv4.getTtl());
        assertEquals(PROTOCOL, ipv4.getProtocol());
        assertEquals(CHECKSUM, ipv4.getChecksum());
        assertEquals(SOURCE_ADDRESS, ipv4.getSourceAddress());
        assertEquals(DESTINATION_ADDRESS, ipv4.getDestinationAddress());
        assertTrue(ipv4.isTruncated());
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringIPv4() throws Exception {
        IPv4 ipv4 = deserializer.deserialize(headerBytes, 0, headerBytes.length);
        String str = ipv4.toString();

        assertTrue(StringUtils.contains(str, "version=" + VERSION));
        assertTrue(StringUtils.contains(str, "headerLength=" + HEADER_LENGTH));
        assertTrue(StringUtils.contains(str, "diffServ=" + DIFF_SERV));
        assertTrue(StringUtils.contains(str, "totalLength=" + TOTAL_LENGTH));
        assertTrue(StringUtils.contains(str, "identification=" + IDENTIFICATION));
        assertTrue(StringUtils.contains(str, "flags=" + FLAGS));
        assertTrue(StringUtils.contains(str, "fragmentOffset=" + FRAGMENT_OFFSET));
        assertTrue(StringUtils.contains(str, "ttl=" + TTL));
        assertTrue(StringUtils.contains(str, "protocol=" + PROTOCOL));
        assertTrue(StringUtils.contains(str, "checksum=" + CHECKSUM));
        assertTrue(StringUtils.contains(str, "sourceAddress=" + SOURCE_ADDRESS));
        assertTrue(StringUtils.contains(str, "destinationAddress=" + DESTINATION_ADDRESS));
    }
}
