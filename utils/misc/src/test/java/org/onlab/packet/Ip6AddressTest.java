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
package org.onlab.packet;

import com.google.common.net.InetAddresses;
import com.google.common.testing.EqualsTester;
import org.junit.Test;

import java.net.InetAddress;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Tests for class {@link Ip6Address}.
 */
public class Ip6AddressTest {
    /**
     * Tests the immutability of {@link Ip6Address}.
     */
    @Test
    public void testImmutable() {
        assertThatClassIsImmutable(Ip6Address.class);
    }

    /**
     * Tests the IPv4 address version constant.
     */
    @Test
    public void testAddressVersion() {
        assertThat(Ip6Address.VERSION, is(IpAddress.Version.INET6));
    }

    /**
     * Tests the length of the address in bytes (octets).
     */
    @Test
    public void testAddrByteLength() {
        assertThat(Ip6Address.BYTE_LENGTH, is(16));
    }

    /**
     * Tests the length of the address in bits.
     */
    @Test
    public void testAddrBitLength() {
        assertThat(Ip6Address.BIT_LENGTH, is(128));
    }

    /**
     * Tests returning the IP address version.
     */
    @Test
    public void testVersion() {
        IpAddress ipAddress;

        // IPv6
        ipAddress = IpAddress.valueOf("::");
        assertThat(ipAddress.version(), is(IpAddress.Version.INET6));
    }

