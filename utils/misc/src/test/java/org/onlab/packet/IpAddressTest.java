/*
 * Copyright 2014-present Open Networking Laboratory
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

import java.net.InetAddress;

import org.junit.Test;

import com.google.common.net.InetAddresses;
import com.google.common.testing.EqualsTester;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutableBaseClass;

/**
 * Tests for class {@link IpAddress}.
 */
public class IpAddressTest {
    /**
     * Tests the immutability of {@link IpAddress}.
     */
    @Test
    public void testImmutable() {
        assertThatClassIsImmutableBaseClass(IpAddress.class);
    }

    /**
     * Tests the length of the address in bytes (octets).
     */
    @Test
    public void testAddrByteLength() {
        assertThat(IpAddress.INET_BYTE_LENGTH, is(4));
        assertThat(IpAddress.INET6_BYTE_LENGTH, is(16));
        assertThat(IpAddress.byteLength(IpAddress.Version.INET), is(4));
        assertThat(IpAddress.byteLength(IpAddress.Version.INET6), is(16));
    }

    /**
     * Tests the length of the address in bits.
     */
    @Test
    public void testAddrBitLength() {
        assertThat(IpAddress.INET_BIT_LENGTH, is(32));
        assertThat(IpAddress.INET6_BIT_LENGTH, is(128));
    }

    /**
     * Tests returning the IP address version.
     */
    @Test
    public void testVersion() {
        IpAddress ipAddress;

        // IPv4
        ipAddress = IpAddress.valueOf("0.0.0.0");
        assertThat(ipAddress.version(), is(IpAddress.Version.INET));

        // IPv6
        ipAddress = IpAddress.valueOf("::");
        assertThat(ipAddress.version(), is(IpAddress.Version.INET6));
    }

    /**
     * Tests whether the IP version of an address is IPv4.
     */
    @Test
    public void testIsIp4() {
        IpAddress ipAddress;

        // IPv4
        ipAddress = IpAddress.valueOf("0.0.0.0");
        assertTrue(ipAddress.isIp4());

        // IPv6
        ipAddress = IpAddress.valueOf("::");
        assertFalse(ipAddress.isIp4());
    }

    /**
     * Tests whether the IP version of an address is IPv6.
     */
    @Test
    public void testIsIp6() {
        IpAddress ipAddress;

        // IPv4
        ipAddress = IpAddress.valueOf("0.0.0.0");
        assertFalse(ipAddress.isIp6());

        // IPv6
        ipAddress = IpAddress.valueOf("::");
        assertTrue(ipAddress.isIp6());
    }

    /**
     * Tests getting the Ip4Address and Ip6Address view of the IP address.
     */
    @Test
    public void testGetIp4AndIp6AddressView() {
        IpAddress ipAddress;
        Ip4Address ip4Address;
        Ip6Address ip6Address;

        // Pure IPv4 IpAddress
        ipAddress = IpAddress.valueOf("1.2.3.4");
        ip4Address = ipAddress.getIp4Address();
        ip6Address = ipAddress.getIp6Address();
        assertThat(ip4Address.toString(), is("1.2.3.4"));
        assertNull(ip6Address);

        // IPv4 IpAddress that is Ip4Address
        ipAddress = Ip4Address.valueOf("1.2.3.4");
        ip4Address = ipAddress.getIp4Address();
        ip6Address = ipAddress.getIp6Address();
        assertThat(ip4Address.toString(), is("1.2.3.4"));
        assertNull(ip6Address);

        // Pure IPv6 IpAddress
        ipAddress = IpAddress.valueOf("1111:2222::");
        ip4Address = ipAddress.getIp4Address();
        ip6Address = ipAddress.getIp6Address();
        assertNull(ip4Address);
        assertThat(ip6Address.toString(), is("1111:2222::"));

        // IPv6 IpAddress that is Ip6Address
        ipAddress = Ip6Address.valueOf("1111:2222::");
        ip4Address = ipAddress.getIp4Address();
        ip6Address = ipAddress.getIp6Address();
        assertNull(ip4Address);
        assertThat(ip6Address.toString(), is("1111:2222::"));
    }

