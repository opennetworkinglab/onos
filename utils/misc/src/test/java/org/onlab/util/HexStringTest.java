/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onlab.util;

import org.junit.Test;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

/**
 * Test of the Hexstring.
 *
 */

public class HexStringTest {

    @Test
    public void testMarshalling() throws Exception {
        String dpidStr = "00:00:00:23:20:2d:16:71";
        long dpid = HexString.toLong(dpidStr);
        String testStr = HexString.toHexString(dpid);
        assertEquals(dpidStr, testStr);
    }

    @Test
    public void testToLong() {
        String dpidStr = "3e:1f:01:fc:72:8c:63:31";
        long valid = 0x3e1f01fc728c6331L;
        long testLong = HexString.toLong(dpidStr);
        assertEquals(valid, testLong);
    }

    @Test
    public void testToLongMsb() {
        String dpidStr = "ca:7c:5e:d1:64:7a:95:9b";
        long valid = -3856102927509056101L;
        long testLong = HexString.toLong(dpidStr);
        assertEquals(valid, testLong);
    }

    @Test
    public void testFromHexString() {
        String dpidStr = "3e:1f:01:fc:72:8c:63:31";
        String dpidStrNoSep = "3e1f01fc728c6331";
        long valid = 0x3e1f01fc728c6331L;
        byte[] validBytes = ByteBuffer.allocate(Long.BYTES).putLong(valid).array();
        byte[] testBytes = HexString.fromHexString(dpidStr);
        byte[] testBytesNoSep = HexString.fromHexString(dpidStrNoSep, null);
        byte[] testBytesUCase = HexString.fromHexString(dpidStr.toUpperCase());
        byte[] testBytesUCaseNoSep = HexString.fromHexString(dpidStrNoSep.toUpperCase(), null);
        assertArrayEquals(validBytes, testBytes);
        assertArrayEquals(validBytes, testBytesNoSep);
        assertArrayEquals(validBytes, testBytesUCase);
        assertArrayEquals(validBytes, testBytesUCaseNoSep);
    }

    @Test(expected = NumberFormatException.class)
    public void testToLongError() {
        String dpidStr = "09:08:07:06:05:04:03:02:01";
        HexString.toLong(dpidStr);
        fail("HexString.toLong() should have thrown a NumberFormatException");
    }

    @Test
    public void testToStringBytes() {
        byte[] dpid = {0, 0, 0, 0, 0, 0, 0, -1 };
        String valid = "00:00:00:00:00:00:00:ff";
        String testString = HexString.toHexString(dpid);
        assertEquals(valid, testString);

        String validNoSep = "00000000000000ff";
        assertEquals(validNoSep, HexString.toHexString(dpid, null));
    }

    @Test(expected = NumberFormatException.class)
    public void testFromHexStringError() {
        String invalidStr = "00:00:00:00:00:00:ffff";
        HexString.fromHexString(invalidStr);
        fail("HexString.fromHexString() should have thrown a NumberFormatException");
    }
}

