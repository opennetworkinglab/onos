/*
 * Copyright 2014 Open Networking Laboratory
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.onlab.packet.IpAddress.Version;

import com.google.common.testing.EqualsTester;

public class IpPrefixTest {

    private static final byte [] BYTES1 = new byte [] {0xa, 0x0, 0x0, 0xa};
    private static final byte [] BYTES2 = new byte [] {0xa, 0x0, 0x0, 0xb};
    private static final int INTVAL0 = 0x0a000000;
    private static final int INTVAL1 = 0x0a00000a;
    private static final int INTVAL2 = 0x0a00000b;
    private static final String STRVAL = "10.0.0.12/16";
    private static final int MASK_LENGTH = 16;

    @Test
    public void testEquality() {
        IpPrefix ip1 = IpPrefix.valueOf(IpAddress.Version.INET,
                                        BYTES1, IpPrefix.MAX_INET_MASK_LENGTH);
        IpPrefix ip2 = IpPrefix.valueOf(INTVAL1, IpPrefix.MAX_INET_MASK_LENGTH);
        IpPrefix ip3 = IpPrefix.valueOf(IpAddress.Version.INET,
                                        BYTES2, IpPrefix.MAX_INET_MASK_LENGTH);
        IpPrefix ip4 = IpPrefix.valueOf(INTVAL2, IpPrefix.MAX_INET_MASK_LENGTH);
        IpPrefix ip5 = IpPrefix.valueOf(STRVAL);

        new EqualsTester().addEqualityGroup(ip1, ip2)
        .addEqualityGroup(ip3, ip4)
        .addEqualityGroup(ip5)
        .testEquals();

        // string conversions
        IpPrefix ip6 = IpPrefix.valueOf(IpAddress.Version.INET,
                                        BYTES1, MASK_LENGTH);
        IpPrefix ip7 = IpPrefix.valueOf("10.0.0.10/16");
        IpPrefix ip8 = IpPrefix.valueOf(IpAddress.Version.INET,
                                        new byte [] {0xa, 0x0, 0x0, 0xc}, 16);
        assertEquals("incorrect address conversion", ip6, ip7);
        assertEquals("incorrect address conversion", ip5, ip8);
    }

    @Test
    public void basics() {
        IpPrefix ip1 = IpPrefix.valueOf(IpAddress.Version.INET,
                                        BYTES1, MASK_LENGTH);
        final byte [] bytes = new byte [] {0xa, 0x0, 0x0, 0x0};

        // check fields
        assertEquals("incorrect IP Version", Version.INET, ip1.version());
        assertEquals("incorrect netmask", 16, ip1.prefixLength());
        assertTrue("faulty toOctets()",
                   Arrays.equals(bytes, ip1.address().toOctets()));
        assertEquals("faulty toInt()", INTVAL0, ip1.address().toInt());
        assertEquals("faulty toString()", "10.0.0.0/16", ip1.toString());
    }

    @Test
    public void netmasks() {
        // masked
        IpPrefix ip1 = IpPrefix.valueOf(IpAddress.Version.INET,
                                        BYTES1, MASK_LENGTH);
        IpPrefix ip2 = IpPrefix.valueOf("10.0.0.10/16");
        IpPrefix ip3 = IpPrefix.valueOf("10.0.0.0/16");
        assertEquals("incorrect binary masked address",
                     ip1.toString(), "10.0.0.0/16");
        assertEquals("incorrect string masked address",
                     ip2.toString(), "10.0.0.0/16");
        assertEquals("incorrect network address",
                     ip2.toString(), "10.0.0.0/16");
    }

    @Test
    public void testContainsIpPrefix() {
        IpPrefix slash31 = IpPrefix.valueOf(IpAddress.Version.INET,
                                            BYTES1, 31);
        IpPrefix slash32 = IpPrefix.valueOf(IpAddress.Version.INET,
                                            BYTES1, 32);
        IpPrefix differentSlash32 = IpPrefix.valueOf(IpAddress.Version.INET,
                                                     BYTES2, 32);

        assertTrue(slash31.contains(differentSlash32));
        assertFalse(differentSlash32.contains(slash31));

        assertTrue(slash31.contains(slash32));
        assertFalse(slash32.contains(differentSlash32));
        assertFalse(differentSlash32.contains(slash32));

        IpPrefix zero = IpPrefix.valueOf("0.0.0.0/0");
        assertTrue(zero.contains(differentSlash32));
        assertFalse(differentSlash32.contains(zero));

        IpPrefix slash8 = IpPrefix.valueOf("10.0.0.0/8");
        assertTrue(slash8.contains(slash31));
        assertFalse(slash31.contains(slash8));
    }

    @Test
    public void testContainsIpAddress() {
        IpPrefix slash31 = IpPrefix.valueOf(IpAddress.Version.INET,
                                            BYTES1, 31);
        IpAddress addr32 = IpAddress.valueOf(IpAddress.Version.INET, BYTES1);

        assertTrue(slash31.contains(addr32));

        IpPrefix intf = IpPrefix.valueOf("192.168.10.101/24");
        IpAddress addr = IpAddress.valueOf("192.168.10.1");

        assertTrue(intf.contains(addr));

        IpPrefix intf1 = IpPrefix.valueOf("10.0.0.101/24");
        IpAddress addr1 = IpAddress.valueOf("10.0.0.4");

        assertTrue(intf1.contains(addr1));
    }
}