    /**
     * Tests returning an IPv6 address as a byte array.
     */
    @Test
    public void testAddressToOctetsIPv6() {
        Ip6Address ipAddress;
        byte[] value;

        value = new byte[] {0x11, 0x11, 0x22, 0x22,
                            0x33, 0x33, 0x44, 0x44,
                            0x55, 0x55, 0x66, 0x66,
                            0x77, 0x77,
                            (byte) 0x88, (byte) 0x88};
        ipAddress =
            Ip6Address.valueOf("1111:2222:3333:4444:5555:6666:7777:8888");
        assertThat(ipAddress.toOctets(), is(value));

        value = new byte[] {0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00};
        ipAddress = Ip6Address.valueOf("::");
        assertThat(ipAddress.toOctets(), is(value));

        value = new byte[] {(byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff};
        ipAddress =
            Ip6Address.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
        assertThat(ipAddress.toOctets(), is(value));
    }

    /**
     * Tests valueOf() converter for IPv6 byte array.
     */
    @Test
    public void testValueOfByteArrayIPv6() {
        Ip6Address ipAddress;
        byte[] value;

        value = new byte[] {0x11, 0x11, 0x22, 0x22,
                            0x33, 0x33, 0x44, 0x44,
                            0x55, 0x55, 0x66, 0x66,
                            0x77, 0x77,
                            (byte) 0x88, (byte) 0x88};
        ipAddress = Ip6Address.valueOf(value);
        assertThat(ipAddress.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888"));

        value = new byte[] {0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00};
        ipAddress = Ip6Address.valueOf(value);
        assertThat(ipAddress.toString(), is("::"));

        value = new byte[] {(byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff};
        ipAddress = Ip6Address.valueOf(value);
        assertThat(ipAddress.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));
    }

    /**
     * Tests invalid valueOf() converter for a null array for IPv6.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidValueOfNullArrayIPv6() {
        Ip6Address ipAddress;
        byte[] value;

        value = null;
        ipAddress = Ip6Address.valueOf(value);
    }

    /**
     * Tests invalid valueOf() converger for an array that is too short for
     * IPv6.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfShortArrayIPv6() {
        Ip6Address ipAddress;
        byte[] value;

        value = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9};
        ipAddress = Ip6Address.valueOf(value);
    }

    /**
     * Tests valueOf() converter for IPv6 byte array and an offset.
     */
    @Test
    public void testValueOfByteArrayOffsetIPv6() {
        Ip6Address ipAddress;
        byte[] value;

        value = new byte[] {11, 22, 33,                         // Preamble
                            0x11, 0x11, 0x22, 0x22,
                            0x33, 0x33, 0x44, 0x44,
                            0x55, 0x55, 0x66, 0x66,
                            0x77, 0x77,
                            (byte) 0x88, (byte) 0x88,
                            44, 55};                            // Extra bytes
        ipAddress = Ip6Address.valueOf(value, 3);
        assertThat(ipAddress.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888"));

        value = new byte[] {11, 22,                             // Preamble
                            0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00,
                            33};                                // Extra bytes
        ipAddress = Ip6Address.valueOf(value, 2);
        assertThat(ipAddress.toString(), is("::"));

        value = new byte[] {11, 22,                             // Preamble
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            33};                                // Extra bytes
        ipAddress = Ip6Address.valueOf(value, 2);
        assertThat(ipAddress.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));
    }

    /**
     * Tests invalid valueOf() converger for an array and an invalid offset
     * for IPv6.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfArrayInvalidOffsetIPv6() {
        Ip6Address ipAddress;
        byte[] value;

        value = new byte[] {11, 22, 33,                         // Preamble
                            0x11, 0x11, 0x22, 0x22,
                            0x33, 0x33, 0x44, 0x44,
                            0x55, 0x55, 0x66, 0x66,
                            0x77, 0x77,
                            (byte) 0x88, (byte) 0x88,
                            44, 55};                            // Extra bytes
        ipAddress = Ip6Address.valueOf(value, 6);
    }

    /**
     * Tests valueOf() converter for IPv6 InetAddress.
     */
    @Test
    public void testValueOfInetAddressIPv6() {
        Ip6Address ipAddress;
        InetAddress inetAddress;

        inetAddress =
            InetAddresses.forString("1111:2222:3333:4444:5555:6666:7777:8888");
        ipAddress = Ip6Address.valueOf(inetAddress);
        assertThat(ipAddress.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888"));

        inetAddress = InetAddresses.forString("::");
        ipAddress = Ip6Address.valueOf(inetAddress);
        assertThat(ipAddress.toString(), is("::"));

        inetAddress =
            InetAddresses.forString("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
        ipAddress = Ip6Address.valueOf(inetAddress);
        assertThat(ipAddress.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));
    }

    /**
     * Tests valueOf() converter for IPv6 string.
     */
    @Test
    public void testValueOfStringIPv6() {
        Ip6Address ipAddress;

        ipAddress =
            Ip6Address.valueOf("1111:2222:3333:4444:5555:6666:7777:8888");
        assertThat(ipAddress.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888"));

        ipAddress = Ip6Address.valueOf("::");
        assertThat(ipAddress.toString(), is("::"));

        ipAddress =
            Ip6Address.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
        assertThat(ipAddress.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));
    }

    /**
     * Tests invalid valueOf() converter for a null string.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidValueOfNullString() {
        Ip6Address ipAddress;

        String fromString = null;
        ipAddress = Ip6Address.valueOf(fromString);
    }

    /**
     * Tests invalid valueOf() converter for an empty string.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfEmptyString() {
        Ip6Address ipAddress;

        String fromString = "";
        ipAddress = Ip6Address.valueOf(fromString);
    }

    /**
     * Tests invalid valueOf() converter for an incorrect string.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfIncorrectString() {
        Ip6Address ipAddress;

        String fromString = "NoSuchIpAddress";
        ipAddress = Ip6Address.valueOf(fromString);
    }

    /**
     * Tests making a mask prefix for a given prefix length for IPv6.
     */
    @Test
    public void testMakeMaskPrefixIPv6() {
        Ip6Address ipAddress;

        ipAddress = Ip6Address.makeMaskPrefix(8);
        assertThat(ipAddress.toString(), is("ff00::"));

        ipAddress = Ip6Address.makeMaskPrefix(120);
        assertThat(ipAddress.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ff00"));

        ipAddress = Ip6Address.makeMaskPrefix(0);
        assertThat(ipAddress.toString(), is("::"));

        ipAddress = Ip6Address.makeMaskPrefix(128);
        assertThat(ipAddress.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));

        ipAddress = Ip6Address.makeMaskPrefix(64);
        assertThat(ipAddress.toString(), is("ffff:ffff:ffff:ffff::"));
    }

    /**
     * Tests making a mask prefix for an invalid prefix length for IPv6:
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMakeNegativeMaskPrefixIPv6() {
        Ip6Address ipAddress;

        ipAddress = Ip6Address.makeMaskPrefix(-1);
    }

    /**
     * Tests making a mask prefix for an invalid prefix length for IPv6:
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMakeTooLongMaskPrefixIPv6() {
        Ip6Address ipAddress;

        ipAddress = Ip6Address.makeMaskPrefix(129);
    }

    /**
     * Tests making of a masked address for IPv6.
     */
    @Test
    public void testMakeMaskedAddressIPv6() {
        Ip6Address ipAddress =
            Ip6Address.valueOf("1111:2222:3333:4444:5555:6666:7777:8885");
        Ip6Address ipAddressMasked;

        ipAddressMasked = Ip6Address.makeMaskedAddress(ipAddress, 8);
        assertThat(ipAddressMasked.toString(), is("1100::"));

        ipAddressMasked = Ip6Address.makeMaskedAddress(ipAddress, 120);
        assertThat(ipAddressMasked.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8800"));

        ipAddressMasked = Ip6Address.makeMaskedAddress(ipAddress, 0);
        assertThat(ipAddressMasked.toString(), is("::"));

        ipAddressMasked = Ip6Address.makeMaskedAddress(ipAddress, 128);
        assertThat(ipAddressMasked.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8885"));

        ipAddressMasked = Ip6Address.makeMaskedAddress(ipAddress, 64);
        assertThat(ipAddressMasked.toString(), is("1111:2222:3333:4444::"));
    }

    /**
     * Tests making of a masked address for invalid prefix length for IPv6:
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMakeNegativeMaskedAddressIPv6() {
        Ip6Address ipAddress =
            Ip6Address.valueOf("1111:2222:3333:4444:5555:6666:7777:8885");
        Ip6Address ipAddressMasked;

        ipAddressMasked = Ip6Address.makeMaskedAddress(ipAddress, -1);
    }

    /**
     * Tests making of a masked address for an invalid prefix length for IPv6:
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMakeTooLongMaskedAddressIPv6() {
        Ip6Address ipAddress =
            Ip6Address.valueOf("1111:2222:3333:4444:5555:6666:7777:8885");
        Ip6Address ipAddressMasked;

        ipAddressMasked = Ip6Address.makeMaskedAddress(ipAddress, 129);
    }

    /**
     * Tests comparison of {@link Ip6Address} for IPv6.
     */
    @Test
    public void testComparisonIPv6() {
        Ip6Address addr1, addr2, addr3, addr4;

        addr1 = Ip6Address.valueOf("1111:2222:3333:4444:5555:6666:7777:8888");
        addr2 = Ip6Address.valueOf("1111:2222:3333:4444:5555:6666:7777:8888");
        addr3 = Ip6Address.valueOf("1111:2222:3333:4444:5555:6666:7777:8887");
        addr4 = Ip6Address.valueOf("1111:2222:3333:4444:5555:6666:7777:8889");
        assertTrue(addr1.compareTo(addr2) == 0);
        assertTrue(addr1.compareTo(addr3) > 0);
        assertTrue(addr1.compareTo(addr4) < 0);

        addr1 = Ip6Address.valueOf("ffff:2222:3333:4444:5555:6666:7777:8888");
        addr2 = Ip6Address.valueOf("ffff:2222:3333:4444:5555:6666:7777:8888");
        addr3 = Ip6Address.valueOf("ffff:2222:3333:4444:5555:6666:7777:8887");
        addr4 = Ip6Address.valueOf("ffff:2222:3333:4444:5555:6666:7777:8889");
        assertTrue(addr1.compareTo(addr2) == 0);
        assertTrue(addr1.compareTo(addr3) > 0);
        assertTrue(addr1.compareTo(addr4) < 0);

        addr1 = Ip6Address.valueOf("ffff:2222:3333:4444:5555:6666:7777:8888");
        addr2 = Ip6Address.valueOf("ffff:2222:3333:4444:5555:6666:7777:8888");
        addr3 = Ip6Address.valueOf("ffff:2222:3333:4443:5555:6666:7777:8888");
        addr4 = Ip6Address.valueOf("ffff:2222:3333:4445:5555:6666:7777:8888");
        assertTrue(addr1.compareTo(addr2) == 0);
        assertTrue(addr1.compareTo(addr3) > 0);
        assertTrue(addr1.compareTo(addr4) < 0);
    }

    /**
     * Tests equality of {@link Ip6Address} for IPv6.
     */
    @Test
    public void testEqualityIPv6() {
        new EqualsTester()
            .addEqualityGroup(
                Ip6Address.valueOf("1111:2222:3333:4444:5555:6666:7777:8888"),
                Ip6Address.valueOf("1111:2222:3333:4444:5555:6666:7777:8888"))
            .addEqualityGroup(
                Ip6Address.valueOf("1111:2222:3333:4444:5555:6666:7777:888a"),
                Ip6Address.valueOf("1111:2222:3333:4444:5555:6666:7777:888a"))
            .addEqualityGroup(
                Ip6Address.valueOf("::"),
                Ip6Address.valueOf("::"))
            .addEqualityGroup(
                Ip6Address.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"),
                Ip6Address.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"))
            .testEquals();
    }

    /**
     * Tests object string representation for IPv6.
     */
    @Test
    public void testToStringIPv6() {
        Ip6Address ipAddress;

        ipAddress =
            Ip6Address.valueOf("1111:2222:3333:4444:5555:6666:7777:8888");
        assertThat(ipAddress.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888"));

        ipAddress = Ip6Address.valueOf("1111::8888");
        assertThat(ipAddress.toString(), is("1111::8888"));

        ipAddress = Ip6Address.valueOf("1111::");
        assertThat(ipAddress.toString(), is("1111::"));

        ipAddress = Ip6Address.valueOf("::8888");
        assertThat(ipAddress.toString(), is("::8888"));

        ipAddress = Ip6Address.valueOf("::");
        assertThat(ipAddress.toString(), is("::"));

        ipAddress =
            Ip6Address.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
        assertThat(ipAddress.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));
    }
}
