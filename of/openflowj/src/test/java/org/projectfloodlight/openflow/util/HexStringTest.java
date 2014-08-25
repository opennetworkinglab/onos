/**
*    Copyright (c) 2008 The Board of Trustees of The Leland Stanford Junior
*    University
*
*    Licensed under the Apache License, Version 2.0 (the "License"); you may
*    not use this file except in compliance with the License. You may obtain
*    a copy of the License at
*
*         http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
*    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
*    License for the specific language governing permissions and limitations
*    under the License.
**/

package org.projectfloodlight.openflow.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Does hexstring conversion work?
 *
 * @author Rob Sherwood (rob.sherwood@stanford.edu)
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
    public void testToLong2() {
        String dpidStr = "1f:1:fc:72:3:f:31";
        long valid = 0x1f01fc72030f31L;
        long testLong = HexString.toLong(dpidStr);
        assertEquals(valid, testLong);
    }

    @Test
    public void testToLongMSB() {
        String dpidStr = "ca:7c:5e:d1:64:7a:95:9b";
        long valid = -3856102927509056101L;
        long testLong = HexString.toLong(dpidStr);
        assertEquals(valid, testLong);
    }

    @Test(expected=NumberFormatException.class)
    public void testToLongErrorTooManyBytes() {
        HexString.toLong("09:08:07:06:05:04:03:02:01");
    }

    @Test(expected=NumberFormatException.class)
    public void testToLongErrorByteValueTooLong() {
        HexString.toLong("234:01");
    }

    @Test(expected=NumberFormatException.class)
    public void testToLongErrorEmptyByte() {
        HexString.toLong("03::01");
    }

    @Test(expected=NumberFormatException.class)
    public void testToLongErrorInvalidHexDigit() {
        HexString.toLong("ss:01");
    }

    @Test(expected=NumberFormatException.class)
    public void testToLongErrorEmptyString() {
        HexString.toLong("");
    }


    @Test
    public void testToStringBytes() {
        byte[] dpid = { 0, 0, 0, 0, 0, 0, 0, -1 };
        String valid = "00:00:00:00:00:00:00:ff";
        String testString = HexString.toHexString(dpid);
        assertEquals(valid, testString);
    }

    @Test(expected=NumberFormatException.class)
    public void testFromHexStringError() {
        String invalidStr = "00:00:00:00:00:00:ffff";
        HexString.fromHexString(invalidStr);
    }
}