    /**
     * Tests returning an IPv4 address as a byte array.
     */
    @Test
    public void testAddressToOctetsIPv4() {
        IpAddress ipAddress;
        byte[] value;

        value = new byte[] {1, 2, 3, 4};
        ipAddress = IpAddress.valueOf("1.2.3.4");
        assertThat(ipAddress.toOctets(), is(value));

        value = new byte[] {0, 0, 0, 0};
        ipAddress = IpAddress.valueOf("0.0.0.0");
        assertThat(ipAddress.toOctets(), is(value));

        value = new byte[] {(byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff};
        ipAddress = IpAddress.valueOf("255.255.255.255");
        assertThat(ipAddress.toOctets(), is(value));
    }

    /**
     * Tests returning an IPv6 address as a byte array.
     */
    @Test
    public void testAddressToOctetsIPv6() {
        IpAddress ipAddress;
        byte[] value;

        value = new byte[] {0x11, 0x11, 0x22, 0x22,
                            0x33, 0x33, 0x44, 0x44,
                            0x55, 0x55, 0x66, 0x66,
                            0x77, 0x77,
                            (byte) 0x88, (byte) 0x88};
        ipAddress =
            IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:8888");
        assertThat(ipAddress.toOctets(), is(value));

        value = new byte[] {0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00};
        ipAddress = IpAddress.valueOf("::");
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
            IpAddress.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
        assertThat(ipAddress.toOctets(), is(value));
    }

    /**
     * Tests valueOf() converter for IPv4 integer value.
     */
    @Test
    public void testValueOfForIntegerIPv4() {
        IpAddress ipAddress;

        ipAddress = IpAddress.valueOf(0x01020304);
        assertThat(ipAddress.toString(), is("1.2.3.4"));

        ipAddress = IpAddress.valueOf(0);
        assertThat(ipAddress.toString(), is("0.0.0.0"));

        ipAddress = IpAddress.valueOf(0xffffffff);
        assertThat(ipAddress.toString(), is("255.255.255.255"));
    }

    /**
     * Tests valueOf() converter for IPv4 byte array.
     */
    @Test
    public void testValueOfByteArrayIPv4() {
        IpAddress ipAddress;
        byte[] value;

        value = new byte[] {1, 2, 3, 4};
        ipAddress = IpAddress.valueOf(IpAddress.Version.INET, value);
        assertThat(ipAddress.toString(), is("1.2.3.4"));

        value = new byte[] {0, 0, 0, 0};
        ipAddress = IpAddress.valueOf(IpAddress.Version.INET, value);
        assertThat(ipAddress.toString(), is("0.0.0.0"));

        value = new byte[] {(byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff};
        ipAddress = IpAddress.valueOf(IpAddress.Version.INET, value);
        assertThat(ipAddress.toString(), is("255.255.255.255"));
    }

    /**
     * Tests valueOf() converter for IPv6 byte array.
     */
    @Test
    public void testValueOfByteArrayIPv6() {
        IpAddress ipAddress;
        byte[] value;

        value = new byte[] {0x11, 0x11, 0x22, 0x22,
                            0x33, 0x33, 0x44, 0x44,
                            0x55, 0x55, 0x66, 0x66,
                            0x77, 0x77,
                            (byte) 0x88, (byte) 0x88};
        ipAddress = IpAddress.valueOf(IpAddress.Version.INET6, value);
        assertThat(ipAddress.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888"));

        value = new byte[] {0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00};
        ipAddress = IpAddress.valueOf(IpAddress.Version.INET6, value);
        assertThat(ipAddress.toString(), is("::"));

        value = new byte[] {(byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff};
        ipAddress = IpAddress.valueOf(IpAddress.Version.INET6, value);
        assertThat(ipAddress.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));
    }

    /**
     * Tests invalid valueOf() converter for a null array for IPv4.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidValueOfNullArrayIPv4() {
        IpAddress ipAddress;
        byte[] value;

        value = null;
        ipAddress = IpAddress.valueOf(IpAddress.Version.INET, value);
    }

    /**
     * Tests invalid valueOf() converter for a null array for IPv6.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidValueOfNullArrayIPv6() {
        IpAddress ipAddress;
        byte[] value;

        value = null;
        ipAddress = IpAddress.valueOf(IpAddress.Version.INET6, value);
    }

    /**
     * Tests invalid valueOf() converger for an array that is too short for
     * IPv4.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfShortArrayIPv4() {
        IpAddress ipAddress;
        byte[] value;

        value = new byte[] {1, 2, 3};
        ipAddress = IpAddress.valueOf(IpAddress.Version.INET, value);
    }

    /**
     * Tests invalid valueOf() converger for an array that is too short for
     * IPv6.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfShortArrayIPv6() {
        IpAddress ipAddress;
        byte[] value;

        value = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9};
        ipAddress = IpAddress.valueOf(IpAddress.Version.INET6, value);
    }

    /**
     * Tests valueOf() converter for IPv4 byte array and an offset.
     */
    @Test
    public void testValueOfByteArrayOffsetIPv4() {
        IpAddress ipAddress;
        byte[] value;

        value = new byte[] {11, 22, 33,                 // Preamble
                            1, 2, 3, 4,
                            44, 55};                    // Extra bytes
        ipAddress = IpAddress.valueOf(IpAddress.Version.INET, value, 3);
        assertThat(ipAddress.toString(), is("1.2.3.4"));

        value = new byte[] {11, 22,                     // Preamble
                            0, 0, 0, 0,
                            33};                        // Extra bytes
        ipAddress = IpAddress.valueOf(IpAddress.Version.INET, value, 2);
        assertThat(ipAddress.toString(), is("0.0.0.0"));

        value = new byte[] {11, 22,                     // Preamble
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            33};                        // Extra bytes
        ipAddress = IpAddress.valueOf(IpAddress.Version.INET, value, 2);
        assertThat(ipAddress.toString(), is("255.255.255.255"));
    }

    /**
     * Tests valueOf() converter for IPv6 byte array and an offset.
     */
    @Test
    public void testValueOfByteArrayOffsetIPv6() {
        IpAddress ipAddress;
        byte[] value;

        value = new byte[] {11, 22, 33,                 // Preamble
                            0x11, 0x11, 0x22, 0x22,
                            0x33, 0x33, 0x44, 0x44,
                            0x55, 0x55, 0x66, 0x66,
                            0x77, 0x77,
                            (byte) 0x88, (byte) 0x88,
                            44, 55};                    // Extra bytes
        ipAddress = IpAddress.valueOf(IpAddress.Version.INET6, value, 3);
        assertThat(ipAddress.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888"));

        value = new byte[] {11, 22,                     // Preamble
                            0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00,
                            33};                        // Extra bytes
        ipAddress = IpAddress.valueOf(IpAddress.Version.INET6, value, 2);
        assertThat(ipAddress.toString(), is("::"));

        value = new byte[] {11, 22,                     // Preamble
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            33};                        // Extra bytes
        ipAddress = IpAddress.valueOf(IpAddress.Version.INET6, value, 2);
        assertThat(ipAddress.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));
    }

    /**
     * Tests invalid valueOf() converger for an array and an invalid offset
     * for IPv4.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfArrayInvalidOffsetIPv4() {
        IpAddress ipAddress;
        byte[] value;

        value = new byte[] {11, 22, 33,                 // Preamble
                            1, 2, 3, 4,
                            44, 55};                    // Extra bytes
        ipAddress = IpAddress.valueOf(IpAddress.Version.INET, value, 6);
    }

    /**
     * Tests invalid valueOf() converger for an array and an invalid offset
     * for IPv6.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfArrayInvalidOffsetIPv6() {
        IpAddress ipAddress;
        byte[] value;

        value = new byte[] {11, 22, 33,                 // Preamble
                            0x11, 0x11, 0x22, 0x22,
                            0x33, 0x33, 0x44, 0x44,
                            0x55, 0x55, 0x66, 0x66,
                            0x77, 0x77,
                            (byte) 0x88, (byte) 0x88,
                            44, 55};                    // Extra bytes
        ipAddress = IpAddress.valueOf(IpAddress.Version.INET6, value, 6);
    }

    /**
     * Tests valueOf() converter for IPv4 InetAddress.
     */
    @Test
    public void testValueOfInetAddressIPv4() {
        IpAddress ipAddress;
        InetAddress inetAddress;

        inetAddress = InetAddresses.forString("1.2.3.4");
        ipAddress = IpAddress.valueOf(inetAddress);
        assertThat(ipAddress.toString(), is("1.2.3.4"));

        inetAddress = InetAddresses.forString("0.0.0.0");
        ipAddress = IpAddress.valueOf(inetAddress);
        assertThat(ipAddress.toString(), is("0.0.0.0"));

        inetAddress = InetAddresses.forString("255.255.255.255");
        ipAddress = IpAddress.valueOf(inetAddress);
        assertThat(ipAddress.toString(), is("255.255.255.255"));
    }

    /**
     * Tests valueOf() converter for IPv6 InetAddress.
     */
    @Test
    public void testValueOfInetAddressIPv6() {
        IpAddress ipAddress;
        InetAddress inetAddress;

        inetAddress =
            InetAddresses.forString("1111:2222:3333:4444:5555:6666:7777:8888");
        ipAddress = IpAddress.valueOf(inetAddress);
        assertThat(ipAddress.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888"));

        inetAddress = InetAddresses.forString("::");
        ipAddress = IpAddress.valueOf(inetAddress);
        assertThat(ipAddress.toString(), is("::"));

        inetAddress =
            InetAddresses.forString("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
        ipAddress = IpAddress.valueOf(inetAddress);
        assertThat(ipAddress.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));
    }

    /**
     * Tests valueOf() converter for IPv4 string.
     */
    @Test
    public void testValueOfStringIPv4() {
        IpAddress ipAddress;

        ipAddress = IpAddress.valueOf("1.2.3.4");
        assertThat(ipAddress.toString(), is("1.2.3.4"));

        ipAddress = IpAddress.valueOf("0.0.0.0");
        assertThat(ipAddress.toString(), is("0.0.0.0"));

        ipAddress = IpAddress.valueOf("255.255.255.255");
        assertThat(ipAddress.toString(), is("255.255.255.255"));
    }

    /**
     * Tests valueOf() converter for IPv6 string.
     */
    @Test
    public void testValueOfStringIPv6() {
        IpAddress ipAddress;

        ipAddress =
            IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:8888");
        assertThat(ipAddress.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888"));

        ipAddress = IpAddress.valueOf("::");
        assertThat(ipAddress.toString(), is("::"));

        ipAddress =
            IpAddress.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
        assertThat(ipAddress.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));
    }

    /**
     * Tests invalid valueOf() converter for a null string.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidValueOfNullString() {
        IpAddress ipAddress;

        String fromString = null;
        ipAddress = IpAddress.valueOf(fromString);
    }

    /**
     * Tests invalid valueOf() converter for an empty string.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfEmptyString() {
        IpAddress ipAddress;

        String fromString = "";
        ipAddress = IpAddress.valueOf(fromString);
    }

    /**
     * Tests invalid valueOf() converter for an incorrect string.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfIncorrectString() {
        IpAddress ipAddress;

        String fromString = "NoSuchIpAddress";
        ipAddress = IpAddress.valueOf(fromString);
    }

    /**
     * Tests making a mask prefix for a given prefix length for IPv4.
     */
    @Test
    public void testMakeMaskPrefixIPv4() {
        IpAddress ipAddress;

        ipAddress = IpAddress.makeMaskPrefix(IpAddress.Version.INET, 25);
        assertThat(ipAddress.toString(), is("255.255.255.128"));

        ipAddress = IpAddress.makeMaskPrefix(IpAddress.Version.INET, 0);
        assertThat(ipAddress.toString(), is("0.0.0.0"));

        ipAddress = IpAddress.makeMaskPrefix(IpAddress.Version.INET, 32);
        assertThat(ipAddress.toString(), is("255.255.255.255"));
    }

    /**
     * Tests making a mask prefix for a given prefix length for IPv6.
     */
    @Test
    public void testMakeMaskPrefixIPv6() {
        IpAddress ipAddress;

        ipAddress = IpAddress.makeMaskPrefix(IpAddress.Version.INET6, 8);
        assertThat(ipAddress.toString(), is("ff00::"));

        ipAddress = IpAddress.makeMaskPrefix(IpAddress.Version.INET6, 120);
        assertThat(ipAddress.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ff00"));

        ipAddress = IpAddress.makeMaskPrefix(IpAddress.Version.INET6, 0);
        assertThat(ipAddress.toString(), is("::"));

        ipAddress = IpAddress.makeMaskPrefix(IpAddress.Version.INET6, 128);
        assertThat(ipAddress.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));

        ipAddress = IpAddress.makeMaskPrefix(IpAddress.Version.INET6, 64);
        assertThat(ipAddress.toString(), is("ffff:ffff:ffff:ffff::"));
    }

    /**
     * Tests making a mask prefix for an invalid prefix length for IPv4:
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMakeNegativeMaskPrefixIPv4() {
        IpAddress ipAddress;

        ipAddress = IpAddress.makeMaskPrefix(IpAddress.Version.INET, -1);
    }

    /**
     * Tests making a mask prefix for an invalid prefix length for IPv6:
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMakeNegativeMaskPrefixIPv6() {
        IpAddress ipAddress;

        ipAddress = IpAddress.makeMaskPrefix(IpAddress.Version.INET6, -1);
    }

    /**
     * Tests making a mask prefix for an invalid prefix length for IPv4:
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMakeTooLongMaskPrefixIPv4() {
        IpAddress ipAddress;

        ipAddress = IpAddress.makeMaskPrefix(IpAddress.Version.INET, 33);
    }

    /**
     * Tests making a mask prefix for an invalid prefix length for IPv6:
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMakeTooLongMaskPrefixIPv6() {
        IpAddress ipAddress;

        ipAddress = IpAddress.makeMaskPrefix(IpAddress.Version.INET6, 129);
    }

    /**
     * Tests making of a masked address for IPv4.
     */
    @Test
    public void testMakeMaskedAddressIPv4() {
        IpAddress ipAddress = IpAddress.valueOf("1.2.3.5");
        IpAddress ipAddressMasked;

        ipAddressMasked = IpAddress.makeMaskedAddress(ipAddress, 24);
        assertThat(ipAddressMasked.toString(), is("1.2.3.0"));

        ipAddressMasked = IpAddress.makeMaskedAddress(ipAddress, 0);
        assertThat(ipAddressMasked.toString(), is("0.0.0.0"));

        ipAddressMasked = IpAddress.makeMaskedAddress(ipAddress, 32);
        assertThat(ipAddressMasked.toString(), is("1.2.3.5"));
    }

    /**
     * Tests making of a masked address for IPv6.
     */
    @Test
    public void testMakeMaskedAddressIPv6() {
        IpAddress ipAddress =
            IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:8885");
        IpAddress ipAddressMasked;

        ipAddressMasked = IpAddress.makeMaskedAddress(ipAddress, 8);
        assertThat(ipAddressMasked.toString(), is("1100::"));

        ipAddressMasked = IpAddress.makeMaskedAddress(ipAddress, 120);
        assertThat(ipAddressMasked.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8800"));

        ipAddressMasked = IpAddress.makeMaskedAddress(ipAddress, 0);
        assertThat(ipAddressMasked.toString(), is("::"));

        ipAddressMasked = IpAddress.makeMaskedAddress(ipAddress, 128);
        assertThat(ipAddressMasked.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8885"));

        ipAddressMasked = IpAddress.makeMaskedAddress(ipAddress, 64);
        assertThat(ipAddressMasked.toString(), is("1111:2222:3333:4444::"));
    }

    /**
     * Tests making of a masked address for invalid prefix length for IPv4:
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMakeNegativeMaskedAddressIPv4() {
        IpAddress ipAddress = IpAddress.valueOf("1.2.3.5");
        IpAddress ipAddressMasked;

        ipAddressMasked = IpAddress.makeMaskedAddress(ipAddress, -1);
    }

    /**
     * Tests making of a masked address for invalid prefix length for IPv6:
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMakeNegativeMaskedAddressIPv6() {
        IpAddress ipAddress =
            IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:8885");
        IpAddress ipAddressMasked;

        ipAddressMasked = IpAddress.makeMaskedAddress(ipAddress, -1);
    }

    /**
     * Tests making of a masked address for an invalid prefix length for IPv4:
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMakeTooLongMaskedAddressIPv4() {
        IpAddress ipAddress = IpAddress.valueOf("1.2.3.5");
        IpAddress ipAddressMasked;

        ipAddressMasked = IpAddress.makeMaskedAddress(ipAddress, 33);
    }

    /**
     * Tests making of a masked address for an invalid prefix length for IPv6:
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMakeTooLongMaskedAddressIPv6() {
        IpAddress ipAddress =
            IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:8885");
        IpAddress ipAddressMasked;

        ipAddressMasked = IpAddress.makeMaskedAddress(ipAddress, 129);
    }

    /**
     * Tests if address is zero for IPv4.
     */
    @Test
    public void testIsZeroIPv4() {
        IpAddress normalIP = IpAddress.valueOf("10.0.0.1");
        IpAddress zeroIP = IpAddress.valueOf("0.0.0.0");
        assertFalse(normalIP.isZero());
        assertTrue(zeroIP.isZero());
    }

    /**
     * Tests if address is zero for IPv6.
     */
    @Test
    public void testIsZeroIPv6() {
        IpAddress normalIP = IpAddress.valueOf("fe80::1");
        IpAddress zeroIP = IpAddress.valueOf("::");
        assertFalse(normalIP.isZero());
        assertTrue(zeroIP.isZero());
    }

    /**
     * Tests if address is self-assigned for IPv4.
     */
    @Test
    public void testIsSelfAssignedIpv4() {
        IpAddress normalIP = IpAddress.valueOf("10.0.0.1");
        IpAddress selfAssignedIP = IpAddress.valueOf("169.1.2.3");
        assertFalse(normalIP.isSelfAssigned());
        assertTrue(selfAssignedIP.isSelfAssigned());
    }

    /**
     * Tests if the address is a multicast address.
     */
    @Test
    public void testIsMulticast() {
        IpAddress v4Unicast = IpAddress.valueOf("10.0.0.1");
        IpAddress v4Multicast = IpAddress.valueOf("224.0.0.1");
        IpAddress v6Unicast = IpAddress.valueOf("1000::1");
        IpAddress v6Multicast = IpAddress.valueOf("ff02::1");
        assertFalse(v4Unicast.isMulticast());
        assertTrue(v4Multicast.isMulticast());
        assertFalse(v6Unicast.isMulticast());
        assertTrue(v6Multicast.isMulticast());
    }

    /**
     * Tests comparison of {@link IpAddress} for IPv4.
     */
    @Test
    public void testComparisonIPv4() {
        IpAddress addr1, addr2, addr3, addr4;

        addr1 = IpAddress.valueOf("1.2.3.4");
        addr2 = IpAddress.valueOf("1.2.3.4");
        addr3 = IpAddress.valueOf("1.2.3.3");
        addr4 = IpAddress.valueOf("1.2.3.5");
        assertTrue(addr1.compareTo(addr2) == 0);
        assertTrue(addr1.compareTo(addr3) > 0);
        assertTrue(addr1.compareTo(addr4) < 0);

        addr1 = IpAddress.valueOf("255.2.3.4");
        addr2 = IpAddress.valueOf("255.2.3.4");
        addr3 = IpAddress.valueOf("255.2.3.3");
        addr4 = IpAddress.valueOf("255.2.3.5");
        assertTrue(addr1.compareTo(addr2) == 0);
        assertTrue(addr1.compareTo(addr3) > 0);
        assertTrue(addr1.compareTo(addr4) < 0);
    }

    /**
     * Tests comparison of {@link IpAddress} for IPv6.
     */
    @Test
    public void testComparisonIPv6() {
        IpAddress addr1, addr2, addr3, addr4;

        addr1 = IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:8888");
        addr2 = IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:8888");
        addr3 = IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:8887");
        addr4 = IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:8889");
        assertTrue(addr1.compareTo(addr2) == 0);
        assertTrue(addr1.compareTo(addr3) > 0);
        assertTrue(addr1.compareTo(addr4) < 0);

        addr1 = IpAddress.valueOf("ffff:2222:3333:4444:5555:6666:7777:8888");
        addr2 = IpAddress.valueOf("ffff:2222:3333:4444:5555:6666:7777:8888");
        addr3 = IpAddress.valueOf("ffff:2222:3333:4444:5555:6666:7777:8887");
        addr4 = IpAddress.valueOf("ffff:2222:3333:4444:5555:6666:7777:8889");
        assertTrue(addr1.compareTo(addr2) == 0);
        assertTrue(addr1.compareTo(addr3) > 0);
        assertTrue(addr1.compareTo(addr4) < 0);

        addr1 = IpAddress.valueOf("ffff:2222:3333:4444:5555:6666:7777:8888");
        addr2 = IpAddress.valueOf("ffff:2222:3333:4444:5555:6666:7777:8888");
        addr3 = IpAddress.valueOf("ffff:2222:3333:4443:5555:6666:7777:8888");
        addr4 = IpAddress.valueOf("ffff:2222:3333:4445:5555:6666:7777:8888");
        assertTrue(addr1.compareTo(addr2) == 0);
        assertTrue(addr1.compareTo(addr3) > 0);
        assertTrue(addr1.compareTo(addr4) < 0);
    }

    /**
     * Tests equality of {@link IpAddress} for IPv4.
     */
    @Test
    public void testEqualityIPv4() {
        new EqualsTester()
            .addEqualityGroup(IpAddress.valueOf("1.2.3.4"),
                              IpAddress.valueOf("1.2.3.4"))
            .addEqualityGroup(IpAddress.valueOf("1.2.3.5"),
                              IpAddress.valueOf("1.2.3.5"))
            .addEqualityGroup(IpAddress.valueOf("0.0.0.0"),
                              IpAddress.valueOf("0.0.0.0"))
            .addEqualityGroup(IpAddress.valueOf("255.255.255.255"),
                              IpAddress.valueOf("255.255.255.255"))
            .testEquals();
    }

    /**
     * Tests equality of {@link IpAddress} for IPv6.
     */
    @Test
    public void testEqualityIPv6() {
        new EqualsTester()
            .addEqualityGroup(
                IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:8888"),
                IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:8888"))
            .addEqualityGroup(
                IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:888a"),
                IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:888a"))
            .addEqualityGroup(
                IpAddress.valueOf("::"),
                IpAddress.valueOf("::"))
            .addEqualityGroup(
                IpAddress.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"),
                IpAddress.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"))
            .testEquals();
    }

    /**
     * Tests object string representation for IPv4.
     */
    @Test
    public void testToStringIPv4() {
        IpAddress ipAddress;

        ipAddress = IpAddress.valueOf("1.2.3.4");
        assertThat(ipAddress.toString(), is("1.2.3.4"));

        ipAddress = IpAddress.valueOf("0.0.0.0");
        assertThat(ipAddress.toString(), is("0.0.0.0"));

        ipAddress = IpAddress.valueOf("255.255.255.255");
        assertThat(ipAddress.toString(), is("255.255.255.255"));
    }

    /**
     * Tests object string representation for IPv6.
     */
    @Test
    public void testToStringIPv6() {
        IpAddress ipAddress;

        ipAddress =
            IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:8888");
        assertThat(ipAddress.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888"));

        ipAddress = IpAddress.valueOf("1111::8888");
        assertThat(ipAddress.toString(), is("1111::8888"));

        ipAddress = IpAddress.valueOf("1111::");
        assertThat(ipAddress.toString(), is("1111::"));

        ipAddress = IpAddress.valueOf("::8888");
        assertThat(ipAddress.toString(), is("::8888"));

        ipAddress = IpAddress.valueOf("::");
        assertThat(ipAddress.toString(), is("::"));

        ipAddress =
            IpAddress.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
        assertThat(ipAddress.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));

        ipAddress =
                IpAddress.valueOf("::1111:2222");
        assertThat(ipAddress.toString(),
                is("::1111:2222"));

        ipAddress =
                IpAddress.valueOf("1:0:0:1:0:0:2:3");
        assertThat(ipAddress.toString(),
                is("1::1:0:0:2:3"));

        ipAddress =
                IpAddress.valueOf("::0123:0004");
        assertThat(ipAddress.toString(),
                is("::123:4"));

        ipAddress =
                IpAddress.valueOf("0:0:1:1:0:0:1:1");
        assertThat(ipAddress.toString(),
                is("::1:1:0:0:1:1"));

        ipAddress =
                IpAddress.valueOf("1:1a2b::");
        assertThat(ipAddress.toString(),
                is("1:1a2b::"));

        ipAddress =
                IpAddress.valueOf("0:0:00:00:0000:00:00:000");
        assertThat(ipAddress.toString(),
                is("::"));

        ipAddress =
                IpAddress.valueOf("0:0:0:1:0:0:0:0");
        assertThat(ipAddress.toString(),
                is("0:0:0:1::"));
    }
}
