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

    private Deserializer<IPv4> deserializer;

    private byte version = 4;
    private byte headerLength = 6;
    private byte diffServ = 2;
    private short totalLength = 20;
    private short identification = 1;
    private byte flags = 1;
    private short fragmentOffset = 1;
    private byte ttl = 60;
    private byte protocol = 4;
    private short checksum = 4;
    private int sourceAddress = 1;
    private int destinationAddress = 2;
    private byte[] options = new byte[] {0x1, 0x2, 0x3, 0x4};

    private byte[] headerBytes;

    @Before
    public void setUp() throws Exception {
        deserializer = IPv4.deserializer();

        ByteBuffer bb = ByteBuffer.allocate(headerLength * 4);

        bb.put((byte) ((version & 0xf) << 4 | headerLength & 0xf));
        bb.put(diffServ);
        bb.putShort(totalLength);
        bb.putShort(identification);
        bb.putShort((short) ((flags & 0x7) << 13 | fragmentOffset & 0x1fff));
        bb.put(ttl);
        bb.put(protocol);
        bb.putShort(checksum);
        bb.putInt(sourceAddress);
        bb.putInt(destinationAddress);
        bb.put(options);

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

        assertEquals(version, ipv4.getVersion());
        assertEquals(headerLength, ipv4.getHeaderLength());
        assertEquals(diffServ, ipv4.getDiffServ());
        assertEquals(totalLength, ipv4.getTotalLength());
        assertEquals(identification, ipv4.getIdentification());
        assertEquals(flags, ipv4.getFlags());
        assertEquals(fragmentOffset, ipv4.getFragmentOffset());
        assertEquals(ttl, ipv4.getTtl());
        assertEquals(protocol, ipv4.getProtocol());
        assertEquals(checksum, ipv4.getChecksum());
        assertEquals(sourceAddress, ipv4.getSourceAddress());
        assertEquals(destinationAddress, ipv4.getDestinationAddress());
        assertTrue(ipv4.isTruncated());
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringIPv4() throws Exception {
        IPv4 ipv4 = deserializer.deserialize(headerBytes, 0, headerBytes.length);
        String str = ipv4.toString();

        assertTrue(StringUtils.contains(str, "version=" + version));
        assertTrue(StringUtils.contains(str, "headerLength=" + headerLength));
        assertTrue(StringUtils.contains(str, "diffServ=" + diffServ));
        assertTrue(StringUtils.contains(str, "totalLength=" + totalLength));
        assertTrue(StringUtils.contains(str, "identification=" + identification));
        assertTrue(StringUtils.contains(str, "flags=" + flags));
        assertTrue(StringUtils.contains(str, "fragmentOffset=" + fragmentOffset));
        assertTrue(StringUtils.contains(str, "ttl=" + ttl));
        assertTrue(StringUtils.contains(str, "protocol=" + protocol));
        assertTrue(StringUtils.contains(str, "checksum=" + checksum));
        assertTrue(StringUtils.contains(str, "sourceAddress=" + sourceAddress));
        assertTrue(StringUtils.contains(str, "destinationAddress=" + destinationAddress));
    }
}
