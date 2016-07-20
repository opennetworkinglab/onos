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
 * Unit tests for LLC class.
 */
public class LLCTest {

    private Deserializer<LLC> deserializer;

    private byte dsap = 10;
    private byte ssap = 20;
    private byte ctrl = 30;

    private byte[] bytes;

    @Before
    public void setUp() throws Exception {
        deserializer = LLC.deserializer();

        ByteBuffer bb = ByteBuffer.allocate(LLC.LLC_HEADER_LENGTH);

        bb.put(dsap);
        bb.put(ssap);
        bb.put(ctrl);

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
        LLC llc = deserializer.deserialize(bytes, 0, bytes.length);

        assertEquals(dsap, llc.getDsap());
        assertEquals(ssap, llc.getSsap());
        assertEquals(ctrl, llc.getCtrl());
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringLLC() throws Exception {
        LLC llc = deserializer.deserialize(bytes, 0, bytes.length);
        String str = llc.toString();

        assertTrue(StringUtils.contains(str, "dsap=" + dsap));
        assertTrue(StringUtils.contains(str, "ssap=" + ssap));
        assertTrue(StringUtils.contains(str, "ctrl=" + ctrl));
    }
}
