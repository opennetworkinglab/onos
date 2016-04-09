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
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for MPLS class.
 */
public class MplsTest {

    private Deserializer<MPLS> deserializer;

    private int label = 1048575;
    private byte bos = 1;
    private byte ttl = 20;
    private byte protocol = MPLS.PROTOCOL_IPV4;

    private byte[] bytes;

    @Before
    public void setUp() throws Exception {
        // Replace normal deserializer map with an empty map. This will cause
        // the DataDeserializer to be used which will silently handle 0-byte input.
        MPLS.protocolDeserializerMap = new HashMap<>();

        deserializer = MPLS.deserializer();

        ByteBuffer bb = ByteBuffer.allocate(MPLS.HEADER_LENGTH);
        bb.putInt(((label & 0x000fffff) << 12) | ((bos & 0x1) << 8 | (ttl & 0xff)));

        bytes = bb.array();
    }

    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(deserializer);
    }

    @Test
    public void testDeserializeTruncated() throws Exception {
        PacketTestUtils.testDeserializeTruncated(deserializer, bytes);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() throws Exception {
        MPLS mpls = deserializer.deserialize(bytes, 0, bytes.length);

        assertEquals(label, mpls.label);
        assertEquals(bos, mpls.bos);
        assertEquals(ttl, mpls.ttl);
        assertEquals(protocol, mpls.protocol);
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringMpls() throws Exception {
        MPLS mpls = deserializer.deserialize(bytes, 0, bytes.length);
        String str = mpls.toString();

        assertTrue(StringUtils.contains(str, "label=" + label));
        assertTrue(StringUtils.contains(str, "bos=" + bos));
        assertTrue(StringUtils.contains(str, "ttl=" + ttl));
        assertTrue(StringUtils.contains(str, "protocol=" + protocol));
    }
}
