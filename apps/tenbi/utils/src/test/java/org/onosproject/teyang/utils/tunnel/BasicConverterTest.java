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

package org.onosproject.teyang.utils.tunnel;

import org.junit.Test;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev20130715.ietfinettypes.IpAddress;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Test of the basic converter tools.
 */
public class BasicConverterTest {
    private byte[] bytes1 = new byte[]{1, 1, 1, 1, 0, 0, 0, 0};
    private byte[] bytes2 = new byte[]{2, 2, 2, 2, 0, 0, 0, 0};
    private byte[] bytes3 = new byte[]{2, 0, 0, 0, 0, 0, 0, 0};
    private IpAddress ip1 = IpAddress.fromString("1.1.1.1");
    private IpAddress ip2 = IpAddress.fromString("2.2.2.2");
    private long longNum1 = 16843009;
    private long longNum2 = 33686018;
    private static final String CVT_F = "Convert failed: ";


    @Test
    public void longToIp() throws Exception {
        assertEquals(CVT_F + "longToIp", ip1, BasicConverter.longToIp(longNum1));
        assertEquals(CVT_F + "longToIp", ip2, BasicConverter.longToIp(longNum2));
    }

    @Test
    public void longToByte() throws Exception {
        assertArrayEquals(CVT_F + "longToByte", bytes1,
                          BasicConverter.longToByte(longNum1));
        assertArrayEquals(CVT_F + "longToByte", bytes2,
                          BasicConverter.longToByte(longNum2));
    }

    @Test
    public void ipToLong() throws Exception {
        assertEquals(CVT_F + "ipToLong", longNum1, BasicConverter.ipToLong(ip1));
        assertEquals(CVT_F + "ipToLong", longNum2, BasicConverter.ipToLong(ip2));
    }

    @Test
    public void bytesToLong() throws Exception {
        assertEquals(CVT_F + "bytesToLong", longNum1,
                     BasicConverter.bytesToLong(bytes1));
        assertEquals(CVT_F + "bytesToLong", longNum2,
                     BasicConverter.bytesToLong(bytes2));

        assertEquals(CVT_F + "bytesToLong", 2,
                     BasicConverter.bytesToLong(bytes3));
    }
}