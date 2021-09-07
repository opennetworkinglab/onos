/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onlab.packet.bmp;

import com.google.common.testing.EqualsTester;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Deserializer;
import org.onlab.packet.PacketTestUtils;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for Bmp class.
 */
public class BmpTest {
    private Deserializer<Bmp> deserializer;

    private byte version = 3;
    private int length = 6;
    private byte type = 1;

    private byte[] headerBytes;

    @Before
    public void setUp() throws Exception {
        deserializer = Bmp.deserializer();
        ByteBuffer bb = ByteBuffer.allocate(Bmp.DEFAULT_HEADER_LENGTH);

        bb.put(version);
        bb.putInt(length);
        bb.put(type);

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
     * Test Deserialize and getters.
     */
    @Test
    public void testDeserialize() throws Exception {
        Bmp bmp = deserializer.deserialize(headerBytes, 0, headerBytes.length);

        assertEquals(version, bmp.getVersion());
        assertEquals(type, bmp.getType());
        assertEquals(length, bmp.getLength());
    }

    /**
     * Tests toString.
     *
     * @throws Exception
     */
    @Test
    public void testToStringBmp() throws Exception {
        Bmp bmp = deserializer.deserialize(headerBytes, 0, headerBytes.length);
        String str = bmp.toString();

        assertTrue(StringUtils.contains(str, "version=" + version));
        assertTrue(StringUtils.contains(str, "type=" + type));
        assertTrue(StringUtils.contains(str, "length=" + length));
    }

    /**
     * Tests equals method.
     *
     * @throws Exception
     */
    @Test
    public void testEquality() throws Exception {
        Bmp bmp = deserializer.deserialize(headerBytes, 0, headerBytes.length);
        new EqualsTester()
                .addEqualityGroup(bmp).testEquals();
    }

}
