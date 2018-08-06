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
 * Unit tests for the ICMP class.
 */
public class ICMPTest {

    private Deserializer<ICMP> deserializer;

    private byte icmpType = ICMP.TYPE_ECHO_REQUEST;
    private byte icmpCode = 4;
    private short checksum = 870;

    private byte[] headerBytes;

    @Before
    public void setUp() throws Exception {
        ICMPEcho icmpEcho = new ICMPEcho();
        icmpEcho.setIdentifier((short) 0)
                .setSequenceNum((short) 0);

        ByteBuffer byteBufferIcmpEcho = ByteBuffer.wrap(icmpEcho.serialize());

        deserializer = ICMP.deserializer();

        ByteBuffer bb = ByteBuffer.allocate(ICMP.ICMP_HEADER_LENGTH + ICMPEcho.ICMP_ECHO_HEADER_LENGTH);

        bb.put(icmpType);
        bb.put(icmpCode);
        bb.putShort(checksum);
        bb.put(byteBufferIcmpEcho);

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
        ICMP icmp = deserializer.deserialize(headerBytes, 0, headerBytes.length);

        assertEquals(icmpType, icmp.getIcmpType());
        assertEquals(icmpCode, icmp.getIcmpCode());
        assertEquals(checksum, icmp.getChecksum());
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringIcmp() throws Exception {
        ICMP icmp = deserializer.deserialize(headerBytes, 0, headerBytes.length);
        String str = icmp.toString();

        assertTrue(StringUtils.contains(str, "icmpType=" + icmpType));
        assertTrue(StringUtils.contains(str, "icmpCode=" + icmpCode));
        assertTrue(StringUtils.contains(str, "checksum=" + checksum));
    }
}
